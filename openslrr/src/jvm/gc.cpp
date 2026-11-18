#include "slrr/jvm.hpp"
#include <algorithm>
#include <chrono>
#include <cstdio>
#include <cstdlib>
#include <limits>
#include <unordered_set>

namespace slrr {
Jvm::RootScope::RootScope(Jvm& vm,GcRootProvider provider):vm_(vm),id_(++vm.gc_frame_id_) {
  vm_.gc_frames_.emplace(id_,std::move(provider));
}
Jvm::RootScope::~RootScope(){vm_.gc_frames_.erase(id_);}
void Jvm::exchange_execution_state(ExecutionState& state) {
  std::swap(state.calls,gc_call_depth_);
  std::swap(state.invokes,invoke_depth_);
  std::swap(state.constructors,field_init_depth_);
}
void Jvm::add_gc_roots(GcRootProvider provider) {
  gc_roots_.push_back(std::move(provider));
}
int Jvm::request_gc() {
  if (gc_report_ready_ && !gc_running_) {
    gc_report_ready_ = false;
    return gc_report_;
  }
  gc_pending_ = true;
  return -1;
}
void Jvm::gc_safe_point() {
  // Drain dead objects gradually; a collection may also run here.
  if (!gc_graveyard_.empty()) {
    const size_t budget = std::min<size_t>(gc_graveyard_.size(), 256);
    for (size_t i = gc_graveyard_.size() - budget; i < gc_graveyard_.size(); ++i) {
      gc_managed_.erase(gc_graveyard_[i].get());
      host_input_forget(gc_graveyard_[i].get());
    }
    gc_graveyard_.resize(gc_graveyard_.size() - budget);
  }
  // Every heap insertion (fresh or recycled) is future dead weight: count
  // both toward the trigger or the heap balloons between collections.
  const size_t insertions = gc_allocated_ + gc_recycle_pops_;
  if (gc_pending_ || insertions >= gc_threshold_ ||
      heap_.size() >= gc_heap_cap_)
    collect_garbage();
}
int Jvm::collect_garbage() {
  // Active callbacks can pump nested frames. Only the quiescent host context
  // may sweep; suspended cooperative stacks have explicit frame-root scopes.
  if (gc_call_depth_ || gc_running_) {
    gc_pending_ = true;
    return -1;
  }
  gc_running_ = true;
  struct Guard { bool& running; ~Guard() { running = false; } } guard{gc_running_};
  const auto gc_start = std::chrono::steady_clock::now();
  const bool requested = gc_pending_;
  gc_pending_ = false;
  const size_t before = heap_.size();
  // Mark bits replace the per-GC `marked` set; gc_managed_ persists between
  // collections instead of being rebuilt from the whole heap every time.
  for (const auto& o : heap_) o->gc_marked = false;
  std::vector<Object*> work;
  auto mark = [&](const void* address) {
    auto* object = static_cast<Object*>(const_cast<void*>(address));
    // Membership first: the pointer may be stale, dereferencing it before
    // the heap registry check would read freed memory.
    if (object && gc_managed_.count(address) && !object->gc_marked) {
      object->gc_marked = true;
      work.push_back(object);
    }
  };
  auto mark_roots = [&] {
    work.clear();
    for (const auto& type : statics_)
      for (const auto& field : type.second)
        if (field.second.tag == ValueTag::Obj) mark(field.second.v.o);
    for (const auto& o : heap_) if (o->gc_disabled) mark(o.get());
    for (const auto& provider : gc_roots_) provider(mark);
    for (const auto& frame : gc_frames_) frame.second(mark);
  };
  auto trace = [&] {
    for (size_t cursor = 0; cursor < work.size(); ++cursor) {
      Object* o = work[cursor];
      for (const auto& field : o->fields)
        if (field.second.tag == ValueTag::Obj) mark(field.second.v.o);
      for (const Value& v : o->slots) if (v.tag == ValueTag::Obj) mark(v.v.o);
    }
  };
  mark_roots(); trace();
  const auto mark_end = std::chrono::steady_clock::now();
  std::vector<Object*> finalizers;
  std::vector<Object*> snapshot;
  snapshot.reserve(heap_.size());
  for (const auto& o : heap_) {
    snapshot.push_back(o.get());
  }
  for (Object* o : snapshot) {
    if (o->gc_marked || o->gc_finalized) continue;
    // Finalize-ability only depends on the class: cache it per class name.
    // The per-object inherited-method search used to dominate full sweeps.
    const auto cached = gc_finalize_cache_.find(o->class_name);
    bool has_finalize;
    if (cached != gc_finalize_cache_.end()) {
      has_finalize = cached->second;
    } else {
      std::string owner;
      const auto* method = find_method_inherited(o->class_name, "finalize", "()V", false, &owner);
      const auto* cls = find_class(owner);
      std::string native_owner;
      const bool bound = native_has_alias(owner, "finalize", "()V", &native_owner);
      const bool script = method && cls && method->tree < cls->trees.size() &&
                          !cls->trees[method->tree].empty();
      // String's native payload is already owned by Object::str. Its C++
      // destructor performs the release; do not make a VM call per temporary
      // string or clear a finalizer's input strings before that finalizer runs.
      has_finalize = (bound && native_owner != "java.lang.String") || script;
      gc_finalize_cache_.emplace(o->class_name, has_finalize);
    }
    if (has_finalize) finalizers.push_back(o);
  }
  const auto finalize_scan_end = std::chrono::steady_clock::now();
  for (Object* o : finalizers) {
    o->gc_finalized = true;
    invoke(o->class_name, "finalize", "()V", {Value::make_obj(o)}, false);
  }
  // A finalizer may resurrect an object, change native roots or allocate.
  // Trace again and keep this cycle's new allocations alive until next time.
  if (!finalizers.empty() || heap_.size() != before) {
    mark_roots();
    for (size_t i = before; i < heap_.size(); ++i) mark(heap_[i].get());
    trace();
  }
  const size_t sweep_size = heap_.size();
  // Dead objects are unreachable (the mark phase is atomic), so destroying
  // them can be spread over the following frames: the per-object dtors and
  // input bookkeeping used to add ~2 ms to every pause.
  size_t write = 0;
  for (size_t i = 0; i < heap_.size(); ++i) {
    auto& object = heap_[i];
    if (object->gc_marked) {
      heap_[write++] = std::move(object);
      continue;
    }
    // The registry entry is dropped when the object is actually destroyed
    // (drain). Erasing ~15k dead pointers here dominated the pause; a stale
    // entry is harmless - the pointer is only ever compared, and the
    // allocator reuses addresses, so the set stays bounded.
    // Recycle only field-bag classes with no native payload: their C++
    // destructors are no-ops once fields/strings are cleared. Resource
    // holders (RenderRef, ResourceRef...) must run their destructors, so
    // they always go to the graveyard.
    static const std::unordered_set<std::string> kRecyclable = {
        "java.lang.Vector3", "java.lang.Ypr"};
    if (kRecyclable.count(object->class_name)) {
      auto& pool = gc_recycle_[object->class_name];
      if (gc_recycled_total_ < 16384 && pool.size() < 8192) {
        // No field clears here: resetting state happens on pop (spread over
        // frames), pooling must stay inside the GC pause budget.
        ++gc_recycled_total_;
        pool.push_back(std::move(object));
        continue;
      }
    }
    gc_graveyard_.push_back(std::move(object));
  }
  heap_.resize(write);
  gc_last_freed_ = sweep_size - heap_.size();
  gc_alive_ = heap_.size();
  const auto sweep_end = std::chrono::steady_clock::now();
  const double mark_ms = std::chrono::duration<double, std::milli>(mark_end - gc_start).count();
  const double fin_ms =
      std::chrono::duration<double, std::milli>(finalize_scan_end - mark_end).count();
  const double sweep_ms =
      std::chrono::duration<double, std::milli>(sweep_end - finalize_scan_end).count();
  if (mark_ms + fin_ms + sweep_ms > 2. || getenv("SLRR_GC_HISTOGRAM")) {
    std::unordered_map<std::string, size_t> alive_types;
    for (const auto& o : heap_) ++alive_types[o->class_name];
    std::vector<std::pair<std::string, size_t>> top(alive_types.begin(), alive_types.end());
    std::partial_sort(top.begin(), top.begin() + std::min<size_t>(5, top.size()), top.end(),
                      [](const auto& a, const auto& b) { return a.second > b.second; });
    std::fprintf(stderr, "[gc-slow] mark=%.1f finalize_scan=%.1f sweep=%.1f ms |", mark_ms,
                 fin_ms, sweep_ms);
    for (size_t i = 0; i < top.size() && i < 5; ++i)
      std::fprintf(stderr, " %s=%zu", top[i].first.c_str(), top[i].second);
    std::fprintf(stderr, "\n");
  }
  if (requested) {
    gc_report_ = static_cast<int>(std::min(gc_last_freed_, static_cast<size_t>(std::numeric_limits<int>::max())));
    gc_report_ready_ = true;
  }
  {
    static const bool hist = [] {
      const char* e = std::getenv("SLRR_GC_HISTOGRAM");
      return e && *e == '1';
    }();
    gc_alloc_histogram_enabled_ = hist;
    if (hist) {
      const size_t total = gc_allocated_;
      std::vector<std::pair<std::string, size_t>> allocs(gc_alloc_histogram_.begin(),
                                                        gc_alloc_histogram_.end());
      std::partial_sort(allocs.begin(),
                        allocs.begin() + std::min<size_t>(6, allocs.size()), allocs.end(),
                        [](const auto& a, const auto& b) { return a.second > b.second; });
      std::fprintf(stderr, "[gc-alloc] objects=%zu pops=%zu misses=%zu pools:", total,
                   gc_recycle_pops_, gc_recycle_misses_);
      for (const auto& [cls, pool] : gc_recycle_)
        std::fprintf(stderr, " %s=%zu", cls.c_str(), pool.size());
      std::fprintf(stderr, " |", total);
      for (size_t i = 0; i < allocs.size() && i < 6; ++i)
        std::fprintf(stderr, " %s=%zu", allocs[i].first.c_str(), allocs[i].second);
      std::fprintf(stderr, "\n");
    }
    gc_alloc_histogram_.clear();
    gc_recycle_pops_ = gc_recycle_misses_ = 0;
  }
  gc_allocated_ = 0;
  const double gc_ms =
      std::chrono::duration<double, std::milli>(
          std::chrono::steady_clock::now() - gc_start)
          .count();
  // Threshold from the LIVE size, not the heap size: dead churn between
  // collections used to inflate the threshold and run the heap away.
  gc_threshold_ = std::max<size_t>(16384, gc_alive_ / 2);
  gc_heap_cap_ = std::max<size_t>(32768, gc_alive_ * 3);
  std::printf("[gc] %.1fms before=%zu alive=%zu freed=%zu finalized=%zu\n",
              gc_ms, before, heap_.size(), gc_last_freed_, finalizers.size());
  return static_cast<int>(std::min(gc_last_freed_, static_cast<size_t>(std::numeric_limits<int>::max())));
}
}

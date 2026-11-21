#include "slrr/stack_sampler.hpp"

#include <algorithm>
#include <atomic>
#include <chrono>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <map>
#include <string>
#include <thread>
#include <vector>

#ifdef _WIN32
#define NOMINMAX
#include <windows.h>
#include <dbghelp.h>
#endif

namespace slrr::stack_sample {
namespace {

#ifdef _WIN32

// One captured stack. Fixed size and POD so recording never allocates: the
// main thread is suspended while this is written.
constexpr int kFrames = 24;
struct RawSample {
  uint16_t count = 0;
  void* frames[kFrames]{};
};
// 2^18 samples at 500 Hz is over eight minutes of history; the ring keeps the
// most recent ones, which is what a freeze investigation needs.
constexpr size_t kRing = 1u << 18;

RawSample* ring = nullptr;
std::atomic<size_t> written{0};
std::atomic<bool> running{false};
std::thread sampler;
HANDLE target = nullptr;
DWORD target_tid = 0;
int interval_ms = 2;
int printed = 0;

std::string symbol_from(void* address) {
  char buffer[sizeof(SYMBOL_INFO) + 256] = {};
  auto* info = reinterpret_cast<SYMBOL_INFO*>(buffer);
  info->SizeOfStruct = sizeof(SYMBOL_INFO);
  info->MaxNameLen = 255;
  DWORD64 displacement = 0;
  if (SymFromAddr(GetCurrentProcess(), reinterpret_cast<DWORD64>(address),
                  &displacement, info))
    return std::string(info->Name);
  return std::string("0x") + std::to_string(reinterpret_cast<uintptr_t>(address));
}

std::string symbol_cached(std::map<void*, std::string>& cache, void* address) {
  auto found = cache.find(address);
  if (found != cache.end()) return found->second;
  const std::string name = symbol_from(address);
  cache.emplace(address, name);
  return name;
}

void report() {
  const size_t total = (std::min)(written.load(std::memory_order_relaxed), kRing);
  if (!total) return;
  std::map<void*, std::string> cache;
  std::map<std::string, uint64_t> self;
  std::map<std::string, uint64_t> stacks;
  uint64_t valid = 0;
  for (size_t i = 0; i < total; ++i) {
    const RawSample& sample = ring[i];
    if (!sample.count) continue;
    ++valid;
    ++self[symbol_cached(cache, sample.frames[0])];
    std::string joined;
    for (int f = 0; f < sample.count && f < 8; ++f) {
      if (f) joined += " <- ";
      joined += symbol_cached(cache, sample.frames[f]);
    }
    ++stacks[joined];
  }
  std::vector<std::pair<uint64_t, std::string>> top_self, top_stacks;
  for (const auto& [k, v] : self) top_self.push_back({v, k});
  for (const auto& [k, v] : stacks) top_stacks.push_back({v, k});
  const auto by_count = [](const auto& a, const auto& b) { return a.first > b.first; };
  std::sort(top_self.begin(), top_self.end(), by_count);
  std::sort(top_stacks.begin(), top_stacks.end(), by_count);
  std::fprintf(stderr, "[sample] %zu samples (%llu with a stack)\n", total,
               (unsigned long long)valid);
  for (size_t i = 0; i < top_self.size() && i < 24; ++i)
    std::fprintf(stderr, "[sample]  %5.1f%% %8llu  %s\n",
                 100.0 * double(top_self[i].first) / double(valid),
                 (unsigned long long)top_self[i].first, top_self[i].second.c_str());
  for (size_t i = 0; i < top_stacks.size() && i < 10; ++i)
    std::fprintf(stderr, "[sample]  stack %5.1f%%  %s\n",
                 100.0 * double(top_stacks[i].first) / double(valid),
                 top_stacks[i].second.c_str());
  std::fflush(stderr);
}

void capture() {
  CONTEXT context = {};
  context.ContextFlags = CONTEXT_FULL;
  if (!GetThreadContext(target, &context)) return;
  RawSample sample;
  STACKFRAME64 frame = {};
  frame.AddrPC.Offset = context.Rip;
  frame.AddrPC.Mode = AddrModeFlat;
  frame.AddrFrame.Offset = context.Rbp;
  frame.AddrFrame.Mode = AddrModeFlat;
  frame.AddrStack.Offset = context.Rsp;
  frame.AddrStack.Mode = AddrModeFlat;
  for (int i = 0; i < kFrames; ++i) {
    if (!StackWalk64(IMAGE_FILE_MACHINE_AMD64, GetCurrentProcess(), target, &frame,
                     &context, nullptr, SymFunctionTableAccess64, SymGetModuleBase64,
                     nullptr))
      break;
    if (!frame.AddrPC.Offset) break;
    sample.frames[sample.count++] =
        reinterpret_cast<void*>(frame.AddrPC.Offset);
  }
  const size_t slot = written.fetch_add(1, std::memory_order_relaxed);
  ring[slot % kRing] = sample;
}

void loop() {
  int ticks = 0;
  while (running.load(std::memory_order_relaxed)) {
    std::this_thread::sleep_for(std::chrono::milliseconds(interval_ms));
    if (!running.load(std::memory_order_relaxed)) break;
    if (SuspendThread(target) == static_cast<DWORD>(-1)) continue;
    capture();
    ResumeThread(target);
    if (++ticks % (5000 / interval_ms) == 0) report();
  }
  report();
}

#endif  // _WIN32

}  // namespace

void start() {
#ifdef _WIN32
  static bool done = false;
  if (done) return;
  done = true;
  if (!std::getenv("SLRR_STACK_SAMPLE")) return;
  if (const char* hz = std::getenv("SLRR_STACK_SAMPLE_HZ")) {
    const int value = std::atoi(hz);
    if (value >= 50 && value <= 2000) interval_ms = (std::max)(1, 1000 / value);
  }
  target_tid = GetCurrentThreadId();
  target = OpenThread(THREAD_SUSPEND_RESUME | THREAD_GET_CONTEXT, FALSE, target_tid);
  if (!target) {
    std::fprintf(stderr, "[sample] cannot open the main thread\n");
    return;
  }
  SymSetOptions(SYMOPT_UNDNAME | SYMOPT_DEFERRED_LOADS | SYMOPT_LOAD_LINES);
  if (!SymInitialize(GetCurrentProcess(), nullptr, TRUE))
    std::fprintf(stderr, "[sample] SymInitialize failed (%lu)\n", GetLastError());
  ring = static_cast<RawSample*>(
      VirtualAlloc(nullptr, sizeof(RawSample) * kRing, MEM_COMMIT | MEM_RESERVE,
                   PAGE_READWRITE));
  if (!ring) {
    std::fprintf(stderr, "[sample] cannot reserve the sample ring\n");
    return;
  }
  running.store(true);
  sampler = std::thread(loop);
  std::fprintf(stderr, "[sample] sampling tid %lu at %d ms\n",
               (unsigned long)target_tid, interval_ms);
#endif
}

void stop() {
#ifdef _WIN32
  if (!running.exchange(false)) return;
  if (sampler.joinable()) sampler.join();
  if (target) CloseHandle(target);
  target = nullptr;
#endif
}

}  // namespace slrr::stack_sample

#include "slrr/boot.hpp"
#include "slrr/game_root.hpp"
#include "slrr/jvm.hpp"
#include "slrr/native_registry.hpp"
#include "slrr/render_backend.hpp"
#include "slrr/script_compiler.hpp"
#include "slrr/dyno_model.hpp"
#include "slrr/sl2_pacejka.hpp"
#include "slrr/natives_boot.hpp"
#include "slrr/gameref.hpp"

#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <exception>
#include <string>
#include <vector>

#include "slrr/crash_log.hpp"

namespace {

void usage() {
  std::printf(
      "slrr_host — clean C++ host for Street Legal Racing Redline\n"
      "\n"
      "Usage:\n"
      "  slrr_host --probe [--root DIR]\n"
      "  slrr_host --probe-pacejka\n"
      "  slrr_host --probe-dyno\n"
      "  slrr_host --probe-gc [--root DIR]\n"
      "  slrr_host --crash-test\n"
      "  slrr_host --game [--root DIR] [--window] [--no-wait] [--auto-new]\n"
  "  slrr_host --root DIR --compile-scripts DIR [DIR...]\n"
      "           [--auto-cmd mainmenu|street|N]\n"
      "           [--spawn-car cars/racers/Baiern.rpk:0x6]\n"
      "           [--vehicle-osd PATH]\n"
      "           [--perf-log CSV|-] [--perf-frames CSV] [--bench-map TRACK]\n"
      "           [--bench-drive SECONDS|manual]\n"
      "           [--bench-time HOUR] [--bench-shots DIR] [--bench-route]\n"
      "           [--no-vsync] [--fps-cap N] [--debug-overlay]\n"
      "           [PlayerName]\n"
      "\n"
      "Default --game: FMV intros → Init(int) (Soft splash / menu).\n"
      "--auto-new skips to Garage (smoke). ESC skips boot intros.\n"
      "Game data: data/game, or SLRR_GAME_ROOT / --root.\n"
      "Reference only: open-SL/repos (do not edit as product code).\n");
}

}  // namespace

static int run_host(int argc, char** argv) {
  using namespace slrr;
  // Line-buffered stdout stalls a dialog on every log line. Keep a block
  // buffer; real failures still go to stderr.
  std::setvbuf(stdout, nullptr, _IOFBF, 1 << 16);

  bool want_probe = false;
  bool want_pacejka = false;
  bool want_dyno = false;
  bool want_game = false;
  bool want_gc = false;
  bool want_crash_test = false;
  BootOptions opt;
  const char* root_arg = nullptr;
  std::vector<std::string> compile_dirs;

  for (int i = 1; i < argc; ++i) {
    const char* a = argv[i];
    if (!a) continue;
    if (std::strcmp(a, "--help") == 0 || std::strcmp(a, "-h") == 0) {
      usage();
      return 0;
    }
    if (std::strcmp(a, "--probe") == 0) {
      want_probe = true;
      continue;
    }
    if (std::strcmp(a, "--probe-pacejka") == 0) {
      want_pacejka = true;
      continue;
    }
    if (std::strcmp(a, "--probe-dyno") == 0) {
      want_dyno = true;
      continue;
    }
    if (std::strcmp(a, "--probe-gc") == 0) {
      want_gc = true;
      continue;
    }
    if (std::strcmp(a, "--crash-test") == 0) {
      want_crash_test = true;
      continue;
    }
    if (std::strcmp(a, "--game") == 0) {
      want_game = true;
      continue;
    }
    if (std::strcmp(a, "--window") == 0) {
      opt.window = true;
      continue;
    }
    if (std::strcmp(a, "--compile-scripts") == 0 && i + 1 < argc) {
      compile_dirs.emplace_back(argv[++i]);
      continue;
    }
    if (std::strcmp(a, "--no-wait") == 0) {
      opt.auto_new = true;
      opt.max_frames = 3;
      continue;
    }
    if (std::strcmp(a, "--auto-new") == 0) {
      opt.auto_new = true;
      continue;
    }
    if(std::strcmp(a,"--spawn-car")==0&&i+1<argc){
      opt.spawn_car=argv[++i];opt.auto_new=true;continue;
    }
    if (std::strcmp(a, "--vehicle-osd") == 0 && i + 1 < argc) {
      opt.vehicle_osd = argv[++i];
      continue;
    }
    if (std::strcmp(a, "--auto-cmd") == 0 && i + 1 < argc) {
      const char* c = argv[++i];
      if (std::strcmp(c, "mainmenu") == 0)
        opt.auto_cmd = 101;
      else if (std::strcmp(c, "street") == 0)
        opt.auto_cmd = 109;
      else if (std::strcmp(c, "escape") == 0)
        opt.auto_cmd = 119;
      else
        opt.auto_cmd = static_cast<int32_t>(std::atoi(c));
      continue;
    }
    if (std::strcmp(a, "--perf-log") == 0 && i + 1 < argc) {
      opt.perf_log = true;
      const char* path = argv[++i];
      if (std::strcmp(path, "-") != 0) opt.perf_log_path = path;
      continue;
    }
    if (std::strcmp(a, "--perf-frames") == 0 && i + 1 < argc) {
      opt.perf_frames_path = argv[++i];
      continue;
    }
    if (std::strcmp(a, "--no-vsync") == 0) {
      opt.vsync = false;
      continue;
    }
    if (std::strcmp(a, "--fps-cap") == 0 && i + 1 < argc) {
      opt.fps_cap = static_cast<float>(std::atof(argv[++i]));
      continue;
    }
    if (std::strcmp(a, "--debug-overlay") == 0) {
      opt.debug_overlay = true;
      continue;
    }
    if (std::strcmp(a, "--bench-route") == 0) {
      opt.bench_route = true;
      continue;
    }
    if (std::strcmp(a, "--bench-time") == 0 && i + 1 < argc) {
      opt.bench_hour = static_cast<float>(std::atof(argv[++i]));
      continue;
    }
    if (std::strcmp(a, "--bench-shots") == 0 && i + 1 < argc) {
      opt.bench_shots = argv[++i];
      continue;
    }
    if (std::strcmp(a, "--bench-map") == 0 && i + 1 < argc) {
      opt.bench_map = argv[++i];
      continue;
    }
    if (std::strcmp(a, "--bench-drive") == 0 && i + 1 < argc) {
      if (_stricmp(argv[++i], "manual") == 0) {
        opt.bench_manual = true;
        opt.bench_drive_seconds = 1.f; // positive so the drive block runs
      } else {
        opt.bench_drive_seconds = static_cast<float>(std::atof(argv[i]));
      }
      continue;
    }
    if (std::strcmp(a, "--root") == 0 && i + 1 < argc) {
      root_arg = argv[++i];
      continue;
    }
    if (a[0] != '-') {
      opt.player_name = a;
      continue;
    }
    std::fprintf(stderr, "unknown arg: %s\n", a);
    usage();
    return 1;
  }

  if (want_crash_test) {
    // Diagnostic: prove the crash reporter end to end. Three events, then a
    // fault, so the report has a ring to print and a stack to symbolise.
    slrr::crash_note("crash-test: first event");
    slrr::crash_note("crash-test: second event");
    slrr::crash_note("crash-test: third event");
    std::fprintf(stderr, "[crash-test] faulting on purpose\n");
    std::fflush(stderr);
    *(volatile int*)0 = 1;
    return 0;
  }

  if (!want_probe && !want_game && !want_pacejka && !want_dyno && !want_gc &&
      compile_dirs.empty()) {
    usage();
    return 1;
  }

  if (want_pacejka) {
    char msg[256];
    const bool ok = sl2_pacejka_self_check(msg, sizeof(msg));
    std::printf("[phys] %s\n", msg);
    if (!ok) return 3;
    if (!want_probe && !want_game && !want_dyno) return 0;
  }

  if (want_dyno) {
    char msg[256];
    const bool ok = dyno_self_check(msg, sizeof(msg));
    std::printf("[phys] %s\n", msg);
    if (!ok) return 3;
    if (!want_probe && !want_game) return 0;
  }

  if (const char* env = std::getenv("SLRR_GAME_ROOT")) {
    if (!root_arg) root_arg = env;
  }

  opt.game_root = find_game_root(root_arg);
  if (opt.game_root.empty()) {
    std::fprintf(stderr,
                 "FAIL: game root not found (need system/Scripts/lang/System.class)\n"
                 "  try: --root data\\game   or set SLRR_GAME_ROOT\n");
    return 1;
  }
  std::printf("[host] game_root=%s\n", opt.game_root.c_str());

  // Script recompilation without a game window: the same compiler the game
  // exposes as System.compileAll, so a patched class can be built from source.
  if (!compile_dirs.empty()) {
    bool ok = true;
    for (const auto& dir : compile_dirs) {
      const auto report = script_compile_all(opt.game_root, dir);
      std::printf("[compile] %s attempted=%d succeeded=%d failed=%d classes=%d\n",
                  dir.c_str(), report.attempted, report.succeeded, report.failed,
                  report.classes);
      if (report.failed != 0 || report.succeeded == 0) ok = false;
    }
    return ok ? 0 : 4;
  }

  NativeRegistry natives;
  Jvm jvm(&natives);
  if (want_gc) {
    std::setvbuf(stdout, nullptr, _IONBF, 0);
    jvm.set_game_root(opt.game_root);
    if (!jvm.bootstrap_classpath()) return 3;
    // Isolated, headless diagnostic. Never starts Init, a game or a renderer.
    Object* host_root = nullptr;
    jvm.add_gc_roots([&](const GcRootVisitor& visit) { visit(host_root); });
    jvm.collect_garbage();
    const size_t baseline = jvm.heap_size();
    Object* a = jvm.new_object("java.lang.Object");
    Object* b = jvm.new_object("java.lang.Object");
    a->fields["peer"] = Value::make_obj(b);
    b->fields["peer"] = Value::make_obj(a);
    host_root = a;
    jvm.collect_garbage();
    if (jvm.heap_size() != baseline + 2) return 11;
    a->gc_disabled = true;
    host_root = nullptr;
    jvm.collect_garbage();
    if (jvm.heap_size() != baseline + 2) return 12;
    a->gc_disabled = false;
    jvm.collect_garbage();
    if (jvm.heap_size() != baseline) return 13;
    Object* array = jvm.new_object("java.lang.Object");
    array->is_array = true;
    array->slots.push_back(Value::make_obj(jvm.new_string("gc retained string")));
    jvm.set_static("java.lang.Object", "_gcProbe", Value::make_obj(array));
    jvm.collect_garbage();
    if (jvm.heap_size() != baseline + 2) return 14;
    jvm.set_static("java.lang.Object", "_gcProbe", Value::make_obj(nullptr));
    jvm.collect_garbage();
    if (jvm.heap_size() != baseline) return 15;
    Object* resurrect = jvm.new_object("java.lang.Object");
    int finalized = 0;
    bool deferred = false;
    bool can_resurrect = true;
    natives.register_method("java.lang.Object", "finalize", "()V",
        [&](Object* o, const std::vector<Value>&) {
          if (can_resurrect && o == resurrect) {
            ++finalized;
            jvm.set_static("java.lang.Object", "_gcProbe", Value::make_obj(o));
            jvm.new_string("allocated by finalizer");
            deferred = jvm.collect_garbage() == -1;
          }
          return Value::make_void();
        });
    jvm.collect_garbage();
    if (finalized != 1 || !deferred || !resurrect->gc_finalized) return 16;
    jvm.set_static("java.lang.Object", "_gcProbe", Value::make_obj(nullptr));
    jvm.collect_garbage();
    if (finalized != 1 || jvm.heap_size() != baseline) return 17;
    can_resurrect = false;
    for (int i = 0; i < 20000; ++i) jvm.new_string("temporary");
    jvm.gc_safe_point();
    if (jvm.heap_size() != baseline) return 18;
    register_gc_natives(jvm);
    Object* pinned = jvm.new_object("java.lang.Object");
    natives.invoke("java.lang.Object", "disableGC", "()V", pinned, {});
    natives.invoke("java.lang.Object", "disableGC", "()V", pinned, {});
    if (!pinned->gc_disabled) return 19;
    const auto code = natives.invoke("java.lang.Object", "hashCode", "()I", pinned, {});
    if (code.tag != ValueTag::Int || !code.v.i) return 20;
    natives.invoke("java.lang.Object", "enableGC", "()V", pinned, {});
    if (pinned->gc_disabled) return 21; // Idempotent flags, not nested counters.
    jvm.collect_garbage();
    if (jvm.heap_size() != baseline) return 22;
    Object* native_root = jvm.new_object("java.lang.Object");
    native_root->fields["payload"] = Value::make_obj(jvm.new_string("native-owned"));
    gameref_upsert(native_root);
    jvm.add_gc_roots(gameref_visit_gc_roots);
    jvm.collect_garbage();
    if (jvm.heap_size() != baseline + 2) return 23;
    gameref_remove(native_root);
    jvm.collect_garbage();
    if (jvm.heap_size() != baseline) return 24;
    for (int i = 0; i < 16; ++i) jvm.new_string("requested cycle");
    Value pending = jvm.invoke("java.lang.System", "gc", "()I", {}, true);
    if (pending.tag != ValueTag::Int) return 25;
    if (pending.v.i >= 0) pending = jvm.invoke("java.lang.System", "gc", "()I", {}, true);
    if (pending.v.i != -1) return 26;
    jvm.gc_safe_point();
    const Value finished = jvm.invoke("java.lang.System", "gc", "()I", {}, true);
    if (finished.tag != ValueTag::Int || finished.v.i < 16) return 27;
    std::printf("[gc] probe: roots, cycles, arrays, pinning, resurrection, deferred collection and allocation pressure OK\n");
    return 0;
  }

  if (want_probe && !want_game) return boot_probe(jvm, opt);


  auto render = create_render_backend(opt.render);
  return boot_game(jvm, *render, opt);
}


int main(int argc, char** argv) {
  // Before anything else: a crash must leave a report behind, not a dialog.
  slrr::install_crash_handlers("");
  try {
    return run_host(argc, argv);
  } catch (const std::exception& e) {
    slrr::crash_note((std::string("uncaught: ") + e.what()).c_str());
    std::fprintf(stderr, "[crash] uncaught: %s\n", e.what());
    throw;
  } catch (...) {
    slrr::crash_note("uncaught: non-std exception");
    throw;
  }
}

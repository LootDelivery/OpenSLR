#pragma once

#include "slrr/jvm.hpp"
#include "slrr/render_backend.hpp"

#include <string>

namespace slrr {

struct BootOptions {
  std::string game_root;
  std::string player_name = "Player";
  std::string spawn_car; // development garage: owning RPK path:local type ID
  std::string vehicle_osd; // optional standalone gauge CFG for preview
  bool window = false;
  bool auto_new = false;
  // After Garage: fire Garage.osdCommand (101 MAINMENU / 109 HITTHESTREET / 0=skip).
  int32_t auto_cmd = 0;
  int32_t max_frames = 0;  // 0 = until quit
  // --perf-log: per-second frame timing on stdout, plus CSV rows when a path
  // other than "-" is given.
  bool perf_log = false;
  std::string perf_log_path;
  // --perf-frames CSV: one CSV row per frame (zone breakdown) for catching
  // single-frame spikes the per-second log averages away.
  std::string perf_frames_path;
  // --bench-drive: once the player drives in an outdoor world, hold full
  // throttle with the wheel straight for this many simulated seconds, then
  // quit. The run advances 1/60 s per frame, so every build covers the same
  // path frame by frame however fast it renders.
  float bench_drive_seconds = 0.f;
  // --bench-drive manual: no forced throttle, natural frame pacing — the
  // player drives while the perf log records until the window closes.
  bool bench_manual = false;
  // --bench-map: from the garage, start the GameLogic.MapBox track whose
  // script class name contains this text (ValoCity_Track for the city).
  std::string bench_map;
  // --bench-route: steer the benchmark car along the city road graph instead
  // of driving straight, so a long run covers streets, junctions and turns.
  bool bench_route = false;
  // --bench-time: game hour set when the benchmark drive starts (-1 keeps it).
  float bench_hour = -1.f;
  // --bench-shots: directory for window captures (TGA) at the start of the
  // benchmark drive and every 10 seconds after it.
  std::string bench_shots;
  // --roc: right after Garage, fire Garage.osdCommand(CMD_BEGIN_ROC=128)
  // then CMD_ROCRACE=120, bypassing the ROC entry dialog (fee / ranking /
  // prestige gates) so the ROCTrack scene and the spline-driven bot can be
  // exercised without winning the career up to ROC first.
  bool roc = false;
  bool vsync = true;          // --no-vsync clears it
  float fps_cap = 0.f;        // --fps-cap N: frame limiter (pair with --no-vsync)
  bool debug_overlay = false; // --debug-overlay opens the Ctrl+F12 panel
  RenderBackendKind render = RenderBackendKind::Bgfx;
};

// Probe only: validate game root + classpath bootstrap, no window loop.
int boot_probe(Jvm& jvm, const BootOptions& opt);

// Interactive / smoke path (Splash → MainMenu → CMD_NEW → Garage).
// Returns 0 on success. Full section machine lands incrementally.
int boot_game(Jvm& jvm, IRenderBackend& render, const BootOptions& opt);

}  // namespace slrr

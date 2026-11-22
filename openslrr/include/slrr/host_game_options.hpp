#pragma once
#include <cstdint>

namespace slrr {
class Jvm;
struct Object;
struct EnhancedGraphics {
  // 2 = MSAA x4: user-tested sweet spot on the RTX 4070 Ti at 1440p
  // (350-500 fps uncapped, no visible cost). x2 left visible stair-stepping.
  int msaa = 2;
  bool dynamic_reflections = true;
  bool volumetric_clouds = true;
  bool dynamic_lighting = true;
  bool local_lights = true;
  // Vehicle reflection tuning (F12 panel).
  // reflection_normal: 0 = authored vertex normal, 1 = geometric normal from
  // screen-space derivatives, 2 = the two added together. The geometric one
  // follows the real surface but is flat per triangle, so it facets the
  // shading on a coarse mesh -- it is kept for reference only, and authored
  // is the default.
  int reflection_normal = 0;
  // Cone radius of the prefiltered lookup. 0 samples the cubemap at a single
  // direction, which is a perfect mirror and therefore shows every error in
  // the normal as a hard artefact.
  float reflection_spread = 1.0f;
  // Head-on reflectance floor; the rest of the range is Fresnel.
  float reflection_strength = 0.35f;
  // Sample the GGX-prefiltered cube instead of the box-filtered mips of the
  // capture. Live, so the difference can be judged by toggling it while
  // looking at the car; the chain keeps its double buffer, so switching on
  // takes a full cycle before the material starts sampling it.
  //
  // Back on: the chain is being finished rather than parked. See
  // docs/RENDER_FOUNDATION_PLAN.md 8.10 for the defects still open.
  bool ggx_reflections = true;
  // Diffuse ambient from the same capture the reflections use, instead of one
  // flat colour for the whole car.
  //
  // Off by default, and not because the idea is wrong. Its chain still has open
  // defects: the probe comes out flat instead of graded, so it cannot show the
  // sky above and the street below, and its level moves with each capture, which
  // is visible as an ambient that pumps while driving. See
  // docs/RENDER_FOUNDATION_PLAN.md 8.10 before switching it on.
  bool irradiance_probe = false;
  // How much of the ambient comes from the probe instead of the flat colour.
  // 1.0 is physically what the environment gives -- and it makes a car's sides
  // noticeably dimmer than the roof, because a vertical surface sees half sky
  // and half road. The stock look lit both equally, so the default keeps some of
  // that expectation; the whole range is on a slider because this is a taste
  // call, not a correctness one.
  float irradiance_strength = 0.7f;
};
EnhancedGraphics& host_enhanced_graphics();
// Options -> Advanced Game Options -> Framerate Adaptive Behaviour.
// Classic draws cars at their last 60 Hz physics tick, as before. Adaptive
// draws them between ticks. Saved in save/game/openslrr_options.
enum class FramerateMode { Classic = 0, Adaptive = 1 };
FramerateMode host_framerate_mode();
void host_set_framerate_mode(FramerateMode mode);
struct VideoMode {
  int width = 800, height = 600, depth = 32;
  bool windowed = true;
  bool operator==(const VideoMode&) const = default;
};
struct VideoOptions {
  VideoMode mode;
  float view_distance = 400.f, shadow_distance = 40.f;
  int shadow_size = 256, shadows = 1;
  bool flares = true;
  bool operator==(const VideoOptions&) const = default;
};
struct DrivingOptions {
  int transmission = 1;
  float steering_help = .666f, abs = 0.f, asr = 0.f;
  bool operator==(const DrivingOptions&) const = default;
};
struct GameOptions {
  DrivingOptions driving;
  VideoOptions video;
  int gps_mode = 0;
  float unit_meters = 1000.f;
  float deformation = 1.6f, internal_damage = 1.f, player_damage = .8f;
  float head_steer = 1.f, head_velocity = 1.f, head_acceleration = 1.f;
  float traffic_density = .2f, pedestrian_density = .1f;
  bool operator==(const GameOptions&) const = default;
};
const GameOptions& host_game_options();
uint64_t host_driving_options_revision();
void host_game_options_load(Jvm& jvm);
void host_game_options_dirty();
void host_set_video_mode(Jvm& jvm, VideoMode mode);
bool host_commit_video_dialog(Jvm& jvm, Object* handler, int command);
void host_game_options_apply(Jvm& jvm);
void host_game_options_set_steering_help(Jvm& jvm, float strength);
void host_game_options_update(Jvm& jvm, bool force = false);
}

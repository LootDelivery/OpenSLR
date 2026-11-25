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
  // Which level of the reflection chain a material asks for.
  //
  // Off is the stock mapping, roughEnv * 1.68, where roughEnv is
  // 1 - specularExponent/24 -- a normalisation for fresnel and occlusion, not a
  // roughness. It puts car paint at about 0.3, below the first level of the
  // chain, so paint reads the sharp capture and the GGX levels are only ever
  // reached by rubber. On is the physical mapping, a Blinn-Phong exponent
  // corresponding to a GGX roughness of sqrt(2/(e+2)) and level m standing for
  // roughness 0.02 * 2^m, which puts paint at levels 3 to 4.
  bool reflection_physical_roughness = true;
  // Ceiling for that level. Four is what the mapping asks for on car paint and
  // it reads as flat pearl; two keeps the shape of the reflected environment.
  // 1.5 by the user's eye: above that the body stops reading as paint and starts
  // reading as film. It also happens to be the range where the chain is barely
  // used, which keeps the cycle's age out of sight.
  float reflection_roughness_cap = 1.5f;
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
  // On by default since the probe's basis, sampling and level were settled; see
  // docs/RENDER_FOUNDATION_PLAN.md 8.14 for the two defects and the test that
  // caught them. What is left is taste -- how much of the ambient the probe takes
  // over, and how strongly -- and that is what the two values below are for.
  bool irradiance_probe = true;
  // How much of the ambient comes from the probe instead of the flat colour.
  // 1.0 is physically what the environment gives -- and it makes a car's sides
  // noticeably dimmer than the roof, because a vertical surface sees half sky
  // and half road. The stock look lit both equally, so the default keeps some of
  // that expectation; the whole range is on a slider because this is a taste
  // call, not a correctness one.
  float irradiance_strength = 0.7f;
  // Multiplier on top of the shader's self-calibration, which anchors an
  // upward-facing surface to the flat ambient the material used before. 1.0 means
  // the probe changes the shape of the ambient and not its level.
  float irradiance_scale = 1.0f;
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
  // `steerhelp_turn` from the game's Config: the weight of the recovered
  // steering aid's own term (`axis/lock * this`) against the driver's input.
  // Retail carries 1.0; the physics reads it through the same store the exe
  // uses (0x004261f0), so it is not a taste knob and nothing in this port
  // writes it.
  float steerhelp_turn = 1.f;
  // Launch aid, off by default: 0 disables it, otherwise the fraction of the rev
  // limit a slipping automatic clutch holds the engine at. It belongs next to
  // ABS and ASR as a player aid, so the default is off -- upstream applied 0.4 to
  // every car with an automatic box, which is the "the engine stops at a quarter
  // of the tachometer" report.
  float launch_rpm = .4f;
  // Drive the car the way the original does: thrust from the engine torque with
  // ClutchF capping the clutch, no engine-speed hold. Off switches to the host's
  // crank-inertia slip model, which is what the solver's own tests pin.
  bool original_drive = true;
  // A driving readout in the corner: speed, rpm and gear. Off hides it.
  bool drive_hud = true;
  // The handbrake moment as a multiple of the recovered law (1 = twice the rear
  // service-brake moment, CAR_MODEL_RECOVERY 14.7c). Saved in Game Options and
  // edited from the F12 panel in 0.1 steps. 1.4 is the driver's default: it is
  // the setting at which an FWD car's rears lock reliably, while 1 is the law.
  // 0 disables the handbrake; 5 is the panel's ceiling.
  float handbrake_strength = 1.4f;
  // Torque converter stall point, as a fraction of the rev limit.
  float converter_stall = .8f;
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
// 0 turns the launch aid off; anything else is the fraction of the rev limit the
// slipping clutch holds. Written to Game Options, then applied like the rest.
void host_game_options_set_launch_rpm(Jvm& jvm, float fraction);
// True: the original's drive coupling. False: the host's crank-inertia model.
void host_game_options_set_original_drive(Jvm& jvm, bool original);
// Show the corner speed/rpm/gear readout.
void host_game_options_set_drive_hud(Jvm& jvm, bool show);
// Torque converter stall point, as a fraction of the rev limit.
void host_game_options_set_converter_stall(Jvm& jvm, float fraction);
// Handbrake moment as a multiple of the recovered law, 0..5 in 0.1 steps.
// Saved in save/game/openslrr_options, not in Config: the stock Config has a
// fixed layout. The Jvm-free form is what the scripted menu calls.
void host_set_handbrake_strength(float strength);
void host_game_options_set_handbrake_strength(Jvm& jvm, float strength);
void host_game_options_update(Jvm& jvm, bool force = false);
}

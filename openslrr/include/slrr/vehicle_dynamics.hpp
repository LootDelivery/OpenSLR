#pragma once
#include "slrr/tire_model.hpp"
#include <array>
#include <cstddef>
#include <cstdint>
#include <functional>
#include <vector>

namespace slrr::physics {
// Script-space metres, kilograms and seconds. Renderer handedness stays
// outside.
struct Vec3 {
  double x = 0, y = 0, z = 0;
  Vec3 operator+(Vec3 b) const { return {x + b.x, y + b.y, z + b.z}; }
  Vec3 operator-(Vec3 b) const { return {x - b.x, y - b.y, z - b.z}; }
  Vec3 operator-() const { return {-x, -y, -z}; }
  Vec3 operator*(double s) const { return {x * s, y * s, z * s}; }
  Vec3 operator/(double s) const { return *this * (1 / s); }
  Vec3 &operator+=(Vec3 b) {
    *this = *this + b;
    return *this;
  }
  double dot(Vec3 b) const { return x * b.x + y * b.y + z * b.z; }
  Vec3 cross(Vec3 b) const {
    return {y * b.z - z * b.y, z * b.x - x * b.z, x * b.y - y * b.x};
  }
  double length() const;
  Vec3 normalized() const;
  bool finite() const;
};
struct Quaternion {
  double x = 0, y = 0, z = 0, w = 1;
  Quaternion operator*(Quaternion b) const;
  Quaternion normalized() const;
  Vec3 rotate(Vec3 v) const;
  Vec3 inverse_rotate(Vec3 v) const;
};
using Matrix = std::array<float, 16>;

// Adapted contracts: SLRR_rev a2ee10de, rigid-body-3d.ts. The 8ac2ba9
// fallback uses a solid box of 0.75 times the bounds dimensions (host tuning,
// not the collision dimensions or the original game's compound inertia).
class RigidBody {
public:
  double mass = 1;
  Vec3 position, velocity, momentum, center, minimum, maximum;
  Quaternion orientation;
  void configure(double kilograms, Vec3 bounds_min, Vec3 bounds_max, Vec3 com);
  // Principal moments about `com` along the body axes, in kg m². All three
  // must be positive and finite; otherwise the entire fallback is retained.
  void configure(double kilograms, Vec3 bounds_min, Vec3 bounds_max, Vec3 com,
                 Vec3 principal_inertia);
  Vec3 inertia() const;
  void set_frame(const Matrix &frame);
  Matrix frame() const;
  Vec3 inverse_world_inertia(Vec3 v) const;
  Vec3 omega() const;
  void set_omega(Vec3 angular_velocity);
  Vec3 point_velocity(Vec3 point) const;
  void add_force(Vec3 force, Vec3 point);
  void impulse(Vec3 impulse, Vec3 point);
  void integrate(double dt);
  // Split semi-implicit integration for constraints that must remove slip
  // after forces/gravity, but before those velocities advance the pose.
  void integrate_forces(double dt);
  void integrate_pose(double dt);
  double contact(Vec3 point, Vec3 normal, double restitution = .12,
                 double friction = .6, RigidBody* other = nullptr);
  std::array<Vec3, 8> corners() const;

private:
  Vec3 inverse_inertia_, force_, torque_;
};

struct GroundHit {
  double height = 0;
  Vec3 normal{0, 1, 0};
};
// from_y is a ray origin/ceiling, not a default ground height. False = air.
using Terrain =
    std::function<bool(double x, double z, double from_y, GroundHit &)>;
// Real-life aerodynamics (ENGINE_CONTRACT §11): ½ρv² forces from the air
// velocity relative to the car, with typical road-car coefficients.
// The mean wind of a weather report: m/s at 10 m height, blowing towards
// `heading` (degrees from +Z towards +X).
struct Wind {
  double speed = 0, heading = 0;
};
// Air velocity (world m/s) at car height at a world position and time: the
// mean wind reduced by the log wind profile, plus gusts that travel with it.
Vec3 wind_velocity(const Wind &wind, Vec3 position, double seconds);
struct AeroWing {
  // One face of a wing in chassis-frame metres: unit normal, planform m².
  Vec3 position, normal;
  double area = 0;
};
struct Aerodynamics {
  // Chassis.C_drag (kg/m): the extra drag of missing body panels, against
  // the velocity of drag_center (chassis frame) above 1 m/s.
  double drag = 0;
  Vec3 drag_center;
  // Drag and lift of the body, sized by the rigid body's bounds.
  bool body = false;
  std::vector<AeroWing> wings;
  // Air velocity around the car (world m/s); every force uses the car's
  // velocity relative to it.
  Vec3 wind;
};
// Resultant aerodynamic force (world) and its torque about the centre of
// mass for one step of `dt`, as VehicleDynamics applies it.
struct AeroLoad {
  Vec3 force, torque;
};
AeroLoad aero_load(const Aerodynamics &aero, const RigidBody &body, double dt);
struct WheelConfig {
  bool enabled = false;
  // Fixed suspension travel with compliant tyre/bump-stop support.
  bool rigid_contact = false;
  bool metal_contact = false; // no installed rim/tyre, only hub hardware
  Vec3 center, down{0, -1, 0};
  double radius = 0, rest = 0, minimum = 0, maximum = 0;
  // CFG spring base + travel, independent of the tunable spring rest length.
  // Zero is the generic kernel fallback (anchor at rest length).
  double anchor = 0;
  double anchor_length() const { return anchor > 0 ? anchor : rest; }
  double spring = 0, bump_damping = 0, rebound_damping = 0;
  // CFG `type` mass. Zero keeps the massless ray. An arm length projects the
  // support onto the native hinge circle; the spring stays the 1-DOF element.
  double unsprung = 0, arm_length = 0;
  Vec3 arm_pivot, arm_axis{0, 0, 1};
  // Raw setHub arguments. Without a hub the arm circle is the support point.
  bool has_hub = false;
  std::array<float, 10> hub{};
  Quaternion hub_alignment;
  // Stored WheelRef.setInstantCenter data, relative to `center` in chassis
  // axes: front-view x/y, side-view z/y. Retained for native compatibility;
  // the intended 8ac2ba9 solver does not apply an instant-centre load force.
  bool front_instant_center = false, side_instant_center = false;
  Vec3 front_center_offset, side_center_offset;
  bool linked() const { return unsprung > 0 || arm_length > 0; }
};
struct WheelState {
  bool contact = false, seeded = false;
  // force: spring and damper support, used as the tyre load. jacking is a
  // reserved legacy field; the intended solver does not add it to the load.
  double length = 0, speed = 0, force = 0, jacking = 0;
  Vec3 center, normal{0, 1, 0}, hub, patch;
  Quaternion hub_rotation;
  double spin = 0, rotation = 0;
  double slip_speed = 0, lateral_speed = 0;
  // Diagnostic mirror of the grip scale's tangent for this wheel: the contact
  // normal measured against the wheel's own axle (1.0 square to the road).
  double tangent = 1;
  // Unspent slip carried between steps for the low-speed path (0x0043BBD0).
  TireSlipState slip_state;
  // Unspent brake/bearing budget at the patch (N s), not free tyre grip.
  double holding_impulse = 0;
  TireForces tire_force{};
};
// Optional host anti-roll approximation, in N/m and N s/m. Invalid or
// negative coefficients act as zero; no installed bar means both stay zero.
struct AntiRollConfig {
  double stiffness = 0, damping = 0;
};
struct DriveWheel {
  double weight = 0, steer = 0, brake = 0, handbrake = 0, bearing = 0;
  double rolling_resistance = 0;
  TireParameters tyre;
};
struct DriveInput {
  bool native_traffic = false; // SLRR_rev TrafficVehicle tyre-force driver.
  std::array<DriveWheel, 4> wheels{};
  std::array<double, 8> ratios{};
  int gears = 0, shift = 0;
  bool engaged = false, reverse = false, automatic_shift = true, auto_clutch = true;
  bool automatic_first = false;
  double throttle = 0, brake = 0, handbrake = 0;
  // Only the bearing term still uses a scale: its wheel[0xFC] meaning is not
  // recovered, so it keeps the pre-recovery form. Brakes and the tyre no longer
  // use a scale at all -- see the three fields below.
  double bearing_scale = 0;
  // wheel+0xEC, the recovered slip denominator (CAR_MODEL_RECOVERY 14.7):
  //   fVar16 = share term + gearbox+0xE8,   wheel[0xEC] = 0.1/fVar16,
  // where the share term vanishes when the wheel has no drive share or no gear
  // is engaged. This field is gearbox+0xE8, the sixth float of the engine
  // record -- NOT driveline_inertia (that one is 500-1000). Measured 35/38/40/50
  // across cars; 35 is the MC and the fallback for a record that carries none.
  double brake_inertia = 35;
  // The two .data coefficients the recovered step multiplies EC by: 0x0060E020
  // (tyre force) and 0x0060E024 (brake and drag). 2.8 and 4.0 sit in the exe;
  // the mod patches them to 7.0 and 6.0, which is what the running game uses.
  double tyre_step_gain = 7, brake_step_gain = 6;
  double abs_strength = 0;
  // car+0x1F54: the ABS's memory of the previous frame's slip measure.
  double abs_prev = 0;
  // car+0x1F50: the ASR's memory of the previous frame's slip measure.
  double asr_prev = 0;
  // The ASR's clutch press (FUN_0044e120:41 writes car+0x1DBC, the clutch pedal,
  // as `open = 1-(1-open)*service`, i.e. engagement *= service). 1.0 leaves the
  // clutch alone. The pedal itself is written every frame by the input stage
  // (0x44F39A) and pressed by the anti-stall regulator (FUN_0044DD70); the ASR
  // only multiplies on top of it.
  double asr_clutch = 1;
  // Flags 0x1000000 / 0x2000000: the ABS / ASR indicators (raised under
  // service < 0.9). The original reads them in FUN_00451730 (0x452746 /
  // 0x452779) and publishes osd_abs / osd_asr (1.0/0.0) for the dashboard, so
  // both lamps are wired there; they stay dark on stock cars only because the
  // car script initialises abs/asr to 0.
  bool abs_active = false, asr_active = false;
  double launch_help = 0;
  double differential_lock = 0;
  // Automatic clutch slip revs, used by the host model only (see `faithful`):
  // the fraction of the rev limit the slipping clutch holds the engine at. 0 is
  // not "off", it stalls the engine against the clutch, which is why the faithful
  // path does not use this number at all.
  double launch_rpm = .4;
  // The original's coupling: Chassis.ClutchF (default 500) caps the torque the
  // clutch can pass. It is a force limit, not an engine-speed hold.
  double clutch_force = 500, brake_balance = .5;
  // Where the torque converter holds the engine under full throttle, as a fraction
  // of the rev limit. It is a tuning number with no recovered counterpart; the
  // driver's report that the engine never passed 3800 on a 6500 block set the
  // default high enough to hear the engine work at a launch.
  double converter_stall = .8;
  // The recovered two-inertia coupling (CAR_MODEL_RECOVERY 4.3, measured in 12.4):
  //   inv_d = driveline_inertia / ratio^2
  //   k     = inv_d / (coupling_ratio * inertia + inv_d)
  // J_drive is per car (500 measured on one, 965 on another). K is 6.0 in memory;
  // 4.0 is what sits in the exe, because the mod patches .data at start-up.
  double driveline_inertia = 500.0;
  // The host slip model's own crank inertia. That model is ours, calibrated at 0.38, while
  // `inertia` carries the recovered J_engine, twelve times larger.
  double host_inertia = .38;
  double coupling_ratio = 6.0;
  // Drive output the way the original computes it -- engage = 1 - clutch, thrust
  // from the engine torque -- instead of the crank-inertia slip model. The engine
  // is free to flare to its limiter and the clutch only limits what it hands to the
  // wheels. Off in the header so the solver's own tests keep pinning the host
  // model; the game turns it on.
  bool faithful = false;
  double inertia = 0, idle = 0, limit = 0, starter = 0;
  // The original's engine losses (recovered model 3.5). `engine_brake` scales a
  // torque that grows with engine speed and is what slows the car when the throttle
  // closes with a gear engaged -- our crank-inertia loss was ~26x weaker than it.
  double friction_fwd = 1e-4, friction_rev = 5e-3, max_rpm = 0;
  double clutch = 1;
  // Net open-throttle crank torque (Nm) at the requested rpm, including
  // the shared mechanical friction loss; the solver applies no extra gain.
  std::function<double(double)> torque;
  // Optional wet dyno bank. Empty/zero leaves the existing dry drive unchanged.
  std::function<double(double)> nitro_torque;
  double nitro = 0;
};
// wheel+0xEC and the slip denominator behind it, both recovered from
// 0x00454500 (CAR_MODEL_RECOVERY 14.7) and pinned to the live measurement.
// `share` is this wheel's drive share (wheel+0xE0), `share_sum` the sum of
// |share| over the wheels, `clutch_open` car+0x1DBC (1 = open), `total_ratio`
// the engaged ratio times the final drive, `engine_inertia` gearbox+0xE0 and
// `brake_inertia` gearbox+0xE8 (the sixth float of the engine record, which
// part_config already loads as brake_inertia).
double wheel_slip_denominator(double share, double share_sum, double clutch_open,
                              double total_ratio, double engine_inertia,
                              double brake_inertia);
double wheel_inverse_inertia(double share, double share_sum, double clutch_open,
                             double total_ratio, double engine_inertia,
                             double brake_inertia);
// Runtime strength for the handbrake, exposed as the F12 panel's slider (Tuning ->
// Handbrake). 1 is the recovered law: the pedal is the original's button and a
// rear wheel carries twice its service-brake moment (CAR_MODEL_RECOVERY 14.7c).
// The slider scales only that moment -- the pedal stays a button -- which is the
// same lever a hydraulic-handbrake mod pulls through the car's brakepower
// handbrake field, except it needs no edit to the car data and no restart.
// Runtime only, like the tyre sliders: 0 disables the handbrake, 5 is the
// strongest the panel offers.
struct DriveTuning {
  float handbrake = 1.f;
};
DriveTuning& drive_tuning();
double suspension_force(double length, double velocity, double rest,
                        double maximum, double spring, double bump,
                        double rebound);

// Compile all consumers with the same experimental solver rate (#203).
// The game additionally requires a multiple of its 60 Hz shared-world tick.
#ifndef SLRR_VEHICLE_PHYSICS_HZ
#define SLRR_VEHICLE_PHYSICS_HZ 120
#endif

class VehicleDynamics {
public:
  // SLRR_rev physics-timing.ts driving frequency; shipped default is 120 Hz.
  static constexpr unsigned frequency = SLRR_VEHICLE_PHYSICS_HZ;
  static constexpr unsigned update_frequency = 60;
  static_assert(SLRR_VEHICLE_PHYSICS_HZ >= 60, "solver rate must be at least 60 Hz");
  static constexpr double timestep = 1.0 / frequency;
  RigidBody body;
  struct Impact {
    Vec3 point, normal;
    double speed = 0;
    // Pair contacts supply the actual impulse and pre-impact rigid motion.
    // Negative retains the historical immovable-wall reconstruction.
    double normal_impulse = -1;
    Vec3 pre_velocity{}, pre_omega{};
    bool has_pre_motion = false;
  } ground_impact;
  std::array<WheelConfig, 4> wheels;
  std::array<WheelState, 4> state;
  // Front (0/1) and rear (2/3): both corners must contact on non-rigid
  // suspension. Couples world-Y hardpoint forces, bounded to half body weight
  // per side, after tyre evaluation; never changes the current tyre loads.
  // Deflection is relative to each corner's rest length (no rest-offset preload).
  std::array<AntiRollConfig, 2> anti_roll{};
  // Installed-body low points in chassis coordinates; no implicit support
  // is created when the runtime supplies none. Contact is floor-only.
  std::array<Vec3, 8> body_supports{};
  std::size_t body_support_count = 0;
  DriveInput drive;
  Aerodynamics aero;
  double engine_omega = 0;
  bool engine_running = false;
  int gear = 0;
  uint64_t ticks = 0;
  unsigned advance(double seconds, const Terrain &terrain);
  void reset_clock();
  void release_wheels();
  void reset_drive();
  // Fraction of the upcoming engine step allowed by the existing rev limiter.
  double engine_limit_fraction() const;
  // The service brake the assist is applying, 0..1. A spun car must read 0 here.
  double brake_service() const { return service_brake_; }
  uint64_t backlog() const;
  void sync_wheels();

private:
  double accumulator_ = 0;
  double previous_rpm_ = 0, shift_time_ = 0;
  double starter_remaining_ = 0, service_brake_ = 0;
  // car+0x1DBC as the wheel step reads it: 1 is an open clutch, so the share
  // term drops out. The faithful path writes it as 1 - engage.
  double wheel_clutch_open_ = 1;
  double launch_factor_ = 1;
  int previous_shift_ = 0;
  bool accelerator_down_ = false, forced_reverse_ = false;
  bool launch_active_ = false;
  void step(const Terrain &terrain);
  void apply_aero();
  void step_drive();
  void update_gear(double rpm, double shaft, double road_shaft);
};
} // namespace slrr::physics

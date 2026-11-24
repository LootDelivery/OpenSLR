#pragma once

#include <array>

namespace slrr::physics {

// The values a tyre script writes through WheelRef (stock Tyre.java), in the
// units that script uses. Defaults are the stock Baiern 205/55 sport tyre.
struct TireParameters {
  // The 17 slots the script writes through WheelRef.setPacejka (wheel+0x1E8 on the
  // EXE side), in order. Slots 0..10 are C0..C10 of the force law in
  // CAR_MODEL_RECOVERY.md 5.3; the named fields below are the same values under
  // their script names -- setSliction is slot 0, setFriction slot 2, setStiffness
  // slot 4, setMaxLoad slot 11, setFrictn_x slot 12, setLoadSmooth slot 14.
  // Defaults are Wheel.java's, i.e. the set used when a wheel carries no tyre.
  std::array<double, 17> pacejka{1.4, 0, 1.49, 0, 15.2, 0, 0, 0, -1, 0,
                                 0, 8000, 1, 0.015, 0.4, 0, 0};
  // setFriction: compound/width input, divided by the reference tyre's
  // default and clamped to scale grip; not itself the resulting peak mu.
  double friction = 1.76863;
  // setStiffness: reference-scaled Magic Formula B multiplier (the script
  // derives it from profile, rim fit and inflation), not an exact slope.
  double stiffness = 17.6839;
  // setSliction: reference-scaled grip multiplier as combined slip grows.
  double sliction = .814;
  // setMaxLoad / setLoadSmooth: reference load and lateral smoothing.
  // Load efficiency decreases above half capacity; zero keeps load linear.
  double maximum_load = 0, load_smoothing = .4;
  // setFrictn_x: longitudinal friction factor.
  double longitudinal_grip = 1;
  // Stored setCPatch tolerance in radians. Not consumed by the 8ac2ba9
  // force law; alignment is supplied separately as a load factor.
  double patch_angle = 0;
};

struct TireForces {
  double longitudinal, lateral;
};

// Per-wheel memory of the low-speed slip path (the EXE keeps it at
// wheel+0x14C / wheel+0x150). Unspent slip accumulates here instead of
// being converted to force through a velocity-proportional cap.
struct TireSlipState {
  double slip = 0, angle = 0;
};

// The normalised slip and slip angle the force law is evaluated at, as
// 0x0043BBD0 produces them: geometric at speed, an accumulated and clamped
// solution blended in below 10.1 m/s so the geometry cannot blow up as the
// speed approaches zero.
struct TireSlipInputs {
  double slip, angle;
};
TireSlipInputs tire_slip_inputs(double wheel_speed, double road_speed,
                                double lateral_speed, double dt,
                                TireSlipState &state);

struct TireTuning {
  float lateral_grip = 1.f;
  float longitudinal_grip = 1.f;
  float lateral_response = 1.f;
  float load_sensitivity = 1.f;
};
TireTuning& tire_tuning();

// Force-ellipse axes with zero-slip grip scaling (N). Static holding can use
// these bounds without requiring motion; this is not a finite-speed force.
TireForces tire_friction_limits(double load, double tangent,
                                const TireParameters& tire);

// The scale s of the recovered force law (0x00454500):
// (sqrt of the contact normal's non-axle part + 1) * 0.5 * load_curve(load).
// The rolling-resistance term consumes it too:
// F_rollres = s * v_roll * wheel[0x21c] * 0.01 - F_long, where
// wheel[0x21c] = surface[0x54] + wheel[0x74] (setRollRes).
double tire_scale(double load, double tangent, const TireParameters& tire);

// The recovered force law (0x0043BA80 clamped, 0x0043BB30 signed) on the 17 script
// slots, with the load curve of 0x00454500 and the composite slip of the same
// listing. `tangent` is the magnitude of the contact normal's components in the
// wheel frame other than its bearing axis -- the quantity 0x00454500 takes from
// sqrt(s[1]^2 + s[2]^2) and turns into the force scale through (tangent + 1) * 0.5.
// It replaces what used to be a load multiplier. Positive longitudinal pushes along
// the wheel heading; lateral opposes lateral_speed.
TireForces tire_forces(double load, double tangent, double wheel_speed,
                       double road_speed, double lateral_speed,
                       const TireParameters& tire);

// The same curves evaluated at the recovered low-speed slip inputs. state and
// dt carry the per-wheel filter that replaces the invented velocity cap.
TireForces tire_forces(double load, double tangent, double wheel_speed,
                       double road_speed, double lateral_speed,
                       const TireParameters& tire, TireSlipState& state,
                       double dt);

} // namespace slrr::physics

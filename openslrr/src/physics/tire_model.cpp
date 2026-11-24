#include "slrr/tire_model.hpp"

#include <algorithm>
#include <cmath>
#include <numbers>

namespace slrr::physics {
namespace {
double magic_formula(double slip, double b, double c, double d, double e) {
  const double bx = b * slip;
  return d * std::sin(c * std::atan(bx - e * (bx - std::atan(bx))));
}
} // namespace

// 0x0043BBD0. The EXE does not cap the force by the slip velocity -- doing
// that makes the force a damper, which stops the car abruptly in a slow slide
// while a fast one feels fine. It keeps the force law intact and instead
// replaces its *input* near zero speed with an accumulated, clamped solution,
// blended in over a speed band. Recovered constants from the .data image:
//
//   0x0060E02C  1.0    slip clamp (radians of curve input)
//   0x0060E030  1.0    slip-angle clamp
//   0x0060E034  0.3    slip accumulation gain  (steady state = slip / 1.5)
//   0x0060E038  0.5    angle accumulation gain (steady state = angle / 5)
//   0x0060E03C  0.1    blend ramp per m/s
//   0x0060E040  0.1    speed band start, m/s
//   0x0060E044  0.1    slip filter rate
//   0x0060E048  0.2    angle filter rate
//
// param_4 of the original is the physics timestep, and the live values live in
// the wheel, so the caller passes both.
TireSlipInputs tire_slip_inputs(double wheel_speed, double road_speed,
                                double lateral_speed, double dt,
                                TireSlipState &state) {
  constexpr double clamp_limit = 1.0;
  constexpr double slip_gain = .3, angle_gain = .5;
  constexpr double ramp = .1, threshold = .1;
  constexpr double slip_rate = .1, angle_rate = .2;

  const double speed = std::abs(road_speed);
  const double weight =
      std::clamp((speed - threshold) * ramp, 0.0, 1.0);

  const double geometric_slip =
      speed <= threshold ? 0.0 : (wheel_speed - road_speed) / speed;
  const double geometric_angle =
      speed <= threshold ? 0.0 : std::atan2(lateral_speed, speed);
  if (weight >= 1.0)
    return {geometric_slip, geometric_angle};

  // Accumulated solution: the slip that has actually gone unspent over time,
  // not the instantaneous ratio that diverges as the speed falls.
  const double accumulated_slip =
      std::clamp((slip_gain * state.slip + (wheel_speed - road_speed) * dt) /
                     ((dt * 50 * slip_rate + 1) * slip_gain),
                 -clamp_limit, clamp_limit);
  const double accumulated_angle =
      std::clamp((lateral_speed * dt + angle_gain * state.angle) /
                     ((dt * 50 * angle_rate + 1) * angle_gain),
                 -clamp_limit, clamp_limit);

  state.slip = accumulated_slip;
  state.angle = accumulated_angle;
  return {geometric_slip * weight + (1 - weight) * accumulated_slip,
          geometric_angle * weight + (1 - weight) * accumulated_angle};
}

TireTuning &tire_tuning() {
  static TireTuning tuning;
  return tuning;
}

namespace {
// 0x0043BA80 (clamped at zero) and 0x0043BB30 (signed): the recovered force law, one
// function with a switch, because the two differ only in that clamp. `argument` is the
// shape input (composite slip for longitudinal, slip angle for lateral), `s` is the
// scale that comes from the load.
double principle_force(double argument, double s, const TireParameters &tire,
                       bool clamp_nonnegative) {
  // C0, C2 and C4 are the named fields too -- setSliction, setFriction and
  // setStiffness write both -- so they are read from the names, which keeps one
  // source of truth per slot. The rest come from the slot array.
  const auto &c = tire.pacejka;
  const double c0 = tire.sliction, c1 = c[1], c2 = tire.friction, c3 = c[3],
               c4 = tire.stiffness, c5 = c[5], c6 = c[6], c7 = c[7], c8 = c[8],
               c9 = c[9], c10 = c[10];
  const double m = s * c1 + c2;
  const double denominator = m * c0;
  if (denominator == 0.0)
    return 0.0;
  const double decay = std::exp(-s * c5);
  const double numeric = (s * c3 + c4) * decay;
  const double argument_scaled =
      (s * c9 + c10 + argument) * (numeric / denominator);
  const double first = std::atan(argument_scaled);
  const double second = std::atan(
      (s * c7 + s * s * c6 + c8) * (first - argument_scaled) + first);
  const double force = m * s * std::sin(c0 * second);
  return clamp_nonnegative ? std::max(0.0, force) : force;
}

// The load curve of 0x00454500: max_load clamps it and load_smooth blends the linear
// branch with a progressive one that saturates at max_load.
double load_curve(double load, const TireParameters &tire) {
  const double maximum = tire.maximum_load;
  if (!(maximum > 0.0))
    return load;
  const double smooth = std::clamp(tire.load_smoothing, 0.0, 1.0);
  const double linear = std::min(maximum, load);
  const double progressive =
      std::atan(load * (std::numbers::pi / 2) / maximum) * maximum *
      (2 / std::numbers::pi);
  return (1 - smooth) * linear + smooth * progressive;
}
} // namespace

TireForces tire_friction_limits(double load, double tangent,
                                const TireParameters& tire) {
  if (!(load > 0.0)) return {};
  // The recovered cap is the force law's own peak. F = m*s*sin(C0*t2) with
  // m = s*C1 + C2 and s = (tangent+1)*0.5*load_curve(load); sin(C0*t2) reaches
  // 1 once C0 > 1 (the stock sliction 1.4), so the peak is exactly m*s -- the
  // classic D. The (tangent+1)/2 floor is the recovered scale (0x00454500:
  // even with the contact normal along the axle the scale is half the load),
  // so tangent=0 holds at half, not zero -- the invented
  // alignment = max(.25, tilt*tilt) load multiplier and the 0.85/1.42
  // force_peaks curve it fed are gone. wheel+0x218 (longitudinal_grip)
  // weights the slip's share of the composite; at exactly 0 the law produces
  // no longitudinal force for any input, which is what the script field means.
  const double s =
      (std::clamp(tangent, 0.0, 1.0) + 1.0) * 0.5 * load_curve(load, tire);
  const double d = (s * tire.pacejka[1] + tire.friction) * s;
  return {tire.longitudinal_grip > 0.0 ? d : 0.0, d};
}

double tire_scale(double load, double tangent, const TireParameters& tire) {
  // The scale s of the recovered law (0x00454500, line of local_3c at the
  // force assembly): (sqrt of the normal's non-axle part + 1) * 0.5, times
  // the load curve. The rolling-resistance term consumes the same s:
  // F_rollres = s * v_roll * wheel[0x21c] * 0.01 - F_long.
  if (!(load > 0.0)) return 0;
  return (std::clamp(tangent, 0.0, 1.0) + 1.0) * 0.5 * load_curve(load, tire);
}

namespace {

TireForces forces_from_inputs(double load, double tangent, double slip,
                              double angle, const TireParameters &tire) {
  const double sin_angle = std::sin(angle);
  const double cos_angle = std::cos(angle);
  const double longitudinal_weight = tire.longitudinal_grip;  // wheel+0x218
  const double composite =
      std::hypot(sin_angle, cos_angle * slip * longitudinal_weight);
  if (composite <= 1e-4)
    return {0.0, 0.0};
  // The scale: half of one plus the contact normal's tangential magnitude, times the
  // load curve. On a square-standing wheel the tangent is near zero and this is half
  // the load -- which is where the recovered grip lands.
  const double s = (tangent + 1.0) * 0.5 * load_curve(load, tire);
  const double longitudinal =
      (slip * longitudinal_weight * cos_angle / composite) *
      principle_force(composite, s, tire, true);
  // 0x00454500: local_7c = (sin(angle)/sigma) * FUN_0043ba80(sigma, s). The
  // lateral magnitude comes from the clamped law of the COMPOSITE, not from a
  // signed law of the angle -- the angle-signed FUN_0043bb30 call in the same
  // listing stores into wheel+0x158 (a grip coefficient), it is not this force.
  // The original's wheel frame measures velocities ground-relative
  // (v_surf = -wheel_speed), so its as-stored force corresponds to the
  // negative of ours; our branch applies forces along the car-relative axis,
  // which is why the minus lives at the application site in vehicle_dynamics.
  const double lateral = (sin_angle / composite) *
                         principle_force(composite, s, tire, true);
  return {longitudinal, lateral};
}
} // namespace

TireForces tire_forces(double load, double tangent, double wheel_speed,
                       double road_speed, double lateral_speed,
                       const TireParameters &tire) {
  if (!(load > 0.0))
    return {0.0, 0.0};
  // Stateless view: the geometric inputs, with the 1.5 m/s reference floor only to keep
  // the ratio finite. Production uses the stateful overload.
  const double reference =
      std::max({1.5, std::abs(road_speed), std::abs(wheel_speed)});
  const double slip = (wheel_speed - road_speed) / reference;
  const double angle =
      std::atan2(lateral_speed, std::max(1.5, std::abs(road_speed)));
  return forces_from_inputs(load, tangent, slip, angle, tire);
}

TireForces tire_forces(double load, double tangent, double wheel_speed,
                       double road_speed, double lateral_speed,
                       const TireParameters &tire, TireSlipState &state,
                       double dt) {
  if (!(load > 0.0))
    return {0.0, 0.0};
  const auto inputs =
      tire_slip_inputs(wheel_speed, road_speed, lateral_speed, dt, state);
  return forces_from_inputs(load, tangent, inputs.slip, inputs.angle, tire);
}

} // namespace slrr::physics

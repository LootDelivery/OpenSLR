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

double reference_grip(double aligned_load, const TireParameters& tire) {
  constexpr TireParameters stock;
  const double grip = std::clamp(tire.friction / stock.friction, 0.2, 2.5);
  double load_efficiency = 1.0;
  if (tire.maximum_load > 0.0) {
    const double normalized_load = aligned_load / tire.maximum_load;
    load_efficiency =
        1.0 / (1.0 + 0.18 * std::max(0.0, normalized_load - 0.5));
  }
  return grip * load_efficiency;
}

TireForces force_peaks(double aligned_load, double effective_grip,
                       const TireParameters& tire) {
  constexpr TireParameters stock;
  const auto& tuning = tire_tuning();
  double lateral_load_factor = 1.0;
  if (tire.maximum_load > 0.0) {
    const double capped = std::min(aligned_load, tire.maximum_load);
    constexpr double half_pi = std::numbers::pi / 2.0;
    const double progressive =
        std::atan(aligned_load * half_pi / tire.maximum_load) *
        tire.maximum_load / half_pi;
    lateral_load_factor =
        std::lerp(1.0, progressive / std::max(1.0, capped),
                  std::clamp(tire.load_smoothing * tuning.load_sensitivity,
                             0.0, 1.0));
  }
  return {aligned_load * 0.85 * effective_grip * stock.friction *
              std::max(0.0, tire.longitudinal_grip) * tuning.longitudinal_grip,
          aligned_load * 1.42 * effective_grip * lateral_load_factor *
              tuning.lateral_grip};
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

TireForces tire_friction_limits(double load, double alignment,
                                const TireParameters& tire) {
  const double aligned_load = std::max(0.0, load * alignment);
  if (aligned_load <= 0) return {};
  const auto peaks = force_peaks(aligned_load, reference_grip(aligned_load, tire), tire);
  return {std::max(0.0, peaks.longitudinal), std::max(0.0, peaks.lateral)};
}

namespace {
// The curves, evaluated at whatever slip inputs the caller supplies. There is
// deliberately no velocity-proportional clamp here: in the recovered model the
// low-speed problem is solved upstream, in the inputs.
TireForces force_from_slip(double aligned_load, double kappa, double alpha,
                           const TireParameters &tire) {
  constexpr TireParameters stock;
  const double stiffness =
      std::clamp(tire.stiffness / stock.stiffness, 0.4, 2.5);
  const double slide_ratio =
      std::clamp(tire.sliction / stock.sliction, 0.3, 1.5);

  const double slide = std::min(1.0, std::hypot(kappa * 1.5, alpha * 2.5));
  const double effective_grip =
      reference_grip(aligned_load, tire) * std::lerp(1.0, slide_ratio, slide);

  const auto &tuning = tire_tuning();
  const auto peaks = force_peaks(aligned_load, effective_grip, tire);

  // Pic latéral : rigidité de dérive assouplie (B = 11.5) pour placer le pic
  // vers 7-9° et garder une dérive contrôlable après décrochage.
  const double lateral_peak = peaks.lateral;

  // Slip combiné : le patinage longitudinal (kappa) réduit la rigidité
  // de dérive avant même d'atteindre la limite de l'ellipse de friction.
  const double longitudinal_saturation =
      std::clamp(std::abs(kappa) / 0.35, 0.0, 0.75);
  const double effective_lateral_b =
      11.5 * stiffness * tuning.lateral_response *
      (1.0 - 0.55 * longitudinal_saturation);

  double lateral =
      -magic_formula(alpha, effective_lateral_b, 1.32, lateral_peak, 0.35);

  // Pic longitudinal : C = 1.55 crée une vraie chute de traction quand la roue
  // bloque ou patine excessivement par rapport au slip optimal (~10-12%).
  const double longitudinal_peak = peaks.longitudinal;

  double longitudinal =
      magic_formula(kappa, 14.0 * stiffness, 1.55, longitudinal_peak, 0.45);

  // Ellipse de friction combinée
  if (lateral_peak > 0.0 && longitudinal_peak > 0.0) {
    const double demand = std::hypot(longitudinal / longitudinal_peak,
                                     lateral / lateral_peak);
    if (demand > 1.0) {
      longitudinal /= demand;
      lateral /= demand;
    }
  }

  return {longitudinal, lateral};
}
} // namespace

TireForces tire_forces(double load, double alignment, double wheel_speed,
                       double road_speed, double lateral_speed,
                       const TireParameters &tire) {
  const double aligned_load = std::max(0.0, load * alignment);
  if (aligned_load <= 0.0)
    return {0.0, 0.0};
  // Stateless view: the geometric inputs, with the old 1.5 m/s reference floor
  // only to keep the ratio finite. Production uses the stateful overload.
  const double reference =
      std::max({1.5, std::abs(road_speed), std::abs(wheel_speed)});
  const double kappa = (wheel_speed - road_speed) / reference;
  const double alpha =
      std::atan2(lateral_speed, std::max(1.5, std::abs(road_speed)));
  return force_from_slip(aligned_load, kappa, alpha, tire);
}

TireForces tire_forces(double load, double alignment, double wheel_speed,
                       double road_speed, double lateral_speed,
                       const TireParameters &tire, TireSlipState &state,
                       double dt) {
  const double aligned_load = std::max(0.0, load * alignment);
  if (aligned_load <= 0.0)
    return {0.0, 0.0};
  const auto inputs = tire_slip_inputs(wheel_speed, road_speed, lateral_speed,
                                       dt, state);
  return force_from_slip(aligned_load, inputs.slip, inputs.angle, tire);
}

} // namespace slrr::physics
// The drive coupling, measured on flat ground: the original contract ("thrust
// from the engine torque, ClutchF caps the clutch, the engine turns with the
// driveline") against the host's crank-inertia slip model, in one binary.
//
// What this pins is the driver's requirement: full throttle with no stability
// system must put the whole torque curve on the road and let the engine rev -- no
// invented limit on the engine, and no launch that bogs while it slips.
//
//   powershell -File scripts/run_native_io_tests.ps1 -Test test_drive_faithful
#include "standalone_test.hpp"

#include "slrr/tire_model.hpp"
#include "slrr/vehicle_dynamics.hpp"

#include <algorithm>
#include <cmath>
#include <limits>
#include <utility>

namespace {
using namespace slrr::physics;

const Terrain flat = [](double, double, double, GroundHit& hit) {
  hit = {};
  return true;
};

// A 1300 kg rear-drive saloon; the same shape the solver's own suite uses, so the
// only difference between the two runs below is the coupling.
VehicleDynamics make_car(bool faithful) {
  VehicleDynamics dynamics;
  dynamics.body.configure(1300, {-.85, .15, -2.2}, {.85, 1.4, 2.2}, {0, .45, .1},
                          {550, 2300, 2200});
  const double x[] = {-.75, .75, -.75, .75}, z[] = {-1.3, -1.3, 1.35, 1.35};
  for (int i = 0; i < 4; ++i) {
    auto& w = dynamics.wheels[i];
    w.enabled = true;
    w.center = {x[i], .31, z[i]};
    w.radius = .31;
    w.anchor = .35;
    w.rest = .478;
    w.minimum = .25;
    w.maximum = .45;
    w.spring = 25000;
    w.bump_damping = w.rebound_damping = 1500;
    w.unsprung = 25;
    auto& d = dynamics.drive.wheels[i];
    d.weight = i < 2 ? 0 : 1;  // rear drive
    d.brake = 2000;
    d.tyre = TireParameters{};
    d.tyre.friction = 1.6;
    d.tyre.maximum_load = 6500;
    d.tyre.load_smoothing = .4;
    d.tyre.longitudinal_grip = 1;
  }
  auto& drive = dynamics.drive;
  drive.gears = 5;
  const double ratios[] = {0, 12.9, 7.9, 5.6, 4.4, 3.6, 0, -12};
  std::copy(std::begin(ratios), std::end(ratios), drive.ratios.begin());
  drive.idle = 800;
  drive.limit = 7000;
  drive.inertia = .38;
  drive.brake_scale = drive.handbrake_scale = 1;
  drive.engaged = drive.automatic_shift = drive.auto_clutch = true;
  drive.clutch = 1;
  drive.clutch_force = 500;
  drive.faithful = faithful;
  drive.torque = [](double rpm) { return std::clamp(150 + rpm * .02, 0., 235.); };
  dynamics.engine_running = true;
  dynamics.engine_omega = 800 / 9.5493;
  Matrix frame{};
  frame[0] = frame[5] = frame[10] = frame[15] = 1;
  frame[13] = .05f;
  dynamics.body.set_frame(frame);
  return dynamics;
}

struct Trace {
  double hundred_kmh = -1;   // seconds to 100 km/h, or -1 never
  double top_kmh = 0;
  double max_rpm = 0, min_rpm_after_start = 1e9;
  int top_gear = 0;
  double max_slip_per_s = 0; // wheel spin against road speed, m/s
  bool finite = true;
};

Trace run_car(bool faithful, double seconds = 25) {
  auto car = make_car(faithful);
  car.drive.throttle = 1;
  Trace trace;
  const double dt = 1. / 120;
  for (double t = 0; t < seconds; t += dt) {
    car.advance(dt, flat);
    const double kph = car.body.velocity.length() * 3.6;
    const double rpm = car.engine_omega * 9.5493;
    if (!std::isfinite(kph) || !std::isfinite(rpm)) {
      trace.finite = false;
      break;
    }
    trace.top_kmh = std::max(trace.top_kmh, kph);
    if (trace.hundred_kmh < 0 && kph >= 100)
      trace.hundred_kmh = t;
    if (t > 1) trace.max_rpm = std::max(trace.max_rpm, rpm);
    trace.top_gear = std::max(trace.top_gear, car.gear);
    for (size_t i = 2; i < 4; ++i) {
      const double wheel_speed = std::abs(car.state[i].spin) * 0.31;
      trace.max_slip_per_s =
          std::max(trace.max_slip_per_s, std::abs(wheel_speed - kph / 3.6));
    }
  }
  return trace;
}

// Full throttle from rest reaches 100 km/h, and the engine is allowed to run: the
// faithful path must not be slower than the host's model that pins it.
SLRR_TEST(faithful_coupling_is_not_slower_than_the_host_model) {
  const Trace original = run_car(true);
  const Trace host = run_car(false);
  for (const auto &entry : {std::pair{"original", original},
                              std::pair{"host", host}}) {
    const char *label = entry.first;
    const Trace &trace = entry.second;
    std::fprintf(stderr,
                 "[drive_faithful] %-8s 0-100=%5.2fs top=%6.1f km/h "
                 "max_rpm=%6.0f gear=%d slip=%.2f m/s\n",
                 label, trace.hundred_kmh, trace.top_kmh, trace.max_rpm,
                 trace.top_gear, trace.max_slip_per_s);
  }
  SLRR_REQUIRE(original.finite && host.finite);
  SLRR_REQUIRE(original.hundred_kmh > 0);
  SLRR_REQUIRE(original.hundred_kmh <= host.hundred_kmh + 1e-9);
}

// "Clean power": the curve is applied, so a car that grips reaches 100 km/h in a
// plausible time instead of crawling on a slipping clutch.
SLRR_TEST(full_throttle_reaches_a_hundred_kilometres_an_hour) {
  const Trace trace = run_car(true);
  SLRR_REQUIRE(trace.finite);
  SLRR_REQUIRE(trace.hundred_kmh > 0);
  SLRR_REQUIRE(trace.hundred_kmh < 15.0);
}

// The revs follow the driveline and the limiter is the only ceiling: never more
// than a fraction above the limit, and the engine does use its upper range.
SLRR_TEST(the_engine_revs_without_an_invented_ceiling) {
  const Trace trace = run_car(true);
  SLRR_REQUIRE(trace.finite);
  SLRR_REQUIRE(trace.max_rpm <= 7000 * 1.05);
  SLRR_REQUIRE(trace.max_rpm > 7000 * .55);
}

// Gears are used: a launch that stays in first past 100 km/h is the old slip bug.
SLRR_TEST(the_gearbox_moves_through_the_gears) {
  const Trace trace = run_car(true);
  SLRR_REQUIRE(trace.finite);
  SLRR_REQUIRE(trace.top_gear >= 3);
}

// The clutch is a limit, not a wall: full torque reaches the tyres even when the
// engine is pinned by the limiter.
SLRR_TEST(torque_reaches_the_road_at_full_throttle) {
  const Trace trace = run_car(true);
  SLRR_REQUIRE(trace.finite);
  SLRR_REQUIRE(trace.top_kmh > 120);
}

// 500 Nm is above the engine's own torque here, so an intact car must not slip
// away the difference: the top speed still arrives.
SLRR_TEST(a_clutch_force_above_the_engine_torque_does_not_hold_the_car_back) {
  VehicleDynamics car = make_car(true);
  car.drive.clutch_force = 500;
  car.drive.throttle = 1;
  double top = 0;
  for (double t = 0; t < 20; t += 1. / 120) {
    car.advance(1. / 120, flat);
    top = std::max(top, car.body.velocity.length() * 3.6);
  }
  SLRR_REQUIRE(std::isfinite(top));
  SLRR_REQUIRE(top > 120);
}
}  // namespace

int main() {
  const slrr::standalone::Test tests[] = {
      faithful_coupling_is_not_slower_than_the_host_model,
      full_throttle_reaches_a_hundred_kilometres_an_hour,
      the_engine_revs_without_an_invented_ceiling,
      the_gearbox_moves_through_the_gears,
      torque_reaches_the_road_at_full_throttle,
      a_clutch_force_above_the_engine_torque_does_not_hold_the_car_back,
  };
  return slrr::standalone::run("drive_faithful", tests);
}

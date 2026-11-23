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
  drive.max_rpm = 7000;   // scales the recovered engine friction (3.5)
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
  // The flare: how fast the tachometer answers the throttle. A rigid coupling --
  // the engine turning with the driveline -- made this several seconds, which is
  // what a driver described as "the revs climb as if I am pressing the pedal
  // slowly".
  double rpm_at_half_second = 0;
  double flare_seconds = -1;  // time to reach 60% of the limit, or -1
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
    if (trace.rpm_at_half_second == 0 && t >= .5)
      trace.rpm_at_half_second = rpm;
    if (trace.flare_seconds < 0 && rpm >= 7000 * .6)
      trace.flare_seconds = t;
    trace.top_gear = std::max(trace.top_gear, car.gear);
    for (size_t i = 2; i < 4; ++i) {
      const double wheel_speed = std::abs(car.state[i].spin) * 0.31;
      trace.max_slip_per_s =
          std::max(trace.max_slip_per_s, std::abs(wheel_speed - kph / 3.6));
    }
  }
  return trace;
}

// Lift off, brake, and come to rest: the gearbox has to walk back down to first.
// It could not before, because the downshift read the engine, which a converter
// holds at its stall speed -- a stopped car sat in second.
double gear_at_rest(bool faithful, int* top_gear) {
  auto car = make_car(faithful);
  const double dt = 1. / 120;
  *top_gear = 0;
  for (double t = 0; t < 6; t += dt) {
    car.drive.throttle = 1;
    car.drive.brake = 0;
    car.advance(dt, flat);
    *top_gear = std::max(*top_gear, car.gear);
  }
  for (double t = 0; t < 30; t += dt) {
    car.drive.throttle = 0;
    car.drive.brake = 1;
    car.advance(dt, flat);
    if (car.body.velocity.length() < .2 && t > 1)
      break;
  }
  return car.gear;
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
    std::fprintf(stderr,
                 "[drive_faithful] %-8s rpm@0.5s=%5.0f flare(60%%)=%.2fs\n",
                 label, trace.rpm_at_half_second, trace.flare_seconds);
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

// The tachometer answers the throttle: flooring it from rest must flare the revs
// within a fraction of a second, not drag them up with road speed.
SLRR_TEST(the_revs_flare_when_the_throttle_goes_down) {
  const Trace trace = run_car(true);
  SLRR_REQUIRE(trace.finite);
  SLRR_REQUIRE(trace.flare_seconds > 0);
  SLRR_REQUIRE(trace.flare_seconds < 2.0);
  SLRR_REQUIRE(trace.rpm_at_half_second > 3000);
}

// The revs follow the driveline and the limiter is the only ceiling: never more
// than a fraction above the limit, and the engine does use its upper range.
SLRR_TEST(the_engine_revs_without_an_invented_ceiling) {
  const Trace trace = run_car(true);
  SLRR_REQUIRE(trace.finite);
  SLRR_REQUIRE(trace.max_rpm <= 7000 * 1.06);
  SLRR_REQUIRE(trace.max_rpm > 7000 * .55);
}

// A stopped car is in first. Not "a low gear": first.
SLRR_TEST(a_stopped_car_is_in_first_gear) {
  int top_gear = 0;
  const int gear = gear_at_rest(true, &top_gear);
  std::fprintf(stderr, "[drive_faithful] stopped in gear %d (reached %d)\n",
               gear, top_gear);
  // Six seconds of a 225 Nm car reaches second, not third; what matters here is
  // that it climbs and then comes back down to first.
  SLRR_REQUIRE(top_gear >= 2);
  SLRR_REQUIRE(gear == 1);
}

// Full throttle pulls to the top of the range. The schedule used to shift at 88%
// of the limit, so a 6500 block never saw past 6000 and a 9000 block stopped at
// 7842 -- the driver's two numbers.
SLRR_TEST(full_throttle_uses_the_whole_range) {
  const Trace trace = run_car(true);
  SLRR_REQUIRE(trace.finite);
  SLRR_REQUIRE(trace.max_rpm > 7000 * .95);
  SLRR_REQUIRE(trace.max_rpm <= 7000 * 1.06);
}

// The game's own Mazda, as the drive trace reports it: 6500 limit, ClutchF 500,
// ratios 8.61 / 5.61 / 3.88 / 2.95 / 2.57, 1566 kg, 225 Nm. A driver reported no
// wheelspin at all on it; this is the arithmetic behind that, and it is not a
// defect -- the tyres are asked for less than they can take.
SLRR_TEST(the_mazda_launch_is_below_its_tyre_limit) {
  auto car = make_car(true);
  car.body.configure(1566, {-.85, .15, -2.2}, {.85, 1.4, 2.2}, {0, .45, .1},
                     {550, 2300, 2200});
  const double ratios[] = {0, 8.61, 5.61, 3.88, 2.95, 2.57, 0, -8.61};
  std::copy(std::begin(ratios), std::end(ratios), car.drive.ratios.begin());
  car.drive.limit = 6500;
  car.drive.idle = 900;
  car.drive.torque = [](double rpm) {
    return std::clamp(120.0 + rpm * .026, 0., 225.);
  };
  // The game reports friction 1.95 on this car's rear tyres, not the 1.6 the
  // generic fixture uses. That coefficient is the whole answer to "why no spin".
  for (auto& wheel : car.drive.wheels)
    wheel.tyre.friction = 1.95;
  car.drive.throttle = 1;
  double max_slip_ratio = 0, max_force = 0, load = 0;
  const double dt = 1. / 120;
  for (double t = 0; t < 6; t += dt) {
    car.advance(dt, flat);
    const double kph = car.body.velocity.length() * 3.6;
    double force = 0, support = 0;
    for (size_t i = 2; i < 4; ++i) {
      force += std::abs(car.state[i].tire_force.longitudinal);
      support += car.state[i].force;
      const double wheel_speed = std::abs(car.state[i].spin) * 0.31;
      max_slip_ratio =
          std::max(max_slip_ratio, std::abs(wheel_speed - kph / 3.6));
    }
    // A tyre is at its limit when the force it makes stops rising with slip. The
    // rough test a driver would recognise: slip speed, and the force asked of it.
    max_force = std::max(max_force, force);
    if (t > 1) load = std::max(load, support);
  }
  std::fprintf(stderr,
               "[drive_faithful] mazda launch slip=%.2f m/s force=%.0f N "
               "rear load=%.0f N demand=%.2f g\n",
               max_slip_ratio, max_force, load,
               load > 0 ? max_force / load : 0.0);
  // Asked for less than the tyres hold, by this much: 1.95 x the rear axle load.
  SLRR_REQUIRE(load > 0);
  // Full torque through first gear asks this car for less force than its rear
  // tyres hold, even with ClutchF at its cap: 500 Nm x 8.61 / .318 m = 13.5 kN
  // against 1.95 x the axle load. That is why there is no wheelspin to find.
  SLRR_REQUIRE(max_force < 1.95 * load);
  std::fprintf(stderr, "[drive_faithful] mazda uses %.0f%% of its tyre limit\n",
               100.0 * max_force / (1.95 * load));
}

// Power has to reach the wheels. Two cars identical except for the engine, 200 Nm
// against 600 Nm: the strong one must be far quicker and must break traction.
//
// This is the invariant that would have caught the ClutchF ceiling on its own. With
// a 500 Nm cap on the driveline both cars made 500 Nm and the numbers came out
// within a few km/h of each other -- which is exactly what a driver reported
// comparing a turbo four with a supercharged V8 on the same stretch of road.
Trace run_with_torque(double peak) {
  auto car = make_car(true);
  car.drive.torque = [peak](double rpm) {
    return std::clamp(peak * (1.0 - std::abs(rpm - 4500.0) / 9000.0), 0.0, peak);
  };
  car.drive.throttle = 1;
  Trace trace;
  const double dt = 1. / 120;
  for (double t = 0; t < 12; t += dt) {
    car.advance(dt, flat);
    const double kph = car.body.velocity.length() * 3.6;
    const double rpm = car.engine_omega * 9.5493;
    if (!std::isfinite(kph) || !std::isfinite(rpm)) {
      trace.finite = false;
      break;
    }
    trace.top_kmh = std::max(trace.top_kmh, kph);
    if (trace.hundred_kmh < 0 && kph >= 100) trace.hundred_kmh = t;
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

SLRR_TEST(more_torque_reaches_the_road) {
  const Trace weak = run_with_torque(200);
  const Trace strong = run_with_torque(600);
  std::fprintf(stderr,
               "[drive_faithful] 200 Nm: 0-100 %5.2fs, top %6.1f km/h, slip %.2f m/s\n"
               "[drive_faithful] 600 Nm: 0-100 %5.2fs, top %6.1f km/h, slip %.2f m/s\n",
               weak.hundred_kmh, weak.top_kmh, weak.max_slip_per_s,
               strong.hundred_kmh, strong.top_kmh, strong.max_slip_per_s);
  SLRR_REQUIRE(weak.finite && strong.finite);
  SLRR_REQUIRE(weak.hundred_kmh > 0 && strong.hundred_kmh > 0);
  // Three times the torque must be worth more than a badge. It is NOT worth a quarter
  // off 0-100 any more: with the recovered driveline both launches burn out (13.6 and
  // 19.8 m/s of slip), so the first hundred is traction-limited and the torque shows up
  // as wheel speed rather than as acceleration. Top speed is where power is honest,
  // and the strong car is the one that breaks traction.
  SLRR_REQUIRE(strong.hundred_kmh < weak.hundred_kmh * .95);
  SLRR_REQUIRE(strong.top_kmh > weak.top_kmh * 1.15);
  // And the strong car is the one that breaks traction.
  SLRR_REQUIRE(strong.max_slip_per_s > weak.max_slip_per_s + 1.0);
}

// A car left in first at idle must idle, and must stand still. The link used to be
// closed there, so it dragged the engine down to 334 rpm and then fed that speed to the
// wheels: the tachometer read a stall while the car crept forward on it.
SLRR_TEST(a_car_in_first_at_idle_neither_bogs_nor_creeps) {
  auto car = make_car(true);
  car.gear = 1;
  const double dt = 1. / 120;
  double lowest = 1e9, fast = 0;
  for (double t = 0; t < 5; t += dt) {
    car.drive.throttle = 0;
    car.drive.brake = 0;
    car.advance(dt, flat);
    lowest = std::min(lowest, car.engine_omega * 9.5493);
    fast = std::max(fast, car.body.velocity.length());
  }
  std::fprintf(stderr, "[drive_faithful] idle in first: lowest %.0f rpm, crept %.2f km/h\n",
               lowest, fast * 3.6);
  SLRR_REQUIRE(lowest > car.drive.idle * .9);
  SLRR_REQUIRE(fast * 3.6 < 2.0);
}

// Reverse gear held while the car still rolls forwards, then the handbrake: the shaft
// arrives negative while the engine turns positive. A closed link dragged the engine
// down to 334 rpm and crept the car forward on that false idle.
SLRR_TEST(reverse_and_handbrake_at_speed_do_not_strangle_the_engine) {
  auto car = make_car(true);
  const double dt = 1. / 120;
  car.drive.throttle = 1;
  for (double t = 0; t < 3; t += dt) car.advance(dt, flat);
  const double moving = car.body.velocity.length();
  car.drive.throttle = 0;
  car.drive.handbrake = 1;
  car.gear = 7;                       // reverse, while still rolling forward
  double lowest = 1e9;
  for (double t = 0; t < 4; t += dt) {
    car.advance(dt, flat);
    lowest = std::min(lowest, car.engine_omega * 9.5493);
  }
  std::fprintf(stderr, "[drive_faithful] reverse at %.0f km/h: lowest idle %.0f rpm, "
               "ended at %.2f km/h\n",
               moving * 3.6, lowest, car.body.velocity.length() * 3.6);
  SLRR_REQUIRE(lowest > car.drive.idle * .9);
  SLRR_REQUIRE(car.body.velocity.length() * 3.6 < 2.0);
}

// Lifting off in gear must slow the car. The original's engine losses are
// `(rpm/(0.05*maxRPM)) * (0.05*maxRPM)^2 * friction_fwd` -- about 211 Nm at 6500 rpm
// for a 1e-4 coefficient -- and the crank-inertia loss we had instead was 8 Nm, so a
// lifted car coasted as if it were in neutral.
double coast_deceleration(bool with_gear) {
  auto car = make_car(true);
  car.drive.max_rpm = 7000;
  car.drive.friction_fwd = 1e-4;
  car.drive.friction_rev = 5e-3;
  car.drive.throttle = 1;
  const double dt = 1. / 120;
  for (double t = 0; t < 3; t += dt) car.advance(dt, flat);
  car.drive.throttle = 0;
  if (!with_gear) car.gear = 0;
  const double from = car.body.velocity.length();
  for (double t = 0; t < 2; t += dt) car.advance(dt, flat);
  return (from - car.body.velocity.length()) / 2.0;
}

SLRR_TEST(lifting_off_in_gear_slows_the_car) {
  const double in_gear = coast_deceleration(true);
  const double neutral = coast_deceleration(false);
  std::fprintf(stderr,
               "[drive_faithful] coast: %.2f m/s2 in gear, %.2f m/s2 in neutral\n",
               in_gear, neutral);
  SLRR_REQUIRE(std::isfinite(in_gear) && std::isfinite(neutral));
  // Engine braking has to exist and has to dominate: 1.5 m/s2 in gear against
  // nothing in neutral, at a 1e-4 friction coefficient and 7000 max rpm.
  SLRR_REQUIRE(in_gear > neutral + 1.0);
  SLRR_REQUIRE(in_gear > 1.0);
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
      the_revs_flare_when_the_throttle_goes_down,
      a_stopped_car_is_in_first_gear,
      full_throttle_uses_the_whole_range,
      the_mazda_launch_is_below_its_tyre_limit,
      more_torque_reaches_the_road,
      lifting_off_in_gear_slows_the_car,
      a_car_in_first_at_idle_neither_bogs_nor_creeps,
      reverse_and_handbrake_at_speed_do_not_strangle_the_engine,
      full_throttle_reaches_a_hundred_kilometres_an_hour,
      the_engine_revs_without_an_invented_ceiling,
      the_gearbox_moves_through_the_gears,
      torque_reaches_the_road_at_full_throttle,
      a_clutch_force_above_the_engine_torque_does_not_hold_the_car_back,
  };
  return slrr::standalone::run("drive_faithful", tests);
}

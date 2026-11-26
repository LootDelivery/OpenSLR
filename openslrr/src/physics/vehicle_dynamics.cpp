#include "slrr/suspension_geometry.hpp"
#include "slrr/dyno_model.hpp"
#include "slrr/vehicle_dynamics.hpp"
#include <algorithm>
#include <cmath>
#include <numbers>
#include <stdexcept>

namespace slrr::physics {

double Vec3::length() const { return std::sqrt(dot(*this)); }

Vec3 Vec3::normalized() const {
  const double n = length();
  return n > 1e-12 ? *this / n : Vec3{};
}

bool Vec3::finite() const {
  return std::isfinite(x) && std::isfinite(y) && std::isfinite(z);
}

Quaternion Quaternion::operator*(Quaternion b) const {
  return {w * b.x + x * b.w + y * b.z - z * b.y,
          w * b.y - x * b.z + y * b.w + z * b.x,
          w * b.z + x * b.y - y * b.x + z * b.w,
          w * b.w - x * b.x - y * b.y - z * b.z};
}

Quaternion Quaternion::normalized() const {
  double n = std::sqrt(x * x + y * y + z * z + w * w);
  if (!(n > 1e-12) || !std::isfinite(n))
    throw std::runtime_error("invalid rigid orientation");
  return {x / n, y / n, z / n, w / n};
}

Vec3 Quaternion::rotate(Vec3 v) const {
  Vec3 axis{x, y, z};
  Vec3 t = axis.cross(v) * 2;
  return v + t * w + axis.cross(t);
}

Vec3 Quaternion::inverse_rotate(Vec3 v) const {
  return Quaternion{-x, -y, -z, w}.rotate(v);
}

void RigidBody::configure(double kilograms, Vec3 lo, Vec3 hi, Vec3 com) {
  if (!(kilograms > 0) || !std::isfinite(kilograms) || !lo.finite() ||
      !hi.finite() || !com.finite() || hi.x <= lo.x || hi.y <= lo.y ||
      hi.z <= lo.z)
    throw std::invalid_argument("invalid rigid body mass/bounds");
  mass = kilograms;
  minimum = lo;
  maximum = hi;
  center = com;

  // A uniform box over the authored bounds. This used to be 0.75 of the
  // dimensions, which is 0.56 of the box once squared; on the Baiern that put
  // the yaw inertia at 1481 kg m2 where a plain box over its own 1.73 x 4.21 m
  // bounds gives 2244, against 1800-2500 for a real 1300 kg car. The 0.75 was
  // host tuning, and it is what made the tail rotate about 1.5x faster than the
  // geometry allows: on a 70 km/h flick the box value grows the slide to 0.29 rad
  // instead of 0.41 and keeps 55 km/h instead of 17. The exe's own tensor is not
  // recovered; a plain box is the honest baseline.
  Vec3 s = hi - lo;
  inverse_inertia_ = {12.0 / (mass * (s.y * s.y + s.z * s.z)),
                      12.0 / (mass * (s.x * s.x + s.z * s.z)),
                      12.0 / (mass * (s.x * s.x + s.y * s.y))};
}

void RigidBody::configure(double kilograms, Vec3 lo, Vec3 hi, Vec3 com,
                          Vec3 inertia) {
  configure(kilograms, lo, hi, com);
  if (inertia.finite() && inertia.x > 0 && inertia.y > 0 && inertia.z > 0) {
    inverse_inertia_ = {1.0 / inertia.x, 1.0 / inertia.y, 1.0 / inertia.z};
  }
}

Vec3 RigidBody::inertia() const {
  return {1.0 / inverse_inertia_.x, 1.0 / inverse_inertia_.y, 1.0 / inverse_inertia_.z};
}

void RigidBody::set_frame(const Matrix &m) {
  for (float v : m)
    if (!std::isfinite(v))
      throw std::invalid_argument("invalid rigid frame");
  const double trace = m[0] + m[5] + m[10];
  if (trace > 0) {
    double s = std::sqrt(trace + 1) * 2;
    orientation = {(m[6] - m[9]) / s, (m[8] - m[2]) / s, (m[1] - m[4]) / s,
                   s / 4};
  } else if (m[0] > m[5] && m[0] > m[10]) {
    double s = std::sqrt(1 + m[0] - m[5] - m[10]) * 2;
    orientation = {s / 4, (m[4] + m[1]) / s, (m[8] + m[2]) / s,
                   (m[6] - m[9]) / s};
  } else if (m[5] > m[10]) {
    double s = std::sqrt(1 + m[5] - m[0] - m[10]) * 2;
    orientation = {(m[4] + m[1]) / s, s / 4, (m[9] + m[6]) / s,
                   (m[8] - m[2]) / s};
  } else {
    double s = std::sqrt(1 + m[10] - m[0] - m[5]) * 2;
    orientation = {(m[8] + m[2]) / s, (m[9] + m[6]) / s, s / 4,
                   (m[1] - m[4]) / s};
  }
  orientation = orientation.normalized();
  position = {m[0] * center.x + m[4] * center.y + m[8] * center.z + m[12],
              m[1] * center.x + m[5] * center.y + m[9] * center.z + m[13],
              m[2] * center.x + m[6] * center.y + m[10] * center.z + m[14]};
  velocity = momentum = force_ = torque_ = {};
}

Matrix RigidBody::frame() const {
  Vec3 x = orientation.rotate({1, 0, 0}), y = orientation.rotate({0, 1, 0}),
       z = orientation.rotate({0, 0, 1});
  Vec3 p = position - orientation.rotate(center);
  return {float(x.x), float(x.y), float(x.z), 0,
          float(y.x), float(y.y), float(y.z), 0,
          float(z.x), float(z.y), float(z.z), 0,
          float(p.x), float(p.y), float(p.z), 1};
}

Vec3 RigidBody::inverse_world_inertia(Vec3 v) const {
  Vec3 a = orientation.inverse_rotate(v);
  return orientation.rotate({a.x * inverse_inertia_.x, a.y * inverse_inertia_.y,
                             a.z * inverse_inertia_.z});
}

Vec3 RigidBody::omega() const { return inverse_world_inertia(momentum); }

void RigidBody::set_omega(Vec3 angular_velocity) {
  const Vec3 local = orientation.inverse_rotate(angular_velocity);
  momentum = orientation.rotate({local.x / inverse_inertia_.x,
                                 local.y / inverse_inertia_.y,
                                 local.z / inverse_inertia_.z});
}

Vec3 RigidBody::point_velocity(Vec3 p) const {
  return velocity + omega().cross(p - position);
}

void RigidBody::add_force(Vec3 f, Vec3 p) {
  force_ += f;
  torque_ += (p - position).cross(f);
}

void RigidBody::impulse(Vec3 j, Vec3 p) {
  velocity += j / mass;
  momentum += (p - position).cross(j);
}

void RigidBody::integrate(double dt) {
  integrate_forces(dt);
  integrate_pose(dt);
}

void RigidBody::integrate_forces(double dt) {
  if (!std::isfinite(dt) || dt <= 0)
    throw std::invalid_argument("invalid physics dt");
  velocity += force_ * (dt / mass);
  velocity.y -= 9.81 * dt;
  momentum = (momentum + torque_ * dt) * std::exp(-0.015 * dt);
  force_ = torque_ = {};
  if (!velocity.finite() || !momentum.finite())
    throw std::runtime_error("non-finite rigid state");
}

void RigidBody::integrate_pose(double dt) {
  if (!std::isfinite(dt) || dt <= 0)
    throw std::invalid_argument("invalid physics dt");
  position += velocity * dt;
  Vec3 w = omega();
  double angle = w.length() * dt;
  if (angle > 1e-12) {
    Vec3 axis = w.normalized() * std::sin(angle * .5);
    orientation =
        (Quaternion{axis.x, axis.y, axis.z, std::cos(angle * .5)} * orientation)
            .normalized();
  }
  if (!position.finite() || !velocity.finite() || !momentum.finite())
    throw std::runtime_error("non-finite rigid state");
}

double RigidBody::contact(Vec3 p, Vec3 normal, double restitution,
                         double friction, RigidBody *other) {
  const Vec3 n = normal.normalized();
  const auto relative_velocity = [&] {
    return point_velocity(p) - (other ? other->point_velocity(p) : Vec3{});
  };
  const auto inverse_mass = [&](const RigidBody &body, Vec3 axis) {
    const Vec3 r = p - body.position;
    return 1 / body.mass +
           axis.dot(body.inverse_world_inertia(r.cross(axis)).cross(r));
  };
  const auto exchange = [&](Vec3 j) {
    impulse(j, p);
    if (other)
      other->impulse(-j, p);
  };
  const Vec3 v = relative_velocity();
  double vn = v.dot(n);
  if (vn >= 0)
    return 0;
  const double denom =
      inverse_mass(*this, n) + (other ? inverse_mass(*other, n) : 0);
  if (!(denom > 0))
    return 0;
  double magnitude = -(1 + (vn < -.8 ? restitution : 0)) * vn / denom;
  exchange(n * magnitude);
  const Vec3 post = other ? relative_velocity() : v;
  Vec3 tangent = post - n * post.dot(n);
  double speed = tangent.length();
  if (speed > 1e-8) {
    tangent = tangent / speed;
    const double d = inverse_mass(*this, tangent) +
                     (other ? inverse_mass(*other, tangent) : 0);
    exchange(tangent * (-std::min(friction * magnitude, speed / d)));
  }
  return magnitude;
}

std::array<Vec3, 8> RigidBody::corners() const {
  std::array<Vec3, 8> out;
  unsigned i = 0;
  for (double x : {minimum.x, maximum.x})
    for (double y : {minimum.y, maximum.y})
      for (double z : {minimum.z, maximum.z})
        out[i++] = position + orientation.rotate(Vec3{x, y, z} - center);
  return out;
}

Vec3 body_point(const RigidBody &body, Vec3 local) {
  return body.position + body.orientation.rotate(local - body.center);
}

double suspension_force(double length, double speed, double rest, double max,
                        double k, double bump, double rebound) {
  if (length >= max)
    return 0.0;
  const double spring_force = (rest - length) * k;
  const double damping_coeff =
      speed >= 0.0 ? (rebound > 0.0 ? rebound : 0.7 * bump) : bump;
  // Atténuation progressive de la détente près de la butée haute (top-out)
  // pour éviter un claquement numérique quand la roue décolle.
  const double travel_left = std::clamp((max - length) / std::max(0.02, max - rest), 0.0, 1.0);
  const double effective_damping = speed >= 0.0 ? damping_coeff * travel_left : damping_coeff;
  return std::max(0.0, spring_force - speed * effective_damping);
}

namespace {
bool slide_trace() {
  static const bool on = std::getenv("SLRR_SLIDE_TRACE") != nullptr;
  return on;
}
// Typical real-world values, not recovered from the original game.
constexpr double air_density = 1.225;      // kg/m³, ISA sea level at 15 °C
constexpr double frontal_fill = .85;       // frontal area / (width × height)
constexpr double body_drag = .32;          // road-car Cd on the frontal area
constexpr double body_cross_drag = 1.0;    // crossflow Cd of the side and plan
constexpr double body_lift = .25;          // road-car CL on the frontal area
constexpr double wing_lift_slope = 4.2;    // per radian, 2π·A/(A+2) at A = 4
constexpr double wing_normal_limit = 1.5;  // stalled single-element wing
constexpr double wing_profile_drag = .05;
// Wind at car height: the log wind profile over open terrain (roughness
// length 0.03 m) at 1 m, about the height of a car's aerodynamic centre.
constexpr double wind_roughness = .03, wind_height = 1, wind_report_height = 10;
// Gusts: plane waves frozen in the moving air (Taylor's hypothesis), three
// along the wind and three across it, with fixed phases so a run repeats.
// Their 28–240 m wavelengths span the turbulence length scales near the
// ground.
struct Gust {
  double wavelength, angle, phase;  // m; wave direction from the wind, rad
  bool across;
};
constexpr Gust gusts[] = {{240, 0, 0, false},   {90, .6, 2.1, false},
                          {35, -.9, 4.4, false}, {160, 1.2, 1.3, true},
                          {60, -.4, 3.7, true},  {28, .3, 5.6, true}};

// Calls apply(force, point) with world vectors for each aerodynamic force.
template <class Apply>
void each_aero_force(const Aerodynamics &aero, const RigidBody &body, double dt,
                     Apply &&apply) {
  // One step's impulse never stops a point past rest along a drag force.
  const auto stopping_force = [&](Vec3 point, Vec3 axis, double speed) {
    const Vec3 arm = point - body.position;
    const double inverse =
        1 / body.mass + axis.dot(body.inverse_world_inertia(arm.cross(axis)).cross(arm));
    return inverse > 0 ? std::abs(speed) / (inverse * dt) : 0.0;
  };
  // EXE 0x456A83: Chassis.C_drag as -C |v| v above 1 m/s.
  if (aero.drag > 0) {
    const Vec3 point = body_point(body, aero.drag_center);
    const Vec3 velocity = body.point_velocity(point) - aero.wind;
    const double square = velocity.dot(velocity);
    if (square > 1) {
      const double speed = std::sqrt(square);
      const Vec3 direction = velocity / speed;
      apply(direction * -std::min(aero.drag * square,
                                  stopping_force(point, direction, speed)),
            point);
    }
  }
  // The body, from the air's velocity in the car's axes: drag along each
  // axis on the projected bounds, a side force at a quarter of the length
  // from the leading end, and lift from the flow along the car.
  if (aero.body) {
    const Vec3 size = body.maximum - body.minimum;
    const Vec3 centre = (body.minimum + body.maximum) * .5;
    const Vec3 point = body_point(body, centre);
    const Vec3 local = body.orientation.inverse_rotate(body.point_velocity(point) - aero.wind);
    const double frontal = frontal_fill * size.x * size.y;
    const double side = frontal_fill * size.z * size.y;
    const double plan = frontal_fill * size.x * size.z;
    // Frontal Cd on the flow along the car, crossflow Cd on the flow across
    // its side and plan views, each at the centre of the bounds.
    const auto axis_drag = [&](Vec3 axis, double speed, double cd_area) {
      if (!(std::abs(speed) > 1e-3)) return;
      const Vec3 direction = body.orientation.rotate(axis);
      apply(direction * -std::copysign(std::min(.5 * air_density * cd_area * speed * speed,
                                                stopping_force(point, direction, speed)),
                                       speed),
            point);
    };
    axis_drag({0, 0, 1}, local.z, body_drag * frontal);
    axis_drag({1, 0, 0}, local.x, body_cross_drag * side);
    axis_drag({0, 1, 0}, local.y, body_cross_drag * plan);
    // Slender-body side force: the side view as a wing of chord L and span
    // 2H, the ground mirroring it, so πAR/2 = πH/L per radian of yaw on the
    // side area, at the quarter chord.
    if (std::abs(local.x) > 1e-3 && std::abs(local.z) > 1e-3) {
      const Vec3 quarter{centre.x, centre.y,
                         local.z < 0 ? body.minimum.z + .25 * size.z
                                     : body.maximum.z - .25 * size.z};
      const Vec3 at = body_point(body, quarter);
      const Vec3 direction = body.orientation.rotate({1, 0, 0});
      const double slope = std::numbers::pi * size.y / size.z;
      apply(direction * -std::copysign(
                            std::min(.5 * air_density * slope * side *
                                         std::abs(local.x * local.z),
                                     stopping_force(at, direction, local.x)),
                            local.x),
            at);
    }
    apply(body.orientation.rotate({0, 1, 0}) *
              (.5 * air_density * body_lift * frontal * local.z * local.z),
          point);
  }
  // Wings: the face meeting the air carries the normal force of the whole
  // element, rising with incidence until it stalls, plus its profile drag.
  for (const auto &wing : aero.wings) {
    const Vec3 point = body_point(body, wing.position);
    const Vec3 normal = body.orientation.rotate(wing.normal);
    const Vec3 velocity = body.point_velocity(point) - aero.wind;
    const double square = velocity.dot(velocity);
    if (!(square > 1e-6)) continue;
    const double speed = std::sqrt(square);
    const double incidence = velocity.dot(normal) / speed;  // sin α
    if (incidence <= 0) continue;
    const double pressure = .5 * air_density * wing.area * square;
    apply(normal * -(pressure * std::min(wing_lift_slope * incidence, wing_normal_limit)) -
              velocity / speed * (pressure * wing_profile_drag),
          point);
  }
}
} // namespace

Vec3 wind_velocity(const Wind &wind, Vec3 position, double seconds) {
  if (!(wind.speed > 0) || !std::isfinite(wind.speed) || !std::isfinite(wind.heading) ||
      !position.finite() || !std::isfinite(seconds))
    return {};
  const double profile = std::log(wind_height / wind_roughness);
  const double mean = wind.speed * profile / std::log(wind_report_height / wind_roughness);
  // Turbulence intensity 1/ln(z/z0) along the wind (EN 1991-1-4 §4.4), 0.75
  // of it across; each set of three waves shares its variance.
  const double along_gust = mean / profile * std::sqrt(2. / 3);
  const double across_gust = .75 * along_gust;
  const double heading = wind.heading * std::numbers::pi / 180;
  const Vec3 along{std::sin(heading), 0, std::cos(heading)};
  const Vec3 across{along.z, 0, -along.x};
  // The point's coordinates in the air that the mean wind carries.
  const double x = position.dot(along) - mean * seconds, y = position.dot(across);
  Vec3 air = along * mean;
  for (const auto &gust : gusts) {
    const double phase = 2 * std::numbers::pi / gust.wavelength *
                             (x * std::cos(gust.angle) + y * std::sin(gust.angle)) +
                         gust.phase;
    air += (gust.across ? across * across_gust : along * along_gust) * std::sin(phase);
  }
  return air;
}

AeroLoad aero_load(const Aerodynamics &aero, const RigidBody &body, double dt) {
  AeroLoad load;
  each_aero_force(aero, body, dt, [&](Vec3 force, Vec3 point) {
    load.force += force;
    load.torque += (point - body.position).cross(force);
  });
  return load;
}

void VehicleDynamics::apply_aero() {
  each_aero_force(aero, body, timestep, [&](Vec3 force, Vec3 point) {
    if (force.finite()) body.add_force(force, point);
  });
}

void solve_linked(RigidBody &body, const WheelConfig &w, WheelState &s,
                  const Terrain &terrain, double ceiling, double dt) {
  const Vec3 local_down = w.down.normalized();
  const Vec3 down = body.orientation.rotate(local_down);
  const Vec3 hard = body_point(body, w.center - local_down * w.anchor_length());
  if (!s.seeded) {
    s.length = std::clamp(w.rest, w.minimum, w.maximum);
    s.speed = 0;
    s.seeded = true;
  }
  s.length = std::clamp(s.length, w.minimum, w.maximum);
  const auto project = [&](double length) {
    return body_point(body, wheel_pose(w, length).hub_origin);
  };
  const auto sample = [&](Vec3 world, GroundHit &hit) {
    return down.y < -.05 &&
           terrain(world.x, world.z, std::max(ceiling, world.y + 1), hit);
  };
  const auto tyre_error = [&](Vec3 world, const GroundHit &hit) {
    const Vec3 normal = hit.normal.normalized();
    return normal.dot(Vec3{world.x, hit.height, world.z} - world) + w.radius;
  };
  double speed = 0;
  double free_length = w.maximum;
  if (w.unsprung > 0) {
    const double hold = suspension_force(s.length, s.speed, w.rest, w.maximum,
                                         w.spring, w.bump_damping, w.rebound_damping);
    speed = s.speed + (hold - w.unsprung * 9.81 * down.y) / w.unsprung * dt;
    free_length = s.length + speed * dt;
  }
  double seated = std::clamp(s.length, w.minimum, w.maximum);
  for (int i = 0; i < 8; ++i) {
    const Vec3 world = project(seated);
    GroundHit hit;
    if (!sample(world, hit))
      break;
    const double error = tyre_error(world, hit);
    if (std::fabs(error) < 1e-4)
      break;
    const double probe =
        seated < w.maximum - 1e-4 ? seated + 1e-3 : seated - 1e-3;
    const double slope =
        (project(probe) - world).dot(hit.normal.normalized()) / (probe - seated);
    if (std::fabs(slope) < 1e-5)
      break;
    const double next =
        std::clamp(seated + error / slope, w.minimum, w.maximum);
    if (next == seated)
      break;
    seated = next;
  }
  GroundHit hit;
  const Vec3 seated_hub = project(seated);
  const bool contact = free_length >= seated - 1e-4 &&
                       sample(seated_hub, hit) &&
                       tyre_error(seated_hub, hit) >= -2e-3;
  const double length =
      contact ? seated : std::clamp(free_length, w.minimum, w.maximum);
  if (contact)
    speed = 0;
  const auto pose = wheel_pose(w, length);
  const Vec3 hub_w = body_point(body, pose.hub_origin);
  const Vec3 normal = contact ? hit.normal.normalized() : Vec3{0, 1, 0};
  const double rate = speed + body.point_velocity(hard).dot(normal);
  double force = suspension_force(length, rate, w.rest, w.maximum, w.spring,
                                  w.bump_damping, w.rebound_damping);
  force = std::clamp(force, 0.0, body.mass * 9.81 * 3);
  if (length <= w.minimum && speed < 0)
    speed = 0;
  if (length >= w.maximum && speed > 0)
    speed = 0;
  s.length = length;
  s.speed = speed;
  s.contact = contact;
  s.force = s.contact ? force : 0;
  if (s.contact)
    body.add_force(normal * s.force, hard);
  s.normal = normal;
  s.hub = pose.hub_origin;
  s.hub_rotation = pose.hub_rotation;
  s.center = hub_w;
  s.patch = s.center - normal * w.radius;
}

namespace {
// Host-designed compliant support, solved implicitly and jointly. Independent
// explicit dampers can exceed the 120 Hz stability bound when several low
// points act on the same rotation, even if each individual damper is stable.
struct FixedSupport {
  Vec3 point, normal;
  double inverse = 0, softness = 0, target = 0, limit = 0, impulse = 0;
  int wheel = -1;
};
FixedSupport fixed_support(const RigidBody& body, Vec3 point, Vec3 normal,
                           double depth, bool metal, double dt, int wheel = -1,
                           double installed_damping = 0) {
  const Vec3 arm = point - body.position;
  const double inverse = 1 / body.mass +
      normal.dot(body.inverse_world_inertia(arm.cross(normal)).cross(arm));
  const double stiffness = body.mass * (metal ? 800.0 : 160.0);
  const double damping = 1.4 * std::sqrt(stiffness / inverse) +
                         std::max(0.0, installed_damping);
  const double response = damping + stiffness * dt;
  return {point, normal, inverse, 1 / (dt * response),
          stiffness * depth / response, body.mass * 9.81 * 3 * dt, 0, wheel};
}
void solve_fixed_support(const RigidBody& body, std::array<FixedSupport, 12>& contacts,
                         size_t count, double dt) {
  if (!count) return;
  auto predicted = body;
  predicted.integrate_forces(dt); // healthy springs, external forces and gravity
  for (int iteration = 0; iteration < 16; ++iteration) {
    for (size_t i = 0; i < count; ++i) {
      auto& c = contacts[i];
      const double speed = predicted.point_velocity(c.point).dot(c.normal);
      const double delta = (c.target - speed - c.softness * c.impulse) /
                           (c.inverse + c.softness);
      const double next = std::clamp(c.impulse + delta, 0.0, c.limit);
      predicted.impulse(c.normal * (next - c.impulse), c.point);
      c.impulse = next;
    }
  }
}

struct RestContact {
  Vec3 point, normal, heading;
  double limit_x = 0, limit_y = 0;
  Vec3 impulse;
};

// Project the contact impulses into their friction disks, after gravity but
// before pose integration. Unlike velocity damping this can actually stick
// without advancing the chassis downhill by a tiny amount every tick.
void solve_rest_contacts(RigidBody& body, std::array<RestContact, 12>& contacts,
                         size_t count) {
  for (int iteration = 0; iteration < 12; ++iteration) {
    for (size_t i = 0; i < count; ++i) {
      auto& c = contacts[i];
      const Vec3 seed = std::abs(c.normal.y) < .9 ? Vec3{0, 1, 0} : Vec3{1, 0, 0};
      const Vec3 x = c.heading.length() > .5 ? c.heading.normalized() : seed.cross(c.normal).normalized();
      const Vec3 y = c.normal.cross(x);
      const Vec3 r = c.point - body.position;
      const auto inverse = [&](Vec3 a, Vec3 b) {
        return a.dot(b) / body.mass +
            a.dot(body.inverse_world_inertia(r.cross(b)).cross(r));
      };
      const double xx = inverse(x, x), yy = inverse(y, y), xy = inverse(x, y);
      const double determinant = xx * yy - xy * xy;
      if (!(determinant > 1e-18)) continue;
      const Vec3 v = body.point_velocity(c.point);
      const double vx = v.dot(x), vy = v.dot(y);
      const double old_x = c.impulse.dot(x), old_y = c.impulse.dot(y);
      const double gx = vx - xx * old_x - xy * old_y;
      const double gy = vy - xy * old_x - yy * old_y;
      double ix = 0, iy = 0;
      if (c.limit_x <= 0) {
        if (c.limit_y > 0) iy = std::clamp(-gy / yy, -c.limit_y, c.limit_y);
      } else if (c.limit_y <= 0) {
        ix = std::clamp(-gx / xx, -c.limit_x, c.limit_x);
      } else {
        ix = (xy * gy - yy * gx) / determinant;
        iy = (xy * gx - xx * gy) / determinant;
        if (std::hypot(ix / c.limit_x, iy / c.limit_y) > 1) {
          // Project in the effective-mass metric, not radially. This is the
          // minimum post-impulse kinetic energy inside the friction ellipse;
          // arbitrary anisotropic clipping can otherwise add energy.
          double a = xx * c.limit_x * c.limit_x, b = yy * c.limit_y * c.limit_y;
          double cross = xy * c.limit_x * c.limit_y;
          double fx = gx * c.limit_x, fy = gy * c.limit_y;
          const double scale = std::max({a, b, std::abs(cross), std::abs(fx), std::abs(fy)});
          a /= scale; b /= scale; cross /= scale; fx /= scale; fy /= scale;
          const auto at = [&](double multiplier) {
            const double d = (a + multiplier) * (b + multiplier) - cross * cross;
            return Vec3{(cross * fy - (b + multiplier) * fx) / d,
                        (cross * fx - (a + multiplier) * fy) / d, 0};
          };
          double lo = 0, hi = std::hypot(fx, fy);
          for (int pass = 0; pass < 24; ++pass) {
            const double middle = (lo + hi) * .5;
            if (at(middle).length() > 1) lo = middle;
            else hi = middle;
          }
          const auto q = at(hi);
          ix = q.x * c.limit_x;
          iy = q.y * c.limit_y;
        }
      }
      const auto energy = [&](double a, double b) {
        return .5 * (xx * a * a + 2 * xy * a * b + yy * b * b) + gx * a + gy * b;
      };
      if (!std::isfinite(ix) || !std::isfinite(iy) || energy(ix, iy) > energy(old_x, old_y)) {
        ix = old_x;
        iy = old_y;
      }
      const Vec3 candidate = x * ix + y * iy;
      body.impulse(candidate - c.impulse, c.point);
      c.impulse = candidate;
    }
  }
}

// Host approximation following the bounded paired chassis couple in
// SLRR_rev 77b0fb6, suspension-3d.ts (not recovered MWM physics). Reimplemented
// mathematically, with per-corner rest offsets removed to avoid a static
// preload from unequal spring rest lengths. Tyre loads have already been used.
void solve_anti_roll(RigidBody& body, const std::array<WheelConfig, 4>& wheels,
                     const std::array<WheelState, 4>& state,
                     const std::array<AntiRollConfig, 2>& config) {
  for (size_t axle = 0; axle < config.size(); ++axle) {
    const auto coefficient = [](double value) {
      return std::isfinite(value) && value > 0 ? value : 0.0;
    };
    const double stiffness = coefficient(config[axle].stiffness);
    const double damping = coefficient(config[axle].damping);
    const double scale = std::max(stiffness, damping);
    if (!(scale > 0)) continue; // Preserve the unconfigured force path exactly.
    const size_t left = axle * 2, right = left + 1;
    if (!wheels[left].enabled || !wheels[right].enabled ||
        wheels[left].rigid_contact || wheels[right].rigid_contact ||
        !state[left].contact || !state[right].contact)
      continue;
    const auto hardpoint = [&](size_t corner) {
      const auto& w = wheels[corner];
      return body_point(body, w.center - w.down.normalized() * w.anchor_length());
    };
    const Vec3 a = hardpoint(left), b = hardpoint(right);
    if (!a.finite() || !b.finite()) continue;
    const double deflection = (state[right].length - wheels[right].rest) -
                              (state[left].length - wheels[left].rest);
    const double speed = body.point_velocity(a).y - body.point_velocity(b).y;
    const double limit = body.mass * (.5 * 9.81);
    if (!std::isfinite(deflection) || !std::isfinite(speed) ||
        !std::isfinite(limit) || !(limit > 0))
      continue;
    // Scale before combining: even finite DBL_MAX coefficients must not
    // overflow two products into an indeterminate infinity-minus-infinity.
    const double response = (stiffness / scale) * deflection -
                            (damping / scale) * speed;
    if (response == 0) continue;
    const double force = std::abs(response) >= limit / scale
        ? std::copysign(limit, response) : response * scale;
    body.add_force({0, force, 0}, a);
    body.add_force({0, -force, 0}, b);
  }
}

constexpr double rpm_per_rad = 9.54929658551372;
constexpr double wheel_inertia = 1.6;

Vec3 wheel_forward(const RigidBody &body, double steer, Quaternion hub = {}) {
  return body.orientation.rotate(
      (hub * wheel_steering(steer)).rotate({0, 0, -1}));
}

// The wheel's own axle (its local +X) in world axes. The recovered grip scale
// measures the contact normal in the WHEEL's frame -- 0x00454500 takes
// sqrt(s[1]^2 + s[2]^2) with component 0 on the axle -- so the axle has to come
// from the hub's own orientation, not from a vector built out of the normal:
// anything derived from the normal is perpendicular to it by construction and
// measures exactly zero.
Vec3 wheel_axle(const RigidBody &body, double steer, Quaternion hub = {}) {
  return body.orientation.rotate((hub * wheel_steering(steer)).rotate({1, 0, 0}));
}

// Recovered engine friction (3.5), in the units it is written in: the rpm terms
// are rev/min, the viscous bound is rad/s, and the original takes the two as a
// MINIMUM -- a bound on how much friction the engine can make, not a sum. Read as
// a maximum it became 1131 Nm at 6000 rpm and the car could not pull away.
//
//   drag = (rpm / (0.05*maxRPM)) * (0.05*maxRPM)^2 * friction_fwd   (rpm > 0)
//   drag = rpm^2 * friction_rev                                     (rpm <= 0)
//   visc = 0.5 * |omega_rad| * engine_inertia
//   friction = min(drag, visc)
double original_engine_friction(double rpm, double max_rpm, double fwd,
                               double rev, double inertia) {
  const double scale = .05 * std::max(1.0, max_rpm);
  const double drag = rpm > 0
                          ? (rpm / scale) * scale * scale * std::max(0.0, fwd)
                          : rpm * rpm * std::max(0.0, rev);
  const double visc = .5 * std::abs(rpm) * engine_rpm_to_rad *
                      std::max(0.0, inertia);
  return std::min(drag, visc);
}

double limiter_factor(double rpm, double previous, double limit) {
  const double predicted = 2 * rpm - previous;
  if (predicted <= limit)
    return rpm > limit ? (limit - predicted) / (rpm - predicted) : 1;
  return rpm > limit ? 0 : (limit - rpm) / (predicted - rpm);
}
} // namespace

// wheel+0xEC, the slip denominator of 0x00454500 (CAR_MODEL_RECOVERY 14.7). The
// listing writes, per wheel:
//
//   if (sum|share| <= 0.0001 || share <= 0.0001 || total_ratio == 0)
//       fVar16 = gearbox+0xE8;
//   else
//       fVar16 = (share/sum|share|)*(1-car+0x1DBC)*|total_ratio|*gearbox+0xE0
//                + gearbox+0xE8;
//   wheel[0xEC] = 0.1/fVar16;
//
// The per-wheel `share <= 0.0001` test is what gives the undriven wheels exactly
// gearbox+0xE8: measured live, the MC's fronts read 35.0000 and its rears
// 35.0228195 in first gear with the clutch open (clutch 0.999608), which this
// reproduces to nine significant digits. `brake_inertia` is the sixth float of
// the engine record (part_config's brake_inertia).
double wheel_slip_denominator(double share, double share_sum, double clutch_open,
                              double total_ratio, double engine_inertia,
                              double brake_inertia) {
  const double e8 = brake_inertia;
  if (!(share_sum > 0.0001) || !(share > 0.0001) || total_ratio == 0)
    return e8;
  return (share / share_sum) * (1 - clutch_open) * std::abs(total_ratio) *
             engine_inertia +
         e8;
}

DriveTuning &drive_tuning() {
  static DriveTuning tuning;
  return tuning;
}

double wheel_inverse_inertia(double share, double share_sum, double clutch_open,
                             double total_ratio, double engine_inertia,
                             double brake_inertia) {
  const double denominator = wheel_slip_denominator(
      share, share_sum, clutch_open, total_ratio, engine_inertia, brake_inertia);
  return denominator > 0 ? 0.1 / denominator : 0.0;
}

void VehicleDynamics::update_gear(double rpm, double shaft,
                                  double road_shaft) {
  const bool shift_pressed = drive.shift && drive.shift != previous_shift_;
  previous_shift_ = drive.shift;
  if (!drive.engaged || drive.gears <= 0) {
    gear = 0;
    shift_time_ = 0;
    forced_reverse_ = false;
    brake_reverse_latched_ = false;
    return;
  }

  double road_radius = 0;
  for (size_t i = 0; i < wheels.size(); ++i)
    if (wheels[i].enabled && std::abs(drive.wheels[i].weight) <= 1e-3 && wheels[i].radius > 0) {
      road_radius = wheels[i].radius;
      break;
    }
  const double road_kph =
      std::abs(road_shaft) * (road_radius > 0 ? road_radius : .31) * 3.6;

  shift_time_ = std::max(0.0, shift_time_ - timestep);
  if (gear != 7 && gear > drive.gears)
    gear = drive.gears;
  // One-pedal classic: Down held brakes while moving forward; at a
  // standstill it selects reverse, and reverse stays selected after the
  // pedal is released (leaving R is a shift action, not a pedal release).
  // The pedal swap that makes the held Down drive backwards is in the
  // input stage (part_runtime), gated on gear==7 && brake>0.5.
  const double raw_brake = drive.raw_brake > 0.001 ? drive.raw_brake : drive.brake;
  if ((road_kph < 2.0 && raw_brake > .5) || drive.reverse)
    brake_reverse_latched_ = true;
  if (brake_reverse_latched_ && !forced_reverse_ && drive.ratios[7] < 0) {
    forced_reverse_ = true;
    gear = 7;
    shift_time_ = 0;
  } else if (shift_pressed && shift_time_ == 0) {
    const int low = (drive.ratios[7] < 0 && road_kph < 2.0) ? -1 : 0;
    const int next = std::clamp((gear == 7 ? -1 : gear) + drive.shift,
                                low, drive.gears);
    if (forced_reverse_ && next >= 0) {
      // Shifting out of reverse clears the selection; staying at -1 keeps it.
      forced_reverse_ = false;
      brake_reverse_latched_ = false;
    }
    gear = next < 0 ? 7 : next;
    shift_time_ = .22;
  }

  if (road_kph < 2.0 && gear > 1 && gear != 7) {
    // A stopped car in a forward gear is in first: no lock, no conditions.
    // Reverse (7) is excluded: resetting it here made R unreachable at a
    // standstill, which is exactly where reverse is selected.
    gear = 1;
    shift_time_ = .1;
    return;
  }

  if ((!drive.automatic_shift && !drive.automatic_first) || !engine_running ||
      shift_time_ > 0 || drive.clutch < .5)
    return;
  if (gear == 0 && drive.throttle > .1)
    gear = 1;
  if (!drive.automatic_shift)
    return;
  // A standstill must still be evaluated: the old `shaft <= 2` early return skipped
  // the whole schedule once the shaft stopped turning, which is why a car that came
  // to rest in second stayed in second. Only the upshift needs the shaft to move.
  if (gear <= 0 || gear == 7)
    return;
  // Both decisions read the driveline, not the engine. With a torque converter the
  // engine speed is a throttle-set number while the car is slow, so a schedule built
  // on it shifted up at 88% of the limit under full throttle (a 6500 block never saw
  // past 6000) and never shifted down at a standstill, because the engine was still
  // sitting on its stall speed: a stopped car stayed in second.
  const double ratio = drive.ratios[gear];
  static const bool allow_spin_shift =
      std::getenv("SLRR_GEAR_NOSPIN") != nullptr;
  // TEST (DRV-12): with SLRR_GEAR_NOSPIN the schedule is judged on the
  // input shaft (mean wheel spin, slip included) instead of the road
  // shaft, so a slipping clutch no longer hides the shift speed.
  const double coupled_rpm =
      std::abs((allow_spin_shift ? shaft : road_shaft) * ratio) * rpm_per_rad;
  // Throttle-dependent, like an automatic's own schedule: full throttle pulls to the
  // limiter, a light foot shifts early.
  static const bool gear_trace = std::getenv("SLRR_GEAR_TRACE") != nullptr;
  if (gear_trace)
    std::fprintf(stderr,
                 "[gear] g=%d shaft=%.2f road=%.2f thr=%.2f coupled=%.0f "
                 "rpm=%.0f st=%.2f\n",
                 gear, shaft, road_shaft, drive.throttle, coupled_rpm, rpm,
                 shift_time_);
  // The original blocks the shift while the driven wheels are spinning
  // (0x0044D410, average |wheel+0x11C| over the driven wheels). The measured launch
  // row shows why it matters: the engine sat on the limiter in first for the whole
  // 2.5 s of the burn-out and never shifted. Judged against the car's own speed, so a
  // spinning wheel is the only thing that can trip it.
  double driven_spin = 0, driven_count = 0;
  for (size_t i = 0; i < wheels.size(); ++i)
    if (wheels[i].enabled && std::abs(drive.wheels[i].weight) > 1e-3) {
      driven_spin += std::abs(state[i].spin);
      ++driven_count;
    }
  // SLRR_GEAR_NOSPIN also ignores the retail spinning block. Default keeps
  // the 0x0044D410 behaviour.
  const bool spinning = !allow_spin_shift && driven_count > 0 &&
      driven_spin / driven_count > std::abs(road_shaft) * 1.15 + 1.0;
  // Realistic schedule: WOT just under the limiter, part throttle earlier,
  // but the coast floor stays high enough that lifting the gas no longer
  // drops the upshift point below speed and walks up through the gears.
  const double up_rpm = drive.limit *
      (.72 + .23 * std::clamp(drive.throttle, 0.0, 1.0));
  const double down_rpm = drive.limit * (.34 + .16 * std::clamp(drive.throttle, 0.0, 1.0));
  // The upshift is judged on the driveline alone, closed throttle included: an
  // automatic upshifts while coasting at speed too, and because coupled_rpm falls
  // with the car it cannot walk up through the gears during a stop the way a
  // schedule built on engine speed did.
  if (shaft > 2 && !spinning && drive.brake < .3 && gear < drive.gears &&
      drive.ratios[gear + 1] > 0 && coupled_rpm > up_rpm) {
    ++gear;
    shift_time_ = .12;
  } else if (gear > 1 && coupled_rpm < down_rpm &&
             coupled_rpm * drive.ratios[gear - 1] / ratio < drive.limit * .95) {
    --gear;
    shift_time_ = .22;
  }
}

double VehicleDynamics::engine_limit_fraction() const {
  return std::clamp(limiter_factor(engine_omega * rpm_per_rad, previous_rpm_, drive.limit), 0.0, 1.0);
}

void VehicleDynamics::step_drive() {
  const double rpm = engine_omega * rpm_per_rad;
  double weights = 0, shaft = 0, road_shaft = 0, square_weights = 0,
         launch_slip = 0;
  for (size_t i = 0; i < wheels.size(); ++i) {
    if (!wheels[i].enabled)
      continue;
    const auto &cfg = drive.wheels[i];
    const auto &s = state[i];
    const double w = cfg.weight;
    weights += w;
    shaft += w * s.spin;
    square_weights += w * w;
    if (s.contact && wheels[i].radius > 0) {
      const Vec3 forward = wheel_forward(body, cfg.steer, s.hub_rotation);
      const Vec3 tangent =
          (forward - s.normal * forward.dot(s.normal)).normalized();
      const double road_speed = body.point_velocity(s.patch).dot(tangent);
      road_shaft += w * road_speed / wheels[i].radius;
      if (drive.launch_help > 0 && w > 0)
        launch_slip = std::max(
            launch_slip, (std::abs(s.spin) * wheels[i].radius -
                          std::abs(road_speed)) /
                             std::max(2.5, std::abs(road_speed)));
    }
  }
  if (weights > 0) {
    shaft /= weights;
    road_shaft /= weights;
  }
  update_gear(rpm, shaft, road_shaft);
  const double ratio = drive.ratios[gear];
  const double forward_speed = body.velocity.dot(wheel_forward(body, 0));
  // An assist for rolling the wrong way with no power requested -- NOT for a spun car.
  // The condition is satisfied by any vehicle that has rotated past ~90 degrees, because
  // the body's velocity then points backwards along its own heading. Firing there set the
  // service brake to 1.0, forced the throttle to zero and skipped the whole driveline
  // There was a "direction brake" here: `(ratio > 0 && forward_speed < -1)` set the service
  // brake to 1.0, forced the throttle to zero and skipped the driveline. It has no
  // recovered provenance -- it arrived with the first import -- and its condition is met by
  // every car that has rotated past ~90 degrees, because the body's velocity then points
  // backwards along its own heading. Its measured effect was a spun car stopping dead
  // (16.9 -> 1.5 m/s in 2 s) with the body pitching, which the driver reported as "as if
  // the tyre hit a curb, the body rocks". Gating it on the throttle was not enough: at the
  // end of a spin the driver lifts, and then it fires anyway. The original brakes on nothing
  // but the pedal, and direction is the gearbox's business, so it is deleted rather than
  // tuned.
  const bool automatic_shift = drive.automatic_shift && shift_time_ > 0;
  const bool accelerator = drive.throttle > .1;
  if (!engine_running && accelerator && !accelerator_down_)
    starter_remaining_ = 2;
  accelerator_down_ = accelerator;
  const bool starting = !engine_running && drive.starter > 0 &&
                        (accelerator || starter_remaining_ > 0);
  starter_remaining_ = std::max(0.0, starter_remaining_ - timestep);
  // The recovered ABS (FUN_0044df70) and ASR (FUN_0044e120), whole-car pedal
  // modulation. ABS: with the brake pressed, the mean of the braked loaded
  // wheels' accumulated slip (wheel[0x14C], the same filter state the force
  // law consumes) reduces the brake pedal; thresholds 0.2/0.4 on 1/abs
  // (gearbox+0xA28), rate blend (x - prev)*4 + x, memory car+0x1F54, the
  // indicator flag 0x1000000 under service < 0.9. ASR: same skeleton on the
  // throttle over the DRIVEN loaded wheels, thresholds 0.2/0.3 on 1/asr
  // (gearbox+0xA2C), rate blend *2, memory car+0x1F50, indicator 0x2000000
  // (the icon that does light in the original); it also opens the clutch:
  // car+0x1DBC (the openness the exchange consumes as 1-openness) becomes
  // 1 - openness*service, i.e. our engagement is simply scaled by service.
  // Both replace our invented per-wheel |road|/r*0.85 branch. abs/asr <=
  // 0.01 leave the memories untouched, no qualifying wheels zeroes them --
  // both as in the listings. The modulations sit before the pedal smoothing
  // so the smoothed pedal follows the modulated one.
  drive.abs_active = drive.asr_active = false;
  if (drive.abs_strength > 0 && drive.brake > .01) {
    double sum = 0;
    int loaded = 0;
    for (size_t i = 0; i < wheels.size(); ++i) {
      const auto &s = state[i];
      if (!wheels[i].enabled || drive.wheels[i].brake <= 0 || !s.contact ||
          s.force <= 0)
        continue;
      sum += std::abs(s.slip_state.slip);
      ++loaded;
    }
    if (loaded > 0) {
      const double inv = 1 / std::max(1e-6, (double)drive.abs_strength);
      const double mean = sum / loaded;
      double x = mean - inv * .2;
      x = (x - drive.abs_prev) * 4.0 + x;
      if (x > 0) {
        const double pedal = std::max(0.0, 1.0 - x / (inv * .4));
        drive.brake = std::min(pedal * drive.brake, drive.brake);
        drive.abs_active = pedal < .9;
      }
      drive.abs_prev = mean - inv * .2;
    } else {
      drive.abs_prev = 0;
    }
  }
  drive.asr_clutch = 1;
  if (drive.launch_help > 0 && drive.throttle > .01) {
    double sum = 0;
    int driven = 0;
    for (size_t i = 0; i < wheels.size(); ++i) {
      const auto &s = state[i];
      if (!wheels[i].enabled || drive.wheels[i].weight == 0 || !s.contact ||
          s.force <= 0)
        continue;
      sum += std::abs(s.slip_state.slip);
      ++driven;
    }
    if (driven > 0) {
      const double inv = 1 / std::max(1e-6, (double)drive.launch_help);
      const double mean = sum / driven;
      double x = mean - inv * .2;
      x = (x - drive.asr_prev) * 2.0 + x;
      if (x > 0) {
        const double pedal = std::max(0.0, 1.0 - x / (inv * .3));
        drive.throttle = pedal * drive.throttle;
        drive.asr_active = pedal < .9;
        if (drive.asr_active)
          drive.asr_clutch = pedal;
        // The listing also presses the clutch (car+0x1DBC, FUN_0044e120:41:
        // `open = 1 - (1-open)*service`). 0x1DBC is the clutch pedal: the input
        // stage writes it every frame (0x44F39A: `pedal = (pedal-x)*k + x`) and
        // FUN_0044DD70 presses it to keep the engine off the driveline, so the
        // ASR multiplies the engagement on top of a value it does not own.
        // Ported literally and measured 2026-11-25 (ASR strength 1, launch):
        // 0.25 m/s after 3 s against 9.97 m/s with ASR off, i.e. the cut fires
        // fully and the car crawls -- the driver's original complaint. The law
        // itself is now complete; the fault is the INPUT scale: both laws read
        // wheel+0x11C (FUN_0044df70:25 and FUN_0044e120:25 read the same field,
        // NOT 0x14C), and our stand-in saturates at 1.0 where the listing's
        // field does not, so `x` clears the 0.3 threshold every frame. Decode
        // wheel+0x11C before enabling the write, and do not retune the
        // threshold to hide it.
      }
      drive.asr_prev = mean - inv * .2;
    } else {
      drive.asr_prev = 0;
    }
  }

  service_brake_ =
      std::lerp(service_brake_, drive.brake, -std::expm1(-timestep / .08));
  const double friction = engine_friction_torque(rpm);
  const double pumping = 14 + std::max(0.0, rpm - drive.idle) * .012;
  const double idle_torque =
      rpm < drive.idle ? std::min(80.0, 25.0 + (drive.idle - rpm) * .25) : 0;
  const double combustion = std::clamp((rpm - 200) / 450, 0.0, 1.0);
  const double kph = std::abs(forward_speed) * 3.6;
  double gas = std::clamp(drive.throttle, 0.0, 1.0);
  if (drive.launch_help <= 0 || !engine_running || drive.clutch < .5 ||
      drive.brake > 0 || drive.handbrake > .2 || gas < .2 ||
      (gear != 1 && gear != 7) || kph >= 30) {
    launch_active_ = false;
    launch_factor_ = 1;
  } else if (launch_active_ || (kph < 2 && gas >= .6)) {
    launch_active_ = true;
    const double target =
        std::clamp(1 - std::max(0.0, launch_slip - .15) * 2, .05, 1.0);
    launch_factor_ = std::lerp(
        launch_factor_, target,
        -std::expm1(-timestep / (target < launch_factor_ ? .04 : .15)));
    gas *= std::lerp(1.0, launch_factor_, drive.launch_help);
  }
  double combustion_torque = 0;
  if (engine_running && rpm < drive.limit && !automatic_shift) {
    const double limiter = engine_limit_fraction();
    // Dyno banks are net torque: recover indicated torque before scaling.
    double torque = (drive.torque(rpm) + friction) * gas * limiter;
    // MWM 454A15: blend the limited dry output toward the wet bank; the
    // nitrous input is independent of the accelerator (unlike visual flames).
    if (drive.nitro > 0 && drive.nitro_torque)
      torque = std::lerp(torque, drive.nitro_torque(rpm) + friction,
                         std::clamp(drive.nitro, 0.0, 1.0) * limiter);
    combustion_torque = (torque + idle_torque * limiter) * combustion;
  }
  previous_rpm_ = rpm;
  // The dyno supplies net WOT torque. Recover its indicated contribution
  // before scaling combustion, then apply the shared mechanical loss once.
  // The existing drag curve is incremental closed-throttle pumping, not a
  // second WOT friction loss. Fuel cut leaves mechanical friction active.
  const double net = combustion_torque + (starting ? 4 * drive.starter : 0) -
                     friction - pumping * (1 - gas);
  // Recovered 4.3: the original writes `rpm += M/J_engine` with no `dt` in it, and the
  // step IS the frame. Measured on a live launch it is ~51 000 rpm/min per second --
  // 1000 -> 7430 rpm in 125 ms -- which no dt-scaled integration produces. That is the
  // whole of "the revs are lazy": our engine was 60x too slow, and the converter's stall
  // point was papering over it.
  engine_omega = std::max(0.0,
                          engine_omega +
                              net * (drive.faithful
                                         ? 1.0 / drive.inertia
                                         : timestep / std::max(1e-6, drive.host_inertia)));
  // Per-step integration is violent in the other direction too: with the throttle
  // shut, friction alone takes the engine to zero in a single step, and a stopped
  // engine stops the gearbox schedule from running at all (it returned early and the
  // car sat in third). The original holds idle through its own governor; keeping the
  // recovered idle speed is the smallest honest stand-in for it.
  if (drive.faithful && gas <= .05 && drive.idle > 0)
    engine_omega = std::max(engine_omega, drive.idle * .98 / rpm_per_rad);

  double drive_impulse = 0;
  if (drive.faithful && engine_running && weights > 0 && ratio != 0 &&
      shift_time_ == 0 && gear != 0 && drive.clutch > .01) {
    // The recovered driveline, written to reproduce the measured launch row
    // (docs/PHYSICS_MODEL_PROVENANCE.md 12.4):
    //
    //   wheel_shaft = sum(spin * C4 * drive) / sum|drive|     // rad/s, C4 = 1
    //   shaft       = wheel_shaft * total_ratio                // engine side
    //   inv_d       = J_drive / ratio^2
    //   k           = inv_d / (K*J_engine + inv_d)
    //   gap         = rpm_free - shaft
    //   shaft'      = shaft      + 0.999*(1-k)*gap             // the light side moves most
    //   rpm'        = rpm_free   - 0.999*k*gap
    //   spin_i      = spin_i + (shaft'/ratio - wheel_shaft)*|drive_i|/sum|drive|
    //
    // rpm_free is the engine after the step above. J_engine and J_drive come from the
    // car (4.87 / 500 on the measured one, 13.5 / 965 on another); K is 6.0 in memory,
    // 4.0 in the file, because the mod patches .data at start-up.
    //
    // The clutch is a regulator in the original, but it never actually slips: measured
    // across the launch the engine/shaft ratio held 13.05..13.20 against a 13.18 ratio,
    // under 1%, while car+0x1DBC moved 0 -> 0.357 -> 0. So the driveline is modelled
    // locked, and what slips is the tyre -- which is what the row shows: the driven
    // wheels run 0 -> 538 rpm in 125 ms with the car still standing.
    //
    // The one guard kept: a standing car with the engine at idle must not drag the
    // engine to a stop, so below idle with no shaft speed the link is open.

    double wh_sum = 0, wh_abs = 0;
    for (size_t i = 0; i < wheels.size(); ++i) {
      const double d = drive.wheels[i].weight;
      if (!wheels[i].enabled || std::abs(d) <= 1e-3) continue;
      wh_sum += state[i].spin * d;
      wh_abs += std::abs(d);
    }
    const double wheel_shaft = wh_abs > 1e-9 ? wh_sum / wh_abs : 0.0;
    const double shaft_omega = wheel_shaft * ratio;
    const double inv_d = drive.driveline_inertia > 1e-9
                             ? drive.driveline_inertia / (ratio * ratio)
                             : 0.0;
    const double j_engine = std::max(1e-9, drive.inertia);
    const double coupling_k = inv_d / (drive.coupling_ratio * j_engine + inv_d);
    const double gap = engine_omega - shaft_omega;
    constexpr double exchange = .9990000128746033;
    // Two states open the link, both of them places where a closed one would either
    // stall the engine or hold the car against its own brakes:
    //   - the engine at idle with no shaft speed (standing still, not driving off);
    //   - braking at low speed, where the driveline would otherwise keep the wheels
    //     turning at idle speed and the car would creep and never stop.
    double engage = 1.0;
    // Anti-stall, from the regulator's own branch (0x0044DD70: engine at or below idle ->
    // `wanted = 1.0`, fully disengaged) and NOT gated on the throttle, which is where this
    // port had it. Measured in a live slide: throttle 1.00, first gear, the driven wheels
    // brought to a standstill, crank 2489 -> 2170 -> 0 rpm, then gear 0 and no drive for
    // the rest of the corner -- the reported "the wheels stop and the revs fall, as if the
    // gas were released". The link was dragging a dead crank because the only guard was
    // `gas <= .05`. Opening here lets the engine rev on its own and re-engage the moment it
    // climbs past idle.
    if (drive.faithful && engine_omega * rpm_per_rad <= drive.idle * 1.02)
      engage = 0.0;
    {
      const double engine_rpm = engine_omega * rpm_per_rad;
      const double shaft_rpm = std::abs(shaft_omega) * rpm_per_rad;
      const bool idle_and_stopped =
          engine_rpm <= drive.idle * 1.02 && shaft_rpm < drive.idle * .5;
      const bool braking_at_rest = drive.brake > .2 && engine_rpm < drive.idle * 1.3 &&
                                   shaft_rpm < drive.idle * 1.3;
      // Closed throttle with the driveline slower than idle: a clutch here would either
      // slip or stall the engine, and a closed link does both wrong at once -- it drags
      // the engine to 334 rpm and feeds that speed to the wheels, so the car creeps
      // forward at an idle that is really a stall.
      const bool idling_against_a_stop = gas <= .05 && shaft_rpm < drive.idle * 1.3;
      // Reverse gear held while the car still rolls forwards, with the handbrake down:
      // the engine-side shaft arrives negative against a positive crank and a closed
      // link drags the engine to zero -- the 334 rpm idle that crept forward. The
      // recovered boundary branch of 0x0044DD70 (`rpm < shaft -> open`) covers it.
      //
      // Gated on the gear being reverse, and that gate is the whole point: in a spin the
      // driven wheels can briefly rotate backwards about their own axles while the car
      // pivots, which empties the same condition in a FORWARD gear -- and opening there
      // dropped the engine to idle mid-slide instead of keeping the drift alive. In a
      // forward gear the driveline is what the engine drives, whatever the wheels are
      // doing; in reverse against the car's motion it is the road driving the engine.
      const bool dragged_backwards = ratio < 0 && shaft_omega < 0 && engine_omega > 0;
      if (idle_and_stopped || braking_at_rest || idling_against_a_stop || dragged_backwards)
        engage = 0.0;
    }
    // car+0x1DBC, the clutch measure the wheel step reads (0.1/fVar16 keeps the
    // share term only while it is closed). Measured: 1.000 standing in gear, 0.000
    // in neutral, which is exactly the port's `engage` inverted.
    wheel_clutch_open_ = 1.0 - engage * drive.asr_clutch;
    // The recovered exchange, exactly as 0x00454500 (683-695) writes it:
    //
    //   shaft' = shaft + engage*0.999*(1-k)*(rpm_free - shaft)
    //   rpm'   = rpm   - engage*0.999*k*(rpm_free - shaft)
    //
    // It was carrying an extra "torque cap" here for a while -- a bound on how much the link
    // could take off the crank in one step -- on the reasoning that a clutch is a torque
    // path, not a speed constraint. The recovery says otherwise: this IS a speed exchange,
    // `clutch_torque` (car+0x1E24) is only a slip measure for gearbox wear, and there is no
    // bound. That cap was the reason a driven wheel could be stopped by its own tyre in a
    // slide: it limited precisely the term that outruns the tyre every frame.
    const double shaft_new = shaft_omega + engage * exchange * (1.0 - coupling_k) * gap;
    const double rpm_new = engine_omega - engage * exchange * coupling_k * gap;
    const double wheel_step = shaft_new / ratio - wheel_shaft;
    if (slide_trace())
      std::fprintf(stderr,
                   "[drive] eng=%.0f gas=%.2f brk=%.2f erpm=%5.0f shrpm=%5.0f "
                   "ratio=%5.2f shift=%.2f clutch=%.2f k=%.3f gap=%6.1f "
                   "abs=%d asr=%d\n",
                   engage, gas, drive.brake, engine_omega * rpm_per_rad,
                   std::abs(shaft_omega) * rpm_per_rad, ratio, shift_time_,
                   drive.clutch, coupling_k, gap, (int)drive.abs_active,
                   (int)drive.asr_active);
    if (wh_abs > 1e-9)
      for (size_t i = 0; i < wheels.size(); ++i) {
        const double d = drive.wheels[i].weight;
        if (!wheels[i].enabled || std::abs(d) <= 1e-3) continue;
        state[i].spin += wheel_step * (std::abs(d) / wh_abs);
      }
    engine_omega = std::clamp(rpm_new, 0.0, drive.limit * 1.05 / rpm_per_rad);
    // The idle governor above runs before the link, so the link could pull the engine
    // below idle -- which is how a standing car came to idle at 334 rpm. A closed
    // throttle never lets the driveline decide the engine's speed.
    if (gas <= .05 && drive.idle > 0)
      engine_omega = std::max(engine_omega, drive.idle * .98 / rpm_per_rad);
    drive_impulse = coupling_k * gap * j_engine * ratio;
    {
      static const bool trace = std::getenv("SLRR_COUPLING_TRACE") != nullptr;
      static int counter = 0;
      if (trace && (counter++ % 6) == 0)
        std::fprintf(stderr,
                     "[coupling] rpm=%.0f shaft=%.0f k=%.3f engage=%.2f rpm_free=%.0f "
                     "gear=%d gas=%.2f w0=%.0f w2=%.0f\n",
                     engine_omega * rpm_per_rad, shaft_new * rpm_per_rad, coupling_k,
                     engage, engine_omega * rpm_per_rad, gear, gas,
                     state[0].spin * rpm_per_rad, state[2].spin * rpm_per_rad);
    }
  } else if (weights > 0 && ratio != 0 && shift_time_ == 0 &&
      (engine_running || !drive.auto_clutch)) {
    const double transmission =
        wheel_inertia * weights * weights / square_weights;
    const double reflected = transmission / (ratio * ratio);
    const double mix = reflected / (drive.inertia + reflected);
    const double mismatch = shaft * ratio - engine_omega;
    constexpr double exchange_rate = .9990000128746033;
    const double closed_delta = mismatch * mix * exchange_rate;
    double grip = drive.clutch;
    const double stall_risk = std::clamp(
        (drive.idle * 1.5 -
         std::min(rpm, std::abs(shaft * ratio) * rpm_per_rad)) /
            std::max(1.0, drive.idle * .5),
        0.0, 1.0);
    // Launch aid. Off by 0: the clutch transmits what it is asked to, the engine
    // revs freely and the wheels take the torque -- which is what a driver
    // comparing with the original game expects. A positive fraction holds the
    // slipping clutch at that fraction of the rev limit until the shaft catches
    // up, which is a player aid, not a property of the gearbox.
    if (drive.auto_clutch && drive.launch_rpm > 0 && closed_delta < 0) {
      double engagement_rpm = drive.idle * .85;
      if (drive.automatic_shift && (gear == 1 || gear == 7) &&
          drive.brake < .05 && drive.handbrake < .05 && gas > 0) {
        engagement_rpm = std::lerp(
            engagement_rpm,
            std::max(engagement_rpm,
                     drive.limit * std::clamp(drive.launch_rpm, .0, 1.0)),
            gas);
      }
      grip = std::min(grip,
                      std::max(0.0, engine_omega - engagement_rpm / rpm_per_rad) /
                          -closed_delta);
    }
    if (drive.auto_clutch && gas <= .05) {
      const double low_speed = std::clamp((12 - kph) / 10, 0.0, 1.0);
      grip *= 1 - stall_risk * low_speed;
    }
    engine_omega = std::max(0.0, engine_omega + closed_delta * grip);
    drive_impulse =
        -mismatch * (1 - mix) * grip * exchange_rate / ratio * transmission;
    for (size_t i = 0; i < wheels.size(); ++i)
      if (wheels[i].enabled)
        state[i].spin +=
            drive_impulse * drive.wheels[i].weight / (weights * wheel_inertia);
  } else if (automatic_shift && engine_running) {
    const double target =
        std::max(drive.idle, std::abs(road_shaft * ratio) * rpm_per_rad);
    engine_omega = std::lerp(engine_omega, target / rpm_per_rad,
                             -std::expm1(-timestep / .07));
  }

  // Différentiel autobloquant (LSD 1.5-way avec précharge minimale)
  if (drive.differential_lock > 0 && weights > 0) {
    const double lock_ratio = std::clamp(drive.differential_lock, 0.0, 1.0);
    for (size_t left = 0; left < wheels.size(); left += 2) {
      const size_t right = left + 1;
      if (right >= wheels.size())
        break;
      const double left_weight = drive.wheels[left].weight;
      const double right_weight = drive.wheels[right].weight;
      if (!wheels[left].enabled || !wheels[right].enabled ||
          left_weight <= 0 || right_weight <= 0)
        continue;

      double transfer =
          (state[left].spin - state[right].spin) * wheel_inertia * 0.5;
      if (lock_ratio < 1.0) {
        const double axle_share = (left_weight + right_weight) / weights;
        const double preload = 35.0 * lock_ratio * timestep;
        // Plein verrouillage sous couple positif, ~40% en décélération (1.5-way)
        const double ramp = drive_impulse >= 0.0 ? 1.0 : 0.4;
        const double limit =
            preload +
            std::abs(drive_impulse) * axle_share * lock_ratio * ramp;
        transfer = std::clamp(transfer, -limit, limit);
      }
      state[left].spin -= transfer / wheel_inertia;
      state[right].spin += transfer / wheel_inertia;
    }
  }

  const double next_rpm = engine_omega * rpm_per_rad;
  if (engine_running && next_rpm < std::max(250.0, drive.idle * .35))
    engine_running = false;
  else if (!engine_running && next_rpm >= std::max(500.0, drive.idle * .65) &&
           (starting || (!drive.auto_clutch && gas > 0))) {
    engine_running = true;
    starter_remaining_ = 0;
  }

  // The ABS/ASR pedal modulations run above, before the pedal smoothing --
  // see FUN_0044df70 / FUN_0044e120 there.

  // The wheel step's own inertia (0x00454500, recovered in 14.7): the listing asks
  // each wheel for 0.1/fVar16 instead of a constant, so a driven wheel is heavier
  // while a gear is engaged and every wheel falls back to gearbox+0xE8 when the
  // clutch is open or the ratio is zero. `wheel_inertia` stays in the coupling and
  // the differential above, which are the port's own model, not this listing's.
  double share_sum = 0;
  for (const auto &d : drive.wheels) share_sum += std::abs(d.weight);
  const double clutch_open =
      drive.faithful
          ? wheel_clutch_open_
          : 1 - std::clamp(drive.clutch * drive.asr_clutch, 0.0, 1.0);
  const double total_ratio = drive.ratios[std::clamp(gear, 0, 7)];
  for (size_t i = 0; i < wheels.size(); ++i) {
    const auto &wheel = wheels[i];
    const auto &cfg = drive.wheels[i];
    auto &s = state[i];
    if (!wheel.enabled)
      continue;
    const double ec = wheel_inverse_inertia(cfg.weight, share_sum, clutch_open,
                                            total_ratio, drive.inertia,
                                            drive.brake_inertia);
    // dt*50*EC, the common prefix of both recovered steps.
    const double brake_step = timestep * 50 * drive.brake_step_gain * ec;
    const double tyre_step = timestep * 50 * drive.tyre_step_gain * ec;
    const auto brake = [](double speed, double amount) {
      return std::copysign(std::max(0.0, std::abs(speed) - amount), speed);
    };
    const Vec3 heading = wheel_forward(body, cfg.steer, s.hub_rotation);
    const Vec3 forward =
        (heading - s.normal * heading.dot(s.normal)).normalized();
    const Vec3 side = forward.cross(s.normal);
    const Vec3 speed = body.point_velocity(s.patch);
    const double road = speed.dot(forward), lateral = speed.dot(side);

    // Consume one brake-impulse budget, before traction sees the driven spin.
    // Any unused budget can hold a locked wheel against the tyre reaction
    // below; applying the full brake a second time would double its torque.
    double remaining_brake = 0;
    const auto apply_brake = [&](double amount) {
      amount = std::max(0.0, amount);
      const double before = s.spin;
      s.spin = brake(s.spin, amount);
      remaining_brake += std::max(0.0, amount - std::abs(before - s.spin));
    };
    apply_brake(500 * timestep * cfg.bearing * drive.bearing_scale);
    // The recovered brake/drag decrement (0x455C5B..0x455C6F):
    //   dw = dt*50*EC*[(pedal + wheel[0xFC])*phys[0xD8]*wheel[0xD8]
    //                  + hbrake*phys[0xDC]*wheel[0xDC] + wheel[0xF0]]*[0x0060E024]
    // The moments are the wheel's own (cfg.brake is wheel+0xD8, cfg.handbrake is
    // wheel+0xDC) -- measuring 2284/3270/6540 on the MC settled that the cfg-level
    // brake_power must NOT scale them (14.8). The bearing term keeps its old form:
    // wheel[0xFC] is not recovered. wheel[0xF0] stays out as well, because the port
    // applies its own rolling resistance as the body force roll_drag below.
    apply_brake(brake_step * service_brake_ * drive.brake * cfg.brake);
    // The F12 strength scales the moment, not the pedal: 1 leaves the recovered
    // law untouched and the pedal still reads as the original's button.
    apply_brake(brake_step * drive.handbrake * cfg.handbrake *
                std::max(0.0, static_cast<double>(drive_tuning().handbrake)));

    if (!s.contact || s.force <= 0) {
      // Recovered 0x00454500: without contact the slip memory is cleared
      // (wheel 0x14C/0x150/0x158 zeroed) and the spin is left alone -- the
      // listing has no airborne decay. The decay we had here was invented and
      // is what finished off a wheel that lifts in a slide already pinned at
      // zero by its tyre; the driver's own comparison agrees: airborne wheels
      // with throttle keep spinning, lifted ones did not.
      s.slip_state = {};
    } else {
      s.slip_speed = s.spin * wheel.radius - road;
      s.lateral_speed = lateral;
      const double tilt =
          body.orientation.rotate(s.hub_rotation.rotate({0, -1, 0}))
              .dot(s.normal);
      // The recovered force scale is sqrt of the contact normal's components other
      // than the first one (0x00454500: sqrt(s[1]^2 + s[2]^2)) in the WHEEL's
      // frame, component 0 being the axle -- the force vector is assembled as
      // (lateral, 0, rolling), so axis 1 is up and the excluded axis is the axle.
      // The normal is unit, so everything but its axle part is the cosine of the
      // camber: 1.0 for a square wheel, falling to 0.5 when the wheel lies on its
      // side. This replaces the invented alignment = max(.25, tilt*tilt) factor.
      //
      // Measured 2026-11-29: this used to read normal.dot(side) with
      // side = forward x normal, which is perpendicular to the normal by
      // construction -- along_axle was identically 0 and the camber term was dead.
      const double along_axle = s.normal.dot(wheel_axle(body, cfg.steer, s.hub_rotation));
      const double tangent = std::sqrt(std::max(0.0, 1.0 - along_axle * along_axle));
      s.tangent = tangent;
      TireForces force{};
      if (wheel.metal_contact) {
        // Hub/disc contact is sliding metal, never the removed tyre's curve.
        const double slip = std::hypot(s.slip_speed, lateral);
        const double factor = slip > 1e-9 ? .4 * s.force / slip : 0;
        force = {s.slip_speed * factor, -lateral * factor};
      } else {
        // The low-speed path needs the wheel's own slip memory and the step:
        // 0x0043BBD0 is fed the timestep and stores its result in the wheel.
        force = tire_forces(s.force, tangent, s.spin * wheel.radius, road,
                            lateral, cfg.tyre, s.slip_state, timestep);
        // The recovered listing measures the filter velocities ground-relative
        // (v_surf = -wheel_speed) and applies its lateral output as stored, so
        // in this branch's car-relative axis the force must be negated to
        // oppose the measured lateral velocity. Measured, not tuned: without
        // the negation the bench auto-drive spins in place -- Flat grows along
        // the lateral velocity (lat=+6.3 m/s, Flat=+8891 N) and the accumulated
        // angle saturates and holds the full lateral peak in one direction.
        force.lateral = -force.lateral;
      }

      const Vec3 arm = s.patch - body.position;
      const double inverse_mass =
          1 / body.mass +
          forward.dot(
              body.inverse_world_inertia(arm.cross(forward)).cross(arm));
      // The same derivative the servo uses: d(wheel rotation)/d(force).
      const double inverse_spin =
          wheel.radius * wheel.radius * 50 * drive.tyre_step_gain * ec;
      const double limit =
          std::abs(s.slip_speed) / (timestep * (inverse_mass + inverse_spin));
      force.longitudinal = std::clamp(force.longitudinal, -limit, limit);
      s.tire_force = force;
      // The recovered rolling-resistance force (0x00454500, line of local_20):
      // s * v_roll * wheel[0x21c] * 0.01 - F_long applied to the body through
      // the wheel frame, where wheel[0x21c] = surface[0x54] + wheel[0x74]
      // (setRollRes). The tyre's own F_long already enters through
      // force.longitudinal above, so only the s*v*0x21c*0.01 part is new here;
      // it stays out of the slip clamp and the wheel servo on purpose -- in
      // the listing the wheel spin is dragged by bearing/brakes, not by this
      // force. The surface adder surface[0x54] stays 0 until a surface layer
      // exists (the map material loader FUN_00546e10 zero-inits it and has no
      // rolling-resistance property). Sign measured: the term must oppose the
      // road speed, the listing's frame-axis reading is ambiguous in Ghidra's
      // output (both readings of the rolling axis flip the sign).
      const double roll_drag =
          tire_scale(s.force, tangent, cfg.tyre) * road * cfg.rolling_resistance *
          0.01;
      body.add_force(forward * (force.longitudinal - roll_drag) +
                         side * force.lateral,
                     s.patch);
      // The recovered tyre response (0x4563B6..0x4563CB):
      //   dw = dt*50*EC*F_long*wheel[0x60]*[0x0060E020]
      // and nothing else. The invented spin damper (spin*0.15) and the wheel-side
      // rolling torque that used to ride here are gone: the listing's only rolling
      // term is the body force above, and the wheel is dragged by the tyre, the
      // brakes and the bearing, never by a damping of its own.
      s.spin -= force.longitudinal * wheel.radius * tyre_step;
      if (slide_trace() && std::abs(cfg.weight) > 1e-3)
        std::fprintf(stderr,
                     "[slide] w%zu spin=%7.2f road=%7.2f slip=%7.2f lat=%7.2f "
                     "Flong=%8.1f Flat=%8.1f load=%7.0f rpm=%5.0f shaft=%7.1f "
                     "gap=%7.1f gear=%d ang=%6.3f mslip=%6.3f\n",
                     i, s.spin, (s.spin * wheel.radius - s.slip_speed), s.slip_speed,
                     s.lateral_speed, force.longitudinal, force.lateral, s.force,
                     engine_omega * rpm_per_rad, s.spin * 12.9,
                     engine_omega - s.spin * 12.9, gear,
                     s.slip_state.angle, s.slip_state.slip);
      // Tried twice and reverted twice: letting the driveline own a driven wheel's speed
      // outright, with the tyre acting through force alone. It does not fix the wheel
      // stopping in a slide, and it removes torque sensitivity -- 200 against 600 Nm
      // reach 80-160 km/h in 3.26 and 3.11 s, because the wheel is then always at engine
      // speed and the car is always traction limited. Measured with the recovered
      // J_engine too, so it is not an artefact of the old inertia.
    }
    const double before_hold = s.spin;
    s.spin = brake(s.spin, remaining_brake);
    const double unspent = std::max(0.0, remaining_brake - std::abs(before_hold - s.spin));
    // Unspent budget back to an impulse through the same step the brake used:
    // dw = dt*50*K*EC*T  =>  T*dt = dw/(50*K*EC), impulse = T*dt/radius.
    s.holding_impulse =
        unspent / std::max(.01, 50 * drive.brake_step_gain * ec * wheel.radius);
    s.rotation =
        std::remainder(s.rotation + s.spin * timestep, 6.283185307179586);
  }
}

void VehicleDynamics::step(const Terrain &terrain) {
  apply_aero();
  double ceiling = body.position.y;
  for (Vec3 p : body.corners())
    ceiling = std::max(ceiling, p.y);
  std::array<RestContact, 12> rest_contacts{};
  size_t rest_count = 0;
  std::array<FixedSupport, 12> fixed_contacts{};
  size_t fixed_count = 0;
  for (size_t i = 0; i < std::min(body_support_count, body_supports.size()); ++i) {
    const Vec3 point = body_point(body, body_supports[i]);
    GroundHit hit;
    if (!terrain(point.x, point.z, ceiling, hit)) continue;
    const Vec3 normal = hit.normal.normalized();
    if (normal.y <= .05) continue;
    const double depth = (hit.height - point.y) * normal.y;
    if (depth < 0) continue;
    const Vec3 surface = point + normal * depth;
    fixed_contacts[fixed_count++] = fixed_support(body, surface, normal, depth, true, timestep);
  }
  for (size_t i = 0; i < wheels.size(); ++i) {
    const auto &w = wheels[i];
    auto &s = state[i];
    s.contact = false;
    s.force = 0;
    s.slip_speed = s.lateral_speed = 0;
    s.holding_impulse = 0;
    s.tire_force = {};
    if (!s.seeded)
      s.length = w.maximum;
    if (!w.enabled)
      continue;
    if (w.rigid_contact) {
      s.length = w.minimum;
      s.speed = 0;
      s.seeded = true;
      const auto pose = wheel_pose(w, s.length);
      const Vec3 hub = body_point(body, pose.hub_origin);
      GroundHit hit;
      if (terrain(hub.x, hub.z, ceiling, hit)) {
        s.normal = hit.normal.normalized();
        s.patch = hub - s.normal * w.radius;
        const double depth = w.radius -
            (hub - Vec3{hub.x, hit.height, hub.z}).dot(s.normal);
        s.contact = depth >= 0;
        if (s.contact) s.patch += s.normal * depth;
        const double speed = body.point_velocity(s.patch).dot(s.normal);
        const double damper = speed >= 0 ?
            (w.rebound_damping > 0 ? w.rebound_damping : .7 * w.bump_damping) : w.bump_damping;
        s.hub_rotation = pose.hub_rotation;
        if (s.contact)
          fixed_contacts[fixed_count++] = fixed_support(body, s.patch, s.normal,
              depth, w.metal_contact, timestep, static_cast<int>(i), damper);
      }
      continue;
    }
    if (w.linked()) {
      solve_linked(body, w, s, terrain, ceiling, timestep);
      continue;
    }
    s.length = w.maximum;
    Vec3 down = body.orientation.rotate(w.down.normalized());
    Vec3 hard =
        body.position +
        body.orientation.rotate(
            w.center - w.down.normalized() * w.anchor_length() - body.center);
    GroundHit hit;
    if (down.y >= -.05 ||
        !terrain(hard.x, hard.z, std::max(ceiling, hard.y), hit))
      continue;
    const double length = (hard.y - hit.height - w.radius) / -down.y;
    s.length = std::clamp(length, w.minimum, w.maximum);
    s.hub_rotation = w.hub_alignment;
    s.contact = length < w.maximum;
    if (!s.contact)
      continue;
    s.normal = hit.normal.normalized();
    double force = suspension_force(
        length, body.point_velocity(hard).dot(s.normal), w.rest, w.maximum,
        w.spring, w.bump_damping, w.rebound_damping);
    force += std::max(0.0, w.minimum - length) * w.spring * 8;
    s.force = std::clamp(force, 0.0, body.mass * 9.81 * 3);
    body.add_force(s.normal * s.force, hard);

    const Vec3 point =
        body_point(body, wheel_pose(w, s.length).hub_origin) - s.normal * w.radius;
    s.patch = point;
  }

  solve_fixed_support(body, fixed_contacts, fixed_count, timestep);
  for (size_t i = 0; i < fixed_count; ++i) {
    const auto& c = fixed_contacts[i];
    const double force = c.impulse / timestep;
    body.add_force(c.normal * force, c.point);
    if (c.wheel >= 0) state[static_cast<size_t>(c.wheel)].force = force;
    else if (force > 0)
      rest_contacts[rest_count++] = {c.point, c.normal, {}, .4 * c.impulse, .4 * c.impulse, {}};
  }

  if (drive.torque && drive.inertia > 0)
    step_drive();
  if (drive.native_traffic) {
    for (size_t i = 0; i < wheels.size(); ++i) {
      auto &s = state[i];
      if (!wheels[i].enabled || !s.contact)
        continue;
      auto forward = wheel_forward(body, drive.wheels[i].steer, s.hub_rotation);
      forward = (forward - s.normal * forward.dot(s.normal)).normalized();
      auto side = forward.cross(s.normal).normalized(),
           velocity = body.point_velocity(s.patch);
      double speed = velocity.dot(forward),
             sign = speed > 0 ? 1 : speed < 0 ? -1 : 0;
      double braking =
          sign * std::min(drive.brake * 8, std::abs(speed) / timestep);
      double hold = drive.brake > .5 ? 9.81 * forward.y : 0;
      double longitudinal =
          (drive.throttle * 5 - braking - speed * .05 + hold) * body.mass / 4;
      double lateral = -velocity.dot(side) * body.mass / 4 * 8;
      auto force = forward * longitudinal + side * lateral;
      double limit = s.force * .9;
      if (force.length() > limit)
        force = force.normalized() * limit;
      body.add_force(force, s.patch);
      s.rotation += speed / wheels[i].radius * timestep;
      s.spin = speed / wheels[i].radius;
    }
  }
  // Bars also work without an engine/drive, and never feed back into the tyre
  // forces already evaluated above. Always apply both sides as one couple.
  solve_anti_roll(body, wheels, state, anti_roll);
  const bool parked = !drive.torque && !drive.native_traffic;
  for (size_t i = 0; i < wheels.size(); ++i) {
    const auto& s = state[i];
    const auto& cfg = drive.wheels[i];
    if (!wheels[i].enabled || !s.contact || !(s.force > 0)) continue;
    // The F12 strength counts here too: at 0 the handbrake is genuinely absent,
    // so a parked car must roll instead of being held by a disabled brake.
    const bool handbraked = drive.handbrake > .05 && cfg.handbrake > 0 &&
                            drive_tuning().handbrake > 0;
    const bool braked = (drive.brake > .05 && cfg.brake > 0) || handbraked ||
                        (drive.native_traffic && drive.brake > .5);
    const Vec3 v = body.point_velocity(s.patch);
    const double speed = (v - s.normal * v.dot(s.normal)).length();
    // The standstill hold must survive the settle bounce: at 0.05 m/s a
    // parked-on-slope car drifts past the gate once and the hold never
    // re-engages, so it creeps at the moving-path balance (~1 m/s). The
    // handbrake hold is therefore allowed well past rest (still far below
    // any drift speed); service brakes keep the tight gate so braking to a
    // stop is not finished by a standstill solver.
    const double hold_speed = handbraked ? 0.5 : .05;
    if (!parked && !(braked && speed < hold_speed &&
                     std::abs(s.spin * wheels[i].radius) < .05))
      continue;
    const double mu = wheels[i].metal_contact ? .4 : .8;
    TireForces caps{mu * s.force, mu * s.force};
    if (!wheels[i].metal_contact && !drive.native_traffic) {
      // The recovered scale replaces both the invented mu=0.8 stand-in and the
      // alignment = max(.25, tilt*tilt) load multiplier: the cap is the force
      // law's own peak with s = (tangent+1)*0.5*load_curve, where tangent is
      // the contact normal's tangential magnitude against the axle -- the same
      // quantity the moving path computes.
      // The same axle the moving path uses: the normal's component along the
      // wheel's own axle, not a vector derived from the normal itself.
      const double along_axle =
          s.normal.dot(wheel_axle(body, cfg.steer, s.hub_rotation));
      const double tangent =
          std::sqrt(std::max(0.0, 1.0 - along_axle * along_axle));
      caps = tire_friction_limits(s.force, tangent, cfg.tyre);
    }
    const double used = std::hypot(
        caps.longitudinal > 0 ? s.tire_force.longitudinal / caps.longitudinal : 0,
        caps.lateral > 0 ? s.tire_force.lateral / caps.lateral : 0);
    const bool no_capacity = (caps.longitudinal <= 0 && s.tire_force.longitudinal != 0) ||
                             (caps.lateral <= 0 && s.tire_force.lateral != 0);
    const double remaining = no_capacity ? 0 : std::max(0.0, 1 - used);
    double limit_x = caps.longitudinal * remaining * timestep;
    const double limit_y = caps.lateral * remaining * timestep;
    if (!parked && !drive.native_traffic) limit_x = std::min(limit_x, s.holding_impulse);
    Vec3 heading = wheel_forward(body, cfg.steer, s.hub_rotation);
    heading = (heading - s.normal * heading.dot(s.normal)).normalized();
    Vec3 surface = s.patch;
    GroundHit hit;
    if (terrain(surface.x, surface.z, ceiling, hit))
      surface += s.normal * ((hit.height - surface.y) * s.normal.y);
    rest_contacts[rest_count++] = {surface, s.normal, heading, limit_x, limit_y, {}};
  }
  body.integrate_forces(timestep);
  solve_rest_contacts(body, rest_contacts, rest_count);
  body.integrate_pose(timestep);
  for (int iteration = 0; iteration < 4; ++iteration) {
    double depth = 0;
    Vec3 correction;
    auto solve = [&](Vec3 point, double restitution, bool chassis = false,
                     bool tyre = false, double allowance = 0) {
      GroundHit ground;
      if (!terrain(point.x, point.z, ceiling, ground))
        return false;
      double overlap = ground.height - point.y - allowance;
      if (overlap <= 0)
        return false;
      if (chassis && iteration == 0) {
        const double speed = -body.point_velocity(point).dot(ground.normal);
        if (speed > ground_impact.speed)
          ground_impact = {point, ground.normal, speed};
      }
      const double friction = tyre && drive.torque && drive.inertia > 0 ? 0 : .6;
      body.contact(point, ground.normal, restitution, friction);
      if (overlap > depth) {
        depth = overlap;
        correction = ground.normal.normalized();
      }
      return true;
    };
    // An AABB corner is not necessarily material on the underside of a car.
    // Prefer installed-mesh low points; keep the old box fallback for callers
    // without them, and for the roof/side after a rollover.
    if (!body_support_count || body.orientation.rotate({0, 1, 0}).y < .5)
      for (Vec3 point : body.corners()) solve(point, .12, true);
    for (size_t i = 0; i < std::min(body_support_count, body_supports.size()); ++i)
      solve(body_point(body, body_supports[i]), 0, true, false, .03);
    for (size_t i = 0; i < wheels.size(); ++i) {
      const auto &wheel = wheels[i];
      if (!wheel.enabled)
        continue;
      const bool linked = wheel.linked() && !wheel.rigid_contact;
      if (linked && state[i].length > wheel.minimum + .002)
        continue;
      const auto down = wheel.down.normalized();
      if (!wheel.rigid_contact && body.orientation.rotate(down).y >= -.05)
        continue;
      const auto local = wheel_pose(wheel, wheel.minimum).hub_origin;
      const auto point = body.position +
                         body.orientation.rotate(local - body.center) -
                         Vec3{0, wheel.radius, 0};
      // Fixed-travel corners carry a same-step compliant load above. This
      // is only a deep-penetration safety stop, not the normal support path.
      if (solve(point, 0, false, true, wheel.rigid_contact ? .03 : 0) && wheel.rigid_contact)
        state[i].contact = true;
    }
    if (depth > .001)
      body.position += correction * ((depth - .001) * .6);
    else
      break;
  }
  sync_wheels();
}

void VehicleDynamics::sync_wheels() {
  for (size_t i = 0; i < wheels.size(); ++i) {
    const auto &w = wheels[i];
    auto &s = state[i];
    const auto pose = wheel_pose(w, s.length);
    s.hub = pose.hub_origin;
    s.hub_rotation = pose.hub_rotation;
    s.center = body_point(body, s.hub);
  }
}

unsigned VehicleDynamics::advance(double seconds, const Terrain &terrain) {
  if (!std::isfinite(seconds) || seconds < 0)
    throw std::invalid_argument("invalid elapsed physics time");
  ground_impact = {};
  accumulator_ += seconds;
  unsigned count = 0;
  constexpr unsigned max_steps =
      (frequency + update_frequency - 1) / update_frequency;
  while (accumulator_ + timestep * 1e-10 >= timestep && count < max_steps) {
    step(terrain);
    accumulator_ = std::max(0.0, accumulator_ - timestep);
    ++ticks;
    ++count;
  }
  return count;
}

void VehicleDynamics::reset_clock() {
  accumulator_ = 0;
  ticks = 0;
}

void VehicleDynamics::reset_drive() {
  engine_omega = previous_rpm_ = shift_time_ = 0;
  starter_remaining_ = service_brake_ = 0;
  gear = previous_shift_ = 0;
  launch_active_ = false;
  launch_factor_ = 1;
  accelerator_down_ = engine_running = forced_reverse_ = false;
}

void VehicleDynamics::release_wheels() {
  for (auto &wheel : state)
    wheel = {};
}

uint64_t VehicleDynamics::backlog() const {
  return static_cast<uint64_t>((accumulator_ + timestep * 1e-10) / timestep);
}

} // namespace slrr::physics
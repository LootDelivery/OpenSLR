// Ambient city traffic: creation, lifecycle and movement along the map's road
// graph (U2-B). Ported from upstream PR #180 `src/resources/traffic_runtime.cpp`
// together with the recovered native bodies in
// `slrr-reverse/engine/Runtime/Resources/GroundRef_traffic.cpp`
// (addTrafficN @ 0x00484050, addTrafficP @ 0x00484420,
// addTrafficCar @ 0x00484730, remTrafficCar @ 0x00484A90,
// notifyTrafficCar @ 0x00484AF0, delTraffic @ 0x00484B50,
// setTrafficCarBehaviour @ 0x00484BD0).
//
// Deliberately not ported, because this block must not grow into U2-C/D/F:
//   * signal phases that stop cars (U2-C). The crossing expiry map and the
//     shared clock exist, so the consumer can be added without reshaping the
//     runtime, but nothing here reads `RoadRoutes::signal` yet;
//   * race bot commands and physics driving of traffic (U2-D). Every car this
//     module owns is driven through external motion, never through the drive
//     model, so it cannot fight Race Brain or the vehicle dynamics;
//   * `native_vehicle` / `drive.native_traffic` and the physics assembly of
//     script-less cars (U2-F).
//
// Streaming policy (which car appears where and how many stay resident) is a
// host model, as in upstream: the shipped natives spawn `n` cars at once, which
// is unbounded on a large map. The natives still return their stock values.
#include "slrr/race_runtime.hpp"

#include "slrr/gameref.hpp"
#include "slrr/host_game_options.hpp"
#include "slrr/jvm.hpp"
#include "slrr/object.hpp"
#include "slrr/part_runtime.hpp"
#include "slrr/rpk_index.hpp"
#include "slrr/frame_profiler.hpp"
#include "slrr/scene_instances.hpp"
#include "slrr/section_machine.hpp"
#include "slrr/traffic_streaming.hpp"

#include <algorithm>
#include <cmath>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <limits>
#include <string>
#include <unordered_map>
#include <unordered_set>

namespace slrr {
namespace {
// One entry of the ground's traffic template list (addTrafficN).
struct Model {
  uint32_t type = 0;
  float weight = 0;
  float begin = 0, end = 0, wheelbase = 0;
};
// A positioned request (addTrafficP): keep `remaining` cars around `position`.
struct Seed {
  Model model;
  RoadPoint position{};
  int remaining = 0;
};
struct Population {
  uint64_t incarnation = 0;
  std::vector<Model> models;
  std::vector<Seed> seeds;
  std::unordered_set<uint32_t> blocked;
  std::unordered_map<uint32_t, double> crossings;
};
// One live traffic entry. `owned` cars were created here and are destroyed when
// they leave; other cars belong to a script and are only associated with a road
// position.
struct Registration {
  int32_t id = 0;
  void *ground = nullptr;
  uint64_t ground_incarnation = 0;
  Object *car = nullptr;
  uint64_t car_incarnation = 0;
  Object *target = nullptr;  // event receiver, resolved on first use
  uint64_t target_incarnation = 0;
  bool owned = false, visible = false, notify = false, notified = false,
       unplaced = true;
  int mode = 0;
  size_t edge = 0;
  // Edge the car came from, so the pose can fillet a junction instead of
  // snapping to the new lane's chord direction.
  size_t entry = 0;
  // When this module created the car. A car younger than the minimum lifetime
  // is not retired for being far away, so a spot cannot be filled and emptied
  // in a loop while the player is moving.
  double born = 0;
  float along = 0, speed = 0, cruise = 0;
  // Last published frame, for the discontinuity diagnostic.
  bool posed = false;
  size_t posed_edge = 0;
  float posed_along = 0, posed_road_y = 0, posed_body_y = 0, posed_yaw = 0;
};
struct Event {
  Object *target = nullptr;
  uint64_t incarnation = 0;
  Object *ground = nullptr;
  uint64_t ground_incarnation = 0;
  std::string text;
};
std::unordered_map<void *, Population> populations;
std::unordered_map<int32_t, Registration> registrations;
std::vector<Event> events;
SectionMachine *sections = nullptr;
RpkIndex *resources = nullptr;
int32_t next_id = 1;
// Ambient traffic belongs to the city, not to a race scene: while a race/track
// section is active the runtime neither drives nor refills, and the cars it
// created are retired. Script-owned cars (police scouts, race bots) are a
// different mechanism and are never touched here.
bool ambient_enabled = true;
uint32_t random_state = 0x8f12345;
double clock_seconds = 0;
size_t spawned_total = 0, dropped_total = 0;

float random() {
  random_state ^= random_state << 13;
  random_state ^= random_state >> 17;
  random_state ^= random_state << 5;
  return float(random_state & 0xffffff) / 16777216.f;
}
bool tracing() {
  static const bool on = std::getenv("SLRR_TRAFFIC_LOG") != nullptr;
  return on;
}
// Discontinuity diagnostic: reports a per-frame yaw or height jump with the data
// needed to locate it (edge, arc position, world pose, road surface, body
// height, delta time, edge change). Off unless SLRR_TRAFFIC_DIAG is set.
bool diagnosing() {
  static const bool on = std::getenv("SLRR_TRAFFIC_DIAG") != nullptr;
  return on;
}
struct Tracked {
  bool valid = false;
  size_t edge = 0;
  float along = 0, road_y = 0, body_y = 0, yaw = 0;
  RoadPoint world{};
};
void report_discontinuity(const Registration &r, const Tracked &was,
                          const RoadPoint &now, float seconds) {
  const float dy = r.posed_body_y - was.body_y;
  const float dyaw = std::remainder(r.posed_yaw - was.yaw, 6.2831853f);
  // A 0.4 m step in one frame or a 7 degree heading step is a discontinuity at
  // walking pace; below that the filter must stay silent.
  if (std::fabs(dy) < .4f && std::fabs(dyaw) < .12f)
    return;
  std::fprintf(stderr,
               "[traffic-diag] id=%d edge=%zu->%zu along=%.1f->%.1f "
               "world=(%.1f,%.1f,%.1f)->(%.1f,%.1f,%.1f) road_y=%.2f->%.2f "
               "body_y=%.2f->%.2f yaw=%.3f->%.3f dy=%.2f dyaw=%.3f dt=%.4f "
               "edge_change=%d\n",
               r.id, was.edge, r.edge, was.along, r.posed_along, was.world[0],
               was.world[1], was.world[2], now[0], now[1], now[2], was.road_y,
               r.posed_road_y, was.body_y, r.posed_body_y, was.yaw, r.posed_yaw,
               dy, dyaw, seconds, was.edge != r.edge ? 1 : 0);
}
// ---------------------------------------------------------------------------
// Stage profile (SLRR_TRAFFIC_PROFILE=1).
//
// The frame profiler's `traffic` zone measures the whole tick; this says which
// part of it costs what. Stages are timed with the same monotonic clock, the
// samples are kept so the summary reports p50/p95/p99 and not just a mean, and
// a frame that cost more than a rendering budget prints its own breakdown.
// Nothing is printed otherwise: one summary line every five seconds of
// simulated time and one table at exit.
//
// Cost when off: one branch per stage, no clock reads, no allocation.
enum ProfStage {
  kProfTick, kProfRetire, kProfDrive, kProfPose, kProfFloor, kProfPark,
  kProfWheels, kProfSearch, kProfCreate, kProfEvents, kProfProject,
  kProfCount
};
const char *const kProfNames[kProfCount] = {
    "tick", "retire", "drive", "pose", "floor", "park", "wheels", "search",
    "create", "events", "project"};
struct ProfSamples {
  std::vector<float> ms;
  uint32_t stride = 1;
  double sum = 0;
  float max = 0;
  uint64_t count = 0;
};
struct ProfState {
  bool on = false, started = false, reported = false;
  ProfSamples st[kProfCount];
  int64_t frame[kProfCount]{};
  std::vector<float> window_tick;
  double window_at = 0;
  uint64_t frames = 0, spawns = 0, despawns = 0, attempts = 0, failures = 0;
  uint64_t creates = 0, create_failures = 0;
  int active = 0, active_max = 0, cap = 0;
};
void prof_summary();
ProfState &prof() {
  static ProfState p;
  if (!p.started) {
    p.started = true;
    p.on = std::getenv("SLRR_TRAFFIC_PROFILE") != nullptr;
    if (p.on) std::atexit(prof_summary);
  }
  return p;
}
bool profiling() { return prof().on; }
int64_t prof_now() { return profiling() ? perf::now_ticks() : 0; }
void prof_add(ProfStage stage, int64_t start) {
  if (start) prof().frame[stage] += perf::now_ticks() - start;
}
void prof_count(uint64_t &counter) {
  if (profiling()) ++counter;
}
void prof_push(ProfSamples &s, float ms) {
  s.sum += ms;
  if (ms > s.max) s.max = ms;
  if (s.ms.size() >= 20000) {
    // Bounded memory without losing the shape of the distribution: keep every
    // second sample and halve the sampling rate from here on.
    std::vector<float> kept;
    kept.reserve(s.ms.size() / 2);
    for (size_t i = 0; i < s.ms.size(); i += 2) kept.push_back(s.ms[i]);
    s.ms.swap(kept);
    s.stride *= 2;
  }
  if (s.stride <= 1 || (s.count % s.stride) == 0) s.ms.push_back(ms);
  ++s.count;
}
float prof_quantile(std::vector<float> v, double q) {
  if (v.empty()) return 0;
  const size_t k = std::min(v.size() - 1, size_t(q * double(v.size() - 1)));
  std::nth_element(v.begin(), v.begin() + k, v.end());
  return v[k];
}
void prof_report_window() {
  auto &p = prof();
  if (p.window_tick.empty()) return;
  const float worst = *std::max_element(p.window_tick.begin(), p.window_tick.end());
  double sum = 0;
  for (float v : p.window_tick) sum += v;
  std::fprintf(stderr,
               "[tprof] t=%.1f frames=%llu active=%d/%d spawn=%llu drop=%llu "
               "tick avg=%.3f p95=%.3f max=%.3f ms\n",
               p.window_at, (unsigned long long)p.frames, p.active, p.cap,
               (unsigned long long)p.spawns, (unsigned long long)p.despawns,
               sum / double(p.window_tick.size()),
               prof_quantile(p.window_tick, .95), worst);
  p.window_tick.clear();
}
// Closes one tick: folds the per-frame counters into the samples, prints an
// abnormal frame with its breakdown, and closes the five second window.
void prof_flush_frame(double sim_seconds) {
  auto &p = prof();
  int64_t frame[kProfCount];
  for (int i = 0; i < kProfCount; ++i) {
    frame[i] = p.frame[i];
    p.frame[i] = 0;
    prof_push(p.st[i], float(double(frame[i]) * 1e-6));
  }
  const double tick_ms = double(frame[kProfTick]) * 1e-6;
  ++p.frames;
  p.window_tick.push_back(float(tick_ms));
  if (p.active > p.active_max) p.active_max = p.active;
  if (tick_ms > 4.) {
    std::fprintf(stderr, "[tslow] t=%.1f tick=%.2f ms active=%d |", sim_seconds,
                 tick_ms, p.active);
    for (int i = 1; i < kProfCount; ++i) {
      const double ms = double(frame[i]) * 1e-6;
      if (ms > .05) std::fprintf(stderr, " %s=%.2f", kProfNames[i], ms);
    }
    std::fprintf(stderr, "\n");
  }
  if (sim_seconds - p.window_at >= 5.) {
    prof_report_window();
    p.window_at = sim_seconds;
  }
}
void prof_summary() {
  auto &p = prof();
  if (!p.on || p.reported || p.frames == 0) return;
  p.reported = true;
  std::fprintf(stderr,
               "[tprof] summary frames=%llu spawn=%llu despawn=%llu create_ok=%llu "
               "create_fail=%llu place=%llu place_fail=%llu active_max=%d cap=%d\n",
               (unsigned long long)p.frames, (unsigned long long)p.spawns,
               (unsigned long long)p.despawns, (unsigned long long)p.creates,
               (unsigned long long)p.create_failures, (unsigned long long)p.attempts,
               (unsigned long long)p.failures, p.active_max, p.cap);
  std::fprintf(stderr,
               "[tprof] stage                            n     avg     p50     p95"
               "     p99     max   total_ms\n");
  for (int i = 0; i < kProfCount; ++i) {
    auto &s = p.st[i];
    if (!s.count) continue;
    std::fprintf(stderr, "[tprof] %-24s %8llu %7.3f %7.3f %7.3f %7.3f %7.3f %10.2f\n",
                 kProfNames[i], (unsigned long long)s.count, s.sum / double(s.count),
                 prof_quantile(s.ms, .5), prof_quantile(s.ms, .95),
                 prof_quantile(s.ms, .99), s.max, s.sum);
  }
  std::fflush(stderr);
}
// Closes the tick on every return path, including the early ones.
struct ProfTickGuard {
  int64_t start;
  double sim_seconds;
  ProfTickGuard(double seconds) : start(prof_now()), sim_seconds(seconds) {}
  ~ProfTickGuard() {
    if (!start) return;
    prof().frame[kProfTick] = perf::now_ticks() - start;
    prof_flush_frame(sim_seconds);
  }
};

float number(const std::vector<Value> &args, size_t i, float fallback = 0) {
  if (i >= args.size())
    return fallback;
  return args[i].tag == ValueTag::Float ? args[i].v.f
         : args[i].tag == ValueTag::Int ? float(args[i].v.i)
                                        : fallback;
}
Object *object(const std::vector<Value> &args, size_t i) {
  return i < args.size() && args[i].tag == ValueTag::Obj ? args[i].v.o : nullptr;
}
std::optional<RoadPoint> point(Object *o) {
  if (!o)
    return {};
  RoadPoint p;
  for (int i = 0; i < 3; ++i) {
    auto it = o->fields.find(i == 0 ? "x" : i == 1 ? "y" : "z");
    if (it == o->fields.end())
      return {};
    p[i] = it->second.tag == ValueTag::Float
               ? it->second.v.f
               : it->second.tag == ValueTag::Int
                     ? float(it->second.v.i)
                     : std::numeric_limits<float>::quiet_NaN();
    if (!std::isfinite(p[i]))
      return {};
  }
  return p;
}
void *canonical(void *key) {
  auto *n = gameref_get(key);
  return n ? n->key : nullptr;
}
uint32_t model_type(Object *o) {
  if (!o)
    return 0;
  if (auto *n = gameref_get(o); n && n->type_id)
    return uint32_t(n->type_id);
  auto it = o->fields.find("id");
  return it != o->fields.end() && it->second.tag == ValueTag::Int
             ? uint32_t(it->second.v.i)
             : 0;
}
// The population of one ground, cleared when the ground object is re-created on
// the same address (`incarnation` comes from U2-A).
Population *population(void *key) {
  auto *n = gameref_get(key);
  if (!n)
    return nullptr;
  auto &p = populations[n->key];
  if (p.incarnation != n->incarnation) {
    p = {};
    p.incarnation = n->incarnation;
  }
  return &p;
}
RoadPoint position(Object *car) {
  PartMatrix frame;
  if (!scene_instances_script_matrix(car, frame.data()))
    return {};
  return {frame[12], frame[13], frame[14]};
}
bool belongs(void *key, void *world) {
  for (int i = 0; key && i < 64; ++i) {
    auto *n = gameref_get(key);
    if (!n)
      return false;
    if (n->key == world)
      return true;
    key = n->parent;
  }
  return false;
}
void queue(Registration &r, bool add) {
  if (add == r.notified)
    return;
  if (add && !r.notify)
    return;
  if (!gameref_get(r.car))
    return;
  if (!r.target && sections) {
    r.target = sections->actual();
    auto *target = gameref_get(r.target);
    r.target_incarnation = target ? target->incarnation : 0;
  }
  if (r.target)
    events.push_back({r.target, r.target_incarnation,
                      static_cast<Object *>(r.ground), r.ground_incarnation,
                      (add ? "car_add " : "car_rem ") +
                          std::to_string(gameref_make_live(r.car)) +
                          (add ? " " + std::to_string(r.id) : "")});
  r.notified = add;
}
// Drop one entry, destroying the car only when this module created it.
void remove(int32_t id) {
  auto found = registrations.find(id);
  if (found == registrations.end())
    return;
  auto r = found->second;
  queue(r, false);
  registrations.erase(found);
  if (auto *n = gameref_get(r.car); n && n->incarnation == r.car_incarnation) {
    if (r.owned) {
      part_set_external_motion(r.car, false);
      part_destroy(r.car);
      ++dropped_total;
    }
  }
}
bool blocked(const Population &p, const RoadRoutes::Edge &e) {
  for (auto id : e.roads)
    if (p.blocked.contains(id))
      return true;
  auto cross = p.crossings.find(e.to);
  return cross != p.crossings.end() && cross->second > clock_seconds;
}
// Choose a destination branch first, then one of its lanes, so a wide outgoing
// street is not more likely just because it has more lanes.
size_t choose_transition(const std::vector<size_t> &choices,
                         const RoadRoutes &graph) {
  std::vector<uint32_t> branches;
  for (auto i : choices) {
    auto branch = graph.traffic_edges()[i].branch;
    if (std::find(branches.begin(), branches.end(), branch) == branches.end())
      branches.push_back(branch);
  }
  auto branch = branches[std::min<size_t>(size_t(random() * branches.size()),
                                          branches.size() - 1)];
  std::vector<size_t> lanes;
  for (auto i : choices)
    if (graph.traffic_edges()[i].branch == branch)
      lanes.push_back(i);
  return lanes[std::min<size_t>(size_t(random() * lanes.size()),
                                lanes.size() - 1)];
}
// Unit XZ direction of the first (or last) non-degenerate step of an edge.
bool edge_tangent(const RoadRoutes::Edge &e, bool start, RoadPoint &out) {
  if (e.points.size() < 2)
    return false;
  const size_t n = e.points.size();
  for (size_t i = 1; i < n; ++i) {
    const auto &a = start ? e.points[i - 1] : e.points[n - 1 - i];
    const auto &b = start ? e.points[i] : e.points[n - i];
    RoadPoint d{b[0] - a[0], 0, b[2] - a[2]};
    float l = std::sqrt(d[0] * d[0] + d[2] * d[2]);
    if (l > .05f) {
      out = {d[0] / l, 0, d[2] / l};
      return true;
    }
  }
  return false;
}
// The world pose of a car that stands `along` metres into `edge`. `entry` is the
// edge it came from (the pose then fillets the junction), `npos` anywhere else.
//
// The map stores a junction as a straight chord between the incoming and the
// outgoing lane ends, so a chord's own direction is tens of degrees off the lane
// at both of its ends: following it snapped the car's heading twice per turn.
// On a crossing edge the pose therefore follows a cubic Hermite whose end
// tangents are the incoming and the outgoing lane directions, which makes both
// the position and the heading continuous through the junction. On a lane the
// heading is the tangent over a +-1.2 m window, so a 1.5 m geometry sample does
// not step the yaw either.
TrafficPose traffic_pose(const RoadRoutes &graph, size_t edge, float along,
                         size_t entry) {
  TrafficPose pose;
  const auto &edges = graph.traffic_edges();
  if (edges.empty())
    return pose;
  if (edge >= edges.size())
    edge = edges.size() - 1;
  const auto &e = edges[edge];
  RoadRoute route;
  route.points = e.points;
  if (e.points.empty())
    return pose;
  along = std::clamp(along, 0.f, e.length);
  auto direction = [&](const RoadPoint &a, const RoadPoint &b) {
    RoadPoint d{b[0] - a[0], 0, b[2] - a[2]};
    float l = std::sqrt(d[0] * d[0] + d[2] * d[2]);
    if (l > 1e-4f) {
      d[0] /= l;
      d[2] /= l;
    }
    return d;
  };
  if (e.crossing && !e.next.empty() && e.length > .2f) {
    const RoadPoint p0 = e.points.front(), p1 = e.points.back();
    RoadPoint chord = direction(p0, p1);
    const float reach = std::sqrt((p1[0] - p0[0]) * (p1[0] - p0[0]) +
                                  (p1[2] - p0[2]) * (p1[2] - p0[2]));
    RoadPoint tin{chord[0], 0, chord[2]}, tout{chord[0], 0, chord[2]};
    if (entry < edges.size() && entry != edge)
      edge_tangent(edges[entry], false, tin);
    if (e.next[0] < edges.size())
      edge_tangent(edges[e.next[0]], true, tout);
    // Opposite tangents (a hairpin) would overshoot; shorten both.
    float scale = reach;
    if (tin[0] * tout[0] + tin[2] * tout[2] < 0)
      scale *= .5f;
    const float t = along / e.length;
    const float t2 = t * t, t3 = t2 * t;
    const float h00 = 2 * t3 - 3 * t2 + 1, h10 = t3 - 2 * t2 + t;
    const float h01 = -2 * t3 + 3 * t2, h11 = t3 - t2;
    pose.position[0] = h00 * p0[0] + h10 * scale * tin[0] + h01 * p1[0] +
                       h11 * scale * tout[0];
    pose.position[2] = h00 * p0[2] + h10 * scale * tin[2] + h01 * p1[2] +
                       h11 * scale * tout[2];
    pose.position[1] = p0[1] + (p1[1] - p0[1]) * t;
    const float d00 = 6 * t2 - 6 * t, d10 = 3 * t2 - 4 * t + 1;
    const float d01 = -6 * t2 + 6 * t, d11 = 3 * t2 - 2 * t;
    RoadPoint d{d00 * p0[0] + d10 * scale * tin[0] + d01 * p1[0] +
                    d11 * scale * tout[0],
                0,
                d00 * p0[2] + d10 * scale * tin[2] + d01 * p1[2] +
                    d11 * scale * tout[2]};
    if (std::fabs(d[0]) + std::fabs(d[2]) < 1e-5f)
      d = chord;
    pose.heading = std::atan2(-d[0], -d[2]);
    return pose;
  }
  pose.position = road_at(route, along);
  const float window = 1.2f;
  auto was = road_at(route, std::max(0.f, along - window));
  auto next = road_at(route, std::min(e.length, along + window));
  auto d = direction(was, next);
  if (d[0] == 0 && d[2] == 0)
    d = direction(road_at(route, std::max(0.f, along - .25f)),
                  road_at(route, std::min(e.length, along + .25f)));
  pose.heading = std::atan2(-d[0], -d[2]);
  return pose;
}
// Publish the pose for one car: the graph point is the tyre contact, so the
// chassis is parked onto it with its loaded springs. The caller then animates
// the wheels with the distance it advanced.
void publish(Registration &r, const RoadRoutes &graph, float at) {
  int64_t t = prof_now();
  auto pose = traffic_pose(graph, r.edge, at, r.entry);
  prof_add(kProfPose, t);
  auto p = pose.position;
  auto direction = RoadPoint{-std::sin(pose.heading), 0, -std::cos(pose.heading)};
  const float road_y = p[1];
  SceneFloorHit floor;
  // A bridge deck, an arch or a canopy four metres above the road is not the
  // road. The graph point is the lane centreline, so only a surface within a
  // metre of it may replace the height: without this clamp the probe (which
  // starts 4 m up) hit a deck and lifted the car for one frame.
  t = prof_now();
  const bool hit = scene_instances_sample_floor(p[0], p[2], p[1] + 4, r.car, floor);
  prof_add(kProfFloor, t);
  if (hit && std::fabs(floor.height - road_y) <= 1.2f) p[1] = floor.height;
  t = prof_now();
  gameref_set_matrix(r.car, p[0], p[1], p[2], pose.heading, 0, 0);
  part_park_chassis(r.car);
  prof_add(kProfPark, t);
  if (auto *n = gameref_get(r.car)) {
    for (int i = 0; i < 3; ++i)
      n->vel[i] = direction[i] * r.speed;
    r.posed_body_y = n->pos[1];
  }
  r.posed = true;
  r.posed_edge = r.edge;
  r.posed_along = at;
  r.posed_road_y = road_y;
  r.posed_yaw = gameref_get(r.car) ? gameref_get(r.car)->ypr[0] : 0;
}
// Streaming policy (U2-B.2). The old ring asked one question — how far is this
// point from the player — and answered with a band whose far end (240 m) sat
// inside the forward cone it forbade below 260 m, so the road ahead was never
// populated. The replacement measures the lane network the player can reach:
//
//   * the population target comes from the reachable lane length
//     (`stream_target_count`), not from a fixed twenty;
//   * a candidate is a lane position reachable by driving, not on a junction
//     chord, not over the lane's own capacity and `min_gap` from the cars
//     already standing on that lane;
//   * the only forward exclusion is the pop-in bubble, which grows with the
//     player's speed and collapses to `min_bubble` when standing still;
//   * spawn (260 m) and retire (330 m) radii differ and a car has a minimum
//     lifetime, so nothing is filled and emptied in a loop.
StreamRules stream_rules(float density) {
  StreamRules rules;
  const float d = std::isfinite(density) ? std::clamp(density, 0.f, 1.f) : 0.f;
  // The slider controls how tightly the streets around the player are packed,
  // not just how many cars exist somewhere. A car costs about thirteen draw
  // calls, so the ceiling drops from a hundred to eighty: the same or better
  // visible density, a fifth less geometry.
  rules.budget = int(std::lround(d * 80.f));
  rules.min_gap = 30.f - 14.f * d;
  rules.per_car_metres = 40.f - 20.f * d;
  return rules;
}
// A car this module did not create still occupies its lane, so the scheduler
// sees it; only the budget and the retirement rules care about the owner.
constexpr float kRetireScripted = 400.f;
constexpr double kMinLifetime = 6.;
// An edge that was just emptied stays empty for a moment, so a car is not
// destroyed and immediately re-created in the same place as the player passes.
constexpr double kEdgeCooldown = 4.;
std::unordered_map<size_t, double> edge_cooldown;
// Lanes the player can drive to: the player's own lane and its opposite
// direction (so oncoming traffic on the same street counts), then everything
// reachable forward through junctions. A lane across a wall is not reachable
// even when it is a few metres away.
std::unordered_set<size_t> reachable(const RoadRoutes &graph, size_t from) {
  const auto &edges = graph.traffic_edges();
  std::unordered_set<size_t> seen;
  std::vector<size_t> queue;
  auto push = [&](size_t edge) {
    if (edge < edges.size() && seen.insert(edge).second) queue.push_back(edge);
  };
  push(from);
  if (from < edges.size())
    for (size_t i = 0; i < edges.size(); ++i)
      if (edges[i].from == edges[from].to && edges[i].to == edges[from].from)
        push(i);
  for (size_t cursor = 0; cursor < queue.size() && queue.size() < 512; ++cursor)
    for (size_t next_edge : edges[queue[cursor]].next) push(next_edge);
  return seen;
}
// Snapshot of the lane network for the scheduler, including the cars already
// standing on each lane.
void build_stream_view(const RoadRoutes &graph, const StreamRules &rules,
                       size_t player_edge, const RoadPoint &focus, float heading,
                       float speed, void *ground, StreamView &view) {
  const auto &edges = graph.traffic_edges();
  view = StreamView{};
  view.player = focus;
  view.heading = heading;
  view.speed = speed;
  const auto lanes_near = reachable(graph, player_edge);
  const RoadPoint forward{-std::sin(heading), 0, -std::cos(heading)};
  std::unordered_map<size_t, std::vector<float>> residents;
  for (const auto &entry : registrations) {
    const auto &r = entry.second;
    if (r.ground != ground || !r.visible || r.unplaced || r.edge >= edges.size())
      continue;
    residents[r.edge].push_back(r.along);
  }
  view.lanes.reserve(lanes_near.size());
  for (size_t edge : lanes_near) {
    const auto &e = edges[edge];
    if (e.points.empty())
      continue;
    StreamLane lane;
    lane.edge = edge;
    lane.length = e.length;
    lane.crossing = e.crossing;
    lane.reachable = true;
    // Distance to the nearest sampled point of the lane, then how much of the
    // lane lies in front of the player.
    float best = std::numeric_limits<float>::infinity();
    size_t best_index = 0;
    for (size_t i = 0; i < e.points.size(); i += 4) {
      const float d = road_distance(e.points[i], focus);
      if (d < best) {
        best = d;
        best_index = i;
      }
    }
    lane.distance = best;
    // Coarse polyline for measuring a candidate position for real.
    const int sample_count =
        std::clamp(int(e.length / 16.f) + 1, 2, 24);
    RoadRoute line;
    line.points = e.points;
    line.length = e.length;
    lane.samples.reserve(size_t(sample_count));
    for (int i = 0; i < sample_count; ++i)
      lane.samples.push_back(
          road_at(line, e.length * float(i) / float(sample_count - 1)));
    const RoadPoint &mid = e.points[best_index];
    const float dx = mid[0] - focus[0], dz = mid[2] - focus[2];
    const float len = std::sqrt(dx * dx + dz * dz);
    lane.dot = len > 1e-3f ? (dx * forward[0] + dz * forward[2]) / len : 0.f;
    auto found = residents.find(edge);
    if (found != residents.end()) {
      lane.residents = found->second;
      std::sort(lane.residents.begin(), lane.residents.end());
    }
    // Only the part of the lane that is inside the spawn circle counts towards
    // the target, and junction chords never do: a long lane whose nearest point
    // is close still contributes only the chord through the circle.
    if (!lane.crossing && lane.distance < rules.spawn_radius) {
      const float radius = rules.spawn_radius;
      const float half = std::sqrt(std::max(0.f, radius * radius -
                                                   lane.distance * lane.distance));
      view.lane_metres += std::min(lane.length, 2.f * half);
    }
    view.lanes.push_back(std::move(lane));
  }
  // Nearest first: the scheduler draws towards the front of the list, so the
  // budget packs the streets the player is on before it reaches a side street
  // two hundred metres away. Sorting here, once per rebuild, keeps the picker
  // itself free of geometry.
  std::sort(view.lanes.begin(), view.lanes.end(),
            [](const StreamLane &a, const StreamLane &b) {
              return a.distance < b.distance;
            });
}
// Register a car that already exists. `owned` marks the cars this module
// created and may destroy.
int32_t add_car(const RpkIndex &rpk, void *ground, Object *car, Object *where,
                bool owned) {
  population(ground);
  auto *g = gameref_get(ground);
  auto *c = gameref_get(car);
  if (!g || !c)
    return 0;
  for (const auto &entry : registrations)
    if (entry.second.ground == g->key && entry.second.car == c->key &&
        entry.second.car_incarnation == c->incarnation)
      return entry.first;
  if (next_id == INT32_MAX)
    return 0;
  Registration r;
  r.id = next_id++;
  r.ground = g->key;
  r.ground_incarnation = g->incarnation;
  r.car = static_cast<Object *>(c->key);
  r.car_incarnation = c->incarnation;
  r.born = clock_seconds;
  r.owned = owned;
  r.notify = owned;
  r.cruise = (15 + random() * 20) / 3.6f;
  // Traffic never rides the physics drive model in this block.
  if (owned)
    part_set_external_motion(r.car, true);
  if (auto p = point(where)) {
    gameref_set_matrix(r.car, (*p)[0], (*p)[1], (*p)[2], 0, 0, 0);
    r.unplaced = false;
    auto graph = race_ground_roads(rpk, ground);
    if (auto projected = graph ? graph->traffic_project(*p) : std::nullopt) {
      r.edge = projected->edge;
      r.entry = r.edge;
      r.along = projected->along;
      publish(r, *graph, r.along);
    } else {
      r.unplaced = true;
    }
  }
  // A registration always describes a live car, so it is resident traffic from
  // this point on: it counts against the cap and blocks a spawn slot. Cars a
  // script owns are driven by that script (U2-D), but they still occupy a lane.
  r.visible = gameref_get(r.car) != nullptr;
  registrations.emplace(r.id, r);
  return r.id;
}
// Spawn one owned car of `type` on an already chosen road spot. Returns null
// when the resource cannot be assembled.
Object *create_on_graph(Jvm &vm, const RpkIndex &rpk, void *ground,
                        const RoadRoutes &graph, uint32_t type, size_t edge,
                        float along) {
  auto *car = part_create(vm, *resources, static_cast<Object *>(ground), type,
                          "traffic");
  if (!car)
    return nullptr;
  const auto id = add_car(rpk, ground, car, nullptr, true);
  auto it = registrations.find(id);
  if (!id || it == registrations.end()) {
    part_destroy(car);
    return nullptr;
  }
  auto &r = it->second;
  r.edge = std::min(edge, graph.traffic_edges().size() - 1);
  r.entry = r.edge;
  r.along = along;
  r.unplaced = false;
  r.visible = true;
  publish(r, graph, r.along);
  queue(r, true);
  ++spawned_total;
  return car;
}
// Spawn evidence for SLRR_TRAFFIC_LOG: distance to the player and how far the
// spot sits in front of the car at the moment of creation. A pop-in inside the
// minimap or inside the camera would show up here as small `dist` or high
// `ahead`.
void log_spawn(const Registration &r, const RoadPoint &focus,
               const RoadPoint &forward) {
  if (!tracing())
    return;
  const auto pos = position(r.car);
  const auto d = road_distance(pos, focus);
  const float ahead = d > .001f ? ((pos[0] - focus[0]) * forward[0] +
                                   (pos[2] - focus[2]) * forward[2]) /
                                      d
                                : 0.f;
  std::fprintf(stderr,
               "[traffic-spawn] id=%d edge=%zu along=%.1f dist=%.1f ahead=%.2f\n",
               r.id, r.edge, r.along, d, ahead);
}
const Model *weighted_model(const Population &p) {
  float total = 0;
  for (const auto &m : p.models)
    total += m.weight;
  if (!std::isfinite(total) || total <= 0)
    return nullptr;
  float choice = random() * total;
  for (const auto &m : p.models) {
    choice -= m.weight;
    if (choice < 0)
      return &m;
  }
  return &p.models.back();
}
}  // namespace

void race_traffic_forget(void *key) {
  populations.erase(key);
  std::vector<int32_t> ids;
  for (const auto &e : registrations)
    if (e.second.ground == key || e.second.car == key)
      ids.push_back(e.first);
  for (auto id : ids)
    remove(id);
}

void race_traffic_reset() {
  registrations.clear();
  populations.clear();
  events.clear();
  next_id = 1;
  clock_seconds = 0;
  spawned_total = dropped_total = 0;
  ambient_enabled = true;
}

bool race_traffic_ambient_enabled() { return ambient_enabled; }

// Which state of `GameLogic.actualState` owns the road. The street race runs
// in `java.game.RaceSetup` and every track/ROC scene in a `Track` class; the
// city states are the ones whose map scripts fill the traffic templates.
// Unknown sections keep the current policy, so a menu or a garage visit does
// not change anything.
void race_traffic_note_section(const std::string &section) {
  const bool race = section.find("RaceSetup") != std::string::npos ||
                    section.find("Track") != std::string::npos ||
                    section.find("Race") != std::string::npos;
  const bool city = section.find("Valocity") != std::string::npos ||
                    section.find("City") != std::string::npos;
  if (race)
    race_traffic_set_ambient(false);
  else if (city)
    race_traffic_set_ambient(true);
}

void race_traffic_set_ambient(bool enabled) {
  if (enabled == ambient_enabled)
    return;
  ambient_enabled = enabled;
  if (enabled)
    return;
  // Retire the cars this module created. The id list is copied first, because
  // remove() erases from `registrations` and destroys the car; a script-owned
  // entry stays registered and keeps its owner.
  std::vector<int32_t> ids;
  for (const auto &entry : registrations)
    if (entry.second.owned)
      ids.push_back(entry.first);
  for (auto id : ids)
    remove(id);
}

int race_traffic_count(void *ground) {
  void *key = canonical(ground);
  int count = 0;
  for (const auto &e : registrations)
    if (e.second.ground == key && e.second.visible)
      ++count;
  return count;
}

Object *race_traffic_car_at(void *ground, int index) {
  void *key = canonical(ground);
  std::vector<int32_t> ids;
  for (const auto &e : registrations)
    if (e.second.ground == key && e.second.visible)
      ids.push_back(e.first);
  std::sort(ids.begin(), ids.end());
  if (index < 0 || size_t(index) >= ids.size())
    return nullptr;
  return registrations.at(ids[size_t(index)]).car;
}

TrafficPose race_traffic_pose(const RoadRoutes &graph, size_t edge, float along,
                             size_t entry) {
  return traffic_pose(graph, edge, along, entry);
}

// Benchmark autodrive: steer toward a point a short way down the lane the
// player car is standing on, following the same directed transitions the
// traffic uses, so a long run drives the city instead of a straight line.
bool race_traffic_steer(float speed, float *steer) {
  if (!steer || !resources)
    return false;
  auto *world = scene_instances_world();
  auto *car = part_controlled_chassis();
  if (!world || !car)
    return false;
  auto *node = gameref_get(car);
  if (!node)
    return false;
  auto graph = race_ground_roads(*resources, world);
  if (!graph)
    return false;
  const auto &edges = graph->traffic_edges();
  if (edges.empty())
    return false;
  const RoadPoint here{node->pos[0], node->pos[1], node->pos[2]};
  // Project with the car's heading so a lane running the other way is not
  // chosen and the follower does not ask for a U-turn.
  const RoadPoint heading{-std::sin(node->ypr[0]), 0, -std::cos(node->ypr[0])};
  auto projected = graph->traffic_project(here, &heading);
  if (!projected || projected->edge >= edges.size())
    return false;
  // Look further ahead the faster the car goes, and walk the junction chain so
  // the target does not sit on the chord of a turn.
  float look = std::clamp(8.f + .9f * std::max(0.f, speed), 8.f, 45.f);
  size_t edge = projected->edge;
  float at = projected->along + look;
  for (int guard = 0; guard < 8 && at > edges[edge].length; ++guard) {
    if (edges[edge].next.empty())
      break;
    at -= edges[edge].length;
    edge = edges[edge].next.front();
  }
  RoadRoute line;
  line.points = edges[edge].points;
  line.length = edges[edge].length;
  const RoadPoint target = road_at(line, std::min(at, edges[edge].length));
  const float dx = target[0] - here[0], dz = target[2] - here[2];
  if (std::fabs(dx) + std::fabs(dz) < .5f)
    return false;
  const float want = std::atan2(-dx, -dz);
  const float error = std::remainder(want - node->ypr[0], 6.2831853f);
  *steer = std::clamp(error * 1.6f, -1.f, 1.f);
  return true;
}

int race_traffic_pointer(Object *car) {
  auto *n = gameref_get(car);
  if (!n)
    return 0;
  for (const auto &e : registrations)
    if (e.second.car == n->key && e.second.car_incarnation == n->incarnation)
      return e.first;
  return 0;
}

void race_halt_path(const RpkIndex &rpk, void *ground, const RoadPoint &a,
                    const RoadPoint &b) {
  auto graph = race_ground_roads(rpk, ground);
  auto *p = population(ground);
  if (!graph || !p)
    return;
  auto route = graph->query(a, b);
  if (!route)
    return;
  std::unordered_set<uint32_t> selected(route->roads.begin(), route->roads.end());
  std::vector<std::pair<uint32_t, uint32_t>> pairs;
  for (const auto &e : graph->edges())
    if (std::any_of(e.roads.begin(), e.roads.end(),
                    [&](auto id) { return selected.contains(id); }))
      pairs.push_back({std::min(e.from, e.to), std::max(e.from, e.to)});
  for (const auto &e : graph->edges())
    if (std::find(pairs.begin(), pairs.end(),
                  std::pair{std::min(e.from, e.to), std::max(e.from, e.to)}) !=
        pairs.end())
      for (auto id : e.roads)
        p->blocked.insert(id);
}

void race_halt_cross(const RpkIndex &rpk, void *ground, const RoadPoint &pos,
                     float duration) {
  if (!std::isfinite(duration) || duration <= 0)
    return;
  auto graph = race_ground_roads(rpk, ground);
  auto *p = population(ground);
  if (!graph || !p)
    return;
  uint32_t node = 0;
  float best = std::numeric_limits<float>::infinity();
  for (const auto &e : graph->edges()) {
    float a = road_distance(pos, e.points.front()),
          b = road_distance(pos, e.points.back());
    if (a < best) {
      best = a;
      node = e.from;
    }
    if (b < best) {
      best = b;
      node = e.to;
    }
  }
  if (!node)
    return;
  double expiry = clock_seconds + std::min(duration, 86400.f);
  p->crossings[node] = std::max(p->crossings[node], expiry);
}

void race_clear_halts(void *ground) {
  if (auto *p = population(ground)) {
    p->blocked.clear();
    p->crossings.clear();
  }
}

void register_traffic_natives(Jvm &vm, RpkIndex &rpk, SectionMachine &machine) {
  race_traffic_reset();
  sections = &machine;
  resources = &rpk;
  const char *k = "java.util.resource.GroundRef";
  // 0x00484730: attach an existing car to the road near `pos` and give it a
  // lane and a cruising speed. The car belongs to the caller, so its movement
  // stays with its owner (Race Brain, U2-D).
  vm.natives().register_method(
      k, "addTrafficCar", "(Ljava.util.resource.GameRef;Ljava.lang.Vector3;)I",
      [&rpk](Object *self, const std::vector<Value> &a) {
        auto *car = object(a, 0);
        if (auto *chassis = part_chassis(car))
          car = chassis;
        if (!car)
          return Value::make_int(0);
        auto graph = race_ground_roads(rpk, self);
        auto where = object(a, 1);
        auto snap = point(where);
        if (graph && snap)
          if (auto cross = graph->nearest_cross(*snap, 0))
            snap = cross;
        Object anchor;
        anchor.fields["x"] = Value::make_float(snap ? (*snap)[0] : 0);
        anchor.fields["y"] = Value::make_float(snap ? (*snap)[1] : 0);
        anchor.fields["z"] = Value::make_float(snap ? (*snap)[2] : 0);
        return Value::make_int(add_car(rpk, self, car, snap ? &anchor : where, false));
      });
  vm.natives().register_method(k, "remTrafficCar", "(I)V",
                               [](Object *self, const std::vector<Value> &a) {
                                 auto id = int32_t(number(a, 0));
                                 auto it = registrations.find(id);
                                 if (it != registrations.end() &&
                                     it->second.ground == canonical(self))
                                   remove(id);
                                 return Value::make_void();
                               });
  // 0x00484AF0: the byte the scripts read back. It marks cars the caller wants
  // `car_add`/`car_rem` messages for.
  vm.natives().register_method(k, "notifyTrafficCar", "(II)I",
                               [](Object *self, const std::vector<Value> &a) {
                                 auto it = registrations.find(int32_t(number(a, 0)));
                                 if (it == registrations.end() ||
                                     it->second.ground != canonical(self))
                                   return Value::make_int(0);
                                 bool enabled = number(a, 1) != 0;
                                 if (!enabled)
                                   queue(it->second, false);
                                 it->second.notify = enabled;
                                 if (enabled && it->second.visible)
                                   queue(it->second, true);
                                 return Value::make_int(enabled ? 1 : 0);
                               });
  // 0x00484050: the map registers n cars of one template. The stock call is
  // addTrafficN(type, n, lenBegin, lenEnd, wheelBase), n already scaled by
  // Config.trafficDensity in Java.
  vm.natives().register_method(
      k, "addTrafficN", "(Ljava.util.resource.GameRef;IFFF)I",
      [](Object *self, const std::vector<Value> &a) {
        auto *p = population(self);
        auto type = model_type(object(a, 0));
        float count = number(a, 1);
        if (!p || !type || !std::isfinite(count) || count <= 0)
          return Value::make_int(0);
        Model m{type, count, number(a, 2), number(a, 3), number(a, 4)};
        for (size_t i = 0; i < p->models.size(); ++i)
          if (p->models[i].type == type) {
            p->models[i].weight += count;
            return Value::make_int(int(count));
          }
        p->models.push_back(m);
        return Value::make_int(int(count));
      });
  // 0x00484420: one car near a position. The shipped body ignores n and spawns
  // a single car; a repeated request is kept as a seed until it is satisfied.
  vm.natives().register_method(
      k, "addTrafficP",
      "(Ljava.util.resource.GameRef;Ljava.lang.Vector3;IFFF)I",
      [](Object *self, const std::vector<Value> &a) {
        auto *p = population(self);
        auto type = model_type(object(a, 0));
        auto at = point(object(a, 1));
        if (!p || !type || !at)
          return Value::make_int(0);
        Seed seed;
        seed.model = {type, 1, number(a, 3), number(a, 4), number(a, 5)};
        seed.position = *at;
        seed.remaining = 1;
        p->seeds.push_back(seed);
        return Value::make_int(1);
      });
  vm.natives().register_method(k, "delTraffic", "()V",
                               [](Object *self, const std::vector<Value> &) {
                                 auto key = canonical(self);
                                 std::vector<int32_t> ids;
                                 for (const auto &e : registrations)
                                   if (e.second.ground == key)
                                     ids.push_back(e.first);
                                 for (auto id : ids)
                                   remove(id);
                                 populations.erase(key);
                                 return Value::make_void();
                               });
  vm.natives().register_method(
      k, "setTrafficCarBehaviour", "(II)V",
      [](Object *self, const std::vector<Value> &a) {
        auto it = registrations.find(int32_t(number(a, 0)));
        if (it != registrations.end() &&
            it->second.ground == canonical(self))
          it->second.mode = int(number(a, 1));
        return Value::make_void();
      });
  vm.natives().register_method(
      k, "haltTrafficCross", "(Ljava.lang.Vector3;F)V",
      [&rpk](Object *self, const std::vector<Value> &a) {
        auto p = point(object(a, 0));
        if (p)
          race_halt_cross(rpk, self, *p, number(a, 1));
        return Value::make_void();
      });
  vm.natives().register_method(
      k, "haltTrafficPath", "(Ljava.lang.Vector3;Ljava.lang.Vector3;)V",
      [&rpk](Object *self, const std::vector<Value> &a) {
        auto x = point(object(a, 0)), y = point(object(a, 1));
        if (x && y)
          race_halt_path(rpk, self, *x, *y);
        return Value::make_void();
      });
  vm.natives().register_method(k, "clearHalts", "()V",
                               [](Object *self, const std::vector<Value> &) {
                                 race_clear_halts(self);
                                 return Value::make_void();
                               });
}

void race_traffic_tick(Jvm &vm, float seconds, double elapsed_seconds) {
  if (!std::isfinite(seconds) || seconds < 0 || !resources)
    return;
  ProfTickGuard tick_guard(clock_seconds);
  // The clock follows the pause/warp-aware simulation time; the motion step is
  // the already bounded one from `Jvm::advance_sim_time`. A slow frame must not
  // stretch a halt, and a paused game must not move anything.
  if (elapsed_seconds < 0)
    elapsed_seconds = seconds;
  if (std::isfinite(elapsed_seconds) && elapsed_seconds > 0)
    clock_seconds += elapsed_seconds;
  auto *world = scene_instances_world();
  auto *focus_car = part_controlled_chassis();
  const auto focus = focus_car ? position(focus_car) : RoadPoint{};
  // The player's facing, for the spawn cone. ypr[0] is set by the same
  // convention the traffic pose uses (yaw = atan2(-dx, -dz)), so the forward
  // axis is (-sin, 0, -cos).
  float focus_yaw = 0.f;
  float focus_speed = 0.f;
  if (focus_car)
    if (auto *n = gameref_get(focus_car)) {
      focus_yaw = n->ypr[0];
      focus_speed = std::hypot(n->vel[0], n->vel[2]);
    }
  const RoadPoint forward{-std::sin(focus_yaw), 0, -std::cos(focus_yaw)};
  // Profiling knobs, not gameplay settings: `SLRR_TRAFFIC_DENSITY` overrides
  // the shipped slider so one build can be measured at several populations,
  // and `SLRR_TRAFFIC_FREEZE=1` stops spawning and retiring once the population
  // first reaches its cap, which isolates the cost of driving the cars that
  // already exist from the cost of creating and destroying them.
  float density = host_game_options().traffic_density;
  static const float density_override = [] {
    const char *v = std::getenv("SLRR_TRAFFIC_DENSITY");
    return v ? std::strtof(v, nullptr) : -1.f;
  }();
  if (density_override >= 0) density = density_override;
  static const bool freeze_enabled = std::getenv("SLRR_TRAFFIC_FREEZE") != nullptr;
  static bool frozen = false;
  const int cap = std::isfinite(density) ? int(std::clamp(density, 0.f, 1.f) * 100) : 0;
  std::vector<void *> ground_keys;
  for (const auto &e : populations)
    ground_keys.push_back(e.first);
  // One car per maintenance pass: a long deficit fills over several passes
  // instead of dropping a burst into a single frame.
  int spawn_budget = 1;
  static double logged = -1;
  static double next_pass = 0;
  static int last_target = 0;
  static const char *last_refused = "";
  for (void *key : ground_keys) {
    if (!world || !focus_car || !belongs(key, world) || seconds == 0)
      continue;
    // A race section owns the road: ambient cars stay out until the city comes
    // back. Retiring happens on the transition, so this is only a guard.
    if (!ambient_enabled)
      continue;
    auto graph = race_ground_roads(*resources, key);
    auto *p = population(key);
    if (!graph || !p || graph->traffic_edges().empty())
      continue;
    const auto &edges = graph->traffic_edges();
    const StreamRules rules = stream_rules(density);
    std::vector<int32_t> ids;
    for (const auto &e : registrations)
      if (e.second.ground == key)
        ids.push_back(e.first);
    std::sort(ids.begin(), ids.end());
    // Population target for the network around the player, from the slider and
    // the reachable lane length. A straight street holds more cars than the same
    // radius of city blocks, and the number is a target, not a counter.
    //
    // The view walks every reachable lane, so it is rebuilt on the same quarter
    // second cadence as the population pass and not once per frame: the player
    // does not outrun a 260 m radius in 250 ms at any speed this game reaches.
    static StreamView view;
    static int target = 0;
    static double view_at = -1e9;
    static void *view_ground = nullptr;
    const auto player_lane = graph->traffic_project(
        focus, focus_car ? &forward : nullptr);
    const float slider = std::isfinite(density) ? std::clamp(density, 0.f, 1.f) : 0.f;
    if (!player_lane)
      target = 0;
    else if (key != view_ground || clock_seconds - view_at >= .25) {
      build_stream_view(*graph, rules, player_lane->edge, focus, focus_yaw,
                        focus_speed, key, view);
      target = stream_target_count(view, rules, slider);
      view_at = clock_seconds;
      view_ground = key;
    }
    if (!player_lane) view.lanes.clear();
    // 1. Retire stale, out-of-range and blocked entries. A car whose GameRef
    //    was re-created on the same address fails the incarnation check.
    int visible = 0;
    int owned_count = 0;
    int64_t t = prof_now();
    for (auto id : ids) {
      auto it = registrations.find(id);
      if (it == registrations.end())
        continue;
      auto &r = it->second;
      auto *n = gameref_get(r.car);
      if (!n || n->incarnation != r.car_incarnation) {
        remove(id);
        continue;
      }
      if (r.edge >= edges.size())
        r.unplaced = true;
      if (!r.visible)
        continue;
      const float distance = road_distance(position(r.car), focus);
      // The retire radius is wider than the spawn radius, so a car created at
      // the inner edge of the band does not leave it on the next frame.
      // A script-owned car (police scout, race bot) is not counted against the
      // budget and is never retired to make room for ambient traffic.
      const bool young = clock_seconds - r.born < kMinLifetime;
      const bool far =
          distance > (r.owned ? rules.retire_radius : kRetireScripted);
      // Over target: only cars already outside the spawn radius may go, and
      // only by a margin, so the population settles instead of oscillating.
      const bool over = r.owned && distance > rules.spawn_radius &&
                        owned_count >= target + 4;
      const bool closed =
          r.owned && !r.unplaced && r.edge < edges.size() && blocked(*p, edges[r.edge]);
      if (frozen)
        continue;
      if (r.owned && (closed || ((far || over) && !young))) {
        if (r.edge < edges.size())
          edge_cooldown[r.edge] = clock_seconds + kEdgeCooldown;
        queue(r, false);
        r.visible = false;
        remove(id);
        if (profiling()) ++prof().despawns;
        continue;
      }
      if (far && !r.owned) {
        queue(r, false);
        r.visible = false;
        continue;
      }
      ++visible;
      if (r.owned) ++owned_count;
    }
    prof_add(kProfRetire, t);
    if (profiling()) {
      prof().active = visible;
      prof().cap = cap;
    }
    if (freeze_enabled && !frozen && visible >= cap && cap > 0) {
      frozen = true;
      std::fprintf(stderr, "[traffic] frozen at %d cars (SLRR_TRAFFIC_FREEZE)\n",
                   visible);
    }
    // 2. Drive the cars this module owns along their directed lane.
    t = prof_now();
    for (auto id : ids) {
      auto it = registrations.find(id);
      if (it == registrations.end())
        continue;
      auto &r = it->second;
      if (!r.visible || !r.owned)
        continue;
      auto *n = gameref_get(r.car);
      if (!n || n->incarnation != r.car_incarnation)
        continue;
      if (r.unplaced || r.edge >= edges.size())
        continue;
      const auto &e = edges[r.edge];
      float desired = r.cruise;
      if (blocked(*p, e))
        desired = 0;
      // Keep a gap to the car ahead on the same lane. This is spacing, not
      // collision: the runtime only slows down, it never applies impulses.
      for (const auto &entry : registrations) {
        const auto &peer = entry.second;
        if (peer.id != r.id && peer.visible && peer.ground == key &&
            peer.edge == r.edge && peer.along > r.along)
          desired = std::min(
              desired, std::sqrt(8 * std::max(0.f, peer.along - r.along - 8)));
      }
      r.speed += std::clamp(desired - r.speed, -4.f * seconds, .8f * seconds);
      float advance = r.speed * seconds;
      if (desired == 0)
        advance = std::min(advance, std::max(0.f, e.length - r.along));
      r.along += advance;
      if (advance <= 0)
        r.speed = 0;
      if (r.along > e.length) {
        std::vector<size_t> choices;
        for (size_t i : e.next)
          if (!blocked(*p, edges[i]))
            choices.push_back(i);
        if (choices.empty()) {
          r.along = e.length;
        } else {
          r.along -= e.length;
          r.entry = r.edge;
          r.edge = choose_transition(choices, *graph);
          r.along = std::min(r.along, edges[r.edge].length);
        }
      }
      const float previous_yaw = n->ypr[0];
      Tracked was;
      if (diagnosing() && r.posed) {
        was.valid = true;
        was.edge = r.posed_edge;
        was.along = r.posed_along;
        was.road_y = r.posed_road_y;
        was.body_y = r.posed_body_y;
        was.yaw = r.posed_yaw;
        was.world = {n->pos[0], n->pos[1], n->pos[2]};
      }
      publish(r, *graph, std::min(r.along, edges[r.edge].length));
      if (was.valid) {
        auto *published = gameref_get(r.car);
        report_discontinuity(r, was,
                             {published->pos[0], published->pos[1],
                              published->pos[2]},
                             seconds);
      }
      const float delta = std::remainder(n->ypr[0] - previous_yaw, 6.2831853f);
      const int64_t tw = prof_now();
      part_animate_external_wheels(r.car, advance,
                                   advance > .001f ? std::atan(2.5f * delta / advance)
                                                   : 0);
      prof_add(kProfWheels, tw);
    }
    prof_add(kProfDrive, t);
    // 3. Fill the population from the scheduler, four times a second. The
    //    spot is chosen before anything is allocated, so a network with no room
    //    costs a few random draws and nothing else.
    if (!frozen && !view.lanes.empty() && clock_seconds >= next_pass &&
        owned_count < target && spawn_budget > 0) {
      next_pass = clock_seconds + .25;
      while (spawn_budget > 0 && owned_count < target) {
        // A positioned request (addTrafficP) is satisfied first, in the same
        // placement policy as the templates.
        const Model *selected = nullptr;
        std::vector<Seed>::iterator seed_at = p->seeds.end();
        for (auto it = p->seeds.begin(); it != p->seeds.end(); ++it)
          if (it->remaining > 0) {
            seed_at = it;
            selected = &it->model;
            break;
          }
        if (!selected)
          selected = weighted_model(*p);
        if (!selected)
          break;
        t = prof_now();
        uint32_t draw = uint32_t(random() * 4294967295.f);
        StreamSpot spot = stream_pick_spot(view, rules, draw);
        // An edge that was just emptied keeps its cooldown: destroying a car and
        // putting another one in the same place is the churn U2-B.1 removed.
        for (int retry = 0; spot.valid && retry < 4; ++retry) {
          auto cool = edge_cooldown.find(spot.edge);
          if (cool == edge_cooldown.end() || cool->second <= clock_seconds) break;
          spot = stream_pick_spot(view, rules, draw);
        }
        if (spot.valid) {
          auto cool = edge_cooldown.find(spot.edge);
          if (cool != edge_cooldown.end() && cool->second > clock_seconds)
            spot.valid = false;
        }
        prof_add(kProfSearch, t);
        last_refused = spot.valid ? "" : spot.refused;
        if (profiling()) {
          ++prof().attempts;
          if (!spot.valid) ++prof().failures;
        }
        if (!spot.valid)
          break;
        --spawn_budget;
        t = prof_now();
        auto *made = create_on_graph(vm, *resources, key, *graph, selected->type,
                                     spot.edge, spot.along);
        prof_add(kProfCreate, t);
        if (profiling()) {
          if (made) {
            ++prof().creates;
            ++prof().spawns;
          } else {
            ++prof().create_failures;
          }
        }
        if (!made) {
          if (seed_at != p->seeds.end()) seed_at->remaining = 0;
          break;
        }
        ++owned_count;
        if (seed_at != p->seeds.end()) --seed_at->remaining;
        if (auto it2 = registrations.find(race_traffic_pointer(made));
            it2 != registrations.end()) {
          log_spawn(it2->second, focus, forward);
          // Keep the view current, so a second car in the same pass cannot be
          // put next to this one.
          for (auto &lane : view.lanes)
            if (lane.edge == it2->second.edge) {
              lane.residents.push_back(it2->second.along);
              std::sort(lane.residents.begin(), lane.residents.end());
              break;
            }
        }
      }
      std::erase_if(p->seeds, [](const Seed &seed) { return seed.remaining <= 0; });
    }
    if (tracing() && clock_seconds - logged >= 1) {
      logged = clock_seconds;
      // One line a second: the resident count plus the first car's lane
      // position and world pose, so movement is checkable from the log.
      int32_t first = 0;
      for (auto id : ids) {
        auto snapshot = registrations.find(id);
        if (snapshot != registrations.end() && snapshot->second.visible) {
          first = id;
          break;
        }
      }
      // What the player actually sees: how many cars stand within 140 m, and
      // how many of those are ahead inside 220 m. This is the number that tells
      // whether a density setting looks dense, which a global count cannot.
      int near_count = 0, ahead_count = 0;
      for (auto id : ids) {
        auto snapshot = registrations.find(id);
        if (snapshot == registrations.end() || !snapshot->second.visible)
          continue;
        const auto at = position(snapshot->second.car);
        const float dx = at[0] - focus[0];
        const float dz = at[2] - focus[2];
        const float d = std::sqrt(dx * dx + dz * dz);
        if (d <= 140.f) ++near_count;
        if (d <= 220.f && d > .5f &&
            (dx * forward[0] + dz * forward[2]) / d > .3f)
          ++ahead_count;
      }
      last_target = target;
      std::fprintf(stderr,
                   "[traffic] t=%.1f ground=0x%x visible=%d owned=%d target=%d "
                   "cap=%d lane_m=%.0f spacing=%.0f total=%zu dropped=%zu "
                   "models=%zu seeds=%zu near=%d ahead=%d refused=%s",
                   clock_seconds, gameref_get(key) ? gameref_get(key)->type_id : 0,
                   visible, owned_count, last_target, cap, view.lane_metres,
                   stream_spacing(std::isfinite(density) ? density : 0.f, rules),
                   spawned_total, dropped_total, p->models.size(), p->seeds.size(),
                   near_count, ahead_count,
                   last_refused && *last_refused ? last_refused : "-");
      if (auto found = registrations.find(first); found != registrations.end()) {
        const auto &r = found->second;
        auto pos = position(r.car);
        std::fprintf(stderr, " first=id%d edge=%zu along=%.1f v=%.1f pos=%.1f,%.1f",
                     first, r.edge, r.along, r.speed, pos[0], pos[2]);
      }
      std::fprintf(stderr, "\n");
    }
  }
  // Deliver after placement, and after the calling native has returned.
  const int64_t te = prof_now();
  auto pending = std::move(events);
  events.clear();
  for (const auto &e : pending) {
    auto *target = gameref_get(e.target);
    if (!target || target->incarnation != e.incarnation ||
        !(target->event_mask & 0x10) || !gameref_get(e.ground) ||
        gameref_get(e.ground)->incarnation != e.ground_incarnation)
      continue;
    auto *message = vm.new_string(e.text);
    Jvm::RootScope roots(vm, [&](const GcRootVisitor &visit) {
      visit(e.target);
      visit(e.ground);
      visit(message);
    });
    vm.invoke(e.target->class_name, "handleEvent",
              "(Ljava.util.resource.GameRef;ILjava.lang.String;)V",
              {Value::make_obj(e.target), Value::make_obj(e.ground),
               Value::make_int(0x10), Value::make_obj(message)},
              false);
  }
  prof_add(kProfEvents, te);
}
}  // namespace slrr

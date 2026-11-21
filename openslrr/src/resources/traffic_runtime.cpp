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
#include "slrr/scene_instances.hpp"
#include "slrr/section_machine.hpp"

#include <algorithm>
#include <cmath>
#include <cstdio>
#include <cstdlib>
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
  float along = 0, speed = 0, cruise = 0;
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
// Publish the pose for one car: the graph point is the tyre contact, so the
// chassis is parked onto it with its loaded springs. The caller then animates
// the wheels with the distance it advanced.
void publish(Registration &r, const RoadRoutes &graph, float at) {
  const auto &e = graph.traffic_edges()[r.edge];
  RoadRoute route;
  route.points = e.points;
  auto p = road_at(route, at);
  auto q = road_at(route, std::min(e.length, at + .25f));
  auto previous = road_at(route, std::max(0.f, at - .25f));
  auto direction = road_normalize(
      {q[0] - previous[0], q[1] - previous[1], q[2] - previous[2]});
  SceneFloorHit floor;
  if (scene_instances_sample_floor(p[0], p[2], p[1] + 4, r.car, floor))
    p[1] = floor.height;
  gameref_set_matrix(r.car, p[0], p[1], p[2],
                     std::atan2(-direction[0], -direction[2]), 0, 0);
  part_park_chassis(r.car);
  if (auto *n = gameref_get(r.car))
    for (int i = 0; i < 3; ++i)
      n->vel[i] = direction[i] * r.speed;
}
// Find a free spot on a lane before anything is allocated. The candidate is
// rejected when it is blocked, too short, occupied, or outside the band the
// streaming policy wants around the player.
bool place(Registration &r, const RoadRoutes &graph, const Population &p,
           const RoadPoint &focus) {
  const auto &edges = graph.traffic_edges();
  if (edges.empty())
    return false;
  for (int attempt = 0; attempt < 96; ++attempt) {
    size_t edge =
        std::min<size_t>(size_t(random() * edges.size()), edges.size() - 1);
    const auto &e = edges[edge];
    if (blocked(p, e) || e.crossing || e.next.empty() || e.length < 15)
      continue;
    float at = std::clamp(random() * e.length, 5.f, e.length - 5);
    RoadRoute line;
    line.points = e.points;
    auto candidate = road_at(line, at);
    float distance = road_distance(candidate, focus);
    if (distance < 45 || distance > 160)
      continue;
    bool occupied = false;
    for (const auto &entry : registrations)
      if (entry.first != r.id && entry.second.visible &&
          road_distance(candidate, position(entry.second.car)) < 12) {
        occupied = true;
        break;
      }
    if (occupied)
      continue;
    r.edge = edge;
    r.along = at;
    r.unplaced = false;
    return true;
  }
  return false;
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
  r.along = along;
  r.unplaced = false;
  r.visible = true;
  publish(r, graph, r.along);
  queue(r, true);
  ++spawned_total;
  return car;
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
  const float density = host_game_options().traffic_density;
  const int cap = std::isfinite(density) ? int(std::clamp(density, 0.f, 1.f) * 100) : 0;
  std::vector<void *> ground_keys;
  for (const auto &e : populations)
    ground_keys.push_back(e.first);
  int spawn_budget = 2;
  static double logged = -1;
  for (void *key : ground_keys) {
    if (!world || !focus_car || !belongs(key, world) || seconds == 0)
      continue;
    auto graph = race_ground_roads(*resources, key);
    auto *p = population(key);
    if (!graph || !p || graph->traffic_edges().empty())
      continue;
    const auto &edges = graph->traffic_edges();
    std::vector<int32_t> ids;
    for (const auto &e : registrations)
      if (e.second.ground == key)
        ids.push_back(e.first);
    std::sort(ids.begin(), ids.end());
    // 1. Retire stale, over-cap and out-of-range entries. A car whose GameRef
    //    was re-created on the same address fails the incarnation check.
    int visible = 0;
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
      const bool over = visible >= cap;
      const bool far = distance > (r.owned ? 260 : 400);
      const bool closed = !r.unplaced && r.edge < edges.size() && blocked(*p, edges[r.edge]);
      if (over || far || closed) {
        queue(r, false);
        r.visible = false;
        if (r.owned) {
          remove(id);
          continue;
        }
      } else {
        ++visible;
      }
    }
    // 2. Drive the cars this module owns along their directed lane.
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
          r.edge = choose_transition(choices, *graph);
        }
      }
      const float previous_yaw = n->ypr[0];
      publish(r, *graph, std::min(r.along, edges[r.edge].length));
      const float delta = std::remainder(n->ypr[0] - previous_yaw, 6.2831853f);
      part_animate_external_wheels(r.car, advance,
                                   advance > .001f ? std::atan(2.5f * delta / advance)
                                                   : 0);
    }
    // 3. Fill the population: positioned requests first, then the template
    //    weights. The road spot is chosen before a car is allocated.
    for (auto &seed : p->seeds) {
      while (seed.remaining > 0 && visible < cap && spawn_budget > 0) {
        auto projected = graph->traffic_project(seed.position);
        if (!projected || blocked(*p, edges[projected->edge]))
          break;
        Registration candidate;
        if (!place(candidate, *graph, *p, focus))
          break;
        --spawn_budget;
        if (!create_on_graph(vm, *resources, key, *graph, seed.model.type,
                             candidate.edge, candidate.along)) {
          seed.remaining = 0;
          break;
        }
        ++visible;
        --seed.remaining;
      }
    }
    std::erase_if(p->seeds, [](const Seed &seed) { return seed.remaining <= 0; });
    for (int attempt = 0; visible < cap && spawn_budget > 0 && attempt < cap * 3;
         ++attempt) {
      const Model *selected = weighted_model(*p);
      if (!selected)
        break;
      Registration candidate;
      if (!place(candidate, *graph, *p, focus))
        continue;
      --spawn_budget;
      if (!create_on_graph(vm, *resources, key, *graph, selected->type,
                           candidate.edge, candidate.along))
        continue;
      ++visible;
    }
    if (tracing() && clock_seconds - logged >= 1) {
      logged = clock_seconds;
      // One line a second: the resident count plus the first car's lane
      // position and world pose, so movement is checkable from the log.
      int32_t first = 0;
      for (auto id : ids)
        if (registrations.at(id).visible) {
          first = id;
          break;
        }
      std::fprintf(stderr,
                   "[traffic] t=%.1f ground=0x%x visible=%d cap=%d total=%zu "
                   "dropped=%zu models=%zu seeds=%zu",
                   clock_seconds, gameref_get(key) ? gameref_get(key)->type_id : 0,
                   visible, cap, spawned_total, dropped_total, p->models.size(),
                   p->seeds.size());
      if (first) {
        const auto &r = registrations.at(first);
        auto pos = position(r.car);
        std::fprintf(stderr, " first=id%d edge=%zu along=%.1f v=%.1f pos=%.1f,%.1f",
                     first, r.edge, r.along, r.speed, pos[0], pos[2]);
      }
      std::fprintf(stderr, "\n");
    }
  }
  // Deliver after placement, and after the calling native has returned.
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
}
}  // namespace slrr

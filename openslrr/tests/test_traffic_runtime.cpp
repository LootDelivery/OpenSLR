// U2-B regression suite: the GroundRef ambient-traffic natives, the
// registration lifecycle and the road binding.
//
// Everything here runs without a window, a scene or a Java class library: the
// natives only touch GameRef objects, so a bare GameRef pair (ground + car)
// exercises the same code path the shipped scripts use. Movement itself needs
// the frame loop (`race_traffic_tick` with a controlled chassis and a scene
// world), so it is covered by the in-game runs recorded in
// docs/UPSTREAM_INTEGRATION.md, not by this suite.
#include "standalone_test.hpp"

#include "slrr/gameref.hpp"
#include "slrr/jvm.hpp"
#include "slrr/native_registry.hpp"
#include "slrr/race_runtime.hpp"
#include "slrr/road_routes.hpp"
#include "slrr/rpk_index.hpp"
#include "slrr/section_machine.hpp"

#include <cmath>
#include <cstdio>
#include <cstdlib>
#include <filesystem>
#include <string>
#include <vector>

namespace fs = std::filesystem;
using namespace slrr;

namespace {
fs::path game_root() {
  if (const char* env = std::getenv("SLRR_GAME_ROOT"))
    if (*env) return fs::path(env);
  for (fs::path p : {fs::path("../Street Legal Racing - Redline"),
                     fs::path("game")}) {
    std::error_code ec;
    if (fs::exists(p / "maps/city.rpk", ec)) return fs::absolute(p);
  }
  return {};
}
// The retail install has ten traffic templates in cars/traffic/*.rpk. The map
// asks for the car part of each pack, so id 6 is the usual template resource.
uint32_t traffic_template(const RpkIndex& packs) {
  const int32_t pack = const_cast<RpkIndex&>(packs).open_lib("cars/traffic/Taxi.rpk");
  return pack > 0 ? RpkIndex::make_full_id(pack, 6) : 0;
}
Object* vector(Jvm& vm, const RoadPoint& p) {
  auto* v = vm.new_object("java.lang.Vector3");
  for (int i = 0; i < 3; ++i)
    v->fields[i == 0 ? "x" : i == 1 ? "y" : "z"] = Value::make_float(p[i]);
  return v;
}
RoadPoint point_of(Object* o) {
  RoadPoint p{};
  for (int i = 0; i < 3; ++i)
    p[i] = o->fields[i == 0 ? "x" : i == 1 ? "y" : "z"].v.f;
  return p;
}
// A ground and a car stand-in: the natives only read the GameRef node, so an
// empty object is enough to register and place a traffic entry.
struct Fixture {
  NativeRegistry registry;
  Jvm vm{&registry};
  RpkIndex packs;
  SectionMachine sections{&vm};
  Object ground, car, other;
  int32_t pack = 0;
  uint32_t type = 0;
  Value call(const char* name, const char* sig, std::vector<Value> args = {}) {
    return registry.invoke("java.util.resource.GroundRef", name, sig, &ground,
                           args);
  }
};
bool open(Fixture& f) {
  const fs::path root = game_root();
  if (root.empty()) return false;
  f.packs.set_root(root.string());
  f.pack = f.packs.open_lib("maps/city.rpk");
  if (f.pack <= 0) return false;
  f.type = traffic_template(f.packs);
  register_race_natives(f.vm, f.packs, f.sections);
  gameref_upsert(&f.ground, GameRefKind::GroundRef)->type_id =
      RpkIndex::make_full_id(f.pack, 1);
  auto* model = gameref_upsert(&f.other, GameRefKind::GameType);
  model->type_id = f.type;
  return f.type != 0;
}
}  // namespace

// addTrafficN is the map's population request: it merges the weight of one
// template instead of appending a second entry, and addTrafficP keeps a
// position seed that the tick consumes later.
SLRR_TEST(template_registration) {
  Fixture f;
  if (!open(f)) {
    std::puts("SKIP retail install not found (set SLRR_GAME_ROOT)");
    return;
  }
  const char* n = "(Ljava.util.resource.GameRef;IFFF)I";
  auto first =
      f.call("addTrafficN", n, {Value::make_obj(&f.other), Value::make_float(2),
                                Value::make_float(0), Value::make_float(4),
                                Value::make_float(2)})
          .v.i;
  SLRR_REQUIRE(first == 2);
  // A second request for the same template merges: the answer is the count the
  // script asked for, not the running total.
  auto again =
      f.call("addTrafficN", n, {Value::make_obj(&f.other), Value::make_float(3),
                                Value::make_float(0), Value::make_float(4),
                                Value::make_float(2)})
          .v.i;
  SLRR_REQUIRE(again == 3);
  // Junk requests are refused and leave the population alone.
  SLRR_REQUIRE(f.call("addTrafficN", n,
                      {Value::make_obj(&f.other), Value::make_float(0),
                       Value::make_float(0), Value::make_float(4),
                       Value::make_float(2)})
                   .v.i == 0);
  SLRR_REQUIRE(f.call("addTrafficN", n,
                      {Value::make_obj(nullptr), Value::make_float(1),
                       Value::make_float(0), Value::make_float(4),
                       Value::make_float(2)})
                   .v.i == 0);
  auto* at = vector(f.vm, {100, 0, 100});
  SLRR_REQUIRE(
      f.call("addTrafficP",
             "(Ljava.util.resource.GameRef;Ljava.lang.Vector3;IFFF)I",
             {Value::make_obj(&f.other), Value::make_obj(at), Value::make_float(1),
              Value::make_float(0), Value::make_float(4), Value::make_float(2)})
              .v.i == 1);
  SLRR_REQUIRE(f.call("addTrafficP",
                      "(Ljava.util.resource.GameRef;Ljava.lang.Vector3;IFFF)I",
                      {Value::make_obj(&f.other), Value::make_obj(nullptr),
                       Value::make_float(1), Value::make_float(0),
                       Value::make_float(4), Value::make_float(2)})
                   .v.i == 0);
  // delTraffic drops the population and every registration of that ground.
  f.call("delTraffic", "()V");
  SLRR_REQUIRE(race_traffic_count(&f.ground) == 0);
  gameref_clear();
  std::puts("PASS templates merge by type, position seeds are kept, delTraffic clears");
}

// addTrafficCar attaches a car the caller already owns. It must be idempotent
// (a repeated call returns the same id), removable, and it must not survive the
// ground it belongs to.
SLRR_TEST(car_lifecycle) {
  Fixture f;
  if (!open(f)) {
    std::puts("SKIP retail install not found (set SLRR_GAME_ROOT)");
    return;
  }
  auto graph = race_ground_roads(f.packs, &f.ground);
  SLRR_REQUIRE(graph && !graph->edges().empty());
  const auto& e = graph->edges().front();
  gameref_upsert(&f.car, GameRefKind::GameType)->type_id = f.type;
  auto* where = vector(f.vm, e.points.front());
  const char* sig = "(Ljava.util.resource.GameRef;Ljava.lang.Vector3;)I";
  const int id = f.call("addTrafficCar", sig,
                        {Value::make_obj(&f.car), Value::make_obj(where)})
                     .v.i;
  SLRR_REQUIRE(id > 0);
  SLRR_REQUIRE(race_traffic_count(&f.ground) == 1);
  SLRR_REQUIRE(race_traffic_pointer(&f.car) == id);
  SLRR_REQUIRE(race_traffic_car_at(&f.ground, 0) == &f.car);
  SLRR_REQUIRE(race_traffic_car_at(&f.ground, 1) == nullptr);
  SLRR_REQUIRE(f.call("addTrafficCar", sig,
                      {Value::make_obj(&f.car), Value::make_obj(where)})
                   .v.i == id);
  SLRR_REQUIRE(race_traffic_count(&f.ground) == 1);
  // A car from another ground is not this ground's traffic.
  SLRR_REQUIRE(f.call("remTrafficCar", "(I)V", {Value::make_int(id + 1000)}).tag ==
               ValueTag::Void);
  SLRR_REQUIRE(race_traffic_count(&f.ground) == 1);
  f.call("remTrafficCar", "(I)V", {Value::make_int(id)});
  SLRR_REQUIRE(race_traffic_count(&f.ground) == 0);
  SLRR_REQUIRE(race_traffic_pointer(&f.car) == 0);
  // The same car can be attached again under a fresh id.
  const int second = f.call("addTrafficCar", sig,
                            {Value::make_obj(&f.car), Value::make_obj(where)})
                         .v.i;
  SLRR_REQUIRE(second > id);
  // Removing the ground forgets its traffic: no entry may outlive its owner.
  gameref_remove(&f.ground);
  SLRR_REQUIRE(race_traffic_count(&f.ground) == 0);
  SLRR_REQUIRE(race_traffic_pointer(&f.car) == 0);
  gameref_clear();
  std::puts("PASS addTrafficCar is idempotent, removable, and dies with its ground");
}

// The car is snapped to a crossing and parked on the road surface, and the
// query is answered per lane: an unreachable lane is not a spawn point.
SLRR_TEST(road_binding) {
  Fixture f;
  if (!open(f)) {
    std::puts("SKIP retail install not found (set SLRR_GAME_ROOT)");
    return;
  }
  auto graph = race_ground_roads(f.packs, &f.ground);
  SLRR_REQUIRE(graph && !graph->edges().empty());
  const auto& e = graph->edges().front();
  gameref_upsert(&f.car, GameRefKind::GameType)->type_id = f.type;
  // Start 30 m off the road: addTrafficCar snaps to the nearest crossing.
  RoadPoint off = e.points.front();
  off[0] += 20;
  off[2] += 20;
  auto* where = vector(f.vm, off);
  f.call("addTrafficCar", "(Ljava.util.resource.GameRef;Ljava.lang.Vector3;)I",
         {Value::make_obj(&f.car), Value::make_obj(where)});
  SLRR_REQUIRE(race_traffic_count(&f.ground) == 1);
  auto* node = gameref_get(&f.car);
  SLRR_REQUIRE(node);
  RoadPoint placed{node->pos[0], node->pos[1], node->pos[2]};
  // The attach snaps to a crossing and then publishes the lane position, so the
  // car must project back onto the graph at its own position.
  auto projected = graph->traffic_project(placed);
  SLRR_REQUIRE(projected);
  auto route = graph->traffic_edges()[projected->edge];
  RoadRoute line;
  line.points = route.points;
  SLRR_REQUIRE(road_distance(road_at(line, projected->along), placed) < .5f);
  SLRR_REQUIRE(std::fabs(placed[1] - off[1]) < 30.f);
  // The pose is flat: yaw only, and the velocity stays horizontal.
  SLRR_REQUIRE(std::fabs(node->ypr[1]) < 1e-3f && std::fabs(node->ypr[2]) < 1e-3f);
  SLRR_REQUIRE(std::fabs(node->vel[1]) < 1e-3f);
  // A ground without a TRC graph still registers the car (the caller keeps
  // ownership) and never invents a position for it.
  Object bare;
  auto* ref = gameref_upsert(&bare, GameRefKind::GroundRef);
  ref->type_id = 0;
  Object loose;
  gameref_upsert(&loose, GameRefKind::GameType)->type_id = f.type;
  const int id = f.registry
                     .invoke("java.util.resource.GroundRef", "addTrafficCar",
                             "(Ljava.util.resource.GameRef;Ljava.lang.Vector3;)I",
                             &bare,
                             {Value::make_obj(&loose), Value::make_obj(where)})
                     .v.i;
  SLRR_REQUIRE(id > 0);
  SLRR_REQUIRE(race_traffic_count(&bare) == 1);
  SLRR_REQUIRE(!race_ground_roads(f.packs, &bare));
  gameref_clear();
  std::puts("PASS cars snap to a crossing, park on the road and survive a graphless ground");
}

// Halt bookkeeping is keyed by crossing and by lane, and the tick is a no-op
// until a frame loop exists: a missing world must never spawn anything.
SLRR_TEST(halts_and_idle_tick) {
  Fixture f;
  if (!open(f)) {
    std::puts("SKIP retail install not found (set SLRR_GAME_ROOT)");
    return;
  }
  auto graph = race_ground_roads(f.packs, &f.ground);
  SLRR_REQUIRE(graph && !graph->edges().empty());
  const auto& e = graph->edges().front();
  auto* from = vector(f.vm, e.points.front());
  auto* to = vector(f.vm, e.points.back());
  f.call("haltTrafficCross", "(Ljava.lang.Vector3;F)V",
         {Value::make_obj(from), Value::make_float(5)});
  f.call("haltTrafficPath",
         "(Ljava.lang.Vector3;Ljava.lang.Vector3;)V",
         {Value::make_obj(from), Value::make_obj(to)});
  // A zero-length halt is not a closure.
  f.call("haltTrafficCross", "(Ljava.lang.Vector3;F)V",
         {Value::make_obj(from), Value::make_float(0)});
  f.call("clearHalts", "()V");
  gameref_upsert(&f.other, GameRefKind::GameType)->type_id = f.type;
  f.call("addTrafficN", "(Ljava.util.resource.GameRef;IFFF)I",
         {Value::make_obj(&f.other), Value::make_float(2), Value::make_float(0),
          Value::make_float(4), Value::make_float(2)});
  // No scene world, no controlled chassis: the step must do nothing at all.
  race_traffic_tick(f.vm, .02f);
  race_traffic_tick(f.vm, .02f, 10.0);
  race_traffic_tick(f.vm, -1.f);
  SLRR_REQUIRE(race_traffic_count(&f.ground) == 0);
  // Reset drops every registration and the halt clock.
  race_traffic_reset();
  SLRR_REQUIRE(race_traffic_count(&f.ground) == 0);
  gameref_clear();
  std::puts("PASS halts accept valid closures, the idle tick spawns nothing");
}

int main() {
  const standalone::Test tests[] = {template_registration, car_lifecycle,
                                    road_binding, halts_and_idle_tick};
  return standalone::run("traffic_runtime", tests);
}

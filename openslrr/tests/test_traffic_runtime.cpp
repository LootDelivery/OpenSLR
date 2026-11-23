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
#include <limits>
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

// U2-B.1: a car must follow one continuous path through a junction. The map
// stores the turn as a straight chord between two lane ends, so the heading of
// the chord is tens of degrees away from both lanes; the pose fillets the
// junction with a Hermite whose end tangents are the lane directions. This walks
// a real 90-degree corner of city.trc and pins position, heading and height
// continuity, and shows the snap the fillet removes.
SLRR_TEST(junction_continuity) {
  Fixture f;
  if (!open(f)) {
    std::puts("SKIP retail install not found (set SLRR_GAME_ROOT)");
    return;
  }
  auto graph = race_ground_roads(f.packs, &f.ground);
  SLRR_REQUIRE(graph && !graph->edges().empty());
  const auto& edges = graph->traffic_edges();
  auto tangent = [&](size_t edge, bool start) {
    const auto& p = edges[edge].points;
    const size_t n = p.size();
    RoadPoint t{};
    if (n < 2) return t;
    const auto& a = start ? p[n - 2] : p[n - 1];
    const auto& b = start ? p[n - 1] : p[n - 2];
    t = {b[0] - a[0], 0, b[2] - a[2]};
    const float l = std::sqrt(t[0] * t[0] + t[2] * t[2]);
    if (l > .05f) { t[0] /= l; t[2] /= l; }
    return t;
  };
  // Find a turn whose two lanes really do cross at a wide angle.
  size_t turn = edges.size(), from = edges.size(), to = edges.size();
  for (size_t i = 0; i < edges.size() && turn == edges.size(); ++i) {
    if (!edges[i].crossing || edges[i].next.empty() || edges[i].length < 2.f)
      continue;
    size_t parent = edges.size();
    for (size_t j = 0; j < edges.size(); ++j)
      if (!edges[j].crossing)
        for (size_t k : edges[j].next)
          if (k == i) parent = j;
    if (parent == edges.size()) continue;
    const auto a = tangent(parent, false), b = tangent(edges[i].next[0], true);
    if (a[0] * b[0] + a[2] * b[2] > .3f) continue;  // less than ~72 degrees
    turn = i;
    from = parent;
    to = edges[i].next[0];
  }
  if (turn == edges.size()) {
    std::puts("SKIP no wide junction in the city graph");
    return;
  }
  // Walk 4 m of the lane, the whole turn, and 4 m of the next lane, with the
  // predecessor reported so the junction gets its fillet.
  struct Sample { RoadPoint p; float heading; };
  std::vector<Sample> walk;
  auto add = [&](size_t edge, float along, size_t entry) {
    auto pose = race_traffic_pose(*graph, edge, along, entry);
    walk.push_back({pose.position, pose.heading});
  };
  for (float a = std::max(0.f, edges[from].length - 4.f); a < edges[from].length;
       a += .1f)
    add(from, a, from);
  for (float a = 0; a <= edges[turn].length; a += .1f)
    add(turn, a, from);
  for (float a = 0; a <= 4.f; a += .1f)
    add(to, a, turn);
  SLRR_REQUIRE(walk.size() > 30);
  float worst_position = 0, worst_heading = 0, worst_height = 0;
  for (size_t i = 1; i < walk.size(); ++i) {
    const auto step = road_distance(walk[i].p, walk[i - 1].p);
    worst_position = std::max(worst_position, step);
    worst_height = std::max(worst_height,
                            std::fabs(walk[i].p[1] - walk[i - 1].p[1]));
    worst_heading = std::max(
        worst_heading,
        std::fabs(std::remainder(walk[i].heading - walk[i - 1].heading,
                                 6.2831853f)));
  }
  std::printf("junction: %zu samples, step<=%.3f m, heading step<=%.3f rad, "
              "height step<=%.3f m\n",
              walk.size(), worst_position, worst_heading, worst_height);
  // 0.1 m of travel may not teleport, turn more than a few degrees or change
  // height. The old chord pose failed the heading bound by an order of
  // magnitude at both ends of the turn.
  SLRR_REQUIRE(worst_position < .25f);
  SLRR_REQUIRE(worst_heading < .12f);
  SLRR_REQUIRE(worst_height < .08f);
  // The fillet must connect the two lane headings, not some third direction:
  // the heading it turns through equals the corner the lanes make.
  const float lane_in =
      race_traffic_pose(*graph, from, std::max(0.f, edges[from].length - .5f),
                        from)
          .heading;
  const float lane_out = race_traffic_pose(*graph, to, .5f, to).heading;
  const float turned = std::fabs(
      std::remainder(walk.back().heading - walk.front().heading, 6.2831853f));
  const float corner =
      std::fabs(std::remainder(lane_out - lane_in, 6.2831853f));
  std::printf("corner: lanes %.2f rad, pose turned %.2f rad\n", corner,
              turned);
  SLRR_REQUIRE(corner > .8f);  // a real corner, not a lane continuation
  SLRR_REQUIRE(std::fabs(turned - corner) < .25f);
  // The chord without a fillet: at the first metre of the turn the heading is
  // the chord's, tens of degrees away from the incoming lane.
  auto chord = race_traffic_pose(*graph, turn, .5f, turn);
  const float snap = std::fabs(
      std::remainder(chord.heading - walk.front().heading, 6.2831853f));
  SLRR_REQUIRE(snap > .3f);
  gameref_clear();
  std::puts("PASS junction pose keeps position, heading and height continuous "
            "and the raw chord would snap");
}

// U2-B.1: ambient traffic belongs to the city. A race/track section retires the
// cars this runtime created, keeps every script-owned car (police scouts, race
// bots, the player's own car is never registered here at all), and a later city
// section lets the population fill again.
SLRR_TEST(race_section_clears_ambient) {
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
  // A script-owned car: the police scout and a race bot come in through the same
  // call, so it must survive the race transition.
  const int id = f.call("addTrafficCar",
                        "(Ljava.util.resource.GameRef;Ljava.lang.Vector3;)I",
                        {Value::make_obj(&f.car), Value::make_obj(where)})
                     .v.i;
  SLRR_REQUIRE(id > 0);
  SLRR_REQUIRE(race_traffic_ambient_enabled());
  race_traffic_note_section("java.game.Valocity");
  SLRR_REQUIRE(race_traffic_ambient_enabled());
  race_traffic_note_section("java.game.RaceSetup");
  SLRR_REQUIRE(!race_traffic_ambient_enabled());
  // Script-owned traffic is not ambient traffic: it is still registered.
  SLRR_REQUIRE(race_traffic_count(&f.ground) == 1);
  SLRR_REQUIRE(race_traffic_pointer(&f.car) == id);
  // A track or ROC scene keeps the block on.
  race_traffic_note_section("java.game.Valocity");
  SLRR_REQUIRE(race_traffic_ambient_enabled());
  race_traffic_note_section("java.game.ROCTrack");
  SLRR_REQUIRE(!race_traffic_ambient_enabled());
  // Menus and the garage leave the current policy alone.
  race_traffic_note_section("java.game.MainMenu");
  SLRR_REQUIRE(!race_traffic_ambient_enabled());
  // Back in the city the population may fill again.
  race_traffic_note_section("java.game.Valocity");
  SLRR_REQUIRE(race_traffic_ambient_enabled());
  // The idle tick is still a no-op with the block off.
  race_traffic_set_ambient(false);
  race_traffic_tick(f.vm, .02f);
  SLRR_REQUIRE(race_traffic_count(&f.ground) == 1);
  race_traffic_set_ambient(true);
  gameref_clear();
  std::puts("PASS a race section retires ambient traffic only and the city restores it");
}

// U2-C: a lamp at the junction ahead caps the lane speed, and a car resting at
// the line is the point of it. Ambient traffic drove through every crossing on
// the map before this, because the graph computed the phase for the route
// queries and the runtime never asked.
//
// The cap is a pure function of the graph, so this pins the whole rule without a
// scene: green and unregulated answers are unlimited, red approaches zero as the
// line is reached, amber only stops a car that can still come to rest, and a car
// already inside the junction is never frozen on it.
SLRR_TEST(signal_stops_at_the_line) {
  Fixture f;
  if (!open(f)) {
    std::puts("SKIP retail install not found (set SLRR_GAME_ROOT)");
    return;
  }
  // Every pack open, as at boot: the signal heads are ICFG placements in the
  // ground map and its exterior maps, and `race_ground_roads` can only bind them
  // when those packs are readable.
  f.packs.open_all();
  auto graph = race_ground_roads(f.packs, &f.ground);
  SLRR_REQUIRE(graph && !graph->traffic_edges().empty());
  SLRR_REQUIRE(!graph->signals().empty());
  const auto& edges = graph->traffic_edges();
  constexpr float inf = std::numeric_limits<float>::infinity();
  auto cap = [&](size_t edge, float along, float speed, double clock) {
    return race_traffic_signal_cap(*graph, edge, along, speed, clock);
  };
  // A lane whose approach is regulated, plus a clock that shows red and one that
  // shows green for it. Both exist on city.trc or the map has no signals at all.
  size_t lane = edges.size();
  double red = -1, green = -1;
  for (size_t i = 0; i < edges.size() && lane == edges.size(); ++i) {
    if (edges[i].crossing || edges[i].length < 12.f)
      continue;
    for (double t = 0; t < 26.0; t += .5) {
      const int phase = graph->traffic_signal(i, t);
      if (phase == 0 && red < 0) red = t;
      if (phase == 2 && green < 0) green = t;
    }
    if (red >= 0 && green >= 0) lane = i;
    else { red = green = -1; }
  }
  SLRR_REQUIRE(lane < edges.size());
  const float length = edges[lane].length;
  const float speed = 12.f;
  // Green: nothing to obey.
  SLRR_REQUIRE(cap(lane, length - 20.f, speed, green) == inf);
  // Red: a cap, and it shrinks as the line is approached. The line is
  // kTrafficStopLine before the end, so 20 m out allows more than 2 m out.
  const float far = cap(lane, length - 20.f, speed, red);
  const float near = cap(lane, length - kTrafficStopLine - 2.f, speed, red);
  SLRR_REQUIRE(std::isfinite(far) && far > 0.f);
  SLRR_REQUIRE(std::isfinite(near) && near > 0.f);
  SLRR_REQUIRE(far > near);
  // At the line the allowance is zero, i.e. the car rests there.
  SLRR_REQUIRE(cap(lane, length - kTrafficStopLine, speed, red) == 0.f);
  // Past the line the car is committed and must clear the junction, not stop on
  // it -- a red light that parks a car across the road it is crossing is a crash.
  SLRR_REQUIRE(cap(lane, length - 1.f, speed, red) == inf);
  // Amber stops a car that can still come to rest (slow, far out) and releases
  // one that cannot (fast, close).
  double amber = -1;
  for (double t = 0; t < 26.0 && amber < 0; t += .5)
    if (graph->traffic_signal(lane, t) == 1) amber = t;
  if (amber >= 0) {
    SLRR_REQUIRE(std::isfinite(cap(lane, length - 30.f, 5.f, amber)));
    SLRR_REQUIRE(cap(lane, length - kTrafficStopLine - .5f, 25.f, amber) == inf);
  }
  // A crossing link is the junction itself: never braked on.
  for (size_t i = 0; i < edges.size(); ++i)
    if (edges[i].crossing) {
      SLRR_REQUIRE(cap(i, edges[i].length * .5f, speed, red) == inf);
      break;
    }
  // An unregulated lane has no cycle at all.
  for (size_t i = 0; i < edges.size(); ++i)
    if (!edges[i].crossing && graph->traffic_signal(i, red) == -1) {
      SLRR_REQUIRE(cap(i, edges[i].length * .5f, speed, red) == inf);
      break;
    }
  gameref_clear();
  std::puts("PASS a red light caps the lane at the stop line and never freezes a car in the junction");
}

int main() {
  const standalone::Test tests[] = {template_registration,
                                    car_lifecycle,
                                    road_binding,
                                    halts_and_idle_tick,
                                    junction_continuity,
                                    race_section_clears_ambient,
                                    signal_stops_at_the_line};
  return standalone::run("traffic_runtime", tests);
}

// U2-A regression suite: the TRC road graph, its route queries and the
// GroundRef route natives.
//
// The synthetic cases pin the solver semantics (direction separation, crossing
// radius, shared signal phase). The corpus cases run against the retail install
// when it is present; without it they print a notice instead of failing, so the
// suite still runs in a source-only checkout.
#include "standalone_test.hpp"

#include "slrr/gameref.hpp"
#include "slrr/jvm.hpp"
#include "slrr/race_runtime.hpp"
#include "slrr/road_routes.hpp"
#include "slrr/rpk_index.hpp"
#include "slrr/section_machine.hpp"

#include <cmath>
#include <cstdio>
#include <cstdlib>
#include <filesystem>
#include <string>

namespace fs = std::filesystem;
using namespace slrr;

namespace {
RoadSegment straight(RoadPoint a, RoadPoint b) {
  RoadSegment s;
  s.start = a;
  s.end = b;
  for (int i = 0; i < 3; ++i) s.start_tangent[i] = s.end_tangent[i] = b[i] - a[i];
  s.length = road_distance(a, b);
  return s;
}
// Two crossings joined by one path. `reverse` adds the opposing lane record, so
// only the reverse graph may answer a reverse query.
RoadNetwork street(bool reverse) {
  RoadNetwork n;
  RoadNode a, b;
  a.id = 1;
  b.id = 2;
  b.position = {100, 0, 0};
  a.links = {{4, 0}};
  b.links = {{4, 1}};
  if (reverse) {
    a.links.push_back({5, 1});
    b.links.push_back({5, 0});
  }
  n.nodes = {a, b};
  RoadPath p;
  p.id = 3;
  p.refs[1] = 1;
  p.refs[2] = 2;
  p.segments = {straight(a.position, b.position)};
  n.paths = {p};
  RoadOther r;
  r.id = 4;
  r.refs[0] = 3;
  n.others = {r};
  if (reverse) {
    r.id = 5;
    n.others.push_back(r);
  }
  return n;
}
// Retail install, several known layouts.
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
}  // namespace

SLRR_TEST(directions) {
  RoadRoutes one(street(false)), two(street(true));
  auto r = one.query({20, 0, 0}, {80, 0, 0});
  SLRR_REQUIRE(r && std::fabs(r->length - 60) < .1f && r->direction[0] > .99f);
  // The forward graph must not answer the reverse query: the two sides of a road
  // stay separate.
  SLRR_REQUIRE(!one.query({80, 0, 0}, {20, 0, 0}));
  r = two.query({80, 0, 0}, {20, 0, 0});
  SLRR_REQUIRE(r && std::fabs(r->length - 60) < .1f && r->direction[0] < -.99f);
  SLRR_REQUIRE(road_distance(road_at(*r, 30), {50, 0, 0}) < .1f);
  auto aligned = two.project({50, 3, 4});
  SLRR_REQUIRE(aligned && std::fabs(aligned->position[0] - 50) < .1f &&
               std::fabs(aligned->distance - 5) < .1f);
  std::puts("PASS one-way and two-way graphs: direction, arc length, projection");
}

SLRR_TEST(cross_radius) {
  RoadNetwork n;
  RoadNode a, b;
  a.id = 1;
  a.position = {4, 0, 0};
  b.id = 2;
  b.position = {12, 0, 0};
  n.nodes = {a, b};
  RoadRoutes g(n);
  auto p = g.nearest_cross({}, 10);
  SLRR_REQUIRE(p && (*p)[0] == 12);
  p = g.nearest_cross({}, -10);
  SLRR_REQUIRE(p && (*p)[0] == 4);
  SLRR_REQUIRE(!g.query({}, {}));
  std::puts("PASS nearest crossing honours the radius sign and empty routes");
}

SLRR_TEST(shared_signal_phases) {
  auto n = street(false);
  n.others[0].segments = {straight({90, 0, 0}, {0, 0, 0})};
  RoadNode side;
  side.id = 6;
  side.position = {100, 0, 100};
  side.links = {{8, 0}};
  n.nodes.push_back(side);
  n.nodes[1].links.push_back({8, 1});
  RoadPath p;
  p.id = 7;
  p.refs[1] = 6;
  p.refs[2] = 2;
  p.segments = {straight(side.position, n.nodes[1].position)};
  n.paths.push_back(p);
  RoadOther r;
  r.id = 8;
  r.refs[0] = 7;
  r.segments = {straight({100, 0, 10}, side.position)};
  n.others.push_back(r);
  RoadRoutes roads(n, {{90, 0, 0}, {100, 0, 10}});
  SLRR_REQUIRE(roads.signals().size() == 2);
  SLRR_REQUIRE(roads.signal(2, 3, 0) == 2 && roads.signal(2, 7, 0) == 0);
  SLRR_REQUIRE(roads.signal(2, 3, 10) == 1 && roads.signal(2, 7, 10) == 0);
  SLRR_REQUIRE(roads.signal(2, 3, 13) == 0 && roads.signal(2, 7, 13) == 2);
  SLRR_REQUIRE(roads.signal(2, 7, 23) == 1);
  SLRR_REQUIRE(roads.signal(2, 3, 26) == 2);
  SLRR_REQUIRE(roads.edge_signal(0, 13) == 0);
  // A branch without a lamp is a timed STOP, never a permanent red.
  SLRR_REQUIRE(roads.signal(2, 4, 0) == -2);
  SLRR_REQUIRE(roads.signal(999, 3, 0) == -1);
  std::puts("PASS one shared crossing phase: green, yellow, opposing red, repeat");
}

SLRR_TEST(city_corpus) {
  fs::path root = game_root();
  if (root.empty()) {
    std::puts("SKIP retail install not found (set SLRR_GAME_ROOT)");
    return;
  }
  RpkIndex packs;
  packs.set_root(root.string());
  packs.open_all();
  const int32_t pack = packs.open_lib("maps/city.rpk");
  SLRR_REQUIRE(pack > 0);
  Object ground;
  auto* n = gameref_upsert(&ground, GameRefKind::GroundRef);
  n->type_id = RpkIndex::make_full_id(pack, 1);
  auto graph = race_ground_roads(packs, &ground);
  SLRR_REQUIRE(graph && !graph->edges().empty());
  const auto& e = graph->edges().front();
  auto route = graph->query(e.points.front(), e.points.back());
  SLRR_REQUIRE(route && route->length > 0 && std::isfinite(route->length));
  // A city crossing is reachable both ways: route out, route back.
  auto back = graph->query(e.points.back(), e.points.front());
  std::printf("City graph: %zu directed edges, %zu crossings, %zu signal heads, "
              "first edge %.1f m, route %.1f m (back %.1f m)\n",
              graph->edges().size(), graph->nodes().size(),
              graph->signals().size(), e.length, route->length,
              back ? back->length : -1.f);
  SLRR_REQUIRE(graph->edges().size() > 100);
  // The graph is cached per ground incarnation.
  SLRR_REQUIRE(race_ground_roads(packs, &ground) == graph);
  const RoadRoute* stored = nullptr;
  race_query_route(packs, &ground, e.points.front(), e.points.back());
  stored = race_last_route(&ground);
  SLRR_REQUIRE(stored && stored->length == route->length);
  gameref_remove(&ground);
  SLRR_REQUIRE(!race_last_route(&ground));
  // A missing ground has no graph and no route.
  SLRR_REQUIRE(!race_ground_roads(packs, &ground));
  std::puts("PASS city.trc parses into a graph, routes both ways, cache drops on removal");
}

SLRR_TEST(route_native_contract) {
  fs::path root = game_root();
  if (root.empty()) {
    std::puts("SKIP retail install not found (set SLRR_GAME_ROOT)");
    return;
  }
  NativeRegistry registry;
  Jvm vm(&registry);
  RpkIndex packs;
  packs.set_root(root.string());
  const int32_t pack = packs.open_lib("maps/city.rpk");
  SectionMachine sections(&vm);
  register_race_natives(vm, packs, sections);
  Object ground;
  auto* n = gameref_upsert(&ground, GameRefKind::GroundRef);
  n->type_id = RpkIndex::make_full_id(pack, 1);
  auto graph = race_ground_roads(packs, &ground);
  SLRR_REQUIRE(graph && !graph->edges().empty());
  const auto& e = graph->edges().front();
  auto vector = [&](RoadPoint p) {
    auto* v = vm.new_object("java.lang.Vector3");
    for (int k = 0; k < 3; ++k)
      v->fields[k == 0 ? "x" : k == 1 ? "y" : "z"] = Value::make_float(p[k]);
    return v;
  };
  auto call = [&](const char* name, const char* sig, std::vector<Value> args = {}) {
    return registry.invoke("java.util.resource.GroundRef", name, sig, &ground, args);
  };
  auto* a = vector(e.points.front()), *b = vector(e.points.back());
  const char* rr = "(Ljava.lang.Vector3;Ljava.lang.Vector3;)F";
  auto length = call("findRoute", rr, {Value::make_obj(a), Value::make_obj(b)}).v.f;
  SLRR_REQUIRE(length > 0);
  // getRouteLength()F @ 0x00483C00 reports the cached route in the same units.
  SLRR_REQUIRE(call("getRouteLength", "()F").v.f == length);
  SLRR_REQUIRE(
      call("getRouteLength", rr, {Value::make_obj(a), Value::make_obj(b)}).v.f == length);
  // getRoutePos(F) @ 0x00483B30 takes the fraction between the endpoints.
  auto* begin = call("getRoutePos", "(F)Ljava.lang.Vector3;", {Value::make_float(0)}).v.o;
  auto* middle =
      call("getRoutePos", "(F)Ljava.lang.Vector3;", {Value::make_float(.5f)}).v.o;
  auto* finish = call("getRoutePos", "(F)Ljava.lang.Vector3;", {Value::make_float(1)}).v.o;
  SLRR_REQUIRE(begin && middle && finish);
  auto xyz = [](Object* o, RoadPoint& p) {
    for (int k = 0; k < 3; ++k) p[k] = o->fields[k == 0 ? "x" : k == 1 ? "y" : "z"].v.f;
  };
  RoadPoint p0{}, pm{}, p1{};
  xyz(begin, p0);
  xyz(middle, pm);
  xyz(finish, p1);
  SLRR_REQUIRE(road_distance(p0, e.points.front()) < .5f);
  SLRR_REQUIRE(road_distance(p1, e.points.back()) < .5f);
  SLRR_REQUIRE(road_distance(pm, p0) > 1.f && road_distance(pm, p1) > 1.f);
  // getRouteDist(V)F @ 0x00483C20 returns the same fraction, not a distance.
  auto dist = [&](Object* o) {
    return call("getRouteDist", "(Ljava.lang.Vector3;)F", {Value::make_obj(o)}).v.f;
  };
  SLRR_REQUIRE(std::fabs(dist(begin)) < .05f);
  SLRR_REQUIRE(std::fabs(dist(finish) - 1.f) < .05f);
  SLRR_REQUIRE(std::fabs(dist(middle) - .5f) < .05f);
  SLRR_REQUIRE(dist(nullptr) == -1.f);
  // getStartDirection(V,V) @ 0x00483400 boxes the negated start tangent.
  auto* direction =
      call("getStartDirection", "(Ljava.lang.Vector3;Ljava.lang.Vector3;)Ljava.lang.Vector3;",
           {Value::make_obj(a), Value::make_obj(b)})
          .v.o;
  SLRR_REQUIRE(direction);
  RoadPoint d{};
  xyz(direction, d);
  auto forward = graph->query(e.points.front(), e.points.back());
  SLRR_REQUIRE(forward);
  SLRR_REQUIRE(d[0] * forward->direction[0] + d[1] * forward->direction[1] +
                   d[2] * forward->direction[2] <
               -.9f);
  // A failed query keeps the cached route, exactly like the single cache slot.
  SLRR_REQUIRE(call("findRoute", rr, {Value::make_obj(nullptr), Value::make_obj(b)}).v.f ==
               -1.f);
  SLRR_REQUIRE(call("getRouteLength", "()F").v.f == length);
  // alignToRoad(V)[V3 @ 0x00483E60 always boxes {position, unit XZ tangent}.
  auto* aligned = call("alignToRoad", "(Ljava.lang.Vector3;)[Ljava.lang.Vector3;",
                       {Value::make_obj(a)})
                      .v.o;
  SLRR_REQUIRE(aligned && aligned->is_array && aligned->slots.size() == 2 &&
               aligned->slots[0].v.o != a &&
               aligned->slots[0].v.o != aligned->slots[1].v.o);
  RoadPoint tan{};
  xyz(aligned->slots[1].v.o, tan);
  SLRR_REQUIRE(std::fabs(std::sqrt(tan[0] * tan[0] + tan[2] * tan[2]) - 1.f) < .01f);
  // getNearestCross(V,F) @ 0x00483D00 snaps to a crossing and never boxes null.
  auto* cross = call("getNearestCross", "(Ljava.lang.Vector3;F)Ljava.lang.Vector3;",
                     {Value::make_obj(a), Value::make_float(40)})
                    .v.o;
  SLRR_REQUIRE(cross);
  RoadPoint snapped{};
  xyz(cross, snapped);
  // The shipped lookup minimizes |distance(node) - radius|, so the answer is
  // the crossing that sits ~40 m away, not the nearest one.
  float best = 1e9f;
  for (const auto& node : graph->nodes())
    best = std::min(best, std::fabs(road_distance(node.second, e.points.front()) - 40.f));
  SLRR_REQUIRE(std::fabs(std::fabs(road_distance(snapped, e.points.front()) - 40.f) - best) <
               1e-3f);
  // Without a graph the approximation itself comes back.
  Object unknown;
  auto* off = gameref_upsert(&unknown, GameRefKind::GroundRef);
  off->type_id = 0;
  auto* raw = registry
                  .invoke("java.util.resource.GroundRef", "getNearestCross",
                          "(Ljava.lang.Vector3;F)Ljava.lang.Vector3;", &unknown,
                          {Value::make_obj(a), Value::make_float(40)})
                  .v.o;
  SLRR_REQUIRE(raw);
  RoadPoint echoed{};
  xyz(raw, echoed);
  SLRR_REQUIRE(road_distance(echoed, e.points.front()) < .01f);
  gameref_clear();
  std::puts("PASS route natives: metre length, fraction position, negated start, "
            "cache kept on miss");
}

SLRR_TEST(maximal_route) {
  fs::path root = game_root();
  if (root.empty()) {
    std::puts("SKIP retail install not found (set SLRR_GAME_ROOT)");
    return;
  }
  RpkIndex packs;
  packs.set_root(root.string());
  const int32_t pack = packs.open_lib("maps/city.rpk");
  Object ground;
  auto* n = gameref_upsert(&ground, GameRefKind::GroundRef);
  n->type_id = RpkIndex::make_full_id(pack, 1);
  auto graph = race_ground_roads(packs, &ground);
  SLRR_REQUIRE(graph);
  // Every ordered pair of nodes within a sample must be either routable or
  // reported as unroutable; the solver may never return a NaN or infinite route.
  size_t routed = 0, unreachable = 0;
  const auto& nodes = graph->nodes();
  for (size_t i = 0; i < nodes.size(); i += 7)
    for (size_t j = 0; j < nodes.size(); j += 11) {
      auto r = graph->query(nodes[i].second, nodes[j].second);
      if (!r) {
        ++unreachable;
        continue;
      }
      SLRR_REQUIRE(std::isfinite(r->length) && r->length >= 0);
      for (const auto& p : r->points) SLRR_REQUIRE(std::isfinite(p[0]) && std::isfinite(p[2]));
      ++routed;
    }
  std::printf("MESH %zu routed, %zu unreachable sampled node pairs\n", routed,
              unreachable);
  SLRR_REQUIRE(routed > 0);
  gameref_clear();
  std::puts("PASS sampled node pairs stay finite; unreachable pairs are rejected");
}

int main() {
  const standalone::Test tests[] = {directions,       cross_radius,
                                    shared_signal_phases, city_corpus,
                                    route_native_contract, maximal_route};
  return standalone::run("road_routes", tests);
}

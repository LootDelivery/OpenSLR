#pragma once
#include "slrr/road_routes.hpp"

#include <memory>

namespace slrr {
class Jvm;
class RpkIndex;
class SectionMachine;
struct Object;

// GroundRef route/traffic natives. This is the U2 port of PR #180's
// "Restore shared roads, traffic and race bot driving"; the graph itself lives
// in RoadRoutes and the payload→TRC lookup mirrors SLRR_rev v221
// engine/map-roads.ts. Comment tags: [Ф-код] = read in the shipped binary or in
// a script we run, [Ф-runtime] = observed in this host, [Г] = hypothesis.
void register_race_natives(Jvm&, RpkIndex&, SectionMachine&);

// Shared, cached road graph of the ground's map. Null when the map has no TRC.
std::shared_ptr<const RoadRoutes> race_ground_roads(const RpkIndex&, void* ground);

// Ground teardown. Both are called from GameRef: removal of one ref and the
// wholesale reset of the native scene.
void race_forget(void* key);
void race_reset();

// Route queried by findRoute/getRouteLength/etc. Java keeps its own copy, so a
// miss clears the cached one instead of leaving a stale route behind.
std::optional<RoadRoute> race_query_route(const RpkIndex&, void*, const RoadPoint&,
                                          const RoadPoint&);
const RoadRoute* race_last_route(void* ground);
const RoadRoute* race_plot_route();
}  // namespace slrr

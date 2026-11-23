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

// --- U2-B ambient traffic ---------------------------------------------------
// GroundRef traffic natives: addTrafficN/P/Car, remTrafficCar,
// notifyTrafficCar, delTraffic, setTrafficCarBehaviour and the halt calls.
// Every car the traffic runtime creates is driven through external motion, so
// the physics step never simulates it; the runtime owns the pose.
void register_traffic_natives(Jvm&, RpkIndex&, SectionMachine&);
// One step. `seconds` is the bounded, pause-aware simulation delta; the halt
// clock advances by the same amount, or by `clock_seconds` when that is
// non-negative (additive, so a caller with its own time base stays consistent).
void race_traffic_tick(Jvm&, float seconds, double clock_seconds = -1);

// U2-C: the speed a lamp allows a car on `edge` standing `along` metres into
// it, at `speed`, on the shared phase clock. A finite answer is a cap; the car
// brakes on the same ramp it uses for lane spacing and comes to rest at the stop
// line, `kTrafficStopLine` metres before the junction. Infinity means no lamp
// applies -- unregulated approach, green, or a car that is already committed and
// must clear the junction instead of stopping on it.
//
// The phases are the graph's (`RoadRoutes::signal`): -1 unregulated, -2 a
// regulated approach whose lamp is missing, 0 red, 1 amber, 2 green. -2 has no
// lamp to obey, so it is a timed stop on the same 13 s cycle and never a
// permanent red; that policy is the host's, not the original's.
float race_traffic_signal_cap(const RoadRoutes&, size_t edge, float along,
                              float speed, double clock_seconds);
// The stop line used above, exported for tests and for the native trace.
constexpr float kTrafficStopLine = 5.f;

// Road closures (`haltTrafficPath`/`haltTrafficCross`/`clearHalts`).
void race_halt_path(const RpkIndex&, void*, const RoadPoint&, const RoadPoint&);
void race_halt_cross(const RpkIndex&, void*, const RoadPoint&, float seconds);
void race_clear_halts(void*);
// Ambient traffic is a city feature: the race scene retires the cars this
// module created and stops refilling, and the city section restores it. Called
// with the class name of the active section on every section change; only the
// ambient cars are touched, never a script-owned car (police, race bot).
void race_traffic_note_section(const std::string& section);
void race_traffic_set_ambient(bool enabled);
bool race_traffic_ambient_enabled();
// Lifecycle, called from GameRef removal and from the wholesale scene reset.
void race_traffic_forget(void*);
void race_traffic_reset();
// The world pose a car standing `along` metres into `edge` must have. `entry` is the
// edge it came from, which makes the pose fillet the junction instead of snapping to
// the new lane chord; pass it for every frame after a transition.
struct TrafficPose {
  RoadPoint position{};
  float heading = 0.f;  // world yaw, radians
};
TrafficPose race_traffic_pose(const RoadRoutes&, size_t edge, float along,
                             size_t entry = static_cast<size_t>(-1));
// Benchmark autodrive (--bench-route): a steering command in [-1, 1] that keeps
// the player car in its lane, computed from where the car stands on the same
// directed graph the traffic uses. Positive is left, like the stock axis.
// False when the car is not on a lane, in which case the caller leaves the
// steering alone. Measurement support only: nothing in the game calls it.
bool race_traffic_steer(float speed, float* steer);
// The traffic id a car is registered under, 0 when it is not traffic.
int race_traffic_pointer(Object*);
// Diagnostics: resident count and the nth resident of one ground.
int race_traffic_count(void* ground);
Object* race_traffic_car_at(void* ground, int index);
}  // namespace slrr

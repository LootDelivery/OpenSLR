#pragma once

// City traffic streaming (U2-B.2).
//
// U2-B decided where a car may appear with one distance band around the player
// and forbade the forward cone below 260 m, which left the road ahead empty: the
// band's far end was 240 m, so the cone rule excluded every spot it was given.
// A ring is also the wrong shape for a road network, because what matters is
// how much lane the player can reach, not how far a point is as the crow flies.
//
// This module is the pure decision half of the replacement:
//
//   * `stream_target_count` turns the user's density slider into a number of
//     cars for the lane length that is actually around the player, so the
//     population follows the network instead of a fixed twenty;
//   * `stream_pick_spot` chooses one lane position for a new car, from a view
//     the caller built, honouring spacing, junctions, the player's speed and
//     the lane's own occupancy.
//
// Nothing here touches the JVM, GameRefs or the scene, so the whole distribution
// policy is testable without a running game; `traffic_runtime.cpp` builds the
// view and performs the placement.

#include "slrr/road_routes.hpp"

#include <cstdint>
#include <vector>

namespace slrr {

// One directed lane as the scheduler sees it.
struct StreamLane {
  size_t edge = 0;
  float length = 0;
  // Distance from the player to the nearest point of the lane, and whether the
  // lane lies mostly ahead of the player's heading.
  float distance = 0;
  float dot = 0;
  // Coarse copy of the lane's polyline, uniformly spaced in arc length, so a
  // candidate position can be measured against the player for real. A curved
  // lane that doubles back is otherwise much further away than its nearest
  // point suggests, and cars were being created past the radius that way.
  std::vector<RoadPoint> samples;
  // A junction chord is never a place for a car to appear.
  bool crossing = false;
  // Lane positions of the cars already standing on this lane, in metres from
  // its start. Kept sorted by the caller; only a handful per lane.
  std::vector<float> residents;
  // Reachable from the player's current lane by driving forward, ignoring
  // blocked lanes. A geometrically close lane behind a wall is not a candidate.
  bool reachable = true;
};

struct StreamView {
  RoadPoint player{};
  float heading = 0;  // world yaw, radians
  float speed = 0;    // m/s, along the heading
  // Lane length that is inside the spawn radius, in metres. Not the length of
  // `lanes`, which the caller may truncate when it has no room for more.
  float lane_metres = 0;
  std::vector<StreamLane> lanes;
};

struct StreamRules {
  // Cars are never created closer than this to a car that already exists, and
  // never closer than the player's own stopping distance grows to.
  float min_gap = 26.f;
  // Pop-in bubble: a spot nearer than `speed * headway + min_bubble` in front of
  // the player is refused, because the player reaches it before a driver could
  // react. Standing still it collapses to `min_bubble`, so a parked player still
  // gets traffic around them instead of an empty street.
  float headway = 1.4f;
  float min_bubble = 45.f;
  float max_bubble = 110.f;
  // The player has to be unable to see a car appear. A three zone clearance
  // replaces the old "bubble, but only strictly ahead": a spot on a crossing
  // street beside the player has `dot` near zero, so the bubble never covered
  // it and cars materialised ten metres away in plain sight.
  //
  //   ahead  (dot >  0.35)  at least `max(front_clear, bubble)` metres away
  //   side   (|dot| <= .35) at least `side_clear`
  //   behind (dot < -0.35)  at least `rear_clear`
  //
  // The bubble still grows with speed inside the front zone, so a fast player
  // gets his spawns further out than a crawling one.
  float front_clear = 160.f;
  float side_clear = 105.f;
  float rear_clear = 60.f;
  // Absolute floor, whatever the zone says: never on the bumper, never beside
  // the door.
  float min_player_distance = 45.f;
  // Spawn and retire distances form the hysteresis band: a car created inside
  // `spawn_radius` is not retired until it leaves `retire_radius`.
  float spawn_radius = 260.f;
  float retire_radius = 330.f;
  // Desired spacing between cars, linearly from the slider: a light city keeps
  // ~200 m of lane per car, the shipped maximum ~26 m, so the streets around
  // the player queue up instead of holding one car per block. The shipped
  // slider sits at 0.2.
  float spacing_min = 200.f;  // at density 0
  float spacing_max = 26.f;   // at density 1
  // Hard guard: the most cars this system will ever own, from the slider. A car
  // costs about thirteen draw calls, so the ceiling is a budget as much as a
  // population: eighty packed cars look denser than a hundred spread over every
  // side street and cost a fifth less.
  int budget = 80;
  // A lane may not hold more than one car per this many metres.
  float per_car_metres = 32.f;
  // Keep clear of the ends, where the lane meets a junction.
  float end_clearance = 8.f;
  float min_lane_length = 15.f;
};

// Cars the network around the player should hold at this density.
int stream_target_count(const StreamView& view, const StreamRules& rules,
                        float density);

// One place to create a car, or `valid == false` when the network has no room.
struct StreamSpot {
  size_t edge = 0;
  float along = 0;
  bool valid = false;
  // Why the last pick failed, for tests and the runtime trace.
  const char* refused = "";
};

// Choose a lane position. `random` is any 32 bit value; the policy consumes it
// as a sequence, so the caller stays deterministic under a fixed seed. Callers
// must add the chosen car to `view` before asking again, or a repeated call can
// return the same spot.
//
// Lanes are expected nearest first (the runtime sorts by `StreamLane::distance`)
// and the draw is biased towards the front of the list, so a limited budget
// packs the streets around the player before it reaches across the district.
// Without that bias a hundred cars were spread over seven kilometres of
// reachable lane, which is why the slider felt like it did nothing.
StreamSpot stream_pick_spot(const StreamView& view, const StreamRules& rules,
                            uint32_t& random);

// Position of `along` on the lane's coarse polyline.
RoadPoint stream_lane_point(const StreamLane& lane, float along);
// Real distance from the player to a lane position.
float stream_spot_distance(const StreamLane& lane, float along,
                           const RoadPoint& player);

// Spacing the slider asks for, and the bubble in front of the player. Exposed
// so the trace can print what the policy decided without duplicating the maths.
float stream_spacing(float density, const StreamRules& rules);
float stream_bubble(const StreamView& view, const StreamRules& rules);
// How far a spot with this direction (`dot` against the player's heading) has to
// be from the player before a car may be created there.
float stream_min_spawn_distance(float dot, const StreamView& view,
                                const StreamRules& rules);

}  // namespace slrr

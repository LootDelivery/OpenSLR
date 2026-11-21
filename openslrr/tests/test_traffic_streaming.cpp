// U2-B.2 regression suite: the traffic streaming scheduler.
//
// The scheduler is the pure decision half of the streaming rewrite, so it can be
// driven here with synthetic lanes: where a car may appear, how many the network
// should hold, and what the player's speed does to the pop-in bubble. The
// runtime that builds the view and creates the cars is covered by the in-game
// runs in docs/UPSTREAM_INTEGRATION.md.
#include "standalone_test.hpp"

#include "slrr/traffic_streaming.hpp"

#include <cmath>
#include <cstdio>
#include <string>
#include <vector>

using namespace slrr;

namespace {

// A straight lane: `distance` metres to the side of the player, starting
// `start` metres along the player's forward axis and `length` metres long, in
// the given direction (`dot` -1 behind, +1 ahead).
StreamLane straight(size_t edge, float length, float start, float distance,
                    float dot, bool crossing = false, bool reachable = true) {
  StreamLane lane;
  lane.edge = edge;
  lane.length = length;
  lane.distance = distance;
  lane.dot = dot;
  lane.crossing = crossing;
  lane.reachable = reachable;
  const int count = std::max(2, int(length / 16.f) + 1);
  for (int i = 0; i < count; ++i) {
    const float along = length * float(i) / float(count - 1);
    // The player stands at the origin looking down -Z. A lane ahead runs away
    // from the player, one behind runs past them.
    const float z = dot >= 0.f ? -(start + along) : (start + along);
    lane.samples.push_back(RoadPoint{distance, 0, z});
  }
  return lane;
}

// A street crossing the player's axis: `offset` metres to the side, running
// perpendicular, `start` metres ahead of the player's line.
StreamLane crossing_street(size_t edge, float length, float start, float offset,
                           float dot = 0.f) {
  StreamLane lane;
  lane.edge = edge;
  lane.length = length;
  lane.distance = offset;
  lane.dot = dot;
  const int count = std::max(2, int(length / 16.f) + 1);
  for (int i = 0; i < count; ++i)
    lane.samples.push_back(
        RoadPoint{offset + length * float(i) / float(count - 1), 0, -start});
  return lane;
}

// A chase camera behind the player, looking down the car's nose, with the cone
// the engine reports for the driving camera (half angle 45 degrees, widened by
// the six degree margin the scheduler adds).
void chase_camera(StreamView& view, float range = 300.f) {
  view.eye[0] = 0.f;
  view.eye[1] = 2.6f;
  view.eye[2] = 6.f;  // the player looks down -Z, so the camera sits at +Z
  view.look[0] = 0.f;
  view.look[1] = 0.f;
  view.look[2] = -1.f;
  view.view_cos = std::cos(51.f * 3.14159265f / 180.f);
  view.view_range = range;
}

// Most cases below are about spacing, junctions or the target, not about what
// the camera can see, so they switch the cone off and the visibility rule is
// tested on its own in `nothing_appears_in_plain_sight`.
StreamView view_with(const std::vector<StreamLane>& lanes, float speed = 0.f,
                     float heading = 0.f) {
  StreamView view;
  view.player = RoadPoint{0, 0, 0};
  view.heading = heading;
  view.speed = speed;
  view.view_cos = 2.f;  // no spot can ever satisfy sin < cos > 1, so the cone is off
  view.lanes = lanes;
  for (const auto& lane : lanes)
    if (!lane.crossing) view.lane_metres += lane.length;
  return view;
}

StreamRules rules() {
  StreamRules r;
  r.budget = 100;
  return r;
}

}  // namespace

// 1. A player standing on a road gets traffic in every direction: the network
//    decides, not a ring around the player.
SLRR_TEST(standing_player_is_populated_all_around) {
  const auto r = rules();
  // The player stands in the middle of a street, with a cross street 60 m to
  // the side: a real junction, not three lanes that happen to start nearby.
  StreamView view = view_with({straight(1, 600, 0, 0, 1),            // ahead
                               straight(2, 600, 0, 0, -1),           // behind
                               crossing_street(3, 400, 0, 60, .2f)}); // side
  uint32_t random = 0x12345;
  int ahead = 0, behind = 0, side = 0;
  // The lanes fill up (one car per `per_car_metres`), so the loop stops accepting
  // spots long before the last draw; what matters is the spread.
  for (int i = 0; i < 200; ++i) {
    StreamSpot spot = stream_pick_spot(view, r, random);
    if (!spot.valid) continue;
    for (auto& lane : view.lanes)
      if (lane.edge == spot.edge) {
        lane.residents.push_back(spot.along);
        if (lane.dot > .5f) ++ahead;
        else if (lane.dot < -.5f) ++behind;
        else ++side;
      }
  }
  // Every direction has to be used, and none of the three may be starved just
  // because the scheduler draws towards the nearest lane.
  SLRR_REQUIRE(ahead >= 2);
  SLRR_REQUIRE(behind >= 3);
  SLRR_REQUIRE(side >= 2);
  std::puts("PASS a standing player gets traffic ahead, behind and beside");
}

// 2. The forward sector is not forbidden: a moving player keeps getting cars in
//    front of them, which the U2-B.1 policy never did.
SLRR_TEST(driving_fills_the_road_ahead) {
  const auto r = rules();
  StreamView view = view_with({straight(1, 600, 0, 0, 1), straight(2, 600, 0, 0, -1)},
                              /*speed=*/15.f);
  uint32_t random = 0x9e3779b9;
  int ahead = 0, behind = 0;
  std::string refusals = [&] {
    std::string report;
    uint32_t probe = 0x5150;
    StreamView copy = view;
    for (int i = 0; i < 20; ++i) {
      StreamSpot spot = stream_pick_spot(copy, r, probe);
      report += spot.valid ? "ok " : spot.refused;
      report += ' ';
      if (spot.valid)
        for (auto& lane : copy.lanes)
          if (lane.edge == spot.edge) lane.residents.push_back(spot.along);
    }
    return report;
  }();
  for (int i = 0; i < 100; ++i) {
    StreamSpot spot = stream_pick_spot(view, r, random);
    if (!spot.valid) continue;
    for (auto& lane : view.lanes)
      if (lane.edge == spot.edge) {
        lane.residents.push_back(spot.along);
        if (lane.dot > .5f) ++ahead; else ++behind;
      }
  }
  std::printf("  ahead=%d behind=%d (each lane fills to the spawn radius)\n", ahead,
              behind);
  (void)refusals;
  // The usable band ahead runs from the front clearance (160 m) to the spawn
  // radius (260 m), which leaves room for a few cars at the minimum gap; the
  // point is that the sector is filled at all, not that it is unbounded.
  SLRR_REQUIRE(ahead >= 2);
  std::puts("PASS traffic keeps being created ahead of a moving player");
}

// 3. A fast player must not get a car dropped into their stopping distance: the
//    bubble grows with speed and nothing appears ahead inside it.
SLRR_TEST(fast_player_keeps_headway) {
  StreamRules r = rules();
  StreamView view = view_with({straight(1, 600, 0, 0, 1)}, /*speed=*/30.f);
  const float bubble = stream_bubble(view, r);
  SLRR_REQUIRE(bubble > 80.f);
  const float clearance = std::max(bubble, r.front_clear);
  uint32_t random = 0xabcdef01;
  int picked = 0;
  for (int i = 0; i < 50; ++i) {
    StreamSpot spot = stream_pick_spot(view, r, random);
    if (!spot.valid) continue;
    ++picked;
    const float distance = stream_spot_distance(view.lanes[0], spot.along, view.player);
    SLRR_REQUIRE(distance >= clearance);
    view.lanes[0].residents.push_back(spot.along);
  }
  SLRR_REQUIRE(picked >= 2);
  std::puts("PASS a fast player never gets a car inside the speed-dependent bubble");
}

// 4. The speed term works where the fixed clearance does not already dominate:
//    the bubble grows with speed and saturates at `max_bubble`, so a fast
//    player's street is filled further out than a parked one's.
SLRR_TEST(parked_player_still_gets_ahead_traffic) {
  StreamRules r = rules();
  // A short fixed clearance, so the test is about the speed term and not about
  // the shipped 160 m.
  r.front_clear = 10.f;
  r.min_bubble = 25.f;
  r.max_bubble = 200.f;
  StreamView parked = view_with({straight(1, 600, 0, 0, 1)}, 0.f);
  StreamView moving = view_with({straight(1, 600, 0, 0, 1)}, 30.f);
  SLRR_REQUIRE(stream_bubble(parked, r) < stream_bubble(moving, r));
  SLRR_REQUIRE(stream_bubble(parked, r) <= r.min_bubble + .001f);
  SLRR_REQUIRE(std::fabs(stream_min_spawn_distance(1.f, moving, r) -
                         (30.f * r.headway + r.min_bubble)) < .01f);
  SLRR_REQUIRE(stream_min_spawn_distance(1.f, parked, r) <
               stream_min_spawn_distance(1.f, moving, r));
  // Saturation: a very fast player does not push the bubble to infinity.
  StreamView blazing = view_with({straight(1, 600, 0, 0, 1)}, 400.f);
  SLRR_REQUIRE(stream_bubble(blazing, r) <= r.max_bubble + .001f);
  // And the parked player is still served: the street fills at 25 m.
  uint32_t random = 0x5f3759df;
  int picked = 0;
  for (int i = 0; i < 30; ++i) {
    StreamSpot spot = stream_pick_spot(parked, r, random);
    if (!spot.valid) continue;
    ++picked;
    SLRR_REQUIRE(stream_spot_distance(parked.lanes[0], spot.along, parked.player) >=
                 r.min_bubble);
    parked.lanes[0].residents.push_back(spot.along);
  }
  SLRR_REQUIRE(picked >= 2);
  std::puts("PASS a parked player gets traffic closer than a moving one");
}

// 5. Junction chords are never a place for a car to appear.
SLRR_TEST(junction_chords_are_never_used) {
  const auto r = rules();
  StreamView view = view_with({straight(1, 100, 20, 0, 1, /*crossing=*/true),
                               straight(2, 100, 20, 0, 1, /*crossing=*/true)});
  uint32_t random = 7;
  StreamSpot spot = stream_pick_spot(view, r, random);
  SLRR_REQUIRE(!spot.valid);
  SLRR_REQUIRE(std::string(spot.refused) == "junction");
  // Adding one usable lane is enough.
  view.lanes.push_back(straight(3, 300, 0, 0, 1));
  bool used_lane = false;
  for (int i = 0; i < 20; ++i) {
    StreamSpot picked = stream_pick_spot(view, r, random);
    if (!picked.valid) continue;
    SLRR_REQUIRE(picked.edge == 3);
    used_lane = true;
    break;
  }
  SLRR_REQUIRE(used_lane);
  std::puts("PASS a junction chord is never chosen for a spawn");
}

// 6. A lane the player cannot drive to is not a candidate, however close it is.
SLRR_TEST(unreachable_lanes_are_skipped) {
  const auto r = rules();
  StreamLane blocked = straight(1, 200, 20, 5, 1);
  blocked.reachable = false;
  StreamView view = view_with({blocked});
  uint32_t random = 11;
  StreamSpot spot = stream_pick_spot(view, r, random);
  SLRR_REQUIRE(!spot.valid);
  SLRR_REQUIRE(std::string(spot.refused) == "unreachable");
  std::puts("PASS an unreachable lane is refused even when it is metres away");
}

// 7. The population target follows the lane length the player can reach.
SLRR_TEST(target_follows_lane_length) {
  const auto r = rules();
  StreamView small = view_with({straight(1, 500, 40, 0, 1)});
  StreamView large = view_with({straight(1, 500, 40, 0, 1), straight(2, 500, 40, 0, -1)});
  const int a = stream_target_count(small, r, 1.f);
  const int b = stream_target_count(large, r, 1.f);
  SLRR_REQUIRE(a > 0);
  SLRR_REQUIRE(b > a);
  // A longer network asks for more cars, which a fixed count could not express.
  SLRR_REQUIRE(a == int(std::lround(500.f / stream_spacing(1.f, r))));
  std::puts("PASS the target scales with the reachable lane length");
}

// 8. The slider budget stays a hard guard: an enormous network cannot ask for
//    more cars than the slider allows.
SLRR_TEST(target_respects_the_budget) {
  StreamRules r = rules();
  r.budget = 24;
  StreamView view = view_with({straight(1, 40000, 20, 0, 1)});
  SLRR_REQUIRE(stream_target_count(view, r, 1.f) == 24);
  r.budget = 0;
  SLRR_REQUIRE(stream_target_count(view, r, 1.f) == 0);
  std::puts("PASS the slider budget caps the target and zero means no traffic");
}

// 9. A lane holds one car per `per_car_metres` and refuses once it is full.
SLRR_TEST(lane_capacity_is_respected) {
  const auto r = rules();
  StreamView view = view_with({straight(1, 120, 30, 0, 1)});
  const float capacity = 120.f / r.per_car_metres;  // 3.75 cars
  // Five cars is over the lane's capacity, whatever order they stand in.
  for (float along = 12.f; along < 110.f; along += 18.f)
    view.lanes[0].residents.push_back(along);
  uint32_t random = 3;
  StreamSpot spot = stream_pick_spot(view, r, random);
  SLRR_REQUIRE(!spot.valid);
  SLRR_REQUIRE(std::string(spot.refused) == "lane full");
  SLRR_REQUIRE(float(view.lanes[0].residents.size()) > capacity);
  std::puts("PASS a lane refuses more cars than its length allows");
}

// 10. Spacing: no new car within `min_gap` of one already standing on its lane.
SLRR_TEST(gap_to_existing_traffic_is_kept) {
  const auto r = rules();
  StreamView view = view_with({straight(1, 800, 0, 0, 1)});
  view.lanes[0].residents.push_back(200.f);
  uint32_t random = 0x1234abcd;
  int accepted = 0;
  for (int i = 0; i < 40; ++i) {
    StreamSpot spot = stream_pick_spot(view, r, random);
    if (!spot.valid) continue;
    for (const float other : view.lanes[0].residents)
      SLRR_REQUIRE(std::fabs(other - spot.along) >= r.min_gap);
    ++accepted;
    view.lanes[0].residents.push_back(spot.along);
  }
  SLRR_REQUIRE(accepted >= 2);
  std::puts("PASS a new car keeps the minimum gap to cars already on the lane");
}

// 11. The same view and the same seed give the same spot: the scheduler adds no
//     hidden state, so a second run cannot invent duplicates.
SLRR_TEST(same_inputs_give_the_same_spot) {
  const auto r = rules();
  const StreamView view = view_with({straight(1, 600, 0, 0, 1), straight(2, 600, 0, 0, -1)});
  uint32_t a = 0xfeedbeef, b = 0xfeedbeef;
  const StreamSpot first = stream_pick_spot(view, r, a);
  const StreamSpot second = stream_pick_spot(view, r, b);
  SLRR_REQUIRE(first.valid);
  SLRR_REQUIRE(first.edge == second.edge);
  SLRR_REQUIRE(std::fabs(first.along - second.along) < 1e-4f);
  // After the car exists, the same seed may not produce the same spot again.
  StreamView after = view;
  for (auto& lane : after.lanes)
    if (lane.edge == first.edge) lane.residents.push_back(first.along);
  uint32_t c = 0xfeedbeef;
  int same = 0;
  for (int i = 0; i < 40; ++i) {
    StreamSpot again = stream_pick_spot(after, r, c);
    if (!again.valid) continue;
    if (again.edge == first.edge && std::fabs(again.along - first.along) < r.min_gap)
      ++same;
    for (auto& lane : after.lanes)
      if (lane.edge == again.edge) lane.residents.push_back(again.along);
  }
  SLRR_REQUIRE(same == 0);
  std::puts("PASS the scheduler is deterministic and never reuses an occupied spot");
}

// 12. The distance is measured on the lane, not to its nearest point: a lane that
//     curves back is rejected even though the player stands close to one end.
SLRR_TEST(curved_lane_distance_is_real) {
  StreamRules r = rules();
  // This test is about measuring distance on the lane, so the clearance is set
  // low enough that near spots are legal.
  r.front_clear = 45.f;
  StreamLane hairpin;
  hairpin.edge = 1;
  hairpin.length = 400;
  hairpin.distance = 30;
  hairpin.dot = 1;
  for (int i = 0; i <= 10; ++i) {
    const float t = float(i) / 10.f;
    // A hairpin: away from the player and back again, 30 m to the side.
    const float z = -40.f - 200.f * std::sin(t * 3.14159f);
    hairpin.samples.push_back(RoadPoint{30.f, 0, z});
  }
  StreamView view = view_with({hairpin});
  view.lane_metres = hairpin.length;
  // The far end of the hairpin sits ~240 m away, so at least some spots are out
  // of the radius while the lane's own nearest point is 30 m.
  int far = 0, near = 0;
  uint32_t random = 99;
  for (int i = 0; i < 60; ++i) {
    StreamSpot spot = stream_pick_spot(view, r, random);
    if (!spot.valid) continue;
    const float distance = stream_spot_distance(hairpin, spot.along, view.player);
    SLRR_REQUIRE(distance <= r.spawn_radius);
    SLRR_REQUIRE(distance >= r.min_player_distance);
    if (distance > 150.f) ++far; else ++near;
    view.lanes[0].residents.push_back(spot.along);
  }
  SLRR_REQUIRE(near > 0);
  SLRR_REQUIRE(far > 0);
  std::puts("PASS the spawn radius is checked against the position, not the lane");
}

// 13. Nothing is created on the player: the clearance holds in every direction.
SLRR_TEST(player_clearance_holds) {
  const auto r = rules();
  StreamView view = view_with({straight(1, 700, 0, 0, 1), straight(2, 700, 0, 0, -1)});
  uint32_t random = 0x0badf00d;
  int picked = 0;
  for (int i = 0; i < 60; ++i) {
    StreamSpot spot = stream_pick_spot(view, r, random);
    if (!spot.valid) continue;
    ++picked;
    for (auto& lane : view.lanes)
      if (lane.edge == spot.edge)
        SLRR_REQUIRE(stream_spot_distance(lane, spot.along, view.player) >=
                     r.min_player_distance);
    for (auto& lane : view.lanes)
      if (lane.edge == spot.edge) lane.residents.push_back(spot.along);
  }
  SLRR_REQUIRE(picked >= 4);
  std::puts("PASS no car is created on top of the player");
}

// 14. A saturated network refuses instead of scanning forever, and says why, so
//     the runtime can log a reason instead of an empty street.
SLRR_TEST(saturated_network_refuses_with_a_reason) {
  const auto r = rules();
  StreamView view = view_with({straight(1, 60, 10, 0, 1)});
  view.lanes[0].residents = {12.f, 30.f, 48.f};
  uint32_t random = 5;
  StreamSpot spot = stream_pick_spot(view, r, random);
  SLRR_REQUIRE(!spot.valid);
  SLRR_REQUIRE(spot.refused[0] != '\0');
  std::printf("PASS a saturated network refuses with a reason (%s)\n", spot.refused);
}

// 15. The slider keeps its meaning: the shipped 0.2 asks for the original
//     game's sparse city, the maximum for a dense one.
SLRR_TEST(slider_maps_to_spacing) {
  const auto r = rules();
  SLRR_REQUIRE(std::fabs(stream_spacing(0.f, r) - r.spacing_min) < .01f);
  SLRR_REQUIRE(std::fabs(stream_spacing(1.f, r) - r.spacing_max) < .01f);
  SLRR_REQUIRE(stream_spacing(.2f, r) > stream_spacing(.8f, r));
  StreamView view = view_with({straight(1, 7000, 40, 0, 1)});
  const int shipped = stream_target_count(view, r, .2f);
  const int dense = stream_target_count(view, r, 1.f);
  SLRR_REQUIRE(shipped < dense);
  SLRR_REQUIRE(dense <= r.budget);
  std::printf("PASS slider 0.2 -> %d cars, 1.0 -> %d cars on 7 km of lane\n",
              shipped, dense);
}

// 16. The player must not see a car appear. Every accepted spot has to be
//     outside the camera's cone and respect the clearance of its own direction.
//     This is the property the road test broke twice: first a crossing street
//     beside the car had `dot` near zero and escaped the "bubble, but only
//     ahead" rule, then a car at the 160 m front clearance was still plainly
//     visible down a straight road, because this engine's fog only starts at
//     1.5 km and distance cannot hide anything.
SLRR_TEST(nothing_appears_in_plain_sight) {
  const auto r = rules();
  StreamView view = view_with({straight(1, 900, 0, 0, 1),             // ahead
                               straight(2, 900, 0, 0, -1),            // behind
                               crossing_street(3, 500, 0, 60, 0.f),   // left
                               crossing_street(4, 500, 0, 70, 0.f)},  // right
                              /*speed=*/20.f);
  chase_camera(view);
  uint32_t random = 0x51ed270b;
  const float head_x = -std::sin(view.heading);
  const float head_z = -std::cos(view.heading);
  int picked = 0;
  for (int i = 0; i < 120; ++i) {
    StreamSpot spot = stream_pick_spot(view, r, random);
    if (!spot.valid) continue;
    for (auto& lane : view.lanes) {
      if (lane.edge != spot.edge) continue;
      const RoadPoint at = stream_lane_point(lane, spot.along);
      const float dx = at[0] - view.player[0];
      const float dz = at[2] - view.player[2];
      const float flat = std::sqrt(dx * dx + dz * dz);
      const float dot = flat > 1e-3f ? (dx * head_x + dz * head_z) / flat : 0.f;
      const float distance = stream_spot_distance(lane, spot.along, view.player);
      const float required =
          std::max(r.min_player_distance, stream_min_spawn_distance(dot, view, r));
      SLRR_REQUIRE(distance >= required);
      // And it may not be on screen at all: ahead of the car is exactly where
      // the player is looking, so that whole sector is refused and traffic
      // reaches the view by driving into it.
      SLRR_REQUIRE(!stream_spot_visible(view, lane, spot.along));
      // The three zones ask for genuinely different distances, so a car cannot
      // slip through a corner of the cone.
      if (dot > .35f) SLRR_REQUIRE(distance >= r.front_clear);
      if (dot < -.35f) SLRR_REQUIRE(distance >= r.rear_clear);
      if (std::fabs(dot) <= .35f) SLRR_REQUIRE(distance >= r.side_clear);
      lane.residents.push_back(spot.along);
    }
    ++picked;
  }
  SLRR_REQUIRE(picked >= 6);
  std::printf("  %d cars placed, none of them on screen\n", picked);
  std::puts("PASS every spawn is outside the view and the clearance");
}

// 18. The forward sector is not banned, only the part the player is watching: a
//     spot in front is refused while the camera looks at it, and accepted when
//     it is out of the cone (around a corner, or the camera turned away).
SLRR_TEST(ahead_spawns_depend_on_the_camera) {
  const auto r = rules();
  StreamView watched = view_with({straight(1, 900, 0, 0, 1)});
  chase_camera(watched);
  uint32_t random = 0x2545f491;
  int ahead = 0;
  for (int i = 0; i < 60; ++i) {
    const StreamSpot spot = stream_pick_spot(watched, r, random);
    if (spot.valid) ++ahead;
  }
  SLRR_REQUIRE(ahead == 0);

  // Turn the camera around: the same lane is now behind the eye and fills.
  StreamView turned = view_with({straight(1, 900, 0, 0, 1)});
  turned.eye[0] = 0.f;
  turned.eye[1] = 2.6f;
  turned.eye[2] = 6.f;
  turned.look[0] = 0.f;
  turned.look[1] = 0.f;
  turned.look[2] = 1.f;  // looking back up the road
  turned.view_cos = std::cos(51.f * 3.14159265f / 180.f);
  turned.view_range = 300.f;
  uint32_t other = 0x2545f491;
  for (int i = 0; i < 60; ++i) {
    const StreamSpot spot = stream_pick_spot(turned, r, other);
    if (!spot.valid) continue;
    ++ahead;
    turned.lanes[0].residents.push_back(spot.along);
  }
  SLRR_REQUIRE(ahead >= 2);
  std::puts("PASS ahead spawns are refused only while the camera watches them");
}

// 19. Beyond the range at which a car is a couple of pixels on screen the cone
//     stops mattering, so a far straight road can still be seeded.
SLRR_TEST(far_spawns_are_allowed_beyond_the_resolvable_range) {
  const auto r = rules();
  StreamView view = view_with({straight(1, 2000, 0, 0, 1)});
  chase_camera(view, /*range=*/120.f);
  uint32_t random = 0x9e3779b9;
  int far = 0;
  for (int i = 0; i < 120; ++i) {
    const StreamSpot spot = stream_pick_spot(view, r, random);
    if (!spot.valid) continue;
    const float distance = stream_spot_distance(view.lanes[0], spot.along, view.player);
    SLRR_REQUIRE(distance > view.view_range);
    ++far;
  }
  SLRR_REQUIRE(far >= 2);
  std::puts("PASS spawns beyond the resolvable range are allowed");
}

// 17. A limited budget packs the streets the player is on before it reaches a
//     side street two hundred metres away. The runtime keeps the lane list
//     nearest first, and the scheduler draws towards the front of it, so with
//     two equally usable lanes the first one takes most of the cars. Without
//     that bias a hundred cars spread over seven kilometres of reachable lane
//     and the slider did nothing visible.
SLRR_TEST(budget_draws_towards_the_nearest_lanes) {
  const auto r = rules();
  // Geometrically identical lanes, so the only difference is their order: the
  // runtime sorts by distance, this list therefore starts with the near street.
  StreamView view = view_with({straight(1, 900, 0, 0, 1), straight(2, 900, 0, 0, 1)});
  uint32_t random = 0xc2b2ae35;
  int near = 0, far = 0;
  for (int i = 0; i < 200; ++i) {
    const StreamSpot spot = stream_pick_spot(view, r, random);
    if (!spot.valid) continue;
    if (spot.edge == 1) ++near; else ++far;
  }
  SLRR_REQUIRE(near + far == 200);
  std::printf("  near=%d far=%d of 200 draws\n", near, far);
  // A uniform draw would split them 100/100; the bias has to be clear.
  SLRR_REQUIRE(near >= 130);
  SLRR_REQUIRE(far > 0);
  std::puts("PASS the budget is drawn towards the nearest lanes");
}

int main() {
  const standalone::Test tests[] = {standing_player_is_populated_all_around,
                                    driving_fills_the_road_ahead,
                                    fast_player_keeps_headway,
                                    parked_player_still_gets_ahead_traffic,
                                    junction_chords_are_never_used,
                                    unreachable_lanes_are_skipped,
                                    target_follows_lane_length,
                                    target_respects_the_budget,
                                    lane_capacity_is_respected,
                                    gap_to_existing_traffic_is_kept,
                                    same_inputs_give_the_same_spot,
                                    curved_lane_distance_is_real,
                                    player_clearance_holds,
                                    saturated_network_refuses_with_a_reason,
                                    slider_maps_to_spacing,
                                    nothing_appears_in_plain_sight,
                                    budget_draws_towards_the_nearest_lanes,
                                    ahead_spawns_depend_on_the_camera,
                                    far_spawns_are_allowed_beyond_the_resolvable_range};
  return standalone::run("traffic_streaming", tests);
}

#include "slrr/traffic_streaming.hpp"

#include <algorithm>
#include <cmath>

namespace slrr {
namespace {

float unit(uint32_t& state) {
  state ^= state << 13;
  state ^= state >> 17;
  state ^= state << 5;
  return static_cast<float>(state & 0xFFFFFF) / 16777216.f;
}

}  // namespace

RoadPoint stream_lane_point(const StreamLane& lane, float along) {
  if (lane.samples.empty())
    return RoadPoint{};
  if (lane.samples.size() == 1 || lane.length <= 0.f)
    return lane.samples.front();
  const float step = lane.length / float(lane.samples.size() - 1);
  const float position = std::clamp(along / step, 0.f, float(lane.samples.size() - 1));
  const size_t index = std::min<size_t>(size_t(position), lane.samples.size() - 2);
  const float t = position - float(index);
  const RoadPoint& a = lane.samples[index];
  const RoadPoint& b = lane.samples[index + 1];
  return RoadPoint{a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t,
                   a[2] + (b[2] - a[2]) * t};
}

bool stream_point_visible(const StreamView& view, const RoadPoint& at) {
  const float dx = at[0] - view.eye[0];
  const float dy = at[1] - view.eye[1];
  const float dz = at[2] - view.eye[2];
  const float length = std::sqrt(dx * dx + dy * dy + dz * dz);
  if (length <= 1e-3f)
    return true;
  if (length > view.view_range)
    return false;
  const float along_view = (dx * view.look[0] + dy * view.look[1] +
                            dz * view.look[2]) / length;
  return along_view > view.view_cos;
}

bool stream_spot_visible(const StreamView& view, const StreamLane& lane,
                         float along) {
  return stream_point_visible(view, stream_lane_point(lane, along));
}

float stream_spot_distance(const StreamLane& lane, float along,
                           const RoadPoint& player) {
  if (lane.samples.empty()) return lane.distance;
  return road_distance(stream_lane_point(lane, along), player);
}

float stream_spacing(float density, const StreamRules& rules) {
  const float d = std::clamp(density, 0.f, 1.f);
  return rules.spacing_min + (rules.spacing_max - rules.spacing_min) * d;
}

float stream_bubble(const StreamView& view, const StreamRules& rules) {
  const float reach = std::max(0.f, view.speed) * rules.headway;
  return std::clamp(reach + rules.min_bubble, rules.min_bubble, rules.max_bubble);
}

float stream_min_spawn_distance(float dot, const StreamView& view,
                                const StreamRules& rules) {
  if (dot > .35f)
    return std::max(rules.front_clear, stream_bubble(view, rules));
  if (dot < -.35f) return rules.rear_clear;
  return rules.side_clear;
}

int stream_target_count(const StreamView& view, const StreamRules& rules,
                        float density) {
  if (rules.budget <= 0)
    return 0;
  const float spacing = stream_spacing(density, rules);
  if (!std::isfinite(spacing) || spacing <= 0.f)
    return 0;
  // The lane length the player can reach, not a count of cars: a long straight
  // holds more traffic than a tight block of the same radius.
  const int from_network = int(std::lround(std::max(0.f, view.lane_metres) / spacing));
  return std::clamp(from_network, 0, rules.budget);
}

StreamSpot stream_pick_spot(const StreamView &view, const StreamRules &rules,
                            uint32_t &random) {
  StreamSpot spot;
  if (view.lanes.empty()) {
    spot.refused = "no lanes";
    return spot;
  }
  // A bounded number of draws: a busy network still yields a spot, an empty or
  // saturated one gives up instead of scanning the whole graph.
  const float head_x = -std::sin(view.heading);
  const float head_z = -std::cos(view.heading);
  for (int attempt = 0; attempt < 48; ++attempt) {
    // Biased draw: the list is nearest first, and squaring the uniform turns a
    // uniform sweep of the district into a sweep of the streets the player is
    // standing on, spilling outwards as those fill up.
    const float draw = unit(random);
    const size_t index =
        std::min<size_t>(size_t(draw * draw * float(view.lanes.size())),
                         view.lanes.size() - 1);
    const StreamLane &lane = view.lanes[index];
    if (lane.crossing) {
      spot.refused = "junction";
      continue;
    }
    if (lane.length < rules.min_lane_length) {
      spot.refused = "short lane";
      continue;
    }
    if (!lane.reachable) {
      spot.refused = "unreachable";
      continue;
    }
    if (lane.distance > rules.spawn_radius) {
      spot.refused = "too far";
      continue;
    }
    // Room on this lane: one car per `per_car_metres`, and none of them closer
    // than `min_gap` to the spot we are about to pick.
    const float capacity = std::max(1.f, lane.length / rules.per_car_metres);
    if (float(lane.residents.size()) >= capacity) {
      spot.refused = "lane full";
      continue;
    }
    // The bubble is applied to the chosen position below, not to the lane: a
    // long lane can pass right beside the player and still have a perfectly
    // good spot a hundred metres down it, and rejecting the lane outright is how
    // the old policy emptied whole streets.
    const float low = std::min(rules.end_clearance, lane.length * .5f);
    const float high = lane.length - low;
    if (high - low < 1.f) {
      spot.refused = "no room inside the ends";
      continue;
    }
    const float along = low + unit(random) * (high - low);
    // The lane's own nearest point can be much closer than the chosen position
    // on a long street, so the radius and the clearance are checked against the
    // spot, not against the lane.
    const RoadPoint at = stream_lane_point(lane, along);
    const float spot_distance = stream_spot_distance(lane, along, view.player);
    if (spot_distance > rules.spawn_radius) {
      spot.refused = "spot too far";
      continue;
    }
    // Direction of the spot itself, not of the lane: a lane that runs past the
    // player towards the horizon has one "dot" for the whole curve, and the old
    // rule let its near end, standing beside the car, through.
    const float dx = at[0] - view.player[0];
    const float dz = at[2] - view.player[2];
    const float flat = std::sqrt(dx * dx + dz * dz);
    const float dot = flat > 1e-3f ? (dx * head_x + dz * head_z) / flat : 0.f;
    const float required =
        std::max(rules.min_player_distance,
                 stream_min_spawn_distance(dot, view, rules));
    if (spot_distance < required) {
      spot.refused = "too close to the player";
      continue;
    }
    // On screen is on screen at any distance the player can resolve, so a spot
    // inside the camera cone is refused outright and traffic reaches the view by
    // driving. This is the rule that stops a car appearing a hundred metres up
    // the road the player is looking down.
    if (stream_spot_visible(view, lane, along)) {
      spot.refused = "in view";
      continue;
    }
    bool crowded = false;
    for (const float other : lane.residents) {
      if (std::fabs(other - along) < rules.min_gap) {
        crowded = true;
        break;
      }
    }
    if (crowded) {
      spot.refused = "gap too small";
      continue;
    }
    // A car already standing near the player on this lane would be right next to
    // it; `residents` covers the lane, `lane.distance` the player.
    spot.edge = lane.edge;
    spot.along = along;
    spot.valid = true;
    spot.refused = "";
    return spot;
  }
  return spot;
}

}  // namespace slrr

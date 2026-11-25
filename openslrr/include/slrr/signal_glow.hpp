#pragma once

// U2-C: the lit lamp on each traffic-light head. The heads are static map
// props; only the phase changes, so this module resamples the graph and
// rebuilds one face per head whenever the traffic clock moves.

#include "slrr/road_routes.hpp"

#include <cstddef>
#include <vector>

namespace slrr {

// Lit lamp faces in the drawn world: metres, Z mirrored the way every prop is.
// Six vertices per quad, interleaved x, y, z, u, v, so the renderer can upload
// the blob as it stands and let the shader round the face off.
struct SignalGlowMesh {
  std::vector<float> vertices;
  // Vertex offsets of the three phase ranges in this order: red, amber, green.
  // A range runs from its start to the next one, the last to `vertices.size()`.
  size_t red = 0, amber = 0, green = 0;
  // Bumped on every rebuild, so a renderer only re-uploads when it moved.
  uint32_t revision = 0;
};

// Rebuild for the phase at `seconds`. One face per head, so the cost is the
// number of heads the map placed, not the number of lamps on a model.
void signal_glow_update(const RoadRoutes& graph, double seconds);
const SignalGlowMesh& signal_glow_mesh();

// Phase colour, premultiplied rgb and intensity in alpha. 0 red, 1 amber,
// 2 green; anything else is red.
void signal_glow_color(int state, float out[4]);

}  // namespace slrr

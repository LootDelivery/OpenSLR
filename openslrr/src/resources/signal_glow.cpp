#include "slrr/signal_glow.hpp"

#include "slrr/scene_instances.hpp"

#include <cmath>
#include <cstdio>
#include <cstdlib>
#include <cstdint>
#include <unordered_map>

namespace slrr {
namespace {

SignalGlowMesh g_mesh;

// The fixtures the graph was built from carry exactly these floats, so a head
// finds its crossing by position. Two heads never share a lamp post.
uint64_t position_key(const float p[3]) {
  constexpr double step = 0.05;
  const auto cell = [&](float v) {
    return static_cast<uint64_t>(static_cast<int64_t>(std::llround(v / step)) + (1 << 20));
  };
  return (cell(p[0]) * 0x9e3779b1u) ^ (cell(p[1]) * 0x85ebca6bu) ^
         (cell(p[2]) * 0xc2b2ae35u);
}

void append_face(std::vector<float>& out, const float corners[4][3]) {
  static const int order[6] = {0, 1, 2, 0, 2, 3};
  static const float uv[4][2] = {{0.f, 0.f}, {1.f, 0.f}, {1.f, 1.f}, {0.f, 1.f}};
  for (int i = 0; i < 6; ++i) {
    const int c = order[i];
    out.push_back(corners[c][0]);
    out.push_back(corners[c][1]);
    out.push_back(corners[c][2]);
    out.push_back(uv[c][0]);
    out.push_back(uv[c][1]);
  }
}

}  // namespace

void signal_glow_color(int state, float out[4]) {
  // The shipped lenses are dull glass; these are what they look like lit.
  const float red[4] = {1.00f, 0.09f, 0.05f, 1.0f};
  const float amber[4] = {1.00f, 0.48f, 0.04f, 1.0f};
  const float green[4] = {0.07f, 0.95f, 0.52f, 1.0f};
  const float* pick = state == 2 ? green : state == 1 ? amber : red;
  for (int i = 0; i < 4; ++i) out[i] = pick[i];
}

void signal_glow_update(const RoadRoutes& graph, double seconds) {
  std::vector<float> phase[3];  // red, amber, green
  const auto& heads = scene_instances_signal_heads();
  static int calls = 0;
  const bool debug = [] {
    const char* e = std::getenv("SLRR_GLOW_DEBUG");
    return e && *e && *e != '0';
  }();
  if (debug && (calls++ % 60) == 0)
    std::fprintf(stderr, "[glow] heads=%zu bound=%zu\n", heads.size(),
                 graph.signals().size());
  // `SLRR_GLOW=0` keeps the lamps unlit so one build can be compared against
  // itself with and without the pass (A/B screenshots of the same frame).
  static const bool enabled = [] {
    const char* e = std::getenv("SLRR_GLOW");
    return !(e && *e == '0');
  }();
  if (enabled && !heads.empty() && !graph.signals().empty()) {
    std::unordered_map<uint64_t, std::pair<uint32_t, uint32_t>> at;
    at.reserve(graph.signals().size() * 2);
    for (const auto& signal : graph.signals())
      at.try_emplace(position_key(signal.position.data()), signal.crossing,
                     signal.branch);
    for (const SignalHead& head : heads) {
      if (!head.lit) continue;
      const auto found = at.find(position_key(head.position));
      if (found == at.end()) continue;
      const int state =
          graph.signal(found->second.first, found->second.second, seconds);
      if (state < 0) continue;  // unregulated, or a regulated approach with no lamp
      const int lamp = state == 2 ? 2 : state == 1 ? 1 : 0;
      const float* box = head.lamp[lamp];  // x0, x1, y0, y1
      const float local[4][3] = {
          {box[0], box[2], head.plane_z},
          {box[1], box[2], head.plane_z},
          {box[1], box[3], head.plane_z},
          {box[0], box[3], head.plane_z},
      };
      float corners[4][3];
      // The batch builder hands the renderer S*M*S and uploads the mesh as S*v
      // (scene_instances.cpp, "(col == 2) != (row == 2)"), so a model point
      // lands at S*(M*v). Writing S*M*S*v put the face one lens-depth on the
      // wrong side of the glass, where its own housing hid it.
      const float* m = head.frame;
      for (int i = 0; i < 4; ++i) {
        const float* v = local[i];
        for (int r = 0; r < 3; ++r)
          corners[i][r] = m[r] * v[0] + m[4 + r] * v[1] + m[8 + r] * v[2] + m[12 + r];
        corners[i][2] = -corners[i][2];
      }
      // Lift the face a centimetre along the lens normal, which is the model's
      // +Z carried through the same mirror. Reversed depth means a face even a
      // millimetre behind the glass is rejected, and the lamps vanish.
      const float normal[3] = {m[8], m[9], -m[10]};
      const float length = std::sqrt(normal[0] * normal[0] + normal[1] * normal[1] +
                                     normal[2] * normal[2]);
      const float step = length > 1e-6f ? .01f / length : 0.f;
      for (int i = 0; i < 4; ++i)
        for (int r = 0; r < 3; ++r) corners[i][r] += normal[r] * step;
      append_face(phase[lamp], corners);
    }
  }
  // One contiguous range per phase: the renderer draws each with its own
  // colour uniform instead of carrying a colour per vertex.
  g_mesh.vertices.clear();
  g_mesh.vertices.reserve(phase[0].size() + phase[1].size() + phase[2].size());
  for (const auto& part : phase)
    g_mesh.vertices.insert(g_mesh.vertices.end(), part.begin(), part.end());
  g_mesh.red = 0;
  g_mesh.amber = phase[0].size();
  g_mesh.green = phase[0].size() + phase[1].size();
  ++g_mesh.revision;
  if (debug && (calls % 60) == 59)
    std::fprintf(stderr,
                 "[glow] faces=%zu (r=%zu a=%zu g=%zu)\n",
                 g_mesh.vertices.size() / 30, (g_mesh.amber - g_mesh.red) / 30,
                 (g_mesh.green - g_mesh.amber) / 30,
                 (g_mesh.vertices.size() - g_mesh.green) / 30);
}

const SignalGlowMesh& signal_glow_mesh() { return g_mesh; }

}  // namespace slrr

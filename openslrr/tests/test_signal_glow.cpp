// U2-C: the lit lamp on a traffic-light head. The behaviour side (cars stopping
// on red) is measured in test_traffic_signals; this covers the picture: which
// lamp face of the head the phase lights, and where that face lands.
#include "standalone_test.hpp"

#include "slrr/gameref.hpp"
#include "slrr/object.hpp"
#include "slrr/part_config.hpp"
#include "slrr/race_runtime.hpp"
#include "slrr/rpk_index.hpp"
#include "slrr/scene_instances.hpp"
#include "slrr/signal_glow.hpp"

#include <algorithm>
#include <cmath>
#include <cstdio>
#include <cstdlib>
#include <filesystem>
#include <string>
#include <vector>

using namespace slrr;
namespace fs = std::filesystem;

namespace {

fs::path game_root() {
  if (const char* env = std::getenv("SLRR_GAME_ROOT"))
    if (*env) return fs::path(env);
  for (fs::path p : {fs::path("../Street Legal Racing - Redline"), fs::path("game")}) {
    std::error_code ec;
    if (fs::exists(p / "maps/city.rpk", ec)) return fs::absolute(p);
  }
  return {};
}

struct Fixture {
  RpkIndex packs;
  std::shared_ptr<const RoadRoutes> graph;
  uint32_t head_type = 0;
  bool ready = false;

  Fixture() {
    const fs::path root = game_root();
    if (root.empty()) return;
    packs.set_root(root.string());
    packs.open_all();
    rpk_set_active(&packs);
    scene_instances_set_resources(&packs);
    const int32_t city = packs.open_lib("maps/city.rpk");
    Object ground;
    auto* node = gameref_upsert(&ground, GameRefKind::GroundRef);
    node->type_id = RpkIndex::make_full_id(city, 1);
    graph = race_ground_roads(packs, &ground);
    // The head model comes from the fixture the map itself placed.
    const auto map = scene_instances_ground_map(packs, node->type_id);
    for (const auto& placement : map.placements) {
      const auto bytes = packs.read_payload(placement.type);
      if (bytes.size() > 16 && std::equal(bytes.begin(), bytes.begin() + 4, "ICFG") &&
          std::equal(bytes.begin() + 8, bytes.begin() + 16, "trlight\0")) {
        head_type = placement.type;
        break;
      }
    }
    ready = graph && !graph->signals().empty() && head_type;
  }
};

Fixture& fixture() {
  static Fixture f;
  return f;
}

// One head of the shipped model, on its own ground, at a known pose.
const SignalHead& place_one_head(Fixture& f, const RoadPoint& at, float yaw) {
  GroundMapData map;
  map.render_id = 1;
  GroundMapData::Placement placement;
  placement.type = f.head_type;
  placement.seed = 0;
  placement.position = at;
  placement.rotation = {yaw, 0.f, 0.f};
  map.placements.push_back(placement);
  scene_instances_set_resources(&f.packs);
  scene_instances_add_ground_props(map);
  const auto& heads = scene_instances_signal_heads();
  SLRR_REQUIRE(heads.size() == 1);
  return heads.front();
}

double seconds_for(const RoadRoutes& graph, uint32_t crossing, uint32_t branch, int want) {
  for (int i = 0; i < 4000; ++i) {
    const double t = i * 0.01;
    if (graph.signal(crossing, branch, t) == want) return t;
  }
  return -1;
}

float mean_corner(const std::vector<float>& vertices, size_t start, size_t count, int axis) {
  float sum = 0.f;
  for (size_t i = 0; i < count; ++i) sum += vertices[start + i * 5 + axis];
  return sum / static_cast<float>(count);
}

// A prop is drawn as S*M*S*v: the mesh point is mirrored, the placement pose
// applies, and the result is mirrored again. The engine's own pose matrix is
// the only reference that can catch a sign in the wrong half of that sandwich.
// A prop is drawn as (S*M*S)*(S*v) = S*(M*v): see the mirror sandwich in
// scene_instances_collect_batches. The lamp face is then lifted a centimetre
// along that same normal so the reversed depth buffer keeps it in front.
std::array<float, 3> drawn_point(const float* m, float x, float y, float z) {
  const float v[3] = {x, y, z};
  std::array<float, 3> out{};
  for (int r = 0; r < 3; ++r)
    out[static_cast<size_t>(r)] =
        m[r] * v[0] + m[4 + r] * v[1] + m[8 + r] * v[2] + m[12 + r];
  out[2] = -out[2];
  const float normal[3] = {m[8], m[9], -m[10]};
  const float length =
      std::sqrt(normal[0] * normal[0] + normal[1] * normal[1] + normal[2] * normal[2]);
  if (length > 1e-6f)
    for (int r = 0; r < 3; ++r) out[static_cast<size_t>(r)] += normal[r] * .01f / length;
  return out;
}

}  // namespace

SLRR_TEST(the_head_model_yields_three_lamp_faces_in_red_amber_green_order) {
  Fixture& f = fixture();
  if (!f.ready) { std::puts("SKIP retail install not found"); return; }
  const RoadPoint at = f.graph->signals().front().position;
  const SignalHead& head = place_one_head(f, at, 0.f);
  SLRR_REQUIRE(head.lit);
  SLRR_REQUIRE(head.type == f.head_type);
  SLRR_REQUIRE(head.position[0] == at[0] && head.position[1] == at[1] &&
               head.position[2] == at[2]);
  // The three faces are the front of the head: a plane a few centimetres in,
  // each face about a third of the head's height and the full width.
  SLRR_REQUIRE(head.plane_z > .02f && head.plane_z < .08f);
  for (int lamp = 0; lamp < 3; ++lamp) {
    const float* box = head.lamp[lamp];
    std::printf("[glow] lamp %d x %.3f..%.3f y %.3f..%.3f\n", lamp, double(box[0]),
                double(box[1]), double(box[2]), double(box[3]));
    SLRR_REQUIRE(box[1] - box[0] > .38f && box[1] - box[0] < .48f);
    SLRR_REQUIRE(box[3] - box[2] > .25f && box[3] - box[2] < .42f);
  }
  // Red on top, amber in the middle, green at the bottom: the atlas rows and
  // the head's own Y have to agree, or the phase would light the wrong lens.
  const auto middle = [&](int lamp) { return (head.lamp[lamp][2] + head.lamp[lamp][3]) * .5f; };
  SLRR_REQUIRE(middle(0) > middle(1) + .2f);
  SLRR_REQUIRE(middle(1) > middle(2) + .2f);
  (void)head;
}

SLRR_TEST(the_phase_lights_its_own_face_on_the_head) {
  Fixture& f = fixture();
  if (!f.ready) { std::puts("SKIP retail install not found"); return; }
  const auto& fixture_signal = f.graph->signals().front();
  const RoadPoint at = fixture_signal.position;
  const SignalHead& head = place_one_head(f, at, 0.f);
  const double red = seconds_for(*f.graph, fixture_signal.crossing, fixture_signal.branch, 0);
  const double amber = seconds_for(*f.graph, fixture_signal.crossing, fixture_signal.branch, 1);
  const double green = seconds_for(*f.graph, fixture_signal.crossing, fixture_signal.branch, 2);
  SLRR_REQUIRE(red >= 0 && amber >= 0 && green >= 0);
  // One face per head, in the range of its phase and nowhere else.
  const auto check = [&](double seconds, int lamp) {
    signal_glow_update(*f.graph, seconds);
    const auto& mesh = signal_glow_mesh();
    SLRR_REQUIRE(mesh.vertices.size() == 30);
    // Red starts at zero, so the range of the lit phase is the whole buffer
    // and the other two must be empty.
    SLRR_REQUIRE(mesh.red == 0);
    SLRR_REQUIRE(mesh.amber == (lamp == 0 ? 30u : 0u));
    SLRR_REQUIRE(mesh.green == (lamp == 2 ? 0u : 30u));
    for (size_t i = 0; i < 6; ++i)
      for (int axis = 0; axis < 3; ++axis) {
        const float value = mesh.vertices[i * 5 + axis];
        SLRR_REQUIRE(std::isfinite(value));
      }
    // The face lands on the lens the renderer would draw: the box centre of
    // that lamp, through the head's own draw transform.
    const float box_cx = (head.lamp[lamp][0] + head.lamp[lamp][1]) * .5f;
    const float box_cy = (head.lamp[lamp][2] + head.lamp[lamp][3]) * .5f;
    const auto want = drawn_point(head.frame, box_cx, box_cy, head.plane_z);
    const float x = mean_corner(mesh.vertices, 0, 6, 0);
    const float y = mean_corner(mesh.vertices, 0, 6, 1);
    const float z = mean_corner(mesh.vertices, 0, 6, 2);
    std::printf("[glow] t=%.2f lamp %d at %.3f,%.3f,%.3f (want %.3f,%.3f,%.3f)\n",
                seconds, lamp, double(x), double(y), double(z), double(want[0]),
                double(want[1]), double(want[2]));
    SLRR_REQUIRE(std::fabs(x - want[0]) < 1e-3f);
    SLRR_REQUIRE(std::fabs(y - want[1]) < 1e-3f);
    SLRR_REQUIRE(std::fabs(z - want[2]) < 1e-3f);
  };
  check(red, 0);
  check(amber, 1);
  check(green, 2);
}

SLRR_TEST(the_head_turns_with_its_placement_yaw) {
  Fixture& f = fixture();
  if (!f.ready) { std::puts("SKIP retail install not found"); return; }
  const auto& fixture_signal = f.graph->signals().front();
  const RoadPoint at = fixture_signal.position;
  const double red = seconds_for(*f.graph, fixture_signal.crossing, fixture_signal.branch, 0);
  SLRR_REQUIRE(red >= 0);
  // Two poses, and for each one every corner of the lit face is compared with
  // the point the renderer would draw for that same corner of the model.
  for (const float yaw : {0.f, 1.5707963f, .7f}) {
    const SignalHead& head = place_one_head(f, at, yaw);
    SLRR_REQUIRE(head.lit);
    signal_glow_update(*f.graph, red);
    const auto& mesh = signal_glow_mesh();
    SLRR_REQUIRE(mesh.vertices.size() == 30);
    const float* box = head.lamp[0];  // red is the face lit at `red`
    const float plane = head.plane_z;
    const std::array<std::array<float, 3>, 4> want = {
        drawn_point(head.frame, box[0], box[2], plane),
        drawn_point(head.frame, box[1], box[2], plane),
        drawn_point(head.frame, box[1], box[3], plane),
        drawn_point(head.frame, box[0], box[3], plane),
    };
    for (size_t i = 0; i < 6; ++i) {
      const float got[3] = {mesh.vertices[i * 5], mesh.vertices[i * 5 + 1],
                            mesh.vertices[i * 5 + 2]};
      // The quad repeats the first corner twice, so a vertex matches when it
      // sits on any wanted corner.
      bool hit = false;
      for (const auto& w : want)
        hit = hit || (std::fabs(got[0] - w[0]) < 1e-3f &&
                      std::fabs(got[1] - w[1]) < 1e-3f &&
                      std::fabs(got[2] - w[2]) < 1e-3f);
      if (!hit)
        std::printf("[glow] yaw %.3f vertex %zu at %.3f,%.3f,%.3f\n", double(yaw), i,
                    double(got[0]), double(got[1]), double(got[2]));
      SLRR_REQUIRE(hit);
    }
  }
}

int main() {
  const slrr::standalone::Test tests[] = {
      the_head_model_yields_three_lamp_faces_in_red_amber_green_order,
      the_phase_lights_its_own_face_on_the_head,
      the_head_turns_with_its_placement_yaw,
  };
  return slrr::standalone::run("signal_glow", tests);
}

// U2-C: the lit lamp on a traffic-light head. The behaviour side (cars stopping
// on red) is measured in test_traffic_signals; this covers the picture: which
// lamp face of the head the phase lights, and where that face lands.
#include "standalone_test.hpp"

#include "slrr/gameref.hpp"
#include "slrr/object.hpp"
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
    const float y = mean_corner(mesh.vertices, 0, 6, 1);
    const float z = mean_corner(mesh.vertices, 0, 6, 2);
    const float x = mean_corner(mesh.vertices, 0, 6, 0);
    const float face = (head.lamp[lamp][2] + head.lamp[lamp][3]) * .5f;
    std::printf("[glow] t=%.2f lamp %d y %.3f (face %.3f)\n", seconds, lamp,
                double(y), double(face));
    SLRR_REQUIRE(std::fabs(y - (at[1] + face)) < .01f);
    SLRR_REQUIRE(std::fabs(x - at[0]) < .01f);
    SLRR_REQUIRE(std::fabs(z + (at[2] + head.plane_z)) < .01f);
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
  const float plane_z = place_one_head(f, at, 0.f).plane_z;
  signal_glow_update(*f.graph, red);
  const auto& flat = signal_glow_mesh();
  SLRR_REQUIRE(!flat.vertices.empty());
  const size_t count = flat.vertices.size() / 5;
  float flat_x0 = 1e9f, flat_x1 = -1e9f, flat_z0 = 1e9f, flat_z1 = -1e9f;
  for (size_t i = 0; i < count; ++i) {
    flat_x0 = std::min(flat_x0, flat.vertices[i * 5]);
    flat_x1 = std::max(flat_x1, flat.vertices[i * 5]);
    flat_z0 = std::min(flat_z0, flat.vertices[i * 5 + 2]);
    flat_z1 = std::max(flat_z1, flat.vertices[i * 5 + 2]);
  }
  place_one_head(f, at, 1.5707963f);
  signal_glow_update(*f.graph, red);
  const auto& turned = signal_glow_mesh();
  SLRR_REQUIRE(turned.vertices.size() == flat.vertices.size());
  float x0 = 1e9f, x1 = -1e9f, z0 = 1e9f, z1 = -1e9f;
  for (size_t i = 0; i < count; ++i) {
    x0 = std::min(x0, turned.vertices[i * 5]);
    x1 = std::max(x1, turned.vertices[i * 5]);
    z0 = std::min(z0, turned.vertices[i * 5 + 2]);
    z1 = std::max(z1, turned.vertices[i * 5 + 2]);
  }
  std::printf("[glow] yaw 0: x %.2f..%.2f z %.2f..%.2f\n", double(flat_x0), double(flat_x1),
              double(flat_z0), double(flat_z1));
  std::printf("[glow] yaw 90: x %.2f..%.2f z %.2f..%.2f\n", double(x0), double(x1),
              double(z0), double(z1));
  // A quarter turn trades the face's width and depth, and centres stay put.
  SLRR_REQUIRE(x1 - x0 < flat_x1 - flat_x0 - .2f);
  SLRR_REQUIRE(z1 - z0 > flat_z1 - flat_z0 + .2f);
  // The face sits a few centimetres in front of the post, so a quarter turn
  // moves the centre by that offset and nothing else.
  const float flat_centre = (flat_x0 + flat_x1) * .5f;
  SLRR_REQUIRE(std::fabs((x0 + x1) * .5f - flat_centre) <= plane_z + .01f);
}

int main() {
  const slrr::standalone::Test tests[] = {
      the_head_model_yields_three_lamp_faces_in_red_amber_green_order,
      the_phase_lights_its_own_face_on_the_head,
      the_head_turns_with_its_placement_yaw,
  };
  return slrr::standalone::run("signal_glow", tests);
}

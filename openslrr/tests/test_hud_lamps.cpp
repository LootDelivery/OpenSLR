// The HUD lamps (ABS / ASR / handbrake) are pixel quads of the OSD panel, not
// world meshes. This pins the three things a port silently gets wrong: the
// resource ids the shipped cfg resolves to (it ships 0xFFFFFFFF, i.e. "stock
// LED"), the panel atlas those entries carry, and the panel-space transform
// that places the row in the bottom-right corner of the screen.
#include "standalone_test.hpp"

#include "slrr/rpk_index.hpp"
#include "slrr/scene_instances.hpp"
#include "slrr/scx_mesh.hpp"

#include <cmath>
#include <cstdlib>
#include <filesystem>
#include <string>

using namespace slrr;
namespace fs = std::filesystem;

namespace {
// Retail install, several known layouts (same convention as test_road_routes).
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

struct Packs {
  RpkIndex index;
  bool ready = false;
  Packs() {
    const fs::path root = game_root();
    if (root.empty()) return;
    index.set_root(root.string());
    index.open_all();
    rpk_set_active(&index);
    scene_instances_set_resources(&index);
    ready = true;
  }
};

Packs& packs() {
  static Packs instance;
  return instance;
}

// Alias in cars.rpk -> mesh id, left edge of the quad in panel pixels, and the
// clip x that draw_vehicle_hud anchors it to.
struct Lamp {
  const char* alias;
  uint32_t id;
  float left;
  float clip_x;
};
constexpr Lamp kLamps[] = {{"led_ABS", 0x61, -499.19f, .649f},
                           {"led_ASR", 0x62, -471.85f, .652f},
                           {"led_HB", 0x63, -444.89f, .659f}};

// The lamp transform used by draw_vehicle_hud: panel pixel space mapped onto
// the bottom edge of the screen, where the game paints its own unlit labels
// (ABS, ASR, (O), warning triangle). OSD clip space is y down, so the panel's
// +y steps downwards; the anchors were lined up on screen until every icon
// covered its label, and the height is 0.9 of the measured one to match the
// label's aspect.
constexpr float kLedSx = .00209f;
constexpr float kLedY = .993f, kLedSy = .003915f;
}  // namespace

SLRR_TEST(lamps_resolve_to_the_panel_quads_with_the_panel_atlas) {
  if (!packs().ready) {
    std::puts("SKIP retail install not found (set SLRR_GAME_ROOT)");
    return;
  }
  for (const Lamp& lamp : kLamps) {
    SLRR_REQUIRE(packs().index.pack_by_name("cars.rpk") != nullptr);
    const uint32_t full = rpk_resolve_resource("cars.rpk", lamp.id);
    const auto geo = scene_instances_geometry(full);
    SLRR_REQUIRE(geo && geo->info.ok);
    // A lamp icon, not a world mesh: four corners and two triangles.
    SLRR_REQUIRE(geo->positions.size() == 12);
    SLRR_REQUIRE(geo->indices.size() == 6);
    SLRR_REQUIRE(std::fabs(geo->info.bbox_min[0] - lamp.left) < .05f);
    SLRR_REQUIRE(geo->info.bbox_max[0] - geo->info.bbox_min[0] > 20.f);
    SLRR_REQUIRE(geo->info.bbox_max[0] - geo->info.bbox_min[0] < 32.f);
    SLRR_REQUIRE(std::fabs(geo->info.bbox_min[1] - 0.69f) < .05f);
    SLRR_REQUIRE(std::fabs(geo->info.bbox_max[1] - 17.42f) < .05f);
    // The panel atlas has to come from the model table, not the loose mesh.
    SLRR_REQUIRE(!geo->textures.empty());
    SLRR_REQUIRE(geo->textures.front().find("LongOSDGauge") != std::string::npos);
  }
}

SLRR_TEST(the_lamp_row_lands_on_the_game_labels) {
  if (!packs().ready) {
    std::puts("SKIP retail install not found (set SLRR_GAME_ROOT)");
    return;
  }
  float previous_left = -2.f, previous_right = -2.f;
  for (const Lamp& lamp : kLamps) {
    const auto geo =
        scene_instances_geometry(rpk_resolve_resource("cars.rpk", lamp.id));
    SLRR_REQUIRE(geo && geo->info.ok);
    const float left = lamp.clip_x + geo->info.bbox_min[0] * kLedSx;
    const float right = lamp.clip_x + geo->info.bbox_max[0] * kLedSx;
    const float bottom = kLedY - geo->info.bbox_min[1] * kLedSy;
    const float top = kLedY - geo->info.bbox_max[1] * kLedSy;
    std::printf("[lamps] %s x %.3f..%.3f  y %.3f..%.3f\n", lamp.alias,
                double(left), double(right), double(bottom), double(top));
    // Left of centre and on the bottom edge, where the labels are.
    SLRR_REQUIRE(left > -.41f && right < -.20f);
    SLRR_REQUIRE(bottom <= 1.f && top > .9f && bottom > top);
    // One icon about a twentieth of the width and a fifteenth of the height,
    // which is the label, so the row cannot drift back to another aspect.
    SLRR_REQUIRE(right - left > .05f && right - left < .06f);
    SLRR_REQUIRE(bottom - top > .06f && bottom - top < .07f);
    // Three lamps in a row, in order, without overlapping.
    SLRR_REQUIRE(left > previous_left + .045f);
    SLRR_REQUIRE(left > previous_right);
    previous_left = left;
    previous_right = right;
  }
}

int main() {
  const slrr::standalone::Test tests[] = {
      lamps_resolve_to_the_panel_quads_with_the_panel_atlas,
      the_lamp_row_lands_on_the_game_labels,
  };
  return slrr::standalone::run("hud_lamps", tests);
}

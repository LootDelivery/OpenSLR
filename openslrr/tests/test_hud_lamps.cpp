// The HUD lamps (ABS / ASR / handbrake) are pixel quads of the OSD panel, not
// world meshes. This pins the three things a port silently gets wrong: the
// resource ids the shipped cfg resolves to (it ships 0xFFFFFFFF, i.e. "stock
// LED"), the panel atlas those entries carry, and the panel-space transform
// that places the row in the bottom-right corner of the screen.
#include "standalone_test.hpp"

#include "slrr/game_root.hpp"
#include "slrr/hud_lamp_tuning.hpp"
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

// Alias in cars.rpk -> mesh id, left edge of the quad in panel pixels.
struct Lamp {
  const char* alias;
  uint32_t id;
  float left;
};
constexpr Lamp kLamps[] = {{"led_ABS", 0x61, -499.19f},
                           {"led_ASR", 0x62, -471.85f},
                           {"led_HB", 0x63, -444.89f}};

// The lamp transform used by draw_vehicle_hud: panel pixel space mapped onto a
// frame of the shipped HUD, where the game paints the four unlit labels that
// the lit icons have to cover. OSD clip space is y down, so the panel's +y
// steps downwards.
constexpr float kLedX = .647f, kLedSx = .00209f;
constexpr float kLedY = 1.f, kLedSy = .00435f;
}  // namespace

SLRR_TEST(lamps_resolve_to_the_panel_quads_with_the_panel_atlas) {
  if (!packs().ready) {
    std::puts("SKIP retail install not found (set SLRR_GAME_ROOT)");
    return;
  }
  for (const Lamp& lamp : kLamps) {
    SLRR_REQUIRE(packs().index.pack_by_name("cars.rpk") != nullptr);
    const uint32_t full =
        rpk_resolve_resource("cars.rpk", lamp.id);
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

SLRR_TEST(the_lamp_row_lands_along_the_bottom_right_corner) {
  if (!packs().ready) {
    std::puts("SKIP retail install not found (set SLRR_GAME_ROOT)");
    return;
  }
  float previous = -2.f;
  for (const Lamp& lamp : kLamps) {
    const auto geo =
        scene_instances_geometry(rpk_resolve_resource("cars.rpk", lamp.id));
    SLRR_REQUIRE(geo && geo->info.ok);
    const float left = kLedX + geo->info.bbox_min[0] * kLedSx;
    const float right = kLedX + geo->info.bbox_max[0] * kLedSx;
    const float bottom = kLedY - geo->info.bbox_min[1] * kLedSy;
    const float top = kLedY - geo->info.bbox_max[1] * kLedSy;
    std::printf("[lamps] %s x %.3f..%.3f  y %.3f..%.3f\n", lamp.alias,
                double(left), double(right), double(bottom), double(top));
    // Left of centre, on the bottom edge, one lamp about a hundredth of the
    // height tall, three in a row without overlapping.
    SLRR_REQUIRE(left > -.44f && right < -.20f);
    SLRR_REQUIRE(bottom <= 1.f && top > .9f && bottom > top);
    SLRR_REQUIRE(right - left > .05f && right - left < .06f);
    SLRR_REQUIRE(left > previous + .045f);
    previous = left;
  }
}

SLRR_TEST(hud_lamps_file_moves_each_lamp_without_a_rebuild) {
  // One line per lamp, three numbers: right, up, size.
  const slrr::HudLampTuning shipped = slrr::parse_hud_lamp_tuning(
      "ABS       0.000   0.000   1.000\r\n"
      "ASR       0.000   0.000   1.000\r\n"
      "HBRAKE    0.000   0.000   1.000\r\n");
  SLRR_REQUIRE(shipped.abs.dx == 0.f && shipped.asr.dx == 0.f &&
               shipped.handbrake.dx == 0.f);
  SLRR_REQUIRE(shipped.abs.scale == 1.f && shipped.asr.scale == 1.f &&
               shipped.handbrake.scale == 1.f);

  // Each lamp takes its own numbers, and the names are case insensitive.
  const slrr::HudLampTuning moved = slrr::parse_hud_lamp_tuning(
      "# columns: right up size\r\n"
      "abs    -0.004   0.000   1.000\r\n"
      "Asr     0.006   0.002   1.100\r\n"
      "hbrake  0.010  -0.003   0.950\r\n");
  std::printf("[lamps] abs %.3f asr %.3f/%.3f hb %.3f/size %.3f\n",
              double(moved.abs.dx), double(moved.asr.dx),
              double(moved.asr.dy), double(moved.handbrake.dx),
              double(moved.handbrake.scale));
  SLRR_REQUIRE(moved.abs.dx < -.003f && moved.abs.dx > -.005f);
  SLRR_REQUIRE(moved.abs.dy == 0.f && moved.abs.scale == 1.f);
  SLRR_REQUIRE(moved.asr.dx > .005f && moved.asr.dy > .001f);
  SLRR_REQUIRE(moved.asr.scale > 1.09f && moved.asr.scale < 1.11f);
  SLRR_REQUIRE(moved.handbrake.dx > .009f && moved.handbrake.dy < -.002f);
  SLRR_REQUIRE(moved.handbrake.scale > .94f && moved.handbrake.scale < .96f);

  // The fourth number squashes one icon vertically without touching the width,
  // and leaving it out means "as tall as the size says".
  const slrr::HudLampTuning flat = slrr::parse_hud_lamp_tuning(
      "ABS 0 0 1 0.900\nASR 0 0 1.200\nHBRAKE 0 0 1 1.100\n");
  SLRR_REQUIRE(flat.abs.height > .89f && flat.abs.height < .91f);
  SLRR_REQUIRE(flat.asr.height == 1.f && flat.asr.scale > 1.19f);
  SLRR_REQUIRE(flat.handbrake.height > 1.09f);

  // Only the lamps that have a complete line move, the rest keep the measured
  // spot: a line without a lamp name and a named line with two numbers do
  // nothing, and a zero size falls back to 1 because it would collapse the icon
  // (the shift on that line still applies).
  const slrr::HudLampTuning partial = slrr::parse_hud_lamp_tuning(
      "0.02 0.03 1\nABS 0.01\nHBRAKE 0.02 0.03 0\n");
  SLRR_REQUIRE(partial.abs.dx == 0.f && partial.abs.scale == 1.f);
  SLRR_REQUIRE(partial.handbrake.dx > .019f && partial.handbrake.dx < .021f);
  SLRR_REQUIRE(partial.handbrake.scale == 1.f && partial.asr.dx == 0.f);
  SLRR_REQUIRE(!slrr::load_hud_lamp_tuning("Z:/no/such/dir").abs.dx);
}

SLRR_TEST(the_shipped_lamp_file_is_the_one_the_game_reads) {
  const std::string root = slrr::find_game_root(nullptr);
  SLRR_REQUIRE(!root.empty());
  const slrr::HudLampTuning shipped = slrr::load_hud_lamp_tuning(root);
  std::printf("[lamps] root %s -> abs %.3f asr %.3f hb %.3f\n", root.c_str(),
              double(shipped.abs.dx), double(shipped.asr.dx),
              double(shipped.handbrake.dx));
  // The shipped file carries the row that was lined up with the labels on
  // screen, so the numbers are the driver's nudge, not zeros: a few thousandths
  // to the right, no vertical shift, no resize.
  for (const slrr::HudLampOffset* lamp :
       {&shipped.abs, &shipped.asr, &shipped.handbrake}) {
    SLRR_REQUIRE(lamp->dx > -.05f && lamp->dx < .05f);
    SLRR_REQUIRE(lamp->dy == 0.f && lamp->scale == 1.f && lamp->height == 1.f);
  }
}

int main() {
  const slrr::standalone::Test tests[] = {
      lamps_resolve_to_the_panel_quads_with_the_panel_atlas,
      the_lamp_row_lands_along_the_bottom_right_corner,
      hud_lamps_file_moves_each_lamp_without_a_rebuild,
      the_shipped_lamp_file_is_the_one_the_game_reads,
  };
  return slrr::standalone::run("hud_lamps", tests);
}

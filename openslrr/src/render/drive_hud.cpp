#include "slrr/drive_hud.hpp"

#include "slrr/host_game_options.hpp"
#include "slrr/object.hpp"
#include "slrr/part_runtime.hpp"
#include "slrr/slrr_font.hpp"

#include <cstdio>
#include <cstring>

namespace slrr {
namespace {

// Stable key for the one line this file owns. `slrr_text_create` is keyed by
// pointer, so the address of a file-static is enough -- the Java text objects use
// their own addresses the same way.
int g_hud_key = 0;

bool is_interface_section(const std::string& section) {
  static const char* const kInterface[] = {"Menu", "Splash", "Loading"};
  for (const char* word : kInterface)
    if (section.find(word) != std::string::npos) return true;
  return false;
}

void hide() {
  void* key = &g_hud_key;
  slrr_text_set_visible(key, false);
  // Still needs an update: visibility lives on the OSD copy, and leaving the
  // previous copy in the draw list would keep the last numbers on screen.
  slrr_text_update(key);
}

}  // namespace

void drive_hud_tick(const std::string& section) {
  if (!host_game_options().driving.drive_hud) return hide();
  if (is_interface_section(section)) return hide();
  Object* chassis = part_controlled_chassis();
  if (!chassis) return hide();
  PartPhysicsState state;
  if (!part_physics_state(chassis, state) || state.ticks == 0) return hide();
  char line[96];
  std::snprintf(line, sizeof line, "%.0f km/h    %.0f rpm    gear %d",
                state.speed_kph, state.rpm, state.gear);
  void* key = &g_hud_key;
  void* font = slrr_font_ui_medium();
  if (!font || !slrr_font_ready(font)) return hide();
  // Created every frame rather than once: a section change clears the whole text
  // store, so a cached key would go silent for the rest of the session.
  slrr_text_create(key, font, .66f, -.02f);
  slrr_text_set_font_slot(key, SlrrFontSlot::Medium);
  slrr_text_set_align(key, 1);  // centre: the line grows both ways as numbers gain
                                // digits, which keeps its centre still
  slrr_text_set_color(key, 0xFFF0F0F0u);
  slrr_text_set_string(key, line);
  slrr_text_update(key);
}

}  // namespace slrr

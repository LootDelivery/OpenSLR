#include "slrr/camera_overlay.hpp"

#include "slrr/camera_settings.hpp"
#include "slrr/slrr_font.hpp"

#include <algorithm>
#include <cstdio>

namespace slrr {
namespace {

// One key per line (see drive_hud.cpp). Text store is cleared on section
// changes, so the lines are re-created every tick while visible.
int g_key_title = 0;
int g_key_hint = 0;
int g_key_rows[5] = {0, 0, 0, 0, 0};
int g_key_foot = 0;

int g_drag_row = -1;

constexpr float kPanelX = 0.16f;     // panel left (OSD coords, -1..1)
constexpr float kTrackX0 = 0.40f;    // value track start
constexpr float kTrackX1 = 0.92f;    // value track end
constexpr float kTitleY = -0.84f;
constexpr float kRowY0 = -0.70f;     // first row baseline
constexpr float kRowStep = 0.095f;
constexpr float kRowHalfH = 0.045f;
constexpr int kRows = 5;

bool in(const char* section, const char* word) {
  return section && std::string(section).find(word) != std::string::npos;
}

float row_y(int row) { return kRowY0 + row * kRowStep; }

int row_at(float osd_x, float osd_y) {
  if (osd_x < kPanelX || osd_x > kTrackX1) return -1;
  for (int row = 0; row < kRows; ++row) {
    const float y = row_y(row);
    if (osd_y >= y - kRowHalfH && osd_y <= y + kRowHalfH) return row;
  }
  return -1;
}

float row_value(int row, float t) {
  const CameraSettings s = camera_settings();
  switch (row) {
    case 0: return 20.f + t * (90.f - 20.f);
    case 1: return 1.f + t * (40.f - 1.f);
    case 2: return 0.f + t * (5.f - 0.f);
    case 3: return -0.6f + t * (0.6f + 0.6f);
    case 4: return 0.2f + t * (3.f - 0.2f);
    default: return 0.f;
  }
  (void)s;
}

void apply_row(int row, float osd_x) {
  if (row < 0 || row >= kRows) return;
  const float t = std::clamp((osd_x - kTrackX0) / (kTrackX1 - kTrackX0), 0.f, 1.f);
  CameraSettings s = camera_settings();
  switch (row) {
    case 0: s.fov = row_value(0, t); break;
    case 1: s.distance = row_value(1, t); break;
    case 2: s.lift = row_value(2, t); break;
    case 3: s.tilt = row_value(3, t); break;
    case 4: s.return_speed = row_value(4, t); break;
  }
  camera_settings_set(s);
}

void set_line(int* key, const char* text, float x, float y, uint32_t color) {
  void* font = slrr_font_ui_medium();
  if (!font || !slrr_font_ready(font)) return;
  void* k = key;
  slrr_text_create(k, font, x, y);
  slrr_text_set_font_slot(k, SlrrFontSlot::Medium);
  slrr_text_set_align(k, 0);
  slrr_text_set_color(k, color);
  slrr_text_set_string(k, text);
  slrr_text_update(k);
}

void hide_line(int* key) {
  void* k = key;
  slrr_text_set_visible(k, false);
  slrr_text_update(k);
}

void hide_panel() {
  hide_line(&g_key_title);
  hide_line(&g_key_hint);
  hide_line(&g_key_foot);
  for (int row = 0; row < kRows; ++row) hide_line(&g_key_rows[row]);
}

}  // namespace

bool camera_overlay_allowed(const std::string& section) {
  // Driving sections only: the garage showroom camera is untouched.
  return in(section.c_str(), "City") || in(section.c_str(), "Valocity") ||
         in(section.c_str(), "TestTrack");
}

void camera_overlay_tick(const std::string& section) {
  if (!camera_panel_open() || !camera_overlay_allowed(section)) {
    if (camera_panel_open() && !camera_overlay_allowed(section))
      camera_panel_set_open(false);
    g_drag_row = -1;
    hide_panel();
    return;
  }
  const CameraSettings s = camera_settings();
  set_line(&g_key_title, "CAMERA (F8)", kPanelX, kTitleY, 0xFFFFC860u);
  set_line(&g_key_hint, "drag a row to change it, F8 closes", kPanelX,
           kTitleY + 0.07f, 0xFFB0B0B0u);
  char line[96];
  const float v[] = {s.fov, s.distance, s.lift, s.tilt, s.return_speed};
  const char* fmt[] = {"FOV %.0f deg", "DISTANCE %.1f m", "LIFT %.2f m",
                       "TILT %+.2f rad", "RETURN SPEED %.2f"};
  for (int row = 0; row < kRows; ++row) {
    std::snprintf(line, sizeof line, fmt[row], (double)v[row]);
    const uint32_t color =
        row == g_drag_row ? 0xFFFFC860u : 0xFFF0F0F0u;
    set_line(&g_key_rows[row], line, kPanelX, row_y(row), color);
  }
  set_line(&g_key_foot, "main camera only, cockpit stays on cfg", kPanelX,
           row_y(kRows - 1) + 0.09f, 0xFFB0B0B0u);
}

bool camera_overlay_mouse_down(float osd_x, float osd_y) {
  if (!camera_panel_open()) return false;
  const int row = row_at(osd_x, osd_y);
  if (row < 0) return false;
  g_drag_row = row;
  apply_row(row, osd_x);
  return true;
}

bool camera_overlay_mouse_move(float osd_x, float osd_y) {
  if (g_drag_row < 0 || !camera_panel_open()) return false;
  apply_row(g_drag_row, osd_x);
  return true;
}

void camera_overlay_mouse_up() { g_drag_row = -1; }
bool camera_overlay_dragging() { return g_drag_row >= 0; }

}  // namespace slrr

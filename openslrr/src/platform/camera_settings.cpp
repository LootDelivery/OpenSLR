// Chase-camera settings (F8 panel): tuning lives here, not in any .cfg,
// and persists in save/game/camera_settings beside the stock options.
#include "slrr/camera_settings.hpp"

#include <algorithm>
#include <cmath>
#include <cstdio>
#include <fstream>
#include <string>

namespace slrr {
namespace {
CameraSettings g_settings;
bool g_panel_open = false;
std::string g_path;

CameraSettings sanitize(CameraSettings s) {
  s.fov = std::clamp(s.fov, 20.f, 90.f);
  s.distance = std::clamp(s.distance, 1.f, 40.f);
  s.lift = std::clamp(s.lift, 0.f, 5.f);
  s.tilt = std::clamp(s.tilt, -0.6f, 0.6f);
  s.return_speed = std::clamp(s.return_speed, 0.2f, 3.f);
  return s;
}
}  // namespace

const CameraSettings& camera_settings() { return g_settings; }

void camera_settings_set(const CameraSettings& settings) {
  g_settings = sanitize(settings);
  camera_settings_save();
}

void camera_settings_load(const std::string& game_root) {
  std::string root = game_root;
  if (!root.empty() && root.back() != '\\' && root.back() != '/')
    root.push_back('\\');
  g_path = root + "save\\game\\camera_settings";
  std::ifstream in(g_path);
  std::string line;
  while (std::getline(in, line)) {
    const auto at = line.find('=');
    if (at == std::string::npos) continue;
    const float value = std::atof(line.c_str() + at + 1);
    if (!std::isfinite(value)) continue;
    const std::string key = line.substr(0, at);
    if (key == "fov") g_settings.fov = value;
    else if (key == "distance") g_settings.distance = value;
    else if (key == "lift") g_settings.lift = value;
    else if (key == "tilt") g_settings.tilt = value;
    else if (key == "return_speed") g_settings.return_speed = value;
  }
  g_settings = sanitize(g_settings);
}

void camera_settings_save() {
  if (g_path.empty()) return;
  std::ofstream out(g_path, std::ios::trunc);
  out << "fov=" << g_settings.fov << '\n';
  out << "distance=" << g_settings.distance << '\n';
  out << "lift=" << g_settings.lift << '\n';
  out << "tilt=" << g_settings.tilt << '\n';
  out << "return_speed=" << g_settings.return_speed << '\n';
  if (!out)
    std::fprintf(stderr, "[options] could not save %s\n", g_path.c_str());
}

bool camera_panel_open() { return g_panel_open; }
void camera_panel_set_open(bool open) { g_panel_open = open; }

}  // namespace slrr

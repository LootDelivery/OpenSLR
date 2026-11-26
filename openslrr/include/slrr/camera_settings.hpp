// Chase-camera settings (F8 panel): the main camera is tuned in-game,
// independent of any .cfg. Secondary cameras (cockpit, bumper, ...) keep
// the original cfg behaviour. Values persist in save/game/camera_settings.
#pragma once

#include <string>

namespace slrr {

struct CameraSettings {
  float fov = 42.f;          // chaser field of view, degrees (20..90)
  float distance = 7.f;      // chase distance, metres (1..40)
  float lift = 1.4f;         // chase height above the hub, metres (0..5)
  float tilt = 0.f;          // added base elevation, radians (-0.6..0.6)
  float return_speed = 2.2f; // orbit-offset decay rate, 1/s (0.2..3)
  bool operator==(const CameraSettings& o) const = default;
};

const CameraSettings& camera_settings();
void camera_settings_set(const CameraSettings& settings);
void camera_settings_load(const std::string& game_root);
void camera_settings_save();

// F8 camera panel visibility (garage and driving only; mainloop owns it and
// the render backend reads it to draw the window and capture the mouse).
bool camera_panel_open();
void camera_panel_set_open(bool open);

}  // namespace slrr

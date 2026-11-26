#pragma once

// F8 chase-camera panel, drawn with the game's own OSD text (the same store
// the drive HUD uses), never with the ImGui debug overlay. It opens in the
// garage and while driving only; everywhere else F8 does nothing.

#include <string>

namespace slrr {

// True in the garage and in the driving sections (City / Valocity / TestTrack).
bool camera_overlay_allowed(const std::string& section);

// Draws (or hides) the panel. Called every frame from mainloop.
void camera_overlay_tick(const std::string& section);

// Mouse handlers, called from the window callback before the game OSD gets
// the event. Coordinates are OSD text-space (osd_pixel_to_text output).
// Return true when the event was consumed by the panel.
bool camera_overlay_mouse_down(float osd_x, float osd_y);
bool camera_overlay_mouse_move(float osd_x, float osd_y);
void camera_overlay_mouse_up();
bool camera_overlay_dragging();

}  // namespace slrr

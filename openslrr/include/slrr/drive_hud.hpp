#pragma once

#include <string>

namespace slrr {

// The driving readout: speed, rpm and gear, drawn as one line of the game's own
// OSD text. Not an overlay window -- the game already owns a text layer that
// knows about menus, groups and layers, and a second interface pass drawn into
// it stole the menu's labels.
//
// `section` is the current Java section name: the readout is only for a section
// with a car in it, and a menu must never show it.
void drive_hud_tick(const std::string& section);

}  // namespace slrr

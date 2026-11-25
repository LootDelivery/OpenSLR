#pragma once

#include <cctype>
#include <cstdio>
#include <cstdlib>
#include <string>

namespace slrr {

// Placement of the stock ABS / ASR / handbrake LEDs drawn by the OSD. The stock
// meshes are positioned by the engine, and the car config ships them as
// "osd_abs 0xFFFFFFFF 0 0 0", so this is the one place a driver can nudge the
// row: <game root>/hud_lamps.txt holds "dx dy scale" in OSD clip units (dx
// right, dy up, 0.01 is half a percent of the screen; scale multiplies the
// icon). Missing file means "as measured" -- 0 0 1.
struct HudLampTuning {
  float dx = 0.f;
  float dy = 0.f;
  float scale = 1.f;
};

inline HudLampTuning parse_hud_lamp_tuning(const char* text) {
  HudLampTuning result;
  float values[3] = {0.f, 0.f, 1.f};
  int read = 0;
  for (const char* p = text; p && *p && read < 3;) {
    if (*p == '#') {
      while (*p && *p != '\n') ++p;
      continue;
    }
    if (std::isdigit(static_cast<unsigned char>(*p)) || *p == '-' ||
        *p == '+' || *p == '.') {
      char* end = nullptr;
      const float value = std::strtof(p, &end);
      if (end != p) {
        values[read++] = value;
        p = end;
        continue;
      }
    }
    ++p;
  }
  if (read == 3) {
    result.dx = values[0];
    result.dy = values[1];
    result.scale = values[2] > 0.f ? values[2] : 1.f;
  }
  return result;
}

inline HudLampTuning load_hud_lamp_tuning(const std::string& game_root) {
  const std::string path =
      (game_root.empty() ? std::string(".") : game_root) + "/hud_lamps.txt";
  std::FILE* file = std::fopen(path.c_str(), "rb");
  if (!file) return {};
  std::string text;
  char chunk[512];
  while (const size_t got = std::fread(chunk, 1, sizeof chunk, file))
    text.append(chunk, got);
  std::fclose(file);
  return parse_hud_lamp_tuning(text.c_str());
}

}  // namespace slrr

#pragma once

#include <cctype>
#include <cstdio>
#include <cstdlib>
#include <string>

namespace slrr {

// Placement of one stock LED (ABS / ASR / handbrake) drawn by the OSD. The
// stock meshes are positioned by the engine and the car config ships them as
// "osd_abs 0xFFFFFFFF 0 0 0", so <game root>/hud_lamps.txt is the only place a
// driver can move the row. It is deliberately one line per lamp, named:
//
//   ABS       0.000   0.000   1.000
//   ASR       0.000   0.000   1.000
//   HBRAKE    0.000   0.000   1.000
//
// columns are right / up / size: the first two are OSD clip units (0.01 is
// half a percent of the screen, so the nudge looks the same at any
// resolution), the third multiplies the icon. Every line is optional; a lamp
// without a line keeps the measured spot. A missing file means the same.
struct HudLampOffset {
  float dx = 0.f;
  float dy = 0.f;
  float scale = 1.f;
};

struct HudLampTuning {
  HudLampOffset abs;
  HudLampOffset asr;
  HudLampOffset handbrake;
};

inline HudLampTuning parse_hud_lamp_tuning(const char* text) {
  HudLampTuning result;
  float values[3] = {0.f, 0.f, 1.f};
  int count = 0;
  HudLampOffset* target = nullptr;
  const auto commit = [&] {
    if (target && count == 3) {
      target->dx = values[0];
      target->dy = values[1];
      target->scale = values[2] > 0.f ? values[2] : 1.f;
    }
    count = 0;
    target = nullptr;
  };
  for (const char* p = text; p && *p;) {
    if (*p == '#') {
      while (*p && *p != '\n') ++p;
      continue;
    }
    if (*p == '\n' || *p == '\r') {
      commit();
      ++p;
      continue;
    }
    if (std::isspace(static_cast<unsigned char>(*p))) {
      ++p;
      continue;
    }
    if (std::isdigit(static_cast<unsigned char>(*p)) || *p == '-' ||
        *p == '+' || *p == '.') {
      char* end = nullptr;
      const float value = std::strtof(p, &end);
      if (end != p) {
        if (count < 3) values[count++] = value;
        p = end;
        continue;
      }
    }
    const char* begin = p;
    while (*p && !std::isspace(static_cast<unsigned char>(*p)) && *p != '#')
      ++p;
    std::string word(begin, p);
    for (char& c : word)
      c = static_cast<char>(std::tolower(static_cast<unsigned char>(c)));
    count = 0;
    if (word == "abs")
      target = &result.abs;
    else if (word == "asr")
      target = &result.asr;
    else if (word == "hbrake" || word == "handbrake" || word == "hb")
      target = &result.handbrake;
    else
      target = nullptr;  // unknown word: the line is skipped
  }
  commit();
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

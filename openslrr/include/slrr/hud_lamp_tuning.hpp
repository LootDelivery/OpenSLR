#pragma once

#include <cctype>
#include <cstdio>
#include <cstdlib>
#include <string>

namespace slrr {

// Placement nudge for one stock LED (ABS / ASR / handbrake) drawn by the OSD.
// The stock meshes are positioned by the engine and the car config ships them
// as "osd_abs 0xFFFFFFFF 0 0 0", so <game root>/hud_lamps.txt is the only
// place a driver can move the row:
//
//   0.01 0 1              base for all three lamps   (dx dy scale)
//   abs     0.005 0 1     extra, only for this lamp
//   asr     0 0 1
//   hbrake  0 0 1
//
// dx moves right, dy moves up, both in OSD clip units (0.01 is half a percent
// of the screen, so it looks the same at any resolution); scale multiplies the
// icon. The per lamp lines add to the base line; a missing file or "0 0 1"
// keeps the measured row.
struct HudLampOffset {
  float dx = 0.f;
  float dy = 0.f;
  float scale = 1.f;
};

struct HudLampTuning {
  HudLampOffset all;        // added to every lamp
  HudLampOffset abs;        // added to the ABS lamp only
  HudLampOffset asr;        // added to the ASR lamp only
  HudLampOffset handbrake;  // added to the handbrake lamp only
};

inline HudLampTuning parse_hud_lamp_tuning(const char* text) {
  struct Slot {
    float value[3] = {0.f, 0.f, 1.f};
    int count = 0;
  };
  HudLampTuning result;
  Slot all, abs, asr, handbrake;
  Slot* target = &all;
  for (const char* p = text; p && *p;) {
    if (*p == '#') {
      while (*p && *p != '\n') ++p;
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
        if (target->count < 3) target->value[target->count++] = value;
        p = end;
        continue;
      }
    }
    const char* begin = p;
    while (*p && !std::isspace(static_cast<unsigned char>(*p)) && *p != '#')
      ++p;
    std::string word(begin, p);
    for (char& c : word) c = static_cast<char>(std::tolower(static_cast<unsigned char>(c)));
    if (word == "all")
      target = &all;
    else if (word == "abs")
      target = &abs;
    else if (word == "asr")
      target = &asr;
    else if (word == "hbrake" || word == "handbrake" || word == "hb")
      target = &handbrake;
  }
  const auto commit = [](const Slot& slot, HudLampOffset& out) {
    if (slot.count != 3) return;
    out.dx = slot.value[0];
    out.dy = slot.value[1];
    out.scale = slot.value[2] > 0.f ? slot.value[2] : 1.f;
  };
  commit(all, result.all);
  commit(abs, result.abs);
  commit(asr, result.asr);
  commit(handbrake, result.handbrake);
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

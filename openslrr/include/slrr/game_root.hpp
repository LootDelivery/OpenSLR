#pragma once

#include <string>

namespace slrr {

// Locate an SLRR install (system/Scripts/lang/System.class present).
// Candidates: explicit path, cwd, data/game, ./game, ../game, common Steam layout.
std::string find_game_root(const char* explicit_path = nullptr);

bool game_root_looks_valid(const std::string& root);

// Locate a script patch (sl/Scripts/game present). Candidates: SLRR_PATCH_DIR,
// the exe's own folder and its parents, and the openslrr tree next to the game.
// An overlay leaves the install untouched while the port runs patched scripts.
std::string find_patch_root(const std::string& game_root);

}  // namespace slrr

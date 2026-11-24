#include "slrr/game_root.hpp"

#include <cstdio>
#include <cstdlib>
#include <filesystem>
#include <vector>
#ifdef _WIN32
#include <windows.h>
#endif

namespace fs = std::filesystem;

namespace slrr {
namespace {

bool probe(const fs::path& root) {
  const fs::path marker = root / "system" / "Scripts" / "lang" / "System.class";
  std::error_code ec;
  return fs::is_regular_file(marker, ec);
}

std::string absolute_root(const fs::path& path) {
  std::error_code ec;
  const fs::path absolute = fs::absolute(path, ec);
  return ec ? std::string{} : absolute.lexically_normal().make_preferred().string();
}

}  // namespace

bool game_root_looks_valid(const std::string& root) {
  if (root.empty()) return false;
  return probe(fs::path(root));
}

std::string find_patch_root(const std::string& game_root) {
  std::error_code ec;
  std::vector<fs::path> candidates;
  if (const char* forced = std::getenv("SLRR_PATCH_DIR"))
    if (forced[0]) candidates.emplace_back(forced);
  if (!game_root.empty())
    candidates.push_back(fs::path(game_root) / ".." / "openslrr" / "patch");
  fs::path exe;
  if (char buffer[MAX_PATH]; GetModuleFileNameA(nullptr, buffer, MAX_PATH))
    exe = fs::path(buffer).parent_path();
  if (!exe.empty()) {
    candidates.push_back(exe / "patch");
    candidates.push_back(exe / ".." / "patch");
    candidates.push_back(exe / ".." / ".." / "patch");
  }
  for (const auto& candidate : candidates) {
    if (fs::is_regular_file(candidate / "sl" / "Scripts" / "game" / "OptionsDialog.class", ec))
      return absolute_root(candidate);
  }
  return {};
}

std::string find_game_root(const char* explicit_path) {
  if (explicit_path && explicit_path[0]) {
    return probe(explicit_path) ? absolute_root(explicit_path) : std::string{};
  }

  static const char* kCandidates[] = {
      ".",
      "data/game",
      "game",
      "../game",
      "../../game",
      "Street Legal Racing - Redline",
      "../Street Legal Racing - Redline",
  };

  for (const char* c : kCandidates) {
    if (probe(c)) return absolute_root(c);
  }
  return {};
}

}  // namespace slrr

// Native localization manager (RU/EN/PL minimum).
//
// Framework follows the Localization mod's script API (localization.java /
// LocalizationMap / LocalizationData) but lives in the host: no script
// patches, no binary hooks. UI text flows through slrr_text_set_string,
// which translates exact-match English phrases for the selected language.
// The language id persists in save/game/localization as one 4-byte int,
// the same format the Localization mod writes, so both can read it.
#pragma once

namespace slrr {

class Jvm;

enum SlrrLanguage {
  SLRR_LANG_EN = 0,
  SLRR_LANG_RU = 1,
  SLRR_LANG_PL = 2,
  SLRR_LANG_COUNT = 3
};

// Current language; loads save/game/localization on first use (EN default).
int slrr_loc_language();
void slrr_loc_set_language(int lang);  // clamps, saves, nothing else
// Binds save/game/localization to the game root. Call once after the Jvm
// knows its root, before any lookup.
void slrr_loc_set_game_root(const Jvm& jvm);
const char* slrr_loc_language_name(int lang);  // native-language caption

// Exact-match translation of a full English UI phrase. Returns the current
// language's entry, the English entry, or the input text when unknown.
// The input stays owned by the caller; the result points at static storage.
const char* slrr_loc_translate(const char* english);

// Script-facing lookup for mods: dictionary key + fallback (mod-style keys
// are resolved through the built-in table; unknown keys return fallback).
const char* slrr_loc_get(const char* key, const char* fallback);
const char* slrr_loc_get_mod(const char* mod_id, const char* key,
                             const char* fallback);

// Registers a script LocalizationMap bundle (the Localization mod's
// dictionary contract: locale code -> map(short key -> translation)). The
// dictionary is a script object (java.game.LocalizationMap instance); keys
// get the "<modId>." namespace, exactly like the script mergeDictionaries.
void slrr_loc_register_mod(const char* mod_id, const void* dictionary);

}  // namespace slrr

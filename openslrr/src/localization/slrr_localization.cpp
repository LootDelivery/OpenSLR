// Native localization manager. See include/slrr/slrr_localization.hpp.
//
// Dictionary layout mirrors the Localization mod: language id -> map of
// English UI phrase (or dictionary key) -> translated text. Entries are
// exact-match; lookups fall back EN -> input text, like the mod's
// getByKey chain (selected language -> English -> original).
#include "slrr/slrr_localization.hpp"
#include "slrr/game_root.hpp"
#include "slrr/jvm.hpp"
#include "slrr/object.hpp"
#include "slrr/types.hpp"
#include <array>
#include <cstdio>
#include <string>
#include <string_view>
#include <unordered_map>
#include <vector>

namespace slrr {
namespace {

struct Entry {
  std::string_view en;
  std::string_view ru;
  std::string_view pl;
};

// Core UI strings. The English member is the lookup key: it must match the
// exact full string the scripts display, including casing.
constexpr Entry kEntries[] = {
    // Main menu (MWM 2.2.1 MainMenuDialog).
    {"MAIN MENU", "ГЛАВНОЕ МЕНЮ", "MENU GŁÓWNE"},
    {"NEW CAREER", "НОВАЯ КАРЬЕРА", "NOWA KARIERA"},
    {"LOAD CAREER", "ЗАГРУЗИТЬ КАРЬЕРУ", "WCZYTAJ KARIERĘ"},
    {"DELETE CAREER", "УДАЛИТЬ КАРЬЕРУ", "USUŃ KARIERĘ"},
    {"BACK TO GARAGE", "ОБРАТНО В ГАРАЖ", "POWRÓT DO GARAŻU"},
    {"LOAD CAR", "ЗАГРУЗИТЬ МАШИНУ", "WCZYTAJ SAMOCHÓD"},
    {"SAVE CAR", "СОХРАНИТЬ МАШИНУ", "ZAPISZ SAMOCHÓD"},
    {"SELECT CAR", "ВЫБОР МАШИНЫ", "WYBÓR SAMOCHODU"},
    {"QUICKRACE", "БЫСТРАЯ ГОНКА", "SZYBKI WYŚCIG"},
    {"SELECT TRACK", "ВЫБОР ТРАССЫ", "WYBÓR TRASY"},
    {"DEMO", "ДЕМО", "DEMO"},
    {"OPTIONS", "НАСТРОЙКИ", "OPCJE"},
    {"CREDITS", "ОБ АВТОРАХ", "TWÓRCY"},
    {"EXIT GAME", "ВЫХОД ИЗ ИГРЫ", "WYJŚCIE Z GRY"},
    {"EXIT", "ВЫХОД", "WYJŚCIE"},
    {"BACK", "НАЗАД", "WSTECZ"},
    {"OK", "ОК", "OK"},
    {"CANCEL", "ОТМЕНА", "ANULUJ"},
    {"YES", "ДА", "TAK"},
    {"NO", "НЕТ", "NIE"},
    // Button bars arrive as one semicolon-joined string (Dialog2 patterns).
    {"OK;Cancel", "ОК;Отмена", "OK;Anuluj"},
    {";Cancel", ";Отмена", ";Anuluj"},
    {"YES;NO", "ДА;НЕТ", "TAK;NIE"},
    {"ABOUT", "О МОДЕ", "O MODZIE"},
    {"RANDOM CAR", "СЛУЧАЙНАЯ МАШИНА", "LOSOWY SAMOCHÓD"},
    {"SELECT DRIVER", "ВЫБОР ВОДИТЕЛЯ", "WYBÓR KIEROWCY"},
    {"TAKE IT", "ЗАБРАТЬ", "BIERZ"},
    {"NO DRIVER", "БЕЗ ВОДИТЕЛЯ", "BEZ KIEROWCY"},
    {"LIGHTS ON/OFF", "СВЕТ ВКЛ/ВЫКЛ", "ŚWIATŁA WŁ/WYŁ"},

    // Garage (Garage.class, MWM 2.2.1) — mode tooltips and plates. Keep the
    // game's exact spellings ("relase" is upstream).
    {"INFO", "ИНФО", "INFO"},
    {"SURCHARGE", "ДОПЛАТА", "DOPŁATA"},
    {"Jack", "Домкрат", "Podnośnik"},
    {"Jack Animate Refresher", "Обновление домкрата", "Podnośnik — odświeżanie"},
    {"Install/Remove parts", "Установка/снятие деталей", "Montaż/demontaż części"},
    {"Fine tune specific parts", "Тонкая настройка деталей", "Regulacja części"},
    {"Body paint", "Покраска кузова", "Lakierowanie"},
    {"Test engine and steering", "Проверка двигателя и руля",
     "Test silnika i kierownicy"},
    {"Click to relase clutch pedal", "Клик, чтобы отпустить сцепление",
     "Kliknij, by zwolnić sprzęgło"},
    {"brake", "ТОРМОЗ", "HAMULEC"},

    // Common dialogs (NoYesDialog flows, stock texts).
    {"Do you really want to Exit?",
     "Действительно выйти?",
     "Czy na pewno chcesz wyjść?"},
    {"Do you really want to Exit? \n \n Note that your current career will "
     "be automatically saved.",
     "Действительно выйти? \n \n Текущий карьер будет автоматически "
     "сохранён.",
     "Czy na pewno chcesz wyjść? \n \n Uwaga: Twoja obecna kariera zostanie "
     "automatycznie zapisana."},
    {"Do you really want to delete your ongoing career?",
     "Действительно удалить текущую карьеру?",
     "Czy na pewno chcesz usunąć trwającą karierę?"},
    {"Do you really want to continue?",
     "Действительно продолжить?",
     "Czy na pewno chcesz kontynuować?"},
    {"Delete career", "Удалить карьеру", "Usuń karierę"},
    {"Overwrite existing file?", "Перезаписать файл?",
     "Nadpisać istniejący plik?"},
    {"File system error.", "Ошибка файловой системы.", "Błąd systemu plików."},
    {"FileName:", "Имя файла:", "Nazwa pliku:"},
};

// mod.getMod dictionary: "modid.key" -> {en, ru, pl}. Mods may also register
// dictionaries at runtime through the java.game.localization natives; those
// merge into kRuntimeMods and take precedence over nothing (the built-in
// table is engine-owned UI text, runtime entries are mod-owned).
using Map = std::unordered_map<std::string, std::array<std::string, 3>>;
Map& runtime_mods() {
  static Map mods;
  return mods;
}

struct State {
  int language = SLRR_LANG_EN;
  bool loaded = false;
  std::string root;  // game root for save/game/localization
};

State& state() {
  static State s;
  return s;
}

std::string language_file() {
  return state().root.empty() ? std::string("save/game/localization")
                              : state().root + "/save/game/localization";
}

int clamp_language(int lang) {
  return lang >= 0 && lang < SLRR_LANG_COUNT ? lang : SLRR_LANG_EN;
}

void load_language() {
  State& s = state();
  s.loaded = true;
  s.language = SLRR_LANG_EN;
  std::FILE* f = std::fopen(language_file().c_str(), "rb");
  if (!f) return;
  int value = -1;
  if (std::fread(&value, sizeof(value), 1, f) != 1) value = -1;
  std::fclose(f);
  s.language = clamp_language(value);
}

// The save path resolves against the game root; set once before first load.
void init_root(const Jvm* jvm) {
  if (jvm) state().root = jvm->game_root();
}

void save_language() {
  State& s = state();
  std::FILE* f = std::fopen(language_file().c_str(), "wb");
  if (!f) {
    std::fprintf(stderr, "[loc] cannot save %s\n", language_file().c_str());
    return;
  }
  const int value = s.language;
  std::fwrite(&value, sizeof(value), 1, f);
  std::fclose(f);
}

// One entry per table row; the language column must be valid.
const char* column(const Entry& e, int lang) {
  switch (lang) {
    case SLRR_LANG_RU: return e.ru.empty() ? e.en.data() : e.ru.data();
    case SLRR_LANG_PL: return e.pl.empty() ? e.en.data() : e.pl.data();
    default: return e.en.data();
  }
}

}  // namespace

int slrr_loc_language() {
  State& s = state();
  if (!s.loaded) load_language();
  return s.language;
}

void slrr_loc_set_game_root(const Jvm& jvm) { init_root(&jvm); }

void slrr_loc_set_language(int lang) {
  State& s = state();
  if (!s.loaded) load_language();
  s.language = clamp_language(lang);
  save_language();
}

const char* slrr_loc_language_name(int lang) {
  // Captions are intentionally in their own native language (the mod's
  // getLanguageNames contract).
  static const char* names[SLRR_LANG_COUNT] = {"ENGLISH", "РУССКИЙ", "POLSKI"};
  return names[clamp_language(lang)];
}

const char* slrr_loc_translate(const char* english) {
  if (!english || !*english) return english;
  const int lang = slrr_loc_language();
  if (lang == SLRR_LANG_EN) return english;
  const std::string_view key{english};
  for (const Entry& e : kEntries)
    if (e.en == key) return column(e, lang);
  // Runtime (mod-registered) dictionaries: exact key match, EN fallback.
  const auto& mods = runtime_mods();
  if (auto it = mods.find(std::string{english}); it != mods.end()) {
    const auto& texts = it->second;
    if (!texts[lang].empty()) return texts[lang].c_str();
    if (!texts[SLRR_LANG_EN].empty()) return texts[SLRR_LANG_EN].c_str();
  }
  return english;
}

const char* slrr_loc_get(const char* key, const char* fallback) {
  if (!key || !*key) return fallback ? fallback : "";
  const int lang = slrr_loc_language();
  const std::string_view needle{key};
  for (const Entry& e : kEntries)
    if (e.en == needle) return column(e, lang);
  const auto& mods = runtime_mods();
  if (auto it = mods.find(std::string{key}); it != mods.end()) {
    const auto& texts = it->second;
    if (!texts[lang].empty()) return texts[lang].c_str();
    if (!texts[SLRR_LANG_EN].empty()) return texts[SLRR_LANG_EN].c_str();
  }
  return fallback ? fallback : "";
}

const char* slrr_loc_get_mod(const char* mod_id, const char* key,
                             const char* fallback) {
  if (!mod_id || !*mod_id || !key || !*key) return fallback ? fallback : "";
  const std::string full = std::string{mod_id} + "." + key;
  const int lang = slrr_loc_language();
  const auto& mods = runtime_mods();
  if (auto it = mods.find(full); it != mods.end()) {
    const auto& texts = it->second;
    if (!texts[lang].empty()) return texts[lang].c_str();
    if (!texts[SLRR_LANG_EN].empty()) return texts[SLRR_LANG_EN].c_str();
  }
  return fallback ? fallback : "";
}

namespace {

const Object* field_object(const Object* o, const char* name) {
  if (!o) return nullptr;
  const auto it = o->fields.find(name);
  return it != o->fields.end() && it->second.tag == ValueTag::Obj
      ? it->second.v.o : nullptr;
}

// Reads one Tufa Vector<String> (elementData slots + elementCount).
std::vector<std::string> vector_strings(const Object* vector) {
  if (!vector) return {};
  std::vector<std::string> out;
  const Object* data = field_object(vector, "elementData");
  if (!data) return out;
  int count = 0;
  if (const auto it = vector->fields.find("elementCount");
      it != vector->fields.end() && it->second.tag == ValueTag::Int)
    count = it->second.v.i;
  for (int i = 0; i < count && i < 4096; ++i) {
    if (static_cast<size_t>(i) >= data->slots.size()) break;
    const Value& v = data->slots[static_cast<size_t>(i)];
    if (v.tag == ValueTag::Obj && v.v.o) out.push_back(v.v.o->str);
  }
  return out;
}

}  // namespace

// Registers one script LocalizationMap bundle (the Localization mod's
// contract): locale code -> LocalizationMap(short key -> translation). Keys
// are namespaced with modId, matching the script mergeDictionaries.
void slrr_loc_register_mod(const char* mod_id, const void* dictionary) {
  if (!mod_id || !*mod_id || !dictionary) return;
  const Object* dict = static_cast<const Object*>(dictionary);
  const auto keys = vector_strings(field_object(dict, "mapKeys"));
  const auto values = vector_strings(field_object(dict, "mapValues"));
  auto& mods = runtime_mods();
  for (size_t i = 0; i < keys.size() && i < values.size(); ++i) {
    int lang = SLRR_LANG_EN;
    if (keys[i] == "ru") lang = SLRR_LANG_RU;
    else if (keys[i] == "pl") lang = SLRR_LANG_PL;
    const Object* inner = nullptr;
    // mapValues holds Object entries; valueAt returns the inner map. The
    // Vector<String> reader only handles strings, so read the raw slots.
    const Object* values_vector = field_object(dict, "mapValues");
    const Object* data = field_object(values_vector, "elementData");
    if (data && i < data->slots.size() &&
        data->slots[i].tag == ValueTag::Obj)
      inner = data->slots[i].v.o;
    if (!inner) continue;
    const auto inner_keys = vector_strings(field_object(inner, "mapKeys"));
    const Object* inner_values = field_object(inner, "mapValues");
    const Object* inner_data = field_object(inner_values, "elementData");
    if (!inner_data) continue;
    const int count = static_cast<int>(inner_keys.size());
    for (int k = 0; k < count; ++k) {
      if (k >= static_cast<int>(inner_data->slots.size())) break;
      const Value& v = inner_data->slots[static_cast<size_t>(k)];
      if (v.tag != ValueTag::Obj || !v.v.o || v.v.o->str.empty()) continue;
      const std::string full =
          std::string{mod_id} + "." + inner_keys[static_cast<size_t>(k)];
      mods[full][static_cast<size_t>(lang)] = v.v.o->str;
    }
  }
}

}  // namespace slrr

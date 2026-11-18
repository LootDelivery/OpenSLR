#pragma once

#include <cstdint>
#include <string>
#include <unordered_map>
#include <vector>

namespace slrr {

struct SlrrFontGlyph {
  // Charset mesh quad, tenths of a pixel (PE FUN_0044bdc0). Y grows down.
  float px[4] = {};
  float py[4] = {};
  float tu[4] = {};
  float tv[4] = {};
  float u0 = 0, v0 = 0, u1 = 0, v1 = 0;
  float adv_px = 0;  // vertex[1].x
};

struct SlrrFont {
  std::string name;
  int32_t atlas_w = 0;
  int32_t atlas_h = 0;
  int32_t px_height = 20;
  std::vector<SlrrFontGlyph> glyphs;
  // RGBA8 white RGB + coverage alpha (legacy A8R8G8B8 atlas).
  std::vector<uint8_t> rgba;
  bool ready = false;

  // TTF path (Fonts/fonts.ini → Fonts/families/<family>.ttf).
  bool ttf = false;
  int32_t ttf_px = 0;            // requested pixel height
  float ttf_ascent = 0.f;        // baseline below the line top, px
  float ttf_descent = 0.f;       // below the baseline, px (positive)
  // Shelf packer cursor inside the atlas.
  int32_t shelf_x = 0;
  int32_t shelf_y = 0;
  int32_t shelf_h = 0;
  bool atlas_dirty = false;      // rows [dirty_y0, dirty_y1) need a GPU upload
  int32_t dirty_y0 = 0;
  int32_t dirty_y1 = 0;
  bool atlas_warned = false;     // one 'atlas full' log per font
  // Codepoint-indexed glyphs. Pointers are only stable until the next
  // slrr_font_glyph() call that inserts.
  std::unordered_map<uint32_t, SlrrFontGlyph> tt_glyphs;
  // Opaque stbtt face; owned and defined in slrr_font.cpp.
  struct SlrrTtfFace* ttf_face = nullptr;
};

// Decodes one UTF-8 codepoint, advancing past it. Invalid bytes decode as
// Latin-1 and advance one byte. NUL terminator yields 0 without advancing.
uint32_t slrr_utf8_next(const char*& p);

// Returns the glyph for a codepoint (TTF: rasterized on demand into the
// atlas; legacy charsets: byte-indexed). May return nullptr. Pointers are
// only stable until the next call that inserts into the same font.
const struct SlrrFontGlyph* slrr_font_glyph(void* key, uint32_t cp);

// Marks the atlas rows changed since the last call. (*data) points into the
// font's stable atlas buffer; rows [y0, y0+h) need a GPU upload. Returns
// false when nothing changed since the previous call.
bool slrr_font_take_atlas_update(void* key, const uint8_t** data, int32_t* y0,
                                 int32_t* h);

struct SlrrOsdText {
  void* text_key = nullptr;  // java.render.Text*
  void* font_key = nullptr;  // charset ResourceRef*
  float x = 0.f;
  float y = 0.f;
  uint32_t color = 0xFFFFFFFFu;  // ARGB
  int32_t align = 2;             // Text.ALIGN_LEFT
  int32_t group = 0;             // Osd group (hideGroup / showGroup)
  std::string text;
  bool visible = true;
  // Survives showOnly — stock version line is endGroup'd and never hidden.
  bool pinned = false;
  // Osd that created this line. hide() suppresses it across showOnly.
  void* osd = nullptr;
  bool osd_suppressed = false;
  // hideGroup. Osd.show() must not clear it.
  bool group_hidden = false;
  // Same priority as the Osd viewport, so menu lines sit under the dialog.
  int32_t layer = 0;
  // Credit scroller: drawn before the UI plates so the frame covers it.
  bool behind_ui = false;
};

void slrr_font_set_game_root(const std::string& root);

// Load by filename stem (simple20 / slii24 / …) or frontend RID.
bool slrr_font_load(void* key, const char* name);
bool slrr_font_load_from_rid(void* key, int32_t rid);
bool slrr_font_ready(void* key);
const SlrrFont* slrr_font_get(void* key);
float slrr_font_measure_px(void* key, const char* text);
const char* slrr_font_name_for_rid(int32_t rid);

// Frontend.setFonts pointers — menu tip labels when createText TREE is lean.
void slrr_font_set_ui_keys(void* small, void* medium, void* large);
void* slrr_font_ui_small();
void* slrr_font_ui_medium();
void* slrr_font_ui_large();

// Which Frontend.setFonts slot created this Text (for live rebuild on resize).
enum class SlrrFontSlot : int32_t { None = 0, Small = 1, Medium = 2, Large = 3 };

void slrr_text_create(void* key, void* font, float x, float y);
void slrr_text_set_default_color(uint32_t color);
uint32_t slrr_text_default_color();
void slrr_text_destroy(void* key);
void slrr_text_set_color(void* key, uint32_t argb);
void slrr_text_set_align(void* key, int32_t align);
void slrr_text_set_pos(void* key, float x, float y);
void slrr_text_set_string(void* key, const char* utf8);
void slrr_text_refresh_language();  // re-translate every live text
void slrr_text_set_font(void* key, void* font);
void slrr_text_set_font_slot(void* key, SlrrFontSlot slot);
void slrr_text_set_group(void* key, int32_t group);
void slrr_text_set_pinned(void* key, bool pinned);
void slrr_text_set_visible(void* key, bool visible);
void slrr_text_set_behind_ui(void* key, bool behind);
void slrr_text_set_osd(void* key, void* osd);
void slrr_text_set_osd_visible(void* osd, bool visible);
void slrr_text_set_group_visible(int32_t group, bool visible);
// One menu page: only texts in `group` stay visible.
void slrr_text_show_only_group(int32_t group);
SlrrFontSlot slrr_text_font_slot(void* key);
void slrr_text_update(void* key);  // push into OSD draw list
void slrr_text_clear_all();
// Rebind Small/Medium/Large texts after Frontend.setFonts (resize band change).
void slrr_text_rebind_frontend_fonts(void* small, void* medium, void* large);

const std::vector<SlrrOsdText>& slrr_osd_texts();

}  // namespace slrr

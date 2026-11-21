#include "slrr/gc_roots.hpp"
#include "slrr/slrr_font.hpp"
#include "slrr/slrr_localization.hpp"

#include "slrr/object.hpp"
#include "slrr/osd_registry.hpp"

#include <algorithm>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <fstream>
#include <unordered_map>
#include <vector>

// stb_truetype: the implementation compiled inside bgfx's example imgui.cpp
// allocates through the ImGui context allocator (s_ctx.m_allocator), which is
// not initialized before imguiCreate — rasterizing menu glyphs crashes there.
// Compile our own copy with static (STBTT_DEF=static) linkage and a neutral
// malloc instead.
#define STBTT_STATIC
#define STB_TRUETYPE_IMPLEMENTATION
#define STBTT_malloc(x, u) ((void)(u), malloc(x))
#define STBTT_free(x, u) ((void)(u), free(x))
#include "stb_truetype_static.h"

#if defined(SLRR_HAS_LIBBGFX)
// Implementation lives in ptx_image.cpp (STB_IMAGE_IMPLEMENTATION).
#include <stb/stb_image.h>
#endif

namespace slrr {
namespace {

std::string g_root;
std::unordered_map<void*, SlrrFont> g_fonts;
uint32_t g_text_default_color = 0xffffffffu;

struct TextState {
  void* font = nullptr;
  float x = 0.f;
  float y = 0.f;
  uint32_t color = 0xFFFFFFFFu;
  int32_t align = 2;
  int32_t group = 0;
  SlrrFontSlot font_slot = SlrrFontSlot::None;
  std::string text;
  // The string the script set. Localization rewrites `text` per language;
  // the original stays for slrr_text_refresh_language.
  std::string english;
  bool alive = false;
  bool visible = true;
  bool pinned = false;
  bool behind_ui = false;
  void* osd = nullptr;
  bool osd_suppressed = false;
  bool group_hidden = false;
  int32_t layer = 0;
};
std::unordered_map<void*, TextState> g_texts;
std::vector<SlrrOsdText> g_osd_text;

bool read_file(const std::string& path, std::vector<uint8_t>* out) {
  if (!out) return false;
  std::ifstream f(path, std::ios::binary);
  if (!f) return false;
  f.seekg(0, std::ios::end);
  const auto sz = f.tellg();
  if (sz <= 0 || sz > 32 * 1024 * 1024) return false;
  f.seekg(0, std::ios::beg);
  out->resize(static_cast<size_t>(sz));
  f.read(reinterpret_cast<char*>(out->data()), sz);
  return static_cast<bool>(f) || f.eof();
}

std::string join_root(const std::string& rel) {
  if (g_root.empty()) return rel;
  std::string r = g_root;
  for (char& c : r)
    if (c == '/') c = '\\';
  while (!r.empty() && (r.back() == '\\' || r.back() == '/')) r.pop_back();
  std::string p = rel;
  for (char& c : p)
    if (c == '/') c = '\\';
  return r + "\\" + p;
}

int32_t font_px_height_from_name(std::string n) {
  for (char& c : n) {
    if (c >= 'A' && c <= 'Z') c = static_cast<char>(c - 'A' + 'a');
  }
  if (n.find("simple40") != std::string::npos) return 40;
  if (n.find("slii24") != std::string::npos) return 24;
  if (n.find("simple20") != std::string::npos) return 20;
  if (n.find("slii17") != std::string::npos) return 15;
  if (n.find("slii11") != std::string::npos) return 10;
  if (n.find("console10") != std::string::npos) return 10;
  if (n.find("console5") != std::string::npos) return 7;
  if (n.find("sl28") != std::string::npos) return 28;
  if (n.find("sl14") != std::string::npos) return 14;
  return 20;
}

// INVO v3 charset mesh. Glyph c = vertices c*4..c*4+3. Position is tenths of
// a pixel (x * 0.1), UV at +24. Advance is vertex 1's x. Index by byte code.
bool parse_font_glyphs(const uint8_t* scx, size_t scx_size,
                       std::vector<SlrrFontGlyph>* out) {
  if (!scx || !out || scx_size < 0x48) return false;
  out->clear();
  if (std::memcmp(scx, "INVO", 4) != 0) return false;
  uint32_t ver = 0, hdr = 0, vcount = 0;
  std::memcpy(&ver, scx + 4, 4);
  std::memcpy(&hdr, scx + 8, 4);
  if (ver != 3 || hdr < 0x2c || hdr > 0x1000) return false;
  if (8u + static_cast<size_t>(hdr) + 4 > scx_size) return false;
  size_t stride = 44;
  if (hdr > 0x3c) {
    uint32_t s = 0;
    std::memcpy(&s, scx + 8 + 0x3c, 4);
    stride = s;
  }
  if (stride < 32 || stride > 256) return false;
  std::memcpy(&vcount, scx + 8 + hdr, 4);
  if (vcount < 4 || (vcount % 4) != 0 || vcount > 65536) return false;
  const size_t vstart = 8u + static_cast<size_t>(hdr) + 4;
  const uint64_t need = static_cast<uint64_t>(vstart) +
                        static_cast<uint64_t>(vcount) * stride;
  if (need > scx_size) return false;

  const size_t nglyphs = static_cast<size_t>(vcount / 4);
  out->reserve(nglyphs);
  int ok = 0;
  auto finite = [](float f) { return f == f && f > -1e9f && f < 1e9f; };
  for (size_t g = 0; g < nglyphs; ++g) {
    SlrrFontGlyph glyph;
    for (int i = 0; i < 4; ++i) {
      const size_t off = vstart + (g * 4 + static_cast<size_t>(i)) * stride;
      float x = 0, y = 0, u = 0, v = 0;
      std::memcpy(&x, scx + off, 4);
      std::memcpy(&y, scx + off + 4, 4);
      std::memcpy(&u, scx + off + 24, 4);
      std::memcpy(&v, scx + off + 28, 4);
      glyph.px[i] = finite(x) ? x * 0.1f : 0.f;
      glyph.py[i] = finite(y) ? y * 0.1f : 0.f;
      glyph.tu[i] = finite(u) ? u : 0.f;
      glyph.tv[i] = finite(v) ? v : 0.f;
    }
    glyph.adv_px = glyph.px[1];
    float umin = 2.f, umax = -1.f, vmin = 2.f, vmax = -1.f;
    for (int i = 0; i < 4; ++i) {
      const float u = glyph.tu[i], v = glyph.tv[i];
      if (!(u >= 0.f && u <= 1.01f && v >= 0.f && v <= 1.01f)) {
        umin = umax = vmin = vmax = 0.f;
        break;
      }
      if (u < umin) umin = u;
      if (u > umax) umax = u;
      if (v < vmin) vmin = v;
      if (v > vmax) vmax = v;
    }
    glyph.u0 = umin;
    glyph.v0 = vmin;
    glyph.u1 = umax;
    glyph.v1 = vmax;
    out->push_back(glyph);
    if ((umax - umin) > 0.002f && (vmax - vmin) > 0.015f) ++ok;
  }
  return ok >= 32;
}

bool decode_tga_font_atlas(const uint8_t* data, size_t size, SlrrFont* font) {
  if (!data || !font || size < 18) return false;
  const uint8_t id_len = data[0];
  const uint8_t cmap_type = data[1];
  const uint8_t img_type = data[2];
  const uint16_t cmap_first =
      static_cast<uint16_t>(data[3] | (data[4] << 8));
  const uint16_t cmap_len =
      static_cast<uint16_t>(data[5] | (data[6] << 8));
  const uint8_t cmap_entry_bits = data[7];
  const uint16_t w = static_cast<uint16_t>(data[12] | (data[13] << 8));
  const uint16_t h = static_cast<uint16_t>(data[14] | (data[15] << 8));
  const uint8_t bpp = data[16];
  const uint8_t desc = data[17];
  if (w == 0 || h == 0 || bpp != 8) return false;
  if (img_type != 3 && img_type != 1) return false;
  if (img_type == 1 && (cmap_type != 1 || cmap_len == 0 || cmap_entry_bits < 24))
    return false;

  const size_t cmap_bytes =
      img_type == 1 ? static_cast<size_t>(cmap_len) * (cmap_entry_bits / 8) : 0;
  const size_t header = 18u + id_len + cmap_bytes;
  const size_t need = header + static_cast<size_t>(w) * h;
  if (size < need) return false;

  uint8_t alpha_lut[256];
  for (int i = 0; i < 256; ++i) alpha_lut[i] = static_cast<uint8_t>(i);
  if (img_type == 1) {
    std::memset(alpha_lut, 0, sizeof(alpha_lut));
    const uint8_t* pal = data + 18u + id_len;
    const int epb = cmap_entry_bits / 8;
    for (uint16_t i = 0; i < cmap_len && i < 256; ++i) {
      const uint8_t* e = pal + static_cast<size_t>(i) * epb;
      const int a = (static_cast<int>(e[2]) + e[1] + e[0]) / 3;
      const int idx = static_cast<int>(cmap_first) + i;
      if (idx >= 0 && idx < 256) alpha_lut[idx] = static_cast<uint8_t>(a);
    }
  }

  font->atlas_w = w;
  font->atlas_h = h;
  font->rgba.assign(static_cast<size_t>(w) * h * 4, 0);
  const bool top_left = (desc & 0x20) != 0;
  for (uint16_t y = 0; y < h; ++y) {
    const uint16_t src_y = top_left ? y : static_cast<uint16_t>(h - 1 - y);
    const uint8_t* src = data + header + static_cast<size_t>(src_y) * w;
    uint8_t* dst = font->rgba.data() + static_cast<size_t>(y) * w * 4;
    for (uint16_t x = 0; x < w; ++x) {
      const uint8_t a = alpha_lut[src[x]];
      dst[x * 4 + 0] = 0xff;
      dst[x * 4 + 1] = 0xff;
      dst[x * 4 + 2] = 0xff;
      dst[x * 4 + 3] = a;
    }
  }
  return true;
}

bool decode_png_font_atlas(const uint8_t* data, size_t size, SlrrFont* font) {
#if defined(SLRR_HAS_LIBBGFX)
  if (!data || !font || size < 8) return false;
  int w = 0, h = 0, comp = 0;
  stbi_uc* img =
      stbi_load_from_memory(data, static_cast<int>(size), &w, &h, &comp, 4);
  if (!img || w <= 0 || h <= 0) {
    if (img) stbi_image_free(img);
    return false;
  }
  font->atlas_w = w;
  font->atlas_h = h;
  font->rgba.resize(static_cast<size_t>(w) * h * 4);

  // SLII*.png = white fill + black outline, coverage in alpha — keep RGB.
  // simple20/40.png = white RGB, the letter is the alpha. Using luma as
  // alpha paints the whole cell (luma is 255), which is the white blocks
  // where the build line should be.
  bool outline_atlas = false;
  bool alpha_coverage = false;
  const int n = w * h;
  int white_clear = 0, white_ink = 0;
  for (int i = 0; i < n; ++i) {
    const uint8_t r = img[i * 4 + 0];
    const uint8_t g = img[i * 4 + 1];
    const uint8_t b = img[i * 4 + 2];
    const uint8_t a = img[i * 4 + 3];
    const int luma = (static_cast<int>(r) + g + b) / 3;
    if (luma < 40 && a > 16) outline_atlas = true;
    if (luma > 240 && a < 16) ++white_clear;
    else if (luma > 240 && a > 16) ++white_ink;
  }
  if (!outline_atlas && white_clear > 64 && white_ink > 64)
    alpha_coverage = true;

  for (int i = 0; i < n; ++i) {
    uint8_t r = img[i * 4 + 0];
    uint8_t g = img[i * 4 + 1];
    uint8_t b = img[i * 4 + 2];
    uint8_t a = img[i * 4 + 3];
    if (alpha_coverage) {
      r = g = b = 0xff;
    } else if (!outline_atlas) {
      // Greyscale coverage → white RGB + luma alpha (tintable via u_color).
      a = static_cast<uint8_t>((static_cast<int>(r) + g + b) / 3);
      r = g = b = 0xff;
    }
    font->rgba[static_cast<size_t>(i) * 4 + 0] = r;
    font->rgba[static_cast<size_t>(i) * 4 + 1] = g;
    font->rgba[static_cast<size_t>(i) * 4 + 2] = b;
    font->rgba[static_cast<size_t>(i) * 4 + 3] = a;
  }
  stbi_image_free(img);
  std::printf("[font] png atlas %dx%d outline=%d alpha=%d\n", w, h,
              outline_atlas ? 1 : 0, alpha_coverage ? 1 : 0);
  return true;
#else
  (void)data;
  (void)size;
  (void)font;
  return false;
#endif
}

}  // namespace

namespace {
void* g_ui_small = nullptr;
void* g_ui_medium = nullptr;
void* g_ui_large = nullptr;
}  // namespace

void slrr_font_set_game_root(const std::string& root) { g_root = root; }

void slrr_font_set_ui_keys(void* small, void* medium, void* large) {
  g_ui_small = small;
  g_ui_medium = medium;
  g_ui_large = large;
}

void* slrr_font_ui_small() { return g_ui_small; }
void* slrr_font_ui_medium() { return g_ui_medium; }
void* slrr_font_ui_large() { return g_ui_large; }

// ------------------------------------------------------------------
// TTF path. Fonts/fonts.ini maps a stock font stem to a family file under
// Fonts/families/ plus a percent scale. Fonts without a profile keep the
// legacy SCX charset path (symbol fonts, part-condition glyphs).
// ------------------------------------------------------------------
struct SlrrTtfFace {
  std::vector<uint8_t> file;
  stbtt_fontinfo info{};
  float scale = 0.f;  // font units → pixels at ttf_px
};

struct FontProfile {
  std::string family;
  int scale = 100;
};

std::unordered_map<std::string, FontProfile> g_font_profiles;
int g_font_ui_scale = 100;
std::string g_font_cfg_root;
bool g_font_cfg_loaded = false;

void font_profile_defaults() {
  // One family per stock face group; 100% keeps the stock pixel heights.
  g_font_profiles["simple20"] = {"simple", 100};
  g_font_profiles["simple40"] = {"simple", 100};
  g_font_profiles["slii11"] = {"slii17", 100};
  g_font_profiles["slii17"] = {"slii17", 100};
  g_font_profiles["slii18"] = {"slii17", 100};
  g_font_profiles["slii24"] = {"slii17", 100};
  g_font_profiles["console5"] = {"console", 100};
  g_font_profiles["console10"] = {"console", 100};
  g_font_profiles["sl14"] = {"sl", 100};
  g_font_profiles["sl28"] = {"sl", 100};
  g_font_profiles["__fallback"] = {"UI-stock", 100};
  g_font_ui_scale = 100;
}

std::string trim_ascii(std::string s) {
  const char* ws = " \t\r\n";
  size_t a = s.find_first_not_of(ws);
  if (a == std::string::npos) return {};
  size_t b = s.find_last_not_of(ws);
  return s.substr(a, b - a + 1);
}

SlrrFont* mutable_font(void* key) {
  auto it = g_fonts.find(key);
  return it != g_fonts.end() ? &it->second : nullptr;
}

void font_load_config() {
  font_profile_defaults();
  g_font_cfg_loaded = true;
  std::vector<uint8_t> bytes;
  if (!read_file(join_root("Fonts\\fonts.ini"), &bytes) || bytes.empty())
    return;  // built-in defaults only
  std::string text(reinterpret_cast<const char*>(bytes.data()), bytes.size());
  size_t pos = 0;
  while (pos < text.size()) {
    size_t eol = text.find('\n', pos);
    if (eol == std::string::npos) eol = text.size();
    std::string line = trim_ascii(text.substr(pos, eol - pos));
    pos = eol + 1;
    if (line.empty() || line[0] == '#' || line[0] == ';') continue;
    const size_t eq = line.find('=');
    if (eq == std::string::npos) continue;
    const std::string key = trim_ascii(line.substr(0, eq));
    std::string value = trim_ascii(line.substr(eq + 1));
    if (key.empty() || value.empty()) continue;
    if (key == "ui_scale") {
      g_font_ui_scale = std::max(10, std::min(400, atoi(value.c_str())));
      continue;
    }
    // value: family [scale%]
    size_t sp = value.find(' ');
    FontProfile prof;
    prof.family = sp == std::string::npos ? value : value.substr(0, sp);
    if (sp != std::string::npos) {
      const int pct = atoi(trim_ascii(value.substr(sp + 1)).c_str());
      if (pct >= 10 && pct <= 400) prof.scale = pct;
    }
    std::string lower = key;
    for (char& c : lower)
      if (c >= 'A' && c <= 'Z') c = static_cast<char>(c - 'A' + 'a');
    g_font_profiles[lower] = prof;
  }
  std::printf("[font] config Fonts\\fonts.ini profiles=%zu ui_scale=%d\n",
              g_font_profiles.size(), g_font_ui_scale);
}

const FontProfile* font_profile_for(const std::string& stem) {
  // Debug switch: force the legacy SCX charset path.
  if (std::getenv("SLRR_NO_TTF")) return nullptr;
  if (!g_font_cfg_loaded || g_font_cfg_root != g_root) font_load_config();
  g_font_cfg_root = g_root;
  std::string lower = stem;
  for (char& c : lower) {
    if (c >= 'A' && c <= 'Z') c = static_cast<char>(c - 'A' + 'a');
  }
  auto it = g_font_profiles.find(lower);
  if (it != g_font_profiles.end()) return &it->second;
  return &g_font_profiles["__fallback"];
}

bool load_ttf(void* key, const std::string& stem, const FontProfile& prof,
              const std::vector<uint8_t>& bytes, int base_px) {
  if (bytes.size() < 12) return false;
  auto* face = new SlrrTtfFace();
  face->file = bytes;
  const int offset = stbtt_GetFontOffsetForIndex(face->file.data(), 0);
  if (offset < 0 ||
      !stbtt_InitFont(&face->info, face->file.data(), offset)) {
    delete face;
    return false;
  }
  int px = std::max(4, (base_px * prof.scale + 50) / 100);
  px = std::max(4, (px * g_font_ui_scale + 50) / 100);
  face->scale = stbtt_ScaleForPixelHeight(&face->info, static_cast<float>(px));
  int ascent = 0, descent = 0, linegap = 0;
  stbtt_GetFontVMetrics(&face->info, &ascent, &descent, &linegap);

  SlrrFont st;
  st.name = stem;
  st.ttf = true;
  st.ttf_px = px;
  st.px_height = base_px;  // stock logical height for layout math
  st.ttf_ascent = ascent * face->scale;
  st.ttf_descent = -descent * face->scale;
  st.atlas_w = 1024;
  st.atlas_h = 1024;
  st.rgba.assign(static_cast<size_t>(st.atlas_w) * st.atlas_h * 4, 0);
  st.ttf_face = face;
  st.ready = true;
  g_fonts[key] = std::move(st);
  // Pre-rasterize the common charset so the atlas texture is complete when
  // first created. Other codepoints stay lazy.
  for (uint32_t cp = 0x20; cp < 0x7f; ++cp) (void)slrr_font_glyph(key, cp);
  for (uint32_t cp = 0xA0; cp < 0x100; ++cp) (void)slrr_font_glyph(key, cp);
  // Latin Extended-A: Polish (ą ć ę ł ń ś ź ż) and other European text.
  for (uint32_t cp = 0x100; cp <= 0x17F; ++cp) (void)slrr_font_glyph(key, cp);
  for (uint32_t cp = 0x400; cp <= 0x45F; ++cp) (void)slrr_font_glyph(key, cp);
  SlrrFont* placed = mutable_font(key);
  if (placed) {
    placed->dirty_y0 = placed->dirty_y1 = 0;
    placed->atlas_dirty = false;
  }
  std::printf("[font] ttf %s family=%s px=%d (base %d, %d%%)\n",
              stem.c_str(), prof.family.c_str(), px, base_px, prof.scale);
  return true;
}

namespace {

// UTF-8 decode. Overlong/surrogate/invalid → Latin-1 byte passthrough.
uint32_t utf8_decode(const char*& p) {
  const unsigned char b0 = static_cast<unsigned char>(*p);
  if (b0 < 0x80) {
    ++p;
    return b0;
  }
  const auto cont = [](unsigned char c) { return (c & 0xC0) == 0x80; };
  if ((b0 & 0xE0) == 0xC0 && cont(p[1])) {
    const uint32_t cp = ((b0 & 0x1Fu) << 6) | (p[1] & 0x3Fu);
    if (cp >= 0x80) {
      p += 2;
      return cp;
    }
  } else if ((b0 & 0xF0) == 0xE0 && cont(p[1]) && cont(p[2])) {
    const uint32_t cp = ((b0 & 0x0Fu) << 12) | ((p[1] & 0x3Fu) << 6) |
                        (p[2] & 0x3Fu);
    if (cp >= 0x800 && !(cp >= 0xD800 && cp <= 0xDFFF)) {
      p += 3;
      return cp;
    }
  } else if ((b0 & 0xF8) == 0xF0 && cont(p[1]) && cont(p[2]) && cont(p[3])) {
    const uint32_t cp = ((b0 & 0x07u) << 18) | ((p[1] & 0x3Fu) << 12) |
                        ((p[2] & 0x3Fu) << 6) | (p[3] & 0x3Fu);
    if (cp >= 0x10000 && cp <= 0x10FFFF) {
      p += 4;
      return cp;
    }
  }
  ++p;
  return b0;
}

// Shelf packer slot with a 1px pad. False when the atlas is full.
bool atlas_alloc(SlrrFont& f, int w, int h, int* x, int* y) {
  const int W = f.atlas_w, H = f.atlas_h, pad = 1;
  if (w + pad > W || h + pad > H) return false;
  if (f.shelf_x + w + pad > W) {
    f.shelf_y += f.shelf_h + pad;
    f.shelf_x = 0;
    f.shelf_h = 0;
  }
  if (f.shelf_y + h + pad > H) return false;
  *x = f.shelf_x;
  *y = f.shelf_y;
  f.shelf_x += w + pad;
  f.shelf_h = std::max(f.shelf_h, h);
  return true;
}

// Rasterize a codepoint on demand. Missing glyphs yield a null pointer so
// the draw loop falls back to the space advance.
const SlrrFontGlyph* glyph_for_cp(SlrrFont& font, uint32_t cp) {
  if (!font.ttf || !font.ttf_face) return nullptr;
  auto it = font.tt_glyphs.find(cp);
  if (it != font.tt_glyphs.end()) return &it->second;
  if (cp == 0 || cp > 0x10FFFF) return nullptr;
  SlrrTtfFace* face = font.ttf_face;

  SlrrFontGlyph g;
  int adv = 0, lsb = 0;
  stbtt_GetCodepointHMetrics(&face->info, static_cast<int>(cp), &adv, &lsb);
  g.adv_px = adv * face->scale;

  int ix0 = 0, iy0 = 0, ix1 = 0, iy1 = 0;
  stbtt_GetCodepointBitmapBox(&face->info, static_cast<int>(cp), face->scale,
                              face->scale, &ix0, &iy0, &ix1, &iy1);
  const int w = ix1 - ix0;
  const int h = iy1 - iy0;
  int ax = 0, ay = 0;
  if (w > 0 && h > 0 && atlas_alloc(font, w, h, &ax, &ay)) {
    // stbtt outputs a packed 8-bit bitmap (1 byte per pixel, rows at
    // out_stride bytes). It cannot write RGBA directly: render to a temp
    // buffer, then expand into the atlas (white RGB + coverage alpha).
    std::vector<unsigned char> bmp(static_cast<size_t>(w) * h, 0);
    stbtt_MakeCodepointBitmap(&face->info, bmp.data(), w, h, w, face->scale,
                              face->scale, static_cast<int>(cp));
    for (int y = 0; y < h; ++y) {
      uint8_t* row = font.rgba.data() +
                     (static_cast<size_t>(ay + y) * font.atlas_w + ax) * 4;
      const unsigned char* src = bmp.data() + static_cast<size_t>(y) * w;
      for (int x = 0; x < w; ++x) {
        row[x * 4 + 0] = 0xff;
        row[x * 4 + 1] = 0xff;
        row[x * 4 + 2] = 0xff;
        row[x * 4 + 3] = src[x];
      }
    }
    font.dirty_y0 = font.dirty_y0 == 0 && !font.atlas_dirty
                        ? ay
                        : std::min(font.dirty_y0, ay);
    font.dirty_y1 = std::max(font.dirty_y1, ay + h);
    font.atlas_dirty = true;
    // Half-texel inset for point sampling.
    const float W = static_cast<float>(font.atlas_w);
    const float H = static_cast<float>(font.atlas_h);
    g.u0 = (ax + 0.5f) / W;
    g.u1 = (ax + w - 0.5f) / W;
    g.v0 = (ay + 0.5f) / H;
    g.v1 = (ay + h - 0.5f) / H;
    g.tu[0] = g.tu[2] = g.u0;
    g.tu[1] = g.tu[3] = g.u1;
    g.tv[0] = g.tv[1] = g.v0;
    g.tv[2] = g.tv[3] = g.v1;
    // Quad relative to the pen and the line top (y down). iy0 is negative
    // above the baseline; the baseline sits ttf_ascent below the line top.
    g.px[0] = g.px[2] = static_cast<float>(ix0);
    g.px[1] = g.px[3] = static_cast<float>(ix0 + w);
    g.py[0] = g.py[1] = font.ttf_ascent + iy0;
    g.py[2] = g.py[3] = font.ttf_ascent + iy1;
  } else if (w > 0 && h > 0) {
    if (!font.atlas_warned) {
      font.atlas_warned = true;
      std::printf("[font] %s atlas %dx%d full, extra glyphs advance-only\n",
                  font.name.c_str(), font.atlas_w, font.atlas_h);
    }
  }
  auto ins = font.tt_glyphs.emplace(cp, g);
  return &ins.first->second;
}

}  // namespace

const char* slrr_font_name_for_rid(int32_t rid) {
  // Text.java RID_* → stem under frontend/meshes|textures/Fonts/
  const uint32_t local = static_cast<uint32_t>(rid) & 0xFFFFu;
  switch (local) {
    case 0x0020:
      return "simple20";
    case 0x0022:
      return "simple40";
    case 0x0023:
      return "console10";
    case 0x0025:
      return "slii24";
    case 0x002E:
      return "sl28";
    case 0x008C:
      return "sl14";
    case 0x00B2:
      return "console5";
    case 0x0138:
      return "slii11";
    case 0x0139:
      return "slii17";
    case 0x003C:
      return "symbol_pointers";
    default:
      return nullptr;
  }
}

uint32_t slrr_utf8_next(const char*& p) {
  if (!p || !*p) return 0;
  return utf8_decode(p);
}

const SlrrFontGlyph* slrr_font_glyph(void* key, uint32_t cp) {
  SlrrFont* font = mutable_font(key);
  if (!font) return nullptr;
  if (font->ttf) return glyph_for_cp(*font, cp);
  const size_t gi = cp < 0x100 ? cp : 0xFF;
  return gi < font->glyphs.size() ? &font->glyphs[gi] : nullptr;
}

bool slrr_font_take_atlas_update(void* key, const uint8_t** data, int32_t* y0,
                                 int32_t* h) {
  SlrrFont* font = mutable_font(key);
  if (!font || !font->atlas_dirty || font->atlas_w <= 0) return false;
  *data = font->rgba.data();
  *y0 = font->dirty_y0;
  *h = font->dirty_y1 - font->dirty_y0;
  font->atlas_dirty = false;
  font->dirty_y0 = font->dirty_y1 = 0;
  return *h > 0;
}

bool slrr_font_load(void* key, const char* name) {
  if (!key || !name || !name[0]) return false;
  std::string n = name;
  {
    const size_t slash = n.find_last_of("/\\");
    if (slash != std::string::npos) n = n.substr(slash + 1);
    const size_t dot = n.find_last_of('.');
    if (dot != std::string::npos) n = n.substr(0, dot);
  }
  std::string up = n;
  for (char& c : up) {
    if (c >= 'a' && c <= 'z') c = static_cast<char>(c - 'a' + 'A');
  }

  std::vector<uint8_t> scx_bytes, atlas_bytes;
  // TTF first: Fonts/fonts.ini profiles the stock stems. Symbol fonts and
  // anything without a TTF family keep the SCX charset path.
  if (const FontProfile* prof = font_profile_for(n)) {
    std::vector<uint8_t> ttf_bytes;
    if (read_file(join_root("Fonts\\families\\" + prof->family + ".ttf"),
                  &ttf_bytes) &&
        load_ttf(key, n, *prof, ttf_bytes, font_px_height_from_name(n)))
      return true;
  }
  const std::string scx_path =
      join_root("frontend\\meshes\\Fonts\\" + n + ".scx");
  if (!read_file(scx_path, &scx_bytes)) {
    std::printf("[font] scx miss %s\n", scx_path.c_str());
    return false;
  }

  std::string atlas_path;
  auto try_atlas = [&](const std::string& rel) -> bool {
    atlas_bytes.clear();
    const std::string p = join_root(rel);
    if (!read_file(p, &atlas_bytes) || atlas_bytes.empty()) return false;
    atlas_path = p;
    return true;
  };
  // The game names the PNG. A sibling .tga is not the charset.
  const bool got_atlas =
      try_atlas("frontend\\textures\\Fonts\\" + n + ".png") ||
      try_atlas("frontend\\textures\\Fonts\\" + up + ".png") ||
      try_atlas("frontend\\textures\\Fonts\\" + n + ".tga") ||
      try_atlas("frontend\\textures\\Fonts\\" + up + ".tga");
  if (!got_atlas) {
    std::printf("[font] atlas miss for %s\n", n.c_str());
    return false;
  }

  SlrrFont st;
  st.name = n;
  st.px_height = font_px_height_from_name(n);
  const bool png =
      atlas_path.size() >= 4 &&
      (atlas_path[atlas_path.size() - 4] == '.') &&
      (atlas_path[atlas_path.size() - 3] == 'p' ||
       atlas_path[atlas_path.size() - 3] == 'P') &&
      (atlas_path[atlas_path.size() - 2] == 'n' ||
       atlas_path[atlas_path.size() - 2] == 'N') &&
      (atlas_path[atlas_path.size() - 1] == 'g' ||
       atlas_path[atlas_path.size() - 1] == 'G');
  const bool atlas_ok =
      png ? decode_png_font_atlas(atlas_bytes.data(), atlas_bytes.size(), &st)
          : decode_tga_font_atlas(atlas_bytes.data(), atlas_bytes.size(), &st);
  if (!atlas_ok) {
    std::printf("[font] atlas decode fail %s\n", atlas_path.c_str());
    return false;
  }
  if (!parse_font_glyphs(scx_bytes.data(), scx_bytes.size(), &st.glyphs)) {
    std::printf("[font] glyph parse fail %s\n", scx_path.c_str());
    return false;
  }
  for (SlrrFontGlyph& g : st.glyphs) {
    if (g.adv_px < 0.f) g.adv_px = 0.f;
  }
  st.ready = !st.glyphs.empty();
  g_fonts[key] = std::move(st);
  std::printf("[font] %s atlas=%s glyphs=%zu px=%d\n", n.c_str(),
              atlas_path.c_str(), g_fonts[key].glyphs.size(),
              g_fonts[key].px_height);
  return g_fonts[key].ready;
}

bool slrr_font_load_from_rid(void* key, int32_t rid) {
  const char* name = slrr_font_name_for_rid(rid);
  if (!name) return false;
  return slrr_font_load(key, name);
}

bool slrr_font_ready(void* key) {
  auto it = g_fonts.find(key);
  return it != g_fonts.end() && it->second.ready;
}

const SlrrFont* slrr_font_get(void* key) {
  auto it = g_fonts.find(key);
  if (it == g_fonts.end() || !it->second.ready) return nullptr;
  return &it->second;
}

float slrr_font_measure_px(void* key, const char* text) {
  const SlrrFont* font = slrr_font_get(key);
  if (!font || !text) return 0.f;
  if (font->ttf) {
    float w = 0.f;
    for (const char* p = text; *p;) {
      const uint32_t cp = utf8_decode(p);
      const SlrrFontGlyph* g = slrr_font_glyph(key, cp);
      if (g && g->adv_px > 0.f)
        w += g->adv_px;
      else if (cp == ' ')
        w += static_cast<float>(font->px_height) * 0.35f;
      else
        // A glyph that has not been rasterized yet contributes nothing, so the
        // first measurement of a string comes out too narrow and the tooltip
        // plate is built the wrong size -- wrong only on that first showing.
        // Fall back to a conservative estimate instead of measuring zero.
        w += static_cast<float>(font->px_height) * 0.6f;
    }
    return w;
  }
  float w = 0.f;
  for (const char* p = text; *p; ++p) {
    const unsigned char ch = static_cast<unsigned char>(*p);
    const size_t gi = static_cast<size_t>(ch);
    if (gi < font->glyphs.size() && font->glyphs[gi].adv_px > 0.f)
      w += font->glyphs[gi].adv_px;
    else if (ch == ' ')
      w += static_cast<float>(font->px_height) * 0.35f;
  }
  return w;
}

void slrr_text_set_default_color(uint32_t color) { g_text_default_color = color; }
uint32_t slrr_text_default_color() { return g_text_default_color; }
void slrr_text_create(void* key, void* font, float x, float y) {
  if (!key) return;
  TextState& t = g_texts[key];
  t.font = font;
  t.x = x;
  t.y = y;
  t.color = g_text_default_color;
  t.align = 2;
  t.group = 0;
  t.font_slot = SlrrFontSlot::None;
  t.text.clear();
  t.alive = true;
  t.visible = true;
}

void slrr_text_set_group(void* key, int32_t group) {
  auto it = g_texts.find(key);
  if (it == g_texts.end()) return;
  it->second.group = group;
  const bool closed = !it->second.pinned && osd_group_is_hidden(group);
  if (closed) {
    it->second.group_hidden = true;
    it->second.visible = false;
  }
  for (auto& ot : g_osd_text) {
    if (ot.text_key != key) continue;
    ot.group = group;
    if (closed) {
      ot.group_hidden = true;
      ot.visible = false;
    }
  }
}

void slrr_text_set_pinned(void* key, bool pinned) {
  auto it = g_texts.find(key);
  if (it == g_texts.end()) return;
  it->second.pinned = pinned;
  if (pinned) it->second.visible = true;
  for (auto& ot : g_osd_text) {
    if (ot.text_key != key) continue;
    ot.pinned = pinned;
    if (pinned) ot.visible = true;
  }
}

void slrr_text_set_visible(void* key, bool visible) {
  auto it = g_texts.find(key);
  if (it == g_texts.end()) return;
  it->second.visible = visible;
  for (auto& ot : g_osd_text) {
    if (ot.text_key == key) ot.visible = visible;
  }
}

int32_t text_osd_layer(void* osd) {
  auto* o = static_cast<Object*>(osd);
  if (!o) return 0;
  auto vp = o->fields.find("vp");
  if (vp == o->fields.end() || vp->second.tag != ValueTag::Obj || !vp->second.v.o)
    return 0;
  auto pri = vp->second.v.o->fields.find("pri");
  if (pri == vp->second.v.o->fields.end() || pri->second.tag != ValueTag::Int)
    return 0;
  return pri->second.v.i;
}

void slrr_text_set_osd(void* key, void* osd) {
  auto it = g_texts.find(key);
  if (it == g_texts.end()) return;
  const auto* owner = static_cast<const Object*>(osd);
  bool suppressed = false;
  if (owner) {
    const auto hidden = owner->fields.find("_host_hidden");
    suppressed = hidden != owner->fields.end() && hidden->second.tag == ValueTag::Int &&
                 hidden->second.v.i != 0;
    // An OSD that was never shown draws neither plates nor text.
    if (!osd_owner_shown(owner)) suppressed = true;
  }
  it->second.osd = osd;
  it->second.layer = text_osd_layer(osd);
  it->second.osd_suppressed = suppressed;
  if (suppressed) it->second.visible = false;
  for (auto& ot : g_osd_text) {
    if (ot.text_key == key) {
      ot.osd = osd;
      ot.layer = it->second.layer;
      ot.osd_suppressed = suppressed;
      if (suppressed) ot.visible = false;
    }
  }
}

void slrr_text_set_osd_visible(void* osd, bool visible) {
  if (!osd) return;
  for (auto& kv : g_texts) {
    if (kv.second.osd != osd) continue;
    kv.second.osd_suppressed = !visible;
    if (kv.second.pinned) {
      // Pinned survives hideGroup (menu version lines). Hiding the whole
      // OSD must still drop them, or the build line stays in the garage
      // and DAY/TIME stay on the main menu.
      kv.second.visible = visible;
      continue;
    }
    kv.second.visible = visible && !kv.second.group_hidden;
  }
  for (auto& ot : g_osd_text) {
    if (ot.osd != osd) continue;
    ot.osd_suppressed = !visible;
    if (ot.pinned) {
      ot.visible = visible;
      continue;
    }
    ot.visible = visible && !ot.group_hidden;
  }
}

void slrr_text_set_group_visible(int32_t group, bool visible) {
  for (auto& kv : g_texts) {
    if (kv.second.group != group || kv.second.pinned) continue;
    kv.second.group_hidden = !visible;
    if (visible && kv.second.osd_suppressed) continue;
    kv.second.visible = visible && !kv.second.osd_suppressed;
  }
  for (auto& ot : g_osd_text) {
    if (ot.group != group || ot.pinned) continue;
    ot.group_hidden = !visible;
    if (visible && ot.osd_suppressed) continue;
    ot.visible = visible && !ot.osd_suppressed;
  }
}

void slrr_text_show_only_group(int32_t group) {
  for (auto& kv : g_texts) {
    if (kv.second.pinned) continue;
    kv.second.group_hidden = kv.second.group != group;
    if (kv.second.osd_suppressed) {
      kv.second.visible = false;
      continue;
    }
    kv.second.visible = !kv.second.group_hidden;
  }
  for (auto& ot : g_osd_text) {
    if (ot.pinned) continue;
    ot.group_hidden = ot.group != group;
    if (ot.osd_suppressed) {
      ot.visible = false;
      continue;
    }
    ot.visible = !ot.group_hidden;
  }
}

void slrr_text_destroy(void* key) {
  if (!key) return;
  for (size_t i = 0; i < g_osd_text.size();) {
    if (g_osd_text[i].text_key == key)
      g_osd_text.erase(g_osd_text.begin() + static_cast<std::ptrdiff_t>(i));
    else
      ++i;
  }
  g_texts.erase(key);
}

void slrr_text_set_color(void* key, uint32_t argb) {
  auto it = g_texts.find(key);
  if (it == g_texts.end()) return;
  it->second.color = argb;
  for (auto& ot : g_osd_text) {
    if (ot.text_key == key) ot.color = argb;
  }
}

void slrr_text_set_align(void* key, int32_t align) {
  auto it = g_texts.find(key);
  if (it == g_texts.end()) return;
  it->second.align = align;
  // changeText publishes the line before changeAlign. The drawn copy
  // kept ALIGN_LEFT, so a right-aligned column started at the anchor
  // and ran into the other column.
  for (auto& ot : g_osd_text) {
    if (ot.text_key == key) ot.align = align;
  }
}

void slrr_text_set_behind_ui(void* key, bool behind) {
  auto it = g_texts.find(key);
  if (it == g_texts.end()) return;
  it->second.behind_ui = behind;
  for (auto& ot : g_osd_text) {
    if (ot.text_key == key) ot.behind_ui = behind;
  }
}

void slrr_text_set_pos(void* key, float x, float y) {
  auto it = g_texts.find(key);
  if (it == g_texts.end()) return;
  it->second.x = x;
  it->second.y = y;
  for (auto& ot : g_osd_text) {
    if (ot.text_key != key) continue;
    ot.x = x;
    ot.y = y;
  }
}

void slrr_text_set_string(void* key, const char* utf8) {
  auto it = g_texts.find(key);
  if (it == g_texts.end()) return;
  it->second.english = utf8 ? utf8 : "";
  it->second.text = slrr_loc_translate(utf8);
}

void slrr_text_refresh_language() {
  for (auto& kv : g_texts) {
    TextState& t = kv.second;
    if (!t.alive || t.english.empty()) continue;
    t.text = slrr_loc_translate(t.english.c_str());
    slrr_text_update(kv.first);
  }
}

void slrr_text_set_font(void* key, void* font) {
  auto it = g_texts.find(key);
  if (it == g_texts.end()) return;
  it->second.font = font;
  for (auto& ot : g_osd_text) {
    if (ot.text_key == key) ot.font_key = font;
  }
}

void slrr_text_set_font_slot(void* key, SlrrFontSlot slot) {
  auto it = g_texts.find(key);
  if (it == g_texts.end()) return;
  it->second.font_slot = slot;
}

SlrrFontSlot slrr_text_font_slot(void* key) {
  auto it = g_texts.find(key);
  if (it == g_texts.end()) return SlrrFontSlot::None;
  return it->second.font_slot;
}

void slrr_text_rebind_frontend_fonts(void* small, void* medium, void* large) {
  for (auto& kv : g_texts) {
    TextState& t = kv.second;
    void* font = nullptr;
    if (t.font_slot == SlrrFontSlot::Small)
      font = small;
    else if (t.font_slot == SlrrFontSlot::Medium)
      font = medium;
    else if (t.font_slot == SlrrFontSlot::Large)
      font = large;
    else
      continue;
    if (!font || font == t.font) continue;
    t.font = font;
    for (auto& ot : g_osd_text) {
      if (ot.text_key == kv.first) ot.font_key = font;
    }
  }
}

void slrr_text_update(void* key) {
  auto it = g_texts.find(key);
  if (it == g_texts.end() || !it->second.alive) return;
  const TextState& t = it->second;
  for (size_t i = 0; i < g_osd_text.size();) {
    if (g_osd_text[i].text_key == key)
      g_osd_text.erase(g_osd_text.begin() + static_cast<std::ptrdiff_t>(i));
    else
      ++i;
  }
  if (t.text.empty() || !t.font) return;
  SlrrOsdText ot;
  ot.text_key = key;
  ot.font_key = t.font;
  ot.x = t.x;
  ot.y = t.y;
  ot.color = t.color;
  ot.align = t.align;
  ot.group = t.group;
  ot.text = t.text;
  ot.visible = t.visible;
  ot.pinned = t.pinned;
  ot.behind_ui = t.behind_ui;
  ot.osd = t.osd;
  ot.osd_suppressed = t.osd_suppressed;
  ot.group_hidden = t.group_hidden;
  ot.layer = t.layer;
  g_osd_text.push_back(std::move(ot));
}

void slrr_text_clear_all() {
  g_osd_text.clear();
  g_texts.clear();
}

const std::vector<SlrrOsdText>& slrr_osd_texts() { return g_osd_text; }


void font_visit_gc_roots(const GcRootVisitor& visit) {
  visit(g_ui_small); visit(g_ui_medium); visit(g_ui_large);
  for (const auto& entry : g_fonts) visit(entry.first);
  for (const auto& entry : g_texts) {
    visit(entry.first); visit(entry.second.font); visit(entry.second.osd);
  }
  for (const auto& line : g_osd_text) {
    visit(line.text_key); visit(line.font_key); visit(line.osd);
  }
}
}  // namespace slrr

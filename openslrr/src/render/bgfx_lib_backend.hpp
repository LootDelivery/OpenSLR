#pragma once

// Internal to the libbgfx renderer. The public surface stays
// slrr/render_backend.hpp. Method bodies live in the sibling cpp files.

#include "slrr/render_backend.hpp"
#include "slrr/scene_instances.hpp"
#include "slrr/gameref.hpp"
#include "slrr/host_game_options.hpp"
#include "slrr/scene_sun.hpp"
#include "slrr/osd_registry.hpp"
#include "debug_overlay.hpp"

#include <bgfx/bgfx.h>

#include <array>
#include <cstdint>
#include <memory>
#include <string>
#include <unordered_map>
#include <vector>

#ifdef _WIN32
#ifndef WIN32_LEAN_AND_MEAN
#define WIN32_LEAN_AND_MEAN
#endif
#ifndef NOMINMAX
#define NOMINMAX
#endif
#include <windows.h>
#endif

namespace slrr {
struct VehicleHudState;

struct MeshVertex {
  float x, y, z;
  float nx, ny, nz;
  float u, v;
  float u2 = 0, v2 = 0, u3 = 0, v3 = 0;
  uint32_t rgba = 0xffffffffu;
};

// U2-B.4 measurement: how many vehicle submits a frame issues, and how many
// distinct draw groups they fall into. A group is what instancing could merge:
// same geometry, same material branch and the same bound texture. Enabled with
// SLRR_BATCH_PROBE=1, silent otherwise.
void batch_probe_begin_frame();
void batch_probe_note(uint64_t key);
void batch_probe_report();

class LibBgfxBackend final : public IRenderBackend {

  // View 0 is the cached map shadow, followed by the vehicle shadow maps.
  // The world is view 5 or 6, depending on the FMV backdrop. Previews start at 8. The
  // interface stays on kOsdView, above those rects.
  static constexpr int kShadowSlots = 4;
  static constexpr uint16_t kShadowResolution = 2048;
  static constexpr uint16_t kStaticShadowView = 0;
  static constexpr uint16_t kVehicleShadowFirst = 1;
  static constexpr uint16_t kWorldView = 5;
  static constexpr uint16_t kFmvLayer = 6;
  static constexpr uint16_t kOsdUnder = 7;
  static constexpr uint16_t kPreviewClear = 8;
  static constexpr uint16_t kPreview0 = 9;
  // Composite order after tone mapping: the SDR catalogue plate, then the
  // tone-mapped 3D preview over it, then the 2D interface on top. The plate
  // has to end up behind the preview in the composite, which the view list
  // alone no longer expresses now that they live in different targets.
  static constexpr uint16_t kOsdView = 71;
  // Tonemap pass between the previews (<=64) and the interface.
  static constexpr uint16_t kPresentView = 68;
  static constexpr uint16_t kPlateView = 69;
  static constexpr uint16_t kPreviewPresent = 70;
  // One view per pyramid level, after the world and the reflection faces.
  static constexpr uint16_t kHizView = 110;
  static constexpr int kHizLevels = 16;
  // Reflection views. The capture and its seed blit run one below each other so
  // the blit lands first, and the bake has its own range because it can be
  // captured in the same frame as the moving layer.
  static constexpr uint16_t kReflectionCaptureView = 130;
  static constexpr uint16_t kReflectionBakeView = 150;

 public:
  const char* name() const override { return "bgfx"; }

  bool open(int32_t width, int32_t height, const char* title) override;


  void close() override;


  bool ready() const override { return ready_; }

  void pump(int32_t /*ms*/) override;


  bool quit_requested() const override { return quit_; }
  void request_quit() override { quit_ = true; }
  bool request_screenshot(const std::string& path) override;

  void flush() override;


  int32_t width() const override { return width_; }
  int32_t height() const override { return height_; }

  void set_scx_smoke(const std::string& path);


  void set_garage_viewport(bool enabled, int32_t osd_buttons);


  void set_osd_overlay(bool enabled) { osd_overlay_ = enabled; }
  bool osd_draw_enabled() const { return garage_ || osd_overlay_; }

  void* native_hwnd() const { return hwnd_; }

  void set_scx_mesh(int32_t version, int32_t groups, int32_t vertices,
                    int32_t triangles, const float bbox_min[3],
                    const float bbox_max[3]);


  void set_scx_geometry(ScxGeometry geo);


  void clear_scene();
  void set_debug_context(Jvm& jvm, const SectionMachine& sections) {
    debug_overlay_.set_context(jvm, sections);
  }
  void set_debug_startup(bool vsync, bool overlay) {
    debug_overlay_.render_options().vsync = vsync;
    // SLRR_EXPOSURE seeds the panel slider (the slider is the single source
    // of truth afterwards); without the env the default is neutral 1.0.
    if (const char* e = std::getenv("SLRR_EXPOSURE")) {
      const float x = static_cast<float>(std::atof(e));
      if (x > 0.f && x < 20.f) debug_overlay_.render_options().exposure = x;
    }
    debug_overlay_.set_visible(overlay);
  }


  void add_scx_geometry(ScxGeometry geo);


 private:
  DebugOverlay debug_overlay_;
  bool debug_was_visible_ = false;
  struct ScenePart {
    ScxGeometry geo;
    std::shared_ptr<const ScxGeometry> source;
    bgfx::VertexBufferHandle vbh = BGFX_INVALID_HANDLE;
    bgfx::VertexBufferHandle skin_vbh = BGFX_INVALID_HANDLE;
    bgfx::IndexBufferHandle ibh = BGFX_INVALID_HANDLE;
    std::vector<bgfx::TextureHandle> tex;
    std::vector<std::string> shared_texture_paths;
    uint32_t revision = 0;
    // Frame-budgeted upload (PERF-1): geo is cached, GPU upload deferred.
    bool pending = false;
    bool share_textures = false;
    size_t upload_bytes = 0;
  };
  struct SharedMapTexture {
    bgfx::TextureHandle handle;
    size_t users;
  };
  std::unordered_map<std::string, SharedMapTexture> map_textures_;
  struct Preview { GameRefState camera; GameRefState viewport; std::vector<SceneInstance> instances;
    // Inset previews use a deliberately simplified material (no lights, no
    // glass, trimmed settings) because they are tiny. The catalogue shows a
    // part large enough to judge, so it asks for the full material instead.
    bool full_materials = false; };
  // Projection depends on scene geometry, never on the viewing camera.
  // Keep parked cars out of the per-vertex floor queries on subsequent frames.
  struct ProjectedShadow {
    struct Stamp {
      void* key = nullptr;
      std::shared_ptr<const ScxGeometry> source;
      std::array<float, 16> transform{};
    };
    std::vector<Stamp> stamps;
    std::array<float, 3> light{};
    void* owner = nullptr;
    float ceiling = 0.f;
    uint64_t floor_revision = 0;
    std::vector<MeshVertex> vertices;
    std::vector<uint32_t> indices;
  };
  std::unordered_map<void*, ProjectedShadow> projected_shadows_;
  std::vector<SceneInstance> instances_;
  std::vector<SceneBatch> prop_batches_;
  std::vector<Preview> previews_;
  std::unordered_map<const ScxGeometry*,ScenePart> instance_meshes_;
  // Geometry queued for a later frame's upload budget (discovery order).
  std::vector<const ScxGeometry*> pending_uploads_;
  void flush_pending_uploads();
  std::unordered_map<void*, const ScxGeometry*> deformed_meshes_;

  void sync_instances(const float* view, const float* projection);
  static SceneFrustum view_frustum(const float* view, const float* projection);
  void apply_video_mode(const VideoMode& mode);
  void update_static_shadow(const float* eye, const VideoOptions& video);

  // Diffuse * baked lightmap, the way the exe draws a map. No lightmap
  // (OSD, FMV, a mesh without the slot) keeps the existing shading.
  void bind_baked_light(const ScxGroup* group,
                        const std::vector<bgfx::TextureHandle>* tex,
                        bgfx::TextureHandle mix = BGFX_INVALID_HANDLE,
                        bool receive_static_shadow = false, const SceneSun* sun = nullptr, const float* sky = nullptr);


  void draw_instance(uint16_t view_id,const SceneInstance& instance,const float* matrix,bool preview,int pass,bool full_materials = false);
  void draw_prop_batches(uint16_t view_id, const std::vector<SceneBatch>& batches,
                         bool shadow, bool conservative = false);
  void refresh_distance_fog(const float render_eye[3]);

  void draw_sky(uint16_t view_id, const float* view, const float* projection);
  void draw_dynamic_world(uint16_t view_id);
  void draw_sun_flare(const float* view, const float* projection, uint16_t view_id);

  bool ensure_vehicle_shadow_target(int slot, uint16_t size);
  void draw_vehicle_shadows(uint16_t view_id, const float* eye, const VideoOptions& video);
  uint16_t vehicle_shadow_size_ = 0;

  void draw_previews();


  void expand_scene_bbox(const float mn[3], const float mx[3]);


  void destroy_part(ScenePart& p);

  void build_camera(float* view, float* proj);


  void upload_part(ScenePart& part);
  void upload_skin(ScenePart& part);
  // Renderer copy only: sections with identical materials become one group
  // over a reordered index buffer. Meshes with blended sections are kept.
  static void merge_material_groups(ScxGeometry& geo);


  static bool is_logo_texture_slot(const ScxGeometry& geo, uint16_t tex_i);


  static bool is_logo_group(const ScxGroup& g, const ScxGeometry& geo);


  // Horizontal mirror (u → 1-u) for banner/logo groups only.
  static void flip_logo_uv_horizontal(ScxGeometry& geo);


  void draw_scene(uint16_t view_id);


  int load_part_textures(ScenePart& part, bool share_map_textures = false);  // returns textures newly created


#ifdef _WIN32
  static LRESULT CALLBACK wnd_proc(HWND hwnd, UINT msg, WPARAM wp, LPARAM lp);

  bgfx::TextureHandle tex_for_osd_icon(const std::string& path, bool srgb = false);


  void draw_osd_quad(const OsdWidget& w, float cr = 1.f, float cg = 1.f,
                     float cb = 1.f, float ca = 1.f);


  bgfx::TextureHandle tex_for_font(void* font_key);


  void draw_osd_texts(int32_t only_layer, bool filter_layer, bool behind_ui);


  // One OSD line. y is the glyph top (y+ down). align 1 centers on x.
  void draw_osd_line(void* font_key, const std::string& text, float x, float y,
                     int align, uint32_t argb);


  // Osd.animate builds this after idleFor(1). The host never runs that
  // loop except for the credits scroller, so the plate is drawn here.
  // Row titles live in tip too; they are already painted beside the slider.
  void draw_tooltip();


  void draw_fmv(uint16_t view_id);


  // Draw a string in OSD text-space (same path as SlrrOsdText glyphs).
  void draw_osd_string(float x, float y, int32_t align, uint32_t argb,
                       const std::string& text, void* font_key);


  void draw_osd();
  void draw_vehicle_hud(const VehicleHudState& hud);
  std::string gps_hud_path_;
  std::unordered_map<std::string, ScxGeometry> hud_preview_meshes_;


  HWND hwnd_ = nullptr;
  bool shown_ = false;
  int mouse_x_ = 0;
  // previews_ is rebuilt every frame, so a spin stored on it never accumulates.
  // Keyed by the preview camera instead, which is stable across rebuilds.
  std::unordered_map<const void*, float> preview_spins_;
  // The slot's pivot is averaged from the instances, and previews_ is rebuilt
  // every frame with a varying set of parts -- so a pivot recomputed each frame
  // jumps around and a car in assembly comes apart. Fix it per camera on first
  // use and keep it.
  std::unordered_map<const void*, std::array<float, 3>> preview_pivots_;
  std::unordered_map<const void*, bool> preview_pivot_set_;
  int mouse_y_ = 0;
  int32_t tip_hover_ = -2;
  float tip_since_ = 0.f;
  bool tip_font_tried_ = false;
  int lmb_x_ = 0;
  int lmb_y_ = 0;
  bool have_mouse_ = false;
  bool raw_mouse_ = false;
  bool camera_cursor_clipped_ = false;
  bool lmb_down_ = false;
  int32_t slider_drag_index_ = -1;
  Object* inventory_drag_source_ = nullptr;
  bool inventory_dragging_ = false;
  char inventory_drag_cursor_ = 'J';
#endif

  bool ready_ = false;
  bool quit_ = false;
  bool garage_ = false;
  bool osd_overlay_ = false;
  // Openingpic stays up after the boot movie. Once the menu or its movie
  // has covered it, a later empty frame must not bring that still back.
  bool opening_retired_ = false;
  bool scx_ok_ = false;
  bool mesh_ok_ = false;
  int32_t osd_buttons_ = 0;
  uint16_t osd_view_ = kOsdUnder;
  int32_t width_ = 0;
  int32_t height_ = 0;
  int32_t frames_ = 0;
  float mesh_min_[3] = {0, 0, 0};
  float mesh_max_[3] = {0, 0, 0};
  std::vector<ScenePart> parts_;
  bgfx::VertexLayout layout_;
  bgfx::ProgramHandle program_ = BGFX_INVALID_HANDLE;
  bgfx::ProgramHandle inst_program_ = BGFX_INVALID_HANDLE;
  bgfx::ProgramHandle inst_shadow_program_ = BGFX_INVALID_HANDLE;
  bgfx::ProgramHandle skin_program_ = BGFX_INVALID_HANDLE;
  bgfx::UniformHandle u_skin_bones_ = BGFX_INVALID_HANDLE;
  bgfx::TextureHandle white_tex_ = BGFX_INVALID_HANDLE;
  // 1x1 far depth. The mesh shader's shadow sampler is a comparison
  // sampler, including the draws that do not receive a shadow.
  bgfx::TextureHandle depth_white_ = BGFX_INVALID_HANDLE;
  bgfx::TextureHandle fmv_tex_ = BGFX_INVALID_HANDLE;
  int32_t fmv_tex_w_ = 0;
  uint64_t fmv_tex_serial_ = 0;
  int32_t fmv_tex_h_ = 0;
  bgfx::UniformHandle s_tex_ = BGFX_INVALID_HANDLE;
  bgfx::UniformHandle s_lightmap_ = BGFX_INVALID_HANDLE;
  bgfx::UniformHandle s_mesh_mix_ = BGFX_INVALID_HANDLE;
  bgfx::UniformHandle u_lightmap_ = BGFX_INVALID_HANDLE;
  bgfx::UniformHandle u_lightmap_uv_ = BGFX_INVALID_HANDLE;
  bgfx::UniformHandle u_mesh_base_uv_ = BGFX_INVALID_HANDLE;
  bgfx::UniformHandle u_mesh_mix_uv_ = BGFX_INVALID_HANDLE;
  std::unordered_map<std::string, bgfx::TextureHandle> osd_tex_;
  std::unordered_map<void*, bgfx::TextureHandle> font_tex_;
  bgfx::UniformHandle u_alpha_test_ = BGFX_INVALID_HANDLE;
  bgfx::UniformHandle u_color_ = BGFX_INVALID_HANDLE;
  bgfx::ProgramHandle vehicle_program_ = BGFX_INVALID_HANDLE;
  bgfx::UniformHandle s_mix_=BGFX_INVALID_HANDLE,s_env_=BGFX_INVALID_HANDLE,s_bake_=BGFX_INVALID_HANDLE;
  bgfx::UniformHandle u_layers_=BGFX_INVALID_HANDLE,u_channels_=BGFX_INVALID_HANDLE;
  bgfx::UniformHandle u_env_filter_=BGFX_INVALID_HANDLE;
  bgfx::UniformHandle u_base_uv_=BGFX_INVALID_HANDLE,u_mix_uv_=BGFX_INVALID_HANDLE,u_env_uv_=BGFX_INVALID_HANDLE,u_diffuse_=BGFX_INVALID_HANDLE,u_spec_=BGFX_INVALID_HANDLE,u_vehicle_fade_=BGFX_INVALID_HANDLE,u_sun_diff_=BGFX_INVALID_HANDLE,u_sun_amb_=BGFX_INVALID_HANDLE,u_sun_spec_=BGFX_INVALID_HANDLE,u_sun_dir_=BGFX_INVALID_HANDLE;
  bgfx::ProgramHandle shadow_program_ = BGFX_INVALID_HANDLE;
  bgfx::ProgramHandle shadow_recv_ = BGFX_INVALID_HANDLE;
  bgfx::UniformHandle u_shadow_mtx_ = BGFX_INVALID_HANDLE;
  bgfx::UniformHandle u_shadow_ramp_ = BGFX_INVALID_HANDLE;
  bgfx::ProgramHandle static_shadow_program_ = BGFX_INVALID_HANDLE;
  bgfx::FrameBufferHandle static_shadow_fb_ = BGFX_INVALID_HANDLE;
  bgfx::TextureHandle static_shadow_tex_ = BGFX_INVALID_HANDLE;
  bgfx::UniformHandle s_static_shadow_ = BGFX_INVALID_HANDLE;
  bgfx::UniformHandle u_static_shadow_mtx_ = BGFX_INVALID_HANDLE;
  bgfx::UniformHandle u_static_shadow_params_ = BGFX_INVALID_HANDLE;
  std::array<float, 16> static_shadow_matrix_{};
  std::array<float, 3> static_shadow_focus_{}, static_shadow_sun_{};
  void* static_shadow_world_ = nullptr;
  bgfx::UniformHandle u_cloud_wind_ = BGFX_INVALID_HANDLE;
  bgfx::UniformHandle u_cloud_sky_ = BGFX_INVALID_HANDLE;
  bgfx::UniformHandle u_fog_color_ = BGFX_INVALID_HANDLE;
  bgfx::UniformHandle u_fog_range_ = BGFX_INVALID_HANDLE;
  bgfx::UniformHandle u_fog_eye_ = BGFX_INVALID_HANDLE;
  bool fog_enabled_ = false;
  float fog_near_ = 1500.f;
  float fog_far_ = 3000.f;
  float fog_color_[3] = {0.57f, 0.73f, 0.86f};
  float fog_eye_[3] = {};
  bool outdoor_lighting_ = false;
  bgfx::UniformHandle u_local_light_ = BGFX_INVALID_HANDLE;
  std::array<float, 4> local_light_{};
  void update_local_light();
  void update_reflections(const float* eye);
  void destroy_reflections();
  std::array<bgfx::TextureHandle, 2> reflection_cubes_{{
      BGFX_INVALID_HANDLE, BGFX_INVALID_HANDLE}};
  size_t reflection_write_ = 0;
  bgfx::TextureHandle reflection_depth_ = BGFX_INVALID_HANDLE;
  // One depth buffer per cube face, shared by the two cubes and by the
  // moving layer drawn onto the bake. A face without one is not depth
  // tested at all, which is what put every culled-in object on top of the
  // garage wall in the paint.
  std::array<bgfx::TextureHandle, 6> reflection_cube_depth_{{
      BGFX_INVALID_HANDLE, BGFX_INVALID_HANDLE, BGFX_INVALID_HANDLE,
      BGFX_INVALID_HANDLE, BGFX_INVALID_HANDLE, BGFX_INVALID_HANDLE}};
  bgfx::TextureHandle reflection_color_ = BGFX_INVALID_HANDLE;
  bgfx::FrameBufferHandle reflection_capture_ = BGFX_INVALID_HANDLE;
  // Prefilter chain (stage 2.2): every cube mip is filtered from the level
  // below it, all of them from the one capture. Roughness then selects a mip
  // instead of widening a runtime cone over the sharp cube.
  // One cube per blur level, per double-buffered capture. Every level lives
  // at mip 0 of its own cube; a non-zero mip of a cube cannot be blitted into.
  // Faces render straight into the cube as render-target attachments, and
  // D3D builds the mip chain itself (BGFX_ATTACHMENT_AUTO_GEN_MIPS). That
  // replaces the intermediate 2D target plus blit, whose levels came back
  // empty. One framebuffer per cube face.
  bgfx::FrameBufferHandle reflection_face_fb_[2][6]{};
  bgfx::TextureHandle reflection_chains_[2][4]{};
  bgfx::TextureHandle reflection_chain_[4]{};
  bgfx::UniformHandle s_reflection_cube1_ = BGFX_INVALID_HANDLE;
  bgfx::UniformHandle s_reflection_cube2_ = BGFX_INVALID_HANDLE;
  bgfx::UniformHandle s_reflection_cube3_ = BGFX_INVALID_HANDLE;
  bgfx::UniformHandle s_reflection_cube4_ = BGFX_INVALID_HANDLE;
  bgfx::FrameBufferHandle reflection_chain_fb_[4]{};
  bgfx::ProgramHandle blur_program_ = BGFX_INVALID_HANDLE;
  bgfx::UniformHandle u_refl_right_ = BGFX_INVALID_HANDLE;
  bgfx::UniformHandle u_refl_up_ = BGFX_INVALID_HANDLE;
  bgfx::UniformHandle u_refl_fwd_ = BGFX_INVALID_HANDLE;
  void build_reflection_mips();
  bgfx::UniformHandle s_reflection_cube_ = BGFX_INVALID_HANDLE;
  bgfx::UniformHandle u_dynamic_reflection_ = BGFX_INVALID_HANDLE;
  std::array<float, 3> reflection_origin_{}, reflection_sample_origin_{};
  std::array<float, 3> reflection_box_min_{}, reflection_box_max_{};
  bool reflection_box_valid_ = false;
  bgfx::UniformHandle u_reflection_probe_ = BGFX_INVALID_HANDLE;
  bgfx::UniformHandle u_reflection_box_min_ = BGFX_INVALID_HANDLE;
  bgfx::UniformHandle u_reflection_box_max_ = BGFX_INVALID_HANDLE;
  // Stage 1: the static world does not change between frames, so it is captured
  // once into its own cube and only recaptured when the world changes or the
  // probe has moved far enough for the parallax box to be wrong. The per-frame
  // capture then only has to deal with what actually moves.
  // Double buffered for the same reason the capture cube is: a cube cannot be
  // sampled while one of its faces is a render target.
  bgfx::TextureHandle reflection_bake_cube_[2]{};
  bgfx::FrameBufferHandle reflection_bake_fb_[2][6]{};
  int reflection_bake_write_ = 0;
  int reflection_bake_face_ = 0;
  bool reflection_bake_ready_ = false;
  int reflection_bake_batches_ = 0;
  int reflection_bake_passes_ = 0;
  uint32_t reflection_bake_frame_ = 0;
  const void* reflection_bake_world_ = nullptr;
  std::array<float, 3> reflection_bake_origin_{};
  std::array<float, 3> reflection_bake_box_min_{}, reflection_bake_box_max_{};
  bool reflection_bake_box_valid_ = false;
  uint16_t reflection_bake_size_ = 0;
  // The cube the materials sample: the bake when it exists, otherwise whatever
  // the per-frame capture produced.
  bgfx::TextureHandle reflection_cube_to_sample() const;
  void* reflection_world_ = nullptr;
  size_t reflection_face_ = 0;
  bool reflection_ready_ = false;
  // Distinct from reflection_scene_: that one decides whether a capture runs,
  // this one says the cubes hold a completed capture worth sampling. Stopping
  // the capture does not make a previously captured environment unusable, so
  // this must not follow reflection_scene_.
  bool reflection_env_valid_ = false;
  // Which world reflection_env_valid_ refers to, so a cubemap captured for one
  // world cannot be sampled by another.
  const void* reflection_env_world_ = nullptr;
  bool reflection_scene_ = false;
  bool static_shadow_active_ = false;
  bgfx::FrameBufferHandle car_depth_fb_ = BGFX_INVALID_HANDLE;
  bgfx::TextureHandle car_depth_tex_ = BGFX_INVALID_HANDLE;
  bgfx::UniformHandle s_car_depth_ = BGFX_INVALID_HANDLE;
  bgfx::UniformHandle u_car_depth_mtx_ = BGFX_INVALID_HANDLE;
  bgfx::UniformHandle u_car_depth_params_ = BGFX_INVALID_HANDLE;
  std::array<float, 16> car_depth_matrix_{};
  bool car_depth_active_ = false;
  void update_car_depth(const float* position, const VideoOptions& video);
  uint16_t static_shadow_size_ = 0;
  float static_shadow_distance_ = 0.f;
  bgfx::UniformHandle u_static_shadow_eye_ = BGFX_INVALID_HANDLE;
  std::array<float, 3> static_shadow_eye_{};
  VideoMode applied_video_mode_{};
  uint32_t reset_flags_ = BGFX_RESET_VSYNC;
  bgfx::TextureHandle shadow_tex_[4] = {
      BGFX_INVALID_HANDLE, BGFX_INVALID_HANDLE, BGFX_INVALID_HANDLE,
      BGFX_INVALID_HANDLE};
  bgfx::FrameBufferHandle shadow_fb_[4] = {
      BGFX_INVALID_HANDLE, BGFX_INVALID_HANDLE, BGFX_INVALID_HANDLE,
      BGFX_INVALID_HANDLE};

  // World color and depth live off the window so the depth can be read.
  // The window itself only receives one fullscreen copy.
  bgfx::ProgramHandle present_program_ = BGFX_INVALID_HANDLE;
  bgfx::UniformHandle u_present_params_ = BGFX_INVALID_HANDLE;
  // Diagnostic cubemap face viewer (SLRR_REFL_DUMP only).
  bgfx::ProgramHandle cubeview_program_ = BGFX_INVALID_HANDLE;
  bgfx::UniformHandle u_face_basis_ = BGFX_INVALID_HANDLE;
  bgfx::UniformHandle u_face_right_ = BGFX_INVALID_HANDLE;
  bgfx::UniformHandle u_face_up_ = BGFX_INVALID_HANDLE;
  bgfx::UniformHandle u_face_params_ = BGFX_INVALID_HANDLE;
  bgfx::UniformHandle u_scene_reflection_ = BGFX_INVALID_HANDLE;
  void draw_cube_faces();
  // True while the world renders into scene_fb_ and kPresentView tonemaps
  // it to the window (PBR plan step 2).
  bool scene_rt_active_ = false;
  bgfx::ProgramHandle hiz_program_ = BGFX_INVALID_HANDLE;
  bgfx::UniformHandle u_hiz_params_ = BGFX_INVALID_HANDLE;
  bgfx::FrameBufferHandle scene_fb_ = BGFX_INVALID_HANDLE;
  // Own HDR target with depth for the 3D catalogue previews, tone mapped
  // separately and composited over the SDR plate.
  bgfx::FrameBufferHandle preview_fb_ = BGFX_INVALID_HANDLE;
  bgfx::TextureHandle scene_color_ = BGFX_INVALID_HANDLE;
  bgfx::TextureHandle scene_depth_ = BGFX_INVALID_HANDLE;
  uint16_t scene_w_ = 0;
  uint16_t scene_h_ = 0;
  uint64_t scene_msaa_ = 0;
  bool ensure_scene_target(uint16_t width, uint16_t height);
  void destroy_scene_target();
  bgfx::TextureHandle scene_color_texture() const;
  bgfx::TextureHandle scene_depth_texture() const;
  void draw_fullscreen(uint16_t view, bgfx::ProgramHandle program,
                       bgfx::TextureHandle texture, const float* hiz_params,
                       bgfx::UniformHandle sampler = BGFX_INVALID_HANDLE,
                       uint64_t state = 0);
  void present_scene(uint16_t view);

  struct HizLevel {
    bgfx::TextureHandle tex = BGFX_INVALID_HANDLE;
    bgfx::TextureHandle read = BGFX_INVALID_HANDLE;
    bgfx::FrameBufferHandle fb = BGFX_INVALID_HANDLE;
    uint16_t w = 0;
    uint16_t h = 0;
  };
  struct HizRead {
    std::vector<std::vector<float>> levels;
    std::vector<uint16_t> width;
    std::vector<uint16_t> height;
    float view[16]{};
    float proj[16]{};
    float eye[3]{};
    void* world = nullptr;
    uint16_t full_w = 0;
    uint16_t full_h = 0;
    uint32_t ready_at = 0;
    uint32_t serial = 0;
    bool pending = false;
    bool valid = false;
    bool discard = false;
    bool garage = false;
  };
  std::array<HizLevel, kHizLevels> hiz_gpu_{};
  HizRead hiz_read_[2]{};
  int hiz_level_count_ = 0;
  int hiz_write_ = 0;
  int hiz_active_ = -1;
  uint32_t hiz_serial_ = 0;
  uint16_t hiz_source_w_ = 0;
  uint16_t hiz_source_h_ = 0;
  float hiz_eye_[3]{};
  float cull_view_[16]{};
  float cull_proj_[16]{};
  void destroy_hiz();
  void build_hiz_pyramid(uint16_t width, uint16_t height);
  void read_hiz_pyramid(const float* view, const float* proj);
  void promote_hiz(uint32_t frame);
  bool hiz_usable(const float eye[3]) const;
  bool hiz_visible(const float center[3], float radius) const;
  static bool hiz_keep(const float center[3], float radius, void* user);

};

}  // namespace slrr

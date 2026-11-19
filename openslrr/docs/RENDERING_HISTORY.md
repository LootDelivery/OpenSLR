# Rendering changes — map against the original

Reference: `upstream/main` = `d1c719d` (github.com/EliasFD3S/openslrr).
Compared tree: `HEAD` = `42d795e`, subtree `openslrr/`.

Statuses: **Verified**, **Build-verified**, **Unverified**, **Superseded/Reverted**.

## How to read this document

The two histories are unrelated. This repository was imported as a single
snapshot (`3371388`), so there are no commit objects for the earlier graphics
work, and `upstream/main` has moved on since that snapshot was taken. A file
can therefore differ for three reasons:

1. **we changed it** — the substance of this document;
2. **upstream advanced** — the original gained work we do not have;
3. **both**.

Where it cannot be told apart from the diff alone, the entry says so. Nothing
here is inferred from commit titles.

## Authorship

- **All rendering code and shader changes listed below were made by us.**
- **SCX car models: changed by the project owner, not in Git at all.**
  - `Focer.rpk` — almost every mesh re-exported with corrected normals.
    Known bug: textures are not assigned on the doors and wings.
  - `Baiern` — known bug: the exported left door was not at zero coordinates.
  This is content-side work. The renderer deliberately does not compensate for
  hard-crease normals.

---

## 1. Shaders

| File | Status | What |
|---|---|---|
| `shaders/fs_present.sc` | Verified | New. Tone mapping pass: exposure, `aces()`, no software gamma encode. `u_presentParams.z` passes source alpha through for the preview composite; `.w` is a debug alpha view. |
| `shaders/fs_cubeview.sc` | Verified | New. On-screen cubemap face viewer, sampling the cube directly. |
| `shaders/fs_hiz.sc` | Build-verified | New. HiZ reduction. |
| `shaders/fs_blur.sc` | **Dead code** | New but unused. Left by an abandoned blur chain. |
| `shaders/fs_white.sc` | Build-verified | New. |
| `shaders/fs_shadow_recv.sc`, `vs_shadow_cast.sc`, `vs_shadow_recv.sc` | Build-verified | New shadow passes. |
| `shaders/fs_mesh.sc` | Verified | World material path. No environment lookup. |
| `shaders/fs_vehicle.sc` | Verified | Vehicle material path: material kinds, environment lookup, `envPrefilter()`, layer mixing. Paint is still `mix(color, environment, k<=0.72)`. |
| `shaders/varying.def.sc`, `vs_mesh.sc` | Verified | Transform space is `S*v` with instance matrix `S*M*S`. |
| `shaders/fs_osd_text.sc`, `vs_osd_text.sc` | ambiguous | Present upstream, absent here. Either upstream added a text batching shader after our snapshot, or our snapshot predates it. Not ours. |

## 2. Render backend

| File | Status | What |
|---|---|---|
| `src/render/bgfx_lib_backend.cpp` | Verified | The bulk of the work. Frame ordering, view constants, `scene_fb_` and `preview_fb_`, reflection capture and publication, `update_local_light()`, `draw_previews()`, the present pass. |
| `src/render/bgfx_lib_backend.hpp` | Verified | View constants (`kPresentView` 68, `kPlateView` 69, `kPreviewPresent` 70, `kOsdView` 71), framebuffer members, `reflection_env_valid_` / `reflection_env_world_`, preview spin and pivot maps. |
| `src/render/bgfx_lighting.cpp` | Verified | `draw_instance`, `bind_baked_light`. Material kinds, `enhanced_env` gated on `reflection_env_valid_`, `live_env` on `reflection_env_valid_`, the `simple` vs `full_materials` split. |
| `src/render/bgfx_osd.cpp` | Verified | Interface routing between `kPlateView` and `kOsdView`, `draw_osd_quad`, premultiplied blending, interface texture loading. |
| `src/render/bgfx_shadows.cpp` | Build-verified | Shadow passes for multiple views. |
| `src/render/bgfx_backend.cpp` | Build-verified | Non-lib path. |
| `src/render/debug_overlay.cpp/.hpp` | Verified | Exposure, MSAA, reflection viewer toggle, render options. |
| `src/render/slrr_font.cpp` | Verified | Glyph rasterization and measurement. |
| `src/render/null_backend.cpp` | ambiguous | Present here, absent upstream. |
| `src/render/bgfx_program.hpp`, `osd_text_batch.hpp`, `scene_mesh_data.hpp` | ambiguous | Present upstream, absent here. |

## 3. Resources and runtime

| File | Status | What |
|---|---|---|
| `src/resources/part_runtime.cpp` | Verified | `part_is_driver_render()` — excludes the driver from the cubemap capture precisely, so pedestrians still reflect. |
| `src/resources/gameref.cpp` | Verified | `gameref_directional_lights()` filters on `directional_light.set`, which is why point and spot lights are unreachable from the renderer. |
| `src/resources/character.cpp` | Unverified | Changed; not examined in this audit. |

## 4. Colour and texture handling

**Verified.**

- Scene colour maps: `BGFX_TEXTURE_SRGB` at `upload_part`.
- Interface colour art: **no** sRGB flag — the OSD path is gamma straight
  through. Adding the flag made it read darker. These are correct
  independently and must not be unified.
- Interface premultiplication is enforced at `tex_for_osd_icon` for
  uncompressed DDS, so edges cannot fringe.

## 5. Superseded and reverted work

Recorded because they explain the current design.

| Thing | Status | Why it matters |
|---|---|---|
| Interface drawn into `scene_fb_` | Superseded | Interface went through exposure and ACES; backgrounds came out uniformly darker than the original. |
| Moving `kOsdUnder` wholesale past the present pass | **Reverted** | It carried the catalogue plate *and* sat in front of the previews; the plate painted over them and the meshes appeared to vanish. Led to `kPlateView` and the corrected ordering. |
| `4f13ca3` fullscreen backdrop rule | **Reverted** (`749dee2`) | Broke the vehicle catalogue; the backdrop needs `kPlateView` to actually draw first. |
| `FrameBufferHandle{}` as "no framebuffer" | Fixed | Value-initialises the index to 0, which is a framebuffer, not the window. `kPlateView` output went elsewhere and the plate vanished. |
| `fs_present` forcing alpha to 1.0 | Fixed | The preview composite blends by alpha; forcing 1.0 made it claim the whole window and the screen went black. |
| `sRGB` flag on all UI textures | Superseded | Scoped back to scene maps only; on interface it double-processed. |
| `update_local_light` as the garage light | Present | One hardcoded point over `misc.garage:0x001D`, +3.3 in Y, radius 8, intensity 8, plus a x3.0 and warm tint in the shader. |

## 6. Reflection blur

`shaders/fs_blur.sc`, `build_reflection_mips()`, `reflection_chain_` and
`blur_program_` are **live, not dead**: the blur runs on the reflection chain.
It is simply weak. Earlier notes in this repository called this dead code;
that was wrong.

## 7. Not present here, present upstream

Vehicle dynamics, race runtime, road routes, wheel mounts, mass properties,
job system, texture batch OSD, various `assets/validation` suites. These are
upstream work after our snapshot and are listed so they are not mistaken for
things we removed.

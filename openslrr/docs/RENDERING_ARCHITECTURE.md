# Rendering architecture at HEAD

Audited against the source at commit `42d795e` (branch `main`).
Reference tree for "what the original did": `upstream/main` =
`d1c719d` (github.com/EliasFD3S/openslrr).

Statuses used below: **Verified** (confirmed in game), **Build-verified**
(builds, not exercised in game), **Unverified** (code exists, correctness not
confirmed), **Planned**, **Superseded/Reverted**.

> Note on evidence. The two Git histories are unrelated: this repository was
> imported as a single snapshot (`3371388`), so earlier graphics work has no
> commit objects here. Everything below is therefore asserted from the source
> at HEAD, not from commit archaeology.

---

## 1. Frame in one paragraph

World geometry renders into `scene_fb_` (RGBA16F + D32F). Views 0-4 build
shadows, view 5 draws the world, view 7 used to carry interface. `kPresentView`
(68) tone-maps that target to the window with `fs_present.sc`. Catalogue
previews render into `preview_fb_` (RGBA16F + D32F) and are composited at
`kPreviewPresent` (70) by premultiplied alpha. Interface plates draw at
`kPlateView` (69), icons and text at `kOsdView` (71). Reflections capture at
views 72+, the cubemap viewer at 240+.

Order that matters: scene -> present -> plates -> preview composite -> UI.

## 2. Colour pipeline

**Verified.**

- Colour maps (`part.tex[]`) are created with `BGFX_TEXTURE_SRGB`
  (`bgfx_lib_backend.cpp`, `upload_part`), so they decode on sample.
- Lighting multiplies linear values.
- `fs_present.sc` applies `exposure` and `aces()` and does **no** software
  gamma encode.
- The window is an sRGB backbuffer (`BGFX_RESET_SRGB_BACKBUFFER` at
  `bgfx::init` and every `bgfx::reset`), so linear -> sRGB happens in
  hardware. Exactly one decode and one encode.
- `SLRR_NO_HDR=1` selects the legacy path: no RGBA16F target, no ACES pass.
  Used for A/B measurements.

Interface deliberately does **not** follow this. See section 4.

## 3. Reflections

**Verified** in garage, catalogue and inspection mode. Not a complete system:
see limitations.

Capture (`update_reflections`, `bgfx_lib_backend.cpp`):
- one 512x512 RGBA8 cubemap per frame budget, six faces per frame;
- faces render into `reflection_face_fb_[2][6]`, attachments built with
  `AUTO_GEN_MIPS` on the last face only;
- two cubes are kept and swapped: capture writes one, materials sample the
  other (`reflection_cubes_[reflection_write_ ^ 1]`), so a half-built cube is
  never read;
- publication flips `reflection_write_` only after all six faces are in.

Sampling is gated by `reflection_env_valid_`, which is **not** the same as
whether a capture is running:

- set only after a full capture is published;
- cleared by `destroy_reflections()` and when the captured world differs from
  the current one (`reflection_env_world_`);
- deliberately **not** cleared when `reflection_scene_` goes false, because
  stopping the capture does not make a captured environment unusable. That is
  what lets the catalogue use the garage cubemap.

Excluded from capture: `object.sky`, `vehicle_material` parts, the driver
(`part_is_driver_render`, `part_runtime.cpp`), and skinless skinned meshes. The
driver exclusion is precise rather than "no characters", so pedestrians still
reflect.

**Limitations, stated because they matter:**

- The mip chain is `AUTO_GEN_MIPS`, i.e. plain downsampling. This is **not**
  GGX prefiltering. `fs_vehicle.sc` `envPrefilter()` approximates roughness by
  sampling a fixed LOD range, which is not a prefilter.
- The cubemap is **RGBA8 / LDR**. HDR reflection capture is a separate task.
- Box projection applies to interiors only; outdoors is direction-only lookup.
- `build_reflection_mips()`, `fs_blur.sc`, `reflection_chain_` and
  `blur_program_` are **live**: the blur runs on the reflection chain but is
  weaker than intended.

## 4. Interface, and why it cannot simply join the scene

**Verified.**

Interface is display-referred and draws to the window, not into `scene_fb_`.
Routing is in `bgfx_osd.cpp`; the decision is per widget:

```cpp
osd_view_ = modal ? kOsdView
          : (plate_behind_preview(w) || w.background || layer == front_window_layer)
                ? kPlateView
                : kOsdView;
```

Two separate reasons, both learned the hard way:

1. If interface draws into `scene_fb_` it goes through exposure and ACES with
   the world. The backgrounds came out uniformly darker than the original art.
2. If interface is moved past the preview composite it covers the 3D previews.
   A first attempt moved `kOsdUnder` wholesale and destroyed the catalogue.

The composite blends premultiplied (`ONE / INV_SRC_ALPHA`) and passes the
source alpha through; the world present forces alpha to 1, because the desktop
compositor reads the window's alpha and a zero alpha darkens the whole window.

`fs_present.sc` selects this with `u_presentParams.z` (pass alpha through) and
`.w` (debug: show the composite's own alpha, `SLRR_SHOW_ALPHA=1`).

Textures: colour interface art is **not** sRGB-flagged. The OSD path carries
gamma data straight to the window; the flag made it decode and re-encode and
read darker. Scene colour maps **are** flagged. These two are correct
independently and must not be unified.

Premultiplication is enforced at the interface loading point
(`tex_for_osd_icon`) for uncompressed DDS, so every piece of interface art is
premultiplied and edges cannot fringe. PNG and PTX already arrive multiplied.
Compressed DDS cannot be multiplied without decoding; none is used as interface
art.

## 5. Catalogue and 3D previews

**Verified.**

Previews render into their own HDR target `preview_fb_` with its own depth,
tone-mapped separately, then composited over the SDR plate and under the 2D UI.
A dedicated `kPreviewClear` view clears the whole target each frame; clearing
only the first preview view left the slots accumulating.

Material: `draw_instance` takes `preview` (which pass) and `full_materials`
separately. `simple = preview && !full_materials` is what trims the material
(no local lights, no glass, no cutouts). The catalogue asks for the full
material; small insets keep the trimmed one.

Rotation: the slot turns, not its parts. The angle accumulates in a map keyed by
preview camera (`previews_` is rebuilt every frame, so storing it on the preview
reset it), and the rotation folds into the preview's view matrix as
`about * view` -- bx vertices are row vectors, so `view * about` would turn the
camera instead. The pivot is fixed per slot on first use.

## 6. Scene instances and transforms

Vertex data is uploaded Z-negated, so shader render space is `S*v` and an
instance matrix is `S*M*S`. Preview cameras are built in that same space
(`view = inv(M)*S`), which is why negating only the Z column of the view turned
the lens around. The reflection origin is likewise mirrored in Y/Z. Anything
that adds a transform to this path must respect the conjugation.

## 7. Materials today

Two different models, not one.

- `fs_vehicle.sc` has material kinds (paint, chrome, glass, colour mix), an
  environment lookup, `envPrefilter()`, and layer mixing. The current paint is
  `mix(color, environment, k<=0.72)`.
- `fs_mesh.sc` has no equivalent and is a simpler diffuse path.

They share `u_localLight` but not the surrounding model. Unifying them is its
own step and is not to be done as a side effect of adding lights.

`bind_baked_light()` sets a **white texture** where a lightmap would go and
ignores the texture list it is passed. Parsed lightmap and illumination data
does not reach the GPU.

## 8. Lighting today

**Verified**, and limited.

- One directional light: `SceneSun`, resolved by `scene_instances_light()`.
  Colour and intensity come from `setLight` (time of day); the resource file
  supplies direction.
- One local light: `update_local_light()` takes the first `misc.garage:0x001D`
  node, offsets it +3.3 in Y, intensity 8, radius 8. The shader then multiplies
  by 3.0 and applies a warm tint. Those two constants are the entire garage
  lighting.
- `part_lamp_lit()` controls whether a lamp mesh is **visible**. It creates no
  light.

The game data already describes Point and Spot lights in a D3D-style text
format (`theta`, `phi`, `attenuation`, `range`, `attH_texture`, `lit_dynamic`).
The backend parses `type`, `diffuse`, `ambient`, `specular`, `direction` and
steps over the rest; `gameref_directional_lights()` filters non-directional
nodes out entirely. See `LIGHTING_AUDIT.md` for the scanned resource blocks.

## 9. Shadows

Static shadow map for the world (views 0-4), a garage shadow map, vehicle depth
and projected shadow decals. Alpha-cutout and transparent groups have their own
shadow handling. Built for one sun frustum; multiple lights casting shadows is
not supported.

## 10. Integration points for the original project

What can be taken independently:

- the sRGB backbuffer and the single-encode present pass;
- the HDR target plus `SLRR_NO_HDR` A/B switch;
- the reflection capture/publish double-buffer and the `reflection_env_valid_`
  split from `reflection_scene_`;
- the interface routing that keeps UI out of the scene target.

What depends on what:

- interface routing depends on the preview composite existing;
- the preview composite depends on the source alpha being preserved in
  `fs_present.sc`;
- reflections sampling in the catalogue depends on `reflection_env_valid_`
  surviving a scene rebuild;
- `BGFX_TEXTURE_SRGB` on scene maps depends on the backbuffer being sRGB.
  Changing either alone changes brightness.

Requirements: bgfx with `BGFX_RESET_SRGB_BACKBUFFER`, RGBA16F render targets,
`AUTO_GEN_MIPS` on cube faces. Shader changes are confined to `fs_present.sc`,
`fs_vehicle.sc`, `fs_cubeview.sc` and the mesh varyings.

Legacy SCX content keeps working; there is no new material descriptor.

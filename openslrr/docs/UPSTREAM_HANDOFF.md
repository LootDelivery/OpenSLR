# UPSTREAM HANDOFF — taking this tree's renderer to OpenSLRR

Everything else in `docs/` describes this tree to itself. This file is written
for the OpenSLRR maintainer: what is here that upstream does not have, which
files carry it, and what an integrator has to know before lifting it. It is not
a diff and not a patch series — the two histories have no merge base, and most of
this work would have to be re-landed against upstream's own backend rather than
cherry-picked.

Checked against `upstream/main` = `640bf78c` (`bgfx_lib_backend.cpp`,
`shaders/fs_vehicle.sc`). Status words are the ones used across `docs/`:
**Verified** (seen in game), **Build-verified** (compiles, not exercised),
**Planned**.

The short version, as of that commit: upstream renders each reflection face into
a separate colour+depth target and blits it into a 256² RGBA8 cube with **no mip
chain**, and `fs_vehicle.sc` samples that cube at mip 0 — `textureCube(...)`, no
LOD. So reflections are perfectly sharp, roughness has nowhere to go, and the
diffuse ambient is still one flat colour. Those two gaps are what items 1 and 3
close.

Highest value first. Nothing below needs assets we cannot share: the work is C++,
GLSL (`.sc`) and Markdown.

---

## 1. A roughness chain: GGX-prefiltered cube levels

**Verified.** The single largest difference, and the one that changes what a car
looks like.

| | upstream `640bf78c` | here |
|---|---|---|
| face size | 256² | 512² (`SLRR_REFL_RES`, clamped 64-2048) |
| cube mips | none (`createTextureCube(size, false, 1, …)`) | six levels, each a separate face whose level 0 is a blit of the capture |
| material input | `textureCube(cube, ray)` — mip 0, sharp | physical roughness → level, `textureCubeLod` |
| diffuse ambient | one flat colour | probe integrated from the same capture |

Files: `src/render/bgfx_lib_backend.cpp` (`build_ggx_cube`, the view ids
`kReflectionPrefilterView` + `level * 6 + face`), `shaders/fs_prefilter.sc`,
`shaders/fs_vehicle.sc` (`envPrefilter`, `u_envLod`),
`src/render/debug_overlay.cpp` (`Physical reflection roughness`,
`Reflection roughness cap`), `include/slrr/host_game_options.hpp`.

Shape of the change: level `m` stands for roughness `0.02 * 2^m`, which is the
angular size of `2^m` capture texels, so a prefiltered level occupies the place a
box-filtered mip would have had and nothing downstream changes its softness
except the lobe shape. Levels 1-5 are drawn by `fs_prefilter.sc` (twelve plus
eight taps, importance sampled); level 0 is a `bgfx::blit` of the capture face,
because drawing it costs 512²×12 taps to reproduce a face that already exists.

What an integrator must know, in the order it bites:

1. **`bgfx::touch(view)` discards the encoder.** It is `discard()` plus an empty
   submit, so uniforms set before it are gone. Setting the per-face basis first
   produced zero vectors, `normalize` of nothing, NaN, and every convolution
   wrote one flat colour — on screen, reflections that vanished a third of a
   second after start. `touch` first, then `setUniform`.
2. **A convolution pass needs a linear sampler.** Our `draw_fullscreen` helper
   hard-coded point sampling, which is correct for one-texel copies (present,
   blur, dump, Hi-Z) and destructive for a convolution: twelve point taps of a
   512² mip are twelve arbitrary texels, different every cycle. In bgfx, linear
   is the *absence* of `MIN_POINT`/`MAG_POINT`; the sampler here is
   `U_CLAMP|V_CLAMP|W_CLAMP` and nothing else. Also let the source mip follow
   the level — level `m` reads mip `m-1` — because an integral must be sampled
   at the scale it integrates.
3. **The face basis is the capture's own lookAt rows with the v axis negated.**
   A cube face is written with the projection's up while the fullscreen quad's
   texcoord runs the other way, so cross products of the face axis come out a
   mirror image of what the capture wrote — on a car, the road in the sky of the
   reflection. Do not settle this by brightness: a swapped pair of faces carries
   almost the same mean, and comparing means passes an upside-down chain. Compare
   the image: sky pixels in the top half against the bottom half of each face.
4. **A chain cycle must fit in one gate.** Spreading thirty-six faces over frames
   makes the published cube hold for ~50 ms and then jump, which on a smooth body
   reads as a framerate drop, not as lag. Publishing the whole chain per gate put
   the age at 10 ms and cost 0.18 ms a frame against the chain switched off.
5. **Roughness to level must be physical.** `1 - specularExponent/24` is a
   fresnel/occlusion normalisation, not a roughness; with it every paint landed
   below level 1, i.e. back on the capture, and the chain looked like it did
   nothing. `alpha = sqrt(2/(e+2))`, `lod = clamp(log2(alpha/0.02) + bias, 0,
   cap)` puts paint on levels 3-4, chrome on 1-2, a mirror below 0.5 — the last
   one still reads the capture, which is the right answer for a mirror.

Note on capture structure: upstream renders a face into a separate colour+depth
target and blits it into the cube, which is why depth already works there. This
tree renders *directly* into the cube face instead, which needs a per-face
`D24S8` attachment — and that attachment has to be created with
`BGFX_ATTACHMENT_NONE`, because the default `AUTO_GEN_MIPS` makes bgfx reject it
**silently** and the face is then painted in submission order: cars and trees show
through walls. If you keep the separate-target structure, none of that applies to
you; if you switch to direct-to-face for the chain, it does.

## 2. HDR reflection capture

**Planned here too**, listed because it is the next thing the chain needs: the
cube is still RGBA8, so a highlight is clipped before the prefilter sees it and
the chain cannot recover what was never written. `PBR_PLAN.md` order of work.
Levels for `alpha` ≥ 0.8 (8² and 4²) belong with it.

## 3. A diffuse irradiance probe

**Verified.** Sixteen texels a face, six faces integrated in one frame,
ninety-six cosine-weighted taps a face read from capture mip 5 (there one tap
already spans a probe face), no mip chain of its own. On by default here.

Files: `build_irradiance_cube` in `bgfx_lib_backend.cpp`,
`shaders/fs_irradiance.sc`, the ambient mix in `shaders/fs_vehicle.sc`,
`u_irrParams` in `src/render/bgfx_lighting.cpp`.

Two traps, both of which cost a pass:

- The probe is an integral of a captured image, so its absolute level is whatever
  that image holds — about 1.5× the flat ambient here, and 1.5× *under* it when a
  gain was derived from the average of sRGB-encoded 8-bit texels. It now
  calibrates itself in the shader: `u_sunAmb / textureCube(probe, up(vec3(0,1,0)))`
  anchors an upward-facing surface to the level the material had before, clamped
  because at night the sky is darker than the lit road. Switching the probe on
  then changes the *shape* of the ambient, not its brightness, which is what makes
  it safe to ship on.
- Dividing by the probe's own average fails outright: the probe writes only mip 0,
  so the divisor came from an empty chain, collapsed to its floor and saturated
  the car white.

## 4. Interface out of the scene, and HDR previews

**Verified, and a prerequisite for anything upstream does with AO or post.** The
scene renders to RGBA16F + D32F, `kPresentView` tone-maps with ACES to an sRGB
backbuffer with exactly one decode and one encode; catalogue previews get their
own HDR target composited by premultiplied alpha between SDR plates and the 2D
UI; interface is display-referred at `kPlateView`/`kOsdView`. Details, including
the three attempts that were reverted: `RENDERING_ARCHITECTURE.md` §2, §4, §5.

## 5. What not to take

- **Instanced submission of vehicles.** Implemented and reverted (`bcbc4f1b`):
  CPU cost halved and 2100 of 6250 draws disappeared, but the GPU paid
  3.59 → 5.77 ms for the same triangles. `bgfx::allocInstanceDataBuffer` also
  clamps silently, returns a pointer anyway, and corrupted the buffers of the
  draws that followed (cars collapsed into sheets). The number to measure is the
  GPU one, not the draw count. Rationale in `UPSTREAM_INTEGRATION.md`, "What was
  built and why it is gone".
- **Box-average reflection blur** (`fs_blur.sc`, `build_reflection_mips()`): dead
  here too — no call site. The GGX chain replaces it; the file is kept only for
  its design comment.
- Reflection cadence or uniform-binding changes made "for performance" without a
  visual check: throttling the capture made painted doors lose their reflections,
  and a chain that spans gates reads as an fps drop (item 1.4).

## 6. Integration order

1. Interface out of the scene + HDR targets (item 4): it is what makes the rest
   measurable, and it is independent.
2. Linear sampler for the fullscreen helper, `touch`-then-`setUniform`, the
   lookAt basis (item 1.1-1.3) — these three are what make any convolution pass
   work at all, and they are small.
3. The GGX chain, then the physical roughness mapping in the material (item 1).
4. The probe (item 3), last: it depends on the same basis and on a working
   capture, and it is the one item whose default is a taste decision.
5. HDR cube plus levels for `alpha` ≥ 0.8 together with authored roughness
   (item 2).

## 7. Verifying it landed

The acceptance used throughout this tree, in order of how much it catches:

- a bench run that spawns a car and drives it (`--bench-map ValoCity
  --bench-drive`), with screenshots every N frames (`SLRR_BENCH_SHOT_FRAMES`);
- paint/chrome/glass brightness and step-to-step change across those shots, not a
  single frame — a pumping ambient and a stepped chain only show up as
  frame-to-frame differences;
- the dump panels compared face by face, orientation included: `SLRR_REFL_DUMP=2`
  draws two rows of six faces (row 0 the chain or the probe, row 1 the raw cube).
  This bgfx build has neither a working `bgfx::read` nor a blit-to-screen path, so
  a viewer like this is the only way to see a cube; `SLRR_REFL_DUMP_MIP` picks the
  level, `SLRR_REFL_DUMP_IRR` puts the probe in row 0;
- `[refl-age]` from `SLRR_REFL_AGE` for the chain cycle, `--perf-log` for the
  frame and `refl` scopes.

Related switches, all environment-only and off unless listed: `SLRR_REFL_GGX`
(`0` off), `SLRR_REFL_GGX_FACES` (1…36), `SLRR_REFL_IRR` (`-1` follow the panel,
`0` off, `>=1` on), `SLRR_REFL_LOD`, `SLRR_REFL_LOD_CAP`, `SLRR_REFL_AGE`,
`SLRR_REFL_RES`, `SLRR_REFL_BAKE`, `SLRR_ENV_DEBUG` (`1` environment only, `7`
ambient term, `8` probe/flat ratio, `9` probe unmixed).

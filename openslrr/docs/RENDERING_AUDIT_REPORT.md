# Rendering audit — final report

**HEAD:** `42d795e` (`42d795ee8e15b26f5dd9d1a0d6b6e6f1e4265640`), branch `main`.
Reference tree: `upstream/main` = `d1c719d`
(github.com/EliasFD3S/openslrr). Working tree clean at the start of the audit.
Production code was not touched; only Markdown changed.

## 1. Map of graphics changes

Full per-file map with statuses: `RENDERING_HISTORY.md`. Summary:

- **Shaders** — `fs_present.sc`, `fs_cubeview.sc`, `fs_hiz.sc`, `fs_white.sc`,
  `fs_blur.sc` and three shadow shaders added; `fs_mesh.sc`, `fs_vehicle.sc`,
  `vs_mesh.sc`, `varying.def.sc` modified.
- **Render backend** — `bgfx_lib_backend.{cpp,hpp}`, `bgfx_lighting.cpp`,
  `bgfx_osd.cpp`, `bgfx_shadows.cpp`, `debug_overlay.{cpp,hpp}`,
  `slrr_font.cpp` modified; `null_backend.cpp` added.
- **Resources** — `part_runtime.cpp` (driver exclusion), `gameref.cpp`
  (light filtering), `character.cpp`.
- **Content, not in Git** — SCX car models, owner work.

A file can differ from `upstream/main` for three reasons: we changed it,
upstream advanced, or both. Where the diff cannot tell, the map says so.

## 2. Chronology

The two histories are unrelated. This repository was imported as one snapshot
(`3371388`, 2026-11-18), so the earlier graphics work has no commit objects
here. The order below is reconstructed from the documents written at the time
and from the code, not from commit archaeology.

1. **Reflection system rebuild** — dynamic cubemap, direct render into faces,
   `AUTO_GEN_MIPS` on the last face, generation-based throttle, box projection.
2. **Stability work** — flicker root cause, stale framebuffers (one fix
   reverted because it broke reflections), driver exclusion from capture.
3. **Cubemap face viewer** — on-screen diagnostics, because `bgfx::read` and
   `blit` are unusable in this bgfx build.
4. **Colour-space audit** — proved the pipeline was gamma end to end.
5. **Linear migration** — stage 1+3 together (sRGB decode on scene maps,
   software gamma removed from the present pass).
6. **HDR and UI separation** — sRGB backbuffer, `preview_fb_`, `kPlateView`,
   interface out of the scene target. Several attempts reverted.
7. **Catalogue work** — full materials in previews, environment sampling,
   pivot rotation, tooltip sizing.
8. **Lighting audit and roadmap** — game data scanned, Point/Spot found
   authored and unused.

In this repository, all 40 post-import commits are documented and verifiable.

## 3. Architecture

`RENDERING_ARCHITECTURE.md`. In short: world renders into `scene_fb_`
(RGBA16F + D32F), `kPresentView` tone-maps to an sRGB window with exactly one
decode and one encode; catalogue previews have their own HDR target and are
composited by premultiplied alpha between SDR plates and the 2D UI; reflections
capture into two cubedmaps that swap on publication, with sampling gated by
`reflection_env_valid_` rather than by whether a capture is running.

## 4. Documentation discrepancies corrected

| Document | Was | Is |
|---|---|---|
| `LINEAR_MIGRATION.md` | `BGFX_TEXTURE_SRGB` "used nowhere" | applied to scene colour maps; deliberately absent on interface art |
| `UI_HDR_SEPARATION.md` | non-modal UI tone-mapped in `kOsdUnder` | that was the problem state; routing has changed, kept as history |
| `AUTOMOTIVE_RENDERING.md` | "pipeline is gamma end to end" | true when audited, since fixed; marked as history |
| `AUTOMOTIVE_RENDERING.md` | stage 6 covered lighting | split: 6 tone mapping, 6b light system |
| our own notes | reflection blur "dead code" | live and working, just weak |
| our own notes | `bind_baked_light` white texture a defect | deliberate: the stock lightmap already holds the sun |
| our own notes | camera click mismatch open | situational, closed |
| our own notes | MSAA artefact open | not reproducible, closed |

## 5. Markdown files

**Created:** `RENDERING_ARCHITECTURE.md`, `RENDERING_HISTORY.md`,
`RENDERING_VALIDATION.md`, `LIGHTING_AUDIT.md`.

**Changed:** `LINEAR_MIGRATION.md`, `UI_HDR_SEPARATION.md`,
`AUTOMOTIVE_RENDERING.md`.

**Studied, unchanged:** `AGENTS.md`, `ENGINE_CONTRACT.md`, `PBR_PLAN.md`,
`BACKLOG.md`, `KNOWLEDGE_BASE.md`, `MILESTONES.md`, `NATIVE_AUDIT.md`.

## 6. Technical problems and unfinished mechanisms

| Item | State |
|---|---|
| GGX prefilter | **not implemented.** The mip chain is `AUTO_GEN_MIPS`, i.e. downsampling. `envPrefilter()` approximates roughness with a fixed LOD range. |
| HDR reflection capture | not implemented; the cubemap is RGBA8/LDR and bright sources clip. |
| Reflection blur | runs, weaker than intended. |
| Minimap in the city | does not expand to full size. |
| `bind_baked_light` | binds `white_tex_`; the lightmap list is discarded, deliberately. |
| `fs_mesh.sc` vs `fs_vehicle.sc` | two different lighting models, not unified. |
| Point/Spot lights | authored in game data, unused by the backend. |
| Multi-light shadows | not supported; shadow passes are built for one sun frustum. |
| Owner's SCX bugs | `Focer.rpk` missing textures on doors and wings; `Baiern` left door not at zero coordinates. |

## 7. Questions needing game tests

1. Does the reflection double buffer stay correct over many scene transitions?
2. What happens when the catalogue opens before the first cubemap capture?
3. Dynamic reflections toggled off and on mid-session.
4. Ultrawide aspect: preview slot placement and tooltip plate sizing.
5. Long-session stability (30+ minutes) of reflections and framebuffers.
6. What exactly the garage look depends on: the +3.3 offset, radius 8 or
   intensity 8 in `update_local_light()`.

## 8. Proposed next order

1. **Light data contract** — one structure, how a light reaches the main render
   *and* the cubemap capture. Written before any branch starts.
2. **Branch A — materials and IBL**: GGX prefilter (real, not mip generation),
   DFG LUT, base Cook-Torrance, layered automotive paint. Legacy SCX keeps
   working with no new descriptor.
3. **Branch B — dynamic lighting**: Point/Spot from game data, light registry,
   game-state wiring, then spot shadows from headlights, then CSM.

Branch A can proceed on the current light rig; B does not need A. The shared
contract is what keeps them independent.

## 9. Handover notes

What the integrator needs to know first:

- The sRGB backbuffer and the sRGB flag on scene maps **must change together**.
- Interface must not draw into `scene_fb_`. Two separate attempts to move it
  broke the catalogue in different ways; see `RENDERING_HISTORY.md` section 5.
- `bx` vertices are row vectors; `view * about` turns the camera, not the
  object.
- Preview transforms live in `S*M*S` conjugated space. Adding a transform
  without respecting it inverts the lens.
- Never measure with `SLRR_REFL_BASE` — it forces reflection weight to 1.0 and
  has invalidated A/B runs twice.

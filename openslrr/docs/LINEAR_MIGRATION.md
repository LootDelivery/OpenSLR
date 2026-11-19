# Migration to a linear rendering pipeline

Target architecture chosen: **option A, a genuinely linear pipeline**, migrated
in stages with separate commits. Nothing here changes the rendering behaviour
in one step.

This document records the migration plan and its outcome. It is split into
three parts so history is not read as current state:

1. **Historical baseline** -- what the audit found before any change.
2. **The implemented migration** -- what actually landed.
3. **Current state and remaining work**.

Only part 3 describes the renderer as it is.

Ground rules taken from the decision:

- `RGBA16F` alone does not make data linear. Every shader writing into a new
  linear render target must be verified to actually emit linear values.
- Texture semantics are explicit: albedo / diffuse / emissive are sRGB, normal
  / roughness / metallic / masks are linear. `BGFX_TEXTURE_SRGB` **is** applied
  now to scene colour maps (stage 1+3 landed); data maps stay unflagged.
  Gamma handling is therefore no longer the constraint on these formulas.
- No double gamma encode/decode at any point.
- In the linear architecture `Present` performs tone mapping and the single
  conversion needed to reach the display. If the backbuffer is sRGB, no
  additional software encode.
- Old SCX materials keep their visual behaviour unless a separate test says
  otherwise.
- The Raw Environment Cubemap and the Prefiltered Environment Cubemap are
  separate `RGBA16F` resources holding linear values. GGX filtering runs only
  over linear HDR.
- The wrong reflections on the right-hand body panels are a separate matter and
  are not mixed into this work.

## 1. Historical baseline (measured before the migration)

> Everything in this section is the **pre-migration** state and is kept as
> history. It does not describe the renderer today.

| Thing | State |
|---|---|
| `BGFX_TEXTURE_SRGB` | **superseded statement**: this document was written before the change landed. It is applied now to scene colour maps in `upload_part` (`bgfx_lib_backend.cpp`). Interface art deliberately does **not** use it -- see `RENDERING_ARCHITECTURE.md` section 4. |
| Texture formats created | `RGBA8`, `D32F` (plus the scene colour target) |
| Scene colour target | `RGBA16F` |
| Present (then) | `pow(x, 2.2)` → exposure → ACES → `pow(x, 1/2.2)` |
| Consequence (then) | the pipeline was gamma-space end to end; the cubemap held gamma-encoded values |

The last row is why a GGX convolution must not start yet: convolution over
gamma-encoded values is wrong, and `RGBA8` also clips the highlights.

## 2. The implemented migration

## Stage 0 — audit, no behaviour change (historical)

**Done.** Result, by reading the arithmetic:

| Shader / resource | Emitted today | Notes |
|---|---|---|
| `fs_mesh.sc` | `albedo × light` | no gamma op anywhere in the file |
| `fs_vehicle.sc` | `lit`, `shaded`, `shine` | no gamma op; `pow()` calls are Phong/Fresnel exponents, not gamma |
| `fs_white.sc` | constant | |
| `fs_present.sc` (then) | `pow(x, 2.2)` → exposure → ACES → `pow(x, 1/2.2)` | was **the only place gamma appeared**; stage 3 removed both `pow` calls |
| `fs_hiz.sc`, `fs_blur.sc` | passes | |
| colour textures (albedo/emissive) | decoded on sample via `BGFX_TEXTURE_SRGB` (stage 1+3 landed) | `RGBA8` + `BGFX_TEXTURE_SRGB` |
| normal / rough / mask textures | linear, sampled raw | `RGBA8`, no flag |
| scene colour target | `RGBA16F` | format float, contents gamma-leaning |

**Consequence:** the shading multiplies gamma-encoded albedo by light values,
so the result is neither strictly linear nor a clean gamma — it is the usual
gamma-space lighting approximation. The float target does not make it linear.

**Useful follow-on:** once the colour maps are decoded on sample (stage 1) and
the lights are linear, the existing shading arithmetic *already* produces
linear values. Stage 2 is therefore expected to be much smaller than the plan
assumed, and the table above is what confirms that.

Establish and document, per shader, what is actually emitted today. This is
the prerequisite the decision asked for: `RGBA16F` must be confirmed to carry
linear values before anything is moved onto it.

Files:
- `shaders/fs_mesh.sc`
- `shaders/fs_vehicle.sc`
- `shaders/fs_white.sc`
- `shaders/fs_present.sc`
- `shaders/fs_hiz.sc`, `shaders/fs_blur.sc`
- `src/render/bgfx_lighting.cpp` (uniform values fed to the above)

Deliverable: a table `shader → output meaning (linear / gamma / other)` and the
same for every texture slot.

Verification: none visual. The table is checked by reading the arithmetic.

Risk: low. Nothing changes.

## Stage 1 — texture semantics

Give each texture the format its data actually has.

- albedo / diffuse / emissive → `RGBA8` + `BGFX_TEXTURE_SRGB`, decoded by the
  hardware on sample and encoded on write;
- normal / roughness / metallic / masks → `RGBA8`, untouched.

Files:
- `src/render/bgfx_lib_backend.cpp` (texture creation, sampler flags)
- the texture cache that loads SCX materials

Verification: A/B screenshots of the same car and garage before and after. Any
change in the paint or the materials means a slot was misclassified; the
classification is corrected, not the shader.

Risk: medium. A wrong classification shifts the whole material appearance.
Kept separate from the shader work so the fault is attributable.

Compatibility: legacy SCX materials are the test case here. If one of them
shifts, it is reported rather than papered over.

## Stage 2 — linear shading, linear targets

Shaders emit linear values into linear targets. The scene colour target becomes
linear `RGBA16F` in fact, not only in format.

Files:
- `shaders/fs_mesh.sc`, `shaders/fs_vehicle.sc`, `shaders/fs_white.sc`
- `src/render/bgfx_lib_backend.cpp` (scene target creation)
- `src/render/bgfx_lighting.cpp`

Verification: a known grey ramp rendered through the pipeline must come out
linear before tone mapping, checked by a debug readout rather than by eye.

Risk: high, and this is the stage most likely to shift the look of everything.
It is split further if needed: meshes first, then vehicles.

## Stage 3 — Present does the single conversion

Present tone maps and encodes once. The `pow(x, 2.2)` decode disappears,
because the input is already linear.

Files:
- `shaders/fs_present.sc`

If the swap chain is sRGB the software encode goes away too and only the tone
mapping stays.

Verification: exposure slider behaviour unchanged at 1.0; the ACES curve is
checked against the reference renders.

Risk: medium. This is where a double encode would show up as washed-out or
crushed blacks, and it is checked specifically for both.

## Stage 4 — cubemaps in linear HDR

Two separate `RGBA16F` resources, both linear:

- **Raw Environment Cubemap** — the capture, source for the filter, never
  written while being read;
- **Prefiltered Environment Cubemap** — the GGX result, its own mip chain,
  `BGFX_ATTACHMENT_NONE` on the filtered levels.

GGX runs only over linear HDR, so it starts only after stages 1–3.

Files:
- `src/render/bgfx_lib_backend.cpp` (capture, cube creation, publication)
- `shaders/fs_prefilter.sc`
- `shaders/fs_vehicle.sc` (sampling)

Verification: the cubemap viewer already built shows the raw faces; the
prefiltered chain is checked level by level against the spread of a known
light.

Risk: high. This is where the earlier reflection work concentrated.

## Stage 5 — legacy SCX compatibility pass

Old materials must render as they did. Any deliberate difference is recorded
and tested.

Files: `shaders/fs_vehicle.sc`, `src/render/bgfx_lighting.cpp`

Verification: side-by-side of a stock car and a modded car, garage and city.

## Order and dependencies

```
0 audit
1 texture semantics      (independent of 2)
2 linear shading          (needs 0)
3 present conversion      (needs 2)
4 cubemaps in linear HDR  (needs 1, 2, 3)
5 legacy compatibility    (needs 2, 3)
```

Each stage is its own commit and is expected to be visually checkable on its
own.

## Functions touched, per stage

| Stage | Function / site | Change |
|---|---|---|
| 0 | — | none, audit only |
| 1 | `bgfx_lib_backend.cpp:2611`, `part.tex[i] = bgfx::createTexture2D(...)` — the SCX material texture loader (formats `BGRA8` for `.dds`, `RGBA8` otherwise, cached in `map_textures_`) | **done**: `BGFX_TEXTURE_SRGB` on colour maps |
| 1 | `bgfx_lighting.cpp`, the sampler binding block around lines 105–146 (`setTexture` for `s_tex_`, `s_lightmap_`, `s_mesh_mix_`, `s_static_shadow_`) | no change expected; it binds whatever the cache hands it |
| 2 | `LibBgfxBackend::draw_instance` (`bgfx_lighting.cpp:177`) | shading already yields linear once the input is; verify rather than rewrite |
| 2 | `bgfx_lib_backend.cpp:597–615`, the scene colour / depth target creation | keep `RGBA16F`, contents become linear |
| 3 | `shaders/fs_present.sc`, `main()` | **done**: the `pow(x, 2.2)` decode is gone, exposure + ACES remain, and the encode is hardware sRGB. Note: a stale comment in `fs_present.sc` still describes the old `pow` path -- comment debt, fix separately |
| 3 | `LibBgfxBackend` present draw (`draw_fullscreen` at the `present_program_` call) | unchanged |
| 4 | `LibBgfxBackend::update_reflections` — cube creation near the `createTextureCube` call, and the face capture loop | two separate `RGBA16F` linear resources |
| 4 | `LibBgfxBackend::destroy_reflections` | release both |
| 4 | `shaders/fs_prefilter.sc`, `shaders/fs_vehicle.sc` | GGX over linear HDR only |
| 5 | `shaders/fs_vehicle.sc`, `LibBgfxBackend::draw_instance` | legacy SCX path kept byte-compatible |

Stage 1 starts from the loader at `bgfx_lib_backend.cpp:2611`. Its two
formats (`BGRA8` for `.dds`, `RGBA8` otherwise) and its `flags = 0` are the
single place where colour maps are currently marked as raw data.

## Out of scope here

The wrong reflections on the right-hand body panels. Their diagnosis continues
separately and is not to be folded into these commits.

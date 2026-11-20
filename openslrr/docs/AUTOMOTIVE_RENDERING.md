# Automotive Rendering 2.0

Target: car paint that reads as car paint. No ray tracing, work inside what
bgfx and plain rasterisation can do, and keep old SLRR models working
untouched.

Project rules:

1. Old SLRR materials keep working.
2. The new PBR is switched on independently of the old system.
3. Automotive materials are not tied to a particular car model.
4. Material parameters are tunable without recompiling shaders.
5. Modders are never required to redo old SCX files.
6. Reflection defects are not hidden by raising roughness.

The technical design (layered paint model, map format, IBL) lives in
`PBR_PLAN.md`. This file is the programme and its evidence.

## Roadmap

| stage | name | status |
|---|---|---|
| 1 | Revision of the existing system | **in progress** |
| 2 | New reflection system (cubemap, mips, GGX prefilter, box projection) | |
| 3 | Base PBR (Cook-Torrance, GGX, Fresnel, Smith, IBL) | |
| 4 | Automotive materials (clearcoat, candy, gloss, matte, satin, chrome) | |
| 5 | Reflection quality on the body (specular AA, curvature) | |
| 6 | Tone mapping and colour (HDR, exposure, ACES) | done in part: HDR + ACES |
| 6b | **Light system** (Point/Spot from game data, light registry, shadows) | separate branch |
| 7 | Material editor | deliberately last |

Stage 7 is last on purpose: a nice editor around mediocre shaders is the
wrong order.

## Roadmap reconciliation

Stage 6 was named "Lighting and colour", which read as if it covered the light
system. It does not: HDR, exposure and tone mapping are the *output* stage, and
Point/Spot lights are an *input* stage. Splitting them matters because they
have different risks, and because `docs/PBR_PLAN.md` treats lighting as art
direction while this file treated it as an output problem.

The agreed shape is two independent branches:

- **Branch A - materials and IBL**: linear pipeline, HDR reflection capture,
  real GGX prefilter, DFG LUT, base PBR, layered automotive paint, legacy SCX
  compatibility.
- **Branch B - dynamic lighting**: Point/Spot restored from game data, a common
  light structure and registry, game-state wiring, integration into the main
  render **and into the reflection capture**, shadows.

Shared contract, to be written before either branch starts: how a light is
represented, where it lives, how it updates, and how it reaches both the main
render and the cubemap capture. If a light is missing from the capture,
reflections and lighting disagree and it looks like a material bug.

Shadow priority is agreed: spot shadows from car headlights first, CSM second.
Existing shadows stay until a full replacement is verified.

Units: point and spot in candela, directional in lux, area in cd/m^2 when it
arrives. One documented global conversion factor is allowed; per-type fudge
factors are not. Ctrl+F12 stays a scene exposure control, not a way to
compensate for badly set lights.

## Acceptance criteria

- **Gloss body** — smooth reflections with no visually broken transitions.
- **Candy** — deep colour and a clearly readable transparent lacquer.
- **Metallic** — metallic character, no plastic look.
- **Matte** — correct light response without mirror behaviour.

Test set: Mazda 3 BK (the problem model), several cars with different
geometry, garage and open world, compared against Marmoset Toolbag and
against Forza on the *character* of the reflections rather than brightness.

---

# Stage 1 — revision of the existing system

Everything below was read back out of the code and the shader sources, not
remembered. Line references are current as of this writing.

## Pipeline as it stands

Scene renders into an HDR target, then the present pass applies ACES plus
the exposure slider. MSAA (off/2x/4x/8x) is on the scene target. View ids:
shadows 0-4, world 5, FMV 6, OSD-under 7, previews 8-63, present 68, OSD 70,
reflection capture 72+, HiZ 110+.

Vehicle shading (`shaders/fs_vehicle.sc`): Blinn-Phong specular from
`u_vehicleSpec`, then the reflection is painted over the lit colour:

```
reflectance = clamp(weight * (0.55 + 0.55 * fres), 0.0, 0.72)
color       = mix(color, environment, reflectance)
```

There is a second, stock path: a spheremap (`s_vehicleEnv`) whose UVs carry
a camera-yaw compensation, used when the live cubemap is not available.

## Reflection capture

Read from `LibBgfxBackend::update_reflections`:

| aspect | current |
|---|---|
| resolution | **512** per face by default (`reflection_face_size()`, clamped 64-2048, `SLRR_REFL_RES` overrides) |
| format | RGBA8 — LDR |
| mip levels | automatic mip chain via `BGFX_ATTACHMENT_AUTO_GEN_MIPS` (plain downsampling, **not** GGX prefiltering) |
| faces per frame | budget from the "Reflection faces per frame" slider |
| probe position | `eye + (0, 0.8, 0)`, Z negated |
| probe tracking | car, or the camera in a showroom |
| the car itself | excluded from the capture |
| spatial correction | box projection, garage only. Bounds come from the garage shell geometry, whose Z is already flipped at `upload_part`, so they land in the shader space; **fallback** to the ground corners when no shell is loaded |

## Problems found

**1. Normal transform has no inverse-transpose — latent, not active.**
`vs_mesh.sc:11` does `v_normal = mul(u_model[0], vec4(a_normal, 0.0)).xyz`
and `vs_vehicle.sc:7` the same through `u_modelView`. That is only correct
for rigid transforms.

Checked whether that holds: `PartPose` carries just `position` and
`rotation` (`part_config.hpp`), `part_pose_matrix` builds pure rotation plus
translation, and `position_scale` is a single scalar (`scx_mesh.hpp:68`,
0.01 for centimetres), not per-axis. So current content is rigid and the
normal transform is **harmless today**. It is a latent trap, not the cause of
the crooked reflections. Anyone who later adds scaled parts inherits silently
wrong normals in both lighting and reflection direction.

Earlier drafts of this file claimed SCX carried a per-axis scale and that
this bug was visible now. That was wrong and is recorded here so the claim
does not come back.

**2. The reflection cube is LDR.** RGBA8 clips a bright window at 1.0, so
the HDR present pass has nothing left to recover. A proper IBL needs an HDR
capture; otherwise the specular energy of the environment is destroyed
before the material ever sees it.

**3. One mip level. -- SUPERSEDED.** The cubemap now carries an automatic mip
chain (`AUTO_GEN_MIPS`), and `envPrefilter()` selects a level with
`textureCubeLod`, mixing two neighbouring levels. Roughness therefore has
somewhere to go, which is the reflection blur seen in game. It remains an
approximation: downsampling is not a GGX convolution, so wide roughness is
still modelled by a level choice rather than by a prefilter. GGX prefiltering
is unimplemented.

**4. The cube lags the camera. -- PARTLY SUPERSEDED.** The default budget is
`reflection_faces = 6` (`debug_overlay.hpp:27`), so the full cube is captured
in a single frame and the lag described here applies only when the slider is
lowered. Publication still happens once per frame, and `SLRR_REFL_INTERVAL`
can throttle it further.

**5. The car is excluded from its own capture.** Bumpers, mirrors, chrome
and panel transitions cannot reflect the car at all.

**6. The reflection is an overlay, not a BRDF.** `mix(color, environment, k)`
with `k` up to 0.72 paints a mirror over an already-lit colour. This is the
main reason the body reads as a mirror glued onto the paint. It must not
survive into the PBR stage.

**7. Specular is discarded whenever a lightmap is present.**
`u_vehicleSpec.rgb * (spec + sheen*0.35) * (1.0 - use)` zeroes the highlight
for any material using the baked lightmap, which is most of them. That alone
makes the paint read matte.

> **L1 correction (HEAD `ee1ad36`).** `use` is `step(0.5, u_lightmap.x)` and
> `draw_instance()` writes `0` into that component for every material except a
> lamp mesh with `light_flags`, because `bind_baked_light()` binds a white
> texture and `draw_instance()` binds `white_tex_` as the bake. So at HEAD the
> factor is `1.0` in practice: the kill is **latent**, not active. It becomes
> real the moment a lightmap is actually bound, which is the design question
> the last paragraph below is about.

**8. Box projection across two spaces. -- HISTORICAL DIAGNOSIS, current status below.**

> The mismatch described here was established against an earlier revision and
> is kept as history. The current code computes `position = mul(u_invView,
> v_viewPos).xyz` in world space before the box test, and the bounds are built
> from the garage shell whose Z is flipped when `upload_part` builds the
> vertex buffers, which is stated in the source to put them in the same space
> as the shader. That is consistency **by construction**; it has not been
> confirmed on screen. Neither "fixed" nor "still broken" is claimed here.

Original diagnosis:

Two places compute a world point from the same geometry bounds and the same
instance transform, and they disagree by a sign:

- `part_centre` (bgfx_lib_backend.cpp:~1119) negates the local Z, applies
  `m`, and then **negates the result's Z**:
  `out[2] = -(m[2]*x + m[6]*y + m[10]*z + m[14])`. That is render space.
- The reflection box (bgfx_lib_backend.cpp:~1612) negates the local Z,
  applies `m`, and **does not** negate the result. That is game space.

The shader then tests `position = mul(u_invView, v_viewPos).xyz` against
those bounds, and `u_reflectionProbe.xyz` is `reflection_origin_`, which is
render space (built as `-eye[2]` from an already-flipped eye). So the point
compared against the box, the probe, and the box itself are not in the same
space; the Z of the box is mirrored relative to the other two.

Consequence: the parallax correction is applied with a mirrored Z, which is
exactly the shape of "reflections do not run across panels". This is the
strongest live suspect in the whole audit and belongs at the top of Stage 2.

Independently of the space bug, the computed direction
`worldRay = position + worldRay * distance - u_reflectionProbe.xyz` was
reported as never normalised. **That is no longer true**: the current
`fs_vehicle.sc` wraps the expression in `normalize()` and carries a comment
explaining that the result feeds `saturate(worldRay.y)` and the prefilter
tangent basis, both of which need a unit direction. Corrected.

**8b. The whole engine carries an undocumented Z-negation convention,**
applied independently at the camera (`-world_eye[2]`), the capture origin
(`-eye[2]`, on an eye that already had `-camera[14]` applied), distance
culling (`-i.transform[14]`), local lights (`-matrix[14]`), mesh centres
(`part_centre`) and the box corners. It needs one documented rule before
anyone extends it, because the convention is applied in many places
  independently and is **not fully audited**.

> **Status.** The claim that "two of those sites already disagree" is an
> earlier diagnosis and is **not re-confirmed here**. What *was* checked for
> this pass: the parallax box bounds are built from `parts_` geometry with Z
> negated at construction (`axis == 2`), matching the Z flip `upload_part`
> applies when building vertex buffers, and the ground fallback does the same;
> and `fs_vehicle.sc` normalises `worldRay` after the correction. That is
> consistency of the box path by construction. The full Z-negation convention
> across camera, capture origin, distance culling, local lights, mesh centres
> and box corners has **not** been traced end to end, so neither agreement nor
> disagreement is asserted for the other sites.

**9. Two reflection sources with different behaviour.** The spheremap path
compensates for camera yaw in its UVs, which makes it inherently
view-dependent; the cubemap path is direction-based. Switching between them
changes how reflections behave.

**10. Varying spaces are inconsistent.** `v_normal` is view space in
`vs_vehicle.sc` and world space in `vs_mesh.sc`. Every consumer has to know
which one it is holding.

**11. Roughness is a heuristic.** Derived as `1 - shininess/24`. There is no
metallic term at all, and no normal, roughness or AO maps — the stock SCX
format has no slots for them.

## Closed during this revision

- **Box space. -- Reopened as unverified.** The original diagnosis (problem 8)
  was against an earlier revision; the current path is self-consistent by
  construction but not confirmed in game. Draw the box before calling it
  fixed.
- **`position_scale`.** Closed: one uniform scalar, not per-axis.
- **Non-uniform scale in content.** Closed: `PartPose` has no scale at all,
  so part transforms are rigid and the normal transform is safe today.
- **Reflection face budget.** Closed: `reflection_faces` defaults to **6**
  (`debug_overlay.hpp:27`), so a full cube is captured **within one frame** at
  the default. Lowering the slider makes it
  worse; there is no setting that captures a whole cube per frame.

## Open questions before Stage 2

- Does the 0.15 near plane in the capture clip anything meaningful inside a
  garage? The capture FOV is 90 degrees at 512 per face.
- Do any stock or modded materials depend on the spheremap path
  (`s_vehicleEnv` with camera-yaw compensation), and can it be retired in
  favour of the cubemap, or must both stay?
- Specular is killed by `(1.0 - use)`. Is that deliberate (the lightmap is
  meant to carry the highlight) or a shortcut? The answer decides whether
  Stage 3 restores the highlight or works around the lightmap.
## How the reflection is applied today, per material kind

`u_vehicleLayers.w` selects one of five unrelated formulas. There is no
single reflection model; each kind mixes or adds the environment its own way.

| kind | `w` | formula | mode |
|---|---|---|---|
| color-mix-env | `> 4.5` | `pane += environment * weight` | add, full weight |
| color-mix | `> 3.5` | no environment at all | — |
| glass | `> 2.5` | `lit*alpha + environment*weight*envK*0.5 + spec*0.5` | add, halved |
| chrome / bare cap | `> 1.5` | `lit + environment * weight + spec` | add, full weight |
| paint | `> 0.5` | `mix(color, environment, clamp(weight*strength, 0, 0.72))` | **mix** |
| bare metal | else | `color += environment * weight * 0.5` | add, halved |

`envK` is `0.4` for a headlight lens and `1 + fres*0.5` for cabin glass.
`strength` is either the legacy `0.55 + 0.55*fres` or
`envBase + (1 - envBase)*fresR`, and `fres = pow(1 - NoV, 3)`.

Two things stand out. Paint is the only kind that *mixes*, and it is the one
that reads as a mirror glued to the body. And the paint comment says the
authored weight "stands in for the clearcoat the stock model never had" —
so one scalar is currently playing the role of the entire lacquer layer.
Stage 4 replaces it with the real thing.

## Normals in the fragment shaders

`fs_vehicle.sc` takes `n = normalize(v_normal)`, which arrives in **view**
space. It feeds: the diffuse term (`ndl`), the Blinn half vector (`ndh`) for
`spec = pow(ndh, shininess) * ndl`, the `sheen` lobe, `fres`, `fresR`, and
`reflect(normalize(v_viewPos), n)` for the reflection direction.

`fs_mesh.sc` does the same in **world** space. There is no normal mapping
anywhere, and no tangent basis is built or passed, which Stage 4 will need.

## Speculative changes sitting in the working tree

The reflection path also carries changes made during this session, before
this audit, aimed at "reflections gathering in corners" — which the Marmoset
comparison has since shown to be correct behaviour. These are candidates for
removal at the start of Stage 2 so the new work starts from a clean baseline:

- `occ` — Lagarde specular occlusion driven by the baked lightmap luminance.
- `fresR` — fresnelSchlickRoughness, and `envBase`, a tunable reflectance
  floor replacing the constant `0.55`.
- `envPrefilter` — a 13-tap cone over the cubemap, widened by the
  screen-space variance of the normal (specular AA).
- a switchable geometric normal for the reflection direction. Its "Geometric"
  and "Blend" modes were tried and rejected: the geometric normal is flat per
  triangle and facets the shading.

Keeping `envPrefilter` is defensible — a roughness-driven prefilter is
required anyway by Stage 2. The occlusion and the reflectance reshaping are
not, because they were compensating for a non-problem.

## The questions that were left open, now closed

**Probe orientation: correct.** `bx::mtxLookAt` defaults to
`Handedness::Left` (bx/math.h:1895). The face table in `update_reflections`
-- `up = (0,0,-1)` for +Y, `(0,0,1)` for -Y, `(0,1,0)` elsewhere -- is the
classic left-handed D3D cubemap table, and the `axes[]` order matches the
D3D face order that `destination.z = face` writes into. `reverse_scene_projection`
only remaps depth, it does not touch handedness. So the capture orientation
is self-consistent.

**The specular kill `(1.0 - use)` is deliberate, not a shortcut.** With a
lightmap present the shader sets `illum` to white
(`illum = mix(illum, vec3(1), use)`) and puts all the lighting into `bake`,
so `color = albedo * bake`.

> **L1 note:** no lightmap is present today (`use = 0`, see correction 7), so
> this paragraph describes the branch, not the current image. The decision it
> frames still has to be taken before that branch is turned on.

Adding a runtime highlight on top would double-count it. The consequence is
still real though: a baked highlight is static, so it cannot follow the camera
or the light, and the paint reads matte from most angles. Stage 3 has a decision
to make here -- whether the lightmap should carry only ambient/AO and leave the
specular to the runtime BRDF -- and it is a design decision, not a bug fix.

**The spheremap path cannot simply be retired.** `enhanced_env` requires
`reflection_scene_`, which is false for menus and previews. Those fall back
to the material's own reflection texture through `s_vehicleEnv`. Removing it
would leave menus with no reflection at all, so it must stay as the fallback
or the cubemap must become available there too.

**Capture near plane 0.15 is low risk.** Scene units are metres (SCX
positions are centimetres scaled by 0.01), so 0.15 m is small against a
garage. Not the cause of anything visible; the resolution and the LDR format
matter far more.

## Problem 12: the pipeline is gamma-space end to end

> **Status: fixed.** This records the state found by the audit at the time and
> is kept as history. Since then stage 1+3 landed: scene colour maps carry
> `BGFX_TEXTURE_SRGB`, and `fs_present.sc` no longer does any software gamma
> conversion -- the window is an sRGB backbuffer and the encode is in hardware.
> Interface art deliberately does **not** use the flag. See
> `RENDERING_ARCHITECTURE.md` sections 2 and 4.

This was listed as a prerequisite in `PBR_PLAN.md` but had not actually been
audited. It has been now, and it is wrong in a way that will poison Stage 3.

- **No texture is uploaded as sRGB.** There is no `BGFX_TEXTURE_SRGB` flag
  anywhere in the renderer, so diffuse maps are sampled as raw
  (gamma-encoded) values.
- **Lighting is therefore done on gamma values.** Adding ambient and diffuse,
  evaluating the specular lobe and mixing the reflection all happen in
  gamma space, which is not what any of those formulas assume.
- **The present pass then decodes the accumulated result** with
  `pow(color, 2.2)`, runs ACES, and re-encodes with `pow(color, 1/2.2)`
  (`fs_present.sc:28-31`).

So there is a decode at the very end, but it is applied after the lighting
math rather than before it. It partially masks the problem and does not fix
it. The `pow(x, 2.2)` is also an approximation -- the real sRGB transfer has
a linear segment near black.

The scene target is `RGBA16F`, which is linear-capable, so the fix is
available: mark the colour maps sRGB so sampling delivers linear values,
keep the render target linear, and encode only in the present pass. That has
to happen before Stage 3, because PBR energy conservation assumes linear
radiance throughout.

The reflection cube being `RGBA8` LDR compounds this -- it cannot hold the
bright end at all, so the environment's specular energy is lost before the
material sees it.

## Stage 1 completeness check

Against the checklist this stage was supposed to cover:

| item | state |
|---|---|
| algorithm behind the dynamic cubemap | done |
| how often the six faces update | done -- 6 per frame by default (`debug_overlay.hpp:27`) |
| reflection probe position | done |
| reflection probe orientation | done -- verified, correct |
| box projection | **unverified** -- position and bounds are consistent by code construction and `worldRay` is normalised, but no visual check has been done. Not confirmed fixed, not confirmed broken |
| reflection direction transforms | done |
| normals in vertex and fragment shaders | done |
| how the reflection coefficient is computed | done -- five unrelated formulas |
| limits of the current material system | done |
| sRGB / linearity of the pipeline | done -- added, it was a real gap |

Nothing else outstanding for this stage. What is *not* covered here and
belongs to later stages: shadow map setup, the present pass beyond its
tonemap, the HiZ path, and anything to do with physics or the OSD.

---

# Roadmap, agreed

Agreed at source revision `42d795e`. This replaces the informal ordering
earlier in the document where they disagree.

## The shared data contract

Written **before** any branch starts. Fixed contract between the game resource
system, SCX and its parser, the internal material model, the renderer,
reflection capture and the SCX Exporter.

The hard requirement: **a light must reach the cubemap capture as well as the
main render**, or reflections and lighting disagree and no later tuning can
fix it.

## Branch A — materials and IBL

- paint descriptor
- basecoat / clearcoat, energy-conserving layer interaction
- BRDF
- GGX prefilter
- DFG LUT
- then generic PBR for static geometry

Paint kinds required: Gloss, Candy, Metallic, Matte, Satin, Chrome. The model
is the one in `PBR_PLAN.md`; `mix(color, environment, k)` does not come back.

GGX prefilter and the DFG LUT are **separate, explicitly named tasks**. They
are not implemented and must not be described as if they were.

The paint descriptor and BRDF do not have to wait for the whole IBL system.
Develop them in parallel on a reproducible test scene; judge the final
reflections only once a correct prefilter exists.

## Branch B — lighting

Reframe: **restore and extend the existing SLRR light model**, not invent one.

Game resources already carry Point and Spot lights whose parameters the backend
drops. The old resource format stays compatible; the new renderer takes a
common light structure through an adapter.

Required types: Point, Spot, Directional. Parameters to keep and use: position,
direction, diffuse/ambient/specular where applicable, range, attenuation,
theta/phi, attH_texture/attH/attV_texture, lit_dynamic.

Scope of the first cut: correct direct lighting, attenuation, cones and on/off
states. Projection textures and flare/glow are separate follow-ups.

- common light registry
- restore Point/Spot
- Directional
- lamp states
- shadows (spot from headlights first, CSM second)
- identical lights in the main render and reflection capture

## Shared late stage — SCX data pipeline

Fix the material schema, then extend SCX, the parser and the SCX Exporter, then
verify compatibility and round-trip. Specification and acceptance criteria:
`SCX_MATERIAL_FORMAT.md`. Separate checkable steps, never mixed into Branch A or
B commits.

## Units and calibration

Point / Spot in cd, Directional in lx, Area in cd/m^2 (future).

Old D3D9 values must **not** be assumed to already be in those units. First
establish their actual semantics, distance units and world scale; then design a
documented conversion into the photometric system. No per-light arbitrary
factors. Ctrl+F12 stays the single global exposure control.

## Vehicle headlights

The main scenario. Bind the existing vehicle lamp state to a real Spot light
rather than inventing independent headlight logic.

To check: the link between `part_lamp_lit()` and `lit_dynamic`; low vs high
beam if the game data distinguishes them; brake lights and indicators;
projection textures for the beam; updating the light source when the light
switches or the vehicle state changes.

Emissive lamps, direct road lighting and bloom stay three separate things.

## Reflection capture

Unchanged hard requirement: dynamic lights and their states are accounted for
identically in the main render and the reflection capture.

## Roadmap stages, restated

Stage 6 of the older roadmap is tone mapping / colour. **Stage 6b is the light
system.** Stage 6 is not a dynamic lighting system and must not be read as one.

## Order of work

1. Update documentation and show the diff.
2. Fix the internal material model and the interfaces between subsystems.
3. Agree the updated roadmap.
4. Branch A: paint descriptor and the base material model, IBL prepared in
   parallel.
5. Branch B separately: light registry, then restored Point/Spot.
6. Once the material model is checked and stable, freeze the SCX spec.
7. Implement SCX, parser and exporter extensions as separate verifiable steps.
8. Finish with testing of old and new models and a full round-trip.

No mass implementation before the documents, contracts and per-stage acceptance
criteria are agreed.

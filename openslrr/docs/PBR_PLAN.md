# PBR plan (metallic / roughness / AO / normal)

Goal: standard PBR (Cook-Torrance, metallic-roughness) like modern games,
integrated natively into the engine, with automatic fallback so unmodded
content keeps working.

## What must happen first (prerequisites)

The frame itself also has to change shape before PBR is affordable: the city is
currently rendered more than once per frame and every car is submitted as ~23
separate draws. Probes, material sorting and parallel encoding are planned in
[RENDER_FOUNDATION_PLAN.md](RENDER_FOUNDATION_PLAN.md), which also records the
measured frame budget those stages are meant to move.

1. **GFX-1: present program link failure** (see BACKLOG.md).
   The dead present path blocks the offscreen scene target, and the scene
   target is the backbone of everything PBR needs:
   - HDR rendering (RGBA16F target instead of LDR backbuffer)
   - post chain: tonemap (ACES), bloom, optional FXAA (MSAA stays for
     geometry)
   The HiZ program failing to link is the same family of bugs.
2. **sRGB audit.** PBR math is linear; today the pipeline is gamma-space
   end to end. Decide: sRGB-flagged diffuse textures + linear render
   target + sRGB conversion in the tonemap. Audit the existing texture
   upload path (DDS BC1/2/3 formats and their sRGB variants) and the sky /
   lightmap colors.

## Asset strategy (the game has no PBR textures)

Stock materials carry: diffuse, specular + shininess, mix, lightmap,
illumination, reflection index. Mapping to PBR:

- **roughness** = 1 - glossiness; glossiness derived from the specular map
  luminance and the material's shininess parameter.
- **metallic** = heuristic at first (0 for everything, 1 for known
  chrome/mirror material types), then per-material overrides.
- **AO** = the existing lightmaps are effectively AO/bake data; also accept
  a real `_ao` texture when provided.
- **normal maps** = nothing to derive from; start flat. The real source is
  mods (see below).

**Mod path (the real long-term source of PBR textures):** allow mods to
drop `*_metal.dds`, `*_rough.dds`, `*_normal.dds`, `*_ao.dds` next to the
diffuse (same stem, name-suffix convention like the Localization mod uses).
`load_part_textures` binds them when present; the PBR shader takes the PBR
branch; without them the material runs the derived heuristics or the legacy
path.

## Renderer integration (native, not bolted on)

- `fs_mesh.sc`: add the PBR branch (GGX distribution, Smith geometry,
  Schlick fresnel, diffuse = albedo*(1-metallic)). Material selects the
  branch via a uniform (per-material PBR flag), keeping the legacy path for
  A/B and fallback. Same for `fs_vehicle.sc`.
- **IBL**: we already capture a reflection cubemap at the eye position.
  Build an irradiance map (downsampled/blurred cube) + prefiltered
  specular chain from it each capture cycle; sample in the PBR branch.
  The sun is already a directional light with shadow maps.
- **Tonemap + bloom** in the present pass (unblocked by GFX-1). Tone
  mapping also fixes the blown-out highlights the legacy pipeline has.
- **Perf budget:** GGX+IBL at 1440p is ALU-bound; estimate +1-2 ms GPU on
  the 4070 Ti. HDR target doubles scene-target bandwidth. Bench before
  enabling by default; keep a `SLRR_NO_PBR`-style A/B switch during rollout.

## What we learned before touching the shader again

**Reflections gathering in a concave point is correct behaviour, not a
bug.** Verified against ground truth: the same Mazda 3 body rendered in
Marmoset on an HDRI gathers the reflection in exactly the same spot under
the rear light. Everything spent trying to stop it -- softening the lookup,
flattening the weight, damping the corners -- was fighting physics and had
to be backed out. Do not suppress it.

The first PBR attempt looked very bad for specific, fixable reasons:

1. **Cross-content lerps.** Roughness walked between a sharp captured cube
   and a separate irradiance cube that was a *different render* with
   different content. Blending two different pictures is what "mush" is.
   Rule: every roughness level must be derived from ONE capture.
2. **No real blur.** The sharp cube was sampled through low-poly normals, so
   roughness did nothing and the detail just smeared into streaks. Rule:
   roughness must actually widen the lookup.
3. **Discontinuous levels.** Specular AA pushed neighbouring pixels into
   non-adjacent levels, so the transition was visible. Rule: levels form one
   chain and a pixel may only ever mix two *adjacent* ones.
4. **Position-dependent lookup.** The box parallax makes the sampled
   direction depend on world position, so the reflection does not run
   continuously across panels. Rule: lookup is direction-only, like a distant
   HDRI. Real SSR is a separate later stage if wanted.

## Material model: layered paint, not one generic MR material

The target is **car paint that reads as car paint**, even where the
reflection is strongly compressed by the body shape. That is a different
aim from hiding the compression -- roughness only decides how blurred the
reflection is, it never changes its spatial shape.

Two independent reflecting layers, with **separate** roughness /
reflectance / intensity / normal, combined energy-conservingly (Fresnel:
some light leaves the clearcoat, the rest reaches the basecoat and comes
back through it):

| layer | role | parameters |
|---|---|---|
| **basecoat** | colour, pigment, metallic flake, pearl | baseColor, metallic, roughness, normal, ao |
| **clearcoat** | transparent lacquer, dielectric | roughness, reflectance/IOR, intensity, optional normal |

Finish types built from those two layers:

| type | how |
|---|---|
| Gloss | plain base + clear lacquer |
| Candy | translucent colour layer over a metallic base + clear lacquer |
| Metallic | flake pigment + lacquer |
| Matte | high basecoat roughness, weak or no lacquer |
| Satin | between matte and gloss |
| Chrome | metallic, very high reflectance |

The generic map set a non-paint surface uses stays classic
metallic-roughness: `baseColor`, `normal`, `metallic`, `roughness`, `ao`,
with `F0 = mix(vec3(0.04), baseColor, metallic)` and
`diffuse = baseColor * (1 - metallic)`.

**The current paint blend must not survive into PBR.** Today
`color = mix(color, environment, reflectance)` with the coefficient reaching
0.72 paints a mirror image over an already-lit colour. That is an artistic
overlay, not a lacquer model. The layered BRDF replaces it.

## Material selection: key it on the paint texture id

The engine already identifies the paint unambiguously: the cfg names it as
`texture 0x<id>`, and the game can swap it at runtime (select car already
does). We track both `paint_source` (the id the cfg names) and
`paint_texture` (the id currently applied), plus the resolved texture name.

So a PBR set attaches to a **paint texture id**, not to a file stem and not
to a material group index. It follows the paint through repainting and
through the car-selection swap, and it survives SCX groups being
renumbered. Stock SCX has no metallic / roughness / normal / AO slots, so
the set ships as a sidecar next to the car data:

```ini
; cars/racers/Maz3_2005_data/paint.pbr
paint_id       = 0x00030022    ; or the resolved texture name
type           = gloss         ; gloss | candy | metallic | matte | satin | chrome
base           = body_base.dds
normal         = body_n.dds
metallic       = body_m.dds
roughness      = body_r.dds
ao             = body_ao.dds
clear_roughness = 0.08
clear_ior       = 1.5
clear_intensity = 1.0
metallic_scale  = 0.0
roughness_scale = 0.45
```

- With no descriptor the material keeps the legacy path exactly. That is
  the fallback contract for unmodded content.
- Scalars allow one map set to drive several finishes.
- The F12 panel overrides the scalars live and reports which descriptor a
  material resolved to, so a misnamed file is visible instead of silently
  ignored.
- A name-suffix convention (`body_n.dds`, `body_m.dds`, `body_r.dds`,
  `body_ao.dds`) is accepted as shorthand when there is no descriptor.

## Environment

Current capture, as a reminder: 256² per face, **1 mip level**, six faces
taken over several frames, probe at the car or the camera, the car itself
excluded from the capture, box projection in the garage.

1. **Roughness chain.** Give the cube GGX-prefiltered mip levels from that
   single capture, so roughness has somewhere to go. This does not change
   the geometric compression of a reflection, it just makes the PBR
   response correct.
2. **Self-reflections.** The car is excluded from its own capture, so its
   own details cannot show in the body. That matters most for bumpers,
   mirrors, chrome and panel transitions.
3. **Capture rate.** Six faces over several frames smears the cube in time;
   capture a whole cube per frame when the budget allows.
4. **SSR later**, falling back to the cubemap; hardware ray tracing after
   that.

Caveat to keep in mind: if Marmoset shows the same compression while
tracing real rays, no amount of cubemap work changes the shape. Then it is
the reflecting surface's behaviour and belongs in the BRDF, not in moving
the probe.

## Principle: no forced normal correction

Do not add a normal correction that changes reflection shapes on every car
-- it will break the models whose reflections are already right. Build the
full paint model first, then judge whether any artistic correction is
needed at all.

## Order of work

Done: HDR scene target + ACES tonemap (steps 1-2).

1. **Paint descriptor + map loading**, keyed on the paint texture id. Nothing
   else can be judged until a body can carry its own maps and finish type.
2. **Layered paint BRDF** in `fs_vehicle.sc`: basecoat + clearcoat with their
   own roughness, energy-conserving combination, normal mapping, AO on the
   indirect only. The `mix(color, environment, k)` overlay goes away. Legacy
   path stays byte-identical for materials without a descriptor.
3. **IBL from one capture**: GGX-prefiltered roughness chain from that single
   capture, plus the DFG LUT. No cross-content mixing, adjacent levels only.
4. Self-reflections of the car in the capture, and a whole cube per frame.
5. Generic metallic-roughness branch for static world materials in
   `fs_mesh.sc`.
6. Bloom; SSR as a separate later stage.

SSR and ray tracing are deliberately out of scope here; they are separate
stages and must not be mixed into the material work.

Carrying the reflection overlay into PBR is the one thing this stage must
not do -- it is what made the body read as a mirror painted over the paint.## Stage 2.2 evidence: why the prefilter chain is required

Measured on the Mazda 3 at 512-per-face cube. Widening the lookup on a
*sharp* cube produces fine scratch-like streaks that follow the body
geometry -- at high spread they read as a "recursion" texture.

The cause is undersampling, not the environment content. A 24-tap disc at a
useful blur width spaces its taps about 16 cube texels apart, and the cube
holds high-frequency content (window strips, shelving). A sparse average of
a sharp signal aliases into exactly those threads. The tap spacing can never
be made finer than roughly one texel without an impractical tap count, so no
number of taps fixes this.

Therefore every blur level has to come from a **pre-blurred** version of the
one capture -- which is precisely the plan's requirement that all levels
derive from a single capture through GGX prefiltering. Roughness then
selects a level instead of widening a runtime cone.

Consequence for the pipeline: the cube needs a real mip chain (512/256/128/
64/32), each level filtered from the level below, and the material lookup
becomes a `textureCubeLod` selection driven by roughness. The runtime cone
in `fs_vehicle.sc` is a stand-in until that lands and is not the final
shape.



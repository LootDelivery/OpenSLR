# LIGHTING AUDIT

> **Revision note.** This document predates the rendering audit (`9b9513f`) and
> was re-checked against **HEAD `42d795e`**. The statements below were verified
> against source at that revision. Where a claim rests on a code comment rather
> than on observed resource contents, that is now marked explicitly.
# Lighting audit — current state

Read-only audit, part 1 of the lighting design. No code was changed.
Everything below is from the tree at `488144a`..`994b13e` (local work, not on
`origin`).

## 1. Current state

### What actually lights a surface today

Two mechanisms, and only two.

**A single directional light** — `SceneSun`, resolved by
`scene_instances_light()`. Its direction comes from the light resource file;
its colour and intensity are overwritten by `setLight` (the time-of-day type),
so the file is effectively direction-only.

**One local light** — `update_local_light()`:
- gated on `garage_` and `host_enhanced_graphics().local_lights`;
- scans `gameref_directional_lights()` for a node of type `misc.garage:0x001D`;
- hardcodes an emitter 3.3 units above the node, intensity 8, radius 8;
- takes the **first** match and stops.

So the garage is lit by one hardcoded point above one hardcoded node. That is
why changing the garage lighting has meant patching this one function.

### Point and spot lights: present in data, dropped in code

This is the most important finding.

The light resource format is text. `apply_directional_light()`
(`natives_renderref.cpp:99`) parses colour, direction, diffuse, ambient and
specular, and its own comment says:

> Point lights are left alone.

So the original resources **do describe point lights**. They are loaded into
`g_lights`, and then `gameref_directional_lights()` filters them out:

```cpp
if (node && ... && node->directional_light.set) out.push_back(node);
```

Nothing surfaces them. There is no `gameref_point_lights()` at all. This is not
a data limitation — it is a backend limitation, and the plan's instruction not
to assume point lights are absent from the resources is confirmed correct.

### Shaders

`u_localLight` reaches `fs_mesh.sc` and `fs_vehicle.sc` through
`bind_baked_light()`. That call sets a zero local light and a zero sky unless
the caller passes them. Vehicles and world meshes therefore share the uniform
but not the surrounding model: `fs_vehicle.sc` adds environment reflection and
material layers that `fs_mesh.sc` has no equivalent of. Unifying them is a real
task, not a rename.

### Baked lighting: parsed, then discarded

`bind_baked_light()` sets a **white texture** where the lightmap should be, and
the texture list passed to it is ignored. The SCX parser recognises lightmap
and illumination fields; they do not reach the GPU in any meaningful way.

So the baked data is available and unused. Whether it is worth keeping as AO is
an open question, but it is not currently doing anything.

### Shadows

- a static shadow map for the world (views 0-4);
- a garage shadow map;
- vehicle depth and projected shadow decals;
- shadow rendering has its own handling for alpha-cutout and transparent
  groups.

These work and are in use. The limitation for multiple lights is structural:
the shadow passes are built for one sun frustum.

### HDR and reflections

Implemented and working: linear pipeline, RGBA16F scene target, ACES with a
single exposure control, dynamic cubemap capture with generation-based
publication, box projection for interiors, and interface drawn display-referred
outside the scene.

**Not implemented**: the GGX prefilter. It is designed as stage 4 of the linear
migration and does not exist yet. The cubemap has auto-generated mips, which is
plain downsampling, not a GGX chain. Any plan that says "use the existing
prefilter" is counting work that has not been done.

## 2. Consequences for the design

1. **Point and spot lights are the right next step, not a luxury.** The data
   already exists; the backend throws it away. Picking it up is closer to
   restoration than to invention.
2. **`update_local_light()` must go**, and its replacement must not special-case
   `0x001D`. But the garage look people like is a product of that hack, so the
   replacement has to reproduce it before it improves on it.
3. **The prefilter is on the critical path** for any specular claim. Budget it.
4. **Light must be added to the reflection capture**, or reflections and
   lighting will disagree. This has to be a hard requirement, not a note.
5. **`fs_mesh.sc` and `fs_vehicle.sc` are not one model today.** Unifying them
   needs its own step, not a side effect of adding lights.
6. **Baked lightmaps are dead weight right now.** Cheap win if used as AO;
   costly if mistaken for GI.

## 3. What I could not determine from code alone

- Which original resources actually contain point lights, and how many. Needs
  a scan of the game data.
- Whether the spot-light shape exists in the data at all.
- What the garage look depends on specifically: the position offset (+3.3), the
  radius (8), or the intensity (8). These are hardcoded and unlabelled.
- Whether `light_flags` and `part_lamp_lit()` carry real lamp state or are
  vestigial.

These four are the questions the next pass should answer, and they are cheap.

---

# Game data scan

Read-only scan of `Street Legal Racing - Redline` resources.

## Confirmed by game resources

All three light types exist in the shipped data. The format is D3D-style
(`D3DLIGHT9`), text, and far richer than what the backend consumes.

A spot light from `cars/racers.rpk` -- a car headlight:

```
type spot
diffuse  a r g b
specular a r g b
ambient  a r g b
position x y z
direction x y z
range 250.000
attenuation 1.000 0.030 0.000     ; constant, linear, quadratic
theta 1.896                       ; inner cone
phi 2.020                         ; outer cone
lf_texture 0x00000100
lf_glow_color a r g b
lit_dynamic 1.000
attH_texture 0x0000000B           ; horizontal projection gobo
attV_texture 0x0000000C           ; vertical projection gobo
```

A point light from `objects.rpk`:

```
type point
diffuse  a r g b
specular a r g b
ambient  a r g b
position x y z
direction x y z
range 500.000
attenuation 1.000 0.030 0.000
lf_texture 0x000004D2
lf_glow_color a r g b
lf_glow_colortype 2.000
lf_glow_offset 4.000
lf_glow_minsize 2.000
lf_glow_maxsize 40.000
lf_ray_minwidth 2.000
lf_ray_maxwidth 2.000
lf_ray_minlenght 1.000
lf_ray_maxlenght 70.000
lf_flare_minsi...
```

Directional lights carry the same fields with `position`, `range` and
`attenuation` present but zeroed -- the format is uniform across types.

### Where they live

| Resource | Light types found |
|---|---|
| `cars/racers.rpk` | `type spot` -- car headlights |
| `parts.rpk` | `type spot` |
| `particles.rpk` | `type spot` |
| `objects.rpk` | `type point` -- lamps and street lights |
| `frontend.rpk`, `maps/city.rpk`, `maps/city2.rpk` | `type directional` |

### Fields the backend ignores entirely

- `theta`, `phi` -- cone angles, so spot shape is already authored;
- `attenuation` -- the constant/linear/quadratic triple;
- `range` -- parsed nowhere;
- `position` -- only used by the garage hack;
- `attH_texture`, `attV_texture` -- projection gobos for the light cone;
- `lf_*` -- the whole lens-flare block (glow colour and size, ray width and
  length, flare size);
- `lit_dynamic` -- the lamp on/off flag.

## Confirmed by code

- `apply_directional_light()` parses `type`, `diffuse`, `ambient`, `specular`
  and `direction` only. Everything above is read past.
- Its comment "Point lights are left alone" refers to this: non-directional
  resources are skipped, not understood.
- `gameref_directional_lights()` filters on `directional_light.set`, so even
  loaded point/spot nodes are unreachable from the renderer.
- `part_lamp_lit()` decides whether a lamp mesh is **visible**, from flags, the
  engine and time. It never creates a light source. `lit_dynamic` is the data
  side of the same idea and is unused.
- `update_local_light()` picks the first `0x001D` node, offsets it +3.3 in Y,
  and hardcodes intensity 8 and radius 8. The shader then multiplies by 3.0 and
  applies a warm tint. Those two constants are the entire garage lighting.

## Still unknown

- How many spot and point resources exist per map and per car -- the scan found
  types, not counts.
- Whether `theta`/`phi` are half-angles or full angles in this data.
- Units of `range` and `attenuation` relative to world scale.
- Whether `attH_texture`/`attV_texture` are meaningful for most lights or only
  for a few authored ones.
- What the old lightmaps contain, and whether any of it is worth keeping as AO.

## Consequence for the roadmap

The "restore Point/Spot from game data" step is smaller than it looked: the
data is complete and already parsed for the directional case. What is missing
is a general light structure, an accessor that does not filter by type, and a
shader loop. Cone angles and attenuation are already authored and simply
waiting.

The lens-flare block is a separate feature and should not be on the critical
path for lighting.

---

## Framing of the work this audit supports

Agreed at source revision `42d795e`.

The task is **restore and extend the existing SLRR light model**, not build a
new one from nothing. The game data already carries Point and Spot lights with
full parameters; the backend drops them. The old resource format stays
compatible and the new renderer takes a common light structure through an
adapter.

Required types: Point, Spot, Directional. Parameters to preserve and use:
position, direction, diffuse/ambient/specular where applicable, range,
attenuation, theta/phi, attH_texture/attV_texture, lit_dynamic.

First cut is direct lighting, attenuation, cones and on/off states. Projection
textures and flare/glow are separate follow-ups and are not in scope initially.

### Vehicle headlights

The primary scenario. Bind the existing vehicle lamp state to a real Spot
light; do not create independent headlight logic when the game states already
exist.

Check explicitly:

- the link between `part_lamp_lit()` and `lit_dynamic`;
- low vs high beam, if the game data distinguishes them;
- brake lights and indicators;
- projection textures for the beam;
- updating the source when the light switches or vehicle state changes.

Emissive lamps, direct road lighting and bloom are three separate things.

### Units

Point / Spot in cd, Directional in lx, Area in cd/m^2. Old D3D9 values must not
be assumed to be in those units: establish their actual semantics, distance
units and world scale first, then define a documented conversion. No per-light
arbitrary multipliers. Ctrl+F12 remains the single global exposure control.

### Reflection capture

Dynamic lights and their states must be accounted for identically in the main
render and in the reflection capture.

### Storage of the new materials

Material storage, parser and exporter requirements are in
`SCX_MATERIAL_FORMAT.md`. Note that Point/Spot also need somewhere to live in
the extended SCX; that is part of the same format work and is not settled yet.

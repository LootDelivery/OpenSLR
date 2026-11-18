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

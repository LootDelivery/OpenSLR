# Render data contract

Status: **architecture proposal**. No code, no binary layout, no final C++
types. Field lists are conceptual and are frozen only after the material model
and BRDF are validated. Where materials are concerned, this contract follows the
accepted decisions A1–A3 in `MATERIAL_SYSTEM_ARCHITECTURE.md`.

Scope: the internal material model and the interfaces between the resource
system, SCX and its parser, the Material System, the renderer, reflection
capture and the SCX Exporter.

## 1. Responsibility split

| Owner | Holds |
|---|---|
| **SCX v5** | geometry, Material Slots (`slot_id`, `role`, `material_ref`), Material Binding (group → `slot_id`), **author model-owned Material Definitions** |
| **RPK / OpenSLRR resources** | the typed **Paint Definition library** (own OpenSLRR resource type, not SCX, not `CARCOLORS`) |
| **Game resource system** (RPK / RenderRef) | light definitions and their placement in the object hierarchy |
| **Vehicle state** | optional Material Assignment (`slot_id` → Paint Definition), Layer Stack (paintjob, decals) |
| **Runtime Light System** | active light instances with world transforms and live state |
| **Renderer** | receives geometry, resolved materials and lighting as independent data |

The paint library and a model's own materials have different owners: a model
never carries a copy of the library, and the library never carries a copy of a
model's surface material.

Point/Spot are **not** added to SCX for the sake of material extension. A future
light export from a 3D editor is a resource-system or placement-format task, not
part of SCX material slots.

## 2. Combined material system

Three different things, deliberately kept apart.

**A model's own materials live in SCX v5.** Plastic, seat leather, rubber,
glass, plain metal — and also the authored surface material of a Paint Slot.
Each is a complete model-owned Material Definition: an arbitrary author name and
its own PBR parameters and maps. It does not have to exist in the game library,
and it is never auto-linked to it by material name, texture name, map set, RPK
id or matching parameters.

**Automotive paint is a library entity.** A Paint Definition is a Material
Definition with `shading_model = AutomotivePaint` (Gloss, Candy, Metallic, Matte,
Satin, Chrome-as-paint). It exists independently of any one car and is stored as
a typed OpenSLRR resource inside RPK, so it can be reused by many models and
slots and extended by mods.

**Choosing a paint is an instance assignment, not a material replacement.** The
user picks a Paint Definition for a Paint Slot; the choice is stored in vehicle
state as a Material Assignment addressed by `slot_id` and held as a permanent
reference, not a runtime id. It does **not** overwrite, delete or replace the
slot's authored `material_ref`.

Both the authored surface material and the assigned paint remain available to
the Material System. How a user paint is combined with the author material when
`ResolvedMaterial` is produced is a rendering question and is **not defined**
here; this contract only fixes storage, ownership and identity.

A single internal representation for the renderer does **not** mean every
material has the same storage home.

## 3. MaterialSlot

A Material Slot is declared by the model in SCX v5 and bound to one or more
geometry groups. Every slot, including `role = Paint`, carries:

- a stable own `slot_id` — not a group index, not a texture index, not derived
  from `role` or from the material id; it survives group reordering and
  geometry edits;
- a semantic `role` (`Ordinary`, `Paint`, `Glass`, `Emissive`, `Trim`, ...);
- a mandatory `material_ref` to the **model's own** Material Definition.

Material Binding connects a geometry group to a `slot_id` and is the **sole
source of truth** for that link; the list of groups of a slot is derived from
binding and is not an independently stored field. A group belongs to exactly one
slot; several groups may belong to the same slot.

For `role = Paint` an optional Material Assignment from the RPK paint library is
additionally allowed (section 5). There is no second, material-less slot kind: a
Paint Slot keeps its authored material and gains an optional assignment on top
of it.

The author marks a paintable surface in 3ds Max with the reserved material name
`SLRR_Paint_<SlotName>`. The exporter recognises the reserved `SLRR_Paint_`
prefix and writes an explicit `role = Paint` into SCX v5; `<SlotName>` is an
arbitrary author name — not an enum entry, not a registration, not a `slot_id` —
and runtime does not parse the original string. An ordinary material name, or
the mere presence of a PBR map set, does not make a surface paintable.

Allow several independent paint slots if real content needs them (primary and
secondary coating). Do not assume every car has the same slot set.

## 4. Ordinary materials in SCX

Generic PBR plus a legacy fallback. Preliminary fields: material id, shading
model, baseColor, metallic, roughness, normal, AO, emissive, alpha/transparency,
texture slots, UV channel and UV transform where needed.

Assigned to a geometry group directly or through a MaterialSlot. Editable
through the SCX Exporter later.

No global library per plastic/rubber/leather variant — these are defined in the
model. The architecture must not forbid external material reuse later, but a
universal external library is not to be built without a demonstrated need.

The authored Material Definition of a `role = Paint` slot is also a full
model-owned PBR material, not a placeholder: author PBR materials are not
limited to Ordinary slots. The paint library is the only shared material library
in this contract, and it holds AutomotivePaint definitions only — it is not a
general warehouse for plastic, rubber or leather.

## 5. Paint material

**5.1. Assignment.** Conceptually

`VehicleInstance → MaterialAssignment(slot_id) → PaintDefinition (RPK library)`

e.g. `Mazda3_Instance.MaterialAssignment[body] = Candy_Red`. Repainting changes
the chosen Paint Definition; geometry, SCX, the slot's authored `material_ref`
and the car's other materials do not change.

Requirements: addressed by `slot_id`; persisted in vehicle state; restored when
a saved car loads and resolved against the current resource set; holds a
permanent reference to the library resource, never a runtime id; independent of
any texture id; reusable across models; does not destroy slot properties **or**
the authored material when the paint changes. The serialised form of the
assignment is not fixed here.

**5.2. Storage of the paint library.** Decided (A1): the reusable Paint
Definition library lives in **typed OpenSLRR resources inside RPK**, loaded by
OpenSLRR's own resource system. It is not part of SCX, not `CARCOLORS` and not a
sidecar. The numeric type id, the binary structure of the resource, the loader
and the permanent reference format are **not designed yet**; see
`MATERIAL_SYSTEM_ARCHITECTURE.md` §10.4. Compatibility of the new resource type
with the original executable is not claimed.

**5.3. Slot count.** The number of Paint slots is defined by the specific car
model. The architecture must support an arbitrary count.

- No hard-coded `PrimaryPaint` / `SecondaryPaint` fields.
- Multiple slots are not required on every car.
- One slot may be bound to several geometry groups.
- Different slots may reference the same `PaintDefinition`.
- Each slot has an independent `MaterialAssignment`.
- Slot identity is the stable `slot_id`, not a group index and not a position in
  the slot list.

**One slot is enough for the first implementation.** Supporting an arbitrary
count is architecture, not a promise that two-tone paint ships now. SCX carries
stable `slot_id`s, roles, authored materials and bindings; vehicle state
persists an independent assignment for every slot in use.

## 6. Core entities

| Entity | Meaning |
|---|---|
| `MaterialDefinition` | a material description. Source: SCX v5 (model-owned, including the authored material of a Paint Slot), typed RPK resource (Paint Definition library), sidecar (development only) |
| `MaterialSlot` | the model-owned target of a binding: `slot_id`, `role`, mandatory `material_ref` to the model's own material |
| `MaterialBinding` | geometry group → `slot_id`; the **sole source of truth** for the group↔slot link |
| `MaterialAssignment` | instance state: `slot_id` → Paint Definition (permanent reference) plus its layers. Optional, only for Paint Slots; it never replaces `material_ref` |
| `ResolvedMaterial` | the CPU-side result handed to the shader |

`MaterialDefinition`, `MaterialSlot` and `MaterialBinding` are model content:
immutable for a given model revision. `MaterialAssignment` and the Layer Stack
are dynamic instance state. The two must not be mixed in storage or in the
entity model.

`ResolvedMaterial` must keep the source, id and original material kind for
diagnostics. The renderer must not have to work out where a material came from.

## 7. Data paths and sidecar

`legacy SCX → legacy material`;
`SCX v5 → model-owned Material Definitions + Material Slots + Material Binding`;
`RPK typed resource → Paint Definition library`;
`vehicle state → MaterialAssignment + Layer Stack`;
`sidecar → temporary development definitions`.
All resolve through one Material System.

The sidecar is development/test only and must not become a permanent library or
a second source of truth next to the typed RPK resources.

## 8. SCX and its extension

SCX v5 stores the model-owned side of the contract:

- authored `MaterialDefinition`s;
- `MaterialSlot` with `slot_id`, `role` and mandatory `material_ref`;
- `MaterialBinding` (group → `slot_id`);
- the model-owned data those need: texture references, UV channels, versioned
  extra data, optional fallback.

It must **not** store copies of Paint Definition variants. The library is shared
by many models, so a copy per SCX would duplicate it and drift.

The exporter recognises the reserved material name `SLRR_Paint_<SlotName>` at
authoring/export time and writes an explicit role into SCX v5. Runtime reads
that role and does not parse the material name. The exporter creates and edits
authored materials and slots, binds geometry, and keeps ids and links stable. It
does not author the paint library — that belongs to OpenSLRR's typed RPK
resources and their tooling.

Old SCX must load without mandatory conversion. Round-trip must separately
verify geometry, UV, authored materials, `slot_id`s, roles, bindings, legacy
parameters, alpha and texture assignments. The chosen Paint Definition is saved
as instance state and must never be silently overwritten by an SCX export.

## 9. Light system

**`LightDefinition`** — Point/Spot/Directional type, source resource parameters,
local position and direction, range, attenuation, cone parameters, colour and
intensity, projection texture references, source flags.

**`LightInstance`** — reference to a definition, owner node in the resource
tree, transformed world position and direction, current on/off state, parameters
after game logic has run.

**`LightRegistry`** — the available lights, identification and updating, scene
and camera selection, later the lists consumed by shaders and shadow passes.

Names are conceptual. Do not reduce this to pointers on `RenderRef` or to the
existing `gameref_directional_lights()`; a typed model of all three light kinds
is required. Old D3D9 parameters go through an explicit adapter and are not
discarded before their semantics and calibration are established.

## 10. Emissive vs real lights

Emissive belongs to a model-owned material or to a library PaintDefinition. It
is visible surface emission; by itself it is **not** a Point or Spot light and
does not guarantee surrounding objects are lit. The link between glowing
geometry and a real light is game logic / resource system.

For headlights keep three separate things: the emissive lamp surface, the direct
Spot light, and bloom.

## 11. Subsystem interfaces

Data flows:

1. SCX parser → geometry, model-owned `MaterialDefinition`, `MaterialSlot`, Material Binding
2. Paint library loader (typed RPK resource) → `PaintDefinition`
3. Vehicle / game state → `MaterialAssignment`
4. Sidecar loader → test definitions into the same Material System
5. Material System → `ResolvedMaterial`
6. `ResolvedMaterial` + geometry → draw submission
7. RPK / resource loader → `LightDefinition`
8. Scene graph / RenderRef → transforms and `LightInstance` state
9. LightRegistry → main render **and** Reflection Capture
10. Future SCX parser / exporter → read and write model-owned materials, slots and bindings

The **same** `LightInstance` feeds the main render and the reflection capture.
Two independent lighting implementations are not allowed.

Materials and lights do not own each other's data; they meet at frame assembly.

The Material System must not depend on bgfx objects. Internal representation is
CPU-side; GPU handles and bind calls belong to the backend.

## 12. Repaint mechanism — read-only audit

Observed in the current code, at `42d795e`.

- `part_runtime.cpp` `paint_one(Object*, uint32_t texture)` sets
  `rec->state.texture = texture` and writes `node->paint_texture` on every
  render node of the part.
- `paint_tree(root, texture)` walks the part tree and applies one texture to
  all parts, capped at 512 nodes.
- Each node holds `paint_source` (the original, from `rec.config->texture`) and
  `paint_texture` (the current one), plus `color` derived from a colour list,
  defaulting to `0xffffff`.

**Confirmed fact:** repaint is currently a **texture substitution** with an
associated colour. It is exactly the mechanism section 2 and section 5 exclude
from the new material model, because swapping a texture ID cannot carry
clearcoat, roughness or layer parameters.

### Repaint mechanisms — keep them apart

Five different things exist today and must not be conflated:

| Mechanism | What it does | Where |
|---|---|---|
| `PartConfig::texture` | the part's original texture ID | `part_config.cpp` |
| `InstalledPart::texture` / `Record::state.texture` | the part's current texture ID | `part_runtime.cpp` |
| `Part.setTexture()` / `getTexture()` | per-Part texture swap, exposed to scripts | `part_runtime.cpp:4977-4991` |
| `paint_tree()` | one texture ID applied to a whole part tree, capped at 512 nodes | `part_runtime.cpp:1316` |
| `PartRender::colors` | a list of RGB variants per render node | `part_config.cpp:323` |

**Additional audit, confirmed facts:**

- The create string's **seventh field** is `colour_index`, an index into each
  render's `colors` list, gated by a `colour_set` flag
  (`part_runtime.cpp:106`). `paint_of()` returns `0xffffff` when the flag is
  unset or the list is empty, otherwise `colors[pick]`.
- `state.texture` **is persisted per part**: it is written from the state field
  named `"color"` (`part_runtime.cpp:570`) and defaults to
  `PartConfig::texture` (`part_runtime.cpp:1192`). So a per-part texture
  survives a save and reload.
- `render.variants` chooses a *resource* variant by seed; `render.colors`
  chooses a *colour* variant by seed. These are separate mechanisms.

**Not established by this audit** (at the time of this pass):

- where the original game stores the available paint variants *(closed later:
  nowhere — the legacy data has no definitions, §15)*;
- how paint definitions are represented in the game resources *(closed later:
  they are not; "paint" is a texture id, §15)*;
- how the user selects and applies a paint in game;
- whether more than one paint slot type exists;
- how the chosen paint is persisted per vehicle;
- what exactly changes on repaint beyond the texture;
- repaint behaviour in garage, preview and saved-car load;
- whether the original logic ever calls `setTexture()` per body panel, or
  always applies one colour to the whole body;
- whether original resources or scripts carry the concept of several
  independent body colour groups;
- how `PartRender::colors` is actually used in real content;
- whether two different textures on two body panels is a supported mechanic or
  a side effect of the API.

**Do not conclude that two-tone paint is supported merely because `setTexture()`
can be called on individual parts.** That is API capability, not evidence of a
game mechanic.

The library's **storage home** is now decided (A1, §5.2); this list still
records what the legacy audit itself did not establish. Follow-up read-only
research (A1) closed part of it — legacy data contains no material definitions
and no RPK resource carries paint parameters (§15). The **shape** of
`PaintDefinition` itself remains future work. Do not infer the original game's
internals from the architecture we would like to have.

## 13. Open questions

1. How many paint slots does real content actually need?
2. What are the real units of the legacy D3D9 light parameters, and the world
   scale needed to convert them?
3. How do `attH_texture` / `attV_texture` behave, and do they need to reach the
   reflection capture as well?
4. How do exported lights get attached to a game object? See §14.
5. What is the fallback behaviour when a Paint Slot has no Material Assignment —
   a default paint from the library, or the authored SCX material as-is?
6. Should `ResolvedMaterial` be per-draw or cached per slot? Affects the frame
   assembly path but not the contract.

## 14. Light definitions and SCX — decision

Fixed:

- SCX does **not** store Point/Spot/Directional definitions.
- Lights stay part of the game resource system and the object hierarchy.
- The Runtime Light System receives them through the resource loader and the
  scene graph.
- The future SCX Exporter must **not** write light definitions into SCX.

A future 3D editor must still be able to author and place lights. The export
would be several related artefacts: SCX for geometry and materials, separate
game light resources, and the links binding them to the object and resource
tree nodes.

Do **not** add light attachment fields to SCX. First find out how existing RPK
resources connect lights to objects. If that genuinely needs a locator, propose
a separate format or binding mechanism.

**Open question:** how are exported lights attached to an object? Existing RPK
resource linking has not been examined yet, and no locator format is proposed
here.

## 15. Paint library audit

Read-only, source revision `42d795e`.

**Confirmed facts**

- `java.game.GameLogic.CARCOLORS` is the only paint source referenced from the
  native side (`part_runtime.cpp:4531`). It is a static holding an array of
  **integer texture IDs**, nothing more.
- `vehicle_spawn_stock()` reads it only as a fallback: when no texture is
  supplied it takes the **first non-zero** int from `CARCOLORS` and uses that
  (`part_runtime.cpp:4532-4539`).
- The value then travels as `Descriptor.fields["color"]`
  (`part_runtime.cpp:4545`), and lands in `Record::state.texture` via
  `field("color", 0)` (`part_runtime.cpp:570`).
- `Part.setTexture()/getTexture()` read and write the same `state.texture`.
- The part creation signature carries a `java.game.parts.Descriptor`
  (`part_runtime.cpp:4999`), and that descriptor is where the colour is
  attached at install time.

**Conclusion from those facts:** in the data the game uses today, "paint" **is**
a texture ID. `CARCOLORS` carries no material parameters -- no clearcoat,
roughness, reflectance, layers or flake. There is no existing paint library to
extend, because there is no material definition behind the colour at all.

**Decision (A1).** The legacy store contains no suitable Paint Definition, so one
is **created**: the reusable paint library is a typed OpenSLRR resource inside
RPK, loaded by OpenSLRR's own resource system. Extending `CARCOLORS` in place
would inherit the texture-only shape and cannot carry a layered paint model.
The original executable is **not** claimed to be compatible with the new
resource type. The numeric type id, the payload, the loader and the reference
format remain future work (`MATERIAL_SYSTEM_ARCHITECTURE.md` §10.4).

**Closed by later read-only research (A1)**

- The game scripts (`java.game.*`) hold no richer paint data than texture ids
  and RGB colours; there are no PBR paint fields there.
- No RPK resource carries material parameters: the packs hold model, part,
  particle and light descriptors, not material definitions.

**Not established**

- How the game's own catalogue of variants is presented to the player.
- How a repaint is persisted in a saved vehicle beyond the per-part
  `state.texture` already described in §12.

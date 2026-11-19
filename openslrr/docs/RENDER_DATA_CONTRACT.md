# Render data contract

Status: **architecture proposal**. No code, no binary layout, no final C++
types. Field lists are conceptual and are frozen only after the material model
and BRDF are validated.

Scope: the internal material model and the interfaces between the resource
system, SCX and its parser, the Material System, the renderer, reflection
capture and the SCX Exporter.

## 1. Responsibility split

| Owner | Holds |
|---|---|
| **SCX** | geometry, material slots, geometry→slot bindings, ordinary surface materials |
| **Game material system** | paint material definitions |
| **Vehicle state** | the chosen paint and its assignment to the paint slot |
| **Game resource system** (RPK / RenderRef) | light definitions and their placement in the object hierarchy |
| **Runtime Light System** | active light instances with world transforms and live state |
| **Renderer** | receives geometry, resolved materials and lighting as independent data |

Point/Spot are **not** added to SCX for the sake of material extension. A future
light export from a 3D editor is a resource-system or placement-format task, not
part of SCX material slots.

## 2. Combined material system

Two storage scenarios, deliberately different.

**Ordinary materials belong to the model and live in SCX.** Plastic, seat
leather, rubber, glass, plain metal, other surfaces.

**Automotive paint is a separate game entity.** It is a specialised material
that exists independently of any one car model and is assigned to a special
Paint slot. Gloss, Candy, Metallic, Matte, Satin, Chrome-as-paint.

Selecting a paint is **not** a texture or colour swap. It assigns a different
full material with its own shading model, parameters, textures and layers.

A single internal representation for the renderer does **not** mean every
material has the same storage home.

## 3. MaterialSlot

Declared by the model, bound to one or more geometry groups. It defines what
the surface is and how its material is obtained.

**SCX-owned slot** — the material is defined in SCX. SCX carries slot/material
id, purpose, geometry group bindings, ordinary material properties, texture
references.

**Game-managed slot** — SCX declares the slot, the material definition comes
from a separate game system. The main case is `Paint`. SCX carries a stable slot
id, the semantic role `Paint`, geometry bindings, the permitted material model,
and an optional fallback. SCX does **not** carry a copy of the paint library or
of each variant's parameters.

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

## 5. Paint material

**5.1. Assignment.** Conceptually `VehicleInstance → PaintSlot → PaintDefinition`,
e.g. `Mazda3_Instance.Paint = Candy_Red`. Repainting changes the chosen
PaintDefinition; geometry, SCX and the car's other materials do not change.

Requirements: persisted in vehicle state; restored when a saved car loads;
independent of any texture ID; reusable across models; does not destroy slot
properties when the paint changes.

**5.2. Storage of the paint library. Not fixed yet.** Audit the existing game
resources and repaint mechanism first. If a suitable structure already exists,
extend it preferentially. Do not mix the PaintDefinition library with SCX
geometry.

**5.3. Slot count.** The number of Paint slots is defined by the specific car
model. The architecture must support an arbitrary count.

- No hard-coded `PrimaryPaint` / `SecondaryPaint` fields.
- Multiple slots are not required on every car.
- One slot may be bound to several geometry groups.
- Different slots may reference the same `PaintDefinition`.
- Each slot has an independent `MaterialAssignment`.

**One slot is enough for the first implementation.** Supporting an arbitrary
count is architecture, not a promise that two-tone paint ships now. SCX carries
stable slot ids and geometry bindings; vehicle state persists the assignment for
every slot in use.

## 6. Core entities

| Entity | Meaning |
|---|---|
| `MaterialDefinition` | a material description. Source: SCX (ordinary), game library (paint), sidecar (development) |
| `MaterialBinding` | geometry group → MaterialSlot, or → an SCX-owned material directly |
| `MaterialAssignment` | dynamic material→slot assignment on a concrete game object. Needed for PaintSlot; not invented artificially for SCX-owned materials the model already fixes |
| `ResolvedMaterial` | the CPU-side result handed to the shader |

`ResolvedMaterial` must keep the source, id and original material kind for
diagnostics. The renderer must not have to work out where a material came from.

## 7. Data paths and sidecar

`legacy SCX → legacy material`; `extended SCX → SCX-owned materials + slots`;
`game library → PaintDefinition`; `vehicle state → MaterialAssignment`;
`sidecar → test definitions`. All resolve through one Material System.

The sidecar is temporary and must not become a rival permanent material library.

## 8. SCX and its extension

SCX must support ordinary materials, MaterialSlot, geometry→material/slot
bindings, game-managed slots including Paint, optional fallbacks, versioned
extra data. It must **not** store copies of all PaintDefinition variants.

Old SCX must load without mandatory conversion. The exporter must create and
edit ordinary materials and slots, bind geometry, assign geometry to the Paint
slot, and keep ids and links stable. It does not have to author the paint
library — that belongs to the game material system.

Round-trip must separately verify geometry, UV, SCX-owned materials, slots and
bindings, game-managed slot references, legacy parameters, alpha and texture
assignments. The chosen PaintDefinition is saved by the game mechanism and must
never be silently overwritten by an SCX export.

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

Emissive belongs to an SCX-owned material or a PaintDefinition. It is visible
surface emission; by itself it is **not** a Point or Spot light and does not
guarantee surrounding objects are lit. The link between glowing geometry and a
real light is game logic / resource system.

For headlights keep three separate things: the emissive lamp surface, the direct
Spot light, and bloom.

## 11. Subsystem interfaces

Data flows:

1. SCX parser → geometry, SCX-owned `MaterialDefinition`, `MaterialSlot`, group bindings
2. Material library loader → `PaintDefinition`
3. Vehicle / game state → `MaterialAssignment`
4. Sidecar loader → test definitions into the same Material System
5. Material System → `ResolvedMaterial`
6. `ResolvedMaterial` + geometry → draw submission
7. RPK / resource loader → `LightDefinition`
8. Scene graph / RenderRef → transforms and `LightInstance` state
9. LightRegistry → main render **and** Reflection Capture
10. Future SCX parser / exporter → read and write SCX-owned materials and slots

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
associated colour. It is exactly the mechanism section 2 says must not survive
into the new material model, because swapping a texture ID cannot carry
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

**Not established by this audit** (needs its own pass over the game resources):

- where the original game stores the available paint variants;
- how paint definitions are represented in the game resources;
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

Until those are answered, the shape of `PaintDefinition` and its storage home
stay open. Do not infer the original game's internals from the architecture we
would like to have.

## 13. Open questions

1. Where does the paint library live? (blocked on the audit in §12)
2. How many paint slots does real content actually need?
3. What are the real units of the legacy D3D9 light parameters, and the world
   scale needed to convert them?
4. How do `attH_texture` / `attV_texture` behave, and do they need to reach the
   reflection capture as well?
5. How do exported lights get attached to a game object? See §14.
6. What is the fallback behaviour when a game-managed slot has no assigned
   material — a default paint, or the SCX fallback?
7. Should `ResolvedMaterial` be per-draw or cached per slot? Affects the frame
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

**Not decided:** the storage home of the paint library. Design decision taken:
paint definitions are game content. The concrete location is still open and
depends on the RPK / script / catalogue audit in §3.

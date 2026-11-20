# Legacy paint — audit record

Base revision: `06ce08ec75fe2882ee1d4c3c311989ec28678eb8`.
Read-only audit. No code, resource or format change.

This file records the audit stages so they do not live only in the chat
transcript. It holds stages 2–14 (the runtime mechanics of the legacy part
texture), stage 15 (the B1 content inventory and the Legacy Paint
definitions), stage 16 (the B2 Skin Preset → surface mapping), stage 17
(the B3 `DiffuseMixSecond` alpha survey) and stage 18 (the B4 instance
persistence model); no stage 1
text exists, the first recorded stage is 2. Stage 15 states explicitly where
it corrects earlier provisional numbers; stage 16 records one open point of
stage 13 instead of rewriting it; stage 17 corrects the Mazda control case
of stage 15 with the stock file from HEAD; stage 18 corrects the storage
statement of §13.2 in the architecture document (a copy, not a reference to
the source skin).

---

# Stage 2 — RPK resources

## Confirmed

Factory colours are PNG textures, not material definitions.

All 21 `GameLogic.CARCOLORS` entries in `cars.rpk` resolve to type-7 resources
whose payload is RSD text:

| ID | Alias | Payload |
|---|---|---|
| `cars:0x2ar` | `Devil's` | `sourcefile cars\textures\factory_colors\Baiern\Devils_eye_red.png` |
| `cars:0x42r` | `Zucker.png` | `sourcefile cars\textures\factory_colors\Einvagen\Zucker.png` |
| `cars:0x65r` | `rusty_cherry` | `sourcefile cars\textures\factory_colors\Used\used_rusty_Cherry.png` |

All are `sourcefile …png` plus `flags 1.000`.

`paintcan` exists in `frontend.rpk` in four resource types:

| Type | Payload |
|---|---|
| 5 | `sourcefile frontend\meshes\spray.SCX` |
| 7 | `sourcefile frontend\textures\spray_256.dds` |
| 14 | `mesh 0x5E, flags 640, texture 0x55` |
| 8 | `native part frontend\scripts\paintcan.cfg` |

`frontend/scripts/paintcan.cfg` is `render 0x00000021` and
`body 0 0 0 0 0 0 0 sphere 0.1` — an inventory 3D object with a collision
sphere, not material data.

`decals.rpk` holds type-7 texture entries. Painter UI art lives in
`frontend.rpk` as `paint`, `paint_decal`, `paint_loadskin`, `paint_saveskin`,
`paint_spray`, `paint_spraypart`, `paintcan` — all type 7.

## Unknown

- `cannaatska`, referenced by `InventoryItem_Paint.class`, is **not in any RPK
  and not on disk**. Unresolved.
- Where `PaintCan.color` / `capacity` values come from — no RPK table found.
- Skin file format and location (`save/` contains only `carrer`, `controls`,
  `game`, `temp`).

---

# Stage 3 — `factory_colors` catalogue

## A. Inventory

**82 PNG files.** All 256×256, colour type 2 (RGB), **no alpha channel on any
of them**. Sizes 2.4–85 KB; `Used/used_rusty_*` are much larger (71–85 KB)
because they carry rust and wear.

| Folder | Files |
|---|---|
| Baiern | 8 |
| Duhen | 9 |
| Einvagen | 16 |
| Focer | 6 |
| Ishima | 5 |
| Mc | 13 |
| Nonus | 9 |
| Prime | 1 |
| SuperDuty | 7 |
| Used | 8 |

## B/C. Registration statistics

| Category | Files |
|---|---|
| Total PNG | 82 |
| **Registered in RPK** | **26** |
| **Not registered** | **56** |
| In `CARCOLORS` | 21 |
| Registered but **not** in `CARCOLORS` | **5** |

The five extra registrations are `0x03b`, `0x03d`, `0x03e`, `0x03f` (four
Baiern GT variants, all sharing the alias `GT`) and `0x03c`
(`Mc/Liberty.png`).

Note: `Baiern/Nacht.png` exists on disk but has **no registration of its own**
— `0x044` points at `Einvagen/Nacht.png`.

Registration map (RPK ID → file):

- `0x02a` Baiern/Devils_eye_red, `0x02b` Baiern/Spring_yellow
- `0x03b` Baiern/GT_III_black, `0x03d` Baiern/GT_III_gold,
  `0x03e` Baiern/GT_III_white, `0x03f` Baiern/GT_III_red
- `0x03c` Mc/Liberty
- `0x042`–`0x050` Einvagen/Zucker … Saratoga (15 files)
- `0x065`–`0x068` Used/used_rusty_*

## D. Load mechanism (confirmed from code)

1. `RpkIndex::find_entry(pack_id, type_id)` / `resolve(full_id)` — lookup by
   **ID**, never by alias (`rpk_index.cpp:305`, `:318`).
2. `read_payload(type_id)` → RSD text; `payload_prop(id, "sourcefile")`
   extracts the path (`rpk_index.cpp:462-465`, `:485`).
3. The file opens as `game_root / relative` (`part_runtime.cpp:3718-3720`).
4. For part textures, `load_part_textures()` takes `part.geo.textures` from
   SCX, resolves relative paths, then `load_texture_file(path)`
   (`bgfx_lib_backend.cpp:2625-2703`).

Alias is used **only** for garage folder lookup (`rpk_index.cpp:703`), never
for textures. There is no "load by filename from disk" path.

## E. Conclusions

1. **Not all 82 are part of the resource system** — only 26 (32%) are
   registered.
2. **An unregistered PNG cannot be loaded by ID.** Resolution is
   `ID → RSD → sourcefile → file`.
3. **Only 26 are confirmed factory variants**; 56 have no registration and no
   found reference.
4. **Model binding is confirmed only for the 21 `CARCOLORS` entries**, and
   indirectly via the `RID_CARCOLOR_*` name. For `0x03b-0x03f` and `0x03c` the
   model link is **unconfirmed** — four GT variants share one alias `GT`.
   Otherwise the model link is **only the folder name**, which is not evidence
   of a runtime relationship.
5. **The whole catalogue cannot be used as a source without verification.**
6. For compatibility keep: the 21 `CARCOLORS` IDs, the 26 RSD descriptors with
   `sourcefile` and `flags 1.000`, the `factory_colors/<Model>/<Name>.png`
   layout, and the paint-stub mechanism (stage 4).

---

# Stage 4 — paint-stub

## 1. Detection conditions

`bgfx_lib_backend.cpp:2686-2700`, lambda `is_paint_stub(path, img)`.

**Two independent conditions joined by OR.**

**A — by name (line 2690):** substring `auto.dds`, lowercased, not anchored.
Fires on `g1Auto.dds`, `benzinkut_automoso.dds`, `xauto.dds.bak`. **File
contents are not inspected at all.**

**B — by content (lines 2691-2699), only if A fails:**
requires `img.ok`, `format == DdsFormat::DXT1`, `data.size() >= 8`, then reads
the first DXT1 block colour `c0` and tests
`g > 140 && r > 100 && b < 40 && g >= r`.

Only the **first block** is examined, not the whole image.

## 2. `white_tex_`

Created at `bgfx_lib_backend.cpp:340` as `createTexture2D(1, 1, … RGBA8 …)` —
a 1×1 RGBA8 texture, not a file or resource. Destroyed at `:386`. Deliberately
**not** destroyed during texture cleanup (`:2402-2403`) because paint stubs
alias the same handle.

Fourteen uses total. Besides fallback duty, `bgfx_lighting.cpp:199,225,250`
use `idx != white_tex_.idx` as a **"has texture" marker** for glass, mix and
reflection detection. So after a paint-stub substitution the slot is
indistinguishable from "no texture".

The original texture is **never loaded** — `continue` at `:2712` precedes
`createTexture2D`. Only `part.tex[i]` changes; SCX group data is untouched.

## 3. Load chain

`part.geo.textures` (scx_mesh.cpp:559-565) → `load_part_textures()`
(bgfx_lib_backend.cpp:2625) → path normalisation (`:2650-2662`) →
`load_texture_file(path)` (`:2703`) → `is_paint_stub` (`:2710`) →
`part.tex[i] = white_tex_` (`:2711`) or `createTexture2D` (`:2718+`) →
`texture(slot)` (bgfx_lighting.cpp:185-192) → paint override (`:196-197`) →
`setTexture` (`:397+`).

## 4. Colour chain — two independent mechanisms

**Tint (`instance.color`)** — `bgfx_lighting.cpp:271-278`:
```
if (instance.color != 0xffffffu):
  if (has_tex && !painted) diffuse[i] *= paint[i];
  else if (!painted)       diffuse = paint;
```
`instance.color` ← `n->color` (`scene_instances.cpp:1822`) ← `paint_of()`
(`part_runtime.cpp:456`) ← `PartRender::colors` picked by the create string's
7th field.

**Texture substitution (`instance.paint_texture`)** —
`bgfx_lighting.cpp:196-197`:
```
painted = instance.paint_slot >= 0 && group.texture_index == instance.paint_slot;
if (painted && !instance.paint_texture.empty()) base = tex_for_osd_icon(instance.paint_texture);
```
`instance.paint_texture` is a **string**, written at
`scene_instances.cpp:1860` from `scene_instances_texture(n->paint_texture)`,
i.e. `uint32_t` resource ID → path via `RpkIndex::texture_path_for_id()`.

`instance.paint_slot` is computed at `scene_instances.cpp:1836-1846` by
comparing the `n->paint_source` path against `geometry.textures`.

When `painted` is true the tint is suppressed (`if (has_tex && !painted)`), so
a substituted texture is not tinted twice.

## 5. Original resources

**Confirmed:** exactly one file matches condition A:
`misc/garage/g1/g1Auto.dds`, 296 bytes, DDS DXT1 16×16, first block
RGB(164, 190, 0) — a lime-green fill. It passes both A and B.

`maps/textures/benzinkut_automoso.dds` is a **false positive of the substring
test** (`automoso` contains `auto`) and is unrelated to painting.

**Not confirmed:** `g1Auto.dds` lives under `misc/garage/g1/`, i.e. it is a
**garage** texture, not a car body texture. No RSD/SCX linking it to a body
was found. The code comment says "paintable body slots (g1Auto.dds etc.)" but
that link is not evidenced by the resources.

## 6. Mechanism table

| Mechanism | Input | Result | Where | Confirmed role |
|---|---|---|---|---|
| paint-stub by name | substring `auto.dds` | `part.tex[i] = white_tex_` | bgfx_lib_backend.cpp:2690 | OpenSLRR heuristic; not in the original format |
| paint-stub by content | first DXT1 block, thresholds | same | `:2691-2699` | OpenSLRR heuristic |
| tint | `instance.color` (int) | diffuse multiply/replace | bgfx_lighting.cpp:271-278 | confirmed original mechanic |
| paint texture | `n->paint_texture` (ID) → path | `base = texture` | `:196-197` | confirmed substitution mechanic |
| paint slot | path comparison | `paint_slot` | scene_instances.cpp:1836-1846 | confirmed mechanic |
| `white_tex_` as marker | idx comparison | glass/mix/reflection classification | bgfx_lighting.cpp:199,225,250 | OpenSLRR-specific |

## 7. Answers

- **What is paint-stub?** An OpenSLRR heuristic for placeholder textures that
  must not be uploaded.
- **Why `white_tex_`?** Colour arrives by a separate path (tint or
  `paint_texture`); a white base avoids double-applying it.
- **Where is the colour applied?** `bgfx_lighting.cpp:271-278` (tint, all
  materials) and `:196-197` (substitution, `paint_slot` only).
- **Part of the original format?** No evidence. One candidate resource, a
  garage texture. The thresholds are an OpenSLRR rule with no stated
  justification.
- **A valid integration point?** **Not as it stands** — it has false
  positives, is not tied to SCX data, and conflates "no texture" with
  "placeholder".
- **What to keep?** The tint mechanism and `paint_slot`/`paint_texture`, not
  the heuristic.

---

# Stage 5 — provenance and lifecycle

Base revision `06ce08ec75fe2882ee1d4c3c311989ec28678eb8`.

## 1. `Record::state.texture`

**Type:** `uint32_t`. **Semantics: a type-7 resource ID**, not an RGB value.

| Site | Direction | Note |
|---|---|---|
| `part_runtime.cpp:1192` | write | default = `cfg->texture` (the original) |
| `part_runtime.cpp:570` | write | from `Descriptor.fields["color"]` |
| `part_runtime.cpp:1311` | write | `paint_one()` |
| `part_runtime.cpp:4977` | read | `Part.getTexture()` |
| `part_runtime.cpp:4991` | write | `Part.setTexture()` |
| `part_runtime.cpp:455, 572, 4994` | read | mirrors into `node->paint_texture` |

**`Part.setTexture(I)I`** (`part_runtime.cpp:4978-4995`) is the decisive
evidence:

```cpp
const RpkEntry *entry = id && index ? index->resolve(id) : nullptr;
if (!entry || entry->typeof_ != 7) return Value::make_int(old);
p->state.texture = static_cast<int>(id);
```

The argument is validated as a **type-7 texture resource**. A non-resolving or
non-texture ID is rejected and the part is left alone. The comment records why:
a loaded car writes 0 for an unpainted piece and a factory colour for the rest,
and clearing the render there made panels vanish.

`Descriptor.fields["color"]` — **written only** at `part_runtime.cpp:4545` in
`vehicle_spawn_stock()`, **read only** at `:570`. Despite the name it carries
the texture resource ID.

**Persistence boundary:** no native code writes `Descriptor.fields["color"]`
for saving. Vehicle serialisation is Java-side; the native layer only consumes
the descriptor it is handed.

## 2. `paint_source`, `paint_texture`, `paint_slot`

All three are fields on the render node / scene instance:

```cpp
include/slrr/gameref.hpp:178   uint32_t paint_source = 0, paint_texture = 0;
include/slrr/scene_instances.hpp:26   std::string paint_texture;
```

| Field | Set at | Value |
|---|---|---|
| `paint_source` | `part_runtime.cpp:452` | `rec.config->texture` — the **original** texture ID |
| `paint_texture` | `part_runtime.cpp:455, 572, 1314, 4994` | `rec.state.texture` — the **current** texture ID |
| `paint_slot` | `scene_instances.cpp:1843` | index into `geometry.textures` |

**This is the key relationship:** `paint_source` says **which slot to
replace**, `paint_texture` says **what to put there**.

- `paint_source` is resolved to a path by `scene_instances_texture()`
  (`scene_instances.cpp:1860` region, `RpkIndex::texture_path_for_id`,
  `rpk_index.cpp:485-500`, following `sourcefile` up to 4 levels deep).
- Path normalisation (`scene_instances.cpp:1825-1835`): `\` → `/`, lowercased,
  extension stripped, and anything before `game/` trimmed. This makes the
  comparison case- and separator-insensitive and lets a DDS sidecar match a
  resource that names the original image.
- `paint_slot` is the **first** match; if there is no match the slot stays
  `-1` and `painted` is false; if several match, only the first is used. It is
  recomputed on every scene collection, so it can change if the geometry list
  changes.

Confirmed: the three fields are **not independent**. `paint_source` and
`paint_texture` are the same stage of one mechanism (slot selection and slot
content), while `paint_slot` is the runtime binding of that slot to a geometry
index.

## 3. `PartRender::colors` and tint

Chain: create string 7th field → `colour_index` / `colour_set`
(`part_runtime.cpp:106`) → `paint_of()` (`:427-435`) → `n->color` (`:456`) →
`instance.color` (`scene_instances.cpp:1822`) → shader (`bgfx_lighting.cpp:271`
and `:386-397`).

- `PartRender::colors` is a `std::vector<uint32_t>` of **RGB** values, filled
  by the `colors` command in `part_config.cpp:323-325`.
- `paint_of()` returns `0xffffff` when `colour_set` is false or the list is
  empty; otherwise `colors[clamped index]`.
- White `0xffffff` is the "no paint" sentinel, and the shader skips the block
  on `!= 0xffffffu`.
- Uniforms: `u_color_` gets the tint as RGBA (`bgfx_lighting.cpp:100, 397`).

**`PartRender::colors` and `Record::state.texture` are unrelated.** One is an
RGB tint list picked at part creation; the other is a texture resource ID that
can be swapped at runtime. They only meet in the shader.

## 4. Java bridge

- `Part.setTexture(I)I` / `getTexture()I` — the natives above. Argument is a
  **texture resource ID**, not RGB. Only `state.texture` and the render nodes'
  `paint_texture` change. `paint_source` and `paint_slot` are untouched.
- `RenderRef` native at `natives_renderref.cpp:528` sets `state->color` from an
  int arg — this is `InventoryItem_Paint.setColor`, and it writes the **RGB**
  colour, a completely different field.

**Limitation:** `Painter.class`, `PaintCan.class` and `InventoryItem_Paint.class`
are compiled to the game's own VM format, not standard Java class files, so
`javap` cannot read them. Only string-level inspection was possible. Method
semantics beyond the natives are therefore **not proven**.

## 5. Persistence

| Data | Persisted? | Evidence |
|---|---|---|
| `Record::state.texture` | yes, via `Descriptor.fields["color"]` | `part_runtime.cpp:570` read, `:4545` write |
| `paint_texture` | **no — recomputed** | written from `state.texture` at `:455, 572, 4994` |
| `paint_source` | **no — from SCX** | `:452` from `config->texture` |
| `paint_slot` | **no — recomputed** | `scene_instances.cpp:1843` |
| `n->color` / tint | **no — recomputed** | `paint_of()` at `:456` |

Saving a `state.texture` does **not** imply saving `paint_texture` or the tint;
the latter two are derived on load.

## 6. Field table

| Field | Type | Meaning | Source | Storage | Consumer | Persistence |
|---|---|---|---|---|---|---|
| `PartConfig::texture` | uint32 | original texture resource ID | SCX / config | `Record::config` | `paint_source`, default `state.texture` | no (from SCX) |
| `Record::state.texture` | uint32 | current texture resource ID | `Descriptor["color"]` or `setTexture` | `Record::state` | `paint_texture`, `getTexture` | **yes** |
| `node->paint_source` | uint32 | slot selector | `config->texture` | GameRef node | `paint_slot` | no |
| `node->paint_texture` | uint32 | slot content | `state.texture` | GameRef node | `instance.paint_texture` | no |
| `instance.paint_slot` | int | geometry index of the slot | path match | SceneInstance | `painted` | no |
| `instance.paint_texture` | string | replacement texture path | `texture_path_for_id` | SceneInstance | `tex_for_osd_icon` | no |
| `PartRender::colors` | vector<uint32> | RGB tint variants | part config | `PartConfig::renders` | `paint_of()` | no (from config) |
| `n->color` / `instance.color` | uint32 | RGB tint, `0xffffff` = none | `paint_of()` | GameRef / SceneInstance | shader `u_color_`, diffuse | no |

## 7. Answers

1. **`state.texture` == `paint_texture`?** Same *value*, different
   representation. `state.texture` is the authoritative `uint32_t` resource ID;
   `paint_texture` is a mirror on the render node and, in `SceneInstance`, a
   resolved path string. They are written together but are not the same field.
2. **Does tint use separate storage?** Yes. `PartRender::colors` is an RGB
   variant list unrelated to the texture ID; they only meet in the shader.
3. **How does Java Painter change native data?** Confirmed only for
   `Part.setTexture` → `state.texture` + `paint_texture`. The rest is inside
   unparseable VM class files and is **not proven**.
4. **Which mechanism is saved?** Only `Record::state.texture`, via
   `Descriptor.fields["color"]`.
5. **Recomputed on load?** `paint_texture`, `paint_source`, `paint_slot`, and
   `n->color` all are.
6. **Original logic vs OpenSLRR implementation?** The ID-based paint model,
   `Descriptor["color"]`, `colors` and the create string's 7th field are
   original. The paint-stub heuristic, `white_tex_` as a "no texture" marker,
   and the path normalisation rules are **OpenSLRR implementation**.
7. **A single integration point?** **No single confirmed point exists.** The
   closest is `Record::state.texture`, because it is the only persisted value
   and the only one validated against the resource system
   (`part_runtime.cpp:4986-4991`). But it carries a texture ID, not a material,
   so a new material system must be attached above it — at the slot selected by
   `paint_source`, not by replacing the texture ID.

## Open questions

- Does Java-side vehicle save write `Descriptor["color"]` on its own, or
  through a native not yet examined?
- Do `Painter` / `PaintCan` change anything beyond `state.texture`?
- Why `misc/garage/g1/g1Auto.dds` is treated as a paintable body slot.
- Whether any of the 56 unregistered `factory_colors` PNGs are reachable
  through a path not covered by this audit.

---

# Stage 6 — persistence and restoration of the part texture

Base revision `06ce08ec75fe2882ee1d4c3c311989ec28678eb8`.

## 1. The save path

**Confirmed by exhaustive search:** the native layer registers **no** save
method for parts.

- `register_part_natives()` (`part_runtime.cpp:4559-5660`) binds
  `Part` / `Chassis` / `WheelRef` / `SfxTable` / `DynoData` / `Vehicle`. The
  full bind list was enumerated: slot access, `getMass`, `getTexture`,
  `setTexture`, `addPart`, wheel physics, SFX, dyno. **Nothing named save,
  serialize or write.**
- `natives_io.cpp`, `natives_resref.cpp` and `natives_gameref.cpp` contain no
  part serialisation either.
- Only two native sites touch `Descriptor.fields["color"]`:
  `part_runtime.cpp:4545` (write, inside `vehicle_spawn_stock`) and `:570`
  (read, in the `addPart` path). **There is no reverse write.**

The save path is therefore entirely Java. The native layer never observes a
save.

**Consequence: `Part.setTexture()` cannot propagate to the save file through
native code.** Whether Java reads it back with `getTexture()` and writes it into
a `Descriptor` is a Java-side question, addressed in §4.

## 2. The load path for a saved car

Same core as `vehicle_spawn_stock()`, reached differently: Java builds a
`Descriptor`, fills `fields`, and hands it to the native `addPart`.

`part_runtime.cpp:567-573`:

```cpp
rec->state.wear    = field("wear", 1);
rec->state.tear    = field("tear", 1);
rec->state.texture = static_cast<uint32_t>(field("color", 0));
for (auto *render : rec->renders)
  gameref_get(render)->paint_texture = rec->state.texture;
```

Behaviour on bad input: `field()` returns its fallback when the key is absent or
the value is neither Float nor Int, so a missing `"color"` yields **0**. A zero
`state.texture` sets `paint_texture = 0`; `scene_instances_texture(0)` returns
an empty string, so `instance.paint_texture` stays empty and the paint override
is skipped, leaving the original SCX texture visible. That matches the code
comment at `:4986-4990`: a loaded car writes 0 for an unpainted piece.

## 3. `setTexture` and `getTexture` semantics

| Method | Reads | Writes | Java `Descriptor` touched? |
|---|---|---|---|
| `getTexture()I` (`:4975`) | `p->state.texture` | — | no |
| `setTexture(I)I` (`:4978`) | `p->state.texture` (old value) | `p->state.texture`, every `node->paint_texture` | **no** |

`setTexture` validates the argument: `index->resolve(id)` must exist **and** have
`typeof_ == 7`. Otherwise it returns the old value and changes nothing. There is
no callback, observer or synchronisation hook anywhere in the native code.

So `setTexture()` mutates **runtime state only** and never touches the Java
object it was invoked on.

## 4. Save-file format and observed content

The save is proprietary binary (`SDAT` magic) and cannot be decoded from its
structure alone. Finding a bare number would be unreliable, so the following
rests on **structural context**.

`save/carrer/1-1/PlayerCar0` (4016 bytes) contains records shaped like

```
02 00 00 00 | 00 00 00 00 | 0d 00 00 00 | 4a 00 05 00 | 01 00 00 00
```

where the fourth field is a **full resource ID**: a little-endian u32 with the
pack number in the high 16 bits.

| Offset | Value | Pack | Type | Meaning |
|---|---|---|---|---|
| `0x5b0` | `0x05004a` | 5 | `0x4a` | Einvagen paint texture |
| `0x65c` | `0x05004b` | 5 | `0x4b` | Einvagen paint texture |
| `0x6a8` | `0x05004e` | 5 | `0x4e` | Einvagen paint texture |
| `0x808` | `0x060043` | 6 | `0x43` | Einvagen paint texture |
| `0x8e4` | `0x050049` | 5 | `0x49` | Einvagen paint texture |

**Confirmed:** the save file stores **full type-7 paint resource IDs** from the
`0x42`-`0x50` CARCOLORS family, pack-qualified. The stored value is a resource
ID, not RGB, not a path, and not a bare type index.

The same file also contains long runs of small sequential integers
(`0x2a, 0x2b, 0x2c, …` with `ff ff ff ff` neighbours). Those are **slot
indices**, a different structure, and must not be confused with texture IDs.

**Limitation:** the record layout is inferred from ID plausibility and pack
consistency, not from a decoder. Which record field belongs to which part is
**not established**.

## 5. Three scenarios

**A — factory car.** The ID originates in `Descriptor["color"]` handed in by
Java and read at `:570`. The save demonstrably contains type-7 IDs from the
CARCOLORS family. On reload the value is restored through the same `:570` path.
**Restoration confirmed by file format; the round trip was not exercised in
game.**

**B — repaint via `setTexture()`.** `state.texture` changes at `:4991`. No
native code writes it anywhere else. The save file must therefore receive it
from Java, and the only Java-visible accessor is `getTexture()`.
**Whether Java calls `getTexture()` during save is not established** — the save
logic lives in unreadable VM class files. This is the **break point**, and it
sits on the Java side, not the native side.

**C — tint only.** `PartRender::colors` comes from part config
(`part_config.cpp:323`), not from save state; `n->color` is recomputed at
`part_runtime.cpp:456`. No native path writes tint into a save. **Tint is not
persisted** by anything observed, and it cannot collide with
`Descriptor["color"]` because they are fields of different objects.

## 6. Java VM limits

`GameLogic.java` exists as source. `Painter.class`, `PaintCan.class`,
`InventoryItem_Paint.class`, `Garage.class` and the rest are in **the game's own
VM format, not standard Java class files** — `javap` reports *Bad magic number*.
No Tufa decompiler is present in the workspace. String-level inspection works
(used in stage 2) but call sequences cannot be recovered.

**This is where the chain breaks.** Whether Java reads `getTexture()` before
saving cannot be answered with the tools present. That is **not** evidence of
absence.

## 7. Persistence table

| Data | Runtime store | Load source | Serialised | Serialiser | Status |
|---|---|---|---|---|---|
| `Record::state.texture` | `Record::state` | `Descriptor["color"]` (`:570`) | yes — full resource ID present in `PlayerCar0` | Java; no native save exists | **confirmed restored; round trip via `setTexture` unproven** |
| `Descriptor["color"]` | Java `Descriptor.fields` | Java save file | yes | Java | **confirmed** (write `:4545`, read `:570`) |
| `PartRender::colors` | `PartConfig::renders` | part config | not from save | — | **not persisted** |
| `n->color` / tint | GameRef / SceneInstance | `paint_of()` (`:456`) | no | — | **not persisted, recomputed** |
| `paint_source` | GameRef node | `config->texture` (`:452`) | no | — | **not persisted, from SCX** |
| `paint_texture` | GameRef / SceneInstance | mirrored from `state.texture` (`:455, 572, 4994`) | no | — | **not persisted, recomputed** |
| `paint_slot` | SceneInstance | path comparison (`scene_instances.cpp:1843`) | no | — | **not persisted, recomputed** |

## 8. Answers

1. **Is the current `state.texture` saved after `Part.setTexture()`?**
   **Not by the native layer — that much is certain.** The save file does hold
   type-7 paint resource IDs, so something writes them. Whether a runtime
   `setTexture()` value reaches that path depends on Java calling
   `getTexture()`, which is **unproven**.
2. **By what mechanism, if yes?** The only possible route is Java reading
   `Part.getTexture()I` and writing it into a `Descriptor` before serialising.
   No native function does this.
3. **Where does the chain break?** Between `Record::state.texture` and the Java
   save object. The native side has no write-back into `Descriptor["color"]`,
   and the Java save code is not readable with the available tooling.
4. **Is the same resource ID restored?** For factory cars yes — full IDs of the
   CARCOLORS family are present in `save/carrer/1-1/PlayerCar0`. For a repaint,
   unproven.
5. **Is tint saved separately?** **No** native path saves it; it is recomputed
   from `PartRender::colors` in the part config.
6. **Authoritative vs runtime-only:** authoritative is `state.texture` and the
   save file it travels through. `paint_texture`, `paint_source`, `paint_slot`
   and `n->color` are all derived runtime representations.
7. **Is `Descriptor["color"]` a reliable persistence contract?** **Partially.**
   It is the only durable carrier, it demonstrably holds full type-7 resource
   IDs, and the read path is proven. But it is an int carrying a texture ID, it
   has no reverse-write native path, and the write-back that must exist is
   invisible. It is a *texture* contract, not a material contract; relying on it
   for a new material system would inherit both the ID coupling and the
   unverified round trip.

## Open questions (stage 6)

- Does Java call `Part.getTexture()` when saving a car?
- Which record field of `PlayerCar0` corresponds to which part?
- Does a repaint survive a save/load cycle in the original game?
- Why the save mixes pack-qualified IDs (`0x05004a`) with bare slot indices in
  the same file.

---

## Этап 7 — Java VM и обратная сериализация текстуры

**Проверяемый HEAD:** `06ce08ec75fe2882ee1d4c3c311989ec28678eb8`.
Документ остаётся локальным и незакоммиченным. C++, Java, RPK/SCX, игровые
ресурсы и сохранения не изменялись; игра не запускалась и экспериментальные
сохранения не создавались. Предыдущие разделы не переписывались.

### 1. Инструментарий VM и его ограничения

Файлы `.class` игры — **не** JVM-классы, а контейнер `TUFA` (magic `TUFA`,
версия 4), внутри которого лежат CONST-таблица, таблица членов и TREE-опкоды
собственной VM (проверено: `parts/scripts/Part.class` начинается с
`54 55 46 41 04 00 00 00`). Поэтому `javap` и стандартные Java-декомпиляторы
неприменимы — вывод этапа 6 подтверждён, но не является тупиком: байткод VM
восстанавливается.

Доступный инструмент найден внутри рабочего окружения:

| Инструмент | Расположение | Что делает |
|---|---|---|
| SLRR-Class-Reader (native WinForms) | `E:\OpenSLR\SLRR-Class-Reader`, `TufaDecompiler.cs` (1565 строк) | разбирает TUFA/TREE и печатает Java-подобный исходник |
| Происхождение | C#-порт `SLRR-JVM-decompiler` (nedobylskiy, LootDelivery fork, MIT) | указано в `THIRD-PARTY-NOTICES.txt` |
| Режим CLI | drop файла на EXE или запуск `EXE <path>.class` | молча пишет `<name>.java` рядом с входным файлом |

Работа велась **на копиях** классов во временном каталоге; в каталоге игры
файлы не создавались. Проверено на 39 классах, включая `Part`, `Chassis`,
`Vehicle`, `GameLogic`, `Inventory`, `Painter`, `Catalog`, `CarSelect`.

**Достоверность выхода.** Порядок вызовов, аргументы, константы, строковые
литералы и таблицы ресурсов восстанавливаются точно и совпадают с данными
файла сохранения (см. §4). Реконструкция условий местами лоссовая: встречаются
конструкции `if ((!(x)))` без восстановленного `else`, потерянные сравнения,
`local` без инициализации. Поэтому все утверждения ниже опираются на
**последовательность вызовов и аргументы**, а не на восстановленные условия.

Части, лежащие в RPK, — это `script`-ресурсы (`typeof 8`) с текстовым
содержимым (`script p...`), а сами классы частей лежат на диске в
`parts/scripts/...`; TOC `RPAK` версии `0x200` читается, что позволило
резолвить все идентификаторы из сохранения в имена ресурсов.

Ключевое отличие от этапа 6: **Java-код сохранения прочитан полностью**, и
именно он, а не native, является сериализатором деталей.

### 2. Восстановленный путь сохранения

`GameLogic.save(String)` (декомпилированный `GameLogic.java`):

```java
player.parts.save(local1);                 // инвентарь гаража -> файл "main"
...
player.carlot.lockPlayerCar();
player.carlot.saveCar(player.carlot.curcar);   // машина -> save/temp/PlayerCarN
...
File.copy(GameLogic.tempSaveDir, "PlayerCar*", arg0);
```

Далее (декомпилированные `CarLot.java`, `Vehicle.java`, `Chassis.java`,
`Part.java`):

```java
// CarLot.saveCar
cars[arg0].save(((GameLogic.tempSaveDir + "PlayerCar") + arg0));

// Vehicle.save
local2.write(Vehicle.SAVEFILEID_VHC);      // 0x88664422
local2.write(Vehicle.SAVEFILEVERSION_VHC); // 5
if (chassis) { chassis.save(local2); }     // -> Chassis.save -> Part.save

// Chassis.save
super.save(arg0);                          // Part.save: сама деталь
int local2 = 3; arg0.write(local2);        // версия хвоста Chassis
arg0.write(wheels);
while (local3--) { arg0.write(getWheelDamage(local3)); }
arg0.write(brake_balance);
arg0.write(getMileage());

// Part.save  <-- ГЛАВНОЕ МЕСТО
arg0.write(new GameRef(getInfo(GameType.GII_TYPE)));  // part type id
arg0.write(Part.SAVEFILEVERSION_PART);                // = 1
int local2 = getTexture();                            // <-- native getter
ResourceRef local3 = new ResourceRef(local2);
arg0.write(local3);                                   // <-- ЗАПИСЬ ТЕКСТУРЫ
int local4 = getMesh();
ResourceRef local5 = new ResourceRef(local4);
arg0.write(local5);
arg0.write(getWear());
arg0.write(getTear());
... по каждому слоту: write(slot), write(getSlotDamage(slot)),
    write(slotIDOnSlot(slotId)), затем рекурсивно local11.save(arg0)
arg0.write(-1);                                       // конец списка слотов
```

Инвентарь гаража (`Inventory.save`) использует **тот же** `Part.save`:

```java
int local3 = items.size(); arg0.write(local3);
while (local4 < local3) {
  if (local2 instanceof InventoryItem_Part) { arg0.write(0); local2.getPart().save(arg0); }
  else { arg0.write(1); local2.save(arg0); }   // InventoryItem_Folder: set.save + inv.save
}
```

Третий независимый сериализатор — `Vehicle.saveSkin(String)`:

```java
int local4 = local3.getTexture();
if (local4) { local2.write(new GameRef(...GII_TYPE)); local2.write(new ResourceRef(local4)); }
```

Итого: **три** независимых места вызывают `Part.getTexture()` при записи
(`Part.save`, `Inventory.save` → `Part.save`, `Vehicle.saveSkin`).

### 3. Восстановленный путь загрузки

```java
// GameLogic.load: player.parts.load(local3) ...
// Inventory.load
int local2 = arg0.readInt();
while (local3 < local2) {
  int local4 = arg0.readInt();
  if (local4 == 0) { Part local5 = Part.createFromFile(arg0, player); addItem(local5); }
  else { InventoryItem_Folder local5_2 = InventoryItem_Folder.createFromFile(arg0, this); ... }
}

// CarLot.loadCar -> Vehicle.load -> Part.createFromFile -> Part.load
public static Part createFromFile(File arg0, GameRef arg1) {
  int local2 = arg0.readResID();
  if (local2) {
    int local3 = arg0.readInt();               // версия записи детали
    Part local5 = local4.create(arg1, new GameRef(local2), "100,0,0,0,0,0", "loaded carpart");
    local5.load(arg0);
    return local5;
  }
  return null;
}

// Part.load  <-- ГЛАВНОЕ МЕСТО
setTexture(arg0.readResID());   // <-- восстановление текстуры
setMesh(arg0.readResID());
setWear(arg0.readFloat()); setTear(arg0.readFloat());
while ((local3 = arg0.readInt()) != -1) {
  local2 = arg0.readString(); local5 = arg0.readInt();
  setSlotDamage(local3, local2);
  if (local5) { Part local6 = Part.createFromFile(arg0, getParent()); ... }
}
```

**Важно:** загруженная деталь создаётся через `GameRef.create(...)`, а не через
`addPart(..., Descriptor)`. В native `part_create`
(`src/resources/part_runtime.cpp:1192`) ставит `state.texture = cfg->texture`
(значение из SCX/конфига), после чего `Part.load` перекрывает его значением из
файла. `Descriptor` в пути загрузки сохранённой машины **не участвует вообще** —
это уточнение к §2 этапа 6, где загрузка была отождествлена с
`vehicle_spawn_stock()`.

`Descriptor` попадает в игру только там, где детали создаются впервые:

```java
// Vehicle ctor: заводской цвет по индексу из VehicleDescriptor
chassis.addStockParts(new Descriptor(GameLogic.CARCOLORS[arg2], arg3, arg4, arg5, arg6));
// CarSelect / Catalog: пересоздание стоковых деталей текущим цветом кузова
new Descriptor(player.car.chassis.getTexture(), Tunning, Tunning, 1, 1)
// Chassis.addStockParts(Descriptor): setWear/setTear/setTexture(arg0.color)
```

`Descriptor` — обычный класс с полем `int color` (не `HashMap`); он никогда не
сериализуется: ни один `save`/`load` его не читает и не пишет.

### 4. Формат `PlayerCar0` и фактический разбор файла

#### 4.1 Кодирование значений

`java.io.File` (`system/Scripts/io/File.class`, декомпилирован) объявляет
`write(int)/write(float)/write(ResourceRef)/write(String)` и соответствующие
`read*` как **native**. Реализация — `src/natives/natives_io.cpp:643-751`:

| Вызов | Байты |
|---|---|
| `write(int)` / `write(float)` | 4 байта, без тега типа |
| `write(String)` | `int32 len` (с `\0`), затем `len` байт |
| `write(ResourceRef)` | 4 байта resID, «перепрошитые» через `sdat_wire_resource` |

`sdat_wire_resource` (`natives_io.cpp:205`) кодирует паковый ресурс как
`(ordinal << 16) | local`, где `ordinal` — номер пака **в таблице этого файла**
(не `pack_id`), а `sdat_remap_resource` (`:248`) при чтении возвращает обычный
id пака. Именно это объясняет «pack-qualified» вид идентификаторов в
сохранении.

Контейнер: `SDAT`, версия `0x0100`, тип 2, payload с `+8`, трейлер
(unsaved-ресурсы + список паков) в конце (`natives_io.cpp:81-203`).

#### 4.2 Запись детали

Из `Part.save` + `Part.load` (см. §2–3) запись детали имеет вид:

```
[+0]  u32  part type id        (ResourceRef, typeof 8)
[+4]  i32  part save version   (= 1, SAVEFILEVERSION_PART)
[+8]  u32  TEXTURE             (ResourceRef; Part.getTexture())
[+12] u32  mesh                (ResourceRef)
[+16] f32  wear
[+20] f32  tear
[+24] ...  слоты: i32 slot, i32 nameLen + bytes, i32 childSlotID,
           и при childSlotID != 0 — рекурсивная запись детали
      i32 -1  конец слотов
[..]  хвост подкласса (см. §4.6)
```

**Поле текстуры детали — третье 32-битное слово записи, смещение `+8` от её
начала.** Это единственное место, куда попадает `Part.getTexture()`.

#### 4.3 Что реально лежит в `PlayerCar0`

Файл `save/carrer/1-1/PlayerCar0` (4016 байт, SDAT тип 2): payload
`0x08..0xed0`, таблица паков — 7 записей
(`cars\racers\Einvagen.rpk`, `cars.rpk`, `parts\wheels.rpk`, `parts.rpk`,
`parts\engines\Einvagen_Duhen_Ishima_Focer.rpk`, `...\Baiern_Emer.rpk`,
`parts\mufflers.rpk`).

Структурное сканирование (признак записи детали: `[typeof-8 res][1][res или 0]
[res или 0]`) нашло **75 записей деталей**, и все идентификаторы резолвятся в
имена ресурсов этих паков. Шапка:

```
0x08  fileid  0x88664422  (Vehicle.SAVEFILEID_VHC)
0x0c  version 5           (Vehicle.SAVEFILEVERSION_VHC)
0x10  part    Einvagen_110_GT   (cars\racers\Einvagen.rpk local 0x06)
0x14  version 1
0x18  TEXTURE 0x00020050 -> cars.rpk local 0x50 = "Saratoga.png" (typeof 7)
0x1c  mesh    Einvagen.rpk local 0x49
0x20  wear 1.0 / 0x24 tear 1.0
```

`Saratoga.png` — это `GameLogic.CARCOLORS[16]` (`cars:0x50r`,
`RID_CARCOLOR_Einvagen_Saratoga`). Тот же id стоит во всех покрашенных деталях
кузова: 13 из 75 записей имеют ненулевую текстуру, остальные 62 — `0`.

| Смещение записи | Смещение текстуры | Деталь (part type) | Значение текстуры |
|---|---|---|---|
| `0x0010` | `0x0018` | `Einvagen_110_GT` (шасси) | `0x00020050` Saratoga.png |
| `0x054c` | `0x0554` | `hood` | `0x00020050` |
| `0x0960` | `0x0968` | бампер `F` | `0x00020050` |
| `0x0a68` | `0x0a70` | `FL` | `0x00020050` |
| `0x0a9c` | `0x0aa4` | `FL` | `0x00020050` |
| `0x0ae8` | `0x0af0` | `L` | `0x00020050` |
| `0x0b14` | `0x0b1c` | `RL` | `0x00020050` |
| `0x0bc0` | `0x0bc8` | `FR` | `0x00020050` |
| `0x0bf4` | `0x0bfc` | `FR` | `0x00020050` |
| `0x0c40` | `0x0c48` | `R` | `0x00020050` |
| `0x0c6c` | `0x0c74` | `RR` | `0x00020050` |
| `0x0d18` | `0x0d20` | `R` | `0x00020050` |
| `0x0d58` | `0x0d60` | `R` | `0x00020050` |

(«Деталь» — `alias` ресурса типа 8, на который указывает первое слово записи;
имена `F/FL/FR/L/R/RL/RR` — это передний/задний бампер, двери и т. п.)

Конец файла сходится точно: последняя запись детали заканчивается на `0xea4`,
далее хвост `Chassis` версии 3 (`0xea4`: `3`, `0xea8`: `4` колеса, 4 строки
повреждений, `brake_balance = 0.5`, `mileage`), затем хвост `Vehicle`
(8 float + 3 int = 44 байта) до `0xed0`.

#### 4.4 Исправление выводов этапа 6 по смещениям

Этап 6 трактовал пять «полных Resource ID» как текстуры. Это неверно:

| Смещение из этапа 6 | Что это на самом деле | Текстура этой детали |
|---|---|---|
| `0x5b0` `0x05004a` | запись детали `Einvagen_alternator` (typeof 8) | `0x5b8 = 0` |
| `0x65c` `0x05004b` | `Einvagen_camshaft_bearing_...` | `0x664 = 0` |
| `0x6a8` `0x05004e` | `Einvagen_PerPro_GTA_fuel_rail` | `0x6b0 = 0` |
| `0x808` `0x060043` | `Baiern_Kraftwerk_clutch` | `0x810 = 0` |
| `0x8e4` `0x050049` | `Einvagen_oil_pan` | `0x8ec = 0` |

То есть эти значения — **part type id** (первое слово записи), а не текстура;
двигательные детали не красятся и пишут текстуру `0`. Вывод этапа 6 «в файле
лежат полные type-7 paint ID» верен только для записей кузовных деталей, где
такое значение стоит на позиции `+8`.

#### 4.5 Почему в файле смешаны ID паков и «голые» индексы

Открытый вопрос этапа 6 закрывается структурой, а не догадкой: ресурсными
(«pack-qualified») являются только значения, записанные через
`write(ResourceRef)` — part type, texture, mesh, id машины, скины. Всё
остальное — обычные `int`: индексы слотов, `childSlotID`, длины строк, версии,
флаги. Пустая строка повреждения пишется как `len = 0`, поэтому рядом с
индексами слотов идут нули.

#### 4.6 Хвосты подклассов

`Part.save` расширяется подклассами, поэтому плоский парсер файла невозможен
(совпадает с независимым наблюдением в `docs/EXTERNAL_RESEARCH.md`):

| Класс | Хвост после `-1` |
|---|---|
| `Part` (базовый) | нет |
| `Chassis` | `int 3`, `int wheels`, `wheel`×строка повреждения, `float brake_balance`, `float mileage` |
| `Tyre` | `int 1`, `float inflation` |
| `ShockAbsorber` | `int 2`, `float damping`, `float rebound_factor` |
| `Pneumatic_ShockAbsorber` | 4 float без версии |
| `TurboCharger` | `int 2`, `float P_turbo_waste` |
| `FuelInjectorSystem` | `int 1`, `float mixture_ratio`, `int fuel_type` |
| `NOSInjectorSystem` | `int 1`, `float nitro_consumption` |
| `Block` | `int 3`, 2 float, 4 float (`time_spark_*`) |
| `Camshaft` | `int 1`, `float advance` |
| `Canister` | `int 1`, `float actual` |
| `Transmission` | `int 2`, 8 float передач, `float end_ratio`, `float diff_lock`, `int drive_front` |
| `Set` | `String name`, `String description` |

Поэтому в §4.3 разбор ведётся структурным сканированием записей и вручную
проверенным путём шасси, а не «плоским» чтением всего файла.

### 5. Все обращения к `getTexture()` / `setTexture()`

Поиск по строковым таблицам всех 1569 `.class` каталога игры даёт пять классов, ссылающихся
на `getTexture`, и четыре — на `setTexture`:

| Класс | Метод | Что делает | Относится к |
|---|---|---|---|
| `Part` | `getTexture()I` / `setTexture(I)I` | native, `state.texture` | база |
| `Part` | `save(File)` | `getTexture()` → `write(ResourceRef)` | **сохранение** |
| `Part` | `load(File)` | `setTexture(readResID())` | **загрузка** |
| `Part` | `addStockParts(Descriptor)` / `addPart(...)` | `setTexture(arg.color)` | спавн |
| `Vehicle` | `saveSkin(String)` | `getTexture()` → пара (type, texture) | сохранение скина |
| `Vehicle` | `loadSkin(String)` | `setTexture(readResID())` | загрузка скина |
| `Chassis` | `addStockParts(Descriptor)` | `setTexture(arg0.color)` | спавн |
| `Painter` | `MODE_COPY` | `getTexture()` (источник) → `ResourceRef.duplicate` → `setTexture(dup.id())` | игровой процесс/UI |
| `Catalog` | `CMD_VEHI` | `new Descriptor(chassis.getTexture(), ...)` + `changeTexture` | UI/спавн |
| `CarSelect` | `CMD_VEH*` | `new Descriptor(chassis.getTexture(), ...)` + `command("texture <id> 1")` | UI/спавн |

Перед `saveCar()`/`player.parts.save()` никаких дополнительных вызовов
`getTexture()` нет: единственный, который исполняется в момент сохранения, —
внутри `Part.save`. Обращений, которые бы писали результат в `Descriptor`,
в пути сохранения не существует.

### 6. Три сценария

**A. Заводская машина.** `VehicleDescriptor.colorIndex` (файл `main`) →
`GameLogic.CARCOLORS[colorIndex]` → `new Descriptor(color, ...)` →
`Chassis.addStockParts` → `Part.setTexture(color)` + native `addStockParts`
(раскрашивает дочерние детали, `part_runtime.cpp:4545` пишет
`Descriptor["color"]` в каждую деталь). При сохранении та же величина
читается обратно `Part.getTexture()` и пишется в запись каждой детали.
Подтверждено: `main` содержит `colorIndex` (4 new + 4 used дескриптора,
`colorIndex` 1…20), `PlayerCar0` содержит `0x00020050` в 13 записях.

**B. Перекраска.** Два разных механизма:
`command("texture <pack id> 1")` → native `part_command` (`:1467`) →
`paint_tree` → `paint_one` → `state.texture = <pack id>` (id остаётся
паковым), и `Painter` `MODE_COPY` → `ResourceRef.duplicate` → **новый**
рантайм-ресурс (id с высокой половиной `0xFFFF`) → `setTexture`.
В сохрании обе ветки проходят один и тот же `Part.save`, но результат разный:
паковый id пишется как `(ordinal<<16)|local` и полностью восстанавливается, а
рантайм-ресурс кодируется как ordinal в таблице unsaved-блобов, причём
`sdat_wire_resource` создаёт блоб с **пустыми** байтами. При чтении
`sdat_remap_resource` вернёт сохранённый `abs_id`, который в новой сессии уже
не резолвится, и `Part.setTexture` отвергнет его (`typeof != 7`), оставив
текстуру из SCX.

**C. Только tint.** В Java у `Part` нет поля цвета: `PartRender::colors`
приходит из конфига детали, `n->color`/`instance.color` пересчитываются
(`paint_of()`, `part_runtime.cpp:456`). `Part.save` не пишет ничего похожего на
RGB. Тинт не сохраняется и не может конкурировать с текстурой: это разные поля
разных объектов.

### 7. Две схемы

**Сохранение**

```
runtime Part (Record::state.texture)
  --[декомп. VM, Part.getTexture()]--> Java int id
  --[декомп. VM, Part.save: write(new ResourceRef(id))]--> 4 байта в потоке
  --[декомп. VM, Chassis/Part.save-рекурсия, Inventory.save, Vehicle.saveSkin]-->
  --[исходник host, java.io.File.write(ResourceRef) + sdat_wire_resource]-->
  --[исходник host, sdat_frame]--> PlayerCar0 / main / save/skins/*
```

**Загрузка**

```
файл
  --[исходник host, sdat_take_payload]--> payload + таблица паков
  --[декомп. VM, Vehicle.load -> Part.createFromFile -> readResID]--> id
  --[декомп. VM, Part.load: setTexture(readResID())]--> вызов native
  --[исходник host, part_runtime.cpp:4978: проверка typeof==7]-->
     Record::state.texture + node->paint_texture
  --[исходник host, scene_instances.cpp]--> paint_slot/paint_texture/painted
```

### 8. Таблица переходов

| Переход | Статус |
|---|---|
| `Part.save` вызывает `getTexture()` | **подтверждено декомпиляцией VM** (+ совпадение с байтами файла) |
| `Part.save` пишет `ResourceRef` = 4 байта resID | подтверждено декомпиляцией VM + `natives_io.cpp:667` |
| Текстура детали = слово `+8` записи | **подтверждено файлом**: 13 записей резолвятся в `Saratoga.png` |
| `Chassis.save` = `super.save` + хвост v3 | подтверждено декомпиляцией + проверено на `0xea4` |
| `Vehicle.save` = id, версия, `chassis.save`, 8 float, 3 int | подтверждено: конец `PlayerCar0` сходится в `0xed0` |
| `Inventory.save` → `Part.save` | подтверждено декомпиляцией |
| `Part.load` → `setTexture(readResID())` | подтверждено декомпиляцией |
| `Descriptor` участвует в сохранении | **опровергнуто**: ни один save/load его не читает |
| `Descriptor.color` = `chassis.getTexture()` | подтверждено декомпиляцией (`Catalog`, `CarSelect`) — только для спавна/UI |
| `colorIndex` в `main` — отдельный механизм | подтверждено: 8 дескрипторов с `colorIndex` в `main` |
| round trip пакового type-7 id | **подтверждено структурно** (симметричные write/read + резолв в `Saratoga.png`) |
| round trip рантайм-текстуры (`0xFFFFxxxx`) | **не подтверждён**: хост пишет пустой блоб; оригинальная игра по внешним данным хранит байты (`kind 7`) — не проверено |
| сохранение тинта | опровергнуто (в Java-состоянии и в `Part.save` тинта нет) |
| `Painter.paintPart`/`doPaint` меняют `state.texture` в OpenSLRR | **невозможно** в этом хосте: native не зарегистрированы (fail-closed throw) |

### 9. Ответы на восемь вопросов

1. **Вызывает ли Java `Part.getTexture()` при сохранении машины?**
   **Да.** В `Part.save`, третьим оператором после записи part type id и версии.
   Этот же метод обслуживает `Inventory.save` и дополнительно
   `Vehicle.saveSkin`. Путь сохранения машины:
   `GameLogic.save → CarLot.saveCar → Vehicle.save → Chassis.save → Part.save`.

2. **Записывает ли она результат в `Descriptor.fields["color"]`?**
   **Нет.** Результат пишется прямо в файл как `ResourceRef`. `Descriptor`
   (в этой сборке VM это класс с полем `int color`, без `HashMap fields`) в
   сохранении не участвует вообще: он конструируется только при первичном
   создании деталей (`Vehicle`, `Chassis.addStockParts`, `Catalog`,
   `CarSelect`) и никогда не сериализуется. Native-путь `Descriptor["color"]`
   (`part_runtime.cpp:4545` → `:570`) — это спавн, а не сохранение.

3. **Сериализуется ли это поле?**
   Сериализуется не поле `Descriptor`, а само значение: третье 32-битное слово
   каждой записи детали (`+8` от начала записи). Надёжный контракт — запись
   `Part.save`: `[partType][version=1][texture][mesh][wear][tear][slots…][-1]`.

4. **Какое конкретно поле `PlayerCar0` соответствует текстуре детали?**
   Слово `+8` записи этой детали. Проверено: шасси `0x10` → текстура `0x18 =
   0x00020050` ∈ `cars.rpk` (`Saratoga.png`, `CARCOLORS[16]`), `hood 0x54c →
   0x554`, бамперы и двери `0x960…0xd58`. Всего 75 записей, 13 с текстурой.
   Смещения этапа 6 (`0x5b0`, `0x65c`, `0x6a8`, `0x808`, `0x8e4`) — это
   **part type id**, а не текстуры; см. §4.4.

5. **Восстанавливается ли ID при загрузке?**
   **Да:** `Part.createFromFile` читает тип, `Part.load` первым действием
   вызывает `setTexture(readResID())`, native проверяет `typeof == 7` и ставит
   `state.texture` + `node->paint_texture`. `0` означает «оставить текстуру из
   SCX» (native отвергает нераскрывающийся id).

6. **Переживает ли `Part.setTexture()` сохранение и повторную загрузку?**
   Для id, указывающего на ресурс пака, — **да**, и это подтверждено
   структурно. Для рантайм-текстуры (`ResourceRef.duplicate` в `Painter`, id с
   высокой половиной `0xFFFF`) — **нет** в OpenSLRR: в SDAT пишется только
   ordinal и пустой блоб, байты изображения не сохраняются, а remap вернёт id,
   который уже не резолвится. Оригинальная игра, по внешним данным
   (`EXTERNAL_RESEARCH.md`: unsaved-ресурсы `kind 7` = image bytes), хранила
   байты — это остаётся непроверенным.

7. **Если round-trip не доказан, где разрыв?**
   Разрыв не в native и не в порядке вызовов Java: `getTexture()` в
   сохранении есть, `setTexture()` при загрузке есть. Разрыв — в судьбе
   **рантайм-созданных** текстур: (а) `sdat_wire_resource` создаёт блоб без
   байтов, (б) `Painter.paintPart`/`doPaint` в этом хосте не зарегистрированы
   (исключение вместо вызова), поэтому spray-покраска не может изменить
   `state.texture`. Для паковых id разрыва нет.

8. **Какие данные можно считать надёжным контрактом сохранения?**
   `Record::state.texture` (uint32, type-7 resID) ↔ слово `+8` записи
   `Part.save`, пер-деталь, независимо от `Descriptor`. Не контракт:
   `Descriptor` как объект, `paint_texture`/`paint_source`/`paint_slot`
   (пересчитываются), тинт (`PartRender::colors`, из конфига),
   рантайм-текстуры без пакового id. Отдельный, независимый механизм —
   `VehicleDescriptor.colorIndex` в `main` (цвет для рынка/каталога), который
   на текстуры уже собранной машины не влияет.

### 10. Что это меняет в картине (без правок кода)

- Сохранение деталей делает **Java**, а не native; native только отдаёт и
  принимает `state.texture` (`part_runtime.cpp:4975/4978`). Прежняя формулировка
  «native не пишет обратно в `Descriptor`» верна, но неполна: native и не
  должен — Java пишет в файл напрямую из `getTexture()`.
- `Descriptor` — не контракт сохранения, а конструктор спавна. Любая новая
  система материалов, которая попытается «жить в `Descriptor`», не будет
  сохранена.
- Единственная точка, через которую проходит персистентность текстуры, —
  4 байта `ResourceRef` в записи детали. Всё остальное (слот, тинт, путь)
  выводится заново.
- Практическое следствие: покраска, дающая паковый type-7 id, переживает
  сохранение; покраска, создающая рантайм-текстуру, в этом хосте — нет.

Документ оставлен незакоммиченным; коммит не создавался.

---

## Этап 8 — Painter и жизненный цикл динамических текстур

**Проверяемый HEAD:** `06ce08ec75fe2882ee1d4c3c311989ec28678eb8`.
Аудит read-only: C++, Java, игровые ресурсы, RPK/SCX и сохранения не
изменялись, игра не запускалась, скины и текстуры не создавались, native-методы
не реализовывались, предыдущие разделы не переписывались. Документ остаётся
незакоммиченным.

Метод: декомпиляция классов игры (этап 7) + чтение native-кода OpenSLRR.
Ниже явно разделены три уровня: **Java-логика игры**, **native bindings хоста**
и **фактически исполнимая в хосте реализация**.

### 1. `ResourceRef` и динамические идентификаторы

#### 1.1 Java-контракт (`system/Scripts/util/resource/ResourceRef.class`)

```java
ResourceRef()            { super(); newNative(); }
ResourceRef(int arg0)    { this(); set(arg0); }
ResourceRef(ResourceRef) { this(); set(arg0); }

native void newNative();  native void deleteNative();
native void set(int);     native void set(ResourceRef);
native int  type();       native int  id();
native void destroy();    native void load();  native void unload();
native void cache();      native void precache();
native void makeTexture(ResourceRef parent, String file);
native void makeSound(ResourceRef parent, String file);
native void duplicate(ResourceRef arg0);
```

Типы ресурсов заданы константами того же класса: `RESOURCE_TEXTURE = 7`,
`RESOURCE_MESH = 5`, `RESTYPE_GAME = 8`.

#### 1.2 Что реально делает хост (`src/natives/natives_resref.cpp`)

| Метод | Строка | Реализация в OpenSLRR |
|---|---|---|
| `set(int)` | `:200` | если `gameref_find_live(id)` (id ≥ `0x70000000`) — связать с живым объектом и записать `live_id`; иначе `type_id = id`, `rpk.resolve(id)` → `typeof`/alias, `fields["id"] = id` |
| `set(ResourceRef)` | `:234` | копирует `live_id` либо `type_id`/`fields["id"]` источника |
| `id()` | `:363` | `live_id` → иначе `type_id` → иначе `fields["id"]` → иначе `0` |
| `type()` | `:376` | `fields["typeof"]` |
| `newNative()` | `:183` | создаёт `GameRefState`, **id не выдаёт** |
| `destroy()` | `:333` | `gameref_remove`, у части — `part_destroy` |
| `makeTexture(parent,file)` | `:472` | **только** нормализует путь и кладёт его в `fields["_texpath"]`; id, `typeof` и пиксели не появляются |
| `duplicate(src)` | `:528` | **копирует ссылку, а не ресурс**: `fields["id"] = src.type_id / src.fields["id"]`, копирует `_mw/_mh/_tid/_texpath` и `texture_replacements` |
| `makeSound(parent,file)` | `:492` | единственный путь, который выдаёт живой id: `gameref_make_live` |

#### 1.3 Кто выдаёт «динамические» ID

- В OpenSLRR живой (runtime) id выдаёт `gameref_make_live`
  (`src/resources/gameref.cpp:98`), и нумерация начинается с
  **`0x70000000`** (`:27`), а не с `0xFFFFxxxx`.
- `gameref_find_live` (`:94`) признаёт живыми только id `≥ 0x70000000`.
- Поиск по всему `src/` не находит ни одного места, которое порождало бы
  значение с высокой половиной `0xFFFF`: этот диапазон встречается только в
  SDAT-кодере (`natives_io.cpp:205-247`) как **маркер «временный ресурс»**
  при записи.
- Следствие: утверждение «`ResourceRef.duplicate` создаёт runtime ID вида
  `0xFFFFxxxx`» для текущего хоста **неверно**. `duplicate` вообще ничего не
  создаёт. `0xFFFFxxxx` — это формат SDAT для временных ресурсов, а не
  диапазон, который хост выдаёт.

#### 1.4 Что происходит при записи такого ID в `Part.save()`

`write(ResourceRef)` (`natives_io.cpp:667-692`) берёт **не** `id()`, а поле
`fields["id"]` (`res_id_of`, `natives_boot.cpp:102`), затем:

```cpp
int32_t wired = id;
if (it->second.sdat && !sdat_wire_resource(rpk, it->second, id, restype, &wired)) {
  std::printf("[sdat] resource 0x%08x has no pack\n", ...);
  return Value::make_int(0);          // НИ ОДНОГО байта не записано
}
file_push(self, &wired, 4);
```

`sdat_wire_resource` (`:205`) различает три случая:

| id | Запись |
|---|---|
| `0` | `0` |
| `(id & 0xFFFF0000) == 0xFFFF0000`, `typeof` 5 или 7 | номер blob’а в трейлере (без сдвига); blob создаётся с **пустыми** `bytes` |
| иначе | паковый id: `(ordinal << 16) \| local`; паков должно быть найдено по `pack_by_id` |

Живой id `0x70000001` попадает в третий случай с `pack_id = 0x7000`, пак не
находится → `write` возвращает `0`, **не сдвинув поток**. Это не «сохранение
битого значения», а потеря 4 байт: последующие поля записи читались бы со
сдвигом. В текущем коде это недостижимо через `Part.setTexture` (см. §1.6), но
достижимо через `command("texture <id> 1")` (см. §1.6, `paint_one`).

Обратное чтение (`sdat_remap_resource`, `:248`): для временного id ищется blob
с тем же ordinal и **непустым** `abs_id`; при чтении файла `abs_id` никогда не
заполняется (в `SdatBlobRef` его заполняет только запись), поэтому результат —
`0`. Байты blob’ов читаются в память (`sdat_take_payload`, `:141`), но **нигде
не потребляются**: ни один код не превращает их в текстуру.

#### 1.5 Где хранится содержимое динамической текстуры

| Носитель | В оригинале (по внешнему корпусу) | В OpenSLRR |
|---|---|---|
| пиксели/изображение | SDAT type-2 trailer, unsaved-ресурсы, `kind 7` = image bytes (`docs/EXTERNAL_RESEARCH.md:135-141`) | нигде: `makeTexture` хранит только путь `_texpath` |
| путь к файлу | не установлено | `fields["_texpath"]`, потребляется **только OSD** (`natives_renderref.cpp:357` — иконка виджета) |
| runtime-идентификатор | `0xFFFFxxxx` (внешний корпус) | `0x70000000+`, только для живых объектов, не для текстур |

Проверка по факту: в установке нет ни одного сохранения с непустым
blob-разделом (`save/carrer/1-1/main`, `PlayerCar0`, `save/temp/PlayerCar0` —
`blobs=0`), каталога `save/skins/` не существует вовсе, файлов скинов нет.
Поэтому формат временных ресурсов проверяем только по коду, а не по данным.

#### 1.6 Принимает ли `Part.setTexture()` динамический ID

`part_runtime.cpp:4979-4996`:

```cpp
const auto id = static_cast<uint32_t>(number(a, 0));
const RpkEntry *entry = id && index ? index->resolve(id) : nullptr;
if (!entry || entry->typeof_ != 7) return Value::make_int(old);   // отказ
p->state.texture = static_cast<int>(id);
for (auto *render : p->renders) node->paint_texture = p->state.texture;
```

`RpkIndex::resolve` (`rpk_index.cpp:318`) берёт `pack_from_full = id >> 16` и
ищет пак по id; для `0x7000xxxx` и `0xFFFFxxxx` пака нет → `nullptr` → отказ,
`state.texture` не меняется. Дополнительно отказ, если у детали вообще нет
`config->texture`.

Важное исключение: команда `texture <id> <mode>` (`part_runtime.cpp:1467`)
идёт не через `setTexture`, а через `paint_one` (`:1307`), где проверки типа
**нет**:

```cpp
void paint_one(Object *part, uint32_t texture) {
  auto *rec = record(part);
  if (!rec || !rec->config->texture) return;
  rec->state.texture = texture;                 // без resolve и typeof
  for (auto *render : rec->renders) ...->paint_texture = texture;
}
```

То есть единственный путь, которым невалидный (в том числе живой) id может
попасть в `state.texture` и затем в `Part.save`, — это `paint_tree`/`paint_one`.
Именно им пользуются `Catalog`/`CarSelect`, но передают туда настоящие паковые
id (`GameLogic.CARCOLORS[...]`).

### 2. Логика Painter

#### 2.1 Состояние

| Поле | Тип | Назначение |
|---|---|---|
| `mode` | int | `MODE_NONE/PAINTCOLOR/PAINTPART/PAINTDECAL/COPY` |
| `overVehicle` | int | курсор над деталью (ставится только по `EC_HOVER`) |
| `paintCans` | `PaintInventory` | список банок; `paintColor`, `lastCanId` |
| `paintBrush` | `ResourceRef` | кисть: `PAINTBRUSH` (`misc.garage:0x102r`) или `Decal` |
| `copy_texture` | **int** | id текстуры-источника в `MODE_COPY` |
| `brushColor` | int = `-1` | аргумент `doPaint` в режиме декалей |
| `paintCursor` | `GameRef` | деталь, по которой ведётся рисование |
| `decalRotation`, `decalSize = 0.7`, `decalFlip` | | параметры штампа |

`PaintCan` (`sl/Scripts/game/PaintCan.class`) — чистые данные:
`int color; float capacity;`. `capacity` в декомпилированном Java **нигде не
читается и не уменьшается**, `PaintInventory` пересоздаёт ~40 банок в
конструкторе Painter.

#### 2.2 Восстановленная последовательность

**Вход в режим** — `Painter.show()`:

```java
changeMode(lastPaintMode);
paintCans.paintColor = paintCans.getCanbyIndex(paintCans.lastCanId).color;
Input.cursor.addHandler(this);            // java: addNotification(cursor, EVENT_CURSOR, EVENT_SAME)
setEventMask(GameRef.EVENT_CURSOR);       // 65536
```

`changeMode(MODE_PAINTCOLOR)` дополнительно ставит `paintBrush = PAINTBRUSH`
(`misc.garage:0x102r`).

**Выбор цвета**: `PaintInventory.panelLeftClick(idx)` → `lastCanId = idx`,
`paintColor = getCanbyIndex(idx).color`.

**Определение точки на поверхности**: `Input.cursor.getPickedPos()`
(`java.io.MouseCursor`, native; в хосте реализован настоящим луч-пиком через
`scene_instances_pick`, `natives_io.cpp:766-793`).

**Наведение**: `handleEvent(EVENT_CURSOR, "15 0 <gameRefId> -1")` → `EC_HOVER`:
`local8_2.getScriptInstance() instanceof Part` → `overVehicle = 1`; уход с
детали → `overVehicle = 0`, и в режиме декалей `endPaint(); xPaint(player.car)`.

**Нажатие/рисование**:

| Режим | Событие | Вызов (точные аргументы) |
|---|---|---|
| `MODE_PAINTCOLOR` | `EC_LDOWN` при `overVehicle` | `SfxSPRAY.play(getPickedPos(), …)`, `beginPaint(local6)` → `paintCursor = new GameRef(id)`, `enableControlHook()` (`registerCallback(GII_CONTROL)`) |
| `MODE_PAINTCOLOR` | каждый кадр `control(float dt)` | `doPaint(paintCursor, paintColor \| 0x66000000, PAINTBRUSH.id(), 0, decalRotation, decalSize, decalFlip)` |
| `MODE_PAINTCOLOR` | `EC_LUP` | `SfxSPRAY.stop`, `endPaint()` → `disableControlHook()`, `paintCursor = null` |
| `MODE_PAINTPART` | `EC_LCLICK` | `paintPart(new GameRef(local6), paintColor \| 0xFF000000)` |
| `MODE_PAINTDECAL` | `EC_LCLICK` | `doPaint(new GameRef(local6), brushColor, paintBrush.id(), 0, decalRotation, decalSize, decalFlip)`; затем `--((Decal)paintBrush).stickies` |
| `MODE_COPY` | 1-й `EC_LCLICK` | `copy_texture = part.getTexture()`; превью: `copy_prev.changeTexture(new ResourceRef(copy_texture))` |
| `MODE_COPY` | 2-й `EC_LCLICK` | `ResourceRef r = new ResourceRef(); r.duplicate(new ResourceRef(copy_texture)); part.setTexture(r.id())` |

Ключевое: **в режимах рисования Java не создаёт текстуру и не вызывает
`setTexture`**. Ни `paintPart`, ни `doPaint` не возвращают значение; `paintCursor`
— это `GameRef` детали. Значит, создание/изменение текстуры и её назначение
детали целиком находятся **внутри native** `doPaint`/`paintPart`, и
останавливаться нужно именно там:

- `Painter.paintPart(GameRef arg0, int arg1)` — `arg1` = цвет `0xFFBBGGRR`, не id;
- `Painter.doPaint(GameRef arg0, int arg1 /*цвет|альфа*/, int arg2 /*id кисти*/,
  int arg3 /*режим 0/1*/, float rot, float size, int flip)`;
- `Painter.xPaint(GameRef arg0)` — вызывается для `player.car` при уходе курсора
  с машины в режиме декалей (вероятно, фиксация/сброс нарисованного; точный
  смысл без реализации неизвестен).

`MODE_COPY` — единственное место, где Java сама связывает текстуру с деталью, и
там используется `duplicate` + `setTexture`. В хосте `duplicate` возвращает тот
же id (§1.2), поэтому «копирование» свелось бы к присваиванию уже имеющегося id.

Две аномалии декомпиляции, которые нельзя достраивать:
`brushColor` инициализирован `-1` и передаётся в `doPaint` как цвет;
`paintBrush.id()` для декаля — это id ресурса-декаля, а у `Decal(String)`
он образуется через `makeTexture` (в хосте — `0`, см. §2.3).

#### 2.3 Откуда берутся кисти-декали

```java
// Decal.class
public Decal(String path) { super(); makeTexture(new ResourceRef(system:0x8r), path); }
public Decal(int id)      { super(id); }
int stickies = 5;
```

`Catalog.collectDecals(pack)` создаёт `new Decal("decals/textures/catalog/<pack>/<file>.png")`
для каждого PNG, а покупка добавляет `new Decal(<id>)` в `player.decals`.
То есть декаль — это `makeTexture`-ресурс, созданный из файла: в оригинале это
и есть источник «динамической» текстуры с собственным id. В OpenSLRR
`makeTexture` не выдаёт id, поэтому `Decal.id() == 0`, `type() == 0`, а путь
живёт в `_texpath` и используется только для OSD-иконок.

### 3. Native bindings и ограничения host

#### 3.1 Классификация методов, критичных для Painter

| Java-класс | Метод (сигнатура) | Место вызова | Статус в OpenSLRR | Доказательство |
|---|---|---|---|---|
| `java.game.Painter` | `doPaint(GameRef,int,int,int,float,float,int)V` | `handleEvent`, `control` | **не зарегистрирован** | нет в `src/natives/*`; grep по `doPaint` пуст |
| `java.game.Painter` | `paintPart(GameRef,int)V` | `EC_LCLICK` | **не зарегистрирован** | — |
| `java.game.Painter` | `xPaint(GameRef)V` | `EC_HOVER` уход | **не зарегистрирован** | — |
| `java.game.Part` | `getTexture()I` / `setTexture(I)I` | Painter, Part.save/load | **реализованы** | `part_runtime.cpp:4975/4979` |
| `java.util.resource.ResourceRef` | `duplicate(ResourceRef)V` | `MODE_COPY` | зарегистрирован, **ограничен**: копия ссылки | `natives_resref.cpp:528` |
| `java.util.resource.ResourceRef` | `makeTexture(ResourceRef,String)V` | `Decal(String)` | зарегистрирован, **ограничен**: только путь | `natives_resref.cpp:472` |
| `java.util.resource.ResourceRef` | `id()I`, `type()I`, `set`, `destroy` | повсеместно | реализованы | `natives_resref.cpp:200-400` |
| `java.io.MouseCursor` | `getPickedPos()LVec3;` | `MODE_PAINTCOLOR` | реализован (луч-пик) | `natives_io.cpp:766` |
| `java.io.MouseCursor` | `addHandler(GameType)V` | `show()` | **Java-метод** → `addNotification(cursor, EVENT_CURSOR, …)` | `MouseCursor.java:112` |
| `java.render.Rectangle` | `changeTexture(ResourceRef)V` | превью копии | **Java-метод** (пересоздаёт `RenderRef`) | `Rectangle.java:23` |
| `java.io.File` | `open/close/write/readInt/readResID/readString/exists/delete` | `Vehicle.saveSkin/loadSkin` | реализованы | `natives_io.cpp:572-751` |
| `java.game.Vehicle` | `saveSkin/loadSkin/fileCheck_Skin` | `Painter.osdCommand` | **Java-методы** | `Vehicle.java:578/623/…` |

Терминология из задания «`ResourceRef.changeTexture`» не соответствует коду:
`changeTexture` объявлен только у `java.render.Rectangle` (и у OSD-виджетов,
которые его используют). У `ResourceRef` такого метода нет.

#### 3.2 Что именно происходит с незарегистрированным native

Это уточнение к этапу 7 (там сказано «fail-closed throw»). Точная механика:

- `NativeRegistry::invoke` действительно бросает `Missing native: …`
  (`native_registry.cpp:60-70`), но это внутренний путь.
- Вызов из интерпретатора сначала проверяет наличие
  (`native_has_alias`), и **если метода нет вообще**, выполняется
  `jvm.cpp:894` / `:1008`:

  ```cpp
  if (halt_on_unbound_ && is_native) halt_unbound(owner, method, jni);
  std::fprintf(stderr, "[jvm] native/empty TREE without host binding %s.%s %s (from %s)\n", …);
  return Value::make_void();
  ```

- `halt_on_unbound_` включён **только** на время `Init(int)` (`boot.cpp:156`)
  и выключается сразу после (`:163`).

Итог для Painter: `paintPart`, `doPaint`, `xPaint` в рабочей игре — это
**залогированный no-op с возвратом `void`**, а не исключение. Java-код
продолжает выполняться, эффекта нет.

#### 3.3 Блокировка наступает раньше paint-вызовов

События курсора в хост-цикле формируются только для механика: `mainloop.cpp:326`
эмитит `EVENT_CURSOR` (0x10000) со строкой `"5 …"/"15 …"` **на объект детали**
и напрямую вызывает `handleEvent` механика, минуя систему подписок. Для
`Input.cursor` (объекта-курсора) такие события не эмитятся нигде;
`include/slrr/osd_registry.hpp:155` прямо фиксирует: «EC_HOVER is not
dispatched».

Поэтому `Painter.handleEvent` в хосте не вызывается вообще: `overVehicle`
остаётся `0`, `beginPaint` не выполняется, `control()` не регистрируется,
`paintPart`/`doPaint` не вызываются ни разу. Даже если бы вызывались — они
no-op (§3.2).

Работает в хосте из этого блока: открытие киоска покраски (`Garage` создаёт
`new Painter(...)`, `Garage.java:232`), выбор банки (чистый Java), превью
копии (`Rectangle.changeTexture`), файловые диалоги скинов.

### 4. `saveSkin` / `loadSkin`

**`Painter.saveSkin()` не существует.** В `Painter` есть только обработчик OSD:

```java
if (arg0 == Painter.CMD_SAVESKIN) {                       // Painter.java
  VehicleSkinFileReqDialog d = new VehicleSkinFileReqDialog(
      player.controller, DF_FREEZE|DF_MODAL|DF_DEFAULTBG|FRF_SAVE,
      "SAVE PAINT JOB", "SAVE", GameLogic.skinSaveDir, "*");
  if (d.display() == 0) {
    String path = GameLogic.skinSaveDir + d.fileName;
    if (File.exists(path)) { File.delete(path); File.delete(path + ".*"); }
    player.car.saveSkin(path);                            // -> Vehicle.saveSkin
    new WarningDialog(..., "Paint job has been saved.").display();
  }
}
```

- `GameLogic.skinSaveDir = "save/skins/"` (`GameLogic.java:70`).
- Имя файла — то, что ввёл пользователь; расширение **не добавляется**.
  Предустановленной маски нет (`mask = "*"`), список для загрузки фильтруется
  валидатором `VehicleSkinFileReqDialog.validator` → `Vehicle.fileCheck_Skin`.
- Строка `File.delete(path + ".*")` удаляет возможные sidecar-файлы
  (`save/skins/<name>.*`) — намёк на сопутствующие файлы, но сам
  `Vehicle.saveSkin` пишет **один** файл.

**Формат файла** (`Vehicle.saveSkin`, `Vehicle.java:623`):

```java
local2.write(Vehicle.SAVEFILEID_SKIN);       // -2108385177
local2.write(Vehicle.SAVEFILEVERSION_SKIN);  // 0
while ((local3 = iterateParts())) {          // обход всего дерева деталей
  local4 = local3.getTexture();
  if (local4) {
    local2.write(new GameRef(local3.getInfo(GameType.GII_TYPE)));  // тип детали
    local2.write(new ResourceRef(local4));                         // id текстуры
  }
}
local2.write(0);                             // терминатор
```

Содержимое: только пары «тип детали → id текстуры», без мешей, износа,
повреждений, состояния слотов, без идентификатора машины и **без изображения**.
Файл оборачивается в SDAT type 2 теми же `java.io.File`-native, что и
сохранения, поэтому id проходит через `sdat_wire_resource`.

**Загрузка** (`Vehicle.loadSkin`): для каждой пары ищет **первую ещё не
тронутую** деталь с данным `GII_TYPE` и вызывает `setTexture(id)`; счётчик
`local4[]` в декомпиляции — артефакт, реальная семантика — «по одной детали на
пару».

**Может ли `saveSkin` сохранить результат динамического рисования?** Нет:
он сохраняет только `getTexture()`, то есть id. Если id паковый — файл
пригоден (и это работает в хосте). Если id временный — в хосте он либо
попадёт в пустой blob (для `0xFFFFxxxx`, чего хост не порождает), либо
`write(ResourceRef)` не запишет 4 байта и сдвинет поток (для `0x7000xxxx`).
Байты изображения `saveSkin` не пишет никогда — ни в оригинале, ни в хосте:
в декомпилированном коде нет ни одного вызова, способного выгрузить пиксели.

**Состояние в установке:** каталога `save/skins/` нет, файлов скинов нет,
blob-разделы во всех сохранениях пусты — проверить формат «живого» скина на
данных невозможно.

### 5. `Vehicle.saveSkin` против обычного сохранения

| | `GameLogic.save()` → `PlayerCarN` + `main` | `Vehicle.saveSkin(path)` | `Painter.saveSkin()` |
|---|---|---|---|
| Существует | да | да | **нет**, есть OSD-команда `CMD_SAVESKIN`, вызывающая `Vehicle.saveSkin` |
| Кто вызывает | `autoSaveQuiet`, выход из карьеры, `Garage` | только `Painter` (CMD_SAVESKIN) | — |
| Файл | `save/carrer/<name>-<club>/main`, `PlayerCarN` через `save/temp` | `save/skins/<имя>` (расширение не навязывается) | — |
| Заголовок | `SAVEFILEID_MAIN` v21 / `SAVEFILEID_VHC` v5 | `SAVEFILEID_SKIN` v0 | — |
| Сериализатор деталей | `Part.save` (рекурсивно: тип, версия, текстура, меш, wear, tear, слоты, хвост подкласса) | свой цикл: пара (тип, текстура) | — |
| Что о текстуре | id в записи каждой детали | id в паре | — |
| Изображение | нет | нет | — |
| Меши/износ/слоты | да | нет | — |
| Привязка к конкретной машине | по каталогу сохранения | нет (карта типов деталей) | — |
| Восстановление | `Vehicle.load → Part.load → setTexture` | `loadSkin → setTexture` по типу | — |
| Работает в хосте | да (для паковых id) | да (для паковых id) | вызов проходит, результат тот же |

Три «механизма» из задания — это фактически **два сериализатора** и **одна
UI-обёртка**: `Painter.saveSkin` не является отдельным форматом.

### 6. Таблица персистентности

Столбец `Painter.saveSkin` = обёртка `CMD_SAVESKIN` → `Vehicle.saveSkin`.

| Данные | Runtime-хранение | `Part.save` | `Vehicle.saveSkin` | `Painter.saveSkin` | Восстановление |
|---|---|---|---|---|---|
| RPK texture ID (type 7) | `Record::state.texture` + `node->paint_texture` | **да**: `getTexture()` → `ResourceRef` (`part_runtime.cpp:4975`, `natives_io.cpp:667`) | **да**: пара (тип, id) | да (через тот же вызов) | **да**: `Part.load` → `setTexture`; `loadSkin` → `setTexture` |
| Dynamic `0xFFFFxxxx` | хост такие id не порождает (**не поддерживается**) | только как ordinal пустого blob’а (`natives_io.cpp:213-226`) — значение не восстанавливается | то же | то же | **нет**: `sdat_remap_resource` → `0` (`:248`) |
| Живой id `0x70000000+` | `GameRefState.live_id` (`gameref.cpp:98`) | `write` вернёт 0 без записи байт → сдвиг потока (**не поддерживается**) | то же | то же | невозможно (id умирает с объектом) |
| Содержимое изменённой текстуры (пиксели) | в хосте не хранится нигде | нет | нет | нет | **нет** |
| PaintCan `color` / `capacity` | `Painter.paintCans` (Java, ~40 банок в конструкторе) | нет | нет | нет | **нет**: список пересоздаётся; `capacity` вообще не читается |
| Данные декалей (id + `stickies`) | `Player.decals` (`Vector` из `Decal`) | нет | нет | нет | только через `GameLogic.save` → `main` (`write(Decal)` + `stickies`); в хосте id декали = `0`, т.к. `makeTexture` не выдаёт id |

Легенда: «да» — подтверждено кодом (Java + native) и структурой файла;
«нет»/«не поддерживается» — подтверждено отсутствием пути в коде.

### 7. Ответы на восемь вопросов

1. **Как Painter создаёт или получает динамическую текстуру?**
   Java **не создаёт** её. В `MODE_PAINTCOLOR`/`PAINTPART`/`PAINTDECAL` вызовы
   `doPaint`/`paintPart` получают `GameRef` детали и цвет (или id кисти), а
   создание/изменение текстуры целиком внутри native. В `MODE_COPY` текстура
   берётся у детали через `getTexture()`, «дублируется»
   (`ResourceRef.duplicate`) и присваивается другой детали через `setTexture`.
   Кисти-декали создаются как `Decal(path)` → `makeTexture(system:0x8r, path)`.

2. **В каком виде она хранится в runtime?**
   В оригинале — как ресурс с собственным id (в файлах это транзиентный
   `0xFFFFxxxx`), плюс `Record::state.texture` у детали и `paint_texture` на
   узле рендера. В OpenSLRR «динамической текстуры» как ресурса нет: `makeTexture`
   хранит **путь** в `fields["_texpath"]` (потребляется только OSD), а живые id
   (`0x70000000+`) выдаются объектам, не текстурам.

3. **Как она передаётся детали?**
   Режимы рисования — только внутри native `doPaint`/`paintPart` (Java не
   вызывает `setTexture`). Режим копирования — `part.setTexture(r.id())` после
   `duplicate`. Паковые цвета — `command("texture <id> 1")` → `paint_tree`.

4. **Принимает ли `Part.setTexture()` динамический ID?**
   В OpenSLRR — **нет**: требуется `rpk.resolve(id) != null && typeof_ == 7`
   (`part_runtime.cpp:4979-4990`); `0x7000xxxx`/`0xFFFFxxxx` не разрешаются.
   Для оригинала это не доказано: проверка живёт в коде хоста. Обходной путь в
   хосте — `paint_one` через `command("texture …")`, где валидации нет.

5. **Что происходит с динамическим ID при `Part.save()`?**
   `write(ResourceRef)` берёт `fields["id"]`. `0xFFFFxxxx` → номер blob’а с
   пустыми байтами (значение не переживёт загрузку); `0x7000xxxx` → запись
   **не выполняется вообще**, поток укорачивается на 4 байта; неизвестный
   паковый id — то же самое (`"[sdat] resource 0x… has no pack"`).

6. **Может ли `Painter.saveSkin()` сохранить содержимое текстуры отдельно?**
   Нет. `Vehicle.saveSkin` пишет только пары «тип детали → id текстуры».
   Отдельного файла изображения нет; `File.delete(path + ".*")` в обработчике
   намекает на возможные sidecar-файлы, но ни один код их не создаёт.

7. **Работает ли этот механизм в текущем OpenSLRR host?**
   Нет, и блокировка двойная: (а) события `EVENT_CURSOR` не доставляются
   `Painter` (`mainloop.cpp:326` — только механику; `osd_registry.hpp:155`);
   (б) `paintPart`/`doPaint`/`xPaint` не зарегистрированы и превращаются в
   залогированный no-op (`jvm.cpp:894/1008`). Работают: открытие киоска, выбор
   банки, превью копии, сохранение/загрузка скина для паковых id.

8. **Что можно считать подтверждённым контрактом, а что — только логикой
   оригинала без работающей native-реализации?**
   Подтверждено: `getTexture()`→`ResourceRef` в `Part.save`/`saveSkin` и
   обратный `setTexture(readResID())`; формат skin-файла; `skinSaveDir` и
   процедура выбора имени; `Record::state.texture` как единственный носитель
   текстуры детали; паковые type-7 id переживают оба сохранения.
   Только Java-логика оригинала: весь сценарий рисования
   (`beginPaint/control/doPaint/paintPart/xPaint`), смысл `duplicate` как
   создания копии ресурса, `makeTexture` как аллокатора текстур, сохранение
   транзиентных текстур через SDAT-blob’ы. Всё это упирается в отсутствующие
   native и не проверено на работающей реализации.

### 8. Неизвестное и технические ограничения

**Чего нельзя установить без реализации/оригинального бинарника:**

- Семантика `Painter.doPaint/paintPart/xPaint`: создают ли они новую текстуру,
  меняют ли пиксели существующей, когда именно пишут `state.texture` детали.
- Выдаёт ли оригинальный `makeTexture`/`duplicate` транзиентный id
  `0xFFFFxxxx` и какова его жизнь (сессия, кадр, до `destroy`).
- Хранил ли оригинальный SDAT байты изображения для транзиентных текстур:
  внешний корпус (`docs/EXTERNAL_RESEARCH.md:135-141`) утверждает `kind 7 =
  image bytes`; в хосте blob пишется пустым, а прочитанные байты нигде не
  используются. Это **внешнее** утверждение, не проверенное на файле.
- Совпадает ли проверка `typeof == 7` в `setTexture` с оригинальной.

**Что найдено в хосте (только зафиксировано, ничего не исправлялось):**

1. `write(ResourceRef)` для неразрешимого id не пишет 4 байта и возвращает 0 —
   поток сохранения укорачивается (латентный сдвиг при живом/неизвестном id).
2. `sdat_wire_resource` создаёт blob с пустыми `bytes`; `sdat_remap_resource`
   на чтении всегда даёт `0` для транзиентных ordinal’ов; байты blob’ов,
   прочитанные из чужого файла, нигде не потребляются.
3. `ResourceRef.duplicate` не создаёт ресурс, а копирует ссылку, поэтому
   `MODE_COPY` в хосте неотличим от присваивания того же id.
4. `makeTexture` не выдаёт id: `Decal.id() == 0`, `type() == 0`; путь живёт в
   `_texpath` и используется только OSD.
5. `EVENT_CURSOR` доставляется только механику (`mainloop.cpp:326`);
   `Painter` своих событий не получает, `EC_HOVER` не рассылается.
6. `paintPart`, `doPaint`, `xPaint` — незарегистрированные native: лог в
   stderr и `void`, без исключения (уточнение формулировки этапа 7).
7. `PaintCan.capacity` не читается ни в одном декомпилированном классе.

**Ограничения метода:** декомпилятор TUFA восстанавливает вызовы, аргументы и
константы надёжно, но условия местами теряет (`if ((!(x)))`, отсутствующие
`else`, неинициализированные `local`), поэтому выводы §2 опираются на
последовательности вызовов и аргументы. Классы, лежащие только внутри RPK,
декомпилировались после извлечения по TOC `RPAK`.

Документ оставлен незакоммиченным; коммит не создавался.

---

## Этап 9 — фактический формат пользовательского скина

**Проверяемый HEAD:** `06ce08ec75fe2882ee1d4c3c311989ec28678eb8`.
Аудит read-only: игровые файлы не изменялись и не переименовывались, изображения
не перекодировались, C++/Java/шейдеры/RPK/SCX/сохранения не трогались, игра не
запускалась, новые скины не создавались, native-функции не реализовывались.
Предыдущие этапы не переписывались. Документ остаётся незакоммиченным.

Файлы найдены в установке: `<game>/save/skins/`, все с одной меткой времени
`2026-11-19 20:46:00`. Наличие `diag.log` (`SL2 build 606 (Feb  7 2005
11:40:53), distr. v2.2.1`) и `res_ban.log`, а также отсутствие в host любого
JPEG-энкодера (§4) указывают, что файлы созданы **оригинальным исполняемым
файлом игры** (`StreetLegal_Redline.exe`, 2 408 448 байт), а не OpenSLRR.

### 1. Обнаруженный файл и его характеристики

В каталоге 15 файлов: **один манифест** без расширения и **14 изображений** с
суффиксом `.1`…`.14`.

| Файл | Размер, Б | SHA-256 |
|---|---|---|
| `21312` (манифест) | 345 | `812f1bc226eb4eb125dce59b142bdb34821196cabc8750bf7c5852ecc38fde1a` |
| `21312.1` | 21 562 | `d580f05fc2273d255f09be52e6379b583f5517bfaa2e038b8603783711ee47dc` |
| `21312.2` | 9 258 | `76143f67e36864a6c56bcab858d4be8cd9405310ac90626914a0b76351d148f6` |
| `21312.3` | 25 898 | `05567e1773917e3f91eb3e0c86e8f4dbde22f2b8b2690ac771448620d341d175` |
| `21312.4` | 1 654 | `5c11e621f80007f2e1f691c0b9f320af4fe9860725ead7568f8dd85a437bb4b2` |
| `21312.5` | 11 894 | `ebd3cd11246a94ab3ddee899fc3a38ba7aa1d2ca6388b312a5887cf838e43d37` |
| `21312.6` | 1 812 | `dda2b71b065fee6952ebcd318f51cd1c0558b50c5152de52df9365dfd67064c1` |
| `21312.7` | 4 967 | `a52c132adcf623847d81c89fb7dc260919763f8db5e3dbb2ceaf4aaef2937d91` |
| `21312.8` | 3 455 | `a4ab3b5c5bab882656b15d334ad6c17b099045907e913d2d731f51fb57e40eb7` |
| `21312.9` | 9 026 | `b8e11eba1f2456b488be6622d5cfc760eaffe6374128e8b49bd391a88faf0348` |
| `21312.10` | 6 172 | `efce6372c1896e44b633649008342bc8e1f0fc6bbce65882ec67f3d9f5087e72` |
| `21312.11` | 6 023 | `83cd4b67aedf1034eda11f71210d06d32307b4476478279a09ac2ea4ba0d61f8` |
| `21312.12` | 21 488 | `ee14cb11beb1441f15daeea7d795e6943bc67168cb661082173181af179f1fb4` |
| `21312.13` | 1 912 | `90203302d916b95f572336818ddc4c72c18764b5b5c55b18ab3cd48058a3c4e5` |
| `21312.14` | 11 748 | `869db5c0a1b7c569fcc6c7163134470a9dcd015cfceabd471c5c4b92460775f9` |

Изображения: 136 869 байт суммарно; вместе с манифестом — 137 214 байт.
Дата изменения у всех одна — сохранение выполнено одной операцией.

**`21312` — не изображение**, а манифест в формате сохранения:

```
0000  53 44 41 54 00 01 02 00 67 98 54 82 00 00 00 00  SDAT....g.T.....
0010  12 01 01 00 01 00 00 00 b0 00 01 00 02 00 00 00  ................
0020  b4 00 01 00 03 00 00 00 ac 00 01 00 04 00 00 00  ................
0030  b9 00 01 00 05 00 00 00 bd 00 01 00 06 00 00 00  ................
...
0139  00 00 00 63 61 72 73 5c 72 61 63 65 72 73 5c 45  ...cars\racers\E
0149  69 6e 76 61 67 65 6e 2e 72 70 6b 00 d1 00 00 00  invagen.rpk.....
```

**`21312.1` — изображение** (типичная шапка, у всех 14 одинаковая до `SOS`):

```
0000  ff d8 ff e0 00 10 4a 46 49 46 00 01 01 00 00 01  ......JFIF......
0010  00 01 00 00 ff db 00 43 00 01 01 01 01 01 01 01  .......C........
0020  01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01  ................
0030  01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01  ................
...
541a  ee da 6b cb 7b 6b a4 ac da 6b 43 1e 8a 28 af a3  ..k.{k...kC...(..
542a  32 0a 28 a2 80 0a 28 a2 80 0a 28 a2 80 3f ff d9  2.(...(...(...?..
```

### 2. Формат изображения и доказательства

Структура разобрана по маркерам, а не по «открылось после переименования».
Все 14 файлов идентичны по структуре:

| Признак | Значение |
|---|---|
| Сигнатура | `FF D8` (SOI), завершение `FF D9` (EOI) |
| APP0 | `JFIF\0`, версия 1.01, density 1×1, thumbnail 0×0 |
| DQT | две таблицы (id 0 luminance, id 1 chrominance), **все 64 коэффициента = 1** |
| SOF0 | baseline DCT, 8 бит, **256 × 256**, 3 компонента |
| Разметка | Y 2×2, Cb 1×1, Cr 1×1 → **4:2:0** (субдискретизация цветности) |
| DHT | 4 таблицы (DC/AC luminance + DC/AC chrominance) |
| Метаданные | нет EXIF, ICC, COM и вложенных превью |
| Поток | один скан (`SOS`), без прогрессивных проходов |

Это **обычный JFIF/JPEG**, а не PNG, BMP, TGA, DDS или «raw»: сигнатуры
`89 50 4E 47` (PNG), `DDS `, `BM`, TGA-подобной шапки нет. Декодирование
доступным `Pillow` подтверждает: `JPEG`, `256×256`, режим `RGB` (3 канала,
**альфы нет**), палитры нет.

Плоские таблицы квантования (все единицы) соответствуют максимальному
качеству; 4:2:0 объясняет слабые цветовые «ореолы» вокруг декалей. Именно
поэтому файл открывается и после переименования в `.png`: большинство
просмотрщиков определяют формат по содержимому, а не по расширению. По
структуре это **JPEG**, а не PNG.

### 3. Назначение изображения

Манифест `21312` содержит 14 пар «part type → ordinal» и таблицу
несохранённых ресурсов:

```
pairs (14):
  0x010 part=0x00010112  texture=1      0x048 part=0x000100c4  texture=8
  0x018 part=0x000100b0  texture=2      0x050 part=0x000100af  texture=9
  0x020 part=0x000100b4  texture=3      0x058 part=0x000100bb  texture=10
  0x028 part=0x000100ac  texture=4      0x060 part=0x000100c7  texture=11
  0x030 part=0x000100b9  texture=5      0x068 part=0x000100b3  texture=12
  0x038 part=0x000100bd  texture=6      0x070 part=0x000100c0  texture=13
  0x040 part=0x000100be  texture=7      0x078 part=0x000100c9  texture=14
terminator 0
blobs = 14: ordinal 1..14, kind 7 (RESOURCE_TEXTURE), size 0, данных нет
packs = 1: ordinal 1 -> cars\racers\Einvagen.rpk
```

Номера `texture=1…14` **точно совпадают** с суффиксами файлов
`21312.1 … 21312.14`. Типы деталей (pack `cars\racers\Einvagen.rpk`)
резолвятся в конкретные детали кузова:

| ordinal | part type | класс детали | доминирующий цвет | доля |
|---|---|---|---|---|
| 1 | `0x00010112` | `Einvagen_110_GTK` (шасси) | (16,16,80) | 87.9 % |
| 2 | `0x000100b0` | `Einvagen_FL_quarterpanel` | (16,16,80) | 94.5 % |
| 3 | `0x000100b4` | `Einvagen_FR_quarterpanel` | (16,16,80) | 81.4 % |
| 4 | `0x000100ac` | `Einvagen_F_bumper` | (16,16,80) | **100.0 %** |
| 5 | `0x000100b9` | `Einvagen_hood_2` | (16,16,80) | 92.2 % |
| 6 | `0x000100bd` | `Einvagen_R_bumper` | (16,16,80) | 99.9 % |
| 7 | `0x000100be` | `Einvagen_R_door` | (16,16,80) | 95.9 % |
| 8 | `0x000100c4` | `Einvagen_R_wing` | (16,16,80) | 97.0 % |
| 9 | `0x000100af` | `Einvagen_FL_door` | (16,16,80) | 89.4 % |
| 10 | `0x000100bb` | `Einvagen_L_mirror` | (16,16,80) | 79.8 % |
| 11 | `0x000100c7` | `Einvagen_RL_door` | (16,16,80) | 91.6 % |
| 12 | `0x000100b3` | `Einvagen_FR_door` | (16,16,80) | 81.8 % |
| 13 | `0x000100c0` | `Einvagen_R_mirror` | (16,16,80) | 99.9 % |
| 14 | `0x000100c9` | `Einvagen_RR_door` | (16,16,80) | 77.3 % |

Каждое изображение — это **запечённая текстура краски одной детали**:
сплошная заливка цветом кузова (во всех 14 файлах доминирует один и тот же
`(16,16,80)`, покрытие 77–100 %) плюс наложенные декали. На `21312.3`
визуально опознаётся надпись «STREET», совпадающая с игровым ресурсом
`decals/textures/catalog/logos/aa_StreetLegal.png` (512×256 RGBA); на других
деталях — другие декали в разных положениях и поворотах, то есть рисование
выполнялось **по каждой детали отдельно**.

Почему все файлы ровно 256×256: у всех 31 конфигурации деталей Einvagen
покрасочный слот один и тот же — `texture 0x00020025`, а это ресурс
`cars/textures/default_white.png` (256×256, равномерный RGB 235). Размер и
формат экспорта наследуются от исходной текстуры слота, а не от меша.

Часть изображений содержит чисто чёрные области (например, у зеркала
`21312.10` — 18.6 % пикселей). Достоверно объяснить их нельзя: это либо потеря
альфы при экспорте в RGB-JPEG, либо чёрные (RGB=0) пиксели прозрачных участков
декалей, попавшие в канву. Вопрос оставлен открытым.

### 4. Путь создания файла

**Java-часть (декомпилированный VM-код):**

```java
// Painter.java:807-835, CMD_SAVESKIN = 108
VehicleSkinFileReqDialog d = new VehicleSkinFileReqDialog(player.controller,
        ((Dialog.DF_FREEZE | Dialog.DF_MODAL) | Dialog.DF_DEFAULTBG) | Dialog.FRF_SAVE,
        "SAVE PAINT JOB", "SAVE", GameLogic.skinSaveDir, "*");
if (d.display() == 0) {
    String path = GameLogic.skinSaveDir + d.fileName;   // "save/skins/" + "21312"
    if (File.exists(path)) {
        File.delete(path);
        File.delete(path + ".*");                      // <-- sidecar-файлы
    }
    player.car.saveSkin(path);
    new WarningDialog(..., "SAVE PAINT JOB", "Paint job has been saved.").display();
}
```

```java
// Vehicle.saveSkin(path)
local2.write(Vehicle.SAVEFILEID_SKIN);          // -2108385177 / 0x82549867
local2.write(Vehicle.SAVEFILEVERSION_SKIN);     // 0
while ((local3 = iterateParts())) {
  local4 = local3.getTexture();
  if (local4) {
    local2.write(new GameRef(local3.getInfo(GameType.GII_TYPE)));  // тип детали
    local2.write(new ResourceRef(local4));                         // id текстуры
  }
}
local2.write(0);
```

Java пишет **только манифест** (`21312`). Байтов изображения в этом коде нет ни
в одном вызове. Манифест `21312` — ровно эта структура: `SAVEFILEID_SKIN`,
версия 0, 14 пар, терминатор, затем таблица несохранённых ресурсов
(kind 7, size 0) и таблица паков.

**Native-часть (оригинальный движок).** Пиксели записывает слой работы с
данными (datafile) при сериализации `ResourceRef`: встретив временный
(несохранённый) ресурс, он выгружает его в отдельный файл `<путь>.<ordinal>` и
оставляет в манифесте только номер. Доказательства:

1. Имена файлов `21312.<ordinal>` побитово соответствуют ordinal’ам таблицы
   blob’ов манифеста (1…14), у которых `kind = 7` (`RESOURCE_TEXTURE`) и
   `size = 0` — то есть «данные объявлены, но лежат не здесь».
2. В `StreetLegal_Redline.exe` есть форматная строка **`%s.%d`** (4 вхождения
   подряд в одной строковой секции) рядом со строками того же слоя:
   `save\`, `save/`, `sourcefile %5s`, `Invalid datafile: %s`,
   `Invalid datafile saved: %s`, `vm_file_open: invalid file open mode`,
   **`ERROR! Unsaveable resource!`**, **`ERROR! Unsaved resource!`**.
3. В том же исполняемом файле присутствует **энкодер** IJG/libjpeg — строки
   `JPEG parameter struct mismatch`, `Sampling factors too large`,
   `Too many color components`, `Bogus input colorspace`, `Maximum supported
   image dimension` (декодер даёт другие сообщения). Значит движок умеет
   писать JPEG сам.
4. Java-код игры сам ожидает такие sidecar-файлы: `Painter` перед сохранением
   делает `File.delete(path + ".*")`, а `CarLot.saveCar` —
   `File.delete(tempSaveDir, "PlayerCar0.*")`, а затем `GameLogic.save`
   копирует `PlayerCar*` (маска захватывает и `PlayerCar0.N`). Никакой другой
   смысл у шаблона `"<файл>.*"` в коде игры отсутствует.
5. В host OpenSLRR JPEG-энкодера нет вообще (в `src/` только декодеры через
   stb/PTX), а `sdat_wire_resource` создаёт blob с пустыми байтами — значит
   sidecar-файлы физически не могли быть созданы host’ом.

**Точка обрыва для host:** `sdat_wire_resource` (`natives_io.cpp:205-247`)
формирует таблицу несохранённых ресурсов, но не экспортирует изображение и не
создаёт `<путь>.<ordinal>`. Компенсирующей операции в host нет.

### 5. Путь обратной загрузки

```java
// Painter.java:790-805, CMD_LOADSKIN = 107
VehicleSkinFileReqDialog d = new VehicleSkinFileReqDialog(player.controller,
        (Dialog.DF_FREEZE | Dialog.DF_MODAL) | Dialog.DF_DEFAULTBG,
        "LOAD PAINT JOB", "LOAD", GameLogic.skinSaveDir, "*");
if (d.display() == 0) player.car.loadSkin(GameLogic.skinSaveDir + d.fileName);

// Painter.java:1023-1033 — фильтр списка файлов в диалоге
public class VehicleSkinFileReqDialog extends FileRequesterDialog {
    public int validator(String arg0) { return Vehicle.fileCheck_Skin(arg0); }
}

// Vehicle.java:84-101
public static int fileCheck_Skin(String arg0) {
    File f = new File(arg0);
    if (f.open(File.MODE_READ)) {
        if (f.readInt() == SAVEFILEID_SKIN) {          // 0x82549867
            if (f.readInt() == SAVEFILEVERSION_SKIN) { // 0
                local1 = 1;
            }
        }
        f.close();
    }
    return local1;
}
```

То есть в списке видны только манифесты, а не изображения.

```java
// Vehicle.loadSkin(path)
iteratePartsInit(); while ((local3 = iterateParts())) local2++;   // счёт деталей
File local5 = new File(path);
if (local5.open(File.MODE_READ)) {
  if (local5.readInt() == SAVEFILEID_SKIN && local5.readInt() == SAVEFILEVERSION_SKIN) {
    while ((local6 = local5.readResID())) {          // тип детали
      local7 = local5.readResID();                   // ordinal (1..14)
      ... для первой ещё не тронутой детали с GII_TYPE == local6:
          local3.setTexture(local7);
    }
  }
  local5.close();
}
```

- Целевая деталь определяется **только по типу детали** (`GII_TYPE`), а не по
  имени, индексу или id экземпляра; применяется к первой подходящей детали
  данного типа. Информации о машине в файлах нет.
- `readResID` для значений 1…14 в оригинале разрешает ordinal через таблицу
  несохранённых ресурсов **этого файла** и подхватывает данные из
  `<путь>.<ordinal>` — то есть изображение загружается в новую (временную)
  текстуру, а не используется как файл напрямую. Прямого дизассемблирования
  этой функции не выполнялось; вывод опирается на структуру манифеста
  (ordinal’ы + kind 7 + size 0), строку `%s.%d` в слое datafile и на то, что
  Java-код игры сам удаляет `<путь>.*`.
- Дальше — обычный путь: `Record::state.texture` детали, при следующем
  сохранении машины `Part.save` пишет уже этот id.

**В host OpenSLRR этот путь не работает:** `sdat_remap_resource`
(`natives_io.cpp:248-270`) для ordinal’ов ищет blob с непустым `abs_id`, а при
чтении `abs_id` не заполняется никогда → возвращается `0`; sidecar-файлы не
ищутся. `Part.setTexture(0)` ничего не меняет. Итог: оригинальный скин в host
загружается «вхолостую» — пары применяются, но все id равны 0.

### 6. Сопоставление с `Vehicle.saveSkin()`

Противоречия нет: это **один механизм и два файла на один скин**.

| | Skin Preset (`21312`) | Skin image (`21312.N`) |
|---|---|---|
| Кто пишет | Java `Vehicle.saveSkin` через `java.io.File` | native datafile-слой оригинала |
| Формат | SDAT type 2 + `SAVEFILEID_SKIN` v0 + пары + таблица blob’ов + паки | JFIF JPEG 256×256 RGB |
| Содержимое | какие детали покрашены и номер ресурса каждой | сами пиксели одной детали |
| Размер | 345 Б | 1,6–25,9 КБ |
| Имя | `<skinSaveDir>/<имя>` | `<skinSaveDir>/<имя>.<ordinal>` |
| Самодостаточность | без изображений бесполезен (в host — вообще) | без манифеста бесполезен: не к чему привязать |
| Кто читает | `Vehicle.fileCheck_Skin`, `Vehicle.loadSkin` | разрешение ordinal в `readResID` |

Один вызов `Painter.CMD_SAVESKIN` создаёт и манифест, и все изображения;
`Painter` удаляет и то, и другое (`path` и `path + ".*"`).

**Уточнение к этапу 8.** Там было сказано «изображение не сохраняется» и
«blob пустой». Для *манифеста* это верно и теперь объяснено: оригинал тоже
пишет `size = 0`, потому что данные вынесены в sidecar-файлы. Вывод «skins не
содержат изображений» был неполным: скин — это набор файлов, а не один файл.
Также снимается вопрос этапа 7 «переживает ли перекраска сохранение»: пиксели
покидают рантайм в `<save>.<n>` (для машины это `save/temp/PlayerCar0.<n>`,
которые `CarLot.saveCar` удаляет по маске `PlayerCar0.*` и `GameLogic.save`
копирует маской `PlayerCar*`). Проверить это на файле машины нельзя: в
`save/carrer/2-1/` лежит только `main`, `PlayerCar0` для этой карьеры
отсутствует.

### 7. Связь с ResourceRef и Part

1. **Изображение загружается в новую текстуру или используется файл?**
   В оригинале — загружается в текстуру (временный ресурс); файл сам по себе
   ресурсом не является. В host — ни то, ни другое.
2. **Получает ли оно runtime Resource ID?** В оригинале да: именно этот id
   попадает в манифест как ordinal (kind 7). В host — нет: id не выдаётся
   вообще (`makeTexture` хранит только путь, живые id — `0x70000000+`).
3. **Может ли такой ID быть передан в `Part.setTexture()`?** В оригинале —
   да, это и есть штатный путь скина. В host — нет: `setTexture`
   (`part_runtime.cpp:4979`) требует `rpk.resolve(id) && typeof_ == 7`, а
   временный id не разрешается.
4. **Сохраняется ли содержимое изображения в файле машины?** Нет: `Part.save`
   пишет только 4-байтовый id. Пиксели выносятся в sidecar-файлы
   (`<машина>.<n>`), а не в основной файл.
5. **Если нет, остаётся ли оно в отдельном файле?** Да — `<имя>.<ordinal>` в
   том же каталоге, что и манифест/сохранение машины.
6. **Как при следующей загрузке восстанавливается связь?** Через пары
   «тип детали → ordinal» в манифесте и через разрешение ordinal в
   `readResID`, который подхватывает `<файл>.<ordinal>`. В host эта связь не
   восстанавливается: ordinal → 0.

### 8. Таблица персистентности

| Данные | Физический формат | Где хранится | Кто записывает | Кто читает | Связь с машиной/деталью |
|---|---|---|---|---|---|
| **Skin image** | JFIF JPEG, baseline, 256×256, RGB, 4:2:0, DQT=1 | `save/skins/<имя>.<ordinal>` (1,6–25,9 КБ) | native datafile-слой оригинала (IJG-энкодер); **в host отсутствует** | разрешение ordinal в `readResID` оригинала; **в host не читается** | только через пару в манифесте: `part type → ordinal` |
| **Skin Preset** | SDAT type 2 + `SAVEFILEID_SKIN` (0x82549867) + v0 + пары + таблица blob’ов (kind 7, size 0) + паки | `save/skins/<имя>` (345 Б) | `Vehicle.saveSkin` (Java + `java.io.File`) — **работает и в host** | `Vehicle.fileCheck_Skin`, `Vehicle.loadSkin` — **работают и в host** | тип детали (`GII_TYPE`) → номер ресурса; машина не идентифицируется |
| **Runtime texture** | ресурс движка (временный, id с высокой половиной `0xFFFF` по данным файлов) | память процесса; в host — только `_texpath` без id | `doPaint`/`paintPart` (оригинал, native) — **в host не зарегистрированы** | рендер и `Part.getTexture()` | `Record::state.texture` детали |
| **Part texture Resource ID** | int32 в записи `Part.save` и в парах манифеста | `PlayerCarN` (слово `+8` записи детали), `save/skins/<имя>` | `Part.save`, `Vehicle.saveSkin` | `Part.load` → `setTexture`, `loadSkin` → `setTexture` | прямая: id записан в записи конкретной детали |

### 9. Ответы на вопросы

1. **Каков фактический формат сохранённого файла?** Скин — это набор: манифест
   (`SDAT`+`SAVEFILEID_SKIN`, без расширения) и 14 файлов изображений
   `<имя>.<ordinal>`, каждый — обычный baseline JPEG 256×256 RGB (JFIF 1.01,
   4:2:0, таблицы квантования из единиц).
2. **Является ли он обычным растровым изображением?** Да, изображения —
   стандартный JFIF/JPEG без альфы и без метаданных. Манифест изображением не
   является.
3. **Какая операция игры его создаёт?** Один вызов `Painter.CMD_SAVESKIN`:
   Java пишет манифест (`Vehicle.saveSkin`), native datafile-слой выгружает
   временные текстуры в `<имя>.<ordinal>`.
4. **Как игра загружает файл обратно?** `Painter.CMD_LOADSKIN` →
   `Vehicle.loadSkin` читает пары и вызывает `setTexture(ordinal)`; ordinal
   разрешается через таблицу несохранённых ресурсов файла (в оригинале — с
   подхватом `<имя>.<ordinal>`).
5. **Как это соотносится с `Vehicle.saveSkin()`?** Это его манифест. Формат
   из этапа 8 — не альтернатива изображению, а его индекс.
6. **Кто именно записывает байты изображения?** Не Java: в декомпилированном
   коде нет ни одной операции экспорта пикселей. Пишет native-слой datafile
   оригинального движка (в host такой операции нет — это и есть точка обрыва).

### 10. Неизвестное и технические ограничения

- Точная функция-экспортёр в `StreetLegal_Redline.exe` (2 408 448 байт, 2006-11-10) не локализована
  (дизассемблирование не выполнялось). Доказательства косвенные, но
  согласованные: совпадение ordinal’ов, `kind 7`/`size 0`, строка `%s.%d` в
  секции datafile, наличие JPEG-энкодера, удаление `<файл>.*` самим Java-кодом
  игры.
- Как именно оригинальный `readResID` связывает ordinal с sidecar-файлом
  (имя файла + номер, поиск в каталоге, кэш) — не восстановлено.
- Источник чёрных областей в части изображений не установлен (потеря альфы
  при экспорте либо чёрные пиксели прозрачных участков декалей).
- Точный цвет краски не сопоставлен: доминирующий `(16,16,80)`, ближайшая
  банка в списке `Painter` — `0x041C5B` (4,28,91), ближайший заводской цвет —
  `Ozean.png` (4,40,60); совпадения нет, поэтому заявлять источник цвета
  нельзя.
- Проверить sidecar’ы сохранения машины (`PlayerCar0.N`) невозможно: для
  карьеры `2-1` файл `PlayerCar0` отсутствует, а `save/temp/` пуст.
- Поведение host зафиксировано по коду (этап 8) и не проверялось запуском:
  игра не запускалась, новые скины не создавались.
- Декомпилятор TUFA восстанавливает вызовы и константы надёжно, условия —
  частично; выводы §4–5 опираются на последовательности вызовов, аргументы и
  на сами файлы.

Документ оставлен незакоммиченным; коммит не создавался.

---

## Этап 10 — native skin export/import и разрешение текстур

**Проверяемый HEAD:** `06ce08ec75fe2882ee1d4c3c311989ec28678eb8`.
Аудит read-only: EXE/DLL не изменялись и не патчились, C++/Java/шейдеры/RPK/SCX/
сохранения и sidecar-файлы не трогались, новые скины не создавались, повторный
экспорт из игры не выполнялся, художественное содержимое изображений не
анализировалось. Предыдущие разделы (этапы 2–9) не изменялись.

Инструменты, доступные в окружении: `capstone 5.0.7` (дизассемблер x86),
`pefile 2024.8.26` (разбор PE), `Pillow`/`numpy`. Ghidra, IDA, radare2, objdump,
dumpbin в системе **отсутствуют** и не устанавливались. Отладочных символов,
PDB, map-файлов и экспортируемых имён в исследованном EXE **нет**
(`IMAGE_DIRECTORY_ENTRY_DEBUG` пуст, таблица экспорта отсутствует), поэтому
анализ вёлся по строкам, импортам, ссылкам на строки (`push imm32`), вызовам
(`call rel32`) и локальному дизассемблированию. Это ограничение определяет
степень уверенности в §3–§4.

### 1. Источник цветовой текстуры и её связь с деталью

Ключ к разрешению — **не** текстура поверхности детали, а отдельный слот краски
(`PartConfig::texture`), который в рендер-объекте детали стоит **первой картой**.

Цепочка для каждой из 14 сохранённых деталей (всё проверено по файлам
`cars\racers\Einvagen.rpk`, `cars.rpk`, `cars\racers.rpk`):

```
Java GII_TYPE (wired: ordinal пака = 1 -> cars\racers\Einvagen.rpk, local)
  -> Einvagen.rpk, typeof 8 (part), payload: "native part <cfg>"
     -> <cfg>: render <pack0 local> ; mesh <pack0 local> ; texture 0x00020025
        -> Einvagen.rpk, typeof 14 (render), payload: "mesh ... + texture <id>" x N
           -> map0 = texture 0x00020025 = pack 2 (cars.rpk) local 0x25
                 -> cars.rpk, typeof 7: "sourcefile cars\textures\default_white.png"
                                       "flags 1.000"
           -> map1..N = pack 0 (Einvagen.rpk) local -> "sourcefile ...Einvagen_data\textures\*.dds"
```

Фактическая таблица (pack-биты: `0` = сам `Einvagen.rpk`, `2` = `cars.rpk`;
`0x00020025` подтверждён разбором `cars.rpk` — запись `local 0x25 typeof 7`,
alias `white`, payload `sourcefile cars\textures\default_white.png\r\nflags 1.000`):

| ord | part (alias) | cfg | слот краски | размеры слота | карты рендера (map0 … mapN) |
|---|---|---|---|---|---|
| 1 | `Einvagen_110_GTK` (шасси) | `_main_110_GTK.cfg` | `0x00020025` `default_white.png` | **256×256** | render `Einvagen.rpk:0x48` (`chassis`): `0x4C`, `0x4B`, **`0x00020025`**, `0x4A`=`Einvagen_chassis.dds` **512×512** |
| 2 | FL quarterpanel | `FL_quarterpanel.cfg` | `0x00020025` | 256×256 | `default_white.png` 256×256 \| `Einvagen_F_quarterpanel.dds` 256×256 |
| 3 | FR quarterpanel | `FR_quarterpanel.cfg` | `0x00020025` | 256×256 | `Einvagen_F_quarterpanel.dds` 256×256 |
| 4 | F bumper | `F_bumper.cfg` | `0x00020025` | 256×256 | `Einvagen_F_bumper.dds` 256×256 |
| 5 | hood_2 | `hood_2.cfg` | `0x00020025` | 256×256 | `Einvagen_hood_2.dds` 256×256 |
| 6 | R bumper | `R_bumper.cfg` | `0x00020025` | 256×256 | `Einvagen_R_bumper.dds` 256×256 |
| 7 | R door | `R_door.cfg` | `0x00020025` | 256×256 | `Einvagen_hatch_door.dds` 256×256 |
| 8 | R wing | `R_wing.cfg` | `0x00020025` | 256×256 | `Einvagen_R_wing.dds` **256×128** |
| 9 | FL door | `FL_door.cfg` | `0x00020025` | 256×256 | `Einvagen_F_door.dds` 256×256 |
| 10 | L mirror | `L_mirror.cfg` | `0x00020025` | 256×256 | `Einvagen_mirror.dds` **128×128** |
| 11 | RL door | `RL_door.cfg` | `0x00020025` | 256×256 | `Einvagen_R_door.dds` 256×256 |
| 12 | FR door | `FR_door.cfg` | `0x00020025` | 256×256 | `Einvagen_F_door.dds` 256×256 |
| 13 | R mirror | `R_mirror.cfg` | `0x00020025` | 256×256 | `Einvagen_mirror.dds` **128×128** |
| 14 | RR door | `RR_door.cfg` | `0x00020025` | 256×256 | `Einvagen_R_door.dds` 256×256 |

Размеры взяты из самих файлов: PNG — из IHDR, DDS — из `dwHeight/dwWidth`
заголовка. Все 31 `*.cfg` в `cars\racers\Einvagen_data\scripts` содержат
`texture 0x00020025` (проверено `grep`; других значений в конфигурациях нет).

**Разграничение объектов (это разные ресурсы, не взаимозаменяемые):**

| Объект | Что это | Пример | Разрешение |
|---|---|---|---|
| заводская текстура цвета | PNG-палитры `cars\textures\factory_colors\*` | `Einvagen\Jazz.png` | 256×256 |
| **цветовая текстура (слот краски)** | `PartConfig::texture` = map0 рендера | `cars.rpk:0x25` → `cars\textures\default_white.png` | **256×256** |
| текстура поверхности детали | map1..N рендера (материал меша) | `Einvagen_data\textures\Einvagen_mirror.dds` | 128×128 … 512×512 |
| белая основа | тот же `default_white.png` | — | 256×256 |
| результат покраски | runtime-текстура, созданная движком | id вида «временный» | наследует размер слота краски |
| sidecar скина | JFIF JPEG `<manifest>.<ordinal>` | `21312.1` … `21312.14` | **совпадает с результатом покраски** |

Замечание: `.ptx`-алиасы в RPK (`alias 'Einvagen_chassis.ptx'` при
`sourcefile ...Einvagen_chassis.dds`) — это контейнер/обёртка текстуры, а не
отдельная текстура; размер берётся из источника.
Отдельная нерешённая мелочь: `render 0x0003001C` из `_main_110_GTK.cfg` не
резолвится в `cars\racers.rpk` (там `local 0x1C` — `racer_default_steering_wheel`);
шасси сопоставлено по рендеру `Einvagen.rpk:0x48`, который ссылается на тот же
mesh `0x49` и на `default_white.png`, что и конфиг шасси.

### 2. Источник разрешения экспортируемого изображения

Экспортируемое изображение имеет **ровно те же ширину и высоту, что слот
краски**, и не совпадает с текстурами поверхности детали. Это видно из
сопоставления фактических файлов, без обращения к содержимому картинок:

| деталь | текстура материала | sidecar скина |
|---|---|---|
| шасси | `Einvagen_chassis.dds` **512×512** | `21312.1` **256×256** |
| R wing | `Einvagen_R_wing.dds` **256×128** | `21312.8` **256×256** |
| L/R mirror | `Einvagen_mirror.dds` **128×128** | `21312.10`, `21312.13` **256×256** |
| все остальные | 256×256 | 256×256 |

Если бы экспортёр брал пиксели (или размеры) из текстуры поверхности, скины
шасси, крыла и зеркал имели бы 512×512, 256×128 и 128×128. Они 256×256, то есть
источник — слот краски `0x00020025` (`default_white.png`, 256×256). Все 14
sidecar'ов имеют один и тот же размер, хотя материалы деталей различаются —
это исключает версию «размер берётся из меша/материала».

Следовательно ширина и высота берутся **из объекта текстуры слота краски**
(размер загруженной текстуры), а не из исходного файла на диске напрямую и не
из меша. Практическое следствие: `default_white.png` — loose-файл
(`sourcefile cars\textures\default_white.png`), игра читает его с диска, поэтому
замена файла на версию 2048×2048 меняет размер загружаемой текстуры, а значит и
размер канвы покраски.

### 3. Точная цепочка JPEG export

Исследованный файл: `E:\OpenSLR\Street Legal Racing - Redline\StreetLegal_Redline.exe`
(это тот EXE, который запускает игра; `diag.log`: `SL2 build 606 (Feb  7 2005
11:40:53), distr. v2.2.1`).

| | |
|---|---|
| размер | 2 408 448 байт |
| mtime | 2006-11-10 22:48:56 |
| SHA-256 | `b284a7a53909d0b9ce59d59ddc11e14df40a7eb74d86fa76efb06305aadc2b03` |
| PE | i386, ImageBase `0x400000`, TimeDateStamp `0x420765D4`, 5 секций, EntryPoint `0x1514C0` |
| отладка | каталог отладки пуст (PDB нет), таблица экспорта отсутствует |
| импорт | `d3d9`, `KERNEL32`, `USER32`, `GDI32`, `DINPUT8`, `DSOUND`, `ADVAPI32`, `WINMM`, `ole32`, `OLEAUT32` — **ни одной библиотеки изображений** |

Для сравнения: `StreetLegal_Redline.bak.exe` — тот же размер, mtime 2005-08-16,
SHA-256 `7d77466a8c1a34d16a59d9ccf2e8059664dff1bfb421338074a461a932e8edde`;
содержит те же `%s.%d` (4), `ERROR! Unsaveable resource!`, `JPEG parameter struct
mismatch` и тот же код масштабирования качества (4 вхождения) — механизм не
зависит от сборки.

**libjpeg слинкован статически.** Прямых импортов нет, но в `.text` найдены
характерные функции IJG:

| адрес | что это | подтверждение |
|---|---|---|
| `0x560B50` | `jpeg_quality_scaling(quality)` | точный код IJG: `5000/quality` при `<50`, `200-2*quality` при `≥50`, клампы 1 и 100 |
| `0x560B90` | `jpeg_set_quality(cinfo, quality, force_baseline)` (scaling встроен) | те же константы + `push 0x5F3AE0` (std_luminance_quant_tbl) |
| `0x560C00` | проверка `scale_factor` c `ERREXIT1(JERR_BAD_QUALITY)` (код 18 пишется в `[cinfo+0x14]`) | `mov [eax+0x14],0x12` + `call [eax]` (error_exit) |
| `0x553785`, `0x553799` | вызовы `set_quality(cinfo, q, TRUE)` | `push 1` (force_baseline), `q` приходит **аргументом** функции, не константой |

Строки сообщений библиотеки лежат в `.rdata`: `JPEG parameter struct mismatch`
(`0x600AA8`), `Maximum supported image dimension` (`0x600814`), `Bogus input
colorspace` (`0x600CA8`), `Not a JPEG file` (`0x600650`). Они относятся и к
энкодеру, и к декодеру, то есть IJG присутствует в обе стороны.

**Точка сбора sidecar-имени (writer).** Функция `0x484FA0` — обход списка
записей о ресурсах при записи файла (трейлер SDAT). Она пишет для каждого узла два u32
(`[node+0xc]`, `[node+0x10]`), затем ветвится по типу:

```
00485007  mov eax,[esi]            ; хендл выходного файла
0048500b  lea ebx,[edi+0xc]
00485015  ...                      ; запись 4 байт [node+0xc]
00485019  add edi,0x10             ; запись 4 байт [node+0x10] = тип ресурса
0048502e  sub edi,5  ; == 5 -> 0x485112   (меш/SCX)
0048503a  sub edi,2  ; == 7 -> 0x485043   (текстура)
...
004850e3  mov ecx,[ebx]            ; ordinal
004850e6  lea edx,[esi+0x4c]       ; путь самого файла (манифеста)
004850f0  push 0x613818            ; "%s.%d"
004850fb  call 0x551220            ; sprintf(dst, "%s.%d", path, ordinal)
0048510b  call 0x48da30            ; this=&file+0x14 -> push_back(имя sidecar)
```

Ветвление `sub edi,5` / `sub edi,2` — то есть специальная обработка **только
типов 5 и 7** — независимо подтверждается host-реализацией:
`sdat_wire_resource` (`natives_io.cpp:214`) содержит ровно то же условие
`if (restype != 5 && restype != 7) return false;`. Тип 5 — меш (SCX),
тип 7 — текстура (`RESOURCE_TEXTURE`). Оба типа могут создаваться в рантайме
(покраска, деформация), поэтому и попадают в таблицу несохранённых ресурсов.

Второй вариант записи того же места (ветка `0x485090`) пишет `u32 длина` и
строку из `[sub+0x58]/[sub+0x5c]`; выбор ветки определяется разбором поля
`sourcefile` ресурса (`sscanf(str, "sourcefile %5s", buf)` — `0x551240` →
`0x553890`, затем `strcasecmp`-подобное сравнение `0x5D7190` с `save\` и
`save/`): то есть для ресурса, чей источник — файл в `save\...`, в манифест
встраивается его путь. Для наших 14 текстур такого источника нет, поэтому
пишется нулевая длина и работает соглашение об имени sidecar'а.

**Граница исследования (честно).** Вызов самого JPEG-энкодера **не привязан**
к этой функции: `jpeg_set_quality` вызывается из `0x5537xx` (функция
`libjpeg`-обвязки с большим кадром, `[esp+0x214]/[esp+0x218]` — аргументы),
но цепочку «регистрация имени sidecar → дамп текстуры → энкодер → запись файла»
проследить по call graph не удалось: сборка без символов, а точка записи файла
ищется только по косвенным вызовам. Подтверждено: (а) наличие и работоспособный
код энкодера IJG; (б) механизм регистрации sidecar-имени `<файл>.<ordinal>`;
(в) соответствие типов 5/7; (г) сам факт JFIF-файлов на диске. Не подтверждено:
конкретный call-site «взять пиксели текстуры → скормить энкодеру».

### 4. Конвертация пикселей и параметры JPEG

Параметры восстановлены по фактическим файлам, а код энкодера подтверждает
используемые функции:

| параметр | значение | откуда известно | уверенность |
|---|---|---|---|
| контейнер | JFIF 1.01, APP0 density 1×1, без EXIF/ICC/COM/thumbnail | маркеры файла | высокая |
| базовый режим | baseline sequential DCT, 8 бит | `SOF0`, один `SOS` | высокая |
| размер | 256×256 (все 14) | `SOF0` | высокая |
| компоненты | 3 (YCbCr), без альфы, без палитры | `SOF0` | высокая |
| субдискретизация | 4:2:0 (2×2, 1×1, 1×1) | `SOF0` sampling factors | высокая |
| таблицы квантования | **все 64 коэффициента = 1** (обе таблицы) | `DQT` | высокая |
| качество | 100 (scale_factor = 1) | `DQT`=1 ⇒ `jpeg_quality_scaling` даёт 1 только при q≈100 | высокая (по данным), код `set_quality` найден |
| таблицы Хаффмана | стандартные, 4 шт. | `DHT` | высокая |
| ресайз/кроп | нет: размеры JPEG = размеры канвы | сравнение с §2 | высокая |
| поворот/flip/UV-трансформ | признаков не найдено | в JFIF нет ориентации; трансформаций в найденном коде обвязки нет | средняя |

Смысл 4:2:0 и DQT=1: это **значения по умолчанию IJG** при явно выставленном
качестве, а не подстройка под текстуру. `jpeg_set_defaults` для YCbCr задаёт
2×2 для компоненты 0, а `jpeg_set_quality(...,100,...)` даёт таблицы из единиц
(scaling клампится в 1). Никакого «агрессивного сжатия» нет — картинка пишется
почти без потерь по яркости, с потерями только по цветности (4:2:0).

Пиксели берутся из **результата покраски** (runtime-текстура детали), а не из
файла `default_white.png`: в скинах присутствует заливка цветом кузова и
наложенные декали, которых в исходной текстуре нет. Формат канвы — по
поведению движка (D3D9, `d3d9.dll` импортируется) это поверхность с альфой;
в JPEG альфа отбрасывается (в файлах 3 компоненты), что согласуется с
чёрными участками, отмеченными на этапе 9.

### 5. Запись sidecar и связь с manifest ordinal

Манифест `21312` разобран побайтово (все смещения проверены):

| смещение | содержимое |
|---|---|
| `0x00` | `SDAT` + `00 01 02 00` (версия 0x0100, тип 2) |
| `0x08` | `0x82549867` (`SAVEFILEID_SKIN`), версия `0` |
| `0x10`–`0x83` | 14 пар `(wired part id, wired texture ordinal)`: `0x00010112`→1, `0x000100b0`→2, …, `0x000100c9`→14 |
| `0x84` | терминатор пары `0` |
| `0x84` | `u32` = 14 (число несохранённых ресурсов) |
| `0x88`–`0x133` | 14 записей по 12 байт: `ordinal = 1..14`, `kind = 7`, `0` |
| `0x13C` | `u32` = 1 (число паков) |
| `0x140` | `ordinal = 1`, `len = 25`, `"cars\racers\Einvagen.rpk\0"` |
| `0x155` | `u32` = `0xD1` (209) — размер трейлера |
| итого | 345 байт; данных изображений внутри нет |

Правило связи: **манифест не содержит ни байтов изображения, ни его имени** —
все 14 записей имеют нулевую длину. Имя sidecar'а строится по соглашению
`<путь манифеста> + "." + <ordinal>`, что и делает `sprintf("%s.%d", file->path
@+0x4c, ordinal)` в `0x4850e3`. Отсюда `21312` + `.1` … `.14`.

`kind = 7` — это тип ресурса «текстура» (совпадает с `RESOURCE_TEXTURE` в
терминах host'а), то есть запись говорит «ресурс ordinal — текстура, данные
внешние».

### 6. Обратная загрузка и восстановление runtime texture

Читатель трейлера — функция `0x485190` (разбирает счётчик, размещает узлы по
`0x24` байта через `operator new` (`0x54F560`), связывает их в список
`[file+0x3c]/[file+0x44]`, увеличивает `[file+0x28]`). Имя sidecar'а строится
там же, тем же способом:

```
004852d9  mov eax,[edi]            ; ordinal
004852dc  lea ecx,[ebx+0x4c]       ; путь манифеста
004852e4  push 0x613828            ; "%s.%d"
004852ea  call 0x551220            ; sprintf
004852f8  repne scasb              ; strlen
004852ff  add edi,0x1d             ; +29 = размер узла с заголовком
00485303  call 0x54f560            ; operator new
...
00485398  push 0x613820            ; "%s.%d" (второй сайт, та же схема)
004853b7  call 0x539080            ; поиск/регистрация в таблице (this=0x6408c0)
```

То есть **ordinal не разрешается через таблицу паков** (`hi_word != 0`), а
порождает отдельный «внешний» ресурс с именем `<манифест>.<ordinal>`; загрузка
пикселей откладывается до фактического обращения (lazy), после чего
декодированный JPEG становится текстурой с обычным runtime-идентификатором.

Ответ на вопрос этапа 9: связь «`kind 7 / size 0` → реальный JPEG» держится
**не на поле в манифесте, а на соглашении об имени**: `readResID()` возвращает
wired-значение (ordinal), а файловый слой немедленно/лениво ставит ему в
соответствие ресурс, чьи данные лежат в `<путь манифеста>.<ordinal>`. Поэтому
sidecar нельзя переименовать или перенести, не сломав загрузку, и поэтому же
«пустой blob» в манифесте — норма, а не признак потери данных.

Декодирование выполняет тот же статически слинкованный IJG (строки декодера
`Not a JPEG file`, `Bogus JPEG`, `SOS parameters for sequential JPEG` в
`.rdata`), результат — новая текстура; присвоение детали делает Java:
`Vehicle.loadSkin` → `Part.setTexture(<id>)`, привязка по `GII_TYPE` (этап 9).

### 7. Отличия исследованного EXE и OpenSLRR host

| Операция | Исследованный EXE (оригинал) | Текущий OpenSLRR host | Подтверждение |
|---|---|---|---|
| Запись manifest SDAT | есть: `0x484FA0` + `sprintf("%s.%d")` | есть: `sdat_frame` (`natives_io.cpp:171`), тот же байтовый формат (проверено на `21312`) | дизассемблер + исходник + файл |
| Запись sidecar JPEG | есть: регистрация имени в `0x48DA30` (`push_back` в `file+0x14`), энкодер IJG в `.text` | **нет**: ни экспортёра, ни энкодера | дизассемблер vs `grep` по `src/` |
| JPEG encoder | статический IJG (`0x560B50/0x560B90/0x560C00`) | **отсутствует** (в `src/` только декодирование) | дизассемблер vs `src/resources/ptx_image.cpp` |
| Чтение sidecar JPEG | есть: `0x4852D9`, `0x485391` + lazy load | **нет**: sidecar-файлы не ищутся | дизассемблер vs `sdat_remap_resource` |
| JPEG decoder | IJG декодер (строки `.rdata` `0x600650…`) | есть, но другой: `stb_image` через `ptx_image.cpp` | `.rdata`/`src` |
| Remap ordinal → ResourceRef | внешний ресурс по имени `<файл>.<ordinal>` | `sdat_remap_resource` ищет `abs_id` в blob'ах, `abs_id` никем не заполняется → **0** | дизассемблер vs `natives_io.cpp:248` |
| Создание runtime texture | есть (покраска создаёт временную текстуру) | **нет**: `doPaint`/`paintPart`/`xPaint` не зарегистрированы (этап 8) | `jvm.cpp:894/1008` |
| Назначение детали | `Part.setTexture(<transient id>)` | пары применяются, но id = 0 → no-op | Java-байткод + `part_runtime.cpp:4979` |

Отдельно: host **пишет** manifest корректно по форме (тот же SDAT-трейлер,
те же `ordinal/kind/len`), но с нулевой длиной и без sidecar'ов; поэтому
host-скин будет «пустым» и для самого host'а, и для оригинала.

### 8. Объяснение зависимости разрешения скина от цветовой текстуры

**Ответ на прямой вопрос:** да. Если текстура слота краски детали имеет
2048×2048 вместо 256×256, sidecar JPEG будет 2048×2048 — при условии, что
покраска выполняется после подмены текстуры (канва создаётся по размерам
загруженной текстуры слота).

Механизм:

1. `PartConfig::texture` (map0 рендера) указывает на ресурс `cars.rpk:0x25` с
   `sourcefile cars\textures\default_white.png` — **loose-файл**, читаемый с
   диска при загрузке.
2. Движок загружает эту текстуру и использует её как основу канвы покраски:
   цвет/декали накладываются в UV-пространстве этой текстуры, поэтому канва
   обязана иметь те же размеры (иначе UV не совпадут).
3. Экспорт пишет канву целиком, без ресайза и без привязки к степеням двойки:
   ширина/высота в JPEG = ширина/высота канвы = ширина/высота текстуры слота.
4. Ни фиксированного предела, ни ресайза в найденном коде нет; физический
   предел — возможности D3D9-устройства (`MaxTextureWidth/Height`), а не
   константа движка. Практика пользователя (скины 2K) это подтверждает.

Что это **не** такое: это не текстура поверхности детали (иначе шасси дало бы
512×512, зеркало 128×128, крыло 256×128 — см. §2), не заводская текстура цвета
(`factory_colors`) и не фиксированный размер 256.

Условия, при которых зависимость работает: замена файла должна попасть в
загружаемую текстуру (для RPK-ресурса это loose `sourcefile`, а не встроенные
байты пака); покраска должна произойти после загрузки новой текстуры, поскольку
канва наследует размер в момент создания; уже созданная канва своего размера не
меняет.

### 9. Таблица механизмов

| Шаг | Объект | Путь / адрес | Аргументы / данные | Уверенность |
|---|---|---|---|---|
| 1 | `PartConfig::texture` | `cars\racers\Einvagen_data\scripts\*.cfg`, строка `texture 0x00020025` | wired id (pack 2, local 0x25) | высокая (файлы) |
| 2 | Ресурс текстуры | `cars.rpk`, local 0x25, typeof 7, alias `white` | `sourcefile cars\textures\default_white.png; flags 1.000` | высокая (файл) |
| 3 | Файл текстуры | `cars\textures\default_white.png` | PNG 256×256 RGB | высокая (IHDR) |
| 4 | Карты рендера | `Einvagen.rpk` typeof 14 (`0x48`, `0x72`, `0x89`, `0x91`, …) | `mesh …` + `texture …` (map0 = слот краски) | высокая (файлы) |
| 5 | Канва покраски | runtime-текстура движка (D3D9 поверхность) | размер = размер текстуры шага 2-3 | **средняя**: прямого call-site создания не найдено; следует из §2 и практики |
| 6 | Запись манифеста | `StreetLegal_Redline.exe:0x484FA0` | обход списка записей о ресурсах (трейлер); `u32 id`, `u32 type`, `u32 len`; для типа 7 — ветка `0x485043` | высокая (дизасм) |
| 7 | Имя sidecar | `0x4850E3` → `sprintf` `0x551220` | `("%s.%d", file->path@+0x4c, ordinal)` | высокая (дизасм) |
| 8 | Регистрация sidecar | `0x48DA30` (this = `file+0x14`) | `push_back(name)` | высокая (дизасм) |
| 9 | JPEG encode | IJG: `0x560B90` `jpeg_set_quality`, `0x560C00` проверка `JERR_BAD_QUALITY`, `0x560B50` `jpeg_quality_scaling` | quality из аргумента обвязки `0x553799`; по DQT фактически 100 | средняя (функции найдены; вызов из дампа текстуры не локализован) |
| 10 | Параметры JPEG | `SOF0`/`DQT`/`DHT` файлов sidecar | baseline, 256×256, 3 компонента, 4:2:0, DQT=1 | высокая (файлы) |
| 11 | Чтение трейлера | `0x485190` | count, узлы `0x24` байта, `operator new` `0x54F560` | высокая (дизасм) |
| 12 | Имя sidecar при чтении | `0x4852D9`, `0x485391` → `sprintf` `0x551220` | то же правило `%s.%d` | высокая (дизасм) |
| 13 | Регистрация внешнего ресурса | `0x539080` (this = `0x6408C0`) | имя sidecar как ключ | средняя (семантика таблицы не восстановлена) |
| 14 | Назначение детали | Java `Vehicle.loadSkin` → `Part.setTexture` | пара `(GII_TYPE, id)` | высокая (байткод, этап 9) |

### 10. Неизвестное и технические ограничения

- **Главная граница:** call-site, который берёт пиксели канвы и передаёт их в
  IJG, не локализован. Известны обе стороны (регистрация sidecar-имени в
  `0x48DA30`; наличие и рабочий код энкодера в `0x55xxxx`–`0x57xxxx`), но не
  соединяющий их переход. Причина: PE без символов/PDB/экспортов, обратный
  трассировщик в окружении отсутствует (нет Ghidra/IDA), а вызовы идут через
  указатели и обёртки.
- Создание канвы покраски (какая функция D3D9 создаёт поверхность и откуда
  берёт `Width/Height`) не найдено; вывод §2/§8 опирается на фактическое
  совпадение размеров и на практическое наблюдение пользователя. Прямого
  доказательства «читается `GetLevelDesc()` текстуры слота» в машинном коде нет.
- Семантика поля `u32` после `(ordinal, kind)` в записи трейлера: по коду
  писателя это длина опциональной строки (путь `sourcefile` для ресурсов из
  `save\…`), по коду host-читателя — длина payload. Для исследованного файла
  поле равно 0, поэтому различия не проявляются; какая трактовка верна для
  ненулевых значений, не установлено.
- Почему в меню сохранения участвуют именно типы 5 и 7 (меш и текстура) —
  согласуется с тем, что только они создаются в рантайме, но исчерпывающего
  списка рантайм-ресурсов этап не составлял.
- `render 0x0003001C` из конфигурации шасси не резолвится в `cars\racers.rpk`;
  сопоставление шасси выполнено по `Einvagen.rpk:0x48`.
- Различия между `StreetLegal_Redline.exe` и `.bak.exe` по skin-пути не
  изучались (оба содержат одинаковый набор маркеров).
- Поведение host зафиксировано по исходникам и не проверялось запуском: игра не
  запускалась, скины не сохранялись и не загружались.
- Художественное содержимое изображений (что именно нарисовано, какие декали,
  композиция) не анализировалось — только форматы, размеры и структура.

Документ оставлен незакоммиченным; коммит не создавался.

---

## Этап 11 — уточнение источника изображения и разрешения скина

**Проверяемый HEAD:** `06ce08ec75fe2882ee1d4c3c311989ec28678eb8`.
Аудит read-only: игра не запускалась, скины не создавались и не экспортировались,
ресурсы/RPK/SCX/сохранения/sidecar-файлы не изменялись, EXE и C++/Java не
патчились. Разделы этапов 2–10 не переписывались (исправление формулировок
этапа 10 сделано отдельным подразделом §5 ниже).

Продолжено исследование `E:\OpenSLR\Street Legal Racing - Redline\StreetLegal_Redline.exe`
(SHA-256 `b284a7a53909d0b9ce59d59ddc11e14df40a7eb74d86fa76efb06305aadc2b03`).

### 1. Три объекта: `paint_source`, `paint_texture`, runtime surface

Это три разные сущности, и в host'е они действительно разделены:

```cpp
// include/slrr/gameref.hpp:178
uint32_t paint_source = 0, paint_texture = 0;

// src/resources/part_runtime.cpp:1307-1314  (paint_one)
void paint_one(Object *part, uint32_t texture) {
  auto *rec = record(part);
  if (!rec || !rec->config->texture) return;   // paint_source обязателен
  rec->state.texture = texture;                // <-- paint_texture
  for (auto *render : rec->renders)
    gameref_get(render)->paint_texture = texture;
}
```

| Объект | Тип | Происхождение | Размер | Где используется |
|---|---|---|---|---|
| **`paint_source`** = `Rec.config->texture` | `uint32_t`, id ресурса RPK `typeof 7` | строка `texture <id>` конфигурации детали; тот же id стоит **map0** в рендер-объекте детали | 256×256 (`cars.rpk:0x25` → `cars\textures\default_white.png`) | определяет, **какой слот геометрии** заменяется: host ищет группу SCX, чей путь текстуры совпадает с путём `paint_source` (`scene_instances.cpp:1841-1848`), результат — `paint_slot`; в шейдере покрашенной считается группа `group.texture_index == paint_slot` (`bgfx_lighting.cpp:196`). После покраски сам не рендерится |
| **`paint_texture`** = `Rec.state.texture` | `uint32_t`, id ресурса: либо упакованный (`pack<<16|local`), либо временный (`0xFFFFxxxx`) | при спавне — заводская текстура цвета из `Descriptor[color]` (`GameLogic.CARCOLORS`), после покраски — **созданная движком runtime-текстура** | у временной — своя, задаётся при создании | подставляется в найденный слот: `node->paint_texture` → `instance.paint_texture` (`scene_instances.cpp:1857-1858`), т.е. это и есть «назначенная цветовая текстура» |
| **Runtime paint surface** | D3D9-текстура (объект внутри ресурса), `IDirect3DTexture9`/surface | создаётся движком в момент покраски; обёрнута ресурсом с временным id | её собственный дескриптор поверхности | пишется paint-ядром; **это то, что читает экспортёр** (§3) |
| **JPEG output** | файл `<манифест>.<ordinal>` | пишется при сериализации манифеста | = размеру runtime surface | sidecar скина |

Ключевое различие: `paint_source` отвечает на вопрос «куда рисовать» (какой слот
и какие UV), `paint_texture` — «что там сейчас нарисовано», а runtime surface —
это физическая поверхность, из которой берутся пиксели при экспорте.

### 2. Повторная проверка 14 деталей из сохранения

Разбор манифеста `21312` (этап 10) даёт для каждой детали только *ordinal*
временной текстуры; сам id `state.texture` в манифесте не сохраняется
(в `PlayerCar0` он был бы, но файла машины для этой карьеры нет).

| ord | part type (wired) | деталь | `paint_source` | размер `paint_source` | `paint_texture` в этом сейве | размер sidecar |
|---|---|---|---|---|---|---|
| 1 | `0x00010112` | `Einvagen_110_GTK` | `0x00020025` → `default_white.png` | 256×256 | **временная текстура, ordinal 1** | 256×256 |
| 2 | `0x000100b0` | FL quarterpanel | `0x00020025` | 256×256 | временная, ordinal 2 | 256×256 |
| 3 | `0x000100b4` | FR quarterpanel | `0x00020025` | 256×256 | временная, ordinal 3 | 256×256 |
| 4 | `0x000100ac` | F bumper | `0x00020025` | 256×256 | временная, ordinal 4 | 256×256 |
| 5 | `0x000100b9` | hood_2 | `0x00020025` | 256×256 | временная, ordinal 5 | 256×256 |
| 6 | `0x000100bd` | R bumper | `0x00020025` | 256×256 | временная, ordinal 6 | 256×256 |
| 7 | `0x000100be` | R door | `0x00020025` | 256×256 | временная, ordinal 7 | 256×256 |
| 8 | `0x000100c4` | R wing | `0x00020025` | 256×256 | временная, ordinal 8 | 256×256 |
| 9 | `0x000100af` | FL door | `0x00020025` | 256×256 | временная, ordinal 9 | 256×256 |
| 10 | `0x000100bb` | L mirror | `0x00020025` | 256×256 | временная, ordinal 10 | 256×256 |
| 11 | `0x000100c7` | RL door | `0x00020025` | 256×256 | временная, ordinal 11 | 256×256 |
| 12 | `0x000100b3` | FR door | `0x00020025` | 256×256 | временная, ordinal 12 | 256×256 |
| 13 | `0x000100c0` | R mirror | `0x00020025` | 256×256 | временная, ordinal 13 | 256×256 |
| 14 | `0x000100c9` | RR door | `0x00020025` | 256×256 | временная, ordinal 14 | 256×256 |

**Ответ на поставленный вопрос: `paint_texture` ни у одной из 14 деталей не
разрешается в `default_white.png`.** `default_white.png` — это только
`paint_source`. Доказательство структурное, а не по совпадению размеров:

1. Манифест записывает **все 14** текстур как *несохранённые* ресурсы
   (`kind = 7`, `ordinal = 1…14`, length 0) — то есть на момент сохранения
   `state.texture` каждой детали была **временная runtime-текстура**, а не
   ресурс пака.
2. Если бы `paint_texture` указывала на `0x00020025` (`default_white.png`,
   pack 2 = `cars.rpk`), писатель записал бы её как **ссылку на пак**
   (`(ordinal_пака<<16)|local`), и в таблице паков появилась бы запись про
   `cars.rpk`. В таблице паков манифеста ровно **одна** запись —
   `cars\racers\Einvagen.rpk` (ordinal 1), и она нужна для 14 *типов деталей*.
   Ни одной упакованной текстуры в скине нет.
3. `default_white.png` не могла быть назначенной текстурой ещё и потому, что
   рисуемая поверхность содержит заливку цветом кузова и декали, которых в
   исходном файле нет: `default_white.png` равномерна (RGB 235, СКО = 0), а все
   14 sidecar'ов неоднородны (СКО > 0). Пиксели — не из `paint_source`.

Итого: в исследованном сейве `paint_source` = `default_white.png` 256×256,
`paint_texture` = отдельная временная текстура на каждую деталь, sidecar = её
дамп.

### 3. Источник JPEG-пикселей и источника ширины/высоты

Цепочка восстановлена полностью (все адреса — `StreetLegal_Redline.exe`):

```
0x484FA0  писатель трейлера SDAT, ветка type==7
   0x4850E3  sprintf("%s.%d", file->path, ordinal)        -> имя sidecar
   0x48510B  call 0x48DA30(resource, name)
0x48DA30  дамп текстуры ресурса в файл (this = контекст ресурса, arg = имя)
   0x48DA31  esi = [this+0xC]                              -> объект ресурса
   0x48DA38  if (res->[0x4C] != 1) call [vtbl+0x14](1.0f)  -> подготовка/подгрузка
   0x48DA55  call 0x5447D0(res, 0, 0, 0x80000000)          -> операция над ресурсом
   0x48DA5A  test eax, 0x80000000 ; jne -> отказ            -> проверка результата
   0x48DA6A  call [vtbl+0xC](1.0f)                          -> объект текстуры
   0x48DA77  mov eax,[eax+0x18]                             -> обёртка текстуры
   0x48DA89  call 0x506460(this=обёртка, name)
0x506460  сохранение текстуры в файл
   0x5064A4  mov ecx,[edi+0x4C]                            -> D3D-текстура
   0x5064B1  call 0x4F6000                                 -> пиксели + w + h
   0x5064C6  w*h ; 0x5064CB 3*w*h+0x40 ; call 0x54F560      -> буфер RGB
   0x50651B  mov [desc+8],  w
   0x506522  mov [desc+0xC], h
   0x506598  call 0x5536F0(desc, pixels, quality)
0x4F6000  чтение поверхности
   0x4F6010  call [vtbl+0x44]                              -> GetLevelDesc(level 0, &desc)
   w,h берутся из дескриптора D3D-поверхности
0x5536F0  обёртка JPEG-энкодера
   0x553744  push 0x3D (61)  ; 0x55373B push 0x168 (360)    -> jpeg_CreateCompress(cinfo, 61, 360)
   0x553765  call 0x564C00                                 -> вывод в память
   0x55377D  input_components = 3, in_color_space = 2 (RGB)
   0x553799  call 0x560B90(cinfo, [0x6188B0], TRUE)         -> jpeg_set_quality
```

Отсюда однозначно:

- **Пиксели берутся из runtime-текстуры** (той самой, что стоит в
  `state.texture`), а не из `paint_source` и не из файла на диске.
- **Ширина и высота берутся из дескриптора D3D-поверхности этой runtime-текстуры**
  (`GetLevelDesc(0)`), а не из `PartConfig::texture`, не из меша и не из
  манифеста.
- Формат канвы — 32-битный BGRA (в коде идёт побайтовое преобразование
  BGR→RGB для 3 компонент: `0x50655A`–`0x50657C`), альфа отбрасывается.
- Качество JPEG приходит из **глобальной переменной `0x6188B0`**, а не
  хардкодом: при сохранении `21312` её значение давало scale factor 1
  (таблицы квантования из единиц). Субдискретизация 4:2:0 — умолчание IJG.
- Найденная версия библиотеки: `JPEG_LIB_VERSION = 61` (IJG **v6a**),
  `sizeof(struct jpeg_compress_struct) = 0x168 = 360` байт.
- Энкодер пишет в **память** (3 аргумента вывода), файл создаёт и записывает
  уже `0x506460`; это закрывает пробел этапа 10 («call-site энкодера не
  найден»).

Степень уверенности: высокая для всей цепочки (каждый переход — прямой `call`
с проверенными аргументами и смещениями; `GetLevelDesc` опознан по слоту
vtable `0x44` и по раскладке `D3DSURFACE_DESC`: ширина/высота читаются из
`[esp+0x24]`/`[esp+0x2c]` дескриптора).

### 4. Практический случай с 2K

Поскольку размер JPEG = размер runtime surface, зависимость от цветовой
текстуры может войти **только в момент создания** этой поверхности. Три
варианта:

1. **Канва создаётся как копия `paint_source`** (`PartConfig::texture` →
   `cars\textures\default_*.png`). Тогда увеличение файлов `default_*.png` до
   2048 даёт канву 2048 и скины 2048 — для любой покраски, в том числе
   повторной.
2. **Канва создаётся как копия текущей `paint_texture`.** Тогда увеличение
   файлов в `cars\textures` влияет только на детали, чья текущая текстура — этот
   самый файл (у только что купленной машины это заводская текстура цвета из
   `cars\textures\factory_colors\…`), а повторная покраска уже покрашенной
   детали сохранила бы старый размер канвы.
3. **Канва имеет фиксированный размер.** Тогда наблюдение пользователя
   невозможно; вариант отпадает.

Наблюдение пользователя (увеличение цветовых текстур в `cars\textures` →
скины высокого разрешения) согласуется с вариантом 1 и лишь частично с
вариантом 2. Дополнительно за вариант 1 говорят:

- у всех 14 деталей `paint_source` — один и тот же файл `default_white.png`
  (256×256), и все 14 sidecar'ов ровно 256×256; текстуры материала при этом
  разные (128×128, 256×128, 512×512) — то есть размер однороден там, где
  однороден именно `paint_source`;
- paint-ядро пишет в поверхность по UV слота (`0x48D450` считает координаты
  через `0x4FDB40`/`0x513720` и границы −0.9…0.9, −0.85…0.85 — это поля
  размещения декалей), то есть работа идёт в UV-пространстве слота
  `paint_source`, и канва должна этому слоту соответствовать;
- никаких ограничений размера в paint-коде не найдено; верхняя граница —
  возможности D3D9-устройства.

**Чего не хватает для окончательного ответа:** место создания канвы (кто
вызывает `CreateTexture` и откуда берёт width/height) не локализовано.
Paint-ядро `0x48D450` работает уже с **существующей** текстурой ресурса:
`0x4FDC40` — чистый аксессор (возвращает `res->[+8]->[+8]`, объекта текстуры, без
создания), `0x4FDC80` вызывает `0x516C20(текстура, 0)` (подготовка/захват).
Вызовов `CreateTexture` в цепочке покраски нет, значит канва создаётся
где-то раньше — вероятнее всего на стороне Java через нативные
`makeTexture`/`duplicate` (этап 8), но это не проверено. Поэтому формулировка «канва наследует размер `paint_source`»
опирается на наблюдаемый результат пользователя и на структуру данных, а не на
дизассемблированный вызов создания. В таблице ниже это отмечено как
«не установлено».

### 5. Исправление формулировок этапа 10

Не изменяя текст этапа 10, фиксирую уточнения:

1. **Неверно:** «ширина и высота берутся из объекта текстуры слота краски
   (`paint_source`)», «размер наследуется от исходной текстуры слота».
   **Верно:** экспортёр читает размер из **runtime-текстуры**, назначенной
   детали (`paint_texture`/runtime surface), через `GetLevelDesc(0)`.
   Совпадение 256×256 у всех 14 деталей объясняется тем, что канва была создана
   от 256-й текстуры, а не тем, что экспортёр смотрит на `PartConfig::texture`.
2. **Неверно/неточно:** «`paint_texture` у деталей — это `default_white.png`».
   В исследованном сейве `paint_texture` — временная текстура на каждую деталь
   (ordinal 1…14); `default_white.png` относится только к `paint_source`.
3. **Уточнение:** место вызова энкодера найдено (`0x48DA30` → `0x506460` →
   `0x5536F0`), версия IJG — 61 (v6a), а не «v6b/неизвестно»; качество — из
   глобальной `0x6188B0`.
4. **Уточнение:** JPEG пишется не напрямую из энкодера в файл, а через
   промежуточный буфер в памяти.

Обязательная таблица:

| Объект | Назначение | Источник | Размер | Влияет на JPEG |
|---|---|---|---|---|
| `paint_source` | определяет слот геометрии (какая группа SCX заменяется) и UV-пространство покраски | строка `texture <id>` конфигурации детали / map0 рендер-объекта → `cars.rpk:0x25` → `cars\textures\default_white.png` | 256×256 | **не установлено** (влияет только косвенно, если канва создаётся как его копия; создания канвы в дизассемблере не найдено) |
| `paint_texture` | текущая текстура, подставляемая в слот | при спавне — заводская текстура цвета; после покраски — временный ресурс, созданный движком | равна размеру своей текстуры; в сейве — временная, ordinal 1…14 | **да** — размер берётся из её D3D-поверхности (`GetLevelDesc`) |
| Runtime paint surface | физическая поверхность, в которую пишет paint-ядро и которую дампит экспортёр | создаётся движком при покраске; владелец — временная текстура | 256×256 в исследованном сейве; задаётся при создании | **да** — это и есть источник пикселей и размеров |
| JPEG output | sidecar-файл скина | `0x506460` → `0x5536F0` (IJG v6a) → запись файла | = размеру runtime surface | — (результат) |

### 6. Механизмы: подтверждённые адреса и степень уверенности

| Шаг | Адрес / файл | Аргументы | Уверенность |
|---|---|---|---|
| native `paintPart(GameRef,I)V` | `0x4827D0` | регистрация: `push impl; push sig; push name; push class; call 0x416B00` | высокая (таблица регистрации в `.text`: `0x489D90`) |
| native `doPaint(GameRef,IIIFFI)V` | `0x482960` | idem | высокая |
| native `xPaint(GameRef)V` | `0x482C00` | внутри — вызов Java-метода строкой `"bigdecal 0,0 0,0,0 0,0,0 0 0 1 0"` (`0x6134D8`) | высокая |
| paint-ядро | `0x48D450` | UV-координаты, границы ±0.9/±0.85; `0x4FDC40` — аксессор текстуры ресурса, `0x4FDC80` → `0x516C20` (захват) | высокая |
| сериализация ресурса | `0x48DA30` | `(контекст_resource, name)`; подготовка `[vtbl+0x14]`, проверка `0x5447D0`, `[vtbl+0xC](1.0f)` → обёртка `[+0x18]` | высокая (смысл `0x5447D0` не установлен) |
| дамп текстуры в файл | `0x506460` | `(name)`, this = обёртка текстуры (`this->texture` @ +0x4C) | высокая |
| чтение w/h | `0x4F6000` | `GetLevelDesc(0, &desc)` через `[vtbl+0x44]` | высокая |
| преобразование пикселей | `0x50655A`–`0x50657C` | BGRA → RGB, 3 байта на пиксель | высокая |
| JPEG-энкодер | `0x5536F0` (`jpeg_CreateCompress` 61/360, `jpeg_mem_dest`, `jpeg_set_quality`) | размеры из `[desc+8]/[desc+0xC]`, качество `[0x6188B0]` | высокая |
| создание канвы | не найдено | — | **не установлено** |

### 7. Неизвестное и технические ограничения

- Место создания runtime-канвы (какой объект копируется при первой покраске и
  откуда берутся `width/height` для `CreateTexture`) не локализовано; именно
  поэтому вопрос «`paint_source` или `paint_texture` задаёт размер» в таблице
  §5 помечен как «не установлено». Всё, что находится после создания канвы,
  доказано (§3). Следующий конкретный шаг, если аудит будет продолжен: найти в
  EXE реализации нативных `makeTexture`/`duplicate` (строки есть в пуле имён),
  их call-site в Java-байткоде и проверить, откуда берутся размеры новой
  текстуры — из `paint_source`, из текущей текстуры или из константы.
- Косвенная причина, по которой вариант «канва от `paint_source`» выглядит
  предпочтительным: практический результат пользователя + однородность
  `paint_source` у всех 14 деталей при разнородных текстурах материала. Это
  согласованность, а не доказательство.
- Значение глобальной `0x6188B0` в момент сохранения восстановлено косвенно (по
  таблицам квантования sidecar'ов = все единицы, что даёт scale factor 1).
  Где она инициализируется, не исследовалось.
- Нижние уровни (мип-уровни) не рассматривались: экспорт читает уровень 0;
  поведение при неполном наборе мипов не проверялось.
- Поведение host не проверялось запуском (игра не запускалась), выводы по host
  сделаны по исходникам.
- `PlayerCar0` для карьеры `2-1` отсутствует, поэтому сопоставить ordinal'ы
  манифеста с конкретными id `state.texture` из сохранения машины нельзя;
  вывод о «временности» сделан по структуре таблицы паков манифеста.
- **Состояние рабочего дерева во время аудита.** Файл
  `cars\textures\default_white.png` отличается от версии в HEAD
  (`4964` байт против `5060`, mtime `2026-11-19 22:19:03`), при этом размеры
  остались `256×256 RGB`; одновременно появились/обновились `error.log`
  (`22:18:10`) и `diag.log` (`22:18:12`), то есть в каталоге игры шёл
  посторонний процесс (запуск игры). Аудит файл не изменял и только читал его;
  все измерения этапов 9–11 относятся к текущей рабочей версии файла.

Документ оставлен незакоммиченным; коммит не создавался.

---

## Этап 12 — создание и инициализация runtime paint surface

**Проверяемый HEAD:** `06ce08ec75fe2882ee1d4c3c311989ec28678eb8`.
Read-only: игра не запускалась, EXE/DLL/C++/Java/шейдеры/RPK/SCX/сохранения и
sidecar-файлы не изменялись, тестовые скины не создавались. Разделы этапов 2–11
не переписывались. Исследовался `StreetLegal_Redline.exe`
(SHA-256 `b284a7a5…b03`).

Проверка `cars\textures\default_white.png` (по требованию):
`4964` байт, SHA-256 `6356faed66fba1fec388b1f432ebc5a970aba32d033bcc031c059a456b43ecba`,
mtime `2026-11-19 22:19:03`; в HEAD (`9950ec6 Initial commit`) blob
`aaa7bcf91cdf112b11ddd5ad984090d678a3fc85`, `5060` байт. Файл отличается от
HEAD и не восстанавливался; размеры в текущей версии — `256×256 RGB`.

### 1. Исходное состояние детали до рисования

**Java-сторона.** Цвет машины задаётся не именем файла, а *id текстуры* из
`GameLogic.CARCOLORS` (`int[21]`):

```java
// d1/Chassis.java:717-718 — случайный заводской цвет при спавне
int local11 = (GameLogic.CARCOLORS.length * random());
local6.color = GameLogic.CARCOLORS[local11];

// Vehicle.java:130 — цвет из индекса (CarSelect/Catalog/CarMarket)
chassis.addStockParts(new Descriptor(GameLogic.CARCOLORS[arg2], ...));
```

`Descriptor.color` — это Resource ID текстуры, а не цвет в RGB. Дальше
`Chassis.addStockParts` вызывает `Part.setTexture(...)`, и этот id попадает в
`Record::state.texture` (`Part.getTexture()` возвращает именно его).

**Файлы.** Заводская палитра Einvagen в `cars.rpk` (проверено разбором RPK —
записи `typeof 7`, payload `sourcefile …flags 1.000`):

| local | sourcefile | размер |
|---|---|---|
| `0x42` | `cars\textures\factory_colors\Einvagen\Zucker.png` | 256×256 |
| `0x43` | `…\Tornado_rot.png` | 256×256 |
| `0x44` | `…\Nacht.png` | 256×256 |
| `0x45` | `…\Smaragd.png` | 256×256 |
| `0x46` | `…\Black_mage.png` | 256×256 |
| `0x47` | `…\Hamvas_Grun.png` | 256×256 |
| `0x48` | `…\Indigo.png` | 256×256 |
| `0x49` | `…\Jazz.png` | 256×256 |
| `0x4A` | `…\Antracit.png` | 256×256 |
| `0x4B` | `…\Mercator_Blau.png` | 256×256 |
| `0x4C` | `…\Murano.png` | 256×256 |
| `0x4D` | `…\Champagner.png` | 256×256 |
| `0x4E` | `…\Ozean.png` | 256×256 |
| `0x4F` | `…\Reflex.png` | 256×256 |
| `0x50` | `…\Saratoga.png` | 256×256 |

Плюс «подержанные» цвета `0x65…0x68` → `factory_colors\Used\used_rusty_*.png`
(256×256). `0x51`/`0x52` — не цвета, а OSD-текстуры (`LongOSDGauge.png`,
`SL2_Digital_Font_render.png`).

То есть **исходная назначенная текстура детали — не `default_white.png`**, а
заводская текстура цвета из `cars\textures\factory_colors\Einvagen\`; этап 7
подтвердил это на живом сохранении: у шасси `state.texture = 0x00020050` =
`Saratoga.png`.

Ограничение: для исследованного сейва `2-1` файл `PlayerCar0` отсутствует,
поэтому конкретный цвет той машины до покраски восстановить нельзя — известен
только класс значений (палитра 0x42…0x50) и то, что к моменту сохранения все
14 текстур были уже временными (этап 11).

### 2. Создание paint surface

**Полная таблица нативных методов извлечена из EXE** (записи вида
`push impl; push sig; push name; push class; call 0x416B00`, реестр —
`[0x63641C]`; всего 360 записей). Это позволило опознать все native-функции,
связанные с покраской:

| класс | метод | сигнатура | impl |
|---|---|---|---|
| `java.game.Painter` | `paintPart` | `(Ljava.util.resource.GameRef;I)V` | `0x4827D0` |
| `java.game.Painter` | `doPaint` | `(Ljava.util.resource.GameRef;IIIFFI)V` | `0x482960` |
| `java.game.Painter` | `xPaint` | `(Ljava.util.resource.GameRef;)V` | `0x482C00` |
| `java.game.parts.Part` | `getTexture` | `()I` | `0x469AB0` |
| `java.game.parts.Part` | `setTexture` | `(I)I` | `0x469B80` |
| `java.util.resource.ResourceRef` | `makeTexture` | `(LResourceRef;Ljava.lang.String;)V` | `0x47FFE0` |
| `java.util.resource.ResourceRef` | `makeSound` | `(LResourceRef;Ljava.lang.String;)V` | `0x480100` |
| `java.util.resource.ResourceRef` | `duplicate` | `(LResourceRef;)V` | `0x4802C0` |

`beginPaint`/`endPaint` в таблице **отсутствуют** — это чисто Java-методы
`Painter` (создают/завершают сеанс, но native-поверхности не трогают).

**Что делает paint-путь (paintPart, `0x4827D0`):**

```
0x4827EE  call 0x45D910            ; GameRef -> объект/ресурс (резолвер всех Part-нативов)
0x482819  call 0x426470(vm=0x636338, id, 2, &out)   ; Java getInfo(GII_TYPE)=2
0x482826  call 0x5D7408            ; rand() (LCG 0x343FD/0x269EC3)
0x482861  call 0x48D450(...)       ; ЯДРО ПОКРАСКИ
0x4828C0  sprintf(buf, "paint 0 0 %d 0 -1", ...)    ; fmt @0x613484
0x4828DB  call 0x426800(vm, obj, 0x10, buf, ...)    ; вызов Java-метода с командой
```

**Ядро покраски `0x48D450`** (координаты UI → UV → запись в текстуру):

```
0x48D501  call 0x4FDC40(manager=0x6187B0, res, 0)   ; аксессор: текстура ресурса
0x48D5A5  call 0x4FDC80(manager, texture)           ; подготовка (0x516C20)
0x48D5E7  call 0x4FDB70(...)                        ; UV -> пиксели
0x48D68D  call 0x49E3D0(renderer=0x617650, tex, coords, target@+0x658)
0x48D6A1  call 0x4FDCA0(manager, tex)               ; обход списка поверхностей
```

Внутри ядра есть только чтение уже существующей текстуры ресурса и вызов
отрисовки в неё; **создания текстуры нет**. Аргументы нативов тоже не содержат
ни `paint_source`, ни какого-либо id текстуры: `paintPart(GameRef, int colour)`,
`doPaint(GameRef, int colour, int decalId, int, float rot, float size, int flip)`.
Вывод: Painter не выбирает текстуру по `PartConfig::texture` — он работает с
текстурой того ресурса, который сейчас назначен детали.

**`xPaint` (`0x482C00`)** вообще не рисует: он резолвит GameRef и вызывает
Java-метод с командной строкой `"bigdecal 0,0 0,0,0 0,0,0 0 0 1 0"`
(`0x6134D8`) — то есть просит Java выполнить покраску «большим декалем».

**Проверка гипотезы «новую текстуру создаёт `duplicate`».** В Java есть
`Painter.java:630`:

```java
ResourceRef local14 = new ResourceRef();
local14.duplicate(new ResourceRef(copy_texture));   // режим MODE_COPY
local12_3.setTexture(local14.id());
```

Реализация `duplicate` — `0x4802C0` → `0x48D860`, и это **не копирование
пикселей**: функция проверяет тип ресурса (`[res+0x4C] ∈ {0x0E, 0x0D, 5, 7}`),
снимает узел со старого списка (`+0x44/+0x48`), вставляет в новый и правит
refcount (`+0x04`). Это присваивание/разыменование ссылки, а `local14.id()`
после него указывает на **тот же** ресурс. Гипотеза «MODE_COPY создаёт копию»
отвергнута; подтверждается вывод этапа 8 о том, что `duplicate` копирует
ссылку, а не ресурс.

**Функции, которые действительно создают текстуры** (vtable-вызов
`IDirect3DDevice9::CreateTexture` = слот 23 → `call [reg+0x5C]`):

| функция | что делает | вызывающий |
|---|---|---|
| `0x4F5E70` | `CreateTexture`, `GetLevelDesc`, `GetSurfaceLevel` | `0x4F77C4` |
| `0x4F6360` | `CreateTexture`, `GetSurfaceLevel`, `GetLevelDesc` | `0x506432` |
| `0x4F6580` | `GetSurfaceLevel`, `CreateTexture`, `GetLevelDesc` | `0x5064B9` (экспортёр, этап 11) |
| `0x4F6E30` | `CreateTexture`, `GetLevelDesc` | прямых вызовов нет |
| `0x4F9AA0` | 2×`CreateTexture`, 2×`GetLevelDesc`, `LockRect` (лучший кандидат на «клон текстуры») | прямых вызовов нет |
| `0x5011B0`, `0x501420` | `CreateTexture` | `0x4FCBDE`, `0x4FCBD4` |

Ни один из этих сайтов не вызывается из `paintPart`/`doPaint`/`xPaint`/
`Part.setTexture`. Все они находятся в модуле менеджера текстур/загрузчика
(`0x4Fxxxx`, `0x50xxxx`), а вызов `0x4F6580` из экспортёра — это подготовка
читаемой копии поверхности для `LockRect` (D3D9 не даёт лочить все форматы),
а не создание канвы.

**Итог §2:** отдельного объекта «paint surface», создаваемого Painter'ом,
**не существует**: канва — это D3D-текстура того ресурса, на который указывает
`state.texture` детали. Точка, где ресурс становится *временным* (и где,
соответственно, создаётся его текстура), в EXE не локализована; в paint-пути её
нет.

### 3. Связь с Painter (путь данных)

| Java-вызов | Native | Что происходит с текстурой |
|---|---|---|
| `beginPaint(part)` | нет натива | только Java-состояние сеанса (`spraySfxID`, `overVehicle`); поверхность не создаётся и не выбирается |
| `doPaint(part, colour, decalId, 0, rot, size, flip)` | `0x482960` | резолв part → снятие узла из списка (`0x545FC0`) → `getInfo(GII_TYPE)` → `0x48D450` (покраска в существующую текстуру) |
| `paintPart(part, colour\|0xFF000000)` | `0x4827D0` | резолв → `rand()` → `0x48D450` → Java-команда `"paint 0 0 %d 0 -1"` |
| `xPaint(part)` | `0x482C00` | резолв → Java-команда `"bigdecal 0,0 0,0,0 0,0,0 0 0 1 0"` (сама покраска — в Java) |
| `getTexture()` | `0x469AB0` | читает id из записи детали, резолвит ресурс, при `[res+0x4C]!=1` готовит его (`[vtbl+0x14](1.0f)`), затем `0x5447D0(res,0,0,0xA0000000)` → объект текстуры |
| `setTexture(id)` | `0x469B80` | то же резолв-подготовка, затем запись нового id/текстуры в запись детали |

`0x5447D0(res, 0, 0, mode)` — общая для get/set/экспорта функция обновления
состояния ресурса (`mode` = `0xA0000000` для чтения/записи из игры,
`0x80000000` для экспорта); она работает с LOD/анимацией (`[res+0x6C]`,
`[res+0x68]`, таблица `[0x619DC0]`) и в создание текстур не вмешивается.

Таким образом: **`doPaint` находит поверхность через `state.texture` детали**
(`0x45D910` → реестр `[0x62E008]`/`0x42AB50` → `[res+0x0C]` → `0x4FDC40`),
`paintPart` работает с той же поверхностью (оба вызывают `0x48D450`), а
`endPaint` — только Java-флаг, после которого Java перерисовывает UI.

### 4. Источник размеров: разбор вариантов

| Вариант | Что должно быть в коде | Что найдено |
|---|---|---|
| **A. Копия `paint_source`** (`PartConfig::texture`) | paint-натив получает id слота и создаёт по нему текстуру | **не подтверждается**: id `paint_source` не передаётся ни в один paint-натив; чтения `PartConfig::texture` в paint-пути нет |
| **B. Текстура текущего цвета (`state.texture`)** | paint пишет в существующую текстуру ресурса | **подтверждается**: `0x48D450` берёт текстуру через `0x4FDC40(res)` из ресурса, назначенного детали; создания/клонирования нет |
| **C. Отдельная канва Painter** | собственный `CreateTexture` с собственными размерами | **не подтверждается**: создания текстуры в `0x4827D0/0x482960/0x482C00/0x48D450` нет |
| **D. Иное** | — | не обнаружено |

Формально: paint-путь вообще не создаёт поверхность, он **мутирует уже
существующую** текстуру того ресурса, который деталь получила ранее. Значит
размер paint surface = размер этой текстуры, а её размер — из файла/загрузчика
цветовой текстуры (для заводской палитры — 256×256 PNG из
`cars\textures\factory_colors\Einvagen\`).

Слабое место вывода: шаг «ресурс стал временным и получил собственную
D3D-текстуру» не локализован (см. §2). Логически он обязан быть, потому что в
сохранении все 14 текстур — временные; вероятное место — материализация
пакованного ресурса при запросе доступа на запись
(`0x5447D0(res, 0, 0, 0xA0000000)`), но это гипотеза, а не найденный код.

### 5. Назначение результата

- Новый `ResourceRef` **не** создаётся: `duplicate` — присваивание ссылки
  (`0x4802C0` → `0x48D860`), `new ResourceRef()` в Java не порождает ресурс.
- Текстурный id детали меняется только там, где Java явно вызывает
  `setTexture(...)` (native `0x469B80`): при загрузке машины/скина и в режиме
  `MODE_COPY`. Native пишет значение в запись детали — это и есть
  `Record::state.texture`.
- Дальше — уже установленная цепочка: `Part.save()` пишет `ResourceRef`
  (этап 7) → `Vehicle.saveSkin`/`Vehicle.save` → SDAT-манифест; временные
  текстуры попадают в таблицу несохранённых ресурсов как `kind 7`, а их данные
  выгружаются в sidecar (`0x484FA0` → `0x48DA30` → `0x506460` → `0x5536F0`,
  этапы 10–11). Размер JPEG читается из `GetLevelDesc(0)` той же текстуры
  (`0x4F6000`), в которую рисовал Painter.

### 6. Пользовательский сценарий с 2K

Практический результат (цветовые текстуры в `cars\textures` 256 → 2048 дают
скины повышенного разрешения) объясняется так:

1. Деталь при спавне получает `state.texture` = id заводской текстуры цвета
   (`CARCOLORS[i]` → `cars.rkp:0x42…0x50` → `cars\textures\factory_colors\…png`).
2. Эта текстура загружается с диска **в своём разрешении** (loose `sourcefile`,
   не встроенные байты пака) — при подмене файла на 2048×2048 загружается
   текстура 2048×2048.
3. Painter рисует **в неё же** (никакого ресайза и никакой новой канвы): цвет и
   декали накладываются в UV-пространстве этой текстуры.
4. Экспорт читает `GetLevelDesc(0)` этой текстуры → JPEG 2048×2048; IJG пишет
   без масштабирования, ограничение только со стороны D3D9
   (`MaxTextureWidth/Height` устройства) и памяти.

Условия: подмена файла должна попасть в загружаемую текстуру (loose
`sourcefile`), и покраска должна произойти после загрузки текстуры в новом
разрешении. Если деталь уже имеет временную текстуру (после предыдущей
покраски/копирования), подмена файлов на неё не влияет — она унаследовала
размер в момент своего создания.

Уточнение к этапу 11: вариант «канва создаётся из `paint_source`»
(`default_white.png`) **не подтверждается** — paint-путь не видит `paint_source`
вообще. Наиболее согласованный с кодом вариант — **B**: разрешение наследуется
от текстуры, назначенной детали (`state.texture`), то есть от цветовой текстуры
из `cars\textures`. Это соответствует и наблюдению пользователя, если
увеличивались именно те цветовые текстуры, которые были назначены деталям.

### 7. Схемы

**Инициализация (до рисования):**

```
GameLogic.CARCOLORS[i]  (int, id текстуры)                    [Java, d1/Chassis.java:717]
   ↓ Descriptor(color) → Chassis.addStockParts → Part.setTexture(id)   [Java]
   ↓ native setTexture(I)I  @0x469B80                          [высокая]
        резолв id → реестр [0x62E008] / 0x42AB50                [высокая]
        [res+0x4C] != 1 → [vtbl+0x14](1.0f)  (подготовка)       [высокая]
        0x5447D0(res,0,0,0xA0000000)                            [высокая]
        [vtbl+0xC](1.0f) → объект текстуры                      [высокая]
   ↓ текстура грузится из loose sourcefile (cars\textures\…) с её разрешением [высокая]
   ↓ запись детали: Record::state.texture = id                  [высокая]
   ⇒ PartConfig::texture (paint_source) в этом пути не участвует [средняя-высокая]
```

**Рисование и сохранение:**

```
Painter.doPaint(part,…) → native 0x482960  /  paintPart → 0x4827D0   [высокая]
   ↓ 0x45D910 резолв GameRef; 0x426470 getInfo(GII_TYPE)             [высокая]
   ↓ 0x48D450: 0x4FDC40(res) → текстура; 0x4FDC80/0x516C20 захват    [высокая]
                0x4FDB70 UV→пиксели; 0x49E3D0 отрисовка в текстуру    [высокая]
   ↓ (создания новой текстуры в этом пути нет)                        [высокая]
   ↓ Part.getTexture() @0x469AB0 → id для Part.save()                 [высокая]
   ↓ Part.save → ResourceRef(id)                                      [высокая, этап 7]
   ↓ SDAT: несохранённый ресурс kind 7 → ordinal                      [высокая, этапы 9–10]
   ↓ 0x484FA0 → 0x48DA30 → 0x506460 → GetLevelDesc(0) → 0x5536F0 (IJG v6a) → файл
                                                                      [высокая, этап 11]
```

### 8. Ответы на восемь вопросов

1. **Из какой текстуры создаётся начальная paint surface?** Отдельная
   поверхность не создаётся: Painter пишет в D3D-текстуру того ресурса, на
   который указывает `state.texture` детали, — то есть в текущую цветовую
   текстуру (при спавне — заводская `cars\textures\factory_colors\…png`).
2. **Где именно создаётся поверхность?** Не найдено. В paint-пути
   (`0x4827D0`, `0x482960`, `0x482C00`, `0x48D450`) вызовов `CreateTexture`
   нет; функции-создатели текстур (`0x4F5E70`, `0x4F6360`, `0x4F6580`,
   `0x4F6E30`, `0x4F9AA0`, `0x5011B0`, `0x501420`) вызываются из менеджера
   текстур и из экспортёра, не из покраски.
3. **Откуда берутся её размеры?** Из уже загруженной текстуры назначенного
   ресурса (её D3D-дескриптора); для палитры Einvagen — из PNG
   `factory_colors\Einvagen\*.png` (в стоке 256×256).
4. **Почему 2K в `cars\textures` повышает разрешение скина?** Потому что
   деталь рисуется прямо в назначенную цветовую текстуру, она грузится с диска
   в своём разрешении, а экспорт снимает размеры с неё же — без ресайза.
5. **Создаётся ли отдельная поверхность для каждой детали?** Отдельная канва —
   нет. Но текстурный ресурс у детали свой: в исследованном сейве 14 разных
   временных текстур (ordinal 1…14). Разделяются ли они между деталями —
   зависит от `setTexture`/`duplicate`: `duplicate` лишь разделяет ссылку, так
   что две детали могут указывать на один ресурс.
6. **Когда результат становится новым ResourceRef?** Никогда в процессе
   рисования: `duplicate` — присваивание ссылки (`0x48D860`). Новый id
   появляется только если Java вызывает `setTexture(...)` (native `0x469B80`)
   с чужим/новым id.
7. **Как новая текстура назначается детали?** Через `Part.setTexture(id)`
   (`0x469B80`), записывающий id в запись детали (`Record::state.texture`).
8. **Что подтверждено, а что нет?** Подтверждено: набор native-методов и их
   адреса (§2), путь данных paint→текстура (§3), отсутствие создания и
   клонирования в paint-пути, семантика `duplicate`, источник размеров экспорта
   (этап 11), палитра заводских цветов (§1). Не установлено: где и когда
   ресурс текстуры детали становится временным (создание D3D-текстуры), и,
   следовательно, точная точка наследования разрешения при материализации.

### 9. Неизвестное и технические ограничения

- Не найдена функция, создающая D3D-текстуру для ресурса детали. Гипотеза:
  материализация при запросе доступа на запись
  (`0x5447D0(res, 0, 0, 0xA0000000)`) либо отдельный путь загрузки/промоушена
  ресурса; проверки не хватило (функция большая, работает с LOD/анимацией).
- `GameLogic` не входит в 39 декомпилированных классов, поэтому полный список
  значений `int[21] CARCOLORS` из байткода не извлечён; палитра восстановлена
  по `cars.rpk` (0x42…0x50 + `Used` 0x65…0x68).
- Для сейва `2-1` нет `PlayerCar0`: конкретный исходный цвет деталей до
  покраски неизвестен; вывод о классе значений сделан по палитре и по
  сохранению `1-1` (этап 7: `0x00020050` = `Saratoga.png`).
- `0x48D450` разобран частично (первые ~0x190 байт и фрагмент от `0x48D5BE`);
  полного разбора отрисовки (`0x49E3D0`) не делалось — для вопроса о размерах
  это не требуется.
- `0x4F9AA0` (2×`CreateTexture` + `LockRect`) остаётся лучшим кандидатом на
  «клон текстуры», но вызывающих его сайтов найти не удалось: возможен вызов
  через указатель/таблицу.
- Состояние рабочего дерева: `cars\textures\default_white.png` изменён
  посторонним процессом (см. шапку раздела); аудит файл только читал.

Документ оставлен незакоммиченным; коммит не создавался.

---

## Этап 13 — идентичность ресурса, изменяемого Painter

**Проверяемый HEAD:** `06ce08ec75fe2882ee1d4c3c311989ec28678eb8`.
Read-only: игра не запускалась, EXE/DLL/C++/Java/шейдеры/RPK/SCX/сохранения и
sidecar-файлы не изменялись. Разделы этапов 2–12 не переписывались; уточнения к
собственным формулировкам этапа 12 даны отдельным подразделом §2.4.

### 1. Аргументы и типы native Painter

**Первый аргумент — не указатель.** Все нативные методы игры получают аргументы
через общий распаковщик `0x45D910`: он берёт дескриптор метода из VM-объекта
(`[esi+0xC] → [+0x14]`, вызов `0x41DEF0`), разбирает его по таблице переходов
`0x45DB5C` (индекс = символ дескриптора минус `0x46`, т.е. `'F'`, `'I'`, `'J'`,
`'L'`, `'['`, …; конец — `')'` = `0x29`) и складывает **32-битные значения**
слотов Java-стека в выходные параметры:

```
0045D938  cmp ecx, ebx (0)        ; ecx = &out
0045D93A  je  ...
0045D93C  mov edx, [eax + 8]      ; eax = объект аргумента, +8 = значение (int/handle)
0045D93F  mov [ecx], edx          ; -> *out
```

Отсюда: `GameRef` в нативном коде — это **32-битный токен** (Java `GameRef`
хранит `int`), а не C++-указатель на `Part` и не `ResourceRef`. Части и их
ресурсы живут в Java-куче, поэтому натив обращается к ним через VM-вызовы и
реестр ресурсов.

| Метод | Реализация | Первый аргумент после распаковки | Что native делает с ним |
|---|---|---|---|
| `paintPart(GameRef;I)V` | `0x4827D0` | токен (`int`) + цвет (`int`) | `0x426470(vm=0x636338, токен, 2, &out)` — вызов Java-метода `getInfo(GII_TYPE)`; результат сравнивается с `9` и `5` (категория объекта) |
| `doPaint(GameRef;IIIFFI)V` | `0x482960` | токен + цвет + id декали + 0 + rot + size + flip | тот же `0x426470(…,2,&out)`, затем `0x545FC0` (снятие узла из списка), затем ядро покраски |
| `xPaint(GameRef;)V` | `0x482C00` | токен | вызов Java-метода `0x426800(vm, obj, 0x10, "bigdecal 0,0 0,0,0 0,0,0 0 0 1 0")` — сам не рисует |
| `Part.getTexture()I` | `0x469AB0` | — | резолв записи детали → `[rec+0xC]` = ресурс → `0x5447D0(res,0,0,0xA0000000)` → `[vtbl+0xC](1.0f)` → текстура; возвращает id |
| `Part.setTexture(I)I` | `0x469B80` | id (`int`) | резолв ресурса → подготовка (`[vtbl+0x14](1.0f)`, `0x5447D0(res,0,0,0x20000000)`) → `push 7; call 0x546070` — запись ссылки типа 7 в запись детали |

### 2. Переход к `res` и к D3D9 surface

**2.1. Ядро покраски `0x48D450` не получает ни `res`, ни id текстуры.** Его
аргументы — локальная структура запроса (токен, цвет, координаты, флаги),
собранная на стеке вызывающего (`0x482837`–`0x482861`: десять `push`), а цель
покраски берётся из **контекста менеджера текстур** `[0x6187B0]`:

```
0048D4FB  mov ecx, [0x6187B0]         ; менеджер
0048D501  push esi (0)
0048D502  call 0x4FDC40               ; -> [mgr+0xBC]  (голова цепочки)
0048D507  mov edi, eax                ; edi = элемент контекста
0048D509  cmp edi, 0 ; je -> return 0
0048D511  fld  [edi+0x5C]             ; x     \
0048D518  fadd st(0),st(0) / fsub 1.0 ;        } нормализация координат
0048D520  fsubr [eax]                 ; u     } относительно окна элемента
0048D522  fdiv [edi+0x64]             ; / w   /
0048D52F  fld [edi+0x60] … fdiv [edi+0x68]   ; аналогично для v и h
0048D5A5  call 0x4FDC80(mgr, edi)     ; -> 0x516C20(edi, 0) = объект поверхности
0048D5AA  mov esi, eax                ; esi = ПОВЕРХНОСТЬ, в которую идёт запись
0048D5E7  call 0x4FDB70(...)          ; UV -> пиксели
0048D68D  call 0x49E3D0(renderer=[0x617650], esi, coords, target@+0x658)
0048D6A1  call 0x4FDCA0(mgr, esi)     ; следующий элемент цепочки
0048D6B2  call 0x4FDC40(mgr, edi)     ; продолжение обхода
```

Аксессор `0x4FDC40(mgr, arg)`:

```
004FDC40  mov eax,[esp+4] ; test eax,eax ; jne -> (arg)
004FDC48  mov eax,[ecx+0xAC]          ; список слоёв
004FDC4E  mov edx,[eax+4] ; test ; je -> 0
004FDC55  mov eax,[ecx+0xBC]          ; голова цепочки целей
004FDC5E  (arg)  mov eax,[eax+8] ; mov eax,[eax+8]   ; -> следующий элемент
```

То есть цель покраски — это **цепочка «слоёв/материалов» внутри менеджера**
(`+0xAC` — список, `+0xBC` — голова), каждый элемент хранит окно
`x,y,w,h` в `+0x5C…+0x68`; обход затрагивает все элементы (у детали может
краситься не одна карта). Идентичность ресурса входит в эту цепочку не через
аргумент натива, а через предварительную настройку контекста.

**2.2. Что известно о настройке контекста.** Она выполняется до `0x48D450` в
`paintPart`/`doPaint`, но конкретный сеттер поля `[mgr+0xBC]` не локализован:
поиск записей `mov [reg+0xBC], r32` рядом с обращением к `[0x6187B0]` дал только
конструкторы посторонних объектов (`0x52A353`, `0x52A8A3`, `0x52AD4F` — с
vtables `0x5F3354`, `0x5F338C`, `0x5F33C4`).

**2.3. Что это доказывает.** Токен `GameRef` описывает деталь; натив спрашивает
у Java её тип (`getInfo(2)`), а пиксели пишет в поверхности из контекста
менеджера. Значит связь «деталь → её текстура → поверхность» материализуется
между этими двумя шагами, и этот шаг — граница нынешнего доказательства.

**2.4. Уточнение к этапу 12.** Там было сказано, что ядро покраски «берёт
текстуру ресурса» через `0x4FDC40(res)`. Точнее: `0x4FDC40` вызывается с
аргументом **0** и возвращает `[mgr+0xBC]` — голову цепочки контекста, а не
ресурс детали. Формулировка «paint core получает существующий текстурный
ресурс» остаётся верной по смыслу (создания текстуры в ядре действительно нет),
но указатель приходит из контекста менеджера, а не из аргумента натива.

### 3. Связь с `Record::state.texture`

| Гипотеза | Аргументы за | Аргументы против | Вердикт |
|---|---|---|---|
| Цель = текстура текущего `state.texture` | (а) Java не меняет id в режимах рисования (§4); (б) в результате покраски появляются ровно те временные текстуры, что попали в манифест (14 деталей → 14 blob’ов); (в) экспорт читает `GetLevelDesc` той же поверхности, что видна рендеру; (г) иных кандидатов в контексте нет | прямого перехода «токен → `state.texture` → `[mgr+0xBC]`» не найдено | **подтверждено частично** (по совокупности, не по указателю) |
| Цель = `paint_source` (`PartConfig::texture`) | — | `paint_source` не передаётся ни в один paint-натив и не читается в paint-пути (этап 12) | **опровергнуто** |
| Цель = результат `duplicate()` | `duplicate` вызывается в `MODE_COPY` | `duplicate` — разделение ссылки (`0x48D860`), новых текстур не создаёт | **опровергнуто** |
| Цель = собственный ресурс Painter | — | Painter (Java) не хранит текстур кроме `copy_prev`/`copy_texture` (UI-превью) и `paintBrush` (декаль) | **опровергнуто** |

Косвенное усиление пункта (б): в `Vehicle.saveSkin` пишутся **все** детали с
`getTexture() != 0`, и в манифесте исследованного скина ровно 14 записей с
`kind 7` — столько же, сколько красилось; если бы Painter писал в отдельную
свою поверхность, эта поверхность не имела бы отношения к `getTexture()` и в
манифест не попала бы.

### 4. Все вызовы `Part.setTexture()` в декомпилированном коде

| # | Место | Режим/контекст | Что записывается | Меняет ли содержимое ресурса |
|---|---|---|---|---|
| 1 | `Part.java:130` (`load`) | загрузка сохранения | id из `readResID()` | нет (только ссылка) |
| 2 | `Part.java:619` (`addStockParts(Descriptor)`) | спавн детали | `arg0.color` (заводской id) | нет |
| 3 | `Part.java:660` (`addPart(...)`) | установка новой детали | `arg2.color` | нет |
| 4 | `d1/Chassis.java:493` (`addStockParts(Descriptor)`) | спавн шасси | `arg0.color` | нет |
| 5 | `Vehicle.java:609` (`loadSkin`) | применение скина | id из пары манифеста | нет |
| 6 | `Painter.java:631` (`MODE_COPY`) | копирование краски | `local14.id()` (= id источника) | нет |

**Ни одного вызова в `MODE_PAINTCOLOR`, `MODE_PAINTPART`, `MODE_PAINTDECAL`.**
В этих режимах Java вызывает только `paintPart(...)`, `doPaint(...)`,
`xPaint(...)` и не переназначает текстуру. Следовательно Painter **меняет
пиксели уже назначенного ресурса**, а не подменяет ресурс.

Четыре операции разделяются явно:

| Операция | Где | Подтверждение |
|---|---|---|
| назначение ресурса детали | `Part.setTexture` native `0x469B80` (`push 7` + `0x546070`) | высокая |
| получение доступа к ресурсу | `[0x62E008]`/`0x42AB50` → `[res+0xC]` → `0x5447D0(res,0,0,mode)` → `[vtbl+0xC](1.0f)` | высокая |
| изменение пикселей ресурса | `0x48D450` → `0x516C20` → `0x49E3D0` (в контексте `[mgr+0xBC]`) | высокая для механизма, средняя для тождества ресурса |
| сохранение изменённого | `Part.save` → `ResourceRef` → SDAT blob `kind 7` → sidecar JPEG (этапы 7–11) | высокая |

### 5. Семантика `MODE_COPY`

```java
// Painter.java:625-632
ResourceRef local14 = new ResourceRef();
local14.duplicate(new ResourceRef(copy_texture));
local12_3.setTexture(local14.id());
```

`duplicate` = native `0x4802C0` → `0x48D860`. Восстановленная семантика
`0x48D860` (полный разбор в этапе 12):

- параметр — объект-приёмник, `this` — текущая ссылка;
- проверка типа ресурса `[res+0x4C] ∈ {0x0E, 0x0D, 5, 7}` (render-типы) — иначе
  только отвязка;
- снятие узла со старого списка (`+0x44`/`+0x48`), вставка в новый, правка
  счётчика `+4` (`dec`/`mov`), копирование `id`-поля (`[res+0x50] → [esi+8]`).

Следствия:

- `local14.id()` возвращает **тот же id**, что и `copy_texture` — нового ресурса
  не создаётся;
- после `setTexture` обе детали ссылаются на **один** ресурс (refcount +1);
- механизма copy-on-write в найденном коде нет: рисование пишет в поверхность
  ресурса напрямую (`0x48D450` → `0x49E3D0`), без проверки «сколько
  владельцев»;
- освобождение — при обнулении счётчика `+4` в `0x48D860` (unlink-ветка),
  освобождение поверхности выполняет менеджер текстур.

Поэтому `MODE_COPY` — это не «копия пикселей», а «назначить ту же краску».
Чтобы получить независимую копию, Java должна была бы создать новый ресурс
(`ResourceRef.makeTexture(...)` по пути к файлу), но в режимах рисования такого
вызова нет.

### 6. Жизненный цикл Resource ID

| Стадия | Значение | Где | Подтверждение |
|---|---|---|---|
| заводской ID | packed `(pack<<16)|local`, напр. `0x00020050` = `Saratoga.png` | `GameLogic.CARCOLORS` → `Descriptor.color` → `Part.setTexture` | высокая (этап 12) |
| назначение детали | `Record::state.texture` = тот же int | native `0x469B80` → `0x546070` | высокая |
| доступ | резолв ресурса по id + `0x5447D0(res,0,0,mode)` | `0x469AB0`/`0x469B80`/`0x48DA30` | высокая |
| изменение | пиксели поверхности (id не меняется) | `0x48D450`/`0x49E3D0` | высокая (механизм) |
| превращение в временный ресурс | id вида «временный» в `state.texture` | не локализовано | **неизвестно** |
| запись в SDAT | временный → `kind 7` + `ordinal` | `sdat`-writer `0x484FA0` (этап 10) | высокая |
| sidecar | `<манифест>.<ordinal>` | `0x4850E3` → `0x48DA30` → `0x506460` (этап 11) | высокая |
| восстановление | пара `(тип детали, ordinal)` → `setTexture` | `Vehicle.loadSkin` (этап 9) | высокая |

Разделение, которое нельзя смешивать: **идентичность ресурса** (объект в
реестре) ≠ **ID** (`int` в записи детали) ≠ **указатель на D3D9-текстуру**
(`[vtbl+0xC]`) ≠ **содержимое текстуры** (пиксели) ≠ **номер sidecar**
(`ordinal`, порядковый номер в таблице несохранённых ресурсов конкретного
файла).

### 7. Схемы

**Назначение и изменение:**

```
GameLogic.CARCOLORS[i] / skin pair / Descriptor.color        [Java]
   │ подтверждено
   ▼
Part.setTexture(id)  native 0x469B80
   │ подтверждено
   ▼
реестр ресурсов [0x62E008] / 0x42AB50 → объект ресурса
   │ подтверждено
   ▼
0x5447D0(res,0,0,mode) + [vtbl+0x14](1.0f) → [vtbl+0xC](1.0f) = D3D9 texture
   │ подтверждено
   ▼
запись ссылки типа 7 в запись детали (push 7; 0x546070) → Record::state.texture
   │ подтверждено частично (поле записи не идентифицировано по смещению)

Painter.doPaint(token,…) → native 0x482960                            [подтверждено]
   │ 0x45D910 распаковка аргументов → 32-bit токен                     [подтверждено]
   ▼ 0x426470(vm, токен, 2) = getInfo(GII_TYPE)                        [подтверждено частично]
   ▼ 0x48D450: контекст [mgr 0x6187B0]+0xBC → цепочка целей            [подтверждено]
   ▼ 0x4FDC80 → 0x516C20(el,0) = поверхность                          [подтверждено]
   ▼ 0x49E3D0(renderer, поверхность, uv, target) = запись пикселей     [подтверждено]
   ▓ переход «токен → state.texture → контекст»                       [НЕИЗВЕСТНО]
```

**Сохранение:**

```
Record::state.texture (int) → Part.getTexture 0x469AB0                [подтверждено]
   ▼ Part.save пишет ResourceRef(id)                                  [подтверждено, этап 7]
   ▼ SDAT: ресурс несохранённый → kind 7 + ordinal                    [подтверждено, этап 10]
   ▼ 0x484FA0 → 0x48DA30 → 0x506460 → 0x4F6000 GetLevelDesc(0)        [подтверждено, этап 11]
   ▼ 0x5536F0 (IJG v6a, q=глобальная 0x6188B0) → буфер → файл         [подтверждено]
   ▼ <манифест>.<ordinal>, JPEG                                      [подтверждено]
   ▓ переход packed-ID → временный ресурс                             [НЕИЗВЕСТНО]
```

### 8. Ответы

1. **В какую текстуру пишет `doPaint()`?** В поверхность, взятую из цепочки
   контекста менеджера (`[mgr+0xBC]` → `0x4FDC80` → `0x516C20(el,0)`); аргумент
   натива содержит только токен детали и параметры кисти. Что именно попало в
   эту цепочку для конкретной детали — не локализовано.
2. **Совпадает ли она с текущим `state.texture`?** На уровне указателя —
   не доказано. По совокупности косвенных данных (Java не меняет id при
   рисовании; в манифесте ровно столько временных текстур, сколько красилось;
   экспорт читает ту же поверхность) — **вероятно да**, статус «подтверждено
   частично».
3. **Меняет ли Painter ID ресурса?** Нет. Во всех режимах рисования Java не
   вызывает `setTexture`; меняются только пиксели поверхности.
4. **Зачем `setTexture` в `MODE_COPY`?** Чтобы целевая деталь стала ссылаться
   на тот же ресурс, что источник: `duplicate` копирует ссылку, а не пиксели,
   поэтому без `setTexture` «копирование краски» не произошло бы.
5. **Разделяют ли детали одну texture после `duplicate()`?** Да: один ресурс,
   refcount +1, `id()` одинаковый.
6. **Может ли изменение текстуры одной детали повлиять на другую?** Да, если
   они делят ресурс: запись идёт в его поверхность напрямую, copy-on-write не
   найден.
7. **Как постоянный RPK ID превращается в ресурс, сохраняемый через sidecar?**
   В сохранении ресурс оказывается «несохранённым» (`kind 7` + ordinal) — то
   есть к моменту сохранения он уже не пакованный. Точка превращения не найдена;
   ниже неё цепочка прослежена полностью (ordinal → сторона → JPEG).
8. **Что остаётся неизвестным?** (а) точка создания/промоушена временного
   ресурса и наследования им размеров; (б) поле записи детали, куда
   `0x546070` пишет ссылку типа 7; (в) сеттер контекста `[mgr+0xBC]` и переход
   «токен → текстура»; (г) наличие/отсутствие copy-on-write в менеджере
   ресурсов.

### 9. Неизвестное и технические ограничения

- Главная граница: не найден код, наполняющий paint-контекст менеджера
  (`[0x6187B0]+0xBC`) текстурами конкретной детали. Без него тождество
  «поверхность Painter == поверхность `state.texture`» остаётся косвенным
  выводом.
- `0x48D450` разобран фрагментами (0x48D450–0x48D5BE, 0x48D5BE–0x48D6D1);
  функция отрисовки `0x49E3D0` не разбиралась — она принимает уже готовую
  поверхность и на вопрос об идентичности не влияет.
- `0x546070` (запись ссылки типа 7 в запись детали) не разобран до конца:
  виден тип `7` и вызов, но не проверено, в какое поле записи пишется id и
  пишется ли вообще id (возможно, только указатель на объект ресурса).
- Семантика флагов доступа `0x5447D0` (`0xA0000000`, `0x20000000`,
  `0x80000000`) не расшифрована; предполагается «режим доступа/использования»,
  но это не доказано.
- Механизм освобождения поверхности при refcount → 0 в менеджере текстур не
  прослежен.
- Все выводы сделаны по дизассемблеру без символов (PDB/карты нет) и по
  декомпилированному Java; игра не запускалась, поведение не проверялось
  эмпирически.

Документ оставлен незакоммиченным; коммит не создавался.

---

## Этап 14 — связывание ресурсов, контекст Painter и доступ на запись

**Проверяемый HEAD:** `06ce08ec75fe2882ee1d4c3c311989ec28678eb8`.
Read-only: игра не запускалась, EXE/DLL/Java/C++/шейдеры/RPK/SCX/сохранения не
изменялись. Разделы этапов 2–13 не переписывались.

### 1. `0x5447D0` — «обеспечить резидентность ресурса»

**Сигнатура** (по восстановлению стека на входе и `ret 0xC`):

```
int  __thiscall EnsureResident(this = res, u32 a1, u32 a2, u32 mode, float f1, float f2)
```

Разбор кадра: `sub esp,8; push ebx` → `ebx = a5`; `push ebp` → `ebp = a4`;
`push esi/push edi` → `edi = a3 = mode`, `esi = ecx = res`.

**Возвращает** `int`: `0` — успех, `0x80000001` — ресурс не готов
(`0x5449D1: mov eax, 0x80000001` при `test byte [res+0x64],1 == 0`). Вызывающие
проверяют именно старший бит: `0x469BE6` и `0x48DA5A` — `test eax,0x80000000;
jne <ошибка>`.

**Что делает** (весь путь, 0x5447D0–0x544A05):

| Участок | Условие | Действие |
|---|---|---|
| `0x5447E3` | `mode & 0x40000000` | пропустить блок синхронизации параметров устройства |
| `0x5447F2` | `[res+0x64] & 1` и `[res+0x6C]` vs `[res+0x68]` (float-сравнения) | подготовка к обновлению уровня детализации |
| `0x544823` | `[res+0x64] & 0x10000000` | обход (ресурс не требует обновления) |
| `0x544832` | `[res+0x54] & 0x2000000` + `f1`/`f2` vs `[0x5E73CC]` | обход |
| `0x54485D` | — | `0x537240(device=[0x618D48], res, …)` — синхронизация параметров текстуры (внутри: `[res+0x5C]`, `[res+0x50]>>16`, `0x544B90`, запись `[res+0x68]=1.0f`) |
| `0x544870` | `!(mode & 0x20000000)` и `[res+0x54] & 0x100` | `0x537790(device, res, …, [res+0x6C])` — выгрузка пиксельных данных на устройство (внутри `call [vtbl+0x10]`, проверка `[res+0x54] & 0x4000000`) |
| `0x544895` | `type = [res+0x4C]`, `bool[type]` из таблицы `0x619DC0` | таблица `0x619DC0` — dword-массив, ненулевой только элемент `7` (текстура); для остальных типов сразу к `0x5449C8` |
| `0x5448A7`–`0x5449C8` | `type == 7` | сравнение `[res+0x68]` (требуемый уровень) и `[res+0x6C]` (текущий), вызовы виртуальных `[vtbl+0x4]` и `[vtbl+0x8]` (получение размеров/уровней), проверка `[res+0x64] & 0x10000000`, повторная проверка `[res+0x54] & 0x2000000` |
| `0x5449C8` | `[res+0x64] & 1 == 0` | `return 0x80000001` |
| `0x5449DD` | `a1 == 0` и `[res+0x74] != [0x6200A4]` (номер кадра) | `0x537D10(device, res)` — «касание» ресурса: запись `[res+0x74] = [0x6200A4]`, снятие с LRU-списка (`+0xC`/`+0x10`), перестановка в списке по типу (`[res+0x4C]*0x90 + device + 0x140`) |
| `0x5449FC` | всегда | `return 0` |

**Режимы (по реальным call sites, а не по числам):**

| Call site | Аргументы | Что означает на практике |
|---|---|---|
| `0x469BDE` (`Part.setTexture`) | `(res, 0, 0, 0xA0000000)` | `0x80000000|0x20000000` — **без выгрузки пикселей** (назначение не требует загрузки) |
| `0x48DA55` (JPEG-экспорт) | `(res, 0, 0, 0x80000000)` | полный путь, включая выгрузку (`0x20000000` нет) — экспорт читает уже валидную поверхность |
| `0x48DA55`-семейство (`0x48DAC5`) | см. этап 11 | тот же вид вызова |
| `0x48D501`-путь покраски | `0x4FDC40(mgr,0)` + `0x4FDC80` | `0x5447D0` в этом пути не вызывается |
| остальные ~300 сайтов | как правило `(res,0,0,0x80000000)` или `(res,0,0,0)` | обычная проверка готовности перед использованием |

**Вывод.** `0x5447D0` — это **`EnsureResident`: проверка/обновление готовности уже
существующего узла** (параметры устройства, уровень детализации, выгрузка
пикселей, LRU-«касание»). Внутри нет: создания узла, копирования пикселей,
смены `id`, изменения refcount. Бит `0x40000000` — «не синхронизировать
параметры», `0x20000000` — «не выгружать пиксели». Бит `0x80000000` внутри
функции **не тестируется** (есть только `test edi,0x40000000`, `test
edi,0x20000000`, `test dword [esi+0x64],…`), поэтому его роль в самой функции —
нулевая; он используется вызывающими как соглашение «требую валидности»
(проверка `test eax,0x80000000` после вызова). Приписывать `0x80000000`
отдельную семантику внутри `0x5447D0` было бы домыслом.

### 2. `0x546070` — привязка `ResourceRef` к узлу ресурса

```
void __thiscall AttachRef(this = ref, u32 id, u32 type, u32 flags)   ; ret 0xC
```

Разбор (0x546070–0x546116):

```
esi = this (ref)                 ; edi = arg1 = ID
if ([ref+8] == id) return        ; уже привязан к этому id
if ([ref+0xC]) {                 ; есть текущий узел — отцепить ref от его списка
    if ([ref+0]) [ [ref+0] + 4 ] = [ref+4];
    else         [ [ref+0xC] + 0x48 ] = [ref+4];
    if ([ref+4]) [ [ref+4] + 0 ] = [ref+0];
    [ref+0xC]=0; [ref+8]=0; [ref+0]=0; [ref+4]=0;
}
if (id == 0) return;
node = 0x536820(device=[0x618D48], id, type, flags);   ; 0x5460D9
if (!node) return;                                      ; 0x5460E0
list = node + 0x44;                                     ; 0x5460E2
[ref+8] = id;                                           ; 0x5460E5
if ([list+4]) [ [list+4] + 0 ] = ref;                   ; вставка в хвост
[ref+0] = 0;
[ref+4] = [list+4];
[list+4] = ref;
[ref+0xC] = node;                                       ; 0x546107
```

Отсюда **layout объекта `ResourceRef`**: `+0` — next, `+4` — prev, `+8` — ID
(`int`), `+0xC` — указатель на узел ресурса. Это же объясняет `0x48D860`
(`duplicate`): он копирует в `ref+8` поле `node+0x50` (ID узла) и переставляет
ref между списками узлов `+0x44`/`+0x48` — то есть `duplicate` — это частный
случай той же привязки, без обращения к `0x536820`.

**`0x536820` — резолвер ID → узел** (0x536820–0x536882):

```
pack = id >> 16
if (pack == 0xFFFF || pack >= [device + 0xFFE20])  goto common;   ; 0x536838/0x536846
rec = device + 0xFFE94 + pack * 0x70;                            ; 0x536848-0x536854
if (0x544000(rec) == 0) 0x544170(rec);     ; 0x544000 = { return rec->[0x50] } — проверка ID
common: 0x537000(device, id, type, flags)
```

- `device + 0xFFE20` — число пакетов, `device + 0xFFE94` — таблица записей
  пакетов с шагом `0x70`;
- **`pack == 0xFFFF` — отдельное пространство ID** (не пакетное). Это и есть
  технический признак «временного/рантайм» ресурса, отличающий его от
  пакованного RPK-ID; ветка ведёт себя так же, как «пакет вне диапазона»;
- `0x537000(device, id, type, flags)` (0x537000–0x5370xx) — **выдача узла из
  списка свободных**: берёт голову `device + 0x106E94`, вызывает `0x545FC0`
  (снятие из списка), линкует узел в активный список `device + 0x106EC0`
  (хвост `device + 0x106EC8`). Это и есть точка **материализации узла** —
  единственное место в исследованной цепочке, где узел берётся/создаётся.

**Вывод.** `0x546070` только связывает объект-обёртку с узлом; узел выдаётся
резолвером `0x536820` → `0x537000`. `0x546070` не копирует содержимое ресурса и
не меняет ID ресурса — он лишь фиксирует ID в обёртке (`ref+8`) и узел
(`ref+0xC`). В `Part.setTexture` вызов идёт как
`0x546070(ref, id, 7, 0)` (0x469C69), т.е. `type = 7` — тип «текстура».

### 3. Менеджер `0x6187B0` и элементы контекста

- **Создание менеджера**: конструктор в районе `0x4FE1E0`–`0x4FE290`; в
  `0x4FE24F` записывается vtable `0x5F2BE4`, затем обнуляются поля
  `+0xA4, +0xA8, +0xAC, +0xB0, +0xB8, +0xBC, +0xC0, +0xC4`. То есть **`+0xAC`
  (список элементов) и `+0xBC` (текущий элемент) стартуют нулевыми**.
- **Поиск элемента по ID**: `0x4FD220(mgr, obj)` — сначала просматривает
  список `mgr+0xC8`, затем `mgr+0xAC`, сравнивая `elem+0x1C` с `obj+8` (ID
  обёртки). Возвращает элемент или 0.
- **Удаление/освобождение**: `0x4FD390(mgr, obj)` — находит элемент по
  `obj+8`, вызывает `0x5161D0(elem)` (освобождение поверхности), отцепляет от
  `mgr+0xAC` и кладёт в список свободных `mgr+0xD8`.
- **Поверхность элемента**: `0x4FDC80(x) = x ? 0x516C20(x, 0) : 0` (0x4FDC80–
  0x4FDC90).
- **Следующий элемент цепочки**: `0x4FDC40(x≠0) = [[x+8]+8]`; `0x4FDCA0`
  аналогично (0x4FDCA0).
- **Типовая проверка**: `0x4FD150`/`0x4FD1A0` — если `node = [obj+0xC]` и
  `[node+0x4C] == 0x12`, выполняется поиск элемента (`0x4FD220`) и операция над
  ним (`0x516900` / `0x516A10`); при `[node+0x4C] == 0x15` возвращается `1`;
  иные типы — `0`.
- **Геометрия окна**: элемент хранит `x` в `+0x5C`, `y` в `+0x60`, `w` в
  `+0x64`, `h` в `+0x68` (используются в `0x48D450` как делители при
  нормализации координат).

**Кто читает контекст.** Все обращения к `0x4FDC40`, `0x4FDC80`, `0x4FDCA0`
собраны полностью:

| Функция | Вызовы |
|---|---|
| `0x4FDC40` | `0x48D502`, `0x48D6B9` — только внутри `0x48D450` |
| `0x4FDC80` | `0x4830B6`, `0x487AFB`, `0x48D5A5` |
| `0x4FDCA0` | `0x487B17`, `0x48D6A1` |
| `0x4FD220` | `0x4830A2`, `0x48626B`, `0x487AEB`, `0x4FD167`, `0x4FD1B7`, `0x4FD445`, `0x4FD475` |

**Граница доказательства.** Запись в `[mgr+0xBC]` не найдена: поиск записей
`mov [reg+0xBC], r32` в окне `+0x300` после каждой загрузки `[0x6187B0]` дал
только конструктор (`0x4FE261` — обнуление) и посторонние конструкторы
(`0x52A353`, `0x52A8A3`, `0x52AD4F` с vtable `0x5F3354`/`0x5F338C`/`0x5F33C4`).
Дополнительно установлено, что участок `0x4830A2`/`0x4830B6` **не является
частью paint-натива**: рядом с ним лезут строки `java.util.Vector`,
`dynamarker`, `java.lang.Object`, `elementData`, `elementCount` (`0x613564`,
`0x613578`, `0x613584`, `0x613598`, `0x6135A4`), а вызовы `0x42A690`/`0x42A430`
— это JNI-хелперы доступа к полям/классам. То есть структура `mgr+0xAC/+0xC8/
+0xD8/+0xBC` обслуживает не только покраску, и отождествлять её элементы
исключительно с «слоями кисти» нельзя.

### 4. Связь с `Record::state.texture`

Что доказано по указателям:

```
ResourceRef+0xC ──► узел ресурса                     [подтверждено: 0x546070/0x546107]
узел+0x4C  = тип, узел+0x50 = ID, узел+0x44 = список ref'ов
  [подтверждено: 0x544000 (return [ecx+0x50]), 0x5460E2, 0x469C05]
узел → D3D-текстура: [vtbl+0xC](1.0f) → объект, у которого +0x18 = D3D texture
  [подтверждено: одинаковый аксессор в 0x469BFA (setTexture), 0x48DA6A (экспорт),
  0x469ACx (getTexture)]
```

То есть **назначение, покраска и экспорт работают с одним и тем же узлом**,
полученным из `ref+0xC`, а D3D-текстуру все три берут через один и тот же
виртуальный аксессор `[vtbl+0xC]` (у экспортёра далее `+0x18` → `0x506460`).

Что **не** доказано: тождество узла и элемента контекста
(`mgr+0xAC`/`+0xBC`). Элемент ищется по ID обёртки (`elem+0x1C == obj+8`,
`0x4FD220`), а узел хранит ID в `+0x50` (`0x544000`). Совпадение «ID обёртки ==
ID узла» следует из `0x546070` (в `ref+8` пишется тот же ID, по которому
резолвится узел), но прямая проверка «элемент с `+0x1C == X` оборачивает узел с
`+0x50 == X`» в коде не найдена: `0x516C20(elem, 0)` не разобран до поля
поверхности.

### 5. Copy-on-write и совместное владение

Проверен весь путь «после `duplicate()` → доступ на запись»:

| Шаг | Функция | Клонирование/подмена узла |
|---|---|---|
| `duplicate` | `0x48D860` | нет: только refcount/списки, `ref+8 = node+0x50` |
| назначение | `0x546070` → `0x536820` → `0x537000` | нет: узел берётся существующий/из списка свободных по тому же ID |
| доступ для назначения | `0x5447D0(res,0,0,0xA0000000)` | нет: только проверка/подготовка |
| доступ для экспорта | `0x5447D0(res,0,0,0x80000000)` | нет |
| запись пикселей | `0x48D450` → `0x4FDC80` → `0x516C20` → `0x49E3D0` | нет: пишет в поверхность, полученную для текущего элемента |

**Вывод.** Механизма copy-on-write в исследованном пути нет: ни `duplicate`, ни
`0x5447D0`, ни `0x546070`, ни ядро покраски не создают и не подменяют узел при
первой записи; refcount (`node+0x44`-список) не участвует в выборе поведения —
единственное чтение счётчика происходит при отвязке (`0x546070`, `0x48D860`) и
в `0x537D10`/LRU. Это **не** то же самое, что «изменение одной детали обязано
изменить другую»: совместное изменение следует из того, что обе обёртки
привязаны к одному узлу (`0x546070` по одинаковому ID), а поверхность берётся
для узла/элемента без копии. Остающийся незакрытый переход — «элемент ↔ узел»
(§4), поэтому окончательная формулировка: **две детали с одинаковым ID работают
с одним узлом и одной D3D-текстурой; отдельного механизма разделения при записи
не найдено**, но тождество «поверхность элемента == D3D-текстура узла» доказано
только через совпадение аксессора, а не через разбор `0x516C20`.

### 6. Связь с SDAT/JPEG

- Экспорт получает узел напрямую из обёртки: `0x48DA30 { esi = [this+0xC] }`
  (0x48DA31), затем `0x5447D0(res,0,0,0x80000000)` — то есть **перед чтением
  пикселей выполняется полная проверка готовности, включая выгрузку**
  (`0x20000000` не выставлен), затем `[vtbl+0xC]` → объект → `+0x18` → D3D
  texture (`0x506460`) → `GetLevelDesc(0)` → JPEG.
- Признак «внешний/временный» определяется **пространством ID**: пакет
  `id>>16 == 0xFFFF` (`0x536838`) выводит ресурс из пакетной таблицы. Писатель
  SDAT для таких записей выдаёт `kind 7` + `ordinal` (этапы 9–10), читатель
  восстанавливает их по имени `<файл>.<ordinal>` (этап 10).
- `ordinal` — не ID и не свойство узла: это позиция в таблице несохранённых
  ресурсов конкретного файла (этап 9, §2).
- Момент, когда запись детали получает ID с пакетом `0xFFFF`, по-прежнему не
  локализован: `0x537000` лишь выдаёт узел, но не присваивает ID.

### 7. Схемы

**A. Идентичность ресурса**

```
Record::state.texture (int)                        [подтверждено: 0x469BDE..0x469C69]
   ▼ Part.setTexture → 0x546070(ref, id, 7, 0)      [подтверждено]
ResourceRef: +8 = id, +0xC = node                  [подтверждено: 0x5460E5/0x546107]
   ▼ 0x536820(device, id, 7, 0)                     [подтверждено]
узел: +0x4C тип, +0x50 id, +0x44 список ref'ов      [подтверждено: 0x544000, 0x5460E2]
   ▼ [vtbl+0xC](1.0f)                               [подтверждено: 0x469BFA, 0x48DA6A]
D3D9 texture (объект+0x18)                          [подтверждено частично: поле +0x18
                                                     следует из 0x506460(name) на этом объекте]
```

**B. Контекст Painter**

```
узел ресурса ──► элемент контекста mgr+0xAC / mgr+0xBC          [НЕИЗВЕСТНО]
   ▼ поиск по ID: elem+0x1C == obj+8  (0x4FD220)                 [подтверждено]
элемент: +0x5C..+0x68 = x,y,w,h                                  [подтверждено: 0x48D511-0x48D53D]
   ▼ 0x4FDC80 → 0x516C20(elem, 0)                                [подтверждено]
surface                                                            [подтверждено частично]
   ▼ 0x49E3D0(renderer=[0x617650], surface, uv, target@+0x658)    [подтверждено]
запись пикселей                                                    [подтверждено]
```

**C. Copy и сохранение**

```
ResourceRef.duplicate()  (0x48D860) → та же узел-привязка, refcount+1  [подтверждено]
   ▼ write access: 0x5447D0(res,0,0,0xA0/0x80)  — без клонирования       [подтверждено]
изменяемый узел (id не меняется)                                        [подтверждено]
   ▼ SDAT writer: id с пакетом 0xFFFF ⇒ kind 7 + ordinal                [подтверждено частично:
                                                                          пространство ID 0xFFFF
                                                                          подтверждено, присвоение — нет]
   ▼ 0x48DA30: ref+0xC → 0x5447D0(0x80000000) → [vtbl+0xC] → +0x18       [подтверждено]
   ▼ GetLevelDesc(0) → RGB → IJG v6a → JPEG → <файл>.<ordinal>           [подтверждено, этап 11]
```

### 8. Ответы

1. **Что возвращает `0x5447D0`?** Код состояния: `0` — ресурс готов, `0x80000001`
   — не готов (не выставлен флаг `[res+0x64]&1`). Никакого объекта он не
   возвращает и не создаёт.
2. **Режимы `0xA0/0x80/0x20/0x00`?** Проверяемые биты: `0x40000000` — не
   синхронизировать параметры устройства (`0x537240`), `0x20000000` — не
   выгружать пиксели (`0x537790`). `0x80000000` внутри функции не тестируется
   (соглашение вызывающих «требую валидности»). Полные значения у сайтов:
   `0xA0000000` = `0x80000000|0x20000000` (назначение текстуры), `0x80000000`
   (экспорт, полный путь), `0x00000000` (обычная проверка).
3. **Кто создаёт и наполняет `manager+0xBC`?** Создаётся (обнуляется) в
   конструкторе менеджера `0x4FE261`; наполнение — **не найдено**. Известно, что
   читают его только `0x48D502`/`0x48D6B9` внутри `0x48D450`.
4. **Откуда в него попадают текстуры?** Из элементов контекста: элемент ищется
   по ID (`0x4FD220`, сравнение `elem+0x1C`), поверхность берётся
   `0x516C20(elem,0)`. Как элемент получает свой ресурс — не локализовано;
   гейт по типу узла — `[node+0x4C] == 0x12` (`0x4FD150`).
5. **Совпадает ли ресурс в контексте с `Record::state.texture`?** Узел,
   полученный из `state.texture`, и узел, из которого берётся D3D-текстура при
   экспорте, — **один и тот же** (одинаковый путь `ref+0xC` → `[vtbl+0xC]`).
   Тождество с элементом контекста **не доказано** — это единственный
   незакрытый переход.
6. **Есть ли copy-on-write после `duplicate()`?** Нет: ни один шаг пути записи
   (`0x48D860`, `0x546070`, `0x536820`, `0x5447D0`, `0x48D450`) не создаёт и не
   подменяет узел.
7. **Может ли изменение одной детали повлиять на другую с тем же ID?** Да, при
   условии, что обе обёртки привязаны к одному узлу (`0x546070` по одному ID) и
   поверхность берётся без копии. Уточнение: это следует из общей привязки по
   ID, а не из «отсутствия копирования в `duplicate`» — сам путь записи также
   проверен и копирования не содержит.
8. **Как ресурс становится временным и попадает в SDAT/JPEG?** Технический
   признак — ID с пакетом `0xFFFF` (`0x536838`), для которого резолвер идёт
   обходным путём. Дальше писатель SDAT выдаёт `kind 7` + `ordinal`, экспорт
   берёт узел из обёртки, проверяет готовность (`0x5447D0(…,0x80000000)`),
   получает D3D-текстуру (`[vtbl+0xC]` → `+0x18`), снимает размеры
   `GetLevelDesc(0)` и кодирует JPEG. Где именно ID получает пакет `0xFFFF` —
   неизвестно.
9. **Какие переходы остаются неизвестными?** (а) запись в `manager+0xBC` и
   создание элементов контекста; (б) связь «элемент ↔ узел ресурса»
   (`0x516C20` не разобран до поля поверхности); (в) присвоение ID с пакетом
   `0xFFFF` (момент превращения в временный ресурс); (г) роль `0x545FC0` и
   точная семантика `0x537000` за пределами выдачи узла из списка свободных.

### 9. Неизвестное и технические ограничения

- `0x516C20` (поверхность элемента) и `0x5161D0` (освобождение) не разобраны;
  именно там лежит последний неподтверждённый переход схемы B.
- `manager+0xBC` — запись не найдена; поиск по `mov [reg+0xBC], r32` в окне
  `+0x300` после загрузок `[0x6187B0]` дал только конструктор и посторонние
  объекты. Участок `0x4830xx` оказался JNI-хелпером (`java.util.Vector`,
  `elementData`, `elementCount`), а не paint-кодом — это ограничивает
  интерпретацию списка `mgr+0xAC/+0xC8/+0xD8`.
- `0x537000` разобран только до выдачи узла из списка свободных; сравнение ID и
  заполнение полей узла (включая `+0x18`, `+0x50`) не локализованы.
- Таблица `0x619DC0` читалась как dword-массив: ненулевой только элемент `7`.
  Если это байтовый массив флагов, вывод «только текстуры проходят LOD-ветку»
  сохраняется, но конкретная раскладка байтов не проверена.
- `0x5447D0` содержит виртуальные вызовы `[vtbl+0x4]`, `[vtbl+0x8]`,
  `[vtbl+0x10]`, `[vtbl+0x14]` на объекте ресурса; конкретные реализации для
  типа 7 не разобраны (влияет на детали LOD, не на идентичность).
- Всё получено дизассемблированием без символов и декомпиляцией Java; игра не
  запускалась, поведение не проверялось эмпирически. Совпадение ID, размеров
  или содержимого доказательством идентичности объектов не считалось.

---

# Stage 15 — vehicle asset inventory and legacy paint groups (B1)

Ревизия кода: `0f8ce1af2f46468a7d1bfed0ecd2aeed41581602` (совпадает с
ориентиром `main`). Ресурсы: установка `E:\OpenSLR\Street Legal Racing - Redline`.
Аудит read-only: код, RPK и SCX не изменялись, коммит не создавался.

Этап фиксирует инвентарь содержимого автомобилей и точные определения
«legacy paint», на которые дальше опираются `MATERIAL_SYSTEM_ARCHITECTURE.md` и
`RENDER_DATA_CONTRACT.md`. Сам механизм подстановки текстуры (этапы 2–14) здесь
не пересматривается; ссылки на `bgfx_lighting.cpp` и `Part.setTexture` даны как
контекст, а не как повторный вывод.

Обозначения достоверности: `[Ф-рес]` — проверено по файлам RPK/CFG/SCX;
`[Ф-код]` — проверено по исходникам OpenSLRR указанной ревизии;
`[Т]` — пользовательский тест в игре; `[П]` — предварительная классификация;
`[?]` — не установлено.

---

## 15.1. Сущности, которые нельзя приравнивать друг другу

| Сущность | Что это фактически | Где живёт |
|---|---|---|
| Logical Part | «деталь» как её видит игрок и слот | нигде не хранится отдельно |
| Part Resource | запись RPK типа 8 | TOC пакета |
| CFG | текстовый файл конфигурации (`render`, `slot`, `body`, …) | `*.cfg` на диске, ссылка из `native part` |
| Java class | класс `Part`/`Wheel`/… из строки `script` | `*.class` |
| Render Resource | запись RPK типа 14: `mesh`, `texture`, `flags`, тени | TOC пакета |
| Mesh Resource | запись RPK типа 5: `sourcefile …scx` | TOC пакета |
| SCX file | `INVO`-файл с вершинами и material groups | диск |

`[Ф-рес]` Одна запись типа 8 не равна одной устанавливаемой детали (см. 15.4).
Render Resource не равен SCX: один Render ссылается на один Mesh, но несколько
Render могут вести к одному Mesh и различаться только списком текстур, а могут
вести к разным Mesh.

Три наблюдаемых схемы `[Ф-рес]`:

1. **Много Render → один Mesh → один SCX.** Например, 8 вариантов цвета диска:
   Render `0x52–0x59` → один Mesh → один SCX, различаются `texture`.
2. **Много Render → разные Mesh → разные SCX.** Например, варианты геометрии
   (`cap` с 4 корпусами).
3. **Несколько SCX в одном CFG одновременно.** Состояния света (`_CITY`,
   `_BRAKE`, `_REVERSING`, `_INDICATOR`) через `flags` и LOD-подуровни — они
   сосуществуют, а не выбираются как альтернатива.

Число SCX не является числом логических деталей, а число Render — числом
вариантов покраски.

---

## 15.2. Инвентарь SCX `[Ф-рес]`

Перечисление `**/*.scx` по корню установки, проверка сигнатуры `INVO`,
версия — `u32` по смещению 4.

| Категория | Наборов данных | SCX |
|---|---:|---:|
| Racers (`cars/racers/*_data`) | 10 | 1390 |
| AI (`cars/fake_racers/*`) | 5 | 30 |
| Traffic (`cars/traffic/*_data`) | 10 | 89 |
| Police_V3 (`cars/misc/Police_V3`) | 1 | 31 |
| **Итого наборов машин** | **26** | **1540** |
| Общие ресурсы машин | — | 24 |
| **Всего под `cars/`** | | **1564** |

- `1540 = 1390 + 30 + 89 + 31` — это ровно 26 каталогов наборов данных.
- 24 общих ресурса: `cars/meshes/` (15: `speedo`, `pin*`, `ABS_led`, `osdfont`,
  `car_info_OSD`, …), `cars/racers/meshes/` (1: `traffic_racer_default_wheel`),
  `cars/traffic/meshes/` (8: `wheel_1..4`, `traffic_wheel_1..4`).
- Версии: **для 1540** — v3 = 139, v4 = 1401. **Для 24** — v3 = 15, v4 = 9.
  Разбивку 139/1401 нельзя переносить на 24: суммы не совпадут (139 + 1401 = 1540).
- Все 1564 файла под `cars/` имеют сигнатуру `INVO`.
- Полное дерево установки содержит **3244** `.scx` (все `INVO`): `cars` 1564,
  `objects` 865, `parts` 706, `DestructionTools` 29, `frontend` 22, `misc` 18,
  `humans` 16, `maps` 12, `MW_Mod` 7, `particles` 4, `Data` 1.

SCX v3 — полноценный используемый формат (139 файлов в наборах машин: 43
Racers, 51 traffic, 25 AI, 20 Police_V3). Формат v5 не существует:
экспортёра и файлов нет.

---

## 15.3. Инвентарь RPK `[Ф-рес]`

| Каталог | Пакетов | Записей |
|---|---:|---:|
| root | 13 | 3195 |
| cars | 30 | 3969 |
| maps | 25 | 2048 |
| misc | 5 | 264 |
| parts | 9 | 2411 |
| **Всего** | **82** | **11887** |

В число входит один пользовательский пакет в `parts/` (122 записи). Без него —
**81 пакет / 11765 записей**; это объясняет более раннюю цифру 11765 как
замер того же дерева до установки этого пакета.

Ранее встречавшееся `53 / 8908` воспроизвести не удалось ни при одном
ограничении по каталогам (проверены все комбинации групп). Как действующий
baseline оно непригодно; это результат другого состояния установки или другого
сканирования, причина не установлена.

### 15.3.1. Полная гистограмма типов

Тип записи — **один байт** TOC (`ent.typeof_ = e[8]`,
`src/resources/rpk_index.cpp:173`); категории взаимоисключающие — каждое поле
имеет ровно одно значение. Полный подсчёт по всем 82 пакетам `[Ф-рес]`:

| type | записей | | type | записей |
|---|---:|---|---|---:|
| 1 | 20 | | 11 | 1 |
| 2 | 1 | | 12 | 2 |
| 3 | 2 | | 13 | 49 |
| 4 | 1 | | 14 | 3094 |
| 5 | 2670 | | 15 | 1 |
| 6 | 248 | | 16 | 1 |
| 7 | 1929 | | 17 | 1 |
| 8 | 2221 | | 18 | 1 |
| 9 | 1624 | | 19 | 19 |
| 10 | 1 | | 20 | 1 |
| | | | **Σ (20 типов)** | **11887** |

Ранее приводился частичный перечень из шести типов (5, 7, 8, 9, 13, 14) с суммой
11587 — это **не** полная гистограмма. Недостающие 300 записей распределены по
14 остальным типам: type 6 = 248, type 1 = 20, type 19 = 19, type 3 = 2,
type 12 = 2, и по одной записи у типов 2, 4, 10, 11, 15, 16, 17, 18, 20.
Сумма сходится: 11587 + 300 = 11887.

Семантика типов 13 и 15–20 известна по прототипам в `system.rpk`
(`typ_render_light`, `ins_render_sprite`, `ins_render_text`, `ins_render_horizon`,
`res_viewport`, `res_animation`, `ins_render_sound`). Тип 6 в RPK-описании
формата не документирован; в содержимом встречается как отдельная категория
(248 записей), его назначение в этом этапе не исследовалось.

---

## 15.4. Part Resource ≠ деталь `[Ф-рес]`

623 записи типа 8 в десяти racer-пакетах, разложенные по payload:

| Категория payload | Кол-во |
|---|---:|
| `native part <cfg>` — обычная деталь | **455** |
| `native car <_main_*.cfg>` — вариант кузова | 26 |
| только `script` — контроллеры (`*_models`, `*_VT`, `kit_*`) | 26 |
| маркеры с данными (`lod_amp`, `>Stock`, `>Running`, `>pipes`, …) | 87 |
| пустые алиасы (`>Replacement`, `>Body`, `>seats`, …) | 29 |
| **Всего** | **623** |

Сумма проверена: `455 + 26 + 26 + 87 + 29 = 623`.

По пакетам: Baiern 71, Duhen 56, Einwagen 62, Enula 68, Focer 58, MC 55,
Mazda 3 2005 76, Nonus 57, Prime 71, SuperDuty 49.

Для 455 настоящих деталей `[Ф-рес]`: `native part` ссылается на 455 различных
CFG, повторно используемых CFG — **0**. То есть «одна запись детали → свой CFG»
для этих десяти пакетов подтверждено.

### 15.4.1. Декларации `script` у Part Resource

| Метрика | Значение |
|---|---:|
| записей типа 8 | 623 |
| Part Resource с декларацией `script` | **507** |
| различных значений пути `script` | **507** |
| различных basename (имя файла без каталога) | 472 |
| существующих файлов по этим путям | 507 из 507 |

`[Ф-рес]` 507 = 455 деталей + 26 вариантов кузова + 26 контроллеров. Все 116
маркеров (87 + 29) строки `script` не имеют. Все 507 значений пути `script`
различны и указывают на существующие `.class`; 472 basename вместо 507 —
совпадение имён файлов в разных каталогах.

Это **не** «507 уникальных Java-классов»: установлено только 507 различных
путей `script` и наличие файлов по ним. Уникальность Java-типов, разрешение
имён и загрузку классов этот подсчёт не покрывает — для такого вывода нужен
отдельный анализ, в границы этапа не входящий.

Правило «одна деталь = один Java-класс» статистикой не поддержано:
пользовательское содержимое может назначать нескольким деталям один
Java-класс, а стоковые детали класса из декларации не имеют вовсе — они
рисуются через `java.game.parts.Part`.

---

## 15.5. CFG → SCX: 517 строк `[Ф-рес]`

Метод: для каждого `cars/**/*.cfg` (517 файлов) читаются строки `render <id>`;
id разрешается по правилу хоста — старшие 16 бит = номер dep-слота пакета,
младшие = локальный id (`RpkIndex::resolve_embedded_id`,
`src/resources/rpk_index.cpp:444–460`), затем Render → `mesh <id>` →
`sourcefile …scx`. Дедупликация SCX — по пути без учёта регистра.

| Различных SCX на CFG | CFG |
|---|---:|
| 1 | **416** |
| 2 | 24 |
| 3 | 33 |
| 4 | 2 |
| 5 | 18 |
| 0 | 24 |
| **Всего** | **517** |

77 CFG (14,9 %) имеют от 2 до 5 SCX по строкам `render`; с учётом строки
`mesh` самого CFG — 82 (15,9 %). Ранее приводилось «77 из 516» — расхождение в
одной строке: предыдущий резолвер отбрасывал CFG, для которого не смог
определить пакет (516 из 517), и терял одно-единственное попадание.

### 15.5.1. Классификация много-SCX CFG

| Причина нескольких SCX | CFG |
|---|---:|
| состояния света: `render` + `flags` (`_CITY`, `_BRAKE`, `_REVERSING`, `_INDICATOR`) | 66 |
| LOD-подуровни: `lod`/`lods` | 9 |
| прочее: разные меши одного CFG | 2 |
| **Итого** (по строкам `render`) | **77** |

Если к строкам `render` добавить строку `mesh` самого CFG (см. 15.5.2),
много-SCX становится 82: 66 свет + 9 LOD + 7 «прочих» (к двум названным ниже
добавляются 5 AI `_main_.cfg` с `chassis.scx` + `wheel.scx`).

Два «прочих» случая названы явно:

- `cars/misc/Police_V3/scripts/_main_.cfg` — 3 SCX: `steering_wheel`, `wheel_L`,
  `wheel_R` (машинный CFG рисует руль и колёса отдельными Render);
- `cars/racers/Duhen_data/scripts/FL_McPherson_strut.cfg` — 2 SCX: стойка и
  верхний стабилизатор, то есть деталь из двух мешей.

Ни один из 77 (и ни один из 82) случаев не является «альтернативной моделью,
выбираемой в рантайме»: состояния света и LOD — одновременно присутствующие
под-рендеры, а «прочие» — составная геометрия.

### 15.5.2. 24 CFG без SCX в строках `render`

`[Ф-рес]` Все 24 — racer-файлы `_main_*.cfg` (Baiern 3, Duhen 4, Einwagen 3,
Enula 3, Focer 3, MC 3, Nonus 2, Prime 1, SuperDuty 2). У них:

- `render 0x0003001C` → `cars/racers.rpk:0x1c`, alias
  `racer_default_steering_wheel`, тип 14, payload содержит только параметры тени
  (`shd_center`, `shd_diru`, `shd_dirv`, `shd_vbase`, `shd_vup`) и **не содержит
  `mesh`**;
- геометрия кузова объявлена отдельной строкой CFG `mesh 0x00000064` (и
  `texture 0x00020025`) — то есть мимо Render Resource.

Поэтому категория «0 SCX» здесь — не ошибка разбора и не отсутствие геометрии:

| Причина | CFG |
|---|---:|
| CFG без поля `render` | 0 |
| `render` не разрешился | 0 |
| `render` разрешился, но не в Render (тип ≠ 14) | 0 |
| Render без `mesh` (только тень) + геометрия в строке `mesh` | 24 |

`[Ф-код]` В OpenSLRR поле `cfg->mesh` используется только аксессорами
`getMesh`/`setMesh` (`src/resources/part_runtime.cpp:5118–5132`) и не участвует в
`render_part()`; отрисовка идёт от `cfg->render`. Это расхождение хоста с
содержимым для машинных CFG в этом этапе не устраняется и в выводы о покраске
не входит.

Если учитывать и строку `mesh` CFG, то ни одного CFG без геометрии не остаётся:
`{1: 435, 2: 23, 3: 34, 4: 7, 5: 18}` (517), а «прочих» много-SCX становится 7
(добавляются 5 AI `_main_.cfg` с `chassis.scx` + `wheel.scx` и Police `_main_.cfg`).

---

## 15.6. Две метрики Legacy Paint

### 15.6.1. Как получены исходные данные

`assets4.json` — промежуточный файл сканирования, не источник истины. Он
собран так `[Ф-рес]`:

1. берутся все 517 CFG-строк `cars/**/*.cfg` (строки `parts/**` исключены);
2. геометрия строки ищется в двух источниках по приоритету:
   - **CFG `mesh` path** — последняя строка `mesh 0x…` самого CFG разрешается
     в Mesh Resource (тип 5) **в пакете этого CFG** (без перехода по
     dep-слоту), затем берётся её `sourcefile` → SCX. Так разрешились
     **514** строк — в том числе все 24 racer `_main_*.cfg`, у которых
     `render` ведёт на теневой Render без `mesh` (15.5.2): их кузов приходит
     именно из строки `mesh`;
   - **Render path** — если строки `mesh` нет или она не даёт SCX: берётся
     `render`, в payload которого указан этот `mesh` (иначе первый `render`),
     далее Render → `mesh` → Mesh Resource → `sourcefile` → SCX. Так
     разрешилась **1** строка — `Duhen FL_McPherson_strut.cfg` (две строки
     `render`, строки `mesh` нет);
   - **Unresolved** — ни один путь не дал SCX: **2** строки (см. ниже).

   Итого `514 + 1 + 2 = 517`: каждая строка учтена ровно один раз; с
   геометрией — 515 строк, без неё — 2;
3. строки группируются по нормализованному абсолютному пути SCX
   (`normcase` + `normpath`) → **496** различных ассетов; 483 ассета
   используются одной CFG, 8 — двумя, 4 — тремя, 1 — четырьмя (515 ссылок,
   19 из них повторные). Повторного счёта одной геометрии нет: если `mesh` и
   `render` ведут к одному SCX, в набор попадает один путь;
4. в ассет попадают material groups из SCX (имя, `tex`, `mix`, `refl`) и
   версия файла.

Две неразрешённые строки:

- `cars/racers/scripts/traffic_racer_default_wheel.cfg` — для каталога
  `cars/racers/scripts` не выводится пакет-владелец (`cars/racers/scripts.rpk`
  отсутствует), строки `mesh` нет;
- `cars/traffic/Wreck_data/scripts/_main_.cfg` — `mesh 0x1a` не разрешается в
  Mesh Resource (тип 5) в `cars/traffic/Wreck.rpk`, а Render-путь для этой
  строки SCX не даёт.

Повторные ссылки (13 ассетов): 5 пар окон (`FL/FR/RL/RR_window`,
`R_windshield` — по две CFG на файл) и 8 `chassis.scx` разных машин, на каждый
из которых ссылаются все `_main_*` варианты этой машины.

**Область метрик.** 496 — это по одному *представительному* SCX на CFG-строку,
а не все SCX, достижимые из CFG: если идти по **всем** строкам `render`
(включая под-рендеры), достижимо **667** различных SCX. Под-рендеры, на
которые указывают строки `render` с `flags` (состояния света) и `lod`
(LOD-подуровни), в набор 496 не попадают, поэтому метрики 337/469 описывают
представительную геометрию, а не все под-рендеры. Это ограничение области, а
не пересчёт чисел.

Группы считаются по material-записям SCX (property `0x08000000` — имя,
`0x06000000/1/3` — `tex`/`mix`/`refl`). Группа с пустым именем не встречается.

### 15.6.2. Метрика A — Legacy Paint Group (337)

Критерий: CFG объявляет `texture <id>`; группа засчитывается, если её `tex`
индекс лежит внутри списка текстур Render и соответствующий элемент списка
имеет объявленный id.

| Показатель | Значение |
|---|---:|
| различных SCX-ассетов (по одному представителю на CFG-строку) | 496 |
| ассетов с ≥1 совпадающей группой (**PF**) | **276** |
| **Legacy Paint Groups** | **337** |

337 — это количество legacy paint targets (совпадений «объявленная текстура ↔
material group»), а не количество material groups и не число будущих
Material Slot.

### 15.6.3. Метрика B — именованные material groups (469)

Критерий: все группы с непустым именем в тех же 276 PF-ассетах.

| Категория | Критерий | Кол-во |
|---|---|---:|
| независимый mix | `mix` есть, `mix ≠ tex`, `mix ≠ 65535` | **300** |
| sentinel mix | `mix = 65535` | **133** |
| self-mix | `mix == tex` | **36** |
| **Всего** | | **469** |

Ранее «133» называлось self-mix — это ошибка подписи: 133 = `mix = 65535`,
self-mix = 36.

Пересечение проверено по фактическим группам, а не вычитанием:

| | paint (337) | не-paint (132) |
|---|---:|---:|
| независимый mix | **300** | 0 |
| `mix = 65535` | 37 | 96 |
| `mix == tex` | 0 | 36 |
| **Σ** | **337** | **132** |

Отсюда `469 − 337 = 132 = 96 + 36`: все 300 независимых mix-групп входят в 337,
а остаток складывается из 96 sentinel-групп и 36 self-mix. Ни одна self-mix
группа не является legacy paint target.

**Не все 469 — paint targets.** 469 — полный охват именованных групп выбранных
ассетов.

### 15.6.4. По наборам данных

| Набор | Ассеты | paint | прочие | всего | paint + mix | paint без mix |
|---|---:|---:|---:|---:|---:|---:|
| Baiern | 34 | 47 | 6 | 53 | 44 | 3 |
| Duhen | 28 | 30 | 1 | 31 | 27 | 3 |
| Einwagen | 29 | 30 | 3 | 33 | 25 | 5 |
| Enula | 33 | 37 | 1 | 38 | 37 | 0 |
| Focer | 22 | 22 | 3 | 25 | 6 | 16 |
| MC | 20 | 23 | 2 | 25 | 22 | 1 |
| Mazda 3 2005 | 35 | 62 | 112 | 174 | 62 | 0 |
| Nonus | 31 | 35 | 1 | 36 | 29 | 6 |
| Prime | 22 | 28 | 1 | 29 | 25 | 3 |
| SuperDuty | 22 | 23 | 2 | 25 | 23 | 0 |
| **Σ** | **276** | **337** | **132** | **469** | **300** | **37** |

Строки взаимоисключающие и сходятся по каждому столбцу.

---

## 15.7. Шасси: снятие противоречия 276 / 337

Предварительный отчёт содержал одновременно таблицу из десяти строк (276 / 337)
и отдельную добавку «+12 моделей шасси / +22 группы». Проверка по путям `[Ф-рес]`:

| Категория | Ассеты | paint | прочие | всего |
|---|---:|---:|---:|---:|
| детали (не `*chassis*`) | 264 | 313 | 83 | 396 |
| модели шасси | 12 | 24 | 49 | 73 |
| **Σ** | **276** | **337** | **132** | **469** |

12 chassis-ассетов: Baiern `chassis.scx` и `cabrio_chassis.SCX`, Duhen, Einwagen,
Enula, Focer, MC, Nonus, Prime, SuperDuty — по одному `chassis.scx`, и Mazda
`chassis.scx` + `chassis_wide.scx`. Все они лежат в каталогах `*_data/meshes`
своих наборов, поэтому попадают **в строку своего автомобиля**, а не в отдельную
категорию.

Вывод: добавка «+12 / +22» была **двойным счётом** и вдобавок неверной по числу —
шасси дают **24** legacy paint groups, а не 22, и уже входят в 337 и 469.
Корректный итог остаётся 276 / 337 / 469, но его состав описывается как
264 + 12 ассетов, 313 + 24 paint groups, 396 + 73 groups.

Побочная причина появления «+22»: шасси входят в набор через `_main_*.cfg`
(24 CFG на 12 ассетов), и при подсчёте по CFG-строкам, а не по ассетам, число
групп удваивается.

---

## 15.8. `default_*`, `mix`, reflection

Placeholder-текстуры `default_white/black/blue/gray/brown/yellow/red/green/silver.png`
— это базовые «заглушки» для покраски. Классификация по фактическим SCX и
спискам текстур Render `[Ф-рес]`:

| Шаг | Кол-во |
|---|---:|
| групп с placeholder-базой в 276 PF-ассетах | 339 |
| из них `mix = 65535` | 39 |
| из них независимый mix | 300 |
| минус 2 группы `Duhen/chassis.scx` (шасси) | **37** |
| из 37: DDS разрешается через `refl` | 23 |
| из 37: DDS не разрешается | 14 |
| из 14: `refl = 65535` | 2 |
| из 14: индекс за пределами списка (`refl == len(texlist)`) | 12 |

339 ≠ 337: две группы используют placeholder-базу, которая не совпадает с
объявленным в CFG `texture`, поэтому они не входят в метрику A.

**12 случаев «за пределами списка»** `[Ф-рес]`: во всех двенадцати `refl` равен
длине списка текстур Render (`refl = 1` при одном элементе, `refl = 2` при двух),
то есть указывает на следующий слот после последнего. Это систематический
паттерн, а не порча данных. Пример: Duhen `R_wing.scx`, материал `R wing`,
`refl = 1`, список = [`default_white.png`].

**2 случая `refl = 65535`** `[Ф-рес]`: Baiern `L_headlights_4.scx` и
`R_headlights_4.scx`, материал `carbon`, список из 4 текстур. Значение не может
быть допустимым индексом в этом списке.

`[Ф-код]` В OpenSLRR `ScxGroup::texture_index`, `mix_index` и `reflection_index`
инициализируются `0xffff` (`include/slrr/scx_mesh.hpp:30–31`), то есть `0xFFFF`
используется как значение по умолчанию / признак отсутствующего индекса.

`[?]` Полная семантика `0xFFFF` и индексов за пределами списка в **оригинальном**
renderer не подтверждена. «Sentinel» и «индекс за пределами списка» — разные
случаи и здесь не объединяются.

### 15.8.1. Широкий охват reflection

Для 169 групп без независимого mix (133 sentinel + 36 self-mix) в тех же
276 ассетах `[Ф-рес]`: у 79 через `refl` разрешается существующий DDS.

| Набор | Групп с DDS через `refl` |
|---|---:|
| Mazda 3 2005 | 56 |
| Focer | 16 |
| Nonus | 4 |
| Baiern | 3 |
| **Σ** | **79** |

Оставшиеся 90 групп не дают DDS через `refl` (в том числе 2 sentinel и
индексы за пределами списка). Это статистика про отражения, а не про покраску:
albedo, `mix_index`, reflection-текстура, `reflection_index` и назначение цвета
детали — разные каналы и здесь не смешиваются.

---

## 15.9. `setRenderType()` и `rendertypes` `[Ф-код]` `[Ф-рес]`

- `rendertypes` в CFG **отсутствует во всех `cars/**` и `parts/**`** (0 из 517
  машинных CFG, 0 из 182 колёсных). Он встречается в двух стоковых объектных
  CFG: `objects/scripts/tabla_b.cfg`, `objects/scripts/tabla_d.cfg`. То есть
  механизм описан форматом и используется стоковым содержимым, но не
  автомобильным.
- Строка `setRenderType` присутствует в **34** class-файлах:

| Категория | Файлов |
|---|---:|
| стоковые классы шин (`parts/wheels/tyres/scripts/*`), вызывающие | 30 |
| стоковый `Brake.class`, вызывающий | 1 |
| стоковый `Part.class` — объявление native-метода | 1 |
| сторонние классы вне стокового содержимого, вызывающие | 2 |

Стоковых вызывающих — 31; «34 стоковых класса» неверно.

Подтверждённый вывод: механизм динамической смены основного Render существует в
штатном API и используется штатными классами (шины, тормоза). Из самого факта
его наличия **не следует**, что какие-либо случаи нескольких SCX в CFG
используют динамическое переключение: все 82 много-SCX CFG объясняются светом,
LOD и составной геометрией.

---

## 15.10. Категории для Material System

### A. Legacy Base Paint Replacement — 337 групп

Соответствия «объявленная в CFG текстура ↔ material group» в 276 ассетах
(313 у деталей + 24 у шасси). Это legacy paint targets. Они **не** являются
будущими Material Slot: у них другая единица адресации (текстура детали, а не
слот материала).

### B. Legacy Mix Overlay — 300 групп

Независимые `mix`-слои (300 в paint-наборе, 0 вне его). Термин
`Legacy Mix Overlay`; описательное `Content Albedo Layer` допустимо только там,
где семантика подтверждена. Это **не** `DecalInstance` из A4 и не «декали».

Mazda 3 2005 `[Ф-рес]`: 35 ассетов, 62 paint-группы, все 62 имеют
`mix = paintable.png`. `[Т]` Пользователь проверил в игре, что `paintable.png`
работает как alpha-overlay поверх базового слоя. Это подтверждённый пример
legacy overlay; он не доказывает, что любой `mix_index` — будущая Decal
Instance.

### C. Кандидаты на будущий Paint Slot без legacy paint path

Колёса `[Ф-рес]`: 182 CFG (153 диска + 29 шин), из них **0** объявляют
`texture` legacy-механизма; независимых `mix` — 0.

| Метрика | Значение |
|---|---:|
| колёсных CFG | 182 |
| SCX, на которые они ссылаются через Render→mesh | 207 |
| material groups в этих SCX | 364 |
| SCX в `parts/wheels/**` на диске | 422 |
| material groups в них | 582 |
| групп с независимым mix | 0 |

`422 / 582` — инвентарь **каталога**, `207 / 364` — только **используемое**
CFG. Ранее эти две величины приводились одной строкой, что неверно.

Кандидаты без legacy-пути, отмеченные ранее: колёса и шины; хромированные
бамперы MC/Prime; часть чёрных боковых юбок Mazda; выхлопные трубы; радиатор
Prime. Отсутствие старого механизма **не** означает, что деталь непокрашиваема:
это отсутствие legacy-пути, а не ограничение будущего Material System.

### D. Вне A/B/C

Отражения (`reflection_index` и reflection-текстуры), `lightmap`, эмиссия и
прочие каналы материала не являются ни базовой покраской, ни mix-overlay, и
автоматически не причисляются к A, B или будущим Paint Slot.

### 15.10.1. Ограничения `PartConfig::texture` как объявления

`[Ф-рес]` `texture` объявлен в **307** из 517 машинных CFG. Он не является
надёжным универсальным объявлением будущего Paint Slot:

- **16 CFG объявляют `texture`, но ни одна material group его не использует:**
  `Police_V3` (`hood`, `misc_siren`, `trunk`, `_main_`), Mazda
  (`L_sideskirt_3/4`, `R_sideskirt_3/4`), MC (`F_bumper`, `F_bumper_2`,
  `F_grill`, `R_bumper`), Prime (`F_bumper`, `R_bumper`, `L_mirror_2`,
  `R_mirror_2`);
- **10 ассетов имеют placeholder-группы без объявления `texture`:** выхлопные
  трубы (Duhen, Einwagen, Enula, Focer, Nonus, MC ×2, SuperDuty ×2) и
  радиатор Prime (2 группы).

Это согласуется с решением A3: будущий Paint Slot объявляется явно, а не
выводится из legacy-поля. Количество будущих Material Slot нельзя выводить из
числа `PartConfig::texture`, `texture_index`, legacy paint groups или
`mix_index`.

### 15.10.2. Граница A6 — Legacy Tint

`Legacy Tint` — отдельный legacy-only runtime-модификатор цвета; это не Legacy
Texture Ref, не Material Definition, не Material Slot и не Paint Assignment.
Наличие палитры `PartRender::colors` не означает пользовательской покраски
автомобиля. Палитры встречаются в AI/traffic/map props; в `cars/racers/` и
автомобильных RPK их нет. Границы механизма зафиксированы в
`MATERIAL_SYSTEM_ARCHITECTURE.md` §11.2 (A6) и здесь не повторяются.

---

## 15.11. Сверка сумм

| Проверка | Значение | Сходится |
|---|---|---|
| SCX под `cars/` | 1540 + 24 = 1564 | да |
| Версии 1540 | v3 139 + v4 1401 | да |
| Версии 24 | v3 15 + v4 9 | да |
| RPK | 82 пакета / 11887 записей = 81 / 11765 + пользовательский пакет 122 | да |
| Гистограмма типов | 20 типов, Σ = 11887 | да |
| Частичный перечень | 11587 + 300 (14 типов) = 11887 | да |
| type 8 в 10 racer-пакетах | 455 + 26 + 26 + 87 + 29 = 623 | да |
| CFG-строки | 416 + 24 + 33 + 2 + 18 + 24 = 517 | да |
| Много-SCX | 66 + 9 + 2 = 77 (по `render`) | да |
| Ассеты | 276 = 264 + 12 | да |
| Paint groups | 337 = 313 + 24 | да |
| Все группы | 469 = 396 + 73 = 337 + 132 | да |
| mix-категории | 300 + 133 + 36 = 469 | да |
| paint-пересечение | 300 + 37 + 0 = 337; 0 + 96 + 36 = 132 | да |
| placeholder | 339 = 39 + 300; 37 = 23 + 14; 14 = 2 + 12 | да |
| без независимого mix | 169 = 133 + 36; 79 c DDS через `refl` | да |
| Колёса | 182 CFG; 207 SCX / 364 groups (используемые); 422 / 582 (на диске) | да |

---

## 15.12. Что уточнено относительно предварительных формулировок B1

1. «1540 автомобильных + 24 UI/общих = 1564» — состав верен, но 24 — это общие
   **меши машин** (`cars/meshes/` HUD + общие колёса), а не UI-ресурсы; версии
   этих 24 (15/9) ранее не были известны.
2. Гистограмма типов была **частичной** (11587). Полная — 20 типов / 11887;
   недостающие 300 объяснены.
3. «507 уникальных классов на 623 записи» → 507 Part Resource с декларацией
   `script`, 507 различных путей, 472 различных basename, 507 существующих
   файлов по этим путям; число уникальных Java-типов здесь не устанавливается.
4. «одна деталь = один CFG = один класс = один SCX» верно только для CFG
   (0 общих CFG на 455 деталей). Для классов и SCX правило не выполняется
   (см. 15.4.1 и 15.5).
5. «77 много-SCX CFG» → 82 из 517 (разница из-за отброшенной строки и строки
   `mesh`); все случаи классифицированы, «альтернативных геометрий» нет.
6. «24 CFG с 0 SCX — неразобранные/ошибки» → все 24 — racer `_main_*.cfg` с
   теневым Render без `mesh` и геометрией в строке `mesh`.
7. «133 self-mix» → 133 = `mix = 65535`; self-mix = 36.
8. «+12 моделей шасси / +22 группы» → двойной счёт и неверное число: 12 шасси
   входят в 276/337/469 и дают 24 paint-группы.
9. «`rendertypes` отсутствует во всех штатных CFG» → отсутствует в
   автомобильных; присутствует в 2 объектных CFG.
10. «34 класса вызывают `setRenderType`» → 34 файла, из них 31 стоковый
    вызывающий, 1 объявление native-метода, 2 сторонних.
11. «182 CFG / 422 SCX / 582 группы» → смешаны два охвата: 207 / 364
    используемых против 422 / 582 в каталоге.

Этапы 2–14 не переписаны: все перечисленные формулировки были
предварительными и в документ ранее не вносились.

---

## 15.13. Неизвестное

- `[?]` Семантика `0xFFFF` и индексов за пределами списка в **оригинальном**
  renderer (в хосте подтверждено только значение по умолчанию).
- `[?]` Совпадает ли множество legacy paint targets с тем, что оригинальный
  движок считал «красящимся»: проверка возможна только на EXE/в игре.
- `[?]` 13 ассетов, на которые ссылаются 2–4 CFG (в основном окна): как
  оригинал выбирает текстуру при конфликте объявлений.
- `[?]` Назначение типа 6 RPK (248 записей).
- `[?]` Поведение `cfg->mesh` у машинных CFG: OpenSLRR его не рисует
  (`part_runtime.cpp:5118–5132`), оригинал использует как оболочку кузова.
- `[?]` Причина недостижимости исторического замера `53 / 8908`.
- `[?]` Статус пользовательских пакетов в инвентаре: один такой пакет в
  `parts/` учтён отдельно и вычтен из общего числа; других в выборке не
  обнаружено.

Сам аудит выполнен без изменений кода и ресурсов; документальная фиксация
Stage 15 выполнена отдельно.

---

# Stage 16 — Legacy Skin Preset to Material Slot mapping (B2)

**Проверяемый HEAD:** `e24710a2036a996c44d19fb054ba9554722d0cd1`.
Read-only аудит: C++, Java, шейдеры, SCX/CFG/RPK, сохранения и sidecar-файлы не
изменялись; новые форматы и поля не создавались; игра не запускалась. Stages
2–15 не переписывались. Вопрос ставился так: существует ли надёжное
соответствие «запись legacy Skin Preset → Paint Slot новой Material System».
Результат мог быть отрицательным — он и оказался частично отрицательным
(16.9). B1 (Stage 15) не пересматривался; инвентарь используется как база.

## 16.1. Что именно идентифицирует запись Skin Preset

### 16.1.1. Структура файла (подтверждено ранее, перепроверено по коду)

Skin Preset — манифест `SDAT` type 2 с телом `SAVEFILEID_SKIN` (`0x82549867`),
версией `0`, парами «ключ → ordinal», терминатором, таблицей blob-ресурсов
(`kind 7`, `size 0`) и таблицей паков (`[Э9 §5]`, `[Э10 §5]`). Байтовая
раскладка исследованного файла:

| смещение | содержимое |
|---|---|
| `0x08` | `SAVEFILEID_SKIN`, версия `0` |
| `0x10`–`0x83` | 14 пар `(wired part id, wired texture ordinal)` |
| `0x84` | терминатор `0` |
| `0x88`–`0x133` | 14 записей `ordinal 1…14, kind 7, size 0` |
| `0x140` | `ordinal 1, len 25, "cars\racers\Einvagen.rpk"` |

Пары исследованного скина: `0x00010112→1, 0x000100b0→2, 0x000100b4→3,
0x000100ac→4, 0x000100b9→5, 0x000100bd→6, 0x000100be→7, 0x000100c4→8,
0x000100af→9, 0x000100bb→10, 0x000100c7→11, 0x000100b3→12, 0x000100c0→13,
0x000100c9→14` `[Э9 §3]`, `[Э10 §5]`.

`[Ф-рес]` Каждый ключ разрешается в записи `cars\racers\Einvagen.rpk`:
`0x0112` — `native car …\_main_110_GTK.cfg` (шасси, тип записи 8), остальные
13 — `native part …` (тип 8) с CFG `FL_quarterpanel`, `FR_quarterpanel`,
`F_bumper`, `hood_2`, `R_bumper`, `R_door`, `R_wing`, `FL_door`, `L_mirror`,
`RL_door`, `FR_door`, `R_mirror`, `RR_door`. Ни один ключ не является классом,
категорией или именем: все 14 — идентификаторы Part Resource внутри одного
пака.

### 16.1.2. Что означает число в паре

`[Ф-код]` Значение пары — не «номер типа детали» в смысле класса, а
**wired-идентификатор ресурса** `(ordinal пака в таблице трейлера << 16) |
локальный id записи RPK`. Запись выполняется в
`java.io.File.write(ResourceRef)` → `sdat_wire_resource`
(`src/natives/natives_io.cpp:205-247`): хост-идентификатор `pack_id<<16|local`
заменяется на `(ordinal<<16)|local`, а путь пака попадает в таблицу
`file.sdat_packs`. Чтение — `readResID` → `sdat_remap_resource`
(`natives_io.cpp:248-270`): wired-значение с ненулевой старшей половиной
разрешается обратно в `local | (pack_id<<16)` по таблице паков; при
невозможности — `0`.

Отсюда фактическая идентичность записи Skin Preset:

```
(путь пака из таблицы трейлера, локальный id ресурса)
```

а не «тип детали» в абстрактном смысле. Практическое следствие: числовое
значение ключа без пака смысла не имеет (16.1.4), а сам пак в файле **есть** —
но не как «идентичность машины», а как техническая зависимость SDAT.

### 16.1.3. `GII_TYPE` — что это за сущность

`[Ф-код]` Константы `java.lang.GameType` прочитаны из TUFA-файла
`system\Scripts\lang\GameType.class` (TUFA-чанки `CONS`/`FILD`/`MTHD`/`CLSS`/
`TREE`; значения статических полей — в их инициализирующих деревьях):

| константа | значение |
|---|---:|
| `GII_ID` | 5 |
| **`GII_TYPE`** | **6** |
| `GII_CATEGORY` | 7 |
| `GII_SIZE` | 11 |
| `GII_OWNER` | 24 |
| `GII_AXIS` | 25 |
| `GII_ANIMATE` | 28 |
| `GII_RENDER` | 48 |
| `GII_CAR_DRIVETYPE` | 52 |
| `GII_PART_CATEGORY` | 55 |
| `GIR_CAT_VEHICLE` / `GIR_CAT_PART` | 5 / 9 |

`[Ф-код]` Диспетчер хоста согласуется с этой таблицей:
`Part.getInfo(5)` → `live_id`, `getInfo(6)` → `type_id`
(`src/resources/part_runtime.cpp:4623-4631`), `getInfo(11)` — габарит,
`getInfo(24)`/`(25)` — контроллер и его ось, `getInfo(48)` — камеры,
`getInfo(52)` — привод, `getInfo(55)` — категория детали. `type_id`
устанавливается при создании/установке детали как **полный id ресурса**
(`part_runtime.cpp:1206, 1221`; `natives_gameref.cpp:126`).

Вывод: `GII_TYPE` — это не тип класса и не категория, а
**идентификатор Part Resource** (`native car` для шасси, `native part` для
остальных), в полном виде `pack_id<<16 | local`. Класс детали (`script`)
живёт отдельно и в паре скина не участвует.

### 16.1.4. Числовое совпадение ключей между паками

`[Ф-рес]` В 10 racer-пакетах 481 Part Resource с `native car`/`native part`,
и они используют **201** различный локальный id; **128** из них встречаются
более чем в одном пакете. Крайние случаи:

| локальный id | где и что это |
|---|---|
| `0x006` | `native car` во **всех 10** пакетах, каждый раз другой CFG (`_main_CoupeSport`, `_main_SunStrip_1_5_DVC`, `_main_110_GT`, …) |
| `0x112` | `Einvagen` шасси `_main_110_GTK`, `SuperDuty` шасси `_main_SuperDuty_Extra_750`, `Baiern` **окно** `FR_window`, `Maz3_2005` и `Nonus` **порог** `R_sideskirt` |

Поэтому «тип детали» на уровне числа не существует: ключ скина без пака
неоднозначен в принципе, и именно таблица паков делает его осмысленным.

## 16.2. Как ключ разрешается в реальную деталь

`[Ф-код]` Полная цепочка разрешения (все звенья подтверждены кодом):

```
манифест: (wired id, ordinal)
  → File.readResID() → sdat_remap_resource
        wired с hi != 0 (и != 0xFFFF) → ищется ordinal в file.sdat_packs
        → local | (pack_id<<16)            [natives_io.cpp:248-270]
  → Vehicle.loadSkin: сравнение с getInfo(GII_TYPE) установленных деталей
        → первая ещё не задействованная деталь с равным значением  [Э9 §5]
  → Part.setTexture(ordinal)               [Э9 §5; ср. 16.6 о host]
```

Требования и отказы:

| условие | следствие |
|---|---|
| пак из трейлера известен загрузчику (по пути `cars\racers\X.rpk`) | ключ разрешается в `pack_id<<16` + локальный id |
| пак отсутствует/переименован | `sdat_remap_resource` → `0` → совпадений нет, ошибки нет (молча) |
| в машине нет детали с таким id | пара пропускается, ошибки нет |
| две детали с одинаковым id | изображение получает **первая** (счётчик-флаг в `loadSkin`) |
| ordinal текстуры | blob с `abs_id = 0` → в host `0`; `setTexture(0)` — no-op `[Э9 §7]` |

`[Т]` Идентификация детали работает **без машины**: ключ указывает на конкретный
Part Resource, а не на «слот такой-то машины». Машина не записана в файле;
принадлежность определяется тем, какие детали в неё установлены. Поэтому скин
одной модели формально применим к любой машине, где установлен тот же Part
Resource (в пределах семейства моделей одного пака, 16.4.4).

## 16.3. От детали к геометрии и material groups

`[Ф-рес]` Цепочка для каждого ключа (правило выбора геометрии — как в 15.6.1):

```
Part Resource (тип 8)
  → payload "native car|part <cfg>"
  → CFG: texture <id>  (объявленный слот краски)  + render/mesh
  → Render Resource (тип 14): список texture 0x… (порядок = индексы групп)
  → Mesh Resource (тип 5) → sourcefile … .scx
  → SCX: material groups {name, tex, mix, refl}
```

Критерий «группа относится к legacy-покраске» — тот же, что в 15.6.2:
группа засчитывается, если её `tex` равен позиции объявленной текстуры CFG в
списке текстур соответствующего Render. Никакие имена для этого не
используются.

### 16.3.1. Все 14 ключей исследованного скина

Цепочка целиком (`[Ф-рес]`; Render/Mesh — локальные id в `Einvagen.rpk`,
CFG `texture` у всех 14 — `0x00020025`):

| ord | ключ (local) | вид ресурса | CFG | Render / Mesh | SCX | групп | с paint-tex | имя |
|---:|---|---|---|---|---|---:|---:|---|
| 1 | `0x0112` | `native car` | `_main_110_GTK.cfg` | `0x48` / `0x49` | `chassis.scx` | 3 | 2 | `chassis` |
| 2 | `0x00b0` | `native part` | `FL_quarterpanel.cfg` | `0x58` / `0x59` | `FL_quarterpanel.scx` | 1 | 1 | `F quarterpanel` |
| 3 | `0x00b4` | `native part` | `FR_quarterpanel.cfg` | `0x62` / `0x63` | `FR_quarterpanel.scx` | 1 | 1 | `F quarterpanel` |
| 4 | `0x00ac` | `native part` | `F_bumper.cfg` | `0x4d` / `0x4e` | `F_bumper.scx` | 1 | 1 | `F bumper` |
| 5 | `0x00b9` | `native part` | `hood_2.cfg` | `0x6d` / `0x6e` | `hood_2.scx` | 1 | 1 | `hood3` |
| 6 | `0x00bd` | `native part` | `R_bumper.cfg` | `0x78` / `0x79` | `R_bumper.scx` | 1 | 1 | `R bumper` |
| 7 | `0x00be` | `native part` | `R_door.cfg` | `0x7b` / `0x7c` | `R_door.scx` | 1 | 1 | `hatch door` |
| 8 | `0x00c4` | `native part` | `R_wing.cfg` | `0x89` / `0x8a` | `R_wing.scx` | 1 | 1 | `R wing` |
| 9 | `0x00af` | `native part` | `FL_door.cfg` | `0x55` / `0x56` | `FL_door.scx` | 1 | 1 | `F door` |
| 10 | `0x00bb` | `native part` | `L_mirror.cfg` | `0x72` / `0x73` | `L_mirror.scx` | 1 | 1 | `mirror` |
| 11 | `0x00c7` | `native part` | `RL_door.cfg` | `0x91` / `0x92` | `RL_door.scx` | 1 | 1 | `R door` |
| 12 | `0x00b3` | `native part` | `FR_door.cfg` | `0x60` / `0x61` | `FR_door.scx` | 1 | 1 | `F door` |
| 13 | `0x00c0` | `native part` | `R_mirror.cfg` | `0x80` / `0x81` | `R_mirror.scx` | 1 | 1 | `mirror` |
| 14 | `0x00c9` | `native part` | `RR_door.cfg` | `0x96` / `0x97` | `RR_door.scx` | 1 | 1 | `R door` |

Вердикт по той же выборке:

| ord | ключ | Paint Slot mapping | причина | достоверность |
|---:|---|---|---|---|
| 1 | `0x0112` | нет; кандидат один — (`chassis.scx`, `chassis`) | две групповых записи с одним именем; слот не объявлен | `[Ф-рес]` цепочка, `[Т]` вывод |
| 2, 3 | `0x00b0`, `0x00b4` | нет; кандидат один на ключ | имя `F quarterpanel` повторено в двух SCX — нужен scope SCX | `[Ф-рес]` |
| 4 | `0x00ac` | нет; кандидат один | — | `[Ф-рес]` |
| 5 | `0x00b9` | нет; кандидат один | имя группы (`hood3`) не совпадает с именем детали — совпадение имён не основание | `[Ф-рес]` |
| 6 | `0x00bd` | нет; кандидат один | — | `[Ф-рес]` |
| 7 | `0x00be` | нет; кандидат один | — | `[Ф-рес]` |
| 8 | `0x00c4` | нет; кандидат один | SCX детали также имеет `_BRAKE`-вариант, paint-tex туда не попадает | `[Ф-рес]` |
| 9, 12 | `0x00af`, `0x00b3` | нет; кандидат один на ключ | общее имя `F door` в двух SCX | `[Ф-рес]` |
| 10, 13 | `0x00bb`, `0x00c0` | нет; кандидат один на ключ | общее имя `mirror` в двух SCX | `[Ф-рес]` |
| 11, 14 | `0x00c7`, `0x00c9` | нет; кандидат один на ключ | общее имя `R door` в двух SCX | `[Ф-рес]` |

Все 14 ключей разрешились: 14/14, без пропусков и без альтернатив. У шасси две
групповых **записи** с одним именем `chassis` (плюс несвязанная `interior`), у
остальных — одна запись. Ни один ключ не даёт двух разных имён.

`[Т]` Для этой машины соответствие «ключ → поверхность» однозначно на уровне
пары *(SCX, имя material group)*. Это не то же самое, что соответствие
«ключ → Paint Slot» (16.9): имя группы — свойство legacy-геометрии, а не
объявленный слот.

### 16.3.2. Что скина в файле нет

`[Ф-рес]` Einvagen содержит 47 Part Resource (44 `native part` + 3
`native car`); **31** из них объявляют `texture`, а в скине присутствуют
только **14**. То есть Skin Preset — не «все красящиеся детали машины», а
разреженный набор: `saveSkin` пишет лишь те детали, у которых на момент
сохранения есть непустая текстура (фактически покрашенные) `[Э9 §4]`.

## 16.4. Объём: 481 Part Resource, 302 с объявленным `texture`

`[Ф-рес]` Скан по 10 штатным racer-пакетам (без пользовательских и без
`cars/misc`, `cars/traffic`, `cars/fake_racers`):

| категория | шт. |
|---|---:|
| Part Resource с `native car`/`native part` | **481** (26 + 455) |
| из них объявляют CFG `texture` | **302** |
| не объявляют (покраска неприменима) | 179 |

Распределение 302 «красящихся» ресурсов по числу **различных имён** групп,
попавших в paint-tex:

| различных имён | ресурсов | вид |
|---|---:|---|
| 1 | **283** | 264 `native part` + 19 `native car` |
| 2 | **7** | только `native car` (шасси Duhen ×4, Enula ×3) |
| 0 | **12** | только `native part` |

Дополнительно:

- `[Ф-рес]` **52** различных имени painted-групп; **33** из них используются
  более чем в одном SCX (`carcolor` — в 35 SCX, `F quarterpanel` — 20,
  `R quarterpanel` — 20, `F door` — 18, `mirror` — 18, `sideskirt` — 18…);
- `[Ф-рес]` **3** из 52 имён — автогенерированные (`Material #11884`,
  `Material #2610`, `Material #2607`), то есть имя не является осмысленным
  авторским идентификатором;
- `[Ф-рес]` у **71** красящегося ресурса число групповых записей больше числа
  различных имён (одна поверхность описана несколькими записями: Mazda — 27
  ресурсов, Baiern — 14);
- `[Ф-рес]` внутри одного SCX ни разу не встретилось одно имя с разными
  `tex`-индексами: в пределах SCX имя ↔ группа 1:1.

### 16.4.1. 12 ресурсов с `texture`, но без единой matching-группы

`[Ф-рес]` MC `F_bumper`, `R_bumper`, `F_grill`, `F_bumper_2`; Mazda
`L_sideskirt_3`, `L_sideskirt_4`, `R_sideskirt_3`, `R_sideskirt_4`; Prime
`F_bumper`, `R_bumper`, `L_mirror_2`, `R_mirror_2`. Это те же случаи, что
описаны в 15.10.1 («объявлен `texture`, но ни одна группа его не
использует»): CFG объявляет слот краски, канва существует, но ни один материал
её не читает. Для B2 это означает: у таких деталей legacy-покраска
**визуально ни на что не отображается**, поэтому «поверхность для Paint Slot»
не определяется вообще (не «неоднозначно», а отсутствует).

### 16.4.2. 7 шасси с двумя различными именами

`[Ф-рес]` Duhen `_main_SunStrip_1_5_DVC`, `_main_SunStrip_1_8_DVC`,
`_main_SunStrip_2_2_DVC`, `_main_Racing_SunStrip_2_0_CDVC` → в одном
`chassis.scx` обе группы `chassis` и `frame, engine bay` читают объявленную
текстуру; Enula `_main_Enula_WRY`, `_main_Enula_WRZ`,
`_main_Enula_WR_SuperTurizmo` → `chassis` и `frame`. Как и в 16.4, это записи
с одним именем в нескольких экземплярах плюс второе имя.

`[Т]` Это единственный в штатном контенте случай, когда **одна legacy-покраска
покрывает две различимые material group**. Legacy-механизм не различает их:
канва одна, изображение одно, обе группы получают один и тот же результат.
Будущие Paint Slot для этих двух имён — авторское решение, а не следствие
старого ключа.

### 16.4.3. Одна деталь — несколько SCX

`[Ф-рес]` 18 красящихся ресурсов имеют геометрию более чем в одном SCX
(14 headlights-деталей с `_CITY`/`_INDICATOR`, Mazda `trunk` с `_BRAKE`, Mazda
шасси ×2 с `_CITY`/`_INDICATOR`, Einvagen `Rf_wing` с `_BRAKE`). Ни у одного из
них объявленная текстура краски не встречается в под-рендерах: paint-tex
читается **только** в базовом SCX. То есть состояния света и LOD не расширяют
набор поверхностей покраски.

### 16.4.4. Несколько ключей — одна поверхность

`[Ф-рес]` В 8 из 10 пакетов `chassis.scx` используется **несколькими**
Part Resource этого же пака: Baiern 2, Duhen 4, Einvagen 3, Enula 3, Focer 3,
MC 3, Nonus 2, SuperDuty 2 (всего 22 ресурса → 8 файлов SCX). У всех вариантов
совпадают и Render Resource, и список текстур, и id объявленной текстуры
(проверено по строкам `render`/`texture`: например, три Einvagen-шасси
указывают на `Einvagen.rpk:0x48` и `texture 0x00020025`).

`[Т]` Один и тот же legacy-слот краски принадлежит нескольким `GII_TYPE`
(несколько моделей семейства). Это устойчивое отношение «несколько типов →
одна поверхность», а не совпадение.

Дополнительно `[Ф-рес]`: Baiern использует общие SCX и для окна
(`FL_window.scx`, `FR_window.scx`, `RL_window.scx`, `RR_window.scx`,
`R_windshield.scx` — по 2 ресурса), но эти ресурсы `texture` не объявляют и в
скин попасть не могут.

## 16.5. Проверенные сценарии

| # | Сценарий | Наблюдается | Вердикт |
|---|---|---|---|
| 1 | один `GII_TYPE` у нескольких моделей | как **одна сущность** — нет (id уникален внутри пака); числовое совпадение между паками — да (128/201) | ключ не является общим типом; без пака неоднозначен |
| 2 | одна модель, несколько деталей одного типа | только как несколько экземпляров одного Part Resource (теоретически колёса/шины ×4); среди красящихся кузовных деталей не наблюдается | `loadSkin` берёт первую деталь; для кузова не встречается |
| 3 | одна логическая деталь — несколько SCX | 18 ресурсов, из них с paint-tex в под-рендерах — 0 | покраска остаётся в базовом SCX; неоднозначности нет |
| 4 | один SCX — несколько подходящих групп | 2 SCX (Duhen, Enula `chassis.scx`), по 2 имени; ещё 71 ресурс имеет дубли записей с одним именем | в 2 случаях неоднозначно на уровне имён |
| 5 | одна деталь — несколько независимых поверхностей для отдельных слотов | 7 шасси-ресурсов (2 имени в одном SCX) | legacy красит обе одним изображением; разделение — только авторское |
| 6 | один тип у моделей с разной геометрией/материалами | на уровне числа — да (16.1.4); на уровне сущности — нет | без пака соответствие невозможно |
| 7 | запись скина нельзя однозначно связать с деталью/поверхностью | 12 ресурсов с `texture` без matching-групп; 179 некрасящихся; отсутствующий пак → 0 | отсутствие соответствия подтверждено |

## 16.6. Совместимость и перенос (6.3)

`[Т]` Сохранение старого вида через `LegacyFlattenedSurface` **возможно** и не
зависит от Paint Slot: ключ скина уже адресует конкретный Part Resource
(пак + локальный id), а sidecar — запечённую поверхность именно этой детали.
Привязка «legacy-поверхность → деталь» подтверждена данными и кодом (16.1–16.2)
и совпадает с §11.3.

`[Т]` Автоматическое **преобразование** в новую редактируемую краску
невозможно, и не только из-за идентичности:

1. sidecar — сплющенный результат (базовый цвет + графика), разделение не
   существует (§11.2.1, `[Э9 §3]`); это ограничение не зависит от того,
   насколько точно определён слот;
2. в 7 случаях одна канва покрывает две различимые группы — целевой слот не
   единственный;
3. имя группы не является идентификатором (52 имени, 33 переиспользованы,
   3 автогенерированы);
4. одна поверхность принадлежит нескольким `GII_TYPE` (8 шасси-файлов);
5. Paint Slot по A3 объявляется явно и не выводится из legacy-полей, поэтому
   «слот» и «группа, читающая paint-tex» — разные сущности.

`[Т]` В host путь применения скина по-прежнему не работает для изображений
(`ordinal → 0`), а идентификация детали работает; это не менялось и в B2 не
исправлялось.

## 16.7. Кардинальность (6.1)

| отношение | статус | подтверждение |
|---|---|---|
| один `GII_TYPE` → одна логическая деталь | **наблюдается** (в пределах пака) | id уникален; 455 ресурсов → 455 CFG (Stage 15) |
| один `GII_TYPE` → несколько деталей | возможно только как несколько экземпляров одного ресурса | `loadSkin` берёт первую |
| один тип → несколько material groups | **наблюдается**: 2 имени у 7 шасси; дубли записей у 71 ресурса | 16.4 |
| один тип → несколько будущих Paint Slot | **не определено данными**; кандидатов 2 только в 7 случаях | 16.4.2 |
| несколько типов → одна поверхность | **наблюдается**: 22 шасси-ресурса → 8 SCX | 16.4.4 |
| один тип → одна поверхность (однозначно) | **наблюдается** у 283 из 302 красящихся | 16.4 |
| тип → 0 поверхностей | **наблюдается** у 12 красящихся | 16.4.1 |

## 16.8. Минимальный безопасный механизм (6.4)

Универсального отображения «ключ скина → Paint Slot» нет. Минимальный
механизм, опирающийся только на подтверждённые данные:

1. `[П]` **Привязка `LegacyFlattenedSurface` к детали** по ключу
   *(путь пака, локальный id)* — это то, что файл уже содержит; ничего не
   требуется додумывать, и это сохраняет старый вид.
2. `[П]` **Производный якорь** для будущего авторинга: `(SCX, имя material
   group)` вычисляется из CFG `texture` + списка текстур Render; он однозначен
   у 283/302 и должен храниться как provenance, а не как `slot_id`.
3. `[Т]` Для 7 шасси и любых будущих многогрупповых случаев нужна **явная
   авторская разметка** (какая группа становится каким слотом), потому что
   legacy-данные этого не различают.
4. `[Т]` `slot_id` не может быть производным от `GII_TYPE`: это нарушило бы A2
   (инварианты `slot_id`) и A3 (явная декларация), а данные к тому же дают
   отношение «несколько типов → одна поверхность» (16.4.4).

Комбинация 1 + 2 + 3 — минимальна и не требует нового архитектурного решения:
1 закрывает сохранение, 2 даёт детерминированный кандидат, 3 закрывает остаток.

## 16.9. Итог B2

**Можно ли построить надёжное автоматическое соответствие Legacy Skin Preset →
Paint Slot?**

`[Т]` **Нет.** Достоверно и автоматически выводится только промежуточная
ступень: *ключ скина → Part Resource → CFG → поверхность покраски
(SCX + material group)*. Она однозначна у 283 из 302 красящихся ресурсов, у 7
даёт два кандидата, у 12 не даёт ни одного. Ступень *поверхность → Paint Slot*
данными не покрывается:

- Paint Slot по A3 — явно объявляемая сущность, а не legacy-поле;
- legacy-«слот» — это одна канва на деталь, а не на группу;
- имя группы не идентификатор (52 имени, 33 переиспользованы, 3 автогенерированы);
- одна поверхность принадлежит нескольким `GII_TYPE`;
- переносить из изображения нечего как редактируемую краску: оно сплющено.

`[Т]` Практический вывод для миграции: legacy-скин привязывается к **детали** и
сохраняется как `LegacyFlattenedSurface`; переход на Paint Slot — отдельное
явное действие, в котором автор указывает слот (A3). Для 283/302 деталей
кандидат можно предложить автоматически, но это подсказка, а не назначение.

## 16.10. Сводные числа B2

| показатель | значение |
|---|---:|
| найдено Skin Preset | **1** (манифест + 14 sidecar; содержание зафиксировано в `[Э9]`, в текущей установке файлы отсутствуют) |
| уникальных `GII_TYPE` исследовано | **14** |
| связаны с конкретными Part Resource | **14 / 14** |
| из них `native car` / `native part` | 1 / 13 |
| с однозначным путём до legacy-поверхности (1 имя) | **14 / 14** |
| с несколькими кандидатами | 0 в выборке; **7** во всём штатном контенте |
| без какой-либо поверхности | 0 в выборке; **12** во всём штатном контенте |
| Part Resource в 10 racer-пакетах | 481 (302 с `texture`, 179 без) |
| 302 красящихся: 1 / 2 / 0 имён | 283 / 7 / 12 |
| различных painted-имён | 52 (33 переиспользованы, 3 автогенерированы) |
| SCX, общих для нескольких Part Resource | 13 (8 `chassis.scx` + 5 окон Baiern) |

## 16.11. Что уточнено и что осталось неизвестным

Уточнения к предыдущим этапам:

1. `[Ф-код]` `GII_TYPE` = **6** (а не «тип детали» вообще): константа получена
   из `GameType.class`, диспетчер хоста с ней согласуется.
2. `[Ф-код]` Ключ скина — wired `(ordinal пака << 16) | local`; таблица паков в
   трейлере — не «идентичность машины», а техническая зависимость SDAT,
   благодаря которой ключ всё же разрешается в конкретный пак (16.1.2).
3. `[Ф-рес]` `[Э13 §1]` описывает вызов paint-натива как `getInfo(GII_TYPE)=2`.
   По таблице констант 2 — это `GII_POS`, а сравниваемые значения `9` и `5` —
   `GIR_CAT_PART` / `GIR_CAT_VEHICLE` (категория, `GII_CATEGORY` = 7). Номер
   запроса в этом вызове этапом 16 не переопределяется; для B2 он не
   используется. Stage 13 не переписывался — противоречие зафиксировано здесь.
4. `[Ф-рес]` Skin Preset разрежен: 14 ключей из 31 красящейся детали Einvagen.
5. `[Ф-рес]` Файлы `save/skins/21312*` в текущей установке отсутствуют (каталог
   пуст); разбор опирается на зафиксированные в `[Э9]`/`[Э10]` дампы и
   проверен по содержимому `Einvagen.rpk`.

Неизвестное:

- `[?]` Почему у Duhen/Enula два имени читают одну текстуру (авторская
  раскладка 2005 года) — по данным не восстановимо.
- `[?]` Есть ли в других установках скины с ключами из нескольких паков
  (в исследованном — один пак); механизм это допускает, экземпляра нет.
- `[?]` Как поведёт себя `loadSkin` при нескольких экземплярах одного Part
  Resource на машине (теоретически — только первая деталь; в контенте не
  проверено).
- `[?]` Точная семантика `Personality`/`Set`-описаний машин в части состава
  деталей не исследовалась: для B2 достаточно того, что ключ адресует ресурс.

---

# Stage 17 — DiffuseMixSecond alpha semantics (B3)

**Проверяемый HEAD:** `739b0c607f74d5bd02d36e5c356cd5e23d208d91`.
Read-only аудит: C++, Java, шейдеры, SCX/CFG/RPK, текстуры и сохранения не
изменялись; форматы не проектировались; игра не запускалась. Stages 2–16 не
переписывались. Вопрос B3: какова фактическая семантика alpha-канала
существующего второго текстурного слоя `DiffuseMixSecond` (`0x06000001`) в
игровом контенте.

Три утверждения разделены, как требовалось:

1. alpha-композитинг существует в шейдере — было подтверждено ранее (§6.3);
2. alpha конкретной текстуры содержит полезную маску — проверено здесь (17.4–17.6);
3. маска используется автором именно как overlay — проверено здесь (17.4, 17.7)
   с оговоркой о том, что «overlay» в автомобилях и в мире означает разное.

## 17.1. Объём исследования и границы

В отличие от Stage 15 (496 представительных SCX из CFG), здесь выполнен
**полный проход по всем `.scx` на диске** и по всем Render-ресурсам всех 82
пакетов, включая световые состояния, LOD и повторные контексты.

| Величина | Значение |
|---|---:|
| `.scx` на диске, разобрано | **3244** (100 %; ошибок разбора 0) |
| material groups всего | 5538 |
| групп с независимым mix (`mix ≠ 0xffff`, `mix ≠ tex`) | **357** |
| SCX, содержащих такие группы | **284** |
| Render-ресурсов (тип 14) во всех 82 пакетах | 2954 |
| SCX, используемых Render-ресурсами | 2466 |
| SCX, используемых более чем одним Render | 78 |
| из них с разными списками текстур в разных Render | 28 |
| Render-контекстов, попадающих в группу с mix | 344 |
| различных использований (SCX + группа + mix + файл) | **283** |
| уникальных файлов второго слоя | **132** |
| SCX с mix, для которых Render-контекст не найден | 10 |

Разбор формата SCX (v3 и v4) предварительно сверен с B1: на тех же 276
PF-ассетах критерий 15.6.3 даёт ровно `133 + 300 + 36 = 469`
(`mix = 0xffff` / независимый / `mix == tex`), то есть парсер и критерий
совпадают с принятыми в Stage 15 `[Ф-рес]`.

Категории (по путям):

| Категория | SCX с mix | Использований |
|---|---:|---:|
| `cars/racers/**` | 249 | **245** |
| `parts/engines/**` | 22 | 22 |
| `parts/Neons/**` | 3 | 3 |
| `misc/**` (2 дилер + 5 гаражей) | 7 | 10 |
| `objects/**` | 1 | 1 |
| `DestructionTools/**` (сторонний пакет) | 2 | 2 |

Меньше использований, чем SCX, потому что 63 SCX рисуются несколькими Render с
одинаковым результатом (схлопываются в одно использование), а часть SCX не
достигается вовсе.

`[Ф-рес]` Второй слой **не используется вообще** в: `cars/traffic/**` (97 SCX),
`cars/fake_racers/**` (30), `cars/misc/**` (31), `parts/wheels/**` (422),
`frontend/**` (22), `humans/**` (16), `maps/**` (12), `MW_Mod/**` (7),
`particles/**` (4), `Data/**` (1). То есть mix — свойство кузовных деталей,
двигателей и отдельных объектов мира, а не сквозной механизм рендера.

`[Ф-рес]` 10 SCX с независимым mix не достижимы из Render-ресурсов: две
пользовательские копии (`F_bumper — копия.scx`, `chassis — копия.scx`), два
физических меша `_phys_L/R_headlights.scx`, `dual_cold_air_intake.scx` (Enula),
`marketbelso.scx` (дилер) и четыре меша гаражей (`garazs_01`, `garazs2`,
`garazs3`, `roc_g_talaj.SCX`). Для них второй слой не разрешается (класс F), и
это не выдаётся за «alpha отсутствует».

## 17.2. Как `mix_index` разрешается в текстуру

`[Ф-код]` `mix_index` — не глобальный ID, а **индекс в списке текстур того
Render-ресурса**, который рисует данный SCX (`texture 0x…` строки payload
типа 14, порядок строк = порядок массива).

`[Ф-рес]` Один и тот же SCX может рисоваться разными Render-ресурсами с
**разными** списками: таких SCX 28 из 2466; среди SCX с mix — 63 имеют более
одного контекста, и в 344 контекстах получается 283 различных использования.
Пример на знакомом ассете: `cars/racers/Einvagen_data/meshes/chassis.scx`,
группа `chassis`: `tex = 2` → `cars\textures\default_white.png`, `mix = 3` →
`Einvagen_chassis.dds` `[Ф-рес]`.
Поэтому «один SCX = один второй слой» неверно: пара *(SCX, Render)* — единица
разрешения.

Крайние случаи:

- `Focer L_headlights_2_CITY.scx`, группа `headlights2`, `mix = 1` при списке
  из одной текстуры → индекс вне диапазона; в host `texture(slot)` возвращает
  белую заглушку, `mixed` становится ложным, слой выключается. Один такой
  случай (класс F).
- Текстуры-источники: `sourcefile` типа 7 → loose-файл на диске; 132 файла
  существуют, ни один не потерян.

## 17.3. Форматы и фактическое наличие alpha

`[Ф-рес]` 132 уникальных файла второго слоя:

| Формат | Файлов | Alpha в формате | Наблюдаемое поведение |
|---|---:|---|---|
| DDS DXT5 (BC3) | 75 | 8-битная, интерполяция | 74 с реальной маской, 1 полностью прозрачный |
| DDS DXT3 (BC2) | 47 | 4-битная explicit (16 уровней) | все 47 с маской |
| DDS DXT1 (BC1) | 5 | 1 бит / отсутствует | у всех 5 alpha = 255 везде |
| PNG | 3 | RGBA | 2 маски (`Registration.png`, `Cubes.png`), 1 полностью прозрачный (stock `paintable.png`) |
| PTX (stock-контейнер) | 2 | JPEG RGB + **отдельный JPEG alpha** | маска |

`[Ф-рес]` PTX-случай важен как доказательство авторского намерения: в
`Enula_rally_headlights.ptx` (7 мипов, 256×256) каждый уровень хранится двумя
JPEG — RGB и alpha; у старшего мипа `rgb_size = 9716`, `alpha_size = 4221`, у
младшего (32×32) — 653 и 361. Alpha записывалась **отдельным изображением**, а
не как побочный продукт RGB. `Focer_rally_headlights.ptx` байт-в-байт
идентичен файлу Enula (md5 `0ea5b830…`) — Focer переиспользует ту же текстуру.
Формат PTX описан в `src/resources/ptx_image.cpp:21-36`.

Итог по 132 файлам: **125** имеют пространственно меняющуюся alpha, **5**
полностью непрозрачны (все DXT1), **2** полностью прозрачны (DXT5
`Einvagen_R_wing.dds` и stock `Mazda paintable.png`, см. 17.6).

## 17.4. Что alpha делает в контенте: четыре практики

Классификация проведена по данным (текстура + контекст), а не по именам.

### 17.4.1. Автомобили: маска покрытия краской (204 использования)

`[Ф-рес]` В 243 из 245 автомобильных использований база группы — это
**объявленный в CFG слот краски** (`PartConfig::texture`); ещё в одном
(`radiator.scx`, CFG без объявления `texture`) база тоже `default_white`. База
— не «нейтральный фон», а заводской цвет краски: во всех 10 racer-моделях
встречаются только пять глобальных текстур-заглушек:

| База | Файл | Использований | Модели |
|---|---|---:|---|
| `0x00020025` | `cars\textures\default_white.png` | 168 | Baiern, Duhen, Einwagen, Enula, Focer, Nonus, Prime |
| `0x00020026` | `cars\textures\default_yellow.png` | 31 | Mazda 3 2005 |
| `0x0002001d` | `cars\textures\default_blue.png` | 22 | SuperDuty |
| `0x00020020` | `cars\textures\default_gray.png` | 19 | MC |
| `0x0002001c` | `cars\textures\default_black.png` | 4 | Mazda 3 2005 |

Второй слой — собственная текстура детали, и формула приобретает вид:

```
albedo = mix(краска, текстура_детали, alpha)
```

то есть alpha = **доля, в которой видна текстура детали, а не краска**. Это не
«декаль поверх краски», а авторская маска: где alpha = 1, поверхность не
красится (стекло, хром, решётка, пластик, багажник), где alpha = 0 — красится
целиком.

Наглядные примеры (композит над условной красной краской):

- `Einvagen_chassis.dds` — кузовной атлас: моторный отсек, пол, подвеска
  непрозрачны, внешние панели прозрачны для краски;
- `Baiern_F_bumper_2.dds` — бампер проявляется сквозь краску, непрозрачны
  только решётки и молдинги;
- `Enula_rally_headlights.ptx` — непрозрачны только круглые фары и крепёж;
- `MC_mirror.dds` — непрозрачно зеркальное стекло, корпус красится;
- `Baiern_F_door.dds` (DXT3, 16 уровней) — жёсткая маска панели.

Крайние значения той же семантики:

| alpha | Что видно | Пример |
|---|---|---|
| 0 везде | только краска, деталь целиком красится | stock `paintable.png` (Mazda, 35 использований) |
| 255 везде | только текстура детали, краски нет | `Baiern_headlights.dds` (4 использования) |

Поэтому «полностью непрозрачные» автомобильные случаи — не другая практика, а
предел той же маски. Отдельно стоит `Einvagen_R_wing.dds`: alpha = 0 при
заданном `mix` — слой фактически отключён (ошибка автора или намеренное
выключение).

### 17.4.2. Двигатели: второй слой как основная текстура (22 использования)

`[Ф-рес]` Все 22 использования в `parts/engines/MC_Prime_SuperDuty/**` —
`alpha = 255` везде, текстуры DXT1 (`vee8_all.dds`,
`vee8_cylinder_head_cover.dds`). База при этом — `0x00000009`,
`parts\textures\Noise.dds` (глобальный шум): первый слой является заглушкой, а
вся видимая текстура приходит вторым слоем. 21 из 22 использований — меши
`Lod1`/`Lod2`/`Lod3`, то есть для двигателей второй слой сознательно повторён во
всех LOD.

`[Т]` Это не маска и не overlay: `mix` использован как «второй основной
диффуз». Семантика alpha здесь не задействована, а формула даёт полную замену
базы.

### 17.4.3. Мир, объекты, гаражи: маска графики поверх поверхности (6 использований)

`[Ф-рес]` `centers.dds` (2048×256) — атлас дорожной разметки и травы;
`beginend.dds` — парный атлас. Используются в `objects/…/roadtest26_egyedi`,
`misc/dealer/marketkulso.scx`, `misc/garage/g4/roc_g_egyeb.SCX`. Здесь alpha
действительно работает как **маска наложения графики** (разметка, знаки,
трава) поверх базовой поверхности; 86 % пикселей имеют промежуточные значения,
то есть это непрерывная маска.

`[Ф-рес]` Рядом — 5 использований `sivatag.dds` / `dirt1.dds` с `alpha = 255`:
полная замена базовой текстуры земли в сцене (та же «заглушка + второй слой»,
что у двигателей).

### 17.4.4. Детали `parts/Neons`: маски-шаблоны (3 использования)

`[Ф-рес]` `Registration.png` (482×313, 42 уровня alpha, 78 % прозрачных) —
маска панели с номерным знаком; `Cubes.png` (169×65, ровно два уровня alpha
89/255) — шаблон кубов неона. Обе — маски поверхности, а не пользовательская
графика.

## 17.5. Статистика по использованиям

Различных использований 283 (`SCX + группа + индекс + файл`):

| Класс | Значение | Использований |
|---|---|---:|
| **A** | подтверждённая маска наложения (графика поверх базы) | **6** (`centers`/`beginend`, мир и объекты) |
| **B** | маска включения/выключения, без подтверждения overlay-назначения | **1** (`Cubes.png`) |
| **C** | непрерывная/полутоновая маска | **208** |
| **D** | alpha не даёт пространственной маски: 255 везде | **31** |
| **D** | alpha не даёт маски: 0 везде (слой отключён) | **36** (stock Mazda + `R_wing`) |
| **F** | связь не разрешена (индекс вне списка) | **1** |

По категориям:

| Категория | Использований | Маска (A/B/C) | alpha = 255 | alpha = 0 | F |
|---|---:|---:|---:|---:|---:|
| `cars/racers` | 245 | 204 | 4 | 36 | 1 |
| `parts/engines` | 22 | 0 | 22 | 0 | 0 |
| `parts/Neons` | 3 | 3 | 0 | 0 | 0 |
| `misc` | 10 | 5 | 5 | 0 | 0 |
| `objects` | 1 | 1 | 0 | 0 | 0 |
| `DestructionTools` | 2 | 2 | 0 | 0 | 0 |

`[Т]` В автомобилях маска присутствует в **204 из 245** использований (83 %),
но это маска *покрытия краской*, а не overlay пользовательской графики. Маска
наложения в буквальном смысле (графика поверх независимой базы) достоверно
подтверждена в **7** использованиях (6 «мир/объекты» + 1 `Cubes.png`), то есть
в 2.5 % от всех 283.

Распределение по десяти racer-моделям:

| Модель | Использований | Маска | alpha = 255 | alpha = 0 |
|---|---:|---:|---:|---:|
| Baiern | 31 | 27 | 4 | 0 |
| Duhen | 27 | 27 | 0 | 0 |
| Einwagen | 24 | 23 | 0 | 1 |
| Enula | 34 | 34 | 0 | 0 |
| Focer | 7 | 6 | 0 | 0 (+1 F) |
| MC | 19 | 19 | 0 | 0 |
| Mazda 3 2005 | 35 | 0 | 0 | 35 |
| Nonus | 25 | 25 | 0 | 0 |
| Prime | 21 | 21 | 0 | 0 |
| SuperDuty | 22 | 22 | 0 | 0 |

Световые состояния и LOD: у автомобилей среди 245 использований под-рендер
всего один (`L_headlights_2_CITY.scx`) — и именно он не разрешается; ни одно
световое состояние (`_INDICATOR`, `_BRAKE`) второй слой не использует. У
двигателей, наоборот, 21 из 22 использований — именно LOD-меши (`Lod1`–`Lod3`)
с той же парой текстур, то есть authoring там осознанно перенесён во все LOD.

## 17.6. Mazda 3 2005 — контрольный пример

`[Ф-рес]` Все группы Mazda (35 SCX, единственное имя группы — `carcolor`;
62 записи групп из §15.10 — это 35 уникальных плюс 27 дублей внутри SCX, как в
Stage 16 §16.4) указывают `mix` на
`cars/racers/Maz3_2005_data/textures/paintable.png` — 35 различных
использований, 1 файл.

`[Ф-рес]` **Stock-файл в репозитории полностью прозрачен**: 256×256 RGBA,
alpha = 0 во всех 65 536 пикселях, RGB = (0,0,0) везде; так он выглядит и в
`9950ec6` (1 617 Б), и в текущем HEAD (3 231 Б).

Отсюда: в исходном контенте второй слой Mazda **не даёт никакого визуального
эффекта** — `mix(base, black, 0) = base`, вся панель красится базой. Имя файла
(`paintable`) описывает результат: деталь целиком «покрашиваемая».

`[Т]` Уточнение к §15.10 (пункт B): подтверждённое пользователем поведение
«`paintable.png` работает как alpha-overlay» получено на **заменённом
пользовательском файле** (в рабочем дереве лежит версия с надписью
«ONE ESCAPEE»: 86,2 % alpha = 0, 11,2 % = 255, 2,6 % промежуточных [Ф-рес]).
Это корректное подтверждение **механизма** (шейдер смешивает по alpha и
результат виден в игре), но не подтверждение того, что stock-контент
использует слой как графический overlay. Обе формулировки разделены в §6.3
архитектуры.

## 17.7. Что выдаёт текущая формула в крайних случаях (9.3)

`[Ф-код]` `albedo = mix(base.rgb, detail.rgb, detail.a)` (`shaders/fs_vehicle.sc:68`,
`shaders/fs_mesh.sc:181`) при `mixed = mix_index ∉ {0xffff, texture_index}` и
валидной текстуре:

| Ситуация | `detail.a` | Результат |
|---|---|---|
| alpha отсутствует в формате (DXT1 без 1-бит alpha) | 1.0 | **полная замена** базы текстурой детали |
| alpha = 255 везде | 1.0 | полная замена базы (двигатели, фары, земля) |
| alpha = 0 везде | 0.0 | слой невидим, остаётся только база |
| бинарная маска | 0/1 | жёсткое переключение «текстура/база» |
| непрерывная маска | 0…1 | плавный переход |

`[Т]` «Отсутствие alpha» не является нейтральным: это `detail.a = 1`, то есть
самый агрессивный вариант. Всё, что удерживает второй слой от полной замены
базы, — именно содержимое alpha.

`[Ф-код]` Дополнительная нагрузка на ту же alpha: `shaders/fs_vehicle.sc:23`
объявляет `u_vehicleLayers = (mix, reflection weight, complemented alpha,
 painted)`, и строки `:179-180` умножают вес отражения на `1 - detail.a`.
Таких использований 67 из 283 (все — кузовные панели, тип материала `0x15`).
То есть в этих группах alpha управляет и albedo-смешением, и силой отражения;
это существующее поведение, а не предположение.

## 17.8. Ответы на вопросы 9.1–9.5

**9.1. Единая семантика?** Нет `[Т]`. Наблюдаются четыре практики: маска
покрытия краской (автомобили), второй основной диффуз при alpha = 1
(двигатели, земля), маска наложения графики (мир/объекты), маски-шаблоны
(Neons). Общее у них только одно: **alpha — пространственный коэффициент
смешивания двух albedo**, а не самостоятельная сущность.

**9.2. Как часто alpha реально работает маской?** `[Ф-рес]` 215 из 283
использований (76 %) имеют пространственно меняющуюся alpha; 204 из них — в
автомобилях. Но «работает маской» ≠ «используется как overlay»: как overlay
графики — 7 использований.

**9.3. Что при отсутствии alpha?** Полная замена базы (17.7). Никакого
«нейтрального» поведения нет.

**9.4. Отличия автомобилей и мира?** `[Ф-рес]` Да, и по данным, и по смыслу:
в автомобилях база — слот краски, второй слой — текстура детали (маска
покраски); в мире/объектах база — поверхность, второй слой — графика (маска
наложения). Формулы `fs_vehicle` и `fs_mesh` различаются лишь множителем
`u_vehicleLayers.x` и обработкой отражения; сама семантика alpha задаётся
контентом.

**9.5. Готовый `ContentAlbedoLayer`?** `[Т]` Как **низкоуровневый примитив** —
да: один дополнительный albedo-слой с alpha-смешиванием, собственным
`ScxTextureMap` (channel/tiling/scale/offset) и композитингом до освещения.
Как **свободный слот** — нет: во всех 245 автомобильных использованиях он уже
занят авторским содержимым (маска покрытия или её предельные значения);
положить туда пользовательский paintjob, не потеряв авторскую маску, нельзя.
Это же ограничение зафиксировано в §6.3 архитектуры («ровно один mix-слой на
группу, владелец — модель»).

## 17.9. Итог B3

**Можно ли считать alpha существующего `DiffuseMixSecond` универсальной маской
overlay?**

`[Т]` **Нет — но alpha почти всегда является пространственной маской.**
Универсальной «маски overlay» она не является, потому что:

1. в автомобилях (83 % использований) alpha означает **покрытие краской**, а
   не наложение пользовательской графики;
2. в 31 использовании alpha вообще не участвует (255 везде) — второй слой
   работает как основная текстура;
3. в 36 использованиях alpha = 0 — слой отключён;
4. маска наложения в буквальном смысле подтверждена только в 7 случаях
   (мир/объекты/Neons);
5. назначение маски задаётся автором модели и не выводится из формата.

**Пригодность как примитива:** `[П]` alpha-композитинг пригоден как
низкоуровневый механизм и для `ContentAlbedoLayer`, и для `PaintjobLayer` —
формула одна и та же. Но это **не** делает сущности одинаковыми: контентный
слой принадлежит модели и уже занят, пользовательский слой принадлежит
экземпляру и требует отдельного слота (§6.3, §11). B3 закрывает именно вопрос
о семантике данных, а не о владении.

## 17.10. Ограничения и неизвестное

- `[?]` Для 10 SCX без Render-контекста второй слой не разрешается; их
  назначение не установлено (пользовательские копии, физические меши, гаражи).
- `[?]` Один случай `mix` вне диапазона списка (`Focer L_headlights_2_CITY`):
  поведение оригинала (чтение за границей массива или отключение) не
  восстановлено.
- `[?]` Почему авторы двигателей выбрали схему «Noise.dds + второй слой»
  вместо обычной базовой текстуры — по данным не восстановимо.
- `[?]` Визуальная интерпретация выполнена по декодированным пикселям и
  композиту над условным цветом; игровой рендер не запускался. Для stock
  `paintable.png` вывод «эффекта нет» следует из `alpha = 0` и не зависит от
  рендера.
- `[?]` UV-преобразования `mix_map` (channel/tiling/offset) в статистику не
  входили: разбор свойств `0x06000001` в `scxscan` пропускает 28-байтовый
  `ScxTextureMap`; утверждение «у mix свой UV» взято из кода
  (`scx_mesh.cpp:276-293`), а не из подсчёта.
- `[Ф-рес]` Рабочее дерево содержит пользовательскую версию
  `Mazda paintable.png`; статистика stock-контента в 17.3–17.6 считается по
  версии из HEAD, пользовательская версия описана отдельно (17.6).

---

# Stage 18 — Material instance persistence and LegacyFlattenedSurface storage (B4)

**Проверяемый HEAD:** `b5033822102a3897ac8612d1210f796ca9baeb21` (`B3`).
Read-only исследование: C++, Java-классы игры, шейдеры, SCX/CFG/RPK, текстуры и
сохранения не изменялись; бинарные форматы не проектировались и не вводились;
игра не запускалась. Stages 2–17 не переписывались. Вопрос B4: как хранить
запечённые поверхности, пользовательские изображения и ссылки `legacy_flat` в
постоянном состоянии экземпляра автомобиля.

Результат — логическая модель владения и хранения. Байтовый формат
(`SAVEFILEID`, SDAT trailer, таблица ресурсов, имена файлов) намеренно **не**
фиксируется (§10 задания, §18.12).

## 18.1. Границы исследования

| Что проверено | Как |
|---|---|
| Сериализация машины/инвентаря/скина | игровые Java-классы (`sl/Scripts/game/*.class`), записанные дампы `[Э9]`–`[Э11]` |
| Формат контейнера сохранения | код host (`natives_io.cpp`), реальные файлы `save/**` на HEAD |
| Запись/чтение ссылок на ресурсы | `sdat_wire_resource`, `sdat_remap_resource`, `File.write/readResID` |
| Пиксели в сохранении | измерения реальных файлов + дамп скина `21312` (Stage 9–10) |
| Динамические текстуры и покраска | `part_runtime.cpp` (`paint_one`, `setTexture`), реестр `RpkIndex` |
| Надёжность записи | `File.open/close/delete/copy/move`, `BACKLOG.md`, `MILESTONES.md` (M5) |
| Прецедент собственного файла состояния | `src/platform/host_game_options.cpp` |

Не исследовалось: содержимое пользовательских скинов за пределами дампа
`21312`; поведение движка при одновременной записи двух сохранений; точный
момент вызова `GameLogic.save` из UI (для B4 не требуется — важен порядок
«сначала legacy-поток, потом собственное хранилище», §18.9).

## 18.2. Установленная база B1–B3 (не переаудируется)

- `[Ф-рес]` **B1** (Stage 15): 276 ассетов, 337 Legacy Paint Groups, 469
  именованных групп; legacy-покраска — это выбор текстуры для слота,
  объявленного CFG, а не генерация пикселей.
- `[Ф-рес]` **B2** (Stage 16): ключ Skin Preset — Part Resource
  `(пак, локальный id)`, не «тип детали»; поверхность — одна канва на деталь;
  универсального отображения на Paint Slot нет (283/7/12), имена групп не
  идентификаторы.
- `[Ф-рес]` **B3** (Stage 17): `DiffuseMixSecond` — авторский слой модели
  (маска покрытия краской или маска наложения), слот занят моделью; это не
  пользовательская графика.

Из этого следует главное для B4: **пользовательские данные не могут опираться
на группу, имя группы, индекс текстуры или `GII_TYPE`** — ни как на ключ, ни
как на адрес.

## 18.3. Четыре вида данных и их уровни

`[Т]` До выбора формата разведены четыре объекта с разными владельцами:

| Данные | Уровень владения | Первичное (источник истины) | Производное |
|---|---|---|---|
| `LegacyFlattenedSurface` | **деталь внутри `CarRecord`** конкретного сохранения | готовое изображение (RGB, без alpha) + provenance отдельно | — |
| `PaintjobLayer` | **слот** (через `Assignment`) | изображение + alpha + трансформ + параметры | GPU-текстура |
| `Decal Instance` | **экземпляр автомобиля** | изображение + одна трансформация + проекция + порядок | `Surface Contribution`, bake/atlas |
| `Surface Contribution` / bake | целевая поверхность | — | ✔ пересчитывается всегда |
| `ContentAlbedoLayer` | **модель** (SCX) | авторский `DiffuseMixSecond` | — |

`[Т]` `LegacyFlattenedSurface` и bake декали — **разные вещи**: первое
сохраняется как готовое изображение старой системы, второе восстанавливается
из `Decal Instance`. Смешивать их нельзя (§3.4 задания, §11.2.1).

## 18.4. Как сохранение устроено на HEAD

### 18.4.1. Сериализатор — игровой Java-код, а не host

`[Ф-код]` В host нет ни одного упоминания `SAVEFILEID`, `saveCar`,
`createFromFile` (поиск по `src/` и `include/` — 0 вхождений). Всю
сериализацию выполняют игровые классы (`java.game.GameLogic`,
`java.game.Vehicle`, `java.game.Part`, `java.game.Inventory`), работающие на
хостовой JVM через `java.io.File`.

`[Т]` Следствие: **расширить legacy-поток нельзя**. Новое поле в `Part.save`
или `Vehicle.save` потребовало бы правки игровых классов, что запрещено
условиями задания. Пользовательское состояние OpenSLRR обязано жить в
собственном хранилище, а не внутри `main`/`PlayerCarN`.

### 18.4.2. Что пишется про деталь

`[Ф-код]` `Part.save` (игровой Java, дамп `[Э9 §2]`; сверено с байтами
сохранений в §4 AUDIT):

```
[+0]  u32  part type id        (ResourceRef, typeof 8)
[+4]  i32  version             (= 1)
[+8]  u32  TEXTURE             (ResourceRef; Part.getTexture())
[+12] u32  mesh                (ResourceRef)
[+16] f32  wear, [+20] f32 tear
[+24] ...  слоты: [slot][nameLen][name][childSlotID][child…]  до i32 -1
[..]  хвост подкласса
```

`[Ф-код]` `getTexture()` на HEAD возвращает `state.texture`
(`part_runtime.cpp:4975`). Поле инициализируется CFG-текстурой при создании
детали (`:1192`), меняется `paint_one` (`:1307`) и `setTexture` (`:4979`).

`[Т]` Пикселей в записи детали нет вообще: единственное «изображение» —
4-байтовая ссылка на текстурный ресурс. Пользовательский paintjob, декаль или
запечённая поверхность в этот поток не помещаются физически.

### 18.4.3. Контейнер SDAT: payload + blobs + packs

`[Ф-код]` `natives_io.cpp`: `File.open(mode≠0)` только помечает файл как SDAT
(`:573`), `File.close` дописывает конверт (`:607`), `sdat_frame` (`:171`)
формирует:

```
"SDAT" 01 00 02 00 | payload | [unsaved: ordinal,kind,size,bytes]…
                             | [packs: ordinal,len,path]…
                             | span(u32)
```

`sdat_take_payload` (`:81`) при чтении отделяет payload, сохраняя blob-записи и
таблицу паков в памяти (`HostFile::sdat_blobs/sdat_packs`), а `readResID`
(`:724`) пропускает прочитанное через `sdat_remap_resource` (`:248`).

`[Ф-рес]` Измеренные сохранения на HEAD (парсер `b4_sdat.py`, все 7 файлов):

| Файл | Размер | payload | blob-записей | паков |
|---|---:|---:|---:|---:|
| `save/carrer/1-1/main` | 10 798 | 10 478 | **0** | 10 |
| `save/carrer/1-1/PlayerCar0` | 4 609 | 4 428 | **0** | 5 |
| `save/carrer/1-1/PlayerCar1` | 4 321 | 4 068 | **0** | 8 |
| `save/carrer/2-1/main` | 10 783 | 10 463 | **0** | 10 |
| `save/carrer/2-1/PlayerCar0` | 4 936 | 4 755 | **0** | 5 |
| `save/temp/PlayerCar0` | 4 609 | 4 428 | **0** | 5 |
| `save/temp/CopyCar` | 4 292 | 4 012 | **0** | 9 |

Payload `PlayerCar0` начинается с `0x88664422` (`SAVEFILEID_VHC`), версия `5`,
далее запись шасси с текстурой `0x00020026`
(`cars\textures\default_yellow.png`, заводской цвет Mazda — совпадает с B3).

### 18.4.4. Хост: как ссылки превращаются в числа

`[Ф-код]` `sdat_wire_resource` (`:205`):

| Вход | Результат в потоке | Что в трейлере |
|---|---|---|
| `0` | `0` | — |
| ресурс пака (`pack<<16\|local`) | `(ordinal<<16)\|local` | строка пака (путь от корня игры) |
| `0xFFFFxxxx`, restype 5/7 | номер сессии (`ordinal`) | blob-запись: `ordinal, kind, size = 0`, **байтов нет** |
| иное | ошибка → `write` возвращает `0` | — |

`[Ф-код]` `sdat_remap_resource` (`:248`): для пака — обратная замена по
ordinal; для blob — возврат `blob.abs_id`, **только если он ненулевой**.

`[Ф-код]` `abs_id` заполняется единственным местом — `sdat_wire_resource` в
процессе записи (`:221-226`). При чтении с диска `sdat_take_payload` кладёт
байты в `blob.bytes`, но `abs_id` остаётся `0` (`SdatBlobRef`,
`natives_internal.hpp:72-77`). Поэтому **после перезагрузки любая blob-ссылка
разрешается в `0`**, даже если байты физически лежат в трейлере.

`[Т]` Трейлер структурно способен нести произвольные байты и хост сохраняет их
при перезаписи файла (`sdat_frame` выводит `blob.bytes` обратно), но
**декодировать их в ресурс нечем**: нет ни пути, ни вызывающего кода.

### 18.4.5. Skin Preset: пиксели лежат вне контейнера

`[Ф-рес]` Дамп манифеста `21312` (Stage 9): 14 пар «wired part id → ordinal»,
14 blob-записей `kind = 7`, **`size = 0`**, одна строка пака; изображения —
отдельные JFIF-файлы `21312.1 … 21312.14` (1 654 … 25 898 Б).

`[Ф-код]` Stage 10 (дизассемблер оригинала): имя sidecar строится как
`sprintf("%s.%d", path, ordinal)`; загрузка привязывает к ordinal внешний
ресурс с этим именем (ленивая загрузка). То есть даже оригинальный движок
**не встраивал пиксели в SDAT** — трейлер использовался как таблица
идентичностей, а содержимое лежало рядом.

`[Ф-код]` В host нет ни энкодера JPEG, ни построения имён sidecar (поиск
`sidecar`/`"%d"` в `src/` — только комментарий про DDS-сайдкар мешей),
`stb_image` используется только для декодирования PTX/PNG. Поэтому
`saveSkin` на HEAD создаёт манифест с нулевыми blob-записями и **не создаёт
изображений**; `loadSkin` получает `0` и `setTexture(0)` ничего не меняет.

`[Ф-док]` Тот же механизм применяется и к сохранению машины: Stage 9 фиксирует
`CarLot.saveCar` с удалением `PlayerCar0.*` и копированием `PlayerCar*`, то есть
sidecar-набор существует и для `PlayerCarN`. В текущей установке таких файлов
нет (`save/carrer/*` содержит только `main` и `PlayerCarN`), поэтому их запись и
чтение не наблюдались; для модели это меняет только источник импорта, не ключ.

### 18.4.6. Чего в хосте нет

`[Ф-код]` Проверено поиском и чтением:

- нет динамических текстур: `0xFFFFxxxx` встречается только в SDAT-ветке;
  `RpkIndex::resolve` работает лишь с паками (`rpk_index.cpp`);
- `Part.setTexture` (`part_runtime.cpp:4979`) принимает только разрешимый
  ресурс `typeof 7`, иначе оставляет прежнее значение;
- нет клонирования/записи пикселей: `paint_one`/`paint_tree` (`:1307`, `:1316`)
  лишь присваивают `state.texture` существующий id;
- рендер принимает **путь**, а не id: `instance.paint_texture` — строка
  (`scene_instances.cpp:1857`), полученная из `texture_path_for_id`
  (`rpk_index.cpp:467`); «покрашенность» вычисляется сравнением
  нормализованных путей (`scene_instances.cpp:1825-1848`).

### 18.4.7. Надёжность записи (факты, без исправлений)

`[Ф-код]` `File.close` (`:607`) пишет целевой файл напрямую с
`std::ios::trunc` — без временного файла, без переименования, без `fsync`.
Метод возвращает `void`, поэтому Java не может узнать о неудаче. При неудаче
байты остаются в памяти процесса (`g_unflushed_saves`) и подставляются
следующему `open` на чтение — но только внутри сессии.

`[Ф-код]` `File.copy` (`:551`), `File.move` (`:562`) и `File.exists` (`:527`)
эту карту не учитывают: они работают с диском. `File.delete` (`:506`) карту
чистит.

`[Ф-док]` `MILESTONES.md` (M5): «the stock scripts delete saves before writing
their replacement and `File.close` cannot report failure». То есть окно
потери данных у legacy-потока существует и известно; B4 его не исправляет, но
обязан не усиливать (§18.10).

### 18.4.8. Вывод раздела

`[Т]` Legacy-потоки (`main`, `PlayerCarN`, `Skin Preset`) для наших данных
**закрыты**: их пишет игровой Java-код, поля для изображений нет, а
единственный «контейнерный» механизм (blob-таблица) в host не декодируется и
в реальных файлах пуст. Единственный корректный путь — собственное хранилище
OpenSLRR рядом с сохранением.

`[Ф-код]` Прецедент уже есть: `host_game_options.cpp:18-42` пишет
`save\game\openslrr_options` с комментарием «Config has a fixed binary layout,
so a new field there would need a patched Config class». Тот же принцип
применяется к состоянию материалов.

## 18.5. Противоречие §3.3: `Assignment` против per-part `legacy_flat`

### 18.5.1. Что именно доказано B2 (напоминание)

`[Ф-рес]` Ключ Skin Preset — Part Resource. Поверхность — одна канва на
деталь. Отображение «деталь → Paint Slot» не выводится: 283 ресурса дают
однозначную промежуточную ступень, 7 — двух кандидатов, 12 — ни одного; одна
поверхность принадлежит нескольким деталям (22 шасси → 8 SCX); имена групп не
идентификаторы (52 имени, 33 переиспользованы, 3 автогенерированы).

`[Т]` Значит `LegacyFlattenedSurface` **не является** slot-scoped объектом: у
него нет `slot_id` в источнике, и он не может его получить без явного
авторского/пользовательского решения (A2/A3).

### 18.5.2. Четыре варианта

| Вариант | Содержание | Последствия |
|---|---|---|
| **а** | `legacy_flat` — форма `Assignment` (буквально как в §3.3) | требует `slot_id`, которого нет в данных; получить его можно только эвристикой по имени группы (противоречит B2) или автогенерацией слота (противоречит A3) |
| **б** | `legacy_flat` строго per-part, `Assignment` на него не ссылается никогда | нельзя частично мигрировать машину: слот, объявленный автором, не может показать старую канву; «переход — явное действие пользователя» превращается в «всё или ничего» |
| **в** | два владельца: per-part запись + slot-scoped копия пикселей | дублирование изображений, расхождение при правке, неясно, что делать при замене детали, сложный GC |
| **г** | **один владелец — per-part запись; `Assignment` хранит только ссылку** и только после явного маппинга | нет дублирования, нет эвристики, A2/A3 соблюдены, частичная миграция возможна |

### 18.5.3. Решение

`[Т]` Принимается вариант **г**:

- владелец изображения и provenance — **запись детали внутри `CarRecord`**
  конкретного автомобиля (`LegacySurfaceRecord`, §18.8.1); сама запись
  связывается с постоянной идентичностью Part Resource (стабильная идентичность
  пака + локальный id, §18.6.2);
- `MaterialAssignment` (slot-scoped) может содержать **необязательную ссылку**
  `legacy_flat_ref` на уже существующую запись детали — но только если слот
  явно сопоставлен с этой поверхностью (автор объявил слот, пользователь
  подтвердил переход);
- `Assignment` никогда не владеет пикселями и не создаёт вторую копию;
- удаление `Assignment` не удаляет поверхность; удаление детали делает
  `legacy_flat_ref` неразрешённой ссылкой (диагностика), но не удаляет данные
  молча.

`[Т]` Формулировка §3.3 «legacy-форма — ссылка на `LegacyFlattenedSurface`»
верна только в этом смысле: ссылка, а не владение, и только при явном
маппинге. Формулировка исправлена в §3.3 архитектуры (§18.12).

### 18.5.4. Где происходит переключение

`[Т]` Переключение — решение **резолвера состояния при загрузке**, а не
свойство формата и не эвристика рендера:

```
деталь загружена (legacy state.texture + наша запись)
   ├─ есть legacy_flat и нет маппинга → legacy-представление (показ канвы)
   ├─ есть legacy_flat и есть маппинг → слот показывает канву,
   │                                     поверх — новые слои по политике §11.2.1
   └─ нет legacy_flat → обычный путь (Assignment / авторский материал)
```

`[Ф-код]` Сегодня рендер сам решает, какой tex-слот подменить
(`scene_instances.cpp:1825-1848`), потому что другого слоя принятия решений нет.
В целевой модели этот адаптер остаётся механизмом применения уже принятого
решения: резолвер выдаёт «какая текстура/материал у детали», рендер её только
потребляет.

## 18.6. Идентичность и provenance

### 18.6.1. Идентичность содержимого

`[Т]` Изображение адресуется **содержимым**: хэш байтов (например SHA-256) +
тип носителя + размеры. Это даёт дедупликацию, устойчивость к переименованию и
переносимость. Логическое имя (`legacy_flat/<part>/<ordinal>`,
`decal/<decal_id>/source`) хранится рядом как удобство, но ключом не является.

### 18.6.2. Идентичность источника и provenance

`[Т]` Постоянная идентичность ресурса — пара **(стабильная идентичность пака,
локальный id ресурса)**. Идентичность пака — нормализованный относительный
путь пака (тот же, что использует Stage 16: `cars\racers\Einvagen.rpk`), а
**не** runtime `pack_id`.

`[Ф-код]` Почему packed id (`pack_id<<16|local`) не годится как постоянная
ссылка:

- `RpkIndex::open_all` обходит `*.rpk` файловой системой в порядке, который не
  специфицирован (`recursive_directory_iterator`, `rpk_index.cpp:202-228`);
- `open_lib` выдаёт `pack_id` последовательным `next_id_++` (`:231-250`), а
  `next_id_` начинается с 1 в каждом процессе (`rpk_index.hpp:127`);
- паки открываются не только `open_all`: игровые скрипты вызывают
  `System.openLib` и `System.rpkScan` (`natives_io.cpp:368-402`), поэтому
  нумерация зависит и от сценария загрузки, а не только от состава файлов;
- `make_full_id` собирает runtime ID из текущего `pack_id` (`rpk_index.hpp:62`),
  а `resolve` при `pack_id == 0` ищет по всем пакам и берёт **последнее**
  совпадение (`last match wins — debug only`);
- `pack_by_name` после точного пути и попытки добавить `.rpk` сравнивает
  **базовые имена** и возвращает первый пак с таким именем
  (`rpk_index.cpp:263-284`) — два пака с одинаковым именем в разных папках
  неразличимы;
- `paint_one` присваивает `state.texture` переданное значение **без проверки**
  (`part_runtime.cpp:1307-1314`); проверяет только `setTexture`.

`[Т]` Следствие: один и тот же локальный id в другом пакете после смены порядка
обнаружения укажет на другой ресурс, а `paint_one` подмену не заметит. Поэтому
packed id — сессионное значение, а постоянная ссылка — пара (стабильная
идентичность пака, локальный id).

`[Т]` При загрузке пара разрешается в текущий runtime ID через `RpkIndex`. Если
пак не найден, идентичность неоднозначна или локальный id отсутствует — ссылка
**неразрешённая** с диагностикой; подставлять другой пак или локальный id
запрещено.

`[Ф-код]` **Текущие helper’ы небезопасны для постоянных ссылок.**
`rpk_resolve_resource(pack, local)` (`rpk_index.cpp:292-302`) при отсутствии
активного индекса или ненайденном пакете возвращает **просто `local`**, без
pack ID. Дальше срабатывают те же нестрогие механизмы, что перечислены выше:
`RpkIndex::resolve()` видит `pack_id == 0` и ищет локальный id **по всем
пакетам** (последнее совпадение), `pack_by_name()` подбирает пакет по базовому
имени, а `find_entry()` после точного `type_id` ищет по младшим 16 битам. Итог:
отсутствие нужного пакета может молча дать ресурс из чужого пакета (или другую
запись того же пака), а `paint_one()` присвоит это значение без проверки.
Поэтому `rpk_resolve_resource()` и `resolve()` с `pack_id == 0` —
legacy-механика для констант и отладочных путей, **не** контракт persistence.

`[Т]` Контракт будущего загрузчика постоянных ссылок (не реализуется в B4):

1. вход — стабильная идентичность пакета (нормализованный относительный путь)
   и локальный id ресурса;
2. пакет ищется по нормализованному пути, а **не** по basename;
3. пакет отсутствует / локальный id не найден / идентичность пакета
   неоднозначна (в том числе повторная регистрация одного файла под другим
   ключом) → результат «неразрешённая ссылка» + диагностика;
4. запрещены fallback на unscoped local id, поиск по всем пакетам и выбор
   «первого/последнего» совпадения;
5. runtime ID выдаётся только после успешной проверки и пакета, и записи в нём;
6. неразрешённая ссылка не передаётся в `paint_one()` как число: `paint_one`
   присваивает значение без проверки (`part_runtime.cpp:1307-1314`).

`[Ф-код]` Нормализация пути сегодня — только `norm_key()` (замена `/` на `\`
и приведение к нижнему регистру, `rpk_index.cpp:33-38`). Она **не** убирает
`.`, `..`, ведущий `.\`, лишние и хвостовые разделители, не приводит
абсолютный путь к относительному и не нормализует букву диска. `open_all()`
регистрирует паки по `fs::relative(path, root_)`, а `System.openLib` /
`System.rpkScan` передают путь из скрипта (`natives_io.cpp:368-402`). В
stock-скриптах все строки — относительные пути от корня игры с `\`
(`cars\racers\Einvagen.rpk`), поэтому ключи совпадают; это свойство текущего
контента, а не гарантия функции.

`[Т]` Требование к будущей реализации: канонизировать путь (относительно корня
игры, без `.`/`..`, с одним разделителем и без хвостового, регистронезависимо)
и **обнаруживать** повторную регистрацию одного физического пака как
неоднозначность. Сегодня такой проверки нет: `open_lib` дедуплицирует только
по `norm_key` (`:231-238`), поэтому абсолютный путь или путь с `.\` дадут
второй `pack_id` для того же файла.

Provenance (хранится отдельно и от идентичности записи, и от пикселей):

- путь исходного манифеста Skin Preset и ordinal;
- исходное имя файла sidecar и его хэш;
- модель/набор данных (`cars\racers\<X>_data`).

`[Т]` Provenance не используется как ключ хранения: смена пака или
переименование манифеста не должны терять уже импортированную поверхность.

### 18.6.3. Идентичность целевого экземпляра

`[Т]` Три разных ключа для трёх уровней:

| Уровень | Ключ | Почему |
|---|---|---|
| слот | `slot_id` (A2, объявлен моделью) | единственная стабильная идентичность слота; не индекс, не имя группы |
| декаль | `decal_id` (собственная идентичность декали) | декаль не принадлежит слоту и может затрагивать несколько деталей |
| деталь (legacy-канва) | **(CarRecord сохранения, постоянная идентичность Part Resource)** | источник (Skin Preset) сам адресует ресурс, а не экземпляр; paint в legacy «путешествует» вместе с записью детали |

`[Т]` Несколько экземпляров одного Part Resource в одной машине: legacy-формат
не умеет их различать (адресация — ресурс), поэтому legacy-канва у них
**общая**. Это сохраняемое ограничение совместимости, а не новое решение: пока
такой детали не сопоставлен отдельный экземпляр, канва одна на всех, и менять
это поведение B4 не вправе.

`[Т]` Различать установленные экземпляры в будущем сможет **самостоятельная
устойчивая идентичность `instance_id`**, а не порядковый индекс варианта.
Порядковый номер (позиция в списке деталей, `own_slot`, `parent_slot`, индекс
массива) идентичностью не является: он меняется при перестановке, удалении и
переупорядочивании. `instance_id` так же не выводится из `GameRef`, `live_id`,
`type_id` или runtime-адреса. Его схема, тип, область уникальности, момент
назначения и правила сериализации **не определены**; в формат данных B4 он не
вводится. До отдельного решения legacy-поведение (общая канва) сохраняется.

`[Т]` Путь по дереву слотов (`own_slot`/`parent_slot`, `InstalledPart`,
`include/slrr/part_runtime.hpp:11-18`) — **локатор**, а не идентификатор: он
меняется при перестановке деталей. Runtime `GameRef`/`live_id`/`type_id` не
используется как постоянный ID: он зависит от загрузки и сессии (§11.1).

### 18.6.4. Provenance-подсказки

`[Т]` CFG, SCX, имя material group, индекс группы/текстуры, модель — только
подсказки для диагностики и будущего авторского маппинга. В `slot_id` они не
превращаются.

### 18.6.5. Достаточно ли `(пак, локальный id)`

`[Т]` Для **источника** — да: B2 показал, что это однозначная идентичность
детали. Для **слота** — нет (слот объявляется отдельно), для **экземпляра при
дубликатах** — нет (см. выше). Поэтому ключ хранения всегда парный: уровень
владения + идентичность этого уровня; `(пак, id)` — атрибут записи, а не её
ключ.

`[Т]` Изменение конфигурации автомобиля:

- деталь **снята** → запись остаётся в состоянии и не участвует в рендере;
  при повторной установке той же детали поверхность возвращается;
- деталь **заменена** другой (другой Part Resource) → прежняя запись
  становится недостижимой по идентичности и сохраняется как диагностическая,
  а не переносится на новую деталь;
- **шасси/модель** заменены → якорь машины не совпадает, slot-scoped записи
  не разрешаются; part-scoped записи остаются валидными для тех деталей,
  которые всё ещё присутствуют.

`[Т]` Недостающая часть identity, таким образом, — не «ещё один id ресурса», а
**якорь экземпляра**: какая именно машина в карьере. `(пак, id)` его не
заменяет.

`[Ф-код]`/`[Ф-рес]` Устойчивого идентификатора автомобиля в legacy-данных не
найдено:

- `PlayerCarN` — имя файла, то есть позиция машины в списке карьеры: файлов
  ровно столько, сколько машин (`save/carrer/1-1` — `PlayerCar0`, `PlayerCar1`;
  `2-1` — только `PlayerCar0`), поэтому удаление/продажа машины сдвигает
  нумерацию;
- `main` (`SAVEFILEID_MAIN` v21) содержит каталог/лот (десятки названий
  моделей), но не уникальный id конкретной машины игрока;
- хвост `Vehicle` (8 float + 3 int) в проверенных сохранениях полностью нулевой;
- идентичность шасси (`пак + локальный id`) не уникальна — две одинаковые
  машины дают одно значение;
- имя модели не уникально (в лоте повторяется).

`[Т]` Поэтому якорь `CarRecord` остаётся **открытым архитектурным вопросом**
(§18.12.1). До его решения допустима только консервативная политика:
сопоставление по позиции с обязательной проверкой идентичности шасси, при
несовпадении — неразрешённая запись и диагностика, без переназначения и без
удаления данных. Ни `PlayerCarN`, ни позиция машины, ни имя модели стабильной
идентичностью не объявляются.

## 18.7. Варианты хранения изображений

### 18.7.1. A. Встраивание payload в контейнер сохранения

`[Ф-код]` SDAT type 2 структурно позволяет: `[unsaved][ordinal,kind,size,bytes]`.
Но: (1) хост пишет blob только для `0xFFFFxxxx` и **с пустыми байтами**;
(2) чтение не декодирует blob; (3) в реальных файлах blob-записей 0; (4) в
дампе оригинала blob-записи скина имеют `size = 0` — движок пиксели не
встраивал; (5) `File.write` принимает только `int`/`float`/`String`/`ResourceRef`;
строковая запись добавляет длину и NUL, произвольного блока байтов нет.

`[Т]` Вариант требует: собственного механизма записи blob + собственного
декодера + собственного id-пространства для восстановленных текстур; при этом
он использует поле, которое в контенте никогда не использовалось для пикселей.

### 18.7.2. B. Отдельные sidecar-файлы рядом с сохранением

`[Ф-рес]` Совпадает со stock-практикой (`<манифест>.<ordinal>`), но имя —
единственная связь; `File.copy`/`move` оперируют отдельными файлами, а
`File.delete` не умеет удалять «семейство» по шаблону (только конкретный путь
или каталог целиком). Ordinal в legacy-манифесте — номер **сессии записи**
(1..N), а не устойчивая идентичность.

`[Т]` Годится как **импорт** stock-скинов, не как хранилище новой системы.

### 18.7.3. C. Каталог содержимого, принадлежащий сохранению

`[Т]` Состояние ссылается на контент по хэшу; изображения лежат в каталоге
сохранения. Даёт: самодостаточность (копируется каталог), дедупликацию
(одинаковые картинки — одна запись), сборку мусора по достижимости, отсутствие
зависимости от исходного Skin Preset и от legacy-имён.

### 18.7.4. D. Общая библиотека пользовательских изображений профиля

`[Т]` Ломает автономность сохранения: перенос одной карьеры на другой
компьютер теряет изображения; владение и GC становятся глобальными; появляется
скрытая зависимость от общей среды. Для первой версии не годится; допустима
позже как явный «экспорт в библиотеку», а не как хранилище записи.

### 18.7.5. E. Ссылка только на оригинальный Skin Preset/sidecar

`[Т]` Не выдерживает проверки: переименование/удаление скина, перенос карьеры,
замена изображения и изменение относительных путей ломают вид машины молча.
Годится только как источник импорта (однократного чтения).

### 18.7.6. Сравнение

| Критерий | A (embed) | B (sidecar) | C (content dir) | D (shared lib) | E (ref-only) |
|---|---|---|---|---|---|
| Самодостаточность | ✔ один файл | ◐ нужен полный набор | ✔ каталог | ✘ | ✘ |
| Переносимость | ✔ | ✘ имена/шаблон | ✔ копия каталога | ✘ | ✘ |
| Надёжность при частичной записи (с учётом механизма записи) | ◐ зависит от механизма: один контейнер тоже можно писать через временный файл и замену; без этого — trunc | ◐ каждый файл отдельно, набор может быть неполным | ◐ immutable-блобы + индекс последним; требует безопасной замены индекса | ✘ | ✘ |
| Дедупликация | ✘ нет поля | ✘ по имени | ✔ по хэшу | ✔✔ | — |
| Владение | сохранение | сохранение, связь по имени | сохранение, GC по достижимости | неясно | внешнее (игра) |
| Обслуживание (orphans) | ✘ непрозрачный файл | ✘ по именам | ✔ недостижимый хэш | ◐ глобально | невозможно |
| Совместимость со stock | ◐ новое использование blob | ✔ как в stock | ✔ игнор посторонних файлов | — | ✔ |
| Реализационная стоимость | средняя (blob + id-пространство) | низкая, но логика копирования/очистки | средняя (хэш, GC, каталог) | высокая | низкая, но недостаточно |

`[Т]` **Рекомендация: C** как модель хранения содержимого; **A** — допустимая
альтернатива, если потребуется строго один файл сохранения (логическая модель
та же: ссылки на контент + замыкание); **B** и **E** — только импорт
legacy-данных; **D** — не для v1.

`[Т]` Надёжность здесь оценивается по **механизму записи**, а не по форме
хранения: «один файл» сам по себе не хуже и не лучше каталога — и контейнер,
и индекс можно писать через временный файл с последующей заменой. Текущий host
такого механизма не даёт ни для одного варианта (§18.10), поэтому строка
«надёжность» означает «при корректной реализации записи», а не «сейчас».

## 18.8. Источник истины и контракты

### 18.8.1. `LegacyFlattenedSurface`

- `[Т]` Полное изображение — авторитетные данные; provenance хранится отдельно.
- `[Т]` Запись: `content_ref` (хэш), `source` (постоянная идентичность пакета +
  локальный id, манифест, ordinal, исходное имя), `pre_import_texture_ref`
  (постоянная ссылка на текстуру, которая была у детали **непосредственно
  перед импортом**), `display = legacy`.
- `[Т]` `pre_import_texture_ref` — не «заводская текстура» и не гарантия
  заводского вида: на момент импорта у детали могла быть выбрана
  пользовательская текстура, и восстановление вернёт именно её. Значение
  хранится в постоянной форме (идентичность пакета + локальный id); если на
  момент импорта `state.texture` был runtime/unsaved (`0xFFFFxxxx`) или
  неразрешим, ссылка помечается неразрешённой, а не превращается в число.
- `[Т]` Заводская текстура — **отдельное** понятие: это `texture` из CFG детали
  (`PartConfig::texture`, `part_runtime.cpp:1192`). Она принадлежит модели, не
  хранится в записи и может отсутствовать (CFG без объявления `texture`).
- `[Т]` Переименование/удаление исходного скина не уничтожает импортированную
  поверхность: она уже скопирована в хранилище, source — только для диагностики.
- `[Т]` Неразрешённая деталь не получает чужое изображение: нет записи — нет
  подмены.
- `[Т]` Изменённая/отсутствующая геометрия — диагностика, не remap.
- `[Т]` Исходные файлы Skin Preset не изменяются.

### 18.8.2. `PaintjobLayer`

- `[Т]` Изображение — постоянный пользовательский контент (в хранилище по
  хэшу); состояние хранит ссылку, трансформацию, `opacity`, порядок, provenance.
- `[Т]` Смена базовой краски не удаляет слой; удаление слоя не затрагивает
  другие слои и не удаляет контент, на который ссылаются другие записи.

### 18.8.3. `Decal Instance`

- `[Т]` Сохраняется всегда как логическая сущность: `decal_id`, ссылка на
  изображение, одна трансформация, проекция, порядок, видимость, provenance.
- `[Т]` Не заменяется набором per-part запечённых копий.

### 18.8.4. `Surface Contribution` / bake

- `[Т]` Только производный кэш: версия алгоритма, идентичность целевой
  геометрии, параметры источника, правило инвалидации.
- `[Т]` Может быть удалён целиком и пересобран; устаревший не используется
  молча; исходную декаль не заменяет.

### 18.8.5. `ContentAlbedoLayer`

- `[Т]` Остаётся авторским содержимым модели (`DiffuseMixSecond`, B3) и в
  состояние экземпляра не попадает.

## 18.9. Правила Save / Load / Copy / Restore

### 18.9.1. Save

1. `[Т]` Сначала пишется legacy-поток (игровой Java: `main`, `PlayerCarN`) —
   он остаётся якорем карьеры.
2. `[Т]` Затем — собственное хранилище: новые блобы контента (immutable, по
   хэшу), затем индекс состояния. **Требование:** индекс заменяется так, чтобы
   при неудаче оставался прежний рабочий индекс (временный файл + замена).
   Сегодня host такой замены не предоставляет (`File.close` пишет целевой файл
   с `trunc` и не сообщает об ошибке), поэтому это требование к будущей
   реализации, а не существующая возможность (§18.10).
3. `[Т]` Состояние никогда не пишется раньше содержимого: иначе сохранение
   ссылается на отсутствующие пиксели.
4. `[Т]` Неудача записи нашего хранилища не должна удалять ни legacy-сохранение,
   ни предыдущую версию хранилища (правило «не перезаписывать пользовательские
   данные»).

### 18.9.2. Load

1. `[Т]` Читается legacy-состояние (как сейчас) и наше хранилище.
2. `[Т]` Проверяются версия хранилища и якорь машины. Устойчивого
   идентификатора автомобиля в legacy-данных нет (§18.6.5), поэтому до решения
   §18.12.1 действует консервативная политика: сопоставление по позиции с
   обязательной проверкой идентичности шасси; при несовпадении — неразрешённая
   запись и диагностика, без переназначения и без удаления данных.
3. `[Т]` Каждая ссылка на контент разрешается по хэшу; отсутствие — диагностика
   и заглушка (авторский материал), запись сохраняется.
4. `[Т]` Резолвер выбирает представление детали (§18.5.4).

### 18.9.3. Copy

- `[Т]` Копия сохранения = legacy-файлы + каталог хранилища. Ссылки внутри —
  хэши, поэтому копия самодостаточна.
- `[Т]` Копирование одной машины (встроенный `CopyCar`) наше хранилище не
  копирует: новая машина получает авторский материал. Это ограничение
  фиксируется как известное, а не как ошибка данных.

### 18.9.4. Restore

`[Т]` Три разные операции, которые нельзя смешивать:

1. **снять legacy-представление** — убрать `legacy_flat` и `legacy_flat_ref`;
   поверхность снова рисуется по обычному пути;
2. **вернуть состояние до импорта** — `state.texture` =
   разрешённый `pre_import_texture_ref`; это ровно то, что было у детали перед
   импортом, включая возможную пользовательскую текстуру;
3. **вернуть заводской вид** — `state.texture` = CFG `texture` детали;
   возможно только если CFG объявляет `texture`, и это отдельное действие,
   а не следствие снятия скина.

- `[Т]` Все три операции опираются на постоянные ссылки, разрешаемые через
  `RpkIndex`; неразрешённая ссылка — диагностика, без подстановки другого
  ресурса.
- `[Т]` Изображение при этом не обязано удаляться сразу: удаление — задача GC
  после того, как на него не осталось ссылок.

### 18.9.5. Сборка мусора

- `[Т]` Только по достижимости из индексов состояния **внутри одного
  сохранения**; никогда не по времени, не по имени и не глобально.
- `[Т]` Запускается явно (обслуживание) или после успешной записи; при
  сомнительном состоянии (неразрешённые записи) — не запускается вовсе.

## 18.10. Надёжность и частичная запись

`[Ф-код]` Ограничения, которые нельзя обойти логической моделью:

| Ограничение | Следствие для модели |
|---|---|
| `File.close` пишет trunc и не сообщает об ошибке | запись хранилища не может считаться успешной по факту вызова; нужна проверка после (размер/повторное чтение) |
| удаление старого перед записью (stock-скрипты, M5) | наше хранилище не должно зависеть от того, что legacy-файл уже перезаписан |
| нет транзакции между legacy-файлами и хранилищем | допустимо расхождение; лечится проверкой якоря и диагностикой, а не откатом |
| `File.copy`/`exists` не видят незаписанные байты | после неудачной записи проверка «файл есть» бессмысленна; проверять чтением |
| отсутствующий/повреждённый блоб | заглушка + диагностика; запись не удаляется |
| orphans | обнаруживаются сравнением хэшей с индексами; чистка только явная |

`[Т]` Три уровня, которые нельзя смешивать:

| Уровень | Содержание |
|---|---|
| логическое требование | сначала содержимое, затем индекс; старый рабочий индекс не теряется при неудаче; частичный набор блобов невидим без индекса; индекс без блобов диагностируется по каждому хэшу |
| текущее состояние host | `File.close` пишет целевой файл с `trunc`, без временного файла и замены, возвращает `void`; при сбое байты остаются только в памяти процесса (`g_unflushed_saves`), а `copy`/`move`/`exists` их не видят |
| будущая реализация | безопасная запись и замена индекса, проверка результата после записи, согласование с legacy-потоком; в B4 не проектируется |

`[Т]` Поэтому «атомарная замена» в §18.9.1 — требование к реализации, а не
описание существующего механизма. Известная проблема `File.close` (M5) не
исправляется и не обходится логической моделью.

## 18.11. Рекомендуемая модель

`[П]` Итоговая схема (логическая, без байтового формата):

```
сохранение (карьера, каталог save/carrer/<слот>/)
├── legacy-потоки: main, PlayerCar0..N        (пишет игровой Java, не трогаем)
└── хранилище OpenSLRR (собственный каталог)
    ├── index: CarRecord[]                     (состояние, версия, якорь)
    │   └── CarRecord
    │       ├── anchor: позиция + chassis identity + model name
    │       │            (нестабильный, §18.12.1)
    │       ├── SlotState[slot_id]
    │       │   ├── Assignment: paint_ref | legacy_flat_ref (ссылка!)
    │       │   └── PaintjobLayer[]: content_ref + transform + params
    │       ├── DecalInstance[]: decal_id + content_ref + transform + proj
    │       └── PartSurface[стабильная идентичность пака + локальный id]
    │           └── LegacySurfaceRecord: content_ref + source + pre_import_ref
    ├── content/<hash>                          (immutable, дедуплицируется)
    └── cache/<surface>/<version>               (bake, можно удалить целиком)
```

`[Т]` Что где:

| Данные | Где | Ссылка |
|---|---|---|
| `MaterialAssignment` | `index` (slot-scoped) | `slot_id` + постоянная ссылка на библиотечный Paint Definition |
| `LegacyFlattenedSurface` (пиксели) | `content/<hash>` | `content_ref` из `PartSurface` |
| `LegacyFlattenedSurface` (provenance) | `index` (part-scoped, внутри `CarRecord`) | рядом с `content_ref`, отдельно от пикселей и от ключа |
| `PaintjobLayer` (изображение) | `content/<hash>` | `content_ref` в `SlotState` |
| `Decal Instance` (изображение) | `content/<hash>` | `content_ref` в `DecalInstance` |
| `Surface Contribution` / bake | `cache/<…>` | пересчитывается, версия обязательна |
| `ContentAlbedoLayer` | SCX модели | не в хранилище |

`[Т]` Что система обязана хранить автономно: изображения legacy-канвы,
paintjob и декалей, их параметры и provenance. Что можно получить заново:
`Surface Contribution`, bake, GPU-текстуры, разрешение ссылок на паки. Что
можно ссылать извне: библиотечные Paint Definition (A1) и авторские материалы
модели. Что нельзя адресовать: runtime ID (включая `pack_id<<16|local`),
`PlayerCarN`, индекс группы, индекс текстуры, `GII_TYPE`, имя группы,
позицию в дереве деталей.

## 18.12. Итог B4

`[Т]` **Логическая модель завершена; B4 не объявляется закрытым без оговорок.**
Установлено:

1. Legacy-потоки не расширяемы: сериализатор — игровой Java, поля для
   изображений нет, blob-таблица SDAT в host не декодируется и в контенте
   пуста (§18.4).
2. `LegacyFlattenedSurface` — **part-scoped** запись внутри `CarRecord`;
   `Assignment` может ссылаться на неё только после явного маппинга; одна копия
   пикселей, один владелец; постоянная идентичность ресурса — стабильная
   идентичность пака + локальный id, а не packed id (§18.5–18.6).
3. Контент адресуется содержимым (хэш), а не legacy-именем, ordinal или
   runtime ID (§18.6).
4. Рекомендуемая модель хранения — каталог содержимого внутри сохранения;
   встраивание в контейнер допустимо как альтернатива с той же логической
   моделью; sidecar и ссылка на исходный скин — только импорт (§18.7).
5. Порядок записи «контент → индекс», неизменяемые блобы и GC по достижимости
   задают требования к восстановимости; сам механизм безопасной записи —
   будущая работа, а не существующая возможность host (§18.9–18.10).

Осталось открытым. Вопросы разнесены по природе, а не по удобству отчёта.

### 18.12.1. A. Архитектурные решения, необходимые до реализации

- `[?]` **Идентичность автомобиля (`CarRecord`).** Устойчивого идентификатора в
  legacy-данных нет (§18.6.5): `PlayerCarN` — позиция, `main` не содержит id
  машины игрока, хвост `Vehicle` нулевой, ни шасси, ни имя модели не уникальны.
  Нужно решение: либо принять позиционную привязку с обязательной проверкой
  идентичности шасси и диагностикой, либо ввести собственный постоянный
  идентификатор автомобиля — не runtime ID и не позицию. Пока решения нет,
  действует только консервативная политика без переназначения.
- `[?]` **Поведение при перестановке машин, смене `PlayerCarN`, копировании
  автомобиля и смене шасси:** какие записи становятся неразрешёнными, что
  происходит с пикселями, как это сообщается пользователю.
- `[?]` **Границы идентичности установленной детали при дубликатах.** Legacy
  Skin Preset не различает два экземпляра одного Part Resource, поэтому общая
  legacy-канва — совместимое поведение и сохраняется до отдельного решения
  (§18.6.3). Для различения в будущем нужен самостоятельный устойчивый
  `instance_id`; он не выводится из порядка деталей, `own_slot`/`parent_slot`,
  индекса массива, `GameRef`, `live_id`, `type_id` или runtime-адреса. Схема
  `instance_id` (тип, уникальность, момент назначения, сериализация) не
  определена; порядковый «индекс варианта» эту задачу не решает.

### 18.12.2. B. Реализационные задачи (на будущее)

- `[?]` физический выбор между каталогом содержимого (C) и встраиванием (A);
- `[?]` момент вызова собственного Save/Load относительно `GameLogic.save`
  (хук в host: `File.close` целевого пути или отдельный триггер);
- `[?]` передача изображения из хранилища в рендер: сегодня `paint_texture`
  разрешается только из id пака (`scene_instances.cpp:1857`,
  `rpk_index.cpp:467`), поэтому потребуется id-пространство экземпляра
  (класс 2 из §9.3) или путь-канал;
- `[?]` конкретная схема индекса и формат хранения;
- `[?]` безопасная реализация записи и замены индекса с проверкой результата
  (§18.10);
- `[?]` GC: интерфейс и момент запуска. Для v1 поведение консервативное:
  автоматического удаления нет, пока достижимость и целостность индекса не
  доказаны; ручная очистка остаётся возможной будущей функцией.

## 18.13. C. Неизвестные детали оригинала (не блокируют логическую модель)

- `[?]` Поведение оригинала при повторном `saveSkin` в тот же путь (перезапись
  sidecar или новый набор файлов) — по дампу не восстановимо.
- `[?]` Что делает оригинальный движок с blob-записью, если `size > 0`
  (декодирует как ресурс или игнорирует) — не проверено; хост такие байты
  сохраняет, но не читает.
- `[?]` Взаимодействие `CopyCar` и скинов в оригинале (переносится ли
  поверхность при копировании машины) — не исследовано.
- `[?]` Повторный импорт скина поверх уже импортированного: сохранять первый
  `pre_import_texture_ref` или обновлять его значением на момент нового
  импорта — не установлено (наблюдался один импорт).
- `[Ф-рес]` Пользовательский `Mazda paintable.png` в рабочем дереве не
  затрагивался; B4 его не читает и не изменяет (сравнение stock-версии
  зафиксировано в Stage 17).

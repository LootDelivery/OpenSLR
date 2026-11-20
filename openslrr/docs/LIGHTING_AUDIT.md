# LIGHTING AUDIT

> **Revision note (L1, HEAD `ee1ad36`).** The file has three layers and they are
> kept apart on purpose.
>
> * `§0` is the **historical** audit, written at `488144a`..`994b13e` and once
>   re-checked at `42d795e`. Its text is left as it was written. Where L1
>   measured something different, a `> **L1.n:**` note follows the claim. Do
>   not cite `§0` as current without that note.
> * `§L1.1`..`§L1.13` are the L1 refresh, measured against **HEAD `ee1ad36`**
>   (`B4.1.2`) and against the resources shipped in the working tree.
> * Nothing here was implemented and nothing outside this documentation changed:
>   no C++, no Java, no shader, no SCX, no RPK, no config, no save.
>
> L1 also withdraws the old closing claim that Point/Spot "need somewhere to
> live in the extended SCX" — see `§L1.12` and `RENDER_DATA_CONTRACT.md` §14.

## 0. Historical audit (part 1 of the lighting design)

> Written at `488144a`..`994b13e` (local work, not on `origin`), re-checked at
> `42d795e`. Kept verbatim; corrections are inserted inline.

**Lighting audit — current state**

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

> **L1.2 (correction):** on a street map the file's *direction* is not used
> either. `scene_sun_apply_day_cycle()` replaces the direction with the time of
> day and rescales the colours. The colours come from the file and are then
> multiplied by that cycle; `setLight` overwrites colours only in the menu and
> showroom scenes that actually call it (4 call sites in shipped scripts).

**One local light** — `update_local_light()`:
- gated on `garage_` and `host_enhanced_graphics().local_lights`;
- scans `gameref_directional_lights()` for a node of type `misc.garage:0x001D`;
- hardcodes an emitter 3.3 units above the node, intensity 8, radius 8;
- takes the **first** match and stops.

So the garage is lit by one hardcoded point above one hardcoded node. That is
why changing the garage lighting has meant patching this one function.

> **L1.4 (correction):** `0x001D` is a `type directional` resource in
> `misc/garage.rpk`, not a point light, and the fourth component in the shader
> is a **radius**, not an intensity. The host reads no intensity from the file
> at all: the look is the node position, `+3.3`, radius `8`, a hardcoded warm
> tint and a `×3` gain inside the fragment shader.

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

> **L1.3 (correction):** they are not "loaded into `g_lights` and filtered
> out". `apply_directional_light()` returns at its
> `if (!directional || !have_d) return;` line, **before**
> `gameref_register_light()`, so a Point or Spot node never enters `g_lights`
> and never gets `directional_light.set`. The accessor's filter is a second
> line of defence, not the first. The inventory (`§L1.6`) is 15 directional,
> 4 spot, 2 point and 25 `fake*` resources — the `fake*` family was not known
> to this section at all.

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

> **L1.9 (correction):** three of the four are live — the static world map, the
> garage map (an orthographic top-down pass built from the local-light
> position) and the car depth map. The fourth is not: `draw_vehicle_shadows()`,
> which owns the per-car projected maps and the `kocsiarnyek` decal, is
> compiled but has **no call site**. "One sun frustum" is also too narrow: the
> three live passes have three different frustums. What they share is that
> none of them casts from a Point or Spot light.

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

> **L1.6, L1.4, L1.5:** the first three are answered. Point: 2 resources;
> Spot: 4; Directional: 15; plus 25 `fake*` definitions — 46 filled resources
> out of 49 TOC records (`§L1.6.1`). The spot
> shape is authored (`theta`/`phi`). The garage look is offset `+3.3`, radius
> `8`, a hardcoded warm tint and a `×3` gain — no intensity is read from the
> file. The fourth is only partly answered: `light_flags` selects lamp geometry
> **and** makes its material unlit-style; `lit_dynamic` is parsed by the original
> executable but read by no script and no host code.

---

# Game data scan

> Part of `§0`, written at `488144a`..`994b13e`. Counts and corrections are in
> `§L1.6`; the sample texts below were re-read against the shipped packs and are
> accurate.

Read-only scan of `Street Legal Racing - Redline` resources.

## Confirmed by game resources

All three light types exist in the shipped data. The format is D3D-style
(`D3DLIGHT9`), text, and far richer than what the backend consumes.

> **L1.6:** all three exist, but they are a minority. Of the **46 filled** light
> resources (`typeof 13`; there are 49 TOC records, 3 of them empty
> placeholders) across **82** packs, 15 are directional, 4 spot, 2 point and
> **25 are `fakespot`/`fakepoint` variants**. The `Where they live` table below is a map of where to look,
> not a count, and it misses that `cars/racers.rpk` also holds five
> `fakespot` definitions.

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

> **L1.3, L1.6.3, L1.7 (correction):** "already parsed for the directional
> case" is exactly the gap. Only `type`, `diffuse`, `ambient`, `specular` and
> `direction` are parsed, and only for directional resources; `range`,
> `attenuation`, `theta`, `phi`, `lit_dynamic`, `attH/attV_texture` and the
> whole `lf_*` block are read past. Worse, **no packed config links a light to
> a car part** (`§L1.6.3`), so "an accessor and a shader loop" is not the
> remaining work; the link and the numeric semantics are.

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

> **L1.7:** still the target, and still only the target. What is measured now:
> placement space is metres and SCX vertices are centimetres (`§L1.7.2`);
> `range` spans 2..10 000 with no confirmed unit (`§L1.7.3`); `attenuation` is
> a D3D9-style triple whose formula is confirmed nowhere in our code
> (`§L1.7.4`); `theta`/`phi` are radians that only make sense as full cone
> angles (`§L1.7.5`).

### Reflection capture

Dynamic lights and their states must be accounted for identically in the main
render and in the reflection capture.

### Storage of the new materials

Material storage, parser and exporter requirements are in
`SCX_MATERIAL_FORMAT.md`.

> **L1.12 (corrected, the old sentence is withdrawn):** "Point/Spot also need
> somewhere to live in the extended SCX" is wrong.
> `RENDER_DATA_CONTRACT.md` §14 keeps light definitions in the game resource
> system and object hierarchy: a `LightDefinition` is loaded from an RPK, a
> `LightInstance` is attached to a scene node, and the SCX exporter must not
> add light definitions. What a future editor needs is an exporter for light
> **resources** plus their hierarchy links — a different task, and not a reason
> to move light definitions into SCX.

---

## L1.1. Метод и охват

`[Т]` Всё ниже измерено на **HEAD `ee1ad36`** (`B4.1.2`). Код, шейдеры и
ресурсы не менялись; L1 — только чтение и документация.

Что именно проверено:

- **Ресурсы.** Просканированы все **82** `.rpk` рабочего дерева — **11 887**
  записей TOC. Запись читается по тем же полям, что и в
  `RpkIndex::parse_rpak` (`type_id`, `typeof`, `offset`, `size`, alias), payload
  берётся по `offset/size` без разбора. Ресурс света — запись с байтом
  `typeof == 13`; вид света — первый токен после `type` в тексте payload.
  Дополнительно все текстовые payload'ы просмотрены на строки
  `render <id>` / `light <id>`, чтобы найти ссылки из конфигов.
- **Параметры.** Для каждого ресурса собраны встречаемость полей, значения и
  распределения (`range`, `theta`, `phi`, `attenuation`, цвета, флаги,
  текстурные ссылки).
- **Оригинальный движок.** В `StreetLegal_Redline.exe` найдена таблица
  ключевых слов парсера света (см. L1.7.1). Это строки и форматы `sscanf`,
  а не декомпиляция логики.
- **Код OpenSLRR.** `natives_renderref.cpp`, `natives_gameref.cpp`,
  `gameref.cpp`, `scene_instances.cpp`, `scene_sun.cpp`, `bgfx_lighting.cpp`,
  `bgfx_shadows.cpp`, `bgfx_lib_backend.cpp`, `part_runtime.cpp`,
  `part_config.cpp`, `scx_mesh.cpp`, `fs_mesh.sc`, `fs_vehicle.sc`,
  `system/Scripts/util/resource/RenderRef.class`.
- **Документы.** `RENDER_DATA_CONTRACT.md` (§14), `AUTOMOTIVE_RENDERING.md`,
  `RENDERING_VALIDATION.md`, `PBR_PLAN.md`.

Чего в L1 **нет**:

- запуска игры: категория `[Ф-тест]` отсутствует полностью, ни одно поведение
  не подтверждено кадром;
- декомпиляции `.exe`: из него взята только таблица ключевых слов и строки;
- анализа карт размещения (сколько экземпляров фонарей стоит в городе) —
  это требует либо runtime-лога, либо отдельного прохода по object placement.

`[Т]` Оговорка о происхождении контента. Установка — это не один релиз:
рядом лежат пакеты эпохи 2003–2006 (`system.rpk`, `objects.rpk`, `maps/*`,
`cars/racers.rpk`), аддон-паки (`MW_Mod.rpk`, `DestructionTools.rpk`) и
пользовательский контент (`cars/racers/Maz3_2005.rpk` — 2018,
`maps/Addon_Maps/*`). Даты и стиль текста (например, дописанные вручную
комментарии `lf_texture 0x00030100 (maps\skydome\lensflare\Flares.ptx)` в
`DestructionTools.rpk`) позволяют их разделить, но это **не доказательство**.
Поэтому в L1.6 количества даны по пакетам: любой пакет можно исключить и
пересчитать.

## L1.2. Directional: фактическая цепочка

`[Ф-код]` Последовательность на HEAD, звено за звеном.

**1. Создание узла и разбор ресурса.**
`RenderRef.create(ResourceRef parent, RenderRef type, String alias)`
(`natives_renderref.cpp`, native `create`) делает
`gameref_upsert(self, RenderRef)`, копирует `type_id` из `type`, ставит
родителя (`gameref_set_parent`) и затем вызывает
`apply_directional_light(rpk, st->type_id, type, *st)` (`:100`).

`apply_directional_light` читает payload ресурса как **текст** и разбирает
построчно:

- `type ... directional` → флаг `directional`;
- `diffuse a r g b`, `ambient a r g b`, `specular a r g b` — по четыре float,
  **первый компонент отбрасывается** (в `light.diffuse` идут r,g,b);
- `direction x y z`.

Затем `if (!directional || !have_d) return;` (`:150`). То есть ресурс без
строки `type ... directional` **или** без `diffuse` не даёт вообще ничего:
ни `set`, ни регистрации, ни `live_id`.

**2. Запись состояния.** `directional_light.set = true`, `has_direction` =
`|d|² > 1e-8`. Цвета по умолчанию: `ambient = 0`, если строки нет;
`specular = 1`, если строки нет (sic); `direction = 0`, если строки нет.
`flare` копируется с объекта `type` (`:153`) — но только сюда, то есть только
у directional.

**3. `live_id`.** `gameref_make_live()` вызывается, только если у узла есть
родитель (`:159`). `live_id` — счётчик `g_next_live` от `0x70000000`
(`gameref.cpp:27`), выдаётся по порядку создания
(`gameref.cpp:98-102`), **монотонно** и без повторного использования
освобождённых номеров: цикл `while (gameref_find_live(g_next_live)) ++g_next_live;`
пропускает только уже занятые значения, после чего номер выдаётся и счётчик
увеличивается. Проверка занятости защищает от совпадения с существующим
`live_id`, но переиспользованием удалённых номеров не является. К
`0x70000000` счётчик возвращается только при полном сбросе реестра
(`gameref.cpp:64`).

**4. Регистрация.** `gameref_register_light(key)` кладёт ключ в
`g_lights` — `std::unordered_set<void*>` (`gameref.cpp:26`, `:1002`).

**5. Accessor.** `gameref_directional_lights()` (`:1005`) отбирает
`kind == RenderRef && !empty && type_id && directional_light.set`. Порядок
обхода — порядок `unordered_set`, то есть не определён.

**6. Выбор.** `scene_instances_light(root)` (`scene_instances.cpp:1723`):

```cpp
SceneSun selected = scene_sun();          // глобальный, мог быть задан setLight
int newest = 0;
for (auto* node : gameref_directional_lights()) {
  if (!node->directional_light.set || node->live_id <= newest) continue;
  float frame[16];
  if (!chain_matrix(node->key, root, frame, false)) continue;
  selected = node->directional_light;
  newest = node->live_id;
  for (int i = 0; i < 3; ++i)
    selected.direction[i] = frame[i]*d[0] + frame[4+i]*d[1] + frame[8+i]*d[2];
}
```

Отсюда точные правила:

- берётся **самый новый по `live_id`** из применимых, а не первый в файле и
  не ближайший к камере; `live_id` — порядок создания узла скриптом;
- «применимый» = `chain_matrix(node, root)` дошёл до `root`: узел жив, не
  `kWorldTreeLeaf`, по пути нет чужого `kWorldTreeRoot` (меши инвентаря не
  протекают в гараж);
- если ни один узел не применим, остаётся глобальный `scene_sun()`;
- `selected` — копия: цвета и направление узла, flare тоже (структура
  `SceneSun` содержит `Flare`).

**7. Мировое направление.** `frame` — матрица мира в скриптовом пространстве,
колонки `frame[0..2]`, `frame[4..6]`, `frame[8..10]` — оси X, Y, Z. Строка
выше — умножение `R · d`, то есть корректный поворот локального направления в
мир. Никакого учёта `position` из ресурса нет: у directional `position`
практически всегда `0 0 0` (L1.6.4).

**8. Ежедневный цикл.** `scene_sun_apply_day_cycle()` вызывается в двух
местах:

- `scene_instances_light()` — если `root == g_world` и в мире есть ground
  props (`scene_instances.cpp:1735`), то есть на любой уличной карте;
- `draw_sky()` — если `!outdoor_lighting_` (`bgfx_lib_backend.cpp:2207`), то
  есть только для неба в гараже/меню.

Цикл **перезаписывает направление полностью**:

```
phase = t/86400*2π − π/2;  elevation = sin(phase)
direction = (−cos(phase), −0.85·elevation, 0.5268·elevation)
```

и масштабирует цвета: `daylight`/`twilight` рампа, ночной ambient-пол
`(0.10, 0.13, 0.20)`, тёплый оттенок `(1, 0.66, 0.44)`, gain до 2.8 по
яркости файла. `set` и `has_direction` остаются true.

Следствие: **на улице направление из ресурса карты не используется вообще**, а
цвета файла участвуют только как база под множители цикла.

**9. В шейдер.** `collect_root()` кладёт полученный `SceneSun` в
`instance.lighting` каждого `SceneInstance` (`scene_instances.cpp:1783`).
`draw_instance()` для автомобильных материалов выставляет `u_sunDiff`,
`u_sunAmb`, `u_sunSpec`, `u_sunDir` из `instance.lighting` **всегда**.
`bind_baked_light()` (мир) выставляет их только при `scene_light`, а
направление конвертирует как `{-d0, -d1, +d2, has_direction}` — «к свету» плюс
отражение Z. `fs_mesh.sc` при `u_sunDir.w < 0.5` уходит на статическую
формулу `light = 0.65+0.35·ndl` (или `ndl`), то есть **без источника**.

**10. `RenderRef.setLight`.** Native зарегистрирован как `(III)V`
(`natives_renderref.cpp:532`) и вызывается из игровых скриптов: `CarSelect`,
`DriverSelect`, `MW_Mod/DealerTrack` (4 места вызова в байткоде). Он пишет
`_sun_d/_sun_a/_sun_s` в объект **и** применяет `scene_sun_apply_packed()` к
своему узлу + регистрирует его. Класс `RenderRef` объявляет ещё
`setLight(ResourceRef,int,float,float,int,int)` и `setFlare(ResourceRef)`, но
**ни один скрипт их не вызывает**; native `setFlare` в host зарегистрирован с
сигнатурой `(ResourceRef;IFFII)V`, которая не совпадает с объявленной
`(ResourceRef)V`, то есть недостижим.

**11. Приоритет источников (итог).**

| Сцена | Что даёт цвет | Что даёт направление |
|---|---|---|
| Уличная карта | файл карты × дневной цикл | дневной цикл (файл игнорируется) |
| Гараж / салон / лот | файл сцены; `setLight` может переписать при создании | файл сцены (без цикла) |
| Меню / заставка | `setLight` из `CarSelect`/`DriverSelect`, иначе файл | файл |
| Нет ни одного узла | глобальный `scene_sun()` | `has_direction = false` → формула-заглушка |

**12. Флаги графики.** Влияют на *источник* только в одном месте:
`dynamic_lighting` («Map sunlight») запрещает выставлять sun-uniform'ы для
**мировых мешей**; автомобили получают их всегда. `local_lights` гасит
локальный свет (и, как следствие, гаражную тень). `map_shadows` отключает
получение статической тени. `flares` — только солнечный flare.
`dynamic_reflections` — захват куба. То есть «выключить свет» одной галочкой
нельзя: это разные подсистемы.

## L1.3. Point и Spot: фактическое состояние

`[Ф-код]` По восьми вопросам из задания:

| Вопрос | Ответ на HEAD |
|---|---|
| Разбираются как отдельные типы? | **Нет.** Один текстовый парсер, и он выходит на `!directional`. |
| Самостоятельные runtime-структуры? | **Нет.** Есть только `SceneSun directional_light` и `Flare` внутри `GameRefState`. |
| Попадают в `g_lights`? | **Нет.** `gameref_register_light()` стоит *после* раннего `return`. |
| Accessor? | Только `gameref_directional_lights()`; `gameref_point_lights()` не существует. |
| Попадают в `SceneInstance`? | **Нет.** В `SceneInstance` одно поле `lighting` типа `SceneSun`. |
| Получают world transform? | **Нет.** Для directional трансформируется только направление; позиция света не вычисляется нигде. |
| Доходят до `fs_mesh`/`fs_vehicle`? | **Нет.** В шейдерах нет ни массива источников, ни uniform'ов point/spot. |
| Reflection Capture? | **Нет.** Захват получает тот же один directional. |
| Тени? | **Нет.** Ни point-, ни spot-тени не строятся. |

`[Т]` Три уровня, которые нельзя смешивать:

1. **ресурс в RPK** — есть: 4 spot и 2 point (плюс 25 `fake*`), L1.6;
   автомобильный контекст по данным подтверждён для трёх из четырёх spot;
2. **узел RenderRef в дереве** — создаётся скриптами; для directional
   подтверждён кодом, для point/spot — нет: в упакованных конфигах на них
   **нет ни одной ссылки** (L1.6.3);
3. **рабочий источник в renderer** — отсутствует: ни структуры, ни accessor'а,
   ни uniform'а, ни цикла.

`[Ф-код]` Отдельно: `type fakespot` / `fakepoint` host не знает вообще — они
не проходят даже проверку `directional`. При этом оригинальный движок эти
токены знает (L1.7.1), то есть это часть формата, а не мусор.

## L1.4. Гаражный локальный свет

`[Ф-код]` `update_local_light()` (`bgfx_lib_backend.cpp:1782`), вызов — один
раз за кадр из основного прохода, перед тенями и захватом отражений
(`:1241`).

```
local_light_ = {};
if (!garage_ || !host_enhanced_graphics().local_lights) return;
for (const auto* node : gameref_directional_lights()) {
  float matrix[16];
  if ((node->type_id & 0xffff) != 0x001d ||
      !scene_instances_script_matrix(node->key, matrix)) continue;
  local_light_ = {matrix[12], matrix[13] + 3.3f, -matrix[14], 8.f};
  break;
}
```

- **Условия:** только гараж, только при включённых `local_lights`, только если
  найден узел и у него есть мировая матрица.
- **Поиск:** идёт по `gameref_directional_lights()`, то есть по тому же
  реестру, что и солнце; фильтр — младшие 16 бит `type_id == 0x001D`; берётся
  **первый** найденный (порядок `unordered_set` не определён, но узлов с таким
  id в гараже один).
- **Позиция:** `matrix[12..14]` — трансляция скриптовой мировой матрицы;
  `+3.3` по Y — подъём до уровня светильника; `-matrix[14]` — переход в
  рендер-пространство (вершины грузятся как `(x, y, −z)`).
- **Четвёртый компонент — радиус, не интенсивность.** В шейдере это
  `u_localLight.w`, и он используется только как делитель и как выключатель
  (`w > 0`). Значения `8.0` из ресурса не читаются: ресурс `misc/garage.rpk`
  `0x001D` — это `type directional` с `position 0 3 0`, `range 5000`,
  `diffuse 1.000 0.919 0.958 1.000`.

`[Ф-код]` Формула (`fs_mesh.sc:269`, `fs_vehicle.sc:85`):

```
delta = lightPos − worldPos            (fs_mesh, world space)
delta = V·lightPos − viewPos           (fs_vehicle, view space)
d2    = dot(delta, delta)
fade  = saturate(1 − d2 / r²)
face  = max(dot(n, normalize(delta)), 0)
term  = (1.0, 0.94, 0.80) · fade² · face · 3.0 / (1 + d2)
```

Различия ветвей:

- `fs_mesh`: термин умножается на `mix(0.35, 1.0, carVisibility)` — локальный
  свет **затеняется** картой глубины автомобиля; сам термин прибавляется к
  `illumination`, которая при наличии sun заменяется на ambient+diffuse, а
  локальный свет прибавляется уже после.
- `fs_vehicle`: теней нет вовсе, термин прибавляется к `illum` без множителя.

Кто получает:

| Путь | Локальный свет |
|---|---|
| Автомобильные материалы (`draw_instance`, `vehicle_material`) | да, если не `simple` (preview) |
| Мировые меши (`draw_instance`, `world_mesh`) | да, `bgfx_lighting.cpp:420` |
| Инстансные пропы (`draw_prop_batches`) | да, но только при `view_id < 72` |
| Небо | нет (`bind_baked_light` ставит ноль) |
| OSD / flare / тени | нет |

`[Ф-код]` Почему для поиска используется `gameref_directional_lights()`:
другого реестра с данными света нет, а `0x001D` — это **directional**-ресурс
гаража, чей узел скрипт поставил под потолок. Host берёт из него только
трансляцию узла и игнорирует все поля света. Это не «Point Light из
ресурсов», а синтетический источник: позиция узла + хардкод `+3.3`, радиус `8`,
тёплый цвет и `×3` в шейдере.

`[Ф-код]` Гаражная тень использует ту же точку: `update_static_shadow()` при
`garage_ && local_light_[3] > 0` строит **орто**-карту 20×20 м, смотрящую из
`local_light_` строго вниз, и отбрасывает в неё только кузовные меши
(`vehicle_material`, без `light_flags`). Это не «тень от point light»: нет
куба, нет six-face, нет перспективы.

## L1.5. `part_lamp_lit`, `light_flags`, `lit_dynamic`

`[Ф-код]` Происхождение `light_flags`:

- CFG детали, строка `render <resource> ... flags <n>` →
  `PartConfig::renders[].flags` (`part_config.cpp:332`);
- `render_part()` вызывает `add(..., r.flags)` только для записей с
  `(flags & 0x13f) != 0` и без бита `0x40` (`part_runtime.cpp:487-489`);
- `add()` кладёт значение в `GameRefState::light_flags` (`:454`);
- `collect_root()` копирует его в `SceneInstance::light_flags` (`:1823`).

`[Ф-код]` `part_lamp_lit(part, flags, preview)` (`part_runtime.cpp:1128`):

```cpp
if (!(flags & 4u) || (flags & 0x40u) || !runtime) return false;
if (!preview) { если двигатель не running → false }
t = GameLogic.time mod 86400;
return t < 21600 || t >= 75600;      // ночь: до 06:00 и с 21:00
```

`[Ф-код]` Точная область действия `preview`: флаг обходит **только** проверку
работающего двигателя. Проверка времени стоит вне `if (!preview)` и в preview
выполняется так же, как в игре — то есть и в гараже ламповый меш
показывается/скрывается по расписанию, а не постоянно.

`[Ф-код]` Сам флаг приходит из `scene_instances_select(world, skydome,
preview_lamps)` → `g_preview_lamps` (`scene_instances.cpp:776-780`) и
передаётся в `part_lamp_lit()` как `root == g_world && g_preview_lamps`
(`:1790`), то есть действует только для мирового корня.

`[Т]` Почему это важно: «preview показывает все лампы всегда» — неверно.
Гаражный просмотр отличается от игры только тем, что лампа горит при
заглушённом двигателе; расписание суток сохраняется.

`[Ф-код]` Что функция проверяет по битам: **только бит `4`**, плюс отбрасывание
`0x40`, наличие runtime, состояние двигателя (кроме preview) и время суток.

Комментарий в `gameref.hpp:181-183` перечисляет четыре бита — `1` brake,
`2` reverse, `4` city, `0x10` indicator — но это описание **назначения битов
в формате CFG**, а не поведение `part_lamp_lit()`. Записи с `0x13f` и без
`0x40` попадают в эту функцию все, но решение «гореть» принимается для них
одинаково и только по биту `4`. Отдельных путей отображения brake / reverse /
indicator в просмотренном runtime-коде нет; утверждать, что эти биты
реализуют включение соответствующих огней, нельзя.

`[Ф-код]` Что флаг делает дальше — **две независимые вещи**:

1. **Видимость геометрии.** `collect_root()` пропускает узел, если
   `light_flags` стоит, а `part_lamp_lit()` вернул false (`:1789`). Отдельный
   ламповый меш показывается, только когда функция вернула true.
2. **Режим материала.** `SceneInstance::light_flags != 0` → в `draw_instance()`
   `bake_u[0] = 1` → `u_lightmap.x = 1` → в шейдере `use = 1`.

`[Ф-код]` Что именно делает `use = 1` — это **не** emissive-материал и не
«свет выключен полностью»:

- `fs_mesh.sc`: `light = mix(light, 1.0, use)` — базовая N·L-составляющая
  заменяется единицей, `bake = mix(1, bake, use)`, а ветка
  `if (u_sunDir.w > 0.5 && use < 0.5)` **не выполняется**, то есть sun и
  ambient не применяются. Итог — `illumination = 1 · visibility · mix(0.55, 1,
  carVisibility)`, и уже **после** этого прибавляется `u_localLight`, если он
  включён. Статическая тень в этой ветке не считается (её условие тоже
  `use < 0.5`), но контактная тень через `carVisibility` остаётся.
- `fs_vehicle.sc`: `illum = mix(illum, vec3(1), use)`, `bake = mix(1, bake,
  use)`, затем **прибавляется** `u_localLight`; спекуляр гасится множителем
  `(1.0 - use)`.
- ни в одном шейдере `use` не добавляет собственного излучения, свечения или
  bloom: цвет остаётся `albedo × 1` (плюс локальная подсветка).

`[Т]` Корректная формулировка — **unlit-style**: без обычных sun и bake
составляющих, с сохранением локальной подсветки. «Emissive-материал» и
«поверхность вообще без освещения» — неверно в обе стороны.

`[Ф-код]` `lit_dynamic`:

- в C++ host и в шейдерах **нет ни одного упоминания**; ни один `.class` игры
  и ни один рыхлый конфиг его тоже не содержит;
- строка есть только в `StreetLegal_Redline.exe`, в таблице парсера света,
  формат `%s %d` — то есть оригинальный движок читает его как **целое**;
- `[Ф-рес]` распределение по видам: spot 3/4, fakespot 16/19, fakepoint 3/5,
  `type fake point` 0/1, directional 0/15, point 0/2. Значение всегда
  `1.000`.

`[Т]` Пересчёт, чтобы числа не путались:

- spot + fakespot = **23** ресурса, `lit_dynamic` есть у **19** из них; нет у
  `objects.rpk:0x1c6`, `particles.rpk:0x20`, `0x21`, `0x22`;
- если добавить fakepoint и `fake point`, семейство = **29**, поле есть у
  **22** — те же 4 плюс `objects.rpk:0x16`, `0x1ce`, `particles.rpk:0x1d`;
- наборы `theta`/`phi` и `lit_dynamic` совпадают: те же 19 из 23 и те же 4
  ресурса без полей.

`[?]` Связь `lit_dynamic` ↔ `part_lamp_lit()` **не доказана**. Правдоподобная
версия: `lit_dynamic` — авторский признак «этот свет включается по состоянию»,
то есть данные, к которым штатный скрипт обращается; но ни строки скрипта, ни
кода, который их читает, в установке нет. Утверждать, что host «реализует
данные `lit_dynamic`», нельзя: `part_lamp_lit` смотрит на CFG-флаги и время, а
не на световой ресурс.

`[Т]` Механизмы, которые нельзя считать взаимозаменяемыми:

| Механизм | Существует в host | Чем задаётся |
|---|---|---|
| Видимость геометрии лампы | да | CFG `flags` + `part_lamp_lit` (только бит `4`) |
| Unlit-style материал лампы | да | тот же `light_flags` → `use = 1` |
| Собственное свечение / bloom лампы | **нет** | — |
| Прямое освещение дороги | **нет** | spot/point из ресурса |
| Point/Spot источник | **нет** | — |
| Bloom / lens flare | только солнечный flare | `Flare` + `draw_sun_flare` |

## L1.6. Инвентаризация ресурсов

`[Ф-рес]` Записей с `typeof == 13` — **49** в **18** пакетах из 82. Из них
**3 пустые** (payload `size == 0`), то есть заполненных resource descriptors —
**46**. Считать «уникальные Light resources» нужно именно 46; 49 — это число
TOC-записей, включая папочные заглушки. RenderRef'ы и ссылки считаются
отдельно (L1.6.3).

### L1.6.1. Количества по пакетам

| Пакет | Directional | Spot | Point | `fake*` | пусто | всего |
|---|---:|---:|---:|---:|---:|---:|
| `DestructionTools.rpk` | 0 | 0 | 0 | 7 | 0 | 7 |
| `cars/fake_racers.rpk` | 0 | 0 | 0 | 5 | 1 | 6 |
| `cars/racers.rpk` | 0 | 2 | 0 | 5 | 1 | 8 |
| `frontend.rpk` | 3 | 0 | 0 | 0 | 0 | 3 |
| `maps.rpk` | 0 | 0 | 0 | 0 | 1 | 1 |
| `maps/DamageTrack.rpk` | 1 | 0 | 0 | 0 | 0 | 1 |
| `maps/DriftTrack.rpk` | 1 | 0 | 0 | 0 | 0 | 1 |
| `maps/MP1.rpk` | 1 | 0 | 0 | 0 | 0 | 1 |
| `maps/city.rpk` | 1 | 0 | 0 | 0 | 0 | 1 |
| `maps/city2.rpk` | 1 | 0 | 0 | 0 | 0 | 1 |
| `maps/skydome.rpk` | 2 | 0 | 0 | 0 | 0 | 2 |
| `maps/test_track.rpk` | 1 | 0 | 0 | 0 | 0 | 1 |
| `misc/carlot.rpk` | 1 | 0 | 0 | 0 | 0 | 1 |
| `misc/garage.rpk` | 2 | 0 | 0 | 0 | 0 | 2 |
| `objects.rpk` | 0 | 0 | 2 | 3 | 0 | 5 |
| `particles.rpk` | 0 | 1 | 0 | 4 | 0 | 5 |
| `parts.rpk` | 0 | 1 | 0 | 1 | 0 | 2 |
| `system.rpk` | 1 | 0 | 0 | 0 | 0 | 1 |
| **итого** | **15** | **4** | **2** | **25** | **3** | **49** |

Расшифровка `fake*`: 19 `fakespot` + 5 `fakepoint` + 1 `type fake point`.
«Пусто» — ресурсы `typeof 13` с нулевым payload (папочные заглушки:
`cars/racers.rpk:0x0007`, `cars/fake_racers.rpk:0x0007`, `maps.rpk:0x0004`).

`[Т]` В строке «итого» — 49 TOC-записей; вычитая 3 пустых, получаем **46**
заполненных: 15 + 4 + 2 + 25 = 46. Дальше по тексту «ресурс света» означает
именно заполненный дескриптор.

`[Т]` Практический вывод: «в игре есть Point и Spot» верно, но это 6
заполненных ресурсов на всю установку. Автомобильный контекст по данным
подтверждён для **трёх** Spot-ресурсов — `cars/racers.rpk` `0x0008`,
`cars/racers.rpk` `0x000d`, `parts.rpk` `0xd362`. Четвёртый,
`particles.rpk:0x0021` (`white_light`), — тоже `spot`, но его связь с фарами
ничем не подтверждена: ни одна конфигурация на него не ссылается, а alias
`white_light` говорит о назначении не больше, чем тип. Считать его частью
«автомобильного набора» нельзя, пока это не покажет runtime-лог. Плюс
`objects.rpk` даёт два Point для уличных фонарей. Всё остальное, что
светится, — `fake*`.

### L1.6.2. Репрезентативные примеры

Directional (все 15 — по одному на карту/сцену):

| Пакет | id | alias | Ключевые поля |
|---|---|---|---|
| `maps/city.rpk` | `0x000a` | `city_com` | `diffuse 0 1 1 1`, `direction 1 -1.5 0.2`, `range 10000` |
| `maps/skydome.rpk` | `0x0024` | `sun` | `diffuse 0 0.097 0.092 1`, `direction 1 -1.5 0.5`, полный `lf_*` |
| `misc/garage.rpk` | `0x001d` | `garage` | `diffuse 1 0.919 0.958 1`, `direction 0 -1 0`, `position 0 3 0` |
| `frontend.rpk` | `0x0001` | `menulight` | `diffuse 0 1 1 0.948`, `direction 0 -0.707 -0.707` |
| `system.rpk` | `0x000b` | `typ_render_light` | шаблон: все цвета 0, `direction 0.707 -0.707 0` |

Spot:

| Пакет | id | alias | Ключевые поля |
|---|---|---|---|
| `cars/racers.rpk` | `0x0008` | `headlights` | `range 250`, `theta 1.896`, `phi 2.020`, `attH/attV`, `lit_dynamic 1` |
| `cars/racers.rpk` | `0x000d` | `headlights` | `range 150`, `theta 1.396`, `phi 1.920`, те же текстуры |
| `parts.rpk` | `0xd362` | `headlights` | все цвета `0`, `range 300`, `attV 0x0000d363` |
| `particles.rpk` | `0x0021` | `white_light` | `range 100`, `diffuse 0.994 1 1 1`, без `theta/phi` |

Point:

| Пакет | id | alias | Ключевые поля |
|---|---|---|---|
| `objects.rpk` | `0x01c7` | `street` | `range 500`, `attenuation 1 0.03 0`, `diffuse 1 0.972 0.888 0.615` |
| `objects.rpk` | `0x01cd` | `testtrack` | `range 350`, голубой `0 0.986 1 0.912` |

`fake*` (примеры):

| Пакет | id | alias | Вид | Комментарий |
|---|---|---|---|---|
| `cars/racers.rpk` | `0x000f` | `taillights` | fakespot | `range 5`, янтарный, `lit_dynamic 1` |
| `cars/racers.rpk` | `0x0026` | `headlights` | fakespot | `range 80`, `theta 1.896`; по alias и цветам похож на пару к spot `0x8` |
| `objects.rpk` | `0x0016` | `street_fake_proba` | `type fake point` | `range 200`, те же цвета, что у `point` `0x1c7` |
| `objects.rpk` | `0x01c6` | `traffic` | fakespot | `range 100`, единственный «настоящий» `light`-конфиг (L1.6.3) |
| `objects.rpk` | `0x01ce` | `testtrack_fake` | fakepoint | `range 50`, пара к `point` `0x1cd` |

`[Т]` Пары `point`/`fakepoint` и `spot`/`fakespot` с почти одинаковыми
цветами и разными `range` (`street` 500 против `street_fake_proba` 200,
`testtrack` 350 против `testtrack_fake` 50) — единственное свидетельство
назначения `fake`: облегчённый вариант для тех мест, где полное освещение не
нужно. Семантика не доказана ни кодом, ни документом.

### L1.6.3. Ссылки из конфигов

`[Ф-рес]` Просмотрены все текстовые payload'ы на строки `render <id>` и
`light <id>`, где `<id>` — ресурс света того же пакета. Найдено **9** ссылок,
все в `objects.rpk`:

| Свет | Ссылается из | Строка |
|---|---|---|
| `point 0x01c7` | `lamp_base`, `lamp_road`, `kislampa`, `lamp_lane0_hold` | `render 0x000001c7 …` |
| `fake point 0x0016` | `lamp_a`, `lamp_b`, `lamp_c`, `pozna` | `render 0x00000016 …` |
| `fakespot 0x01c6` | `trafficlamp` | `light 0x000001c6 1 0 0` |

`[Ф-рес]` **Ни одна из 4 spot- и 2 point-записей не упомянута ни в одном
конфиге** — не только автомобильном. Ссылки на свет из car-part CFG нет;
значит связь «деталь → световой ресурс» устанавливается не данными, а скриптом
(через `RenderRef.create` с id) или не устанавливается вовсе. Установить это без
runtime-лога нельзя — по байткоду `cars/**/*.class` целочисленные константы
неотличимы от прочих (`0x26`, `0x0d` встречаются в сотнях методов).

`[Ф-рес]` Обратная сторона: `light <id> x y z` — отдельная строка ICFG, которой
пользуется только `trafficlamp`. Для уличных фонарей используется `render` с
`flags 4`.

`[Т]` Уровни доказанности этих связей — три разных, и их нельзя сливать:

| Уровень | Что подтверждено | Статус |
|---|---|---|
| 1 | В ICFG есть текстовая ссылка на resource id (`render 0x1c7`, `light 0x1c6`) | `[Ф-рес]` — да, строки найдены |
| 2 | Из контекста (alias объекта, позиция, соседние строки) следует, что объект «фонарь» ссылается на свет | `[Г]` — совместимая интерпретация, не доказательство |
| 3 | OpenSLRR действительно загружает эту ссылку, создаёт узел и использует свет | не прослежено: в host нет парсера строк `light`/`render` для ICFG-света, а object-путь идёт через свои таблицы |

`[Т]` Отдельно про `flags 4`: **нельзя** приравнивать его к
`PartConfig::renders[].flags`/`light_flags` без прослеженного пути передачи.
Число совпадает и семантика «city» выглядит правдоподобной (та же маска `4` в
`part_lamp_lit`), но это два разных парсера и два разных владельца данных:
у деталей — `part_config.cpp` → `part_runtime.cpp`, у объекта — текст ICFG.
Здесь это записано как гипотеза, а не как факт; реконструкция ICFG-runtime-пути
в L1.1 не входит.

### L1.6.4. Заполнение и распределение параметров

`[Ф-рес]` Заполненность (числитель — сколько ресурсов вида имеют поле):

| Поле | directional (15) | spot (4) | point (2) | fake* (25) |
|---|---|---|---|---|
| `diffuse/specular/ambient/position/direction` | 15 | 4 | 2 | 25 |
| `range` | 15 | 4 | 2 | 25 |
| `attenuation` | 14 | 4 | 2 | 25 |
| `theta`, `phi` | **0** | 3 | **0** | 19 |
| `lit_dynamic` | **0** | 3 | **0** | 19 |
| `attH_texture`, `attV_texture` | **0** | 3 | 0 | 13 |
| `lf_texture` | 1 | 4 | 2 | 25 |
| `lf_glow_*` | 2–13 | 4 | 2 | 25 |
| `lf_ray_*` | 2–3 | 4 | 1 | 20 |
| `lf_flare_*` | 3–4 | 3–4 | 1 | 20 |
| `lf_texturemode` | 2 | 4 | 1 | 24 |

`[Т]` Колонки не складываются напрямую: «19» в колонке `fake*` — это 16
`fakespot` + 3 `fakepoint`, а «3» в колонке spot — три из четырёх spot.
Поэтому 3 + 19 ≠ 22 из 23. Корректные итоги: **19 из 23** в группе
spot+fakespot и **22 из 29** в группе spot + все `fake*` (см. пересчёт в L1.5).

`[Ф-рес]` Числовые распределения:

- `range`: directional — `1000` (7), `100` (3), `10000` (2), `1300`, `400`,
  `5000`; spot — `100`, `150`, `250`, `300`; point — `350`, `500`;
  fakespot — `5` (8), `100` (3), `400` (2), `30` (2), `1500`, `80`, `20`, `10`;
  fakepoint — `2` (3), `50`, `100`.
- `attenuation`: spot/point/fake — **всегда** `1.000 0.030 0.000`;
  directional — `0 0 0` (13 из 14), исключение `maps/MP1.rpk` `1 1 1`.
- `theta`: `1.396` (13 у fake*, 1 у spot), `1.896` (3 + 2).
- `phi`: `1.920` (13 + 1), `1.896`→ только у spot `2.020`/`2.000`, у fake*
  `2.220`.
- `lit_dynamic`: всегда `1.000`.
- `position`: у spot/point/fake — всегда `0 0 0`; у directional — `0 0 0` (8),
  `0 3 0` (garage), `0 10 30` (skydome), `0.3 -1 0` (city), `0.247 1 -0.9`
  (DamageTrack), `-200 220 130` (DriftTrack).
- `direction`: у point — `0 0 0`; у spot/fake — `0 0 -1` (23) или `0 0 1` (4);
  у directional — `1 -1.5 0.5` (3), `1 -1.5 0.2` (2) и по одному разу
  `0 -0.707 ±0.707`, `0 0 -1`, `0.3 -0.26 0.26`, `0.247 -1 0.9`, `0 0 0`.

`[Ф-рес]` Цвета: первый компонент `diffuse/specular/ambient` — практически
всегда `0.000` или `1.000`, редко `0.6`–`0.9`; далее три канала. Все
`ambient` у spot/fake — либо `1 0 0 0`, либо `0 0 0 0`. У directional
`ambient` всегда ненулевой в RGB и это единственный вид с осмысленным ambient.

`[Ф-рес]` Текстурные ссылки разрешаются (с учётом dep-slot, L1.7.7):

| Поле | Значения | Куда указывает |
|---|---|---|
| `lf_texture` | `0x00000100` | `cars/racers.rpk` (и `maps/skydome.rpk`) → `maps\skydome\lensflare\Flares.ptx` |
| | `0x000004d2` | `objects.rpk` → та же `Flares.ptx` |
| | `0x000001c8` | `objects.rpk` → `objects\textures\Flare.ptx` |
| | `0x00030100` | `DestructionTools.rpk` → dep-слот 3 → `Flares.ptx` |
| `attH_texture` | `0x0000000b` / `0x0003000b` | `cars\racers\textures\light_beams\attH.dds` |
| `attV_texture` | `0x0000000c` / `0x0003000c` | `cars\racers\textures\light_beams\attV.ptx` |
| `attV_texture` (parts) | `0x0000d363` | `parts.rpk`, локальный id `0xd363` |

Файлы на диске есть: `attH.dds` 640 Б, `attV.ptx` 8 272 Б,
`maps/skydome/lensflare/Flares.ptx` 10 315 Б, `objects/textures/Flare.ptx`
22 467 Б. То есть projection- и flare-текстуры — не пустые ссылки.

## L1.7. Семантика параметров

### L1.7.1. Что говорит сам оригинальный движок

`[Ф-код]` В `StreetLegal_Redline.exe` по смещению ~`0x21a4xx` лежит таблица
ключевых слов парсера света в порядке разбора, с форматами `sscanf`:

```
type            %s %s      значения: directional, point, fakepoint, fakespot, spot
diffuse         %s %f %f %f %f
specular        %s %f %f %f %f
ambient         %s %f %f %f %f
position        %s %f %f %f
direction       %s %f %f %f
range           %s %f
falloff         %s %f
attenuation     %s %f %f %f
theta           %s %f
phi             %s %f
lit_dynamic     %s %d
iscolorable     %s %d
attH_texture    %s %d
attV_texture    %s %d
lit_sobj        %s %d
lit_dobj        %s %d
shd_sc_sr       %s %d
shd_sc_dr       %s %d
shd_dc_sr       %s %d
shd_dc_dr       %s %d
plane           %s %f %f %f %f
lf_texture      %s %d
lf_glow_offset  %s %f
lf_texturemode  %s %d
lf_minNdotV     %s %f
lf_useNdotH     %s %d
lf_glow_minsize / maxsize / size  %f
lf_glow_color   %s %f %f %f %f
lf_glow_colortype %s %d
lf_ray_* / lf_flare_*  %f | %d
```

Что из этого следует, а что нет:

- `[Ф-код]` **движок знает все пять видов**, включая `fakespot`/`fakepoint`:
  это не мод-артефакт, а формат;
- `[Ф-код]` движок разбирает `theta`, `phi`, `attenuation`, `range`,
  `lit_dynamic`, `attH/attV_texture` — то есть эти поля не «мусор», у них есть
  потребитель;
- `[Ф-код]` `lit_dynamic` — **целое** (`%d`), значит `1.000` в тексте
  читается как `1`;
- `[Ф-код]` в таблице есть поля, которых **нет ни в одном ресурсе**:
  `falloff`, `iscolorable`, `lit_sobj`, `lit_dobj`, `shd_sc_sr`, `shd_sc_dr`,
  `shd_dc_sr`, `shd_dc_dr`, `plane`, `lf_minNdotV`, `lf_useNdotH`. Значит
  формат шире контента, и «поля нет» не означает «движок этого не умеет»;
- `[?]` **как** движок применяет значения — из таблицы не следует. Формула
  затухания, трактовка углов и единицы остаются неизвестными (L1.7.3–7.5).

`[Т]` Отдельно: `type fake point` (с пробелом, `objects.rpk:0x0016`) не
совпадает ни с одним токеном таблицы. Либо парсер берёт только первое слово
(`fake`) и вид не распознаётся, либо распознаётся как `point` с отдельным
флагом. Проверить без запуска нельзя.

### L1.7.2. Пространственный масштаб

`[Ф-код]` Контракт в коде (`scx_mesh.hpp:66-68`): «SCX positions are
centimetres. POLY / script positions are already metres», `position_scale`
по умолчанию `0.01`.

`[Ф-рес]` Независимое подтверждение из данных: границы объектов в ICFG заданы
в метрах и физически правдоподобны — `lamp_base bounds … 10.16 × 8.02 × 13.03`,
`trafficlamp bounds -0.7 -0.5 -0.3 … 1.7 0.5 0.3` (светофор 2.4 × 1.0 × 0.6 м),
`lamp_road` ставит свет на `y = 7.8` при высоте фонаря 8.02.

`[Т]` Итог: **пространство размещения — метры**, пространство вершин SCX —
сантиметры, переход один (`×0.01`). Значения света заданы в пространстве
размещения, то есть в тех же единицах, что `position` узла и `range`.

`[?]` Это не доказывает, что оригинальный движок трактовал `range` в этих же
единицах (L1.7.3).

### L1.7.3. `range`

- `[Ф-рес]` Значения: 2 … 10 000, с кластерами `5` (стопы), `100`–`500`
  (фары, фонари), `1000`–`10 000` (солнце).
- `[Ф-код]` host не разбирает поле вообще: в тексте ресурса оно читается
  «мимо».
- `[?]` Жёсткий ли это cutoff, дистанция culling или вход формулы — не
  установлено. Единственная внешняя опора — D3D9 `D3DLIGHT9.Range`, где это
  максимальная дистанция действия; но подтверждения, что движок отдавал
  значение прямо в `SetLight`, у нас нет (`SetLight`/`LightEnable` в импортах
  exe не видны по имени).
- `[Т]` Практический вывод для будущей реализации: **нельзя** брать `range`
  за метры без проверки. Проверяемо только опытом: включить источник с
  `range 100` в известной сцене и измерить, где гаснет.

### L1.7.4. `attenuation`

- `[Ф-рес]` У всех локальных видов строго `1.000 0.030 0.000`; у directional
  `0 0 0` (кроме `MP1`, где `1 1 1`).
- `[Ф-код]` host не разбирает.
- `[?]` Порядок коэффициентов (constant/linear/quadratic или наоборот) и
  формула не подтверждены. При `1 / 0.03 / 0` D3D9-формула
  `1/(c + l·d + q·d²)` даёт на `d = 100` ≈ `0.25`, на `d = 250` ≈ `0.12` — то
  есть свет «доживает» до `range`, не обрываясь. Это согласуется с парой
  `range`+`attenuation`, но не доказывает её.
- `[Т]` При восстановлении нельзя молча взять D3D9-формулу «по названию
  поля»; нужен либо эксперимент, либо отдельное решение о новой формуле.

### L1.7.5. `theta` / `phi`

`[Ф-рес]` Значения и диапазоны (только spot + fakespot, 19 ресурсов):

| Пара | Сколько | В градусах (если это радианы) |
|---|---:|---|
| `1.396` / `1.920` | 14 | 80.0° / 110.0° |
| `1.896` / `2.020` | 1 | 108.6° / 115.7° |
| `1.896` / `2.000` | 1 | 108.6° / 114.6° |
| `1.896` / `2.220` | 3 | 108.6° / 127.2° |

У spot `theta`/`phi` есть у 3 из 4, у fakespot — у 16 из 19; у directional и
point — ни у одного. `theta < phi` во всех парах, `phi/theta` = 1.055…1.375.

`[Г]` Совместимая интерпретация — **радианы, inner/outer cone**:

- это соответствует стандартному описанию `D3DLIGHT9` (`Theta` — внутренний
  конус, `Phi` — внешняя граница, оба в радианах, `Phi` до π):
  <https://learn.microsoft.com/en-us/windows/win32/direct3d9/d3dlight9>;
- значения попадают в диапазон: 1.396…2.220 < π, `theta < phi` во всех парах,
  и порядок величин совпадает с «узкий/широкий конус»;
- как **половина** угла эти значения не проходят для четырёх пар из четырёх:
  `phi = 1.920` рад = 110° > 90°, то есть полуугол больше прямого. Либо это
  полный угол конуса (тогда физически осмысленно), либо движок брал
  `cos`/делил на два по своему правилу — и то, и другое без кода не
  проверяется.

`[?]` Почему это всё-таки **не факт для SLRR**: в таблице парсера exe видны
только ключи и форматы (`theta %f`, `phi %f`) — ни формулы, ни единиц она не
показывает. Никакой код host этих полей не читает, а оригинальный путь
неизвестен. Поэтому трактовка остаётся гипотезой, а не установленной
семантикой; отличается от «неизвестно» только тем, что для неё есть
стандарт-совместимое и численно непротиворечивое объяснение.

### L1.7.6. Цвета

- `[Ф-рес]` Порядок — `a r g b`: первый компонент отделён от тройки и
  практически всегда `0` или `1`. Проверка: `objects.rpk:0x01cd`
  `0.000 0.986 1.000 0.912` — как `argb` даёт почти белый, как `rgba` дал бы
  `r = 0`, то есть чёрный по красному. `argb` согласуется с остальными
  записями.
- `[Ф-код]` host читает `a r g b` и **отбрасывает `a`** — совпадает с
  данными.
- `[?]` Смысл `a` (альфа? признак? тип?) не установлен; значения бинарные.
- `[Ф-код]` В рендере цвет источника проходит три преобразования: файл →
  `setLight` (только меню) → дневной цикл (только улица, gain ≤ 2.8 и тёплый
  оттенок). Гамма/линейность старых значений не установлена: сравнение с
  `AUTOMOTIVE_RENDERING.md` §«Lighting is therefore done on gamma values»
  говорит, что исторически это гамма-значения, а не линейные.
- `[Т]` Принимать RGB за фотометрическую интенсивность нельзя: у фар
  `diffuse` = `(0.2, 0.2, 0.2)`, у фонаря `(0.972, 0.888, 0.615)` — это цвет, а
  не сила.

### L1.7.7. Проекционные текстуры

- `[Ф-рес]` `attH_texture`/`attV_texture` есть у 16 ресурсов (3 spot + 13
  fake*), отсутствуют у directional/point. Указывают на `attH.dds` и
  `attV.ptx` в `cars/racers.rpk` — H горизонтальная, V вертикальная (по
  именам и по отдельным файлам).
- `[Ф-код]` host не разбирает; `attH.dds` — 640 байт, то есть это маленькая
  карта (не атлас), `attV.ptx` — 8 272 байта PTX.
- `[Т]` Это cookie/gobo для конуса, а не diffuse: у них нет ни `sourcefile`
  на общую текстуру кузова, ни совпадения с `lf_texture`.
- `[?]` Как именно H и V комбинируются (две проекции? одна по горизонтали,
  одна по вертикали?) и нужны ли они первой версии — открыто. Для минимального
  прямого освещения они не обязательны; для «настоящего» вида фар — вероятно
  да.

### L1.7.8. `lf_*` и flare

- `[Ф-рес]` Блок `lf_*` есть почти у всех: `lf_texture` (25 fake*, 4 spot,
  2 point, 1 directional), `lf_glow_color`, `lf_glow_offset`,
  `lf_glow_minsize/maxsize` (или `lf_glow_size`+`lf_glow_uv` у 6 ресурсов),
  `lf_ray_*`, `lf_flare_*`, `lf_texturemode`.
- `[Ф-рес]` Текстуры — `Flares.ptx` (10 315 Б) и `Flare.ptx` (22 467 Б).
- `[Ф-код]` host реализует **только солнечный flare**:
  `draw_sun_flare()` рисует спрайты из `SceneSun::flare`, который
  заполняется единственным путём — native `setFlare`, чья сигнатура не
  совпадает с объявленной в классе, и которую ни один скрипт не вызывает.
  Следовательно в штатном контенте flare не рисуется.
- `[Т]` Flare/glow — отдельная подсистема, не часть прямого освещения.
  В минимальный контракт Point/Spot её включать нельзя.

## L1.8. Baked lightmaps

`[Ф-код]` Три уровня, которые надо держать раздельно:

1. **Парсер SCX** узнаёт слот lightmap у материала (`scx_mesh.cpp:300`) и
   слот illumination; `scx_resolve_baked_lightmaps()` **повышает**
   illumination до lightmap только для списка из 15 «legacy» атласов
   (`misc/garage/g{1..4}/*shadow.dds`, `misc/dealer*/…shadow.dds`).
2. **Backend** передаёт в шейдер **белую текстуру** и нулевой флаг:
   `bind_baked_light()` игнорирует `tex` (`(void)tex;`), ставит
   `u_lightmap_ = {0, base_channel, 1, mix}` и `s_lightmap = white_tex_`.
   В `draw_instance()` для автомобилей `bake` — тоже всегда `white_tex_`.
3. **Шейдер** поэтому всегда считает `bake = 1` и `use = 0`. Ветка
   `use = 1` включается **только** для ламповых мешей (`light_flags`).

`[Ф-рес]` Что реально лежит в штатных атласах (декодировано DXT1, 512²/256²):

| Файл | Размер | Средняя яркость | Белых > 0.97 | Тёмных < 0.10 | Хрома |
|---|---|---:|---:|---:|---:|
| `misc/garage/g1/g1shadow.dds` | 512² | 0.368 | 17.0 % | 43.3 % | 10.1 % |
| `misc/garage/g1/g1objectshadow.dds` | 512² | 0.257 | 10.1 % | 58.3 % | 4.4 % |
| `misc/garage/g4/t_01_shadow.dds` | 512² | 0.881 | 75.2 % | 0.0 % | 24.4 % |
| `misc/dealer/textures/marketkulsoshadow.dds` | 256² | 0.256 | 15.4 % | 69.6 % | 8.9 % |
| `maps/textures/shadowM2.dds` | 512² | 0.194 | 0.4 % | 47.8 % | 5.7 % |
| `maps/textures/lightmap_ablakok02.dds` | 128×256 | 0.364 | 0.0 % | 48.9 % | 0.0 % |
| `maps/textures/kirakatok2_lightmap.dds` | 128² | 0.264 | 0.7 % | 70.4 % | 24.5 % |

`[Т]` Вывод по содержимому: это **не** «sun + shadows»-карты в общем случае.
Часть (`t_01_shadow`) — почти белая multiply-карта, часть (`g1shadow`,
`marketkulsoshadow`, `shadowM2`) — маски с большой долей тёмного, то есть
затемнение, а не запечённое прямое освещение. Городские `*_lightmap.dds`
(окна) — тёмные маски свечения с нулевой или локальной хромой.

`[Т]` Поэтому и «выбрасывать как мусор», и «использовать как AO» одинаково
неверно: без разбора по типам атласов нельзя сказать, что именно умножается.
Одно можно утверждать твёрдо: **в текущем рендере они не участвуют вообще**, и
кодовый комментарий про «не дублировать солнце» объясняет решение, но не
описывает содержимое.

## L1.9. Тени

`[Ф-код]` Фактическое состояние на HEAD — четыре разных механизма, а не один:

| Механизм | Живой | Фрустум | Кто отбрасывает |
|---|---|---|---|
| Статическая карта мира (`kStaticShadowView`) | да, при ground props и `sun.direction[1] < 0.02` | орто `±(range+step)`, где `range = min(56 \| 36, view_distance)`, `step = max(8, 0.15·range)` → примерно ±44 м или ±64 м; глубина 1024 | пропы/объекты мира; **`vehicle_material` исключён**, также исключены `sky`, скины, `light_flags` и `ground_polygons` |
| Гаражная карта (тот же view) | да, при `garage_ && local_light_[3] > 0` | орто 20×20 м сверху вниз из `local_light_`, глубина 0.15…16 | **только** `vehicle_material` без `light_flags` и только непрозрачные группы |
| Карта глубины автомобиля (`update_car_depth`) | да, при движении солнца вниз | орто 28×28 м вдоль солнца, глубина 48 | `vehicle_material` без `light_flags` |
| Проекционные карты на машину + `kocsiarnyek` | **нет** | — | `draw_vehicle_shadows()` объявлен и реализован, но вызова в host нет |

`[Ф-код]` Приёмники: `fs_mesh` читает `s_staticShadow` (только при
`u_staticShadowParams.x > 0` и `use < 0.5`) и `s_carDepth` (контактная тень).
`fs_vehicle` **не читает ни одну из них** — автомобили не получают ни карту
мира, ни контактную тень.

`[Т]` Отсюда два следствия. Первое: кузова не попадают в outdoor-карту — для
них существует отдельный проход `update_car_depth`, поэтому «outdoor static
map отбрасывает машины» неверно. Второе: утверждение старого аудита «все тени
построены вокруг одного sun frustum» тоже неверно — фрустумов три и они
разные. Общее одно: **ни один из них не строится от Point/Spot**, и
кубической тени нет.

## L1.10. Reflection Capture

`[Ф-код]` Захват идёт в view `72 + 2·face` (6 граней по бюджету
`reflection_faces`), `far = 150`, размер грани по умолчанию 512.

- **Что попадает:** объекты из `scene_instances_collect(&frustum)` — тот же
  сбор, что и в основной проход, с тем же `SceneInstance.lighting`, плюс
  проп-батчи. Исключены: небо (рисуется отдельно `draw_sky`), все
  `vehicle_material` (кузов и детали), водитель, скины без палитры, мелочь
  меньше 4 px.
- **Свет:** `bind_baked_light()` получает `instance.lighting` при
  `outdoor_lighting_ && dynamic_lighting`, то есть на улице — да, в гараже —
  нет. `bind_baked_light` выставляет `u_localLight = 0`, после чего
  `draw_instance` для `world_mesh` **снова** ставит `local_light_`
  (`bgfx_lighting.cpp:420`) — в гараже локальный свет в куб попадает.
- **Тени:** `receive = world_mesh && view_id < 72`; `view_id` захвата ≥ 72,
  поэтому **тени в куб не попадают** ни статические, ни контактные.
- **Проп-батчи:** в захвате локальный свет им не ставится
  (`if (receive) setUniform(u_local_light_, …)`, `receive = view_id < 72`) —
  расхождение с одиночными пропами.
- **Один и тот же `SceneInstance`?** Да для освещения (поле `lighting`
  считается один раз на кадр из `scene_instances_light(g_world)`), но **нет**
  для состояния: тени, локальный свет на батчах и flare в захвате отличаются.

`[Т]` `RENDER_DATA_CONTRACT.md` требует, чтобы один и тот же логический
`LightInstance` использовался и в основном проходе, и в захвате. Сейчас это
выполнено только для одного directional: point/spot нет ни там, ни там, а
локальный свет применяется к разным наборам объектов в двух проходах.
Считать контракт реализованным нельзя.

## L1.11. Итоговая матрица фактов

| Тема | Подтверждено | Неизвестно / требует исследования | Статус OpenSLRR |
|---|---|---|---|
| Directional | `[Ф-код]` полная цепочка от `RenderRef.create` до `u_sunDir`; выбор по `live_id`; `[Ф-рес]` 15 ресурсов | как согласуются «новейший узел» и семантика карты | работает |
| Point | `[Ф-рес]` 2 ресурса, `attenuation 1/0.03/0`; `[Ф-код]` движок их знает | связь с объектом (ICFG-ссылка найдена, runtime-путь — нет); формула | не реализовано |
| Spot | `[Ф-рес]` 4 ресурса, `theta/phi`, `attH/attV`; автомобильный контекст — только у 3 | трактовка углов; как связывается с фарой; нужны ли gobo | не реализовано |
| `fake*` | `[Ф-рес]` 25 ресурсов; `[Ф-код]` токены в exe | семантика отличия от полного света | не реализовано |
| Garage local light | `[Ф-код]` синтетический: узел `0x1D` + `+3.3`, радиус 8, тёплый `×3` | зачем именно `0x1D` в оригинале | работает, хардкод |
| Lamp geometry/state | `[Ф-код]` CFG `flags` + `part_lamp_lit` (только бит `4`) + `use = 1` (unlit-style) | соответствие оригинальной логике; brake/reverse/indicator отдельно не реализованы | работает |
| `lit_dynamic` | `[Ф-рес]` 19/23 (spot+fakespot) или 22/29 с fakepoint, всегда 1; `[Ф-код]` `%d` в exe | связь с `part_lamp_lit` не доказана | не читается |
| Attenuation | `[Ф-рес]` тройка у всех локальных | формула и порядок | не читается |
| Cone angles | `[Ф-рес]` только spot-семейство, 19/23 | `[Г]` радианы, inner/outer по `D3DLIGHT9`; применение в SLRR не доказано | не читается |
| World units | `[Ф-код]` SCX = см, размещение = м; `[Ф-рес]` границы объектов в метрах | единицы `range` | — |
| Photometric units | ничего | всё | нет системы |
| Lightmap | `[Ф-код]` парсится, в шейдер идёт белая; `[Ф-рес]` содержимое измерено | что именно запечено в каждый атлас | не используется |
| ICFG-ссылки на свет | `[Ф-рес]` 9 строк `render`/`light` в `objects.rpk`; `[Г]` связь «объект → свет» | runtime-путь в host не прослежен; `flags 4` не равно `light_flags` | не реализовано |
| Shadows | `[Ф-код]` 3 живых механизма (outdoor без кузовов, гараж, car depth), 4-й без call site | — | работает, без Point/Spot |
| Reflection Capture | `[Ф-код]` тот же directional, без теней, локальный свет частично | согласованность с основным проходом | частично |
| Projection textures | `[Ф-рес]` 16 ссылок, файлы существуют | назначение H/V | не читается |
| Flare/glow | `[Ф-рес]` полный блок `lf_*` у 25+ ресурсов | как рисовался оригиналом | только солнечный, не вызывается |

## L1.12. Блокеры до реализации

`[П]` Упорядочено по зависимости: каждый пункт закрывает следующий.

1. **Связь «деталь/объект → световой ресурс».** Без неё нельзя создать
   `LightInstance`: в конфигах ссылок нет (L1.6.3), а скриптовый путь
   (`RenderRef.create` с id) не подтверждён. Нужен runtime-лог: какие узлы
   создаются с `type_id` вида `0x…0008`/`0x…000d`/`0x…0026`.
2. **Единицы `range`.** Определяют, попадёт ли свет в сцену вообще. Нужен
   эксперимент с известной геометрией или находка в exe.
3. **Формула затухания.** Пока не известна, любая реализация — догадка.
4. **Применение `theta`/`phi`.** Гипотеза `D3DLIGHT9` (радианы, inner/outer,
   L1.7.5) — совместима, но не доказана. От неё зависят форма конуса и
   `attH/attV`.
5. **Семантика `fake*`.** Если это «glow без освещения», то 25 из 46
   заполненных ресурсов вообще не должны попадать в LightRegistry — иначе
   сцена заполнится лишними источниками.
6. **Согласованность с Reflection Capture.** `RENDER_DATA_CONTRACT.md` §14
   требует одинакового `LightInstance` в двух проходах; сейчас даже
   локальный свет применяется по-разному (L1.10).
7. **`lit_dynamic` ↔ `part_lamp_lit`.** Определяет, кто владеет состоянием
   «лампа включена»: CFG-флаги и время или данные ресурса.
8. **Судьба `SceneSun`.** Один directional как глобал + приоритет по
   `live_id` — рабочая, но неочевидная модель; при появлении списка
   источников её нужно либо сохранить как «солнце», либо заменить, и это
   решение нельзя принимать по ходу.

## L1.13. Что осталось неизвестным

- Единицы и формула `range`/`attenuation` — свидетельства проверены
  (L1.7.3–7.4), подтверждения нет.
- Применяет ли SLRR трактовку `theta`/`phi` из `D3DLIGHT9` (радианы,
  inner/outer) — интерпретация совместима с данными и стандартом, но прямого
  подтверждения нет (§L1.7.5).
- Смысл первого компонента цвета и его бинарность.
- Отличие `fakepoint`/`fakespot` от полных видов на уровне движка.
- Как оригинал связывал свет с автомобилем и переключал его; как ICFG-строки
  `render`/`light` превращаются в узлы (в host этот путь не прослежен).
- Что именно запечено в каждый lightmap-атлас по отдельности (измерены
  распределения, не назначение).
- Назначение `attH`/`attV` и `lf_minNdotV`/`lf_useNdotH` (последние в контенте
  не встречаются).
- Поведение `type fake point` (с пробелом) в оригинальном парсере.

`[Т]` Ни один из этих пунктов не закрывается чтением кода host: он их не
реализует. Закрыть их можно только runtime-наблюдением или разбором
оригинального exe, что выходит за рамки L1.

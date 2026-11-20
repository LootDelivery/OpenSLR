# SCX material format — requirements and architecture

Status: **requirements only**. Nothing here is implemented. The binary layout
is deliberately not fixed yet: it is frozen only after the material model and
the BRDF have been validated on real content.

This document owns the material storage format, the parser and the SCX
Exporter. `AUTOMOTIVE_RENDERING.md` owns the roadmap, `PBR_PLAN.md` owns the
material and BRDF model, `LIGHTING_AUDIT.md` owns the actual light state.

## 1. Why this is not optional

The new renderer needs somewhere to store and edit the new materials. Extending
SCX, its parser and the 3ds Max exporter is the delivery mechanism for that,
not a later convenience.

Two constraints hold throughout:

- **Old SCX must keep loading**, unmodified, with a correct legacy fallback.
- **Format and exporter work comes after the material model stabilises**, but
  its requirements are fixed now so the model is shaped by them.

## 2. Unified internal material model

The renderer must not care where a material came from. One internal
representation, filled from any of:

- old SCX;
- temporary development descriptors (sidecars) while the BRDF is being built;
- future extended SCX;
- future typed OpenSLRR resources inside RPK (the reusable Paint Definition
  library).

Old materials convert to a compatible **legacy representation**. New materials
convert to the full **PBR representation**. This is what lets the BRDF be
developed and checked before the new SCX format exists.

The sidecars are a **development tool only** — for prototyping the BRDF, testing
parameters and preparing content before it is packaged. They are not a permanent
runtime store and must not become a rival source of truth.

Permanent placement follows one split (A1 in
`MATERIAL_SYSTEM_ARCHITECTURE.md` §10.4):

- **model-owned Material Definitions** — the material of a particular model —
  are stored permanently in **SCX v5**;
- the **reusable Paint Definition library** (automotive paint variants shared by
  many cars) lives in separate **typed OpenSLRR resources inside RPK**. It is not
  copied into SCX, and this document does not define the RPK payload.

A paint slot merely offers an optional choice: a model may have none and may not
depend on the paint library at all. Original content had no such library:
`CARCOLORS` and `PaintCan` are texture IDs and a colour/capacity pair, not PBR
paint definitions. The new resource type belongs to OpenSLRR; reading it in the
original executable is not claimed.

## 3. Material is not a texture

A texture and a material are different entities.

The material is bound to geometry or to an SCX section explicitly. A texture ID
may be used for lookup and for compatibility with old SCX, but it must not be
the only material identifier.

Concretely: **repainting a car must not destroy clearcoat, roughness or any
other material property.**

## 4. Required material fields

Preliminary. This is a requirements list, not the final binary spec.

**Common**

- material id
- shading model / type
- binding to a geometry group or SCX section
- texture references
- base colour and alpha

**Generic PBR**

- baseColor, metallic, roughness, normal, AO, emissive

**Layered automotive paint** (per `PBR_PLAN.md`, not `mix(color, environment, k)`)

- basecoat parameters
- clearcoat parameters, with its own roughness and reflectance
- intensity
- layer normals where used
- metallic flake / pearl, only within the approved model

## 5. SCX extension design

Design separately:

1. internal C++ SCX structures
2. on-disk data structure
3. rules binding materials to geometry
4. references to external textures and extra resources
5. versioning and compatibility

Preference: an extensible format carrying additional versioned data, so old SCX
needs no change or conversion.

Do not break existing structures or the read order of old versions. Do not
introduce a new binary layout before the material model is defined and checked.

**Compatibility boundary, stated explicitly:** OpenSLRR must read both old and
new versions. Whether the original game can read the new SCX is **not**
guaranteed and must be tested separately.

## 6. Parser extension

The parser must:

- keep reading existing SCX;
- recognise the new material data;
- validate versions, block sizes and references;
- tolerate missing optional data safely;
- preserve a correct legacy fallback;
- not fail on unknown or unsupported extra fields.

New functionality must never break loading of old mods.

## 7. SCX Exporter extension

The exporter is a full part of this task, not an afterthought. It must:

- understand the new material structure;
- let a material be assigned to specific geometry sections;
- edit PBR and layered paint properties;
- assign texture slots;
- save the additional data in the new format;
- export old SCX in a legacy-compatible mode;
- guarantee its own output is re-readable by the parser.

**Round-trip test is mandatory:** import SCX → edit material → export → import
again. Check not only the new properties but also the survival of geometry, UV,
old texture bindings, alpha and every other legacy parameter.

## 8. Ordering

Do not change SCX or the exporter during early BRDF development.

1. Now: fix the internal material contract and the future SCX requirements.
2. During BRDF work: use temporary descriptor / sidecar files.
3. When the material model and field list stabilise: freeze the new SCX spec.
4. Extend the internal SCX structures and the parser.
5. Extend the SCX Exporter.
6. Verify compatibility and round-trip.
7. Only then move material creation and editing onto the final format.

## 9. Acceptance criteria

- Old SCX loads unchanged, with no regression on any existing mod.
- New SCX loads in OpenSLRR; unknown extra fields do not break loading.
- Round-trip preserves geometry, UV, texture bindings, alpha and legacy fields.
- Material properties survive a repaint.
- The renderer produces the same result whether the material came from an old
  SCX, a sidecar, a new SCX, or a typed RPK Paint Definition resource.
- Reading new SCX — and the new typed RPK paint resource — in the original game
  is reported separately, with evidence; compatibility is not claimed.

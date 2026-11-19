# Separating the HDR scene from the LDR interface

> **Status: step 3 is complete.** The routing described in the table below was
> the *problem* state that motivated this document. It has since changed:
> interface now draws to the window, plates at `kPlateView` (69) before the
> preview composite and icons/text at `kOsdView` (71) after it. Nothing reaches
> `scene_fb_`. The table is kept because it records what was wrong and why the
> naive fix failed -- see `RENDERING_ARCHITECTURE.md` section 4 for the current
> state and `RENDERING_HISTORY.md` section 5 for the reverted attempts.

Goal: the ordinary screen UI must be drawn after scene post-processing and tone
mapping, in display space, and must not be affected by exposure or HDR
processing. The world keeps its HDR treatment.

## Audit — where the UI draws today

Draw entry points found (`bgfx_osd.cpp`): every UI batch submits to a single
`osd_view_`, which is switched per widget.

| Widget class | View | Target | Correct? |
|---|---|---|---|
| modal windows (`osd_owner_modal`) | `kOsdUnder` = 7 (then) | `scene_fb_` | **historical** |
| non-modal — HUD, catalogue, car cards, tooltips, cursor | `kOsdUnder` = 7 | `scene_fb_` | **historical** |

**Current routing (authoritative):** `kPlateView` = 69 for plates, preview
composite = 70, `kOsdView` = 71 for icons and text. Nothing draws into
`scene_fb_`. See `RENDERING_ARCHITECTURE.md` section 4.

The switch is at `bgfx_osd.cpp:880-886`. Everything routed to `kOsdUnder` is
composited into the HDR target and then tone mapped and encoded along with the
world. That is the whole of the reported symptom: catalogue and carinfo come
out brighter because they went through exposure, ACES and the output encode.

View layout as measured:

| View | Content | Target |
|---|---|---|
| 5 `kWorldView` | 3D scene | `scene_fb_` (HDR) |
| 6 `kFmvLayer` | video layer | `scene_fb_` |
| 7 `kOsdUnder` | **historical**: non-modal UI | `scene_fb_` |
| 8–63 `kPreview0`+ | inset previews | `scene_fb_` |
| 68 `kPresentView` | tone mapping | backbuffer |
| 70 `kOsdView` | modal UI | backbuffer |

So the correct ordering already exists for the modal path. The work is to bring
the rest onto it.

## Belonging is defined by how something is drawn

Not by what it is. A physical display on the dashboard is part of the HDR
scene; a speedometer painted over the game is LDR UI. This settles the
catalogue/carinfo question: look at the layer, not at the object.

## Decisions to fix before moving code

**Final output encoding.** Settled: hardware, `BGFX_RESET_SRGB_BACKBUFFER`.
The manual `pow(1/2.2)` is removed from `fs_present`. Checked against the
vendored bgfx first -- `BGFX_SWAP_CHAIN_SRGB_BACKBUFFER` is static_asserted
equal to `BGFX_RESET_SRGB_BACKBUFFER`, and D3D11 creates the swap chain as
`DXGI_FORMAT_R8G8B8A8_UNORM_SRGB`. Standard upstream behaviour.

**Blending.** Settled by the above: the sRGB backbuffer decodes the
destination to linear, blends and encodes on write. No manual linear
blending is needed.

## Alpha and premultiplied alpha audit

The interface blends **premultiplied**:

```
BGFX_STATE_BLEND_FUNC(BGFX_STATE_BLEND_ONE, BGFX_STATE_BLEND_INV_SRC_ALPHA)
```

and the texture loader picks the convention per file --
`const bool premultiplied = !opaque && extension != ".dds"` -- so `.dds` UI
textures use straight alpha while everything else is premultiplied. Two
conventions live side by side in one layer.

The open problem that follows from the hardware sRGB output:

- the backbuffer destination is **decoded to linear** before blending;
- UI textures are `RGBA8` with no `BGFX_TEXTURE_SRGB`, so the source arrives
  **gamma-encoded**;
- premultiplied blending of a gamma source against a linear destination is a
  mismatch, and shows as dark or light fringes around transparent edges.

This is the "do not mix sRGB-encoded RGB as linear values" case from the
plan. The fix is semantic, not a conversion of the assets: colour UI
textures are decoded on sample with `BGFX_TEXTURE_SRGB`. Glyph masks, coverage
maps and grayscale masks must **not** be marked sRGB -- they are data, not
colour.

Still to classify before that fix:

- font atlases and glyph coverage;
- grayscale masks;
- colour UI atlases;
- textures with transparent edges;
- textures where RGB and alpha mean different things.

## Planned sequence, one commit each

1. Audit — this file. Complete the entry point list above.
2. Decide the final output encoding, documented. **Done: hardware sRGB.**
3. Separate the catalogue layer from the 2D UI, described below.
4. Verify colour stability, compositing, post-processing isolation,
   compatibility and the single final encode.

## Step 3, corrected — isolate both halves

The first attempt failed by moving `kOsdUnder` wholesale. It carries **two**
different things:

- the catalogue **plate**, a 2D background;
- and it sits *in front of* the 3D part previews, which draw at views 8+.

Moving the layer past `kPresentView` moved the plate past the previews too, so
the plate painted over them and the meshes appeared to vanish. They were never
broken -- they were covered.

So isolating the plate alone is not enough. The 3D preview has to be isolated
as well, so that the final order can be assembled correctly after tone
mapping:

1. SDR catalogue plate;
2. tone-mapped 3D preview on top of it;
3. the rest of the 2D UI -- text, icons, tooltips.

Which means:

- the plate draws **display-referred through `kPlateView` to the window**,
  composited first. There is no separate SDR UI render target -- that was an
  earlier proposal, and it is not what was built;
- the 3D preview renders to **its own HDR target** (`preview_fb_`, RGBA16F +
  D32F), tone mapped separately;
- both are composited in order, and the remaining 2D UI draws afterwards.

The preview keeps its depth buffer, which it needs and which the window does
not have -- that is the other half of what broke.

Ordering constraint to respect throughout: the plate must end up *behind* the
preview in the composite, not merely earlier in the view list. The view list
alone does not express this once the two live in different targets.

## Constraints

Do not touch: PBR, BRDF, GGX, dynamic reflections, cubemap capture, vehicle
materials, normals, the tone mapping algorithm if ordering solves it, and the
sRGB classification of mesh textures done in the linear migration.

No separate UI render target is needed: `kOsdView` already writes to the
backbuffer over `present`, which is the simplest correct option.

Out of scope: HDR10, Rec.2020, ST.2084/PQ, Paper White, Windows Advanced
Color, an SDR/HDR user switch.

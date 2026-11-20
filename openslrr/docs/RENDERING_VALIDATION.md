# Rendering validation — what is proven, what is not

Audited at `42d795e`. Every "Verified" here is tied to a scenario that was
actually exercised on screen; everything else is marked down accordingly.

## 1. Verified in game

| Area | Scenario | Evidence |
|---|---|---|
| Colour pipeline | garage, catalogue, menus | colours match the original art; exposure moves the scene and not the interface |
| Interface routing | catalogue, garage, menus, city HUD | backgrounds no longer react to exposure; previews stay visible above the plates |
| Preview composite | catalogue | slots reset every frame; plate behind, previews above, 2D UI on top |
| Reflections | garage | body, chrome and glass show the garage environment |
| Reflection blur | garage, catalogue | visible softening of reflections; weak |
| Reflections | catalogue, part tabs | body parts, exhaust tips and glass all sample the environment |
| Reflections | inspection mode | works; the driver is excluded from the capture |
| Reflection capture lifecycle | garage -> catalogue -> garage | the captured environment survives the catalogue and is invalidated on world change |
| Full materials in previews | catalogue | glass transparent, chrome reflective, paint shows reflections |
| Pivot rotation | single part | turns and holds its angle |
| Pivot rotation | car in assembly | turns as one rigid body |
| UI text measurement | first tooltip | plate is sized correctly on first showing |
| Textures | catalogue | **interface** textures match the source art after removing the sRGB flag from them. Scene colour maps keep the flag; the two are correct independently |
| Alpha / haloes | catalogue, menus | no fringing; premultiplication enforced at load |
| City HUD | minimap, speedometer | textures do not react to exposure |

## 2. Build-verified only

These compile and are wired up, but were not exercised on screen.

- shadow passes (`fs_shadow_recv`, `vs_shadow_cast`, `vs_shadow_recv`);
- HiZ reduction (`fs_hiz.sc`);
- `fs_white.sc`;
- the non-lib backend path (`bgfx_backend.cpp`);
- MSAA toggling (`SLRR_MSAA`, `SLRR_NO_MSAA`).

## 3. Unverified

Code exists; correctness not confirmed.

- `src/resources/character.cpp` — changed, not examined.
- Long-running stability of the reflection double buffer across many scene
  changes. The invalidation path is reasoned about, not stress-tested.
- Behaviour with dynamic reflections disabled and re-enabled mid-session.
- Catalogue entered before the first cubemap capture completes. The expected
  behaviour is "no reflections, no artefacts" because `reflection_env_valid_`
  stays false; not observed.
- Ultrawide and non-16:9 aspect ratios. The preview rect maths is
  proportional but untested.

## 4. Known problems

| Problem | Status |
|---|---|
| Minimap in the city does not expand to full size | open, reported, not investigated |
| Reflection blur is weaker than intended | open, cosmetic. **The blur works in game** and is visible; it is produced by the cubemap mip chain plus `envPrefilter()` level selection, not by the separate blur pipeline. GGX prefiltering is not implemented. |
| Camera click-target mismatch (`scene_instances_pointer_ray` vs `SLRR_CAM_*`) | closed as situational: observed only in specific runs where the camera was changing |
| MSAA toggle artefact | closed: not reproducible now |
| `bind_baked_light()` binds `white_tex_` and discards its texture list | **deliberate**, not a defect -- the source comment says this avoids double-counting the sun and its shadows. The stock atlases were decoded for L1 and they are not uniform: garage/dealer `*shadow.dds` are dark masks (mean 0.19-0.37, 43-70 % of pixels below 0.10), `misc/garage/g4/t_01_shadow.dds` is a near-white multiply map (mean 0.88, 75 % above 0.97), city `*_lightmap.dds` are dark emissive masks. "The bake already contains the sun" is true for some atlases and false for others, so one verdict cannot cover all of them (`LIGHTING_AUDIT.md` §L1.8) |

## 5. Limitations of the reflection system

Stated because the system is often described as done and it is not.

- The mip chain is `AUTO_GEN_MIPS`, plain downsampling. **Not GGX
  prefiltering.** `envPrefilter()` in `fs_vehicle.sc` picks a mip level with
  `textureCubeLod` and mixes two neighbouring levels. That is the working
  reflection blur; it is weak because level selection over a downsampled chain
  is not a convolution.
- A separate blur pipeline (`fs_blur.sc`, `blur_program_`,
  `build_reflection_mips()`) is compiled but has no call site and does not run.
  Its absence does not make reflections sharp, and its presence is not the blur
  you see.
- The cubemap is **RGBA8 / LDR**. Bright sources clip. HDR capture is a
  separate task.
- Box projection covers interiors only. Outdoors is direction-only.
- Capture excludes the sky and vehicle materials by design, and the driver by
  a precise test. Anything else in the scene is captured, including
  pedestrians — intentional.
- Reflections gather in concave geometry. That is correct behaviour, not a
  defect; the fix is a layered paint BRDF, not suppression.

## 6. Limitations of the colour pipeline

- Interface is gamma straight through and scene maps are sRGB-decoded. These
  are correct independently and unifying them would break one of the two.
- The single encode depends on `BGFX_RESET_SRGB_BACKBUFFER` being set at
  `bgfx::init` **and** on every `bgfx::reset`. Changing either alone changes
  brightness across the whole image.
- Exposure is the single source of truth (Ctrl+F12). `SLRR_EXPOSURE` only
  seeds it.

## 7. Tests that would settle the open questions

None of these can be done without running the game.

1. Dynamic reflections off, then on, mid-session in the garage and the city.
2. Catalogue opened immediately after loading, before any capture.
3. Repeated garage -> city -> garage transitions, watching for stale
   reflections.
4. MSAA toggled in the garage with the overlay open.
5. Ultrawide window: preview slot placement and the tooltip plate size.
6. Long session (30+ minutes) watching for reflection or framebuffer decay.

## 8. How to reproduce the main scenarios

- Garage: `slrr_host.exe --game --window --root "E:/OpenSLR/Street Legal
  Racing - Redline" --spawn-car cars/racers/Maz3_2005.rpk:0x6`. Mazda 3 is the
  clean mesh; Focer has known model defects and is the control for model-side
  problems.
- Screenshot: `powershell -ExecutionPolicy Bypass -File E:\OpenSLR\cap.ps1`
  writes `E:\OpenSLR\shot_garage.png`.
- `SLRR_NO_HDR=1` for the legacy A/B path. `SLRR_SHOW_ALPHA=1` shows the
  preview composite's own alpha.
- Never use `SLRR_REFL_BASE` in a measurement: it forces reflection weight to
  1.0 and invalidates the test. It has contaminated A/B runs twice.

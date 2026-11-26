# OpenSLRR Enhanced

Do **not** copy this tree into the game root: the dialog calls natives that only `slrr_host` implements, and the original `StreetLegal_Redline.exe` stops with `native implementation missing for ...getRendererOption`. `slrr_host` finds the tree on its own (`SLRR_PATCH_DIR`, next to the executable, or `../patch` of the build folder) and puts it ahead of the game's class roots, so the install stays stock and the original game keeps working. The Java source ships next to the class; the host rebuilds a source that is newer than its class when it loads it.

The **Video Options** menu applies the resolution and the Enhanced settings in game. Exterior scenery range is 5 km. The scenery shadow map and the vehicle shadow map each use a resolution of 2048 × 2048. **F12 → Renderer** also sets how many reflection faces are computed per frame.

## Java natives

| Class and signature | Behavior |
| --- | --- |
| `java.game.OptionsDialog.getRendererOption(I)I` | Reads the active value for the requested identifier. An unknown identifier returns 0. |
| `java.game.OptionsDialog.setRendererOption(II)V` | Applies the value to the session. Unknown identifiers are ignored. |
| `java.game.OptionsDialog.getHandbrakeStrength()F` | Reads the handbrake moment as a multiple of the recovered law (0…5, 1 = original). |
| `java.game.OptionsDialog.setHandbrakeStrength(F)V` | Applies it, rounded to 0.1 steps and saved in `save/game/openslrr_options`. |
| `java.game.MainMenuDialog.toggleLanguage()V` | Cycles the interface language (EN→RU→PL) and re-translates live texts in place; driven by the LANGUAGE row of the main menu. |
| `java.game.CameraDialog.getFov()F` / `setFov(F)V` | F8 camera panel dialog (main chase only): read/write the chase FOV in degrees, saved in `save/game/camera_settings`. |
| `java.game.CameraDialog.getDistance()F` / `setDistance(F)V` | Same panel: chase distance in metres. |
| `java.game.CameraDialog.getLift()F` / `setLift(F)V` | Same panel: chase height above the hub in metres. |
| `java.game.CameraDialog.getTilt()F` / `setTilt(F)V` | Same panel: added base elevation in radians. |
| `java.game.CameraDialog.getReturnSpeed()F` / `setReturnSpeed(F)V` | Same panel: orbit-offset decay rate in 1/s. |

| ID | Option | Values |
| --- | --- | --- |
| 0 | Anti-aliasing | 0 off, 1 MSAA 2×, 2 MSAA 4×, 3 MSAA 8×. Other values are clamped to this range. |
| 1 | Dynamic reflections | 0 off, any non-zero value on |
| 2 | Volumetric clouds | 0 off, any non-zero value on |
| 3 | Scenery sunlight | 0 off, any non-zero value on |
| 4 | Local lights | 0 off, any non-zero value on |
| 5 | Framerate adaptive behaviour | 0 Classic (60 Hz), any non-zero value Adaptive |

The menu and ImGui share options 0–4. They reset on the next launch. Resolution, the global shadow toggle, and flares use the game's existing configuration.

## Valocity spawn fix

The patched `Valocity` script also accepts `DriverSelect` in the branch that
initializes `posStart` from `posGarage[club]`. Career `HITTHESTREET` routes
through `DriverSelect` first; without it, a fresh `Valocity` left `posStart`
unset and repeated city entries kept the car's previous transform.

## Running from OpenSLRR without touching the install

`slrr_host` looks for a patch tree (`sl/Scripts/game/src/OptionsDialog.java`, or a compiled class) through `SLRR_PATCH_DIR`, next to the executable, or in `../patch` of the build folder, and puts it ahead of the game's own class roots. Copying `sl` into the game root is only needed when the original `StreetLegal_Redline.exe` should use the patch. `--compile-scripts DIR` rebuilds a class from a shipped `.java` on demand with the same TUFA compiler the game exposes as `System.compileAll`.

## Framerate adaptive behaviour

**Advanced Game Options → Framerate Adaptive Behaviour** selects how cars are drawn between the game's fixed 60 Hz physics steps:

- **Classic (60 Hz)** draws each car at its last physics step, as before. When the frame rate is not exactly 60 FPS, car motion and turns appear stepped.
- **Adaptive**, the default, draws each car between its last two physics steps, so it moves smoothly at any frame rate. The drawn car is up to one physics step (16.7 ms) behind the simulation.

The physics is the same in both modes. The choice is saved in `save/game/openslrr_options`.

## Pedestrians

The **Pedestrian density** slider is gone. The crowd stays at the game's original maximum (1.5). Day and night keep the map coefficient.

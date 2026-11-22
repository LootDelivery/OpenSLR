# UPSTREAM DIFF — this tree against `github.com/EliasFD3S/openslrr`

Written to answer "could you document the diff with openslrr" with numbers
instead of a narrative. Nothing here is a patch: the two repositories have no
merge base (`git merge-base` finds nothing), so the only honest form is a map
from tree to tree plus a note per file.

Three trees are compared:

| | tree | what it is |
|---|---|---|
| **A** | upstream `d1c719d` | the upstream revision this tree's snapshot was audited against (`RENDERING_HISTORY.md`, `RENDERING_ARCHITECTURE.md` reference it) |
| **B** | this tree | branch `upstream-gameplay-sync` |
| **C** | upstream `main` = `640bf78c` | upstream today |

A and C are the same repository at two dates, so:

- `A -> B` is **our** work, and it is what can be taken;
- `A -> C` is **your** work since then, and it is what collides with ours;
- `C` alone is what we have never had.

## Reproduce it

```bash
git -C <this repo> archive d1c719d | tar -x -C A
git -C <this repo> archive HEAD:openslrr | tar -x -C B      # this tree
git -C <openslrr clone> archive 640bf78c | tar -x -C C
diff -ru A B > our.diff
diff -ru A C > yours.diff
```

Numbers below were produced exactly that way, with `docs/`, `assets/`,
`third_party/` and `patch/` excluded and CRLF normalised.

## Summary

| | files |
|---|---|
| added by this tree (`A -> B`) | **19** |
| modified by this tree (`A -> B`) | **87**, +9970 / -6180 lines |
| present in A, absent here | 69 |
| added upstream since A (`A -> C`) | 52 |
| modified by both (`A -> B` and `A -> C`) | **53** |

The last number is the one that matters for integration: fifty-three files where
your newer version and ours are both edits of the same original. Those cannot be
copied, only merged.

Where the bulk is, in three files:

| File | A -> B | |
|---|---|---|
| `src/render/bgfx_lib_backend.cpp` | +2073 / -375 | the whole reflection rewrite |
| `src/resources/traffic_runtime.cpp` | +1216 / -479 | ambient traffic, absent upstream |
| `src/resources/part_runtime.cpp` | +424 / -1152 | ours is the smaller side here: part assembly and paint/skin descriptors are your work, not ours |

Everything else is under six hundred lines, and most of it is under sixty.

## 1. Files this tree adds

| File | What |
|---|---|
| `include/slrr/slrr_localization.hpp` | New. Localisation interface. |
| `include/slrr/stack_sampler.hpp` | New. Profiling sampler. |
| `include/slrr/traffic_streaming.hpp` | New. Streaming interface. |
| `scripts/run_native_io_tests.ps1` | New. Native IO test runner. |
| `shaders/fs_blur.sc` | New, and never drawn: kept for its stage-2.2 design comment, replaced by the GGX chain. |
| `shaders/fs_cubeview.sc` | New. Cubemap face viewer for the dump panels. |
| `shaders/fs_hiz.sc` | New. Hi-Z reduction. |
| `shaders/fs_irradiance.sc` | New. Cosine-sampled diffuse irradiance, ninety-six taps a face. |
| `shaders/fs_prefilter.sc` | New. GGX importance-sampled prefilter, one draw per level and face. |
| `shaders/fs_present.sc` | New. Tone map (exposure + ACES) with no software gamma encode. |
| `shaders/fs_shadow_recv.sc` | New. Static shadow receiver. |
| `shaders/fs_white.sc` | New. Flat-fill helper. |
| `shaders/vs_shadow_cast.sc` | New. Static shadow caster. |
| `shaders/vs_shadow_recv.sc` | New. Static shadow receiver vertex stage. |
| `src/localization/slrr_localization.cpp` | New. Localisation string table for host-side UI text. |
| `src/platform/stack_sampler.cpp` | New. Stack sampling used by the profiling reports. |
| `src/render/null_backend.cpp` | New. A `RenderBackend` that draws nothing, so the whole boot path and the test suite run without a GPU adapter. |
| `src/resources/traffic_streaming.cpp` | New. Distance-banded streaming of the traffic templates with a fade-in, so traffic appears at the edge of the band instead of popping. |
| `tests/test_traffic_streaming.cpp` | New. Streaming band and fade tests. |

Twelve of the nineteen are shaders. `fs_present.sc`, `fs_cubeview.sc`,
`fs_hiz.sc`, `fs_white.sc`, `fs_prefilter.sc`, `fs_irradiance.sc` and the three
shadow shaders are new passes; `fs_blur.sc` is new and never drawn (kept for its
design comment, replaced by the GGX chain). The rest are the null backend, the
traffic streaming pair, the localisation table and the stack sampler.

## 2. Files this tree changes

Grouped by subsystem. The line counts are the whole diff for that file against
A, so a large count can also mean our snapshot simply predates a rewrite; the
notes say which.

### Reflections, lighting, colour

| File | A -> B | What changed |
|---|---|---|
| `src/render/bgfx_lib_backend.cpp` | +2073 / -375 | Reflection capture rewritten: direct-to-face rendering with a per-face `D24S8` attachment, the blended replay into the face, a two-slot swap, camera cadence in milliseconds, the GGX prefiltered chain (`build_ggx_cube`), the diffuse irradiance probe (`build_irradiance_cube`), the two-row dump panels, and the null-backend/present split. This is the largest single change in the diff. |
| `src/render/bgfx_lib_backend.hpp` | +274 / -36 | Declarations for the above: chain level/face/write/ready state, probe cube, `reflection_*_to_sample()`, `draw_fullscreen` with `sampler_flags`. |
| `src/render/bgfx_backend.cpp` | +758 / -2 | Backend registry split so a build without libbgfx still links (`null_backend.cpp`), plus HDR/UI present paths that only exist here. |
| `src/render/bgfx_shadows.cpp` | +747 / -7 | Static world shadow view plus per-vehicle slots, view-sequential ordering, and the fault behaviour that keeps a bad shadow target from taking the frame down. Theirs has cascades and point-light shadows in the same file — this is a hand merge, not a copy. |
| `src/render/bgfx_osd.cpp` | +430 / -194 | Interface routed out of the scene target: plates at `kPlateView`, text at `kOsdView`, preview composite between them, so UI is display-referred and no longer tone-mapped. |
| `src/render/bgfx_lighting.cpp` | +174 / -82 | Sun/ambient uniforms for the vehicle path, `u_envLod` and `u_irrParams`, the environment-debug channels, and the per-material light batch. Theirs added the point/spot light registry in the same file. |
| `src/render/slrr_font.cpp` | +377 / -42 | Own `stb_truetype` implementation with static linkage and plain `malloc`: the copy inside bgfx's imgui example allocates through the ImGui context allocator, which is not initialised before `imguiCreate`, and rasterising menu glyphs crashed there. |
| `src/render/debug_overlay.cpp` | +37 / -27 | F12 panel for the enhanced graphics options (reflection spread, physical roughness and its cap, irradiance probe, strength, scale). |
| `src/render/debug_overlay.hpp` | +11 / -3 | Accessors for the panel above. |
| `shaders/fs_vehicle.sc` | +218 / -14 | Vehicle material: layer mixing kept, plus `envPrefilter()` (physical roughness to chain level), the probe ambient mix with its self-calibrating anchor, and the environment debug channels. Theirs edited the same file for point lights. |
| `shaders/fs_mesh.sc` | +24 / -44 | World material. Differs mostly because our snapshot predates their AO/light work in this shader. |
| `shaders/vs_mesh.sc` | +0 / -9 | Instance transform space (`S*M*S`). |
| `shaders/varying.def.sc` | +2 / -0 | Varyings for the passes above. |
| `include/slrr/host_game_options.hpp` | +59 / -4 | Enhanced graphics block: reflection spread, physical roughness + cap, irradiance probe/strength/scale. |
| `include/slrr/render_backend.hpp` | +2 / -2 | Backend interface extended for the present/UI split and the null backend. |

### Resources, parts, traffic

| File | A -> B | What changed |
|---|---|---|
| `src/resources/part_runtime.cpp` | +424 / -1152 | Part assembly and render identity: `part_render_car`, driver exclusion from capture, per-part pose. Ours is smaller here — theirs carries the skin/paint-descriptor work from #206. |
| `src/resources/scene_instances.cpp` | +251 / -464 | Instance collection and submission, instance transforms, and the rule that collection must not cull for the capture. Theirs has batching/job-system submission in the same file. |
| `src/resources/traffic_runtime.cpp` | +1216 / -479 | Ambient traffic: population from the map's own requests, lifecycle and retirement by distance, lane movement along `RoadRoutes`, externally animated wheels. Not upstream work — this does not exist there. See `UPSTREAM_GAMEPLAY_PLAN.md`. |
| `src/resources/race_runtime.cpp` | +158 / -134 | Race runtime plumbing for the road graph, plus the traffic-clearing policy on race entry. |
| `src/resources/rpk_index.cpp` | +67 / -135 | RPK descriptor index and `children_of` without an arbitrary child limit; strict-resolution behaviour per `RENDER_DATA_CONTRACT.md`. |
| `src/resources/gameref.cpp` | +50 / -74 | GameRef matrix/pose plumbing, light filtering, incarnation keys used by traffic. |
| `src/resources/character.cpp` | +7 / -98 | Character/pedestrian handling; ours is smaller because driver exclusion lives in `part_runtime` here. |
| `src/resources/scx_mesh.cpp` | +3 / -21 | SCX mesh loading. |
| `src/resources/skeleton.cpp` | +0 / -18 | Skeleton loading. |
| `src/resources/scene_sun.cpp` | +1 / -1 | Sun/ambient extraction from the scene. |
| `src/resources/road_routes.cpp` | +6 / -5 | Road graph construction from `city.trc`. |
| `src/resources/ptx_image.cpp` | +13 / -0 | PTX image loading. |
| `src/resources/script_cfg.cpp` | +4 / -17 | CFG script loading for parts. |
| `include/slrr/part_runtime.hpp` | +16 / -21 | Part render identity and pose interface. |
| `include/slrr/scene_instances.hpp` | +14 / -6 | Instance collection interface. |
| `include/slrr/rpk_index.hpp` | +7 / -20 | Descriptor index interface. |
| `include/slrr/gameref.hpp` | +8 / -19 | Incarnation keys and pose accessors. |
| `include/slrr/road_routes.hpp` | +2 / -0 | Road graph queries (`findRoute`, projection, signals). |
| `include/slrr/race_runtime.hpp` | +69 / -37 | Race runtime interface for the road graph. |
| `include/slrr/scx_mesh.hpp` | +1 / -39 | SCX mesh loader interface. |
| `include/slrr/skeleton.hpp` | +2 / -15 | Skeleton interface. |
| `include/slrr/object.hpp` | +2 / -2 | Object flags used by render identity. |

### Physics

| File | A -> B | What changed |
|---|---|---|
| `src/physics/vehicle_dynamics.cpp` | +149 / -307 | Single-vehicle model with the containment shape U1-D left: configure, then swap in; a bad body disables itself instead of throwing out of the step. Theirs replaced this with the `ContactVehicle` architecture (#205/#48). |
| `src/physics/tire_model.cpp` | +45 / -83 | Tyre force model adjustments made against the same original code theirs later rewrote. |
| `include/slrr/vehicle_dynamics.hpp` | +3 / -19 | Single-vehicle model interface. |
| `include/slrr/tire_model.hpp` | +4 / -35 | Tyre model interface. |

### VM, natives, UI

| File | A -> B | What changed |
|---|---|---|
| `src/jvm/gc.cpp` | +247 / -118 | Reachability and finalizer invariants kept reachable from the VM roots; see `VM_GC.md`. Theirs rewrote the heap lookup (#200). |
| `src/jvm/tree_eval.cpp` | +108 / -121 | TREE evaluation fixes: argument and array safety, integer/float boundary conversion, `new_init` dispatch through `tree_call_by_name`. |
| `src/jvm/jvm.cpp` | +60 / -63 | Binding resolution order — a host binding is resolved before the TREE — plus alias handling; this is what U1-A depends on. |
| `src/jvm/tufa.cpp` | +45 / -30 | TUFA validation of script arguments. |
| `src/natives/natives_io.cpp` | +18 / -11 | The `Player.<init>` binding removal (U1-A): the stock TREE constructor must run, because the VM resolves a host binding before the TREE. |
| `src/natives/natives_renderref.cpp` | +44 / -41 | RenderRef natives: pose, visibility, and the traffic-facing calls. |
| `src/natives/natives_script.cpp` | +79 / -5 | Script natives: ownership and lifetime of returned references. |
| `src/natives/natives_resref.cpp` | +19 / -50 | ResourceRef natives: strict path resolution. |
| `src/natives/natives_gameref.cpp` | +5 / -21 | GameRef natives over the incarnation-keyed lookup. |
| `src/natives/natives_osd.cpp` | +19 / -9 | OSD natives, including the shown-only rule from `osd_registry.cpp`. |
| `src/natives/natives_boot.cpp` | +7 / -8 | Boot-section natives used by the `--bench-*` harness. |
| `src/natives/native_registry.cpp` | +25 / -12 | Native registration table and the alias/override order. |
| `src/natives/natives_physicsref.cpp` | +1 / -1 | PhysicsRef native fixes, including the non-finite matrix rejection. |
| `src/natives/natives_internal.hpp` | +5 / -3 | Native helper declarations. |
| `src/ui/osd_registry.cpp` | +62 / -82 | Only an OSD an owner has actually shown is drawn: a freshly constructed `Osd` draws nothing, which is what keeps the street HUD off the main menu. |
| `src/ui/ui_time.cpp` | +8 / -13 | UI clock. |
| `src/ui/osd_script.cpp` | +12 / -0 | Script-side OSD helpers. |
| `include/slrr/jvm.hpp` | +25 / -13 | VM interface: binding-before-TREE resolution and alias lookup. |
| `include/slrr/native_registry.hpp` | +0 / -2 | Registry interface. |
| `include/slrr/osd_registry.hpp` | +3 / -7 | Shown-only OSD registry. |
| `include/slrr/slrr_font.hpp` | +36 / -4 | Font atlas interface (own stb implementation). |

### Boot, platform, build

| File | A -> B | What changed |
|---|---|---|
| `src/boot/boot.cpp` | +215 / -60 | Boot/section machine: dialog-to-OSD association, `--bench-*`, `--spawn-car`, splash handling, and the bench screenshot cadence. |
| `src/boot/mainloop.cpp` | +23 / -31 | Main loop: present pacing, frame profiler scopes, per-frame reflection/chain gates. |
| `src/main.cpp` | +87 / -7 | Command-line surface used by every measurement in the docs (`--bench-map`, `--bench-drive`, `--bench-shots`, `--perf-log`, `--spawn-car`). |
| `src/platform/frame_profiler.cpp` | +86 / -56 | Per-zone CPU/GPU scopes behind `SLRR_GPU_STATS`; the per-view profiler costs ~40% of fps when left on. |
| `src/platform/video_fmv.cpp` | +62 / -51 | FMV playback safety: a failed decoder must not take the boot down. |
| `src/platform/host_sfx.cpp` | +16 / -48 | Sound-effect host with fault containment. |
| `src/platform/host_music.cpp` | +7 / -21 | Music host, same containment. |
| `src/platform/host_input.cpp` | +38 / -16 | Input mapping and focus handling. |
| `src/platform/host_game_options.cpp` | +14 / -43 | Persistence of the host options, including the enhanced graphics block. |
| `include/slrr/boot.hpp` | +10 / -2 | Boot hooks for the bench harness. |
| `include/slrr/frame_profiler.hpp` | +35 / -36 | Profiler zones. |
| `include/slrr/host_input.hpp` | +5 / -0 | Input mapping additions. |
| `include/slrr/video_fmv.hpp` | +2 / -3 | FMV container interface. |
| `src/formats/matrixdata.cpp` | +0 / -6 | Matrix data format fixes. |
| `CMakeLists.txt` | +34 / -35 | Build: shader embedding table (a shader missing from `k_fs` is a silent no-op), feature probes, the null backend, and the test list. |
| `README.md` | +6 / -33 | Host-specific build and run notes. |
| `.gitignore` | +3 / -0 | Build and capture artefacts. |
| `AGENTS.md` | +0 / -3 | Working conventions for this tree. |
| `scripts/render_native_coverage.py` | +9 / -45 | Native coverage report generator. |

### Tests

| File | A -> B | What changed |
|---|---|---|
| `tests/test_traffic_runtime.cpp` | +439 / -802 | Traffic population, lifecycle, movement and retirement tests. No upstream equivalent. |
| `tests/test_road_routes.cpp` | +251 / -217 | Road graph tests against the retail `city.trc`. |
| `tests/test_save_io.cpp` | +92 / -1 | Save IO tests; ours and theirs both moved this file. |
| `tests/test_physicsref.cpp` | +0 / -2 | PhysicsRef tests. |

### Everything else

| File | A -> B | What changed |
|---|---|---|
| `include/slrr/host_music.hpp` | +0 / -3 | One declaration removed with the music host rework. |
| `include/slrr/host_sfx.hpp` | +0 / -3 | One declaration removed with the sound-effect host rework. |

## 3. Files both sides changed since A

These are merge work. Ours is described above; for each one, start from your
current version and apply the intent, not the text. Where the intent is a
subsystem (shadows, lighting, part assembly, instance submission), the two designs
differ and a hand merge is the only option.

| File | ours | yours |
|---|---|---|
| `.gitignore` | +3 / -0 | +2 / -1 |
| `AGENTS.md` | +0 / -3 | +43 / -0 |
| `CMakeLists.txt` | +34 / -35 | +164 / -6 |
| `README.md` | +6 / -33 | +121 / -12 |
| `include/slrr/frame_profiler.hpp` | +35 / -36 | +24 / -1 |
| `include/slrr/gameref.hpp` | +8 / -19 | +2 / -0 |
| `include/slrr/host_game_options.hpp` | +59 / -4 | +5 / -0 |
| `include/slrr/host_input.hpp` | +5 / -0 | +6 / -0 |
| `include/slrr/jvm.hpp` | +25 / -13 | +14 / -0 |
| `include/slrr/osd_registry.hpp` | +3 / -7 | +3 / -0 |
| `include/slrr/part_runtime.hpp` | +16 / -21 | +53 / -4 |
| `include/slrr/scene_instances.hpp` | +14 / -6 | +79 / -2 |
| `include/slrr/scx_mesh.hpp` | +1 / -39 | +4 / -0 |
| `include/slrr/skeleton.hpp` | +2 / -15 | +7 / -0 |
| `include/slrr/slrr_font.hpp` | +36 / -4 | +18 / -2 |
| `include/slrr/tire_model.hpp` | +4 / -35 | +18 / -25 |
| `include/slrr/vehicle_dynamics.hpp` | +3 / -19 | +28 / -12 |
| `include/slrr/video_fmv.hpp` | +2 / -3 | +3 / -2 |
| `shaders/fs_mesh.sc` | +24 / -44 | +30 / -48 |
| `shaders/fs_vehicle.sc` | +218 / -14 | +49 / -19 |
| `shaders/vs_mesh.sc` | +0 / -9 | +10 / -9 |
| `src/boot/boot.cpp` | +215 / -60 | +8 / -1 |
| `src/boot/mainloop.cpp` | +23 / -31 | +119 / -1 |
| `src/jvm/jvm.cpp` | +60 / -63 | +33 / -0 |
| `src/natives/natives_boot.cpp` | +7 / -8 | +7 / -4 |
| `src/natives/natives_gameref.cpp` | +5 / -21 | +57 / -0 |
| `src/natives/natives_internal.hpp` | +5 / -3 | +15 / -3 |
| `src/natives/natives_io.cpp` | +18 / -11 | +539 / -62 |
| `src/natives/natives_renderref.cpp` | +44 / -41 | +40 / -3 |
| `src/natives/natives_resref.cpp` | +19 / -50 | +70 / -11 |
| `src/natives/natives_script.cpp` | +79 / -5 | +14 / -0 |
| `src/physics/tire_model.cpp` | +45 / -83 | +46 / -30 |
| `src/physics/vehicle_dynamics.cpp` | +149 / -307 | +272 / -87 |
| `src/platform/frame_profiler.cpp` | +86 / -56 | +10 / -1 |
| `src/platform/host_game_options.cpp` | +14 / -43 | +26 / -2 |
| `src/platform/host_music.cpp` | +7 / -21 | +28 / -108 |
| `src/platform/host_sfx.cpp` | +16 / -48 | +52 / -110 |
| `src/platform/video_fmv.cpp` | +62 / -51 | +34 / -3 |
| `src/render/bgfx_lib_backend.cpp` | +2073 / -375 | +730 / -66 |
| `src/render/bgfx_lib_backend.hpp` | +274 / -36 | +423 / -13 |
| `src/render/bgfx_lighting.cpp` | +174 / -82 | +642 / -134 |
| `src/render/bgfx_osd.cpp` | +430 / -194 | +149 / -86 |
| `src/render/bgfx_shadows.cpp` | +747 / -7 | +1423 / -169 |
| `src/render/debug_overlay.cpp` | +37 / -27 | +461 / -16 |
| `src/render/slrr_font.cpp` | +377 / -42 | +53 / -0 |
| `src/resources/character.cpp` | +7 / -98 | +19 / -6 |
| `src/resources/gameref.cpp` | +50 / -74 | +35 / -7 |
| `src/resources/part_runtime.cpp` | +424 / -1152 | +822 / -152 |
| `src/resources/ptx_image.cpp` | +13 / -0 | +22 / -0 |
| `src/resources/scene_instances.cpp` | +251 / -464 | +703 / -137 |
| `src/resources/scene_sun.cpp` | +1 / -1 | +18 / -1 |
| `src/ui/osd_registry.cpp` | +62 / -82 | +10 / -2 |
| `tests/test_save_io.cpp` | +92 / -1 | +452 / -0 |

## 4. Present in A, absent here

Sixty-nine files. They are not omissions to fix on our side and they are not in
this tree: worker-thread plumbing (`job_system`), contact physics
(`vehicle_contacts`, `mass_properties`, `wheel_mount`, `flap_hinge`), the race
brain, OSD text batching, and the upstream test suite that goes with them. If a
name from your tree is missing here, it is almost certainly one of these.

## 5. New upstream since A

Fifty-two files, none of which exist here: the AO pass (`fs_ao.sc`, `vs_ao.sc`,
`bgfx_ao.cpp`, `ambient_occlusion.sh`), point and spot shadows
(`point_shadows.hpp`, `scene_shadow.sh`, `vs_shadow.sc`,
`shadow_cascades.hpp`, `shadow_quality.hpp`), the job system, vehicle contacts
and its tests, the JSON save format, Cinepak, the OSD text batcher, the shared
shaderc cache, and about sixty tests. They are listed here so that nobody reading
the diff mistakes "absent" for "removed".

## 6. What the diff is not

It is not an argument about which tree is ahead. Upstream is ahead in shadows,
AO, worker threads, vehicle contacts, video and testing; this tree is ahead in the
reflection chain, the diffuse probe, interface/HDR separation and ambient traffic.
The two sets are independent, which is why they collide in only fifty-three files.

Two things in this tree are deliberately not offered for integration: the
instanced vehicle submission (reverted, `bcbc4f1b` — the GPU cost rose from 3.59
to 5.77 ms for the same triangles) and the box-blur reflection chain
(`build_reflection_mips()`, no call site). Both are documented so they are not
re-attempted blind.

## 7. Traps specific to the reflection work

Four of the defects behind item 2 cost a full debugging pass each because they
fail silently. They are recorded here because they apply to any implementation of
this pass, not only to ours.

1. **`bgfx::touch(view)` discards the encoder.** It is `discard()` plus an empty
   submit, so uniforms set before it are gone. Setting the per-face basis first
   gave zero vectors, `normalize` of nothing, NaN, and every convolution wrote one
   flat colour — on screen, reflections that vanished a third of a second after
   start. `touch` first, then `setUniform`.
2. **A convolution needs a linear sampler.** Point sampling is right for
   one-texel copies and destructive for a prefilter: twelve point taps of a 512²
   mip are twelve arbitrary texels, different every cycle. In bgfx linear is the
   *absence* of `MIN_POINT`/`MAG_POINT`.
3. **Sample at the scale you integrate.** Level `m` reads capture mip `m-1`; the
   probe reads a mip whose texels are the size of a probe face.
4. **The face basis is the capture's own lookAt rows with the v axis negated.**
   Cross products come out a mirror image of what the capture wrote. Do not test
   this by brightness: a swapped pair of faces carries nearly the same mean, so
   comparing means passes an upside-down chain. Compare the image — share of sky
   pixels in the top half against the bottom half of each face.
5. **Roughness to level must be physical.** `1 - specularExponent/24` is a
   fresnel/occlusion normalisation, not a roughness; with it all paint landed
   below level 1, back on the capture, and the chain looked like it did nothing.
   `alpha = sqrt(2/(e+2))`, `lod = log2(alpha/0.02) + bias`.
6. **A chain cycle must fit in one gate.** Spreading thirty-six faces over frames
   makes the published cube hold ~50 ms and jump, which on a smooth body reads as
   a framerate drop, not as lag.
7. **The probe's absolute level is not a constant.** It is an integral of a
   captured image, and with a gain derived from sRGB-encoded 8-bit averages it
   lands a factor of 1.5 the wrong way. Anchoring it in the shader —
   `u_sunAmb / probe(up)` — makes switching it on a change of shape rather than
   of brightness, which is what allows it to be on by default here.

## 8. Where the detail lives

`RENDER_FOUNDATION_PLAN.md` 8.5-8.14 is the engineering record: each defect with
its measurement, and the reasoning that turned out to be wrong.
`UPSTREAM_INTEGRATION.md` covers the opposite direction: which upstream work was
ported here, which was deferred and which was rejected, with the reason each.


# OpenSLRR backlog / known issues

Working bug tracker. Each entry: symptom, evidence, suspect, status.
Move fixed items to the bottom with the fixing commit.

## Known defects

### STB-1 Crashes on scene transitions
- **Symptom:** the game crashes on various scene transitions: garage -> map,
  -> shop, shop -> back, double-clicking the map chooser, and others.
- **Timing:** began before the tyre work; unrelated to the tyre port (the
  full rollback to `09b209f0` did not change it).
- **Evidence gathered:** `SectionMachine::change_active_section`
  (`section_machine.cpp:130`) notifies Java only; no native per-scene state
  is reset. Proof that no unload hook exists at all: `fade_table()`
  (`scene_instances.cpp:1783`) is keyed by raw pointers and is never cleared,
  so after a scene change new objects allocated at the same addresses inherit
  a dead object's fade. Cleared of suspicion (read-only): `mesh_span` cache
  (keyed by resource id, value copied), traffic `warm_ground` (raw pointer
  survives a scene, but only ever compared, never dereferenced),
  `frontend_scripts` cache (built once, affects a second game start, not
  transitions).
- **Crash log (done, `--crash-test` proves it):** an unhandled exception, a
  terminate, a pure-call and an invalid CRT parameter now write
  `build\Release\logs\crash-<date>-<time>-<pid>.log` plus a `.dmp` next to
  it: exception code and address, already-symbolised stack with source lines
  (the Release build now carries its PDB), the loaded modules, and the last 64
  host events -- section exits/enters, the spawn. `slrr_host --crash-test`
  faulting on purpose writes a report that resolves `run_host` to
  `main.cpp:203`, which is the whole point: a stack of RVAs is useless after a
  rebuild, and that is exactly what the old log was.
- **What the old log already said:** `slrr_host-crash.log` from 2026-11-24
  23:13 holds 11 entries -- one C++ `std::_Xout_of_range` and ten access
  violations -- all raised from the same place: a recursive script/VM invoke
  chain (`+0x1924FB` repeating) with nothing of ours below it. No PDB existed
  for that build, so the names are gone for good; the shapes are not, and they
  say the faulting code is reached through Java calls, not through a transition
  hook -- which fits a use-after-GC or a stale object rather than a dangling
  scene pointer.
- **The signed report (2026-11-25 16:06, entering ValoCity):** the stack names
  the fault, and it is the used-after-GC shape this entry predicted:
  `tree_eval.cpp:460` -- `recv->fields.find()` inside a `getfield` -- with
  `Jvm::gc_safe_point` (`gc.cpp:45`) below it, which is the line that runs
  `finalize` on a queued dead object. The fault address is `0x18`, a
  `unordered_map` read from a null base: the receiver was not object memory any
  more.
- **The window:** a finalizer queued by one collection was kept alive only for
  that cycle (`o->gc_marked = true`, `gc.cpp:176`). The queue is drained with a
  1.5 ms budget, so 96 finalizers took three safe points to run, and a
  collection in between clears every mark and sweeps the queued object and its
  field graph, leaving the queue holding freed pointers. Measured during a city
  load: one collection queued 96, the next safe points ran 58 then 38 -- a
  single frame in between is enough.
- **Fix:** queued objects are marked as roots at every collection
  (`mark_roots`, `gc.cpp`), so their graphs outlive the queue, and a queued
  pointer that is no longer heap memory is skipped with a `[gc-finalize]` line
  instead of being called. A 180 s city drive runs without the line ever
  printing.
- **Next:** a long live session across map changes, then read the report. The
  scene unload hook still comes after a signed report, not before.
- **Status:** cause identified and closed (`gc.cpp`); awaiting a session long
  enough to call it gone.

### STB-2 Second spawn lands at a different point
- **Symptom:** in ValoCity the player can spawn twice in a row; the first
  spawn is by the garage, the second somewhere else. Reproduced by pressing
  the map-chooser transition twice.
- **Suspect:** same class as STB-1 -- spawn selection reads state that did
  not reset on the previous scene exit.
- **Status:** open.

### GFX-D Driver triangle (fixed)
- **Symptom:** a black triangle stretched from the driver model downward, one
  vertex pulled toward the ground. Present on several cars; the car pivoted
  around it only when the tyre port was active.
- **Root cause, proved:** our SCX loader, not the mesh. `parse_v4` initialised
  the skin arrays lazily at the first skinned vertex; with `prior == 0` the
  `assign` left the vectors empty, the append gated on non-emptiness skipped
  vertex 0, and the second vertex's init wrote vertex 0's slots as zeros. A
  zero-weight vertex keeps its bind position forever. `vin.SCX`'s vertex 0
  sits at the right foot, so when our leg IK raised the feet to the pedals the
  triangles around it stretched ~44 cm -- the sliver. Stock never posed the
  legs, so the latent loader defect stayed invisible until ours exposed it.
- **Evidence chain:** the mesh file is clean (0 degenerate, weights sum to 1,
  bone ids in range); the loader probe showed vertex 0 skinned `[1,0,0,0] / [4,
  4,4,4]` inside `parse_v4` while the returned geometry carried zeros there;
  `SLRR_HIDE_DRIVER=1` removed the sliver, proving the driver draw was the
  source; the OBJ dump of the posed driver showed vertex 0 as the single
  anomaly (84 cm edge, 4.8 cm thickness).
- **Fix:** `skin_started` flag replaces the emptiness test, so the first
  skinned vertex appends its own slots. After the fix the cached geometry has
  `v0 w=1.0 bone=4` and the skin trace is silent.
- **Instruments kept (all off by default):** `SLRR_SKIN_TRACE` (names any
  vertex that is non-finite or does not travel with its dominant bone, also
  logs to `slrr_skin_trace.log`), `SLRR_SKIN_DUMP` (OBJ dump of the posed
  driver and its bind), `SLRR_HIDE_DRIVER` (skips drawing the seated driver).
- **Status:** fixed; awaiting the driver's confirmation in game.

### GFX-E Red inner fender
- **Symptom:** the inner fender renders bright red (a material default),
  most visible on the Mazda wheel-arch shot.
- **Owner:** the car model itself (user confirmed); not a render defect.
  Recorded so nobody hunts for it in the renderer.
- **Status:** open, belongs to the model.

- [x] **The handbrake did nothing at all.** Three layers, each found only by
      measuring: no shipped wheel script writes `hbrake`, so every car's per-wheel
      torque was 0; the fallback used the rear brake moment unscaled; and the input
      was capped at `0.3` by the first import with no provenance. Fixing the first
      layer made the *stand* work (60 -> 0.6 km/h) while the game stayed dead,
      because the stand sets the pedal to 1.0 and the game used .3: 0.3*3270 = 981
      N m against a ~1700 N m rear grip limit cannot lock anything. The driver kept
      reporting it, correctly. Now `handbrake = rear brake * handbrake_power/brake_power`
      (`brakepower 1000 2000` on every racer, and the live rear wheel reads 0xDC
      6540.775 against 0xD8 3270.387 -- exactly 2x) with the button pedal at 1.0.
      Guarded by `the_handbrake_locks_the_rears_and_holds_far_beyond_the_pedal_hack`
      (66 -> 43 km/h in 1 s, rears 0.00 rad/s, fronts 38.8).
      Under throttle the law is a torque balance, not a mode: the engine can
      out-pull the brake above ~1010 N m in first gear on an MC, which is what
      the original does too. Two places set it -- Ctrl+F12 -> Tuning -> Driving
      and the `HANDBRAKE STRENGTH` row on the GAME OPTIONS page (the patch class
      in `patch/` calls `getHandbrakeStrength`/`setHandbrakeStrength`) -- 0..5 in
      0.1 steps, **default 1.4**, saved as `handbrake=` in
      `save/game/openslrr_options` rather than in the fixed-layout Config, so it
      survives a restart. It scales the moment, so feel and a future hydraulic
      mod can be judged without touching the data; 1 is the recovered law, 0
      disables the handbrake including the parked hold. `the_handbrake_strength_scales_the_
      recovered_moment` pins 0/1/2 with the throttle held down (rears 51.8 /
      33.1 / 0.0 rad/s after 0.5 s). Only the rear wheels get the moment; the
      smaller effect on acceleration is their locked-tyre drag, measured at
      -4860 N per rear wheel on an FWD car, and the three aids the pedal also
      switches off (launch aid, auto-clutch launch, steering help).

### SHDW-1 A dark car-shaped blot trails the car and fades after a second
- **Symptom:** a soft, elongated dark stain on the asphalt a few metres behind
  or beside the car, shaped like a car seen from above; it moves along with the
  car and vanishes a moment later. The driver found that switching *Map
  shadows* off in the debug overlay removes it, which points at the cached map
  shadow, not at the vehicle shadow.
- **Cause (measured):** the cached map is rebuilt only when the camera has moved
  `step = max(8, range*.15)` metres (8.4 m in the city). Every rebuild draws
  the player's own car body into the map: the caster filter skips
  `instance.vehicle_material`, but the body instance of the driven car reaches
  the pass with that flag clear. `SLRR_SHADOW_TRACE=1` prints, per rebuild,
  `cars=1 car=0x0023000c at <x,y,z>` with the position identical to the car's
  own `[route] pos` at that instant. The map then keeps that copy while the car
  drives on, so the stale silhouette stays on the road for up to 8.4 m of
  travel -- at 10-30 km/h about one second, exactly the observed fade -- and it
  reappears at a new spot with the next rebuild, which reads as a shadow that
  follows the car.
- **Fix:** a caster that resolves to a car (`part_shadow_source`) is skipped in
  the cached map; cars keep their own depth map. `SLRR_SHADOW_KEEP_CARS=1`
  restores the old behaviour for A/B runs. A/B on the same 14 s ValoCity route
  (`carAB-keep` / `carAB-fixed`) differs exactly where the stale copies lie on
  the road behind the car, and the car's real shadow is unchanged.
- **Status:** fixed, awaiting a live run.
## Awaiting driver verification

Code-level evidence is not acceptance. Items here are implemented, tested or
measured, and have not been checked by driving the game. Each one names what to
look at, so the check takes a minute rather than a session.

- [x] **Traffic stops at a red light** (U2-C). Confirmed by the driver: cars
      stop on red, go on green. Three rounds of measurement got there. The first
      in-game run said 1 of 30 red approaches rested, the rest drove through: the
      cap used a 5 m/s² braking curve while the speed could only fall at 4 m/s²,
      so every car overshot the line and was released as committed. Fixing the
      response rate left 570 of 613 resting cars pulling away again while the lamp
      was still red (`released`, added to the signal trace): the cap is a target
      speed, so it approaches zero without ever reaching it, the car crawls over
      the line at about 1 m/s, and the next step reads it as committed. A lamp that
      says stop is now a wall at the line, not only a speed target
      (`traffic_runtime.cpp`), after which no resting car leaves on red and 68 of
      357 red approaches are driven through, all of them entered too fast to stop.
      Note for later: every crossing in the city switches at the same instant
      (one clock, `RoadRoutes::signal`), which is why the mean speed drops to
      21 km/h where the cheating version held 38; a per-crossing offset would
      look like a real city and is not recovered yet.
- [ ] **Reflections after the irradiance probe became default-on** (`90ff7dcf`).
      Verified on paint, chrome and glass shots; the driver has seen it once and
      approved, and this entry exists so the state is not forgotten.
- [ ] **Reflection roughness default 1.5** (`ded44b59`). Approved once ("1.5
      fine"); revisit if the body ever reads as film rather than paint.

## Performance

### PERF-1 Streaming hitch: object/texture upload stalls (fixed)
- **Symptom:** fps drops to 40-50 in specific city places (always the same
  spots). Frame 20-23 ms while GPU shows 6.7-7.5 ms total; flush and present
  both wait. `batch_instances` changes right around the drop (props streaming
  in/out).
- **Fix (dad51aa):** frame-budgeted GPU uploads in
  `LibBgfxBackend::sync_instances` + `flush_pending_uploads()`
  (8 MB geometry / 6 textures per frame; `pending_uploads_` queue);
  `load_part_textures` now returns int and participates in the budget.
  Bench: avg 7.56 → 1.82 ms, p95 22 → 2.87 ms.
- **Status:** fixed. Objects appear one frame later (user approved).

### PERF-5 Windowed present pacing: beats / freezes / judder (fixed)
- **Symptom history:** vsync-on frames alternated 13.3 / 26-33 ms with flat
  GPU work (DWM composition "beats"); later, with the two-buffer chain,
  vsync-on Present(1) alternated one/two composition periods
  (13.3/26.6 ms) — perceived as freezes at 75 fps; vsync off uncapped
  juddered because the present queue drains at the composition rate
  (11% of frames blocked 16-30 ms).
- **Root cause:** two back buffers make the flip queue empty and refill
  every frame, so Present(1) sometimes waits a second composition period.
- **Fix (f986f51):** three back buffers (`numBackBuffers = 3`). VSync on:
  p99 27.4 → 13.5 ms, frames >20 ms 2% → 0.1%. VSync off uncapped:
  blocks 11% → 1.3% (remaining blocks are DWM composed-flip drain; the
  engine-level cure is independent flip / exclusive fullscreen — this bgfx
  has no SetFullscreenState).
- **Also:** the per-view GPU profiler (`BGFX_DEBUG_PROFILER`) cost ~40% fps
  (300 vs 500+ uncapped); now behind `SLRR_GPU_STATS=1`.
- **Status:** fixed with vsync on (user-tested smooth). VSync off keeps
  ~1.3% composition-paced blocks; `--fps-cap` remains for benches.

### PERF-6 JVM GC pauses (major part of the old "spikes")
- **Symptom:** rare 27-39 ms full-stop hitches, place-correlated (allocation
  pressure). The old vsync stutter masked them.
- **Root cause 1 (fixed):** the finalizer scan ran an inherited-method
  search per dead object (~8k/cycle). Cached per class -> GC is now 7-9 ms.
- **Root cause 2 (fixed):** ~8k temporary objects per cycle are
  Vector3/Ypr churn from the wheel/camera update paths (PhysicsRef.getOri /
  getPos / getAngVel allocate fresh each call). The collector now pools
  those two field-bag classes (recycled on allocation, bounded 8192/class),
  which covers ~99.9% of the churn.
- **Also:** threshold counts all heap insertions (fresh + pooled pops) and
  scales with the live set, the heap registry is maintained incrementally,
  dead objects are destroyed gradually (256/frame), state resets happen at
  pop time, not in the pause.
- **Status:** fixed for now: pauses 27-39 ms -> ~4.5 ms every ~4.4 s. If
  still perceptible, the next step is a proper nursery (generational GC).

### PERF-2 3-second stall when the city loads
- **Symptom:** one frame takes 1931-4304 ms around city entry.
- **Evidence:** `spike_hunt.csv` seconds 2-3, 7.
- **Suspect:** synchronous rpk/SCX load batch (city pack + garage preloads).
- **Status:** open (user said observe, don't fix yet). (M3.)

### PERF-3 Backbuffer MSAA cost at high resolution
- **Symptom:** MSAA 2× costs ~3 ms/frame average at 1440p and roughly doubles
  the frame-time tail (p95 22 → 12 ms when off). A/B with
  `SLRR_NO_MSAA=1`: `bench_city.csv` vs `bench_city_nomsaa.csv`.
- **Cause:** our MSAA is **not an in-engine MSAA**. It is driver-level
  backbuffer MSAA (`BGFX_RESET_MSAA_*` in `LibBgfxBackend`): the driver
  resolves the whole swap chain every present, and the engine cannot pace or
  optimize that. In-engine MSAA (render into an MSAA target with an
  explicit, paced resolve) is the "real" MSAA and is not implemented; the
  scene-target scaffolding exists in `ensure_scene_target` but is dead code.
- **Resolved state:** after the upload/GC/vsync fixes the original x2 cost
  estimate proved to be an artifact of queue backpressure; x4 is now the
  default (user-tested: 350-500 fps uncapped, no visible cost on the RTX
  4070 Ti at 1440p). `SLRR_MSAA=0..3` env switch for experiments.
- **Status:** closed as a perf issue. The in-engine MSAA scaffold (GFX-1)
  remains open as a quality/flexibility item.

### PERF-4 Bench-mode measurement quirks
- **Symptom:** `--bench-drive` with `--no-vsync` advances the simulation
  1/60 s per frame; at 400+ fps the run time-warps ~7× (the car "drove like a
  rocket" and the 90 s drive took ~13 s wall clock). Live `--perf-log`
  sessions with vsync off also degrade the play experience.
- **Fix direction:** cap fps during bench runs so sim time == wall time, or
  measure with per-frame CSV.
- **Status:** open. Bench A/B env switches: `SLRR_NO_MSAA`,
  `SLRR_NO_REFLECTIONS`, `SLRR_NO_VOLUMETRIC` (all `=1`), see `boot.cpp`.

## Rendering

### GFX-1 Present program fails to link since the first build (fixed)
- **Symptom:** `[bgfx] program present failed to link` and `hiz failed to
  link` on every start (all logs, old and new).
- **Impact:** the offscreen scene target + present pass (`ensure_scene_target`,
  `present_scene`, HiZ pyramid) can never run. Blocks ALL post-processing:
  FXAA, bloom, future PBR post passes.
- **Root cause (fixed, e353131):** this bgfx version requires
  `vs.m_hashOut == fs.m_hashIn` in createProgram - the fragment shader must
  declare every varying the vertex shader outputs. `fs_present`/`fs_hiz`
  declared only `v_texcoord0`. Declaring the full vs_mesh varying list fixed
  the link.
- **Status:** fixed. Unlocks the scene target for HDR/tonemap (PBR plan
  step 2). HiZ pyramid still unconnected (views 110+, dead code).

### GFX-2 Alpha textures in the city partially broken
- **Symptom:** alpha works on trees but not on bridges/buildings (alpha parts
  render opaque or invisible).
- **Suspect:** the alpha-test path (`u_alpha_test_`) vs the blend path for
  static world groups; city meshes may use a different material flag than
  vegetation.
- **Status:** open.

### GFX-3 Jagged edges beyond alpha-test
- **Symptom:** alpha-test edges (trees/fences) stair-step at any MSAA level
  (by design), and the user also reports stair-stepping on car body
  geometry edges at MSAA x4 - which MSAA should smooth.
- **Geometry case (fixed, 26f8fb1):** the scene-target MSAA never engaged -
  the RT_MSAA_* flags are 64-bit but the level array was declared uint32_t,
  truncating every entry to zero. With uint64_t the x8/off screenshots
  differ on exactly the geometric edges (pixel diff verified). This also
  retroactively explains the tiny x2-vs-off perf delta.
- **Alpha case (open):** alpha-tested edges (trees/fences/foliage) still
  stair-step at any MSAA level, and leaves look thinner under MSAA (resolve
  fade). Fix direction: alpha-to-coverage or manual alpha supersampling in
  the shader.
- **Status:** geometry fixed; alpha case open (low priority).

## Driving

### DRV-10 The OSD indicator icons (abs/asr): closed -- lamps drawn in panel space (2026-11-25)
- **The commands are lamp declarations, not published values.** The car config
  parser table at `0x60E3D0` pairs each command with its handler: `osd_abs` ->
  `FUN_00445520`, `osd_asr` -> `FUN_004455b0`, `osd_hbrake` -> `FUN_00445640`,
  `osd_emergency` -> `FUN_004456d0`, `osd_damage_slot` -> `FUN_004450e0`; the
  handlers parse `%d %f %f %f` (formats `0x60F874` / `0x60F894` / `0x60F8B4`)
  and build the element with `FUN_00544590` + `FUN_00546070(handle, 0xE, 0)`.
- **`0xFFFFFFFF` is "no car specific render".** No `led_*` string exists in the
  exe, so the lamp is not named from code; the stock LEDs are the only lamp
  meshes the package ships. Verified from `cars.rpk` (pack 31): `led_ABS` 0x5D,
  `led_ASR` 0x5E, `led_HB` 0x5F, `led_ER` 0x60 are `typeof 5` (model entries)
  and 0x61..0x64 repeat the four as `typeof 14`; those four are the only ones
  whose model resolves the panel atlas (`cars/textures/LongOSDGauge.png`), and
  0x61./0x62./0x63 resolve to `ABS_led.scx` / `ASR_led.scx` / `HB_led.scx`.
- **The lamps live in the panel's pixel space, anchored to the bottom-right
  corner of the screen.** `newosd` (0x55) spans x -502..0, y 0..99.2; the LED
  quads are 26x17 px at x -499.19 (ABS) / -471.85 (ASR) / -444.89 (HB) /
  -416.80 (ER), y 0.69..17.42. The mapping was then measured against a frame
  of the shipped HUD, where the game itself paints the four unlit labels. Panel
  units map to clip as `x = .649 + px*.00209`, `y = .993 - py*.003915` (OSD
  clip is x right, y **down**), so the row covers 30..39% of the width along
  the bottom edge, one icon 0.055 wide and 0.065 tall, which is the label's
  own box -- the labels are what pinpoints the transform.
- **What was broken in the port, three times over.**
 The name table knew only
  `osd_hbrake` of the four lamps; a `0xFFFFFFFF` id became resource 0, which
  `draw_vehicle_hud` skipped on its first line; and the LED quads were drawn
  with the generic HUD transform (`xscale*position_scale`, position (0,0)),
  which put a 26 px icon ~7 screen-widths to the left, so even the handbrake
  lamp was invisible. Fixed: `HudSlot::Abs`/`Asr`, built-in LED substitution
  (0x61 / 0x62 / 0x63 for HB), a dedicated panel-space draw, and
  `VehicleHudState::abs/asr` fed from `drive.abs_active/asr_active`.
- **Regression test:** `test_hud_lamps` pins the three ids, the atlas and the
  clip-space row (`x -0.394..-0.216`, `y 0.925..0.990` -- the bottom edge, over
  the labels, at the label's aspect), so the row cannot drift off screen again
  without a failing test.
- **Final placement is baked into the renderer, not read from a file.** The row
  was lined up on screen against the game's own labels (the label pitch is a
  touch wider than the mesh pitch) and those numbers now live in
  `draw_vehicle_hud` as `kLampX[] = {.649, .652, .659}`, `kLedY = .993`,
  `kLedSx = .00209`, `kLedSy = .003915` (0.9 of the measured height, the label's
  aspect): no start-up file, nothing to keep in sync with the install.
  `osd_abs/asr/hbrake` still honour a non-zero cfg offset if a car ever sets one.
  `rebuild_openslrr.bat` at the repo root just closes the game (it locks the
  exe), rebuilds `slrr_host` and waits, so a code change needs one double click.
- **Status:** closed. The lamps are drawn only while their aid intervenes,
  which is the original rule: ASR on a hard launch, ABS only on a locking
  wheel (stock cars also ship `abs 0`).
### DRV-11 ASR: closed -- law, clutch press and launch behaviour verified (2026-11-25)
- **The law and its writer are fully recovered:** `FUN_0044e120` reads the mean
  `|wheel+0x14C|` over enabled, driven, contacting wheels (`piVar5[0x27]` is
  wheel+0xCC, the drive share), computes `x0 = mean - 0.2*inv`, `x = (x0 -
  car+0x1F50)*2 + x0`, and for `x > 0` sets `service = max(0, 1 - x/(0.3*inv))`,
  applies `throttle *= service` and `car+0x1DBC = 1-(1-car+0x1DBC)*service`.
  Constants verified byte for byte from `0x60E02C..0x60E048` (1.0, 1.0, 0.3,
  0.5, 0.1, 0.1, 0.1, 0.2). Port: `DriveInput`/`vehicle_dynamics.cpp` ASR block.
- **The field is wheel+0x14C** (`0x30 + 0x47*4`; an earlier `0x11C` reading
  forgot the `+0x30` base) -- the accumulated slip the tyre filter keeps, i.e.
  our `slip_state.slip`. Nothing else has to move: the scale was right.
- **The "bogs to 0.25 m/s" measurement was wrong, twice over:**
  (a) the fixture called `run(3, 1, 0, 0)`, which sets the throttle ONCE and
  lets the law's own `throttle *= service` compound over the next 179 frames,
  so the pedal decayed to zero on its own; (b) it ran the HOST driveline
  (`faithful = false`), whose invented 0.5 s launch aid holds the driven wheels
  at zero spin while the engine revs 873 -> 2733 rpm, so the mean slip jumps
  0.00 -> 0.54 and lands outside the law's 0.3..0.5 band by construction. The
  player never takes that path: `host_game_options.cpp:100` defaults
  `player_originaldrive` to 1, and the driver's `save/game/openslrr_options`
  carries no override (only `framerate_mode` and `handbrake`).
- **Corrected measurement (faithful driveline, held pedal, 3 s from rest):**
  ASR off -> **17.62 m/s**; ASR strength 1 -> **3.74 m/s**, mean driven slip
  **0.40** (inside the band), service **0.256**, speed climbing monotonically
  (1.45 m/s at 1 s, 2.61 at 2 s, 3.53 at 2.8 s) while the engine rises
  790 -> 1627 rpm. No limit cycle, no stall: a partial proportional cut, which
  is exactly the driver's reading of the original ("the revs come up slowly and
  smoothly, as if I am pressing the pedal slowly") and the car still pulls away.
  The same service value presses the clutch to 0.256 --
  `open' = 1-(1-0)*0.256`, a quarter-pressed pedal, exactly `0x44E271:41`.
- **Pinned by `test_vehicle_dynamics`** (faithful + held pedal): the press is a
  0..1 multiplier below 0.9, ASR slows the launch and still leaves the car
  moving (> 2 m/s in 3 s). 39/39.
- **Status:** closed. The only ASR item still open in the port is publishing the
  lamp value (DRV-10). Do not retune the 0.2/0.3/0.4 thresholds to chase a
  behaviour difference.
### DRV-9 The car creeps downhill on the handbrake with locked wheels
- **Reported by the driver (2026-11-24):** on a slope with the handbrake on
  and the wheels visually locked, the car still creeps downhill. The driver
  states the original game does the same, so this is original behaviour, not
  a port defect. Recorded so it is not "fixed" as a deviation, and so the
  mechanism is documented: our rest-contact hold caps the impulse by the
  tyre's peak, and the brake torque at rest competes with gravity through
  `braking_at_rest`; a slope steeper than the balance creeps in both.
- **If a fix is ever wanted** (a deviation from the original would be needed):
  raise the static brake hold or the rest-contact friction for handbraked
  wheels. Not scheduled.
- **Status:** open, no action.

### DRV-8 A braked rear wheel hovers below rolling in a braking slide
- **Symptom:** the driver's MC GT trace (2026-11-24): in braking slides
  (brk 0.90, gas 0.08) both rear wheels are held near zero while the road
  moves at 8+ m/s — w2 at 2.6-3.0 rad/s (load 1375-1497 N, Flong -1650..-1828),
  w3 at 10-11 rad/s (load 5100-5400 N, Flong -6100..-6700). The lighter wheel
  visually "stops", the heavier one creeps — the reported "outer wheel stops
  when it lifts". The exchange is pushing both up (+3.7/step at engine 1846
  rpm), the tyre is spinning both up, so the holder is the brake path
  balanced against them.
- **CORRECTED 2026-11-25: the ABS suspect below is stale and wrong.** The ABS was
  recovered on 2026-11-24 and ported (`b37a98e7`): `FUN_0044df70`, whole-car pedal
  modulation (mean of the braked loaded wheels' accumulated slip, thresholds
  0.2/0.4 on `1/abs`, blend `(x-prev)*4+x`, memory `car+0x1F54`, icon 0x1000000 under
  service < 0.9) -- CAR_MODEL_RECOVERY 5.3b', pinned by
  `abs_releases_a_locked_wheel_while_the_road_moves`. ASR (`FUN_0044e120`) is ported
  too (`ef5e2206`), only its clutch write is deferred (`c53f5d26`) and that does not
  act on braking. Our invented per-wheel `|road|/r*0.85` branch is gone. So the hover
  is NOT a missing ABS branch: it survives the whole recovered brake chain (brake/tyre
  drag steps on `EC`, bearing, rolling force, static hold), and it is not reproduced by
  the bench (8 frames of 480 below 1 rad/s). What would settle it is a live trace of
  the original.
- **Bench, 2026-11-25:** the 105-degree slide fixture does NOT reproduce the hover --
  its light driven wheel falls below 1 rad/s for 8 frames out of 480 and climbs back.
  So the 10+ frame hover is a live-only observation, and the bench cannot settle it.
- **Next step:** recover the original's ABS branch (search 0x0044xxxx/
  0x00454500 callers for the brake path) or, if the original has none for
  these cars, establish what its braked-slide wheel actually does from a live
  trace before touching ours -- that trace needs the original driven by hand, so it
  is the driver's call when to run it (`diag_car.ps1` reads the live wheel block).
- **Status:** open.

### DRV-7 A driven wheel pinned at zero in a long slide (open diff)
- **Symptom:** one driven rear wheel hovers near zero while its twin burns at
  twice case speed. WHICH wheel pins depends on the situation: the driver's
  live trace (MC GT, 2026-11-24) pins the LIGHTLY-loaded one (w2, load
  1375-1497 N, spin 2.6-3.0 rad/s under an 8 m/s road, 10+ frames, engine
  7000 rpm, link engaged) while the heavily-loaded w3 (5100-5400 N) burns —
  exactly what the driver reported. The 105-degree slide fixture (62 km/h,
  throttle 1.0) pins the LOADED one (load 5690) with the unloaded twin at
  95-105 rad/s. Both are the same balance: the exchange's share versus the
  wheel's own tyre force; the fixture alone is not the driver's case and must
  not stand in for it.
- **Recovered so far (0x00454500):** the wheel's response to the tyre force is
  `wheel[0xEC] = 0.1/fVar16`, step `dt*50*EC*F_long*radius*2.8`, with
  `fVar16 = (wheel_E0/sum|E0|)*(1-clutch_slip)*|total_ratio|*gearbox+0xE0
  + gearbox+0xE8`; the undriven/no-ratio fallback is `fVar16 = gearbox+0xE8`.
  gearbox+0xE0 = J_engine, +0xE4 = J_drive (both verified against our coupling
  k). `sum|E0|` is the same share the exchange distributes by; `car+0x1dbc` is
  the CLUTCH PEDAL (0 released, 1 pressed; the 0 -> 0.357 -> 0 was the pedal
  moving), see the recovered writer below.
- **Two portings measured and rejected (2026-11-24):** with `+0xE8 :=
  driveline_inertia` the response is 0.026/s — the pin dies (fixture wheel
  39.4 rad/s, was 0.0) but the launch collapses (0-100 4.41 -> 7.52 s, top
  227 -> 180, test 6 NaN). With `+0xE8 := 0` for driven wheels (and J_drive
  for the undriven fallback) the response is 0.436/s — the pin is marginal
  (fixture 16.0 rad/s), the launch collapses worse (8.68 s) and the undriven
  wheels freeze (0.028/s), 9 tests red. Our committed response 1/1.6 =
  0.625/s holds every known row. The truth must sit between, and the
  `(1-clutch_slip)` factor plus gearbox+0xE8's real value are exactly what
  moves it — neither is recovered yet.
- **MEASURED (2026-11-24, §14.7 of CAR_MODEL_RECOVERY):** gearbox+0xE8 = **35**
  on the MC, and wheel+0xEC came back non-zero at last: front wheels
  0.00285714283 (fVar16 = 35.0000, share 0 -> the fallback term alone), rears
  0.00285528135 (fVar16 = 35.0228195 = 35 + 0.0228 from a clutch slip of
  0.0004 in 1st gear). The listing formula reproduces the rear value to nine
  significant digits. So E8 is the per-engine brake/wheel inertia (the sixth
  float of the engine record, read by part_config as brake_inertia), NOT
  driveline_inertia: at FIXED clutch the wheel sees E8, and the share term adds
  0.5*8.611*13.5162 = 58.2 only as the clutch re-engages. The two rejected
  portings were therefore both wrong for the same reason -- the undriven/fallback
  branch is also 35 (measured on the fronts), and the `E8 := 0` trial fed
  J_drive there (0.028/s), which is what froze the undriven wheels and collapsed
  the launch. The driven value was never the culprit.
- **Both step coefficients read off the instructions (not guessed):**
  `0x455C5B` brake: `spin -= dt*50*EC*T*[0x0060E024]` with
  `T = (pedal+wheel[0xfc])*physics[0xd8]*wheel[0xd8] +
  hbrake*physics[0xdc]*wheel[0xdc] + wheel[0xf0]`; `0x4563B6` tyre:
  `spin -= dt*50*EC*F_long*wheel[0x60]*[0x0060E020]`. In the file those two
  constants are 4.0 and 2.8; live in memory they are 6.0 and 7.0 (the mod
  patches .data), i.e. the brake term is `300*EC*T` and the tyre term
  `350*EC*F*r`. Effective inertias: 1.17 and 1.0 kg*m^2 at fVar16 = 35, so the
  committed `wheel_inertia = 1.6` is 1.4-1.6x heavy but the same order, and the
  port's brake (`50/35 * cfg/1000 / 1.6`) is 2.08x too strong, not 1000x.
- **Neutral confirms the ratio fallback on the DRIVEN wheels (2026-11-24, §14.7a):**
  the same MC, standing still in neutral (gear 0, clutch 0, pedals 0): all four
  wheels read EC = 0.00285714283 -> fVar16 = 35 exactly, i.e. the rears dropped
  from 35.0228195 in 1st to the same bit pattern as the fronts. Only the ratio
  changed, so the `total_ratio == 0` branch is now closed on driven wheels too,
  and the drive term is confirmed to switch off wholesale, not shrink. The same
  pass caught a traffic car (AWD, shares 0.1/0.1/2/2, J_engine 20, E8 50): all
  four wheels 0.00200000009 -> 50, so E8 is per-engine and the fallback ignores
  the shares' magnitude; EC is stored as float32 (0.1/50 = 0.002 in double,
  0.00200000009 in memory).
- **Law in code, pinned (09a3c7b2):** wheel_slip_denominator /
  wheel_inverse_inertia in src/physics/vehicle_dynamics.cpp, tested by
  wheel_slip_denominator_reproduces_the_live_measurement (36/36,
  drive_faithful 18/18, missing_support 16/16). No behaviour change: the wheel
  model still runs wheel_inertia = 1.6.
- **PORTED (CAR_MODEL_RECOVERY 14.7b):** both steps now run on EC. The wheel
  model takes brake_inertia (E8), tyre_step_gain (0x0060E020) and brake_step_gain
  (0x0060E024) from DriveInput, wheel_clutch_open_ carries car+0x1DBC, the cfg
  brake_power/handbrake_power scales are gone, and wheel_inertia survives only in
  the coupling/differential (our model, not this listing). 36/36, 18/18, 16/16.
  Rows: 0-100 4.41 -> 4.02 s, top 227 -> 224.5, stopping 32.28 -> 31.86 m, and
  the burnout fixture asserts saturation now (slip 3.69 m/s, F/Fz 1.55).
  test_physics_tick_rate is 6/9, the SAME three rows red at HEAD (verified by
  stashing the port and rebuilding), so that is a separate limiter debt.
- **The pin did NOT move (0.1 rad/s, was 0.0):** the wheel's own inertia was not
  its cause -- it scales the response, it does not set the moment balance. So the
  next suspect is the balance itself: the exchange's share versus the tyre, and
  what holds a driven wheel at ~0 while its twin runs.
- **The invented wheel drag is gone (2026-11-25):** the tyre step also carried a
  `copysign` rolling torque and a `spin*0.15` damper, neither in the listing. The step
  is now exactly `dw = dt*50*EC*F_long*r*[0x0060E020]`; rolling resistance stays where
  the listing puts it, as the body force `roll_drag`. Rows: 0-100 4.22 -> 4.15 s, top
  227.7 -> 228.0 km/h, neutral coast 0.07 -> -0.03 m/s2; the handbrake stop from 78 km/h
  now takes 4.2 s (rear-axle grip), so that fixture's window grew from 4 to 5 s.
  Slide fixture: 1709 rpm floor, slowest driven wheel 0.0 rad/s, and the light rear is
  below 1 rad/s for **8 frames of 480** -- a transient, not the driver's hover.
- **Driver check 2026-11-15:** in a live slide the revs do NOT fall, and the
  behaviour is close to the original. That retires the engine-drag concern (the
  fixture's 1687 rpm is a pessimistic fixture, not the car), and leaves DRV-7 as
  the wheel-level pin only: the car is drivable, but a driven wheel at ~0 rad/s
  while its twin burns is still not what the original does.
- **`car+0x1DBC` recovered (2026-11-25), the dependency this item carried:** the
  factor `(1 - car+0x1DBC)` in `fVar16` is the CLUTCH ENGAGEMENT, because
  `0x1DBC` is the clutch pedal position: it is written every frame from the input
  axis (`0x44E7C9`: `pedal = axis*[0x5F0C30]`, smoothed at `0x44F39A-0x44F3BA` as
  `pedal = (pedal-x)*k + x`), pressed automatically by `FUN_0044DD70` when the
  engine is about to be dragged below the driveline, and pressed further by ASR
  (`0x44E271`: `open = 1-(1-open)*service`). So `(1-0x1DBC)` is 1.0 in ordinary
  driving (pedal up) and only goes small when a pedal/auto-clutch/ASR presses it;
  the measured 35.0228 on the rear pair was taken with the pedal at 0.9996.
  There is no separate unknown regulator: this item no longer waits on one.
- **The 0.4 s pinned launch is the HOST driveline, not the player's (corrected
  2026-11-25):** the trace that showed the rear pair standing still for 0.4 s
  while the engine revved 873 -> 2733 rpm, then breaking loose in one frame, was
  taken with `faithful = false` -- that is the host model's invented launch aid
  slipping the clutch, and the player never runs it (`player_originaldrive`
  defaults to 1). On the faithful driveline the same launch is connected from
  the first frame: spin 1.62 rad/s at t=0 and mean slip 0.42 by 0.2 s, steady
  0.40..0.43 through the second, no step. So the host path is the only place
  this regime exists, and DRV-7's pin must be measured on the faithful driveline
  to know whether it is real at all.
- **Measured on the faithful driveline (2026-11-25), and it is no longer a pin:**
  the 105-degree slide fixture reports `lowest 1709 rpm` (the engine holds, > 2x
  idle) and `below 1 rad/s for 8/0 frames` out of 240 at 120 Hz -- 67 ms of
  transit through zero, not a wheel held there. What remains is the tyre's
  longitudinal force reversing as the wheel's forward axis turns against the
  car's motion: a curve artefact of the axis flip, not a driveline one, and not
  something the anti-stall regulator (`FUN_0044DD70`) can touch -- the regulator
  acts on the pedal when the crank falls toward idle, and 1709 rpm is far above
  that. So the regulator port is NOT needed for this item either.
- **Status:** engine side closed (the airborne spin decay is fixed in e8b589d7,
  the crank holds through the slide); the residual is a 67 ms zero transit of a
  driven wheel at the axis flip. Re-open only if a live trace shows a wheel held
  at ~0 rad/s for more than ~0.1 s.

### DRV-1 Poor steering at speed
- **Symptom:** the car barely turns at high speed ("поворачиваемость говно").
- **Suspect (user hypothesis):** steering help blends the steering angle and
  hurts turning at speed — but with it disabled the car is genuinely hard to
  control, so a plain removal is not the fix. Need to reread the stock
  steering-help/steer-lock logic (Java script side) and reproduce its exact
  behaviour instead of guessing.
- **Status:** open.

### DRV-2 Bench drive has no route
- **Symptom:** `--bench-drive` holds full throttle straight; the car crashes
  into a tree minutes into the run, so long runs are not reproducible.
- **Fix direction:** follow traffic splines (Navigator plotRoute paths) or
  let a human drive while a passive logger records.
- **Status:** open. (M3/M4.)

### DRV-3 Handbrake barely locks the wheels
- **Symptom:** the handbrake has minimal effect; wheels keep rolling.
- **Cause (2026-11-15, CAR_MODEL_RECOVERY 14.7c):** two multipliers, both wrong.
  The runtime fed the rear *service* moment (half of what the game uses) and the
  pedal was a hard-coded `.3`, so the handbrake applied ~981 N m against a rear
  grip limit of ~1700 N m: it could not lock a wheel at any speed. The stand
  looked fixed only because it sets the pedal to 1.0.
- **Fix:** moment from `brakepower <brake_power> <handbrake_power>` (2x on every
  shipped racer; the live rear wheel reads 0xDC 6540.775 vs 0xD8 3270.387) and the
  button pedal at 1.0. Regression test added.
- **Status:** awaiting driver verification (does the handbrake now lock and rotate
  the car?).

### DRV-4 Wheel rotation changes on part attach/detach; sky shifts
- **Symptom:** removing or attaching any part in the garage rotates the
  wheels (from straight) and visibly shifts the sky position. Reproducible
  on both attach and detach.
- **Suspect:** the attach/detach path runs a physics reset/step with a stale
  chassis transform (or rebuilds the vehicle body and re-derives the wheel
  steer angle from a dirty state).
- **Status:** open.

### DRV-5 Wheel rotation changes on clutch toggle in test mode
- **Symptom:** switching the clutch on/off in the garage test mode also
  rotates the wheels. Likely the same root cause as DRV-4.
- **Status:** open.

### DRV-6 Camera does not auto-return while driving
- **Symptom:** after rotating the camera with the mouse while driving in the
  city, the camera stays where the user left it. In the original game it
  drifts back behind the car after ~2 s.
- **Suspect:** the auto-return (centering) logic in the chase-camera path
  was not carried over from stock; look for a "return to car" timer/blend in
  the stock camera script.
- **Status:** open.

## UI

### UI-3 Part condition icons change all at once
- **Symptom:** all part-condition icons change together even when a single
  part is damaged. Icons for missing parts must turn black but don't.
- **Status:** open.

### UI-4 Purchased parts don't land in the inventory (resolved)
- **Symptom:** parts bought in the shop didn't appear in the inventory.
- **Resolved state:** user confirmed parts DO arrive when buying in career;
  the only miss was the rare quick-start spawn (`--spawn-car`) path, which is
  not a shop/career flow. Removed from the tracker.

## Fixed

- PERF-1 dad51aa, PERF-5 f986f51 (+ --fps-cap 6695999), PERF-6 00f4c6c/a2de730,
  GFX-1 e353131, GFX-3 geometry 26f8fb1.

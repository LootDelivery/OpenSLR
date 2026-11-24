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
- **Next:** ring log of transitions written unbuffered to disk, then a scene
  unload hook; the log must land first so a new clean-up cannot hide the
  cause.
- **Status:** open.

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

## Awaiting driver verification

Code-level evidence is not acceptance. Items here are implemented, tested or
measured, and have not been checked by driving the game. Each one names what to
look at, so the check takes a minute rather than a session.

- [ ] **Traffic stops at a red light** (U2-C). The first in-game measurement
      after the physics swap said the opposite of what a driver saw: 30 red
      approaches, 1 car rested, 29 drove through -- the cap was computed with a
      5 m/s² braking curve while the speed could only fall at 4 m/s², so every car
      overshot the line and was then released as committed. With the response rate
      fixed: 25 rest, 7 through, the seven having entered at a speed from which
      stopping was no longer possible. Still needs a driver: the lamp heads do not
      glow, so which way the light is cannot be seen. That glow is the open item;
      the behaviour behind it is now measured.
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

### DRV-10 The OSD indicator icons (abs/asr) must be wired through the flags word
- **Driver requirement (2026-11-24): mandatory check.** The original's
  dashboard script reads the flags word through `GameRef.getFlags`
  (native 0047df40: returns the object's +0x54). Our flags live as
  `drive.abs_active` / `drive.asr_active` (bits 0x1000000 / 0x2000000) but are
  not exposed to the script system, so the icons never light. In the original
  the ASR icon lights, the ABS one does not (the 0x1000000 bit has no OSD
  reader there either).
- **Acceptance gate:** the ASR icon must light exactly when the original's
  does (ASR cutting, service < 0.9), verified side by side with the original
  game before/after; the ABS icon must stay dark. Do not close without the
  comparison.
- **Status:** open.

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
- **Suspect:** our ABS path is not recovered. `abs_strength` is the game
  option, but the logic itself (minimum = |road|/radius*0.85, allowed =
  |spin| - minimum, service = lerp(service, min(service, allowed),
  abs_strength)) has no section in PHYSICS_MODEL_PROVENANCE.md and no known
  exe source. With abs_strength < 1 the brake never fully releases below the
  minimum and the wheel parks at the lerp equilibrium instead of climbing to
  rolling.
- **Next step:** recover the original's ABS branch (search 0x0044xxxx/
  0x00454500 callers for the brake path) or, if the original has none for
  these cars, establish what its braked-slide wheel actually does from a live
  trace before touching ours.
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
  the clutch slip measure (0 -> 0.357 -> 0 in the measured launch).
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
  what holds a driven wheel at ~0 while its twin runs. New lead: in the 77 km/h
  slide the engine floor fell 6912 -> 1687 rpm against a 1600 guard (5% margin),
  i.e. the pin is now dragging the crank -- exactly the driver's 'the wheels stop
  and the revs fall'. Watch it live in a long slide before touching it.
- **Status:** open (the airborne spin decay that used to finish the lifted
  wheel off is fixed in e8b589d7; the pin itself is this item).

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

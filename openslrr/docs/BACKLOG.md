# OpenSLRR backlog / known issues

Working bug tracker. Each entry: symptom, evidence, suspect, status.
Move fixed items to the bottom with the fixing commit.

## Known defects

### DRV-11 Шина сверена с оригиналом: сама шина верна, момент на колёсах — нет (2026-11-29)
- **Жалоба водителя:** «в оригинале Baiern Coupe Sport GT3 хорошо держит дорогу и
  ускоряется, в OpenSLRR — как будто колёса в мыле».
- **Первый проход — шина целиком, сверено с бинарником 606 и скриптами:**
  параметры пятна совпадают с `kBaiernSportPacejka` и с retail-скриптом
  (`radius 0.329`, `friction 1.76863`, `sliction 0.814`, `stiffness 17.6839`,
  `max_load 7433`, `frictn_x 1.0`); закон `0x0043BA80/BB30` и входы `0x0043BBD0`
  перенесены дословно (сверено построчно, `CAR_MODEL_RECOVERY` 5.3b); масштаб
  `s` = полная нагрузка (ось 1 в сборке силы — нормаль), поэтому μ = 1.62…1.66;
  приложенная сила совпадает с законом при тех же входах (замер по колесу:
  `F_lat 409 Н` при `load 2173`, `α 1.4°`). **Шина не занижена — она верна.**
- **Что действительно не так:** сцепление съедается продольным каналом, а не шиной.
  Замер (`SLRR_DRIVE_TRACE`, полный газ, 1-я передача): `slip` задних колёс
  +50…78 %, `F_long 7.1…7.8 кН` на колесо, при этом `F_lat` на том же колесе
  падает до 236 Н (при запасе 3.5–4.3 кН) — круг трения занят разгоном.
- **Причина продольного момента (DRV-12):** коробка не выходит с первой передачи
  и двигатель висит на отсечке с пробуксовкой сцепления 500–600 об/мин.
- **Следствие по крену:** μ 1.66 против порога опрокидывания 1.00 g — в повороте
  машина встаёт на два колеса (замер: нагрузки 236/6115/650/6619 Н) раньше, чем
  шина начинает скользить целиком. Это и есть «не держит дорогу» на пределе.
- **Исправлено в шинном пути (2026-11-29, `51b7e035`→):**
  1. `along_axle` теперь берётся из **оси колеса** (`wheel_axle`, локальный +X
     через `hub_rotation`), а не из `normal·(forward×normal)`, что тождественно
     нулю: кембер снова отнимает сцепление, как `sqrt(ŝ[1]²+ŝ[2]²)` в `0x00454500`.
     То же исправление в статическом `tire_friction_limits`.
  2. `tire_friction_limits` = пик закона: `sin(C0·π/2)` при `C0<1`
     (`sliction 0.814` → 0.957), а не `m·s` (было на 4.5 % выше).
  3. Безсостоятельный `tire_forces` делит slip на скорость дороги, а не на
     максимум из дороги и колеса — расходился с боевым путём на 7 %.
- **Замер после правки:** `tan=0.9957…1.0000` по колёсам (было ровно 1.0000),
  `mu` 1.63→1.56 (это и есть снятые 4.5 % пика); на ровной дороге сила не
  изменилась: задний `slip +0.498` при `F 7059/255` — те же числа, что до правки.
- **Статус:** шина верна, шинный путь исправлен. Остальное («мыло») — DRV-12
  и помощник руля.
- **Roll/passport замеры 2026-11-29 (порт vs retail, тест-трек):**
  * Roll stiffness совпадает: порт 1.4–1.6°/g (по честной перегрузке скорости
    v·dθ_v/dt) против retail 1.7°/g (Baiern, живые сэмплы: 2.2° при 1.25 g,
    2.3° при 1.3 g). Порт кренится не сильнее retail.
  * Переворот порта: held 0.3 на стенде — внутренняя ось в 505 Н при
    скорости ~49 км/ч (use 0.57), затем 443/5742/882/6621 при use 0.70
    (lat 2.0 g_v) и кувырок (roll 25°). Флик 64 км/ч доводит до двух нулей
    (0/6137/0/6100), но не кувыркает (roll -3.39°, slide -0.69).
  * Слоты Pacejka стендового Baiern (дамп `SLRR_DUMP_PACEJKA`): C0 0.814, C2 1.769,
    C4 8, C8 -1 (плато без спада), maxload 7433 -- совпадают со скриптами,
    закон дословно из 0x0043BA80. «17.6839» из раннего чтения скрифта --
    промежуточная константа, скрипт отдаёт 8.
  * Остаток: статика перегрузки в порте соответствует полной высоте CG
    (h=0.71, без центра крена); retail-loads для сравнения не хватает.
    Нужен живой тест порта на пределе (липкая резина, занос) от водителя --
    кувырок там виден только вручную.
  * **Вердикт водителя 2026-11-29:** на Baiern CoupeSport GT3 оригинал дрифтует,
    порт переворачивает не всегда; склонность к перевороту видна именно с места
    спавна на дрифте, а после проезда и сброса газа дрифт обычно снос без
    переворота. Закрыто как «потенциально закрыто»; оставшийся репро.
    первичная проверка: холодная резина/первый разгон от спавна.

### DRV-12 Коробка не переключается выше первой под полным газом
- **Симптом:** 10 с полного газа, 77 км/ч, `gear=1 ratio=10.85` всё время;
  `rpm 6900…7138` при `limit=7000`, вал 6200–6500 — постоянная пробуксовка
  сцепления, двигатель прижат к отсечке.
- **Следствие:** тяга на колёсах 11…16 кН при двигателе 114–127 кВт
  (на 54 км/ч 11.9 кН = 180 кВт) — момент на колесе превышает то, что двигатель
  может дать; отсюда и +50…78 % slip в DRV-11.
- **Статус:** открыто, воспроизводится на стенде без рук (только газ).

### DRV-13 Машина отрывается/подпрыгивает на прямой выше ~64 км/ч (2026-11-29)
- **Симптом:** на стенде (ValoCity, Baiern `0x6`, прямая на полном газу) выше
  ~64-70 км/ч суммарная нагрузка колёс (`ld` в `[steer]`) гуляет 0.58-1.48
  веса, кузов подскакивает (`y`/`vy` в трассе), машина теряет контакт.
  Все замеры углов/сил выше 64 км/ч на этом участке недействительны.
- **Уточнение 2026-11-29 (со слов водителя и повторный прогон):** отрыва
  нет -- стартовая решётка стоит задом к гаражу, а через дорогу перед ней
  стоит дом. Прямая упирается в него на ~78 км/ч (повтор: 77.7 -> 3.6 км/ч
  за один кадр, `ld=0.00`, нагрузка колёс 0/0/0/0, `slide` 1.34 рад --
  удар и переворот уже после столкновения). "Подпрыгивание" выше 64 км/ч --
  это пост-столкновенческий кувырок, не баг физики.
- **Статус:** закрыт как стенд-артефакт. Скоростные сценарии на этой прямой
  невалидны выше ~60 км/ч; для чистых замеров нужен спавн с другой точки или
  карта с длинной прямой (см. план стенда в DRV-1).

### DRV-14 Reverse unreachable at a standstill (2026-11-26)
- **Symptom:** in the original, tapping Down (brake) at zero speed selects
  reverse. In OpenSLRR Down did nothing, and reverse could not be selected
  manually either -- only neutral.
- **Root cause:** `VehicleDynamics::update_gear` selected R (`gear=7`), then
  the standstill rule `road_kph < 2 && gear > 1` immediately reset it to 1.
  So R was unreachable exactly where it is selected. Down was also only a
  brake, with no standstill reverse selector.
- **Fix:** the standstill reset excludes gear 7. One-pedal classic: Down held
  brakes while moving forward; stopped with Down still held, reverse engages
  and the same held input becomes the reverse throttle (pedal swap in
  `update_gear`); releasing Down returns to drive. Manual shift-down to R is
  gated to `road_kph < 2` too.
- **Status:** driver confirmed: hold Down -> brake, stop, R engages and the held
  Down drives backwards; no gear oscillation. The one-off purple car at
  05:18 did not reproduce after a restart in identical conditions.

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
- **Reopened (2026-11-26, driver: вход и выход из ValoCity, часто):** те же два
  signed report (`crash-20261126-031146`, `031232`, один стек
  `tree_eval.cpp:460` ← `gc_safe_point` `gc.cpp:55`) при сборке, где `da802c67`
  уже есть (строка `[gc-finalize] skipped` в exe есть). Значит, падение идёт
  не через протухшую очередь: объект-финализатор жив и проходит проверку, а
  мёртвым оказывается *другой* объект, чьё поле читает финализатор. Финалка
  выполняется на host-потоке, где `script_thread_active` ложен и корней кадра
  у tree_eval нет, поэтому окно -- это любой путь, на котором сборка может
  пройти вне `GcCallScope` (натив, качающий кадры, прямой вызов).
- **Hardening (2026-11-26):** объект, чей финализатор сейчас идёт, тоже корень
  (`gc_finalize_current_`), и getfield на освобождённом получателе в tree_eval
  (`push_instance_field`) больше не крашит процесс, а пишет `[gc-stale]`
  (метод+класс+поле, 16 строк) и даёт null -- краш превращается в диагноз.
  `--probe-gc` зелёный. Нужна длинная сессия с переходами, чтобы поймать
  `[gc-stale]` и назвать виновный объект.
- **Next:** a long live session across map changes, then read the report (and
  `stderr` for `[gc-stale]`). The scene unload hook still comes after a signed
  report, not before.
- **Status:** open again (da802c67 was necessary but not sufficient); hardened
  and instrumented, awaiting a session long enough to call it gone.

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

### GFX-5 Blue in the reflection cube on non-stock (mod) maps at night
- **Symptom (driver, 2026-11-29):** на тест-треке и нескольких треках,
  сделанных не стоковыми инструментами (Invictus/ваниль), ночью машину
  заливает синим -- кузов и хром синие, в карту отражений записано
  «что-то синее». На стоковом ValoCity того же ночью нет. Смена времени
  суток не помогает. Подтверждено на нескольких модах.
- **Root cause (code-level, one open question):** каждый face захвата
  очищается в `0x577493ff` (`bgfx_lib_backend.cpp:2375`, flat steel blue);
  процедурное небо дорисовывается в захват только когда
  `outdoor_lighting_` (world cam + GroundProps-узел карты, см.
  `scene_instances_has_ground_props`, `scene_instances.cpp:2045`)
  либо в сцене есть instance с `object.sky` (выбирается из поля `skydome`,
  `mainloop.cpp:220`). Карты без обоих -- тест-трек (bench-drive на нём
  не стартует именно из-за отсутствия ground props) и моды, не несущие
  GroundProps/`skydome` в ожидаемом виде -- оставляют грани со flat-blue.
  Отражаясь в краске/хроме, это и даёт «синеву».
- **Почему ValoCity чист:** есть ground props → `outdoor_lighting_` true →
  захват рисует небо.
- **Почему заметно только ночью:** заливка не зависит от времени; днём
  синяя карта неотличима от неба в отражениях, ночью - яркое пятно на
  тёмной сцене. Фоновый clear главного вида тёмный (`0x1a2230`/
  `0x3a342c`), поэтому на экране «небо» ночью неотличимо от плоского
  тёмного фона, и баг выдал только карту.
- **Открытый вопрос (различает две гипотезы):** на экране днём на таких
  картах плоское тёмно-синее небо (т.е. небо не рисуется нигде)? Проверка
  за 5 секунд у водителя. Если днём небо нормальное - корень не в clear.
- **Fix direction (not applied per driver):** рисовать небо в захвате по
  тому же условию, что в главном виде/`always`-квад процедурного неба,
  либо при отсутствии sky-инстанса подставлять не сталь-синий, а тёмный/нейтральный
  clear. Точка входа - `capture_face` (`bgfx_lib_backend.cpp:2375..2386`).
- **Why the first fix didn't help (2026-11-26, driver: «всё равно заливает
  синим»):** был второй, более глубокий гейт выше захвата -- `update_reflections`
  вообще возвращался, если в сцене нет ни ground-инстанса, ни sky-инстанса
  (`find_if ... ground_polygons || sky` → early return). На таких картах куб
  не захватывался никогда, а `reflection_env_valid_` оставался true от
  предыдущей карты: в краску сэмплился *несвежий*, дневной синий куб ValoCity.
  Оттого и смена времени суток не помогала -- захвата просто не было.
- **Fix (2026-11-26, второй этап):** early return убран: захват идёт и без
  ground/sky, ключ мира -- сентинел (`no_ground_world`), смена карты всё равно
  инвалидирует куб через `reflection_env_world_ != world_key`; гаражный
  fallback бокса (единственное место с дереференсом `ground`) ограждён.
  Sky-фон в захвате (первый этап) оставлен: теперь он реально исполняется на
  модифицированных картах. ValoCity ночь: дамп куба тёмный, без flat-blue;
  TestTrack ночь: публикации идут. Живая проверка на тест-треке и одном моде --
  за водителем.
- **Static-cube follow-up (2026-11-26, driver: «в кубмапе что-то статичное»,
  гараж как последний кадр ValoCity):** флаг baked-слоя переживал смену мира.
  На картах без ground props bake не перезаписывался, но `mode=2` по-прежнему
  блитовал старый Valo bake под moving layer; в гараже то же самое. Теперь
  смена мира сбрасывает `reflection_bake_ready_/face/world`, а bake используется
  только при `reflection_bake_world_ == reflection_world_` (и в
  `reflection_cube_to_sample`). Trace после фикса: TestTrack только `mode=0`
  (полный живой захват), ValoCity `mode=1/2` (bake жив), гараж `mode=0`.
  Живая проверка на гараже/тест-треке/моде -- за водителем.
- **Car sun not day-cycled on non-stock maps (2026-11-26):** cubemap normal at
  night but the car bright light-blue: `scene_instances_light` applied the day
  cycle only when GroundProps existed, so on mod maps the car kept the
  directional light's daytime diffuse/ambient at 02:00. Now the cycle applies
  to every `g_world`. The garage is exempt (own interior lighting, via
  `g_garage` from `scene_instances_select`). Test track has no directional
  light in its data, so the car is reflection-only there (aluminium). A
  neutral-sun fallback was tried and reverted on the driver's word. Pending
  driver checks: garage unchanged, Drag Track 02:20, test track at 05:30.
- **Mod-map location never darkens at night (2026-11-26):** mesh shader falls
  back to a constant ndl light when no sun is bound. The sun was bound only
  when GroundProps existed (`outdoor_lighting_`), so on mod maps the location
  looked identical day and night. Sun now binds for every non-garage world
  mesh (same `garage_` exemption as the car sun). Pending driver check:
  Drag Track / test track night vs day. Test track additionally has no
  directional-light resource at all, so its sun stays unset (constant) until
  a fallback is agreed.
- **Cubemap viewer (2026-11-26):** галочка в F12 не включала вьюер, потому что
  `draw_cube_faces` требовал `SLRR_REFL_DUMP`. Гейт убран: чекбокс работает в
  обычном запуске; freeze куба — тоже по чекбоксу. После этого водитель
  подтвердил: на модовой карте синьки нет.
- **Status:** driver confirmed 2026-11-26: no blue on mod maps (cubemap viewer
  fix), Drag Track 02:20 dark, location cycles day/night on Drag Track and
  test track (mesh-sun fix). Closed.

### GFX-4 The garage keeps the street's reflection map
- **Symptom (driver, 2026-11-26):** "В гараже карта отражений остаётся от улицы,
  не переключается" -- in the garage the paint and chrome still reflect the city
  around the car instead of the shop.
- **Suspect:** the environment capture is bound once and never rebuilt on a scene
  change. The irradiance probe became default-on in `90ff7dcf` (reflection
  roughness default `ded44b59`), so the probe that is bound comes from the last
  scene that produced one (the street); if the garage scene ships no probe of its
  own, nothing re-captures and the old one stays bound.
- **Check:** log the bound probe / cubemap handle and its position at the
  transition into the garage and back out, and compare it with the garage scene's
  own environment data -- the retail garage sets its own sky and lighting, so a
  probe exists to be used.
- **Status:** open, no measurement yet. Related: DRV-4 (the sky shifts on part
  attach/detach in the garage) may share the cause -- both read as "the garage
  keeps the street's environment".

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
- **Symptom:** the car barely turns at high speed ("?>2>@0G8205<>ABL 3>2=>"),
  and removing the port's invented cap made it worse: ":0: 1C4B> ?><>I=8:
  >B:;NG5= ... =5B 02B><0B8G5A:>9 AB018;870F88, B0A:05B ?@8 ?>2>@>B0E".
- **Correction (2026-11-26):** the earlier claim in this file -- that
  `gearbox+0xA20` has no consumer and the original ships no steering help --
  was **wrong**. It came from the 4393-function decompiled set, and the consumer
  is in code Ghidra never defined as a function (`0x0044E690`, body 0x0E41,
  absent from that set). Scanning `.text` for the field offset finds the real
  readers at `0x44EDEE` (`d9 81 20 0a 00 00` = `fld [ecx+0xa20]`), `0x44EE72`
  (`d9 80 20 0a 00 00`) and `0x44EEB3` (`d8 b7 20 0a 00 00` = `fdiv
  [edi+0xa20]`). Absence from the decomp set is not evidence of absence.
- **The law (`FUN_0044E690`, re-read on 2026-11-29 from the disassembly):** the
  car's own `steerhelp` line is the whole parameter block, not one number. The
  file parser's table entry `steerhelp` (`0x0060FDE8` -> handler `0x004460A0`,
  format `%f %f %f %f %f %f`) fills `car+0x12BC lo`, `+0x12C0 hi`, `+0x12C4 min`,
  `+0x12C8 gain_angle`, `+0x12CC clamp`, `+0x12D0 gain_dir`; the script command
  `steerhelp <f>` (the options slider, `Config.player_steeringhelp` = 0.666 in
  this install) writes `gearbox+0xA20` and weights the measure the blend is built
  from. The gate is `help > 0 && car+0x12C0 > 0` (both zero in the EXE's
  constructor, so a car without the line gets no help). In listing order
  (`0x44EE70..0x44EFEB`):
  `fwd = hull velocity . body z` (forward, the one the blend uses);
  `a = 1 while |help*fwd| >= hi/help, else (|help*fwd| - lo)/(hi - lo)`;
  `slip = atan2(-v.col0, -v.col2)` -- the body slip angle -- zero beyond
  `+-pi/2`, clamped to `+-clamp`;
  `dir = 2*(W . body up) - v_prev` (memory in `car+0x1F58`), where `W` is
  the hull's angular velocity -- the triple at each 0xD4 body block's base
  (`hull+0xD4` for the body the toggle picks, `0x44EF49`);
  `tgt = X*a + (1-(1-min)*a)*axis`, where
  `X = slip*gain_angle + (axis/lock*steerhelp_turn - dir)*gain_dir`;
  `axis = clamp(tgt,-1,1)`, then `axis += (axis - before)*min(1, dt*10)`, and a
  sign reversal keeps only `|before|*0.5` of the travel.
  `steerhelp_turn` is a Config key (`0x0063C7A8`, read through `0x004261F0`),
  **1.0** in the shipped install (`SLRR_CONFIG_DUMP=1` prints it).
- **Three readings of the same listing that were wrong, corrected here:**
  * the blend is the forward **speed**, not the lateral acceleration: the
    listing squares `help*fwd` and compares it with `lo*lo` and `(hi/help)^2`,
    and no grip quantity enters. The earlier claim that "nothing in the exe
    scales the lock with speed" was wrong, and so was reading the band as
    hundredths of a g. For the Baiern (lo 1, hi 30, help 0.666) the blend is
    0.09 at 20 km/h, 0.41 at 67, 0.61 at 100 and 1 from 162 km/h.
  * `dir` is the hull's **yaw rate** -- its angular velocity about the
    body's up axis, projected on that same up axis -- not the vertical
    velocity and not the slide angle. 0x44EE37 builds the pointers from
    `hull+0x64+idx*0xD4`: the matrix columns sit at +0x40/+0x50/+0x60 from
    it and the linear velocity at +0x0C (the slip uses it), but the `dir`
    dot at 0x44EF49 multiplies the columns by +0x70/+0x74/+0x78 -- one
    block over, at `hull+0xD4` for the body the toggle picks, which is
    where the physics keeps the angular velocity.
  * the aid **adds** to the driver's axis: with `min 0.5`, `gain_dir 1` and
    `steerhelp_turn 1` a 0.65 lock makes the target `axis*(1 + 1.038*a) - a*dir`,
    i.e. at full blend the car turns about twice what was asked -- the opposite
    of the `(1-(1-min)*a)` cut this file described for a week.
- **Port (`update_steering_help`, default):** every term as written above, the
  six numbers from the car's cfg (`part_config` parses the line; AI cars ship
  five, leaving `gain_dir` at zero), the slider and `steerhelp_turn` from Config,
  the 10 Hz slew and the reversal rule unchanged. The previous absolute
  counter-steer (`a^2 * sin(slide) * 0.5`, added to the request with no reference
  to it) is gone: it took 30-70% of the driver's input at speed and steered
  against a counter-steer.
- **Measured (ValoCity, Baiern `0x6`, 55 km/h, `SLRR_BENCH_STEER=0.3`):** the
  axis reaches 0.44 of a 0.30 request (`a` 0.32 -> `1 + 1.038*0.32` = 1.33) where
  the old build delivered 0.10; the front wheels sit at 17 deg with the throttle
  pinned at 79% of the lateral grip. With no input on the straight (a downgrade)
  `dir` = -0.22 m/s and the aid holds +0.06 of lock, 2.2 deg of front steer at
  55 km/h -- the term is real and terrain-driven, and the driver should test a
  downhill with the wheel centred. That was the port feeding the aid the
  vertical velocity; the live read below shows retail keeps 0.00-0.04 there
  because its `dir` is the yaw rate, which is 0 on a straight.
- **ESP is inert in this build:** `esp_steer_scale`, `esp_angvel_scale` and
  `esp_yaw_allowed` are absent from the install's Config (tag Void -> 0), and
  `FUN_0044E2D0` divides `0.1` by `esp_yaw_allowed` for its threshold -- with 0
  the limit is infinite and the law never intervenes. The old question about
  ESP acting in the retail game is closed.
- **Switches:** `SLRR_STEER_LEGACY=1` keeps the old invented curvature cap for an
  A/B; `SLRR_STEER_TRACE=1` prints `[steer] v req axis tgt a slide slip lat_g
  help cfg du...` every 20 frames; `SLRR_CONFIG_DUMP=1` prints the Config keys
  the physics reads. `--bench-drive` without `--bench-route` accepts
  `SLRR_BENCH_STEER=<axis>` with `SLRR_BENCH_STEER_FROM=<s>` / `_TO=<s>`.
- **Keyboard is already an integrator, measured (`SLRR_INPUT_DUMP=1`, manual bench,
  `PostMessage` WM_KEYDOWN scan `0xCD` held 0.4 s):** `raw1` reads -1.000 while
  `val1` climbs to -0.253 and the car gets -0.169 afterwards. Axis 1 carries the
  retail `VirtualAxisSmoothProperties` (center 0.1, 0.0025/0.0035/0.0025 per ms,
  power 1.75) and its bindings are the two arrow keys (`dev 0 phys 203/205`), so
  a key press is a rate, not a step -- roughly 0.4 s to full lock. The jerk the
  driver reports is therefore downstream of the ramp: the assist turns that
  half-pressed axis into more than full lock (see the doubling above), so the
  gradation the ramp creates is destroyed by the gain, not by a missing filter.
- **Switches (input):** `SLRR_INPUT_DUMP=1` prints the registered smoothing, the
  axis-1 bindings and a per-30-frame `raw1/val1/steer` line;
  `SLRR_INPUT_NOFOCUS=1` keeps posted key messages alive while the window is not
  foreground (the poll otherwise clears every key, `host_input.cpp:167`).
- **Tests:** vehicle_dynamics 39/39, physics_recovery 8/8, missing_support 16/16.
- **Status:** the `dir` term is now measure-verified (live read below);
  port feeds it the chassis yaw rate. **Driver verified 2026-11-29:**
  "прилично лучше, похоже на оригинал; машина держит траекторию даже
  при оборотистом V8 на 230 км/ч" -- the straight holds on its own now.
- **A/B measured 2026-11-29 (ValoCity, Baiern 0x6, bench straight; logs in
  `%TEMP%/steer_ab`, 4 combos x straight/held/flick):** all three driver
  complaints isolated to one term each. (1) *Self-steer on the straight:*
  with no input at 40-65 km/h the car holds axis +0.055 (front wheels 2.0 deg)
  from `dir` alone -- `SLRR_STEER_NODIR=1` gives exactly 0.000. `du` is a
  steady -0.20 (the bench road slopes down), so the pull is systematic, not
  noise, and it changes the trajectory by metres before the kerb. (2) *Gain:*
  held req=0.3 gives tgt/req = 1.27 at a=0.17 and 1.29 at a=0.24 (axis 0.37,
  wheels 15 deg); `SLRR_STEER_NOTURN=1` drops it to 0.97-1.0 (the `min` cut
  alone gives 0.89-0.91, `dir` adds back ~+0.03 on this slope). (3) *No
  stabilisation in any variant:* a full-lock flick at 60 km/h slides in all
  four combos (slide -0.5..-1.27 rad); `dir` removal only changes where the
  car hits the kerb, it does not catch the slide. All combos also spin at
  use~0.8 in the held run -- that is the tyre/roll side (DRV-11), not the
  assist. Bench notes: the straight hits an obstacle at ~67 km/h (~2.7 s),
  so held used FROM=0.5 and flick FROM=2; `SLRR_BENCH_STEER_TO` must always
  be set -- without it `to_frame = (int)(1e9 * 60)` overflows `int` in
  `boot.cpp` and the scripted steer never applies, and after `TO` the value
  is never reset (a 0.8 s pulse is really a step). Both are host bench bugs,
  not game behaviour.
- **RE pass 2026-11-29 (static, `rea/ghidra_out/decomp/`):** the `dir` axis is
  now confirmed as *up*, by two independent consumers: in `FUN_00454500` the
  steered wheel forward is built as `cos(steer)*col2 + sin(steer)*col0`
  (so col0/col2 span the ground plane), and the tyre grip uses components
  1,2 with component 0 as the axle -- hence col1 is the wheel/body up, and
  `dir = V . up` stands. What is still open is *which body* `V` is: the index
  `hull + 0x20c` is a toggling selector (`idx ^ 1` in the sleep handler
  `FUN_004a4ed0`, the only writer found), so at E690 entry it is whichever
  body of the pair was toggled last -- not necessarily the chassis COM the
  port uses. If it is a wheel/point body, `dir` carries rotational velocity
  (roll/pitch rate x offset), i.e. roll feedback into the steering. Closing
  this needs a live read of retail (`rea/live_read.ps1`: sample `hull+0x20c`,
  `hull+0x70+idx*0xD4`, `car+0x1F58` while driving). Also decoded: the gate
  flag `car+0x70 & 0x200` is *steerhelp disabled* -- the `steerhelp <f>`
  script command sets it (and zeroes `gearbox+0xA20`) at value <= 0 and
  clears it above (`FUN_00458c00`, `s_steerhelp` case). The port folds it
  into `controls` (modal window), which is a different condition; and when
  help is disabled the exe skips the block entirely (no slew, no reversal),
  while the port still slews -- a small mismatch worth fixing with the gate.
- **Live read 2026-11-29 (retail `StreetLegal_Redline.exe`, MC Limited Edition
  on the test track, `rea/steer_sample.ps1` at ~7 Hz/car, player car
  `0x0FD08030`, help 0.666):** the sampler writes `axis`/`dir_mem`/`idx`,
  the per-body orientation (12 floats at `hull+0xA4+idx*0xD4`), the linear
  velocity (`hull+0x70+idx*0xD4`) and the angular-velocity triple
  (`hull+0xD4+idx*0xD4`). On the flat straight `W` is 0.00-0.04 and retail
  holds `axis` at 0.001-0.008 up to 133 km/h -- no self-steer at all. In a
  full-power turn `W.up` climbs 0.24 -> 0.53 -> 0.25 rad/s, `dir_mem`
  correlates 0.997 with `W.up` in the same sample, and the steady-state
  `axis` 0.944 is the law's `axis/lock - dir = 1.45 - 0.5 = 0.95`.
  The hull's two body blocks are value-identical (matrices and velocities
  match), so the `idx` toggle -- which alternates 0/1 during the drive --
  does not change `dir`; the body question is moot. Considered and ruled
  out: `V.up` (vertical velocity, corr -0.03), lateral `V.col0` (corr
  0.69, confounded by the turn), yaw estimated from orientation change
  (it under-reads at 7 Hz; `W` matches it at ~unity scale). The port now
  uses `body.omega().dot(body.orientation.rotate(up))` for the term, with
  the same one-frame extrapolation, and the generic code comment says
  `car+0x1F58` remembers the previous yaw rate.

#### 2026-11-25 -- why it feels like bald tyres and why it rolls over

The driver's next report: "В оригинале машина намного лучше держит дорогу, даже
если её жопу носит. А здесь я как будто на лысой резине, какую бы резину я не
взял. При этом если я беру резину с хорошим зацепом и мягким компаундом, машина
на заносе переворачивается, потому что боковым она цепляется за асфальт." Two
measurements on the bench (ValoCity, Baiern `0x6`, `SLRR_STEER_TRACE=1`) explain
both halves without touching the assistance:

* **Grip is not missing -- it is excessive and shapeless.** The trace's new `mu`
  field (peak lateral force per unit load, `tire_friction_limits`) reads 1.62-1.68
  at every speed, with the load sum (`ld`) holding the car's weight (0.98-1.02).
  1.66 is above a racing slick. The curve itself, from the recovered law with the
  slots `Wheel.java` ships (`C8 = -1`), still rises at 45 degrees of slip:
  1 deg 0.28 mu, 4 deg 0.91, 8 deg 1.25, 20 deg 1.47, 45 deg 1.54. There is no
  falloff after the peak, so the tyre never gives the driver a warning band and a
  big slide is punished with *more* grip than the entry had.
* **The chassis tips at 1.00 g.** The same trace prints the centre of gravity over
  the contact patch (`h`) and the track (`tr`): 0.71 m and 1.42 m, so the rollover
  threshold `tr / 2h` is 1.00 g. A tyre that holds 1.66 mu therefore lays the car
  on its roof long before it can slide -- "боковым она цепляется за асфальт" is
  literally what the numbers say. Soft, wide, sticky compounds make it worse
  because they raise `mu`, not because anything in the assistance misbehaves.

`h` comes from `assembly_mass_center`, i.e. from the parts' authored masses, so it
is as faithful as the part data; the *ratio* `mu / (tr/2h)` is what decides whether
a car slides or trips. The open question is the retail game's own roll: the port
lets the chassis roll as a free rigid body on four springs, and no anti-roll is
configured for stock cars (`solve_anti_roll` is a host approximation and stays at
zero). A live read of a cornering car in the retail game (roll angle and `h`) is
what would settle whether the original limits that roll.

Trace fields, in order: `v req axis tgt a sp ld mu use slide slip lat_g help cfg`
-- `a` and `sp` are the two blends (grip, speed), `ld` the load sum over
`mass*g`, `mu` the peak lateral per unit load, `use` the share of it in use, and
`h tr thr` the tip geometry above.
#### 2026-11-25 -- the drift is sharp: two causes, both measured

The driver: "В оригинале я на 100 км/ч закидываю задницу на 90 градусов и машина
скользит. И в опенслр тоже так. Только в оригинале я машиной могу управлять в это
время, я могу перекладываться, она не резкая, она плавная. А в опенслр она резкая,
она дёргается и скорее всего она развернётся." Two separate defects:

1. **The counter-steer was in the wrong units.** The recovered form adds `dir` to
   the axis (`tgt = (axis - dir)*gain_dir + ...`) and a player car's `gain_dir` is
   1.0, so `dir` is a *fraction of the lock*. The port fed it the body slip angle
   in radians: at a 40-degree slide `dir` was -1.4, the term alone was -0.7 rad
   against a 0.58 rad lock, and the axis pinned itself at full counter-lock no
   matter what the driver asked for. That is the "дёргается": the wheel slams
   across, the reversal rule keeps half of the travel, and the car snaps the other
   way. `dir` is now `sin(slide)` extrapolated (`2v - v_prev`, still the exe's
   memory), so it saturates at 90 degrees and the counter-steer is at most half a
   lock, blended twice over. Measured on the fixed-steer run: at a 71-degree slide
   the assist is -0.48 of a lock and the axis is 0.45, i.e. the driver's input
   survives.
2. **The yaw inertia was 1.5x too small.** `RigidBody::configure` used a solid box
   of 0.75 times the bounds, which is 0.56 of the box once squared. Measured on the
   Baiern: bounds 1.73 x 4.21 m, yaw inertia 1481 kg m2, where a plain box over the
   same bounds gives 2244 and a real 1300 kg car sits at 1800-2500. A flick rotated
   the tail about 1.5x faster than the geometry allows. The 0.75 was host tuning
   and is now a plain box.

A/B on the same bench flick (full lock for 0.8 s at ~71 km/h, `SLRR_BENCH_STEER=`,
`--bench-drive`):

| after the flick | before | after |
| --- | --- | --- |
| body slip at +1 s | -0.11 rad | -0.04 rad |
| body slip at +2 s | -0.41 | -0.17 |
| body slip at +3 s | -0.60 | -0.29 |
| end of the run | -1.24 rad (spun), 17 km/h | -0.51 rad, 55 km/h |

The car still slides (that is the point), but the slide builds over seconds instead
of tenths and costs half the speed instead of all of it. Neither change touches the
tyre law or the recovered constants; both were mappings the port had guessed.

**Test note:** `test_physics_tick_rate` reports 6 passed, 3 failed (its tests 3, 6
and 9). It is not these changes: the same three fail on the same peaks (7049 rpm)
with the old inertia restored, and they date from `adc6e518` (DRV-7, the recovered
EC). vehicle_dynamics 39/39, drive_faithful 18/18.

#### 2026-11-25 -- "как кисель": the lock was being cut with speed

The driver: "Всё равно машина ощущается вязкой, неуверенной на дороге. Да, по дрифту
поведение стало лучше, но само ощущение машины... как будто она не держит дорогу, её
постоянно приходится отлавливать. В оригинале же машина держит дорогу. Даже если ABS
- 0."

The port was scaling the driver's own steering. The recovered E690 law builds `a`
from one measure -- the car's lateral acceleration in hundredths of a g
(`car+0x2128`) -- but an earlier build read the same cfg band (`lo 1`, `hi 30`) a
second time as *speed in m/s* and cut the lock with it:

    requested = steer * (1 - (1 - min) * a_speed)

At 60 km/h that is 20% of the input gone before it reaches the wheels, at 100 km/h
35%. The car still answers, just less and later than the wheel says, so the driver
adds more, the assist then pulls the axis back, and the correction has to be made
again -- the viscous, always-catching feel. It is also open loop: nothing in the exe
scales the lock with speed.

Measured at 67 km/h, a held 0.15 ask (trace `axis` against `req`):

| | axis delivered | lat_g |
| --- | --- | --- |
| speed cut on | 0.11 of 0.15 (73%) | 0.47 g |
| speed cut off | 0.143 of 0.15 (95%) | 0.58 g |

The obvious repair -- drive the same cut from the grip blend instead -- was tried
and rejected: with the player's `min 0.5` and `a` reaching 1 at a third of a g, a
held 0.15 ask at 67 km/h settles at axis 0.077, and the car then cannot corner past
~0.5 g at all. The recovered `(1 - (1 - min)*a)` factor multiplies the *held* axis in
the listing; applied to the request it throttles cornering itself, which is not what
a car does. So the input now reaches the wheels 1:1 and the aid is the counter-steer
term alone (at most half a lock, blended by the grip band, scaled by the slider).

On the ground, same bench, 0.15 held from 1.3 s: axis 0.143 against a 0.144 target,
lat_g 0.70-0.71 g at 58 km/h, wheel loads 0.96-1.12 of the weight, no wheel lifts.
The counter-steer is unchanged during a slide (the grip blend is already 1 there):
a 0.5 lock flick at ~50 km/h still shows `as=-0.40` of a lock against a held axis.

**Two measurement traps found on the way, both worth remembering:**

- The trace's `bx bz iy` fields had shifted: `bx` printed the wheelbase (2.74),
  `bz` the yaw inertia (2632 kg m2) and `iy` stack junk. So the Baiern's yaw
  inertia after the box fix is 2632, not the 1481 quoted in the section above --
  that number is the old 0.75-box value (2632 x 0.5625).
- Above ~64 km/h the bench drive car *takes off*: `l=0/0/...` on one axle, `y`
  climbing, `vy` negative, `ld` swinging 0.58..1.48 on a straight, no steering.
  Every load, lat_g or assist number measured at 66-72 km/h on that stretch is
  airborne and invalid; the cornering numbers above were re-taken below 64 km/h.

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

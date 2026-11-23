# Provenance of the car physics: recovered, adapted, invented

**Why this document exists.** A driver compared the car with the original game and
asked whether the physics being installed is a port of the original or something
new. It is something new, and the code says so in its own comments. This file
records which parts are which, with the evidence, so a fidelity decision is made
on facts rather than on the resemblance of the file names.

## 1. What the original actually contains

The original keeps almost nothing in the scripts. Decompiled from the shipped
classes:

| Original script | What it is | Where |
|---|---|---|
| `Clutch` | one field: `float maxF = 500.0;` and a name. No model. | `parts/scripts/enginepart/slidingenginepart/reciprocatingenginepart/Clutch.java` |
| `Transmission` | ratios, `end_ratio`, `starter_torque`, drive split (`drive_front`), `diff_lock`, `capacity`; writes them into `the_car` (`gears`, `ratio[]`, `rearend_ratio`, `diff_lock`, `setDrive` per wheel) | same directory, `Transmission.java` |
| `Chassis` | parameters only: `engine_rpm_idle = 800`, `gears`, `rpm_trans_fwd = 1400`, `sfx_starter_rpm = 200` | `parts/scripts/bodypart/Chassis.java` (decompiled with SLRR Class Reader from `Chassis.class`) |

So the scripts hand over constants, and the physics is native.

## 2. What the reverse work recovered of the native side

`slrr-reverse/engine/Runtime/Parts/world_state.cpp` carries the recovered
contracts, marked "Soft PE":

```
arcade_gear_engage(gear, clutch) = gear == N ? 0 : clamp(1 - clutch, 0, 1)
arcade_drive_thrust(drive, radius, gear, clutch) =
    engage * clamp(drive) * radius_mul(radius)
```

That is the whole of the recovered drive model: **thrust scales with the clutch
release, and there is no engine-rpm hold anywhere in it**. The engine's own rpm
is not recovered: the only `engine_rpm` in the reverse tree is a default value in
a state struct (`PhysicsRef.cpp:53`).

## 3. What this tree runs

`src/physics/vehicle_dynamics.cpp` is a crank-inertia model: an engine with its
own rotational inertia, a slipping clutch that exchanges momentum with the
driveline, a Magic-Formula tyre, and a suspension solver. Its provenance, from
its own comments:

- "Adapted contracts: SLRR_rev a2ee10de, rigid-body-3d.ts" -- a TypeScript
  reimplementation, not the executable.
- "the 8ac2ba9 fallback uses a solid box of 0.75 times the bounds dimensions
  (host tuning, not the collision dimensions or the original game's compound
  inertia)".
- `ENGINE_CONTRACT §11` aerodynamics, `Wind`, `AeroWing`: host additions.
- The automatic launch hold was `drive.limit * .4` -- a constant with no recovered
  counterpart anywhere. It applies to every car whose gearbox reports automatic,
  and it is what a driver sees as "the engine stops at a quarter of the
  tachometer".

## 4. The consequence, measured

Mazda 3, throttle held from standstill, 120 Hz solver, `SLRR_DRIVE_TRACE=1`:

| Clutch slip revs | Engine behaviour | Speed at t = 5.8 s |
|---|---|---|
| 0 (no hold) | **stalls against the clutch**: 323 rpm, torque -6 Nm | 5.9 km/h |
| 0.4 (upstream) | held at 2600 of 6500 | 29.6 km/h |
| 0.95 | held at 6175 of 6500 | 22.0 km/h |
| 1.0 | held at 6500, clutch barely engages | 8.6 km/h |

Two things follow. The 40% hold is not a driver aid that can simply be turned
off -- without it this model stalls -- and no setting of it gives both "the
engine screams to the limit" and "the car pulls away hard", because a fixed rpm
hold spends the difference in slip.

## 5. The choice

1. **Fidelity.** Implement the recovered contract: thrust from
   `drive * (1 - clutch)`, engine rpm as an output of the driveline rather than a
   separately integrated crank, `Clutch.maxF` as the coupling limit. That is what
   the original does, and it is a rewrite of `step_drive`, not a tuning change.
2. **Upstream's model, exposed.** Keep the crank-inertia solver and the slip
   point as a player setting (`Clutch slip revs`, F12; `SLRR_LAUNCH_RPM` for A/B).
   Already implemented; the default stays at upstream's 0.4 so nothing changes
   silently.

The traffic complaint is the same question in another place: ambient speed comes
from this host's route model, not from the original's traffic natives, so
"slower than the original" is a host value to measure and set, not a bug.

## 6. What was not claimed

Nothing here says the original's internals are recovered. `Clutch.maxF`,
`Transmission`'s ratios and the `1 - clutch` thrust are; the crank inertia, the
tyre model, the aerodynamics, the launch hold and the ambient traffic speeds are
not.

## 7. The parameter contract, field by field

The original hands its physics a fixed set of `Chassis` fields; the natives read
them off the Java object. Decompiled defaults are from
`parts/scripts/bodypart/Chassis.java`. The right column is what this tree's host
actually does with each one (`src/resources/part_runtime.cpp`, `configure` path).

| Original field | Default | This host |
|---|---|---|
| `engine_torque`, `engine_torque2` | 0 (filled from the part) | dyno bank built from the same part data; **faithful in kind** -- the original's `DynoData` is native too |
| `maxRPM` | 7000 | read, but only to decide "is there an engine"; not used as a limit |
| `RPM_limit` | 20000 (`Chassis.java:296` sets 12000) | `drive.limit` ✓ |
| `engine_inertia` | 10 | `drive.inertia = .28 + .01 * engine_inertia` -- upstream's scaling, so 10 becomes 0.38 |
| `engine_friction_fwd` | 1e-4 | not read; upstream uses its own `pow(rpm,3)`-style loss |
| `engine_friction_rev` | 5e-3 | not read |
| **`ClutchF`** | **500** | **not read** -- the model has a 0..1 clutch input and no force limit. This is the single biggest divergence: the original couples the driveline through a force, this tree couples it through a momentum exchange with an rpm hold |
| `gears`, `ratio[8]`, `rearend_ratio`, `diff_lock`, `starter_torque` | 0 / 3.3 / 0 / 0 | read ✓ |
| `drive_type`, per-wheel `setDrive` | set by `Transmission` | read ✓ (per-wheel drive share) |
| `brake_balance` | 0.5 | not read; the split comes from each brake part's power instead |
| `max_steer` | 0.7 | not read; this tree has its own steering geometry and help curve |
| `fully_stripped_drag` / `C_drag` | 0.333 | **now read** (below). `Chassis.updatevariables` computes `C_drag = fully_stripped_drag - sum(body part drag_reduction)`, floored at 0, and the original applies it as quadratic drag |

So the fidelity work is finite and enumerable: the parameters below are simply
not being passed, and `ClutchF` is the mechanism behind the launch report. Nothing in the list requires
recovering anything new from the executable -- the fields are in the decompiled
`Chassis.java`, and the natives that consume them are the ones already
documented in `slrr-reverse`.

## 8. Ambient traffic speed: measured, not inherited

The speed of ambient cars is this host's own model -- the original's traffic
natives (`addTrafficCar`, `setTrafficCarBehaviour`) do not carry a cruise speed
this tree can read. So it is set here, and it was set low: cruise 15..35 km/h for
every street, and an acceleration cap of 0.8 m/s^2, which needs seventeen seconds
to reach 50 km/h. That is the "the traffic is asleep" report.

Measured with `SLRR_TRAFFIC_SIGNAL_TRACE=1`, mean speed of the placed cars over a
thirty second drive through ValoCity:

| Cruise range | Acceleration | Mean ambient speed |
|---|---|---|
| 15..35 km/h | 2.5 m/s^2 | 23.5 km/h |
| 40..65 km/h | 2.5 m/s^2 | **34.7 km/h** |

Now 40..65 km/h at 2.5 m/s^2 by default, both adjustable without a rebuild:
`SLRR_TRAFFIC_CRUISE` scales the range, `SLRR_TRAFFIC_ACCEL` sets m/s^2. The lamps
keep working at the higher speed -- 40 red approaches, 28 rest, 12 through, the
twelve entering too fast to stop -- and there are no chassis faults in the run.

## 9. `C_drag`, measured, and a correction

The first draft of this document said the cars run with no aerodynamic drag and
called that a defect. Feeding it properly showed the opposite.

`C_drag` is not on the chassis object in this host -- `Chassis.updatevariables` is
what creates it, and it is not running here: on the same car the field reads
`0.0000` while `fully_stripped_drag` reads `0.5500`. So the script's own formula is
applied here instead, over the installed part tree. Measured on the MC GT:

```
fully_stripped_drag            0.5500
body parts with drag_reduction 21, summing to 0.6385
C_drag = max(0, 0.55 - 0.6385) 0.0000
```

A fully panelled car genuinely has zero drag by the original's own arithmetic: the
panels give back more than the stripped baseline. Drag only appears on a car with
panels removed, which is exactly why the field exists. So the earlier "no drag"
was the right value reached for the wrong reason (nothing was read at all), and
the feed is now faithful to the script -- with `SLRR_AERO_DRAG=0` to run the same
build without it, and `[drive-trace] aero ...` reporting both numbers.

What this changes in practice: nothing for a complete car, and correct behaviour
for a stripped one.

## 10. The original coupling, implemented and measured

Decision: the reference behaviour is the original game. Full throttle with no
stability system has nothing to limit it, so nothing limits it here either;
stability systems, when wanted, will be added as a separate layer.

`DriveInput::faithful` implements the recovered contract and is what the game now
runs (`player_originaldrive`, default on; F12 checkbox "Original drive coupling";
`SLRR_DRIVE_MODEL=host` for the old path in the same binary):

- the gearbox passes the engine's torque, `Chassis.ClutchF` (500) caps what the
  clutch can carry, and the clutch release scales it -- `engage = 1 - clutch`, as
  in `arcade_gear_engage`;
- **no separately integrated crank while a gear is engaged**: the engine turns with
  the driveline, so the tachometer follows the wheels and a spinning wheel flares
  it. An independently integrated crank sat at the limiter with the clutch slipping
  forever -- the car pulled away on a clutch that never locked, which is what the
  first attempt did and why it was thrown away;
- neutral still integrates freely, so the engine revs to its limiter standing still;
- the limiter is the only ceiling, and it clamps the driveline coupling too.

Measured on flat ground by `tests/test_drive_faithful.cpp` (1300 kg, rear drive,
225 Nm curve, 7000 rpm limit), full throttle from rest, 25 s:

| Coupling | 0-100 km/h | Top speed | Max rpm | Wheel slip |
|---|---|---|---|---|
| **original (default)** | **5.88 s** | 237.9 km/h | 7140 | up to 3.40 m/s |
| host crank-inertia | 7.48 s | 222.2 km/h | 6926 | 0.30 m/s |

The original coupling is 1.6 s quicker to 100 km/h and lets the tyres break
traction -- the difference a driver described as "in the original it spins because
the engine revs" against "this drives like a small car". The six tests pin it:
not slower than the host model, 0-100 under 15 s, revs used without passing the
limiter, gears used, torque reaching the road, and a clutch force above the engine
torque not holding the car back.

`test_vehicle_dynamics` (35 cases) still runs the host model, because
`DriveInput::faithful` defaults to false there: the solver's own suite keeps
pinning what upstream wrote, and the game chooses the other path explicitly.

## 11. Why "as if I am pressing the pedal slowly", and the converter

Report: "the revs climb smoothly, very smoothly -- not like full throttle, like I
am gradually pressing the gas". That was the rigid coupling from section 10, and it
was the third attempt at this one coupling:

1. **Crank integration with a fixed transmitted torque.** The engine sat at the
   limiter with the clutch slipping forever; the car pulled away on a clutch that
   never locked, and first gear lasted past 50 km/h.
2. **Rigid link -- engine turns with the driveline.** Physically defensible here,
   because `ClutchF` 500 exceeds what the engine makes (225 Nm), so a real clutch
   would hold. But the tachometer then becomes a copy of road speed: flooring it
   drags the revs up exactly as fast as the car accelerates. That is the report.
3. **Torque converter.** The gearbox is automatic (`player_transmission = 1` in the
   game's own settings), and an automatic's engine speed is set by the throttle
   down to the converter's stall point, and only above that by the driveline:

```
stall_rpm = idle + (limit - idle) * 0.45 * throttle      // 0.45 is host tuning
target    = max(driveline_rpm, stall_rpm)
engine_rpm -> target                                    // 90 ms
thrust    = min(torque(rpm) * throttle * engage, ClutchF) * ratio / radius
```

Now the revs answer the pedal, the converter holds them at its stall speed while the
car is still slow, and once the wheels outrun the stall point the two turn together.
The limiter is still the only ceiling. `tests/test_drive_faithful.cpp`, flat ground,
full throttle from rest:

| Coupling | rpm at 0.5 s | rpm reaches 60% of limit | 0-100 km/h | Top speed | Wheel slip |
|---|---|---|---|---|---|
| **converter (default)** | **4075** | **1.41 s** | **5.81 s** | 234.4 km/h | up to 6.90 m/s |
| host crank-inertia | 2800 | 2.08 s | 7.48 s | 222.2 km/h | 0.30 m/s |

Eight cases pin it, including the flare itself (`rpm at 0.5 s > 3000`, 60% of the
limit in under two seconds), so a future change that makes the revs lazy again fails
a test rather than needing a driver to notice.

Also added: a driving readout in the corner -- speed, rpm and gear -- drawn with the
debug window closed (`player_drivehud`, F12 "Drive readout", on by default), because
the numbers this section is about had to be readable while driving rather than
inferred from a log.

### 11.1 Torque multiplication, and why nothing ever spun a tyre

A converter does not pass the engine's torque while it slips; it multiplies it.
Without that term the wheels only ever saw the engine's own 225 Nm, asked the tyres
for 0.4 g, and no launch could break traction however high the revs were -- which is
exactly what a driver reported after the revs were fixed: "the revs are right now,
but there is still no wheelspin".

```
slip_share     = clamp((stall_rpm - driveline_rpm) / stall_rpm, 0, 1)
multiplication = 1 + slip_share        // 2 at a standstill, 1 once locked
thrust         = min(torque(rpm) * throttle * multiplication, ClutchF) * ratio / radius
```

The same fixture, again full throttle from rest (1 300 kg, 225 Nm, 7 000 limit):

| | 0-100 km/h | Top speed | Wheel slip | rpm at 0.5 s |
|---|---|---|---|---|
| no multiplication | 6.27 s | 224.9 km/h | 44.8 m/s | 6248 |
| **with multiplication** | **5.37 s** | **242.3 km/h** | **55.9 m/s** | 7140 (on the limiter, spinning) |

On the test car the driven wheels outrun the car badly enough that the gearbox
drives the engine into the limiter -- what a real car does with the tyres alight in
first. On the Mazda the arithmetic is 450 Nm through 8.61 and a .31 m radius =
12.5 kN over 1 566 kg = **0.81 g** against roughly 1 g of grip: spin becomes
possible on a launch, not guaranteed. A heavier right foot cannot conjure it on a
car that does not have the torque; a stronger car will.

### 11.2 The readout belongs to the game's OSD, not to a second interface

The first version of the speed/rpm/gear readout was an ImGui window drawn with the
debug overlay closed. That meant forcing the interface pass on every frame --
including menus, where it drew over the main menu and the menu's own labels stopped
appearing. The game already owns a text layer that knows about layers, groups and
menus (`slrr_text_create` / `slrr_text_update`, the same one every menu label uses),
so the readout is one line of that text now, created each frame (a section change
clears the text store, so a cached key would go silent) and hidden outright in any
section whose name contains `Menu`, `Splash` or `Loading`. No extra pass, no second
interface, and it cannot take anything from a menu. Position: right of centre,
vertically centred -- `x = .66`, `y = -.02` in OSD space.

### 11.3 The shift schedule read the wrong shaft

Two driver reports, one cause: the automatic gearbox's shift decisions were built on
**engine** speed, which is now a throttle-set number while the converter slips.

  "On a 6500 limit it only reaches 6000-something; on 9000 it reaches 7842."
  "At a standstill it stops in second and never goes back to first."

Both are `limit * .88` and `limit * .42` in the old schedule. Upshifts happened at 88%
of the limit, so the top of the range was never used; downshifts needed the engine
below 42% of the limit, which a converter at stall (80%) never does.

```
coupled_rpm = |road_shaft * ratios[gear]| * 9.549   // the driveline, not the engine
up_rpm      = limit * (.58 + .40 * throttle)        // full throttle -> 0.98 * limit
down_rpm    = limit * (.34 + .16 * throttle)
upshift     if shaft > 2 and coupled_rpm > up_rpm
downshift   if gear > 1 and coupled_rpm < down_rpm and the lower gear stays under
            .95 * limit
```

Three smaller defects fell out of the same block and are fixed with it:

- `if (shaft <= 2) return;` skipped the whole schedule at a standstill. That is the
  literal reason a stopped car stayed in second. Only the upshift needs motion now.
- The schedule upshifted on engine speed with a **closed throttle**, so braking to a
  stop walked a car up to fifth in the harness. Judged on the driveline it cannot:
  coupled_rpm falls with the car, so an upshift while coasting at speed stays one
  upshift instead of a climb.
- Downshifts now check that the lower gear would not exceed the limit before taking
  it.

Pinned by `test_drive_faithful`: `full_throttle_uses_the_whole_range` (max rpm above
95% of the limit) and `a_stopped_car_is_in_first_gear` (stops in gear 1). Measured on
the Mazda's numbers: full throttle now pulls to the limiter in every gear instead of
shifting at 5720 and 7920.

### 11.4 "There is still no wheelspin on the Mazda": correct, and here is the number

The car's own data, printed by `SLRR_DRIVE_TRACE`:

```
limit 6500   idle 900   ClutchF 500   first ratio 8.61   wheel radius 0.318
rear wheels: drive 1.00  friction 1.95  long grip 1.00  max load 10 860 N
```

Thrust at the clutch cap = 500 Nm x 8.61 / .318 m = **13.5 kN**. Rear axle load in
the fixture = 10 038 N, and 1.95 x that = **19.6 kN** of tyre. The launch asks for
**73% of what the tyres hold**, so nothing can spin: the engine cannot make more
torque than it makes, and the gearing multiplies what it does make into less force
than the tyre is rated for.

The generic fixture (1 300 kg, coefficient 1.6, shorter gearing) does spin -- 71 m/s
of slip, the wheels dragging the engine into the limiter -- so the mechanism is not
missing. It is a property of this car: with friction 1.95 on its rears, the Mazda is
nowhere near the limit. `the_mazda_launch_is_below_its_tyre_limit` pins it, so a
future change that inflates the launch force has to walk past a failing test rather
than a driver's suspicion.

What would change it, each a decision and not a default: `ClutchF` (500 is the
original's own number), the converter's 2:1 stall multiplication (physical, but the
exact ratio is a design choice), or the tyre coefficient (game data, 1.95 as shipped).

## 12. Correction: the executable has been decompiled, and `ClutchF` is not used

The sections above were written from a partial reverse tree. The shipped
`StreetLegal_Redline.exe` has since been decompiled in full (4393 functions,
Ghidra 12.2) and its 353 native methods recovered. Two statements above are now
superseded; the full extraction is in
[CAR_MODEL_RECOVERY.md](CAR_MODEL_RECOVERY.md).

- **`ClutchF` is read and stored, then never consumed.** `0x00448430` loads
  `Chassis.ClutchF` into the physics struct at `+0xC8`; the vehicle step
  `0x00454500` contains no reference to that offset at all. The original's clutch
  is the input value `clutch` (car `+0x1DBC`, 0 = engaged, 1 = released) scaling
  the torque that reaches the wheels as `1 - clutch` -- the `arcade_gear_engage`
  contract recovered earlier is right, but it is the *whole* clutch model, not a
  force limit on top of one. So "`ClutchF` caps what the clutch can carry, and the
  release scales it" is not what the original does; the cap does not exist.
- **The engine's torque tables are not authored values.** They are the output of a
  simulated engine cycle (`0x004B77F0` sweep over `0x004B6BF0`/`0x004B7020`),
  converged per rpm to within 5% over up to ten iterations, with bore, stroke,
  compression, valve timing, spark timing, turbo/supercharger pressure, intercooler
  and mixture as inputs. Runtime only interpolates those tables
  (`0x0043BE70` / `0x0043BEE0`), piecewise-linearly, returning **0** outside the
  table's range. The thermodynamics therefore never runs in the frame loop, which
  is why the recovered runtime model is small: tables, friction, starter, a
  rate-based limiter, nitro and wear.

## 12. `ClutchF` is a phantom, and what removing it exposed

Recovered from the original executable: `Chassis.ClutchF` is read into `physics+0xC8`
and **that offset is never touched by the physics step**. The clutch in the original is
the pedal input, `engage = 1 - clutch`, which scales torque. There is no "force the
clutch can carry" limit anywhere in it.

We had one, at 500 Nm, in the middle of the driveline:

```
thrust = min(torque(rpm) * throttle * multiplication, 500) * ratio / radius
```

That single line deleted every engine above 500 Nm. A driver comparing a turbo four
with a supercharged V8 over the same stretch of road got **120 and 124 km/h** -- within
three percent, because both were clipped to the same number. Installing a supercharger
did nothing, for exactly this reason. The ceiling is gone; the clutch is now only the
engagement input, as in the original.

`more_torque_reaches_the_road` pins it: two identical cars, 200 Nm against 600 Nm, and
the strong one must be at least a quarter quicker to 100 km/h, more than 10 km/h faster
at the top, and the one that breaks traction. Measured now:

| | 0-100 | top speed | wheel slip |
|---|---|---|---|
| 200 Nm | 6.36 s | 140.4 km/h | 13.1 m/s |
| 600 Nm | 4.43 s | 246.7 km/h | **667 m/s** |

### 12.1 The 667 m/s was the next defect, and it is fixed

The original multiplies by two at the clutch: `torque_at_clutch = 2 * M * throttle`
(recovered model 3.5). We shipped the torque without it -- half of every engine's
output, lost before the clutch. With the two in place the wheel took
`2 * torque * ratio` of impulse per tick with nothing reacting on the engine side, so
a strong engine drove the wheel out to **2 220 m/s** of slip and the car was dragged by
a saturated tyre: acceleration set by grip, engine power invisible. That is the same
complaint in a different shape.

The fix is structural, and comes from the same recovery: the wheel's speed is *set by*
the driveline (`shaft' = ...`), not integrated freely -- so the wheel cannot turn faster
than the gearbox commands. `spin` is capped at `engine_omega / ratio` for the driven
wheels. Measured on the same fixture after that:

| | 0-100 | top speed | wheel slip |
|---|---|---|---|
| 200 Nm | 6.16 s | 171.0 km/h | 10.7 m/s |
| 600 Nm | 4.38 s | 227.9 km/h | 12.3 m/s |

Three times the torque now buys 29% off the 0-100 time and 57 km/h at the top, and the
slip is wheelspin rather than a runaway. `more_torque_reaches_the_road` holds it there.

Still to come from the recovery, and not to be guessed: the exact wheel update
(`force = (dShaft / sum|drive|) * drive / radius` -- what it is added to, and with what
inertia), `J_drive`, `mult_normal`, and whether the engine's reaction back through the
coupling changes the launch. The two-inertia coupling of 4 is specified; our branch
still holds the engine at a converter stall point of my invention, which is the last
invented number left in this path.

### 12.1a The old 667 m/s note

That slip is not a physical number; it is an artefact of how the faithful branch
couples the engine. It prescribes the engine's speed from a converter target and then
pushes `torque * ratio` of impulse into the wheel's spin while nothing reacts on the
engine side. A tyre can only take force back out of a wheel at `grip * radius /
inertia`; past that, the wheel accelerates without limit and the car is dragged by a
saturated tyre -- grip-limited acceleration that barely notices engine power, which is
the same complaint in a different shape.

The original does not have a torque balance there either: its drive is a **force**,
`engage * clamp(drive) * radius_mul(radius)`, and how the wheel's spin follows is part
of what has not been recovered yet. Inventing that here would be the third time this
session that a plausible-looking number turned out to be the bug, so the runaway stays
in place, visible and pinned, until the original's wheel/drive relationship is known.

### 12.1b Correction: the factor of two is not drive torque

The closed-answer pass on the original makes this unambiguous:

```
if (throttle <= 0.01) car[0x1E28] = 0;
else { fVar2 = M*throttle + M*throttle; car[0x1E28] = fVar2; }
```

`car+0x1E28` is read in exactly one place -- the transmission wear formula at
`0x0043C260:71`. The driveline does not see it. What the driveline carries is `M`, the
interpolated table value, minus friction.

So my earlier claim -- "we were shipping the torque without the original's factor of
two, half of every engine's output lost before the clutch" -- was wrong, and §12.1 was
written on it. The two is a wear-input quantity that happens to equal `2*M*throttle`.
It has been removed from the drive path.

With `M` in place the numbers become honest rather than dramatic:

| | 0-100 | top speed | wheel slip | share of tyre limit |
|---|---|---|---|---|
| 200 Nm | 8.20 s | 126.7 km/h | 2.9 m/s | -- |
| 600 Nm | 4.38 s | 213.5 km/h | 12.3 m/s | -- |
| Mazda (225 Nm table, friction 1.95) | -- | -- | 0.14 m/s | **33%** |

Three times the torque still halves the time to 100 km/h, so power reaches the road.
And the Mazda's launch asks for **33% of what its rear tyres hold**: with this engine's
table there is nothing to spin. A 506 Nm engine asks 13.7 kN of a 19.5 kN tyre -- 70%,
also short. Any claim that the original spins these cars at a launch has to come from
the tyre model (5.3) or from the table generator (the 225 vs 396 Nm discrepancy), not
from the driveline.

### 12.1c The driveline constants, recovered, and why they are not in the code yet

Everything the two-inertia coupling needs is now known:

```
J_engine  physics+0xE0  = 13.5162      J_drive physics+0xE4 = 964.706   (per car: 625/700/1100 seen)
K         .data 0x0060E028: 4.0 in the file, 6.0 in memory -- the mod patches .data at start-up
k = inv_d / (K*J_engine + inv_d),  inv_d = J_drive/ratio^2
  gear 1 .. 6:  k = 0.138 / 0.274 / 0.441 / 0.577 / 0.643 / 0.742
exchange 0.999, wheel+0xC4 = 1.0 on every wheel, radius lives at wheel+0x60
rpm is rad/s up to car+0x1E18*9.549296; RPM_limit and the table axis are rev/min
```

Implemented against that, and **not shipped** -- the numbers below are what it did:

| attempt | 0-100 | top | note |
|---|---|---|---|
| shared delta on both sides | -- | 1.1 km/h | shaft fell *below* the wheels; engine stuck at idle |
| inelastic split `(1-k)`/`k` | -- | 1.1 km/h | engine flared to 7047, wheels never turned: engagement 0 |
| engagement from the middle branch | -- | 60.5 km/h | moves, then plateaus in first with the engine on the limiter |

Two defects I hit and understood:

1. **The wheel update needs the split, not a shared delta.** It is an inelastic
   exchange: the shaft closes `(1-k)` of the gap and the engine closes `k`. One delta
   applied to both sides pushes the shaft the wrong way at a standstill.
2. **The auto-clutch's own branches contradict each other.** As written, the middle
   branch gives the pedal as `1 - (rpm-idle)/(target-idle)`, while the two boundary
   branches set it to 1 both below shaft speed and at or above the target -- read with
   1 = pressed, that opens the clutch exactly when the engine passes its target, which
   is where a real one closes. Read with 1 = engaged it drags a standing car to a stall.

Until that polarity is settled by watching `car+0x1DBC` (and wheel rpm) through the
first second of a full-throttle launch from rest, the coupling cannot be ported without
inventing the very thing this whole recovery exists to remove. The converter path the
game runs today is left in place; it has one invented number (the 0.8 stall point) and
it is the last one in the drive path.

### 12.4 The measured launch, and the driveline built from it

A live trace of the first second and a half of a full-throttle launch settled three
things that no amount of reading had:

**1. There is no `dt` in the engine step.** `rpm += M/J_engine` runs per frame, and the
frame is the step. Measured: 1000 -> 7430 rpm in 125 ms, about 51 000 rpm/s. Our engine
integrated `M*timestep/J`, sixty times slower, and the converter's invented stall point
was hiding it. That single line is "the revs are lazy".

**2. The clutch does not slip.** `car+0x1DBC` moves 0 -> 0.312 -> 0.357 -> 0 over
235 ms, but the engine/shaft ratio held 13.05..13.20 against a 13.18 overall ratio the
whole time -- under 1%. So it is a regulator for launch smoothness, not the thing
between the engine and the road, and modelling the link as closed is faithful to within
1%. What slips is the **tyre**: the driven wheels went 0 -> 538 rpm in 125 ms with the
car still standing, then sat at 540..600 rpm while the rears grew -13 -> -462. A burnout.

**3. The gearbox does not shift while the wheels spin.** The engine sat on the limiter
in first for the whole 2.5 s of that burnout. `0x0044D410` blocks the shift on the
average driven-wheel speed, which is why "the car stalls in second" and "it upshifts
mid-burnout" are the same missing gate.

The limiter overshoot in the row (7500 limit, peaks to 7890, +5.2%) is what the
`frac` formula of 3.5 predicts, so the clamp is +5%.

Built from that, in the faithful path:

```
engine_omega += net / J_engine                  // per step, no dt
wheel_shaft   = sum(spin * drive) / sum|drive|  // rad/s, C4 = 1
inv_d         = J_drive / ratio^2
k             = inv_d / (K*J_engine + inv_d)    // K = 6.0 in memory
shaft'        = shaft + 0.999*(1-k)*(rpm - shaft)
rpm'          = rpm   - 0.999*k*(rpm - shaft)
spin_i       += (shaft'/ratio - wheel_shaft) * |drive_i| / sum|drive|
```

Measured after: flare 60% at **0.02 s** (was 2.08 s), 0-100 **4.66 s**, top **228.1**,
max rpm 7350 on a 7000 limit (+5.0%), Mazda launch slip **15.95 m/s** at **73%** of its
tyre limit -- a burnout, where the same car previously pulled 0.14 m/s of slip and 33%.

Two things the port needed that the recovery does not spell out, and why:

- **the idle governor.** With no `dt` the friction takes the engine to zero in one step
  the moment the throttle shuts, which stops the gearbox schedule (it returns early on a
  dead engine) and left a braking car stuck in third. The engine is held at the recovered
  idle speed instead.
- **stopping at rest.** The driveline holds the wheels at idle speed through the link,
  so a braking car creeps and never stops. The link opens when the brake is down at low
  speed, and a stopped car is put in first regardless of the shift lock -- both are the
  behaviours the reference row shows at a standstill.

**Open, and honestly so:** the clutch regulator's arithmetic. The middle branch of
`0x0044DD70` gives the pedal as `1 - (rpm-idle)/(target-idle)` while its boundary
branches set it to 1 both below shaft speed and at or above target, which contradicts
itself under either reading of the polarity. Modelled as closed because the measurement
says it is closed to within 1%. The regulator shape remains unported.

### 12.5 The engine is weak because the sweep does not converge

The driver's read on the MC GT -- "it is not as fast, it does not want to slide, there is
no burnout" -- is not the driveline. The driveline now spins the wheels in the test
stand. What the car lacks is torque, and the torque table comes out of the sweep.

Measured state of the sweep for that engine:

```
cyl=8   swept=0.000809 m3 (6.5 L)   compression 9.05
air 0.18 / limit 0.30        fuel 0.0148 / limit 0.06      <- not starved
indicated 236.0 Nm   net 225.3 Nm
```

225 Nm over 6.5 L is an indicated mean effective pressure of **4.4 bar**. A real engine
of that size makes 9 to 12. The game's own engine screen shows 396 and 506 Nm for a block
of this class, which is **7.7 and 9.9 bar** -- factors of 1.75 and 2.25 over what we
produce. That is the "weak engine", quantified, and it is the same 1.76/2.25 the closed
recovery predicted from the boost chain.

**Settled, 2026-11-15: the sweep was fine, the conversion was not.** Reading
`0x004b7020` and `0x004b77f0` found three lines that did not match: the torque carried
`/(4*pi)` where the original carries `*0.125` (1.5708x), and friction was subtracted
from the torque where the original's table is gross torque and friction only ever leaves
the power figure (`rpm^3*2e-8` watts). Everything else -- burn, wall loss, intake and
exhaust blending, the work integral, `prepare()` -- already matched line for line.
Corrected, the MC GT reads **359 Nm** where it read 225, and it spins its wheels.
Details in `CAR_MODEL_RECOVERY.md` 14.

**And the non-monotonic heat response below was my own instrument, not the model.** The
`SLRR_DYNO_HEAT` hook multiplied `mixture_H` in place inside `prepare()`, which runs once
per rpm point on state that persists between them, so x1.5 compounded to 1.5^16 by the
top of the sweep. That is the whole of "the sweep does not converge". The hook is gone.
Reading the sweep directly settled it: `0x004b77f0` iterates 10 times to a 5% band and
the cycle underneath it is the one we already had.

What follows was the road to that answer, kept as a record of a wrong turn:

The obvious repair is the charge heat: the burn is `dT = heat / c_v`, so 1 MJ/kg gives
1400 K where a petrol-air charge carries about 2.8 MJ/kg. Scaling it does not work:

| heat scale | peak torque |
|---|---|
| x1.0 | 225.3 Nm |
| x1.5 | **4352.2** Nm |
| x2.0 | **1727.4** Nm |
| x2.85 | **188.6** Nm |

Those numbers were produced by compounding my own scale factor, and say nothing about
the sweep. The real defect was the conversion (above), and cranking engine parameters
would never have found it.

Next step is a line-by-line comparison of the sweep with `0x004B6BF0`, `0x004B7020` and
`0x004B77F0` (offered and not yet done). The numbers to check against are above; the
target is 7.7 to 9.9 bar of indicated mean pressure on this block.

### 12.6 The driveline must not drag the engine

Reported from driving: at a stop the idle sometimes fell to **334 rpm** and the car crept
forward slowly on it; reproduced with the handbrake held plus reverse engaged while still
rolling. Two causes, both in the link and both fixed by the same rule:

- **The idle governor ran before the link**, so the link could pull the engine below idle
  and the wheels were then fed that number. A closed throttle now never lets the driveline
  decide the engine's speed.
- **The link could turn the engine backwards.** Reverse held while the car rolls forward
  puts the engine-side shaft at a negative speed against a positive crank; a closed link
  drags the engine towards zero. This is the recovered boundary branch of `0x0044DD70`
  (`rpm < shaft -> open`) applied to the case it actually describes: opposite senses, not
  merely a slower engine. A clutch slips or opens there; it never drives the crank the
  wrong way.

Pinned by two tests: `a_car_in_first_at_idle_neither_bogs_nor_creeps` (5 s in first at
idle: lowest 784 rpm on an 800 rpm idle, crept 1.11 km/h) and
`reverse_and_handbrake_at_speed_do_not_strangle_the_engine` (reverse at 67 km/h then
handbrake: lowest 784 rpm, ended at 0.46 km/h).

**The first version of that rule broke cornering**, reported from driving within the
hour: spinning the car dropped the engine to ~900 rpm. In a pivot the driven wheels can
briefly rotate backwards about their own axles, which empties the same "shaft is
negative" condition in a FORWARD gear, and opening the link there cut the throttle
mid-slide. The gate is now `ratio < 0` as well: in reverse, the road is driving the
engine and the link must open; in a forward gear the driveline is what the engine drives,
whatever the wheels are doing. Pinned by `a_spin_does_not_drop_the_engine_to_idle`
(lowest 3558 rpm through a forced backwards wheel spin at full throttle).

### 12.7 J_engine was being shrunk twelvefold

The revs still fell to idle when the car was spun. The cause was not the gate but the
**inertia**, and the trace said so in one line:

```
before:  j_engine=0.415  j_drive=500.0  k=0.730
after:   j_engine=13.516 j_drive=500.0  k=0.077
```

`part_runtime` computed `inertia = .28 + .01 * engine_inertia`. That formula was
invented, and the field it eats is the recovered `physics+0xE0` itself: the MC GT
declares **13.5**, and the live game read **13.5162** on a car of that class. Shrinking it
twelvefold made the two-inertia coupling four times too stiff (k = 0.73 against the
recovered 0.14-0.31), so the link yanked the engine towards the wheels hard enough to
drop it to idle mid-slide. `inertia` now carries the declared value; the host slip model,
which is ours and was calibrated at 0.38, keeps its own `host_inertia`.

Measured after: **the engine holds 6490 rpm on a 6500 limiter while the driven wheels
spin at 71 rad/s against 22 of rolling**, first gear, no upshift, and a forced backwards
wheel spin at full throttle no longer takes it below **6756 rpm**.

**Also tried and reverted** (so it is not tried again): letting the driveline own a driven
wheel's speed outright, with the tyre acting through force alone. It reads well in the
recovery -- the wheel receives the shaft's new speed -- but it removes all torque
sensitivity, because the wheel is then always at engine speed and the car is always
traction limited:

| 80-160 km/h | 200 Nm | 600 Nm |
| --- | --- | --- |
| driveline owns the wheel | 3.26 s | 3.11 s |
| tyre reacts back on the wheel | **6.43 s** | **4.23 s** |

The tyre has to be able to react on the wheel. The spin-to-idle problem was the inertia,
not the ownership.

Two test bounds moved with the real crank, both recorded where they are asserted: the
faithful path is 5.4 s to 100 against the host model's 4.7 (a heavier crank revs slower to
the same power; the old assertion forced the smaller crank), and engine braking is
0.98 m/s2 against 0.08 coasting where it was 1.55 with the small crank.

### 12.8 What actually stops the driven wheel in a slide

Measured, not reasoned. `SLRR_SLIDE_TRACE` on `a_slide_at_speed_does_not_stall_the_driveline`
(61 km/h, 105 degrees of yaw, full throttle, first gear):

```
spin=48.52  road=-5.85  slip=20.89  lat=-16.29  Flong= 2943  Flat=3284  load=3522
spin=41.60  road=-5.81  slip=18.70  lat=-16.20  Flong= 3053  Flat=3400  load=3675
spin=33.68  road=-5.77  slip=16.21  lat=-16.11  Flong= 3155  Flat=3514  load=3824
spin=24.86  road=-5.73  slip=13.44  lat=-16.02  Flong= 3245  Flat=3625  load=3966
spin=15.27  road=-5.69  slip=10.42  lat=-15.92  Flong= 3315  Flat=3736  load=4100
spin= 4.96  road=-5.65  slip= 7.18  lat=-15.83  Flong= 3381  Flat=3837  load=4224
spin=-5.83  road=-5.69  slip= 3.88  lat=-15.73  Flong= 3613  Flat=3813  load=4339
```

Three things are in that table:

1. **The tyre force does not reverse.** `magic_formula` with `C = 1.55` cannot cross
   zero -- `C*atan(.)` reaches 2.435 against pi at 3.1416 -- so past the peak the curve
   flattens at 0.649 of its maximum and keeps its sign. The earlier suspect was wrong.
2. **`road` is negative.** At 105 degrees the wheel's forward axis points against the
   car's motion, so a rolling wheel would turn at `-5.85/0.31 = -18.9 rad/s`. The tyre is
   right to drag the wheel that way; the wheel goes from +48.5 through zero to -6.8 over
   seven steps (58 ms) and only then does the driveline pull it back.
3. **The driveline cannot win that fight, and it should.** Engine torque at the contact
   patch is `M*ratio/r = 315*12.9/0.31 = 13 kN`; the tyre makes 2.9 kN. The reason the
   tyre wins anyway is that the coupling is a **speed** exchange, not a torque source: in
   first gear `k = 0.036`, so it recovers about 2 rad/s of the gap per step while the
   tyre removes 4.8. Over a dozen steps the wheel is dragged through zero.

That is the remaining defect, and it is now pinned by number rather than by feel. The
engine side is fixed: the same slide ends at **4769 rpm** where it used to collapse to
idle, because the link no longer drags the crank down with the wheel.

**Correction, same day.** The conclusion above -- "the driveline needs a torque path" -- is
wrong, and `CAR_MODEL_RECOVERY.md` 4 says so in the recovered listing: the original is a
two-inertia *speed* exchange, exactly what this port implements, and
`clutch_torque = (engage*delta/k^2)*10` there is a slip measure read only by the gearbox
wear formula, not a transmitted torque. The missing piece is not a torque path but `clutch`:

```
engage = 1 - clutch        // continuous 0..1, from the auto-clutch regulator 0x0044DD70
```

This port has `engage` as 0 or 1 -- the open/close rules only -- so the link is either fully
inelastic or absent, with nothing in between. The regulator is unported (11), and the stub
"model it closed, it is closed to within 1%" was taken from a window in which it happened to
be closed. The launch row shows it moving: `car+0x1DBC` 0 -> 0.357 -> 0 over 235 ms.

Two things were done in the attempt that did not stand, recorded so they are not repeated:

- **`ClutchF` as a clutch capacity.** Wrong: 3.8 -- the field is read into `physics+0xC8` and
  the physics step never touches it. Any moment limit keyed to it is not original behaviour.
- **Rewriting the driveline into a torque path.** Seven measured variants, each failing at a
  named interaction (the guards held the drive off until the limiter cut the torque; a lock
  without the free integration lugs at idle torque; momentum bookkeeping freezes the crank;
  the free integration parks it on the limiter where the fuel cut removes the drive). The
  recovery says the shape was never the problem.

The same comparison also produced one measurement worth keeping: the driveline's capacity
through a 500 Nm clutch at 8.61:1 is 13.5 kN at the contact patch against 21.2 kN of tyre
grip, and at the recovered 628 Nm peak 17.0 kN -- i.e. under the tyre either way. Whatever
`clutch` does, first gear is traction limited in this car, and that is arithmetic, not feel.

### 12.9 `car+0x1DBC` is monotone: every writer only raises it

The auto-clutch regulator `0x0044DD70` was ported in full, both ways, and it does not
stand up -- because of what the writers actually are. Exhaustive list of every function in
the decompiled corpus that writes `car+0x1DBC`:

| function | write |
| --- | --- |
| `0x0044DD70` (auto-clutch) | ramps toward `wanted`, guarded by `if (clutch <= wanted)` and clamped to `wanted >= clutch` -- **only ever raises** |
| `0x0044E120` (anti-wheelspin) | `clutch = 1 - (1-clutch)*f` with `f <= 1` -- algebraically `>= clutch` for `clutch <= 1`, so **also only raises** |
| `0x0044D330`, `0x00447830` | force `0x3f800000` = **1.0** (fully disengaged) |

Nothing lowers it. The field is monotone non-decreasing, and `1.0` is disengaged, so a car
that once reaches 1.0 stays disengaged until something resets the whole car object.

Which means the trajectory this port was built on -- `car+0x1DBC` going `0 -> 0.312 ->
0.357 -> 0` over 235 ms of a launch -- **cannot have been that field.** A field that only
rises does not return to zero in 235 ms. One of the two is mislabelled, and the monotone
writers say it is the reading, not the writers: `0x1DBC` is some clutch quantity that
saturates, while the value seen oscillating in the live trace is a different offset (the
regulator's own previous-value field `car+0x1F44`, which is written from `0x1DBC` every
frame, is the obvious candidate to have been confused with it).

Measured consequence of porting it anyway, for the record: with the regulator alone the
car reaches **51.6 km/h** and stops -- the clutch opens and the launch never re-engages.
Adding `0x0044E120` alongside makes it **28.0 km/h**, worse, because the anti-wheelspin only
acts on driven-wheel speed above `0.2/dt` and a standing launch never gets there. Both
reverted.

What this changes about 12.8: the missing piece is still `clutch`, but it is not obtained by
porting `0x0044DD70` as if `0x1DBC` were a launch regulator with a round trip. The next
move is to re-read the live row against the field map -- find which `car+` offset actually
returns to zero over those 235 ms -- before writing any more of this.

### 12.10 The polarity, settled from the main step; and what is actually left

`0x00454500` lines 683-700, read line by line, confirm 4 exactly and settle the sign
question that cost a day:

```
k        = inv_d/(K*J_engine + inv_d)
rpm_free = rpm + M/J_engine
shaft    = wheel_shaft*ratio
delta    = (shaft - rpm_free)*k
engage   = 1 - clutch
shaft'   = ((delta + rpm_free) - shaft)*engage*0.999 + shaft
rpm'     = ((delta + rpm_free) - rpm_free)*engage*0.999 + rpm_free
car[0x1E24] = (engage*delta/k^2)*10            // next line: the gearbox wear formula reads it
```

Three consequences, each checkable against the writers table in 12.9:

1. **`engage = 1 - clutch`, so `clutch = 0` is a locked clutch** and the port's `engage = 1`
   during a launch is right. The launch row's 0 -> 0.312 -> 0.357 -> 0 is `car+0x1E24`, the
   slip measure -- the only quantity in this block that rises and returns to zero -- not
   `car+0x1DBC`, which 12.9 shows can only rise.
2. **There is no continuous clutch regulator to port.** The original's launch is a locked
   driveline, exactly as this port already models it.
3. **My "torque cap" was fiction.** It bounded the very term the listing applies
   unconditionally. Removed; the block is the recovered formula and nothing else.

Measured on the 105-degree slide after the removal (`SLRR_SLIDE_TRACE`, first gear, full
throttle):

```
spin= 48.52  road=-5.85  Flong=2943  rpm=7008  shaft=625.9  gap=+107.9
spin= 15.27  road=-5.69  Flong=3315  rpm=6735  shaft=197.0  gap=+508.3
spin= -5.83  road=-5.69  Flong=3613  rpm=6603  shaft=-75.2  gap=+766.7
```

The engine is thousands of rpm above the shaft and the exchange is running (the gap is
positive and growing, and the driven wheels share the correction by `|drive|/sum|drive|` =
0.5 each). The wheel still crosses zero. So the coupling is not what loses the fight -- the
**tyre is**, and it wins on arithmetic:

| | at the contact patch |
| --- | --- |
| driveline through a locked clutch at 1st, 500 Nm | 13.5 kN |
| same at the recovered 628 Nm peak | 17.0 kN |
| tyre grip, 10 860 N x 1.95 | 21.2 kN |

First gear is traction limited in this car under every clutch model. That is where the
remaining difference from the original lives, and it is a tyre/gearing question, not a
driveline one. Re-checking `max_load` (per wheel or per axle) and the friction scaling
against the script values is the next measurement.

### 12.11 The spin stall: the throttle was being cut by the game, not by the driveline

Captured with the per-frame slide trace in the live game, exactly as reported ("the wheels
stop and the revs fall, as if the gas were released"):

```
t=17.50  rpm=2489  gear=1  thr=1.00  fwd=-1.7  spin=-0.0  spinL=-0.0  F=6658  load=9523
t=17.50  rpm=   0  gear=0  thr=0.00  fwd=-0.0  spin= 0.0  ...        <- stopped, neutral
t=17.53  rpm=2441  gear=1  thr=1.00  fwd=-1.6  spin=-0.0  ...
```

Two separate mechanisms, both in our code and neither in the driveline:

1. **`direction_brake`.** `(ratio > 0 && forward_speed < -1)` is satisfied by any car that
   has rotated past ~90 degrees, because the body's velocity then points backwards along its
   own heading. Firing it sets the service brake to **1.0**, forces `gas = 0` and skips the
   entire driveline block. That is literally "as if the gas were released" -- it was. It is
   now gated on `drive.throttle < .1`: a driver holding the throttle is asking for drive,
   and a spin is not a parking situation.
2. **Anti-stall gated on the throttle.** The driver-only guard `gas <= .05` meant nothing
   opened the link while the wheels were held at a standstill with the throttle open, so the
   link dragged the crank 2489 -> 2170 -> 0 and `engine_running` went false, which drops the
   gearbox to neutral. The recovered regulator's own branch (`engine at or below idle ->
   fully disengaged`) is not gated on the throttle, and now neither is this.

Measured after both, in the stand (`a_spin_that_stops_the_driven_wheels_does_not_stall_the_engine`,
60 km/h spun past 90 degrees at full throttle):

| | before | after |
| --- | --- | --- |
| lowest engine speed | 978 rpm (idle) | **6937 rpm** |
| service brake applied | **1.00** | **0.00** |
| speed after 2 s | 1.5 m/s | 4.1 m/s |

The test asserts the mechanism (`brake_service() < .05`), not a speed threshold, and is
registered as test 17. Note which fix each source supports: the anti-stall gate is proven by
that test (it fails with the gate disabled), while the direction-brake gate is proven by the
live row -- the fixture's instantaneous velocity reversal does not satisfy
`forward_speed < -1` the way a real, rotating car does, so the fixture cannot carry it.

### 12.2 The engine's own numbers, from the game's engine screen

Same engine, with and without a supercharger, as the game itself reports them:

| | idle | redline | peak torque | peak power |
|---|---|---|---|---|
| without charger | 900 | 6500 | 396 Nm @ 4250 | 270 hp / 198 kW @ 6250 |
| with charger | 900 | 6500 | 506 Nm @ 3250 | 276 hp / 203 kW @ 6500 |
| destruction | | 9000 | | |

Three things follow, and they matter more than the ceiling did.

1. **The ceiling was clipping exactly this engine.** 506 Nm against a 500 Nm cap is
   6 Nm of everything the supercharger added, thrown away. It was the right line to
   delete.

2. **The supercharger is worth +28% torque and +2% power.** Peak power barely moves:
   270 to 276 hp. A terminal speed at the end of a fixed distance follows power, so
   120 km/h against 124 km/h is what this data predicts -- the two cars were not the
   same because the physics flattened them, they were *nearly* the same engine at the
   top end. The +28% is all in the mid-range, where a launch and a corner exit live.

3. **Our launch holds the engine in the wrong place.** The converter stall point is
   0.8 of the limit, which for this block is 5 380 rpm. Peak torque is at 3 250. So the
   launch spins the engine where the curve has already come down, which is the exact
   shape of "high revs, no go". 0.8 is my invented number, and it fights the engine's
   own data.

With the cap gone, the thrust this engine can put on the road is
`506 Nm x 8.61 / .318 m = 13.7 kN`. The rear tyres hold `1.95 x axle load`, so the
launch is at the tyre limit when the driven axle carries about 7 kN. `SLRR_DRIVE_TRACE`
now prints that load next to the force, per half second, so the utilisation on a real
car is a number rather than an argument.

Open, and not to be invented again: the 667 m/s wheel spin of 12.1, and the launch
rpm, which the original's own rpm behaviour has to answer.

## 13. The reference model: `docs/CAR_MODEL_RECOVERY.md`

The original executable's vehicle model has been recovered and lives in
`docs/CAR_MODEL_RECOVERY.md` (from the reverse-engineering pass, with addresses,
constants and the step order). It supersedes every "recovered contract" note earlier
in this file. The parts that change decisions here:

- **`ClutchF` is dead** (already removed, §12): the clutch is `engage = 1 - clutch`,
  there is no force ceiling.
- **The driveline is a two-inertia coupling**, `k = inv_d / (4.0*J_engine + inv_d)`,
  exactly the shape the host branch already has -- so my "faithful" rigid/converter
  branch was the least faithful of the three.
- **`torque_at_clutch = 2 * M * throttle`**: the original's own factor of two at the
  clutch. My slip-dependent "converter multiplication" is an invention that happened
  to look similar.
- **The engine's torque is a table**, linearly interpolated, and **zero outside its
  first and last point**; thermodynamics is computed once at part-install time, never
  per frame.
- **Gear selection is not native** -- `car.gear` is written from scripts/input. Any
  shift schedule we implement is ours, not the original's.

### 12.3 Engine losses, and two units that had to be read as written

The recovered model 3.5 gives the friction terms in rev/min and a viscous bound in
rad/s, taken as a **minimum**:

```
drag = (rpm / (0.05*maxRPM)) * (0.05*maxRPM)^2 * friction_fwd     (rpm > 0)
drag = rpm^2 * friction_rev                                       (rpm <= 0)
visc = 0.5 * |omega_rad| * engine_inertia
friction = min(drag, visc)
```

Two ways to get it wrong, and I found both: reading the bound as a maximum made it
1131 Nm at 6000 rpm and the car could not pull away at all (top speed 4 km/h), and
feeding it rev/min where the bound wants rad/s scales it by 9.55. As written it is
211 Nm at 6500 rpm for `friction_fwd = 1e-4` -- real engine braking.

It is applied against the closed part of the throttle: at full throttle the engine
makes power, with the throttle shut this is the only thing slowing a car in gear.
Which of the two the torque table is net of is still open (recovery 11), so the
weighting keeps full throttle honest and gives a lifted car its braking.

Measured (`lifting_off_in_gear_slows_the_car`): **1.55 m/s2 in gear against 0.08 m/s2
in neutral**, where before the car coasted as if it were in neutral.

Also checked and left alone:
- **the rate-based limiter already matches** 3.5 exactly (`rate = 2*rpm - rpm_prev`,
  proportional cut), so nothing to port;
- **the table already reads zero outside its ends** (`sample_curve` returns 0 before
  the first node and after the last), as 3.4 requires.

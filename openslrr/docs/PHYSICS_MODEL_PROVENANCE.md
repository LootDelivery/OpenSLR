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

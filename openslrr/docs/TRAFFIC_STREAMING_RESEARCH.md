# How other projects stream traffic, and what applies here

Status: research note for U2-B.3 (traffic streaming, third pass).
Scope: the spawn/despawn policy only. Car following, signals and routing are
U2-C…U2-E. This note exists because the road test reported the same defect twice
— a car appearing in plain sight — and the second fix (160 m of clearance ahead)
did not cure it, which means the assumption behind both fixes was wrong.

## 1. Why a distance rule cannot work in this engine

The previous two attempts treated the spawn distance as the thing that hides a
car: 180–240 m in U2-B.1, then 160 m ahead / 105 m to the side / 60 m behind in
U2-B.2. The assumption is that a car that appears far enough away is not
noticeable. That is true in a game with a short draw distance and heavy fog, and
false here:

| Quantity | Value in this tree | Source |
|---|---|---|
| Distance fog, driving camera | starts at 1.5 km, saturates at 3 km | `GameRefState::fog_near/fog_far`, `gameref.cpp:1071`, camera default in `gameref.hpp:120` |
| Ground weather fog | separate globals, same order | `gameref.cpp:1071` |
| Camera half angle | 45°, or 18…65° with autozoom | `gameref.cpp:905` |
| Instances drawn | no per-instance distance cut, only the view frustum | `scene_instances_collect_batches` |

So a car at 160 m is a car fully inside the frustum, un-fogged, drawn at ~10 px
height on a 1000 px wide frame. There is no distance at which a spawn is
invisible short of the horizon, and `retire_radius` is 330 m, three orders of
magnitude below the fog start.

The measurement agrees: with the 160 m front clearance in place, the log
reported the nearest ahead spawn at 162 m, and the player reported cars
appearing "at a hundred metres, maybe more". Both were describing the same
thing.

## 2. What other implementations do

**SUMO (microscopic traffic simulator).** Insertion is a *physical* decision, not
a geometric one: a vehicle may only be inserted when it does not intersect
another vehicle plus its minimum gap, when it keeps a safe distance to the leader
according to its car-following model, when every follower also keeps a safe
distance, and when it can brake for upcoming non-prioritised intersections and
stops. If any of that fails the vehicle waits in an *insertion queue* and the
insertion is retried on later steps; the resulting `departDelay` is recorded
rather than hidden
(<https://sumo.dlr.de/docs/Simulation/VehicleInsertion.html>).

Two ideas transfer directly and are already in `traffic_streaming`: candidates
are lane positions with a minimum gap and a per-lane capacity, and a refused
candidate costs nothing but a retry on the next pass. SUMO says nothing about
visibility, because it has no camera.

**Game engines: pop-in is solved by fading, not by distance.** Distance-based
hiding is what draw distance and fog are for (see *Draw distance*, Wikipedia:
"once-distant objects or terrain may suddenly appear without warning",
<https://en.wikipedia.org/wiki/Draw_distance>). When a game cannot afford to hide
the transition, it *blends* it: Unity's LOD Group has a `Fade Mode` of `None` /
`Cross Fade` with an `Animate Cross-fading` time, precisely so a level-of-detail
switch does not pop
(<https://docs.unity3d.com/Manual/class-LODGroup.html>). The same idea appears in
Unreal as dither-based fading (dithered temporal AA), and in countless open-world
games as a dissolve on streamed actors.

**Open-world practice: spawn off screen, recycle off screen.** The rule of thumb
for ambient actors is that they may only be created or destroyed while the
camera cannot see their position — behind the camera, outside the horizontal
frustum, or behind an occluder — and that actors enter the player's view by
moving. That is the only policy that needs no fade and no distance trick.

## 3. What this tree does now

`StreamView` carries the real camera: `eye` and `look` come from
`gameref_world_camera`, `view_cos` is the cosine of the engine's half angle plus
a 6° margin, and `view_range` (300 m) is the distance beyond which a car is a
couple of pixels and a spawn is not noticeable. `stream_point_visible` is one dot
product, and both the spawn picker and the retirement loop use it:

* a candidate spot inside the cone is refused with the reason `in view`;
* a car may not be retired (or replaced) while it is inside the cone, unless it
  is 1.6× past the retire radius, so the population is recycled only where
  nobody is looking.

The clearance zones stay, but they now only describe *how far* a car has to be
from the player's body in each direction; the visibility rule is what makes the
spawn unnoticeable.

Measured over 89 spawns while driving at density 1.0: the angle off the camera
axis ranged 49…180°, with 6 spawns at 30…60°, 25 at 60…90°, 29 at 90…120°, 12 at
120…150° and 17 at 150…180°. Nothing appeared inside the 51° cone, and the
population still filled to its target of 80 cars, with 13…21 cars visible ahead
within 220 m after the population had settled — those arrived by driving.

## 4. The trade-off, stated plainly

Because nothing may appear in view, the forward road is fed only by traffic that
drives into it: cars from side streets, cars on lanes that curve out of the cone,
and cars seeded beyond 300 m. On a long straight with no junction for several
hundred metres the far end of the road can look empty for a while, which is the
opposite complaint from U2-B (the forward sector must not be forbidden).

The way to have both — dense traffic ahead *and* no pop — is a fade-in of about
0.4…0.6 s on newly created cars, which is what the engine references above do.
The hook is small and already located:

* `fs_mesh.sc` ends with `gl_FragColor = vec4(shaded, outA * u_color.a)`, and
  `u_color` is a per-draw uniform the renderer already sets for every mesh, so a
  fade can ride on `u_color.a`;
* the opaque state ignores alpha, so the fade has to be a dithered discard
  (`if (u_color.a < 1 && hash(gl_FragCoord.xy) > u_color.a) discard;`) or a
  blended pass; dithering avoids sorting and works in the opaque path;
* the traffic runtime already owns every part of a car it creates, so it can set
  that alpha per car per frame and ramp it from 0 to 1 over the fade time.

That is a rendering change (one shader branch, one path for traffic-owned parts),
which U2-B deliberately excluded, so it is not in this block. With it, the
front clearance could drop back to ~120 m and the forward sector could be seeded
directly instead of only by driving.

## 5. Decision

Adopted:

* the gap, capacity and refusal-and-retry rules, which match SUMO's insertion
  constraints;
* dithered fade-in of 0.7 s on every car this module creates, riding on the
  existing `u_vehicleDiffuse` draw and a new `u_vehicleFade` uniform. The road
  test confirmed the need: off-screen-only spawning was correct but left the
  first minutes of a drive visibly empty, exactly the trade-off predicted here;
* off-screen retirement, so a car is never deleted — or replaced — in view.

Rejected: solving pop-in with distance alone (impossible with 1.5 km fog) and
spawning behind occluders via ray casts (`scene_instances_pick` is a full CPU ray
against instance meshes, too expensive for 48 candidates four times a second).

## 6. The population has to be reached, not trickled

A target is only half of a population policy; the other half is how fast it is
reached. Filling at one car per quarter second is invisible while driving —
replacements arrive as fast as cars leave — but it is very visible on arrival,
when the checked-in target is 100 and the count starts at zero. The road test
described exactly that: the city "comes alive together with the player". SUMO has
the same distinction in its insertion queue: a vehicle that cannot be inserted is
retried every step rather than every few seconds. The same applies here: the fill
rate is a function of the deficit (24 cars per pass while a ground is being
filled, one otherwise, and a shorter pass interval while short of target), and
the churn guard — the four second cooldown on a lane that was just emptied — only
applies at saturation, because below target it starves long lanes.

## 7. Implementation notes (U2-B.3)

| Piece | Where |
|---|---|
| Dissolve | `shaders/fs_vehicle.sc`: `u_vehicleFade.x < 0.999` discards fragments against a hash of `gl_FragCoord`, so the car dissolves in the opaque state — no blending, no sorting. |
| Uniform | `bgfx_lib_backend.cpp` creates `u_vehicleFade` and destroys it with the other vehicle uniforms; `bgfx_lighting.cpp` sets it per group draw from `SceneInstance::fade`. |
| Plumb | `scene_instances_set_fade(owner, fade)` keeps owner→fade in a small table; the collector applies it to every instance whose `vehicle_material` belongs to that owner, so a car fades in whole. |
| Ramp | `traffic_runtime.cpp` sets 0 at creation and ramps with `stream_fade(age, fade_seconds())`; the entry is dropped once the car is solid, so the lookup cost disappears with it. |
| Knob | `SLRR_TRAFFIC_FADE=<seconds>` overrides the fade time; 0 makes cars appear instantly, which is how the dissolve was proven (1000 s leaves cars present but invisible). `SLRR_TRAFFIC_CLEAR=<scale>` scales the spawn clearance: 1.0 is 200 m ahead / 95 m side / 55 m behind, 1.4 is 280 / 133 / 77. |
| Clearance split | Unequal by direction because the dissolve buys less ahead than beside. At a fixed spot, noon, density 1.0, a 200 m front clearance put the nearest ahead spawn at 206 m with 44 cars inside 340 m of road ahead, while the sides stayed at 103 m and kept 10…14 cars inside 140 m. |

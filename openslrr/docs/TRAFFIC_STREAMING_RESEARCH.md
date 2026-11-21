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

* Adopted now: off-screen spawn and off-screen retirement, plus the existing
  gap/capacity/insertion-retry rules that match SUMO's insertion constraints.
* Deferred with a located hook: dithered fade-in for streamed cars, which would
  remove the forward-flow trade-off above.
* Rejected: solving pop-in with distance alone (impossible with 1.5 km fog) and
  spawning behind occluders via ray casts (`scene_instances_pick` is a full CPU
  ray against instance meshes, too expensive for 48 candidates four times a
  second).

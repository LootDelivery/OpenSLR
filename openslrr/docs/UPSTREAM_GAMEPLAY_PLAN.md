# UPSTREAM GAMEPLAY PLAN — U1-E and the deferred U1-F items

Companion to `UPSTREAM_INTEGRATION.md`. Everything below is **not ported**.
This file records the order, the dependencies and the open questions so the
next pass can continue without re-deriving them.

Baseline after U1-A…U1-F2 plus U1.1/U1.2: the garage opens, `--spawn-car` does
not crash, and `--bench-map ValoCity --bench-drive` drives the car to
`drive done`. That is the new floor every item below must not regress.

Note on the crash: U1-A removed the `Player.<init>` stub, which made the
untouched HUD OSD widgets visible and kept `draw_osd`'s layer list non-empty.
The real defect — `layers.front()` on an empty list — was fixed in U1.1, and
U1.1 also restored the correct HUD visibility (see
[UPSTREAM_INTEGRATION.md](UPSTREAM_INTEGRATION.md), U1.1). Traffic bots in
Phase 1 still need the constructor running: it is what builds every player's
`parts`, `carlot` and `osd`.

## Why these are not in U1

| Item | Reason it was deferred |
|---|---|
| #180 roads / traffic / race AI | Depends on 12 missing `GroundRef` traffic/route natives (`addTrafficCar/N/P`, `remTrafficCar`, `notifyTrafficCar`, `delTraffic`, `setTrafficCarBehaviour`, `haltTrafficCross/Path`, `findRoute`, `getRouteLength/Pos/Dist`, `alignToRoad`, `getStartDirection`). Upstream ports them together with the AI driver, so the work is "implement the route/query layer, then the bots", not a patch we can lift. |
| #205 + #48 vehicle contacts / dynamics | Upstream rewrote `advance_free_bodies`/`advance_loose_panels` around a `ContactVehicle` list with shared 120 Hz substeps, mass/inertia ownership, wheel loss and physical flaps. Our tree still has the single `controlled_vehicle` model. Lifting the diff would mean importing the whole vehicle-dynamics architecture, which is a subsystem port, not an integration fix. U1-D already left the `try`/restore shape that port needs. |
| #146 RPK descriptor/property cache | Rewrites resource resolution: `by_type_id` becomes a lo16 map, `find_entry` changes semantics, payload/RSD caches appear with mutexes, `generation()` is introduced. `RENDER_DATA_CONTRACT.md`/B4.1.2 pin the strict-resolution contract (normalized path + local id, explicit ambiguity, the lo16 fallback is legacy/debug only), so the cache must be designed against that contract, not inverted from upstream. |
| #200 GC finalizer/heap lookup | 172 lines of `gc.cpp` rewrite. `VM_GC.md` documents our reachability and finalizer invariants; the change needs its own before/after evidence, not a blind port. |

## Ordered plan

### Phase 1 — world queries (prerequisite for #180)

1. `GroundRef` water/limit natives: `setWater` ×2, `addWaterLimit`. Pure data,
   no consumers yet; they are what `--bench-map` already logs as unbound.
2. Route layer: `findRoute`, `getRouteLength` ×2, `getRoutePos`,
   `getRouteDist`, `getStartDirection`, `alignToRoad`. Needs the TRC/ROC
   spline data our map loader already parses for `scene_instances`; start by
   proving one route query against `ValoCity` and comparing with the shipped
   `.trc`/`.roc` payload.
3. Traffic population: `addTrafficCar/P/N`, `remTrafficCar`, `delTraffic`,
   `notifyTrafficCar`, `setTrafficCarBehaviour`, `haltTrafficCross/Path`.
   These create and retire script-owned cars, so they depend on the U1-A
   constructor work (a traffic bot's `Player`/`CarLot` context) and on
   `vehicle_spawn_stock`.
4. Only then the AI driver commands and the race flow from #180 (lane
   following, crossing sync, traffic-light sync, finish-plane runout).

Gate for Phase 1: a deterministic bench run that spawns N bots on a fixed
route, logs each bot's command per second, and is reproducible run to run.

### Phase 2 — vehicle contacts (#205 + #48)

1. Replace the single-car assumption with a `ContactVehicle` list built from
   the live controlled/live vehicles; keep `advance_free_bodies()`'s current
   signature behind a wrapper so U1-D's fault containment stays.
2. Car-to-car OBB and AI-to-movable-body contacts, then shared 120 Hz
   substeps, then mass/inertia ownership, then wheel/suspension dynamics,
   then flaps and wheel loss, then damage impulses.
3. Every stage keeps a containment test: one bad body disables itself and the
   step keeps running (extend `tests/test_physics_recovery.cpp`).

Gate for Phase 2: `--bench-drive` with two cars touching, plus the existing
`drive done` line unchanged for the single-car case.

### Phase 3 — deferred U1-F items

1. #146 resource cache: design against B4.1.2, add the strict-resolution tests
   first, then the cache. Keep `payload_disk_reads`/`rsd_parse_count` counters
   as the evidence.
2. #200 GC: write the reachability/finalizer test first, then the sweep.

## Explicitly out of scope (never ported)

Graphics and media from upstream stay out of this tree:

- #235/#237/#279 `LightSource`/`PointLight`/`SpotLight`/`RectLight`, street and
  car lighting, `patch/` shader and lighting changes, shadow/AO/HDR/reflection
  passes and their Java API. Lighting stays with `LIGHTING_AUDIT.md` and the
  L2 work.
- #206 `Painter` replacement: the legacy Painter contract (B4/A-series) is
  ours to design.
- #213 JSON save format: the save format is not changing; U1-B2 only made the
  writer atomic.
- #219 Cinepak and #229 FMOD: separate multimedia projects with external
  dependencies.
- #162 binary `Garage.class`: not copied. Upstream states the shipped
  `Garage.java` order was already correct and the defect was in the MWM class;
  without the exact SHA-256 of the installed binary there is nothing to verify.
- `patch/` as a directory: mixed correct/incorrect content, and the lighting
  half is out of scope anyway.

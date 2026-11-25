# UPSTREAM MAIN AUDIT — what the author's repository has that this tree does not

Audit only: nothing in this tree was changed to produce it.

Question: this tree was downloaded at one upstream state. What has the author done
on top of that state, which of it do we need, and which do we not. The traffic
work is already integrated here, but not necessarily completely.

**State:** §1-§8 audit `17115433` -> `d661d250`; §9 adds the seventeen merges
after that, up to `d13e7102` (2026-11-24), each with a verdict. The short
answer for this tree's open defects is in §9: none of them was fixed upstream.

## 0. Answer to "does `UPSTREAM_INTEGRATION.md` cover the whole difference"

No. It is a registry of upstream changes that were **considered**: nine blocks
ported (U1-A…U1-F2, U2-A, U2-B), seven deferred or rejected with a reason, written
against upstream `5b002e42`. Everything upstream has done since — and everything
between our base and that commit that was never examined — is absent from it. It
covers roughly a third of what is listed below, and by design it never listed the
files that differ because our own work moved.

This document is the missing half: every first-parent merge upstream has made
since the revision this tree was taken from, with a verdict.

## 1. Base, established rather than assumed

Our import snapshot (`3371388`) was compared against every first-parent commit in
upstream's history, counting files whose blob hash is identical:

| upstream commit | date | identical | note |
|---|---|---|---|
| **`17115433`** | 2026-11-14 | **255 / 293** | best match by a clear margin |
| `1de56f08` | 2026-11-16 | 251 / 293 | after #118 |
| `71aec8b3` | 2026-11-16 | 251 / 293 | after #117 |

The remaining 38 files are the pre-import work this tree already carried (the HDR
present pass, interface separation, the reflection backend, the font fix). So:

- **base = `17115433`** — "Draw cars between physics ticks so turns are smooth
  (#45)", 2026-11-14;
- **upstream today = `d661d250`** — 2026-11-23, PR #215 merged;
- between them: **95 first-parent merges, 327 commits**; in `src/`, `include/`
  and `shaders/` that is **155 files, +24 918 / -5 657 lines**, plus 81 test files
  (+18 966).

## 2. Method, and what it can and cannot say

Three filters were run, in this order:

1. **File equality** (our file vs upstream before/after each merge). Nearly
   everything comes out "differs", because this tree ports by re-implementing
   against its own code rather than copying. Useful for "we never had this file",
   useless as a verdict.
2. **Distinctive symbol probe** (identifiers added by a merge, searched in our
   tree). Also weak: shared vocabulary ("scene_instances", "reflection_") makes
   unrelated work look similar. It is a hint, not evidence.
3. **Targeted evidence** — a specific pattern per feature, searched in our source.
   This is what the verdicts marked "checked" below rest on, and where a verdict
   is not marked, it is a first-pass judgement that still needs a diff read.

The full merge inventory is in appendix A; the evidence table for the traffic
block is in section 3.

## 3. Traffic (#180): what of it is here, verified

| Piece | In this tree | Evidence |
|---|---|---|
| `addTrafficCar/N/P`, `remTrafficCar`, `delTraffic`, `notifyTrafficCar`, `setTrafficCarBehaviour`, `haltTrafficCross/Path` | **yes** | 42 hits across `race_runtime.*`, `traffic_runtime.cpp`, `test_traffic_runtime.cpp` |
| Route queries: `findRoute`, `getRouteLength/Pos/Dist`, `alignToRoad`, `getStartDirection`, `getNearestCross` | **yes** | 38 hits in `race_runtime.*`, `test_road_routes.cpp` |
| Road model from `city.trc` (edges, crossings, signal heads) | **yes** | `RoadRoutes`, `traffic_signal`, `traffic_project` — 73 hits |
| Population, lifecycle, retirement by distance, lane movement, externally animated wheels | **yes** | `traffic_runtime.cpp`, `traffic_streaming.cpp` (ours, not upstream's design) |
| Crossing permission and the signal phase consumer | **yes** | `race_traffic_signal_cap` in `traffic_runtime.cpp` caps the lane at the stop line (26…126 capped lane steps a second in a driven run) — U2-C, closed after this audit's first pass |
| Race brain / rivals driving (`race_brain_command`, finish-plane runout) | **no** | zero hits for `race_brain`; U2-D |
| ROC track / `race.spl` | **no** | zero hits for `ROC_track` or `.spl`; U2-E |
| Native vehicle assembly for traffic (physics-driven template cars) | **no** | U2-F |
| Traffic engine sound and per-car panning (#324, two commits) | **no** | `scene_ambience` only; nothing per car |
| Pedestrians passing narrow corridors (#294) | **no** | zero hits |

So the traffic integration is the natives, the road graph, the population model and
lane movement — not the signal consumer, not the race brain, not ROC, not the
native-vehicle path, not their audio. That matches what
`UPSTREAM_GAMEPLAY_PLAN.md` said after U2-B, and it is now confirmed by search
rather than by memory.

## 4. Verdicts

| Verdict | Merges |
|---|---|
| Already integrated here | 8 |
| Integrated in part | 1 |
| Not integrated — needs a look | 51 |
| Not integrated — solved differently here | 4 |
| Not integrated — deferred with a plan | 8 |
| Not integrated — feature, the driver decides | 10 |
| Not integrated — rejected by policy | 6 |
| Does not apply | 7 |

### Already integrated here (8)

| PR | Date | Commit | What | Files |
|---|---|---|---|---|
| #117 | 2026-11-16 | `71aec8b3` | U1-B1 save-file safety (atomic writer). **(checked)** | 9 |
| #118 | 2026-11-16 | `1de56f08` | U1-B2 save persistence. **(checked)** | 9 |
| #137 | 2026-11-16 | `896e9bd5` | U1-F1 initial wheel steering pose. **(checked)** | 2 |
| #147 | 2026-11-16 | `9b4de293` | U1-C1 TUFA validation. **(checked)** | 10 |
| #151 | 2026-11-17 | `6fffbe6c` | U1-C2 script argument and array safety. **(checked)** | 11 |
| #154 | 2026-11-17 | `bb45d125` | U1-F2 live GameRef lookup index. **(checked)** | 7 |
| #166 | 2026-11-17 | `27bce5c2` | U1-D physics fault containment. **(checked)** | 9 |
| #228 | 2026-11-21 | `5b002e42` | U1-A Player constructor, so the stock TREE body runs. **(checked)** | 3 |

### Integrated in part (1)

| PR | Date | Commit | What | Files |
|---|---|---|---|---|
| #180 | 2026-11-18 | `0a44cf44` | Shared roads, traffic and race driving — U2-A and U2-B are in, the rest is listed below. **(checked)** | 42 |

### Not integrated — needs a look (51)

| PR | Date | Commit | What | Files |
|---|---|---|---|---|
| #119 | 2026-11-16 | `c26ef62d` | Skin-mesh texture binding; we rewrote skin handling, needs a diff read. | 8 |
| #120 | 2026-11-16 | `784a7609` | Physics position validation; we reject non-finite frames, may already cover it. | 6 |
| #123 | 2026-11-16 | `933cf0a4` | Garage scene lifetime on re-entry. | 7 |
| #124 | 2026-11-16 | `c5798346` | SCX face-index / animation-key validation before allocation. | 7 |
| #135 | 2026-11-16 | `c7903575` | Standalone map props outside a race. | 8 |
| #136 | 2026-11-16 | `f2e766ed` | Paused character animation. | 7 |
| #138 | 2026-11-16 | `c15a839b` | Race time in milliseconds (integer time formatting). | 6 |
| #139 | 2026-11-16 | `a877a637` | Night-map objects and their lighting. | 14 |
| #140 | 2026-11-16 | `60d9d85c` | Tree foliage rendering. | 6 |
| #142 | 2026-11-16 | `a30c354a` | Clouds in daylight (they were night-only). | 11 |
| #143 | 2026-11-16 | `8ac81f03` | Vehicle polygon/back-face handling. | 8 |
| #144 | 2026-11-16 | `c19cfa0c` | Tree JVM integer money (rounding at the TREE boundary). | 6 |
| #145 | 2026-11-16 | `163d7898` | OSD resource lifetime; U1.1 did own lifecycle work here. | 15 |
| #148 | 2026-11-16 | `e5244f9b` | RPK map validation at load. | 9 |
| #149 | 2026-11-16 | `7737dbf4` | Audio playlist capacity; our audio host differs, worth comparing. | 8 |
| #150 | 2026-11-16 | `240af238` | VM lookup cost; U1-F2 covered scene lookups, not the VM index. | 8 |
| #153 | 2026-11-17 | `a15ab7b0` | Options-save retries. | 8 |
| #155 | 2026-11-17 | `411dda47` | Driver visibility traversal; we exclude the driver from capture, not from the scene. | 17 |
| #158 | 2026-11-17 | `dede4b57` | Gamepad axis fallback when a pad reports no axis. | 7 |
| #160 | 2026-11-17 | `4de3d3ca` | Class-lifetime constructors (objects surviving their owner). | 10 |
| #161 | 2026-11-17 | `9c1ab80b` | Career day static increment across sessions. | 7 |
| #162 | 2026-11-17 | `67cd5814` | Inventory-to-save transition. | 6 |
| #163 | 2026-11-17 | `9df8920d` | Fullscreen map presentation. | 9 |
| #164 | 2026-11-17 | `ae2d236b` | Version/build ordering in the exe. | 5 |
| #165 | 2026-11-17 | `f3638a14` | Map alpha (transparent scenery). | 11 |
| #167 | 2026-11-17 | `db8f0690` | World-position varying precision. | 3 |
| #168 | 2026-11-17 | `a5184d23` | File search paths (`findfile`). | 7 |
| #169 | 2026-11-17 | `57a56fc2` | Splash-window adoption on the first frame. | 6 |
| #170 | 2026-11-17 | `c6e563fe` | Track presentation (start line, banners). | 4 |
| #171 | 2026-11-17 | `d9883593` | Menu input debounce. | 7 |
| #172 | 2026-11-17 | `a2095e8c` | Tooltip double render. | 6 |
| #174 | 2026-11-17 | `6339249f` | Car-select presentation. | 10 |
| #175 | 2026-11-17 | `4e4b7828` | Trigger markers on the map. | 11 |
| #121 | 2026-11-17 | `c63a8cd6` | GameRef alias events; U2-A built incarnation keys on top of this area. | 6 |
| #177 | 2026-11-17 | `6fcc278e` | CMake dependency paths for generated shaders. | 7 |
| #199 | 2026-11-18 | `1a71e9e9` | GPU mesh data without per-frame CPU copies — real perf, in the file we rewrote. | 9 |
| #210 | 2026-11-19 | `0935ac66` | Compiler source missing from the build target. | 4 |
| #211 | 2026-11-19 | `eb029118` | Build speed: parallel cl.exe and a shared shaderc. | 5 |
| #222 | 2026-11-20 | `ed0a0b51` | Borderless window when the mode equals the monitor size. | 2 |
| #218 | 2026-11-20 | `7f629d5b` | Hot paths and dealer wheel preservation. | 2 |
| #245 | 2026-11-21 | `21ca10bb` | HUD fender indicator. | 10 |
| #272 | 2026-11-21 | `09f9f924` | Pedestrians passing narrow corridors and gates. | 3 |
| #305 | 2026-11-21 | `3730a5c6` | Loading scale for large car/mod collections (rpk_index). | 6 |
| #226 | 2026-11-22 | `ab3d284c` | Career race tests: AI routes, City tick, racer replies — behaviour plus tests. | 8 |
| #307 | 2026-11-22 | `24b58a54` | Gameplay triage batch (vehicle dynamics, part runtime, resources). | 17 |
| #308 | 2026-11-22 | `b50bda99` | Input and camera fixes batch. | 20 |
| #316 | 2026-11-22 | `4fef296b` | CarLot current-car bookkeeping. | 10 |
| #319 | 2026-11-22 | `c2daf2e2` | Interior view drawing an empty world — a render bug. | 5 |
| #321 | 2026-11-22 | `694242c2` | Hide the driver head in the interior view. | 6 |
| #331 | 2026-11-22 | `6b2f5ed7` | ROC garage tent sunlight (map lighting). | 8 |
| #349 | 2026-11-23 | `614eef1c` | GPS frame drawn above the map. | 4 |

### Not integrated — solved differently here (4)

| PR | Date | Commit | What | Files |
|---|---|---|---|---|
| #152 | 2026-11-17 | `23199240` | Video frame shutdown safety; our video_fmv path has its own containment. **(checked)** | 10 |
| #178 | 2026-11-17 | `e04e0820` | Removal of dead render fallbacks; we did an equivalent cleanup. **(checked)** | 15 |
| #198 | 2026-11-18 | `a53078c0` | Cleanup of inactive render passes; ours is a different set. **(checked)** | 19 |
| #187 | 2026-11-19 | `1fda7b3b` | Outline fonts; this tree has its own font atlas and text path. **(checked)** | 15 |

### Not integrated — deferred with a plan (8)

| PR | Date | Commit | What | Files |
|---|---|---|---|---|
| #146 | 2026-11-16 | `45949c82` | RPK descriptor/property cache — must be designed against B4.1.2, phase 3.1. | 9 |
| #176 | 2026-11-17 | `b85dc449` | Batched OSD text — a perf block of its own. | 20 |
| #58 | 2026-11-18 | `3f1110ef` | Parallel load of maps/packs/textures (job system, 1210 lines). | 15 |
| #200 | 2026-11-18 | `edd66bfc` | GC finalizer/heap lookup (172 lines), phase 3.2. | 11 |
| #48 | 2026-11-19 | `d1c719d1` | Vehicle dynamics as one body (5 commits, two merges) — a subsystem port. | 42 |
| #197 | 2026-11-19 | `d60b9071` | Shared vehicle and AI-to-prop collisions (#205) — phase 2. | 28 |
| #48 | 2026-11-20 | `ff91437c` | Vehicle dynamics as one body (5 commits, two merges) — a subsystem port. | 19 |
| #215 | 2026-11-23 | `d661d250` | Physics tick continuation (#203) — 39 files, 4637 insertions. | 39 |

### Not integrated — feature, the driver decides (10)

| PR | Date | Commit | What | Files |
|---|---|---|---|---|
| #227 | 2026-11-21 | `52592fbc` | SDL3 peripheral input replaces direct XInput polling — platform swap, decide. | 13 |
| #304 | 2026-11-21 | `786e5b4d` | Nitrous drive and stock exhaust flames. | 22 |
| #312 | 2026-11-22 | `68abdd97` | Stock swaybars — 70 files, a vehicle-dynamics feature. | 70 |
| #314 | 2026-11-22 | `84517db9` | Headlights as a feature (17h). | 3 |
| #317 | 2026-11-22 | `abe3aaa5` | Cockpit gauge needles moving. | 10 |
| #322 | 2026-11-22 | `65265ae0` | Driver hand/wheel contact animation. | 4 |
| #323 | 2026-11-22 | `407fe66a` | Gear-shift gesture animation. | 8 |
| #328 | 2026-11-22 | `f46e1121` | Flap springs opening on their own. | 5 |
| #324 | 2026-11-22 | `bd684108` | Engine sounds for traffic cars (two commits). | 3 |
| #324 | 2026-11-22 | `978ec4c2` | Engine sounds for traffic cars (two commits). | 8 |

### Not integrated — rejected by policy (6)

| PR | Date | Commit | What | Files |
|---|---|---|---|---|
| #191 | 2026-11-19 | `edd6e822` | Save files as JSON with an SDAT fallback (#213) — the save format is a closed contract here. **(checked)** | 8 |
| #64 | 2026-11-20 | `f30b3895` | Garage painting / Painter replacement (#206) — the painter contract is ours. **(checked)** | 50 |
| #216 | 2026-11-20 | `f6512b63` | Cinepak playback (#219). **(checked)** | 8 |
| #192 | 2026-11-20 | `494aedac` | FMOD audio (#229). **(checked)** | 13 |
| #202 | 2026-11-21 | `395dd106` | Rendering foundation (#235/#237/#279): point/spot lights, shadow cascades, AO, their reflection work. Out of scope by instruction — this tree keeps its own renderer. **(checked)** | 112 |
| #292 | 2026-11-21 | `640bf78c` | Readable JSON saves, second half. **(checked)** | 14 |

### Does not apply (7)

| PR | Date | Commit | What | Files |
|---|---|---|---|---|
| #157 | 2026-11-17 | `c506bfe8` | Native coverage descriptor report — tooling. | 6 |
| #104 | 2026-11-17 | `a70aaa0a` | Documentation contradictions. | 4 |
| #207 | 2026-11-19 | `928463ea` | Branch/PR rules in their docs. | 1 |
| #239 | 2026-11-20 | `ffc3d60a` | Documentation refresh. | 3 |
| #241 | 2026-11-20 | `58afd5a5` | Branch build/run helper script. | 2 |
| #290 | 2026-11-21 | `7a430457` | Copies fmod.dll beside tests — no FMOD here. | 1 |
| #326 | 2026-11-22 | `a7fac1e5` | Review notes for #308. | 1 |

## 5. What this audit recommends, in order

1. **Finish #180 before starting anything else from this list.** U2-C (signal
   consumer), U2-D (race brain), U2-E (ROC), U2-F (native vehicle) are already
   scoped, and they are the only items where we have a half-built subsystem whose
   missing half is upstream's. Nothing else below depends on them, but leaving a
   traffic system that cannot obey a traffic light is the largest functional hole
   this audit found.
2. **One pass over the "needs a look" list** (~40 merges). They are all
   single-purpose fixes in files we own; most are small. The honest way is to read
   each diff against our file and keep the intent, as U1 did for eight of them.
   That pass is where the remaining crashes and UI defects live.
3. **Perf block, decided as one:** #199 (no per-frame CPU copy of mesh data),
   #58 (parallel loading), #176 (batched OSD text), #305 (large collections),
   #211 (build speed). Each is measurable; none of them touches the reflection
   design.
4. **Decisions needed from the driver, not from the code:** SDL3 input (#227),
   swaybars (#312), headlights (#314), cockpit needles (#317), nitrous flames
   (#304), driver hand/gear gestures (#322/#323), traffic engine sounds (#324),
   flap springs (#328). They are features, and several would change how the car
   feels rather than how it renders.
5. **Not recommended at all:** #202 (their rendering foundation — it would replace
   this tree's reflection work, which is ahead of theirs), #213/#292 (JSON saves,
   closed contract), #219 (Cinepak), #229 (FMOD), #64/#206 (Painter), #48/#197/#215
   (vehicle dynamics and physics tick) — the last three are worth revisiting only
   if the single-body model becomes the blocker, and then as a planned port, not a
   merge.

## 6. One thing worth saying plainly

Upstream is not ahead of us in the renderer: their reflection cube has no mip
chain and their material samples mip 0, so they have no roughness at all
(`UPSTREAM_DIFF.md` §1). Their #202 does not change that. The areas where they are
ahead are gameplay breadth (physics, careers, animation, audio, input), testing
(~60 test files) and loading performance. An audit that treats "upstream has it"
as "we need it" would take us backwards on the one subsystem this tree has pushed
furthest.

## Appendix A. Every first-parent merge since the base

| # | Date | Commit | Files | Subject |
|---|---|---|---|---|
| 117 | 2026-11-16 | `71aec8b3` | 9 | PR #117 codex/fix-save-file-safety |
| 118 | 2026-11-16 | `1de56f08` | 9 | PR #118 codex/fix-save-persistence |
| 119 | 2026-11-16 | `c26ef62d` | 8 | PR #119 codex/fix-mesh-skin-textures |
| 120 | 2026-11-16 | `784a7609` | 6 | PR #120 codex/fix-physics-position |
| 123 | 2026-11-16 | `933cf0a4` | 7 | PR #123 codex/fix-garage-scene-lifetime |
| 124 | 2026-11-16 | `c5798346` | 7 | Validate SCX face indices and animation key bytes before allocation (#124) |
| 135 | 2026-11-16 | `c7903575` | 8 | PR #135 codex/fix-standalone-map-props |
| 136 | 2026-11-16 | `f2e766ed` | 7 | PR #136 codex/fix-paused-character-animation |
| 137 | 2026-11-16 | `896e9bd5` | 2 | PR #137 codex/fix-initial-wheel-steering |
| 138 | 2026-11-16 | `c15a839b` | 6 | PR #138 codex/fix-race-time-milliseconds |
| 139 | 2026-11-16 | `a877a637` | 14 | PR #139 codex/fix-night-map-objects |
| 140 | 2026-11-16 | `60d9d85c` | 6 | PR #140 codex/fix-tree-foliage |
| 142 | 2026-11-16 | `a30c354a` | 11 | PR #142 codex/restore-clouds-daylight |
| 143 | 2026-11-16 | `8ac81f03` | 8 | PR #143 codex/fix-vehicle-polygons |
| 144 | 2026-11-16 | `c19cfa0c` | 6 | PR #144 codex/fix-tree-integer-money |
| 145 | 2026-11-16 | `163d7898` | 15 | PR #145 codex/fix-osd-resource-lifetime |
| 146 | 2026-11-16 | `45949c82` | 9 | PR #146 codex/fix-resource-cache-lifetime |
| 147 | 2026-11-16 | `9b4de293` | 10 | PR #147 codex/fix-tufa-validation |
| 148 | 2026-11-16 | `e5244f9b` | 9 | PR #148 codex/fix-rpk-map-validation |
| 149 | 2026-11-16 | `7737dbf4` | 8 | PR #149 codex/fix-audio-capacity-playlists |
| 150 | 2026-11-16 | `240af238` | 8 | PR #150 codex/fix-vm-lookup-cost |
| 151 | 2026-11-17 | `6fffbe6c` | 11 | PR #151 codex/fix-script-argument-array-safety |
| 152 | 2026-11-17 | `23199240` | 10 | PR #152 codex/fix-video-frame-shutdown |
| 153 | 2026-11-17 | `a15ab7b0` | 8 | PR #153 codex/fix-options-save-retries |
| 154 | 2026-11-17 | `bb45d125` | 7 | PR #154 codex/fix-scene-object-lookups |
| 155 | 2026-11-17 | `411dda47` | 17 | PR #155 codex/fix-driver-visibility-traversal |
| 157 | 2026-11-17 | `c506bfe8` | 6 | PR #157 codex/fix-native-coverage-descriptors |
| 158 | 2026-11-17 | `dede4b57` | 7 | PR #158 codex/fix-gamepad-axis-fallback |
| 160 | 2026-11-17 | `4de3d3ca` | 10 | PR #160 codex/fix-class-lifetime-constructors |
| 161 | 2026-11-17 | `9c1ab80b` | 7 | PR #161 codex/fix-career-day-static-increment |
| 162 | 2026-11-17 | `67cd5814` | 6 | PR #162 codex/fix-inventory-save-transition |
| 163 | 2026-11-17 | `9df8920d` | 9 | PR #163 codex/fix-fullscreen-map |
| 164 | 2026-11-17 | `ae2d236b` | 5 | PR #164 codex/fix-version-build-order |
| 165 | 2026-11-17 | `f3638a14` | 11 | PR #165 codex/fix-map-alpha |
| 166 | 2026-11-17 | `27bce5c2` | 9 | PR #166 codex/fix-physics-recovery |
| 167 | 2026-11-17 | `db8f0690` | 3 | PR #167 codex/fix-worldpos-varying |
| 168 | 2026-11-17 | `a5184d23` | 7 | PR #168 codex/fix-findfile-paths |
| 169 | 2026-11-17 | `57a56fc2` | 6 | PR #169 codex/fix-splash-adoption |
| 170 | 2026-11-17 | `c6e563fe` | 4 | PR #170 codex/fix-track-presentation |
| 171 | 2026-11-17 | `d9883593` | 7 | PR #171 codex/fix-menu-input-debounce |
| 172 | 2026-11-17 | `a2095e8c` | 6 | PR #172 codex/fix-tooltip-double-render |
| 174 | 2026-11-17 | `6339249f` | 10 | PR #174 codex/fix-car-select-presentation |
| 175 | 2026-11-17 | `4e4b7828` | 11 | PR #175 codex/fix-trigger-markers |
| 121 | 2026-11-17 | `c63a8cd6` | 6 | PR #121 codex/fix-gameref-alias-events |
| 176 | 2026-11-17 | `b85dc449` | 20 | PR #176 codex/batch-osd-text |
| 104 | 2026-11-17 | `a70aaa0a` | 4 | Fix documentation contradictions reported in #104 (#173) |
| 177 | 2026-11-17 | `6fcc278e` | 7 | PR #177 codex/fix-cmake-dependency-paths |
| 178 | 2026-11-17 | `e04e0820` | 15 | PR #178 codex/remove-dead-render-fallbacks |
| 58 | 2026-11-18 | `3f1110ef` | 15 | Load maps, packs and textures on every CPU core (#58) |
| 180 | 2026-11-18 | `0a44cf44` | 42 | PR #180: shared roads, traffic and race driving |
| 198 | 2026-11-18 | `a53078c0` | 19 | PR #198 codex/cleanup-inactive-render-passes |
| 199 | 2026-11-18 | `1a71e9e9` | 9 | PR #199 codex/reduce-gpu-mesh-cpu-copies |
| 200 | 2026-11-18 | `edd66bfc` | 11 | PR #200 codex/reduce-gc-finalizer-overhead |
| 48 | 2026-11-19 | `d1c719d1` | 42 | Integrate current vehicle dynamics work from PR #48 |
| 207 | 2026-11-19 | `928463ea` | 1 | docs: document branch and PR rules for issue #207 (#208) |
| 210 | 2026-11-19 | `0935ac66` | 4 | Restore compiler source omitted from the build target (#210) |
| 197 | 2026-11-19 | `d60b9071` | 28 | #197 Shared vehicle and AI-to-prop collisions (#205) |
| 187 | 2026-11-19 | `1fda7b3b` | 15 | Add outline fonts and shared text effects for #187 (#214) |
| 191 | 2026-11-19 | `edd6e822` | 8 | #191 Store saves as JSON with SDAT read fallback (#213) |
| 211 | 2026-11-19 | `eb029118` | 5 | #211 Faster compile: parallel cl.exe and shared shaderc (#212) |
| 64 | 2026-11-20 | `f30b3895` | 50 | #64 Restore garage painting and stop paint-can previews bouncing (#206) |
| 48 | 2026-11-20 | `ff91437c` | 19 | Make the car move as one vehicle: script-driven tyres and suspension, bones, physical flaps, wheel loss (#48) |
| 216 | 2026-11-20 | `f6512b63` | 8 | #216 Play the original Cinepak movies without VLC (#219) |
| 222 | 2026-11-20 | `ed0a0b51` | 2 | #222 Borderless window when the mode equals the monitor size (#224) |
| 218 | 2026-11-20 | `7f629d5b` | 2 | #218 Optimize hot paths and preserve dealer wheels (#220) |
| 192 | 2026-11-20 | `494aedac` | 13 | #192 Integrate FMOD for audio playback and mixing (#229) |
| 239 | 2026-11-20 | `ffc3d60a` | 3 | #239 Update documentation from current code (#240) |
| 241 | 2026-11-20 | `58afd5a5` | 2 | #241 Add branch build and run helper (#242) |
| 245 | 2026-11-21 | `21ca10bb` | 10 | PR #245 bug/225-hud-fender-indicator |
| 202 | 2026-11-21 | `395dd106` | 112 | PR #202 codex/rendering-foundation-201 |
| 228 | 2026-11-21 | `5b002e42` | 3 | PR #228 bug/226/world-career-validation-4495f2547302 |
| 292 | 2026-11-21 | `640bf78c` | 14 | PR #292 feature/291-readable-json-saves |
| 290 | 2026-11-21 | `7a430457` | 1 | #290 Copy fmod.dll beside test executables from the configured SDK (#293) |
| 272 | 2026-11-21 | `09f9f924` | 3 | #272 Let pedestrians pass narrow corridors and gates (#294) |
| 227 | 2026-11-21 | `52592fbc` | 13 | Replace direct XInput polling with SDL3 peripheral input (#227) |
| 305 | 2026-11-21 | `3730a5c6` | 6 | Scale loading for large car and mod collections (#305) |
| 304 | 2026-11-21 | `786e5b4d` | 22 | #304 Connect nitrous drive and stock exhaust flames (#309) |
| 226 | 2026-11-22 | `ab3d284c` | 8 | #226 Career race tests: AI routes, City tick and racer replies (#310) |
| 307 | 2026-11-22 | `24b58a54` | 17 | PR #307 bug/300/gameplay-triage-f4bc2dbea25c |
| 312 | 2026-11-22 | `68abdd97` | 70 | PR #312 feature/311-stock-swaybars |
| 308 | 2026-11-22 | `b50bda99` | 20 | PR #308 bug/295/input-camera-fixes-2b3d351a0980 |
| 316 | 2026-11-22 | `4fef296b` | 10 | PR #316 bug/315-carlot-current-car |
| 314 | 2026-11-22 | `84517db9` | 3 | PR #314 feature/313-headlights-from-17h |
| 319 | 2026-11-22 | `c2daf2e2` | 5 | PR #319 bug/318-interior-view-empty-world |
| 317 | 2026-11-22 | `abe3aaa5` | 10 | PR #317 feature/301-cockpit-needles |
| 321 | 2026-11-22 | `694242c2` | 6 | PR #321 bug/320-hide-driver-head |
| 322 | 2026-11-22 | `65265ae0` | 4 | PR #322 feature/302-driver-wheel-contact |
| 323 | 2026-11-22 | `407fe66a` | 8 | PR #323 feature/302-gear-shift-gesture |
| 326 | 2026-11-22 | `a7fac1e5` | 1 | PR #326 feature/325-pr308-review-notes |
| 328 | 2026-11-22 | `f46e1121` | 5 | PR #328 feature/327-flap-spring-open |
| 331 | 2026-11-22 | `6b2f5ed7` | 8 | PR #331 bug/330-roc-garage-tent-sunlight |
| 324 | 2026-11-22 | `bd684108` | 3 | #324 Add engine sounds for traffic cars |
| 324 | 2026-11-22 | `978ec4c2` | 8 | #324 Pan traffic engines, vary them per car and play native sfx_loop radios |
| 349 | 2026-11-23 | `614eef1c` | 4 | PR #349 bug/348-gps-frame-above-map |
| 215 | 2026-11-23 | `d661d250` | 39 | PR #215 spike/203/continue-physics-tick-8e669f8bf2dc |

## 9. Since the audit: `d661d250` -> `d13e7102` (2026-11-24)

Seventeen first-parent merges, 462 files, +499 695 / -6 550 lines -- of which
nearly all of the additions are `assets/validation/` captures (webp frames of
shadows and perf runs), not code. Read against the same question: does any of
it change a defect this tree has open.

| PR | commit | files | change | verdict |
|---|---|---|---|---|
| [#387](https://github.com/EliasFD3S/openslrr/pull/388) | `d13e7102` | 3 | patch deployment before launch | **rejected** -- `scripts/` and the `patch/` tree, which this port does not use |
| [#385](https://github.com/EliasFD3S/openslrr/pull/386) | `1cb657bd` | 16 | FMOD -> SDL3_mixer | **rejected** -- this host never linked FMOD; audio is XAudio2 (`host_sfx`) |
| [#383](https://github.com/EliasFD3S/openslrr/pull/384) | `3c5eced6` | 21 | one combined-slip tyre, keyboard steering window, STEERING SPEED SENS. | **rejected as a port, kept as a reference.** Their tyre is a Magic Formula with a 1.15x peak on script friction, i.e. a *new* law; ours is the recovered original. Their steering window (speed-dependent turn-in, follows the slide for countersteer) is the only upstream answer to DRV-1, but it is written against their tyre and input path |
| [#380](https://github.com/EliasFD3S/openslrr/pull/382) | `73edc68e` | 29 | physics at 1000 Hz, `docs/vehicles/physics-tick.md` | **deferred.** A tick architecture of theirs (it continues #215). Our 0-100 4.15 s / 228 km/h / slide-residual measurements are tied to the recovered tick; adopting this invalidates them, and no defect asks for it |
| [#377](https://github.com/EliasFD3S/openslrr/pull/379) | `2414751c` | 146 | native options dialog, `src/ui`, `patch/system` | **deferred.** Needs the `patch/` assembly and their UI registry; our options live in `host_game_options` and the stock scripts |
| [#376](https://github.com/EliasFD3S/openslrr/pull/376) | `5b0a0a83` | 4 | doc drift | **none** -- docs |
| [#374](https://github.com/EliasFD3S/openslrr/pull/374), [#372](https://github.com/EliasFD3S/openslrr/pull/372), [#370](https://github.com/EliasFD3S/openslrr/pull/370), [#369](https://github.com/EliasFD3S/openslrr/pull/369), [#368](https://github.com/EliasFD3S/openslrr/pull/368), [#367](https://github.com/EliasFD3S/openslrr/pull/367), [#366](https://github.com/EliasFD3S/openslrr/pull/366), [#365](https://github.com/EliasFD3S/openslrr/pull/365) | `62846edf`...`5ef33268` | 15-54 each | sun cascades on workers, collect cache, cascade alternation, occlusion spike, GPU pass timing, shadow flicker | **rejected.** Their shadow/cascade tree does not exist here; the lines are validation captures, the code is `src/render/bgfx_*` of a renderer we replaced |
| [#373](https://github.com/EliasFD3S/openslrr/pull/373) | `597f0287` | 1 | AO pass cost note | **none** -- doc |
| [#364](https://github.com/EliasFD3S/openslrr/pull/364) | `3512fc48` | 31 | perf baseline harness (`scripts/perf_baseline.ps1`) | **needs a look** -- a measurement harness, not engine code; useful if PERF-2 is ever attacked |
| [#357](https://github.com/EliasFD3S/openslrr/pull/357) | `25d8fd72` | 45 | docs regroup (`docs/core`, `docs/rendering`, ...) | **none** -- docs, but the paths moved: references to upstream docs from here must use the new layout |

### What it does *not* contain

- **DRV-6 (camera does not return):** upstream has no camera code. `git grep`
  over `src/` for a chase camera finds nothing on either side, so the auto-return
  is stock Java in `system/Scripts` and would have to be restored there, not
  ported from upstream.
- **UI-3 (part condition icons):** no upstream commit touches part icons; the
  icons in this build are the stock ones drawn by our OSD path.
- **DRV-4 / DRV-5 (wheel rotation on part attach / clutch toggle):** nothing
  upstream; both are still our own defects.
- **Traffic light glow:** untouched upstream (its traffic work is #305-style
  scaling, not signal heads), so the open item in `BACKLOG.md` is ours to
  finish.

So the answer to "has the author fixed these already" is no for every one of
them, and the only overlap is DRV-1 through #383, on physics that is not ours.

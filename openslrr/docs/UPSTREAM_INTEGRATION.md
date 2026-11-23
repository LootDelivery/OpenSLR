# UPSTREAM INTEGRATION — EliasFD3S/openslrr → OpenSLR

Registry of upstream changes that were ported into this tree, the ones that were
deliberately deferred, and the ones that were rejected with a reason.

- Base commit: `947d11bf843ea4dad5e7267ff3f8dda4babc1e5a` (L2).
- Upstream source: `https://github.com/EliasFD3S/openslrr`, `main` at
  `5b002e422259e9e7a9f7b55d46bb52c51d8f2b5b`.
- Branch: `upstream-gameplay-sync`.

The two repositories have **no merge base** (`git merge-base` finds nothing),
so nothing here is a cherry-pick: every entry was re-implemented against our
own code, or rejected because our implementation solves the problem
differently. Upstream graphics (`LightSource`/`PointLight`/`SpotLight`, bgfx
passes, shadows, AO, HDR, the `patch/` tree) is out of scope and never ported.

Columns follow the integration protocol: upstream PR, upstream files, our
files, problem, adaptation, tests, result, status.

This file is one direction only (upstream -> here), and only for changes that
were considered: nine blocks ported, seven deferred or rejected, written against
upstream `5b002e42`. It does not enumerate what upstream has done since, nor what
was never examined.

- `UPSTREAM_DIFF.md` -- what this tree changed relative to upstream's `d1c719d`,
  file by file, with the fifty-three files both sides touched since.
- `UPSTREAM_MAIN_AUDIT.md` -- every first-parent merge upstream has made since the
  revision this tree was taken from (`17115433`, 2026-11-14) up to `d661d250`,
  with a verdict for each: integrated, needs a look, superseded, deferred, a taste
  call, or rejected.

## U1-A. Player constructor (#228)

| Field | Content |
|---|---|
| Upstream PR | [#228](https://github.com/EliasFD3S/openslrr/pull/228), commit `48b19c63` "#226 Run Player's constructor so the boot player has a car lot" |
| Upstream files | `src/natives/natives_io.cpp` (3-line removal), `docs/KNOWLEDGE_BASE.md` |
| Our files | `src/natives/natives_io.cpp`, `docs/KNOWLEDGE_BASE.md` |
| Problem | `java.game.Player.<init>()V` was registered as `native_noop`. Our VM resolves a host binding **before** the TREE (`Jvm::invoke_by_name`, `native_has_alias` check), so every `new Player()` in stock Java allocated the object and skipped the constructor body: it had no `parts`, `carlot` or `osd`. `java.game.GameLogic.<init>` performs that `new Player()` twice during `Init`, and `Garage.enter` reuses the resulting `GameLogic.player`. |
| Adaptation | Same single change: the registration is gone and the stock TREE runs. No host-side substitute was added, because the Java path already constructs the object. The comment on the removed block now records **why** the binding must not come back. |
| Tests | See the matrix below. |
| Result | `--spawn-car`: crash 5/5 before, 5/5 clean after; the car spawns with 88 parts, the garage renders and the run reaches the map. `--auto-new` and the plain boot are unchanged. The crash itself was the empty-layer dereference described in U1.1, not the missing constructor; see "Actual lifecycle" below. |
| Status | **Ported** |

### Actual lifecycle (reconciled in U1.2)

The first version of this entry said the constructor is "reached only from
`Garage.enter`" and that "no boot path invokes it during `start_init`". That was
wrong, and U1.2 corrected it with instrumentation that prints the allocating
site, the creating method, the object address and the resolution result:

| Event | Phase | Caller | Stub bound | Stub removed |
|---|---|---|---|---|
| allocate `Player` #1 | `Init` | `java.game.GameLogic.<init>` | yes | yes |
| `Player.<init>()V` #1 | `Init` | `tree_eval` `new_init` → `tree_call_by_name` → `invoke_by_name` | resolves to the host no-op, **body skipped** | TREE body runs |
| allocate `Player` #2 | `Init` | `java.game.GameLogic.<init>` (same object, two `new Player()` sites) | yes | yes |
| `Player.<init>()V` #2 | `Init` | same path | host no-op, **body skipped** | TREE body runs |
| `GameLogic.player` | after `Init` | static field | first of the two objects | first of the two objects |
| garage entry | `--spawn-car` / `Garage.enter` | reuses `GameLogic.player`; **no** `new Player()` | — | — |

Measured with `[u12]` probes (temporary, not shipped) on `--game --window`:
2 allocations and 2 constructor invocations before `[boot] after Init`, all from
`java.game.GameLogic.<init>` with the same receiver; with the binding restored
both invocations report `host=BINDING` and the player comes out with
`osd == null` and no `parts` field, without it both report `none->TREE` and the
constructor creates the `Osd` (two `GPS_Frame.ptx` plates exist afterwards).

Why the original trace missed it: `[u1-call]` hooked `Jvm::invoke`, but
`new Player()` never enters that function — the `new_init` opcode calls
`TreeHost::tree_call_by_name`, which is `Jvm::invoke_by_name`. Across a full
cold boot the `Jvm::invoke` probe records **0** `Player.<init>` calls while the
`invoke_by_name` probe records **2** — the "zero calls before the garage"
reading in this entry came from the hook that cannot see them, not from a
property of the game.

So the stub did not guard a "miss during `start_init`" (the constructor tree
runs to completion in both cases — with the binding it simply never starts).
It silently disabled the stock constructor at **both** sites, which is what
left `GameLogic.player` without `parts`/`carlot`/`osd`.

Consequence for the garage crash — measured, and not what this entry first
claimed:

| Tree | Binding | Result on `--spawn-car` |
|---|---|---|
| base `947d11bf` | stub | `0xC0000005` in `LibBgfxBackend::draw_osd` (symbolised from the linker map of that build: `draw_osd+0x48b`), right after the section-change `[bgfx] reset` |
| `a7f0f39e` | stub re-bound (`SLRR_U12_NOOP`, temporary) | no crash, car spawns, but `GameLogic.player.osd == null` and no `parts` field |
| `a7f0f39e` with U1.1 fix 2 disabled | stub re-bound | `0xC0000005` in `draw_osd` (`+0x54f`) at the same boot point |

So the stub did not crash the garage by leaving the player incomplete: with the
stub bound, the first frame after the section change had nothing to draw and
`draw_osd` called `layers.front()` on an empty vector. Removing the binding
made the untouched HUD widgets visible by the old default, which kept the layer
list non-empty — the crash disappeared without the defect being fixed, and the
same visible-by-default widgets are what U1.1 then had to hide. Fix 2 of U1.1
is the actual repair; U1.1 fix 1 is why the HUD plates no longer reach the
menu.

### Verification

Measured with a player-field probe, `--game --window` (the invoke trace used
at the time hooked `Jvm::invoke` and never saw this call — see "Actual
lifecycle"):

| Scenario | Before (stub bound) | After (TREE runs) |
|---|---|---|
| boot → `MainMenu` | `after Init section=java.game.MainMenu`, exit 10 on `--auto-new` | identical |
| `--auto-new` | builds the car, then `StringRequesterDialog no pump` → `CMD_NEW→Garage … ok=0` | identical (host dialog pump gap, not this fix) |
| `--spawn-car Maz3_2005.rpk:0x6` | `0xC0000005` while entering the garage, 5/5 runs | garage entered, `spawned … parts=88`, 5/5 runs |
| `--spawn-car … --bench-map ValoCity --bench-drive 3` | crash before the map | `[bench] start map java.game.ValoCity_Track`, `drive done`, exit 0 |
| `player.parts/carlot/osd` | 0/0/0 after `Garage.enter` | 1/1/1 (constructor creates `Inventory`, `CarLot`, `Osd`, `GroundRef carlot`) |
| `GameLogic.changeActiveSection` | crash inside the section transition | completes; `[section] actual=java.game.Garage` |

No new `[jvm] invoke miss` lines appear: the constructor tree of the stock
`Player.class` (Build 940, `TUFA` blob) calls only natives our host already
binds (`Player/Inventory/CarLot/GroundRef/Osd/RenderRef/ResourceRef` →
`newNative`/`createNativeInstance`, `ResourceRef.set`, `Osd.setFlags`,
`Viewport.get*`, `Group.*`, `Vector.*`).

Also verified: `Player.<init>` is registered only in `natives_io.cpp`; no other
native group re-registers it.

## U1-B1. File safety (#117)

| Field | Content |
|---|---|
| Upstream PR | [#117](https://github.com/EliasFD3S/openslrr/pull/117), commit `5a51a4d7` "Protect file deletion and keep SDAT save fields aligned" |
| Upstream files | `src/natives/natives_boot.cpp` (`under_game`), `src/natives/natives_io.cpp` (`File.delete`, `File.write(ResourceRef)`), `src/natives/natives_internal.hpp`, `tests/test_save_io.cpp`, `scripts/run_save_io_tests.ps1` |
| Our files | same three sources, `tests/test_save_io.cpp`, `scripts/run_native_io_tests.ps1` |
| Problem | `under_game()` rejected any path containing the two characters `..` and then compared a **string prefix**, so the game root itself passed as a descendant, legal names like `J..R-1` were refused, and `FindFile` did not canonicalize at all (a `..` pattern enumerated outside the root). `File.delete` used `fs::remove_all`, so `delete(".")` erased the install. `File.write(ResourceRef)` returned before writing when a save resource had no pack, which shifted every following field in the stream. |
| Adaptation | `resolve_under_game(root, rel, allow_root)`: rejects NUL, `:`, and `..` components; resolves junctions with `weakly_canonical`; compares path **components** case-insensitively (`_wcsicmp`). `under_game()` is the `allow_root=false` wrapper and stays the only exported entry. `FindFile` uses `allow_root=true` (a listing may address the root) and skips children that resolve outside. `File.delete` is `fs::remove`. A packless SDAT resource writes a null id, keeps the stream aligned, still returns 0, and counts in `degraded_resources` (logged by `File.close`). |
| Tests | `tests/test_save_io.cpp` — 11 cases: root deletion/opening, non-recursive directory delete, stock delete-then-directory, dotted career names (`J..R-1`, `Mr...-1`), traversal rejection, SDAT alignment for unknown/packless/blob resources, valid + null resource roundtrip, raw-file id preservation, and `FindFile` root escape. Run with `powershell -File scripts/run_native_io_tests.ps1 [-BaselineRef 947d11bf]`. |
| Result | `-BaselineRef 947d11bf`: **3 passed, 8 failed** (root deleted, nonempty dirs erased, dotted names refused, three streams misaligned, `FindFile.first("..\*")` returned 4 entries from outside the root). After: **11 passed, 0 failed**. |
| Status | **Ported** |

`under_game` is called from `natives_io.cpp` (delete/exists/copy/move/open),
`natives_resref.cpp` and `natives_script.cpp`; all of them now reject the root
itself and accept dotted file names. `natives_resref.cpp` keeps its own
"open a save path" call site unchanged — the containment rule is shared, the
callers are not.

## U1-B2. Save persistence (#118)

| Field | Content |
|---|---|
| Upstream PR | [#118](https://github.com/EliasFD3S/openslrr/pull/118), commit `7c9392a7` "Publish saves safely and preserve string and slot damage sentinels" |
| Upstream files | `src/natives/natives_io.cpp` (`publish_save`, `transfer_pending_save`, `File.close/exists/copy/move/write(String)`), `src/resources/part_runtime.cpp` (`getSlotDamage`), `tests/save_io_fixture.hpp`, `tests/test_save_persistence.cpp` |
| Our files | same two sources, `tests/save_io_fixture.hpp`, `tests/test_save_persistence.cpp` |
| Problem | `File.close` truncated the destination with `ofstream` and only then wrote, so a failed or partial write destroyed the previous save; nothing checked `flush`/`close`. The `g_unflushed_saves` buffer that keeps the newest save in memory was invisible to `File.exists` and bypassed by `copy`/`move`, so a caller saw a stale disk file or lost the newest bytes. `File.write(String)` mapped an empty string and `null` to the same length 0. `getSlotDamage` returned an empty string for an undamaged slot, which `Part.save` then wrote as if it were a damage record. |
| Adaptation | `publish_save()` writes a sibling `*.openslrr-save.<pid>.<n>.tmp` with `CreateFileW`, checks every `WriteFile`, `FlushFileBuffers`, `CloseHandle`, then publishes with `MoveFileExW(MOVEFILE_REPLACE_EXISTING | MOVEFILE_WRITE_THROUGH)` and deletes the staging file on any failure; the old destination survives until that rename. `File.close` uses it, keeps the buffer in `g_unflushed_saves` on failure and logs `error=<Win32>`. `transfer_pending_save()` makes `copy`/`move` publish the pending bytes; a successful disk copy/move drops an older pending destination. `File.exists` counts a pending buffer. `write(String)`: `null` is length 0, `""` is length 1 + NUL. `getSlotDamage` returns `null` for an undamaged or out-of-range slot and allocates nothing. Save format is untouched: same SDAT envelope, same fields. |
| Tests | `tests/test_save_persistence.cpp` — 10 cases: locked replacement keeps disk and the newest memory copy, pending visible without a disk file, copy/move use pending bytes, failed copy preserves both sides, successful copy/move discard an older pending destination, empty vs null string wire lengths, undamaged/invalid slots return null without allocating, damage roundtrip and clearing. |
| Result | `-BaselineRef 947d11bf`: **4 passed, 6 failed**. After: **10 passed, 0 failed**; `test_save_io` stays 11/0 and the garage boot is unchanged. |
| Status | **Ported** |

The staging file lives next to the destination because a save may sit on
another volume; `MoveFileExW` is then a same-directory rename. `File.close`
still reports `ok=0` to the caller exactly as before — the Java API is
unchanged, only the on-disk outcome is safer.

## U1-C1. TUFA validation (#147)

| Field | Content |
|---|---|
| Upstream PR | [#147](https://github.com/EliasFD3S/openslrr/pull/147), commit `f8434a07` "Validate TUFA counts, constants and local slots before execution" |
| Upstream files | `src/jvm/tufa.cpp`, `include/slrr/tufa.hpp`, `src/jvm/tree_eval.cpp`, `src/jvm/jvm.cpp`, `tests/test_tufa_validation.cpp` |
| Our files | same four sources, `tests/test_tufa_validation.cpp` (verbatim) |
| Problem | Every TUFA count was trusted: `reserve(count)` ran before the first truncation check, so a damaged `CONS`/`FILD`/`MTHD`/`TREE`/`CLSS` count attempted a multi-gigabyte allocation. Constant-bearing nodes were resolved lazily during execution, so a bad pool index only failed mid-run, and `tufa_parse` could publish a partial class list from a concatenated container whose later blob was damaged. Local slots (`op 1/2`) and `method.locals` were unbounded, which reached `locals.resize(imm + 1)`. |
| Adaptation | Same checks, in the same places. Additionally: `tufa_parse` clears the output on failure (all-or-nothing container), and `tufa_load_file` uses `ios::ate` so a failed `tellg` cannot produce a huge `size_t`, and is wrapped so an allocation failure is reported instead of escaping. `enrich_static_fields` catches a throwing static initializer, logs `[jvm] static initializer <owner>.<field>: <error>` and skips it: one bad `<clinit>` must not abort the process. Parser compatibility is unchanged — `reference()` still accepts bare `Utf8`/`NameAndType` carriers. |
| Tests | `tests/test_tufa_validation.cpp`: compiles `probe.Validation` with the in-repo compiler, then corrupts one count at a time (`CONS`, `FILD`, `MTHD`, `TREE`, interface count, node count, method locals, declare slot 65536/`0x7fffffff`/`0xffffffff`, static `String`/`RID` constant with `UINT32_MAX`), plus a trailing-byte container and a runtime `method.locals = UINT32_MAX`. |
| Result | `-BaselineRef 947d11bf -BaselineSources src/jvm/tufa,src/jvm/tree_eval,src/jvm/jvm`: **crash, exit `0xC0000409`, 0 assertions printed**. After: **exit 0, 14 `PASS` lines**. Boot, garage and the ValoCity drive produce byte-identical `[jvm]` diagnostics before and after, so no shipped class is rejected. |
| Status | **Ported** |

`kTufaMaxLocals = 65536` is the only new limit; the stock corpus never comes
close (`Player.<init>` uses a handful of slots, and the largest stock method is
far below it).

## U1-C2. Script argument and array safety (#151)

| Field | Content |
|---|---|
| Upstream PR | [#151](https://github.com/EliasFD3S/openslrr/pull/151), commit `4c1b8e6` "Check script arguments and bound array growth" (merge `6fffbe6c`) |
| Upstream files | `src/natives/natives_internal.hpp`, `src/jvm/tree_eval.cpp`, `src/natives/natives_{io,lang,osd,renderref,script}.cpp`, `tests/test_script_safety.cpp` |
| Our files | same six sources, `tests/test_script_safety.cpp` (verbatim) |
| Problem | Natives read `args[i].v.i` / `args[i].v.o` without checking size or tag, so a short or mistagged call read the union as an integer (an access violation for `v.o`, garbage for `v.i`). `tree_eval` grew script arrays to any index and clamped `new T[n]` at `1<<20` only for allocation, not for `a[i] = x`. |
| Adaptation | `native_arg_number/native_arg_integer/native_arg_object` in `natives_internal.hpp` with documented fallbacks (missing, boxed object, NaN → fallback; float → clamped to `INT32_MIN..MAX`). Applied at the six call sites upstream fixed: `System.exit`, `String.timeToString` ×2, `Camera.create/activate` priority, `Text.setPos`, `RenderRef.setMatrix` field reads (now accepts an `Int` field and rejects NaN), `OptionsDialog.get/setRendererOption`, `GfxEngine.displayModeName/changeVideoMode`. `store_array_slot()` in `tree_eval` bounds every array store to `[0, 1<<20)`; both the direct store and the `a[i] += x` write-back use it, and the `new T[n]` clamp shares the constant. Out-of-range stores are ignored and reported once, so a broken script cannot reallocate the heap array. |
| Tests | `tests/test_script_safety.cpp`: helper fallbacks, empty/short/boxed/NaN arguments through the six native families, array stores at `-1`, `1<<20`, `INT32_MAX` (ignored) and `(1<<20)-1` (grows to the limit), plus direct and compound store paths compiled by the in-repo compiler. |
| Result | `-BaselineRef 947d11bf` with the six sources reverted: **crash `0xC0000005`, no assertion reached**. After: **exit 0, PASS**. ValoCity drive unchanged (13 `[jvm]` diagnostics, `drive done`). |
| Status | **Ported** |

Deliberate deviation: upstream clamps `a[i] = x` by resizing to the index; we
keep that behaviour inside the window and drop anything outside it. Stock
scripts never address a slot above the window, so the observable behaviour is
unchanged.

## U1-D. Physics fault containment (#166)

| Field | Content |
|---|---|
| Upstream PR | [#166](https://github.com/EliasFD3S/openslrr/pull/166), commit `2b3d6659` "Contain rigid-body failures and reject non-finite physics inputs" |
| Upstream files | `src/resources/part_runtime.cpp`, `src/resources/gameref.cpp`, `tests/test_physics_recovery.cpp` |
| Our files | same two sources, `tests/test_physics_recovery.cpp` (verbatim) |
| Problem | One malformed body could take the process down or corrupt its neighbours: `RigidBody::configure` threw out of `dynamics_configure`/`part_add_dynamic_native`/`detach_panel`; a bad `attach` left the chassis body configured with the previous bounds and the `physics_ready` flag set; a non-finite frame from a loose body reached `local_matrix`; a throwing free-body step left the carrier body modified; `PhysicsRef.setMatrix` accepted NaN. |
| Adaptation | Same structure, applied to our code: `initialize_loose_body()` configures a **copy** and commits it only on success, so a rejected type is remembered in `invalid_dynamic_types` and never retried; `dynamics_configure` configures and frames a candidate, then swaps it in, marking the chassis suspended + fault-reported once on failure; `detach_panel` erases the panel when its body cannot be configured; `advance_loose_panels` throws inside a per-panel `try` that restores the carrier body, settles the node and destroys only that part; `advance_free_bodies` keeps the published pose, restores the carrier body and disables that PhysicsRef; non-finite frames are rejected at `gameref_set_matrix` and after every integration step. Strict validation is kept — nothing is skipped or clamped behind a "physics off" switch. |
| Tests | `tests/test_physics_recovery.cpp`: non-finite `GameRef` pose rejected atomically; a malformed free body disables itself while a healthy one keeps falling; a throwing native entry does not stop the frame or the next tick; `PhysicsRef` creation/`start`/`setRadius` recover after a rejected configure; overflow in integration is contained; a failed `part_add_dynamic_native` cleans its partial state and does not retry. |
| Result | `-BaselineRef 947d11bf -BaselineSources src/resources/part_runtime,src/resources/gameref`: **0 passed, 8 failed** (three of them as uncaught `invalid rigid frame` / `invalid rigid body mass/bounds`). After: **8 passed, 0 failed**. ValoCity drive: exit 0, `drive done`, the same 6 `[phys]` lines as before. |
| Status | **Ported** (one adaptation) |

Adaptation detail: upstream rolls back a list of `ContactVehicle` carriers
(`advance_free_bodies(vehicles, …)`, PR #205/#48). Our loop still takes the
single `controlled_vehicle`, so it backs up and restores that one body. The
multi-car rollback arrives with the vehicle-contact port (see the U1-E plan):
the `try`/restore shape is already the one that port needs.

## U1-F1. Garage steering pose (#137)

| Field | Content |
|---|---|
| Upstream PR | [#137](https://github.com/EliasFD3S/openslrr/pull/137), commit `996b3ab1` "Recompute parked wheel poses immediately on setsteer" |
| Upstream files | `src/resources/part_runtime.cpp` |
| Our files | same |
| Problem | `setsteer` (sent by `Garage.java` on entry, and on mechanic-mode changes) updated `steering_input`/`steering_request`, then placed the attached parts through the existing dynamic hub matrices. The steering wheel read the new scalar while the wheels kept the previous pose until some later operation recomputed the assembly. |
| Adaptation | Identical: when `rec->dynamic_frames` is already set, `setsteer` calls `pose_wheels()` and returns; before the frames exist the previous rest-placement path runs unchanged. `pose_wheels` rebuilds the steered/rolling frames from the current suspension state and places/relinks the assembly. It does not integrate physics, move the chassis, repark the car or reset wheel spin — this is pose correctness, not vehicle dynamics. A forward declaration was needed because `pose_wheels` is defined below `part_command`. |
| Tests | Upstream ships no automated assembled-vehicle test for this and neither do we: a fixture would need a full chassis + suspension + wheel tree. Runtime smoke: garage entry (`spawned … parts=88`) and the ValoCity drive (`drive done`, exit 0) are unchanged; 13 steering lines on the garage entry. Manual check remains: brake/steer visible when the garage opens, before touching any part, and normal driving steering afterwards. |
| Result | Compiles; garage and drive unchanged. Pose difference is not asserted automatically — recorded as an open manual check, not as verified. |
| Status | **Ported** (manual verification pending) |

## U1-F2. Live GameRef lookup index (#154)

| Field | Content |
|---|---|
| Upstream PR | [#154](https://github.com/EliasFD3S/openslrr/pull/154), commit `44c45505` "Index live GameRefs and reuse controlled vehicle lookup" |
| Upstream files | `src/resources/gameref.cpp`, `src/resources/part_runtime.cpp`, `tests/test_live_lookup.cpp` |
| Our files | same two sources, `tests/test_live_lookup.cpp` (verbatim) |
| Problem | `gameref_find_live()` scanned every `g_refs` entry, on every lookup, and `gameref_make_live()` called it in its ID-skip loop; `advance_free_bodies()` re-resolved the controlled vehicle for every simulated body. |
| Adaptation | `g_live_refs` (`id → GameRefState*`), maintained on make-live, canonical removal, alias removal and `gameref_clear()`. **Ownership rules are unchanged**: `g_next_live` is a `uint32_t` starting at `0x70000000`, strictly monotonic, IDs are never recycled after `gameref_remove`, and the allocator now refuses (returns 0) instead of overflowing past `INT32_MAX`. The old `while (gameref_find_live(...)) ++g_next_live` skip loop is gone because an ID is only handed out once, so there is nothing to skip. `chassis`/`vehicle` are resolved once per free-body pass. |
| Tests | `tests/test_live_lookup.cpp`: live ID assignment, rehash stability, alias detach/removal, canonical removal, key reuse and `gameref_clear()`, plus a 100 000-lookup timing line (informational). |
| Result | Correctness: **PASS before and after**. Lookup cost for 100 000 lookups over 8 000 nodes: **2 203 592 µs → 608 µs** (~3600×). ValoCity drive unchanged (exit 0, `drive done`, 9 `[phys]` lines). |
| Status | **Ported** |

The contract in `KNOWLEDGE_BASE.md`/L1.1 ("monotonic, no reuse of freed ids,
reset only on a full registry reset") is preserved and now also enforced
against `INT32_MAX` overflow.

## U1.1. Minimap frame lifecycle (follow-up, no upstream PR)

| Field | Content |
|---|---|
| Trigger | After U1-A a frame plate appeared in the left bottom corner of the main menu on a cold start, before `947d11bf` had none. |
| Our files | `src/ui/osd_registry.cpp`, `include/slrr/osd_registry.hpp`, `src/render/slrr_font.cpp`, `src/render/bgfx_osd.cpp` |
| Bisect | Base `947d11bf` … `038f65b3`, every revision rebuilt from source: 329 widgets and no `GPS_Frame.ptx` widget at the base, 331 widgets with two visible `GPS_Frame.ptx` plates from `8ba945e5` on. `038f65b3` is documentation only. |
| Cause | The stock `Player` constructor builds its HUD `Osd` (the `GPS_Frame.ptx` bezel plate) but never shows it. The registry treated "widget exists" as "widget visible", and `restore_other_osds()` additionally revived every OSD that had no `_host_hidden` flag — including owners that had never been shown. |
| Fix 1 (visibility) | `g_shown_owners` plus `osd_owner_shown()`: a widget is created hidden unless its owner was shown (`osd_set_owner_visible`, `osd_show_group`, `osd_show_only_group`); `restore_other_osds()` only restores owners that were shown once; `slrr_text_set_osd()` applies the same rule to OSD text; `osd_clear_widgets()` clears the set. |
| Fix 2 (empty layer list) | `draw_osd` collected the visible layers and then called `layers.front()`. A frame where a section has built its OSD but not shown it yet leaves the list empty: `0xC0000005` inside `draw_osd`. Reproduced with `--spawn-car` (symbolised from the linker map: `draw_osd+0x544`) and now returns early instead. |
| Fix 3 (HUD plate order) | The bezel plate went to `kPlateView` (69) like every other background plate, but the map is an active camera with a small viewport and is composited in `kPreviewPresent` (70), so the map covered its own frame. Plates owned by the vehicle HUD owner now go to `kOsdView` (71); all other plates keep the catalogue order. |
| Tests | Cold boot: no `GPS_Frame` widget visible in `MainMenu` (2 created, 0 visible). Garage: 0 visible. Street (`--bench-map ValoCity --bench-drive 3`): the active player's plate visible and drawn over the map. Native suites unchanged (11/0, 10/0, 14 PASS, PASS, 8/0, 2 PASS). |
| Status | **Ported** (`a7f0f39e`) |

### OSD ownership guarantees (U1.2 audit)

- Where the set is written: `osd_set_owner_visible(true)` (`Osd.show`,
  `Viewport.activate`), `osd_show_group`, `osd_show_only_group` (both iterate
  existing widgets) and `restore_other_osds` (re-inserts an owner it restored).
  It is erased by `osd_set_owner_visible(false)` — which is what
  `Viewport.destroy`, `Viewport.deactivate` and `Osd.hide` reach — and by
  `osd_clear_widgets()`.
- Widgets are never erased individually (retired ones are only marked `dead`),
  so an owner that owns a widget is kept alive by
  `osd_visit_gc_roots()` → `visit(w.owner)`. The only wholesale reset of the
  registry is `osd_clear_widgets()` in `boot_game` before the intros (it clears
  the set too). Section transitions deliberately do not clear it: the Java side
  hides the outgoing OSD (erase) and shows the next one on entry (insert).
- One case has no widget edge: `osd_set_owner_visible(true)` can run for an OSD
  before any widget or text is created on it. Measured on the shipped build: 2
  such activations on a cold boot and 3 across garage → street. Those owners
  were the only members of the set without a native edge, so
  `osd_visit_gc_roots()` now visits `g_shown_owners` directly. Without it the
  collector could free such an address while the set still claimed "shown", and
  a later allocation reusing that address would read back as already shown.
  With it, `g_shown_owners` only ever contains live script objects, which is
  also why address reuse cannot inherit the flag.
- `restore_other_osds()` cannot show a never-shown owner: its collection loop
  skips `!osd_owner_shown(w.owner)`. Before U1.1 it revived every OSD without a
  `_host_hidden` flag, which is how the two `Player` HUD plates reached the
  main menu.

Still **not verified** (recorded as unexecuted, not as working):

- Garage → MainMenu after entering the garage, Street → Garage, and
  Garage → MainMenu → Garage. The headless harness has no input pump for the
  menu commands: `--auto-cmd mainmenu|street` is dispatched before the garage
  finishes loading, and `--auto-new` stops at the `StringRequesterDialog` host
  gap. What is covered instead is one dump per section owned by the section
  machine (`MainMenu` 0/2 visible frame plates, `Garage` 0/2, `Street` 1/2).
- Career reload through the menu (same dialog gap).


## U2. Roads, traffic and race bots (#180)

Upstream PR #180 ("Restore shared roads, traffic and race bot driving"), merge
commit `0a44cf44`, parent base `3f1110ef`. Ported in six blocks on
`upstream-gameplay-sync` over `1539c625`. Graphics from the same PR (debug
overlay, frame profiler, route ribbons, signal lenses, bloom/glow, draw passes)
is **not** ported; `RenderRef.plotRoute` keeps its Java signature and its
geometry contract but draws nothing extra.

The semantic reference is the recovered engine in `slrr-reverse/`
(SLRR_rev v221, `engine/Runtime/Resources/GroundRef_route.cpp` and
`GroundRef_traffic.cpp`), not upstream's re-derivation: #180's own header says
its graph is a "sampled host model, not recovered original solver". Where the
two disagree the recovered native body wins, and the differences are listed
per block.

### U2-A. Road graph and route queries

| Field | Content |
|---|---|
| Upstream files | `include/slrr/road_routes.hpp` (77), `src/resources/road_routes.cpp` (613), `src/resources/race_runtime.cpp` (265), `tests/test_road_routes.cpp` (312), `tests/race_fixture.hpp` |
| Our files | `include/slrr/road_routes.hpp`, `src/resources/road_routes.cpp`, `include/slrr/race_runtime.hpp`, `src/resources/race_runtime.cpp`, `tests/test_road_routes.cpp`, `include/slrr/rpk_index.hpp` (+`generation()`), `include/slrr/gameref.hpp` (+`incarnation`), `src/resources/gameref.cpp`, `src/resources/rpk_index.cpp`, `src/natives/natives_boot.cpp`, `src/natives/natives_renderref.cpp`, `CMakeLists.txt` |
| Problem | The 12 `GroundRef` route/traffic natives were unbound. `getNearestCross` existed as a null-returning stub, the rest were missing, so `City`/`RaceSetup`/`Traffic` scripts had no road model at all. |
| Graph | `RoadRoutes` builds one directed edge per `RoadOther` lane record that is linked into both crossing nodes, plus a curve per grey crossing record; the lane-crossing links are what separates the two directions of a road. Sign heads come from ICFG `trlight` placements of the ground and its exterior maps, bound to the nearest crossing, and cycle per crossing branch. |
| Map lookup | The map TRC is named in the ground's own payload (`maps\city\meshes\city.scm,maps\city\ai\city.trc`), so the loader anchors on the ground, not on a map name: no Valo City fallback, no hardcoded path. Two different `.trc` paths in one payload are ambiguous and leave the ground without a graph. |
| Cache | Keyed by `GameRefState::incarnation` + `RpkIndex::generation()` + game root + `type_id`. `race_forget()` runs from `gameref_remove()`, `race_reset()` from `gameref_clear()`. `RpkIndex::generation()` and `GameRefState::incarnation` are new here; both are monotonic and never recycled. |
| Natives | `getNearestCross(V,F)V3`, `findRoute(V,V)F`, `getRouteLength(V,V)F`, `getRouteLength()F`, `getRoutePos(F)V3`, `getRouteDist(V)F`, `getStartDirection(V,V)V3`, `alignToRoad(V)[V3`. The `getNearestCross` stub was removed from `natives_renderref.cpp` so exactly one registration exists. |
| Adaptation | Upstream parallelises the 64 m projection grid with `jobs_parallel_for` (`slrr/job_system.hpp`), which this tree does not have. The per-lane index split is kept, the pass runs sequentially, and the merge is unchanged; a worker pool can be dropped in later without touching the query path. |
| Units | `city.trc` node coordinates are metres (first edge 139.8 m, city span ≈ 2 km), so route lengths are metres and `getRoutePos`/`getRouteDist` are fractions of that length. SCX stays in cm — the two are not mixed here. |

Deviations from #180, each against the recovered native body:

| Native | Recovered engine | #180 | Ours |
|---|---|---|---|
| `getRouteLength()F` @ 0x00483C00 | cached length, `0` when no cached spline | `-1` | `0` |
| `getRoutePos(F)` @ 0x00483B30 | `u=(1-t)*u0+t*u1`, i.e. `t` is a fraction; unclamped | treats `t` as metres | fraction |
| `getRouteDist(V)F` @ 0x00483C20 | `(u-u0)/(u1-u0)`; `0` without a route, `-1` for null | returns metres | fraction, same 0/-1 rules |
| `getStartDirection(V,V)V3` @ 0x00483400 | tangent at route start, **negated**; zero → null; cache untouched | tangent as-is, cached | negated, local query, cache untouched |
| `getNearestCross(V,F)V3` @ 0x00483D00 | minimizes `abs(len(node-approx)-distance)`; boxes the approximation on a miss | null on a miss | same as recovered |
| `alignToRoad(V)[V3` @ 0x00483E60 | always boxes `{snapped, unit XZ tangent}` | null on a miss | same as recovered |
| route cache on a failed query | replaced only on success | cleared | replaced only on success |

| Tests | `test_road_routes`, 6/6 pass. Synthetic: one-way vs two-way direction separation, arc length, projection, crossing-radius sign, shared crossing phase (green/yellow/opposing red/repeat, `-2` for a regulated approach without a lamp, `-1` for an unregulated one). Corpus (retail install): `city.trc` → **928 directed edges, 271 crossings, 1146 signal heads**, 139.8 m first edge, route both ways; 975 sampled node pairs all finite, none unreachable; every native invoked directly against the real ground, including the failed-query cache rule. |
| Runtime | No route native is called by the `--bench-map ValoCity --bench-drive` path: it enters the map through `Track.StartMap`, not through `City.startRace`/`RaceSetup`, and no traffic is spawned. Calls are therefore proven at the native boundary in the suite (`[Ф-runtime]`), not by a stock script (`[Ф-код]` only). The career/street callers stay in the untested list of this block. |
| Status | **Ported** |

### U2-B. Ambient traffic

| Field | Content |
|---|---|
| Upstream files | `src/resources/traffic_runtime.cpp` (population and registration model, event queue, `race_traffic_tick`) and the external-motion hunks of `part_runtime.cpp` (`part_set_external_motion`, `part_animate_external_wheels`, the inline-wheel list) |
| Our files | new `src/resources/traffic_runtime.cpp` (830), `tests/test_traffic_runtime.cpp` (289), `src/resources/part_runtime.cpp` (+101/−4), `include/slrr/part_runtime.hpp`, `include/slrr/race_runtime.hpp`, `src/resources/race_runtime.cpp` (hooks in `race_forget`/`race_reset`/`register_race_natives`), `src/boot/mainloop.cpp`, `CMakeLists.txt`, `assets/progress/native-coverage.*` |
| Problem | The ten `GroundRef` traffic natives were unbound and nothing moved a car that is not the player: `addTrafficN` is called by the map with the stock `cars/traffic/*.rpk` templates, but no runtime consumed the requests, so the road graph from U2-A had no user. |
| Population | `addTrafficN(type, n, lenBegin, lenEnd, wheelBase)` merges the weight of a template instead of appending a duplicate; `addTrafficP(type, pos, n, …)` keeps one position seed until the tick satisfies it; `addTrafficCar(car, pos)` attaches a car the caller already owns and never takes it over; `delTraffic` drops the ground's population and every registration. The instance cap is `traffic_density × 100` (the slider is 0…1 and the install ships 0.2, so 20 cars). |
| Lifecycle | One `Registration` per live car, keyed by a never-recycled id and holding both `(ground, ground_incarnation)` and `(car, car_incarnation)`, so a `GameRef` re-created on the same address is detected instead of being driven. Owned cars are retired (and destroyed) when they pass 260 m from the player, script-owned ones at 400 m; both are retired when their lane is closed by `haltTrafficPath`. `race_forget` runs from `gameref_remove`, `race_traffic_reset` from `gameref_clear`, so no entry outlives its ground. |
| Movement | U2-A's `traffic_edges()` is the lane set: each car owns `(edge, along)` and advances `cruise` (15…35 km/h, `1/3.6` from the shipped units) with an 0.8 m/s² acceleration limit and a 4 m/s² brake limit, crossing into a random unblocked successor edge at the end of its lane. Spacing to the car ahead on the same lane is a slowdown only — no impulses and no contact resolution. `publish` puts the chassis on the graph point (tyre contact), takes the height from the scene floor when there is a hit, and gives it a yaw-only pose. |
| Wheels | Traffic templates install no rim parts, so their wheels exist only as CFG `wheel`+`render` lines. `render_part` now records those as `InlineWheel`s; `dynamics_wheels` gives them a contact point and the nominal traffic suspension (0.25 m spring, 0.1 m static sag at the loaded corner, 0.65 critical damping) instead of leaving the body on its shell, `pose_wheels` places them on the parked frame and `part_animate_external_wheels` spins and steers them from the distance the runtime advanced. |
| External motion | `part_set_external_motion(car, true)` makes `advance_chassis_tick` skip the chassis, so the physics step never simulates a traffic car; `part_park_chassis` still loads the springs, which is what keeps the pose on the road. The player car is untouched: the inline branch is gated on “no rim part in the slot”, so a car with installed rims keeps its previous path. |
| Safe spawn | The lane spot is chosen before anything is allocated: the candidate must be off a crossing, longer than 15 m, in the 45…160 m band around the player, more than 12 m from any other resident, and unblocked. Nothing is created when the ground has no TRC graph, when no candidate is found in 96 tries, or when `part_create` refuses the template, and the spawn budget is two cars per tick. |
| Natives | `addTrafficN`, `addTrafficP`, `addTrafficCar`, `remTrafficCar`, `notifyTrafficCar`, `delTraffic`, `setTrafficCarBehaviour`, `haltTrafficCross`, `haltTrafficPath`, `clearHalts`. `car_add`/`car_rem` events are queued during the tick and delivered afterwards through `handleEvent` with a GC root scope, never from inside the native call. |
| Main loop | `race_traffic_tick(jvm, sim_seconds)` runs immediately after `part_physics_advance` in the Physics perf scope, so a paused or warped game moves traffic by exactly the bounded simulation step and the halt clock uses the same time base. |

Deviations from #180 and from the recovered engine:

| Topic | #180 / recovered engine | Ours |
|---|---|---|
| Driving | `drive.native_traffic` takes throttle/brake through the vehicle physics; the recovered `traffic_car_frame_update` @ 0x577FA0 also integrates a real body | The pose is published by the runtime and the physics body is skipped, because this tree's drive model is built around one controlled vehicle. Deferred to U2-F |
| Signal phases | Cars stop at red/yellow inside the traffic step | **U2-C**: `race_traffic_signal_cap` caps the lane at the stop line; the rest of the block is described below |
| Streaming | `streamed_hidden` marks a car hidden while it is out of the streamed set | This tree has no such flag, so cars are created and retired by distance instead of being hidden and re-shown |
| Race bots | The same registry feeds the race brain, and `set_traffic_mode` switches a car between ambient and racing | The registry is shared and ready, but the commands are U2-D |
| Font/UI, sounds, debug overlay | Upstream adds overlay counters and route ribbons | Not ported; `RenderRef.plotRoute` keeps its signature and returns 0 |

| Tests | `test_traffic_runtime`, 4/4 pass: template registration (merge by type, junk refused, seeds, `delTraffic`), car lifecycle (idempotent attach, `remTrafficCar`, re-attach under a new id, death with the ground), road binding (snap to a crossing, project back onto the lane, flat pose, graphless ground registers but never places), and halt/idle behaviour (`haltTrafficCross`/`haltTrafficPath`/`clearHalts`, three no-op ticks without a world, reset). Movement needs the frame loop, so it is covered by the in-game run below and not by the suite. |
| Runtime | `--bench-map ValoCity --bench-time 40 --bench-drive 20` with `SLRR_TRAFFIC_LOG=1`: 20 cars resident at `cap=20` from nine stock templates, moving along their lane at 4.9…6.3 m/s with `along` and the world pose advancing together (edge 1050, `along` 353→458 m, pos −229,955 → −208,1057), `total=26 dropped=6` at t=20, i.e. cars that fall behind the player are retired and replaced instead of accumulating. `drive done`, exit 0. Window captures at the same moment show a taxi and two sedans on the carriageway next to the player. With the spawn band forced out of range (no traffic at all) the player frame at t=10 is identical to the traffic run, so ambient traffic does not perturb the player body. |
| Status | **Ported**, physics driving, race brain and native vehicle assembly still open (signals closed in U2-C) |

### U2-C. The lamp at the junction

U2-B moved cars along their lanes and honoured script closures, but nothing read
the phase the graph already computed: `RoadRoutes::signal` existed for the route
queries, and the traffic step never asked. Ambient cars therefore drove through
every crossing on the map.

**The rule.** `race_traffic_signal_cap(graph, edge, along, speed, clock)` returns
the speed a lamp allows, or infinity when nothing applies. It is a pure function of
the graph, which is why the suite can pin it without a scene:

- `-1` unregulated and `2` green: no cap.
- `0` red: cap `sqrt(2 * a * d)` where `d` is the distance left to the stop line,
  i.e. the same braking shape the lane spacing already uses, so the car comes to
  rest exactly at the line, 5 m before the junction.
- `1` amber: a cap only if the car can still stop (`d > v²/2a + 0.5`). A car that
  cannot is committed, and stopping it inside the junction is worse than letting
  it cross.
- A car already past the line is never capped. A red light must not park a car
  across the road it is crossing.
- A crossing link itself is never capped; the junction is not a place to brake.
- `-2`, a regulated approach whose lamp is missing: a timed stop on the graph's own
  13 s cycle (5 s stop), never a permanent red — the map is missing a lamp, not
  closing a road. That policy is the host's, and it is the one decision here that
  has no original to copy.

**Where it applies.** In the drive step, after the closure check:
`desired = min(desired, cap)`. A closure outranks a lamp, because a closed lane is
a wall and a red light is a wait.

**Checked in game.** `SLRR_TRAFFIC_SIGNAL_TRACE=1` counts the lane steps a lamp
capped, once a second. `--bench-map ValoCity --bench-time 20 --bench-drive 18`
with the population at its default: 81, 126, 74, 68, 60, 92, 94, 43, 28, 83, 91,
35, 0, 0, 0, 26, 27, 37 capped steps per second, `drive done`, exit 0. The three
zero seconds are a stretch of road where no resident was approaching a red; the
count returns when the band does.

**Tests.** `test_traffic_runtime` 7/7, the new case `signal_stops_at_the_line`
built on `city.trc` itself: green unlimited, red capped and shrinking as the line
approaches, zero exactly at the line, unlimited past it, amber stopping a slow car
30 m out but not a fast one 5.5 m out, crossing links and unregulated lanes
unlimited. The suite has to call `open_all()` first — the signal heads are ICFG
`trlight` placements in the ground map and its exterior maps, and a graph built
without them has no lamps at all, which is also why the corpus test reports 1146
heads.

**Status: done.** The remaining #180 blocks are U2-D (race brain), U2-E (ROC) and
U2-F (native vehicle assembly).

### U2-B.1. Traffic quality and the race transition

Four defects of the U2-B runtime, each diagnosed from the running host before it
was touched. No part of the runtime was rewritten: the pose calculation moved
into one function so it could be tested, and three call sites changed.

| Defect | Cause | Fix |
|---|---|---|
| Yaw snapped at every turn | The map stores a junction as a straight chord between the two lane ends (`RoadRoutes` `crossing` edges), so the chord direction is 45–70° away from both lanes. `publish()` took the pose heading from the current edge alone, so the car's heading jumped twice per turn — at the chord's entry and at its exit. A 1.5 m geometry sample also stepped the heading on curves. | `race_traffic_pose()` returns `{position, heading}` for `(edge, along, entry)`. On a crossing edge the position follows a cubic Hermite between the lane ends whose end tangents are the incoming and outgoing lane directions (`entry` is the edge the car came from), so position and heading are C1 through the junction; on a lane the heading is the tangent over a ±1.2 m window. No yaw smoothing, no speed change: the path itself is continuous and the heading comes from that path. |
| One-frame vertical jumps | `publish()` took the height from `scene_instances_sample_floor(x, z, road_y + 4)`, which returns the highest surface below the probe start. A bridge deck, arch or canopy ~4 m above the road was accepted as the road and lifted the car for exactly one frame; the diagnostic caught three cars with an identical +3.70 m offset and an unchanged `road_y`. | The graph point is the lane centreline, so only a surface within 1.2 m of it may replace the height. Ceilings, decks and far-below geometry are rejected and the graph height is kept. |
| Cars materialised next to or in front of the player | The U2-B band (45…160 m) was chosen without measuring the HUD, and it allowed the forward direction. The minimap's scale bar is 37 px for "50 m" on a ~215 × 140 px window, i.e. ~290 m wide (~145 m half-width), so 45 m spawns landed inside it. | Spawn band is 180…240 m with a 300 m retire radius (hysteresis, no churn), and a spot inside the forward cone (`dot > 0.5`) is refused below 260 m. Creation therefore happens behind or beside the player and outside the map, and a car only enters the view by being approached. Ambient cars are not drawn on the minimap (it is a baked tile map with the player sprite), so the police markers the player sees there come from the script's marker calls, not from this runtime. |
| Ambient traffic stayed on the race line | Nothing in the city teardown path runs when a street race starts in the same map: the city GroundRef is not destroyed, so `race_traffic_forget` never fires, and `delTraffic` is only reached from `Track.exit`. | `race_traffic_note_section(section)` is called from the frame loop with the active section class. A `RaceSetup`/`Track` section retires every car this runtime created (script-owned cars are a different mechanism and are kept) and stops refilling; a `Valocity`/`City` section re-enables the population, which fills again through the normal mechanism. |

| Point | Content |
|---|---|
| Files | `src/resources/traffic_runtime.cpp` (`traffic_pose`, `publish`, `place`, retire loop, ambient policy), `include/slrr/race_runtime.hpp` (`TrafficPose`, `race_traffic_pose`, `race_traffic_note_section`/`set_ambient`/`ambient_enabled`), `src/boot/mainloop.cpp` (section hook), `tests/test_traffic_runtime.cpp` (+2 tests) |
| Diagnostics | `SLRR_TRAFFIC_DIAG=1` reports a per-frame yaw step > 0.12 rad or height step > 0.4 m with id, edge, arc position, world and road height, body height, both headings, delta time and whether the edge changed. `SLRR_TRAFFIC_LOG=1` also prints `[traffic-spawn]` with the spawn distance and forward projection. Both are off by default. |
| Section policy | `RaceSetup` and any `Track`/`ROC` section disable the ambient population; `Valocity`/`City` enable it; every other section (menus, garage, dialogs) leaves the current policy untouched. The check runs on section adoption, i.e. before the race scene prepares its rivals, and is a one-time transition, so nothing is deleted while a list is being walked. |
| Tests | `test_traffic_runtime`, 6/6. New: `junction_continuity` walks a real 70° corner of `city.trc` at 0.1 m steps (185 samples) and pins position step ≤ 0.104 m, heading step ≤ 0.021 rad, height step ≤ 0.001 m, and that the pose turns through the same angle the lanes make (1.14 rad of 1.23 rad) while the raw chord pose would snap by more than 0.3 rad. `race_section_clears_ambient` pins the policy and that a script-owned car survives it. |
| Runtime | 35–40 s drives on ValoCity: `SLRR_TRAFFIC_DIAG` reported **zero** discontinuities; 39–40 spawns with distance 180.3…239.0 m and forward projection ≤ 0.42; population stayed at `cap=20` for the whole run, `drive done`, exit 0. A section probe (temporary, removed) forced the race transition in a live scene: 20 cars → 0 → 20 after the return, no GC error, no stale `GameRef`, exit 0. The real section path was exercised too: `--bench-map TestTrack` adopts `java.game.TestTrack` and the log contains no `[traffic]` spawn at all, so a track scene gets no ambient traffic. (`TestTrack` itself drops the spawned car through the map to y ≈ −93 000; that is a pre-existing defect of that map scene, unrelated to traffic, because no traffic exists there.) |
| Not verified | A street race cannot be started without the menu: `RaceSetup` is a Java/TREE state, so the in-game check above forced the same policy call rather than the actual `changeActiveSection(RaceSetup)`. The partial NightRace flow (rivals created, race started and finished) was not re-run; nothing in this change touches script-owned cars, and the suite pins that they survive the transition. Race rivals still do not drive (U2-D) and there are no car-to-car or car-to-traffic collisions (U3/#205). Police cars are `cars/misc/Police.rpk` spawned by `java.game.PoliceScout`/`City.policeCars`, not by the ambient templates; their placement is script-side and Police AI is untouched. |

### U2-B.2. Measuring the city

The first thing U2-B.2 needed was a profile that can say where a city frame
goes, including the time no scene zone owns. Everything below is off unless
asked for.

| Knob | Effect |
|---|---|
| `--perf-log CSV\|-` | one `[perf]` line per second per `frame_profiler` zone, plus `[hitch]` for any frame over 17 ms with the zone breakdown and `rest=` for time no zone claimed |
| `--perf-frames CSV` | one row per frame: total, every zone, renderer numbers, and `rest_ms` |
| `SLRR_TRAFFIC_PROFILE=1` | stage profile inside `race_traffic_tick`: tick, retire, drive (of which pose, floor, park, wheels), search, create, events, project. One `[tprof]` window line every five simulated seconds, `[tslow]` for a tick over 4 ms, and a table with n/avg/p50/p95/p99/max at exit |
| `SLRR_STACK_SAMPLE=1` (+`_HZ`) | CPU sampling of the main thread with DbgHelp call stacks (`RelWithDebInfo` for names): top functions and top stacks every five seconds |
| `SLRR_TRAFFIC_DENSITY=`, `SLRR_PEDESTRIAN_DENSITY=` | measurement overrides of the shipped sliders, so one build covers several populations |
| `SLRR_TRAFFIC_FREEZE=1` | drives the cars that exist and stops spawning or retiring, which separates "update" cost from "create/destroy" cost |
| `--bench-route` | the benchmark car follows the road graph instead of driving straight: the run covers streets, junctions and turns, and backs off when it gets stuck |

Zones now cover the loop as a whole: `ambience` (scene ambience and triggers),
`pump` (window pump and section render sync) and `gc` (the stop-the-world sweep)
join the old `scripts`/`physics`/`traffic`/`flush`/`collect`/`shadows`/`refl`/
`world`/`osd`/`present`. `rest` is the frame minus the zones, so a stall in code
without a scope cannot hide; it measures zero in every run so far.

The names in `kZoneNames` are asserted against `Zone::Count` after a mismatch
made every CSV column report another zone's time.

Two things the profile already changed:

* `registrations.at(id)` in the one-second trace threw `std::out_of_range` once
  retires started removing entries, which at density 1.0 happens every second;
  the lookup is a `find` now, because the id list is a frame-start snapshot.
* The first `race_traffic_tick` of a map builds the road graph (≈430 ms) and
  that showed up as a 450 ms first tick; it is once per map load, not per frame.

### U2-B.2. Streaming, and what the freezes were

U2-B.1 answered "where may a car appear" with one distance band, and forbade the
forward cone below 260 m while the band's far end was 240 m. The forward road was
therefore never populated; the player reported exactly that, and the trace
agreed: after U2-B.1 every spawn sat behind or beside the car.

The replacement is a population scheduler, `traffic_streaming.cpp`, with the pure
decision half separated from the runtime so it can be tested without a game:

| Part | Rule |
|---|---|
| Target | `reachable lane metres inside 260 m / spacing`, spacing 200 m at density 0 and 26 m at density 1 (the shipped 0.2 therefore asks for the original game's sparse city). The lane length is the part of each lane that actually falls inside the circle, junction chords excluded, so a target follows the streets the player has, not a fixed count. The budget is `density × 80` cars, which caps the maximum: a car costs about thirteen draw calls, and eighty packed cars look denser than a hundred spread over every side street for a fifth less geometry. |
| Candidate | A lane position, not a point in space. Reachable by driving: the player's own lane, the opposite direction of the same street, then everything forward through junctions (bounded BFS). Junction chords are never used. |
| Spacing | From the slider, so a dense setting really queues: one car per 40 m of lane and 30 m between cars at density 0, one per 20 m and 16 m between cars at density 1. On top of that a lane never holds more than `length / per_car_metres` cars. |
| Concentration | The runtime sorts the lanes nearest first and the scheduler draws towards the front of that list (`index = u² · lanes`), so a limited budget packs the street the player is standing on before it reaches a side street 200 m away. Without the bias a hundred cars were spread over seven kilometres of reachable lane and the slider changed almost nothing on screen. |
| Pop-in rule | Two rules that cover each other. A car is **never removed** where the player can see it: retirement asks the camera cone (`gameref_world_camera`, half angle + 6° margin) and skips anything inside it below 1.6× the retire radius, so the population is recycled out of sight. A car **may** be created in view, because a new car **dissolves in** over `SLRR_TRAFFIC_FADE` seconds (0.7 by default) with a dithered discard in `fs_vehicle.sc`; that is what lets the road ahead be populated immediately instead of only by cars that happen to drive in. The body clearance is now per direction and deliberately unequal: **200 m ahead**, where the eye is fixed on the road and the road test still read 130 m as "close", but **95 m to the side and 55 m behind**, where a dissolve is barely noticed and which is what keeps the streets around the player full. The spawn band grew to match and then some (460 m spawn, 540 m retire), because a 200 m front clearance inside a 260 m radius would leave almost no road ahead to seed, and because a 340 m band left a long straight showing a cluster of cars at the clearance and nothing behind it — the road test read that as "traffic only at the corner". The budget is 100 cars at the maximum. `SLRR_TRAFFIC_CLEAR=<scale>` scales all three together. See `docs/TRAFFIC_STREAMING_RESEARCH.md` for why distance alone could never hide a spawn here (the camera fog starts at 1.5 km). |
| Hysteresis | Spawn inside 260 m, retire beyond 330 m, minimum lifetime 6 s, and a lane that was just emptied is left alone for 4 s. Over-target retirement only applies to cars already outside the spawn radius and only by a margin, so the population settles instead of oscillating. |
| Cadence | The view and the target are rebuilt four times a second (six when the population is short) and one car is created per pass, so a long deficit fills over several passes instead of in one frame. Entering a ground is the exception: the first passes fill at 24 cars each, because a city the player has just entered must already be alive rather than wake up around him — measured at 36 cars after one second and 90 after five, against 4 and 20 before. |
| Churn guard | A lane that was just emptied stays empty for four seconds — but only while the population is at its target. Below target the guard only starves the road, which is what made long lanes look intermittently empty. |
| Boundaries | Script-owned cars (police scouts, race rivals) occupy their lane in the view but are never counted against the budget and are never retired to make room. The race-section policy from U2-B.1 is unchanged. |

| Point | Content |
|---|---|
| Files | `include/slrr/traffic_streaming.hpp`, `src/resources/traffic_streaming.cpp` (scheduler), `src/resources/traffic_runtime.cpp` (view building, reachability, cadence, retirement), `tests/test_traffic_streaming.cpp` |
| Tests | `test_traffic_streaming`, 15/15: population on every side of a standing player, cars ahead of a moving player, headway at speed, the collapsed bubble when parked, junctions never used, unreachable lanes skipped, target from lane length, budget cap, lane capacity, gap to existing cars, determinism, curved-lane distance measured on the lane, player clearance, refusal with a reason, slider-to-spacing mapping. `test_traffic_runtime` still 6/6, all other suites unchanged. |
| Runtime | ValoCity, noon, pedestrians at the slider maximum. Density 1.0: 100 resident cars held against a target of 100, 131 spawns and 31 retirements in 60 s of routed driving, spawn distances 15…260 m with 47 % ahead, 18 % behind, 34 % to the side. Density 0.2: 20 cars, 32 spawns, 40 % ahead. Standing still at density 1.0: 117 spawns, 39 ahead, 49 behind, 29 to the side, 31…260 m. |
| Dissolve, verified | With `SLRR_TRAFFIC_FADE=1000` (so nothing ever settles) the registry reports 78 live cars and the screenshot shows none: the uniform, the per-instance factor and the dithered discard all work. With the default 0.7 s the same scene shows solid cars, and the trace reports `fading=0…1` between spawns, so entries are released as cars settle. |
| No visible removal | Retirement never touches a car inside the camera cone (unless 1.6× past the retire radius), which is what the `[traffic-spawn] ang=` trace and `test_traffic_streaming`'s visibility case pin down. |
| Visible density | The trace counts what the player can actually see, not what exists: cars within 140 m, and cars ahead within 340 m. Standing still at noon at density 1.0 the near count settles at 10…14 and the ahead count at 44 (a 340 m window, so it follows the 200 m clearance rather than the empty band in front of it); driving the same route gives 19 near and 29 ahead at 80 resident cars. Density 0.2 gives 6 near and 3 ahead. |
| Spawn distances | Measured over 86 spawns at the fixed spot: the nearest ahead spawn is 206 m, the nearest side 103 m, the nearest rear 56 m, mean ahead count 44 cars inside 340 m. Before the widening the same measurements were 130/80/45 m. |
| Cost of density | Concentrating the population moved the cost from the simulation to the renderer, where it belongs: 16 cars at 0.2 draw 3619 calls and run at 244 fps uncapped, 80 cars at 1.0 draw 5809 calls and run at 172 fps. The traffic tick itself stays at 0.58 ms mean / 4.0 ms worst at maximum density, and 120 s of driving produced 4 frames over 33 ms and 2 over 50 ms, all of them the known `present` stalls rather than traffic. |
| Long run | 200 s of routed driving at density 1.0: the population held at exactly 100 cars against a target of 100, 269 spawns and 169 retirements (replacement as the player drives, not accumulation), nothing above 33 ms among 12 049 frames after map load (p50 13.29, p95 13.92, p99 14.45, max 31.5), the traffic tick at 0.71 ms mean / 4.4 ms worst, the GC zone at 6.5 ms worst with no frame over 10 ms, and the live `RenderRef` count flat at 2470…2520, so the streamed cars are collected. |
| Race scenes | `--bench-map TestTrack` adopts `java.game.TestTrack` and the new scheduler creates nothing there: the U2-B.1 section policy still holds with the streaming rewrite. |
| Cost | The scheduler made the spawn search almost free: `place` calls fell from 87 271 to 133 in a 60 s run because the candidate list is built once per pass instead of 96 random lanes per attempt, and the search stage went from 656 ms to 0.09 ms per minute. The cached view took the traffic tick from 0.99 to 0.75 ms per frame with 100 cars. |

The freezes the player felt were not in the traffic tick. Profiling with the
frame zones and a call-stack sampler found a stop-the-world GC sweep of
83…134 ms, whose finalizers and inline destruction ran inside the pause; that
fix is described in the commit and in the summary below. What remains over 40 ms
is `present`, the main thread waiting for the render thread while the city draws
~6000 calls, which happens with traffic disabled as well.

### U2-B.4. Batching ambient traffic — measured, implemented, rejected

Scope was to be the cars this module creates and the fake racers the career
scripts build from the traffic templates; cars from `cars/racers` stay out on
purpose — their tuning combinations are open-ended, so there is nothing stable to
batch on.

The concern raised before starting was that traffic cars carry per-car textures,
so the batch key would be unique per car. That is true only for the painted body
group: wheels, tyres, glass, chrome, lights and interiors share textures across
cars of the same model. The draw-group probe (`SLRR_BATCH_PROBE=1`, kept in this
tree) reports at density 1.0 with 100 cars:

| Metric | Value |
|---|---|
| vehicle submits per frame | 1342…1360 |
| distinct draw groups | 233 |
| groups with more than one member | 70, covering 1197 submits |
| submits a grouping pass would save | **83 %** |

## What was built and why it is gone

The instanced path was implemented and reverted in `bcbc4f1b`. It worked — the
CPU cost of the world pass roughly halved and 2100 of 6250 draws disappeared —
but the GPU paid more for the same triangles:

| Parked, same view, density 1.0, deterministic traffic | draws | `world` | **gpu** | fps |
|---|---|---|---|---|
| per-part submits (current) | 6258 | 1.12 ms | **3.59 ms** | 178.1 |
| instanced submits | 4151 | 0.61 ms | **5.77 ms** | 172.8 |

The routed run agrees (gpu 4.47 → 5.85 ms). The frame is decided by the GPU on
this content, so the trade is a loss: ~0.5 ms of CPU for ~2 ms of GPU.

Two lessons are worth keeping:

1. **Instancing is not free on the GPU in this renderer.** The same geometry, the
   same triangles and fewer draws still cost more, which points at how the driver
   handles one small geometry replayed many times per draw. The measurement has to
   be the GPU number, not the draw count.
2. `bgfx::allocInstanceDataBuffer` **clamps** the request to what is left of the
   transient buffer and still returns a pointer when nothing is left. The first
   implementation wrote a whole group into the clamped slice, which corrupted the
   buffers of the draws that followed: cars collapsed into sheets, lost their
   bodies and wheels, or left only a shadow. Any future batching must bound the
   request with `getAvailInstanceDataBuffer` and chunk the group, the way
   `draw_prop_batches` does.

## What is left to try for the GPU cost

The population policy (100 cars over a 460 m band, filled in bursts) is what the
GPU cost scales with, not the submit count. The levers that remain, in order of
expected effect:

1. per-part distance culling — a car 300 m away does not need its interior, glass
   or wheel detail drawn, which removes draws *and* triangles instead of just
   merging them;
2. a lower default budget on the density slider, which is already the only knob
   that scales the population (`budget = density * 100`);
3. model LOD, deferred by instruction.


## Deferred and rejected

Deferred items have a reason and a place in
[UPSTREAM_GAMEPLAY_PLAN.md](UPSTREAM_GAMEPLAY_PLAN.md); rejected items have a
concrete incompatibility.

| Item | Upstream | Status | Why |
|---|---|---|---|
| Roads / traffic / race AI | #180 | **Partially ported** | U2-A (graph, route queries), U2-B (ambient traffic) and U2-C (lamps stopping cars) are in; race brain, ROC and native vehicle assembly are the remaining U2 blocks. |
| Vehicle contacts / dynamics | #205 + #48 | **Deferred** | Upstream replaced the single-car loop with a `ContactVehicle` architecture (shared 120 Hz substeps, mass/inertia ownership, wheel loss, flaps). A subsystem port, not a fix. Phase 2. |
| RPK descriptor/property cache | #146 | **Deferred** | Changes `by_type_id` to lo16 and `find_entry` semantics plus payload/RSD caches and a `generation()`; must be designed against the B4.1.2 strict-resolution contract. Phase 3.1. |
| GC finalizer/heap lookup | #200 | **Deferred** | 172 lines of `gc.cpp`; needs its own reachability/finalizer evidence against `VM_GC.md`. Phase 3.2. |
| Point/spot light system, street and car lighting, shadows, AO, HDR, reflections | #235, #237, #279 | **Rejected** | Out of scope by instruction: this tree keeps `LIGHTING_AUDIT.md` semantics and the L2 investigation; no external render architecture is imported. |
| `Painter` replacement | #206 | **Rejected** | Would replace the legacy Painter and pre-empt the B4/A-series painter architecture. |
| JSON save format | #213 | **Rejected** | The save format is a closed contract; U1-B2 only made the writer atomic. |
| Cinepak / FMOD | #219 / #229 | **Rejected** | Separate multimedia projects with external dependencies. |
| Binary `Garage.class` | #162 | **Rejected** | Requires the exact SHA-256 of the installed `Garage.class`; upstream itself states the shipped `Garage.java` order was correct and the defect was in the MWM class. Nothing to verify without the hash, so nothing was copied. |
| `patch/` directory | — | **Rejected** | Mixed content; its lighting half is out of scope and its `Garage.java` changes are for ceiling lamps we do not have. |

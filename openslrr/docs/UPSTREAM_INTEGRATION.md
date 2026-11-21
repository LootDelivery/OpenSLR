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

## U1-A. Player constructor (#228)

| Field | Content |
|---|---|
| Upstream PR | [#228](https://github.com/EliasFD3S/openslrr/pull/228), commit `48b19c63` "#226 Run Player's constructor so the boot player has a car lot" |
| Upstream files | `src/natives/natives_io.cpp` (3-line removal), `docs/KNOWLEDGE_BASE.md` |
| Our files | `src/natives/natives_io.cpp`, `docs/KNOWLEDGE_BASE.md` |
| Problem | `java.game.Player.<init>()V` was registered as `native_noop`. Our VM resolves a host binding **before** the TREE (`Jvm::invoke_by_name`, `native_has_alias` check), so every `new Player()` in stock Java silently ran nothing: the object had no `parts`, `carlot` or `osd`. `Garage.enter` builds its player that way, so `--spawn-car` crashed on the garage entry. |
| Adaptation | Same single change: the registration is gone and the stock TREE runs. No host-side substitute was added, because the Java path already constructs the object. The comment on the removed block now records **why** the binding must not come back. |
| Tests | See the matrix below. |
| Result | `--spawn-car`: crash 5/5 before, 5/5 clean after; the car spawns with 88 parts, the garage renders and the run reaches the map. `--auto-new` and the plain boot are unchanged. |
| Status | **Ported** |

### Why the stub was there, and why it is not needed

The old comment said "`Player.<init>` during start_init is a long TREE. A miss
there aborts boot". The trace below shows that no boot path invokes it during
`start_init` at all — the constructor is reached only from `Garage.enter`
(`[section] enter java.game.Garage` → `invoke_by_name(Player, <init>)`). The
stub therefore guarded nothing; it suppressed the constructor at the one place
that uses it.

### Verification

Measured with an invoke trace (`[u1-call]`, temporary, not shipped) and a
player-field probe, `--game --window`:

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

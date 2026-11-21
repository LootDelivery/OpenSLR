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

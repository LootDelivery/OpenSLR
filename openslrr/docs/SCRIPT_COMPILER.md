# Script compilation and Language native contracts

Implemented 11 November 2026. See also [ENGINE_CONTRACT.md](ENGINE_CONTRACT.md).

## System.compileAll(String) : int

The host embeds the C++ TUFA compiler vendored under `third_party/slrrc` from
slrr-reverse. It does not spawn javac, Node.js, Python, or the original game.
The adapter is `src/jvm/script_compiler.cpp`; the binding is in
`src/natives/natives_io.cpp`. The compiler library uses C++17 and precise
floating-point semantics. Its literal-conversion self-test must pass before
any output is published. The rest of the host remains C++20.

The path is resolved against the game root. The selected directory is scanned
recursively; paths outside the root are rejected. The compiler reads the
game's existing TUFA declarations and source declarations for dependencies,
and resolves symbolic packs through the compiler's InstallPacks provider.
This provider reproduces a Build 940 session's pack-name conventions; exact
equivalence to every mod's live resource-registration order is not guaranteed.

The source layout is `X/src/Name.java`, with case-insensitive `src`. Output
is `X/Name.class`. Java files outside that source layout are ignored by the
vendored install scanner. The return value is the number of source units
attempted, matching the reference directory-walk contract, not the number of
generated classes. Logs distinguish attempted/succeeded/failed/class counts.

### Multiple classes in a single container

One Java-like source file can declare multiple classes. Its output is one
`.class` containing concatenated TUFA blobs, in declaration order. It is not
a Java SE class file. Independent `.java` files are not arbitrarily merged.
The host's TUFA reader validates all blobs before the file is published.

System sources may omit `package`. The adapter recovers their namespace from
the existing compiled container when possible, or from known engine script
directories. This avoids emitting an unqualified `System` instead of
`java.lang.System`, for example. Sources with an explicit package retain it.
Source-only dependency resolution in unusual mod layouts still needs separate
validation; successful compilation does not establish runtime equivalence.

### Publication and runtime lifetime

- Compilation happens in memory before replacing an output file.
- A syntax/code-generation/validation error preserves the previous output.
- The first replaced original is retained as `Name.class.openslrr.bak`.
- A temporary sibling file is published using a Windows atomic replacement.
- Script-thread checkpoints run between source units. A single compilation
  and initial declaration indexing remain synchronous C++ work.
- Existing VM classes are not replaced or reinitialized while executing.
  Restart the game to use regenerated code already loaded by the current VM.
- This implements explicit compileAll. Class loading never compiles: only the
  start-up pass below and an explicit `System.compileAll` call do.
- The host exposes the same compiler without a game window:
  `slrr_host --root DIR --compile-scripts DIR [DIR...]` prints
  `attempted/succeeded/failed/classes` and exits non-zero when a unit fails.
  Point it at a directory holding only the edited source so sibling units are
  not rebuilt.

### When a source is compiled

The original runtime rebuilds script sources when it starts, before any class is
loaded, so an edited `.java` takes effect on the next launch. The port does the
same in `boot_game` (`script_compile_stale`), with the same tree walk as an
explicit compile:

- Only sources whose container is missing or older than the source are rebuilt;
  an untouched tree compiles nothing and adds no start-up cost.
- Only the runtime's own script trees count: `system/`, `sl/`, `parts/scripts`,
  `maps/*/Scripts`. A car or map mod's private `parts/<name>/scripts` folder is
  left alone, as the original leaves it.
- A directory is compiled as a unit (one provider load, one declaration index),
  so a changed source may rebuild its siblings; that is what compileAll does for
  the whole tree.
- One line is printed when anything was rebuilt:
  `[compile] start-up: N source(s), M ok, F failed`. Failures keep the previous
  container and never stop the launch.

Class loading also compiles: `Jvm::load_class_file` calls
`refresh_script_source` before it reads a container, and rebuilds that one
directory when `<dir>/src/<Name>.java` is newer than `<dir>/<Name>.class`. This
covers every tree, including a mod's private `parts/<name>/scripts`, and it is
where a mod source actually gets rebuilt: a 2020 source in an unloaded mod is
left alone at start-up and compiled the first time the game asks for its class.
Measured on the original runtime: editing `sl/Scripts/game/src/GameLogic.java`
rewrites its container on the next launch (it is loaded during `Init`), while a
stale `parts/Work_VS_XX/scripts` source stays untouched at the main menu.

Compilation does not pretend to enqueue resource loading work in isLoading.

## System.isLoadingReset() : void

The original seeds a 32-sample resource loading history; it does not cancel
ongoing loading or clear the active-load flag. The host now keeps an analogous
history of measured pack-loading milliseconds. `openLib` and `rpkScan` record
their work, and host frames age the samples. Reset seeds the history from the
latest/pending work; a scan already running remains active.

`isLoading` reports active scans or history above a 4 ms average-work threshold.
Reading the flag never advances the ring. Idle frames drain it, and resetting
an idle loader creates no synthetic wait. This is a host approximation using
measured pack work, not a port of the original resource streaming queue.
Mesh/texture streaming queues remain outside its scope.

## System.analyze overloads

`analyze(Object)`, `analyze(float)` and `analyze(int)` deliberately retain
their no-op behavior. The reference PE implementations discard their arguments.
Adding logging, GC, serialization or script inspection would invent behavior.
These signatures remain in the no-op count rather than being relabeled as
newly implemented features.

## Validation

There is no compiler test target. A local probe compiled the installed patched `OptionsDialog.java`
against the game declarations without writing into the game. It generated
three classes in one 83,244-byte TUFA: `java.game.OptionsDialog`,
`java.game.ControlsFileReqDialog`, and `java.game.UR_Dialog`. Both the host
reader and SLRR-JVM-decompiler's strict parser accepted it. This is structural
validation, not an in-game test of every generated method.

Reference projects consulted: SLRR-JVM, SLRR-JVM-compiler,
SLRR-JVM-decompiler and slrr-reverse. The bundled compiler's exact provenance
and license are recorded in `third_party/slrrc/README.openslrr.md` and `LICENSE`.

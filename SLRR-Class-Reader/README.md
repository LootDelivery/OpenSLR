# SLRR Class Reader — native WinForms edition

A native Windows GUI for decompiling Street Legal Racing: Redline TUFA/TREE `.class` files into Java-like `.java` source.

This edition replaces the Electron/Chromium shell with C# Windows Forms. It does not bundle a browser engine, Node.js, or a separate .NET runtime.

## Features

- Open one or many SLRR `.class` files.
- Drag files into the application, or drop them directly onto the EXE for silent batch processing.
- Parse TUFA containers and decompile the contained classes.
- Reassemble multiple class blobs from one TUFA container into a shared `.java` compilation unit.
- Decode TUFA string constants as UTF-8, preserving Cyrillic and other non-ASCII text in generated Java source.
- Show generated source, decompiler diagnostics, and per-stage processing timings.
- Browse Java-like source with syntax highlighting, synchronized line numbers, and eased mouse-wheel scrolling.
- Keep the current source-viewer position and selection when switching between Russian and English.
- Adjust source-viewer wheel-scroll speed (×1, ×1.5, ×2, ×3); the setting is saved per user.
- Choose the source-viewer font size (8, 9.5, 11, 12.5, or 14 pt); the setting is saved per user.
- Copy source or reveal the generated file in Explorer.
- Russian / English interface, with automatic selection from the Windows UI language.
- Save the generated `.java` beside its input `.class` without overwriting existing files. If the normal name is occupied, a `.decompiled.java` alternative is used.

## Requirements

### To run

- Windows 10 or Windows 11, x64.
- .NET Framework 4.8.

The application uses the Windows Forms and .NET Framework libraries supplied by Windows. It does not include or download Electron, Chromium, Node.js, or third-party UI packages. The build produces a standalone EXE without an additional `.config` sidecar.

### To build

- Visual Studio 2019/2022 or Visual Studio Build Tools with the .NET desktop build tools.
- The .NET Framework 4.8 Developer Pack / targeting pack.

Open `SLRRClassReader.sln` and build **Release | x64**, or run `BUILD_WINDOWS.bat`.

Output:

`bin\Release\SLRR Class Reader.exe`

## Use

1. Open one or more `.class` files with **Open .class**, or drag them into the application.
2. The generated `.java` is saved next to each input file.
3. Place the source in the appropriate `src` directory of the game. **The game performs the compilation itself.**

The input `.class` files are never modified. Existing `.java` files are never overwritten.

After processing, the selected result shows separate timings for reading, TUFA parsing, source decompilation, formatting, source assembly, and writing. Each item in the result list shows its processing time. The status bar compares the sum of processing times with the total elapsed time including the GUI. Timing summaries and time units follow the selected Russian or English interface language.

### Silent batch mode

Drop one or more `.class` files directly onto `SLRR Class Reader.exe`. The files are processed without opening the GUI. The process returns exit code `0` if all files succeeded, or `1` if any file failed.

## Compatibility and limitations

- SLRR `.class` files use the game's TUFA/TREE scripting format. They are not standard JVM class files.
- The generated source is intended for the Java-like compiler built into Street Legal Racing: Redline, not `javac`.
- TUFA does not store original comments, whitespace, parameter names, or local-variable names. Names such as `arg0` and `localN` are synthetic.
- Decompilation reconstructs source from the available class data; it does not recover the exact original source text.
- The application does not compile `.java` files. The game handles compilation.

## Language preference

The language preference is stored in:

`%APPDATA%\SLRR Class Reader\language.txt`

The values are `system`, `ru`, or `en`. The source viewer's scroll-speed preference is stored in `scroll-speed.txt` in the same folder. Its font-size preference is stored in `font-size.txt`.

## Decompiler implementation

The TUFA parser and decompilation logic in `TufaDecompiler.cs` are a C# port of the TypeScript implementation maintained in the LootDelivery fork of `SLRR-JVM-decompiler`, which is based on the original project by **nedobylskiy**.

- Original project: https://github.com/nedobylskiy/SLRR-JVM-decompiler
- LootDelivery fork: https://github.com/LootDelivery/SLRR-JVM-decompiler

The original decompiler is licensed under MIT. See `THIRD-PARTY-NOTICES.txt`.

## Testing status

This source package has been prepared for a Windows build. It has **not** been compiled or runtime-tested on Windows in this environment. Before replacing the existing Electron release, build it on Windows and compare its output against the current Class Reader using representative stock and mod `.class` files. The most important acceptance test is the same real-game test already used for the Electron edition: replace a copied script class with its generated source and let the game compile it.

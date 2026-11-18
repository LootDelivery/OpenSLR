# SLRR Reverse Engineering

### Legacy reverse-engineering project for Street Legal Racing: Redline

> **Project archived — Development has moved to [OpenSLRR](https://github.com/EliasFD3S/openslrr).**
>
> This repository is no longer actively maintained. All ongoing engine development, improvements, bug fixes, and new features are now being developed in **OpenSLRR**, a modern C++ reimplementation of the Street Legal Racing: Redline engine.

## OpenSLRR — The Successor

**[github.com/EliasFD3S/openslrr](https://github.com/EliasFD3S/openslrr)**

OpenSLRR continues the work started here, taking the project beyond reverse engineering toward a functional, modernized and extensible game engine.

Rather than reproducing the original executable's architecture directly, OpenSLRR provides a new C++ implementation designed to execute the original game's scripts and load its existing assets.

Key areas include:

- **Modern C++ architecture** — A new engine implementation with a modular design.
- **64-bit support** — Moving beyond the original 32-bit executable.
- **Modern rendering** — bgfx-based graphics with Direct3D 11 support.
- **Script compatibility** — Execution of the original game's TUFA classes and TREE programs.
- **Native functions** — Reimplementation of the engine functions required by the original scripts.
- **Game systems** — Garage, vehicle assembly, interfaces, audio, physics, and driving.
- **Future improvements** — Expanded rendering capabilities, engine modernization, improved compatibility, and further gameplay reconstruction.

For the latest development progress, documentation, build instructions, and contributions, please visit the **[OpenSLRR repository](https://github.com/EliasFD3S/openslrr)**.

---

## About This Repository

This repository contains an earlier attempt to reverse engineer and reimplement the original **Street Legal Racing: Redline** engine, based on the Invictus `StreetLegal_Redline.exe` executable.

The project focused on understanding and reconstructing the original engine's internal behavior, including:

- The custom Java-like virtual machine and script execution.
- TREE instruction evaluation.
- Native method registration and dispatch.
- Game objects, resources, and engine references.
- Vehicle systems, physics, and collision behavior.
- Rendering and platform integration.

The implementation uses C++ and was originally designed as a Windows x86 host capable of running stock and commonly modified game scripts.

This work helped establish technical foundations and provided useful references for the development of OpenSLRR.

## Repository Structure

| Directory | Description |
|---|---|
| `engine/` | Original C++ engine reconstruction, runtime, and supporting systems |
| `tools/` | Reverse-engineering utilities, inventory generation, native stub generation, and IDA helpers |

The code is preserved for historical, educational, and research purposes.

## Project Status

**Archived — November 2026**

This repository represents an earlier stage of the SLRR engine reconstruction effort.

It is retained as a reference for anyone interested in the reverse-engineering process, original engine architecture, and research behind the project.

**No further development is planned in this repository.**

For current development, please use:

### [OpenSLRR — Active Development](https://github.com/EliasFD3S/openslrr)

## License

MIT License — see [LICENSE](LICENSE).

Original game assets, scripts, and executables remain the property of their respective owners and are not redistributed by this project.

This is an independent community project and is not affiliated with the original developers or publishers of Street Legal Racing: Redline.

# Changelog

## 2.2.5

- Fix a source-viewer startup crash caused by disposing a font object that WinForms could still reference.
- Cache all five source-viewer font sizes for the lifetime of the window and dispose them only after the controls are destroyed.
- Preserve the source selection and visible line when changing font size.


## 2.2.4

- Refresh batch timing summaries when switching between Russian and English.
- Localize millisecond units and per-file processing times in the results list.
- Add selectable source-viewer font sizes (8–14 pt) and persist the selected size between launches.

## 2.2.3

- Decode TUFA string constants as UTF-8, preserving Cyrillic and other non-ASCII text in generated Java source.
- Reject malformed UTF-8 constants instead of silently interpreting their bytes through Latin-1.

## 2.2.2

- Preserve the open source document, caret/selection, and scroll position when switching interface languages; refresh localized metadata without reloading the source.

## 2.2.1

- Added labeled language and scroll-speed controls to the top toolbar.
- Added selectable source-viewer scroll speeds (×1, ×1.5, ×2, ×3), persisted per user between launches.

## 2.2.0

- Added Java-like syntax highlighting for keywords, strings, comments, annotations, literals, numbers, and common types.
- Added a double-buffered line-number gutter synchronized with vertical scrolling and caret navigation.
- Added eased one-line-at-a-time mouse-wheel scrolling in the source viewer.

## 2.1.0

- Refreshed the WinForms visual language with a warm amber accent, refined spacing, and consistent button states.
- Redesigned the welcome/drop area, result cards, source header, and status bar while preserving the native WinForms implementation.

## 2.0.3

- Fixed WinForms docking order so the top header and left file list reserve their space before the central viewer fills the remaining workspace.

## 2.0.2

- Restored horizontal scrolling in the source viewer while keeping long code lines unwrapped.
- Reset the source viewer's horizontal offset after window resizing/maximizing, preserving its vertical scroll position.
- Rebuilt the reader pane with a responsive layout so the header, diagnostics, source view, and output path stay within the available viewport.

## 2.0.1

- Added per-stage timings for file reading, TUFA parsing, decompilation, formatting, source assembly, and output writing.
- Added per-file elapsed time to the result list and batch engine/wall-clock totals to the status bar.
- Removed the redundant runtime `app.config`; the Windows build no longer emits a `.exe.config` sidecar.

## 2.0.0 — Native WinForms edition

- Replace the Electron/Chromium application shell with a native C# Windows Forms interface.
- Port the TUFA parser, TREE lowering, source reconstruction, diagnostics, source formatting, and multi-class source bundling to C#.
- Keep multi-file processing, application drag-and-drop, and silent EXE batch mode.
- Keep RU/EN/Auto language selection and persist the preference under `%APPDATA%`.
- Preserve input `.class` files and avoid overwriting existing `.java` files.
- Target .NET Framework 4.8; no browser engine, Node.js, or third-party UI dependencies.

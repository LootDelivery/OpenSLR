// Crash log: what the host was doing when it died.
//
// The game is shipped without a console, so a crash leaves nothing behind unless
// the process writes it itself. Two halves: a small in-memory ring of the events
// that matter (scene changes, map loads, spawns), and a handler that turns an
// unhandled exception into a text report plus a minidump, with symbols resolved
// through the PDB that sits next to the executable.
#pragma once

#include <cstdint>
#include <string>
#include <vector>

namespace slrr {

// Records one event for the crash report. Keeps the last 64, thread-safe, cheap
// enough for transitions and loads; not for per-frame calls.
void crash_note(const char* text);

// Oldest first. Exposed for the report and for the tests.
std::vector<std::string> crash_recent();

// `crash-20261125-135500-1234.log` for a given time and process id.
std::string crash_file_name(std::int64_t unix_seconds, unsigned pid);

// Installs the unhandled-exception, terminate and pure-call handlers. Crash
// logs and minidumps land in `dir`, created if missing. Call once at start-up.
void install_crash_handlers(const std::string& dir);

// The directory the handlers write to (empty until installed).
const std::string& crash_log_dir();

}  // namespace slrr

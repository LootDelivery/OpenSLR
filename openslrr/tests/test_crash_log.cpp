// A crash report is only worth reading if it says what the host was doing. This
// pins the two halves that can be checked without crashing: the event ring
// (newest kept, oldest first, safe from several threads) and the report name,
// which must sort by time and never collide with an earlier run.
#include "standalone_test.hpp"

#include "slrr/crash_log.hpp"

#include <cstdio>
#include <string>
#include <thread>
#include <vector>

using namespace slrr;

namespace {

int index_of(const std::vector<std::string>& notes, const std::string& needle) {
  for (size_t i = 0; i < notes.size(); ++i)
    if (notes[i].find(needle) != std::string::npos) return static_cast<int>(i);
  return -1;
}

}  // namespace

SLRR_TEST(the_event_ring_keeps_the_newest_events_in_order) {
  for (int i = 0; i < 100; ++i) {
    char text[48];
    std::snprintf(text, sizeof text, "ring-%d", i);
    crash_note(text);
  }
  const std::vector<std::string> notes = crash_recent();
  std::printf("[crash] ring: %zu notes, last \"%s\"\n", notes.size(),
              notes.back().c_str());
  // A ring, not a log: bounded, and the bound drops the oldest.
  SLRR_REQUIRE(notes.size() <= 64);
  SLRR_REQUIRE(index_of(notes, "ring-99") >= 0);
  SLRR_REQUIRE(index_of(notes, "ring-0") < 0);
  // Oldest first, so the report reads in the order things happened.
  SLRR_REQUIRE(index_of(notes, "ring-99") == static_cast<int>(notes.size()) - 1);
  SLRR_REQUIRE(notes.front().find("ring-98") == std::string::npos);
  // Every line carries a local clock, `HH:MM:SS.mmm `.
  SLRR_REQUIRE(notes.back().size() > 12);
  SLRR_REQUIRE(notes.back()[2] == ':' && notes.back()[5] == ':' &&
               notes.back()[8] == '.' && notes.back()[12] == ' ');
}

SLRR_TEST(notes_from_several_threads_are_not_lost) {
  // The host records from the frame loop and from script callbacks; a torn or
  // dropped note would leave the report without the event that hurt.
  std::vector<std::thread> threads;
  for (int t = 0; t < 4; ++t) {
    threads.emplace_back([t] {
      for (int i = 0; i < 50; ++i) {
        char text[48];
        std::snprintf(text, sizeof text, "thread-%d-%d", t, i);
        crash_note(text);
      }
    });
  }
  for (std::thread& th : threads) th.join();
  const std::vector<std::string> notes = crash_recent();
  std::printf("[crash] threads: %zu notes kept\n", notes.size());
  SLRR_REQUIRE(notes.size() == 64);
  // Nothing half-written: each line is a clock, a space, and a whole note.
  for (const std::string& note : notes) {
    SLRR_REQUIRE(note.size() > 14);
    SLRR_REQUIRE(note[12] == ' ');
    SLRR_REQUIRE(note.find("thread-") == 13);
    SLRR_REQUIRE(note.back() != '\\0');
    SLRR_REQUIRE(note.find('\\n') == std::string::npos);
  }
}

SLRR_TEST(the_report_name_sorts_by_time_and_names_the_owner) {
  const std::string first = crash_file_name(1764000000, 4242);
  const std::string later = crash_file_name(1764000000 + 3661, 4242);
  std::printf("[crash] name: %s\n", first.c_str());
  SLRR_REQUIRE(first.rfind("crash-", 0) == 0);
  SLRR_REQUIRE(first.size() == std::string("crash-20261124-000000-4242.log").size());
  SLRR_REQUIRE(first.find("-4242.log") == first.size() - 9);
  // One report per run: two runs in the same hour must not overwrite each other,
  // which the pid and the seconds together already prevent, and the name sorts
  // chronologically for `Get-ChildItem -Filter crash-*.log`.
  SLRR_REQUIRE(first < later);
  SLRR_REQUIRE(crash_file_name(1764000000, 4243) != first);
}

int main() {
  const slrr::standalone::Test tests[] = {
      the_event_ring_keeps_the_newest_events_in_order,
      notes_from_several_threads_are_not_lost,
      the_report_name_sorts_by_time_and_names_the_owner,
  };
  return slrr::standalone::run("crash_log", tests);
}

// Crash log: see include/slrr/crash_log.hpp.
#include "slrr/crash_log.hpp"

#include <algorithm>
#include <array>
#include <chrono>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <ctime>
#include <exception>
#include <mutex>
#include <string>
#include <vector>

#ifdef _WIN32
#ifndef NOMINMAX
#define NOMINMAX
#endif
#include <windows.h>
#include <dbghelp.h>
#endif

namespace slrr {
namespace {

constexpr size_t kRecent = 64;
constexpr size_t kNoteMax = 512;

std::mutex& note_mutex() {
  static std::mutex m;
  return m;
}
std::array<std::string, kRecent>& notes() {
  static std::array<std::string, kRecent> r;
  return r;
}
size_t& note_next() {
  static size_t n = 0;
  return n;
}
size_t& note_count() {
  static size_t c = 0;
  return c;
}
std::string& directory() {
  static std::string d;
  return d;
}

std::tm local_tm(std::time_t t) {
  std::tm out{};
#ifdef _WIN32
  localtime_s(&out, &t);
#else
  localtime_r(&t, &out);
#endif
  return out;
}

// `HH:MM:SS.mmm`, the clock a driver can line up with what he was doing.
std::string stamp_ms() {
  const auto now = std::chrono::system_clock::now();
  const auto ms = static_cast<long long>(
      std::chrono::duration_cast<std::chrono::milliseconds>(
          now.time_since_epoch()).count() % 1000);
  const std::tm tm = local_tm(std::chrono::system_clock::to_time_t(now));
  char buf[24];
  std::snprintf(buf, sizeof buf, "%02d:%02d:%02d.%03d", tm.tm_hour, tm.tm_min,
                tm.tm_sec, static_cast<int>(ms));
  return buf;
}

}  // namespace

const std::string& crash_log_dir() { return directory(); }

void crash_note(const char* text) {
  if (!text || !*text) return;
  std::string entry = stamp_ms();
  entry += ' ';
  entry.append(text, std::min(std::strlen(text), kNoteMax));
  std::lock_guard<std::mutex> lock(note_mutex());
  notes()[note_next()] = std::move(entry);
  note_next() = (note_next() + 1) % kRecent;
  if (note_count() < kRecent) ++note_count();
}

std::vector<std::string> crash_recent() {
  std::lock_guard<std::mutex> lock(note_mutex());
  std::vector<std::string> out;
  out.reserve(note_count());
  const size_t start = (note_next() + kRecent - note_count()) % kRecent;
  for (size_t i = 0; i < note_count(); ++i)
    out.push_back(notes()[(start + i) % kRecent]);
  return out;
}

std::string crash_file_name(std::int64_t unix_seconds, unsigned pid) {
  const std::tm tm = local_tm(static_cast<std::time_t>(unix_seconds));
  char buf[96];
  std::snprintf(buf, sizeof buf, "crash-%04d%02d%02d-%02d%02d%02d-%u.log",
                tm.tm_year + 1900, tm.tm_mon + 1, tm.tm_mday, tm.tm_hour,
                tm.tm_min, tm.tm_sec, pid);
  return buf;
}

#ifdef _WIN32
namespace {

std::string exe_path() {
  char path[MAX_PATH] = {};
  GetModuleFileNameA(nullptr, path, MAX_PATH);
  return path;
}

std::string exe_directory() {
  std::string path = exe_path();
  const size_t slash = path.find_last_of("\\/");
  return slash == std::string::npos ? std::string(".") : path.substr(0, slash);
}

// PE time stamp: it identifies the build, so a stack from yesterday is only
// symbolised against a PDB of the same build and not silently against today's.
DWORD build_stamp() {
  auto* base = reinterpret_cast<unsigned char*>(GetModuleHandleA(nullptr));
  if (!base) return 0;
  auto* dos = reinterpret_cast<IMAGE_DOS_HEADER*>(base);
  if (dos->e_magic != IMAGE_DOS_SIGNATURE) return 0;
  auto* nt = reinterpret_cast<IMAGE_NT_HEADERS*>(base + dos->e_lfanew);
  if (nt->Signature != IMAGE_NT_SIGNATURE) return 0;
  return nt->FileHeader.TimeDateStamp;
}

void print_address(std::FILE* f, DWORD64 addr) {
  HANDLE proc = GetCurrentProcess();
  HMODULE mod = nullptr;
  char name[MAX_PATH] = "?";
  DWORD64 base = 0;
  if (GetModuleHandleExA(GET_MODULE_HANDLE_EX_FLAG_FROM_ADDRESS |
                             GET_MODULE_HANDLE_EX_FLAG_UNCHANGED_REFCOUNT,
                         reinterpret_cast<LPCSTR>(addr), &mod)) {
    GetModuleFileNameA(mod, name, MAX_PATH);
    base = reinterpret_cast<DWORD64>(mod);
  }
  const char* slash = std::strrchr(name, '\\');
  std::fprintf(f, "  %s+0x%llX  ", slash ? slash + 1 : name,
               (unsigned long long)(addr - base));
  char buf[sizeof(SYMBOL_INFO) + 256] = {};
  auto* si = reinterpret_cast<SYMBOL_INFO*>(buf);
  si->SizeOfStruct = sizeof(SYMBOL_INFO);
  si->MaxNameLen = 255;
  DWORD64 disp = 0;
  if (!SymFromAddr(proc, addr, &disp, si)) {
    std::fprintf(f, "?\n");
    return;
  }
  std::fprintf(f, "%s+0x%llX", si->Name, (unsigned long long)disp);
  IMAGEHLP_LINE64 line{};
  line.SizeOfStruct = sizeof line;
  DWORD line_disp = 0;
  if (SymGetLineFromAddr64(proc, addr, &line_disp, &line))
    std::fprintf(f, " (%s:%lu)", line.FileName, (unsigned long)line.LineNumber);
  std::fprintf(f, "\n");
}

void print_frames(std::FILE* f, EXCEPTION_POINTERS* ep) {
  HANDLE proc = GetCurrentProcess();
  if (ep && ep->ContextRecord) {
    CONTEXT ctx = *ep->ContextRecord;
    STACKFRAME64 sf{};
#ifdef _WIN64
    sf.AddrPC.Offset = ctx.Rip;
    sf.AddrFrame.Offset = ctx.Rbp;
    sf.AddrStack.Offset = ctx.Rsp;
    const DWORD machine = IMAGE_FILE_MACHINE_AMD64;
#else
    sf.AddrPC.Offset = ctx.Eip;
    sf.AddrFrame.Offset = ctx.Ebp;
    sf.AddrStack.Offset = ctx.Esp;
    const DWORD machine = IMAGE_FILE_MACHINE_I386;
#endif
    sf.AddrPC.Mode = AddrModeFlat;
    sf.AddrFrame.Mode = AddrModeFlat;
    sf.AddrStack.Mode = AddrModeFlat;
    HANDLE thread = OpenThread(THREAD_GET_CONTEXT | THREAD_QUERY_INFORMATION,
                               FALSE, GetCurrentThreadId());
    for (int i = 0; i < 48; ++i) {
      if (!StackWalk64(machine, proc, thread, &sf, &ctx, nullptr,
                       SymFunctionTableAccess64, SymGetModuleBase64, nullptr))
        break;
      if (!sf.AddrPC.Offset) break;
      print_address(f, sf.AddrPC.Offset);
    }
    if (thread) CloseHandle(thread);
    return;
  }
  void* frames[40];
  const USHORT n = RtlCaptureStackBackTrace(0, 40, frames, nullptr);
  for (USHORT i = 0; i < n; ++i)
    print_address(f, reinterpret_cast<DWORD64>(frames[i]));
}

BOOL CALLBACK print_module(PCSTR name, DWORD64 base, ULONG size, PVOID user) {
  std::fprintf(static_cast<std::FILE*>(user), "  %s  base=0x%llX size=0x%lX\n",
               name, (unsigned long long)base, (unsigned long)size);
  return TRUE;
}

void write_report(std::FILE* f, const char* reason, EXCEPTION_POINTERS* ep) {
  SYSTEMTIME st{};
  GetLocalTime(&st);
  std::fprintf(f, "slrr_host crash report\n");
  std::fprintf(f, "reason: %s\n", reason);
  std::fprintf(f, "time: %04d-%02d-%02d %02d:%02d:%02d.%03d\n", st.wYear,
               st.wMonth, st.wDay, st.wHour, st.wMinute, st.wSecond,
               st.wMilliseconds);
  std::fprintf(f, "exe: %s\n", exe_path().c_str());
  std::fprintf(f, "build stamp: 0x%08lX (symbols come from the PDB of this build)\n",
               (unsigned long)build_stamp());
  std::fprintf(f, "pid: %lu  thread: %lu\n", (unsigned long)GetCurrentProcessId(),
               (unsigned long)GetCurrentThreadId());
  if (ep && ep->ExceptionRecord) {
    const EXCEPTION_RECORD* er = ep->ExceptionRecord;
    std::fprintf(f, "exception: 0x%08lX  at 0x%llX\n",
                 (unsigned long)er->ExceptionCode,
                 (unsigned long long)(DWORD64)er->ExceptionAddress);
    if (er->NumberParameters)
      std::fprintf(f, "parameters: %lu, [0]=0x%llX [1]=0x%llX\n",
                   (unsigned long)er->NumberParameters,
                   (unsigned long long)er->ExceptionInformation[0],
                   (unsigned long long)er->ExceptionInformation[1]);
  }
  HANDLE proc = GetCurrentProcess();
  SymSetOptions(SYMOPT_UNDNAME | SYMOPT_DEFERRED_LOADS | SYMOPT_LOAD_LINES);
  const bool sym = SymInitialize(proc, exe_directory().c_str(), TRUE) != FALSE;
  if (!sym) std::fprintf(f, "symbols: unavailable (SymInitialize failed)\n");
  std::fprintf(f, "stack:\n");
  print_frames(f, ep);
  std::fprintf(f, "modules:\n");
  EnumerateLoadedModules64(proc, print_module, f);
  std::fprintf(f, "recent events (oldest first):\n");
  for (const std::string& note : crash_recent())
    std::fprintf(f, "  %s\n", note.c_str());
  std::fflush(f);
}

std::string report_path(const std::string& dir) {
  const std::string name = crash_file_name(
      static_cast<std::int64_t>(std::chrono::system_clock::to_time_t(
          std::chrono::system_clock::now())),
      GetCurrentProcessId());
  return dir + "\\" + name;
}

std::string report_directory() {
  std::string dir = directory().empty() ? exe_directory() + "\\logs" : directory();
  CreateDirectoryA(dir.c_str(), nullptr);
  return dir;
}

// Text report, then a minidump next to it: the text is readable at a glance, the
// dump keeps the whole state for a debugger.
std::string write_crash_files(const char* reason, EXCEPTION_POINTERS* ep) {
  const std::string dir = report_directory();
  const std::string path = report_path(dir);
  std::FILE* f = std::fopen(path.c_str(), "wb");
  if (f) {
    write_report(f, reason, ep);
    std::fclose(f);
  }
  std::string dump = path;
  dump.replace(dump.size() - 4, 4, ".dmp");
  HANDLE h = CreateFileA(dump.c_str(), GENERIC_WRITE, 0, nullptr, CREATE_ALWAYS,
                         FILE_ATTRIBUTE_NORMAL, nullptr);
  if (h != INVALID_HANDLE_VALUE) {
    MINIDUMP_EXCEPTION_INFORMATION info{};
    info.ThreadId = GetCurrentThreadId();
    info.ExceptionPointers = ep;
    info.ClientPointers = FALSE;
    MiniDumpWriteDump(GetCurrentProcess(), GetCurrentProcessId(), h,
                      MiniDumpNormal, ep ? &info : nullptr, nullptr, nullptr);
    CloseHandle(h);
  }
  std::fprintf(stderr, "[crash] report: %s\n", path.c_str());
  std::fflush(stderr);
  return path;
}

LONG WINAPI on_unhandled(EXCEPTION_POINTERS* ep) {
  write_crash_files("unhandled exception", ep);
  // No dialog, no background recovery: the report is written and the process
  // ends, which is what a driver staring at a frozen window wants.
  return EXCEPTION_EXECUTE_HANDLER;
}

[[noreturn]] void on_terminate() {
  write_crash_files("std::terminate", nullptr);
  std::_Exit(3);
}

[[noreturn]] void on_purecall() {
  write_crash_files("pure virtual call", nullptr);
  std::_Exit(3);
}

void on_invalid_parameter(const wchar_t*, const wchar_t*, const wchar_t*,
                          unsigned, uintptr_t) {
  write_crash_files("invalid CRT parameter", nullptr);
  std::_Exit(3);
}

}  // namespace

void install_crash_handlers(const std::string& dir) {
  directory() = dir.empty() ? exe_directory() + "\\logs" : dir;
  SetUnhandledExceptionFilter(on_unhandled);
  std::set_terminate(on_terminate);
  _set_purecall_handler(on_purecall);
  _set_invalid_parameter_handler(on_invalid_parameter);
  const std::string start = "host start: " + exe_path();
  crash_note(start.c_str());
}

#else

void install_crash_handlers(const std::string&) {}

#endif

}  // namespace slrr

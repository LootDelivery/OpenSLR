#pragma once

// Opt-in CPU sampling profiler for the host's main thread (SLRR_STACK_SAMPLE=1).
//
// The frame profiler measures how long each stage took; this says which
// function the main thread was actually inside, which is what separates "the
// stage is expensive" from "the stage waits for a resource". A sampler thread
// suspends the main thread on a fixed interval, records the raw return
// addresses into a preallocated ring buffer and resumes it; symbolisation
// happens afterwards on the sampler thread, so the suspended main thread is
// never asked to allocate, lock or fault in a symbol. Nothing is printed
// unless the env var is set, and no sample is taken before `start()`.
//
// Frames are resolved through DbgHelp, so a build with a PDB (RelWithDebInfo)
// is needed for names other than exported symbols.

namespace slrr::stack_sample {

// Starts sampling the calling thread. No-op unless SLRR_STACK_SAMPLE is set.
// `SLRR_STACK_SAMPLE_HZ` sets the interval (default 500 Hz, clamped 50..2000).
void start();
// Stops sampling and prints the final tables.
void stop();

}  // namespace slrr::stack_sample

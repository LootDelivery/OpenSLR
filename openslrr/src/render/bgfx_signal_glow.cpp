// U2-C: the lit lamp on each traffic-light head. One small additive pass at the
// end of the world: the heads are static props, so this reads nothing but the
// faces the resource side already measured and the phase the traffic clock is
// in. Drawn with depth test on and write off, so a car in front still hides it.
#include "bgfx_lib_backend.hpp"

#include "slrr/signal_glow.hpp"

#include <cstdio>

namespace slrr {

void LibBgfxBackend::draw_signal_glow(uint16_t view_id) {
  const SignalGlowMesh& mesh = signal_glow_mesh();
  if (mesh.vertices.empty() || !bgfx::isValid(glow_program_) || !glow_layout_.getStride())
    return;
  const auto floats = static_cast<uint32_t>(mesh.vertices.size());
  if (!bgfx::isValid(glow_vb_) || floats > glow_vb_floats_) {
    if (bgfx::isValid(glow_vb_)) bgfx::destroy(glow_vb_);
    const auto bytes = static_cast<uint32_t>(floats * sizeof(float));
    const auto* data = reinterpret_cast<const uint8_t*>(mesh.vertices.data());
    glow_vb_ = bgfx::createDynamicVertexBuffer(
        bgfx::copy(data, bytes), glow_layout_, BGFX_BUFFER_NONE);
    glow_vb_floats_ = floats;
    glow_revision_ = mesh.revision;
  } else if (glow_revision_ != mesh.revision) {
    const auto bytes = static_cast<uint32_t>(floats * sizeof(float));
    const auto* data = reinterpret_cast<const uint8_t*>(mesh.vertices.data());
    bgfx::update(glow_vb_, 0, bgfx::copy(data, bytes));
    glow_revision_ = mesh.revision;
  }
  if (!bgfx::isValid(glow_vb_)) return;
  // Ranges in phase order. Nothing to draw is the common case at night only
  // when no head is inside the view, but bgfx skips empty submits anyway.
  const uint32_t starts[3] = {static_cast<uint32_t>(mesh.red),
                              static_cast<uint32_t>(mesh.amber),
                              static_cast<uint32_t>(mesh.green)};
  const uint32_t ends[3] = {static_cast<uint32_t>(mesh.amber),
                            static_cast<uint32_t>(mesh.green), floats};
  for (int phase = 0; phase < 3; ++phase) {
    if (ends[phase] <= starts[phase]) continue;
    float tint[4];
    signal_glow_color(phase, tint);
    // Every prop is drawn with its Z mirrored; these faces are too.
    bgfx::setTransform(nullptr);
    // The scene projection is reversed (near = 1, far = 0), so "closer wins"
    // is GEQUAL here, not LEQUAL. With the normal test the lamps showed
    // through buildings and vanished behind the very housing they light.
    bgfx::setState(BGFX_STATE_WRITE_RGB | BGFX_STATE_DEPTH_TEST_GEQUAL |
                   BGFX_STATE_BLEND_ADD);
    bgfx::setVertexBuffer(0, glow_vb_, starts[phase] / 5, (ends[phase] - starts[phase]) / 5);
    bgfx::setUniform(u_glow_params_, tint);
    bgfx::submit(view_id, glow_program_);
  }
}

}  // namespace slrr

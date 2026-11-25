$input v_texcoord0

#include <bgfx_shader.sh>

uniform vec4 u_glowParams;  // rgb is the lens colour, a its intensity

// U2-C: the lens is a disc inside its face, hot in the middle and fading out
// to nothing at the rim. Drawn additively over dull glass, so the texture
// underneath still reads.
void main() {
  vec2 d = v_texcoord0 * 2.0 - 1.0;
  float r = length(d);
  if (r > 1.0) discard;
  float hot = clamp(1.0 - r, 0.0, 1.0);
  hot *= hot;
  gl_FragColor = vec4(u_glowParams.rgb * (u_glowParams.a * hot), 1.0);
}

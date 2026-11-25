$input a_position, a_texcoord0
$output v_texcoord0

#include <bgfx_shader.sh>

// U2-C: a lamp face is one quad of the drawn world, so the shader has nothing
// to do but pass it through and hand the corner coordinates to the falloff.
void main() {
  gl_Position = mul(u_modelViewProj, vec4(a_position, 1.0));
  v_texcoord0 = a_texcoord0;
}

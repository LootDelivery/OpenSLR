$input a_position, a_normal, a_texcoord0, a_texcoord1, a_texcoord2, i_data0, i_data1, i_data2, i_data3
$output v_normal, v_texcoord0, v_texcoord1, v_texcoord2, v_viewPos
#include <bgfx_shader.sh>
// U2-B.4: the same vehicle vertex path, driven by per-instance matrices instead
// of u_model. Ambient traffic draws dozens of cars that share one geometry, one
// material branch and one texture set, and instancing turns their submits into
// one per group. The fragment shader is the existing fs_vehicle, so the material
// logic (paint, glass, chrome, reflections, fade) is untouched.
void main()
{
    mat4 world = mtxFromCols(i_data0, i_data1, i_data2, i_data3);
    vec4 worldPos = mul(world, vec4(a_position, 1.0));
    gl_Position = mul(u_viewProj, worldPos);
    v_normal = mul(u_view, mul(world, vec4(a_normal, 0.0))).xyz;
    v_viewPos = mul(u_view, worldPos).xyz;
    v_texcoord0 = a_texcoord0;
    v_texcoord1 = a_texcoord1;
    v_texcoord2 = a_texcoord2;
}

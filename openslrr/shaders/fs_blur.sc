$input v_normal, v_texcoord0, v_texcoord1, v_texcoord2, v_color0, v_staticShadow, v_worldPos, v_tint

#include <bgfx_shader.sh>

// One level of the cubemap prefilter chain. The roughness then selects a
// level instead of widening a runtime cone -- a cone over a sharp cube
// undersamples it, which is what left the scratch-like streaks.
SAMPLERCUBE(s_sceneReflection, 0);
// x = source mip, y = kernel step in face-space units.
// Reuses the HiZ param slot so draw_fullscreen can pass it unchanged.
uniform vec4 u_hizParams;
// Camera basis of the cube face being written. The capture rasterises with
// exactly these vectors, so texel (u, v) maps back to the same direction it
// came from. Deriving the basis from a face index instead would depend on
// the cube convention's handedness; taking it from the view matrix cannot
// disagree with the capture.
uniform vec4 u_reflRight;
uniform vec4 u_reflUp;
uniform vec4 u_reflFwd;

void main()
{
    float su = v_texcoord0.x * 2.0 - 1.0;
    float sv = v_texcoord0.y * 2.0 - 1.0;
    float srcMip = u_hizParams.x;
    float kernelStep = u_hizParams.y;
    vec3 right = u_reflRight.xyz;
    vec3 up = u_reflUp.xyz;
    vec3 fwd = u_reflFwd.xyz;
    // A 3x3 tent over one texel of the level below. Chaining this gives a
    // Gaussian-ish blur whose width doubles per level, which is the shape a
    // roughness-driven lookup needs; sampling mip 0 with a wide kernel would
    // cost the taps for no gain.
    vec3 sum = vec3(0.0, 0.0, 0.0);
    float wsum = 0.0;
    for (int j = -1; j <= 1; ++j) {
        for (int i = -1; i <= 1; ++i) {
            float w = (i == 0 ? 2.0 : 1.0) * (j == 0 ? 2.0 : 1.0);
            vec3 dir = normalize(fwd + right * (su + float(i) * kernelStep)
                                          + up * (sv + float(j) * kernelStep));
            sum += textureCubeLod(s_sceneReflection, dir, srcMip).rgb * w;
            wsum += w;
        }
    }
    gl_FragColor = vec4(sum / wsum, 1.0);
}

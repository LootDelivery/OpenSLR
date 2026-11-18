$input v_normal, v_texcoord0, v_texcoord1, v_texcoord2, v_color0, v_staticShadow, v_worldPos, v_tint

/*
 * Diagnostic: draw one cubemap face as a flat panel.
 *
 * This deliberately only visualises the source of the reflections. It does
 * not touch vs_vehicle.sc, fs_vehicle.sc, the vehicle materials, the normals,
 * the reflection vector, box projection, PBR or the capture algorithm.
 *
 * For each pixel the sampling direction is built as
 *
 *     D = F + x * R + y * U
 *
 * with x, y in [-1, 1], where F is the direction the face looks at and R and
 * U are the horizontal and vertical axes of that face. The bases come from
 * the very same table the capture uses in update_reflections(), so the panel
 * shows the raw texture data in the coordinate system it was produced in --
 * no artificial rotation or mirroring to make the cross look tidy.
 */

#include "bgfx_shader.sh"

SAMPLERCUBE(s_sceneReflection, 0);

uniform vec4 u_faceBasis;   // xyz = F, w = unused
uniform vec4 u_faceRight;   // xyz = R
uniform vec4 u_faceUp;      // xyz = U
uniform vec4 u_faceParams;  // x = which cubemap (0 or 1), y = mip, z = enabled

void main()
{
	// The panel is a quad; v_texcoord0 is in [0, 1].
	vec2 uv = v_texcoord0 * 2.0 - 1.0;

	vec3 dir = normalize(u_faceBasis.xyz + uv.x * u_faceRight.xyz + uv.y * u_faceUp.xyz);

	// textureCubeLod because fxc rejects implicit derivatives in loops and we
	// want mip 0 regardless of the panel's screen size.
	float lod = max(u_faceParams.y, 0.0);
	vec3 c = textureCubeLod(s_sceneReflection, dir, lod).rgb;

	gl_FragColor = vec4(c, 1.0);
}

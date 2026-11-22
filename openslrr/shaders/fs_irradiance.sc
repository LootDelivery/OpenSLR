$input v_normal, v_texcoord0, v_texcoord1, v_texcoord2, v_color0, v_staticShadow, v_worldPos, v_tint

#include <bgfx_shader.sh>

/*
 * Irradiance probe: one face of a small cube holding the environment's
 * diffuse term.
 *
 * The vehicle shader used a flat ambient colour, so a car carried the same
 * light whether it stood against a bright wall, under a tree or in the open,
 * and its roof and its sills were lit identically. This is the same capture the
 * reflections come from, integrated over the hemisphere around the normal: the
 * result is what a matte surface would receive, which is exactly the term the
 * flat colour was standing in for.
 *
 * Cosine sampling, so the estimator is a plain average of the radiance. It is
 * sampled from a blurred mip of the capture because irradiance is smooth by
 * construction -- that trades a little accuracy for a large drop in variance,
 * which is what makes 32 taps enough at sixteen texels a face.
 */

SAMPLERCUBE(s_sceneReflection, 0);

// y = sample count, z = capture mip to read.
uniform vec4 u_hizParams;
// Camera basis of the cube face being written, from the capture's own axes.
uniform vec4 u_reflRight;
uniform vec4 u_reflUp;
uniform vec4 u_reflFwd;

#define PI 3.14159265359

// Cosine-weighted direction around n from a Hammersley pair.
vec3 cosineSample(vec2 xi, vec3 n)
{
	float phi = 2.0 * PI * xi.x;
	float cosTheta = sqrt(max(1.0 - xi.y, 0.0));
	float sinTheta = sqrt(max(xi.y, 0.0));
	vec3 h = vec3(sinTheta * cos(phi), sinTheta * sin(phi), cosTheta);
	vec3 up = abs(n.z) < 0.999 ? vec3(0.0, 0.0, 1.0) : vec3(1.0, 0.0, 0.0);
	vec3 tangentX = normalize(cross(up, n));
	vec3 tangentY = cross(n, tangentX);
	return normalize(tangentX * h.x + tangentY * h.y + n * h.z);
}

void main()
{
	float su = v_texcoord0.x * 2.0 - 1.0;
	float sv = v_texcoord0.y * 2.0 - 1.0;
	vec3 n = normalize(u_reflFwd.xyz + u_reflRight.xyz * su + u_reflUp.xyz * sv);

	int samples = int(max(u_hizParams.y, 1.0));
	// A mip whose texels are the size of a probe face: sixteen squared over nine
	// mips, so each tap is already an average of its share of the hemisphere
	// rather than one arbitrary texel of the sharp cube.
	float sourceLod = max(u_hizParams.z, 0.0);
	vec3 sum = vec3(0.0, 0.0, 0.0);
	for (int i = 0; i < 192; ++i) {
		if (i >= samples) break;
		float fi = float(i);
		vec2 xi = vec2((fi + 0.5) / float(samples), fract(fi * 0.6180339887));
		sum += textureCubeLod(s_sceneReflection, cosineSample(xi, n), sourceLod).rgb;
	}
	gl_FragColor = vec4(sum / float(samples), 1.0);
}

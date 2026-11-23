$input v_normal, v_texcoord0, v_texcoord1, v_texcoord2, v_color0, v_staticShadow, v_worldPos, v_tint

#include <bgfx_shader.sh>

/*
 * GGX prefilter: one mip of the environment cube, convolved with the
 * roughness that mip stands for.
 *
 * What this replaces: the cube's mips are a box filter down the chain, so a
 * roughness lookup picked a level that was blurred by area, not by a
 * distribution. On chrome that is invisible, on semi-rough paint it turns a
 * highlight into a flat patch and keeps hard edges of whatever was bright.
 *
 * The sampling is the split-sum prefilter from Karis 2013: GGX importance
 * sampling around the reflection direction with the lobes weighted by N dot L.
 * The view direction is taken equal to the reflection direction, which is the
 * usual approximation for a prefiltered environment, and the BRDF part of the
 * split sum stays where it is for now.
 */

SAMPLERCUBE(s_sceneReflection, 0);

// x = roughness of this mip, y = sample count, z = capture mip to read.
uniform vec4 u_hizParams;
// Camera basis of the cube face being written. Taken from the capture's own
// axes so a texel maps back to the direction it came from.
uniform vec4 u_reflRight;
uniform vec4 u_reflUp;
uniform vec4 u_reflFwd;

#define PI 3.14159265359

// Van der Corput radical inverse, base 2.
float radicalInverse(uint bits)
{
	bits = (bits << 16u) | (bits >> 16u);
	bits = ((bits & 0x55555555u) << 1u) | ((bits & 0xAAAAAAAAu) >> 1u);
	bits = ((bits & 0x33333333u) << 2u) | ((bits & 0xCCCCCCCCu) >> 2u);
	bits = ((bits & 0x0F0F0F0Fu) << 4u) | ((bits & 0xF0F0F0F0u) >> 4u);
	bits = ((bits & 0x00FF00FFu) << 8u) | ((bits & 0xFF00FF00u) >> 8u);
	return float(bits) * 2.3283064365386963e-10;
}

vec3 importanceSampleGGX(vec2 xi, float roughness, vec3 n)
{
	float a = roughness * roughness;
	float phi = 2.0 * PI * xi.x;
	float cosTheta = sqrt((1.0 - xi.y) / (1.0 + (a * a - 1.0) * xi.y));
	float sinTheta = sqrt(max(1.0 - cosTheta * cosTheta, 0.0));
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
	vec3 r = normalize(u_reflFwd.xyz + u_reflRight.xyz * su + u_reflUp.xyz * sv);

	float roughness = max(u_hizParams.x, 0.0);
	int samples = int(u_hizParams.y);
	// The level of the capture these taps are taken from. Sampling the sharpest
	// mip with a dozen taps is not an integral of the lobe, it is a dozen texels,
	// and the twelve change with every capture -- which is the noise that made
	// the cube shimmer while driving.
	float sourceLod = max(u_hizParams.z, 0.0);

	vec3 sum = vec3(0.0, 0.0, 0.0);
	float weight = 0.0;
	// The reflection direction doubles as the view direction: the result is a
	// lobe around r, which is what a roughness lookup wants.
	vec3 v = r;
	for (int i = 0; i < 128; ++i) {
		if (i >= samples) break;
		float fi = float(i);
		vec2 xi = vec2(fi / float(samples), radicalInverse(uint(i)));
		vec3 h = importanceSampleGGX(xi, roughness, r);
		vec3 l = normalize(2.0 * dot(v, h) * h - v);
		float nDotL = max(dot(r, l), 0.0);
		if (nDotL > 0.0) {
			sum += textureCubeLod(s_sceneReflection, l, sourceLod).rgb * nDotL;
			weight += nDotL;
		}
	}
	vec3 colour = weight > 0.0 ? sum / weight : textureCubeLod(s_sceneReflection, r, sourceLod).rgb;
	gl_FragColor = vec4(colour, 1.0);
}

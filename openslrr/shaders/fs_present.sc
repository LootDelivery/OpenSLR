$input v_normal, v_texcoord0, v_texcoord1, v_texcoord2, v_color0, v_staticShadow, v_worldPos, v_tint

#include <bgfx_shader.sh>

// Present pass: the HDR scene color (RGBA16F) is tone-mapped to the window.
// The legacy lighting pipeline is gamma-space, so the input is decoded to
// linear first (pow 2.2); the tonemapped result is encoded back.
// x: exposure, y: 1 = tonemap, 0 = plain copy (debug).
uniform vec4 u_presentParams;

SAMPLER2D(s_texColor, 0);

vec3 aces(vec3 x)
{
	// Narkowicz 2015 ACES approximation.
	const float a = 2.51;
	const float b = 0.03;
	const float c = 2.43;
	const float d = 0.59;
	const float e = 0.14;
	return clamp((x * (a * x + b)) / (x * (c * x + d) + e), 0.0, 1.0);
}

void main()
{
	vec4 src = texture2D(s_texColor, v_texcoord0);
	vec3 color = src.rgb;
	if (u_presentParams.y > 0.5) {
		// The scene target is linear now: the colour maps are decoded on sample
		// (BGFX_TEXTURE_SRGB) and the lighting multiplies linear values. Decoding
		// again here was a double gamma and darkened the whole image.
		color *= u_presentParams.x;
		color = aces(color);
		// No software encode here: the backbuffer is sRGB
		// (BGFX_RESET_SRGB_BACKBUFFER) and the hardware converts linear to sRGB on
		// write. Encoding in the shader as well would be a double gamma.
	}
	// Keep the source alpha only where a pass blends by alpha (the preview
	// composite). The world present writes the window backbuffer, whose alpha
	// the desktop compositor uses, so it must stay opaque or the whole image
	// reads slightly darker.
	gl_FragColor = vec4(color, u_presentParams.z > 0.5 ? src.a : 1.0);
}

$input v_normal, v_texcoord0, v_texcoord1, v_texcoord2, v_viewPos
#include <bgfx_shader.sh>
SAMPLER2D(s_texColor, 0);
SAMPLER2D(s_vehicleMix, 1);
SAMPLER2D(s_vehicleEnv, 2);
SAMPLER2D(s_vehicleBake, 3);
SAMPLERCUBE(s_sceneReflection, 4);
// The same environment convolved with GGX, one mip per roughness. Sharp reads
// still come from the raw cube above, because its mip 0 is 512 across and the
// prefiltered one is 128: on chrome that difference is the reflection.
SAMPLERCUBE(s_sceneReflectionGGX, 9);
// Diffuse ambient: the same capture, integrated over the hemisphere around the
// normal. Sixteen texels a face, one level, refreshed a face a frame.
SAMPLERCUBE(s_sceneIrradiance, 10);
// The prefilter chain, one cube per blur level. Each level is stored at mip 0
// of its own cube because writing a non-zero mip of a cube through blit does
// not take effect -- the levels came back empty and the slider acted as
// reflection strength instead of blur.
SAMPLERCUBE(s_sceneReflection1, 5);
SAMPLERCUBE(s_sceneReflection2, 6);
SAMPLERCUBE(s_sceneReflection3, 7);
SAMPLERCUBE(s_sceneReflection4, 8);
uniform vec4 u_dynamicReflection;
uniform vec4 u_envFilter; // x = prefilter spread, y = legacy weight
uniform vec4 u_envLod;    // x = 1 physical roughness mapping, y = bias, z = cap
uniform vec4 u_irrParams; // x = probe ready, y = scale
uniform vec4 u_reflectionProbe;
uniform vec4 u_reflectionBoxMin;
uniform vec4 u_reflectionBoxMax;
uniform vec4 u_lightmap;
uniform vec4 u_lightmapUv;
uniform vec4 u_vehicleLayers; // mix enabled, reflection weight, complemented alpha, painted
uniform vec4 u_vehicleChannels; // base, mix, reflection, generated reflection
uniform vec4 u_vehicleBaseUv;
uniform vec4 u_vehicleMixUv;
uniform vec4 u_vehicleEnvUv;
uniform vec4 u_vehicleDiffuse;
uniform vec4 u_vehicleSpec; // rgb, shininess
// U2-B.3 streaming fade. x is 1 for every car that is not being streamed in;
// the traffic runtime ramps it from 0 to 1 over the fade time on the cars it
// creates, and the dithered discard below dissolves them instead of popping.
uniform vec4 u_vehicleFade;
uniform vec4 u_sunDiff; // rgb, set
uniform vec4 u_sunAmb;
uniform vec4 u_sunSpec;
uniform vec4 u_localLight; // world position, radius
uniform vec4 u_sunDir; // render-space toward the light, w = present
vec2 channelUv(float channel, vec2 uv0, vec2 uv1, vec2 uv2)
{
    if (channel > 1.5) return uv2;
    if (channel > 0.5) return uv1;
    return uv0;
}
// Prefilter by hand: read the captured cube over a small cone instead of at
// one direction. Every tap comes from the same capture, so widening the cone
// grades the room rather than blending two different renders (which is what
// turned the last attempt into mush).
// Prefiltered environment lookup. The roughness widens a dense disc of
// samples around the reflection direction; every level comes from the one
// capture, as the plan requires.
//
// The taps spread over a golden-angle spiral instead of fixed rings. On
// rings the samples are sparse at a large radius, so the average turned into
// a structured repeating ghost -- the "recursion" that showed up as soon as
// the blur was pushed up. A dense spiral stays smooth at any width.
vec3 envPrefilter(vec3 dir, float radius)
{
    // Level from the roughness, exactly as before: the prefiltered chain is
    // built at the widths a box-filtered level used to have, so the softness a
    // material asks for is the softness it gets. What changed is the kernel --
    // a GGX lobe instead of an area average, which is what makes a highlight
    // fall off instead of breaking into patches.
    // Which level of the chain this material asks for.
    //
    // The old mapping was roughEnv*1.68, and roughEnv is 1 - specularExponent/24
    // -- a normalisation for fresnel and occlusion, not a roughness. It put car
    // paint at about 0.3, below the first level of the chain, so the material read
    // the sharp capture and the GGX levels were only ever reached by rubber. That
    // is why switching the chain on changed nothing to look at.
    //
    // Physical mapping: a Blinn-Phong exponent corresponds to a GGX roughness of
    // about sqrt(2/(e+2)), and level m of the chain stands for roughness
    // 0.02 * 2^m, so the level is log2(roughness/0.02). Paint with an exponent of
    // 20 to 60 lands on levels 3 to 4, chrome on 1 to 2, a mirror below half and
    // keeps reading the capture itself.
    float lod;
    if (u_envLod.x > 0.5) {
        float alpha = sqrt(2.0 / (max(u_vehicleSpec.a, 1.0) + 2.0));
        lod = clamp(log2(max(alpha, 0.02) / 0.02) + u_envLod.y, 0.0, u_envLod.z);
    } else {
        lod = clamp(radius * 14.0 + u_envLod.y, 0.0, u_envLod.z);
    }
    // Level 0 of the prefiltered chain is the capture itself, and it is not
    // written: the sharp end reads the capture cube directly, which is a whole
    // cycle fresher than the convolved levels.
    vec3 result = textureCubeLod(s_sceneReflection, dir, lod).rgb;
    if (u_envFilter.z > 0.5 && lod >= 1.0)
        result = textureCubeLod(s_sceneReflectionGGX, dir, lod).rgb;
    return result;
}
void main()
{
    // A car appearing within sight is the one thing this streaming policy may not
    // do, and distance cannot hide it (the camera fog starts at 1.5 km), so a new
    // car is dissolved in over a fraction of a second. Dithering keeps the
    // opaque state -- no blending, no sorting, and the discarded pixels are the
    // same ones the depth pass sees.
    if (u_vehicleFade.x < 0.999) {
        float dither = fract(sin(dot(gl_FragCoord.xy, vec2(12.9898, 78.233))) * 43758.5453);
        if (dither > u_vehicleFade.x) discard;
    }
    vec3 n = normalize(v_normal);
    vec2 baseUv = channelUv(u_vehicleChannels.x, v_texcoord0, v_texcoord1, v_texcoord2)*u_vehicleBaseUv.xy+u_vehicleBaseUv.zw;
    vec4 base = texture2D(s_texColor, baseUv);
    vec4 detail = texture2D(s_vehicleMix, channelUv(u_vehicleChannels.y, v_texcoord0, v_texcoord1, v_texcoord2)*u_vehicleMixUv.xy+u_vehicleMixUv.zw);
    vec3 albedo = mix(base.rgb, detail.rgb, detail.a*u_vehicleLayers.x)*u_vehicleDiffuse.rgb;
    // Toward the scene light. Rendered vertices flip Z, so the CPU already
    // flipped it. Without a light resource, the stand-in is (3, 5, 4).
    vec3 lightDir = normalize(mul(u_view, vec4(normalize(
        u_sunDir.w > 0.5 ? u_sunDir.xyz : vec3(3.0, 5.0, -4.0)), 0.0)).xyz);
    float ndl = max(dot(n, lightDir), 0.0);
    float shade = 0.45+0.55*ndl;
    // Ambient was one flat colour for the whole car: the same light on the roof
    // and on the sills, under a tree and in the open. The probe replaces it with
    // the captured environment integrated over the hemisphere around the normal,
    // which is the term the flat colour stood in for. Until a whole generation
    // has been integrated the material keeps the flat colour, so switching the
    // probe on never shows an unlit car.
    vec3 ambient = u_sunAmb.rgb;
    if (u_irrParams.x > 0.5) {
        vec3 worldNormal = normalize(mul(u_invView, vec4(n, 0.0)).xyz);
        vec3 probe = textureCube(s_sceneIrradiance, worldNormal).rgb;
        // The probe is the true hemisphere integral, and its absolute level comes
        // out close to the flat constant it replaces (measured ratio about 0.9 on
        // this map), so it is mixed in rather than normalised. Dividing by an
        // average was the previous attempt: the average itself steps whenever the
        // capture's mip chain is rebuilt, which is the last thing an ambient term
        // should do.
        //
        // What the probe changes is the *shape*. A vertical surface sees half sky
        // and half road, so it is dimmer than the roof, and that is correct --
        // the flat constant lit the sills like the roof. x carries the ready
        // flag, y how much of the probe to take over the constant.
        ambient = mix(u_sunAmb.rgb, probe, clamp(u_irrParams.y, 0.0, 1.0));
    }
    vec3 illum = u_sunDiff.a > 0.5
        ? (ambient + u_sunDiff.rgb*ndl)
        : vec3(shade, shade, shade);
    vec2 raw = channelUv(u_lightmap.z, v_texcoord0, v_texcoord1, v_texcoord2);
    vec2 luv = vec2(((raw.x - 0.5) - u_lightmapUv.z) * u_lightmapUv.x + 0.5,
                    0.5 - ((0.5 - raw.y) - u_lightmapUv.w) * u_lightmapUv.y);
    vec3 bake = texture2D(s_vehicleBake, luv).rgb;
    float use = step(0.5, u_lightmap.x);
    illum = mix(illum, vec3(1.0, 1.0, 1.0), use);
    bake = mix(vec3(1.0, 1.0, 1.0), bake, use);
	if (u_localLight.w > 0.0) {
		vec3 delta = mul(u_view, vec4(u_localLight.xyz, 1.0)).xyz - v_viewPos;
		float d2 = dot(delta, delta);
		float fade = saturate(1.0 - d2 / (u_localLight.w * u_localLight.w));
		float facing = max(dot(n, delta * inversesqrt(max(d2, 0.001))), 0.0);
		illum += vec3(1.0, 0.94, 0.80) * (fade * fade * facing * 3.0 / (1.0 + d2));
	}
    vec3 color = albedo*illum*bake;
    vec3 viewDir = normalize(-v_viewPos);
    vec3 halfDir = normalize(lightDir + viewDir);
    float ndh = max(dot(n, halfDir), 0.0);
    float spec = pow(ndh, max(u_vehicleSpec.a, 1.0))*ndl;
    float sheen = pow(ndh, 8.0);
    color += u_vehicleSpec.rgb*(spec+sheen*0.35)*(1.0-use);
    float ndv = max(dot(n, viewDir), 0.0);
    // Roughness from the authored shininess, so Fresnel behaves per material.
    float roughEnv = clamp(1.0 - u_vehicleSpec.a / 24.0, 0.15, 1.0);
    // fresnelSchlickRoughness: a rough surface must not become a perfect
    // mirror at grazing angles.
    float fresR = pow(max(1.0 - ndv, 0.0), 5.0) * (1.0 - 0.7 * roughEnv);
    // Lagarde's specular occlusion from the baked lightmap: where the bake is
    // dark the surface is tucked away and cannot see the room, so it must not
    // mirror it either.
    float ao = dot(bake, vec3(0.299, 0.587, 0.114));
    float occ = clamp(pow(max(ndv + ao, 1e-4), exp2(-16.0 * roughEnv - 1.0)) - 1.0 + ao, 0.0, 1.0);
    // Geometric specular AA (Filament/Toksvig): where the shading normal races
    // across the screen -- ridges and creases on a coarse mesh -- the lookup
    // has to spread out. A single bad authored normal would otherwise snap a
    // whole panel to one bright corner of the room, which is exactly where
    // reflections kept "gathering".
    vec3 dnx = dFdx(n), dny = dFdy(n);
    float variance = 0.25 * (dot(dnx, dnx) + dot(dny, dny));
    float kernelRoughness = sqrt(min(2.0 * variance, 0.18));
    // The reflection direction has to follow the surface rather than the
    // authored normal. On coarse meshes those normals run in steps along a
    // ridge and the direction chases them, bunching a window into one bright
    // band -- the original game shows the same. The geometric normal from
    // screen-space derivatives is flat, but it is honest about the surface.
    vec3 vcross = cross(dFdx(v_viewPos), dFdy(v_viewPos));
    float vlen = length(vcross);
    vec3 geoN = vlen > 1e-20 ? vcross / vlen : n;
    if (dot(geoN, n) < 0.0) geoN = vec3(-geoN.x, -geoN.y, -geoN.z);
    vec3 reflN = n;
    if (u_dynamicReflection.y > 1.5) reflN = normalize(n + geoN);
    else if (u_dynamicReflection.y > 0.5) reflN = geoN;
    // Width is meant to come from the material's roughness. There is no
    // roughness map yet, and deriving it from Phong shininess put typical
    // materials at 0.5-0.7, which washed the whole car into milk. Until
    // stage 2/3 supply a real value the factor stays small so the reflection
    // reads sharp; the panel slider can still widen it on demand.
    // No kernelRoughness here. It comes from dFdx of the shading normal and
    // so is constant inside a triangle. At a large level its share of the
    // radius is a fraction of a mip and invisible, but at the low end the
    // radius is small and that per-triangle constant dominates -- the level
    // jumps between "no blur" and "half a mip" from triangle to triangle,
    // which faceted the paint and broke antialiasing exactly where the slider
    // was near zero.
    float envRadius = u_envFilter.x * roughEnv * 0.12;
    bool envLegacy = u_envFilter.y > 0.5;
    if (envLegacy) occ = 1.0; // SLRR_LEGACY_ENV=1
    vec2 envUv = channelUv(u_vehicleChannels.z, v_texcoord0, v_texcoord1, v_texcoord2);
    if (u_vehicleChannels.w > 0.5) {
        vec3 ray = reflect(normalize(v_viewPos), n);
        // Stock view-space spheremap with camera heading compensation.
        vec3 look = mtxGetRow(u_view, 2).xyz;
        float yaw = atan2(look.x, look.z);
        envUv = vec2(ray.x*0.5-yaw/3.1415927, -ray.y*0.5+0.5);
    }
    vec3 environment = texture2D(s_vehicleEnv, envUv*u_vehicleEnvUv.xy+u_vehicleEnvUv.zw).rgb;
    if (u_dynamicReflection.x > 0.5) {
        vec3 ray = reflect(normalize(v_viewPos), reflN);
        vec3 worldRay = mul(u_invView, vec4(ray, 0.0)).xyz;
        if (u_reflectionProbe.w > 0.5) {
            vec3 position = mul(u_invView, vec4(v_viewPos, 1.0)).xyz;
            if (all(greaterThanEqual(position, u_reflectionBoxMin.xyz)) &&
                all(lessThanEqual(position, u_reflectionBoxMax.xyz))) {
                vec3 direction = sign(worldRay) * max(abs(worldRay), vec3(0.0001, 0.0001, 0.0001));
                vec3 nearHit = (u_reflectionBoxMin.xyz - position) / direction;
                vec3 farHit = (u_reflectionBoxMax.xyz - position) / direction;
                vec3 exitHit = max(nearHit, farHit);
                float distance = min(exitHit.x, min(exitHit.y, exitHit.z));
                // Normalised: the result feeds saturate(worldRay.y) and the
                // tangent basis of the prefilter, both of which assume a unit
                // direction.
                worldRay = normalize(position + worldRay * distance - u_reflectionProbe.xyz);
            }
        }
        vec3 capture = u_sunAmb.rgb + u_sunDiff.rgb * (0.06 + 0.28 * saturate(worldRay.y));
        if (u_dynamicReflection.x < 1.5)
            capture = envPrefilter(worldRay, envRadius);
        // Keep dark parts of the scene visible in the reflection without
        // flattening the brighter shapes captured by the cubemap.
        environment = capture + u_sunAmb.rgb * 0.15;
    }
    float weight = u_vehicleLayers.y;
    if (u_vehicleLayers.z > 0.5) weight *= 1.0-detail.a;
    float fres = pow(1.0-max(dot(n, viewDir), 0.0), 3.0);
    // u_dynamicReflection.w tunes the head-on reflectance floor, so the
    // strength can be matched to a reference render without a rebuild.
    float envBase = clamp(u_dynamicReflection.w, 0.0, 1.0);
    float strength = envLegacy ? (0.55 + 0.55 * fres)
                               : (envBase + (1.0 - envBase) * fresR);
    float reflectanceDbg = clamp(weight * strength, 0.0, 0.72) * occ;
    // SLRR_ENV_DEBUG=1..6 dumps one term of the reflection formula so the
    // artefacts can be attributed to a factor instead of guessed at.
    if (u_dynamicReflection.z > 0.5) {
        vec3 dbg = environment;
        if (u_dynamicReflection.z > 1.5 && u_dynamicReflection.z < 2.5)
            dbg = vec3(occ, occ, occ);
        else if (u_dynamicReflection.z > 2.5 && u_dynamicReflection.z < 3.5)
            dbg = albedo*illum*bake;
        else if (u_dynamicReflection.z > 3.5 && u_dynamicReflection.z < 4.5)
            dbg = n*0.5 + vec3(0.5, 0.5, 0.5);
        else if (u_dynamicReflection.z > 4.5 && u_dynamicReflection.z < 5.5)
            dbg = vec3(reflectanceDbg, reflectanceDbg, reflectanceDbg);
        else if (u_dynamicReflection.z > 5.5 && u_dynamicReflection.z < 6.5)
            dbg = environment * reflectanceDbg;
        // 7: the diffuse ambient alone. If the probe is live this is the
        // captured sky above and the captured street below, not one colour.
        else if (u_dynamicReflection.z > 6.5 && u_dynamicReflection.z < 7.5)
            dbg = ambient * 2.0;
        // 8: the ratio ambient/probe-average. One means the probe was rescaled to
        // the flat colour's level on this pixel; far from one is the bug.
        // 8: the probe's own level against the flat constant. One means the
        // probe can be mixed in without the car changing brightness, and it is
        // how the hand-over was calibrated per map.
        // 9: the probe alone, unmixed and unscaled, so its shape is visible
        // instead of saturating the way ambient*2 does.
        else if (u_dynamicReflection.z > 8.5) {
            vec3 worldNormal = normalize(mul(u_invView, vec4(n, 0.0)).xyz);
            dbg = textureCube(s_sceneIrradiance, worldNormal).rgb;
        }
        else if (u_dynamicReflection.z > 7.5) {
            vec3 worldNormal = normalize(mul(u_invView, vec4(n, 0.0)).xyz);
            vec3 probe = textureCube(s_sceneIrradiance, worldNormal).rgb;
            dbg = probe / max(u_sunAmb.rgb, vec3(1.0/255.0, 1.0/255.0, 1.0/255.0));
        }
        gl_FragColor = vec4(dbg, 1.0);
        return;
    }
    // ColorMixTex. Texture alpha reveals colour 0. A transparent group
    // drops texels at or under alpha ref 4, so a grille's holes stay open.
    if (u_vehicleLayers.w > 3.5) {
        vec3 mixed = mix(u_vehicleDiffuse.rgb, base.rgb, base.a);
        float alpha = base.a*u_vehicleDiffuse.a;
        if (u_lightmap.w > 0.5 && alpha < 4.0/255.0) discard;
        vec3 pane = mixed*illum*bake;
        pane += u_vehicleSpec.rgb*spec;
        if (u_vehicleLayers.w > 4.5) pane += environment*weight;
        gl_FragColor = vec4(pane, alpha);
        return;
    }
    // Stock glass is two passes. The lit tint blends by the material
    // alpha. The spheremap times its strength, plus the specular, is added.
    // u_sunDiff.a marks a scene light (setLight or a directional resource).
    if (u_vehicleLayers.w > 2.5) {
        // The mesh is two-sided, so both windings blend. Stock culls one.
        // Two full alphas of this near-black tint stack into a black pane.
        // 1-sqrt(1-a) twice transmits the same as one authored alpha.
        float alpha = 1.0-sqrt(max(1.0-u_vehicleDiffuse.a, 0.0));
        vec3 lit = u_vehicleDiffuse.rgb*illum;
        // A headlight cover names an empty spheremap and borrows the sky.
        // Cabin glass keeps its strength, plus a graze, so it is not a hole.
        float envK = u_vehicleChannels.w > 1.5 ? 0.4 : (1.0+fres*0.5);
        vec3 shine = environment*weight*envK*0.5 + u_vehicleSpec.rgb*spec*0.5;
        gl_FragColor = vec4(lit*alpha + shine, alpha);
        return;
    } else if (u_vehicleLayers.w > 1.5) {
        // Stock chrome: lit colour + env*strength + specular. The 0.5 is the
        // garage light's specular when no scene light has been read yet.
        vec3 lit = u_vehicleDiffuse.rgb*(u_sunDiff.a > 0.5
            ? illum
            : (vec3(0.347, 0.347, 0.347)+vec3(0.919, 0.958, 1.0)*ndl));
        color = lit + environment*weight;
        vec3 sp = u_sunSpec.a > 0.5 ? u_sunSpec.rgb : vec3(0.5, 0.5, 0.5);
        color += u_vehicleSpec.rgb*pow(ndh, max(u_vehicleSpec.a, 1.0))*sp*step(0.001, ndl);
    } else if (u_vehicleLayers.w > 0.5) {
        // Painted panels keep their color and gain the room on top.
        if (u_dynamicReflection.x > 0.5) {
            // The authored weight stands in for the clearcoat the stock model
            // never had, so it stays the ceiling. What changes is the shape:
            // physical Fresnel instead of a constant 0.55 floor, which is what
            // made the room pool into the dark, recessed parts of the body.
            float reflectance = clamp(weight * strength, 0.0, 0.72);
            color = mix(color, environment, reflectance * occ);
        } else {
            color += environment*weight*(0.9+fres)*occ;
        }
    } else {
        // Half the authored strength, added on the metal color. The full
        // weight paints an exhaust pipe with the room and it reads white.
        color += environment*weight*0.5;
    }
    gl_FragColor = vec4(color, base.a*u_vehicleDiffuse.a);
}

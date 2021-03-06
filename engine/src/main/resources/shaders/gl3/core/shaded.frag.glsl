#ifdef GL_ES
#define LOWP lowp
precision mediump float;
#else
#define LOWP
#endif

#define saturate(x) clamp(x, 0.0, 1.0)
#define PI 3.14159265359

uniform float u_time;

uniform samplerCube u_prefilterMap;
uniform sampler2D u_brdfMap;

#include "pbr.glsl"

uniform sampler2D u_depthTexture;

#ifdef baseColourTextureFlag
uniform sampler2D u_baseColourTexture;
#endif
uniform vec4 u_baseColourModifier;

#ifdef metalRoughnessTextureFlag
uniform sampler2D u_metallicRoughnessTexture;
#endif
uniform float u_metallicModifier;
uniform float u_roughnessModifier;

#ifdef occlusionTextureFlag
uniform sampler2D u_occlusionTexture;
uniform float u_occlusionStrength;

#endif

#ifdef emissiveTextureFlag
uniform sampler2D u_emissiveTexture;
#endif
uniform vec3 u_emissiveModifier;


#ifdef normalTextureFlag
uniform sampler2D u_normalTexture;
uniform float u_normalScale;
#endif

uniform vec3 u_cameraPosition;

uniform vec3 u_lightDir;
uniform vec4 u_lightColour;



uniform samplerCube u_envMap;
uniform samplerCube u_irradianceMap;

#ifdef shadowMapFlag
uniform sampler2D u_shadowMap;
#endif


in vec4 v_worldPosition;
in vec3 v_normal;

#if defined(baseColourTextureFlag) | defined(normalTextureFlag) | defined(emissiveTextureFlag) | defined(metalRoughnessTextureFlag) | defined(occlusionTextureFlag)
in vec2 v_texCoords;
#endif

in vec4 v_FragPosLightSpace;

out highp vec4 fragmentColor;

const vec2 poissonDisk[4] = vec2[](
    vec2(-0.94201624, -0.39906216),
    vec2(0.94558609, -0.76890725),
    vec2(-0.094184101, -0.92938870),
    vec2(0.34495938, 0.29387760)
);

#ifdef shadowMapFlag
float calculateShadow (vec4 fragPosLightSpace, vec3 normal, vec3 lightDir, vec2 uvOffset) {
    vec3 projCoords = fragPosLightSpace.xyz/fragPosLightSpace.w;
    projCoords = projCoords * 0.5 + 0.5;

    float closestDepth = texture(u_shadowMap, projCoords.xy + uvOffset).r;
    float currentdepth = projCoords.z;

    float bias = max(0.0005 * (1.0 - dot(normal, lightDir)), 0.0005);

    if (currentdepth - bias > closestDepth) {
        return 1.0;
    } else {
        return 0.0;
    }

}

float shadowPCF (vec4 fragPosLightSpace, vec3 normal, vec3 lightDir) {
    vec2 texelSize = vec2(1.0 / 2048.0);
    float shadow = 0.0;

    int samples = 4;

    for (int i = 0; i < samples; i++) {
        vec2 uvOffset = poissonDisk[i] * texelSize;
        shadow += calculateShadow(fragPosLightSpace, normal, lightDir, uvOffset);
    }

    return shadow/float(samples);
}
#endif

float SRGB_ALPHA = 0.055;

// Converts a single srgb channel to rgb
float srgb_to_linear(float channel) {
    if (channel <= 0.04045)
    return channel / 12.92;
    else
    return pow((channel + SRGB_ALPHA) / (1.0 + SRGB_ALPHA), 2.4);
}

vec3 srgb_to_rgb(vec3 srgb) {
    return vec3(
    srgb_to_linear(srgb.r),
    srgb_to_linear(srgb.g),
    srgb_to_linear(srgb.b)
    );
}


void main () {

    vec3 N = vec3(0.0);
    vec2 uv = vec2(0.0);

    #if defined(baseColourTextureFlag) | defined(normalTextureFlag) | defined(emissiveTextureFlag) | defined(metalRoughnessTextureFlag) | defined(occlusionTextureFlag)
        uv = v_texCoords;
    #endif

    vec4 albedo = vec4(0.0);
    float metallic = 0.0;
    float roughness = 0.0;
    float ao = 0.0;

    vec3 L = normalize(-u_lightDir);


    #ifdef normalTextureFlag
        vec3 tangentNormal = texture(u_normalTexture, uv).xyz * 2.0 - 1.0;

        vec3 Q1  = dFdx(v_worldPosition);
        vec3 Q2  = dFdy(v_worldPosition);
        vec2 st1 = dFdx(uv);
        vec2 st2 = dFdy(uv);

        N  = normalize(v_normal.xyz);
        vec3 T  = normalize(Q1*st2.t - Q2*st1.t);
        vec3 B  = -normalize(cross(N, T));
        mat3 TBN = mat3(T, B, N);

        N = normalize(TBN * tangentNormal);
    #else
        N = normalize(v_normal.xyz);
    #endif

    vec3 envMap = texture(u_envMap, N).rgb;
    vec3 irradiance = texture(u_irradianceMap, N).rgb;

    #ifdef baseColourTextureFlag
    vec3 linearColour = srgb_to_rgb(texture(u_baseColourTexture, uv).rgb);
//    linearColour = clamp(linearColour, 0.0, 1.0);
    albedo = vec4(linearColour, 1.0) * u_baseColourModifier;
    #else
        albedo = u_baseColourModifier;
    #endif

    #ifdef metalRoughnessTextureFlag
        vec2 metalRoughness = texture(u_metallicRoughnessTexture, uv).bg * vec2(u_metallicModifier, u_roughnessModifier);
        metallic = metalRoughness.x;
        roughness = metalRoughness.y;
    #else
        metallic = u_metallicModifier;
        roughness = u_roughnessModifier;
    #endif

    #ifdef occlusionTextureFlag
        ao = texture(u_occlusionTexture, uv).r;
    #endif


    float shadow = 0.0;

    #ifdef shadowMapFlag
        shadow = shadowPCF(v_FragPosLightSpace, N, L);
    #endif

    shadow = clamp(shadow, 0.0, 1.0);


    vec4 brdf = Bongo_BRDF(
    albedo.rgb, metallic, roughness,
    envMap,
    irradiance,
    N,
    shadow,
    u_lightDir, u_lightColour.rgb * 1.0,
    v_worldPosition.rgb, u_cameraPosition);

    #ifdef occlusionTextureFlag
        brdf.rgb = mix(brdf.rgb, brdf.rgb * ao, u_occlusionStrength);
    #endif

    #ifdef emissiveTextureFlag
        brdf.rgb += srgb_to_rgb(texture(u_emissiveTexture, uv).rgb * u_emissiveModifier);
    #else
        brdf.rgb += u_emissiveModifier;
    #endif


    //hdr tone mapping
//    brdf.rgb = brdf.rgb/(brdf.rgb + vec3(1.0));

    //gamma correction
    float gamma = 2.2;
    brdf.rgb = pow(brdf.rgb, vec3(1.0/gamma));
    fragmentColor = brdf;


}

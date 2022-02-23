#ifdef GL_ES
#define LOWP lowp
precision mediump float;
#else
#define LOWP
#endif


varying float v_depth;
varying vec3 debug;
varying vec3 v_normal;
varying vec3 v_worldPos;
varying mat3 v_TBN;

varying vec2 v_texCoords;

varying vec4 v_FragPosLightSpace;

uniform vec3 u_lightDir;
uniform vec4 u_lightColour;
uniform vec3 u_cameraPosition;
uniform vec2 u_screenSize;
uniform mat4 u_projTrans;
uniform mat4 u_projTransInv;
uniform float u_time;

#ifdef shadowMapFlag
uniform sampler2D u_shadowMap;
#endif

uniform sampler2D u_waveNormal;
uniform sampler2D u_depthTexture;
uniform sampler2D u_foamTexture;

@Control[range(0.0, 30.0)]
uniform float u_foamFalloff;

@Control[range(0.0, 1.0)]
uniform float u_foamScrolling1;

@Control[range(0.0, 1.0)]
uniform float u_foamScrolling2;

const vec2 poissonDisk[4] = vec2[](
vec2(-0.94201624, -0.39906216),
vec2(0.94558609, -0.76890725),
vec2(-0.094184101, -0.92938870),
vec2(0.34495938, 0.29387760)
);

vec3 worldFromDepth (vec2 uv) {

    vec4 clipSpace;
    clipSpace.xy = uv * 2.0 - 1.0;
    clipSpace.z = texture(u_depthTexture, uv).r * 2.0 - 1.0;
    clipSpace.w = 1.0;

    vec4 worldSpace = u_projTransInv * clipSpace;
    return worldSpace.xyz/worldSpace.w;
}




#ifdef shadowMapFlag
float calculateShadow (vec4 fragPosLightSpace, vec3 normal, vec3 lightDir, vec2 uvOffset) {
    vec3 projCoords = fragPosLightSpace.xyz/fragPosLightSpace.w;
    projCoords = projCoords * 0.5 + 0.5;

    float closestdepth = texture(u_shadowMap, projCoords.xy + uvOffset).r;
    float currentdepth = projCoords.z;

    float bias = max(0.0005 * (1.0 - dot(normal, lightDir)), 0.0005);

    if (currentdepth - bias > closestdepth) {
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

void main() {
    vec3 lightDir = -u_lightDir;

    vec3 waterColour = vec3(0.0, 1.0, 1.0);

    float uvScale = 0.55;
    vec2 uv = v_texCoords * uvScale;
    uv += vec2(0.125, -0.05123) * u_time * 0.1235;

    vec3 tangentNormal = texture(u_waveNormal, uv).xyz * 2.0 - 1.0;

    vec3 Q1  = dFdx(v_worldPos);
    vec3 Q2  = dFdy(v_worldPos);
    vec2 st1 = dFdx(uv);
    vec2 st2 = dFdy(uv);

    vec3 N  = normalize(v_normal.xyz);
    vec3 T  = normalize(Q1*st2.t - Q2*st1.t);
    vec3 B  = -normalize(cross(N, T));
    mat3 TBN = mat3(T, B, N);

    N = normalize(TBN * tangentNormal);


    float NdotL = max(dot(N, lightDir), 0.0);


    float specularStrength = 0.0001;
    vec3 viewDir = normalize(u_cameraPosition - v_worldPos);
    vec3 reflectDir = normalize(reflect(-lightDir, N));
    float spec = pow(max(dot(viewDir, reflectDir), 0.0), 400.0);


    vec2 screenSpaceUV = gl_FragCoord.xy/u_screenSize.xy;
    vec3 depthWorldPos = worldFromDepth(screenSpaceUV);

    vec3 surfaceToDepth = depthWorldPos - v_worldPos;
    float depthAtWorldPos = length(surfaceToDepth);
    float foamMaskFallOff = 1.0 - smoothstep(0.0, u_foamFalloff, depthAtWorldPos);
    float foamMask = foamMaskFallOff;

    float foamTex = texture(u_foamTexture, 0.2 * v_texCoords + vec2(u_foamScrolling1, u_foamScrolling2) * sin(u_time * 0.05)).r;
    foamTex = pow(foamTex, 1.0);

    foamMask += pow(foamMaskFallOff, 2.0);
    foamMask += foamTex * foamMask;

    vec3 foamColour = vec3(1.0) * 1.0;

    vec3 result = vec3(foamMask * foamColour);

    result = N;

    float fresnel = 0.0;

    fresnel = pow(1.0 - dot(viewDir, N), 5.0);
    fresnel = mix(0.0, 1.0, min(1.0, fresnel));

    float shadow = 0.0;

    #ifdef shadowMapFlag
        shadow = shadowPCF(v_FragPosLightSpace, N, lightDir);
    #endif

    vec3 light = u_lightColour.rgb * NdotL * (1.0 - shadow);
    light += spec * (1.0 - fresnel);

    vec3 SKY = vec3(0.2, 0.2, 0.2);
    light += SKY * fresnel;

    float distFromCamera = length(v_worldPos - u_cameraPosition);

    float attenuation = max(1.0 - (distFromCamera * 0.02), 0.0);

    float heightDepth = v_worldPos.y - -2.0;

    vec3 WATER_COLOR = vec3(0.4,0.9,0.9);


    light += WATER_COLOR * attenuation * heightDepth * 0.4;

    light += foamColour * foamMask;


    gl_FragColor = vec4(vec3(light), 0.98);

}

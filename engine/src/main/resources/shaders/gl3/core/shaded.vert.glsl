#ifdef normalFlag
in vec3 a_normal;
#endif

#if defined(baseColourTextureFlag) | defined(normalTextureFlag) | defined(emissiveTextureFlag) | defined(metalRoughnessTextureFlag) | defined(occlusionTextureFlag)
in vec2 a_texcoord0;
out vec2 v_texCoords;
#endif

uniform mat4 u_lightMatrix;
uniform mat3 u_normalMatrix;

out vec3 v_normal;
out vec4 v_worldPosition;
out vec4 v_FragPosLightSpace;

#include "vertutils.glsl"
void main () {
    v_worldPosition = Bongo_localToWorld();
    gl_Position = Bongo_worldToClip(v_worldPosition);
    v_FragPosLightSpace = u_lightMatrix * v_worldPosition;

    #ifdef normalFlag
        v_normal = u_normalMatrix * a_normal;
    #else
        v_normal = vec3(0.0, 1.0, 0.0);
    #endif

    #if defined(baseColourTextureFlag) | defined(normalTextureFlag) | defined(emissiveTextureFlag) | defined(metalRoughnessTextureFlag) | defined(occlusionTextureFlag)
        v_texCoords = a_texcoord0;
    #endif
}

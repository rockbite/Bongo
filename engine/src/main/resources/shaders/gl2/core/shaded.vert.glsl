#if defined(baseColourTextureFlag) | defined(normalTextureFlag) | defined(emissiveTextureFlag) | defined(metalRoughnessTextureFlag) | defined(occlusionTextureFlag)
attribute vec2 a_texcoord0;
varying vec2 v_texCoords;
#endif

uniform mat4 u_lightMatrix;

varying vec3 v_normal;
varying vec4 v_worldPosition;
varying vec4 v_FragPosLightSpace;

#include "vertutils.glsl"
void main () {
    v_worldPosition = Bongo_localToWorld();
    gl_Position = Bongo_worldToClip(v_worldPosition);
    v_FragPosLightSpace = u_lightMatrix * v_worldPosition;

    v_normal = Bongo_normalToWorld();

    #if defined(baseColourTextureFlag) | defined(normalTextureFlag) | defined(emissiveTextureFlag) | defined(metalRoughnessTextureFlag) | defined(occlusionTextureFlag)
        v_texCoords = a_texcoord0;
    #endif
}

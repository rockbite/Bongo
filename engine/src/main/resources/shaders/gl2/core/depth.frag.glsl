#ifdef GL_ES
#define LOWP lowp
precision mediump float;
#else
#define LOWP
#endif


#include "depthutils.glsl"

void main () {
    gl_FragColor = Bongo_encodeDepthToRGBA(gl_FragCoord.z);
}

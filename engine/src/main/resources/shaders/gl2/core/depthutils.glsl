#ifdef GL_ES
#define LOWP lowp
#define MED mediump
#define HIGH highp
precision mediump float;
#else
#define MED
#define LOWP
#define HIGH
#endif

vec4 Bongo_encodeDepthToRGBA (float inDepth) {
    HIGH float depth = inDepth;
    const HIGH vec4 bias = vec4(1.0 / 255.0, 1.0 / 255.0, 1.0 / 255.0, 0.0);
    HIGH vec4 color = vec4(depth, fract(depth * 255.0), fract(depth * 65025.0), fract(depth * 16581375.0));
    return color - (color.yzww * bias);
}

float Bongo_decodeDepthFromRGBA (vec4 inDepthRGB) {
    const vec4 bitShifts = vec4(1.0, 1.0 / 255.0, 1.0 / 65025.0, 1.0 / 16581375.0);
    return dot(inDepthRGB, bitShifts);//+(1.0/255.0));
}

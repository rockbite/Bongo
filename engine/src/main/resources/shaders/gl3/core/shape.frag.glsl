#ifdef GL_ES
#define LOWP lowp
precision mediump float;
#else
#define LOWP
#endif

in vec4 v_color;

out vec4 outColour;

void main () {
    outColour = v_color;
}

#ifdef GL_ES
#define LOWP lowp
precision mediump float;
#else
#define LOWP
#endif

out vec4 fragmentColor;

void main () {
    fragmentColor = vec4(1.0, 1.0, 1.0, 1.0);
}

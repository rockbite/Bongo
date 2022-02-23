#ifdef GL_ES
#define LOWP lowp
precision mediump float;
#else
#define LOWP
#endif

uniform sampler2D u_textures[MAX_TEXTURE_UNITS];

in LOWP vec4 v_color;
in vec2 v_texCoords;
in float v_texture_index;

out vec4 colourOut;

vec4 sampleTextureArray (int index, vec2 texCoords) {
    %SAMPLE_TEXTURE_ARRAY_CODE%
}

void main () {
    colourOut = v_color * sampleTextureArray(int(v_texture_index), v_texCoords);
}

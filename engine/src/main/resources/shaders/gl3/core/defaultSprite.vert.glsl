in vec4 a_position;
in vec4 a_color;
in vec2 a_texCoord0;
in float texture_index;
in float custom_info;

uniform mat4 u_projTrans;

out vec4 v_color;
out vec2 v_texCoords;
out float v_texture_index;

void main () {
    v_color = a_color;
    v_texCoords = a_texCoord0;
    v_texture_index = texture_index;

    gl_Position = u_projTrans * a_position;
}

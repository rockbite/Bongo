in vec4 a_position;
in vec4 a_color;

uniform mat4 u_projModelView;

out vec4 v_color;

void main () {
    gl_Position = u_projModelView * a_position;
    v_color = a_color;
    v_color.a *= 255.0 / 254.0;

    gl_PointSize = 1.0;
}

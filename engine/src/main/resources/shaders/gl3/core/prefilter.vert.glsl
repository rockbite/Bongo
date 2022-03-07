in vec4 a_position;

out vec3 v_worldPosition;

uniform mat4 u_projection;
uniform mat4 u_view;

void main() {
    v_worldPosition = a_position.xyz;
    gl_Position = u_projection * u_view * vec4(v_worldPosition, 1.0);
}

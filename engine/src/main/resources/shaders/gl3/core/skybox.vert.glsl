in vec4 a_position;

uniform mat4 u_proj;
uniform mat4 u_view;

out vec3 v_localPos;

void main () {
    v_localPos = a_position.xyz;

    mat4 rotView = mat4(mat3(u_view)); // remove translation from the view matrix
    vec4 clipPos = u_proj * rotView * vec4(v_localPos, 1.0);

    gl_Position = clipPos.xyww;
}

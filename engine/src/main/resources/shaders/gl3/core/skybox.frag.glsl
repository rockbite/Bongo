#ifdef GL_ES
#define LOWP lowp
precision mediump float;
#else
#define LOWP
#endif

in vec3 v_localPos;

out vec4 outColour;

uniform samplerCube u_envMap;

void main() {
    vec3 envColor = texture(u_envMap, v_localPos).rgb;


//    envColor = envColor / (envColor + vec3(1.0));
//    envColor = pow(envColor, vec3(1.0/2.2));

    outColour = vec4(envColor, 1.0);

}

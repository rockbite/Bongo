in vec3 v_worldPosition;

uniform sampler2D u_equirectangularMap;

out vec4 fragColour;


const vec2 invAtan = vec2(0.1591, 0.3183);
vec2 SampleSphericalMap(vec3 v)
{
    vec2 uv = vec2(atan(v.z, v.x), asin(v.y));
    uv *= invAtan;
    uv += 0.5;
    return uv;
}

void main()
{
    vec2 uv = SampleSphericalMap(normalize(v_worldPosition));
    vec3 color = texture(u_equirectangularMap, uv).rgb;

    fragColour = vec4(color, 1.0);
}

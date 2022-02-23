attribute vec3 a_position;


struct WaveInfo {
    vec2 direction;
    float amplitude;
    float steepness;
    float frequency;
    float speed;
};

const int gerstner_waves_length = WAVE_COUNT;
uniform WaveInfo u_waveInfos[WAVE_COUNT];

uniform mat4 u_projTrans;
uniform mat4 u_srt;
uniform float u_time;
uniform vec3 u_cameraPosition;


uniform mat4 u_lightMatrix;
uniform vec3 u_lightDir;
uniform vec4 u_lightColour;

varying vec3 v_normal;
varying vec3 v_lightTerm;
varying vec3 v_specLightTerm;
varying float v_depth;

varying vec3 v_worldPos;
varying mat3 v_TBN;

varying vec4 v_FragPosLightSpace;

varying vec2 v_texCoords;

varying vec3 debug;


vec3 gerstner_wave_normal(vec3 position, float time) {
    vec3 wave_normal = vec3(0.0, 1.0, 0.0);
    for (int i = 0; i < gerstner_waves_length; ++i) {
        float proj = dot(position.xz, u_waveInfos[i].direction),
        phase = time * u_waveInfos[i].speed,
        psi = proj * u_waveInfos[i].frequency + phase,
        Af = u_waveInfos[i].amplitude *
        u_waveInfos[i].frequency,
        alpha = Af * sin(psi);

        wave_normal.y -= u_waveInfos[i].steepness * alpha;

        float x = u_waveInfos[i].direction.x,
        y = u_waveInfos[i].direction.y,
        omega = Af * cos(psi);

        wave_normal.x -= x * omega;
        wave_normal.z -= y * omega;
    } return wave_normal;
}

vec3 gerstner_wave_position(vec2 position, float time) {
    vec3 wave_position = vec3(position.x, 0, position.y);
    for (int i = 0; i < gerstner_waves_length; ++i) {
        float proj = dot(position, u_waveInfos[i].direction),
        phase = time * u_waveInfos[i].speed,
        theta = proj * u_waveInfos[i].frequency + phase,
        height = u_waveInfos[i].amplitude * sin(theta);

        wave_position.y += height;

        float maximum_width = u_waveInfos[i].steepness *
        u_waveInfos[i].amplitude,
        width = maximum_width * cos(theta),
        x = u_waveInfos[i].direction.x,
        y = u_waveInfos[i].direction.y;

        wave_position.x += x * width;
        wave_position.z += y * width;
    } return wave_position;
}

vec3 gerstner_wave(vec2 position, float time) {
    vec3 wave_position = gerstner_wave_position(position, time);
    return wave_position; // Accumulated Gerstner Wave.
}

vec3 gerstner_normal(vec3 wave_position, float time) {
    return gerstner_wave_normal(wave_position, time);
}

void main () {
    vec4 worldPos = u_srt * vec4(a_position, 1.0);

    vec3 wavePos = gerstner_wave(worldPos.xz, u_time);
    worldPos.xyz = wavePos;

    v_normal = gerstner_normal(wavePos, u_time);

    v_texCoords = vec2(worldPos.xz);

    v_FragPosLightSpace = u_lightMatrix * worldPos;

    v_worldPos = worldPos.xyz;

    gl_Position = u_projTrans * worldPos;
}

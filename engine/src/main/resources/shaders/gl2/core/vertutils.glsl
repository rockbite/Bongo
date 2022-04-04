#ifdef positionFlag
attribute vec4 a_position;
#endif

#ifdef normalFlag
attribute vec3 a_normal;
#endif


#ifdef skinningFlag
uniform mat4 u_jointMatrix[12];
attribute vec4 a_weights_0;
attribute vec4 a_joints_0;
#endif

uniform mat4 u_projTrans;
uniform mat4 u_srt;

vec4 Bongo_worldToClip (vec4 worldSpace) {
    return u_projTrans * worldSpace;
}

vec4 Bongo_localToWorld () {
    #if defined(positionFlag)

        #if defined(skinningFlag)
            mat4 skinMatrix =
                a_weights_0.x * u_jointMatrix[int(a_joints_0.x)] +
                a_weights_0.y * u_jointMatrix[int(a_joints_0.y)] +
                a_weights_0.z * u_jointMatrix[int(a_joints_0.z)] +
                a_weights_0.w * u_jointMatrix[int(a_joints_0.w)];
            return u_srt * skinMatrix * vec4(a_position.xyz, 1.0);
        #endif

        return u_srt * vec4(a_position.xyz, 1.0);

    #else
        return vec4(0.5);
    #endif
}


vec4 Bongo_localToClip () {
    return Bongo_worldToClip(Bongo_localToWorld());
}


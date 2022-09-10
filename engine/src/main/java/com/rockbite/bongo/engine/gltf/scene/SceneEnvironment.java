package com.rockbite.bongo.engine.gltf.scene;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Cubemap;
import com.badlogic.gdx.graphics.GLTexture;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.rockbite.bongo.engine.annotations.ComponentExpose;
import com.rockbite.bongo.engine.components.render.PointLight;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import static com.rockbite.bongo.engine.annotations.ComponentExposeFlavour.*;

@Data
public class SceneEnvironment {

	@Getter@Setter
	public static class EnvironmentMap {
		private GLTexture brdfMap;
		private Cubemap radianceMap;
		private Cubemap irradianceMap;
		private Cubemap specularMap;
	}

	@ComponentExpose(flavour = VEC_3)
	private float[] directionalLightDirRaw = new float[]{0f, -1f, -0.01f};
	private Vector3 directionalLightDir = new Vector3(-0.0006f, -1f, 0.001f).nor();

	@ComponentExpose(flavour = COLOUR_4_VEC)
	private float[] directionLightColorRaw = new float[]{46/255f, 46/255f, 46/255f, 1f};
	private Color directionLightColor = new Color(46/255f, 46/255f, 46/255f, 1f);

	@ComponentExpose(flavour = FLOAT)
	private float[] directionalStrength = new float[]{1f};


	@ComponentExpose(flavour = FLOAT)
	private float[] ambientStrength = new float[]{1f};

	int maxPointLights = 5;
	private Array<PointLight> pointLights = new Array<>();

	@ComponentExpose(flavour = COLOUR_4_VEC)
	private float[] ambientEnvironmentRaw = new float[]{46/255f, 46/255f, 46/255f, 1f};
	private Color ambientEnvironmentColor = new Color(46/255f, 46/255f, 46/255f, 1f);


	private EnvironmentMap environmentMap;

	private float time;


	//Shadow map
	private Vector3 tempVec3 = new Vector3();
	private Matrix4 scalingMatrix = new Matrix4();
	private Vector3 tempZeroVector3 = new Vector3();
	private Vector3 tempUpVector = new Vector3();
	private Vector3 tempBaseLookAt = new Vector3();
	private Matrix4 tempLookAt = new Matrix4();
	private Matrix4 tempLookAtinv = new Matrix4();
	private Vector3 frustumCenter = new Vector3();
	private Matrix4 lightView = new Matrix4();
	private Matrix4 lightProjection = new Matrix4();
	public void calculateDirectionLightSpaceMatrix (Camera camera, Matrix4 lightSpaceMatrixOut, int shadowMapSize) {

		float len = tempVec3.set(camera.frustum.planePoints[0]).sub(camera.frustum.planePoints[6]).len();
		float radius = len/2f;

		float texelsPerUnit = shadowMapSize/(radius * 2f);

		scalingMatrix.setToScaling(texelsPerUnit, texelsPerUnit, texelsPerUnit);
		tempZeroVector3.setZero();
		tempUpVector.set(0, 1, 0);

		tempBaseLookAt.set(-directionalLightDir.x, -directionalLightDir.y, -directionalLightDir.z);

		tempLookAt.idt();
		tempLookAtinv.idt();

		tempLookAt.setToLookAt(tempZeroVector3, tempBaseLookAt, tempUpVector);
		tempLookAt.mul(scalingMatrix);
		tempLookAtinv.set(tempLookAt).inv();

		getFrustumCenter(camera, frustumCenter);
		frustumCenter.mul(tempLookAt);
		frustumCenter.x = MathUtils.floor(frustumCenter.x);
		frustumCenter.y = MathUtils.floor(frustumCenter.y);
		frustumCenter.z = MathUtils.floor(frustumCenter.z);
		frustumCenter.mul(tempLookAtinv);

		Vector3 eyeVec = tempVec3.set(directionalLightDir).scl(radius * 2f).scl(-1f).add(frustumCenter);

		lightView.setToLookAt(eyeVec, frustumCenter, tempUpVector);
		lightProjection.setToOrtho(-radius, radius, -radius, radius, 0.01f, radius * 10);
		lightSpaceMatrixOut.set(lightProjection).mul(lightView);
	}

	private Vector3 getFrustumCenter (Camera camera, Vector3 vector3) {
		vector3.setZero();
		for (Vector3 planePoint : camera.frustum.planePoints) {
			vector3.add(planePoint);
		}
		vector3.scl(1/8f);
		return vector3;
	}

	public void packFromRaw () {
		for (int i = 0; i < 3; i++) {
			if (MathUtils.isZero(directionalLightDirRaw[i])) {
				directionalLightDirRaw[i] = 0.01f;
			}
		}

		directionalLightDir.set(directionalLightDirRaw).nor();
		directionalLightDirRaw[0] = directionalLightDir.x;
		directionalLightDirRaw[1] = directionalLightDir.y;
		directionalLightDirRaw[2] = directionalLightDir.z;

		directionLightColor.r = directionLightColorRaw[0]  * directionalStrength[0];
		directionLightColor.g = directionLightColorRaw[1]  * directionalStrength[0];
		directionLightColor.b = directionLightColorRaw[2]  * directionalStrength[0];
		directionLightColor.a = directionLightColorRaw[3]  * directionalStrength[0];

		ambientEnvironmentColor.r = ambientEnvironmentRaw[0] * ambientStrength[0];
		ambientEnvironmentColor.g = ambientEnvironmentRaw[1] * ambientStrength[0];
		ambientEnvironmentColor.b = ambientEnvironmentRaw[2] * ambientStrength[0];
		ambientEnvironmentColor.a = ambientEnvironmentRaw[3] * ambientStrength[0];
	}
}

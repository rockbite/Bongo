package com.rockbite.bongo.engine.gltf.scene;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Array;
import com.rockbite.bongo.engine.gltf.GLTFDataModel;
import lombok.Getter;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class SceneSkin {

	@Getter
	private final int[] joints;
	@Getter
	private Array<Matrix4> ibms;

	public SceneSkin (GLTFDataModel.SkinData skinData, GLTFDataModel dataModel, SceneResourceContext resourceContext) {

		final int inverseBindMatrices = skinData.getInverseBindMatrices();
		joints = skinData.getJoints();

		final GLTFDataModel.AccessorData accessor = dataModel.getAccessors()[inverseBindMatrices];
		if (accessor.getCount() != joints.length) {
			return;
		}
		final GLTFDataModel.ComponentType componentType = accessor.getComponentType();
		final SceneMeshVertexInfo.AccessorType type = accessor.getType();
		final int bufferViewIndex = accessor.getBufferView();

		final GLTFDataModel.BufferViewData bufferView = dataModel.getBufferViews()[bufferViewIndex];

		final ByteBuffer buffer = resourceContext.getBuffer(bufferView.getBuffer());
		buffer.position(accessor.getByteOffset() + bufferView.getByteOffset());
		final FloatBuffer floatBuffer = buffer.asFloatBuffer();

		ibms = new Array<Matrix4>();

		int bonesCount = joints.length;

		for (int i = 0; i < bonesCount; i++) {
			float[] matrixData = new float[16];
			floatBuffer.get(matrixData);
			ibms.add(new Matrix4(matrixData));
		}

	}
}

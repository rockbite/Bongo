package com.rockbite.bongo.engine.meshutils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.rockbite.bongo.engine.gltf.scene.SceneMaterial;
import com.rockbite.bongo.engine.gltf.scene.SceneMesh;
import com.rockbite.bongo.engine.gltf.scene.SceneMeshPrimtive;
import com.rockbite.bongo.engine.gltf.scene.SceneMeshVertexInfo;
import com.rockbite.bongo.engine.gltf.scene.SceneModel;
import com.rockbite.bongo.engine.gltf.scene.SceneNode;
import com.rockbite.bongo.engine.gltf.scene.shader.PBRColourAttribute;

public class CubeUtils {


	public static Mesh createBoxMesh (float width, float height, float depth) {
		float hWidth = width/2f;
		float hHeight = height/2f;
		float hDepth = depth/2f;

		float[] vertexBuffer = new float[] {


			-hWidth, -hHeight, -hDepth,  0.0f,  0.0f, -1.0f,
			hWidth,  hHeight, -hDepth,  0.0f,  0.0f, -1.0f,
			hWidth, -hHeight, -hDepth,  0.0f,  0.0f, -1.0f,

			hWidth,  hHeight, -hDepth,  0.0f,  0.0f, -1.0f,
			-hWidth, -hHeight, -hDepth,  0.0f,  0.0f, -1.0f,
			-hWidth,  hHeight, -hDepth,  0.0f,  0.0f, -1.0f,

			-hWidth, -hHeight,  hDepth,  0.0f,  0.0f, 1.0f,
			hWidth, -hHeight,  hDepth,  0.0f,  0.0f, 1.0f,
			hWidth,  hHeight,  hDepth,  0.0f,  0.0f, 1.0f,

			hWidth,  hHeight,  hDepth,  0.0f,  0.0f, 1.0f,
			-hWidth,  hHeight,  hDepth,  0.0f,  0.0f, 1.0f,
			-hWidth, -hHeight,  hDepth,  0.0f,  0.0f, 1.0f,

			-hWidth,  hHeight,  hDepth, -1.0f,  0.0f,  0.0f,
			-hWidth,  hHeight, -hDepth, -1.0f,  0.0f,  0.0f,
			-hWidth, -hHeight, -hDepth, -1.0f,  0.0f,  0.0f,

			-hWidth, -hHeight, -hDepth, -1.0f,  0.0f,  0.0f,
			-hWidth, -hHeight,  hDepth, -1.0f,  0.0f,  0.0f,
			-hWidth,  hHeight,  hDepth, -1.0f,  0.0f,  0.0f,

			hWidth,  hHeight, -hDepth,  1.0f,  0.0f,  0.0f,
			hWidth,  hHeight,  hDepth,  1.0f,  0.0f,  0.0f,
			hWidth, -hHeight, -hDepth,  1.0f,  0.0f,  0.0f,

			hWidth, -hHeight, -hDepth,  1.0f,  0.0f,  0.0f,
			hWidth,  hHeight,  hDepth,  1.0f,  0.0f,  0.0f,
			hWidth, -hHeight,  hDepth,  1.0f,  0.0f,  0.0f,

			-hWidth, -hHeight, -hDepth,  0.0f, -1.0f,  0.0f,
			hWidth, -hHeight, -hDepth,  0.0f, -1.0f,  0.0f,
			hWidth, -hHeight,  hDepth,  0.0f, -1.0f,  0.0f,

			hWidth, -hHeight,  hDepth,  0.0f, -1.0f,  0.0f,
			-hWidth, -hHeight,  hDepth,  0.0f, -1.0f,  0.0f,
			-hWidth, -hHeight, -hDepth,  0.0f, -1.0f,  0.0f,

			hWidth,  hHeight,  hDepth,  0.0f,  1.0f,  0.0f,
			hWidth,  hHeight, -hDepth,  0.0f,  1.0f,  0.0f,
			-hWidth,  hHeight, -hDepth,  0.0f,  1.0f,  0.0f,

			-hWidth,  hHeight, -hDepth,  0.0f,  1.0f,  0.0f,
			-hWidth,  hHeight,  hDepth,  0.0f,  1.0f,  0.0f,
			hWidth,  hHeight,  hDepth,  0.0f,  1.0f,  0.0f,
		};

		int vertices = vertexBuffer.length/6;
		short[] indices = new short[vertices];
		for (short i = 0; i < indices.length; i++) {
			indices[i] = i;
		}

		Mesh.VertexDataType defaultVertexDataType = Mesh.VertexDataType.VertexArray;

		Mesh.VertexDataType vertexDataType = (Gdx.gl30 != null) ? Mesh.VertexDataType.VertexBufferObjectWithVAO : defaultVertexDataType;

		Mesh boxMesh = new Mesh(vertexDataType, true, vertexBuffer.length/6,  indices.length,
			new VertexAttributes(
				VertexAttribute.Position(),
				VertexAttribute.Normal()
			));
		boxMesh.setVertices(vertexBuffer);
		boxMesh.setIndices(indices);
		return boxMesh;
	}

	public static SceneModel createBox (String name, float width, float height, float depth, SceneMaterial material) {
		SceneModel sceneModel = new SceneModel(name + "-model");

		SceneNode sceneNode = new SceneNode(name + "-node");

		final Mesh boxMesh = createBoxMesh(width, height, depth);
		SceneMeshVertexInfo vertexInfo = new SceneMeshVertexInfo(boxMesh.getVertexAttributes());

		SceneMeshPrimtive sceneMeshPrimtive = new SceneMeshPrimtive(name + "-primitive", boxMesh, vertexInfo);
		SceneMesh sceneMesh = new SceneMesh(name + "-mesh", sceneMeshPrimtive);
		sceneNode.setSceneMesh(sceneMesh);

		sceneMesh.setAllPrimitivesMaterial(material);

		sceneModel.nodes.add(sceneNode);

		return sceneModel;
	}

	public static SceneModel createBox (String name, float width, float height, float depth) {
		SceneMaterial sceneMaterial = SceneMaterial.Empty(name + "-material");
		sceneMaterial.getAttributes().set(PBRColourAttribute.createBaseColourModifier(Color.WHITE));
		return createBox(name, width, height, depth, sceneMaterial);
	}
}

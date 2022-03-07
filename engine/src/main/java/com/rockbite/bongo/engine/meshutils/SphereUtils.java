package com.rockbite.bongo.engine.meshutils;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.ShortArray;
import com.rockbite.bongo.engine.gltf.scene.SceneMaterial;
import com.rockbite.bongo.engine.gltf.scene.SceneMesh;
import com.rockbite.bongo.engine.gltf.scene.SceneMeshPrimtive;
import com.rockbite.bongo.engine.gltf.scene.SceneMeshVertexInfo;
import com.rockbite.bongo.engine.gltf.scene.SceneModel;
import com.rockbite.bongo.engine.gltf.scene.SceneNode;
import com.rockbite.bongo.engine.gltf.scene.shader.PBRColourAttribute;
import com.rockbite.bongo.engine.gltf.scene.shader.PBRFloatAttribute;

public class SphereUtils {


	private static Mesh createSphereMesh (float size) {
		int xSegments = 64;
		int ySegments = 64;
		Array<Vector3> positions = new Array<>();
		Array<Vector3> normals = new Array<>();
		Array<Vector2> uvs = new Array<>();
		ShortArray indexShortArray = new ShortArray();
		for (int i = 0; i <= xSegments; i++) {
			for (int j = 0; j <= ySegments; j++) {
				float xSegment = (float)i / (float)xSegments;
				float ySegment = (float)j / (float)ySegments;

				float xPos = MathUtils.cos(xSegment * 2.0f * MathUtils.PI) * MathUtils.sin(ySegment * MathUtils.PI);
				float yPos = MathUtils.cos(ySegment * MathUtils.PI);
				float zPos = MathUtils.sin(xSegment * 2.0f * MathUtils.PI) * MathUtils.sin(ySegment * MathUtils.PI);

				xPos *= size;
				yPos *= size;
				zPos *= size;

				positions.add(new Vector3(xPos, yPos, zPos));
				uvs.add(new Vector2(xSegment, ySegment));
				normals.add(new Vector3(xPos, yPos, zPos));
			}
		}

		boolean oddRow = false;
		for ( int y = 0; y < ySegments; ++y)
		{
			if (!oddRow) // even rows: y == 0, y == 2; and so on
			{
				for (int x = 0; x <= xSegments; ++x)
				{
					indexShortArray.add(y * (xSegments + 1) + x);
					indexShortArray.add((y + 1) * (xSegments + 1) + x);
				}
			}
			else
			{
				for (int x = xSegments; x >= 0; --x)
				{
					indexShortArray.add((y + 1) * (xSegments + 1) + x);
					indexShortArray.add(y * (xSegments + 1) + x);
				}
			}
			oddRow = !oddRow;
		}

		float[] vertexBuffer = new float[positions.size * 8];
		int idx = 0;
		for (int i = 0; i < positions.size; i++) {
			vertexBuffer[idx++] = positions.get(i).x;
			vertexBuffer[idx++] = positions.get(i).y;
			vertexBuffer[idx++] = positions.get(i).z;

			vertexBuffer[idx++] = normals.get(i).x;
			vertexBuffer[idx++] = normals.get(i).y;
			vertexBuffer[idx++] = normals.get(i).z;

			vertexBuffer[idx++] = uvs.get(i).x;
			vertexBuffer[idx++] = uvs.get(i).y;

		}
		short[] indices = indexShortArray.toArray();

		Mesh boxMesh = new Mesh(true, vertexBuffer.length, indices.length,
			new VertexAttributes(
				VertexAttribute.Position(),
				VertexAttribute.Normal(),
				VertexAttribute.TexCoords(0)
			));
		boxMesh.setVertices(vertexBuffer);
		boxMesh.setIndices(indices);
		return boxMesh;
	}

	public static SceneModel createSphere (String name, float size, SceneMaterial material) {
		SceneModel sceneModel = new SceneModel(name + "-model");

		SceneNode sceneNode = new SceneNode(name + "-node");

		final Mesh boxMesh = createSphereMesh(size);
		SceneMeshVertexInfo vertexInfo = new SceneMeshVertexInfo(boxMesh.getVertexAttributes());

		SceneMeshPrimtive sceneMeshPrimtive = new SceneMeshPrimtive(name + "-primitive", boxMesh, vertexInfo);
		sceneMeshPrimtive.renderMode = GL20.GL_TRIANGLE_STRIP;
		SceneMesh sceneMesh = new SceneMesh(name + "-mesh", sceneMeshPrimtive);
		sceneNode.setSceneMesh(sceneMesh);

		sceneMesh.setAllPrimitivesMaterial(material);

		sceneModel.nodes.add(sceneNode);

		return sceneModel;
	}

	public static SceneModel createSphere (String name, float size) {
		SceneMaterial sceneMaterial = SceneMaterial.Empty(name + "-material");
		sceneMaterial.getAttributes().set(PBRColourAttribute.createBaseColourModifier(Color.WHITE));
		sceneMaterial.getAttributes().set(PBRFloatAttribute.createRoughness(1));
		sceneMaterial.getAttributes().set(PBRFloatAttribute.createMetallic(0));
		return createSphere(name, size, sceneMaterial);
	}
}

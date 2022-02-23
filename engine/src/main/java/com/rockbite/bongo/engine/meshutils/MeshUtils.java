package com.rockbite.bongo.engine.meshutils;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;

public class MeshUtils {

	private static float OCEAN_HEIGHT = 0f;

	public static float[] buildFlatMesh (int width, int depth, float totalWidth, float totalDepth) {

		int totalTriangles = width * depth * 2;
		int totalVerts = totalTriangles * 3;


		float[] verts = new float[totalVerts * 3];

		int idx = 0;

		for (int i = 0; i < width; i++) {
			for (int j = 0; j < depth; j++) {

				float x = (i/(float)width) * totalWidth;
				float z = (j/(float)depth) * totalDepth;

				float x1 = ((i + 1)/(float)width) * totalWidth;
				float z1 = ((j + 1)/(float)depth) * totalDepth;

				//2 triangles

				verts[idx++] = x1;
				verts[idx++] = 0;
				verts[idx++] = z1;

				verts[idx++] = x1;
				verts[idx++] = 0;
				verts[idx++] = z;

				verts[idx++] = x;
				verts[idx++] = 0;
				verts[idx++] = z;


				verts[idx++] = x;
				verts[idx++] = 0;
				verts[idx++] = z;

				verts[idx++] = x;
				verts[idx++] = 0;
				verts[idx++] = z1;

				verts[idx++] = x1;
				verts[idx++] = 0;
				verts[idx++] = z1;





			}

		}
		return verts;
	}

	public static Mesh createFlatMesh (int width, int depth, float totalWidth, float totalDepth) {
		float[] oceanVerts = buildFlatMesh(width, depth, totalWidth, totalDepth);

		Mesh oceanMesh = new Mesh(true, width * depth * 6, 0,
			new VertexAttributes(
				VertexAttribute.Position()
			));
		oceanMesh.setVertices(oceanVerts);

		return oceanMesh;
	}

	public static float[] buildWaterMesh (int width, int depth) {
		float[] verts = new float[width * depth * 8 * 6]; /*  additional term for edges + (maxX/terrainGridSize * stride * 6) * 4 */;
		int idx = 0;
		for (int x = 0; x < width - 1; x++) {
			for (int z = 0; z < depth - 1; z++) {
				verts[idx++] = x;
				verts[idx++] = 0;
				verts[idx++] = z;
				verts[idx++] = OCEAN_HEIGHT;
				verts[idx++] = x;
				verts[idx++] = z + 1;
				verts[idx++] = x + 1;
				verts[idx++] = z + 1;

				verts[idx++] = x;
				verts[idx++] = 0;
				verts[idx++] = z + 1;
				verts[idx++] = OCEAN_HEIGHT;
				verts[idx++] = x + 1;
				verts[idx++] = z + 1;
				verts[idx++] = x;
				verts[idx++] = z;

				verts[idx++] = x + 1;
				verts[idx++] = 0;
				verts[idx++] = z + 1;
				verts[idx++] = OCEAN_HEIGHT;
				verts[idx++] = x;
				verts[idx++] = z;
				verts[idx++] = x;
				verts[idx++] = z + 1;



				verts[idx++] = x;
				verts[idx++] = 0;
				verts[idx++] = z;
				verts[idx++] = OCEAN_HEIGHT;
				verts[idx++] = x + 1;
				verts[idx++] = z + 1;
				verts[idx++] = x + 1;
				verts[idx++] = z;


				verts[idx++] = x + 1;
				verts[idx++] = 0;
				verts[idx++] = z + 1;
				verts[idx++] = OCEAN_HEIGHT;
				verts[idx++] = x + 1;
				verts[idx++] = z;
				verts[idx++] = x;
				verts[idx++] = z;


				verts[idx++] = x + 1;
				verts[idx++] = 0;
				verts[idx++] = z;
				verts[idx++] = OCEAN_HEIGHT;
				verts[idx++] = x;
				verts[idx++] = z;
				verts[idx++] = x + 1;
				verts[idx++] = z + 1;
			}
		}
		return verts;
	}



	public static Mesh createOceanMesh (int width, int height) {
		float[] oceanVerts = buildWaterMesh(width, height);

		Mesh oceanMesh = new Mesh(true, width * height * 6, 0,
			new VertexAttributes(
				VertexAttribute.Position(),
				new VertexAttribute(VertexAttributes.Usage.Position, 1, "a_depth"),
				new VertexAttribute(VertexAttributes.Usage.Position, 2, "a_triOne"),
				new VertexAttribute(VertexAttributes.Usage.Position, 2, "a_triTwo")));
		oceanMesh.setVertices(oceanVerts);

		return oceanMesh;
	}
}

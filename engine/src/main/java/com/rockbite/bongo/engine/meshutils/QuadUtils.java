package com.rockbite.bongo.engine.meshutils;


import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

public class QuadUtils {

	private static float[] verts;

	static {
		verts = new float[20];
		int i = 0;

		verts[i++] = -1; // x1
		verts[i++] = -1; // y1
		verts[i++] = 0;
		verts[i++] = 0f; // u1
		verts[i++] = 0f; // v1

		verts[i++] = 1f; // x2
		verts[i++] = -1; // y2
		verts[i++] = 0;
		verts[i++] = 1f; // u2
		verts[i++] = 0f; // v2

		verts[i++] = 1f; // x3
		verts[i++] = 1f; // y2
		verts[i++] = 0;
		verts[i++] = 1f; // u3
		verts[i++] = 1f; // v3

		verts[i++] = -1; // x4
		verts[i++] = 1f; // y4
		verts[i++] = 0;
		verts[i++] = 0f; // u4
		verts[i++] = 1f; // v4
	}

	private static final int X0 = 0;
	private static final int Y0 = 1;
	private static final int X1 = 5;
	private static final int Y1 = 6;
	private static final int X2 = 10;
	private static final int Y2 = 11;
	private static final int X3 = 15;
	private static final int Y3 = 16;

	public static Mesh createFullScreenQuad () {

		Mesh mesh = new Mesh(true, 4, 0,  // static mesh with 4 vertices and no indices
			new VertexAttribute(VertexAttributes.Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE),
			new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE + "0"));

		mesh.setVertices(verts);
		return mesh;
	}

	public static void setQuad (Mesh mesh, float positionX, float positionY, float width, float height) {
		verts[X0] = positionX;
		verts[Y0] = positionY;

		verts[X1] = positionX + width;
		verts[Y1] = positionY;

		verts[X2] = positionX + width;
		verts[Y2] = positionY + height;

		verts[X3] = positionX;
		verts[Y3] = positionY + height;

		mesh.setVertices(verts);
	}
}

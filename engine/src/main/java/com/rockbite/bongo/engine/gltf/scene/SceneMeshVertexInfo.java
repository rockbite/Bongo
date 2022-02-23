package com.rockbite.bongo.engine.gltf.scene;

import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.utils.Array;
import lombok.Getter;

public class SceneMeshVertexInfo {

	public Array<VertexInfo> vertexInfoArray = new Array<>();

	@Getter
	private VertexAttributes vertexAttributes;

	public int getTotalNumComponentsPerVertex () {
		int numComponents = 0;
		for (VertexInfo vertexInfo : vertexInfoArray) {
			numComponents += vertexInfo.getNumComponents();
		}
		return numComponents;
	}

	public static SceneMeshVertexInfo Empty () {
		return new SceneMeshVertexInfo();
	}

	private SceneMeshVertexInfo () {

	}

	public SceneMeshVertexInfo (VertexAttributes vertexAttributes) {
		this.vertexAttributes = vertexAttributes;
	}

	public VertexAttributes packToVertexAttributes () {
		Array<VertexAttribute> vertexAttributes = new Array<>(VertexAttribute.class);
		for (VertexInfo vertexInfo : vertexInfoArray) {
			vertexAttributes.add(new VertexAttribute(vertexInfo.getVertexType().usage, vertexInfo.getNumComponents(), vertexInfo.vertexType.shaderAttributeName));
		}
		this.vertexAttributes = new VertexAttributes(vertexAttributes.toArray());
		return this.vertexAttributes;
	}

	@Getter
	public static class VertexInfo {

		private VertexType vertexType;
		private int numComponents;

		public VertexInfo (String vertexTypeName, String accessorType) {
			vertexType = VertexType.valueOf(vertexTypeName);
			this.numComponents = AccessorType.valueOf(accessorType).numComponents;
		}
	}

	enum AccessorType {
		SCALAR(1),
		VEC2(2),
		VEC3(3),
		VEC4(4),
		MAT2(4),
		MAT3(9),
		MAT4(16);

		int numComponents;

		AccessorType (int numComponents) {
			this.numComponents = numComponents;
		}
	}

	enum VertexType {
		POSITION(VertexAttributes.Usage.Position, "a_position"),
		NORMAL(VertexAttributes.Usage.Normal, "a_normal"),
		TANGENT(VertexAttributes.Usage.Tangent, "a_tangent"),
		TEXCOORD_0(VertexAttributes.Usage.TextureCoordinates, "a_texcoord0"),
		TEXCOORD_1(VertexAttributes.Usage.TextureCoordinates, "a_texcoord1"),
		TEXCOORD_2(VertexAttributes.Usage.TextureCoordinates, "a_texcoord2"),
		COLOR_0(VertexAttributes.Usage.ColorPacked, "a_color_0"),
		JOINTS_0(VertexAttributes.Usage.Generic, "a_joints_0"),
		WEIGHTS_0(VertexAttributes.Usage.BoneWeight, "a_weights_0");

		private int usage;
		private String shaderAttributeName;

		VertexType (int usage, String shaderAttributeName) {
			this.usage = usage;
			this.shaderAttributeName = shaderAttributeName;
		}
	}




}

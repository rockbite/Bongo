package com.rockbite.bongo.engine.gltf;

import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Base64Coder;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.OrderedMap;
import lombok.Data;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

@Data
public class GLTFDataModel {

	@Data
	public static class SceneData {
		String name;
		int[] nodes;
	}

	@Data
	public static class NodeData {
		String name;
		int[] children;

		float[] rotation = new float[]{0,0,0,0};
		float[] scale = new float[]{1, 1, 1};
		float[] translation = new float[]{0, 0, 0};

		float[] matrix;

		int mesh = -1;
		int camera = -1;

		int skin = -1;


	}

	@Data
	public static class TextureData {
		int sampler = -1;
		int source = -1;
	}

	@Data
	public static class ImageData {
		String uri;


		int bufferView = -1;
		String mimeType;
		String name;
	}

	@Data
	public static class SamplerData {
		int magFilter = -1;
		int minFilter = -1;
		int wrapS = -1;
		int wrapT = -1;
	}

	@Data
	public static class BaseColorTextureData {
		int index = -1;
		int texCoord = 0;
	}

	@Data
	public static class MetallicRoughnessTexture {
		int index = -1;
		int texCoord = 0;
	}

	@Data
	public static class NormalTexture {
		float scale = 1;
		int index = -1;
		int texCoord = 0;
	}

	@Data
	public static class OcclusionTexture {
		float strength = 1;
		int index = -1;
		int texCoord = 0;
	}

	@Data
	public static class EmissiveTexture {
		int index = -1;
		int texCoord = 0;
	}




	@Data
	public static class PBRMetallicRoughnessData {
		BaseColorTextureData baseColorTexture;
		float[] baseColorFactor = new float[]{1f, 1f, 1f, 1f};

		MetallicRoughnessTexture metallicRoughnessTexture;

		float metallicFactor = 1f;
		float roughnessFactor = 1f;

	}

	@Data
	public static class MaterialData {
		private String name;
		private boolean doubleSided;

		private PBRMetallicRoughnessData pbrMetallicRoughness;

		private NormalTexture normalTexture;
		private OcclusionTexture occlusionTexture;
		private EmissiveTexture emissiveTexture;
		private float[] emissiveFactor = new float[]{0f, 0f, 0f};

	}


	@Data
	public static class PrimitivesData {
		private OrderedMap<String, Integer> attributes;
		private int indices = -1;
		private int material = 0;
	}


	@Data
	public static class MeshData {

		private String name;
		private PrimitivesData[] primitives;

	}

	@Data
	public static class AccessorData {
		private int bufferView;
		private int byteOffset;
		private int componentType;
		private int count;
		private float[] max;
		private float[] min;
		private String type;
	}

	@Data
	public static class BufferViewData {
		private int buffer;
		private int byteLength;
		private int byteOffset;
		private int byteStride = -1;
	}

	@Data
	public static class BufferData implements Json.Serializable {

		private int byteLength;
		private byte[] byteData;

		@Override
		public void write (Json json) {

		}

		@Override
		public void read (Json json, JsonValue jsonData) {
			byteLength = jsonData.getInt("byteLength");
			String data = jsonData.getString("uri");

			data = data.split("base64,")[1];

			byteData = Base64Coder.decode(data);
		}
	}

	@Data
	public static class SkinData {

		private String name;
		private int[] joints;
		private int inverseBindMatrices;

		// The skeleton property (if present) points
		// to the node that is the common root of a
		// joints hierarchy or to a direct
		// or indirect parent node of the common root.
		private int skeleton = -1;

	}

	@Data
	public static class AnimationSampler {
		private int input;
		//LINEAR, STEP, CUBICSPLINE
		private String interpolation = "LINEAR";
		private int output;
	}

	@Data
	public static class AnimationChannelTarget {
		private int node = 0;

		//translation, rotation, scale, weights
		private String path;

	}

	@Data
	public static class AnimationChannel {
		private int sampler;
		private AnimationChannelTarget target;

	}

	@Data
	public static class AnimationData {
		private String name;
		private AnimationChannel[] channels;
		private AnimationSampler[] samplers;
	}

	private int scene = -1;
	private SceneData[] scenes;
	private NodeData[] nodes;
	private MaterialData[] materials;
	private TextureData[] textures;
	private ImageData[] images;
	private SamplerData[] samplers;
	private MeshData[] meshes;
	private AccessorData[] accessors;
	private BufferViewData[] bufferViews;
	private BufferData[] buffers;
	private SkinData[] skins;
	private AnimationData[] animations;


}

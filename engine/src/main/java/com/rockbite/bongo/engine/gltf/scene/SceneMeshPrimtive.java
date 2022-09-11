package com.rockbite.bongo.engine.gltf.scene;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ArrayMap;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.OrderedMap;
import com.badlogic.gdx.utils.ShortArray;
import com.rockbite.bongo.engine.gltf.GLTFDataModel;
import lombok.Getter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;

public class SceneMeshPrimtive {

	private OrderedMap<String, Integer> attributes;

	@Getter
	private SceneMeshVertexInfo vertexInfo;

	@Getter
	private String name;

	public Mesh mesh;
	public SceneMaterial sceneMaterial;

	public ArrayMap<SceneNode, Matrix4> invBoneBindTransforms;
	/** The current transformation (relative to the bind pose) of each bone, may be null. When the part is skinned, this will be
	 * updated by a call to {@link ModelInstance#calculateTransforms()}. Do not set or change this value manually. */
	public Matrix4[] bones;

	private float[] vertices;
	private short[] indices;

	public int renderMode = GL20.GL_TRIANGLES;

	public SceneMeshPrimtive (String name, Mesh mesh, SceneMeshVertexInfo vertexInfo) {
		this.name = name;
		this.mesh = mesh;
		this.vertexInfo = vertexInfo;
	}

	private SceneMeshPrimtive () {

	}

	public SceneMeshPrimtive (GLTFDataModel.MeshData meshData, GLTFDataModel.PrimitivesData primitive, GLTFDataModel dataModel, SceneMaterial sceneMaterial, SceneResourceContext resourceContext) {
		this.name = meshData.getName();

		this.sceneMaterial = sceneMaterial;

		attributes = primitive.getAttributes();

		vertexInfo = SceneMeshVertexInfo.Empty();
		for (ObjectMap.Entry<String, Integer> attribute : attributes) {
			final Integer accessorIndex = attribute.value;
			final GLTFDataModel.AccessorData accessor = dataModel.getAccessors()[accessorIndex];

			final SceneMeshVertexInfo.VertexInfo value = new SceneMeshVertexInfo.VertexInfo(attribute.key, accessor.getType());

			vertexInfo.vertexInfoArray.add(value);
		}

		OrderedMap<String, FloatArray> attributeFloatMap = new OrderedMap<>();

		for (ObjectMap.Entry<String, Integer> attribute : attributes) {
			final String key = attribute.key;

			final FloatArray floatArray = new FloatArray();
			attributeFloatMap.put(key, floatArray);

			final Integer accessorIndex = attribute.value;

			final GLTFDataModel.AccessorData accessor = dataModel.getAccessors()[accessorIndex];

			final int bufferViewIndex = accessor.getBufferView();
			final GLTFDataModel.BufferViewData bufferView = dataModel.getBufferViews()[bufferViewIndex];

			final int bufferIndex = bufferView.getBuffer();

			final ByteBuffer buff = resourceContext.getBuffer(bufferIndex);

			if (bufferView.getByteStride() != -1) {
				throw new GdxRuntimeException("Not supported");
			} else {

				//5120 (BYTE)	1
				//5121(UNSIGNED_BYTE)	1
				//5122 (SHORT)	2
				//5123 (UNSIGNED_SHORT)	2
				//5125 (UNSIGNED_INT)	4
				//5126 (FLOAT)	4

				buff.position(bufferView.getByteOffset() + accessor.getByteOffset());


				if (accessor.getComponentType() == GLTFDataModel.ComponentType.C_FLOAT) {
					final FloatBuffer floatBuffer = buff.asFloatBuffer();

					final int byteLength = bufferView.getByteLength();
					int floatCount = byteLength/4;

					for (int i = 0; i < floatCount; i++) {
						final float v = floatBuffer.get(i);
						floatArray.add(v);
					}
					System.out.println(attribute.key);
				} else if (accessor.getComponentType() == GLTFDataModel.ComponentType.C_UBYTE) {


					int [] data = new int[accessor.getCount() * accessor.getType().numComponents];
					for(int i=0 ; i<data.length ; i++){
						data[i] = buff.get() & 0xFF;
						floatArray.add(data[i]);
					}

					System.out.println();

				} else {
					throw new GdxRuntimeException("No type supported for componentType: " + accessor.getComponentType());
				}
			}
		}

		final int indicesIndex = primitive.getIndices();
		final GLTFDataModel.AccessorData indicesAccessor = dataModel.getAccessors()[indicesIndex];
		final int indicesBufferView = indicesAccessor.getBufferView();
		final GLTFDataModel.BufferViewData bufferView = dataModel.getBufferViews()[indicesBufferView];

		final ByteBuffer buff = resourceContext.getBuffer(bufferView.getBuffer());

		buff.position(indicesAccessor.getByteOffset() + bufferView.getByteOffset());

		System.out.println("INDICES TYPE " + indicesAccessor.getType() + " " + indicesAccessor.getComponentType());

		int maxIndices = indicesAccessor.getCount();

		if (indicesAccessor.getComponentType() == GLTFDataModel.ComponentType.C_SHORT || indicesAccessor.getComponentType() == GLTFDataModel.ComponentType.C_USHORT) {

			ShortBuffer shortBuffer = buff.asShortBuffer();
			shortBuffer.position(0);

			this.indices = new short[maxIndices];
			shortBuffer.get(this.indices);


		} else if (indicesAccessor.getComponentType() == GLTFDataModel.ComponentType.C_UINT) {
			indices = new short[maxIndices];
			IntBuffer intBuffer = buff.asIntBuffer();
			long maxIndex = 0;
			for(int i=0 ; i<maxIndices ; i++){
				long index = intBuffer.get() & 0xFFFFFFFFL;
				maxIndex = Math.max(index, maxIndex);
				indices[i] = (short)index;
			}
		}

		final Array<SceneMeshVertexInfo.VertexInfo> vertexInfoArray = vertexInfo.vertexInfoArray;

		final SceneMeshVertexInfo.VertexInfo first = vertexInfoArray.first();
		final int numComponents = first.getNumComponents();
		final String s = attributes.orderedKeys().get(0);
		final FloatArray floatArray = attributeFloatMap.get(s);
		int vertexCount = floatArray.size/numComponents;


		final int totalNumComponentsPerVertex = vertexInfo.getTotalNumComponentsPerVertex();

		vertices = new float[vertexCount * totalNumComponentsPerVertex];

		Arrays.fill(vertices, -1);
		int idx = 0;
		for (int i = 0; i < vertexCount; i++) {
			for (int j = 0; j < vertexInfoArray.size; j++) {
				final SceneMeshVertexInfo.VertexInfo vertex = vertexInfoArray.get(j);
				final String vertexDataKey = attributeFloatMap.orderedKeys().get(j);
				final FloatArray vertexData = attributeFloatMap.get(vertexDataKey);
				int offsetPerVertex = i * vertex.getNumComponents();

				for (int k = 0; k < vertex.getNumComponents(); k++) {
					vertices[idx++] = vertexData.get(offsetPerVertex + k);
				}
			}

		}

		mesh = new Mesh(true, vertices.length/totalNumComponentsPerVertex, indices.length, vertexInfo.packToVertexAttributes());
		mesh.setVertices(vertices);
		mesh.setIndices(indices);
	}

	public SceneMeshPrimtive copy () {
		SceneMeshPrimtive copy = new SceneMeshPrimtive();

		copy.sceneMaterial = sceneMaterial;
		copy.name = name;
		copy.mesh = mesh;
		copy.attributes = attributes;
		copy.vertexInfo = vertexInfo;
		copy.renderMode = renderMode;

		if (invBoneBindTransforms == null) {
			copy.invBoneBindTransforms = null;
			copy.bones = null;
		} else {
			if (copy.invBoneBindTransforms == null) {
				copy.invBoneBindTransforms = new ArrayMap<SceneNode, Matrix4>(true, invBoneBindTransforms.size, SceneNode.class, Matrix4.class);
			} else {
				copy.invBoneBindTransforms.clear();
			}

			copy.invBoneBindTransforms.putAll(invBoneBindTransforms);

			if (copy.bones == null || copy.bones.length != copy.invBoneBindTransforms.size) {
				copy.bones = new Matrix4[invBoneBindTransforms.size];
			}

			for (int i = 0; i < copy.bones.length; i++) {
				if (copy.bones[i] == null) {
					copy.bones[i] = new Matrix4();
				}
			}
		}

		return copy;


	}
}

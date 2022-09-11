package com.rockbite.bongo.engine.gltf.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.graphics.glutils.PixmapTextureData;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Base64Coder;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.ObjectMap;
import com.rockbite.bongo.engine.gltf.GLTFDataModel;
import com.rockbite.bongo.engine.gltf.scene.animation.SceneAnimationData;
import com.rockbite.bongo.engine.gltf.scene.animation.SceneAnimationSampler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Objects;

public class SceneResourceContext {

	private static final Logger logger = LoggerFactory.getLogger(SceneResourceContext.class);

	private IntMap<Texture> loadedTextures = new IntMap<>();
	private ObjectMap<TextureSamplerRef, Texture> hashedTextures = new ObjectMap<>();
	private IntMap<SceneMaterial> sceneMaterials = new IntMap<>();
	private IntMap<SceneMesh> sceneMeshes = new IntMap<>();

	private IntMap<ByteBuffer> buffers = new IntMap<>();
	private IntMap<SceneSkin> sceneSkins = new IntMap<>();
	private IntMap<Array<SceneAnimationData>> sceneAnimationDatas = new IntMap<>();

	public SceneResourceContext () {

	}

	public void loadFromDataModel (GLTFDataModel dataModel) {
		final GLTFDataModel.BufferData[] bufferDatas = dataModel.getBuffers();
		final GLTFDataModel.ImageData[] images = dataModel.getImages();
		final GLTFDataModel.SamplerData[] samplers = dataModel.getSamplers();
		final GLTFDataModel.TextureData[] textures = dataModel.getTextures();
		final GLTFDataModel.MaterialData[] materialDatas = dataModel.getMaterials();
		final GLTFDataModel.MeshData[] meshDatas = dataModel.getMeshes();
		final GLTFDataModel.SkinData[] skins = dataModel.getSkins();
		final GLTFDataModel.AnimationData[] animations = dataModel.getAnimations();

		extractBuffers(bufferDatas);

		extractAndLoadTextures(dataModel, images, samplers, textures);

		extractAndCreateMaterials(materialDatas);

		extractAndCreateGeometry(dataModel, meshDatas);

		extractSkins(dataModel, skins);

		extractAnimations(dataModel, animations);

	}

	private void extractAnimations (GLTFDataModel dataModel, GLTFDataModel.AnimationData[] animations) {
		if (animations == null) return;

		IntMap<float[]> inputFloatArrays = new IntMap<>();
		IntMap<float[]> outputFloatArrays = new IntMap<>();

		for (int i = 0; i < animations.length; i++) {
			final GLTFDataModel.AnimationData animation = animations[i];
			String animationName = animation.getName();

			SceneAnimationData sceneAnimation = new SceneAnimationData(animationName);

			for (int samplerIndex = 0; samplerIndex < animation.getSamplers().length; samplerIndex++) {
				final GLTFDataModel.AnimationSampler sampler = animation.getSamplers()[samplerIndex];

				final int input = sampler.getInput();
				final int output = sampler.getOutput();

				if (!inputFloatArrays.containsKey(input)) {
					float[] inputData = parseAccessorToFloatData(dataModel, input);
					inputFloatArrays.put(input, inputData);
				}

				if (!outputFloatArrays.containsKey(output)) {
					float[] outputData = parseAccessorToFloatData(dataModel, output);
					outputFloatArrays.put(output, outputData);
				}
			}

			for (GLTFDataModel.AnimationChannel channel : animation.getChannels()) {
				final GLTFDataModel.AnimationChannelTarget target = channel.getTarget();
				final GLTFDataModel.AnimationSampler sampler = animation.getSamplers()[channel.getSampler()];

				final int targetNode = target.getNode();
				final String path = target.getPath();

				final float[] inputs = inputFloatArrays.get(sampler.getInput());
				final float[] outputs = outputFloatArrays.get(sampler.getOutput());

				SceneAnimationSampler<?> dataSampler = SceneAnimationSampler.create(inputs, outputs, path, sampler.getInterpolation());
				sceneAnimation.addData(targetNode, dataSampler);

			}

			for (IntMap.Entry<Array<SceneAnimationSampler<?>>> nodesWithDatum : sceneAnimation.getNodesWithData()) {
				final int node = nodesWithDatum.key;
				if (!sceneAnimationDatas.containsKey(node)) {
					sceneAnimationDatas.put(node, new Array<>());
				}
				final Array<SceneAnimationData> sceneAnimationData = sceneAnimationDatas.get(node);
				sceneAnimationData.add(sceneAnimation);
			}
		}

	}

	private float[] parseAccessorToFloatData (GLTFDataModel dataModel, int accessor) {
		final GLTFDataModel.AccessorData outputDataAccessor = dataModel.getAccessors()[accessor];

		final int outputBufferView = outputDataAccessor.getBufferView();
		final GLTFDataModel.BufferViewData outputBufferViewData = dataModel.getBufferViews()[outputBufferView];
		final int outputBuffer = outputBufferViewData.getBuffer();

		if (outputDataAccessor.getComponentType() != GLTFDataModel.ComponentType.C_FLOAT) {
			throw new GdxRuntimeException("Invalid");
		}

		final ByteBuffer byteBuffer = buffers.get(outputBuffer);
		byteBuffer.position(outputDataAccessor.getByteOffset() + outputBufferViewData.getByteOffset());
		final FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();

		int totalFloats = 0;

		if (outputDataAccessor.getType().equals(SceneMeshVertexInfo.AccessorType.SCALAR)) {
			totalFloats = 1;
		}
		if (outputDataAccessor.getType().equals(SceneMeshVertexInfo.AccessorType.VEC3)) {
			totalFloats = 3;
		}
		if (outputDataAccessor.getType().equals(SceneMeshVertexInfo.AccessorType.VEC4)) {
			totalFloats = 4;
		}

		if (totalFloats == 0) {
			throw new GdxRuntimeException("Invalid floats found");
		}

		final int totalTimeEntries = outputDataAccessor.getCount() * totalFloats;
		float[] datas = new float[totalTimeEntries];


		for (int i = 0; i < totalTimeEntries; i++) {
			final float time = floatBuffer.get();
			datas[i] = time;
		}

		return datas;
	}



	private void extractSkins (GLTFDataModel dataModel, GLTFDataModel.SkinData[] skins) {
		if (skins == null) return;
		for (int i = 0, skinsLength = skins.length; i < skinsLength; i++) {
			GLTFDataModel.SkinData skinData = skins[i];

			SceneSkin sceneSkin = new SceneSkin(skinData, dataModel, this);

			sceneSkins.put(i, sceneSkin);

		}
	}

	private void extractAndCreateGeometry (GLTFDataModel dataModel, GLTFDataModel.MeshData[] meshDatas) {
		for (int i = 0; i < meshDatas.length; i++) {
			final GLTFDataModel.MeshData meshData = meshDatas[i];

			SceneMesh sceneMesh = new SceneMesh(meshData, dataModel);

			for (int j = 0; j < meshData.getPrimitives().length; j++) {
				final GLTFDataModel.PrimitivesData primitive = meshData.getPrimitives()[j];
				final int material = primitive.getMaterial();
				final SceneMaterial sceneMaterial = sceneMaterials.get(material);
				SceneMeshPrimtive sceneMeshPrimtive = new SceneMeshPrimtive(meshData, primitive, dataModel, sceneMaterial, this);
				sceneMesh.getSceneMeshPrimtiveArray().add(sceneMeshPrimtive);
			}

			sceneMeshes.put(i, sceneMesh);
		}
	}

	private void extractBuffers (GLTFDataModel.BufferData[] bufferDatas) {
		for (int i = 0; i < bufferDatas.length; i++) {
			final GLTFDataModel.BufferData buffer = bufferDatas[i];
			final ByteBuffer allocate = ByteBuffer.allocate(buffer.getByteLength());
			allocate.order(ByteOrder.LITTLE_ENDIAN);
			allocate.put(buffer.getByteData());
			allocate.position(0);

			buffers.put(i, allocate);
		}
	}

	private void extractAndCreateMaterials (GLTFDataModel.MaterialData[] materialDatas) {
		if (materialDatas == null) return;
		for (int i = 0; i < materialDatas.length; i++) {
			final GLTFDataModel.MaterialData materialData = materialDatas[i];

			SceneMaterial sceneMaterial = new SceneMaterial(this, materialData);
			sceneMaterials.put(i, sceneMaterial);
		}
	}

	public Array<SceneAnimationData> gatherAnimations (int nodeIndex) {
		return sceneAnimationDatas.get(nodeIndex);
	}

	static class TextureSamplerRef {
		int source;
		int sampler;

		@Override
		public boolean equals (Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			TextureSamplerRef that = (TextureSamplerRef)o;
			return source == that.source && sampler == that.sampler;
		}

		@Override
		public int hashCode () {
			return Objects.hash(source, sampler);
		}
	}

	private void extractAndLoadTextures (GLTFDataModel dataModel, GLTFDataModel.ImageData[] images, GLTFDataModel.SamplerData[] samplers, GLTFDataModel.TextureData[] textures) {
		if (textures == null) return;

		for (int i = 0; i < textures.length; i++) {
			final GLTFDataModel.TextureData texture = textures[i];

			final int source = texture.getSource();
			final int sampler = texture.getSampler();

			TextureSamplerRef samplerRef = new TextureSamplerRef();
			samplerRef.source = source;
			samplerRef.sampler = sampler;

			if (hashedTextures.containsKey(samplerRef)) {
				final Texture reference = hashedTextures.get(samplerRef);
				loadedTextures.put(i, reference);
				continue;
			}

			final GLTFDataModel.ImageData sourceImageData = images[source];
			final GLTFDataModel.SamplerData samplerData = samplers[sampler];


			Texture tex = createTexture(createImage(sourceImageData, dataModel), samplerData);
			loadedTextures.put(i, tex);
			hashedTextures.put(samplerRef, tex);
		}
	}

	static int tempFileId = 0;

	private TextureData createImage (GLTFDataModel.ImageData sourceImageData, GLTFDataModel dataModel) {
		final String uri = sourceImageData.getUri();

		if (uri != null) {
			//Load from path

			if (uri.contains("image/jpeg;base64")) {
				final String[] split = uri.split(",", 2);
				String body = split[1];
				final byte[] decode = Base64Coder.decode(body);

				Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888); //todo decode width/height
//				ByteBuffer byteBuffer = BufferUtils.newByteBuffer(decode.length);
//				byteBuffer.put(decode);
//				byteBuffer.position(0);
//				pixmap.setPixels(byteBuffer);

				return new PixmapTextureData(pixmap, Pixmap.Format.RGBA8888, true, true);
			}

		} else {
			final int bufferView = sourceImageData.getBufferView();

			final String mimeType = sourceImageData.getMimeType();
			if (mimeType.equals("image/png")) {
				//Load png from byte array
				final GLTFDataModel.BufferViewData imageBufferView = dataModel.getBufferViews()[bufferView];
				final int buffer = imageBufferView.getBuffer();

				final ByteBuffer byteBuffer = buffers.get(buffer);


				byte[] data = new byte[imageBufferView.getByteLength()];

				byteBuffer.position(imageBufferView.getByteOffset());
				byteBuffer.get(data);

				Pixmap pixmap = new Pixmap(data, 0, data.length);

				return new PixmapTextureData(pixmap, Pixmap.Format.RGBA8888, true, true);
			}
		}

		return null;
	}

	private Texture createTexture (TextureData textureData, GLTFDataModel.SamplerData samplerData) {
		Texture texture = new Texture(textureData);
		texture.bind();
		texture.unsafeSetWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);//This is opengl default apparently

		if (samplerData.getMinFilter() != -1) {
			Gdx.gl.glTexParameteri(texture.glTarget, GL20.GL_TEXTURE_MIN_FILTER, samplerData.getMinFilter());
		}
		if (samplerData.getMagFilter() != -1) {
			Gdx.gl.glTexParameteri(texture.glTarget, GL20.GL_TEXTURE_MAG_FILTER, samplerData.getMagFilter());
		}

		if (samplerData.getWrapS() != -1) {
			Gdx.gl.glTexParameteri(texture.glTarget, GL20.GL_TEXTURE_WRAP_S, samplerData.getWrapS());
		}
		if (samplerData.getWrapT() != -1) {
			Gdx.gl.glTexParameteri(texture.glTarget, GL20.GL_TEXTURE_WRAP_T, samplerData.getWrapT());
		}

		return texture;
	}

	public SceneMesh getMesh (int index) {
		return sceneMeshes.get(index);
	}

	public Texture getTexture (int index) {
		return loadedTextures.get(index);
	}

	public SceneMaterial getMaterial (int materialID) {
		return sceneMaterials.get(materialID);
	}

	public ByteBuffer getBuffer (int bufferIndex) {
		return buffers.get(bufferIndex);
	}

	public SceneSkin getSkin (int skin) {
		return sceneSkins.get(skin);
	}
}

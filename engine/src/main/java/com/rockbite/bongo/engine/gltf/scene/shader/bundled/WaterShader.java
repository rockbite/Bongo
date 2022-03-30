package com.rockbite.bongo.engine.gltf.scene.shader.bundled;

import com.artemis.World;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Attributes;
import com.badlogic.gdx.graphics.g3d.attributes.DepthTestAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.math.Matrix4;
import com.rockbite.bongo.engine.components.singletons.Cameras;
import com.rockbite.bongo.engine.components.singletons.RenderUtils;
import com.rockbite.bongo.engine.gltf.scene.SceneEnvironment;
import com.rockbite.bongo.engine.gltf.scene.SceneRenderable;
import com.rockbite.bongo.engine.gltf.scene.shader.BaseSceneShader;
import com.rockbite.bongo.engine.gltf.scene.shader.WaterAttribute;
import com.rockbite.bongo.engine.gltf.scene.shader.ShaderWaveInfo;
import com.rockbite.bongo.engine.systems.render.DepthPassSystem;
import com.rockbite.bongo.engine.systems.render.ShadowPassSystem;

public class WaterShader extends BaseSceneShader {


	private final static long optionalAttributes = IntAttribute.CullFace | DepthTestAttribute.Type;

	private float time;

	public static class Inputs {
		//global
		public final static Uniform cameraPosition = new Uniform("u_cameraPosition");
		public final static Uniform cameraProjTrans = new Uniform("u_projTrans");
		public final static Uniform cameraProjTransInv = new Uniform("u_projTransInv");
		public final static Uniform time = new Uniform("u_time");
		public final static Uniform waveInfos = new Uniform("u_waveInfos");
		public final static Uniform waveNormal = new Uniform("u_waveNormal");
		public final static Uniform screenSize = new Uniform("u_screenSize");
		public final static Uniform depthTexture = new Uniform("u_depthTexture");
		public final static Uniform foamTexture = new Uniform("u_foamTexture");
		public final static Uniform shadowLightMatrix = new Uniform("u_lightMatrix") {
			@Override
			public boolean validate (BaseSceneShader shader, int inputID, SceneRenderable renderable) {
				boolean hasShadowSystem = shader.hasSystem(ShadowPassSystem.class);
				return hasShadowSystem && super.validate(shader, inputID, renderable);
			}
		};

		public final static Uniform shadowMap = new Uniform("u_shadowMap") {
			@Override
			public boolean validate (BaseSceneShader shader, int inputID, SceneRenderable renderable) {
				boolean hasShadowSystem = shader.hasSystem(ShadowPassSystem.class);
				return hasShadowSystem && super.validate(shader, inputID, renderable);
			}
		};

		//Object
		public final static Uniform objectSRT = new Uniform("u_srt");
		public final static Uniform waves = new Uniform("u_waves");


		//lighting
		public final static Uniform shadowLightDir = new Uniform("u_lightDir");
		public final static Uniform shadowLightColour = new Uniform("u_lightColour");
		public final static Uniform envMap = new Uniform("u_envMap");

	}

	public static class Setters {

		//Global
		public final static Setter cameraPosition = new GlobalSetter() {
			@Override
			public void set (BaseSceneShader shader, int inputID, SceneRenderable renderable, Attributes combinedAttributes) {
				shader.set(inputID, shader.cameras.getGameCamera().position);
			}
		};
		public final static Setter cameraProjTrans = new GlobalSetter() {
			@Override
			public void set (BaseSceneShader shader, int inputID, SceneRenderable renderable, Attributes combinedAttributes) {
				shader.set(inputID, shader.cameras.getGameCamera().combined);
			}
		};
		public final static Setter cameraProjTransInv = new GlobalSetter() {
			private Matrix4 matrix4 = new Matrix4();
			@Override
			public void set (BaseSceneShader shader, int inputID, SceneRenderable renderable, Attributes combinedAttributes) {
				shader.set(inputID, matrix4.set(shader.cameras.getGameCamera().combined).inv());
			}
		};
		public final static Setter waveNormal = new LocalSetter() {
			@Override
			public void set (BaseSceneShader shader, int inputID, SceneRenderable renderable, Attributes combinedAttributes) {
				final TextureAttribute textureAttribute = combinedAttributes.get(TextureAttribute.class, TextureAttribute.Normal);

//				textureAttribute.textureDescription.texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);

				shader.set(inputID, shader.context.textureBinder.bind(textureAttribute.textureDescription));
			}
		};
		public final static Setter screenSize = new GlobalSetter() {
			@Override
			public void set (BaseSceneShader shader, int inputID, SceneRenderable renderable, Attributes combinedAttributes) {
				shader.set(inputID, (float)Gdx.graphics.getBackBufferWidth(), (float)Gdx.graphics.getBackBufferHeight());
			}
		};

		public final static Setter depthTexture = new BaseSceneShader.GlobalSetter() {
			@Override
			public void set (BaseSceneShader shader, int inputID, SceneRenderable renderable, Attributes combinedAttributes) {
				final DepthPassSystem depthPassSystem = shader.world.getSystem(DepthPassSystem.class);
				shader.set(inputID, shader.context.textureBinder.bind(depthPassSystem.getDepthTexture()));
			}
		};



		//object
		public final static Setter objectSRT = new LocalSetter() {
			@Override
			public void set (BaseSceneShader shader, int inputID, SceneRenderable renderable, Attributes combinedAttributes) {
				shader.set(inputID, renderable.worldTransform);
			}
		};


		public final static Setter foamTexture = new BaseSceneShader.LocalSetter() {
			@Override
			public void set (BaseSceneShader shader, int inputID, SceneRenderable renderable, Attributes combinedAttributes) {
				final TextureAttribute textureAttribute = combinedAttributes.get(TextureAttribute.class, TextureAttribute.Diffuse);
				shader.set(inputID, shader.context.textureBinder.bind(textureAttribute.textureDescription));
			}
		};



		//lighting
		public final static Setter shadowLightMatrix = new GlobalSetter() {
			Matrix4 lightSpaceMatrix = new Matrix4();

			@Override
			public void set (BaseSceneShader shader, int inputID, SceneRenderable renderable, Attributes combinedAttributes) {
				final ShadowPassSystem system = shader.world.getSystem(ShadowPassSystem.class);
				final int shadowMapSize = system.getShadowMapSize();

				shader.sceneEnvironment.calculateDirectionLightSpaceMatrix(shader.cameras.getGameCamera(), lightSpaceMatrix, shadowMapSize);

				shader.set(inputID, lightSpaceMatrix);
			}
		};
		public final static Setter shadowLightDir = new GlobalSetter() {
			@Override
			public void set (BaseSceneShader shader, int inputID, SceneRenderable renderable, Attributes combinedAttributes) {
				shader.set(inputID, shader.sceneEnvironment.getDirectionalLightDir());
			}
		};
		public final static Setter shadowLightColour = new GlobalSetter() {
			@Override
			public void set (BaseSceneShader shader, int inputID, SceneRenderable renderable, Attributes combinedAttributes) {
				shader.set(inputID, shader.sceneEnvironment.getDirectionLightColor());
			}
		};
		public final static Setter envMap = new GlobalSetter() {
			public void set (BaseSceneShader shader, int inputID, SceneRenderable renderable, Attributes combinedAttributes) {
				if (shader.sceneEnvironment.getEnvironmentMap() != null) {
					final SceneEnvironment.EnvironmentMap environmentMap = shader.sceneEnvironment.getEnvironmentMap();
					shader.set(inputID, shader.context.textureBinder.bind(environmentMap.getRadianceMap()));
				}
			}
		};
		public final static Setter shadowMap = new GlobalSetter() {
			@Override
			public void set (BaseSceneShader shader, int inputID, SceneRenderable renderable, Attributes combinedAttributes) {
				final ShadowPassSystem system = shader.world.getSystem(ShadowPassSystem.class);
				shader.set(inputID, shader.context.textureBinder.bind(system.getShadowMapDepthTexture()));
			}
		};
	}

	//global
	private int u_time;
	private int u_cameraPosition;
	private int u_projTrans;
	private int u_projTransInv;
	private int u_waveNormal;
	private int u_screenSize;
	private int u_depthTexture;
	private int u_foamTexture;

	//object
	private int u_objectSRT;
	private int u_foam;
	private int u_foamScrolling1;
	private int u_foamScrolling2;

	//lighting
	private int u_lightSpaceMatrix;
	private int u_lightDir;
	private int u_lightColour;
	private int u_envMap;
	private int u_shadowMap;


	public WaterShader (FileHandle vertexSource, FileHandle fragmentSource, SceneRenderable sceneRenderable, World world) {
		super(vertexSource, fragmentSource, sceneRenderable, world);
	}

	@Override
	protected long getOptionalAttributes () {
		return optionalAttributes;
	}

	@Override
	public void begin (Cameras cameras, RenderUtils renderUtils, SceneEnvironment sceneEnvironment) {
		super.begin(cameras, renderUtils, sceneEnvironment);
		context.setDepthTest(GL20.GL_LEQUAL);
		context.setCullFace(GL20.GL_BACK);
		context.setDepthMask(true);


		context.setBlending(true, GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);


		if (has(u_time)) {
			set(u_time, sceneEnvironment.getTime());
		}
	}


	@Override
	public void render (SceneRenderable renderable, Attributes combinedAttributes) {
		super.render(renderable, combinedAttributes);

		final WaterAttribute waterAttribute = combinedAttributes.get(WaterAttribute.class, WaterAttribute.Water);
		final ShaderWaveInfo[] waves = waterAttribute.getWaves();
		for (int i = 0; i < waves.length; i++) {
			final ShaderWaveInfo wave = waves[i];

			wave.getNormedDirection().set(wave.getDirection()[0], wave.getDirection()[1]).nor();

			program.setUniformf("u_waveInfos[" + i + "].direction", wave.normedDirection);
			program.setUniformf("u_waveInfos[" + i + "].amplitude", wave.amplitude[0]);
			program.setUniformf("u_waveInfos[" + i + "].steepness", wave.steepness[0]);
			program.setUniformf("u_waveInfos[" + i + "].frequency", wave.frequency[0]);
			program.setUniformf("u_waveInfos[" + i + "].speed", wave.speed[0]);
		}
	}


	@Override
	protected String createPrefix (SceneRenderable sceneRenderable) {
		String prefix = super.createPrefix(sceneRenderable);

		final Attributes attributes = sceneRenderable.material.getAttributes();
		tmpAttributes.clear();
		tmpAttributes.set(attributes);
		final VertexAttributes vertexAttributes = sceneRenderable.sceneMesh.getVertexInfo().getVertexAttributes();


		final long attributesMask = attributes.getMask();
		final long vertexMask = vertexAttributes.getMask();

		final WaterAttribute attribute = attributes.get(WaterAttribute.class, WaterAttribute.Water);
		final ShaderWaveInfo[] waves = attribute.getWaves();
		final int length = waves.length;

		prefix += "#define WAVE_COUNT " + length + "\n";


		final ShadowPassSystem shadowMapSystem = world.getSystem(ShadowPassSystem.class);
		if (shadowMapSystem != null) {
			prefix += "#define shadowMapFlag\n";
		}


		return prefix;
	}

	@Override
	public void initClassSpecificUniforms () {
		//global
		u_time = register(Inputs.time);
		u_cameraPosition = register(Inputs.cameraPosition, Setters.cameraPosition);
		u_projTrans = register(Inputs.cameraProjTrans, Setters.cameraProjTrans);
		u_projTransInv = register(Inputs.cameraProjTransInv, Setters.cameraProjTransInv);
		u_waveNormal = register(Inputs.waveNormal, Setters.waveNormal);
		u_screenSize = register(Inputs.screenSize, Setters.screenSize);
		u_depthTexture = register(Inputs.depthTexture, Setters.depthTexture);
		u_foamTexture = register(Inputs.foamTexture, Setters.foamTexture);


		//object
		u_objectSRT = register(Inputs.objectSRT, Setters.objectSRT);

		//lighting
		u_lightDir = register(Inputs.shadowLightDir, Setters.shadowLightDir);
		u_lightColour = register(Inputs.shadowLightColour, Setters.shadowLightColour);
		u_envMap = register(Inputs.envMap, Setters.envMap);

		u_lightSpaceMatrix = register(Inputs.shadowLightMatrix, Setters.shadowLightMatrix);
		u_shadowMap = register(Inputs.shadowMap, Setters.shadowMap);
	}

	/**
	 * Compare this shader against the other, used for sorting, light weight shaders are rendered first.
	 *
	 * @param other
	 */
	@Override
	public int compareTo (BaseSceneShader other) {
		if (other instanceof WaterShader) return 0;
		return 1;
	}


}

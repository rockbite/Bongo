package com.rockbite.bongo.engine.systems.render;

import com.artemis.Component;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.graphics.g3d.utils.TextureBinder;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.GLFrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.rockbite.bongo.engine.components.render.CloudLayer;
import com.rockbite.bongo.engine.components.render.ShaderControlResource;
import com.rockbite.bongo.engine.components.singletons.Cameras;
import com.rockbite.bongo.engine.components.singletons.Environment;
import com.rockbite.bongo.engine.components.singletons.RenderUtils;
import com.rockbite.bongo.engine.gltf.scene.shader.BaseSceneShader;
import com.rockbite.bongo.engine.gltf.scene.shader.DefaultSceneShaderProvider;
import com.rockbite.bongo.engine.gltf.scene.shader.SceneShaderProvider;
import com.rockbite.bongo.engine.gltf.scene.shader.bundled.CloudPrepassShader;
import com.rockbite.bongo.engine.meshutils.QuadUtils;
import com.rockbite.bongo.engine.render.ShaderSourceProvider;
import com.rockbite.bongo.engine.systems.RenderPassSystem;
import com.rockbite.bongo.engine.systems.render.ShaderProcessingSystem.CombinedVertFragFiles;

public class CloudRenderSystem extends RenderPassSystem {


	//SINGLETONS
	private RenderUtils renderUtils;
	private Environment environment;
	private Cameras cameras;

	private FrameBuffer targetFrameBuffer;

	private FrameBuffer tempBlurBuffer;
	private FrameBuffer tempBlurBuffer2;

	private FrameBuffer finalNormalMapBlurredBuffer;
	private FrameBuffer finalDepthBlurredBuffer;



	private Mesh quadMesh;
	private Texture noiseTexture;

	private CombinedVertFragFiles depthNoiseProgramFiles;
	private CombinedVertFragFiles normalBlurProgramFiles;
	private CombinedVertFragFiles depthBlurProgramFiles;
	private CombinedVertFragFiles compositeFiles;

	private ShaderProcessingSystem.ProgramExtraction normalBlurPassExtraction = new ShaderProcessingSystem.ProgramExtraction("CloudNormalBlurPass");
	private ShaderProcessingSystem.ProgramExtraction depthNoisePassExtraction = new ShaderProcessingSystem.ProgramExtraction("CloudDepthNoisePass");
	private ShaderProcessingSystem.ProgramExtraction depthBlurPassExtraction = new ShaderProcessingSystem.ProgramExtraction("CloudDepthBlurPass");
	private ShaderProcessingSystem.ProgramExtraction compositeProgramExtraction = new ShaderProcessingSystem.ProgramExtraction("CloudComposite");

	private Matrix4 tempMatrix = new Matrix4();


	public CloudRenderSystem () {
		this(CloudLayer.class);
	}

	public Texture getNormalPassTexture () {
		return targetFrameBuffer.getColorBufferTexture();
	}

	public Texture getDepthPassTexture () {
		return targetFrameBuffer.getTextureAttachments().get(1);
	}

	public Texture getFinalNormalMapBlurred () {
		return finalNormalMapBlurredBuffer.getColorBufferTexture();
	}
	public Texture getFinalDepthMapBlurred () {
		return finalDepthBlurredBuffer.getColorBufferTexture();
	}

	private void buffersResizeAndCreateIfNull () {
		if (targetFrameBuffer != null) {
			targetFrameBuffer.dispose();
		}
		if (tempBlurBuffer != null) {
			tempBlurBuffer.dispose();
		}
		if (tempBlurBuffer2 != null) {
			tempBlurBuffer2.dispose();
		}
		if (finalNormalMapBlurredBuffer != null) {
			finalNormalMapBlurredBuffer.dispose();
		}
		if (finalDepthBlurredBuffer != null) {
			finalDepthBlurredBuffer.dispose();
		}

		targetFrameBuffer = createBuffer();
		tempBlurBuffer = createBlurBuffer();
		tempBlurBuffer2 = createBlurBuffer();
		finalNormalMapBlurredBuffer = createBlurBuffer();
		finalDepthBlurredBuffer = createBlurBuffer();
	}


	private FrameBuffer createBuffer () {
		int bufferWidth = 2048;
		final int width = Gdx.graphics.getWidth();
		final int height = Gdx.graphics.getHeight();
		float aspect = height/(float)width;

		int bufferHeight = (int)(bufferWidth * aspect);

		GLFrameBuffer.FrameBufferBuilder frameBufferBuilder = new GLFrameBuffer.FrameBufferBuilder(bufferWidth, bufferHeight);
		frameBufferBuilder.addBasicColorTextureAttachment(Pixmap.Format.RGBA8888);
		frameBufferBuilder.addDepthTextureAttachment(GL20.GL_DEPTH_COMPONENT, GL20.GL_FLOAT);

		final FrameBuffer build = frameBufferBuilder.build();
		build.getColorBufferTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
		build.getTextureAttachments().get(1).setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

		return build;
	}

	private FrameBuffer createBlurBuffer () {
		int bufferWidth = 2048;
		final int width = Gdx.graphics.getWidth();
		final int height = Gdx.graphics.getHeight();
		float aspect = height/(float)width;

		int bufferHeight = (int)(bufferWidth * aspect);

		GLFrameBuffer.FrameBufferBuilder frameBufferBuilder = new GLFrameBuffer.FrameBufferBuilder(bufferWidth, bufferHeight);
		frameBufferBuilder.addBasicColorTextureAttachment(Pixmap.Format.RGBA8888);

		final FrameBuffer build = frameBufferBuilder.build();
		build.getColorBufferTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
		return build;
	}

	public CloudRenderSystem (Class<? extends Component> componentClazz) {
		this(
			new DefaultSceneShaderProvider(ShaderSourceProvider.resolveVertex("cloud-pre", Files.FileType.Internal), ShaderSourceProvider.resolveFragment("cloud-pre", Files.FileType.Internal), CloudPrepassShader.class),
			componentClazz
		);
	}

	public CloudRenderSystem (SceneShaderProvider sceneShaderProvider, Class<? extends Component>... componentsToGather) {
		super(sceneShaderProvider, componentsToGather);
	}

	@Override
	protected void initialize () {
		super.initialize();

		quadMesh = QuadUtils.createFullScreenQuad();

		buffersResizeAndCreateIfNull();//Call on resize too

		depthNoiseProgramFiles = ShaderProcessingSystem.generateShaderDependencies(
			this::reloadDepthNoiseShader,
			Gdx.files.internal("shaders/cloud/cloud-depth-noise-vert.glsl"),
			Gdx.files.internal("shaders/cloud/cloud-depth-noise-frag.glsl")
		);

		normalBlurProgramFiles = ShaderProcessingSystem.generateShaderDependencies(
			this::reloadBlurShader,
			Gdx.files.internal("shaders/cloud/cloud-blur-vert.glsl"),
			Gdx.files.internal("shaders/cloud/cloud-blur-frag.glsl")
			);

		depthBlurProgramFiles = ShaderProcessingSystem.generateShaderDependencies(
			this::reloadDepthBlurShader,
			Gdx.files.internal("shaders/cloud/cloud-depth-blur-vert.glsl"),
			Gdx.files.internal("shaders/cloud/cloud-depth-blur-frag.glsl")
		);

		compositeFiles = ShaderProcessingSystem.generateShaderDependencies(
			this::reloadCompositeShader,
			Gdx.files.internal("shaders/cloud/cloud-composite-vert.glsl"),
			Gdx.files.internal("shaders/cloud/cloud-composite-frag.glsl")
		);

		final int blurEntity = world.create();
		final ShaderControlResource blurControlResource = world.edit(blurEntity).create(ShaderControlResource.class);
		blurControlResource.setShaderControlProvider(normalBlurPassExtraction);

		final int depthNoiseEntity = world.create();
		final ShaderControlResource depthNoiseControlResource = world.edit(depthNoiseEntity).create(ShaderControlResource.class);
		depthNoiseControlResource.setShaderControlProvider(depthNoisePassExtraction);


		final int depthEntity = world.create();
		final ShaderControlResource depthControlResource = world.edit(depthEntity).create(ShaderControlResource.class);
		depthControlResource.setShaderControlProvider(depthBlurPassExtraction);


		final int compositeEntity = world.create();
		final ShaderControlResource compositeControlResource = world.edit(compositeEntity).create(ShaderControlResource.class);
		compositeControlResource.setShaderControlProvider(compositeProgramExtraction);

		reloadBlurShader();
		reloadDepthNoiseShader();
		reloadDepthBlurShader();
		reloadCompositeShader();


		noiseTexture = new Texture(Gdx.files.internal("textures/defaultNoise.png"));
		noiseTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
		noiseTexture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
	}

	private void reloadDepthNoiseShader () {
		final ShaderProcessingSystem.ProgramExtraction programExtraction = ShaderProcessingSystem.compileAndExtract("", depthNoiseProgramFiles);
		if (programExtraction != null) {
			if (this.depthNoisePassExtraction.getProgram() != null) {
				this.depthNoisePassExtraction.program.dispose();
			}
			this.depthNoisePassExtraction.setFrom(programExtraction);
		}
	}

	private void reloadBlurShader () {
		final ShaderProcessingSystem.ProgramExtraction programExtraction = ShaderProcessingSystem.compileAndExtract("", normalBlurProgramFiles);
		if (programExtraction != null) {
			if (this.normalBlurPassExtraction.getProgram() != null) {
				this.normalBlurPassExtraction.program.dispose();
			}
			this.normalBlurPassExtraction.setFrom(programExtraction);
		}
	}

	private void reloadDepthBlurShader () {
		final ShaderProcessingSystem.ProgramExtraction programExtraction = ShaderProcessingSystem.compileAndExtract("", depthBlurProgramFiles);
		if (programExtraction != null) {
			if (this.depthBlurPassExtraction.getProgram() != null) {
				this.depthBlurPassExtraction.program.dispose();
			}
			this.depthBlurPassExtraction.setFrom(programExtraction);
		}
	}

	private void reloadCompositeShader () {
		final ShaderProcessingSystem.ProgramExtraction programExtraction = ShaderProcessingSystem.compileAndExtract("", compositeFiles);
		if (programExtraction != null) {
			if (this.compositeProgramExtraction.getProgram() != null) {
				this.compositeProgramExtraction.program.dispose();
			}
			this.compositeProgramExtraction.setFrom(programExtraction);
		}
	}

	/**
	 * Process the system.
	 */
	@Override
	protected void processSystem () {
		final RenderContext renderContext = renderUtils.getRenderContext();

		targetFrameBuffer.begin();
		Gdx.gl.glClearColor(0, 0, 0, 0);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

		renderAllCollectedRenderables();
		targetFrameBuffer.end();


		//Blur normal+alpha
		//Blur depth

		//sample and blur horizontally to ping
		//sample ping and blur vertically to pong
		//sample ping and blur to pong

		final TextureBinder textureBinder = renderContext.textureBinder;


		//guassian blur on normals
		blurIntoBuffer(normalBlurPassExtraction, textureBinder, targetFrameBuffer.getColorBufferTexture(), tempBlurBuffer, true);
		blurIntoBuffer(normalBlurPassExtraction, textureBinder, tempBlurBuffer.getColorBufferTexture(), finalNormalMapBlurredBuffer, false);

		//guassian blur on depth
		blurIntoBuffer(depthBlurPassExtraction, textureBinder, targetFrameBuffer.getTextureAttachments().get(1), tempBlurBuffer, true);
		blurIntoBuffer(depthBlurPassExtraction, textureBinder, tempBlurBuffer.getColorBufferTexture(), tempBlurBuffer2, false);
		noiseIntoBuffer(depthNoisePassExtraction, textureBinder, tempBlurBuffer2.getColorBufferTexture(), finalDepthBlurredBuffer);


		//Composite to screenspace

		renderContext.setDepthTest(GL20.GL_NONE);

		renderContext.setBlending(true, GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

		ShaderProgram compositeProgram = compositeProgramExtraction.program;

		compositeProgram.bind();
		for (BaseSceneShader.ShaderControl shaderControl : compositeProgramExtraction.getShaderControls()) {
			shaderControl.injectIntoShader(compositeProgram);
		}

		final DepthPassSystem depthPassSystem = world.getSystem(DepthPassSystem.class);


		compositeProgram.setUniformi("u_noiseTexture", textureBinder.bind(noiseTexture));
		compositeProgram.setUniformi("u_diffuse", textureBinder.bind(finalNormalMapBlurredBuffer.getColorBufferTexture()));
		compositeProgram.setUniformi("u_sceneDepth", textureBinder.bind(depthPassSystem.getDepthTexture()));
		compositeProgram.setUniformi("u_depth", textureBinder.bind(finalDepthBlurredBuffer.getColorBufferTexture()));
		compositeProgram.setUniformf("u_time", environment.getSceneEnvironment().getTime());

		compositeProgram.setUniformf("u_lightDir", environment.getSceneEnvironment().getDirectionalLightDir());
		compositeProgram.setUniformf("u_lightColour", environment.getSceneEnvironment().getDirectionLightColor());
		compositeProgram.setUniformf("u_cameraPosition", cameras.getGameCamera().position);
		compositeProgram.setUniformf("u_resolution", Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());

		tempMatrix.set(cameras.getGameCamera().combined).inv();
		compositeProgram.setUniformMatrix("u_projTransInv", tempMatrix);

		quadMesh.render(compositeProgram, GL20.GL_TRIANGLE_FAN);

	}

	private void noiseIntoBuffer (ShaderProcessingSystem.ProgramExtraction shaderExtraction, TextureBinder textureBinder,
		Texture textureToSampleFrom, FrameBuffer bufferToWriteTo) {

		final ShaderProgram shaderProgram = shaderExtraction.getProgram();

		bufferToWriteTo.begin();
		Gdx.gl.glClearColor(0, 0, 0, 0);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

		shaderProgram.bind();
		shaderProgram.setUniformi("u_texture", textureBinder.bind(textureToSampleFrom));
		shaderProgram.setUniformi("u_noiseTexture", textureBinder.bind(noiseTexture));
		shaderProgram.setUniformi("u_normalTexture", textureBinder.bind(targetFrameBuffer.getColorBufferTexture()));
		shaderProgram.setUniformf("u_time", environment.getSceneEnvironment().getTime());
		shaderProgram.setUniformf("u_near", cameras.getGameCamera().near);
		shaderProgram.setUniformf("u_far", cameras.getGameCamera().far);
		tempMatrix.set(cameras.getGameCamera().combined).inv();
		shaderProgram.setUniformMatrix("u_projTransInv", tempMatrix);
		shaderProgram.setUniformMatrix("u_projTrans", cameras.getGameCamera().combined);

		float time = environment.getSceneEnvironment().getTime();
		float noiseScale = 0.2f;
		Vector3 tempVec3 = new Vector3(
			MathUtils.sin(time * noiseScale),
			MathUtils.sin(time * noiseScale + 0.333f),
			MathUtils.sin(time * noiseScale + 0.666f)
		);
		tempVec3.scl(2f * MathUtils.PI);

		shaderProgram.setUniformf("u_sinTime", tempVec3);


		for (BaseSceneShader.ShaderControl shaderControl : shaderExtraction.getShaderControls()) {
			shaderControl.injectIntoShader(shaderProgram);
		}
		quadMesh.render(shaderProgram, GL20.GL_TRIANGLE_FAN);

		bufferToWriteTo.end();
	}

	private void blurIntoBuffer (ShaderProcessingSystem.ProgramExtraction shaderExtraction, TextureBinder textureBinder,
		Texture textureToSampleFrom, FrameBuffer bufferToWriteTo,
		boolean horizontal) {

		final ShaderProgram blurShader = shaderExtraction.getProgram();

		bufferToWriteTo.begin();
		Gdx.gl.glClearColor(0, 0, 0, 0);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

		blurShader.bind();
		for (BaseSceneShader.ShaderControl shaderControl : shaderExtraction.getShaderControls()) {
			shaderControl.injectIntoShader(blurShader);
		}

		blurShader.setUniformi("u_texture", textureBinder.bind(textureToSampleFrom));
		blurShader.setUniformf("u_resolution", bufferToWriteTo.getWidth(), bufferToWriteTo.getHeight());

		if (horizontal) {
			blurShader.setUniformf("u_direction", 1f, 0f);
		} else {
			blurShader.setUniformf("u_direction", 0, 1f);
		}

		quadMesh.render(blurShader, GL20.GL_TRIANGLE_FAN);

		bufferToWriteTo.end();
	}

	@Override
	protected void dispose () {
		super.dispose();
		targetFrameBuffer.dispose();
		tempBlurBuffer.dispose();
		finalNormalMapBlurredBuffer.dispose();
		finalDepthBlurredBuffer.dispose();
		quadMesh.dispose();
		noiseTexture.dispose();
		normalBlurPassExtraction.program.dispose();
		depthBlurPassExtraction.program.dispose();
		compositeProgramExtraction.program.dispose();
		depthNoisePassExtraction.program.dispose();
	}
}

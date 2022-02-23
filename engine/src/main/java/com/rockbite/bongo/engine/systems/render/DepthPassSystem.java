package com.rockbite.bongo.engine.systems.render;

import com.artemis.Component;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.GLFrameBuffer;
import com.rockbite.bongo.engine.components.render.DepthLayer;
import com.rockbite.bongo.engine.gltf.scene.shader.DefaultSceneShaderProvider;
import com.rockbite.bongo.engine.gltf.scene.shader.SceneShaderProvider;
import com.rockbite.bongo.engine.gltf.scene.shader.bundled.DepthShader;
import com.rockbite.bongo.engine.render.FrameBufferWithDepthOnly;
import com.rockbite.bongo.engine.render.ShaderSourceProvider;
import com.rockbite.bongo.engine.systems.RenderPassSystem;

public class DepthPassSystem extends RenderPassSystem {

	private FrameBuffer sampleableDepthFrameBuffer;

	public DepthPassSystem () {
		this(DepthLayer.class);
	}

	public DepthPassSystem (Class<? extends Component> componentClazz) {
		this(
			new DefaultSceneShaderProvider(ShaderSourceProvider.resolveVertex("core/depth", Files.FileType.Classpath), ShaderSourceProvider.resolveFragment("core/depth", Files.FileType.Classpath), DepthShader.class),
			componentClazz
		);
	}

	public DepthPassSystem (SceneShaderProvider sceneShaderProvider, Class<? extends Component>... componentsToGather) {
		super(sceneShaderProvider, componentsToGather);
	}


	@Override
	protected void initialize () {
		super.initialize();

		if (Gdx.gl30 != null) {
			GLFrameBuffer.FrameBufferBuilder frameBufferBuilder = new GLFrameBuffer.FrameBufferBuilder(Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight()) {
				@Override
				public FrameBuffer build () {
					return new FrameBufferWithDepthOnly(this);
				}
			};
			frameBufferBuilder.addDepthTextureAttachment(GL30.GL_DEPTH_COMPONENT32F, GL20.GL_FLOAT);
			sampleableDepthFrameBuffer = frameBufferBuilder.build();

			final Texture depthTexture = sampleableDepthFrameBuffer.getTextureAttachments().get(0);
			depthTexture.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
			depthTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
		} else {
			GLFrameBuffer.FrameBufferBuilder frameBufferBuilder = new GLFrameBuffer.FrameBufferBuilder(Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
			frameBufferBuilder.addBasicColorTextureAttachment(Pixmap.Format.RGBA8888);
			sampleableDepthFrameBuffer = frameBufferBuilder.build();
		}
	}

	/**
	 * Process the system.
	 */
	@Override
	protected void processSystem () {

		sampleableDepthFrameBuffer.begin();
		Gdx.gl.glClearColor(0.2f, 0, 0, 1f);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

		renderAllCollectedRenderables();

		sampleableDepthFrameBuffer.end();
	}

	public Texture getDepthTexture () {
		return sampleableDepthFrameBuffer.getTextureAttachments().get(0);
	}
}

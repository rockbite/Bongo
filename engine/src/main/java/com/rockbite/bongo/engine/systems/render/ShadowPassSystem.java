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
import com.badlogic.gdx.utils.Array;
import com.rockbite.bongo.engine.components.render.DepthLayer;
import com.rockbite.bongo.engine.components.render.ShadowLayer;
import com.rockbite.bongo.engine.gltf.scene.shader.DefaultSceneShaderProvider;
import com.rockbite.bongo.engine.gltf.scene.shader.SceneShaderProvider;
import com.rockbite.bongo.engine.gltf.scene.shader.bundled.ShadowMapShader;
import com.rockbite.bongo.engine.render.FrameBufferWithDepthOnly;
import com.rockbite.bongo.engine.render.ShaderSourceProvider;
import com.rockbite.bongo.engine.systems.RenderPassSystem;

public class ShadowPassSystem extends RenderPassSystem {

	private FrameBuffer shadowMapBuffer;

	public ShadowPassSystem () {
		this(ShadowLayer.class);
	}

	public ShadowPassSystem (Class<? extends Component> componentClazz) {
		this(
			new DefaultSceneShaderProvider(ShaderSourceProvider.resolveVertex("core/shadow", Files.FileType.Classpath), ShaderSourceProvider.resolveFragment("core/shadow", Files.FileType.Classpath), ShadowMapShader.class),
			componentClazz
		);
	}

	public ShadowPassSystem (SceneShaderProvider sceneShaderProvider, Class<? extends Component>... componentsToGather) {
		super(sceneShaderProvider, componentsToGather);
	}

	public Texture getShadowMapDepthTexture () {

		final Array<Texture> textureAttachments = shadowMapBuffer.getTextureAttachments();
		return textureAttachments.get(0);
	}

	public int getShadowMapSize () {
		return shadowMapBuffer.getWidth();
	}

	@Override
	protected void initialize () {
		super.initialize();

		final int shadowMapSize = 2048;
		if (Gdx.gl30 != null) {
			GLFrameBuffer.FrameBufferBuilder frameBufferBuilder = new GLFrameBuffer.FrameBufferBuilder(shadowMapSize, shadowMapSize) {
				@Override
				public FrameBuffer build () {
					return new FrameBufferWithDepthOnly(this);
				}
			};
			frameBufferBuilder.addDepthTextureAttachment(GL30.GL_DEPTH_COMPONENT32F, GL20.GL_FLOAT);
			shadowMapBuffer = frameBufferBuilder.build();

			final Texture depthTexture = shadowMapBuffer.getTextureAttachments().get(0);
			depthTexture.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
			depthTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
		} else {
			GLFrameBuffer.FrameBufferBuilder frameBufferBuilder = new GLFrameBuffer.FrameBufferBuilder(shadowMapSize, shadowMapSize);
			frameBufferBuilder.addBasicColorTextureAttachment(Pixmap.Format.RGBA8888);
			frameBufferBuilder.addBasicDepthRenderBuffer();

			shadowMapBuffer = frameBufferBuilder.build();
			shadowMapBuffer.getColorBufferTexture().setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
			shadowMapBuffer.getColorBufferTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
		}
	}

	/**
	 * Process the system.
	 */
	@Override
	protected void processSystem () {

		shadowMapBuffer.begin();
		Gdx.gl.glClearColor(0, 0, 0, 1f);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);


		renderAllCollectedRenderables();

		shadowMapBuffer.end(glViewport.x, glViewport.y, glViewport.width, glViewport.height);
	}



}

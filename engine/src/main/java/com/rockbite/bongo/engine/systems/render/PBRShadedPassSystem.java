package com.rockbite.bongo.engine.systems.render;

import com.artemis.Component;
import com.badlogic.gdx.Files;
import com.rockbite.bongo.engine.components.render.ShadedLayer;
import com.rockbite.bongo.engine.gltf.scene.shader.DefaultSceneShaderProvider;
import com.rockbite.bongo.engine.gltf.scene.shader.SceneShaderProvider;
import com.rockbite.bongo.engine.gltf.scene.shader.bundled.ShadedShader;
import com.rockbite.bongo.engine.render.ShaderSourceProvider;
import com.rockbite.bongo.engine.systems.RenderPassSystem;

public class PBRShadedPassSystem extends RenderPassSystem {

	public PBRShadedPassSystem () {
		this(ShadedLayer.class);
	}

	public PBRShadedPassSystem (Class<? extends Component> componentClazz) {
		this(
			new DefaultSceneShaderProvider(ShaderSourceProvider.resolveVertex("core/shaded", Files.FileType.Classpath), ShaderSourceProvider.resolveFragment("core/shaded", Files.FileType.Classpath), ShadedShader.class),
			componentClazz
		);
	}

	public PBRShadedPassSystem (SceneShaderProvider sceneShaderProvider, Class<? extends Component>... componentsToGather) {
		super(sceneShaderProvider, componentsToGather);
	}

	@Override
	protected void initialize () {
		super.initialize();

	}

	/**
	 * Process the system.
	 */
	@Override
	protected void processSystem () {
		renderAllCollectedRenderables();
	}
}

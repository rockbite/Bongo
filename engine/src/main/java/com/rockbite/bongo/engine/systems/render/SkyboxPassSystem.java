package com.rockbite.bongo.engine.systems.render;

import com.artemis.Component;
import com.badlogic.gdx.Files;
import com.rockbite.bongo.engine.components.render.Skybox;
import com.rockbite.bongo.engine.components.render.UnlitLayer;
import com.rockbite.bongo.engine.gltf.scene.shader.DefaultSceneShaderProvider;
import com.rockbite.bongo.engine.gltf.scene.shader.SceneShaderProvider;
import com.rockbite.bongo.engine.gltf.scene.shader.bundled.SkyboxShader;
import com.rockbite.bongo.engine.gltf.scene.shader.bundled.UnlitShader;
import com.rockbite.bongo.engine.render.ShaderSourceProvider;
import com.rockbite.bongo.engine.systems.RenderPassSystem;

public class SkyboxPassSystem extends RenderPassSystem {

	public SkyboxPassSystem () {
		this(Skybox.class);
	}

	public SkyboxPassSystem (Class<? extends Component> componentClazz) {
		this(
			new DefaultSceneShaderProvider(ShaderSourceProvider.resolveVertex("core/skybox", Files.FileType.Classpath), ShaderSourceProvider.resolveFragment("core/skybox", Files.FileType.Classpath), SkyboxShader.class),
			componentClazz
		);
	}

	public SkyboxPassSystem (SceneShaderProvider sceneShaderProvider, Class<? extends Component>... componentsToGather) {
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

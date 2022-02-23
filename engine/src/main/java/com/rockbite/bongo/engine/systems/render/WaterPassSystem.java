package com.rockbite.bongo.engine.systems.render;

import com.artemis.Component;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.rockbite.bongo.engine.components.render.ShadedLayer;
import com.rockbite.bongo.engine.components.render.WaterLayer;
import com.rockbite.bongo.engine.gltf.scene.shader.DefaultSceneShaderProvider;
import com.rockbite.bongo.engine.gltf.scene.shader.SceneShaderProvider;
import com.rockbite.bongo.engine.gltf.scene.shader.bundled.WaterShader;
import com.rockbite.bongo.engine.render.ShaderSourceProvider;
import com.rockbite.bongo.engine.systems.RenderPassSystem;

public class WaterPassSystem extends RenderPassSystem {

	public WaterPassSystem () {
		this(WaterLayer.class);
	}

	public WaterPassSystem (Class<? extends Component> componentClazz) {
		this(
			new DefaultSceneShaderProvider(ShaderSourceProvider.resolveVertex("core/water", Files.FileType.Classpath), ShaderSourceProvider.resolveFragment("core/water", Files.FileType.Classpath), WaterShader.class),
			componentClazz
		);
	}

	public WaterPassSystem (SceneShaderProvider sceneShaderProvider, Class<? extends Component>... componentsToGather) {
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

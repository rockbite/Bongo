package com.rockbite.bongo.engine.gltf.scene.shader;

import com.artemis.World;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.rockbite.bongo.engine.gltf.scene.SceneRenderable;

public abstract class SceneShaderProvider {

	protected Array<BaseSceneShader> shaders = new Array<BaseSceneShader>();

	private World world;

	public BaseSceneShader getShader (SceneRenderable renderable) {
		BaseSceneShader suggestedShader = renderable.shader;
		if (suggestedShader != null && suggestedShader.canRender(renderable)) return suggestedShader;
		for (BaseSceneShader shader : shaders) {
			if (shader.canRender(renderable)) return shader;
		}
		final BaseSceneShader shader = createShader(renderable, world);
		if (!shader.canRender(renderable)) throw new GdxRuntimeException("unable to provide a shader for this renderable");
		shader.init();
		shaders.add(shader);
		return shader;
	}

	protected abstract BaseSceneShader createShader (final SceneRenderable renderable, World world);

	public void injectWorld (World world) {
		this.world = world;
	}
	
	public void dispose () {
		for (BaseSceneShader shader : shaders) {
			shader.dispose();
		}
		shaders.clear();
	}


}

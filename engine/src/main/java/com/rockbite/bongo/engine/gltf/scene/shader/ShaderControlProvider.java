package com.rockbite.bongo.engine.gltf.scene.shader;

import com.badlogic.gdx.utils.Array;


public interface ShaderControlProvider {
	Array<BaseSceneShader.ShaderControl> getShaderControls ();

	String getShaderDisplayName ();
}

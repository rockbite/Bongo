package com.rockbite.bongo.engine.gltf.scene;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Pool;
import com.rockbite.bongo.engine.gltf.scene.shader.BaseSceneShader;
import lombok.Data;

@Data
public class SceneRenderable implements Pool.Poolable {

	public BaseSceneShader shader;
	public SceneNode referenceSceneNode;
	public SceneMeshPrimtive sceneMesh;
	public SceneMaterial material;

	public Matrix4 worldTransform = new Matrix4();
	public Matrix4[] bones = null;

	/**
	 * Resets the object for reuse. Object references should be nulled and fields may be set to default values.
	 */
	@Override
	public void reset () {
		shader = null;
		referenceSceneNode = null;
		sceneMesh = null;
		material = null;

		bones = null;

	}

	public SceneRenderable copy () {
		final SceneRenderable copy = new SceneRenderable();
		copy.referenceSceneNode = referenceSceneNode;
		copy.sceneMesh = sceneMesh;
		copy.material = material;
		copy.worldTransform = worldTransform;
		copy.bones = bones;
		return copy;
	}
}

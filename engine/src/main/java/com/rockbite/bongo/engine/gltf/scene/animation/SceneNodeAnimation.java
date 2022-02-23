package com.rockbite.bongo.engine.gltf.scene.animation;

import com.badlogic.gdx.utils.Array;
import com.rockbite.bongo.engine.gltf.scene.SceneNode;
import lombok.Data;

@Data
public class SceneNodeAnimation {

	private SceneNode sceneNode;

	private Array<SceneAnimationSampler<?>> samplers;

	public SceneNodeAnimation () {

	}

	public SceneNodeAnimation (SceneNode copyReferenceNode, SceneNodeAnimation sceneAnimation) {
		this.sceneNode = copyReferenceNode;
		samplers = sceneAnimation.getSamplers();
	}

}

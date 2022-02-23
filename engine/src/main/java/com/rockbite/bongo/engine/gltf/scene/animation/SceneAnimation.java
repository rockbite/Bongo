package com.rockbite.bongo.engine.gltf.scene.animation;

import com.badlogic.gdx.utils.Array;
import lombok.Data;

@Data
public class SceneAnimation {

	private String name;
	private float maxInputTime;

	private Array<SceneNodeAnimation> sceneNodeAnimationArray = new Array<>();

	public SceneAnimation (String name) {
		this.name = name;
	}

	public SceneAnimation (SceneAnimation sceneAnimation) {
		this.name = sceneAnimation.name;
		this.maxInputTime = sceneAnimation.maxInputTime;
	}

	public float getMaxInputTime () {
		return maxInputTime;
	}

	public void calculateMaxTime () {
		for (SceneNodeAnimation sceneNodeAnimation : sceneNodeAnimationArray) {
			for (SceneAnimationSampler<?> sampler : sceneNodeAnimation.getSamplers()) {
				maxInputTime = Math.max(sampler.maxInputTime, this.maxInputTime);
			}
		}
	}
}

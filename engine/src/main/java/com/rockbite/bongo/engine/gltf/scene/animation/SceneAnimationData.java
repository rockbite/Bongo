package com.rockbite.bongo.engine.gltf.scene.animation;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;
import lombok.Getter;

public class SceneAnimationData {

	@Getter
	private final String animationName;

	@Getter
	private IntMap<Array<SceneAnimationSampler<?>>> nodesWithData = new IntMap<Array<SceneAnimationSampler<?>>>();

	public SceneAnimationData (String animationName) {
		this.animationName = animationName;
	}

	public void addData (int targetNode, SceneAnimationSampler<?> dataSampler) {
		if (nodesWithData.containsKey(targetNode)) {
			final Array<SceneAnimationSampler<?>> sceneAnimationSamplers = nodesWithData.get(targetNode);
			sceneAnimationSamplers.add(dataSampler);
		} else {
			final Array<SceneAnimationSampler<?>> value = new Array<>();
			value.add(dataSampler);
			nodesWithData.put(targetNode, value);
		}
	}
}

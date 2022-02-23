package com.rockbite.bongo.engine.systems.render;

import com.artemis.ComponentMapper;
import com.artemis.Entity;
import com.artemis.annotations.All;
import com.artemis.systems.EntityProcessingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.rockbite.bongo.engine.components.render.Animation;
import com.rockbite.bongo.engine.gltf.scene.SceneModelInstance;
import com.rockbite.bongo.engine.gltf.scene.SceneNode;
import com.rockbite.bongo.engine.gltf.scene.animation.SceneAnimation;
import com.rockbite.bongo.engine.gltf.scene.animation.SceneAnimationSampler;
import com.rockbite.bongo.engine.gltf.scene.animation.SceneNodeAnimation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@All({SceneModelInstance.class, Animation.class})
public class AnimationSystem extends EntityProcessingSystem {

	private static final Logger logger = LoggerFactory.getLogger(AnimationSystem.class);

	//MAPPERS
	private ComponentMapper<SceneModelInstance> modelMapper;
	private ComponentMapper<Animation> animationMapper;

	/**
	 * Process a entity this system is interested in.
	 *
	 * @param e the entity to process
	 */
	@Override
	protected void process (Entity e) {
		final SceneModelInstance sceneModelInstance = modelMapper.get(e.getId());
		final Animation animation = animationMapper.get(e.getId());

		final SceneAnimation anim = sceneModelInstance.findAnimation(animation.getAnimationName());

		if (anim != null) {
			float trackTime = animation.getTrack();
			trackTime += Gdx.graphics.getDeltaTime();
			if (trackTime > anim.getMaxInputTime()) {
				trackTime = 0;
			}
			animation.setTrack(trackTime);

			for (SceneNodeAnimation sceneNodeAnimation : anim.getSceneNodeAnimationArray()) {
				final SceneNode sceneNode = sceneNodeAnimation.getSceneNode();
				final Array<SceneAnimationSampler<?>> samplers = sceneNodeAnimation.getSamplers();

				translation.set(sceneNode.translation);
				rotation.set(sceneNode.rotation);
				scale.set(sceneNode.scale);

				for (SceneAnimationSampler<?> sampler : samplers) {

					if (sampler instanceof SceneAnimationSampler.RotationSampler) {
						rotation.set(((SceneAnimationSampler.RotationSampler)sampler).getInterpolatedValueForTime(trackTime));
					}
					if (sampler instanceof SceneAnimationSampler.ScaleSampler) {
						scale.set(((SceneAnimationSampler.ScaleSampler)sampler).getInterpolatedValueForTime(trackTime));
					}
					if (sampler instanceof SceneAnimationSampler.TranslationSampler) {
						Vector3 translationForNode = ((SceneAnimationSampler.TranslationSampler)sampler).getInterpolatedValueForTime(trackTime);
						translation.set(translationForNode);
					}

				}
				sceneNode.isAnimated = true;
				sceneNode.localTransform.set(translation, rotation, scale);
			}

			sceneModelInstance.calculateTransforms();
		}

	}

	private Vector3 translation = new Vector3();
	private Vector3 scale = new Vector3();
	private Quaternion rotation = new Quaternion();

}

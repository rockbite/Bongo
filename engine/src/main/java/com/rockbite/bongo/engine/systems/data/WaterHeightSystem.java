package com.rockbite.bongo.engine.systems.data;

import com.artemis.ComponentMapper;
import com.artemis.Entity;
import com.artemis.EntitySystem;
import com.artemis.annotations.All;
import com.artemis.utils.Bag;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ObjectSet;
import com.rockbite.bongo.engine.components.render.WaterLayer;
import com.rockbite.bongo.engine.components.singletons.Environment;
import com.rockbite.bongo.engine.gltf.scene.SceneMaterial;
import com.rockbite.bongo.engine.gltf.scene.SceneModelInstance;
import com.rockbite.bongo.engine.gltf.scene.shader.ShaderWaveInfo;
import com.rockbite.bongo.engine.gltf.scene.shader.WaterAttribute;

@All({SceneModelInstance.class, WaterLayer.class})
public class WaterHeightSystem extends EntitySystem {

	private Environment environment;

	private ObjectSet<WaterAttribute> waterAttributes = new ObjectSet<>();

	private ComponentMapper<SceneModelInstance> sceneNodeMapper;

	/**
	 * Process the system.
	 */
	@Override
	protected void processSystem () {

		waterAttributes.clear();
		final Bag<Entity> entities = getEntities();
		for (Entity entity : entities) {
			final SceneModelInstance sceneNodeInstance = sceneNodeMapper.get(entity.getId());

			final SceneMaterial sceneMaterial = sceneNodeInstance.getNodes().first().getSceneMesh().getSceneMeshPrimtiveArray().first().sceneMaterial;

			final WaterAttribute waterAttribute = sceneMaterial.getAttributes().get(WaterAttribute.class, WaterAttribute.Water);
			waterAttributes.add(waterAttribute);
		}
	}

	public Vector3 getOceanHeight (float x, float z, Vector3 out) {
		Vector3 wavePosition = new Vector3(x, 0, z);

		out.set(0, 0, 0);
		float time = environment.getSceneEnvironment().getTime();

		for (WaterAttribute waterAttribute : waterAttributes) {
			final ShaderWaveInfo[] waves = waterAttribute.getWaves();

			Vector2 position = new Vector2(x, z);
			for (int i = 0; i < waves.length; i++) {
				final ShaderWaveInfo wave = waves[i];
				float projection = position.dot(wave.getNormedDirection());
				float phase = time * wave.speed[0];
				float theta = projection * wave.frequency[0] + phase;
				float height = wave.amplitude[0] * MathUtils.sin(theta);

				out.y += height;

				float maximumWidth = wave.steepness[0] * wave.amplitude[0];
				float width = maximumWidth * MathUtils.cos(theta);
				float xDir = wave.normedDirection.x;
				float yDir = wave.normedDirection.y;
				wavePosition.x += xDir * width;
				wavePosition.z += yDir * width;

			}
		}
		return out;
	}
}

package com.rockbite.bongo.engine.systems.render;

import com.artemis.BaseSystem;
import com.artemis.ComponentMapper;
import com.artemis.EntitySubscription;
import com.artemis.EntitySystem;
import com.artemis.annotations.All;
import com.artemis.utils.IntBag;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Cubemap;
import com.rockbite.bongo.engine.components.render.PointLight;
import com.rockbite.bongo.engine.components.singletons.Environment;
import com.rockbite.bongo.engine.gltf.scene.SceneEnvironment;
import lombok.Getter;

public class EnvironmentConfigSystem extends BaseSystem {

	//MAPPERS
	private ComponentMapper<PointLight> pointLightMapper;

	//SUBSCRIPTION
	@All(PointLight.class)
	private EntitySubscription pointLights;

	@Getter
	private Environment environment;

	@Override
	protected void initialize () {
		super.initialize();

		Cubemap cubemap = createCubemap("street");

		environment.getSceneEnvironment().setEnvMap(cubemap);
	}

	private Cubemap createCubemap (String path) {
		final FileHandle parent = Gdx.files.internal("cubemap/" + path);
		Cubemap cubemap = new Cubemap(
			parent.child("px.png"),
			parent.child("nx.png"),

			parent.child("py.png"),
			parent.child("ny.png"),

			parent.child("pz.png"),
			parent.child("nz.png")
		);

		return cubemap;
	}

	/**
	 * Process the system.
	 */
	@Override
	protected void processSystem () {

		SceneEnvironment sceneEnvironment = environment.getSceneEnvironment();
		sceneEnvironment.setTime(sceneEnvironment.getTime() + Gdx.graphics.getDeltaTime());

		sceneEnvironment.packFromRaw();


		sceneEnvironment.getPointLights().clear();

		//todo only put in the closest
		IntBag entities = pointLights.getEntities();
		int idx = 0;
		for (int i = 0; i < entities.size(); i++) {
			PointLight pointLight = pointLightMapper.get(entities.get(i));
			sceneEnvironment.getPointLights().add(pointLight);
			idx++;

			if (idx >= environment.getSceneEnvironment().getMaxPointLights()) break;
		}

		PointLight value = new PointLight();
		value.getWorldPosition().set(0, 0, 0);
		value.setRadiance(1f, 0, 0, 100f);
		sceneEnvironment.getPointLights().add(value);

	}
}

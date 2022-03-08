package com.rockbite.bongo.engine.systems.render;

import com.artemis.BaseSystem;
import com.artemis.ComponentMapper;
import com.artemis.EntityEdit;
import com.artemis.EntitySubscription;
import com.artemis.annotations.All;
import com.artemis.utils.IntBag;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Cubemap;
import com.badlogic.gdx.graphics.glutils.KTXTextureData;
import com.rockbite.bongo.engine.components.render.PointLight;
import com.rockbite.bongo.engine.components.render.Skybox;
import com.rockbite.bongo.engine.components.singletons.Environment;
import com.rockbite.bongo.engine.gltf.scene.SceneEnvironment;
import com.rockbite.bongo.engine.gltf.scene.SceneModel;
import com.rockbite.bongo.engine.gltf.scene.SceneModelInstance;
import com.rockbite.bongo.engine.meshutils.CubeUtils;
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

		int skybox = world.create();
		EntityEdit edit = world.edit(skybox);
		edit.create(Skybox.class);
		SceneModel skybox1 = CubeUtils.createBox("Skybox", 1, 1, 1, null);
		edit.add(new SceneModelInstance(skybox1));

//		environment.getSceneEnvironment().setSkyBox(createCubemap("field/skybox", "png"));
//		environment.getSceneEnvironment().setIrradianceMap(createCubemap("field/irradiance", "png"));
	}


	private static Cubemap createCubemap (String path, String extension) {
		final FileHandle parent = Gdx.files.internal("cubemap/" + path);
		Cubemap cubemap = new Cubemap(
			parent.child("px." + extension),
			parent.child("nx." + extension),

			parent.child("py." + extension),
			parent.child("ny." + extension),

			parent.child("pz." + extension),
			parent.child("nz." + extension)
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

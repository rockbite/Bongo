package com.rockbite.bongo.engine.systems;

import com.artemis.BaseSystem;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.rockbite.bongo.engine.components.singletons.Scenes;
import com.rockbite.bongo.engine.gltf.GLTFDataModel;
import com.rockbite.bongo.engine.gltf.scene.SceneGraph;
import com.rockbite.bongo.engine.gltf.scene.SceneResourceContext;
import lombok.Getter;

public class SceneLoaderSystem extends BaseSystem {


	@Getter
	private Scenes scenes;

	/**
	 * Process the system.
	 */
	@Override
	protected void processSystem () {

	}


	public void loadScene (FileHandle handle) {
		Json json = new Json();
		json.setIgnoreUnknownFields(true);
		final GLTFDataModel gltfModel = json.fromJson(GLTFDataModel.class, handle);

		SceneResourceContext sceneResourceContext = new SceneResourceContext();
		sceneResourceContext.loadFromDataModel(gltfModel);


		//load default scene
		SceneGraph sceneGraph = new SceneGraph(gltfModel.getScenes()[gltfModel.getScene()], gltfModel, sceneResourceContext);
		scenes.addScene(sceneGraph);
	}
}

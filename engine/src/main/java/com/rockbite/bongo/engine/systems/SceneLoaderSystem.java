package com.rockbite.bongo.engine.systems;

import com.artemis.BaseSystem;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.rockbite.bongo.engine.components.singletons.Scenes;
import com.rockbite.bongo.engine.gltf.GLTFDataModel;
import com.rockbite.bongo.engine.gltf.scene.SceneGraph;
import com.rockbite.bongo.engine.gltf.scene.SceneMeshVertexInfo;
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
		json.setSerializer(SceneMeshVertexInfo.AccessorType.class, new Json.Serializer<SceneMeshVertexInfo.AccessorType>() {
			@Override
			public void write (Json json, SceneMeshVertexInfo.AccessorType object, Class knownType) {

			}

			@Override
			public SceneMeshVertexInfo.AccessorType read (Json json, JsonValue jsonData, Class type) {
				return SceneMeshVertexInfo.AccessorType.getForTypeString(jsonData.asString());
			}
		});
		json.setSerializer(GLTFDataModel.ComponentType.class, new Json.Serializer<GLTFDataModel.ComponentType>() {
			@Override
			public void write (Json json, GLTFDataModel.ComponentType object, Class knownType) {

			}

			@Override
			public GLTFDataModel.ComponentType read (Json json, JsonValue jsonData, Class type) {
				return GLTFDataModel.ComponentType.getForInt(jsonData.asInt());
			}
		});

		json.setIgnoreUnknownFields(true);
		final GLTFDataModel gltfModel = json.fromJson(GLTFDataModel.class, handle);

		SceneResourceContext sceneResourceContext = new SceneResourceContext();
		sceneResourceContext.loadFromDataModel(gltfModel);


		//load default scene
		SceneGraph sceneGraph = new SceneGraph(gltfModel.getScenes()[gltfModel.getScene()], gltfModel, sceneResourceContext);
		scenes.addScene(sceneGraph);
	}
}

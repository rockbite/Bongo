package com.rockbite.bongo.engine.components.singletons;

import com.artemis.Component;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ObjectMap;
import com.rockbite.bongo.engine.gltf.scene.SceneGraph;
import com.rockbite.bongo.engine.gltf.scene.SceneModel;
import com.rockbite.bongo.engine.gltf.scene.SceneNode;
import com.rockbite.bongo.engine.gltf.scene.shader.SceneShaderProvider;
import lombok.Data;
import net.mostlyoriginal.api.Singleton;

@Data
@Singleton
public class Scenes extends Component {

	Array<SceneGraph> scenes = new Array<>();

	ObjectMap<String, SceneModel> nameNodeMap = new ObjectMap<>();

	SceneShaderProvider sceneShaderProvider;


	public void addScene (SceneGraph sceneGraph) {
		scenes.add(sceneGraph);

		nameNodeMap.putAll(sceneGraph.getNameModelMap());
	}

	public SceneModel getSceneNode (String name) {
		if (nameNodeMap.containsKey(name)) {
			return nameNodeMap.get(name);
		}

		throw new GdxRuntimeException("No scene node found for name: " + name);
	}

}

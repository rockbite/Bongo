package com.rockbite.bongo.engine.components.singletons;

import com.artemis.Component;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;
import com.rockbite.bongo.engine.gltf.scene.SceneEnvironment;
import lombok.Data;
import net.mostlyoriginal.api.Singleton;

@Data
@Singleton
public class Environment extends Component {

	private SceneEnvironment sceneEnvironment = new SceneEnvironment();

}

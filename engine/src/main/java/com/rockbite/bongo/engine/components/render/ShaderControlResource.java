package com.rockbite.bongo.engine.components.render;

import com.artemis.Component;
import com.rockbite.bongo.engine.gltf.scene.shader.BaseSceneShader;
import com.rockbite.bongo.engine.gltf.scene.shader.ShaderControlProvider;
import lombok.Data;

@Data
public class ShaderControlResource extends Component {

	private ShaderControlProvider shaderControlProvider;


}

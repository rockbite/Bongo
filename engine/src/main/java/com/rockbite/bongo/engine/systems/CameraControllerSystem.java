package com.rockbite.bongo.engine.systems;

import com.artemis.BaseSystem;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.g3d.utils.FirstPersonCameraController;
import com.rockbite.bongo.engine.components.singletons.Cameras;
import com.rockbite.bongo.engine.input.InputProvider;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.rockbite.bongo.engine.systems.RenderPassSystem.glViewport;

public class CameraControllerSystem extends BaseSystem implements InputProvider {

	static Logger logger = LoggerFactory.getLogger(CameraControllerSystem.class);

	//SINGLETONS
	@Getter
	private Cameras cameras;

	@Getter@Setter
	private InputAdapter cameraController;

	@Override
	protected void initialize () {
		super.initialize();
		cameraController = new FirstPersonCameraController(cameras.getGameCamera());
	}

	/**
	 * Process the system.
	 */
	@Override
	protected void processSystem () {
		if (cameraController instanceof FirstPersonCameraController) {
			((FirstPersonCameraController)cameraController).update();
		}
		if (cameraController instanceof CameraInputController) {
			((CameraInputController)cameraController).update();
		}
	}

	@Override
	public InputProcessor getInputProcessor () {
		return cameraController;
	}
}

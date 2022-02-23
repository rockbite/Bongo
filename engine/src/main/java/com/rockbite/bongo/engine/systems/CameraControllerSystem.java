package com.rockbite.bongo.engine.systems;

import com.artemis.BaseSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.g3d.utils.FirstPersonCameraController;
import com.rockbite.bongo.engine.components.singletons.Cameras;
import com.rockbite.bongo.engine.input.InputProvider;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CameraControllerSystem extends BaseSystem implements InputProvider {

	static Logger logger = LoggerFactory.getLogger(CameraControllerSystem.class);

	//SINGLETONS
	@Getter
	private Cameras cameras;

	@Getter
	private FirstPersonCameraController firstPersonCameraController;

	@Override
	protected void initialize () {
		super.initialize();
		firstPersonCameraController = new FirstPersonCameraController(cameras.getGameCamera());
	}

	/**
	 * Process the system.
	 */
	@Override
	protected void processSystem () {
		firstPersonCameraController.update();
	}

	@Override
	public InputProcessor getInputProcessor () {
		return firstPersonCameraController;
	}
}

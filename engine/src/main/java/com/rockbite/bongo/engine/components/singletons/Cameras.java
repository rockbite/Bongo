package com.rockbite.bongo.engine.components.singletons;

import com.artemis.Component;
import com.artemis.annotations.Transient;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Vector3;
import lombok.Getter;
import lombok.Setter;
import net.mostlyoriginal.api.Singleton;

@Getter
@Singleton
@Transient
public class Cameras extends Component {

	@Setter
	private Camera gameCamera;
	private OrthographicCamera screenspaceCamera;

	public Cameras () {
		gameCamera = new PerspectiveCamera(60, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		gameCamera.far = 20f;

		gameCamera = new OrthographicCamera(10, 10);


		screenspaceCamera = new OrthographicCamera();
	}
}

package com.rockbite.bongo.engine.components.singletons;

import com.artemis.Component;
import com.artemis.annotations.Transient;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Vector2;
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

	private static Vector3 temp3 = new Vector3();
	public void touchToWorldSpace (Vector2 temp) {
		touchToWorldSpace(temp3.set(temp.x, temp.y, 0));
		temp.set(temp3.x, temp3.y);
	}

	public void touchToWorldSpace (Vector3 temp) {
		//should probably be viewport
		gameCamera.unproject(temp);
	}

	public boolean isInGameViewport (Vector3 position) {
		float x = position.x;
		float y = position.y;

		//Size of 5
		float halfWidth = 2.5f;

		return gameCamera.frustum.boundsInFrustum(x, y, 0, halfWidth, halfWidth, 0);
	}
}

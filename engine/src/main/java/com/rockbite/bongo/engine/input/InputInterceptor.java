package com.rockbite.bongo.engine.input;

import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
import lombok.Setter;

public class InputInterceptor extends InputAdapter implements InputProvider {

	@Setter
	private boolean blockTouch;

	@Override
	public boolean touchDown (int screenX, int screenY, int pointer, int button) {
		if (blockTouch) return true;

		return super.touchDown(screenX, screenY, pointer, button);
	}

	@Override
	public boolean touchUp (int screenX, int screenY, int pointer, int button) {
		if (blockTouch) return true;

		return super.touchUp(screenX, screenY, pointer, button);
	}

	@Override
	public boolean touchDragged (int screenX, int screenY, int pointer) {
		if (blockTouch) return true;


		return super.touchDragged(screenX, screenY, pointer);
	}

	@Override
	public InputProcessor getInputProcessor () {
		return this;
	}
}

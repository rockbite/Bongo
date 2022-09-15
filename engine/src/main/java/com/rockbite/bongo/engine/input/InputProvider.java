package com.rockbite.bongo.engine.input;

import com.badlogic.gdx.InputProcessor;

public interface InputProvider  {

	InputProcessor getInputProcessor ();

	default int priority () {
		return 0;
	}
}

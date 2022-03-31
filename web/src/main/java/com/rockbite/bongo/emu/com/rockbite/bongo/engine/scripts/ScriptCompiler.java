package com.rockbite.bongo.engine.scripts;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.LifecycleListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScriptCompiler implements LifecycleListener {

	private static final Logger logger = LoggerFactory.getLogger(ScriptCompiler.class);

	private static ScriptCompiler instance;

	public static ScriptCompiler instance () {
		if (instance == null) {
			instance = new ScriptCompiler();
		}
		return instance;
	}


	public ScriptCompiler () {
		Gdx.app.addLifecycleListener(this);
	}

	public void compile (String fileName, String javaString, ScriptObjectRunnable scriptObjectRunnable) {
	}

	@Override
	public void pause () {

	}

	@Override
	public void resume () {

	}

	@Override
	public void dispose () {
	}


}

package com.rockbite.bongo.engine;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.LifecycleListener;
import com.rockbite.bongo.engine.fileutil.ReloadUtils;
import com.rockbite.bongo.engine.threadutil.ThreadUtils;

public class Bongo {

	public static final Boolean DEBUG = true;

	public static void init () {
		ThreadUtils.setGdxThread(Thread.currentThread());

		Gdx.app.addLifecycleListener(new LifecycleListener() {
			@Override
			public void pause () {

			}

			@Override
			public void resume () {

			}

			@Override
			public void dispose () {
				ReloadUtils.dispose();
			}
		});
	}
}

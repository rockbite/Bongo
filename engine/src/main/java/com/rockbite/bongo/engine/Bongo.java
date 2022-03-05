package com.rockbite.bongo.engine;

import com.artemis.utils.reflect.ClassReflection;
import com.artemis.utils.reflect.ReflectionException;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.LifecycleListener;
import com.rockbite.bongo.engine.fileutil.ReloadUtils;
import com.rockbite.bongo.engine.platform.IMGUIPlatform;
import com.rockbite.bongo.engine.render.ShaderSourceProvider;
import com.rockbite.bongo.engine.threadutil.ThreadUtils;

public class Bongo {

	public static Boolean DEBUG = true;
	public static Boolean CORE_SHADER_DEBUG = false;

	public static IMGUIPlatform imguiPlatform;


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

		initPlatform();
	}

	private static void initPlatform () {
		try {
			imguiPlatform = (IMGUIPlatform)ClassReflection.newInstance(ClassReflection.forName("com.rockbite.bongo.engine.platform.IMGUIPlatformImpl"));
		} catch (ReflectionException e) {
			e.printStackTrace();
		}
	}
}

package com.rockbite.bongo.engine.threadutil;

import com.badlogic.gdx.utils.GdxRuntimeException;
import com.rockbite.bongo.engine.Bongo;
import lombok.Setter;

public class ThreadUtils {

	private static Thread gdxThread;

	public static void init () {
		gdxThread = Thread.currentThread();
	}
	
	public static void gdxThreadSafetyCheck () {
		if (Bongo.DEBUG) {
			if (!isOnRightThread()) {
				throw new GdxRuntimeException("Trying to use on non-gdx thread");
			}
		}
	}

	public static void gdxThreadSafetyCheck (Object accessThread) {
		if (Bongo.DEBUG) {
			if (Thread.currentThread() != accessThread) {
				throw new GdxRuntimeException("Trying to use on non-gdx thread");
			}
		}
	}

	public static boolean isOnRightThread () {
		if (Bongo.DEBUG) {
			return Thread.currentThread() == gdxThread;
		}

		return true;
	}

	public static void clearStatics () {
		gdxThread = null;
	}
}

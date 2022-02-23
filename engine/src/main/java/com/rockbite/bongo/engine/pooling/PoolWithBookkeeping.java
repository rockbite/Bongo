package com.rockbite.bongo.engine.pooling;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ObjectSet;
import com.badlogic.gdx.utils.Pool;
import com.rockbite.bongo.engine.Bongo;
import com.rockbite.bongo.engine.threadutil.ThreadUtils;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Field;

public abstract class PoolWithBookkeeping<T> extends Pool<T> {

	@Getter
	final Array<T> existingInstances = new Array<>();

	private static final int SIZE_WARN_THRESHOLD = 1000;

	private boolean customPoolThreshold;
	private int customThreshold;


	public PoolWithBookkeeping<T> setCustomThreshold (int threshold) {
		this.customThreshold = threshold;
		customPoolThreshold = true;
		return this;
	}


	@Getter
	private String poolName;

	@Getter @Setter
	Thread accessThread;

	@Deprecated
	public PoolWithBookkeeping () {
		throw new GdxRuntimeException("Use Constructor with poolName");
	}

	public PoolWithBookkeeping (int initialCapacity) {
		throw new GdxRuntimeException("Use Constructor with poolName");
	}

	public PoolWithBookkeeping (int initialCapacity, int max) {
		throw new GdxRuntimeException("Use Constructor with poolName");
	}

	public PoolWithBookkeeping (String poolName) {
		super();
		this.poolName = poolName;
	}

	public PoolWithBookkeeping (String poolName, int initialCapacity) {
		super(initialCapacity);
		this.poolName = poolName;
	}

	public PoolWithBookkeeping (String poolName, int initialCapacity, int max) {
		super(initialCapacity, max);
		this.poolName = poolName;
	}

	private Array<T> freeObjectsArray = null;
	public boolean contains (Pool<T> pool, T component) {
		try {
			if (freeObjectsArray == null) {
				Field freeObjects = Pool.class.getDeclaredField("freeObjects");
				freeObjects.setAccessible(true);
				freeObjectsArray = (Array<T>)freeObjects.get(pool);
			}
			if (freeObjectsArray.contains(component, true)) {
				return true;
			}
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}

		return false;
	}

	@Override
	public void free(T object) {
		super.free(object);
		if (Bongo.DEBUG) {

			if (Gdx.app != null) {
				//We are Libgdx

				if (accessThread != null) {
					ThreadUtils.gdxThreadSafetyCheck(accessThread);
				} else{
					ThreadUtils.gdxThreadSafetyCheck();
				}
			}
		}
	}

	public T obtain () {
		boolean creatingNewObject = getFree() == 0;
		final T obtain = super.obtain();
		if (Bongo.DEBUG) {

			if (Gdx.app != null && !Application.ApplicationType.HeadlessDesktop.equals(Gdx.app.getType())) {
				//We are Libgdx


				if (accessThread != null) {
					ThreadUtils.gdxThreadSafetyCheck(accessThread);
				} else{
					ThreadUtils.gdxThreadSafetyCheck();
				}
			}

			//We have added a new instance, lets add it to the existing instances count
			if (creatingNewObject) {
				existingInstances.add(obtain);

				if (customPoolThreshold) {
					if (existingInstances.size > customThreshold) {
						System.err.println("Existing instances: " + existingInstances.size + " for pool: " + poolName);
					}
				} else {
					if (existingInstances.size > SIZE_WARN_THRESHOLD) {
						System.err.println("Existing instances: " + existingInstances.size + " for pool: " + poolName);
					}
				}
			}
		}
		return obtain;
	}
}

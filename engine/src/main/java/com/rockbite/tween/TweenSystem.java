package com.rockbite.tween;

import com.artemis.BaseSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.rockbite.bongo.engine.pooling.PoolWithBookkeeping;
import com.rockbite.tween.controllers.*;

import java.util.Iterator;

public class TweenSystem extends BaseSystem {


	public static final TweenGroup DEFAULT = new TweenGroup("DEFAULT");

	public static class TweenGroup {
		static int GLOBAL_ID;

		private final String name;
		private final int id;

		TweenGroup (String name) {
			this.name = name;
			this.id = GLOBAL_ID++;
		}
	}

	public static TweenGroup createGroup (String group) {
		return new TweenGroup(group);
	}

	private TweenGroup currentGroup = DEFAULT;

	public void enableGrouping(TweenGroup tweenGroup) {
		currentGroup = tweenGroup;
	}

	public void disableGrouping() {
		currentGroup = DEFAULT;
	}

	class TweenControllerMap {
		private ObjectMap<Class, TweenController> controllers = new ObjectMap<>();
		private ObjectMap<Class, TweenMask[]> tweenMasks = new ObjectMap<>();

		@SafeVarargs
		final <T> void register (Class<T> clazz, TweenController<? super T> controller, TweenMask<? super T>... masks) {
			controllers.put(clazz, controller);
			tweenMasks.put(clazz, masks);
			for (TweenMask<? super T> mask : masks) {
				controller.masks.put(mask.getMask(), (TweenMask)mask);
			}
		}

		<T> TweenController<T> getController (Class<T> clazz) {
			TweenController tweenController = controllers.get(clazz);
			if (tweenController == null) {
				throw new GdxRuntimeException("No TweenController found for class: " + clazz);
			}
			return tweenController;
		}


		<T> TweenController<T> getControllerUnsafe (Class<T> clazz) {
			TweenController tweenController = controllers.get(clazz);
			return tweenController;
		}

		<T> TweenMask<T>[] getMasks (Class<T> clazz) {
			TweenMask[] masks = tweenMasks.get(clazz);
			if (masks == null) {
				throw new GdxRuntimeException("No masks found for class: " + clazz);
			}
			return masks;
		}
	}

	static class TweenPoolMap {
		private ObjectMap<Class, PoolWithBookkeeping<Tween<?>>> pools = new ObjectMap<>();
		@SuppressWarnings("unchecked")
		<T> void register (Class<T> clazz, PoolWithBookkeeping<Tween<T>> pool) {
			pools.put(clazz, (PoolWithBookkeeping)pool);
		}
		@SuppressWarnings("unchecked")
		<T> PoolWithBookkeeping<Tween<T>> getPool (Class<T> clazz) {
			return (PoolWithBookkeeping)pools.get(clazz);
		}
	}

	class TweenPool {
		TweenPoolMap tweenPoolMap = new TweenPoolMap();

		<T> Tween<T> obtainTween (Class<T> type) {
			PoolWithBookkeeping<Tween<T>> tweenPool = this.tweenPoolMap.getPool(type);
			if (tweenPool == null) {
				tweenPool = new PoolWithBookkeeping<Tween<T>>("TweenPool: " + type.getSimpleName()) {
					@Override
					protected Tween<T> newObject () {
						if (ClassReflection.isAssignableFrom(TweenController.class, type)) {
							return new Tween<>(TweenSystem.this, null);
						} else {
							return new Tween<>(TweenSystem.this, tweenControllerMap.getController(type));
						}
					}
				};
				tweenPoolMap.register(type, tweenPool);
			}
			return tweenPool.obtain();
		}

		public <T> void freeTween (Class<T> clazz, Tween<T> tween) {
			this.tweenPoolMap.getPool(clazz).free(tween);
		}
	}

	private TweenControllerMap tweenControllerMap = new TweenControllerMap();
	private TweenPool tweenPool = new TweenPool();

	private Array<Tween> runningTweens = new Array<>();
	private Array<Tween> completedTweens = new Array<>();
	private Array<Tween> delayedAddTweens = new Array<>();

	public TweenSystem () {
		tweenControllerMap.register(Vector3.class, new Vector3TweenController(), Vector3TweenController.MOVE_TO_X, Vector3TweenController.MOVE_TO_Y, Vector3TweenController.MOVE_TO_Z, Vector3TweenController.MOVE_TO_XYZ);
		tweenControllerMap.register(Vector2.class, new Vector2TweenController(), Vector2TweenController.MOVE_TO_X, Vector2TweenController.MOVE_TO_Y, Vector2TweenController.MOVE_TO_XY);
		tweenControllerMap.register(Rectangle.class, new RectangleTweenController(), RectangleTweenController.MOVE_TO);
		tweenControllerMap.register(ShakeTweenController.ShakeConfiguration.class, new ShakeTweenController(), ShakeTweenController.DEFAULT_SHAKE);
	}

	public <T> void register (Class<T> targetClazz, TweenController<T> tweenController, TweenMask<T>... masks) {
		tweenControllerMap.register(targetClazz, tweenController, masks);
	}


	public boolean isTweenableType (Class<?> type) {
		TweenController<?> controller = tweenControllerMap.getControllerUnsafe(type);
		return controller != null;
	}

	public <T> TweenController<T> getController (Class<T> clazz) {
		return tweenControllerMap.getController(clazz);
	}

	public <T> TweenMask<?>[] getTweenMasks (Class<T> clazz) {
		return tweenControllerMap.getMasks(clazz);
	}

	public <T> Tween<T> Tween (T target, TweenMask<T> mask) {
		Class<T> clazz = resolveClass(target);
		return tweenPool.obtainTween(clazz).mask(mask).objectTarget(target).setGroup(currentGroup);
	}

	private static Vector3 dummy = new Vector3();
	public <T> Tween<T> delay (float delaySeconds) {
		final Tween<T> tween = (Tween<T>)Tween(dummy, Vector3TweenController.MOVE_TO_X);
		tween.duration(delaySeconds);
		return tween;
	}

	public <T> Tween<T> runnable (TweenCompletionListener runnable) {
		final Tween<T> tween = (Tween<T>)Tween(dummy, Vector3TweenController.MOVE_TO_X);
		tween.completion(runnable);
		return tween;
	}

	public Tween sequenceReturnFirst (Tween tween1, Tween tween2) {
		tween2.after(tween1);
		return tween1;
	}

	public Tween sequenceReturnFirst (Tween tween1, Tween tween2, Tween tween3) {
		tween2.after(tween1);
		tween3.after(tween2);
		return tween1;
	}

	public Tween sequenceReturnFirst (Tween tween1, Tween tween2, Tween tween3, Tween tween4) {
		tween2.after(tween1);
		tween3.after(tween2);
		tween4.after(tween3);
		return tween1;
	}

	public Tween sequenceReturnFirst (Tween tween1, Tween tween2, Tween tween3, Tween tween4, Tween tween5) {
		tween2.after(tween1);
		tween3.after(tween2);
		tween4.after(tween3);
		tween5.after(tween4);
		return tween1;
	}

	public Tween sequenceReturnFirst (Tween tween1, Tween tween2, Tween tween3, Tween tween4, Tween tween5, Tween tween6) {
		tween2.after(tween1);
		tween3.after(tween2);
		tween4.after(tween3);
		tween5.after(tween4);
		tween6.after(tween5);

		return tween1;
	}

	public Tween sequenceReturnFirst (Tween tween1, Tween tween2, Tween tween3, Tween tween4, Tween tween5, Tween tween6, Tween tween7) {
		tween2.after(tween1);
		tween3.after(tween2);
		tween4.after(tween3);
		tween5.after(tween4);
		tween6.after(tween5);
		tween7.after(tween6);

		return tween1;
	}

	public Tween sequenceReturnFirst (Tween tween1, Tween tween2, Tween tween3, Tween tween4, Tween tween5, Tween tween6, Tween tween7, Tween tween8) {
		tween2.after(tween1);
		tween3.after(tween2);
		tween4.after(tween3);
		tween5.after(tween4);
		tween6.after(tween5);
		tween7.after(tween6);
		tween8.after(tween7);

		return tween1;
	}

	public Tween sequenceReturnFirst (Tween tween1, Tween tween2, Tween tween3, Tween tween4, Tween tween5, Tween tween6, Tween tween7, Tween tween8, Tween tween9, Tween tween10) {
		tween2.after(tween1);
		tween3.after(tween2);
		tween4.after(tween3);
		tween5.after(tween4);
		tween6.after(tween5);
		tween7.after(tween6);
		tween8.after(tween7);
		tween9.after(tween8);
		tween10.after(tween9);

		return tween1;
	}

	private <T> void free (Tween<T> tween) {
		T targetClazz = tween.getTarget();
		Class<T> clazz = resolveClass(targetClazz);
		tweenPool.freeTween(clazz, tween);
	}

	public void startTweenNow (Tween tween) {
		tween.start();
		runningTweens.add(tween);
	}

	public void update (float deltaTime) {
		for (Tween tween : runningTweens) {
			boolean complete = tween.update(deltaTime);
			if (complete) {
				Array<Tween> tweensToRunAfter = tween.getTweensToRunAfter();
				for (int i = 0; i < tweensToRunAfter.size; i++) {
					Tween child = tweensToRunAfter.get(i);
					delayedAddTweens.add(child);
				}
				completedTweens.add(tween);
			}
		}

		for (Tween delayedAddTween : delayedAddTweens) {
			startTweenNow(delayedAddTween);
		}
		delayedAddTweens.clear();
		for (Tween completedTween : completedTweens) {
			free(completedTween);
			runningTweens.removeValue(completedTween, true);
		}
		completedTweens.clear();
	}

	@SuppressWarnings("unchecked")
	private <T> Class<T> resolveClass (T target) {
		return (Class<T>)target.getClass();
	}

	/**
	 * Process the system.
	 */
	@Override
	protected void processSystem () {
		update(Gdx.graphics.getDeltaTime());

	}

	public void stopRunningTweens (TweenGroup group) {
		for (Iterator<Tween> iterator = runningTweens.iterator(); iterator.hasNext(); ) {
			Tween runningTween = iterator.next();
			if (runningTween.getGroup() == group) {
				// must terminate
				iterator.remove();
//				completedTweens.add(runningTween);
			}
		}
	}

}

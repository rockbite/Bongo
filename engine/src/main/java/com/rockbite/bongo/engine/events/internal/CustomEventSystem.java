package com.rockbite.bongo.engine.events.internal;

import com.artemis.BaseSystem;
import com.artemis.utils.reflect.ClassReflection;
import com.artemis.utils.reflect.ReflectionException;
import com.badlogic.gdx.utils.ObjectMap;
import com.rockbite.bongo.engine.pooling.PoolWithBookkeeping;
import net.mostlyoriginal.api.event.common.Event;

import java.util.List;

/**
 * Listener registration and event dispatch from within artemis.
 * <p>
 * Will scan all systems and managers for @Subscribe annotation
 * at initialization.
 *
 * @author Daan van Yperen
 */
public class CustomEventSystem extends BaseSystem {

	private CustomEventDispatchStrategy dispatcherStrategy;
	private CustomListenerFinderStrategy listenerFinderStrategy;

	/**
	 * Init CustomEventSystem with default strategies.
	 */
	public CustomEventSystem () {
		this(new CustomFastEventDispatcher(), new CustomSubscribeAnnotationFinder());
	}

	/**
	 * Init CustomEventSystem with custom strategies.
	 *
	 * @param dispatcherStrategy     Strategy to use for dispatching events.
	 * @param listenerFinderStrategy Strategy to use for finding listeners on objects.
	 */
	public CustomEventSystem (CustomEventDispatchStrategy dispatcherStrategy, CustomListenerFinderStrategy listenerFinderStrategy) {
		this.dispatcherStrategy = dispatcherStrategy;
		this.listenerFinderStrategy = listenerFinderStrategy;
	}

	@Override
	protected void initialize () {
		// register events for all systems and managers.
		registerAllSystemEvents();
	}

	/**
	 * Resolve all listeners.
	 */
	protected List<CustomEventListenerAbstraction> resolveListeners (Object o) {
		return listenerFinderStrategy.resolve(o);
	}

	/**
	 * Register all @Subscribe listeners in passed object (typically system, manager).
	 */
	public void registerEvents (Object o) {
		registerAll(o, resolveListeners(o));
	}

	public void unregisterEventsForOwner (Object o) {
		dispatcherStrategy.unregisterEventsForOwner(o);
	}

	/**
	 * Dispatch event to registered listeners.
	 */
	public void dispatch (Event event) {
		dispatcherStrategy.dispatch(event);
		freeEvent(event);
	}

	@Override
	protected void processSystem () {
		dispatcherStrategy.process();
	}

	/**
	 * Register all listeners with the handler.
	 */
	private void registerAll (Object owner, List<CustomEventListenerAbstraction> listeners) {
		for (CustomEventListenerAbstraction listener : listeners) {
			dispatcherStrategy.register(owner, listener);
		}

		if (owner instanceof CustomFunctionEventListener) {
			dispatcherStrategy.register(owner, (CustomFunctionEventListener)owner);
		}
	}

	/**
	 * Register all systems in this world.
	 */
	private void registerAllSystemEvents () {
		for (BaseSystem entitySystem : world.getSystems()) {
			registerEvents(entitySystem);
		}
	}

	private static class PoolMaps {

		private ObjectMap<Class<? extends Event>, PoolWithBookkeeping<Event>> pools = new ObjectMap<>();

		<T extends Event> void free (T event) {
			PoolWithBookkeeping<Event> eventPoolWithBookkeeping = pools.get(event.getClass());
			eventPoolWithBookkeeping.free(event);
		}
		<T extends Event> void register (Class<T> clazz) {
			pools.put(clazz, new PoolWithBookkeeping<Event>(clazz.getSimpleName()) {
				@Override
				protected Event newObject () {
					try {
						return ClassReflection.newInstance(clazz);
					} catch (ReflectionException e) {
						throw new RuntimeException(e);
					}
				}
			});
		}

		public <T extends Event> boolean has (Class<T> clazz) {
			return pools.containsKey(clazz);
		}


		@SuppressWarnings("unchecked")
		public <T extends Event> T obtain (Class<T> clazz) {
			if (!has(clazz)) {
				register(clazz);
			}
			return (T)pools.get(clazz).obtain();
		}
	}

	private PoolMaps pools = new PoolMaps();

	private <T extends Event> void freeEvent (T event) {
		pools.free(event);
	}

	public <T extends Event> T obtainEvent (Class<T> clazz) {
		return pools.obtain(clazz);
	}

}

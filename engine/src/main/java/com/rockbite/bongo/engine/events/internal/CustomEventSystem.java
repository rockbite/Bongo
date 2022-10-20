package com.rockbite.bongo.engine.events.internal;

import com.artemis.BaseSystem;
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
	}

	/**
	 * Queue an event to dispatch synchronously.
	 */
	public <T extends Event> T dispatch (Class<T> eventType) {
		return dispatcherStrategy.dispatch(eventType);
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
	}

	/**
	 * Register all systems in this world.
	 */
	private void registerAllSystemEvents () {
		for (BaseSystem entitySystem : world.getSystems()) {
			registerEvents(entitySystem);
		}
	}

}

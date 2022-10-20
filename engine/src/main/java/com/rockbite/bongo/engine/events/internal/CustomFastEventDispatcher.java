package com.rockbite.bongo.engine.events.internal;

import com.artemis.utils.Bag;

import net.mostlyoriginal.api.event.common.Event;
import net.mostlyoriginal.api.utils.ClassHierarchy;
import net.mostlyoriginal.api.utils.BagUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.IdentityHashMap;

/**
 * Faster event dispatcher.
 * <p>
 * Should suffice for most prototyping usecases.
 *
 * @Author DaanVanYperen, Tomski
 */
public class CustomFastEventDispatcher implements CustomEventDispatchStrategy {

	private static final Logger logger = LoggerFactory.getLogger(CustomFastEventDispatcher.class);

	final ClassHierarchy classHierarchy = new ClassHierarchy();

	/**
	 * Listeners of exact event class. Excludes superclasses.
	 */
	final IdentityHashMap<Class<?>, Bag<CustomEventListenerAbstraction>> listenerCache = new IdentityHashMap<>();

	/**
	 * Listeners flattened to include full hierarchy per calling event.
	 */
	final IdentityHashMap<Class<?>, Bag<CustomEventListenerAbstraction>> hierarchicalListenerCache = new IdentityHashMap<>();

	private IdentityHashMap<Object, Bag<CustomEventListenerAbstraction>> objectOwnerToListenersMap = new IdentityHashMap<>();

	@Override
	public void register (Object owner, CustomEventListenerAbstraction listener) {
		if (listener == null)
			throw new NullPointerException("Listener required.");

		// Bind listener to the related event class.
		Bag<CustomEventListenerAbstraction> listenersFor = getListenersFor(listener.getParameterType(), true);
		if (!listenersFor.contains(listener)) {

			if (!objectOwnerToListenersMap.containsKey(owner)) {
				objectOwnerToListenersMap.put(owner, new Bag<>());
			}

			objectOwnerToListenersMap.get(owner).add(listener);

			listenersFor.add(listener);
			// the hierarchical cache is now out of date. purrrrrrrrge!
			invalidateHierarchicalCache();
		}

	}

	@Override
	public void unregisterEventsForOwner (Object owner) {
		Bag<CustomEventListenerAbstraction> customEventListenerAbstractions = objectOwnerToListenersMap.get(owner);

		if (customEventListenerAbstractions == null) {
			logger.error("Trying to remove listeners but not found for owner {}", owner);
			return;
		}

		for (CustomEventListenerAbstraction customEventListenerAbstraction : customEventListenerAbstractions) {
			Bag<CustomEventListenerAbstraction> listenersFor = getListenersFor(customEventListenerAbstraction.getParameterType(), false);

			if (listenersFor != null) {
				listenersFor.remove(customEventListenerAbstraction);
			} else {
				logger.error("No listeners found for abstraction {}", customEventListenerAbstraction);
			}
		}

		objectOwnerToListenersMap.remove(owner);

		invalidateHierarchicalCache();
	}

	private void invalidateHierarchicalCache () {
		if (hierarchicalListenerCache.size() > 0) {
			hierarchicalListenerCache.clear();
		}
	}

	/**
	 * Get listeners for class (non hierarical).
	 *
	 * @param aClass          Class to fetch listeners for.
	 * @param createIfMissing instance empty bag when not exist.
	 * @return Listener, or <code>null</code> if missing and not allowed to create.
	 */
	protected Bag<CustomEventListenerAbstraction> getListenersFor (Class<?> aClass, boolean createIfMissing) {
		Bag<CustomEventListenerAbstraction> listeners = listenerCache.get(aClass);
		if (listeners == null && createIfMissing) {
			// if listener is missing, prep an empty bag.
			listeners = new Bag<>(4);
			listenerCache.put(aClass, listeners);
		}
		return listeners;
	}

	/**
	 * Get listeners for class, including all superclasses.
	 * Backed by cache.
	 * <p>
	 * Not sorted!
	 *
	 * @param aClass Class to fetch listeners for.
	 * @return Bag of listeners, empty if none found.
	 */
	protected Bag<CustomEventListenerAbstraction> getListenersForHierarchical (Class<?> aClass) {
		Bag<CustomEventListenerAbstraction> listeners = hierarchicalListenerCache.get(aClass);
		if (listeners == null) {
			listeners = getListenersForHierarchicalUncached(aClass);

			// presort the listeners by priority.
			// Should speed things up in the case of an oft reused superclass.
			BagUtils.sort(listeners);

			hierarchicalListenerCache.put(aClass, listeners);
		}
		return listeners;
	}

	/**
	 * Get listeners for class, including all superclasses,
	 * sorted by priority.
	 * <p>
	 * Not backed by cache.
	 *
	 * @param aClass Class to fetch listeners for.
	 * @return Bag of listeners, empty if none found.
	 */
	private Bag<CustomEventListenerAbstraction> getListenersForHierarchicalUncached (Class<?> aClass) {

		// get hierarchy for event.
		final Class<?>[] classes = classHierarchy.of(aClass);

		// step through hierarchy back to front, fetching the listeners for each step.
		final Bag<CustomEventListenerAbstraction> hierarchicalListeners = new Bag<>(4);
		for (Class<?> c : classes) {
			final Bag<CustomEventListenerAbstraction> listeners = getListenersFor(c, false);
			if (listeners != null) {
				hierarchicalListeners.addAll(listeners);
			}
		}

		// sort by priority.
		BagUtils.sort(hierarchicalListeners);

		return hierarchicalListeners;
	}

	Bag<CustomEventListenerAbstraction> removalArray = new Bag<>();
	/**
	 * Dispatch event to registered listeners.
	 * Events are called on the call stack, avoid deeply nested or circular event calls.
	 */
	@Override
	public void dispatch (Event event) {
		if (event == null)
			throw new NullPointerException("Event required.");

		final Bag<CustomEventListenerAbstraction> listeners = getListenersForHierarchical(event.getClass());

		removalArray.clear();

		/** Fetch hierarchical list of listeners. */
		Object[] data = listeners.getData();
		for (int i = 0, s = listeners.size(); i < s; i++) {
			final CustomEventListenerAbstraction listener = (CustomEventListenerAbstraction)data[i];
			if (listener != null) {
				boolean shouldDispose = listener.handle(event);
				if (shouldDispose) {
					//Remove it
					removalArray.add(listener);
				}
			}
		}

		if (removalArray.size() > 0) {
			logger.info("Removing one shot listeners");
			listeners.removeAll(removalArray);
		}
	}

	@Override
	public void process () {
		// not interested in this stuff
	}

	@Override
	public <T extends Event> T dispatch (Class<T> type) {
		throw new UnsupportedOperationException("This dispatcher doesn't dispatch events by type!");
	}

}

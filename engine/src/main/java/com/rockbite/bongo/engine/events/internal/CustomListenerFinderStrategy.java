package com.rockbite.bongo.engine.events.internal;

import java.util.List;

/**
 * Helper that resolves listener on entity.
 *
 * @author Daan van Yperen
 */
public interface CustomListenerFinderStrategy {

	/**
	 * Find all listeners in o and return as EventListeners.
	 */
	public List<CustomEventListenerAbstraction> resolve (Object o);
}

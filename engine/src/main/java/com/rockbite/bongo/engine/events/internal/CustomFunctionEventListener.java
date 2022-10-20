package com.rockbite.bongo.engine.events.internal;

import net.mostlyoriginal.api.event.common.Event;

public abstract class CustomFunctionEventListener<T extends Event> implements CustomEventListenerAbstraction {

	private final Class<T> clazz;
	private final int priority;
	private final boolean skipCancelledEvents;

	public abstract boolean evaluate (T event);


	public CustomFunctionEventListener (Class<T> clazz, int priority, boolean skipCancelled) {
		this.clazz = clazz;
		this.priority = priority;
		this.skipCancelledEvents = skipCancelled;
	}

	public CustomFunctionEventListener (Class<T> clazz, int priority) {
		this(clazz, priority, false);
	}

	public CustomFunctionEventListener (Class<T> clazz) {
		this(clazz, 0, false);
	}


	@Override
	public Object owner () {
		return this;
	}

	@Override
	public boolean handle (Event event) {
		return evaluate((T)event);
	}

	@Override
	public int priority () {
		return priority;
	}

	@Override
	public boolean skipCancelledEvents () {
		return skipCancelledEvents;
	}

	@Override
	public Class<?> getParameterType () {
		return clazz;
	}

	@Override
	public int compareTo (CustomEventListenerAbstraction o) {
		// Sort by priority descending.
		return o.priority() - priority;
	}
}

package com.rockbite.bongo.engine.events.internal;

import net.mostlyoriginal.api.event.common.Event;

public interface CustomEventListenerAbstraction {

	Object owner ();

	boolean handle (Event event);

	int priority ();

	boolean skipCancelledEvents ();

	Class<?> getParameterType ();
}

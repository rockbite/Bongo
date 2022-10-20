package com.rockbite.bongo.engine.events.internal;

import com.artemis.utils.reflect.ClassReflection;
import com.artemis.utils.reflect.Method;
import net.mostlyoriginal.api.event.common.Cancellable;
import net.mostlyoriginal.api.event.common.Event;
import net.mostlyoriginal.api.utils.ReflectionHelper;

/**
 * @author Daan van Yperen
 * @todo GWT provide method support.
 */
public class CustomMethodBasedEventListener implements CustomEventListenerAbstraction {

	protected final Object object;
	protected final Method method;
	protected final Class parameterType;
	protected final int priority;
	protected final boolean skipCancelledEvents;

	/**
	 * Instance event listener.
	 * <p>
	 * Default priority 0, does not skip cancelled events.
	 *
	 * @param object Object that contains event handler method.
	 * @param method Event handler method.
	 */
	public CustomMethodBasedEventListener (Object object, Method method) {
		this(object, method, 0, false);
	}

	/**
	 * Instance event listener.
	 *
	 * @param object              Object that contains event handler method.
	 * @param method              Event handler method.
	 * @param priority            Precedence over other handlers. Higher values get called first.
	 * @param skipCancelledEvents if <code>true</code>, cancelled events skip this event listener. <code>false</code>
	 */
	public CustomMethodBasedEventListener (Object object, Method method, int priority, boolean skipCancelledEvents) {
		this.priority = priority;
		this.skipCancelledEvents = skipCancelledEvents;
		if (object == null)
			throw new NullPointerException("Object cannot be null.");
		if (method == null)
			throw new NullPointerException("Method cannot be null.");
		method.setAccessible(true);
		if (method.getParameterTypes().length != 1)
			throw new IllegalArgumentException("Listener methods must have exactly one parameter.");
		this.parameterType = ReflectionHelper.getFirstParameterType(method);
		if (parameterType == Event.class)
			throw new IllegalArgumentException("Parameter class cannot be Event, must be subclass.");
		if (!ClassReflection.isAssignableFrom(Event.class, parameterType))
			throw new IllegalArgumentException("Invalid parameter class. Listener method parameter must extend " + Event.class.getName() + ".");
		this.object = object;
		this.method = method;
	}

	@Override
	public Object owner () {
		return object;
	}

	public boolean handle (Event event) {
		if (event == null)
			throw new NullPointerException("Event required.");

		if (skipCancelledEvents) {
			if (ClassReflection.isInstance(Cancellable.class, event) && ((Cancellable)event).isCancelled()) {
				// event can be cancelled, so do not submit!
				return false;
			}
		}

		try {
			method.invoke(object, event);
		} catch (Exception e) {
			throw new RuntimeException("Could not call event.", e);
		}

		return false;
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
		return parameterType;
	}

	/**
	 * Object that contains method.
	 */
	public Object getObject () {
		return object;
	}

	/**
	 * Method of listener.
	 */
	public Method getMethod () {
		return method;
	}

	@Override
	public int compareTo (CustomEventListenerAbstraction o) {
		// Sort by priority descending.
		return o.priority() - priority;
	}
}

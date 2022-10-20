package com.rockbite.bongo.engine.events.internal;

import com.artemis.utils.reflect.Annotation;
import com.artemis.utils.reflect.ClassReflection;
import com.artemis.utils.reflect.Method;
import net.mostlyoriginal.api.event.common.Subscribe;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Daan van Yperen
 */
public class CustomSubscribeAnnotationFinder implements CustomListenerFinderStrategy {

	@Override
	/** Find all listeners in o based on @Subscribe annotation and return as EventListeners. */
	public List<CustomEventListenerAbstraction> resolve(Object o) {

		final ArrayList<CustomEventListenerAbstraction> listeners = new ArrayList<>();

		for (Method method : ClassReflection.getDeclaredMethods(o.getClass())) {
			if (method.isAnnotationPresent(Subscribe.class)) {
				final Annotation declaredAnnotation = method.getDeclaredAnnotation(Subscribe.class);
				if (declaredAnnotation != null) {
					final Subscribe subscribe = declaredAnnotation.getAnnotation(Subscribe.class);
					listeners.add(new CustomMethodBasedEventListener(o, method, subscribe.priority(), subscribe.ignoreCancelledEvents()));
				}
			}
		}

		return listeners;
	}
}

package com.rockbite.bongo.engine.plugins;

import com.artemis.*;
import com.artemis.EntitySubscription.SubscriptionListener;
import com.artemis.annotations.UnstableApi;
import com.artemis.injection.FieldResolver;
import com.artemis.utils.Bag;
import com.artemis.utils.IntBag;
import com.artemis.utils.reflect.ClassReflection;
import com.artemis.utils.reflect.Field;
import com.artemis.utils.reflect.ReflectionException;
import com.badlogic.gdx.utils.Array;
import net.mostlyoriginal.api.Singleton;
import net.mostlyoriginal.api.SingletonException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static com.artemis.utils.reflect.ClassReflection.isAnnotationPresent;

/**
 * Dependency injection for singleton components. Creates a singleton component upon injection.
 *
 * <p>
 * Takes full responsibility for singleton component lifecycle; do not manage annotated
 * singleton components yourself.</p>
 *
 * <p>
 * By default the plugin runs in strict mode, throwing a {@link SingletonException}
 * when an entity enters the world with a {@link Singleton @Singleton} component.</p>
 *
 * <p>This behaviour can be disabled using {@link #SingletonCustomPlugin(boolean) new SingletonPlugin(false)}.</p>
 *
 * @see Singleton
 * @see SingletonException
 *
 * @author Daan van Yperen
 */
@UnstableApi
public class SingletonCustomPlugin implements ArtemisPlugin {

	private final boolean strict;
	private SingletonFieldResolver singletonResolver;

	/**
	 * Creates the SingletonPlugin in strict mode, throwing a {@link SingletonException}
	 * when an entity enters the world with a {@link Singleton @Singleton} component.
	 */
	public SingletonCustomPlugin() {
		this(true);
	}

	/**
	 * Creates the SingletonPlugin.
	 *
	 * @param strict enables strict mode, see {@link #SingletonCustomPlugin()}
	 */
	public SingletonCustomPlugin(boolean strict) {
		this.strict = strict;
	}

	@Override
	public void setup(WorldConfigurationBuilder b) {
		singletonResolver = new SingletonFieldResolver();
		b.register(singletonResolver);

		if (strict) {
			b.with(new SingletonValidationSystem(singletonResolver));
		}
	}

	public static class FieldObjectPair {
		Object object;
		Field field;

		public FieldObjectPair (Object target, Field field) {
			this.object = target;
			this.field = field;
		}
	}

	/**
	 * Resolves singleton fields in systems.
	 */
	public static class SingletonFieldResolver implements FieldResolver {

		private HashMap<Class<? extends Component>, Component> cachedSingletons;
		private EntityEdit singletonContainerEntity;
		private HashMap<Class<? extends Component>, Array<FieldObjectPair>> cachedFields;
		private Entity singletonEntity;

		@Override
		public void initialize(World world) {
			// we retain some state which should be fine as long as we're bound to the same world.
			this.cachedSingletons = new HashMap<>();
			this.cachedFields = new HashMap<>();
			this.singletonEntity = world.createEntity();
			this.singletonContainerEntity = singletonEntity.edit();
		}

		@Override
		public Object resolve(Object target, Class<?> fieldType, Field field) {
			if (isAnnotationPresent(fieldType, Singleton.class) && ClassReflection.isAssignableFrom(Component.class, fieldType)) {

				if (!cachedFields.containsKey((Class<Component>)fieldType)) {
					cachedFields.put((Class<? extends Component>)fieldType, new Array<>());
				}

				Array<FieldObjectPair> fields = cachedFields.get((Class<Component>)fieldType);
				fields.add(new FieldObjectPair(target, field));

				return getCreateSingletonComponent((Class<Component>) fieldType);
			}
			return null;
		}

		private Component getCreateSingletonComponent(Class<Component> component) {
			if (!cachedSingletons.containsKey(component)) {
				cachedSingletons.put(component, singletonContainerEntity.create(component));
			}
			return cachedSingletons.get(component);
		}
	}

	public class SingletonValidationSystem extends BaseSystem implements SubscriptionListener {

		private final SingletonFieldResolver singletonResolver;
		private final Set<Class<? extends Component>> singletonComponents;

		private AspectSubscriptionManager asm;

		public SingletonValidationSystem(SingletonFieldResolver singletonResolver) {
			this.singletonResolver = singletonResolver;
			this.singletonComponents = new HashSet<>();
		}

		@Override
		protected void initialize() {
			setEnabled(false);

			if (singletonResolver.cachedSingletons.size() > 0) {
				this.singletonComponents.addAll(singletonResolver.cachedSingletons.keySet());
				asm.get(Aspect.one(singletonComponents)).addSubscriptionListener(this);
			}
		}

		@Override
		protected void processSystem() {
		}

		@Override
		public void inserted(IntBag entities) {
			if (entities.size() > 1) {
				throw new SingletonException(singletonComponents);

			}
			for (int i = 0; i < entities.size(); i++) {
				int entityID = entities.get(i);
				Entity entity = world.getEntity(entityID);
				Bag<Component> fillBag = new Bag<>();
				entity.getComponents(fillBag);

				for (Component component : fillBag) {

					singletonResolver.cachedSingletons.put(component.getClass(), component);
					Array<FieldObjectPair> fields = singletonResolver.cachedFields.get(component.getClass());
					if (fields == null) {
						System.out.println("No field reference found for component " + component.getClass().getSimpleName());
						continue;
					}
					for (FieldObjectPair pair : fields) {
						Field field = pair.field;
						Object object = pair.object;
						field.setAccessible(true);
						try {
							field.set(object, component);
						} catch (ReflectionException e) {
							throw new RuntimeException(e);
						}
					}
					EntityEdit edit = singletonResolver.singletonEntity.edit();
					edit.remove(component.getClass());
					edit.add(component);
				}
				world.delete(entityID);
			}
		}

		@Override
		public void removed(IntBag entities) {
		}

	}

}

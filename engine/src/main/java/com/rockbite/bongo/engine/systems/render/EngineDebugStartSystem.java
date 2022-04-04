package com.rockbite.bongo.engine.systems.render;

import com.artemis.Aspect;
import com.artemis.BaseSystem;
import com.artemis.Component;
import com.artemis.Entity;
import com.artemis.EntitySubscription;
import com.artemis.utils.Bag;
import com.artemis.utils.IntBag;
import com.artemis.utils.reflect.ClassReflection;
import com.badlogic.gdx.InputProcessor;
import com.rockbite.bongo.engine.Bongo;
import com.rockbite.bongo.engine.input.InputInterceptor;
import com.rockbite.bongo.engine.input.InputProvider;
import net.mostlyoriginal.api.Singleton;


public class EngineDebugStartSystem extends BaseSystem implements InputProvider {

	private final InputInterceptor interceptor;
	private EntitySubscription allEntities;

	public EngineDebugStartSystem () {
		interceptor = new InputInterceptor();

		Bongo.imguiPlatform.init();

	}

	@Override
	protected void initialize () {
		super.initialize();
		allEntities = world.getAspectSubscriptionManager().get(Aspect.all());
	}


	final Bag fillBag = new Bag();
	final Bag singletons = new Bag();

	public void postInit () {

		final IntBag entities = allEntities.getEntities();
		for (int i = 0; i < entities.size(); i++) {
			final int entityID = entities.get(i);
			final Entity entity = world.getEntity(entityID);
			fillBag.clear();
			entity.getComponents(fillBag);
			if (fillBag.size() > 0) {
				for (int j = 0; j < fillBag.size(); j++) {
					final Component component = (Component)fillBag.get(j);
					if (ClassReflection.isAnnotationPresent(component.getClass(), Singleton.class)) {
						singletons.add(component);
					}
				}
			}
		}

	}


	@Override
	protected void processSystem () {

		Bongo.imguiPlatform.newFrame();
		boolean shouldCapture = Bongo.imguiPlatform.renderDebug(world, allEntities, singletons);

		interceptor.setBlockTouch(shouldCapture);

	}


	@Override
	public InputProcessor getInputProcessor () {
		return interceptor;
	}
}

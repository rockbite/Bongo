package com.rockbite.bongo.engine.platform;

import com.artemis.Component;
import com.artemis.EntitySubscription;
import com.artemis.World;
import com.artemis.utils.Bag;

public class IMGUIPlatformImpl implements IMGUIPlatform {

	public IMGUIPlatformImpl () {

	}

	@Override
	public void init () {
	}

	@Override
	public void newFrame () {
	}

	@Override
	public void renderDrawData (World world) {

	}

	@Override
	public boolean renderDebug (World world, EntitySubscription allEntities, Bag<Component> singletons) {
		return false;
	}
}

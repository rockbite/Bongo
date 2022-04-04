package com.rockbite.bongo.engine.platform;

import com.artemis.Component;
import com.artemis.EntitySubscription;
import com.artemis.World;
import com.artemis.utils.Bag;

public interface IMGUIPlatform {

	void init ();

	void newFrame ();

	void renderDrawData (World world);

	boolean renderDebug (World world, EntitySubscription allEntities, Bag<Component> singletons);
}

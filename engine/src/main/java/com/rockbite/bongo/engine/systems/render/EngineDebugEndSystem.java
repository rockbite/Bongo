package com.rockbite.bongo.engine.systems.render;

import com.artemis.BaseSystem;
import com.rockbite.bongo.engine.Bongo;

public class EngineDebugEndSystem extends BaseSystem {
	@Override
	protected void processSystem () {

		Bongo.imguiPlatform.renderDrawData(world);

	}
}

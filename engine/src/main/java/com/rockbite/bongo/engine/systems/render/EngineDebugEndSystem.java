package com.rockbite.bongo.engine.systems.render;

import com.artemis.BaseSystem;
import com.rockbite.bongo.engine.Bongo;
import imgui.ImGui;
import imgui.gl3.ImGuiImplGl3;

public class EngineDebugEndSystem extends BaseSystem {
	@Override
	protected void processSystem () {

		Bongo.imguiPlatform.renderDrawData(world);

	}
}

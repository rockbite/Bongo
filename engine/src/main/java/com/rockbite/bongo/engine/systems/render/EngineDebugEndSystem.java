package com.rockbite.bongo.engine.systems.render;

import com.artemis.BaseSystem;
import imgui.ImGui;
import imgui.gl3.ImGuiImplGl3;

public class EngineDebugEndSystem extends BaseSystem {
	@Override
	protected void processSystem () {
		final EngineDebugStartSystem system = world.getSystem(EngineDebugStartSystem.class);
		final ImGuiImplGl3 imGuiImplGl3 = system.getImGuiImplGl3();
		imGuiImplGl3.renderDrawData(ImGui.getDrawData());
	}
}

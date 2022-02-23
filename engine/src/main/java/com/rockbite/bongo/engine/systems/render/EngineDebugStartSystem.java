package com.rockbite.bongo.engine.systems.render;

import com.artemis.BaseSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.rockbite.bongo.engine.Bongo;
import com.rockbite.bongo.engine.gltf.scene.SceneEnvironment;
import com.rockbite.bongo.engine.input.InputInterceptor;
import com.rockbite.bongo.engine.input.InputProvider;
import imgui.ImGui;
import imgui.gl3.ImGuiImplGl3;
import lombok.Getter;

public class EngineDebugStartSystem extends BaseSystem implements InputProvider {

	@Getter
	private ImGuiImplGl3 imGuiImplGl3;

	private InputInterceptor interceptor;

	public EngineDebugStartSystem () {
		interceptor = new InputInterceptor();

		ImGui.init();

		Bongo.imguiPlatform.create();
		imGuiImplGl3 = new ImGuiImplGl3();

		ImGui.createContext();

		Bongo.imguiPlatform.init();

		if (Gdx.gl30 != null) {
			imGuiImplGl3.init("#version 330 core");
		} else {
			imGuiImplGl3.init("#version 110");
		}
	}

	@Override
	protected void processSystem () {

		Bongo.imguiPlatform.newFrame();

		ImGui.newFrame();

		renderDebug();

		ImGui.render();

		final boolean wantCaptureKeyboard = ImGui.getIO().getWantCaptureKeyboard();
		final boolean wantCaptureMouse = ImGui.getIO().getWantCaptureMouse();

		interceptor.setBlockTouch(wantCaptureMouse);



	}

	private void renderDebug () {
		environment();
	}

	private void environment () {
		if (world.getSystem(EnvironmentConfigSystem.class) != null) {
			ImGui.begin("Env");
			final EnvironmentConfigSystem system = world.getSystem(EnvironmentConfigSystem.class);

			final SceneEnvironment sceneEnvironment = system.getEnvironment().getSceneEnvironment();
			ImGui.dragFloat3("LightDirection", sceneEnvironment.getDirectionalLightDirRaw(), 0.1f, -1f, 1f);
			ImGui.colorPicker4("LightColour", sceneEnvironment.getDirectionLightColorRaw());

			ImGui.end();
		}
	}

	@Override
	public InputProcessor getInputProcessor () {
		return interceptor;
	}
}

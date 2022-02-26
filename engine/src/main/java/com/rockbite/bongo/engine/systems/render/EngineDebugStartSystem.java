package com.rockbite.bongo.engine.systems.render;

import com.artemis.BaseSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Texture;
import com.rockbite.bongo.engine.Bongo;
import com.rockbite.bongo.engine.gltf.scene.SceneEnvironment;
import com.rockbite.bongo.engine.input.InputInterceptor;
import com.rockbite.bongo.engine.input.InputProvider;
import imgui.ImGui;
import imgui.ImVec2;
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
		debugTextures();
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

	private void debugTextures () {
		ImGui.begin("Render debug");

		final ImVec2 windowSize = ImGui.getWindowSize();
		float windowWidth = windowSize.x;
		float halfWidth = windowWidth/2f;

		ImGui.beginTabBar("RenderBar");

		if (ImGui.beginTabItem("Depth")) {

			final DepthPassSystem depthSystem = world.getSystem(DepthPassSystem.class);

			final Texture depthTexture = depthSystem.getDepthTexture();
			ImGui.text("depth tex");
			ImGui.image(depthTexture.getTextureObjectHandle(), windowWidth, windowWidth, 0, 1, 1, 0);

			ImGui.endTabItem();
		}
		if (ImGui.beginTabItem("Shadow")) {
			final ShadowPassSystem shadowPassSystem = world.getSystem(ShadowPassSystem.class);

			final Texture shadowMapTexture = shadowPassSystem.getShadowMapDepthTexture();
			ImGui.text("shadowmap tex");
			ImGui.image(shadowMapTexture.getTextureObjectHandle(), windowWidth, windowWidth, 0, 1, 1, 0);

			ImGui.endTabItem();
		}
		ImGui.endTabBar();


		ImGui.end();
	}

	@Override
	public InputProcessor getInputProcessor () {
		return interceptor;
	}
}

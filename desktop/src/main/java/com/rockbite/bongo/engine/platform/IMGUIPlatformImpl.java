package com.rockbite.bongo.engine.platform;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import imgui.ImDrawData;
import imgui.ImGui;
import imgui.glfw.ImGuiImplGlfw;

public class IMGUIPlatformImpl implements IMGUIPlatform {

	private ImGuiImplGlfw imGuiImplGlfw;

	public IMGUIPlatformImpl () {

	}

	@Override
	public void create () {
		imGuiImplGlfw = new ImGuiImplGlfw();
	}

	@Override
	public void init () {
		imGuiImplGlfw.init(((Lwjgl3Graphics)Gdx.graphics).getWindow().getWindowHandle(), true);
	}

	@Override
	public void newFrame () {
		imGuiImplGlfw.newFrame();
	}
}

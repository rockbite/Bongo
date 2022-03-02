package com.rockbite.bongo.engine.systems.render;

import com.artemis.Aspect;
import com.artemis.BaseSystem;
import com.artemis.Component;
import com.artemis.Entity;
import com.artemis.EntitySubscription;
import com.artemis.annotations.AspectDescriptor;
import com.artemis.utils.Bag;
import com.artemis.utils.IntBag;
import com.artemis.utils.reflect.ClassReflection;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Attribute;
import com.badlogic.gdx.graphics.g3d.Attributes;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.rockbite.bongo.engine.Bongo;
import com.rockbite.bongo.engine.annotations.ComponentExpose;
import com.rockbite.bongo.engine.annotations.ComponentExposeFlavour;
import com.rockbite.bongo.engine.components.render.ShaderControlResource;
import com.rockbite.bongo.engine.gltf.GLTFDataModel;
import com.rockbite.bongo.engine.gltf.scene.SceneEnvironment;
import com.rockbite.bongo.engine.gltf.scene.SceneMaterial;
import com.rockbite.bongo.engine.gltf.scene.SceneMesh;
import com.rockbite.bongo.engine.gltf.scene.SceneMeshPrimtive;
import com.rockbite.bongo.engine.gltf.scene.SceneModelInstance;
import com.rockbite.bongo.engine.gltf.scene.SceneNode;
import com.rockbite.bongo.engine.gltf.scene.shader.BaseSceneShader;
import com.rockbite.bongo.engine.gltf.scene.shader.PBRColourAttribute;
import com.rockbite.bongo.engine.gltf.scene.shader.PBRFloatAttribute;
import com.rockbite.bongo.engine.gltf.scene.shader.PBRMaterialAttribute;
import com.rockbite.bongo.engine.gltf.scene.shader.ShaderControlProvider;
import com.rockbite.bongo.engine.input.InputInterceptor;
import com.rockbite.bongo.engine.input.InputProvider;
import com.rockbite.bongo.engine.reflect.ReflectUtils;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.gl3.ImGuiImplGl3;
import imgui.type.ImInt;
import lombok.Getter;
import net.mostlyoriginal.api.Singleton;

import java.lang.reflect.Field;

import static com.artemis.utils.reflect.ClassReflection.isAnnotationPresent;

public class EngineDebugStartSystem extends BaseSystem implements InputProvider {

	@Getter
	private ImGuiImplGl3 imGuiImplGl3;

	private InputInterceptor interceptor;
	private EntitySubscription allEntities;

	public interface ImguiMapper {
		void run (String name, Object object);
	}
	private ObjectMap<ComponentExposeFlavour, ImguiMapper> mappers = new ObjectMap<>();
	private ObjectMap<Class, ImguiMapper> objectMappers = new ObjectMap<>();

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

		initMappers();
	}

	@Override
	protected void initialize () {
		super.initialize();
		allEntities = world.getAspectSubscriptionManager().get(Aspect.all());
	}

	private void initMappers () {
		mappers.put(ComponentExposeFlavour.COLOUR_4_VEC, this::colour4Mapper);
		mappers.put(ComponentExposeFlavour.VEC_3, this::vec3Mapper);

		objectMappers.put(SceneModelInstance.class, this::sceneModelInstanceMapper);
	}

	private void sceneModelInstanceMapper (String name, Object o) {
		if (o instanceof SceneModelInstance) {
			final SceneModelInstance sceneModelInstance = (SceneModelInstance)o;

			processNode(sceneModelInstance.getNodes());
		}
	}

	private void processNode (Array<SceneNode> sceneNodes) {
		for (SceneNode sceneNode : sceneNodes) {
			if (ImGui.treeNode(sceneNode.getName())) {

				final SceneMesh sceneMesh = sceneNode.getSceneMesh();
				for (SceneMeshPrimtive sceneMeshPrimtive : sceneMesh.getSceneMeshPrimtiveArray()) {
					if (ImGui.treeNode(sceneMeshPrimtive.getName())) {

						final SceneMaterial sceneMaterial = sceneMeshPrimtive.sceneMaterial;
						ImGui.text("Material");

						final Attributes attributes = sceneMaterial.getAttributes();
						for (Attribute attribute : attributes) {
							if (attribute instanceof PBRMaterialAttribute) {

							}
							if (attribute instanceof PBRFloatAttribute) {
								final long type = attribute.type;
								final float[] value = ((PBRFloatAttribute)attribute).value;

								ImGui.sliderFloat(PBRFloatAttribute.getAttributeAlias(type), value, 0, 1f);
							}
							if (attribute instanceof PBRColourAttribute) {
								final long type = attribute.type;
								final float[] value = ((PBRColourAttribute)attribute).color;

								ImGui.colorEdit4(PBRFloatAttribute.getAttributeAlias(type), value);
							}
						}

						ImGui.treePop();
					}
				}

				final Array<SceneNode> children = sceneNode.getChildren();
				processNode(children);
				ImGui.treePop();
			}
		}
	}

	private void vec3Mapper (String name, Object object) {
		if (object instanceof float[]) {
			if (((float[])object).length == 3) {
				ImGui.sliderFloat3(name, (float[])object, -1, 1);
			}
		}
	}

	private void colour4Mapper (String name, Object object) {
		if (object instanceof float[]) {
			if (((float[])object).length == 4) {
				ImGui.colorEdit4(name, (float[])object);
			}
		}
	}

	final Bag<Component> fillBag = new Bag<>();
	final Bag<Component> singletons = new Bag<>();

	public void postInit () {

		final IntBag entities = allEntities.getEntities();
		for (int i = 0; i < entities.size(); i++) {
			final int entityID = entities.get(i);
			final Entity entity = world.getEntity(entityID);
			fillBag.clear();
			entity.getComponents(fillBag);
			if (fillBag.size() > 0) {
				for (int j = 0; j < fillBag.size(); j++) {
					final Component component = fillBag.get(j);
					if (isAnnotationPresent(component.getClass(), Singleton.class)) {
						singletons.add(component);
					}
				}
			}
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
		debugComponents();
		shadersUI();
	}

	private void entities () {
		ImGui.begin("Entities");

		final IntBag entities = allEntities.getEntities();
		for (int i = 0; i < entities.size(); i++) {
			final int entityID = entities.get(i);
			String name = entityID + "";
			if (i == 0) {
				name += " singletons";
			}
			if (ImGui.treeNode(name)) {

				final Entity entity = world.getEntity(entityID);
				fillBag.clear();
				final Bag<Component> components = entity.getComponents(fillBag);
				for (Component component : components) {
					if (ImGui.treeNode(component.getClass().getSimpleName())) {

						debugComponent(component);

						ImGui.treePop();
					}
				}

				ImGui.treePop();
			}

		}

		ImGui.end();

	}

	int shaderIndex = 0;
	private void shadersUI () {

		final EngineDebugSystem system = world.getSystem(EngineDebugSystem.class);
		if (system != null) {
			ImGui.begin("Shaders");

			final Array<ShaderControlResource> liveShaderResources = system.getLiveShaderResources();

			if (liveShaderResources.size == 0) {
				ImGui.end();
				return;
			}

			String previewName = liveShaderResources.get(shaderIndex).getShaderControlProvider().getShaderDisplayName();
			if (ImGui.beginCombo("Instances", previewName)) {
				for (int i = 0; i < liveShaderResources.size; i++) {
					final ShaderControlResource shaderResource = liveShaderResources.get(i);
					boolean isSelected = i == shaderIndex;
					if (ImGui.selectable(shaderResource.getShaderControlProvider().getShaderDisplayName() + "##" + i, isSelected)) {
						shaderIndex = i;
					}
					if (isSelected) {
						ImGui.setItemDefaultFocus();
					}
				}
				ImGui.endCombo();
			}

			ImGui.text("Shader controls");

			final ShaderControlResource shaderResource = liveShaderResources.get(shaderIndex);
			final ShaderControlProvider shader = shaderResource.getShaderControlProvider();
			final Array<BaseSceneShader.ShaderControl> shaderControls = shader.getShaderControls();

			for (BaseSceneShader.ShaderControl shaderControl : shaderControls) {
				uiForShaderControl(shaderControl);
			}

			ImGui.end();
		}


	}

	private void uiForShaderControl (BaseSceneShader.ShaderControl shaderControl) {
		final String uniformName = shaderControl.getUniformName();
		if (shaderControl instanceof BaseSceneShader.FloatShaderControl) {
			final BaseSceneShader.FloatShaderControl floatControl = (BaseSceneShader.FloatShaderControl)shaderControl;
			final int numComponents = floatControl.getNumComponents();

			if (floatControl.isHasRange()) {
				if (numComponents == 1) {
					ImGui.sliderFloat(uniformName, floatControl.getBuffer1(), floatControl.getMin(), floatControl.getMax());
				} else if (numComponents == 2) {
					ImGui.sliderFloat2(uniformName, floatControl.getBuffer2(), floatControl.getMin(), floatControl.getMax());
				} else if (numComponents == 3) {
					ImGui.sliderFloat3(uniformName, floatControl.getBuffer3(), floatControl.getMin(), floatControl.getMax());
				} else if (numComponents == 4) {
					if (floatControl.isHasColour()) {
						ImGui.colorPicker4(uniformName, floatControl.getBuffer4());
					} else {
						ImGui.sliderFloat4(uniformName, floatControl.getBuffer4(), floatControl.getMin(), floatControl.getMax());
					}
				}
			} else {
				if (numComponents == 1) {
					ImGui.sliderFloat(uniformName, floatControl.getBuffer1(), 0, 1f);
				} else if (numComponents == 2) {
					ImGui.sliderFloat2(uniformName, floatControl.getBuffer2(), 0, 1f);
				} else if (numComponents == 3) {
					ImGui.sliderFloat3(uniformName, floatControl.getBuffer3(), 0, 1f);
				} else if (numComponents == 4) {
					if (floatControl.isHasColour()) {
						ImGui.colorPicker4(uniformName, floatControl.getBuffer4());
					} else {
						ImGui.sliderFloat4(uniformName, floatControl.getBuffer4(), 0, 1f);
					}
				}

			}
		}

	}

	private void debugComponents () {
		singletons();

		entities();

	}

	ImInt singletonPosition = new ImInt();
	Array<String> items = new Array<String>(String.class);
	private void singletons () {

		items.clear();

		for (int i = 0; i < singletons.size(); i++) {
			final Component singleton = singletons.get(i);
			items.add(singleton.getClass().getSimpleName());
		}

		if (items.size > 0) {
			ImGui.begin("Components");

			ImGui.listBox("Singletons", singletonPosition, items.toArray());


			ImGui.beginChild("ComponentDetail");
			final Component component = singletons.get(singletonPosition.get());

			debugComponent(component);

			ImGui.endChild();

			ImGui.end();
		}
	}

	private void debugComponent (Object component) {
		if (objectMappers.containsKey(component.getClass())) {
			objectMappers.get(component.getClass()).run(component.getClass().getSimpleName(), component);
		} else {
			final Iterable<Field> fieldsUpTo = ReflectUtils.getFieldsUpTo(component.getClass(), null);

			for (Field field : fieldsUpTo) {

				if (field.getAnnotation(ComponentExpose.class) != null) {
					try {
						field.setAccessible(true);

						ComponentExpose expose = field.getAnnotation(ComponentExpose.class);
						final ComponentExposeFlavour flavour = expose.flavour();
						if (flavour != null && flavour != ComponentExposeFlavour.NONE && mappers.containsKey(flavour)) {
							mappers.get(flavour).run(field.getName(), field.get(component));
						} else {
							debugComponent(field.get(component));
						}

					} catch (IllegalAccessException e) {
						e.printStackTrace();
					}
				}
			}
		}
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

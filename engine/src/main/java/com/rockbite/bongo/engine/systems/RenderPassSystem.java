package com.rockbite.bongo.engine.systems;

import com.artemis.Aspect;
import com.artemis.BaseSystem;
import com.artemis.Component;
import com.artemis.ComponentMapper;
import com.artemis.EntitySubscription;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.rockbite.bongo.engine.components.singletons.Cameras;
import com.rockbite.bongo.engine.components.singletons.Environment;
import com.rockbite.bongo.engine.components.singletons.RenderUtils;
import com.rockbite.bongo.engine.gltf.scene.SceneModelInstance;
import com.rockbite.bongo.engine.gltf.scene.SceneRenderable;
import com.rockbite.bongo.engine.gltf.scene.SceneRenderableProvider;
import com.rockbite.bongo.engine.gltf.scene.shader.BaseSceneShader;
import com.rockbite.bongo.engine.gltf.scene.shader.SceneShaderProvider;


public abstract class RenderPassSystem extends BaseSystem {


	public static class GLViewportConfig {
		public int x;
		public int y;
		public int width;
		public int height;

		public GLViewportConfig (int x, int y, int width, int height) {
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
		}
	}
	public static GLViewportConfig glViewport = new GLViewportConfig(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

	private Cameras cameras;
	protected Environment environment;
	protected RenderUtils renderUtils;

	protected final SceneShaderProvider sceneShaderProvider;

	private final Class<? extends Component>[] componentsToGather;

	protected ComponentMapper<SceneModelInstance> sceneNodeInstanceMapper;
	protected EntitySubscription renderObjectsSubscription;

	private SceneRenderableProvider sceneRenderableProvider = new SceneRenderableProvider();

	private Array<SceneRenderable> sceneRenderables = new Array<>();

	private boolean shouldStartContext;
	private boolean shouldEndContext;

	public RenderPassSystem (SceneShaderProvider sceneShaderProvider, Class<? extends Component>... componentsToGather) {
		this.sceneShaderProvider = sceneShaderProvider;
		this.componentsToGather = componentsToGather;
	}

	public RenderPassSystem setContextStartEnd (boolean shouldStartContext, boolean shouldEndContext) {
		this.shouldStartContext = shouldStartContext;
		this.shouldEndContext = shouldEndContext;
		return this;
	}


	@Override
	protected void initialize () {
		super.initialize();

		sceneShaderProvider.injectWorld(world);

		createSubscriptions();

	}

	protected void createSubscriptions () {
		createSubscriptions(SceneModelInstance.class);
	}
	protected void createSubscriptions (Class baseModelclass) {
		renderObjectsSubscription = createSubscriptionForRenderType(baseModelclass);
	}

	protected EntitySubscription createSubscriptionForRenderType (Class<? extends Component> renderComponentType) {
		Array<Class<? extends Component>> baseComponentsToGather = new Array<>(Class.class);
		baseComponentsToGather.addAll(componentsToGather);
		baseComponentsToGather.add(renderComponentType);
		return world.getAspectSubscriptionManager().get(Aspect.all(baseComponentsToGather.toArray()));
	}

	@Override
	protected void begin () {
		super.begin();

		if (shouldStartContext) {
			renderUtils.getRenderContext().begin();
		}

		collectRendables();

	}

	protected void collectRendables() {
		sceneRenderableProvider.obtainSceneRenderables(renderObjectsSubscription, sceneNodeInstanceMapper, sceneShaderProvider, sceneRenderables);
		sceneRenderableProvider.sort(sceneRenderables);
	}

	@Override
	protected void end () {
		super.end();

		if (shouldEndContext) {
			renderUtils.getRenderContext().end();
		}

		sceneRenderableProvider.freeAll(sceneRenderables);
	}

	/**
	 * Render all collected and sorted objects, to be called within process function
	 */
	public void renderAllCollectedRenderables () {
		BaseSceneShader currentShader = null;
		for (SceneRenderable renderable : sceneRenderables) {
			if (currentShader != renderable.shader) {
				if (currentShader != null) currentShader.end();
				currentShader = renderable.shader;
				currentShader.begin(cameras, renderUtils, environment.getSceneEnvironment());
			}
			currentShader.render(renderable);
		}
		if (currentShader != null) currentShader.end();

	}

}

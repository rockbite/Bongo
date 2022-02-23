package com.rockbite.bongo.engine.systems.render;

import com.artemis.Component;
import com.artemis.ComponentMapper;
import com.artemis.EntityEdit;
import com.artemis.utils.IntBag;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.math.Vector3;
import com.rockbite.bongo.engine.components.render.Particle;
import com.rockbite.bongo.engine.components.singletons.Cameras;
import com.rockbite.bongo.engine.events.asset.AssetsEndLoadEvent;
import com.rockbite.bongo.engine.gltf.scene.shader.DefaultSceneShaderProvider;
import com.rockbite.bongo.engine.gltf.scene.shader.SceneShaderProvider;
import com.rockbite.bongo.engine.gltf.scene.shader.bundled.ShadedShader;
import com.rockbite.bongo.engine.render.AutoReloadingShaderProgram;
import com.rockbite.bongo.engine.render.ShaderSourceProvider;
import com.rockbite.bongo.engine.systems.RenderPassSystem;
import com.rockbite.bongo.engine.systems.assets.AssetSystem;
//import com.talosvfx.talos.runtime.IEmitter;
//import com.talosvfx.talos.runtime.ParticleEffectDescriptor;
//import com.talosvfx.talos.runtime.ParticleEffectInstance;
//import com.talosvfx.talos.runtime.ScopePayload;
//import com.talosvfx.talos.runtime.modules.DrawableModule;
//import com.talosvfx.talos.runtime.modules.MaterialModule;
//import com.talosvfx.talos.runtime.modules.MeshGeneratorModule;
//import com.talosvfx.talos.runtime.modules.ParticlePointDataGeneratorModule;
//import com.talosvfx.talos.runtime.modules.SpriteMaterialModule;
//import com.talosvfx.talos.runtime.render.ParticleRenderer;
//import com.talosvfx.talos.runtime.render.p3d.Simple3DBatch;
//import com.talosvfx.talos.runtime.values.DrawableValue;
import net.mostlyoriginal.api.event.common.Subscribe;


public class ParticleRenderPassSystem extends RenderPassSystem /*implements ParticleRenderer*/ {
//
//	//SINGLETONS
//	private Cameras cameras;
//	private Simple3DBatch simple3DBatch;
//
//
//	//MAPPERS
//	private ComponentMapper<Particle> particleMapper;
//	private AutoReloadingShaderProgram shaderProgram;
//
	public ParticleRenderPassSystem () {
		this(Particle.class);
	}

	public ParticleRenderPassSystem (Class<? extends Component> componentClazz) {
		this(
			new DefaultSceneShaderProvider(ShaderSourceProvider.resolveVertex("core/particle", Files.FileType.Classpath), ShaderSourceProvider.resolveFragment("core/particle", Files.FileType.Classpath), ShadedShader.class),
			componentClazz
		);
	}

	public ParticleRenderPassSystem (SceneShaderProvider sceneShaderProvider, Class<? extends Component>... componentsToGather) {
		super(sceneShaderProvider, componentsToGather);
	}

//
//	@Subscribe
//	public void onAssetLoaded (AssetsEndLoadEvent event) {
//		simple3DBatch = new Simple3DBatch(4000, new VertexAttributes(VertexAttribute.Position(), VertexAttribute.ColorPacked(), VertexAttribute.TexCoords(0)));
//		shaderProgram = new AutoReloadingShaderProgram(ShaderSourceProvider.resolveVertex("core/particle"), ShaderSourceProvider.resolveFragment("core/particle"));
//
//		int entityID = world.create();
//		EntityEdit edit = world.edit(entityID);
//		Particle particle = edit.create(Particle.class);
//
//		AssetSystem system = world.getSystem(AssetSystem.class);
//		TextureAtlas gameAtlas = system.getGameAtlas();
//
//		ParticleEffectDescriptor effectDescriptor = new ParticleEffectDescriptor(Gdx.files.internal("particles/fire.p"), gameAtlas);
//		ParticleEffectInstance effect = effectDescriptor.createEffectInstance();
//		particle.setParticleEffectInstance(effect);
//
//		effect.restart();
//
//		particle.setPosition(new Vector3(2, 2, 2));
//
//	}
//
//	@Override
//	protected void initialize () {
//		super.initialize();
//	}
//
//	@Override
//	protected void createSubscriptions () {
//		createSubscriptions(Particle.class);
//	}
//
////	public SceneModel createModelForRenderer () {
////		SceneModel sceneModel = new SceneModel("ParticleModel");
////
////		SceneNode sceneNode = new SceneNode("ParticleNode");
////
////		final Mesh oceanMesh = simple3DBatch.getMesh();
////		SceneMeshVertexInfo vertexInfo = new SceneMeshVertexInfo(oceanMesh.getVertexAttributes());
////
////		SceneMeshPrimtive sceneMeshPrimtive = new SceneMeshPrimtive(oceanMesh, vertexInfo);
////		SceneMesh sceneMesh = new SceneMesh("ParticleMesh", sceneMeshPrimtive);
////		sceneNode.setSceneMesh(sceneMesh);
////
////		sceneMaterial = SceneMaterial.Empty("ParticleMaterial");
////
////		final Texture foam = new Texture(Gdx.files.internal("textures/foam.png"));
////		foam.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
////		sceneMaterial.getAttributes().set(TextureAttribute.createDiffuse(foam));
////
////		sceneMesh.setAllPrimitivesMaterial(sceneMaterial);
////
////		sceneModel.nodes.add(sceneNode);
////
////		return sceneModel;
////	}
//
//	@Override
//	protected void collectRendables () {
//		//Override with custom renderable
//
//	}
//
	/**
	 * Process the system.
	 */
	@Override
	protected void processSystem () {
//
//		RenderContext renderContext = renderUtils.getRenderContext();
//		renderContext.end();
//
//		Gdx.gl.glEnable(GL20.GL_BLEND);
//		Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
//		Gdx.gl.glDepthMask(false);
//
//		simple3DBatch.begin(cameras.getGameCamera(), shaderProgram.getShaderProgram());
//
//		float deltaTime = Gdx.graphics.getDeltaTime();
//
//		IntBag entities = renderObjectsSubscription.getEntities();
//		int size = entities.size();
//		for (int i = 0; i < size; i++) {
//			int entityID = entities.get(i);
//
//			Particle particle = particleMapper.get(entityID);
//
//
//			ParticleEffectInstance particleEffectInstance = particle.getParticleEffectInstance();
//			particleEffectInstance.setPosition(particle.getPosition().x + 0, 0);
//			particleEffectInstance.update(deltaTime);
//
//
//			particleEffectInstance.render(this);
//		}
//
//		simple3DBatch.end();
//
//		renderContext.begin();
	}
//
//	@Override
//	public Camera getCamera () {
//		return cameras.getGameCamera();
//	}
//
//	@Override
//	public void render (ParticleEffectInstance particleEffectInstance) {
//
//
//		simple3DBatch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
//
//		for (int i = 0; i < particleEffectInstance.getEmitters().size; i++) {
//			final IEmitter particleEmitter = particleEffectInstance.getEmitters().get(i);
//			if(!particleEmitter.isVisible()) continue;
//			if(particleEmitter.isBlendAdd()) {
//				simple3DBatch.setBlendFunction(GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA);
//			} else {
//				if (particleEmitter.isAdditive()) {
//					simple3DBatch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
//				} else {
//					simple3DBatch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
//				}
//			}
//
//			MeshGeneratorModule meshGenerator = particleEmitter.getParticleModule().getMeshGenerator();
//			if (meshGenerator == null) continue;
//			meshGenerator.setRenderMode(true);
//
//			DrawableModule drawableModule = particleEmitter.getDrawableModule();
//			if (drawableModule == null) continue;
//			if (drawableModule.getMaterialModule() == null) continue;
//			ParticlePointDataGeneratorModule particlePointDataGeneratorModule = particleEmitter.getParticleModule().getPointDataGenerator();
//			if (particlePointDataGeneratorModule == null) continue;
//
//			int cachedMode = particleEmitter.getScope().getRequestMode();
//			int cachedRequesterID = particleEmitter.getScope().getRequesterID();
//
//			particleEmitter.getScope().setCurrentRequestMode(ScopePayload.SUB_PARTICLE_ALPHA);
//
//			meshGenerator.render(this, drawableModule.getMaterialModule(), particlePointDataGeneratorModule.pointData);
//
//			particleEmitter.getScope().setCurrentRequestMode(cachedMode);
//			particleEmitter.getScope().setCurrentRequesterID(cachedRequesterID);
//		}
//
//		simple3DBatch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
//	}
//
//	@Override
//	public void render (float[] verts, MaterialModule materialModule) {
//		if (materialModule instanceof SpriteMaterialModule) {
//			DrawableValue drawableValue = ((SpriteMaterialModule)materialModule).getDrawableValue();
//			TextureRegion textureRegion = drawableValue.getDrawable().getTextureRegion();
//
//			simple3DBatch.render(verts, textureRegion.getTexture());
//		}
//
//	}
}

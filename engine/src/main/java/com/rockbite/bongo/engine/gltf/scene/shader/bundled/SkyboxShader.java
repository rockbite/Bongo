package com.rockbite.bongo.engine.gltf.scene.shader.bundled;

import com.artemis.World;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Cubemap;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Attributes;
import com.badlogic.gdx.graphics.g3d.attributes.DepthTestAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Array;
import com.rockbite.bongo.engine.components.render.PointLight;
import com.rockbite.bongo.engine.components.singletons.Cameras;
import com.rockbite.bongo.engine.components.singletons.RenderUtils;
import com.rockbite.bongo.engine.gltf.scene.SceneEnvironment;
import com.rockbite.bongo.engine.gltf.scene.SceneRenderable;
import com.rockbite.bongo.engine.gltf.scene.shader.BaseSceneShader;
import com.rockbite.bongo.engine.gltf.scene.shader.PBRColourAttribute;
import com.rockbite.bongo.engine.gltf.scene.shader.PBRFloatAttribute;
import com.rockbite.bongo.engine.gltf.scene.shader.PBRMaterialAttribute;
import com.rockbite.bongo.engine.gltf.scene.shader.PBRVec3Attribute;
import com.rockbite.bongo.engine.systems.render.ShadowPassSystem;

public class SkyboxShader extends BaseSceneShader  {

	private final static long optionalAttributes = IntAttribute.CullFace | DepthTestAttribute.Type;


	public static class Inputs {
		//global
		public final static Uniform cameraProj = new Uniform("u_proj");
		public final static Uniform cameraView = new Uniform("u_view");

		public final static Uniform envMap = new Uniform("u_envMap");

	}

	public static class Setters {

		//Global
		public final static Setter cameraProj = new GlobalSetter() {
			@Override
			public void set (BaseSceneShader shader, int inputID, SceneRenderable renderable, Attributes combinedAttributes) {
				shader.set(inputID, shader.cameras.getGameCamera().projection);
			}
		};

		public final static Setter cameraView = new GlobalSetter() {
			@Override
			public void set (BaseSceneShader shader, int inputID, SceneRenderable renderable, Attributes combinedAttributes) {
				shader.set(inputID, shader.cameras.getGameCamera().view);
			}
		};


		public final static Setter envMap = new GlobalSetter() {
			@Override
			public void set (BaseSceneShader shader, int inputID, SceneRenderable renderable, Attributes combinedAttributes) {
				shader.set(inputID, shader.context.textureBinder.bind(shader.sceneEnvironment.getSkyBox()));
			}
		};

	}

	//global
	private int u_proj;
	private int u_view;
	private int u_envMap;



	public SkyboxShader (FileHandle vertexSource, FileHandle fragmentSource, SceneRenderable sceneRenderable, World world) {
		super(vertexSource, fragmentSource, sceneRenderable, world);
	}

	@Override
	protected long getOptionalAttributes () {
		return optionalAttributes;
	}

	@Override
	public void begin (Cameras cameras, RenderUtils renderUtils, SceneEnvironment sceneEnvironment) {
		super.begin(cameras, renderUtils, sceneEnvironment);

		context.setDepthTest(GL20.GL_LEQUAL);
		context.setCullFace(GL20.GL_BACK);
		context.setDepthMask(true);

	}



	@Override
	public void initClassSpecificUniforms () {
		u_proj = register(Inputs.cameraProj, Setters.cameraProj);
		u_view = register(Inputs.cameraView, Setters.cameraView);
		u_envMap = register(Inputs.envMap, Setters.envMap);

	}


	/**
	 * Compare this shader against the other, used for sorting, light weight shaders are rendered first.
	 *
	 * @param other
	 */
	@Override
	public int compareTo (BaseSceneShader other) {
		return -1;
	}







}

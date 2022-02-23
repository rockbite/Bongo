package com.rockbite.bongo.engine.gltf.scene.shader.bundled;

import com.artemis.World;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Attributes;
import com.badlogic.gdx.graphics.g3d.attributes.DepthTestAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Matrix4;
import com.rockbite.bongo.engine.components.singletons.Cameras;
import com.rockbite.bongo.engine.components.singletons.RenderUtils;
import com.rockbite.bongo.engine.gltf.scene.SceneEnvironment;
import com.rockbite.bongo.engine.gltf.scene.SceneRenderable;
import com.rockbite.bongo.engine.gltf.scene.shader.BaseSceneShader;

public class CloudPrepassShader extends BaseSceneShader {


	private final static long optionalAttributes = IntAttribute.CullFace | DepthTestAttribute.Type;

	public static class Inputs {
		//global
		public final static Uniform cameraProjTrans = new Uniform("u_projTrans");

		//Object
		public final static Uniform objectSRT = new Uniform("u_srt");
		public final static Uniform normalMatrix = new Uniform("u_normalMatrix");

	}

	public static class Setters {

		//Global
		public final static Setter cameraProjTrans = new GlobalSetter() {
			@Override
			public void set (BaseSceneShader shader, int inputID, SceneRenderable renderable, Attributes combinedAttributes) {
				shader.set(inputID, shader.cameras.getGameCamera().combined);
			}
		};

		//object
		public final static Setter objectSRT = new LocalSetter() {
			@Override
			public void set (BaseSceneShader shader, int inputID, SceneRenderable renderable, Attributes combinedAttributes) {
				shader.set(inputID, renderable.worldTransform);
			}
		};
		public final static Setter normalMatrix = new LocalSetter() {
			Matrix3 matrix3 = new Matrix3();
			@Override
			public void set (BaseSceneShader shader, int inputID, SceneRenderable renderable, Attributes combinedAttributes) {
				shader.set(inputID, matrix3.set(renderable.worldTransform).inv().transpose());
			}
		};
	}

	//global
	private int u_projTrans;
	//object
	private int u_objectSRT;
	private int u_normalMatrix;


	public CloudPrepassShader (FileHandle vertexSource, FileHandle fragmentSource, SceneRenderable sceneRenderable, World world) {
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
		context.setCullFace(GL20.GL_NONE);
		context.setDepthMask(true);
		context.setBlending(false, 0, 0);
	}



	@Override
	public void initClassSpecificUniforms () {
		//global
		u_projTrans = register(Inputs.cameraProjTrans, Setters.cameraProjTrans);
		//object
		u_objectSRT = register(Inputs.objectSRT, Setters.objectSRT);
		u_normalMatrix = register(Inputs.normalMatrix, Setters.normalMatrix);
	}

	@Override
	public int compareTo (BaseSceneShader o) {
		return -1;
	}

}

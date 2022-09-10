package com.rockbite.bongo.engine.gltf.scene.shader.bundled;

import com.artemis.World;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Attributes;
import com.badlogic.gdx.graphics.g3d.attributes.DepthTestAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.math.Matrix4;
import com.rockbite.bongo.engine.components.singletons.Cameras;
import com.rockbite.bongo.engine.components.singletons.RenderUtils;
import com.rockbite.bongo.engine.gltf.scene.SceneEnvironment;
import com.rockbite.bongo.engine.gltf.scene.SceneRenderable;
import com.rockbite.bongo.engine.gltf.scene.shader.BaseSceneShader;
import com.rockbite.bongo.engine.systems.render.ShadowPassSystem;

public class ShadowMapShader extends BaseSceneShader {


	private final static long optionalAttributes = IntAttribute.CullFace | DepthTestAttribute.Type;

	public static class Inputs {
		//global
		public final static Uniform cameraProjTrans = new Uniform("u_projTrans");
		public final static Uniform lightMatrix = new Uniform("u_projTrans");

		//Object
		public final static Uniform objectSRT = new Uniform("u_srt");
		public final static Uniform bones = new Uniform("u_jointMatrix");



	}

	public static class Setters {

		//Global
		public final static Setter cameraProjTrans = new GlobalSetter() {
			@Override
			public void set (BaseSceneShader shader, int inputID, SceneRenderable renderable, Attributes combinedAttributes) {
				shader.set(inputID, shader.cameras.getGameCamera().combined);
			}
		};

		public final static Setter lightMatrix = new GlobalSetter() {

			Matrix4 lightSpaceMatrix = new Matrix4();
			@Override
			public void set (BaseSceneShader shader, int inputID, SceneRenderable renderable, Attributes combinedAttributes) {
				final ShadowPassSystem system = shader.world.getSystem(ShadowPassSystem.class);
				final int shadowMapSize = system.getShadowMapSize();

				shader.sceneEnvironment.calculateDirectionLightSpaceMatrix(shader.cameras.getGameCamera(), lightSpaceMatrix, shadowMapSize);

				shader.set(inputID, lightSpaceMatrix);
			}
		};

		//object
		public final static Setter objectSRT = new LocalSetter() {
			@Override
			public void set (BaseSceneShader shader, int inputID, SceneRenderable renderable, Attributes combinedAttributes) {
				shader.set(inputID, renderable.worldTransform);
			}
		};

		public static class Bones extends LocalSetter {
			private final static Matrix4 idtMatrix = new Matrix4();
			public final float bones[];

			public Bones (final int numBones) {
				this.bones = new float[numBones * 16];
			}

			@Override
			public void set (BaseSceneShader shader, int inputID, SceneRenderable renderable, Attributes combinedAttributes) {
				for (int i = 0; i < bones.length; i += 16) {
					final int idx = i / 16;
					if (renderable.bones == null || idx >= renderable.bones.length || renderable.bones[idx] == null) {
						System.arraycopy(idtMatrix.val, 0, bones, i, 16);
					} else {
						System.arraycopy(renderable.bones[idx].val, 0, bones, i, 16);
					}
				}
				shader.program.setUniformMatrix4fv(shader.loc(inputID), bones, 0, bones.length);
			}
		}
	}

	//global
	private int u_projTrans;
	private int u_lightMatrix;
	//object
	private int u_objectSRT;
	private int u_bones;


	public ShadowMapShader (FileHandle vertexSource, FileHandle fragmentSource, SceneRenderable sceneRenderable, World world) {
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
		//global
		u_projTrans = register(Inputs.cameraProjTrans, Setters.cameraProjTrans);
		u_lightMatrix = register(Inputs.lightMatrix, Setters.lightMatrix);
		//object
		u_objectSRT = register(Inputs.objectSRT, Setters.objectSRT);
		u_bones = (sceneRenderable.bones != null) ? register(DepthShader.Inputs.bones, new DepthShader.Setters.Bones(60))
			: -1;
	}

	@Override
	public int compareTo (BaseSceneShader o) {
		return 0;
	}

}

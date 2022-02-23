package com.rockbite.bongo.engine.gltf.scene.shader.bundled;

import com.artemis.BaseSystem;
import com.artemis.World;
import com.artemis.utils.ImmutableBag;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Attributes;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.attributes.DepthTestAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.shaders.BaseShader;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
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
import com.rockbite.bongo.engine.systems.render.EnvironmentConfigSystem;
import com.rockbite.bongo.engine.systems.render.ShadowPassSystem;


public class ShadedShader extends BaseSceneShader  {

	private final static long optionalAttributes = IntAttribute.CullFace | DepthTestAttribute.Type;


	public static class Inputs {
		//global
		public final static Uniform cameraPosition = new Uniform("u_cameraPosition");
		public final static Uniform cameraProjTrans = new Uniform("u_projTrans");
		public final static Uniform time = new Uniform("u_time");

		//Object
		public final static Uniform objectSRT = new Uniform("u_srt");
		public final static Uniform normalMatrix = new Uniform("u_normalMatrix");
		public final static Uniform bones = new Uniform("u_jointMatrix");

		//object textures
		public final static Uniform baseColourTexture = new Uniform("u_baseColourTexture");
		public final static Uniform normalTexture = new Uniform("u_normalTexture");
		public final static Uniform metallicRoughnessTexture = new Uniform("u_metallicRoughnessTexture");
		public final static Uniform emissiveTexture = new Uniform("u_emissiveTexture");
		public final static Uniform occlusionTexture = new Uniform("u_occlusionTexture");

		//object floats
		public final static Uniform occlusionStrength = new Uniform("u_occlusionStrength");
		public final static Uniform normalScale = new Uniform("u_normalScale");
		public final static Uniform baseColourModifier = new Uniform("u_baseColourModifier");
		public final static Uniform roughnessModifier = new Uniform("u_roughnessModifier");
		public final static Uniform metallicModifier = new Uniform("u_metallicModifier");
		public final static Uniform emissiveModifier = new Uniform("u_emissiveModifier");



		//lighting
		public final static Uniform shadowLightDir = new Uniform("u_lightDir");
		public final static Uniform shadowLightColour = new Uniform("u_lightColour");
		public final static Uniform pointLights = new Uniform("u_pointLights");
		public final static Uniform envMap = new Uniform("u_envMap");


		public final static Uniform shadowLightMatrix = new Uniform("u_lightMatrix") {
			@Override
			public boolean validate (BaseSceneShader shader, int inputID, SceneRenderable renderable) {
				boolean hasShadowSystem = shader.hasSystem(ShadowPassSystem.class);
				return hasShadowSystem && super.validate(shader, inputID, renderable);
			}
		};

		public final static Uniform shadowMap = new Uniform("u_shadowMap") {
			@Override
			public boolean validate (BaseSceneShader shader, int inputID, SceneRenderable renderable) {
				boolean hasShadowSystem = shader.hasSystem(ShadowPassSystem.class);
				return hasShadowSystem && super.validate(shader, inputID, renderable);
			}
		};



	}

	public static class Setters {

		//Global
		public final static Setter cameraPosition = new GlobalSetter() {
			@Override
			public void set (BaseSceneShader shader, int inputID, SceneRenderable renderable, Attributes combinedAttributes) {
				shader.set(inputID, shader.cameras.getGameCamera().position);
			}
		};
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
			private final Matrix3 tmpM = new Matrix3();

			@Override
			public void set (BaseSceneShader shader, int inputID, SceneRenderable renderable, Attributes combinedAttributes) {
				shader.set(inputID, tmpM.set(renderable.getWorldTransform()).inv().transpose());
			}
		};

		//object textures
		public final static Setter baseColourTexture = new LocalSetter() {
			@Override
			public void set (BaseSceneShader shader, int inputID, SceneRenderable renderable, Attributes combinedAttributes) {
				final PBRMaterialAttribute pbrMaterial = renderable.material.getAttributes().get(PBRMaterialAttribute.class, PBRMaterialAttribute.BaseColourTexture);
				shader.set(inputID, shader.context.textureBinder.bind(pbrMaterial.textureDescriptor));
			}
		};
		public final static Setter normalTexture = new LocalSetter() {
			@Override
			public void set (BaseSceneShader shader, int inputID, SceneRenderable renderable, Attributes combinedAttributes) {
				final PBRMaterialAttribute pbrMaterial = renderable.material.getAttributes().get(PBRMaterialAttribute.class, PBRMaterialAttribute.NormalTexture);
				shader.set(inputID, shader.context.textureBinder.bind(pbrMaterial.textureDescriptor));
			}
		};
		public final static Setter metallicRoughnessTexture = new LocalSetter() {
			@Override
			public void set (BaseSceneShader shader, int inputID, SceneRenderable renderable, Attributes combinedAttributes) {
				final PBRMaterialAttribute pbrMaterial = renderable.material.getAttributes().get(PBRMaterialAttribute.class, PBRMaterialAttribute.MetalRoughnessTexture);
				shader.set(inputID, shader.context.textureBinder.bind(pbrMaterial.textureDescriptor));
			}
		};
		public final static Setter emissiveTexture = new LocalSetter() {
			@Override
			public void set (BaseSceneShader shader, int inputID, SceneRenderable renderable, Attributes combinedAttributes) {
				final PBRMaterialAttribute pbrMaterial = renderable.material.getAttributes().get(PBRMaterialAttribute.class, PBRMaterialAttribute.EmissiveTexture);
				shader.set(inputID, shader.context.textureBinder.bind(pbrMaterial.textureDescriptor));
			}
		};
		public final static Setter occlusionTexture = new LocalSetter() {
			@Override
			public void set (BaseSceneShader shader, int inputID, SceneRenderable renderable, Attributes combinedAttributes) {
				final PBRMaterialAttribute pbrMaterial = renderable.material.getAttributes().get(PBRMaterialAttribute.class, PBRMaterialAttribute.OcclusionTexture);
				shader.set(inputID, shader.context.textureBinder.bind(pbrMaterial.textureDescriptor));
			}
		};

		//object floats
		public final static Setter occlusionStrength = new LocalSetter() {
			@Override
			public void set (BaseSceneShader shader, int inputID, SceneRenderable renderable, Attributes combinedAttributes) {
				final PBRFloatAttribute pbrFloatAttribute = renderable.material.getAttributes().get(PBRFloatAttribute.class, PBRFloatAttribute.OcclusionStrength);
				shader.set(inputID, pbrFloatAttribute.value);
			}
		};
		public final static Setter normalScale = new LocalSetter() {
			@Override
			public void set (BaseSceneShader shader, int inputID, SceneRenderable renderable, Attributes combinedAttributes) {
				final PBRFloatAttribute pbrFloatAttribute = renderable.material.getAttributes().get(PBRFloatAttribute.class, PBRFloatAttribute.NormalMaterialScale);
				shader.set(inputID, pbrFloatAttribute.value);
			}
		};
		public final static Setter roughnessModifier = new LocalSetter() {
			@Override
			public void set (BaseSceneShader shader, int inputID, SceneRenderable renderable, Attributes combinedAttributes) {
				final PBRFloatAttribute pbrFloatAttribute = renderable.material.getAttributes().get(PBRFloatAttribute.class, PBRFloatAttribute.Roughness);
				shader.set(inputID, pbrFloatAttribute.value);
			}
		};
		public final static Setter metallicModifier = new LocalSetter() {
			@Override
			public void set (BaseSceneShader shader, int inputID, SceneRenderable renderable, Attributes combinedAttributes) {
				final PBRFloatAttribute pbrFloatAttribute = renderable.material.getAttributes().get(PBRFloatAttribute.class, PBRFloatAttribute.Metallic);
				shader.set(inputID, pbrFloatAttribute.value);
			}
		};

		//object vecs
		public final static Setter baseColourModifier = new LocalSetter() {
			@Override
			public void set (BaseSceneShader shader, int inputID, SceneRenderable renderable, Attributes combinedAttributes) {
				final PBRColourAttribute pbrColourAttribute = renderable.material.getAttributes().get(PBRColourAttribute.class, PBRColourAttribute.BaseColourModifier);
				shader.set(inputID, pbrColourAttribute.color);
			}
		};
		public final static Setter emissiveModifier = new LocalSetter() {
			@Override
			public void set (BaseSceneShader shader, int inputID, SceneRenderable renderable, Attributes combinedAttributes) {
				final PBRVec3Attribute pbrVec3Attribute = renderable.material.getAttributes().get(PBRVec3Attribute.class, PBRVec3Attribute.EmissiveFactor);
				shader.set(inputID, pbrVec3Attribute.vec3);
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


		//lighting
		public final static Setter shadowLightMatrix = new GlobalSetter() {
			Matrix4 lightSpaceMatrix = new Matrix4();
			@Override
			public void set (BaseSceneShader shader, int inputID, SceneRenderable renderable, Attributes combinedAttributes) {
				final ShadowPassSystem system = shader.world.getSystem(ShadowPassSystem.class);
				final int shadowMapSize = system.getShadowMapSize();

				shader.sceneEnvironment.calculateDirectionLightSpaceMatrix(shader.cameras.getGameCamera(), lightSpaceMatrix, shadowMapSize);

				shader.set(inputID, lightSpaceMatrix);
			}
		};
		public final static Setter shadowLightDir = new GlobalSetter() {
			@Override
			public void set (BaseSceneShader shader, int inputID, SceneRenderable renderable, Attributes combinedAttributes) {
				shader.set(inputID, shader.sceneEnvironment.getDirectionalLightDir());
			}
		};
		public final static Setter shadowLightColour = new GlobalSetter() {
			@Override
			public void set (BaseSceneShader shader, int inputID, SceneRenderable renderable, Attributes combinedAttributes) {
				shader.set(inputID, shader.sceneEnvironment.getDirectionLightColor());
			}
		};
		public final static Setter pointLights = new GlobalSetter() {
			@Override
			public void set (BaseSceneShader shader, int inputID, SceneRenderable renderable, Attributes combinedAttributes) {
				Array<PointLight> pointLights = shader.sceneEnvironment.getPointLights();
				int maxPointLights = shader.sceneEnvironment.getMaxPointLights();
				int idx = 0;
				for (int i = 0; i < maxPointLights; i++) {
					PointLight pointLight = pointLights.get(i);
					shader.program.setUniformf("u_pointLights["+i+"].worldPosition", pointLight.getWorldPosition());
					shader.program.setUniformf("u_pointLights["+i+"].rgbRadiance", pointLight.getR(), pointLight.getG(), pointLight.getB(), pointLight.getStrength());
					idx++;
				}

				int remaining = maxPointLights - idx;
				for (int i = 0; i < remaining; i++) {
					shader.program.setUniformf("u_pointLights["+i+"].worldPosition", 0, 0, 0);
					shader.program.setUniformf("u_pointLights["+i+"].rgbRadiance", 0f, 0f, 0f, 0f);

				}
			}
		};


		public final static Setter envMap = new GlobalSetter() {
			@Override
			public void set (BaseSceneShader shader, int inputID, SceneRenderable renderable, Attributes combinedAttributes) {
				shader.set(inputID, shader.context.textureBinder.bind(shader.sceneEnvironment.getEnvMap()));
			}
		};
		public final static Setter shadowMap = new GlobalSetter() {
			@Override
			public void set (BaseSceneShader shader, int inputID, SceneRenderable renderable, Attributes combinedAttributes) {
				final ShadowPassSystem shadowPassSystem = shader.world.getSystem(ShadowPassSystem.class);
				Texture shadowMapTexture = shadowPassSystem.getShadowMapDepthTexture();
				shader.set(inputID, shader.context.textureBinder.bind(shadowMapTexture));
			}
		};
	}

	//global
	private int u_time;
	private int u_cameraPosition;
	private int u_projTrans;


	//object
	private int u_objectSRT;
	private int u_normalMatrix;
	private int u_bones;

	//object textures
	private int u_baseColourTexture;
	private int u_metallicRoughnessTexture;
	private int u_normalTexture;
	private int u_emissiveTexture;
	private int u_occlusionTexture;

	//object vecs
	private int u_baseColourModifier;
	private int u_metallicModifier;
	private int u_roughnessModifier;
	private int u_normalScale;
	private int u_occlusionStrength;
	private int u_emissiveModifier;

	//lighting
	private int u_lightSpaceMatrix;
	private int u_lightDir;
	private int u_lightColour;
	private int u_pointLights;
	private int u_envMap;
	private int u_shadowMap;


	public ShadedShader (FileHandle vertexSource, FileHandle fragmentSource, SceneRenderable sceneRenderable, World world) {
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

		if (has(u_time)) {
			set(u_time, sceneEnvironment.getTime());
		}
	}



	@Override
	public void initClassSpecificUniforms () {
		//global
		u_time = register(Inputs.time);
		u_cameraPosition = register(Inputs.cameraPosition, Setters.cameraPosition);
		u_projTrans = register(Inputs.cameraProjTrans, Setters.cameraProjTrans);

		//object
		u_objectSRT = register(Inputs.objectSRT, Setters.objectSRT);
		u_normalMatrix = register(Inputs.normalMatrix, Setters.normalMatrix);

		//object materials
		u_baseColourTexture = register(Inputs.baseColourTexture, Setters.baseColourTexture);
		u_metallicRoughnessTexture = register(Inputs.metallicRoughnessTexture, Setters.metallicRoughnessTexture);
		u_normalTexture = register(Inputs.normalTexture, Setters.normalTexture);
		u_emissiveTexture = register(Inputs.emissiveTexture, Setters.emissiveTexture);
		u_occlusionTexture = register(Inputs.occlusionTexture, Setters.occlusionTexture);

		//object vecs
		u_baseColourModifier = register(Inputs.baseColourModifier, Setters.baseColourModifier);
		u_metallicModifier = register(Inputs.metallicModifier, Setters.metallicModifier);
		u_roughnessModifier = register(Inputs.roughnessModifier, Setters.roughnessModifier);
		u_normalScale = register(Inputs.normalScale, Setters.normalScale);
		u_occlusionStrength = register(Inputs.occlusionStrength, Setters.occlusionStrength);
		u_emissiveModifier = register(Inputs.emissiveModifier, Setters.emissiveModifier);

		u_bones = (sceneRenderable.bones != null) ? register(Inputs.bones, new Setters.Bones(60))
			: -1;

		//lighting
		u_lightDir = register(Inputs.shadowLightDir, Setters.shadowLightDir);
		u_lightColour = register(Inputs.shadowLightColour, Setters.shadowLightColour);
		u_pointLights = register(Inputs.pointLights, Setters.pointLights);
		u_envMap = register(Inputs.envMap, Setters.envMap);

		u_lightSpaceMatrix = register(Inputs.shadowLightMatrix, Setters.shadowLightMatrix);
		u_shadowMap = register(Inputs.shadowMap, Setters.shadowMap);
	}




	@Override
	protected String createPrefix (SceneRenderable sceneRenderable) {
		String prefix = super.createPrefix(sceneRenderable);

		final Attributes attributes = sceneRenderable.material.getAttributes();
		tmpAttributes.clear();
		tmpAttributes.set(attributes);
		final VertexAttributes vertexAttributes = sceneRenderable.sceneMesh.getVertexInfo().getVertexAttributes();

		final long attributesMask = attributes.getMask();
		final long vertexMask = vertexAttributes.getMask();


		if ((attributesMask & PBRMaterialAttribute.BaseColourTexture) == PBRMaterialAttribute.BaseColourTexture) {
			prefix += "#define " + PBRMaterialAttribute.BaseColourTextureAlias + "Flag\n";
		}
		if ((attributesMask & PBRMaterialAttribute.EmissiveTexture) == PBRMaterialAttribute.EmissiveTexture) {
			prefix += "#define " + PBRMaterialAttribute.EmissiveTextureAlias + "Flag\n";
		}
		if ((attributesMask & PBRMaterialAttribute.OcclusionTexture) == PBRMaterialAttribute.OcclusionTexture) {
			prefix += "#define " + PBRMaterialAttribute.OcclusionTextureAlias + "Flag\n";
		}
		if ((attributesMask & PBRMaterialAttribute.NormalTexture) == PBRMaterialAttribute.NormalTexture) {
			prefix += "#define " + PBRMaterialAttribute.NormalTextureAlias + "Flag\n";
		}
		if ((attributesMask & PBRMaterialAttribute.MetalRoughnessTexture) == PBRMaterialAttribute.MetalRoughnessTexture) {
			prefix += "#define " + PBRMaterialAttribute.MetalRoughnessTextureAlias + "Flag\n";
		}

		EnvironmentConfigSystem environmentConfigSystem = world.getSystem(EnvironmentConfigSystem.class);
		SceneEnvironment sceneEnvironmentFromSystem = environmentConfigSystem.getEnvironment().getSceneEnvironment();

		prefix += "#define NumDirectionalLights 1\n";
		prefix += "#define NumPointLights " + sceneEnvironmentFromSystem.getMaxPointLights() + "\n";


		final ShadowPassSystem shadowMapSystem = world.getSystem(ShadowPassSystem.class);
		if (shadowMapSystem != null) {
			prefix += "#define shadowMapFlag\n";
		}

		return prefix;
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

package com.rockbite.bongo.engine.gltf.scene.shader;

import com.artemis.BaseSystem;
import com.artemis.EntityEdit;
import com.artemis.World;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GLTexture;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Attributes;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.graphics.g3d.utils.TextureDescriptor;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.IntIntMap;
import com.rockbite.bongo.engine.components.render.ShaderControlResource;
import com.rockbite.bongo.engine.components.singletons.Cameras;
import com.rockbite.bongo.engine.components.singletons.RenderUtils;
import com.rockbite.bongo.engine.fileutil.ReloadUtils;
import com.rockbite.bongo.engine.gltf.scene.SceneEnvironment;
import com.rockbite.bongo.engine.gltf.scene.SceneRenderable;
import com.rockbite.bongo.engine.systems.render.ShaderProcessingSystem;
import lombok.Data;

public abstract class BaseSceneShader implements Comparable<BaseSceneShader>, ReloadUtils.AutoReloadingListener, ShaderControlProvider {

	private final ShaderProcessingSystem.CombinedVertFragFiles combinedVertFragFiles;

	public SceneRenderable sceneRenderable;

	private final long attributesMask;
	private final long vertexMask;

	private String prefix;

	protected Attributes tmpAttributes = new Attributes();

	public BaseSceneShader (FileHandle vertexSource, FileHandle fragmentSource, SceneRenderable sceneRenderable, World world) {

		combinedVertFragFiles = ShaderProcessingSystem.generateShaderDependencies(this, vertexSource, fragmentSource);

		this.sceneRenderable = sceneRenderable.copy();
		this.world = world;

		final Attributes attributes = combineAttributes(sceneRenderable);
		final VertexAttributes vertexAttributes = sceneRenderable.sceneMesh.getVertexInfo().getVertexAttributes();

		attributesMask = attributes.getMask() | getOptionalAttributes();
		vertexMask = vertexAttributes.getMaskWithSizePacked();


		prefix = createPrefix(sceneRenderable);

		resetAndRecompileShader();

		final int i = world.create();
		final EntityEdit edit = world.edit(i);
		final ShaderControlResource shaderResource = edit.create(ShaderControlResource.class);
		shaderResource.setShaderControlProvider(this);
	}

	protected abstract long getOptionalAttributes ();

	private final Attributes combineAttributes (final SceneRenderable renderable) {
		tmpAttributes.clear();
		if (renderable.material != null) tmpAttributes.set(renderable.material.getAttributes());
		return tmpAttributes;
	}

	private final long combineAttributeMasks (final SceneRenderable renderable) {
		long mask = 0;
		if (renderable.material != null) mask |= renderable.material.getMask();
		return mask;
	}


	public static final boolean and (final long mask, final long flag) {
		return (mask & flag) == flag;
	}

	public static final boolean or (final long mask, final long flag) {
		return (mask & flag) != 0;
	}


	private Array<ShaderControl> shaderControls = new Array<>();

	@Override
	public Array<ShaderControl> getShaderControls () {
		return shaderControls;
	}

	@Override
	public String getShaderDisplayName () {
		return getClass().getSimpleName();
	}

	@Data
	public abstract static class ShaderControl {
		String uniformName;

		ShaderControl (String uniformName) {
			this.uniformName = uniformName;
		}

		public abstract void injectIntoShader (ShaderProgram shader);
	}

	@Data
	public static class FloatShaderControl extends ShaderControl {

		int numComponents;
		float[] buffer1 = new float[1];
		float[] buffer2 = new float[2];
		float[] buffer3 = new float[3];
		float[] buffer4 = new float[4];

		boolean hasRange;
		boolean hasColour;
		float min;
		float max;

		public FloatShaderControl (String uniformName, int numComponents) {
			super(uniformName);
			this.numComponents = numComponents;
		}

		public void rangeInfo (float min, float max) {
			hasRange = true;
			this.min = min;
			this.max = max;
		}

		@Override
		public void injectIntoShader (ShaderProgram shader) {
			switch (numComponents) {
			case 1:
				shader.setUniformf(uniformName, buffer1[0]);
				return;
			case 2:
				shader.setUniformf(uniformName, buffer2[0], buffer2[1]);
				return;
			case 3:
				shader.setUniformf(uniformName, buffer3[0], buffer3[1], buffer3[2]);
				return;
			case 4:
				shader.setUniformf(uniformName, buffer4[0], buffer4[1], buffer4[2], buffer4[3]);
				return;
			}
		}

		public void colour (boolean colour) {
    		this.hasColour = colour;
		}

		public void defaults (float[] floatDefaults) {
			if (numComponents == 1) {
				buffer1[0] = floatDefaults[0];
			}
			if (numComponents == 1) {
				buffer2[0] = floatDefaults[0];
				buffer2[1] = floatDefaults[1];
			}
			if (numComponents == 3) {
				buffer3[0] = floatDefaults[0];
				buffer3[1] = floatDefaults[1];
				buffer3[2] = floatDefaults[2];
			}
			if (numComponents == 4) {
				buffer4[0] = floatDefaults[0];
				buffer4[1] = floatDefaults[1];
				buffer4[2] = floatDefaults[2];
				buffer4[3] = floatDefaults[3];
			}
    	}

	}


	private void resetAndRecompileShader () {

		reset();

		final ShaderProcessingSystem.ProgramExtraction programExtraction = ShaderProcessingSystem.compileAndExtract(prefix, combinedVertFragFiles);
		if (programExtraction != null) {
			if (this.program != null) {
				program.dispose();
			}

			this.program = programExtraction.getProgram();
			this.shaderControls = programExtraction.getShaderControls();
		}
		initClassSpecificUniforms();
	}



	protected String createPrefix (SceneRenderable sceneRenderable) {

		final Attributes attributes = sceneRenderable.material.getAttributes();
		tmpAttributes.clear();
		tmpAttributes.set(attributes);
		final VertexAttributes vertexAttributes = sceneRenderable.sceneMesh.getVertexInfo().getVertexAttributes();
		String prefix = "";

		final long attributesMask = attributes.getMask();
		final long vertexMask = vertexAttributes.getMask();

		if (and(vertexMask, VertexAttributes.Usage.Position)) prefix += "#define positionFlag\n";
		if (and(vertexMask, VertexAttributes.Usage.Normal)) prefix += "#define normalFlag\n";
		if (and(vertexMask, VertexAttributes.Usage.ColorPacked)) prefix += "#define colorFlag\n";
		if (and(vertexMask, VertexAttributes.Usage.BoneWeight)) prefix += "#define skinningFlag\n";


		return prefix;
	}


	public abstract void initClassSpecificUniforms ();



	@Override
	public void onAutoReloadFileChanged () {
		resetAndRecompileShader();
		init();
	}

	public boolean hasSystem (Class<? extends BaseSystem> shadowPassSystemClass) {
		return world.getSystem(shadowPassSystemClass) != null;
	}

	public interface Validator {
		/** @return True if the input is valid for the renderable, false otherwise. */
		boolean validate (final BaseSceneShader shader, final int inputID, final SceneRenderable renderable);
	}

	public interface Setter {
		/** @return True if the uniform only has to be set once per render call, false if the uniform must be set for each renderable. */
		boolean isGlobal (final BaseSceneShader shader, final int inputID);

		void set (final BaseSceneShader shader, final int inputID, final SceneRenderable renderable, final Attributes combinedAttributes);
	}

	public abstract static class GlobalSetter implements Setter {
		@Override
		public boolean isGlobal (final BaseSceneShader shader, final int inputID) {
			return true;
		}
	}

	public abstract static class LocalSetter implements Setter {
		@Override
		public boolean isGlobal (final BaseSceneShader shader, final int inputID) {
			return false;
		}
	}

	public static class Uniform implements Validator {

		public final String alias;
		public final long materialMask;
		public final long environmentMask;
		public final long overallMask;

		public Uniform (final String alias, final long materialMask, final long environmentMask, final long overallMask) {
			this.alias = alias;
			this.materialMask = materialMask;
			this.environmentMask = environmentMask;
			this.overallMask = overallMask;
		}

		public Uniform (final String alias, final long materialMask, final long environmentMask) {
			this(alias, materialMask, environmentMask, 0);
		}

		public Uniform (final String alias, final long overallMask) {
			this(alias, 0, 0, overallMask);
		}

		public Uniform (final String alias) {
			this(alias, 0, 0);
		}

		public boolean validate (final BaseSceneShader shader, final int inputID, final SceneRenderable renderable) {
			final long matFlags = (renderable != null && renderable.material != null) ? renderable.material.getMask() : 0;
			return ((matFlags & materialMask) == materialMask)
				&& (((matFlags) & overallMask) == overallMask);
		}
	}

	private final Array<String> uniforms = new Array<String>();
	private final Array<Validator> validators = new Array<Validator>();
	private final Array<Setter> setters = new Array<Setter>();
	private int locations[];
	private final IntArray globalUniforms = new IntArray();
	private final IntArray localUniforms = new IntArray();
	private final IntIntMap attributes = new IntIntMap();

	public ShaderProgram program;
	public SceneEnvironment sceneEnvironment;
	public World world;
	public Cameras cameras;
	public RenderUtils renderUtils;
	public RenderContext context;
	private Mesh currentMesh;

	/** Register an uniform which might be used by this shader. Only possible prior to the call to init().
	 * @return The ID of the uniform to use in this shader. */
	public int register (final String alias, final Validator validator, final Setter setter) {
		if (locations != null) throw new GdxRuntimeException("Cannot register an uniform after initialization");
		final int existing = getUniformID(alias);
		if (existing >= 0) {
			validators.set(existing, validator);
			setters.set(existing, setter);
			return existing;
		}
		uniforms.add(alias);
		validators.add(validator);
		setters.add(setter);
		return uniforms.size - 1;
	}

	public int register (final String alias, final Validator validator) {
		return register(alias, validator, null);
	}

	public int register (final String alias, final Setter setter) {
		return register(alias, null, setter);
	}

	public int register (final String alias) {
		return register(alias, null, null);
	}

	public int register (final Uniform uniform, final Setter setter) {
		return register(uniform.alias, uniform, setter);
	}

	public int register (final Uniform uniform) {
		return register(uniform, null);
	}

	/** @return the ID of the input or negative if not available. */
	public int getUniformID (final String alias) {
		final int n = uniforms.size;
		for (int i = 0; i < n; i++)
			if (uniforms.get(i).equals(alias)) return i;
		return -1;
	}

	/** @return The input at the specified id. */
	public String getUniformAlias (final int id) {
		return uniforms.get(id);
	}

	public void reset () {
		locations = null;
		uniforms.clear();
		validators.clear();
		setters.clear();
		globalUniforms.clear();
		localUniforms.clear();
		attributes.clear();
		shaderControls.clear();

	}

	/** Initialize this shader, causing all registered uniforms/attributes to be fetched. */
	public void init (final ShaderProgram program, final SceneRenderable renderable) {
		if (locations != null) {
			throw new GdxRuntimeException("Already initialized");
		}
		if (!program.isCompiled()) {
			throw new GdxRuntimeException(program.getLog());
		}
		this.program = program;

		final int n = uniforms.size;
		locations = new int[n];
		for (int i = 0; i < n; i++) {
			final String input = uniforms.get(i);
			final Validator validator = validators.get(i);
			final Setter setter = setters.get(i);
			if (validator != null && !validator.validate(this, i, renderable))
				locations[i] = -1;
			else {
				locations[i] = program.fetchUniformLocation(input, false);
				if (locations[i] >= 0 && setter != null) {
					if (setter.isGlobal(this, i))
						globalUniforms.add(i);
					else
						localUniforms.add(i);
				}
			}
			if (locations[i] < 0) {
				validators.set(i, null);
				setters.set(i, null);
			}
		}
		if (renderable != null) {
			final VertexAttributes attrs = renderable.sceneMesh.getVertexInfo().getVertexAttributes();
			final int c = attrs.size();
			for (int i = 0; i < c; i++) {
				final VertexAttribute attr = attrs.get(i);
				final int location = program.getAttributeLocation(attr.alias);
				if (location >= 0) attributes.put(attr.getKey(), location);
			}
		}
	}

	public void begin (Cameras cameras, RenderUtils renderUtils, SceneEnvironment sceneEnvironment) {
		this.cameras = cameras;
		this.renderUtils = renderUtils;
		this.sceneEnvironment = sceneEnvironment;

		this.context = renderUtils.getRenderContext();

		context.setBlending(false, 0, 0);
		program.bind();
		currentMesh = null;
		for (int u, i = 0; i < globalUniforms.size; ++i) {
			if (setters.get(u = globalUniforms.get(i)) != null) {
				setters.get(u).set(this, u, null, null);
			}
		}
		for (ShaderControl shaderControl : shaderControls) {
			shaderControl.injectIntoShader(this.program);
		}
	}

	private final IntArray tempArray = new IntArray();

	private final int[] getAttributeLocations (final VertexAttributes attrs) {
		tempArray.clear();
		final int n = attrs.size();
		for (int i = 0; i < n; i++) {
			tempArray.add(attributes.get(attrs.get(i).getKey(), -1));
		}
		tempArray.shrink();
		return tempArray.items;
	}

	private Attributes combinedAttributes = new Attributes();

	public void render (SceneRenderable renderable) {
		combinedAttributes.clear();
//		if (renderable.environment != null) combinedAttributes.set(renderable.environment);
		if (renderable.material != null) combinedAttributes.set(renderable.material.getAttributes());
		render(renderable, combinedAttributes);
	}

	public void render (SceneRenderable renderable, final Attributes combinedAttributes) {
		for (int u, i = 0; i < localUniforms.size; ++i)
			if (setters.get(u = localUniforms.get(i)) != null) {
				setters.get(u).set(this, u, renderable, combinedAttributes);
			}
		if (currentMesh != renderable.sceneMesh.mesh) {
			if (currentMesh != null) currentMesh.unbind(program, tempArray.items);
			currentMesh = renderable.sceneMesh.mesh;
			currentMesh.bind(program, getAttributeLocations(renderable.sceneMesh.mesh.getVertexAttributes()));
		}

		renderable.sceneMesh.mesh.render(program, GL20.GL_TRIANGLES);

	}

	public void end () {
		if (currentMesh != null) {
			currentMesh.unbind(program, tempArray.items);
			currentMesh = null;
		}
	}

	public void dispose () {
		program = null;
		uniforms.clear();
		validators.clear();
		setters.clear();
		localUniforms.clear();
		globalUniforms.clear();
		locations = null;
	}

	/** Whether this Shader instance implements the specified uniform, only valid after a call to init(). */
	public final boolean has (final int inputID) {
		return inputID >= 0 && inputID < locations.length && locations[inputID] >= 0;
	}

	public final int loc (final int inputID) {
		return (inputID >= 0 && inputID < locations.length) ? locations[inputID] : -1;
	}

	public final boolean set (final int uniform, final Matrix4 value) {
		if (locations[uniform] < 0) return false;
		program.setUniformMatrix(locations[uniform], value);
		return true;
	}

	public final boolean set (final int uniform, final Matrix3 value) {
		if (locations[uniform] < 0) return false;
		program.setUniformMatrix(locations[uniform], value);
		return true;
	}

	public final boolean set (final int uniform, final Vector3 value) {
		if (locations[uniform] < 0) return false;
		program.setUniformf(locations[uniform], value);
		return true;
	}

	public final boolean set (final int uniform, final Vector2 value) {
		if (locations[uniform] < 0) return false;
		program.setUniformf(locations[uniform], value);
		return true;
	}

	public final boolean set (final int uniform, final Color value) {
		if (locations[uniform] < 0) return false;
		program.setUniformf(locations[uniform], value);
		return true;
	}

	public final boolean set (final int uniform, final float value) {
		if (locations[uniform] < 0) return false;
		program.setUniformf(locations[uniform], value);
		return true;
	}

	public final boolean set (final int uniform, final float v1, final float v2) {
		if (locations[uniform] < 0) return false;
		program.setUniformf(locations[uniform], v1, v2);
		return true;
	}

	public final boolean set (final int uniform, final float v1, final float v2, final float v3) {
		if (locations[uniform] < 0) return false;
		program.setUniformf(locations[uniform], v1, v2, v3);
		return true;
	}

	public final boolean set (final int uniform, final float v1, final float v2, final float v3, final float v4) {
		if (locations[uniform] < 0) return false;
		program.setUniformf(locations[uniform], v1, v2, v3, v4);
		return true;
	}

	public final boolean set (final int uniform, final int value) {
		if (locations[uniform] < 0) return false;
		program.setUniformi(locations[uniform], value);
		return true;
	}

	public final boolean set (final int uniform, final int v1, final int v2) {
		if (locations[uniform] < 0) return false;
		program.setUniformi(locations[uniform], v1, v2);
		return true;
	}

	public final boolean set (final int uniform, final int v1, final int v2, final int v3) {
		if (locations[uniform] < 0) return false;
		program.setUniformi(locations[uniform], v1, v2, v3);
		return true;
	}

	public final boolean set (final int uniform, final int v1, final int v2, final int v3, final int v4) {
		if (locations[uniform] < 0) return false;
		program.setUniformi(locations[uniform], v1, v2, v3, v4);
		return true;
	}

	public final boolean set (final int uniform, final TextureDescriptor textureDesc) {
		if (locations[uniform] < 0) return false;
		program.setUniformi(locations[uniform], context.textureBinder.bind(textureDesc));
		return true;
	}

	public final boolean set (final int uniform, final GLTexture texture) {
		if (locations[uniform] < 0) return false;
		program.setUniformi(locations[uniform], context.textureBinder.bind(texture));
		return true;
	}



	public void init () {
		init(program, sceneRenderable);
	}

	public boolean canRender (SceneRenderable renderable) {
		final long renderableMask = combineAttributeMasks(renderable);
		return (attributesMask == (renderableMask | getOptionalAttributes()))
			&& (vertexMask == renderable.sceneMesh.getVertexInfo().getVertexAttributes().getMaskWithSizePacked());
	}

}

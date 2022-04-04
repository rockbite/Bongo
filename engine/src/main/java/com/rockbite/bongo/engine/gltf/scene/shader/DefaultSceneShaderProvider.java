package com.rockbite.bongo.engine.gltf.scene.shader;

import com.artemis.World;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Constructor;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import com.rockbite.bongo.engine.gltf.scene.SceneRenderable;

public class DefaultSceneShaderProvider extends SceneShaderProvider {

	private final Constructor providedConstructor;

	private FileHandle vert;
	private FileHandle frag;

	public DefaultSceneShaderProvider (FileHandle vert, FileHandle frag, Class<? extends BaseSceneShader> sceneShaderClazz) {
		this.vert = vert;
		this.frag = frag;
		try {
			providedConstructor = ClassReflection.getConstructor(sceneShaderClazz, FileHandle.class, FileHandle.class, SceneRenderable.class, World.class);
		} catch (Exception e) {
			e.printStackTrace();
			throw new GdxRuntimeException(e);
		}

	}

	@Override
	protected BaseSceneShader createShader (SceneRenderable renderable, World world) {
		try {
			return (BaseSceneShader)providedConstructor.newInstance(vert, frag, renderable, world);
		} catch (ReflectionException e) {
			e.printStackTrace();
		}
		return null;
	}

}

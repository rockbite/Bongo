package com.rockbite.bongo.engine.render;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.rockbite.bongo.engine.Bongo;

import static com.rockbite.bongo.engine.Bongo.CORE_SHADER_DEBUG;

public class ShaderSourceProvider {

	private static final String GL20_PATH = "shaders/gl2/";
	private static final String GL30_PATH = "shaders/gl3/";

	private static String getPath () {
		if (Gdx.gl30 != null) {
			return GL30_PATH;
		} else {
			return GL20_PATH;
		}
	}


	private static FileHandle getBongoSourcePath () {
		FileHandle local = Gdx.files.local("."); //todo fix absolute path workaround

		while (!local.parent().child("bongo").exists()) {
			local = local.parent();
		}

		return local.parent().child("bongo").child("engine").child("src/main/resources/");
	}

	public static FileHandle resolveVertex (String shaderIdentifier, Files.FileType fileType) {
		if (CORE_SHADER_DEBUG && fileType == Files.FileType.Classpath) {
			//Backup until we find bongo

			return getBongoSourcePath().child(getPath() + shaderIdentifier + ".vert.glsl");
		}


		switch (fileType) {
		case Classpath:
			return Gdx.files.classpath(getPath() + shaderIdentifier + ".vert.glsl");
		case Internal:
			return Gdx.files.internal(getPath() + shaderIdentifier + ".vert.glsl");
		case External:
			return Gdx.files.external(getPath() + shaderIdentifier + ".vert.glsl");
		case Absolute:
			return Gdx.files.absolute(getPath() + shaderIdentifier + ".vert.glsl");
		case Local:
			return Gdx.files.local(getPath() + shaderIdentifier + ".vert.glsl");
		}

		throw new GdxRuntimeException("Unable to resolve file type " + fileType);

	}

	public static FileHandle resolveFragment (String shaderIdentifier, Files.FileType fileType) {
		if (CORE_SHADER_DEBUG && fileType == Files.FileType.Classpath) {
			return getBongoSourcePath().child(getPath() + shaderIdentifier + ".frag.glsl");
		}

		switch (fileType) {
		case Classpath:
			return Gdx.files.classpath(getPath() + shaderIdentifier + ".frag.glsl");
		case Internal:
			return Gdx.files.internal(getPath() + shaderIdentifier + ".frag.glsl");
		case External:
			return Gdx.files.external(getPath() + shaderIdentifier + ".frag.glsl");
		case Absolute:
			return Gdx.files.absolute(getPath() + shaderIdentifier + ".frag.glsl");
		case Local:
			return Gdx.files.local(getPath() + shaderIdentifier + ".frag.glsl");
		}

		throw new GdxRuntimeException("Unable to resolve file type " + fileType);
	}

	public static FileHandle resolveVertex (String shaderIdentifier) {
		return resolveVertex(shaderIdentifier, Files.FileType.Internal);
	}

	public static FileHandle resolveFragment (String shaderIdentifier) {
		return resolveFragment(shaderIdentifier, Files.FileType.Internal);
	}

}

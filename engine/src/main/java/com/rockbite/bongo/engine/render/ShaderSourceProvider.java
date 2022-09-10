package com.rockbite.bongo.engine.render;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.rockbite.bongo.engine.Bongo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.rockbite.bongo.engine.Bongo.CORE_SHADER_DEBUG;

public class ShaderSourceProvider {

	private static final Logger logger = LoggerFactory.getLogger(ShaderSourceProvider.class);

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
		local = Gdx.files.absolute(local.file().getAbsolutePath());
		while (!local.parent().child("bongo").exists()) {
			local = local.parent();
		}

		return local.parent().child("bongo").child("engine").child("src/main/resources/");
	}

	public static FileHandle resolveVertex (String shaderIdentifier, Files.FileType fileType) {
		String path = getPath() + shaderIdentifier + ".vert.glsl";
		if (CORE_SHADER_DEBUG && fileType == Files.FileType.Classpath) {
			//Backup until we find bongo

			return getBongoSourcePath().child(path);
		}


		switch (fileType) {
		case Classpath:
			FileHandle classpath = Gdx.files.classpath(path);
			return classpath;
		case Internal:
			return Gdx.files.internal(path);
		case External:
			return Gdx.files.external(path);
		case Absolute:
			return Gdx.files.absolute(path);
		case Local:
			return Gdx.files.local(path);
		}

		throw new GdxRuntimeException("Unable to resolve file type " + fileType);

	}

	public static FileHandle resolveFragment (String shaderIdentifier, Files.FileType fileType) {
		String path = getPath() + shaderIdentifier + ".frag.glsl";

		if (CORE_SHADER_DEBUG && fileType == Files.FileType.Classpath) {
			return getBongoSourcePath().child(path);
		}
		switch (fileType) {
		case Classpath:
			FileHandle classpath = Gdx.files.classpath(path);
			return classpath;
		case Internal:
			return Gdx.files.internal(path);
		case External:
			return Gdx.files.external(path);
		case Absolute:
			return Gdx.files.absolute(path);
		case Local:
			return Gdx.files.local(path);
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

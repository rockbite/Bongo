package com.rockbite.bongo.engine.render;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.ObjectMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpriteShaderCompiler {

	private static final Logger logger = LoggerFactory.getLogger(SpriteShaderCompiler.class);

	public static boolean UNROLL_TEXTURE_ARRAY = false;

	private final static ObjectMap<String, IntMap<ShaderProgram>> compiledShaders = new ObjectMap<>();
	private static Array<ShaderProgram> totalShaders = new Array<>();


	public static ShaderProgram getOrCreateShader (String shaderIdentifier, String vertexSource, String fragmentSource, ShaderFlags flags) {

		IntMap<ShaderProgram> entries = compiledShaders.get(shaderIdentifier);
		if (entries == null) {
			logger.trace("No shader IntMap found for descriptor. Registering");
			entries = new IntMap<>();
			compiledShaders.put(shaderIdentifier, entries);
		}

		ShaderProgram shaderProgram = entries.get(flags.getPackedMask());
		if (shaderProgram == null) {
			logger.trace("No shader found for packed mask. Compiling and registering a new shader");

			CharSequence shaderPrepend = flags.getPrepend();

			String constantVertexPrepend = "";
			String constantFragmentPrepend = "";

			constantFragmentPrepend += "#define MAX_TEXTURE_UNITS " + PolygonSpriteBatchMultiTextureMULTIBIND.maxTextureUnits + "\n";
			constantFragmentPrepend += "#define TRASH " + 0 + "\n";

			vertexSource = processMacros(vertexSource);
			fragmentSource = processMacros(fragmentSource);

			shaderProgram = new ShaderProgram(shaderPrepend + vertexSource, constantFragmentPrepend + shaderPrepend + fragmentSource);
			if (!shaderProgram.isCompiled()) {
				logger.error("ShaderError: Compile error for: " + shaderIdentifier + " with flags: " + flags.getPackedMask() + "\n" + shaderProgram.getLog());
			}

			entries.put(flags.getPackedMask(), shaderProgram);
			totalShaders.add(shaderProgram);

		}

		return shaderProgram;
	}

	private static String processMacros (String source) {

		String textureSample = Gdx.gl30 != null ? "texture" : "texture2D";

		//vec4 sampleTextureArray (int index, vec2 texCoords) {
		String token = "%SAMPLE_TEXTURE_ARRAY_CODE%";
		if (UNROLL_TEXTURE_ARRAY || Gdx.app.getType() == Application.ApplicationType.WebGL) {
			String codeBuffer = "";
			for (int i = 0; i < PolygonSpriteBatchMultiTextureMULTIBIND.maxTextureUnits; i++) {
				if (i != 0) {
					codeBuffer += " else ";
				}
				codeBuffer += "if (index == " + i + ") {\n";
				codeBuffer += "    return " + textureSample + "(u_textures[" + i + "], texCoords);\n";
				codeBuffer += "}\n";
			}

			codeBuffer += " else {\n return vec4(0.0);\n }";

			source = source.replaceAll(token, codeBuffer);

			//unroll

		} else {
			source = source.replaceAll(token, "return " + textureSample + "(u_textures[index], texCoords);");

		}


		return source;
	}


}

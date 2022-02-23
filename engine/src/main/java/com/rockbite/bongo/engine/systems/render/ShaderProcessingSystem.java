package com.rockbite.bongo.engine.systems.render;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectSet;
import com.badlogic.gdx.utils.OrderedSet;
import com.rockbite.bongo.engine.fileutil.AutoReloadingFileHandle;
import com.rockbite.bongo.engine.fileutil.ReloadUtils;
import com.rockbite.bongo.engine.gltf.scene.shader.BaseSceneShader;
import com.rockbite.bongo.engine.gltf.scene.shader.ShaderControlProvider;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShaderProcessingSystem {

	private static final Logger logger = LoggerFactory.getLogger(ShaderProcessingSystem.class);

	public static class CombinedVertFragFiles {


		ShaderStageFiles vertexStageFiles;
		ShaderStageFiles fragmentStageFiles;

		public CombinedVertFragFiles (ReloadUtils.AutoReloadingListener listener,
			OrderedSet<FileHandle> verts,
			OrderedSet<FileHandle> frags,
			ObjectMap<String, FileHandle> mapping) {

			vertexStageFiles = new ShaderStageFiles(listener, verts, mapping);
			fragmentStageFiles = new ShaderStageFiles(listener, frags, mapping);

		}

		public String getVertexShader () {
			return vertexStageFiles.getCombinedString();
		}

		public String getFragmentShader () {
			return fragmentStageFiles.getCombinedString();
		}
	}

	static class ShaderStageFiles {
		ObjectMap<String, FileHandle> mapping;

		Array<AutoReloadingFileHandle> shaderSources = new Array<>();

		public ShaderStageFiles (ReloadUtils.AutoReloadingListener listener, OrderedSet<FileHandle> handles, ObjectMap<String, FileHandle> mapping) {
			this.mapping = mapping;

			for (FileHandle handle : handles.orderedItems()) {
				final AutoReloadingFileHandle autoReloadingFileHandle = new AutoReloadingFileHandle(handle, listener);
				shaderSources.add(autoReloadingFileHandle);
			}
		}

		private String replaceContentWithInclude (String currentContent, String key, FileHandle value, ObjectMap<String, FileHandle> mapping, ObjectSet<FileHandle> included) {
			String nestedContent = value.readString();

			nestedContent = replaceAndExpandSnippet(nestedContent, mapping, included);

			currentContent = currentContent.replaceFirst(key, nestedContent);
			currentContent = currentContent.replaceAll(key, "");
			return currentContent;
		}

		private ObjectSet<FileHandle> included = new ObjectSet<>();
		private String getCombinedString () {
			included.clear();

			final AutoReloadingFileHandle first = shaderSources.first();
			//Use as source, everything else is includes

			String content = first.getHandle().readString();
			content = replaceAndExpandSnippet(content, mapping, included);
			return content;
		}

		private String replaceAndExpandSnippet (String content, ObjectMap<String, FileHandle> mapping, ObjectSet<FileHandle> included) {
			ObjectMap<String, FileHandle> copy = new ObjectMap<>();
			copy.putAll(mapping);
			for (ObjectMap.Entry<String, FileHandle> stringFileHandleEntry : copy) {
				if (included.contains(stringFileHandleEntry.value)) {
					//Already included, strip the entry
					content = content.replaceAll(stringFileHandleEntry.key, "");
					continue;
				}
				if (content.contains(stringFileHandleEntry.key)) {
					included.add(stringFileHandleEntry.value);
					content = replaceContentWithInclude(content, stringFileHandleEntry.key, stringFileHandleEntry.value, mapping, included);
				}
			}
			return content;
		}

	}

	public static CombinedVertFragFiles generateShaderDependencies (ReloadUtils.AutoReloadingListener listener, FileHandle vertexPath, FileHandle fragmentPath) {
		//Get dependencies and register a listener to recompile the base scene shader depending on individual file changes

		//Two combinedSahderDependencies, one for vert one for frag, if any of these change we need to notify this shader instance to recompile

		OrderedSet<FileHandle> verts = new OrderedSet<>();
		OrderedSet<FileHandle> frags = new OrderedSet<>();

		ObjectMap<String, FileHandle> mapping = new ObjectMap<>();


		extractDependencies(vertexPath, verts, mapping);
		extractDependencies(fragmentPath, frags, mapping);

		CombinedVertFragFiles combinedVertFragFiles = new CombinedVertFragFiles(listener, verts, frags, mapping);
		return combinedVertFragFiles;
	}


	static void extractDependencies (FileHandle currentHandle, OrderedSet<FileHandle> set, ObjectMap<String, FileHandle> includeToFileMapping) {
		final String s = currentHandle.readString();
		final String[] split = s.split("\r\n|\n");

		if (set.contains(currentHandle)) return;
		set.add(currentHandle);

		for (int i = 0; i < split.length; i++) {
			final String line = split[i];
			if (line.contains("#include ")) {
				final String[] inclusion = line.split("#include ");
				if (inclusion.length > 1) {
					String filePathWithoutQuotes = inclusion[1].replaceAll("\"", "");

					final FileHandle child = convertToFileHandleWithoutRelatives(currentHandle, filePathWithoutQuotes);

					includeToFileMapping.put(line, child);

					if (set.contains(child)) {
						continue; // already included
					}

					extractDependencies(child, set, includeToFileMapping);
				}
			}
		}
	}

	static FileHandle convertToFileHandleWithoutRelatives (FileHandle currentHandle, String filePathWithoutQuotes) {

		final String[] split = filePathWithoutQuotes.split("/");

		FileHandle handle = currentHandle.parent();

		for (String s : split) {
			if (s.equals("..")) {
				handle = handle.parent();
			} else {
				handle = handle.child(s);
			}
		}

		return handle;
	}

	@Data
	public static class ProgramExtraction implements ShaderControlProvider {
		private final String name;

		ShaderProgram program;
		Array<BaseSceneShader.ShaderControl> shaderControls;

		public ProgramExtraction (String name) {
			this.name = name;
		}

		public ProgramExtraction () {
			this.name = "NoName";
		}

		public void setFrom (ProgramExtraction programExtraction) {
			this.program = programExtraction.getProgram();
			this.shaderControls = programExtraction.getShaderControls();
		}

		@Override
		public String getShaderDisplayName () {
			return name;
		}
	}

	//@Control[range(0.0, 1.0), default()]
	static BaseSceneShader.ShaderControl extractControl (String annotation, String uniform) {
		try {
			final String[] split = uniform.split(" ");
			final String uniformKeyword = split[0];
			final String uniformType = split[1];
			String uniformName = split[2];
			uniformName = uniformName.substring(0, uniformName.length() - 1);

			boolean hasColour = false;
			boolean hasRange = false;
			float min = 0;
			float max = 1;

			float[] floatDefaults = new float[4];

			boolean isFloatComponent = false;
			int numComponents = 0;
			if (uniformType.equals("float")) {
				numComponents = 1;
				isFloatComponent = true;
			} else if (uniformType.equals("vec2")) {
				numComponents = 2;
				isFloatComponent = true;
			} else if (uniformType.equals("vec3")) {
				numComponents = 3;
				isFloatComponent = true;
			} else if (uniformType.equals("vec4")) {
				numComponents = 4;
				isFloatComponent = true;
			}

			String annotationProperties = annotation.split("@Control\\[")[1];
			annotationProperties = annotationProperties.substring(0, annotationProperties.length() - 1);


			final String[] properties = annotationProperties.split(";");
			for (String property : properties) {
				property = property.trim();
				if (property.startsWith("range")) {
					hasRange = true;
					property = property.split("range\\(")[1];
					property = property.substring(0, property.length() - 1);
					final String[] floatSplits = property.split(",");
					min = Float.parseFloat(floatSplits[0]);
					max = Float.parseFloat(floatSplits[1]);
				}
				if (property.startsWith("colour")) {
					hasColour = true;
				}
				if (property.startsWith("default")) {
					property = property.split("default\\(")[1];
					property = property.substring(0, property.length() - 1);
					final String[] floatSplits = property.split(",");

					if (numComponents > 0 && floatSplits.length > 0) {
						floatDefaults[0] = Float.parseFloat(floatSplits[0]);
					}
					if (numComponents > 1 && floatSplits.length > 1) {
						floatDefaults[1] = Float.parseFloat(floatSplits[1]);
					}
					if (numComponents > 2 && floatSplits.length > 2) {
						floatDefaults[2] = Float.parseFloat(floatSplits[2]);
					}
					if (numComponents > 3 && floatSplits.length > 3) {
						floatDefaults[3] = Float.parseFloat(floatSplits[3]);
					}

				}
			}



			if (isFloatComponent) {
				BaseSceneShader.FloatShaderControl floatShaderControl = new BaseSceneShader.FloatShaderControl(uniformName, numComponents);
				if (hasRange) {
					floatShaderControl.rangeInfo(min, max);
				}
				if (hasColour) {
					floatShaderControl.colour(true);
				}
				floatShaderControl.defaults(floatDefaults);
				return floatShaderControl;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	static String extractControls (String shaderString, Array<BaseSceneShader.ShaderControl> shaderControls) {
		StringBuilder buffer = new StringBuilder();
		final String[] split = shaderString.split("\r\n|\n");
		for (int i = 0; i < split.length; i++) {
			final String line = split[i];
			if (line.startsWith("@Control")) {
				final BaseSceneShader.ShaderControl value = extractControl(line, split[i + 1]);
				if (value != null) {
					shaderControls.add(value);
				}
				continue;
			}
			buffer.append(line);
			buffer.append("\n");
		}

		return buffer.toString();
	}

	static void printWithNum (String string) {
		int lineNum = 1;
		final String[] split = string.split("\n|\r\n");
		for (int i = 0; i < split.length; i++) {
			logger.info(lineNum++ + " " + split[i]);
		}
	}

	public static ProgramExtraction compileAndExtract (String prefix, CombinedVertFragFiles combinedVertFragFiles) {
		String vertString = combinedVertFragFiles.getVertexShader();
		String fragString = combinedVertFragFiles.getFragmentShader();

		Array<BaseSceneShader.ShaderControl> shaderControls = new Array<>();

		vertString = extractControls(vertString, shaderControls);
		fragString = extractControls(fragString, shaderControls);

		final String vertexString = prefix + vertString;
		final String fragmentString = prefix + fragString;

		ShaderProgram newProgram = new ShaderProgram(vertexString, fragmentString);
		if (!newProgram.isCompiled()) {
			printWithNum(vertexString);
			printWithNum(fragmentString);

			logger.error(newProgram.getLog());

			return null;
		}

		final ProgramExtraction programExtraction = new ProgramExtraction();
		programExtraction.program = newProgram;
		programExtraction.shaderControls = shaderControls;
		return programExtraction;
	}

}

package com.rockbite.bongo.engine.render;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.rockbite.bongo.engine.fileutil.AutoReloadingFileHandle;
import com.rockbite.bongo.engine.fileutil.ReloadUtils;
import lombok.Getter;

public class AutoReloadingShaderProgram implements ReloadUtils.AutoReloadingListener {

	private final AutoReloadingFileHandle vertexShader;
	private final AutoReloadingFileHandle fragmentShader;

	@Getter
	private ShaderProgram shaderProgram;

	public AutoReloadingShaderProgram (FileHandle vertexShader, FileHandle fragmentShader) {
		this.vertexShader = new AutoReloadingFileHandle(vertexShader, this);
		this.fragmentShader = new AutoReloadingFileHandle(fragmentShader, this);
		recompile();

	}

	private void recompile () {
		final ShaderProgram compiledProgram = new ShaderProgram(vertexShader.getHandle(), fragmentShader.getHandle());
		if (compiledProgram.isCompiled()) {
			if (this.shaderProgram != null) {
				this.shaderProgram.dispose();
			}
			this.shaderProgram = compiledProgram;
		} else {
			System.err.println(compiledProgram.getLog());
		}
	}

	@Override
	public void onAutoReloadFileChanged () {
		recompile();
	}

	public void dispose () {
		shaderProgram.dispose();
	}
}

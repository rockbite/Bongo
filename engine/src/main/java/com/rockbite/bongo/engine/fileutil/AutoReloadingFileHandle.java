package com.rockbite.bongo.engine.fileutil;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import lombok.Getter;

public class AutoReloadingFileHandle {

	@Getter
	private final FileHandle handle;

	public AutoReloadingFileHandle (FileHandle handle, ReloadUtils.AutoReloadingListener listener) {
		this.handle = handle;

		//Only run this on desktop, file is not
		if (!Gdx.app.getType().equals(Application.ApplicationType.Desktop)) {
			return;
		}

		ReloadUtils.registerFile(handle, listener);
	}


}

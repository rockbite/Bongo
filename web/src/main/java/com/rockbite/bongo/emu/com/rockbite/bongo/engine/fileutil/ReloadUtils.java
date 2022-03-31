package com.rockbite.bongo.engine.fileutil;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;

public class ReloadUtils {

	private static final Logger logger = LoggerFactory.getLogger(ReloadUtils.class);

	public interface AutoReloadingListener {
		void onAutoReloadFileChanged ();
	}


	public static void registerFile (FileHandle handle, AutoReloadingListener listener) {
	}

	public static void dispose () {
	}
}

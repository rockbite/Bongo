package com.rockbite.bongo.engine.fileutil;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import org.apache.commons.vfs2.FileChangeEvent;
import org.apache.commons.vfs2.FileListener;
import org.apache.commons.vfs2.FileMonitor;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.impl.DefaultFileMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;

public class ReloadUtils {

	private static final Logger logger = LoggerFactory.getLogger(ReloadUtils.class);

	static FileSystemManager manager;
	static DefaultFileMonitor fm;

	static {
		if (Gdx.app.getType().equals(Application.ApplicationType.Desktop)) {
			try {
				manager = VFS.getManager();
				fm = new DefaultFileMonitor(new FileListener() {
					@Override
					public void fileCreated (FileChangeEvent event) throws Exception {
					}
					@Override
					public void fileDeleted (FileChangeEvent event) throws Exception {
					}
					@Override
					public void fileChanged (FileChangeEvent event) throws Exception {
						final FileObject fileObject = event.getFileObject();
						Gdx.app.postRunnable(new Runnable() {
							@Override
							public void run () {

								final Array<AutoReloadingListener> autoReloadingListeners = listeners.get(fileObject);
								for (AutoReloadingListener autoReloadingListener : autoReloadingListeners) {
									autoReloadingListener.onAutoReloadFileChanged();
								}
							}
						});
					}
				});

				fm.setRecursive(false);
				fm.start();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public interface AutoReloadingListener {
		void onAutoReloadFileChanged ();
	}

	private static ObjectMap<FileObject, Array<AutoReloadingListener>> listeners = new ObjectMap<FileObject, Array<AutoReloadingListener>>();

	public static void registerFile (FileHandle handle, AutoReloadingListener listener) {
		try {
			final File file = handle.file();
			final URI uri = file.toURI();
			final FileObject fileObject = manager.resolveFile(uri);

			if (listeners.containsKey(fileObject)) {
				//Already registered, so we just want to add it to listeners
				listeners.get(fileObject).add(listener);
			} else {
				final Array<AutoReloadingListener> value = new Array<>();
				value.add(listener);
				listeners.put(fileObject, value);

				fm.addFile(fileObject);
				logger.info("Registered file for auto reload {}", handle.file().toURI());
			}

		} catch (FileSystemException e) {
			e.printStackTrace();
		}
	}

	public static void dispose () {
		if (Gdx.app.getType().equals(Application.ApplicationType.Desktop)) {
			fm.stop();
		}
	}
}

package com.rockbite.bongo.engine.scripts;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.LifecycleListener;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ScriptCompiler implements LifecycleListener {

	private static final Logger logger = LoggerFactory.getLogger(ScriptCompiler.class);

	private static ScriptCompiler instance;

	final ExecutorService executorService;

	public static ScriptCompiler instance () {
		if (instance == null) {
			instance = new ScriptCompiler();
		}
		return instance;
	}

	private final JavaCompiler compiler;

	public ScriptCompiler () {
		compiler = ToolProvider.getSystemJavaCompiler();
	 	executorService = Executors.newSingleThreadExecutor();

		Gdx.app.addLifecycleListener(this);
	}

	public CompletableFuture<Object> compile (String fileName, String javaString) {
		CompletableFuture<Object> completableFuture = new CompletableFuture<>();

		executorService.submit(new Runnable() {
			@Override
			public void run () {
				JavaSourceFromString file = new JavaSourceFromString(fileName, javaString);
				DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
				DynamicClassesFileManager manager = new DynamicClassesFileManager(compiler.getStandardFileManager(null, null, null));

				Iterable<? extends JavaFileObject> compilationUnits = Arrays.asList(file);
				JavaCompiler.CompilationTask task = compiler.getTask(null, manager, diagnostics, null, null, compilationUnits);

				boolean success = task.call();
				for (Diagnostic diagnostic : diagnostics.getDiagnostics()) {
					logger.error(String.format("Script compilation error: Line: %d - %s%n", diagnostic.getLineNumber(), diagnostic.getMessage(null)));
				}
				if (success) {
					try {
						Class clazz = manager.loader.findClass(fileName);

						Object object = ClassReflection.newInstance(clazz);

						completableFuture.complete(object);
					} catch (ReflectionException | ClassNotFoundException e) {
						e.printStackTrace();
					}
				}

				completableFuture.complete(null);
			}
		});

		return completableFuture;
	}

	/**
	 * Called when the {@link Application} is about to pause
	 */
	@Override
	public void pause () {

	}

	/**
	 * Called when the Application is about to be resumed
	 */
	@Override
	public void resume () {

	}

	/**
	 * Called when the {@link Application} is about to be disposed
	 */
	@Override
	public void dispose () {
		executorService.shutdownNow();
	}

	public static class ByteClassLoader extends ClassLoader {

		private ObjectMap<String, JavaSourceFromString> cache = new ObjectMap<>();

		public ByteClassLoader () {
			super(ByteClassLoader.class.getClassLoader());
		}

		public void put (String name, JavaSourceFromString obj) {
			cache.put(name, obj);
		}

		@Override
		protected Class<?> findClass (String name) throws ClassNotFoundException {
			if (cache.containsKey(name)) {
				final JavaSourceFromString javaSourceFromString = cache.get(name);
				final byte[] classBytes = javaSourceFromString.getClassBytes();
				return defineClass(name, classBytes, 0, classBytes.length);
			}

			throw new GdxRuntimeException("Woopsy");
		}
	}


	public static class JavaSourceFromString extends SimpleJavaFileObject {

		private final ByteArrayOutputStream bos = new ByteArrayOutputStream();

		String code;

		JavaSourceFromString(String name, String code) {
			super(URI.create("string:///" + name.replace('.','/') + Kind.SOURCE.extension),Kind.SOURCE);
			this.code = code;
		}

		JavaSourceFromString(String name, Kind kind) {
			super(URI.create("string:///" + name.replace('.', '/') + kind.extension), kind);
		}

		@Override
		public CharSequence getCharContent(boolean ignoreEncodingErrors) {
			return code;
		}

		public byte[] getClassBytes () {
			return bos.toByteArray();
		}

		@Override
		public OutputStream openOutputStream () throws IOException {
			return bos;
		}
	}

	public static class DynamicClassesFileManager<FileManager> extends ForwardingJavaFileManager<JavaFileManager> {

		ByteClassLoader loader = null;

		protected DynamicClassesFileManager (StandardJavaFileManager fileManager) {
			super(fileManager);
			try {
				loader = new ByteClassLoader();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		public JavaFileObject getJavaFileForOutput (Location location, String className, JavaFileObject.Kind kind, FileObject sibling) throws IOException {
			JavaSourceFromString obj = new JavaSourceFromString(className, kind);
			loader.put(className, obj);
			return obj;
		}

		@Override
		public ClassLoader getClassLoader (Location location) {
			return loader;
		}
	}
}

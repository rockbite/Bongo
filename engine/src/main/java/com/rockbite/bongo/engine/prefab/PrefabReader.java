package com.rockbite.bongo.engine.prefab;

import com.artemis.utils.reflect.ArrayReflection;
import com.artemis.utils.reflect.ClassReflection;
import com.artemis.utils.reflect.ReflectionException;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.Queue;
import com.moandjiezana.toml.Toml;
import com.rockbite.bongo.engine.reflect.ReflectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

public class PrefabReader {

	private static final Logger logger = LoggerFactory.getLogger(PrefabReader.class);

	public class PrefabException extends RuntimeException {

		public PrefabException (String message) {
			super(message + "\n\t\t" + componentStackToString());
		}
	}

	private String componentStackToString () {
		String buffer = "";
		for (String s : componentParsingStack) {
			buffer += s + ".";
		}
		return buffer.substring(0, buffer.length() - 1);
	}

	private Queue<String> componentParsingStack = new Queue<>();

	public ObjectMap<String, Class> objectMapper = new ObjectMap<>();

	public Object topLevelParseAndRead (String componentName, Toml data) {
		componentParsingStack.clear();

		componentParsingStack.addLast(componentName);

		Class<Object> clazz = null;
		if (componentName != null) {
			clazz =	objectMapper.get(componentName);
		}

		if (data.getString("type") != null) {
			clazz = objectMapper.get(data.getString("type"));
		}
		if (clazz == null) {
			throw new PrefabException("No class found for type: " + componentName);
		}

		try {
			Object object = createObjectFromClassAndToml(clazz, data);

			componentParsingStack.removeLast();

			return object;

		} catch (ReflectionException e) {
			throw new PrefabException("Reflection exception when trying to parse top level data: " + componentName);
		}
	}

	private Object createObjectFromClassAndToml (Class targetType, Object tomlSideData) throws ReflectionException {

		if (targetType.isEnum()) {
			if (tomlSideData instanceof String) {
				return Enum.valueOf(targetType, ((String)tomlSideData));
			} else {
				throw new PrefabException("Toml data is not compatible with field enum. Incompatible class =  " + tomlSideData.getClass());
			}
		} else if (targetType.isPrimitive()) {
			//Create a new object parsing the toml side data into friendly targetType data
			if (targetType.equals(int.class)) {
				if (tomlSideData instanceof Number) {
					tomlSideData = ((Number)tomlSideData).intValue();
				}
			}
			if (targetType.equals(float.class)) {
				if (tomlSideData instanceof Number) {
					tomlSideData = ((Number)tomlSideData).floatValue();
				}
			}
			return tomlSideData;

		} else if (targetType.equals(String.class)) {

			if (tomlSideData instanceof String) {
				return tomlSideData;
			} else {
				throw new PrefabException("Toml data is not compatible with field string. Incompatible class =  " + tomlSideData.getClass());
			}

		} else if (targetType.equals(ObjectMap.class)) {
			if (tomlSideData instanceof Toml) {
				ObjectMap<String, Object> map = new ObjectMap<>();
				final Toml toml = (Toml)tomlSideData;
				for (Map.Entry<String, Object> stringObjectEntry : toml.entrySet()) {
					map.put(stringObjectEntry.getKey(), stringObjectEntry.getValue());
				}
				return map;
			} else {
				throw new PrefabException("Toml data is not compatible with field map. Incompatible class =  " + tomlSideData.getClass());
			}
		} else if (targetType.isArray()) {
			if (tomlSideData instanceof List) {
				final Class elementType = targetType.getComponentType();
				final List<?> list = (List<?>)tomlSideData;
				final Object[] convertedList = (Object[])ArrayReflection.newInstance(elementType, list.size());
				int idx = 0;
				for (Object o : list) {
					convertedList[idx++] = createObjectFromClassAndToml(elementType, o);
				}
				return convertedList;
			} else {
				throw new PrefabException("Toml data is not compatible with field []. Incompatible class =  " + tomlSideData.getClass());
			}
		} else {
			//Complex object
			//Make a new instance

			final Object objectInstance = ClassReflection.newInstance(targetType);
			//Parse the toml data into the object

			if (tomlSideData instanceof Toml) {
				final Toml toml = (Toml)tomlSideData;

				for (Map.Entry<String, Object> stringObjectEntry : toml.entrySet()) {

					final String tomlSideKey = stringObjectEntry.getKey();
					final Object tomlSideChildData = stringObjectEntry.getValue();

					try {
						final Field declaredField = ReflectUtils.getFieldWithName(tomlSideKey, targetType, null);

						//Get the object
						componentParsingStack.addLast(declaredField.getType().getSimpleName());
						final Object child = createObjectFromClassAndToml(declaredField.getType(), tomlSideChildData);
						componentParsingStack.removeLast();

						declaredField.setAccessible(true);
						declaredField.set(objectInstance, child);


					} catch (NoSuchFieldException e) {
						throw new PrefabException("No field '" + tomlSideKey + "' found for object " + targetType.getSimpleName());
					} catch (IllegalAccessException | IllegalArgumentException e) {
						throw new PrefabException("Failure to set data for field '" + tomlSideKey + "' found for object " + targetType.getSimpleName());
					} catch (Throwable throwable) {
						throw new PrefabException("Oopsi " + throwable.getMessage());

					}

				}
				return objectInstance;

			} else {
				throw new PrefabException("Complex object cannot be parsed from " + tomlSideData);
			}
		}
	}

}

package com.rockbite.bongo.engine.collections;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectIntMap;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.ReflectionException;

import java.util.Iterator;

public class SerializableObjectIntMap<T> implements Json.Serializable, Iterable<ObjectIntMap.Entry<T>> {

		private ObjectIntMap<T> data = new ObjectIntMap<>();
		private Class<T> keyType;

		SerializableObjectIntMap () {

		}

		public SerializableObjectIntMap (Class<T> keyType) {
			this.keyType = keyType;
		}

		public int getAndIncrement (T key, int defaultValue, int increment) {
			return data.getAndIncrement(key, defaultValue, increment);
		}

		public int get (T key, int defaultValue) {
			return data.get(key, defaultValue);
		}

		public void put (T key, int value) {
			data.put(key, value);
		}

		@Override
		public void write (Json json) {
			json.writeValue("keyType", keyType.getName());
			json.writeValue("map", data);
		}

		@Override
		public void read (Json json, JsonValue jsonData) {
			String objectMapKeyType = jsonData.getString("keyType");

			Class aClass;
			try {
				aClass = ClassReflection.forName(objectMapKeyType);
				this.keyType = aClass;

				JsonValue mapData = jsonData.get("map");
				for (JsonValue mapDatum : mapData) {
					String name = mapDatum.name;
					int readValue = mapDatum.asInt();
					JsonValue jsonValue = new JsonValue(name);
					T convertedKey = json.readValue(this.keyType, jsonValue);

					this.data.put(convertedKey, readValue);
				}
			} catch (ReflectionException e) {
				throw new RuntimeException(e);
			}
		}

	@Override
	public Iterator<ObjectIntMap.Entry<T>> iterator () {
		return data.iterator();
	}
}

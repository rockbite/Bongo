package com.rockbite.bongo.engine.reflect;

import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Field;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReflectUtils {
	/**
	 * @param startClass the class whose fields is being looked
	 * @param exclusiveParent the class till which the reflection should go. can be null.
	 * @return
	 */
	public static Iterable<Field> getFieldsUpTo(Class<?> startClass, Class<?> exclusiveParent) {
		List<Field> currentClassFields = new ArrayList<Field>(Arrays.asList(ClassReflection.getDeclaredFields(startClass)));
		Class<?> parentClass = startClass.getSuperclass();

		if (parentClass != null && (exclusiveParent == null || !(parentClass.equals(exclusiveParent)))) {
			List<Field> parentClassFields = (List<Field>) getFieldsUpTo(parentClass, exclusiveParent);
			currentClassFields.addAll(parentClassFields);
		}

		return currentClassFields;
	}

	/**
	 * @param name field name
	 * @param startClass the class whose fields is being looked
	 * @param exclusiveParent the class till which the reflection should go. can be null.
	 * @return
	 * @throws Throwable
	 */
	public static Field getFieldWithName (String name, Class<?> startClass, Class<?> exclusiveParent) throws Throwable {
		Iterable<Field> fieldsUpTo = getFieldsUpTo(startClass, exclusiveParent);
		for (Field field: fieldsUpTo) {
			if (field.getName().equals(name)) {
				return field;
			}
		}

		throw new NoSuchFieldException("No field: " + name + " found in class hierarchy for class: " + startClass.getSimpleName());
	}


}

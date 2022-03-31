package com.moandjiezana.toml;

import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Field;
import com.badlogic.gdx.utils.reflect.ReflectionException;

import static com.moandjiezana.toml.MapValueWriter.MAP_VALUE_WRITER;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

class ObjectValueWriter implements ValueWriter {
  static final ValueWriter OBJECT_VALUE_WRITER = new ObjectValueWriter();

  @Override
  public boolean canWrite(Object value) {
    return true;
  }

  @Override
  public void write(Object value, WriterContext context) {
    Map<String, Object> to = new LinkedHashMap<String, Object>();
    Set<Field> fields = getFields(value.getClass());
    for (Field field : fields) {
      to.put(field.getName(), getFieldValue(field, value));
    }

    MAP_VALUE_WRITER.write(to, context);
  }

  @Override
  public boolean isPrimitiveType() {
    return false;
  }

  private static Set<Field> getFields(Class<?> cls) {
    Field[] declaredFields = ClassReflection.getDeclaredFields(cls);
    Set<Field> fields = new LinkedHashSet<Field>(Arrays.asList(declaredFields));
    while (cls != Object.class) {
      declaredFields = ClassReflection.getDeclaredFields(cls);
      fields.addAll(Arrays.asList(declaredFields));
      cls = cls.getSuperclass();
    }
    removeConstantsAndSyntheticFields(fields);

    return fields;
  }

  private static void removeConstantsAndSyntheticFields(Set<Field> fields) {
    Iterator<Field> iterator = fields.iterator();
    while (iterator.hasNext()) {
      Field field = iterator.next();

      //todo
//      if ((Modifier.isFinal(field.getModifiers()) && Modifier.isStatic(field.getModifiers())) || field.isSynthetic() || Modifier.isTransient(field.getModifiers())) {
//        iterator.remove();
//      }
    }
  }

  private static Object getFieldValue(Field field, Object o) {
    boolean isAccessible = field.isAccessible();
    field.setAccessible(true);
    Object value = null;
    try {
      value = field.get(o);
    } catch (ReflectionException e) {
      e.printStackTrace();
    }
    field.setAccessible(isAccessible);

    return value;
  }

  private ObjectValueWriter() {}
}

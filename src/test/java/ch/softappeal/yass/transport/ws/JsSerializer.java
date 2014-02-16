package ch.softappeal.yass.transport.ws;

import ch.softappeal.yass.serialize.Reflector;
import ch.softappeal.yass.serialize.fast.AbstractFastSerializer;
import ch.softappeal.yass.serialize.fast.BaseTypeHandlers;
import ch.softappeal.yass.serialize.fast.FieldHandler;
import ch.softappeal.yass.serialize.fast.TypeDesc;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is the Java implementation of the yass Javascript serializer.
 * This serializer assigns type and field id's automatically. Therefore, all peers must have the same version of the contract!
 */
public final class JsSerializer extends AbstractFastSerializer {

  private void addClass(final int typeId, final Class<?> type, final boolean referenceable) {
    checkClass(type);
    final List<Field> fields = allFields(type);
    final Map<String, Field> name2field = new HashMap<>(fields.size());
    for (final Field field : fields) {
      final Field oldField = name2field.put(field.getName(), field);
      if (oldField != null) {
        throw new IllegalArgumentException("duplicated fields '" + field + "' and '" + oldField + "' in class hierarchy");
      }
    }
    Collections.sort(fields, new Comparator<Field>() {
      @Override public int compare(final Field field1, final Field field2) {
        return field1.getName().compareTo(field2.getName());
      }
    });
    final Map<Integer, Field> id2field = new HashMap<>(fields.size());
    int fieldId = FieldHandler.FIRST_ID;
    for (final Field field : fields) {
      id2field.put(fieldId++, field);
    }
    addClass(typeId, type, referenceable, id2field);
  }

  public static final TypeDesc BOOLEAN_TYPEDESC = new TypeDesc(TypeDesc.FIRST_ID, BaseTypeHandlers.BOOLEAN);
  public static final TypeDesc INTEGER_TYPEDESC = new TypeDesc(TypeDesc.FIRST_ID + 1, BaseTypeHandlers.INTEGER);
  public static final TypeDesc STRING_TYPEDESC = new TypeDesc(TypeDesc.FIRST_ID + 2, BaseTypeHandlers.STRING);
  private static final int FIRST_ID = TypeDesc.FIRST_ID + 3;

  /**
   * @param concreteClasses instances of these classes can only be used in trees
   * @param referenceableConcreteClasses instances of these classes can be used in graphs
   */
  public JsSerializer(
    final Reflector.Factory reflectorFactory, final Collection<Class<?>> enumerations,
    final Collection<Class<?>> concreteClasses, final Collection<Class<?>> referenceableConcreteClasses
  ) {
    super(reflectorFactory);
    addBaseType(BOOLEAN_TYPEDESC);
    addBaseType(INTEGER_TYPEDESC);
    addBaseType(STRING_TYPEDESC);
    int id = FIRST_ID;
    for (final Class<?> type : enumerations) {
      addEnum(id++, type);
    }
    for (final Class<?> type : concreteClasses) {
      addClass(id++, type, false);
    }
    for (final Class<?> type : referenceableConcreteClasses) {
      addClass(id++, type, true);
    }
    fixupFields();
  }

}

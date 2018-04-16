package ch.softappeal.yass.serialize.fast;

import ch.softappeal.yass.Reflect;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This serializer assigns type and field id's automatically. Therefore, all peers must have the same version of the contract!
 */
public final class SimpleFastSerializer extends FastSerializer {

    private void addClass(final int typeId, final Class<?> type, final boolean referenceable) {
        final Map<Integer, Field> id2field = new HashMap<>(16);
        var fieldId = FieldHandler.FIRST_ID;
        for (final var field : Reflect.allFields(type)) {
            id2field.put(fieldId++, field);
        }
        addClass(typeId, type, referenceable, id2field);
    }

    /**
     * @param concreteClasses instances of these classes can only be used in trees
     * @param referenceableConcreteClasses instances of these classes can be used in graphs
     */
    public SimpleFastSerializer(final List<BaseTypeHandler<?>> baseTypeHandlers, final List<Class<?>> concreteClasses, final List<Class<?>> referenceableConcreteClasses) {
        var id = TypeDesc.FIRST_ID;
        for (final var typeHandler : baseTypeHandlers) {
            addBaseType(new TypeDesc(id++, typeHandler));
        }
        for (final var type : concreteClasses) {
            if (type.isEnum()) {
                addEnum(id++, type);
            } else {
                addClass(id++, type, false);
            }
        }
        for (final var type : referenceableConcreteClasses) {
            checkClass(type);
            addClass(id++, type, true);
        }
        fixupFields();
    }

    public SimpleFastSerializer(final List<BaseTypeHandler<?>> baseTypeHandlers, final List<Class<?>> concreteClasses) {
        this(baseTypeHandlers, concreteClasses, List.of());
    }

}

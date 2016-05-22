package ch.softappeal.yass.util;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Reflect {

    private Reflect() {
        // disable
    }

    public static final Unsafe UNSAFE;
    static {
        try {
            final Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            UNSAFE = (Unsafe)field.get(null);
        } catch (final Exception e) {
            throw Exceptions.wrap(e);
        }
    }

    public static List<Field> ownFields(final Class<?> type) {
        final List<Field> fields = new ArrayList<>(16);
        for (final Field field : type.getDeclaredFields()) {
            final int modifiers = field.getModifiers();
            if (!(Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers))) {
                fields.add(field);
            }
        }
        Collections.sort(fields, (field1, field2) -> field1.getName().compareTo(field2.getName()));
        return fields;
    }

    public static List<Field> allFields(final Class<?> type) {
        final List<Field> fields = new ArrayList<>(16);
        for (Class<?> t = Check.notNull(type); (t != null) && (t != Throwable.class); t = t.getSuperclass()) {
            fields.addAll(ownFields(t));
        }
        return fields;
    }

}

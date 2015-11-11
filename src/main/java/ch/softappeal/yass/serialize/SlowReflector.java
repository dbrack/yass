package ch.softappeal.yass.serialize;

import ch.softappeal.yass.util.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Only works if classes have a default constructor; uses public java reflection API.
 */
public final class SlowReflector implements Reflector {

    public static final Factory FACTORY = SlowReflector::new;

    private SlowReflector(final Class<?> type) throws NoSuchMethodException {
        constructor = type.getDeclaredConstructor();
        if (!Modifier.isPublic(constructor.getModifiers())) {
            constructor.setAccessible(true);
        }
    }

    private final Constructor<?> constructor;

    @Override public Object newInstance() throws Exception {
        return constructor.newInstance();
    }

    @Override public Accessor accessor(final Field field) {
        final int modifiers = field.getModifiers();
        if (!Modifier.isPublic(modifiers) || Modifier.isFinal(modifiers)) {
            field.setAccessible(true);
        }
        return new Accessor() {
            @Override public Object get(final Object object) throws IllegalAccessException {
                return field.get(object);
            }
            @Override public void set(final Object object, final @Nullable Object value) throws IllegalAccessException {
                field.set(object, value);
            }
        };
    }

}

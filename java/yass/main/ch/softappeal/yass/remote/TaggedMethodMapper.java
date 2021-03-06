package ch.softappeal.yass.remote;

import ch.softappeal.yass.Tag;
import ch.softappeal.yass.Tags;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Uses {@link Tag} as {@link Request#methodId}.
 * Uses {@link OneWay} for marking oneWay methods.
 */
public final class TaggedMethodMapper implements MethodMapper {

    private final Map<Integer, Mapping> id2mapping;

    private TaggedMethodMapper(final Class<?> contract) {
        final var methods = contract.getMethods();
        id2mapping = new HashMap<>(methods.length);
        for (final var method : methods) {
            final var id = Tags.getTag(method);
            final var oldMapping = id2mapping.put(id, new Mapping(method, id));
            if (oldMapping != null) {
                throw new IllegalArgumentException("tag " + id + " used for methods '" + method + "' and '" + oldMapping.method + '\'');
            }
        }
    }

    @Override public Mapping mapId(final int id) {
        return id2mapping.get(id);
    }

    @Override public Mapping mapMethod(final Method method) {
        return id2mapping.get(Tags.getTag(method));
    }

    public static final Factory FACTORY = TaggedMethodMapper::new;

}

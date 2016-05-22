package ch.softappeal.yass.transport;

import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Allows having different contracts (and multiple versions of the same contract) on one listener.
 */
public final class SimplePathResolver {

    private final Map<Object, SimpleTransportSetup> pathMappings = new HashMap<>(16);
    private void put(final Object path, final SimpleTransportSetup setup) {
        pathMappings.put(Check.notNull(path), Check.notNull(setup));
    }

    public SimplePathResolver(final Map<?, SimpleTransportSetup> pathMappings) {
        pathMappings.forEach(this::put);
    }

    public SimplePathResolver(final Object path, final SimpleTransportSetup setup) {
        put(path, setup);
    }

    public SimpleTransportSetup resolvePath(final Object path) {
        final @Nullable SimpleTransportSetup setup = pathMappings.get(Check.notNull(path));
        if (setup == null) {
            throw new RuntimeException("no mapping for path '" + path + '\'');
        }
        return setup;
    }

}

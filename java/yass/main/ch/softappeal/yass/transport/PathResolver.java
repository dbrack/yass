package ch.softappeal.yass.transport;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Allows having different contracts (and multiple versions of the same contract) on one listener.
 */
public final class PathResolver {

    private final Map<Object, TransportSetup> pathMappings = new HashMap<>(16);
    private void put(final Object path, final TransportSetup setup) {
        pathMappings.put(Objects.requireNonNull(path), Objects.requireNonNull(setup));
    }

    public PathResolver(final Map<?, TransportSetup> pathMappings) {
        pathMappings.forEach(this::put);
    }

    public PathResolver(final Object path, final TransportSetup setup) {
        put(path, setup);
    }

    public TransportSetup resolvePath(final Object path) {
        return Optional.ofNullable(pathMappings.get(Objects.requireNonNull(path)))
            .orElseThrow(() -> new RuntimeException("no mapping for path '" + path + '\''));
    }

}

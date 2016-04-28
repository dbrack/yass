package ch.softappeal.yass.core.remote;

import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Nullable;

import java.util.concurrent.atomic.AtomicInteger;

public class SimpleInterceptorContext {

    private static final AtomicInteger ID = new AtomicInteger(0);

    public final int id = ID.getAndIncrement();
    public final MethodMapper.Mapping methodMapping;
    public final @Nullable Object[] arguments;

    public SimpleInterceptorContext(final MethodMapper.Mapping methodMapping, final @Nullable Object[] arguments) {
        this.methodMapping = Check.notNull(methodMapping);
        this.arguments = arguments;
    }

}

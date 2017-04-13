package ch.softappeal.yass.core.remote;

import ch.softappeal.yass.core.Interceptor;

import java.lang.reflect.Method;
import java.util.Objects;

public final class Service {

    public final ContractId<?> contractId;
    final Object implementation;
    private final Object interceptor;

    private <C> Service(final ContractId<C> contractId, final C implementation, final Object interceptor) {
        this.contractId = Objects.requireNonNull(contractId);
        this.implementation = Objects.requireNonNull(implementation);
        this.interceptor = Objects.requireNonNull(interceptor);
    }

    <C> Service(final ContractId<C> contractId, final C implementation, final Interceptor interceptor) {
        this(contractId, implementation, (Object)interceptor);
    }

    <C> Service(final ContractId<C> contractId, final C implementation, final InterceptorAsync<?> interceptor) {
        this(contractId, implementation, (Object)interceptor);
    }

    boolean async() {
        return interceptor instanceof InterceptorAsync;
    }

    private Interceptor interceptor() {
        return (Interceptor)interceptor;
    }

    @SuppressWarnings("unchecked")
    InterceptorAsync<Object> interceptorAsync() {
        return (InterceptorAsync)interceptor;
    }

    Reply invokeSync(final Method method, final Object[] arguments) {
        try {
            return new ValueReply(Interceptor.invoke(interceptor(), method, arguments, implementation));
        } catch (final Exception e) {
            return new ExceptionReply(e);
        }
    }

}

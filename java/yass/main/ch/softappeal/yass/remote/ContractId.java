package ch.softappeal.yass.remote;

import ch.softappeal.yass.Interceptor;

import java.util.Objects;

/**
 * Combines a contract with an id.
 * @param <C> the contract type
 */
public final class ContractId<C> {

    public final Class<C> contract;
    public final int id;
    public final MethodMapper methodMapper;

    private ContractId(final Class<C> contract, final int id, final MethodMapper.Factory methodMapperFactory) {
        this.contract = Objects.requireNonNull(contract);
        this.id = id;
        methodMapper = methodMapperFactory.create(contract);
    }

    public Service service(final C implementation, final Interceptor... interceptors) {
        return new Service(this, implementation, Interceptor.composite(interceptors));
    }

    /**
     * @see Server#completer()
     * @see #serviceAsync(Object)
     */
    public Service serviceAsync(final C implementation, final InterceptorAsync interceptor) {
        return new Service(this, implementation, interceptor);
    }

    /**
     * @see #serviceAsync(Object, InterceptorAsync)
     */
    public Service serviceAsync(final C implementation) {
        return serviceAsync(implementation, DirectInterceptorAsync.INSTANCE);
    }

    public static <C> ContractId<C> create(final Class<C> contract, final int id, final MethodMapper.Factory methodMapperFactory) {
        return new ContractId<>(contract, id, methodMapperFactory);
    }

}

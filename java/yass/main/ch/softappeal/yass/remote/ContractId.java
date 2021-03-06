package ch.softappeal.yass.remote;

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

    public static <C> ContractId<C> create(final Class<C> contract, final int id, final MethodMapper.Factory methodMapperFactory) {
        return new ContractId<>(contract, id, methodMapperFactory);
    }

}

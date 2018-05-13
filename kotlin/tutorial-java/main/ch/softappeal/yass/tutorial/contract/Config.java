package ch.softappeal.yass.tutorial.contract;

import ch.softappeal.yass.remote.ContractId;
import ch.softappeal.yass.remote.Services;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.fast.BaseTypeHandler;
import ch.softappeal.yass.serialize.fast.FastSerializer;
import ch.softappeal.yass.serialize.fast.TypeDesc;
import ch.softappeal.yass.tutorial.contract.generic.GenericEchoService;
import ch.softappeal.yass.tutorial.contract.generic.Pair;
import ch.softappeal.yass.tutorial.contract.generic.PairBoolBool;
import ch.softappeal.yass.tutorial.contract.generic.Triple;
import ch.softappeal.yass.tutorial.contract.generic.TripleWrapper;
import ch.softappeal.yass.tutorial.contract.instrument.Bond;
import ch.softappeal.yass.tutorial.contract.instrument.InstrumentService;
import ch.softappeal.yass.tutorial.contract.instrument.stock.Stock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static ch.softappeal.yass.remote.MethodMapperKt.getSimpleMethodMapperFactory;
import static ch.softappeal.yass.serialize.fast.BaseTypeHandlersKt.getBTH_BOOLEAN;
import static ch.softappeal.yass.serialize.fast.BaseTypeHandlersKt.getBTH_BYTE_ARRAY;
import static ch.softappeal.yass.serialize.fast.BaseTypeHandlersKt.getBTH_DOUBLE;
import static ch.softappeal.yass.serialize.fast.BaseTypeHandlersKt.getBTH_INTEGER;
import static ch.softappeal.yass.serialize.fast.BaseTypeHandlersKt.getBTH_STRING;
import static ch.softappeal.yass.serialize.fast.FastSerializerKt.FIRST_TYPE_ID;
import static ch.softappeal.yass.serialize.fast.FastSerializersKt.SimpleFastSerializer;
import static ch.softappeal.yass.transport.MessageSerializerKt.MessageSerializer;
import static ch.softappeal.yass.transport.PacketSerializerKt.PacketSerializer;

public final class Config {

    private static final TypeDesc BOOLEAN_DESC = new TypeDesc(FIRST_TYPE_ID, getBTH_BOOLEAN());
    private static final TypeDesc DOUBLE_DESC = new TypeDesc(FIRST_TYPE_ID + 1, getBTH_DOUBLE());
    private static final TypeDesc STRING_DESC = new TypeDesc(FIRST_TYPE_ID + 2, getBTH_STRING());
    private static final TypeDesc BYTES_DESC = new TypeDesc(FIRST_TYPE_ID + 3, getBTH_BYTE_ARRAY());

    private static List<BaseTypeHandler<?>> baseTypeHandlers(final BaseTypeHandler<?>... handlers) {
        final List<BaseTypeHandler<?>> h = new ArrayList<>();
        h.add((BaseTypeHandler<?>)BOOLEAN_DESC.getHandler());
        h.add((BaseTypeHandler<?>)DOUBLE_DESC.getHandler());
        h.add((BaseTypeHandler<?>)STRING_DESC.getHandler());
        h.add((BaseTypeHandler<?>)BYTES_DESC.getHandler());
        h.addAll(Arrays.asList(handlers));
        return h;
    }

    public static final FastSerializer CONTRACT_SERIALIZER = SimpleFastSerializer(
        baseTypeHandlers( // note: order is important; id's must match with TypeScript implementations
            getBTH_INTEGER(),         // TypeScriptGenerator.FIRST_DESC_ID
            Expiration.TYPE_HANDLER           // TypeScriptGenerator.FIRST_DESC_ID + 1
        ),
        Arrays.asList(
            PriceKind.class,
            Price.class,
            Stock.class,
            Bond.class,
            SystemException.class,
            UnknownInstrumentsException.class,
            Pair.class,
            PairBoolBool.class,
            Triple.class,
            TripleWrapper.class
        )
    );

    public static final Serializer MESSAGE_SERIALIZER = MessageSerializer(CONTRACT_SERIALIZER);
    public static final Serializer PACKET_SERIALIZER = PacketSerializer(MESSAGE_SERIALIZER);

    private abstract static class Role extends Services {
        Role() {
            super(getSimpleMethodMapperFactory());
        }
    }

    public static final class Initiator extends Role { // to be implemented by initiator
        public final ContractId<PriceListener> priceListener = contractId(PriceListener.class, 0);
        public final ContractId<EchoService> echoService = contractId(EchoService.class, 1);
    }

    public static final class Acceptor extends Role { // to be implemented by acceptor
        public final ContractId<PriceEngine> priceEngine = contractId(PriceEngine.class, 0);
        public final ContractId<InstrumentService> instrumentService = contractId(InstrumentService.class, 1);
        public final ContractId<EchoService> echoService = contractId(EchoService.class, 2);
        public final ContractId<GenericEchoService> genericEchoService = contractId(GenericEchoService.class, 3);
    }

    public static final Initiator INITIATOR = new Initiator();
    public static final Acceptor ACCEPTOR = new Acceptor();

}

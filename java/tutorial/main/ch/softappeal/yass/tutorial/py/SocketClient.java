package ch.softappeal.yass.tutorial.py;

import ch.softappeal.yass.remote.Client;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.transport.MessageSerializer;
import ch.softappeal.yass.transport.socket.SimpleSocketTransport;
import ch.softappeal.yass.transport.socket.SocketConnector;
import ch.softappeal.yass.tutorial.contract.Config;
import ch.softappeal.yass.tutorial.contract.Expiration;
import ch.softappeal.yass.tutorial.contract.Node;
import ch.softappeal.yass.tutorial.contract.PriceKind;
import ch.softappeal.yass.tutorial.contract.SystemException;
import ch.softappeal.yass.tutorial.contract.UnknownInstrumentsException;
import ch.softappeal.yass.tutorial.contract.instrument.stock.Stock;
import ch.softappeal.yass.tutorial.shared.Logger;
import ch.softappeal.yass.tutorial.shared.SslConfig;
import ch.softappeal.yass.tutorial.shared.socket.SocketSetup;

import java.util.Arrays;
import java.util.List;

import static ch.softappeal.yass.tutorial.contract.Config.PY_ACCEPTOR;

public final class SocketClient {

    static Object createObjects() {
        final var node1 = new Node(1.0);
        final var node2 = new Node(2.0);
        node1.links.add(node1);
        node1.links.add(node2);
        return Arrays.asList(
            null,
            false,
            true,
            123456,
            -987654,
            1.34545e98d,
            "Hello",
            ">\u0001\u0012\u007F\u0080\u0234\u07FF\u0800\u4321\uFFFF<",
            new byte[] {0, 127, -1, 10, -45},
            new Expiration(2017, 11, 29),
            PriceKind.ASK,
            PriceKind.BID,
            new Stock(123, "YASS", true),
            new UnknownInstrumentsException(List.of(1, 2, 3)),
            node1
        );
    }

    static final Serializer SERIALIZER = Config.PY_CONTRACT_SERIALIZER;

    static void client(final Client client) {
        final var logger = new Logger(null, Logger.Side.CLIENT);
        final var echoService = client.proxy(PY_ACCEPTOR.echoService);
        final var instrumentService = client.proxy(PY_ACCEPTOR.instrumentService, logger);
        System.out.println(echoService.echo("hello"));
        System.out.println(echoService.echo(createObjects()));
        try {
            echoService.echo("exception");
        } catch (final SystemException e) {
            System.out.println(e.message);
        }
        final var big = new byte[1_000_000];
        if (((byte[])echoService.echo(big)).length != big.length) {
            throw new RuntimeException();
        }
        instrumentService.showOneWay(true, 123);
        System.out.println(instrumentService.getInstruments());
    }

    public static void main(final String... args) {
        client(SimpleSocketTransport.client(
            new MessageSerializer(SERIALIZER),
            SocketConnector.create(SslConfig.CLIENT.socketFactory, SocketSetup.ADDRESS)
        ));
    }

}

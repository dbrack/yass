package ch.softappeal.yass.tutorial.acceptor.socket;

import ch.softappeal.yass.transport.TransportSetup;
import ch.softappeal.yass.transport.socket.AsyncSocketConnection;
import ch.softappeal.yass.transport.socket.SocketTransport;
import ch.softappeal.yass.tutorial.acceptor.AcceptorSession;
import ch.softappeal.yass.tutorial.contract.Config;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.NamedThreadFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public final class SocketAcceptor {

    public static final SocketAddress ADDRESS = new InetSocketAddress("localhost", 28947);

    public static void main(final String... args) {
        final Executor executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.STD_ERR));
        new SocketTransport(
            executor,
            AsyncSocketConnection.factory(executor, 1_000),
            TransportSetup.ofContractSerializer(
                Config.CONTRACT_SERIALIZER,
                connection -> new AcceptorSession(connection, executor)
            )
        ).start(executor, ADDRESS);
        System.out.println("started");
    }

}

package ch.softappeal.yass.tutorial.initiator.socket;

import ch.softappeal.yass.core.remote.session.Reconnector;
import ch.softappeal.yass.transport.TransportSetup;
import ch.softappeal.yass.transport.socket.SocketTransport;
import ch.softappeal.yass.transport.socket.SyncSocketConnection;
import ch.softappeal.yass.tutorial.acceptor.socket.SocketAcceptor;
import ch.softappeal.yass.tutorial.contract.Config;
import ch.softappeal.yass.tutorial.initiator.InitiatorSession;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.NamedThreadFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public final class SocketInitiator {

    public static void main(final String... args) {
        final Executor executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.STD_ERR));
        final SocketTransport transport = new SocketTransport(executor, SyncSocketConnection.FACTORY);
        final Reconnector reconnector = new Reconnector(
            executor,
            10,
            connection -> new InitiatorSession(connection, executor),
            sessionFactory -> transport.connect(
                TransportSetup.ofContractSerializer(Config.SERIALIZER, sessionFactory),
                SocketAcceptor.ADDRESS
            )
        );
        System.out.println("connected: " + reconnector.connected());
        System.out.println("started");
    }

}

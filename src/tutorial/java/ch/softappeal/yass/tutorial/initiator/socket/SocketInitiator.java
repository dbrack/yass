package ch.softappeal.yass.tutorial.initiator.socket;

import ch.softappeal.yass.core.remote.session.Connection;
import ch.softappeal.yass.core.remote.session.Session;
import ch.softappeal.yass.core.remote.session.SessionFactory;
import ch.softappeal.yass.transport.TransportSetup;
import ch.softappeal.yass.transport.socket.SimpleSocketConnector;
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
        SocketTransport.connect(
            executor,
            SyncSocketConnection.FACTORY,
            TransportSetup.ofContractSerializer(Config.CONTRACT_SERIALIZER, new SessionFactory() {
                @Override public Session create(final Connection connection) throws Exception {
                    return new InitiatorSession(connection, executor);
                }
            }),
            new SimpleSocketConnector(SocketAcceptor.ADDRESS)
        );
    }

}

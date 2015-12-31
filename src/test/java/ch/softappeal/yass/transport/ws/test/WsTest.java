package ch.softappeal.yass.transport.ws.test;

import ch.softappeal.yass.transport.TransportSetup;
import ch.softappeal.yass.transport.test.TransportTest;
import ch.softappeal.yass.transport.ws.AsyncWsConnection;
import ch.softappeal.yass.transport.ws.SyncWsConnection;
import ch.softappeal.yass.transport.ws.WsConnection;
import ch.softappeal.yass.transport.ws.WsEndpoint;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.NamedThreadFactory;
import org.junit.After;
import org.junit.Before;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import javax.websocket.server.ServerEndpointConfig;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public abstract class WsTest extends TransportTest {

    protected static final int PORT = 9090;
    protected static final String PATH = "/test";
    protected static final URI THE_URI = URI.create("ws://localhost:" + PORT + PATH);

    private static volatile ExecutorService INITIATOR_EXECUTOR;
    private static volatile ExecutorService ACCEPTOR_EXECUTOR;

    @Before public void startDispatcherExecutor() {
        INITIATOR_EXECUTOR = Executors.newCachedThreadPool(new NamedThreadFactory("initiatorExecutor", Exceptions.TERMINATE));
        ACCEPTOR_EXECUTOR = Executors.newCachedThreadPool(new NamedThreadFactory("acceptorExecutor", Exceptions.TERMINATE));
    }

    @After public void stopDispatcherExecutor() {
        INITIATOR_EXECUTOR.shutdown();
        ACCEPTOR_EXECUTOR.shutdown();
    }

    private static volatile TransportSetup TRANSPORT_SETUP_INITIATOR;
    private static volatile TransportSetup TRANSPORT_SETUP_ACCEPTOR;

    public static final class ClientEndpoint extends WsEndpoint {
        @Override protected WsConnection createConnection(final Session session) throws Exception {
            return WsConnection.create(SyncWsConnection.FACTORY, TRANSPORT_SETUP_INITIATOR, session);
        }
    }

    public static final class ServerEndpoint extends WsEndpoint {
        @Override protected WsConnection createConnection(final Session session) throws Exception {
            return WsConnection.create(AsyncWsConnection.factory(100), TRANSPORT_SETUP_ACCEPTOR, session);
        }
    }

    protected static ServerEndpointConfig serverEndpointConfig(final ServerEndpointConfig.Configurator configurator) {
        return ServerEndpointConfig.Builder.create(ServerEndpoint.class, PATH).configurator(configurator).build();
    }

    protected static void setTransportSetup(final boolean serverInvoke, final boolean serverCreateException, final boolean clientInvoke, final boolean clientCreateException) {
        TRANSPORT_SETUP_ACCEPTOR = transportSetup(serverInvoke, serverCreateException, ACCEPTOR_EXECUTOR);
        TRANSPORT_SETUP_INITIATOR = transportSetup(clientInvoke, clientCreateException, INITIATOR_EXECUTOR);
    }

    protected static void setPerformanceSetup(final CountDownLatch latch) {
        TRANSPORT_SETUP_ACCEPTOR = transportSetup(ACCEPTOR_EXECUTOR);
        TRANSPORT_SETUP_INITIATOR = transportSetup(INITIATOR_EXECUTOR, latch, 100);
    }

    protected static void connect(final WebSocketContainer container, final CountDownLatch latch) throws Exception {
        container.connectToServer(new ClientEndpoint(), ClientEndpointConfig.Builder.create().build(), THE_URI);
        latch.await();
        TimeUnit.MILLISECONDS.sleep(400L);
    }

}

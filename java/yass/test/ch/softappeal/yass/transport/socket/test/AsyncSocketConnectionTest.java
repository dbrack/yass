package ch.softappeal.yass.transport.socket.test;

import ch.softappeal.yass.core.remote.ContractId;
import ch.softappeal.yass.core.remote.OneWay;
import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.core.remote.SimpleMethodMapper;
import ch.softappeal.yass.core.remote.session.SimpleSession;
import ch.softappeal.yass.serialize.JavaSerializer;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.transport.TransportSetup;
import ch.softappeal.yass.transport.socket.AsyncSocketConnection;
import ch.softappeal.yass.transport.socket.SocketBinder;
import ch.softappeal.yass.transport.socket.SocketConnector;
import ch.softappeal.yass.transport.socket.SocketTransport;
import ch.softappeal.yass.transport.socket.SyncSocketConnection;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.NamedThreadFactory;
import ch.softappeal.yass.util.Nullable;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class AsyncSocketConnectionTest {

    public interface Busy {
        @OneWay void busy();
    }

    private static final ContractId<Busy> BUSY_ID = ContractId.create(Busy.class, 0, SimpleMethodMapper.FACTORY);
    private static final SocketAddress ADDRESS = new InetSocketAddress("localhost", 28947);
    private static final Serializer PACKET_SERIALIZER = JavaSerializer.INSTANCE;

    public static void main(final String... args) throws InterruptedException {
        final var executor = Executors.newCachedThreadPool(new NamedThreadFactory("Executor", Exceptions.STD_ERR));
        new SocketTransport(
            executor,
            SyncSocketConnection.FACTORY,
            TransportSetup.ofPacketSerializer(
                PACKET_SERIALIZER,
                connection -> new SimpleSession(connection, executor) {
                    @Override protected Server server() {
                        return new Server(
                            BUSY_ID.service(() -> {
                                System.out.println("busy");
                                try {
                                    TimeUnit.MILLISECONDS.sleep(1_000);
                                } catch (InterruptedException e) {
                                    throw new RuntimeException(e);
                                }
                            })
                        );
                    }
                    @Override protected void opened() {
                        System.out.println("acceptor opened");
                    }
                    @Override protected void closed(final @Nullable Exception exception) {
                        System.out.println("acceptor closed: " + exception);
                    }
                }
            )
        ).start(executor, SocketBinder.create(ADDRESS));
        SocketTransport.connect(
            executor,
            AsyncSocketConnection.factory(executor, 10),
            TransportSetup.ofPacketSerializer(
                PACKET_SERIALIZER,
                connection -> new SimpleSession(connection, executor) {
                    @Override protected void opened() {
                        System.out.println("initiator opened");
                        final var busy = proxy(BUSY_ID);
                        for (var i = 0; i < 10_000; i++) {
                            busy.busy();
                        }
                        System.out.println("initiator done");
                    }
                    @Override protected void closed(final @Nullable Exception exception) {
                        System.out.println("initiator closed: " + exception);
                    }
                }
            ),
            SocketConnector.create(ADDRESS)
        );
    }

}

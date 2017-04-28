package ch.softappeal.yass.transport.socket.test;

import ch.softappeal.yass.core.Interceptor;
import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.core.remote.test.ContractIdTest;
import ch.softappeal.yass.transport.PathSerializer;
import ch.softappeal.yass.transport.SimplePathResolver;
import ch.softappeal.yass.transport.SimpleTransportSetup;
import ch.softappeal.yass.transport.socket.SimpleSocketTransport;
import ch.softappeal.yass.transport.test.TransportTest;
import ch.softappeal.yass.util.Closer;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.NamedThreadFactory;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("try")
public class SimpleSocketTransportTest extends TransportTest {

    public static void delayedShutdown(final ExecutorService executor) throws InterruptedException {
        TimeUnit.MILLISECONDS.sleep(100L); // gives client time to finish socket close
        executor.shutdown();
    }

    private static Interceptor socketInterceptor(final String side) {
        return (method, arguments, invocation) -> {
            System.out.println(side + ": " + SimpleSocketTransport.socket().get());
            return invocation.proceed();
        };
    }

    @Test public void invoke() throws Exception {
        Assert.assertFalse(SimpleSocketTransport.socket().isPresent());
        final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.TERMINATE));
        try (
            Closer closer = new SimpleSocketTransport(
                executor,
                MESSAGE_SERIALIZER,
                new Server(
                    ContractIdTest.ID.service(
                        new TestServiceImpl(),
                        socketInterceptor("server"),
                        SERVER_INTERCEPTOR
                    )
                )
            ).start(executor, SocketTransportTest.BINDER)
        ) {
            invoke(
                SimpleSocketTransport.client(MESSAGE_SERIALIZER, SocketTransportTest.CONNECTOR)
                    .proxy(
                        ContractIdTest.ID,
                        socketInterceptor("client"),
                        CLIENT_INTERCEPTOR
                    )
            );
        } finally {
            delayedShutdown(executor);
        }
    }

    @Test public void wrongPath() throws Exception {
        final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.STD_ERR));
        try (
            Closer closer = new SimpleSocketTransport(
                executor,
                MESSAGE_SERIALIZER,
                new Server(ECHO_ID.service(new EchoServiceImpl()))
            ).start(executor, SocketTransportTest.BINDER)
        ) {
            try {
                SimpleSocketTransport.client(MESSAGE_SERIALIZER, SocketTransportTest.CONNECTOR, PathSerializer.INSTANCE, 123)
                    .proxy(ECHO_ID).echo(null);
                Assert.fail();
            } catch (final RuntimeException ignore) {
                // empty
            }
        } finally {
            delayedShutdown(executor);
        }
    }

    @Test public void multiplePathes() throws Exception {
        final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.TERMINATE));
        final Integer path1 = 1;
        final Integer path2 = 2;
        final Map<Integer, SimpleTransportSetup> pathMappings = new HashMap<>(2);
        pathMappings.put(path1, new SimpleTransportSetup(MESSAGE_SERIALIZER, new Server(ECHO_ID.service(new EchoServiceImpl(), (method, arguments, invocation) -> {
            System.out.println("path 1");
            return invocation.proceed();
        }))));
        pathMappings.put(path2, new SimpleTransportSetup(MESSAGE_SERIALIZER, new Server(ECHO_ID.service(new EchoServiceImpl(), (method, arguments, invocation) -> {
            System.out.println("path 2");
            return invocation.proceed();
        }))));
        try (
            Closer closer = new SimpleSocketTransport(
                executor,
                PathSerializer.INSTANCE,
                new SimplePathResolver(pathMappings)
            ).start(executor, SocketTransportTest.BINDER)
        ) {
            SimpleSocketTransport.client(MESSAGE_SERIALIZER, SocketTransportTest.CONNECTOR, PathSerializer.INSTANCE, 1)
                .proxy(ECHO_ID).echo(null);
            SimpleSocketTransport.client(MESSAGE_SERIALIZER, SocketTransportTest.CONNECTOR, PathSerializer.INSTANCE, 2)
                .proxy(ECHO_ID).echo(null);
        } finally {
            delayedShutdown(executor);
        }
    }

}

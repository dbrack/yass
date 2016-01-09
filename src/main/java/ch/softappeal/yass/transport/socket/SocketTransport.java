package ch.softappeal.yass.transport.socket;

import ch.softappeal.yass.serialize.Reader;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.Writer;
import ch.softappeal.yass.transport.PathResolver;
import ch.softappeal.yass.transport.PathSerializer;
import ch.softappeal.yass.transport.TransportSetup;
import ch.softappeal.yass.util.Check;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketAddress;
import java.util.concurrent.Executor;

public final class SocketTransport extends AbstractSocketTransport {

    private final SocketConnection.Factory connectionFactory;
    private final Serializer pathSerializer;

    /**
     * @param readerExecutor used once for each session
     */
    public SocketTransport(final Executor readerExecutor, final SocketConnection.Factory connectionFactory, final Serializer pathSerializer) {
        super(readerExecutor);
        this.connectionFactory = Check.notNull(connectionFactory);
        this.pathSerializer = Check.notNull(pathSerializer);
    }

    /**
     * Uses {@link PathSerializer}.
     */
    public SocketTransport(final Executor readerExecutor, final SocketConnection.Factory connectionFactory) {
        this(readerExecutor, connectionFactory, PathSerializer.INSTANCE);
    }

    public void connect(final TransportSetup setup, final Object path, final SocketFactory socketFactory, final SocketAddress socketAddress) {
        Check.notNull(setup);
        Check.notNull(path);
        try {
            runInReaderExecutor(connect(socketFactory, socketAddress), socket -> {
                final OutputStream out = socket.getOutputStream();
                pathSerializer.write(path, Writer.create(out));
                out.flush();
                SocketConnection.create(connectionFactory, setup, socket, Reader.create(socket.getInputStream()), out);
            });
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Uses {@link PathSerializer#DEFAULT}.
     */
    public void connect(final TransportSetup setup, final SocketFactory socketFactory, final SocketAddress socketAddress) {
        connect(setup, PathSerializer.DEFAULT, socketFactory, socketAddress);
    }

    /**
     * Uses {@link SocketFactory#getDefault()} and {@link PathSerializer#DEFAULT}.
     */
    public void connect(final TransportSetup setup, final SocketAddress socketAddress) {
        connect(setup, SocketFactory.getDefault(), socketAddress);
    }

    /**
     * @param listenerExecutor used once
     */
    public ListenerCloser start(final PathResolver pathResolver, final Executor listenerExecutor, final ServerSocketFactory socketFactory, final SocketAddress socketAddress) {
        Check.notNull(pathResolver);
        return start(listenerExecutor, socketFactory, socketAddress, socket -> {
            final Reader reader = Reader.create(socket.getInputStream());
            final TransportSetup setup = pathResolver.resolvePath(pathSerializer.read(reader));
            SocketConnection.create(connectionFactory, setup, socket, reader, socket.getOutputStream());
        });
    }

    /**
     * Uses {@link PathSerializer#DEFAULT}.
     */
    public ListenerCloser start(final TransportSetup setup, final Executor listenerExecutor, final ServerSocketFactory socketFactory, final SocketAddress socketAddress) {
        return start(new PathResolver(PathSerializer.DEFAULT, setup), listenerExecutor, socketFactory, socketAddress);
    }

    /**
     * Uses {@link ServerSocketFactory#getDefault()} and {@link PathSerializer#DEFAULT}.
     */
    public ListenerCloser start(final TransportSetup setup, final Executor listenerExecutor, final SocketAddress socketAddress) {
        return start(setup, listenerExecutor, ServerSocketFactory.getDefault(), socketAddress);
    }

}

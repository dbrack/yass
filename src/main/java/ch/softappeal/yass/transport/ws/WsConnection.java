package ch.softappeal.yass.transport.ws;

import ch.softappeal.yass.core.remote.session.Connection;
import ch.softappeal.yass.core.remote.session.Packet;
import ch.softappeal.yass.core.remote.session.Session;
import ch.softappeal.yass.core.remote.session.SessionFactory;
import ch.softappeal.yass.serialize.Reader;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.Writer;
import ch.softappeal.yass.serialize.Writer.ByteBufferOutputStream;
import ch.softappeal.yass.transport.TransportSetup;
import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.Nullable;

import javax.websocket.CloseReason;
import javax.websocket.MessageHandler;
import java.io.IOException;
import java.nio.ByteBuffer;

public abstract class WsConnection implements Connection {

    public interface Factory {
        WsConnection create(Serializer packetSerializer, javax.websocket.Session session) throws Exception;
    }

    private final Serializer packetSerializer;
    public final javax.websocket.Session session;
    private Session yassSession;

    protected WsConnection(final Serializer packetSerializer, final javax.websocket.Session session) {
        this.packetSerializer = Check.notNull(packetSerializer);
        this.session = Check.notNull(session);
    }

    public static WsConnection create(final Factory connectionFactory, final TransportSetup setup, final javax.websocket.Session session) throws Exception {
        try {
            final WsConnection connection = connectionFactory.create(setup.packetSerializer, session);
            connection.created(setup.sessionFactory);
            return connection;
        } catch (final Exception e) {
            try {
                session.close();
            } catch (final Exception e2) {
                e.addSuppressed(e2);
            }
            throw e;
        }
    }

    private void created(final SessionFactory sessionFactory) throws Exception {
        session.addMessageHandler(new MessageHandler.Whole<ByteBuffer>() { // $note: this could be replaced with a lambda in WebSocket API 1.1
            @Override public void onMessage(final ByteBuffer in) {
                try {
                    Session.received(yassSession, (Packet)packetSerializer.read(Reader.create(in)));
                    if (in.hasRemaining()) {
                        throw new RuntimeException("input buffer is not empty");
                    }
                } catch (final Exception e) {
                    Session.close(yassSession, e);
                    throw Exceptions.wrap(e);
                }
            }
        });
        yassSession = Session.create(sessionFactory, this);
    }

    protected final ByteBuffer writeToBuffer(final Packet packet) throws Exception {
        final ByteBufferOutputStream buffer = new ByteBufferOutputStream(128);
        packetSerializer.write(packet, Writer.create(buffer));
        return buffer.toByteBuffer();
    }

    final void onClose(final CloseReason closeReason) {
        if (closeReason.getCloseCode().getCode() != CloseReason.CloseCodes.NORMAL_CLOSURE.getCode()) {
            onError(new RuntimeException("WebSocket closed - " + closeReason.toString()));
        }
    }

    protected final void onError(@Nullable Throwable throwable) {
        final Exception exception;
        if (throwable == null) {
            exception = new Exception("<no-throwable>");
        } else if (throwable instanceof Exception) {
            exception = (Exception)throwable;
        } else if (throwable instanceof Error) {
            throw (Error)throwable;
        } else {
            throw new Error(throwable);
        }
        Session.close(yassSession, exception);
        throw Exceptions.wrap(exception);
    }

    @Override public final void closed() throws IOException {
        session.close();
    }

}

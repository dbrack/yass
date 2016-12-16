package ch.softappeal.yass.core.remote.session;

import ch.softappeal.yass.core.remote.Client;
import ch.softappeal.yass.core.remote.ExceptionReply;
import ch.softappeal.yass.core.remote.Message;
import ch.softappeal.yass.core.remote.Reply;
import ch.softappeal.yass.core.remote.Request;
import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Closer;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class Session extends Client implements Closer {

    public final Connection connection;

    protected Session(final Connection connection) {
        this.connection = Check.notNull(connection);
    }

    /**
     * Called if a session has been opened.
     * Must call {@link Runnable#run()} (possibly in an own thread).
     */
    protected abstract void dispatchOpened(Runnable runnable) throws Exception;

    /**
     * Called for an incoming request.
     * Must call {@link Runnable#run()} (possibly in an own thread).
     */
    protected abstract void dispatchServerInvoke(Server.Invocation invocation, Runnable runnable) throws Exception;

    private Server server;
    /**
     * Gets the server of this session. Called only once after creation of session.
     * This implementation returns {@link Server#EMPTY}.
     */
    protected Server server() throws Exception {
        return Server.EMPTY;
    }

    /**
     * Called when the session has been opened.
     * This implementation does nothing.
     * Due to race conditions or exceptions it could not be called or be called after {@link #closed(Exception)}.
     * @see SessionFactory#create(Connection)
     */
    protected void opened() throws Exception {
        // empty
    }

    /**
     * Called once when the session has been closed.
     * This implementation does nothing.
     * @param exception if (exception == null) regular close else reason for close
     * @see SessionFactory#create(Connection)
     */
    protected void closed(final @Nullable Exception exception) throws Exception {
        // empty
    }

    private final AtomicBoolean closed = new AtomicBoolean(true);
    public final boolean isClosed() {
        return closed.get();
    }

    private void unblockPromises() {
        final List<Invocation> invocations = new ArrayList<>(requestNumber2invocation.values());
        for (final Invocation invocation : invocations) {
            try {
                invocation.settle(new ExceptionReply(new SessionClosedException()));
            } catch (final Exception ignore) {
                // empty
            }
        }
    }

    private void close(final boolean sendEnd, final @Nullable Exception exception) {
        if (closed.getAndSet(true)) {
            return;
        }
        try {
            try {
                try {
                    unblockPromises();
                } finally {
                    closed(exception);
                }
                if (sendEnd) {
                    connection.write(Packet.END);
                }
            } finally {
                connection.closed();
            }
        } catch (final Exception e) {
            throw Exceptions.wrap(e);
        }
    }

    /**
     * Must be called if communication has failed.
     * This method is idempotent.
     */
    public static void close(final Session session, final Exception e) {
        session.close(false, Check.notNull(e));
    }

    @Override public void close() {
        close(true, null);
    }

    private void serverInvoke(final int requestNumber, final Request request) throws Exception {
        final Server.Invocation invocation = server.invocation(true, request);
        dispatchServerInvoke(invocation, () -> {
            try {
                invocation.invoke(reply -> {
                    if (!invocation.methodMapping.oneWay) {
                        try {
                            connection.write(new Packet(requestNumber, reply));
                        } catch (final Exception e) {
                            close(this, e);
                        }
                    }
                });
            } catch (final Exception e) {
                close(this, e);
            }
        });
    }

    /**
     * note: it's not worth to use {@link ConcurrentHashMap} here
     */
    private final Map<Integer, Invocation> requestNumber2invocation = Collections.synchronizedMap(new HashMap<>(16));

    private void received(final Packet packet) throws Exception {
        try {
            if (packet.isEnd()) {
                close(false, null);
                return;
            }
            final Message message = packet.message();
            if (message instanceof Request) {
                serverInvoke(packet.requestNumber(), (Request)message);
            } else {
                requestNumber2invocation.remove(packet.requestNumber()).settle((Reply)message); // client invoke
            }
        } catch (final Exception e) {
            close(this, e);
            throw e;
        }
    }

    /**
     * Must be called if a packet has been received.
     * It must also be called if {@link Packet#isEnd()}; however, it must not be called again after that.
     */
    public static void received(final Session session, final Packet packet) throws Exception {
        session.received(packet);
    }

    private final AtomicInteger nextRequestNumber = new AtomicInteger(Packet.END_REQUEST_NUMBER);

    @Override protected final void invoke(final Client.Invocation invocation) throws Exception {
        if (isClosed()) {
            throw new SessionClosedException();
        }
        invocation.invoke(true, request -> {
            try {
                int requestNumber;
                do { // we can't use END_REQUEST_NUMBER as regular requestNumber
                    requestNumber = nextRequestNumber.incrementAndGet();
                } while (requestNumber == Packet.END_REQUEST_NUMBER);
                if (!invocation.methodMapping.oneWay) {
                    requestNumber2invocation.put(requestNumber, invocation);
                    if (isClosed()) {
                        unblockPromises(); // needed due to race conditions
                    }
                }
                connection.write(new Packet(requestNumber, request));
            } catch (final Exception e) {
                close(this, e);
                throw e;
            }
        });
    }

    private void created() {
        closed.set(false);
        try {
            server = Check.notNull(server());
            dispatchOpened(() -> {
                try {
                    opened();
                } catch (final Exception e) {
                    close(this, e);
                }
            });
        } catch (final Exception e) {
            close(this, e);
        }
    }

    public static Session create(final SessionFactory sessionFactory, final Connection connection) throws Exception {
        final Session session = sessionFactory.create(connection);
        session.created();
        return session;
    }

}

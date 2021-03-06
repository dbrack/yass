package ch.softappeal.yass.transport.ws;

import ch.softappeal.yass.Exceptions;
import ch.softappeal.yass.Nullable;
import ch.softappeal.yass.transport.TransportSetup;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Extension;
import javax.websocket.HandshakeResponse;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class WsConfigurator extends ServerEndpointConfig.Configurator {

    final WsConnection.Factory connectionFactory;
    final TransportSetup setup;
    final Thread.UncaughtExceptionHandler uncaughtExceptionHandler;

    public WsConfigurator(final WsConnection.Factory connectionFactory, final TransportSetup setup, final Thread.UncaughtExceptionHandler uncaughtExceptionHandler) {
        this.connectionFactory = Objects.requireNonNull(connectionFactory);
        this.setup = Objects.requireNonNull(setup);
        this.uncaughtExceptionHandler = Objects.requireNonNull(uncaughtExceptionHandler);
    }

    @Override public final <T> T getEndpointInstance(final Class<T> endpointClass) {
        return endpointClass.cast(new Endpoint() {
            volatile @Nullable WsConnection connection = null;
            @Override public void onOpen(final Session session, final EndpointConfig config) {
                try {
                    connection = WsConnection.create(WsConfigurator.this, session);
                } catch (final Throwable t) {
                    Exceptions.uncaughtException(uncaughtExceptionHandler, t);
                }
            }
            @Override public void onClose(final Session session, final CloseReason closeReason) {
                if (connection != null) {
                    connection.onClose(closeReason);
                }
            }
            @Override public void onError(final Session session, final Throwable throwable) {
                if (connection != null) {
                    connection.onError(throwable);
                }
            }
        });
    }

    public final Endpoint getEndpointInstance() {
        return getEndpointInstance(Endpoint.class);
    }

    @Override public String getNegotiatedSubprotocol(final List<String> supported, final List<String> requested) {
        return requested.stream().filter(supported::contains).findFirst().orElse("");
    }

    @Override public List<Extension> getNegotiatedExtensions(final List<Extension> installed, final List<Extension> requested) {
        return requested.stream().filter(r -> installed.stream().anyMatch(i -> i.getName().equals(r.getName()))).collect(Collectors.toList());
    }

    @Override public boolean checkOrigin(final String originHeaderValue) {
        return true;
    }

    @Override public void modifyHandshake(final ServerEndpointConfig sec, final HandshakeRequest request, final HandshakeResponse response) {
        // empty
    }

}

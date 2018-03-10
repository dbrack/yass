package ch.softappeal.yass.tutorial.acceptor.web;

import ch.softappeal.yass.tutorial.shared.SslConfig;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;

public final class JettyAcceptor extends WebAcceptorSetup {

    public static void main(final String... args) throws Exception {
        final var server = new Server();
        final var serverConnector = new ServerConnector(server);
        serverConnector.setHost(HOST);
        serverConnector.setPort(PORT);
        server.addConnector(serverConnector);
        final var https = new HttpConfiguration();
        https.addCustomizer(new SecureRequestCustomizer());
        final var sslContextFactory = new SslContextFactory();
        sslContextFactory.setSslContext(SslConfig.SERVER.context);
        sslContextFactory.setNeedClientAuth(true);
        final var sslServerConnector = new ServerConnector(
            server,
            new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.toString()),
            new HttpConnectionFactory(https)
        );
        sslServerConnector.setHost(HOST);
        sslServerConnector.setPort(PORT + 1);
        server.addConnector(sslServerConnector);
        final var contextHandler = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        contextHandler.setContextPath("/");
        contextHandler.addServlet(new ServletHolder(new XhrServlet()), XHR_PATH);
        final var resourceHandler = new ResourceHandler();
        resourceHandler.setDirectoriesListed(true);
        resourceHandler.setResourceBase(WEB_PATH);
        final var handlers = new HandlerList();
        handlers.setHandlers(new Handler[] {resourceHandler, contextHandler});
        server.setHandler(handlers);
        WebSocketServerContainerInitializer.configureContext(contextHandler).addEndpoint(ENDPOINT_CONFIG);
        server.start();
        System.out.println("started");
    }

}

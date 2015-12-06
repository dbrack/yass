package ch.softappeal.yass.tutorial.acceptor.web;

import ch.softappeal.yass.core.remote.Request;
import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.core.remote.Service;
import ch.softappeal.yass.serialize.Reader;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.Writer;
import ch.softappeal.yass.transport.MessageSerializer;
import ch.softappeal.yass.tutorial.contract.AcceptorServices;
import ch.softappeal.yass.tutorial.contract.Config;
import ch.softappeal.yass.tutorial.contract.EchoServiceImpl;
import ch.softappeal.yass.tutorial.contract.Logger;
import ch.softappeal.yass.tutorial.contract.UnexpectedExceptionHandler;
import ch.softappeal.yass.util.Exceptions;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class XhrServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static void invoke(final Server server, final Serializer messageSerializer, final HttpServletRequest httpRequest, final HttpServletResponse httpResponse) throws Exception {
        final Request request = (Request)messageSerializer.read(Reader.create(httpRequest.getInputStream()));
        final Server.Invocation invocation = server.invocation(request);
        if (invocation.oneWay) {
            throw new IllegalArgumentException("xhr not allowed for oneWay method (serviceId " + request.serviceId + ", methodId " + request.methodId + ')');
        }
        messageSerializer.write(invocation.invoke(), Writer.create(httpResponse.getOutputStream()));
    }

    private static final Server SERVER = new Server(
        Config.METHOD_MAPPER_FACTORY,
        new Service(AcceptorServices.EchoService, new EchoServiceImpl(), UnexpectedExceptionHandler.INSTANCE, new Logger(null, Logger.Side.SERVER))
    );

    private static final Serializer MESSAGE_SERIALIZER = new MessageSerializer(Config.SERIALIZER);

    @Override protected void doPost(final HttpServletRequest request, final HttpServletResponse response) {
        try {
            invoke(SERVER, MESSAGE_SERIALIZER, request, response);
        } catch (final Exception e) {
            throw Exceptions.wrap(e);
        }
    }

}

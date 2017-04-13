package test;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.XnioByteBufferPool;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.websockets.jsr.WebSocketDeploymentInfo;
import org.xnio.ByteBufferSlicePool;
import org.xnio.OptionMap;
import org.xnio.Xnio;

public final class UndertowAcceptor {

    public static void main(final String... args) throws Exception {
        final DeploymentManager deployment = Servlets.defaultContainer()
            .addDeployment(
                Servlets.deployment()
                    .setClassLoader(UndertowAcceptor.class.getClassLoader())
                    .setContextPath("/")
                    .setDeploymentName(UndertowAcceptor.class.getName())
                    .addServletContextAttribute(
                        WebSocketDeploymentInfo.ATTRIBUTE_NAME,
                        new WebSocketDeploymentInfo()
                            .addEndpoint(ApplicationConfig.ENDPOINT_CONFIG)
                            .setWorker(Xnio.getInstance().createWorker(OptionMap.builder().getMap()))
                            .setBuffers(new XnioByteBufferPool(new ByteBufferSlicePool(1024, 10240)))
                    )
            );
        deployment.deploy();
        final HttpHandler servletHandler = deployment.start();
        Undertow.builder()
            .addHttpListener(Initiator.PORT, Initiator.HOST)
            .setHandler(servletHandler)
            .build()
            .start();
    }

}

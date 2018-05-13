package ch.softappeal.yass.transport.ws

import java.util.stream.Collectors
import javax.websocket.CloseReason
import javax.websocket.Endpoint
import javax.websocket.EndpointConfig
import javax.websocket.Extension
import javax.websocket.HandshakeResponse
import javax.websocket.Session
import javax.websocket.server.HandshakeRequest
import javax.websocket.server.ServerEndpointConfig

class WsConfigurator(internal val connectionFactory: WsConnection.Factory, internal val setup: TransportSetup, internal val uncaughtExceptionHandler: Thread.UncaughtExceptionHandler) : ServerEndpointConfig.Configurator() {

    val endpointInstance: Endpoint
        get() = getEndpointInstance(Endpoint::class.java)

    override fun <T> getEndpointInstance(endpointClass: Class<T>): T {
        return endpointClass.cast(object : Endpoint() {
            @Volatile
            internal var connection: WsConnection? = null

            override fun onOpen(session: Session, config: EndpointConfig) {
                try {
                    connection = WsConnection.create(this@WsConfigurator, session)
                } catch (t: Throwable) {
                    Exceptions.uncaughtException(uncaughtExceptionHandler, t)
                }

            }

            override fun onClose(session: Session?, closeReason: CloseReason?) {
                if (connection != null) {
                    connection!!.onClose(closeReason!!)
                }
            }

            override fun onError(session: Session?, throwable: Throwable?) {
                if (connection != null) {
                    connection!!.onError(throwable)
                }
            }
        })
    }

    override fun getNegotiatedSubprotocol(supported: List<String>, requested: List<String>): String {
        return requested.stream().filter(Predicate<String> { supported.contains(it) }).findFirst().orElse("")
    }

    override fun getNegotiatedExtensions(installed: List<Extension>, requested: List<Extension>): List<Extension> {
        return requested.stream().filter { r -> installed.stream().anyMatch { i -> i.name == r.name } }.collect<List<Extension>, Any>(Collectors.toList())
    }

    override fun checkOrigin(originHeaderValue: String): Boolean {
        return true
    }

    override fun modifyHandshake(sec: ServerEndpointConfig?, request: HandshakeRequest?, response: HandshakeResponse?) {
        // empty
    }

}

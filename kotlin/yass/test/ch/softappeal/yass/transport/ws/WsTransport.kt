package ch.softappeal.yass.transport.ws

import ch.softappeal.yass.remote.session.Connection
import ch.softappeal.yass.remote.session.Packet
import ch.softappeal.yass.remote.session.Session
import ch.softappeal.yass.remote.session.close
import ch.softappeal.yass.remote.session.received
import ch.softappeal.yass.serialize.ByteBufferOutputStream
import ch.softappeal.yass.serialize.Serializer
import ch.softappeal.yass.serialize.reader
import ch.softappeal.yass.serialize.writer
import java.nio.ByteBuffer
import java.util.stream.Collectors
import javax.websocket.CloseReason
import javax.websocket.Endpoint
import javax.websocket.EndpointConfig
import javax.websocket.Extension
import javax.websocket.HandshakeResponse
import javax.websocket.MessageHandler
import javax.websocket.RemoteEndpoint
import javax.websocket.server.HandshakeRequest
import javax.websocket.server.ServerEndpointConfig

abstract class WsConnection protected constructor(configurator: WsConfigurator, val session: javax.websocket.Session) : Connection {

    private val packetSerializer: Serializer
    private val uncaughtExceptionHandler: Thread.UncaughtExceptionHandler
    private val yassSession: Session? = null

    init {
        packetSerializer = configurator.setup.packetSerializer
        uncaughtExceptionHandler = configurator.uncaughtExceptionHandler
    }

    protected fun writeToBuffer(packet: Packet): ByteBuffer {
        val buffer = ByteBufferOutputStream(128)
        packetSerializer.write(writer(buffer), packet)
        return buffer.toByteBuffer()
    }

    internal fun onClose(closeReason: CloseReason) {
        if (closeReason.closeCode.code == CloseReason.CloseCodes.NORMAL_CLOSURE.code) {
            yassSession!!.close()
        } else {
            onError(RuntimeException(closeReason.toString()))
        }
    }

    fun onError(t: Throwable?) {
        if (t == null) {
            yassSession.close(Exception())
        } else if (t is Exception) {
            yassSession.close(t as Exception?)
        } else {
            Exceptions.uncaughtException(uncaughtExceptionHandler, t)
        }
    }

    override fun closed() {
        session.close()
    }

    interface Factory {
        fun create(configurator: WsConfigurator, session: javax.websocket.Session): WsConnection
    }

    companion object {

        internal fun create(configurator: WsConfigurator, session: javax.websocket.Session): WsConnection {
            try {
                val connection = configurator.connectionFactory.create(configurator, session)
                connection.yassSession = Session.create(configurator.setup.sessionFactory, connection)
                session.addMessageHandler(MessageHandler.Whole<ByteBuffer> { `in` ->
                    // note: could be replaced with a lambda in WebSocket API 1.1 but we would loose compatibility with 1.0
                    try {
                        connection.yassSession.received(connection.packetSerializer.read(reader(`in`)) as Packet)
                        if (`in`.hasRemaining()) {
                            throw RuntimeException("input buffer is not empty")
                        }
                    } catch (e: Exception) {
                        connection.yassSession.close(e)
                    }
                })
                return connection
            } catch (e: Exception) {
                try {
                    session.close()
                } catch (e2: Exception) {
                    e.addSuppressed(e2)
                }
                throw e
            }

        }
    }

}

/**
 * Sends messages synchronously.
 * Blocks if socket can't send data.
 */
class SyncWsConnection private constructor(configurator: WsConfigurator, session: javax.websocket.Session) : WsConnection(configurator, session) {

    private val remoteEndpoint: RemoteEndpoint.Basic
    private val writeMutex = Any()

    init {
        remoteEndpoint = session.basicRemote
    }

    override fun write(packet: Packet) {
        val buffer = writeToBuffer(packet)
        synchronized(writeMutex) {
            remoteEndpoint.sendBinary(buffer)
        }
    }

    companion object {

        val FACTORY = WsConnection.Factory { configurator, session -> SyncWsConnection(configurator, session) }
    }

}

/**
 * Sends messages asynchronously.
 * Closes session if timeout reached.
 */
class AsyncWsConnection private constructor(configurator: WsConfigurator, session: javax.websocket.Session, sendTimeoutMilliSeconds: Long) : WsConnection(configurator, session) {

    private val remoteEndpoint: RemoteEndpoint.Async

    init {
        remoteEndpoint = session.asyncRemote
        remoteEndpoint.sendTimeout = sendTimeoutMilliSeconds
    }

    override fun write(packet: Packet) {
        remoteEndpoint.sendBinary(writeToBuffer(packet)) { result ->
            if (result == null) {
                onError(null)
            } else if (!result.isOK) {
                onError(result.exception)
            }
        }
    }

    companion object {

        /**
         * @see RemoteEndpoint.Async.setSendTimeout
         */
        fun factory(sendTimeoutMilliSeconds: Long): WsConnection.Factory {
            if (sendTimeoutMilliSeconds < 0) {
                throw IllegalArgumentException("sendTimeoutMilliSeconds < 0")
            }
            return { packetSerializer, session -> AsyncWsConnection(packetSerializer, session, sendTimeoutMilliSeconds) }
        }
    }

}

class WsConfigurator(internal val connectionFactory: WsConnection.Factory, internal val setup: TransportSetup, internal val uncaughtExceptionHandler: Thread.UncaughtExceptionHandler) : ServerEndpointConfig.Configurator() {

    val endpointInstance: Endpoint
        get() = getEndpointInstance(Endpoint::class.java)

    override fun <T> getEndpointInstance(endpointClass: Class<T>): T {
        return endpointClass.cast(object : Endpoint() {
            @Volatile
            internal var connection: WsConnection? = null

            override fun onOpen(session: javax.websocket.Session, config: EndpointConfig) {
                try {
                    connection = WsConnection.create(this@WsConfigurator, session)
                } catch (t: Throwable) {
                    Exceptions.uncaughtException(uncaughtExceptionHandler, t)
                }

            }

            override fun onClose(session: javax.websocket.Session?, closeReason: CloseReason?) {
                if (connection != null) {
                    connection!!.onClose(closeReason!!)
                }
            }

            override fun onError(session: javax.websocket.Session?, throwable: Throwable?) {
                if (connection != null) {
                    connection!!.onError(throwable)
                }
            }
        })
    }

    override fun getNegotiatedSubprotocol(supported: List<String>, requested: List<String>): String =
        requested.stream().filter({ supported.contains(it) }).findFirst().orElse("")

    override fun getNegotiatedExtensions(installed: List<Extension>, requested: List<Extension>): List<Extension> =
        requested.stream().filter { r -> installed.stream().anyMatch { i -> i.name == r.name } }.collect<List<Extension>, Any>(Collectors.toList())

    override fun checkOrigin(originHeaderValue: String) = true

    override fun modifyHandshake(sec: ServerEndpointConfig?, request: HandshakeRequest?, response: HandshakeResponse?) {}

}

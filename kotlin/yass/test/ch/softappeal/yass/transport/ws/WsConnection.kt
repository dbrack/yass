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
import javax.websocket.CloseReason
import javax.websocket.MessageHandler

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

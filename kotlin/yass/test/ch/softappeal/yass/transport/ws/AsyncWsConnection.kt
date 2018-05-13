package ch.softappeal.yass.transport.ws

import ch.softappeal.yass.remote.session.Packet

import javax.websocket.RemoteEndpoint
import javax.websocket.Session

/**
 * Sends messages asynchronously.
 * Closes session if timeout reached.
 */
class AsyncWsConnection private constructor(configurator: WsConfigurator, session: Session, sendTimeoutMilliSeconds: Long) : WsConnection(configurator, session) {

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

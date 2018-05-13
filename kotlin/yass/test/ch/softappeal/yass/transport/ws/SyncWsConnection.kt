package ch.softappeal.yass.transport.ws

import ch.softappeal.yass.remote.session.Packet

import javax.websocket.RemoteEndpoint
import javax.websocket.Session

/**
 * Sends messages synchronously.
 * Blocks if socket can't send data.
 */
class SyncWsConnection private constructor(configurator: WsConfigurator, session: Session) : WsConnection(configurator, session) {

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

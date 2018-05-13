package ch.softappeal.yass.remote.session

import ch.softappeal.yass.remote.ValueReply
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.fail

class PacketTest {

    @Test
    fun end() {
        assertTrue(END_PACKET.isEnd())
        assertTrue(isEndPacket(END_REQUEST_NUMBER))
        assertFalse(isEndPacket(1))
        try {
            END_PACKET.message()
            fail()
        } catch (ignore: KotlinNullPointerException) {
        }
        try {
            END_PACKET.requestNumber()
            fail()
        } catch (ignore: IllegalStateException) {
        }
    }

    @Test
    fun normal() {
        val message = ValueReply(null)
        val packet = Packet(123, message)
        assertTrue(packet.requestNumber() == 123)
        assertSame(message, packet.message())
        assertFalse(packet.isEnd())
        try {
            Packet(END_REQUEST_NUMBER, message)
            fail()
        } catch (ignore: IllegalArgumentException) {
        }
    }

}

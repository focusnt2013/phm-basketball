package com.smartbasketball.app.util

import org.junit.Assert.*
import org.junit.Test

class NetworkMonitorTest {

    @Test
    fun `ConnectionType has all expected values`() {
        val types = ConnectionType.values()
        assertEquals(5, types.size)
        assertTrue(types.contains(ConnectionType.NONE))
        assertTrue(types.contains(ConnectionType.WIFI))
        assertTrue(types.contains(ConnectionType.CELLULAR))
        assertTrue(types.contains(ConnectionType.ETHERNET))
        assertTrue(types.contains(ConnectionType.OTHER))
    }

    @Test
    fun `ConnectionState sealed class variants`() {
        val connected = ConnectionState.Connected
        val disconnected = ConnectionState.Disconnected
        val changing = ConnectionState.Changing(ConnectionType.WIFI, ConnectionType.CELLULAR)

        assertTrue(connected is ConnectionState)
        assertTrue(disconnected is ConnectionState)
        assertTrue(changing is ConnectionState)
    }
}

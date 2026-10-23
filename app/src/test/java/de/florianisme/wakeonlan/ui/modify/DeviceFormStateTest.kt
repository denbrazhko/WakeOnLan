package de.florianisme.wakeonlan.ui.modify

import de.florianisme.wakeonlan.persistence.models.Device
import org.junit.Assert.*
import org.junit.Test

class DeviceFormStateTest {
    private fun form() = DeviceFormState(Device().apply {
        name = "PC"
        macAddress = "00:11:22:33:44:55"
        port = 9
        statusIp = "example.duckdns.org"
        statusPort = 22222
    })

    @Test
    fun invalidStatusPortsPreventSaving() {
        val state = form()
        for (port in listOf("0", "65536", "-1", "abc", "9999999999999")) {
            state.statusPort = port
            assertNotNull(state.statusPortError)
            assertFalse(state.isValid)
        }
    }

    @Test
    fun customPortIsIndependentOfWolAndCountsAsAnEdit() {
        val state = form()
        assertTrue(state.isUnchanged())
        state.statusPort = " 22223 "
        assertTrue(state.isValid)
        assertFalse(state.isUnchanged())
        val device = state.toDevice()
        assertEquals(22223, device.statusPort)
        assertEquals(9, device.port)
    }

    @Test
    fun emptyPortUses22AndEmptyHostDisablesValidation() {
        val state = form()
        state.statusPort = ""
        assertTrue(state.isValid)
        assertEquals(22, state.toDevice().statusPort)
        state.statusIp = ""
        state.statusPort = "invalid"
        assertTrue(state.isValid)
    }
}

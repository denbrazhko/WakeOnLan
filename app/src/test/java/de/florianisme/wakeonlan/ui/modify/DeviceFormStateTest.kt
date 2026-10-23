package de.florianisme.wakeonlan.ui.modify

import de.florianisme.wakeonlan.persistence.models.Device
import org.junit.Assert.*
import org.junit.Test

class DeviceFormStateTest {
    @Test
    fun keyAuthenticationNeedsUsernameKeyAndServerFingerprint() {
        val state = form()
        state.remoteShutdownEnabled = true
        state.sshUseKey = true
        state.sshCommand = "whoami"
        state.sshUsername = "denbr"
        assertFalse(state.isValid)
        state.sshPrivateKey = "private-key"
        assertFalse(state.isValid)
        state.sshHostFingerprint = "SHA256:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
        assertTrue(state.isValid)
        state.sshUsername = ""
        assertFalse(state.isValid)
    }

    @Test
    fun keyPassphrasePreservesSpacesAndDeviceConversionDoesNotChangeOriginal() {
        val original = Device().apply { id = 7; sshPassword = "old-password" }
        val state = DeviceFormState(original)
        state.sshUseKey = true
        state.sshPrivateKey = "private-key"
        state.sshKeyPassphrase = " passphrase with spaces "
        val converted = state.toDevice()
        assertEquals(7, converted.id)
        assertEquals(" passphrase with spaces ", converted.sshKeyPassphrase)
        assertEquals("", converted.sshPassword)
        assertEquals("old-password", original.sshPassword)
        assertFalse(original.sshUseKey)
        assertFalse(state.isUnchanged())
    }

    @Test
    fun disabledShutdownDoesNotRequireKeyCredentials() {
        val state = form()
        state.sshUseKey = true
        assertTrue(state.isValid)
    }

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

package de.florianisme.wakeonlan.ui.list.status;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.net.InetAddress;
import java.net.ServerSocket;

import de.florianisme.wakeonlan.persistence.models.DeviceStatus;

public class TcpRunnableTest {
    @Test
    public void listeningPortIsOnlineWithoutSendingApplicationData() throws Exception {
        try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
            assertEquals(DeviceStatus.ONLINE,
                    TcpRunnable.checkStatus("127.0.0.1", server.getLocalPort()));
            server.setSoTimeout(2000);
            try (java.net.Socket accepted = server.accept()) {
                accepted.setSoTimeout(2000);
                assertEquals(-1, accepted.getInputStream().read());
            }
        }
    }

    @Test
    public void reachableHostWithClosedPortIsOffline() throws Exception {
        int port;
        try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
            port = server.getLocalPort();
        }
        assertEquals(DeviceStatus.OFFLINE, TcpRunnable.checkStatus("127.0.0.1", port));
    }

    @Test
    public void missingHostOrInvalidPortIsUnknown() {
        assertEquals(DeviceStatus.UNKNOWN, TcpRunnable.checkStatus(null, 22));
        assertEquals(DeviceStatus.UNKNOWN, TcpRunnable.checkStatus("  ", 22));
        assertEquals(DeviceStatus.UNKNOWN, TcpRunnable.checkStatus("127.0.0.1", 0));
        assertEquals(DeviceStatus.UNKNOWN, TcpRunnable.checkStatus("127.0.0.1", 65536));
    }
}

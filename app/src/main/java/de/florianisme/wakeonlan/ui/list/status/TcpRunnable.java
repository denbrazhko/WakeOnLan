package de.florianisme.wakeonlan.ui.list.status;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import de.florianisme.wakeonlan.persistence.models.Device;
import de.florianisme.wakeonlan.persistence.models.DeviceStatus;
import de.florianisme.wakeonlan.ui.list.status.pool.StatusTestItem;

public class TcpRunnable implements Runnable {

    private static final int CONNECT_TIMEOUT_MS = 2000;

    private final Device device;
    private final StatusTestItem statusTestItem;

    public TcpRunnable(Device device, StatusTestItem statusTestItem) {
        this.device = device;
        this.statusTestItem = statusTestItem;
    }

    @Override
    public void run() {
        if (Thread.currentThread().isInterrupted()) return;
        DeviceStatus status = checkStatus(device.statusIp, device.statusPort);
        // A cancelled check must not overwrite a newer check's result.
        if (!Thread.currentThread().isInterrupted()) {
            statusTestItem.forAllListeners(listener -> listener.onStatusAvailable(status));
        }
    }

    static DeviceStatus checkStatus(String host, int port) {
        if (host == null || host.trim().isEmpty() || port < 1 || port > 65535) {
            return DeviceStatus.UNKNOWN;
        }
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host.trim(), port), CONNECT_TIMEOUT_MS);
            return DeviceStatus.ONLINE;
        } catch (UnknownHostException e) {
            return DeviceStatus.UNKNOWN;
        } catch (IOException e) {
            // Refused connections and timeouts never count as online.
            return DeviceStatus.OFFLINE;
        }
    }

}

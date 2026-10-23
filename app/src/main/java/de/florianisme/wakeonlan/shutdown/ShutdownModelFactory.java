package de.florianisme.wakeonlan.shutdown;

import androidx.annotation.Nullable;

import com.google.common.base.Strings;

import java.util.Optional;

import de.florianisme.wakeonlan.persistence.models.Device;

public class ShutdownModelFactory {

    private static final int DEFAULT_SSH_PORT = 22;

    public static Optional<ShutdownModel> fromDevice(Device device) {
        boolean shutdownEnabled = device.remoteShutdownEnabled;
        String address = getValueOrFallback(device.sshAddress, device.statusIp);
        int port = getSshPortOrFallback(device.sshPort);
        String username = getValueOrFallback(device.sshUsername, null);
        String password = device.sshUseKey ? "" : getValueOrFallback(device.sshPassword, "");
        String command = getValueOrFallback(device.sshCommand, null);

        String fingerprint = getValueOrFallback(device.sshHostFingerprint, "").trim();
        boolean fingerprintValid = fingerprint.isEmpty() ? !device.sshUseKey
                : SshAuthentication.isValidFingerprint(fingerprint);
        boolean keyValid = !device.sshUseKey || (device.sshPrivateKey != null && !device.sshPrivateKey.trim().isEmpty());
        if (allRequiredFieldsSet(shutdownEnabled, address, username, command) && keyValid && fingerprintValid && port <= 65535) {
            return Optional.of(new ShutdownModel(address, port, username, password, command,
                    device.sshUseKey, device.sshPrivateKey, device.sshKeyPassphrase, fingerprint));
        }

        return Optional.empty();
    }

    private static boolean allRequiredFieldsSet(boolean shutdownEnabled, String address, String username, String command) {
        return shutdownEnabled && address != null && username != null && command != null;
    }

    @Nullable
    private static String getValueOrFallback(@Nullable String value, @Nullable String fallback) {
        if (!Strings.isNullOrEmpty(value)) {
            return value;
        }

        return fallback;
    }

    private static Integer getSshPortOrFallback(@Nullable Integer value) {
        if (value != null && value > 0) {
            return value;
        }
        return ShutdownModelFactory.DEFAULT_SSH_PORT;
    }


}

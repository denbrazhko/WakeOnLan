package de.florianisme.wakeonlan.shutdown;

import com.google.common.io.BaseEncoding;

import java.io.IOException;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.userauth.password.PasswordUtils;

public final class SshAuthentication {
    private SshAuthentication() {}

    public static boolean isValidFingerprint(String fingerprint) {
        if (fingerprint == null || !fingerprint.startsWith("SHA256:")) return false;
        try {
            return BaseEncoding.base64().decode(fingerprint.substring(7)).length == 32;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static void configureHostVerification(SSHClient client, ShutdownModel model) {
        String fingerprint = model.getHostFingerprint();
        if (fingerprint != null && !fingerprint.isEmpty()) {
            if (!isValidFingerprint(fingerprint)) throw new IllegalArgumentException("Invalid SHA256 host fingerprint");
            client.addHostKeyVerifier(new SshHostKeyVerifier(fingerprint));
        } else if (model.isUseKey()) {
            throw new IllegalArgumentException("SSH key authentication requires the server SHA256 fingerprint");
        } else {
            // Compatibility for existing password configurations. New key configurations always pin the server.
            client.addHostKeyVerifier(new net.schmizz.sshj.transport.verification.PromiscuousVerifier());
        }
    }

    public static void authenticate(SSHClient client, ShutdownModel model) throws IOException {
        if (model.isUseKey()) {
            if (model.getPrivateKey() == null || model.getPrivateKey().trim().isEmpty()) {
                throw new IOException("Import an SSH private key first");
            }
            String passphrase = model.getKeyPassphrase();
            client.authPublickey(model.getUsername(), client.loadKeys(model.getPrivateKey(), null,
                    passphrase == null || passphrase.isEmpty() ? null
                            : PasswordUtils.createOneOff(passphrase.toCharArray())));
        } else {
            client.authPassword(model.getUsername(), model.getPassword());
        }
    }
}

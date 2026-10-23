package de.florianisme.wakeonlan.shutdown;

import com.google.common.io.BaseEncoding;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Collections;
import java.util.List;

import net.schmizz.sshj.common.Buffer;
import net.schmizz.sshj.transport.verification.HostKeyVerifier;

/** SHA256 fingerprint pinning, including on Android 7 (before java.util.Base64). */
public final class SshHostKeyVerifier implements HostKeyVerifier {
    private final byte[] expected;

    public SshHostKeyVerifier(String fingerprint) {
        if (!SshAuthentication.isValidFingerprint(fingerprint)) {
            throw new IllegalArgumentException("Invalid SHA256 host fingerprint");
        }
        expected = BaseEncoding.base64().decode(fingerprint.substring(7));
    }

    @Override
    public boolean verify(String hostname, int port, PublicKey key) {
        try {
            byte[] encoded = new Buffer.PlainBuffer().putPublicKey(key).getCompactData();
            return MessageDigest.isEqual(expected, MessageDigest.getInstance("SHA-256").digest(encoded));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    @Override
    public List<String> findExistingAlgorithms(String hostname, int port) {
        return Collections.emptyList();
    }
}

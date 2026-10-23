package de.florianisme.wakeonlan.shutdown;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

public class SshPrivateKeyImportTest {
    @Test
    public void rejectsPublicKeysAndOversizedFiles() {
        assertThrows(IOException.class, () -> SshPrivateKeyImport.read(new ByteArrayInputStream(
                "ssh-ed25519 AAAAC3Nza public-key".getBytes(StandardCharsets.UTF_8))));
        assertThrows(IOException.class, () -> SshPrivateKeyImport.read(new ByteArrayInputStream(new byte[65537])));
    }

    @Test
    public void rejectsMismatchedKeyEnvelope() {
        String text = "-----BEGIN OPENSSH PRIVATE KEY-----\nabc\n-----END RSA PRIVATE KEY-----";
        assertThrows(IOException.class, () -> SshPrivateKeyImport.read(
                new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8))));
    }
}

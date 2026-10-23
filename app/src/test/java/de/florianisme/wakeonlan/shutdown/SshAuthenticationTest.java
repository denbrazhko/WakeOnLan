package de.florianisme.wakeonlan.shutdown;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import android.util.Log;

import com.google.common.io.BaseEncoding;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.Buffer;
import net.schmizz.sshj.transport.TransportException;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
import net.schmizz.sshj.userauth.password.PasswordUtils;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.Security;

import de.florianisme.wakeonlan.persistence.models.Device;
import de.florianisme.wakeonlan.shutdown.listener.ShutdownExecutorListener;
import de.florianisme.wakeonlan.ui.backup.model.DeviceBackupModel;

public class SshAuthenticationTest {
    private static final String PIN = "SHA256:" + BaseEncoding.base64().omitPadding().encode(new byte[32]);

    @BeforeClass
    public static void installCryptoProvider() {
        Security.addProvider(new BouncyCastleProvider());
    }

    private ShutdownModel model(boolean keyMode) {
        return new ShutdownModel("example.duckdns.org", 22222, "denbr", "unused-password", "whoami",
                keyMode, "private-key", "passphrase", PIN);
    }

    @Test
    public void keyModeNeverUsesPasswordAuthentication() throws Exception {
        SSHClient client = mock(SSHClient.class);
        KeyProvider key = mock(KeyProvider.class);
        when(client.loadKeys(eq("private-key"), isNull(), any())).thenReturn(key);
        SshAuthentication.authenticate(client, model(true));
        verify(client).authPublickey("denbr", key);
        verify(client, never()).authPassword(anyString(), anyString());
    }

    @Test
    public void rejectedKeyDoesNotFallBackToPassword() throws Exception {
        SSHClient client = mock(SSHClient.class);
        when(client.loadKeys(eq("private-key"), isNull(), any())).thenThrow(new IOException("Invalid key"));
        assertThrows(IOException.class, () -> SshAuthentication.authenticate(client, model(true)));
        verify(client, never()).authPassword(anyString(), anyString());
    }

    @Test
    public void legacyPasswordModeStillWorks() throws Exception {
        SSHClient client = mock(SSHClient.class);
        SshAuthentication.authenticate(client, model(false));
        verify(client).authPassword("denbr", "unused-password");
    }

    @Test
    public void loadsEncryptedOpenSshEd25519Key() throws Exception {
        try (SSHClient client = new SSHClient();
             java.io.InputStream input = getClass().getResourceAsStream("/ssh/encrypted_ed25519")) {
            String keyText = SshPrivateKeyImport.read(java.util.Objects.requireNonNull(input));
            KeyProvider key = client.loadKeys(keyText, null,
                    PasswordUtils.createOneOff("fixture-passphrase".toCharArray()));
            assertNotNull(key.getPrivate());
            assertNotNull(key.getPublic());
        }
    }

    @Test
    public void wrongKeyPassphraseIsRejected() throws Exception {
        try (SSHClient client = new SSHClient();
             java.io.InputStream input = getClass().getResourceAsStream("/ssh/encrypted_ed25519")) {
            String keyText = SshPrivateKeyImport.read(java.util.Objects.requireNonNull(input));
            KeyProvider key = client.loadKeys(keyText, null,
                    PasswordUtils.createOneOff("wrong".toCharArray()));
            assertThrows(IOException.class, key::getPrivate);
        }
    }

    @Test
    public void loadsUnencryptedPkcs8RsaKey() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        String pem = "-----BEGIN PRIVATE KEY-----\n" +
                BaseEncoding.base64().encode(pair.getPrivate().getEncoded()) + "\n-----END PRIVATE KEY-----\n";
        try (SSHClient client = new SSHClient()) {
            assertEquals(pair.getPrivate(), client.loadKeys(pem, null, null).getPrivate());
        }
    }

    @Test
    public void hostPinMatchesOnlyTheExpectedServerKey() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        byte[] blob = new Buffer.PlainBuffer().putPublicKey(pair.getPublic()).getCompactData();
        String fingerprint = "SHA256:" + BaseEncoding.base64().omitPadding().encode(
                MessageDigest.getInstance("SHA-256").digest(blob));
        SshHostKeyVerifier verifier = new SshHostKeyVerifier(fingerprint);
        assertTrue(verifier.verify("example.duckdns.org", 22222, pair.getPublic()));
        assertFalse(verifier.verify("example.duckdns.org", 22222, generator.generateKeyPair().getPublic()));
    }

    @Test
    public void missingPinIsRejectedBeforeConnectingInKeyMode() {
        ShutdownModel model = new ShutdownModel("host", 22, "user", "", "whoami", true, "key", "", "");
        SSHClient client = mock(SSHClient.class);
        assertThrows(IllegalArgumentException.class, () -> SshAuthentication.configureHostVerification(client, model));
        verifyNoInteractions(client);
    }

    @Test
    public void transportFailureBeforeCommandIsNeverReportedAsSuccess() throws Exception {
        SSHClient client = mock(SSHClient.class);
        doThrow(new TransportException("Host key mismatch")).when(client).connect(anyString(), anyInt());
        ShutdownExecutorListener listener = mock(ShutdownExecutorListener.class);
        try (MockedStatic<Log> ignored = mockStatic(Log.class)) {
            new ShutdownRunnable(model(true), listener, () -> client).run();
        }
        verify(listener).onGeneralError(any(Exception.class), any(ShutdownModel.class));
        verify(listener, never()).onCommandExecuteSuccessful();
        verify(client, never()).startSession();
    }

    @Test
    public void backupKeepsKeyModeButExcludesAllKeyCredentials() {
        Device device = new Device();
        device.sshUseKey = true;
        device.sshPrivateKey = "PRIVATE-KEY-SECRET";
        device.sshKeyPassphrase = "PASSPHRASE-SECRET";
        device.sshPassword = "OLD-PASSWORD-SECRET";
        device.sshHostFingerprint = PIN;
        com.google.gson.Gson gson = new com.google.gson.Gson();
        String json = gson.toJson(new DeviceBackupModel(device));
        assertFalse(json.contains("SECRET"));
        Device restored = gson.fromJson(json, DeviceBackupModel.class).toModel();
        assertTrue(restored.sshUseKey);
        assertNull(restored.sshPrivateKey);
        assertEquals(PIN, restored.sshHostFingerprint);
        restored.remoteShutdownEnabled = true;
        restored.sshAddress = "host";
        restored.sshUsername = "user";
        restored.sshCommand = "whoami";
        assertFalse(ShutdownModelFactory.fromDevice(restored).isPresent());
    }
}

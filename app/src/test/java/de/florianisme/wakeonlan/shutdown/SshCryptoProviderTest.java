package de.florianisme.wakeonlan.shutdown;

import static org.junit.Assert.*;

import com.google.common.io.BaseEncoding;

import java.security.KeyFactorySpi;
import java.security.Key;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.MessageDigest;
import java.security.spec.KeySpec;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.Buffer;
import net.schmizz.sshj.common.SecurityUtils;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
import net.schmizz.sshj.userauth.password.PasswordUtils;

import org.junit.Test;

public class SshCryptoProviderTest {
    @Test
    public void sshUsesBundledProviderEvenWhenPlatformEd25519HasPriority() throws Exception {
        Provider platform = new Provider("TestPlatformEd25519", 1.0, "Simulates Android provider priority") {};
        platform.put("KeyFactory.Ed25519", UnexpectedPlatformKeyFactory.class.getName());
        Security.insertProviderAt(platform, 1);
        try (SSHClient client = SshCryptoProvider.createClient();
             java.io.InputStream input = getClass().getResourceAsStream("/ssh/encrypted_ed25519")) {
            assertEquals("TestPlatformEd25519", java.security.KeyFactory.getInstance("Ed25519").getProvider().getName());
            assertEquals("BC", SecurityUtils.getKeyFactory("Ed25519").getProvider().getName());
            assertEquals("BC", SecurityUtils.getSignature("Ed25519").getProvider().getName());
            KeyProvider key = client.loadKeys(SshPrivateKeyImport.read(java.util.Objects.requireNonNull(input)), null,
                    PasswordUtils.createOneOff("fixture-passphrase".toCharArray()));
            byte[] blob = new Buffer.PlainBuffer().putPublicKey(key.getPublic()).getCompactData();
            PublicKey hostKey = new Buffer.PlainBuffer(blob).readPublicKey();
            assertTrue(hostKey.getClass().getName().startsWith("org.bouncycastle."));
            assertArrayEquals(blob, new Buffer.PlainBuffer().putPublicKey(hostKey).getCompactData());
            String fingerprint = "SHA256:" + BaseEncoding.base64().omitPadding().encode(
                    MessageDigest.getInstance("SHA-256").digest(blob));
            assertTrue(new SshHostKeyVerifier(fingerprint).verify("host", 22222, hostKey));
            assertFalse(new SshHostKeyVerifier("SHA256:" + BaseEncoding.base64().encode(new byte[32]))
                    .verify("host", 22222, hostKey));
            Signature signature = SecurityUtils.getSignature("Ed25519");
            signature.initSign(key.getPrivate());
            byte[] challenge = new byte[] {1, 2, 3, 4};
            signature.update(challenge);
            byte[] signed = signature.sign();
            signature.initVerify(hostKey);
            signature.update(challenge);
            assertTrue(signature.verify(signed));
        } finally {
            Security.removeProvider(platform.getName());
        }
    }

    /** Any accidental use of the platform provider during SSH parsing fails this test. */
    public static class UnexpectedPlatformKeyFactory extends KeyFactorySpi {
        @Override protected PublicKey engineGeneratePublic(KeySpec spec) { throw new AssertionError("Platform provider used"); }
        @Override protected PrivateKey engineGeneratePrivate(KeySpec spec) { throw new AssertionError("Platform provider used"); }
        @Override protected <T extends KeySpec> T engineGetKeySpec(Key key, Class<T> type) { throw new AssertionError("Platform provider used"); }
        @Override protected Key engineTranslateKey(Key key) { throw new AssertionError("Platform provider used"); }
    }
}

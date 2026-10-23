package de.florianisme.wakeonlan.shutdown;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import com.google.common.io.BaseEncoding;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Encrypts imported SSH credentials at rest using a non-exportable, app-specific key. */
public final class SshSecretCipher {
    private static final String ALIAS = "wol-ssh-secrets-v1";
    private static final int IV_LENGTH = 12;

    private SshSecretCipher() {}

    private static synchronized SecretKey getKey() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        if (store.containsAlias(ALIAS)) return (SecretKey) store.getKey(ALIAS, null);
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build());
        return generator.generateKey();
    }

    public static String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) return null;
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, getKey());
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return BaseEncoding.base64().encode(ByteBuffer.allocate(IV_LENGTH + encrypted.length)
                    .put(cipher.getIV()).put(encrypted).array());
        } catch (Exception e) {
            // Never fall back to saving plaintext.
            throw new IllegalStateException("Cannot encrypt SSH credentials", e);
        }
    }

    public static String decrypt(String encrypted) {
        if (encrypted == null || encrypted.isEmpty()) return null;
        try {
            byte[] bytes = BaseEncoding.base64().decode(encrypted);
            if (bytes.length < IV_LENGTH + 16) throw new GeneralSecurityException("Invalid encrypted credential");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, getKey(), new GCMParameterSpec(128, bytes, 0, IV_LENGTH));
            return new String(cipher.doFinal(bytes, IV_LENGTH, bytes.length - IV_LENGTH), StandardCharsets.UTF_8);
        } catch (Exception e) {
            // Restoring a database on another phone cannot restore the Keystore key.
            // Require re-import rather than crashing the device list or falling back to password auth.
            return null;
        }
    }
}

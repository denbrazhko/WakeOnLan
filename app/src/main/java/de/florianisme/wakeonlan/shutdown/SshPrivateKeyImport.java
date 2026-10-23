package de.florianisme.wakeonlan.shutdown;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SshPrivateKeyImport {
    private static final int MAX_BYTES = 65536;
    private static final Pattern HEADER = Pattern.compile(
            "-----BEGIN ((?:OPENSSH |RSA |EC |ENCRYPTED )?PRIVATE KEY)-----");

    private SshPrivateKeyImport() {}

    public static String read(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int count;
        while ((count = input.read(buffer)) != -1) {
            if (output.size() + count > MAX_BYTES) throw new IOException("Private key file is too large");
            output.write(buffer, 0, count);
        }
        String key = new String(output.toByteArray(), StandardCharsets.UTF_8).trim();
        Matcher matcher = HEADER.matcher(key);
        if (!matcher.lookingAt() || !key.endsWith("-----END " + matcher.group(1) + "-----") || key.indexOf('\0') >= 0) {
            throw new IOException("Select an OpenSSH or PEM private key, not a .pub file");
        }
        return key + "\n";
    }
}

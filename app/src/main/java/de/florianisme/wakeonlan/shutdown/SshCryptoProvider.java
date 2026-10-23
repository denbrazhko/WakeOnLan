package de.florianisme.wakeonlan.shutdown;

import java.security.Security;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.SecurityUtils;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

/** Keeps SSHJ key parsing and signatures on the bundled BC implementation. */
public final class SshCryptoProvider {
    private static boolean initialized;

    private SshCryptoProvider() {}

    public static synchronized SSHClient createClient() {
        if (!initialized) {
            // Replace Android's reduced BC provider, but leave other JCA provider priorities alone.
            Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
            Security.addProvider(new BouncyCastleProvider());
            // Do not rely on SSHJ's reflective provider autodetection or the system default.
            SecurityUtils.setSecurityProvider(BouncyCastleProvider.PROVIDER_NAME);
            initialized = true;
        }
        return new SSHClient();
    }
}

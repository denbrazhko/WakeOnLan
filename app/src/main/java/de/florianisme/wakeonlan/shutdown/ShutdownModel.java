package de.florianisme.wakeonlan.shutdown;

public class ShutdownModel {

    private final String sshAddress;
    private final int sshPort;
    private final String username;
    private final String password;
    private final String command;
    private final boolean useKey;
    private final String privateKey;
    private final String keyPassphrase;
    private final String hostFingerprint;

    public ShutdownModel(String sshAddress, int sshPort, String username, String password, String command) {
        this(sshAddress, sshPort, username, password, command, false, null, null, null);
    }

    public ShutdownModel(String sshAddress, int sshPort, String username, String password, String command,
                         boolean useKey, String privateKey, String keyPassphrase, String hostFingerprint) {
        this.sshAddress = sshAddress;
        this.sshPort = sshPort;
        this.username = username;
        this.password = password;
        this.command = command;
        this.useKey = useKey;
        this.privateKey = privateKey;
        this.keyPassphrase = keyPassphrase;
        this.hostFingerprint = hostFingerprint;
    }

    public String getSshAddress() {
        return sshAddress;
    }

    public int getSshPort() {
        return sshPort;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getCommand() {
        return command;
    }

    public boolean isUseKey() { return useKey; }
    public String getPrivateKey() { return privateKey; }
    public String getKeyPassphrase() { return keyPassphrase; }
    public String getHostFingerprint() { return hostFingerprint; }
}

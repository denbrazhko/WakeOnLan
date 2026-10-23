package de.florianisme.wakeonlan.shutdown;

import android.util.Log;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.LoggerFactory;
import net.schmizz.sshj.common.StreamCopier;
import net.schmizz.sshj.connection.channel.direct.Session;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import de.florianisme.wakeonlan.shutdown.exception.CommandExecuteException;
import de.florianisme.wakeonlan.shutdown.listener.ShutdownExecutorListener;

public class ShutdownRunnable implements Runnable {

    private static final int CONNECT_TIMEOUT = 5000;
    private static final int EXECUTE_TIMEOUT = 5000;

    private final ShutdownModel shutdownModel;
    private final ShutdownExecutorListener shutdownExecutorListener;
    private final Supplier<SSHClient> clientFactory;

    public ShutdownRunnable(ShutdownModel shutdownModel, ShutdownExecutorListener shutdownExecutorListener) {
        this(shutdownModel, shutdownExecutorListener, SshCryptoProvider::createClient);
    }

    ShutdownRunnable(ShutdownModel shutdownModel, ShutdownExecutorListener shutdownExecutorListener,
                     Supplier<SSHClient> clientFactory) {
        this.shutdownModel = shutdownModel;
        this.shutdownExecutorListener = shutdownExecutorListener;
        this.clientFactory = clientFactory;
    }

    @Override
    public void run() {
        ByteArrayOutputStream commandOutputStream = new ByteArrayOutputStream();

        try (SSHClient sshClient = clientFactory.get()) {
            SshAuthentication.configureHostVerification(sshClient, shutdownModel);
            sshClient.setConnectTimeout(CONNECT_TIMEOUT);
            sshClient.setTimeout(EXECUTE_TIMEOUT);
            sshClient.connect(shutdownModel.getSshAddress(), shutdownModel.getSshPort());
            shutdownExecutorListener.onTargetHostReached();

            SshAuthentication.authenticate(sshClient, shutdownModel);
            shutdownExecutorListener.onLoginSuccessful();

            Session session = sshClient.startSession();
            shutdownExecutorListener.onSessionStartSuccessful();

            session.allocateDefaultPTY();
            Session.Command exec = session.exec(shutdownModel.getCommand());
            new StreamCopier(exec.getInputStream(), commandOutputStream, LoggerFactory.DEFAULT)
                    .bufSize(exec.getLocalMaxPacketSize())
                    .spawn("stdout");

            exec.join(EXECUTE_TIMEOUT, TimeUnit.MILLISECONDS);
            Integer exitStatus = exec.getExitStatus();
            if (exitStatus == null) {
                throw new IOException("SSH command ended without an exit status; execution could not be confirmed");
            }
            if (exitStatus != 0) {
                throw new CommandExecuteException("Command exited with status code " + exitStatus, exitStatus);
            }

            shutdownExecutorListener.onCommandExecuteSuccessful();
        } catch (Exception e) {
            Log.e(ShutdownRunnable.class.getSimpleName(), "Error during SSH execution", e);

            if (sudoPrompt(commandOutputStream)) {
                shutdownExecutorListener.onSudoPromptTriggered(shutdownModel);
                return;
            }

            shutdownExecutorListener.onGeneralError(e, shutdownModel);
        }
    }

    private boolean sudoPrompt(ByteArrayOutputStream commandOutputStream) {
        return new String(commandOutputStream.toByteArray(), StandardCharsets.UTF_8).contains("[sudo] password for ");
    }
}

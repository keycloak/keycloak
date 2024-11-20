package org.keycloak.test.framework.mail;

import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import jakarta.mail.internet.MimeMessage;
import org.keycloak.test.framework.injection.ManagedTestResource;

public class MailServer extends ManagedTestResource {

    private static final int PORT = 3025;
    private static final String HOST = "localhost";

    private GreenMail greenMail;

    public void start() {
        ServerSetup setup = new ServerSetup(PORT, HOST, "smtp");

        greenMail = new GreenMail(setup);
        greenMail.start();
    }

    public void stop() {
        greenMail.stop();
    }

    public MimeMessage[] getReceivedMessages() {
        return greenMail.getReceivedMessages();
    }

    public MimeMessage getLastReceivedMessage() {
        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
        return receivedMessages != null && receivedMessages.length > 0 ? receivedMessages[receivedMessages.length - 1] : null;
    }

    public boolean waitForIncomingEmail(long timeout, int emailCount) {
        return greenMail.waitForIncomingEmail(timeout, emailCount);
    }

    public boolean waitForIncomingEmail(int emailCount) {
        return greenMail.waitForIncomingEmail(emailCount);
    }

    @Override
    public void runCleanup() {
        try {
            greenMail.purgeEmailFromAllMailboxes();
        } catch (FolderException e) {
            throw new RuntimeException(e);
        }
    }
}

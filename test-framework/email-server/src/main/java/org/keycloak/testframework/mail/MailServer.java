package org.keycloak.testframework.mail;

import jakarta.mail.internet.MimeMessage;

import org.keycloak.testframework.injection.ManagedTestResource;

import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.user.TokenValidator;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;

public class MailServer extends ManagedTestResource {

    private final GreenMail greenMail;

    public MailServer(String host, int port) {
        ServerSetup setup = new ServerSetup(port, host, "smtp");

        greenMail = new GreenMail(setup);
        greenMail.start();
    }

    public void stop() {
        greenMail.stop();
    }

    public void credentials(String username, String password) {
        greenMail.setUser(username, password);
    }

    public void credentials(String username, TokenValidator validator) {
        greenMail.setUser(username, null);
        GreenMailUser user = greenMail.getUserManager().getUser(username);
        // greenmail refactoring required, see https://github.com/greenmail-mail-test/greenmail/pull/838
        ((com.icegreen.greenmail.user.UserImpl)user).setTokenValidator(validator);
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

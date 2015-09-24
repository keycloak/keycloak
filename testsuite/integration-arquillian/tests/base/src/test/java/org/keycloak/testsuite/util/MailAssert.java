package org.keycloak.testsuite.util;

import java.io.IOException;
import javax.mail.MessagingException;

import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMessage.RecipientType;
import javax.mail.internet.MimeMultipart;
import org.jboss.logging.Logger;
import static org.junit.Assert.*;

public class MailAssert {

    private static final Logger log = Logger.getLogger(MailAssert.class);
    
    public static String assertEmailAndGetUrl(String from, String recipient, String content) {

        try {
            MimeMessage message = MailServer.getLastReceivedMessage();
            assertNotNull("There is no received email.", message);
            assertEquals(recipient, message.getRecipients(RecipientType.TO)[0].toString());
            assertEquals(from, message.getFrom()[0].toString());

            String messageContent;
            if (message.getContent() instanceof MimeMultipart) {
                MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();

                messageContent = String.valueOf(mimeMultipart.getBodyPart(0).getContent());
            } else {
                messageContent = String.valueOf(message.getContent());
            }
            logMessageContent(messageContent);
            String errorMessage = "Email content should contains \"" + content
                    + "\", but it doesn't.\nEmail content:\n" + messageContent + "\n";

            assertTrue(errorMessage, messageContent.contains(content));
            for (String string : messageContent.split("\n")) {
                if (string.contains("http://")) {
                    return string;
                }
            }
            return null;
        } catch (IOException | MessagingException | InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static void logMessageContent(String messageContent) {
        log.debug("---------------------");
        log.debug(messageContent);
        log.debug("---------------------");
    }

}

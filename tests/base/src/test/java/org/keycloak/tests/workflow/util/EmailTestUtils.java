package org.keycloak.tests.workflow.util;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.keycloak.testframework.mail.MailServer;
import org.keycloak.tests.utils.MailUtils;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EmailTestUtils {

    public static List<MimeMessage> findEmailsByRecipient(MailServer mailServer, String expectedRecipient) {
        return Arrays.stream(mailServer.getReceivedMessages())
                .filter(msg -> {
                    try {
                        return MailUtils.getRecipient(msg).equals(expectedRecipient);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .toList();
    }

    public static MimeMessage findEmailByRecipient(MailServer mailServer, String expectedRecipient) {
        return Arrays.stream(mailServer.getReceivedMessages())
                .filter(msg -> {
                    try {
                        return MailUtils.getRecipient(msg).equals(expectedRecipient);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .findFirst()
                .orElse(null);
    }

    public static MimeMessage findEmailByRecipientContaining(MailServer mailServer, String recipientPart) {
        return Arrays.stream(mailServer.getReceivedMessages())
                .filter(msg -> {
                    try {
                        return MailUtils.getRecipient(msg).contains(recipientPart);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .findFirst()
                .orElse(null);
    }

    public static void verifyEmailContent(MimeMessage message, String expectedRecipient, String subjectContains,
                                          String... contentContains) {
        try {
            assertEquals(expectedRecipient, MailUtils.getRecipient(message));
            assertThat(message.getSubject(), Matchers.containsString(subjectContains));

            MailUtils.EmailBody body = MailUtils.getBody(message);
            String textContent = body.getText();
            String htmlContent = body.getHtml();

            for (String expectedContent : contentContains) {
                boolean foundInText = textContent.contains(expectedContent);
                boolean foundInHtml = htmlContent.contains(expectedContent);
                assertTrue(foundInText || foundInHtml,
                        "Email content should contain: " + expectedContent +
                                "\nText: " + textContent +
                                "\nHTML: " + htmlContent);
            }
        } catch (MessagingException | IOException e) {
            Assertions.fail("Failed to read email message: " + e.getMessage());
        }
    }
}

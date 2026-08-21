package org.keycloak.email;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultEmailSenderProviderTest {

    @Test
    void testEmailTimeoutPropertiesDefaults() throws EmailException {
        // given
        DefaultEmailSenderProvider defaultEmailSenderProvider = new DefaultEmailSenderProvider(
                null, null
        );
        String from = "test@keycloak.com";

        Map<String, String> config = new HashMap<>();

        // when
        Properties properties = defaultEmailSenderProvider.buildEmailProperties(config, from);

        // then
        assertThat(properties.getProperty("mail.smtp.timeout"), is("10000"));
        assertThat(properties.getProperty("mail.smtp.connectiontimeout"), is("10000"));
        assertThat(properties.getProperty("mail.smtp.writetimeout"), is("10000"));
    }

    @Test
    void testEmailTimeoutPropertiesFromConfig() throws EmailException {
        // given
        DefaultEmailSenderProvider defaultEmailSenderProvider = new DefaultEmailSenderProvider(
                null, null
        );
        String from = "test@keycloak.com";

        Map<String, String> config = new HashMap<>();
        config.put("timeout", "20000");
        config.put("connectionTimeout", "30000");
        config.put("writeTimeout", "40000");

        // when
        Properties properties = defaultEmailSenderProvider.buildEmailProperties(config, from);

        // then
        assertThat(properties.getProperty("mail.smtp.timeout"), is("20000"));
        assertThat(properties.getProperty("mail.smtp.connectiontimeout"), is("30000"));
        assertThat(properties.getProperty("mail.smtp.writetimeout"), is("40000"));
    }

    @Test
    void testValidateWithInvalidFromAddressThrowsException() {
        // given
        DefaultEmailSenderProvider provider = new DefaultEmailSenderProvider(null, null);
        Map<String, String> config = new HashMap<>();
        config.put("from", "invalid-email");

        // when
        EmailException exception = assertThrows(EmailException.class, () -> provider.validate(config));

        // then
        assertThat(exception.getMessage(), containsString("Invalid sender address"));
    }

    @Test
    void testValidateWithInvalidReplyToAddressThrowsException() {
        // given
        DefaultEmailSenderProvider provider = new DefaultEmailSenderProvider(null, null);
        Map<String, String> config = new HashMap<>();
        config.put("from", "valid@example.com");
        config.put("replyTo", "invalid-replyto");

        // when
        EmailException exception = assertThrows(EmailException.class, () -> provider.validate(config));

        // then
        assertThat(exception.getMessage(), containsString("Invalid reply-to address"));
    }

    @Test
    void testCustomizeMessageCanAddHeaders() throws Exception {
        // given
        GreenMail greenMail = new GreenMail(ServerSetupTest.SMTP.dynamicPort());
        greenMail.start();
        try {
            DefaultEmailSenderProvider provider = new DefaultEmailSenderProvider(null,
                    Map.of(EmailAuthenticator.AuthenticatorType.NONE, new DefaultEmailAuthenticator())) {
                @Override
                protected void customizeMessage(Message message, Map<String, String> config) throws MessagingException {
                    message.setHeader("X-Custom-Header", "custom-value");
                }
            };

            Map<String, String> config = new HashMap<>();
            config.put("from", "test@keycloak.com");
            config.put("host", "localhost");
            config.put("port", String.valueOf(greenMail.getSmtp().getPort()));

            // when
            provider.send(config, "user@keycloak.com", "subject", "text body", null);

            // then
            assertThat(greenMail.waitForIncomingEmail(1), is(true));
            MimeMessage received = greenMail.getReceivedMessages()[0];
            assertThat(received.getHeader("X-Custom-Header", null), is("custom-value"));
        } finally {
            greenMail.stop();
        }
    }
}

package org.keycloak.email;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

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
}

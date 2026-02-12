package org.keycloak.email;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import jakarta.mail.internet.MimeMultipart;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

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
    void testEmailContentTypeDefault() throws Exception {
        testEmailContentType(null, MimeMultipart.class);
    }

    @Test
    void testEmailContentTypeMultipart() throws Exception {
        testEmailContentType("multipart", MimeMultipart.class);
    }

    @Test
    void testEmailContentTypeTextOnly() throws Exception {
        testEmailContentType("text_only", String.class);
    }

    @Test
    void testEmailContentTypeHtmlOnly() throws Exception {
        testEmailContentType("html_only", String.class);
    }

    private void testEmailContentType(String contentTypeConfig, Class<?> expectedContentClass) throws Exception {
        // given
        DefaultEmailSenderProvider provider = new DefaultEmailSenderProvider(null, null);
        Map<String, String> config = new HashMap<>();
        if (contentTypeConfig != null) {
            config.put(EmailSenderProvider.CONFIG_EMAIL_CONTENT_TYPE, contentTypeConfig);
        }

        // when
        Object content = provider.buildEmailBody("text body", "html body", 
            contentTypeConfig == null ? EmailSenderProvider.EmailContentType.MULTIPART : 
            EmailSenderProvider.EmailContentType.valueOf(contentTypeConfig.toUpperCase()));

        // then
        assertThat(content, instanceOf(expectedContentClass));
    }
}

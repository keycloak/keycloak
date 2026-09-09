package org.keycloak.email;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMultipart;

import org.keycloak.models.RealmModel;
import org.keycloak.theme.Theme;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
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
    void testExtractCidReferences() {
        String html = "<html><body><img src=\"cid:img/logo.png\" /><div style=\"background: url('cid:img/bg.png')\"></div></body></html>";

        Set<String> paths = DefaultEmailSenderProvider.extractCidReferences(html);

        assertThat(paths.size(), is(2));
        assertThat(paths.contains("img/logo.png"), is(true));
        assertThat(paths.contains("img/bg.png"), is(true));
    }

    @Test
    void testContentIdsForUsesFileNameAndHandlesCollisions() {
        Set<String> paths = new LinkedHashSet<>();
        paths.add("assets/logo.png");
        paths.add("other/logo.png");

        Map<String, String> ids = DefaultEmailSenderProvider.contentIdsFor(paths);

        assertThat(ids.get("assets/logo.png"), is("logo.png"));
        assertThat(ids.get("other/logo.png"), is("logo.png-2"));
    }

    @Test
    void testRewriteCidReferences() {
        String html = "<img src=\"cid:assets/logo.png\" />";
        Map<String, String> mapping = Map.of("assets/logo.png", "logo.png");

        String resolved = DefaultEmailSenderProvider.rewriteCidReferences(html, mapping);

        assertThat(resolved, is("<img src=\"cid:logo.png\" />"));
    }

    @Test
    void testRewriteCidReferencesDoesNotCorruptPrefixPaths() {
        String html = "<img src=\"cid:img/logo\" /><img src=\"cid:img/logo/icon.png\" />";
        Map<String, String> mapping = Map.of(
                "img/logo", "logo",
                "img/logo/icon.png", "icon.png");

        String resolved = DefaultEmailSenderProvider.rewriteCidReferences(html, mapping);

        assertThat(resolved, is("<img src=\"cid:logo\" /><img src=\"cid:icon.png\" />"));
    }

    @Test
    void testBuildMultipartBodyWithoutCidReferences() throws Exception {
        DefaultEmailSenderProvider provider = new DefaultEmailSenderProvider(null, null);

        Multipart multipart = provider.buildMultipartBody("text", "<html>plain</html>", null);

        assertThat(multipart.getCount(), is(2));
        assertThat(multipart.getContentType(), containsString("alternative"));
    }

    @Test
    void testBuildMultipartBodyWithCidEmbeddedImage() throws Exception {
        byte[] logoBytes = new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47};
        Theme theme = new TestTheme(Map.of("assets/logo.png", logoBytes));
        DefaultEmailSenderProvider provider = new DefaultEmailSenderProvider(null, null);
        String html = "<html><body><img src=\"cid:assets/logo.png\" /></body></html>";

        Multipart multipart = provider.buildMultipartBody("text", html, theme);

        assertThat(multipart.getContentType(), containsString("related"));
        assertThat(multipart.getCount(), is(2));

        BodyPart alternativeWrapper = multipart.getBodyPart(0);
        assertThat(alternativeWrapper.getContent(), is(instanceOf(MimeMultipart.class)));
        MimeMultipart alternative = (MimeMultipart) alternativeWrapper.getContent();
        assertThat(alternative.getContentType(), containsString("alternative"));
        assertThat(alternative.getCount(), is(2));

        BodyPart htmlPart = alternative.getBodyPart(1);
        assertThat(htmlPart.getContent().toString(), containsString("cid:logo.png"));
        assertThat(htmlPart.getContent().toString(), not(containsString("cid:assets/logo.png")));

        MimeBodyPart imagePart = (MimeBodyPart) multipart.getBodyPart(1);
        assertThat(imagePart.getContentID(), is("<logo.png>"));
        assertThat(imagePart.getDisposition(), is(MimeBodyPart.INLINE));
        assertThat(imagePart.getFileName(), is("logo.png"));
    }

    @Test
    void testBuildMultipartBodyWithMissingCidResource() throws Exception {
        Theme theme = new TestTheme(Map.of());
        DefaultEmailSenderProvider provider = new DefaultEmailSenderProvider(null, null);
        String html = "<html><body><img src=\"cid:assets/missing.png\" /></body></html>";

        Multipart multipart = provider.buildMultipartBody(null, html, theme);

        assertThat(multipart.getContentType(), containsString("related"));
        assertThat(multipart.getCount(), is(1));

        MimeMultipart alternative = (MimeMultipart) multipart.getBodyPart(0).getContent();
        assertThat(alternative.getBodyPart(0).getContent().toString(), containsString("cid:missing.png"));
    }

    private static class TestTheme implements Theme {

        private final Map<String, byte[]> resources;

        private TestTheme(Map<String, byte[]> resources) {
            this.resources = resources;
        }

        @Override
        public String getName() {
            return "test";
        }

        @Override
        public String getParentName() {
            return null;
        }

        @Override
        public String getImportName() {
            return null;
        }

        @Override
        public Type getType() {
            return Type.EMAIL;
        }

        @Override
        public URL getTemplate(String name) {
            return null;
        }

        @Override
        public InputStream getResourceAsStream(String path) {
            byte[] resource = resources.get(path);
            return resource != null ? new ByteArrayInputStream(resource) : null;
        }

        @Override
        public Properties getMessages(Locale locale) {
            return new Properties();
        }

        @Override
        public Properties getMessages(String baseBundlename, Locale locale) {
            return new Properties();
        }

        @Override
        public Properties getEnhancedMessages(RealmModel realm, Locale locale) {
            return new Properties();
        }

        @Override
        public Properties getProperties() {
            return new Properties();
        }
    }
}

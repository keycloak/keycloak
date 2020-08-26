package org.keycloak.testsuite.theme;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.common.Version;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;
import org.keycloak.testsuite.utils.io.IOUtil;
import org.keycloak.theme.Theme;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@AuthServerContainerExclude(AuthServer.REMOTE)
public class ThemeResourceProviderTest extends AbstractTestRealmKeycloakTest {

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {

    }

    @Test
    public void getTheme() {
        testingClient.server().run(session -> {
            try {
                Theme theme = session.theme().getTheme("base", Theme.Type.LOGIN);
                Assert.assertNotNull(theme.getTemplate("test.ftl"));
            } catch (IOException e) {
                Assert.fail(e.getMessage());
            }
        });
    }

    @Test
    public void getResourceAsStream() {
        testingClient.server().run(session -> {
            try {
                Theme theme = session.theme().getTheme("base", Theme.Type.LOGIN);
                Assert.assertNotNull(theme.getResourceAsStream("test.js"));
            } catch (IOException e) {
                Assert.fail(e.getMessage());
            }
        });
    }

    @Test
    public void getMessages() {
        testingClient.server().run(session -> {
            try {
                Theme theme = session.theme().getTheme("base", Theme.Type.LOGIN);
                Assert.assertNotNull(theme.getMessages("messages", Locale.ENGLISH).get("test.keycloak-8818"));
                Assert.assertNotEquals("Full name (Theme-resources)", theme.getMessages("messages", Locale.ENGLISH).get("fullName"));
            } catch (IOException e) {
                Assert.fail(e.getMessage());
            }
        });
    }

    @Test
    public void gzipEncoding() throws IOException {
        final String resourcesVersion = testingClient.server().fetch(session -> Version.RESOURCES_VERSION, String.class);

        assertEncoded(suiteContext.getAuthServerInfo().getContextRoot().toString() + "/auth/resources/" + resourcesVersion + "/welcome/keycloak/css/welcome.css", "body {");
        assertEncoded(suiteContext.getAuthServerInfo().getContextRoot().toString() + "/auth/js/keycloak.js", "function(root, factory)");

        testingClient.server().run(session -> {
            assertTrue(Paths.get(System.getProperty("java.io.tmpdir"), "kc-gzip-cache", resourcesVersion, "welcome", "keycloak", "css", "welcome.css.gz").toFile().isFile());
            assertTrue(Paths.get(System.getProperty("java.io.tmpdir"), "kc-gzip-cache", resourcesVersion, "js", "keycloak.js.gz").toFile().isFile());
        });
    }

    private void assertEncoded(String url, String expectedContent) throws IOException {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().disableContentCompression().build()) {
            HttpGet get = new HttpGet(url);
            CloseableHttpResponse response = httpClient.execute(get);

            InputStream is = response.getEntity().getContent();
            assertNull(response.getFirstHeader("Content-Encoding"));

            String plain = IOUtils.toString(is, StandardCharsets.UTF_8);

            response.close();

            get = new HttpGet(url);
            get.addHeader("Accept-Encoding", "gzip");
            response = httpClient.execute(get);


            is = response.getEntity().getContent();
            assertEquals("gzip", response.getFirstHeader("Content-Encoding").getValue());

            String gzip = IOUtils.toString(new GZIPInputStream(is), StandardCharsets.UTF_8);

            response.close();

            assertEquals(plain, gzip);
            assertTrue(plain.contains(expectedContent));
        }
    }

    /**
     * See KEYCLOAK-12926
     */
    @Test
    public void getMessagesLocaleResolving() {
        testingClient.server().run(session -> {
            try {
                Theme theme = session.theme().getTheme("base", Theme.Type.LOGIN);
                assertEquals("Test en_US_variant", theme.getMessages("messages", new Locale("en", "US", "variant")).get("test.keycloak-12926"));
                assertEquals("Test en_US", theme.getMessages("messages", new Locale("en", "US")).get("test.keycloak-12926"));
                assertEquals("Test en", theme.getMessages("messages", Locale.ENGLISH).get("test.keycloak-12926"));
                assertEquals("Test en_US", theme.getMessages("messages", new Locale("en", "US")).get("test.keycloak-12926"));
                assertEquals("Test en", theme.getMessages("messages", Locale.ENGLISH).get("test.keycloak-12926"));

                assertEquals("only de_AT_variant", theme.getMessages("messages", new Locale("de", "AT", "variant")).get("test.keycloak-12926-resolving1"));
                assertNull(theme.getMessages("messages", new Locale("de", "AT")).get("test.keycloak-12926-resolving1"));

                assertEquals("only de_AT", theme.getMessages("messages", new Locale("de", "AT", "variant")).get("test.keycloak-12926-resolving2"));
                assertNull(theme.getMessages("messages", new Locale("de")).get("test.keycloak-12926-resolving2"));

                assertEquals("only de", theme.getMessages("messages", new Locale("de", "AT", "variant")).get("test.keycloak-12926-only_de"));
                assertNull(theme.getMessages("messages", Locale.ENGLISH).get("test.keycloak-12926-only_de"));

                assertEquals("fallback en", theme.getMessages("messages", new Locale("de", "AT", "variant")).get("test.keycloak-12926-resolving3"));
                assertEquals("fallback en", theme.getMessages("messages", new Locale("de", "AT")).get("test.keycloak-12926-resolving3"));
                assertEquals("fallback en", theme.getMessages("messages", new Locale("de")).get("test.keycloak-12926-resolving3"));
                assertNull(theme.getMessages("messages", Locale.ENGLISH).get("fallback en"));

            } catch (IOException e) {
                Assert.fail(e.getMessage());
            }
        });
    }
}

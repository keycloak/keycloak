package org.keycloak.testsuite.theme;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.keycloak.common.Version;
import org.keycloak.platform.Platform;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;
import org.keycloak.theme.Theme;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
    public void getResourceIllegalTraversal() {
        testingClient.server().run(session -> {
            try {
                Theme theme = session.theme().getTheme("base", Theme.Type.LOGIN);
                Assert.assertNull(theme.getResourceAsStream("../templates/test.ftl"));
            } catch (IOException e) {
                Assert.fail(e.getMessage());
            }
        });
    }

    @Test
    public void gzipEncoding() throws IOException {
        final String resourcesVersion = testingClient.server().fetch(session -> Version.RESOURCES_VERSION, String.class);

        // This will return true if files did not exists before the test OR they did exists, but were successfully deleted.
        // False will be returned just in case that files were exists, but were NOT successfully deleted.
        // This can happen in rare case when the file were created before in "tmp" directory by different system user and current user can't delete them
        boolean filesNotExistsInTmp = testingClient.server().fetch(session -> {
            boolean deleted = true;
            File file1 = Paths.get(System.getProperty("java.io.tmpdir"), "kc-gzip-cache", resourcesVersion, "welcome", "keycloak", "css", "welcome.css.gz").toFile();
            if (file1.isFile()) {
                deleted = file1.delete();
            }

            File file2 = Paths.get(System.getProperty("java.io.tmpdir"), "kc-gzip-cache", resourcesVersion, "js", "keycloak.js.gz").toFile();
            if (file2.isFile()) {
                deleted = deleted && file2.delete();
            }

            return deleted;
        }, Boolean.class);

        assertEncoded(suiteContext.getAuthServerInfo().getContextRoot().toString() + "/auth/resources/" + resourcesVersion + "/welcome/keycloak/css/welcome.css", "body {");
        assertEncoded(suiteContext.getAuthServerInfo().getContextRoot().toString() + "/auth/js/keycloak.js", "function Keycloak (config)");

        // Check no files exists inside "/tmp" directory. We need to skip this test in the rare case when there are thombstone files created by different user
        if (filesNotExistsInTmp) {
            testingClient.server().run(session -> {
                assertFalse(Paths.get(System.getProperty("java.io.tmpdir"), "kc-gzip-cache", resourcesVersion, "welcome", "keycloak", "css", "welcome.css.gz").toFile().isFile());
                assertFalse(Paths.get(System.getProperty("java.io.tmpdir"), "kc-gzip-cache", resourcesVersion, "js", "keycloak.js.gz").toFile().isFile());
            });
        }

        testingClient.server().run(session -> {
            String serverTmpDir = Platform.getPlatform().getTmpDirectory().toString();
            assertTrue(Paths.get(serverTmpDir, "kc-gzip-cache", resourcesVersion, "welcome", "keycloak", "css", "welcome.css.gz").toFile().isFile());
            assertTrue(Paths.get(serverTmpDir, "kc-gzip-cache", resourcesVersion, "js", "keycloak.js.gz").toFile().isFile());
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

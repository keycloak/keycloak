package org.keycloak.testsuite.theme;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.keycloak.common.Profile;
import org.keycloak.common.Version;
import org.keycloak.platform.Platform;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.arquillian.annotation.EnableFeatures;
import org.keycloak.theme.Theme;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
    public void testThemeFallback() {
        testingClient.server().run(session -> {
            try {
                // Fallback to default theme when requested theme don't exists
                Theme theme = session.theme().getTheme("address", Theme.Type.ADMIN);
                Assert.assertNotNull(theme);
                Assert.assertEquals("keycloak.v2", theme.getName());
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

            return deleted;
        }, Boolean.class);

        assertEncoded(suiteContext.getAuthServerInfo().getContextRoot().toString() + "/auth/resources/" + resourcesVersion + "/welcome/keycloak/css/welcome.css", ".pf-v5-c-background-image");

        // Check no files exists inside "/tmp" directory. We need to skip this test in the rare case when there are thombstone files created by different user
        if (filesNotExistsInTmp) {
            testingClient.server().run(session -> {
                assertFalse(Paths.get(System.getProperty("java.io.tmpdir"), "kc-gzip-cache", resourcesVersion, "welcome", "keycloak", "css", "welcome.css.gz").toFile().isFile());
            });
        }

        testingClient.server().run(session -> {
            String serverTmpDir = Platform.getPlatform().getTmpDirectory().toString();
            assertTrue(Paths.get(serverTmpDir, "kc-gzip-cache", resourcesVersion, "welcome", "keycloak", "css", "welcome.css.gz").toFile().isFile());
        });
    }

    @Test
    public void notFoundOnInvalidThemeType() throws IOException {
        final String resourcesVersion = testingClient.server().fetch(session -> Version.RESOURCES_VERSION, String.class);
        assertNotFound(suiteContext.getAuthServerInfo().getContextRoot().toString() + "/auth/resources/" + resourcesVersion + "/invalid-theme-type/keycloak/css/welcome.css");
    }

    @Test
    @EnableFeatures(@EnableFeature(Profile.Feature.ROLLING_UPDATES_V2))
    public void fetchStaticResourceShouldRedirectOnUnknownVersion() throws IOException {
        final String resourcesVersion = testingClient.server().fetch(session -> Version.RESOURCES_VERSION, String.class);
        assertFound(suiteContext.getAuthServerInfo().getContextRoot().toString() + "/auth/resources/" + resourcesVersion + "/login/keycloak.v2/css/styles.css");
        assertFound(suiteContext.getAuthServerInfo().getContextRoot().toString() + "/auth/resources/" + resourcesVersion + "/login/keycloak.v2/css%2Fstyles.css");
        assertNotFound(suiteContext.getAuthServerInfo().getContextRoot().toString() + "/auth/resources/" + "unkno" + "/login/keycloak.v2/css%2Fstyles.css");
        assertNotFound(suiteContext.getAuthServerInfo().getContextRoot().toString() + "/auth/resources/" + "unkn%2F" + "/login/keycloak.v2/css/styles.css");
        // This on check will fail on Quarkus as Quarkus will normalize the URL before handing it to the REST endpoint
        // It will succeed on Undertow
        // assertNotFound(suiteContext.getAuthServerInfo().getContextRoot().toString() + "/auth/resources/" + "unkno" + "/login/keycloak.v2/css/../css/styles.css");
        assertRedirectAndValidateRedirect(suiteContext.getAuthServerInfo().getContextRoot().toString() + "/auth/resources/" + "unkno" + "/login/keycloak.v2/css/styles.css?name=%2Fvalue",
                suiteContext.getAuthServerInfo().getContextRoot().toString() + "/auth/resources/" + resourcesVersion + "/login/keycloak.v2/css/styles.css?name=%2Fvalue");
    }


    @Test
    @EnableFeatures(@EnableFeature(Profile.Feature.ROLLING_UPDATES_V2))
    public void fetchResourceWithContentHashShouldReturnContentIfVersionIsUnknown() throws IOException {
        final String resourcesVersion = testingClient.server().fetch(session -> Version.RESOURCES_VERSION, String.class);

        String resource = getResourceWithContentHash();

        // The original resource should be accessible.
        assertNoRedirect(suiteContext.getAuthServerInfo().getContextRoot().toString() + resource);

        // The unknown resource should be accessible without a redirect.
        assertNoRedirect(suiteContext.getAuthServerInfo().getContextRoot().toString() + resource.replaceAll(Pattern.quote(resourcesVersion), "unkno"));
    }

    @Test
    @EnableFeatures(@EnableFeature(Profile.Feature.ROLLING_UPDATES_V2))
    public void fetchResourceWithContentHashShouldHonorEtag() throws IOException {
        String resource = getResourceWithContentHash();

        // The first fetch should return an etag
        String etag = fetchEtag(suiteContext.getAuthServerInfo().getContextRoot().toString() + resource);

        // The second fetch with the etag should return not modified
        assertEtagHonored(suiteContext.getAuthServerInfo().getContextRoot().toString() + resource, etag);
    }

    private String getResourceWithContentHash() throws IOException {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            HttpGet get = new HttpGet(suiteContext.getAuthServerInfo().getContextRoot().toString() + "/auth/admin/" + TEST_REALM_NAME + "/console/");
            try (CloseableHttpResponse response = httpClient.execute(get)) {
                assertEquals(200, response.getStatusLine().getStatusCode());
                String body = EntityUtils.toString(response.getEntity());
                Matcher matcher = Pattern.compile("<link rel=\"stylesheet\" href=\"([^\"]*)\">").matcher(body);
                if (matcher.find()) {
                    return matcher.group(1);
                } else {
                    throw new AssertionError("unable to find resource in body");
                }
            }
        }
    }

    private void assertNotFound(String url) throws IOException {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            HttpGet get = new HttpGet(url);
            try (CloseableHttpResponse response = httpClient.execute(get)) {
                assertEquals(404, response.getStatusLine().getStatusCode());
            }
        }
    }

    private void assertFound(String url) throws IOException {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            HttpGet get = new HttpGet(url);
            CloseableHttpResponse response = httpClient.execute(get);

            MatcherAssert.assertThat(response.getStatusLine().getStatusCode(), CoreMatchers.equalTo(200));
        }
    }

    private String fetchEtag(String url) throws IOException {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            HttpGet get = new HttpGet(url);
            CloseableHttpResponse response = httpClient.execute(get);

            MatcherAssert.assertThat(response.getStatusLine().getStatusCode(), CoreMatchers.equalTo(200));

            return response.getFirstHeader("ETag").getValue();
        }
    }

    private void assertRedirectAndValidateRedirect(String url, String redirect) throws IOException {
        assertRedirect(url, redirect);
        assertFound(url);
    }

    private void assertRedirect(String url, String redirect) throws IOException {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().disableRedirectHandling().build()) {
            HttpGet get = new HttpGet(url);
            CloseableHttpResponse response = httpClient.execute(get);

            MatcherAssert.assertThat(response.getStatusLine().getStatusCode(), CoreMatchers.equalTo(307));
            MatcherAssert.assertThat(response.getFirstHeader("Location").getValue(), CoreMatchers.equalTo(redirect));
        }
    }

    private void assertEtagHonored(String url, String etag) throws IOException {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().disableRedirectHandling().build()) {
            HttpGet get = new HttpGet(url);
            get.addHeader("If-None-Match", etag);

            CloseableHttpResponse response = httpClient.execute(get);

            MatcherAssert.assertThat(response.getStatusLine().getStatusCode(), CoreMatchers.equalTo(304));
        }
    }

    private void assertNoRedirect(String url) throws IOException {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().disableRedirectHandling().build()) {
            HttpGet get = new HttpGet(url);
            CloseableHttpResponse response = httpClient.execute(get);

            MatcherAssert.assertThat(response.getStatusLine().getStatusCode(), CoreMatchers.equalTo(200));
        }
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

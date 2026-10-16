package org.keycloak.tests.theme;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.keycloak.common.Version;
import org.keycloak.services.resources.KeycloakApplication;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.server.KeycloakUrls;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = ThemeResourceTest.GzipCacheServerConfig.class)
public class ThemeResourceTest {

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @Test
    public void gzipCacheFailureFallsBackToOriginalResource() throws IOException {
        String resourcesVersion = runOnServer.fetch(session -> Version.RESOURCES_VERSION, String.class);
        String url = keycloakUrls.getBase() + "/resources/" + resourcesVersion + "/login/keycloak.v2/css/styles.css";
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().disableContentCompression().build()) {
            String expected;
            try (CloseableHttpResponse response = httpClient.execute(new HttpGet(url))) {
                assertEquals(200, response.getStatusLine().getStatusCode());
                expected = EntityUtils.toString(response.getEntity());
            }

            HttpGet get = new HttpGet(url);
            get.addHeader("Accept-Encoding", "gzip");
            try (CloseableHttpResponse response = httpClient.execute(get)) {
                assertEquals(200, response.getStatusLine().getStatusCode());
                assertEquals("gzip", response.getFirstHeader("Content-Encoding").getValue());
                EntityUtils.consume(response.getEntity());
            }

            runOnServer.run(session -> {
                Path cachedFile = Path.of(KeycloakApplication.getTmpDirectory()).resolve("kc-gzip-cache")
                        .resolve(Version.RESOURCES_VERSION).resolve("login/keycloak.v2/css/styles.css.gz");
                Files.delete(cachedFile);
                // A directory at the cache path forces the cached-file open to fail
                Files.createDirectory(cachedFile);
            });
            try (CloseableHttpResponse response = httpClient.execute(get)) {
                assertEquals(200, response.getStatusLine().getStatusCode());
                assertNull(response.getFirstHeader("Content-Encoding"));
                assertTrue(response.getEntity().getContentType().getValue().startsWith("text/css"));
                assertEquals(expected, EntityUtils.toString(response.getEntity()));
            } finally {
                runOnServer.run(session -> Files.deleteIfExists(Path.of(KeycloakApplication.getTmpDirectory())
                        .resolve("kc-gzip-cache").resolve(Version.RESOURCES_VERSION).resolve("login/keycloak.v2/css/styles.css.gz")));
            }
        }
    }

    public static class GzipCacheServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            // The test framework uses start-dev, which disables theme caching by default
            return config.option("spi-theme--cache-themes", "true");
        }
    }
}

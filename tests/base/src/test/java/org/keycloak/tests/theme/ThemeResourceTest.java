package org.keycloak.tests.theme;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;

import org.keycloak.common.Version;
import org.keycloak.encoding.GzipResourceEncodingProvider;
import org.keycloak.encoding.ResourceEncodingProvider;
import org.keycloak.services.resources.KeycloakApplication;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.theme.Theme;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Assertions;
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
        String cachedResourcePath = "login/keycloak.v2/css/styles.css.gz";
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
                Path cachedFile = findGzipCacheFile(resourcesVersion, cachedResourcePath);
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
                runOnServer.run(session -> {
                    Path cachedFile = findGzipCacheFile(resourcesVersion, cachedResourcePath);
                    if (cachedFile != null) {
                        Files.deleteIfExists(cachedFile);
                    }
                });
            }
        }
    }

    // the directory name has a generation suffix, so match by prefix instead of an exact name
    private static Path findGzipCacheFile(String resourcesVersion, String relativePath) {
        Path cacheRoot = Path.of(KeycloakApplication.getTmpDirectory()).resolve("kc-gzip-cache");
        try (var dirs = Files.list(cacheRoot)) {
            return dirs.filter(dir -> dir.getFileName().toString().startsWith(resourcesVersion))
                    .map(dir -> dir.resolve(relativePath))
                    .filter(Files::exists)
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static class GzipCacheServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            // The test framework uses start-dev, which disables theme caching by default
            return config.option("spi-theme--cache-themes", "true");
        }
    }

    @Test
    public void getResourceDoesNotCreateGzipCacheEntryForBogusTheme() throws IOException {
        String resourcesVersion = runOnServer.fetch(session -> Version.RESOURCES_VERSION, String.class);
        String bogusTheme1 = "bogus-theme-1";
        String bogusTheme2 = "bogus-theme-2";
        String resourcePath = "css/styles.css";

        try (CloseableHttpClient httpClient = HttpClientBuilder.create().disableContentCompression().build()) {
            // Request with bogus theme 1 — should resolve to the default theme and serve content
            String url1 = keycloakUrls.getBase() + "/resources/" + resourcesVersion + "/login/" + bogusTheme1 + "/" + resourcePath;
            HttpGet get1 = new HttpGet(url1);
            get1.addHeader("Accept-Encoding", "gzip");
            try (CloseableHttpResponse response = httpClient.execute(get1)) {
                Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
                Assertions.assertNotNull(response.getFirstHeader("Content-Encoding"));
                Assertions.assertEquals("gzip", response.getFirstHeader("Content-Encoding").getValue());
                try (InputStream is = response.getEntity().getContent();
                     GZIPInputStream gzip = new GZIPInputStream(is)) {
                    String content = IOUtils.toString(gzip, StandardCharsets.UTF_8);
                    Assertions.assertFalse(content.isEmpty());
                }
            }

            // Request with bogus theme 2 — should resolve to the same default theme, not create a second cache entry
            String url2 = keycloakUrls.getBase() + "/resources/" + resourcesVersion + "/login/" + bogusTheme2 + "/" + resourcePath;
            HttpGet get2 = new HttpGet(url2);
            get2.addHeader("Accept-Encoding", "gzip");
            try (CloseableHttpResponse response = httpClient.execute(get2)) {
                Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
                Assertions.assertNotNull(response.getFirstHeader("Content-Encoding"));
                Assertions.assertEquals("gzip", response.getFirstHeader("Content-Encoding").getValue());
                try (InputStream is = response.getEntity().getContent();
                     GZIPInputStream gzip = new GZIPInputStream(is)) {
                    String content = IOUtils.toString(gzip, StandardCharsets.UTF_8);
                    Assertions.assertFalse(content.isEmpty());
                }
            }
        }

        runOnServer.run(session -> {
            GzipResourceEncodingProvider encodingProvider = (GzipResourceEncodingProvider) session.getProvider(ResourceEncodingProvider.class, "gzip");

            // The single entry must contain the cached resource file
            Theme theme = session.theme().getTheme(bogusTheme1, Theme.Type.LOGIN);
            File resolvedCacheFile = encodingProvider.getResourceFile(theme.getType().toString().toLowerCase(), theme.getName(), resourcePath);
            Assertions.assertNotNull(resolvedCacheFile);
            Assertions.assertTrue(resolvedCacheFile.isFile(),
                    "Cache file should exist under the resolved default theme: " + resolvedCacheFile.getPath());

            // Neither request should create a directory under its bogus theme name.
            File loginCacheDir = resolvedCacheFile.getParentFile();
            while (!loginCacheDir.getName().equals("login")) {
                loginCacheDir = loginCacheDir.getParentFile();
            }
            Assertions.assertFalse(new File(loginCacheDir, bogusTheme1).exists(),
                    "The first bogus theme must not have a cache directory");
            Assertions.assertFalse(new File(loginCacheDir, bogusTheme2).exists(),
                    "The second bogus theme must not have a cache directory");

        });
    }
}

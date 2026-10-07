/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.tests.theme;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;

import org.keycloak.common.Version;
import org.keycloak.services.resources.KeycloakApplication;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.theme.Theme;
import org.keycloak.theme.freemarker.FreeMarkerProvider;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that {@code session.theme().clearCache()} also clears the FreeMarker template cache and the gzip
 * resource-encoding cache, not just the theme cache itself.
 */
@KeycloakIntegrationTest(config = ThemeManagerClearCacheTest.ThemeCachingServerConfig.class)
public class ThemeManagerClearCacheTest {

    @InjectRunOnServer(permittedPackages = "org.keycloak.tests.theme")
    RunOnServerClient runOnServer;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @Test
    public void clearCacheRemovesGzipCachedResource() throws IOException {
        String resourcesVersion = runOnServer.fetch(session -> Version.RESOURCES_VERSION, String.class);
        String url = keycloakUrls.getBase() + "/resources/" + resourcesVersion + "/welcome/keycloak/css/welcome.css";

        // the default injected HttpClient auto-decompresses gzip responses and hides the Content-Encoding header,
        // so a client with content compression disabled is needed to observe it
        try (CloseableHttpClient client = HttpClientBuilder.create().disableContentCompression().build()) {
            requestWithGzip(client, url);
            assertTrue(gzipCacheFileExists(resourcesVersion), "expected the gzip cache file to exist after the request");

            // clearCache swaps to a new directory; the previous one is retained briefly for in-flight requests
            runOnServer.run(session -> session.theme().clearCache());
            // a second clearCache cleans up the previous generation's directory
            runOnServer.run(session -> session.theme().clearCache());

            assertFalse(gzipCacheFileExists(resourcesVersion),
                    "old gzip cache directory should have been removed");

            // subsequent requests still work with gzip encoding
            requestWithGzip(client, url);
        }
    }

    @Test
    public void clearCacheServesUpdatedTemplate() {
        String themeName = "cache-test";
        String templateName = "test-cache.ftl";

        runOnServer.run(session -> {
            try {
                Path themeDir = Paths.get(System.getProperty("kc.home.dir"), "themes", themeName, "login");
                Files.createDirectories(themeDir);
                Files.writeString(themeDir.resolve("theme.properties"), "parent=base\n");
                Files.writeString(themeDir.resolve(templateName), "marker-v1");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        try {
            String v1 = renderTemplate(themeName, templateName);
            assertTrue(v1.contains("marker-v1"), "initial render should contain marker-v1");

            // modify the template on disk
            runOnServer.run(session -> {
                try {
                    Files.writeString(Paths.get(System.getProperty("kc.home.dir"),
                            "themes", themeName, "login", templateName), "marker-v2");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            // without clearing cache, the old cached template is still served
            String stale = renderTemplate(themeName, templateName);
            assertTrue(stale.contains("marker-v1"),
                    "without clearCache, cached template should still serve old content");

            // after clearing cache, the updated template is picked up
            runOnServer.run(session -> session.theme().clearCache());

            String v2 = renderTemplate(themeName, templateName);
            assertTrue(v2.contains("marker-v2"),
                    "after clearCache, updated template content should be served");
        } finally {
            runOnServer.run(session -> {
                try {
                    Path themeRoot = Paths.get(System.getProperty("kc.home.dir"), "themes", themeName);
                    if (Files.exists(themeRoot)) {
                        try(var w = Files.walk(themeRoot)) {
                            w.sorted(Comparator.reverseOrder())
                                    .forEach(p -> { try { Files.delete(p); } catch (IOException ignored) {} });
                        }
                    }
                } catch (IOException ignored) {}
            });
            runOnServer.run(session -> session.theme().clearCache());
        }
    }

    private void requestWithGzip(CloseableHttpClient client, String url) throws IOException {
        HttpGet get = new HttpGet(url);
        get.addHeader("Accept-Encoding", "gzip");
        try (CloseableHttpResponse response = client.execute(get)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            assertEquals("gzip", response.getFirstHeader("Content-Encoding").getValue());
        }
    }

    private boolean gzipCacheFileExists(String resourcesVersion) {
        return runOnServer.fetch(session -> {
            java.io.File cacheRoot = Paths.get(KeycloakApplication.getTmpDirectory(), "kc-gzip-cache").toFile();
            if (!cacheRoot.isDirectory()) return false;
            java.io.File[] dirs = cacheRoot.listFiles();
            if (dirs == null) return false;
            for (java.io.File dir : dirs) {
                if (dir.getName().startsWith(resourcesVersion)
                        && new java.io.File(dir, "welcome/keycloak/css/welcome.css.gz").isFile()) {
                    return true;
                }
            }
            return false;
        }, Boolean.class);
    }

    private String renderTemplate(String themeName, String templateName) {
        return runOnServer.fetch(session -> {
            try {
                Theme theme = session.theme().getTheme(themeName, Theme.Type.LOGIN);
                return session.getProvider(FreeMarkerProvider.class)
                        .processTemplate(new HashMap<>(), templateName, theme);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, String.class);
    }

    public static class ThemeCachingServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            // theme and template caching are disabled by default in dev mode, but this fix only matters when caching
            // is enabled, so override that for this test
            return config.option("spi-theme--cache-themes", "true")
                    .option("spi-theme--cache-templates", "true");
        }
    }

}

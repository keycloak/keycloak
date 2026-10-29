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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;

import org.keycloak.common.Version;
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
    public void clearCacheServesUpdatedGzipResource() throws IOException {
        String themeName = "gzip-cache-test";
        String resourcesVersion = runOnServer.fetch(session -> Version.RESOURCES_VERSION, String.class);
        String url = keycloakUrls.getBase() + "/resources/" + resourcesVersion + "/login/" + themeName + "/css/test-cache.css";

        runOnServer.run(session -> {
            try {
                Path themeDir = Paths.get(System.getProperty("kc.home.dir"), "themes", themeName, "login");
                Files.createDirectories(themeDir.resolve("resources/css"));
                Files.writeString(themeDir.resolve("theme.properties"), "parent=base\n");
                Files.writeString(themeDir.resolve("resources/css/test-cache.css"), "marker-v1");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        // the default injected HttpClient auto-decompresses gzip responses and hides the Content-Encoding header,
        // so a client with content compression disabled is needed to observe it
        try (CloseableHttpClient client = HttpClientBuilder.create().disableContentCompression().build()) {
            try {
                String v1 = requestGzipContent(client, url);
                assertTrue(v1.contains("marker-v1"), "initial response should contain marker-v1");

                // modify the resource on disk
                runOnServer.run(session -> {
                    try {
                        Files.writeString(Paths.get(System.getProperty("kc.home.dir"),
                                "themes", themeName, "login", "resources", "css", "test-cache.css"), "marker-v2");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

                // without clearing cache, the stale gzip-cached response is still served
                String stale = requestGzipContent(client, url);
                assertTrue(stale.contains("marker-v1"),
                        "without clearCache, cached gzip response should still serve old content");

                // a single clearCache() call must be enough to make the updated content visible
                runOnServer.run(session -> session.theme().clearCache());

                String v2 = requestGzipContent(client, url);
                assertTrue(v2.contains("marker-v2"),
                        "after a single clearCache call, updated content should be served");
            } finally {
                deleteThemeDir(themeName);
                runOnServer.run(session -> session.theme().clearCache());
            }
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
            deleteThemeDir(themeName);
            runOnServer.run(session -> session.theme().clearCache());
        }
    }

    private String requestGzipContent(CloseableHttpClient client, String url) throws IOException {
        HttpGet get = new HttpGet(url);
        get.addHeader("Accept-Encoding", "gzip");
        try (CloseableHttpResponse response = client.execute(get)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            assertEquals("gzip", response.getFirstHeader("Content-Encoding").getValue());
            try (GZIPInputStream gis = new GZIPInputStream(response.getEntity().getContent())) {
                return new String(gis.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
    }

    private void deleteThemeDir(String themeName) {
        runOnServer.run(session -> {
            try {
                Path themeRoot = Paths.get(System.getProperty("kc.home.dir"), "themes", themeName);
                if (Files.exists(themeRoot)) {
                    try (var w = Files.walk(themeRoot)) {
                        w.sorted(Comparator.reverseOrder())
                                .forEach(p -> { try { Files.delete(p); } catch (IOException ignored) {} });
                    }
                }
            } catch (IOException ignored) {}
        });
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

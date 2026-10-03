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
import java.lang.reflect.Field;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.keycloak.common.Version;
import org.keycloak.services.resources.KeycloakApplication;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.theme.FreeMarkerException;
import org.keycloak.theme.Theme;
import org.keycloak.theme.freemarker.DefaultFreeMarkerProviderFactory;
import org.keycloak.theme.freemarker.FreeMarkerProvider;
import org.keycloak.theme.freemarker.FreeMarkerProviderFactory;

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
 * resource-encoding cache, not just the theme cache itself
 * (https://github.com/keycloak/keycloak/issues/51066).
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

            runOnServer.run(session -> session.theme().clearCache());

            assertFalse(gzipCacheFileExists(resourcesVersion), "clearCache() should have removed the gzip cache file");

            // a subsequent request still works, and repopulates the cache
            requestWithGzip(client, url);
            assertTrue(gzipCacheFileExists(resourcesVersion));
        }
    }

    @Test
    public void clearCacheRemovesCachedTemplate() {
        runOnServer.run(session -> {
            Theme theme = session.theme().getTheme("base", Theme.Type.LOGIN);
            try {
                // the template is compiled and cached before rendering, so an incomplete data map is enough
                session.getProvider(FreeMarkerProvider.class).processTemplate(new HashMap<>(), "code.ftl", theme);
            } catch (FreeMarkerException e) {
                // expected: the data map above does not provide everything the template needs to fully render
            }
        });

        assertTrue(freeMarkerCacheContainsCodeTemplate(), "expected the template to be cached after rendering it");

        runOnServer.run(session -> session.theme().clearCache());

        assertFalse(freeMarkerCacheContainsCodeTemplate(), "clearCache() should have removed the cached template");
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
        return runOnServer.fetch(session -> Paths.get(KeycloakApplication.getTmpDirectory(), "kc-gzip-cache", resourcesVersion,
                "welcome", "keycloak", "css", "welcome.css.gz").toFile().isFile(), Boolean.class);
    }

    private boolean freeMarkerCacheContainsCodeTemplate() {
        return runOnServer.fetch(session -> {
            try {
                FreeMarkerProviderFactory factory = (FreeMarkerProviderFactory)
                        session.getKeycloakSessionFactory().getProviderFactory(FreeMarkerProvider.class);
                Field field = DefaultFreeMarkerProviderFactory.class.getDeclaredField("cache");
                field.setAccessible(true);
                Map<?, ?> cache = (Map<?, ?>) field.get(factory);
                return cache != null && cache.containsKey("login/base/code.ftl");
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }, Boolean.class);
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

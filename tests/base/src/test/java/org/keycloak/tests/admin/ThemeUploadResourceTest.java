/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.tests.admin;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.info.ThemeInfoRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.theme.FolderThemeProvider;
import org.keycloak.theme.ThemeProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration tests for {@code POST /admin/realms/{realm}/themes} (ThemeUploadResource).
 *
 * Tests that require a configured themes directory (kc.home.dir) are skipped automatically
 * when the server is not started with that option.
 *
 * To run the full suite locally:
 *   ../mvnw -f server/pom.xml compile quarkus:dev -Dkc.config.built=true \
 *           -Dquarkus.args="start-dev" -Dkc.home.dir=.kc
 */
@KeycloakIntegrationTest
public class ThemeUploadResourceTest {

    private static final String THEME_NAME = "test-upload-theme";
    private static final String BOUNDARY = "ThemeUploadTestBoundary";

    @InjectRealm
    ManagedRealm realm;

    @InjectAdminClient
    Keycloak adminClient;

    @InjectHttpClient
    CloseableHttpClient httpClient;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    // -------------------------------------------------------------------------
    // Success cases
    // -------------------------------------------------------------------------

    @Test
    public void testUploadTheme_validZip_returns204() throws IOException {
        assumeThemesDirConfigured();

        byte[] zip = buildThemeZip(THEME_NAME);
        try (CloseableHttpResponse response = httpClient.execute(multipartPost(zip))) {
            assertEquals(204, response.getStatusLine().getStatusCode(),
                    "Valid theme ZIP should return 204 No Content");
        } finally {
            deleteUploadedTheme(THEME_NAME);
        }
    }

    @Test
    public void testUploadTheme_appearsInServerInfo() throws IOException {
        assumeThemesDirConfigured();

        byte[] zip = buildThemeZip(THEME_NAME);
        try (CloseableHttpResponse response = httpClient.execute(multipartPost(zip))) {
            assertEquals(204, response.getStatusLine().getStatusCode());
        }

        try {
            Map<String, List<ThemeInfoRepresentation>> themes =
                    adminClient.serverInfo().getInfo().getThemes();

            // Theme with login sub-dir should appear under login, account, admin, email
            assertThemePresent(themes, "login", THEME_NAME);
            assertThemePresent(themes, "account", THEME_NAME);
            assertThemePresent(themes, "admin", THEME_NAME);
            assertThemePresent(themes, "email", THEME_NAME);
        } finally {
            deleteUploadedTheme(THEME_NAME);
        }
    }

    // -------------------------------------------------------------------------
    // Error cases — do not require themes dir
    // -------------------------------------------------------------------------

    @Test
    public void testUploadTheme_noFilePart_returns400() throws IOException {
        // POST with an empty multipart body (missing "file" part)
        HttpPost post = new HttpPost(themeUploadUrl());
        post.setHeader("Authorization", "Bearer " + token());
        post.setHeader("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);
        String emptyMultipart = "--" + BOUNDARY + "--\r\n";
        post.setEntity(new ByteArrayEntity(emptyMultipart.getBytes(StandardCharsets.UTF_8)));

        try (CloseableHttpResponse response = httpClient.execute(post)) {
            assertEquals(400, response.getStatusLine().getStatusCode(),
                    "Missing file part should return 400 Bad Request");
        }
    }

    @Test
    public void testUploadTheme_unauthenticated_returns401() throws IOException {
        // POST without an Authorization header
        HttpPost post = new HttpPost(themeUploadUrl());
        post.setHeader("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);
        post.setEntity(new ByteArrayEntity(buildMultipartBody(buildThemeZip(THEME_NAME), "theme.zip")));

        try (CloseableHttpResponse response = httpClient.execute(post)) {
            assertEquals(401, response.getStatusLine().getStatusCode(),
                    "Unauthenticated request should return 401 Unauthorized");
        }
    }

    // -------------------------------------------------------------------------
    // Security / error cases — require themes dir (need to reach extraction code)
    // -------------------------------------------------------------------------

    @Test
    public void testUploadTheme_zipSlipAttack_returns400() throws IOException {
        assumeThemesDirConfigured();

        byte[] maliciousZip = buildZipSlipZip();
        try (CloseableHttpResponse response = httpClient.execute(multipartPost(maliciousZip))) {
            assertEquals(400, response.getStatusLine().getStatusCode(),
                    "ZIP with path traversal should be rejected with 400");
        }
    }

    @Test
    public void testUploadTheme_invalidContent_returns400() throws IOException {
        assumeThemesDirConfigured();

        // Send plain text as the file — not a valid ZIP
        byte[] notAZip = "this is not a zip file".getBytes(StandardCharsets.UTF_8);
        try (CloseableHttpResponse response = httpClient.execute(multipartPost(notAZip))) {
            assertEquals(400, response.getStatusLine().getStatusCode(),
                    "Non-ZIP content should return 400 Bad Request");
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String themeUploadUrl() {
        return keycloakUrls.getBase() + "/admin/realms/" + realm.getName() + "/themes";
    }

    private String token() {
        return adminClient.tokenManager().getAccessTokenString();
    }

    private HttpPost multipartPost(byte[] zipBytes) {
        HttpPost post = new HttpPost(themeUploadUrl());
        post.setHeader("Authorization", "Bearer " + token());
        post.setHeader("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);
        post.setEntity(new ByteArrayEntity(buildMultipartBody(zipBytes, "theme.zip")));
        return post;
    }

    /** Builds a raw multipart/form-data body with a single "file" part. */
    private byte[] buildMultipartBody(byte[] fileContent, String fileName) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            String partHeader = "--" + BOUNDARY + "\r\n"
                    + "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n"
                    + "Content-Type: application/octet-stream\r\n\r\n";
            out.write(partHeader.getBytes(StandardCharsets.UTF_8));
            out.write(fileContent);
            out.write(("\r\n--" + BOUNDARY + "--\r\n").getBytes(StandardCharsets.UTF_8));
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Builds a minimal valid theme ZIP with all four type subdirectories.
     * Structure: {themeName}/{login,account,admin,email}/theme.properties
     */
    private byte[] buildThemeZip(String themeName) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            addZipEntry(zip, themeName + "/login/theme.properties", "parent=keycloak\n");
            addZipEntry(zip, themeName + "/account/theme.properties", "parent=keycloak.v2\n");
            addZipEntry(zip, themeName + "/admin/theme.properties", "parent=keycloak.v2\n");
            addZipEntry(zip, themeName + "/email/theme.properties", "parent=base\n");
        }
        return out.toByteArray();
    }

    /** Builds a ZIP with a path-traversal entry (zip slip attack). */
    private byte[] buildZipSlipZip() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            addZipEntry(zip, "../../evil/theme.properties", "parent=keycloak\n");
        }
        return out.toByteArray();
    }

    private void addZipEntry(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    /**
     * Skips the test if the server's FolderThemeProvider has no configured themes directory
     * (i.e. the server was not started with kc.home.dir).
     */
    private void assumeThemesDirConfigured() {
        String path = runOnServer.fetchString(session -> {
            for (ThemeProvider p : session.getAllProviders(ThemeProvider.class)) {
                if (p instanceof FolderThemeProvider folder) {
                    File dir = folder.getThemesDir();
                    return dir != null ? dir.getAbsolutePath() : "";
                }
            }
            return "";
        });
        assumeTrue(path != null && !path.isEmpty(),
                "Skipped: server not started with kc.home.dir — themes directory unavailable");
    }

    /**
     * Deletes the uploaded theme directory from the server's themes folder and clears the cache.
     */
    private void deleteUploadedTheme(String themeName) {
        runOnServer.run(session -> {
            for (ThemeProvider p : session.getAllProviders(ThemeProvider.class)) {
                if (p instanceof FolderThemeProvider folder) {
                    File dir = folder.getThemesDir();
                    if (dir != null) {
                        File themeDir = new File(dir, themeName);
                        if (themeDir.exists()) {
                            try {
                                Files.walk(themeDir.toPath())
                                        .sorted(Comparator.reverseOrder())
                                        .map(java.nio.file.Path::toFile)
                                        .forEach(File::delete);
                            } catch (IOException e) {
                                throw new RuntimeException("Failed to delete theme dir", e);
                            }
                        }
                    }
                    session.theme().clearCache();
                    break;
                }
            }
        });
    }

    private void assertThemePresent(Map<String, List<ThemeInfoRepresentation>> themes,
                                    String type, String name) {
        List<ThemeInfoRepresentation> list = themes.get(type);
        assertTrue(list != null && list.stream().anyMatch(t -> name.equals(t.getName())),
                "Theme '" + name + "' should appear under server-info themes." + type);
    }
}

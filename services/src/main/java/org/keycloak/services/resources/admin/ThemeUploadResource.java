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
package org.keycloak.services.resources.admin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.keycloak.http.FormPartValue;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.theme.FolderThemeProvider;
import org.keycloak.theme.Theme;
import org.keycloak.theme.ThemeProvider;

public class ThemeUploadResource {

    private final KeycloakSession session;
    private final AdminPermissionEvaluator auth;

    public ThemeUploadResource(KeycloakSession session, AdminPermissionEvaluator auth) {
        this.session = session;
        this.auth = auth;
    }

    // -------------------------------------------------------------------------
    // List custom (folder-based) themes
    // -------------------------------------------------------------------------

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.REALMS_ADMIN)
    @Operation(summary = "List custom themes uploaded via the admin API")
    public List<CustomThemeInfo> listCustomThemes() {
        auth.realm().requireManageRealm();

        File themesDir = getThemesDir();
        if (themesDir == null || !themesDir.isDirectory()) {
            return Collections.emptyList();
        }

        File[] entries = themesDir.listFiles(File::isDirectory);
        if (entries == null) return Collections.emptyList();

        List<CustomThemeInfo> result = new ArrayList<>();
        for (File themeDir : entries) {
            List<String> types = new ArrayList<>();
            for (Theme.Type type : Theme.Type.values()) {
                File typeDir = new File(themeDir, type.name().toLowerCase());
                if (typeDir.isDirectory()) {
                    types.add(type.name().toLowerCase());
                }
            }
            if (!types.isEmpty()) {
                result.add(new CustomThemeInfo(themeDir.getName(), types));
            }
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Upload a theme ZIP
    // -------------------------------------------------------------------------

    /**
     * Upload a theme ZIP. The ZIP must contain a folder named after the theme:
     * <pre>
     *   my-theme/
     *     login/theme.properties
     *     account/theme.properties
     * </pre>
     */
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.REALMS_ADMIN)
    @Operation(summary = "Upload a theme ZIP archive")
    public Response uploadTheme() {
        auth.realm().requireManageRealm();

        MultivaluedMap<String, FormPartValue> formDataMap =
                session.getContext().getHttpRequest().getMultiPartFormParameters();
        if (!formDataMap.containsKey("file")) {
            throw new BadRequestException("No file provided");
        }

        File themesDir = getThemesDir();
        if (themesDir == null) {
            throw new BadRequestException(
                    "Theme directory is not configured. Start the server with --spi-theme-folder-dir=<path> or set kc.home.dir.");
        }
        if (!themesDir.isDirectory() && !themesDir.mkdirs()) {
            throw new BadRequestException(
                    "Cannot create theme directory: " + themesDir.getAbsolutePath());
        }

        try (InputStream inputStream = formDataMap.getFirst("file").asInputStream();
             ZipInputStream zipStream = new ZipInputStream(inputStream)) {
            extractZip(zipStream, themesDir.toPath());
        } catch (BadRequestException e) {
            throw e;
        } catch (IOException e) {
            throw new BadRequestException("Failed to process theme archive.");
        }

        session.theme().clearCache();
        return Response.noContent().build();
    }

    // -------------------------------------------------------------------------
    // Delete a custom theme
    // -------------------------------------------------------------------------

    @DELETE
    @jakarta.ws.rs.Path("{themeName}")
    @Tag(name = KeycloakOpenAPI.Admin.Tags.REALMS_ADMIN)
    @Operation(summary = "Delete a custom theme by name")
    public Response deleteTheme(@PathParam("themeName") String themeName) {
        auth.realm().requireManageRealm();

        File themesDir = getThemesDir();
        if (themesDir == null || !themesDir.isDirectory()) {
            throw new NotFoundException("Theme directory not configured");
        }

        // Resolve and normalise to prevent path traversal
        Path resolved = themesDir.toPath().resolve(themeName).normalize();
        if (!resolved.startsWith(themesDir.toPath())) {
            throw new BadRequestException("Invalid theme name");
        }

        File themeDir = resolved.toFile();
        if (!themeDir.isDirectory()) {
            throw new NotFoundException("Theme '" + themeName + "' not found");
        }

        try {
            deleteDirectory(themeDir.toPath());
        } catch (IOException e) {
            throw new InternalServerErrorException("Failed to delete theme: " + e.getMessage());
        }

        // If the realm was using this theme, reset those settings to avoid broken references
        resetRealmThemeIfUsing(themeName);

        session.theme().clearCache();
        return Response.noContent().build();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void extractZip(ZipInputStream zipStream, Path destDir) throws IOException {
        String themeFolderName = null;
        ZipEntry entry;

        while ((entry = zipStream.getNextEntry()) != null) {
            String name = entry.getName();

            // Silently skip OS-generated metadata (.DS_Store, __MACOSX/, Thumbs.db)
            if (isMetadataEntry(name)) {
                zipStream.closeEntry();
                continue;
            }

            // Every valid entry must live inside a top-level folder.
            // No slash = stray top-level file. Slash at position 0 = absolute path.
            // Both are rejected.
            int slash = name.indexOf('/');
            if (slash <= 0) {
                throw new BadRequestException(
                        "Invalid theme archive: '" + name + "' is a stray top-level file. "
                                + "The ZIP must contain exactly one folder named after the theme.");
            }

            String topLevel = name.substring(0, slash);
            if (themeFolderName == null) {
                themeFolderName = topLevel;
            } else if (!themeFolderName.equals(topLevel)) {
                throw new BadRequestException(
                        "Invalid theme archive: multiple top-level folders found ('"
                                + themeFolderName + "' and '" + topLevel + "'). "
                                + "The ZIP must contain exactly one folder named after the theme.");
            }

            // Zip-slip protection
            Path entryPath = destDir.resolve(name).normalize();
            if (!entryPath.startsWith(destDir)) {
                throw new BadRequestException("Invalid theme archive: path traversal detected.");
            }

            if (entry.isDirectory()) {
                Files.createDirectories(entryPath);
            } else {
                Files.createDirectories(entryPath.getParent());
                Files.copy(zipStream, entryPath, StandardCopyOption.REPLACE_EXISTING);
            }
            zipStream.closeEntry();
        }

        if (themeFolderName == null) {
            throw new BadRequestException(
                    "Invalid theme archive: no valid theme entries found after skipping metadata.");
        }
    }

    private static boolean isMetadataEntry(String name) {
        return name.startsWith("__MACOSX/")
                || name.equals(".DS_Store")
                || name.endsWith("/.DS_Store")
                || name.equals("Thumbs.db")
                || name.endsWith("/Thumbs.db");
    }

    private static void deleteDirectory(Path dir) throws IOException {
        try (var stream = Files.walk(dir)) {
            List<Path> paths = stream.sorted(Comparator.reverseOrder()).toList();
            for (Path path : paths) {
                Files.delete(path);
            }
        }
    }

    private void resetRealmThemeIfUsing(String themeName) {
        RealmModel realm = session.getContext().getRealm();
        if (themeName.equals(realm.getLoginTheme()))   realm.setLoginTheme(null);
        if (themeName.equals(realm.getAccountTheme())) realm.setAccountTheme(null);
        if (themeName.equals(realm.getAdminTheme()))   realm.setAdminTheme(null);
        if (themeName.equals(realm.getEmailTheme()))   realm.setEmailTheme(null);
    }

    private File getThemesDir() {
        for (ThemeProvider provider : session.getAllProviders(ThemeProvider.class)) {
            if (provider instanceof FolderThemeProvider) {
                return ((FolderThemeProvider) provider).getThemesDir();
            }
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Response representation
    // -------------------------------------------------------------------------

    public static class CustomThemeInfo {
        private String name;
        private List<String> types;

        public CustomThemeInfo() {}

        public CustomThemeInfo(String name, List<String> types) {
            this.name = name;
            this.types = types;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public List<String> getTypes() { return types; }
        public void setTypes(List<String> types) { this.types = types; }
    }
}

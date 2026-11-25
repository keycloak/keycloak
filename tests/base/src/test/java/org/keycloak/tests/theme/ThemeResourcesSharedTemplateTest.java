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
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.theme.Theme;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression coverage for shared {@code theme-resources.ftl} resolution via ThemeResourceProvider
 * (see #55514). Built-in themes no longer ship per-type copies of that template.
 */
@KeycloakIntegrationTest
public class ThemeResourcesSharedTemplateTest {

    private static final String TEMPLATE = "theme-resources.ftl";
    private static final String SHARED_PATH = "theme-resources/templates/" + TEMPLATE;

    @InjectRunOnServer(permittedPackages = "org.keycloak.tests.theme")
    RunOnServerClient runOnServer;

    @Test
    public void sharedTemplateResolvedForBuiltinThemeTypes() {
        runOnServer.run(session -> {
            try {
                assertSharedTemplate(session.theme().getTheme("base", Theme.Type.LOGIN));
                assertSharedTemplate(session.theme().getTheme("keycloak.v2", Theme.Type.ADMIN));
                assertSharedTemplate(session.theme().getTheme("keycloak.v3", Theme.Type.ACCOUNT));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void customThemeTemplateTakesPrecedenceOverShared() {
        String themeName = "theme-resources-override";
        String marker = "<#-- custom-theme-resources-override -->";

        runOnServer.run(session -> {
            try {
                Path themeDir = Paths.get(System.getProperty("kc.home.dir"), "themes", themeName, "login");
                Files.createDirectories(themeDir);
                Files.writeString(themeDir.resolve("theme.properties"), "parent=base\n");
                Files.writeString(themeDir.resolve(TEMPLATE), marker + "\n");
                session.theme().clearCache();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        try {
            runOnServer.run(session -> {
                try {
                    Theme theme = session.theme().getTheme(themeName, Theme.Type.LOGIN);
                    URL template = theme.getTemplate(TEMPLATE);
                    assertNotNull(template, "custom theme should resolve " + TEMPLATE);
                    assertTrue(template.toString().contains("/themes/" + themeName + "/login/" + TEMPLATE)
                                    || template.toString().endsWith(themeName + "/login/" + TEMPLATE),
                            "expected theme-local template, got: " + template);
                    try (InputStream in = template.openStream()) {
                        String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                        assertTrue(content.contains(marker), "custom theme template content should win over shared copy");
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } finally {
            deleteThemeDir(themeName);
            runOnServer.run(session -> session.theme().clearCache());
        }
    }

    private static void assertSharedTemplate(Theme theme) throws IOException {
        URL template = theme.getTemplate(TEMPLATE);
        assertNotNull(template, theme.getType() + "/" + theme.getName() + " should resolve " + TEMPLATE);
        assertTrue(template.toString().contains(SHARED_PATH),
                theme.getType() + "/" + theme.getName() + " should use shared " + SHARED_PATH + ", got: " + template);
    }

    private void deleteThemeDir(String themeName) {
        runOnServer.run(session -> {
            try {
                Path themeRoot = Paths.get(System.getProperty("kc.home.dir"), "themes", themeName);
                if (Files.exists(themeRoot)) {
                    try (var walk = Files.walk(themeRoot)) {
                        walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException ignored) {
                            }
                        });
                    }
                }
            } catch (IOException ignored) {
            }
        });
    }
}

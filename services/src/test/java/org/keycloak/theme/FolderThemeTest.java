/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.theme;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Locale;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.keycloak.theme.Theme.Type.LOGIN;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class FolderThemeTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static final String NONE = "../";

    private final Set<String> someValidISOLocaleCodes = Set.of("de", "de-DE", "de-x-informal");

    @Test
    public void testLocaleWithExtension() throws Exception {
        FolderTheme theme = new FolderTheme(new File("."), "name", LOGIN);

        for (String localeCode : someValidISOLocaleCodes) {
            Locale locale = Locale.forLanguageTag(localeCode);
            assertNotNull(theme.getMessages("base", locale));
        }
    }

    @Test
    public void testGetTemplatePathContainment() throws Exception {
        File tempDirectory = temporaryFolder.newFolder();
        File themeDir = new File(tempDirectory, "login");
        assertTrue("Failed to create theme directory: " + themeDir, themeDir.mkdirs());
        assertTrue("Failed to create theme subdirectory", new File(themeDir, "one").mkdir());

        Files.createFile(themeDir.toPath().resolve("login.ftl"));
        Files.createFile(tempDirectory.toPath().resolve("forbidden.ftl"));

        FolderTheme theme = new FolderTheme(themeDir, "name", LOGIN);

        assertGetTemplate(theme, themeDir, "login.ftl", true);
        assertGetTemplate(theme, themeDir, NONE + "forbidden.ftl", false);
        assertGetTemplate(theme, themeDir, "one/" + NONE + NONE + "forbidden.ftl", false);
    }

    private void assertGetTemplate(FolderTheme theme, File themeDir, String name, boolean expectValid) throws IOException {
        URL template = theme.getTemplate(name);
        if (expectValid) {
            assertNotNull(template);
        } else {
            assertNull(template);
        }

        assertTrue(new File(themeDir, name).getCanonicalFile().isFile());
    }
}

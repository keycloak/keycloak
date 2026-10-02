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

package org.keycloak.theme;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;

import org.keycloak.theme.freemarker.DefaultFreeMarkerProvider;
import org.keycloak.theme.freemarker.DefaultFreeMarkerProviderFactory;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;

// See https://github.com/keycloak/keycloak/issues/51066
public class DefaultThemeManagerFactoryTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void clearCacheRemovesCachedTemplates() throws Exception {
        File themeDir = temporaryFolder.newFolder("login");
        File templateFile = new File(themeDir, "test.ftl");
        Files.write(templateFile.toPath(), "VERSION-ONE".getBytes(StandardCharsets.UTF_8));

        FolderTheme theme = new FolderTheme(themeDir, "test", Theme.Type.LOGIN);
        DefaultFreeMarkerProviderFactory factory = new DefaultFreeMarkerProviderFactory();
        DefaultFreeMarkerProvider provider = factory.create(null);

        assertEquals("VERSION-ONE", provider.processTemplate(new HashMap<>(), "test.ftl", theme));

        // the template is cached, so a change on disk is not picked up yet
        Files.write(templateFile.toPath(), "VERSION-TWO".getBytes(StandardCharsets.UTF_8));
        assertEquals("VERSION-ONE", provider.processTemplate(new HashMap<>(), "test.ftl", theme));

        factory.clearCache();

        assertEquals("VERSION-TWO", provider.processTemplate(new HashMap<>(), "test.ftl", theme));
    }

}

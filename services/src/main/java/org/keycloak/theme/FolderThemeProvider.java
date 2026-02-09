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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class FolderThemeProvider implements ThemeProvider {

    private final File themesDir;

    public FolderThemeProvider(File themesDir) {
        this.themesDir = themesDir;
    }

    @Override
    public int getProviderPriority() {
        return 100;
    }

    @Override
    public Theme getTheme(String name, Theme.Type type) throws IOException {
        File themeDir = getThemeDir(name, type);
        return themeDir != null ? new FolderTheme(themeDir, name, type) : null;
    }

    @Override
    public Set<String> nameSet(Theme.Type type) {
        if (themesDir == null || !themesDir.isDirectory()) {
            return Collections.emptySet();
        }

        File[] themes = themesDir.listFiles(f -> f.isDirectory() && new File(f, type.name().toLowerCase()).isDirectory());
        if (themes != null && themes.length > 0) {
            Set<String> names = new HashSet<>();
            for (File themeDir : themes) {
                names.add(themeDir.getName());
            }
            return names;
        } else {
            return Collections.emptySet();
        }
    }

    @Override
    public boolean hasTheme(String name, Theme.Type type) {
        return getThemeDir(name, type) != null;
    }

    @Override
    public void close() {
    }

    private File getThemeDir(String name, Theme.Type type) {
        if (themesDir == null || !themesDir.isDirectory()) {
            return null;
        }

        File[] themes = themesDir.listFiles(f -> f.isDirectory() && f.getName().equals(name));
        if (themes != null && themes.length == 1) {
            File themeTypeDir = new File(themes[0], type.name().toLowerCase());
            if (themeTypeDir.isDirectory()) {
                return themeTypeDir;
            }
        }
        return null;
    }

}

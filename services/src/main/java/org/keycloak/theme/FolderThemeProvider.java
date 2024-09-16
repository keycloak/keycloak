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
import java.io.FileFilter;
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
        if (themesDir == null) {
            return Collections.emptySet();
        }

        File[] themeDirs = themesDir.listFiles(new ThemeFilter(type));
        if (themeDirs != null) {
            Set<String> names = new HashSet<>();
            for (File themeDir : themeDirs) {
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
        File[] themes = themesDir.listFiles(new ThemeFilter(name, type));
        return themes != null && themes.length == 1 ? themes[0] : null;
    }

    private class ThemeFilter implements FileFilter {

        private final String name;
        private final String type;

        public ThemeFilter(Theme.Type type) {
            this.name = null;
            this.type = type.name().toLowerCase();
        }

        public ThemeFilter(String name, Theme.Type type) {
            this.name = name;
            this.type = type.name().toLowerCase();
        }

        @Override
        public boolean accept(File f) {
            if (!f.isDirectory()) {
                return false;
            }

            if (name != null && !name.equals(f.getName())) {
                return false;
            }

            return new File(f, type).isDirectory();
        }
    }

}

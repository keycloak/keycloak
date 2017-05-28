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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class FolderThemeProvider implements ThemeProvider {

    private final List<File> themesDirs;

    public FolderThemeProvider(List<File> themesDir) {
        this.themesDirs = themesDir;
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
        final String typeName = type.name().toLowerCase();
        List<File> allThemeDirs = new ArrayList();
        for (File dir : themesDirs) {
            File[] themeDirs = dir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return pathname.isDirectory() && new File(pathname, typeName).isDirectory();
                }
            });
            if (themeDirs != null) {
                allThemeDirs.addAll(Arrays.asList(themeDirs));
            }
        }
        if (!allThemeDirs.isEmpty()) {
            Set<String> names = new HashSet<String>();
            for (File themeDir : allThemeDirs) {
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
        for (File dir : themesDirs) {
            File themeDir = new File(dir, name + File.separator + type.name().toLowerCase());
            if (themeDir.isDirectory()) return themeDir;
        }
        
        return null;
    }

}

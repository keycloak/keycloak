package org.keycloak.theme;

import org.keycloak.freemarker.Theme;
import org.keycloak.freemarker.ThemeProvider;

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

    private File rootDir;

    public FolderThemeProvider(File rootDir) {
        this.rootDir = rootDir;
    }

    @Override
    public int getProviderPriority() {
        return 100;
    }

    @Override
    public Theme getTheme(String name, Theme.Type type) throws IOException {
        if (hasTheme(name, type)) {
            return new FolderTheme(new File(getTypeDir(type), name), type);
        }
        return null;
    }

    @Override
    public Set<String> nameSet(Theme.Type type) {
        File typeDir = getTypeDir(type);
        if (typeDir != null) {
            File[] themes = typeDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return pathname.isDirectory();
                }
            });

            Set<String> names = new HashSet<String>();
            for (File t : themes) {
                names.add(t.getName());
            }
            return names;
        }

        return Collections.emptySet();
    }

    private File getTypeDir(Theme.Type type) {
        if (rootDir != null && rootDir.isDirectory()) {
            File typeDir = new File(rootDir, type.name().toLowerCase());
            if (typeDir.isDirectory()) {
                return typeDir;
            }
        }
        return null;
    }

    @Override
    public boolean hasTheme(String name, Theme.Type type) {
        File typeDir = getTypeDir(type);
        return typeDir != null && new File(typeDir, name).isDirectory();
    }

    @Override
    public void close() {
    }

}

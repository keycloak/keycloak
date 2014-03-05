package org.keycloak.theme;

import org.keycloak.freemarker.Theme;
import org.keycloak.freemarker.ThemeLoader;
import org.keycloak.freemarker.ThemeProvider;
import org.keycloak.models.Config;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class DefaultLoginThemeProvider implements ThemeProvider {

    public static final String BASE = "base";
    public static final String PATTERNFLY = "patternfly";
    public static final String KEYCLOAK = "keycloak";

    private static Set<String> defaultThemes = new HashSet<String>();

    static {
        defaultThemes.add(BASE);
        defaultThemes.add(PATTERNFLY);
        defaultThemes.add(KEYCLOAK);
    }

    @Override
    public int getProviderPriority() {
        return 0;
    }

    @Override
    public Theme createTheme(String name, Theme.Type type) throws IOException {
        if (hasTheme(name, type)) {
            return new ClassLoaderTheme(name, type, getClass().getClassLoader());
        } else {
            return null;
        }
    }

    @Override
    public Set<String> nameSet(Theme.Type type) {
        if (type == Theme.Type.LOGIN || type == Theme.Type.ACCOUNT) {
            return defaultThemes;
        } else {
            return Collections.emptySet();
        }
    }

    @Override
    public boolean hasTheme(String name, Theme.Type type) {
        return nameSet(type).contains(name);
    }

}

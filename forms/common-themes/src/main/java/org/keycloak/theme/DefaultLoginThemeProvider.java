package org.keycloak.theme;

import org.keycloak.freemarker.Theme;
import org.keycloak.freemarker.ThemeLoader;
import org.keycloak.freemarker.ThemeProvider;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class DefaultLoginThemeProvider implements ThemeProvider {

    public static final String RCUE = "rcue";
    public static final String PATTERNFLY = "patternfly";
    public static final String KEYCLOAK = "keycloak";

    private static Set<String> defaultLoginThemes = new HashSet<String>();

    static {
        defaultLoginThemes.add(ThemeLoader.BASE);
        defaultLoginThemes.add(PATTERNFLY);
        defaultLoginThemes.add(KEYCLOAK);
    }

    private static Set<String> defaultAccountThemes = new HashSet<String>();

    static {
        defaultAccountThemes.add(ThemeLoader.BASE);
        defaultAccountThemes.add(RCUE);
        defaultAccountThemes.add(KEYCLOAK);
    }

    @Override
    public int getProviderPriority() {
        return 0;
    }

    @Override
    public Theme createTheme(String name, Theme.Type type) throws IOException {
        if (hasTheme(name, type)) {
            return new ClassLoaderTheme(name, type);
        } else {
            return null;
        }
    }

    @Override
    public Set<String> nameSet(Theme.Type type) {
        if (type == Theme.Type.LOGIN) {
            return defaultLoginThemes;
        } else if (type == Theme.Type.ACCOUNT) {
            return defaultAccountThemes;
        } else {
            return Collections.emptySet();
        }
    }

    @Override
    public boolean hasTheme(String name, Theme.Type type) {
        return nameSet(type).contains(name);
    }

}

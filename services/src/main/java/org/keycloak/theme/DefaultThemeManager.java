package org.keycloak.theme;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ThemeManager;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public class DefaultThemeManager implements ThemeManager {

    private KeycloakSession session;

    public DefaultThemeManager(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Theme getTheme(Theme.Type type) throws IOException {
        String name = session.getProvider(ThemeSelectorProvider.class).getThemeName(type);
        return getTheme(name, type);
    }

    @Override
    public Theme getTheme(String name, Theme.Type type) throws IOException {
        return session.getProvider(ThemeProvider.class, "extending").getTheme(name, type);
    }

    @Override
    public Set<String> nameSet(Theme.Type type) {
        ThemeProvider themeProvider = session.getProvider(ThemeProvider.class, "extending");
        return themeProvider.nameSet(type);
    }
}

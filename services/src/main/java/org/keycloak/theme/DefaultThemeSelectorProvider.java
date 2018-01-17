package org.keycloak.theme;

import org.keycloak.Config;
import org.keycloak.common.Version;
import org.keycloak.models.KeycloakSession;

public class DefaultThemeSelectorProvider implements ThemeSelectorProvider {

    private final KeycloakSession session;

    public DefaultThemeSelectorProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public String getThemeName(Theme.Type type) {
        String name = null;

        switch (type) {
            case ACCOUNT:
                name = session.getContext().getRealm().getAccountTheme();
                break;
            case LOGIN:
                name = session.getContext().getRealm().getLoginTheme();
                break;
            case EMAIL:
                name = session.getContext().getRealm().getEmailTheme();
                break;
            case ADMIN:
                name = session.getContext().getRealm().getAdminTheme();
                break;
            case WELCOME:
                name = Config.scope("theme").get("welcomeTheme");
                break;
        }

        if (name == null) {
            name = Config.scope("theme").get("default", Version.NAME.toLowerCase());
        }

        return name;
    }

    @Override
    public void close() {
    }
}

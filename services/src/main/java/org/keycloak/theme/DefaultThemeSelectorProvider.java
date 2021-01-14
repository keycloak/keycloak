package org.keycloak.theme;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.common.Version;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import java.util.function.Function;

public class DefaultThemeSelectorProvider implements ThemeSelectorProvider {

    public static final String LOGIN_THEME_KEY = "login_theme";
    public static final String EMAIL_THEME_KEY = "email_theme";
    private static final boolean isAccount2Enabled = Profile.isFeatureEnabled(Profile.Feature.ACCOUNT2);

    private final KeycloakSession session;

    public DefaultThemeSelectorProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public String getThemeName(Theme.Type type) {
        String name = null;

        switch (type) {
            case WELCOME:
                name = Config.scope("theme").get("welcomeTheme");
                break;
            case LOGIN:
                name = getThemeName(LOGIN_THEME_KEY, RealmModel::getLoginTheme);
                break;
            case ACCOUNT:
                name = session.getContext().getRealm().getAccountTheme();
                break;
            case EMAIL:
                name = getThemeName(EMAIL_THEME_KEY, RealmModel::getEmailTheme);
                break;
            case ADMIN:
                name = session.getContext().getRealm().getAdminTheme();
                break;
        }

        if (name == null || name.isEmpty()) {
            name = Config.scope("theme").get("default", Version.NAME.toLowerCase());
            if ((type == Theme.Type.ACCOUNT) && isAccount2Enabled) {
                name = name.concat(".v2");
            }
        }

        return name;
    }

    private String getThemeName(String themeKey, Function<RealmModel, String> fallback) {
        ClientModel client = session.getContext().getClient();
        String name = null;
        if (client != null) {
            name = client.getAttribute(themeKey);
        }

        if (name != null && !name.trim().isEmpty()) {
            return name;
        }

        return fallback.apply(session.getContext().getRealm());
    }

    @Override
    public void close() {
    }

}


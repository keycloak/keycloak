package org.keycloak.theme;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.common.Version;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;

public class DefaultThemeSelectorProvider implements ThemeSelectorProvider {

    public static final String LOGIN_THEME_KEY = "login_theme";
    private static final boolean isAccount2Enabled = Profile.isFeatureEnabled(Profile.Feature.ACCOUNT2);
    private static final boolean isAdmin2Enabled = Profile.isFeatureEnabled(Profile.Feature.ADMIN2);

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
                ClientModel client = session.getContext().getClient();
                if (client != null) {
                    name = client.getAttribute(LOGIN_THEME_KEY);
                }

                if (name == null || name.isEmpty()) {
                    name = session.getContext().getRealm().getLoginTheme();
                }
                
                break;
            case ACCOUNT:
                name = session.getContext().getRealm().getAccountTheme();
                break;
            case EMAIL:
                name = session.getContext().getRealm().getEmailTheme();
                break;
            case ADMIN:
                name = session.getContext().getRealm().getAdminTheme();
                break;
        }

        if (name == null || name.isEmpty()) {
            name = Config.scope("theme").get("default", Version.NAME.toLowerCase());
            if ((type == Theme.Type.ACCOUNT) && isAccount2Enabled) {
                name = name.concat(".v2");
            } else if ((type == Theme.Type.ADMIN) && isAdmin2Enabled) {
                name = name.concat(".v2");
            }
        }

        return name;
    }

    @Override
    public void close() {
    }

}


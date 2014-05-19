package org.keycloak.theme;

import org.keycloak.Config;
import org.keycloak.freemarker.ThemeProvider;
import org.keycloak.freemarker.ThemeProviderFactory;
import org.keycloak.provider.ProviderSession;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class DefaultKeycloakThemeProviderFactory implements ThemeProviderFactory {

    private DefaultKeycloakThemeProvider themeProvider;

    @Override
    public ThemeProvider create(ProviderSession providerSession) {
        return themeProvider;
    }

    @Override
    public void init(Config.Scope config) {
        themeProvider = new DefaultKeycloakThemeProvider();
    }

    @Override
    public void close() {
        themeProvider = null;
    }

    @Override
    public String getId() {
        return "default";
    }

}

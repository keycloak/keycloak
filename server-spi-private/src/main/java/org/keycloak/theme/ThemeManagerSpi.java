package org.keycloak.theme;

import org.keycloak.models.ThemeManager;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class ThemeManagerSpi implements Spi {

    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public String getName() {
        return "themeManager";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return ThemeManager.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return ThemeManagerFactory.class;
    }
}

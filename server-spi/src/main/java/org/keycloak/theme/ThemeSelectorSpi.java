package org.keycloak.theme;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class ThemeSelectorSpi implements Spi {

    @Override
    public boolean isInternal() {
        return false;
    }

    @Override
    public String getName() {
        return "themeSelector";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return ThemeSelectorProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return ThemeSelectorProviderFactory.class;
    }
}

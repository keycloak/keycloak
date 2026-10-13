package org.keycloak.cookie;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class CookieSpi implements Spi {
    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public String getName() {
        return "cookie";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return CookieProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return CookieProviderFactory.class;
    }
}

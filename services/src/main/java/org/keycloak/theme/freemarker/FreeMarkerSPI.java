package org.keycloak.theme.freemarker;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class FreeMarkerSPI implements Spi {
    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public String getName() {
        return "freemarker";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return FreeMarkerProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return FreeMarkerProviderFactory.class;
    }
}

package org.keycloak.encoding;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class ResourceEncodingSpi implements Spi {

    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public String getName() {
        return "resource-encoding";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return ResourceEncodingProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return ResourceEncodingProviderFactory.class;
    }

}

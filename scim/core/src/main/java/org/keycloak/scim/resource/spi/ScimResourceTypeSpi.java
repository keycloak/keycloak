package org.keycloak.scim.resource.spi;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class ScimResourceTypeSpi implements Spi {

    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public String getName() {
        return "scimResourceType";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return ScimResourceTypeProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return ScimResourceTypeProviderFactory.class;
    }
}

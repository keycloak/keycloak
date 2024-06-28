package org.keycloak.services.resource;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

/**
 * <p>A {@link Spi} to replace Account resources.
 *
 * <p>Implementors can use this {@link Spi} to override the behavior of the Account endpoints and resources by
 * creating JAX-RS resources that override those served at /account by default.
 */
public class AccountResourceSpi implements Spi {

    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public String getName() {
        return "account-resource";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return AccountResourceProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return AccountResourceProviderFactory.class;
    }
}

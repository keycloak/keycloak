package org.keycloak.requestfilter;

import org.keycloak.common.Profile;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

/**
 * An SPI which allows for HTTP request filtering which is independent of the JAX-RS implementation.
 */
public class RequestFilterSpi implements Spi {

    public static final String SPI_ID = "request-filter";

    @Override
    public boolean isInternal() {
        return false;
    }

    @Override
    public String getName() {
        return SPI_ID;
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return RequestFilterProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return RequestFilterProviderFactory.class;
    }

    @Override
    public boolean isEnabled() {
        return Profile.isFeatureEnabled(Profile.Feature.ACCESS_FILTERING);
    }
}

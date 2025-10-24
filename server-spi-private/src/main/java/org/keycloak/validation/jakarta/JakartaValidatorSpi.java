package org.keycloak.validation.jakarta;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class JakartaValidatorSpi implements Spi {
    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public String getName() {
        return "jakarta-validator";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return JakartaValidatorProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory<?>> getProviderFactoryClass() {
        return JakartaValidatorProviderFactory.class;
    }
}

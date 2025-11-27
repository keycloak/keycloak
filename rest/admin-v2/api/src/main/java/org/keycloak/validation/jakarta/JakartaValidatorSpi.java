package org.keycloak.validation.jakarta;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class JakartaValidatorSpi implements Spi {
    public static final String NAME = "jakarta-validator";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return JakartaValidator.class;
    }

    @Override
    public Class<? extends ProviderFactory<?>> getProviderFactoryClass() {
        return JakartaValidatorFactory.class;
    }

    @Override
    public boolean isInternal() {
        return true;
    }
}

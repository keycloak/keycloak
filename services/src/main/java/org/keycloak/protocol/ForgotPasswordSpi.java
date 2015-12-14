package org.keycloak.protocol;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.services.resources.spi.RealmResourceSPI;

public class ForgotPasswordSpi extends RealmResourceSPI {

    @Override
    public boolean isInternal() {
        return false;
    }

    @Override
    public String getName() {
        return "forgot-password-email";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return ForgotPasswordProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return ForgotPasswordProviderFactory.class;
    }

}
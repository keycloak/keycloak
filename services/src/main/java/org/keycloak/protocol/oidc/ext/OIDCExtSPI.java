package org.keycloak.protocol.oidc.ext;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class OIDCExtSPI implements Spi {

    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public String getName() {
        return "openid-connect-ext";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return OIDCExtProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return OIDCExtProviderFactory.class;
    }

}

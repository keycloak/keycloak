package org.keycloak.protocol.ciba.decoupledauthn;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class DecoupledAuthenticationSpi implements Spi {

    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public String getName() {
        return "decoupled-authn";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return DecoupledAuthenticationProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return DecoupledAuthenticationProviderFactory.class;
    }

}

package org.keycloak.protocol.ciba.resolvers;

import org.keycloak.protocol.ciba.decoupledauthn.DecoupledAuthenticationProvider;
import org.keycloak.protocol.ciba.decoupledauthn.DecoupledAuthenticationProviderFactory;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class CIBALoginUserResolverSpi implements Spi {

    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public String getName() {
        return "ciba-login-user-resolver";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return CIBALoginUserResolver.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return CIBALoginUserResolverFactory.class;
    }

}
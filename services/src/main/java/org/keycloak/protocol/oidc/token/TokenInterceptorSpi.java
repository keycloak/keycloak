package org.keycloak.protocol.oidc.token;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class TokenInterceptorSpi implements Spi {

    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public String getName() {
        return "token-interceptor";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return TokenPostProcessor.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return TokenPostProcessorFactory.class;
    }
}

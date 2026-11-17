package org.keycloak.protocol.oidc.scope;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class ParameterizedScopeTypeSpi implements Spi {

    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public String getName() {
        return "parameterized-scope-type";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return ParameterizedScopeTypeProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory<ParameterizedScopeTypeProvider>> getProviderFactoryClass() {
        return ParameterizedScopeTypeProvider.class;
    }
}

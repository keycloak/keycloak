package org.keycloak.protocol.oidc.ida.mappers.connector;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class IdaConnectorSpi implements Spi {
    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public String getName() {
        return "ida-connector";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return IdaConnector.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return IdaConnectorFactory.class;
    }
}

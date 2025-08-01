package org.keycloak.services.trustchain;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class TrustChainSpi implements Spi {

    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public String getName() {
        return "trust-chain";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return TrustChainProcessor.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return TrustChainProcessorFactory.class;
    }

}

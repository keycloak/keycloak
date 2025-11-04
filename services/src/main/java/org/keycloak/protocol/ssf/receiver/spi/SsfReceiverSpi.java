package org.keycloak.protocol.ssf.receiver.spi;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class SsfReceiverSpi implements Spi {

    @Override
    public boolean isInternal() {
        return false;
    }

    @Override
    public String getName() {
        return "ssf-receiver";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return SsfReceiver.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return SsfReceiverFactory.class;
    }
}

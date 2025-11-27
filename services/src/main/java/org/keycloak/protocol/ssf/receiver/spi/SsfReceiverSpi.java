package org.keycloak.protocol.ssf.receiver.spi;

import org.keycloak.provider.Provider;
import org.keycloak.provider.Spi;

/**
 * SPI for Shared Signals Framework (SSF) Receiver support.
 */
public class SsfReceiverSpi implements Spi {

    @Override
    public String getName() {
        return "ssf-receiver";
    }

    @Override
    public boolean isInternal() {
        return false;
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return SsfReceiverProvider.class;
    }

    @Override
    public Class<? extends SsfReceiverProviderFactory> getProviderFactoryClass() {
        return SsfReceiverProviderFactory.class;
    }
}

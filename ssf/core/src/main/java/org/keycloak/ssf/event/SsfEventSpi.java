package org.keycloak.ssf.event;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

/**
 * SPI that allows extensions to contribute additional SSF event types to the
 * shared {@link SsfEventRegistry}. Multiple factories can be registered and
 * each factory's {@link SsfEventProviderFactory#getContributedEventFactories()} is
 * aggregated at startup.
 */
public class SsfEventSpi implements Spi {

    @Override
    public String getName() {
        return "ssf-events";
    }

    @Override
    public boolean isInternal() {
        return false;
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return SsfEventProvider.class;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return SsfEventProviderFactory.class;
    }
}

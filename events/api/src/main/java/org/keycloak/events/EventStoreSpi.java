package org.keycloak.events;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class EventStoreSpi implements Spi {

    @Override
    public String getName() {
        return "eventsStore";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return EventStoreProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return EventStoreProviderFactory.class;
    }

}

package org.keycloak.events;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class EventListenerSpi implements Spi {

    @Override
    public String getName() {
        return "eventsListener";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return EventListenerProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return EventListenerProviderFactory.class;
    }

}

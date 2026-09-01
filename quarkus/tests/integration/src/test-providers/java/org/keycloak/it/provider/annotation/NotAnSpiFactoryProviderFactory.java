package org.keycloak.it.provider.annotation;

import org.keycloak.Config;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.KeycloakProvider;

/**
 * Invalid {@link KeycloakProvider} value: the annotation names the concrete factory class itself, which the
 * type bound {@code Class<? extends ProviderFactory>} allows and which the class trivially implements. No SPI
 * uses that class as its provider factory class, so the build-time validation must reject it instead of
 * silently registering the factory under a key nobody looks up.
 */
@KeycloakProvider(NotAnSpiFactoryProviderFactory.class)
public class NotAnSpiFactoryProviderFactory implements EventListenerProviderFactory, EventListenerProvider {

    @Override
    public void onEvent(Event event) {
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
    }

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return "not-an-spi-factory";
    }
}

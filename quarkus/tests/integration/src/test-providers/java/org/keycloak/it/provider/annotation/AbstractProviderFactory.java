package org.keycloak.it.provider.annotation;

import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.KeycloakProvider;

/**
 * Invalid {@link KeycloakProvider} target: abstract, so it cannot be instantiated even though it has a
 * public no-arg constructor. The build-time validation must reject it.
 */
@KeycloakProvider(EventListenerProviderFactory.class)
public abstract class AbstractProviderFactory implements EventListenerProviderFactory {

    public AbstractProviderFactory() {
    }

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return null;
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
        return "abstract-factory";
    }
}

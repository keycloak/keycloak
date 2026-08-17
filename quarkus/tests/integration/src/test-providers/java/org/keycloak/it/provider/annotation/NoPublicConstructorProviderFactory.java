package org.keycloak.it.provider.annotation;

import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.KeycloakProvider;

/**
 * Invalid {@link KeycloakProvider} factory: no public no-arg constructor, so the build-time
 * validation must reject it.
 */
@KeycloakProvider
public class NoPublicConstructorProviderFactory implements EventListenerProviderFactory {

    private NoPublicConstructorProviderFactory() {
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
        return "no-public-constructor";
    }
}

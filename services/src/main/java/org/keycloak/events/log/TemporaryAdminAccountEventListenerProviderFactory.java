package org.keycloak.events.log;

import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class TemporaryAdminAccountEventListenerProviderFactory implements EventListenerProviderFactory {

    public static final String ID = "temp-admin-account";

    @Override
    public EventListenerProvider create(KeycloakSession keycloakSession) {
        return new TemporaryAdminAccountEventListenerProvider(keycloakSession);
    }

    @Override
    public void init(Config.Scope scope) {

    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return ID;
    }
}

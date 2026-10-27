package org.keycloak.events.untrustedlogin;

import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class UntrustedLoginEventListenerProviderFactory implements EventListenerProviderFactory {

    public static final String PROVIDER_ID = "untrusted-login-notifier";

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return new UntrustedLoginEventListenerProvider(session);
    }

    @Override
    public void init(Config.Scope config) {
        // no-op
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // no-op
    }

    @Override
    public void close() {
        // no-op
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
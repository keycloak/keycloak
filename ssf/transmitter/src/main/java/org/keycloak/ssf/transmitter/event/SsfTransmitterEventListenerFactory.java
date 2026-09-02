package org.keycloak.ssf.transmitter.event;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;

public class SsfTransmitterEventListenerFactory implements EventListenerProviderFactory, EnvironmentDependentProviderFactory {

    private static final String ID = "ssf-events";

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        // Create and return the event mapper
        return new SsfTransmitterEventListener(session);
    }

    @Override
    public boolean isGlobal() {
        return true;
    }

    @Override
    public void init(Config.Scope config) {
        // No initialization needed
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // NOOP
    }

    @Override
    public void close() {
        // No resources to close
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.SSF);
    }
}

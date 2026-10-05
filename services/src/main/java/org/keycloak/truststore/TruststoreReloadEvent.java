package org.keycloak.truststore;

import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderEvent;

/**
 * An event fired when the system truststore was reloaded and {@link TruststoreProvider} changed.
 */
public final class TruststoreReloadEvent implements ProviderEvent {

    private final KeycloakSession keycloakSession;

    public TruststoreReloadEvent(KeycloakSession keycloakSession) {
        this.keycloakSession = keycloakSession;
    }

    @Override
    public KeycloakSession getKeycloakSession() {
        return keycloakSession;
    }
}

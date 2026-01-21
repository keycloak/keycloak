package org.keycloak.provider;

import org.keycloak.models.KeycloakSessionFactory;

/**
 * Signals that the infrastructure initialization has completed.
 */
public class KeycloakInitializedEvent extends AbstractLifecycleEvent {

    public KeycloakInitializedEvent(KeycloakSessionFactory factory) {
        super(factory);
    }
}

package org.keycloak.provider;

import org.keycloak.models.KeycloakSessionFactory;

/**
 * Signals that Keycloak is about to be shutdown.
 */
public class KeycloakShutdownEvent extends AbstractLifecycleEvent {

    public KeycloakShutdownEvent(KeycloakSessionFactory factory) {
        super(factory);
    }
}

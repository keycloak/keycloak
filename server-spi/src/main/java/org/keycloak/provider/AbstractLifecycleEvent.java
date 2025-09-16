package org.keycloak.provider;

import org.keycloak.models.KeycloakSessionFactory;

public abstract class AbstractLifecycleEvent implements ProviderEvent {

    protected final KeycloakSessionFactory sessionFactory;

    public AbstractLifecycleEvent(KeycloakSessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public KeycloakSessionFactory getFactory() {
        return sessionFactory;
    }
}

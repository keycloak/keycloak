package org.keycloak.models.mapper;

import org.keycloak.models.KeycloakSession;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public abstract class AbstractRepModelMapper <T,U> implements RepModelMapper<T,U> {
    private final KeycloakSession session;

    public AbstractRepModelMapper(KeycloakSession session) {
        this.session = session;
    }

    public KeycloakSession getSession() {
        return session;
    }
}

package org.keycloak.models.sessions.jpa;

import org.keycloak.Config;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.UserSessionProviderFactory;

import javax.persistence.EntityManager;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class JpaUserSessionProviderFactory implements UserSessionProviderFactory {

    public static final String ID = "jpa";

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public UserSessionProvider create(KeycloakSession session) {
        EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        return new JpaUserSessionProvider(session, em);
    }

    @Override
    public void close() {
    }

}

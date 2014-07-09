package org.keycloak.models.sessions.jpa;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.sessions.SessionProvider;
import org.keycloak.models.sessions.SessionProviderFactory;
import org.keycloak.util.JpaUtils;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class JpaSessionProviderFactory implements SessionProviderFactory {

    protected EntityManagerFactory emf;

    @Override
    public void init(Config.Scope config) {
        String persistenceUnit = config.get("persistenceUnit", "jpa-keycloak-identity-store");
        emf = Persistence.createEntityManagerFactory(persistenceUnit, JpaUtils.getHibernateProperties());
    }

    @Override
    public String getId() {
        return "jpa";
    }

    @Override
    public SessionProvider create(KeycloakSession session) {
        return new JpaUserSessionProvider(emf.createEntityManager());
    }

    @Override
    public void close() {
        emf.close();
    }

}

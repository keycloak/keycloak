package org.keycloak.models.users.jpa;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.users.UserProvider;
import org.keycloak.models.users.UserProviderFactory;
import org.keycloak.util.JpaUtils;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class JpaUserProviderFactory implements UserProviderFactory {

    public static final String ID = "jpa";

    protected EntityManagerFactory emf;

    @Override
    public void init(Config.Scope config) {
        String persistenceUnit = config.get("persistenceUnit", "jpa-keycloak-identity-store");
        emf = Persistence.createEntityManagerFactory(persistenceUnit, JpaUtils.getHibernateProperties());
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public UserProvider create(KeycloakSession session) {
        return new JpaUserProvider(emf.createEntityManager());
    }

    @Override
    public void close() {
        emf.close();
    }

}

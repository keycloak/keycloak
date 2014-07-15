package org.keycloak.models.realms.jpa;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.realms.RealmProvider;
import org.keycloak.models.realms.RealmProviderFactory;
import org.keycloak.util.JpaUtils;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class JpaRealmProviderFactory implements RealmProviderFactory {

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
    public RealmProvider create(KeycloakSession session) {
        return new JpaRealmProvider(emf.createEntityManager());
    }

    @Override
    public void close() {
        emf.close();
    }

}

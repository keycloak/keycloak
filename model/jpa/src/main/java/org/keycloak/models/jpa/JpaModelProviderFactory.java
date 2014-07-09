package org.keycloak.models.jpa;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelProvider;
import org.keycloak.models.ModelProviderFactory;
import org.keycloak.util.JpaUtils;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class JpaModelProviderFactory implements ModelProviderFactory {

    protected EntityManagerFactory emf;

    @Override
    public void init(Config.Scope config) {
        emf = Persistence.createEntityManagerFactory("jpa-keycloak-identity-store", JpaUtils.getHibernateProperties());
    }

    @Override
    public String getId() {
        return "jpa";
    }

    @Override
    public ModelProvider create(KeycloakSession session) {
        return new JpaModelProvider(session, emf.createEntityManager());
    }

    @Override
    public void close() {
        emf.close();
    }

}

package org.keycloak.models.jpa;

import org.keycloak.Config;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.RealmProviderFactory;

import javax.persistence.EntityManager;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class JpaRealmProviderFactory implements RealmProviderFactory {

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public String getId() {
        return "jpa";
    }

    @Override
    public RealmProvider create(KeycloakSession session) {
        EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        return new JpaRealmProvider(session, em);
    }

    @Override
    public void close() {
    }

}

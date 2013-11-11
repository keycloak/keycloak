package org.keycloak.models.jpa;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import javax.persistence.EntityManagerFactory;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class JpaKeycloakSessionFactory implements KeycloakSessionFactory {
    protected EntityManagerFactory factory;

    public JpaKeycloakSessionFactory(EntityManagerFactory factory) {
        this.factory = factory;
    }

    @Override
    public KeycloakSession createSession() {
        return new JpaKeycloakSession(factory.createEntityManager());
    }

    @Override
    public void close() {
        factory.close();
    }
}

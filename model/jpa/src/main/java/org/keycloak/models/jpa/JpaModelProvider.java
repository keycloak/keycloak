package org.keycloak.models.jpa;

import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.ModelProvider;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class JpaModelProvider implements ModelProvider {
    @Override
    public KeycloakSessionFactory createFactory() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("jpa-keycloak-identity-store");
        return new JpaKeycloakSessionFactory(emf);

    }
}

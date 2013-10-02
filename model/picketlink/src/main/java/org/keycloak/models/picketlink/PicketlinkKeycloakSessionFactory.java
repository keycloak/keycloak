package org.keycloak.models.picketlink;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.picketlink.idm.PartitionManager;

import javax.persistence.EntityManagerFactory;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class PicketlinkKeycloakSessionFactory implements KeycloakSessionFactory {
    protected EntityManagerFactory factory;
    protected PartitionManager partitionManager;

    public PicketlinkKeycloakSessionFactory(EntityManagerFactory factory, PartitionManager partitionManager) {
        this.factory = factory;
        this.partitionManager = partitionManager;
    }

    @Override
    public KeycloakSession createSession() {
        return new PicketlinkKeycloakSession(partitionManager, factory.createEntityManager());
    }

    @Override
    public void close() {
        factory.close();
    }
}

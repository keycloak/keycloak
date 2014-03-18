package org.keycloak.spi.picketlink.impl;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.spi.picketlink.PartitionManagerProvider;
import org.keycloak.spi.picketlink.impl.PartitionManagerRegistry;
import org.keycloak.util.KeycloakRegistry;
import org.picketlink.idm.PartitionManager;

/**
 * Obtains {@link PartitionManager} instances from shared {@link PartitionManagerRegistry} and uses realm configuration for it
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class RealmPartitionManagerProvider implements PartitionManagerProvider {

    private static final Logger logger = Logger.getLogger(RealmPartitionManagerProvider.class);

    @Override
    public PartitionManager getPartitionManager(RealmModel realm) {
        KeycloakRegistry registry = ResteasyProviderFactory.getContextData(KeycloakRegistry.class) ;
        if (registry == null) {
            logger.warn("KeycloakRegistry not found");
            return null;
        }

        PartitionManagerRegistry partitionManagerRegistry = registry.getService(PartitionManagerRegistry.class);
        if (partitionManagerRegistry == null) {
            partitionManagerRegistry = new PartitionManagerRegistry();
            partitionManagerRegistry = registry.putServiceIfAbsent(PartitionManagerRegistry.class, partitionManagerRegistry);
            logger.info("Pushed PartitionManagerRegistry component");
        }

        return partitionManagerRegistry.getPartitionManager(realm);
    }
}

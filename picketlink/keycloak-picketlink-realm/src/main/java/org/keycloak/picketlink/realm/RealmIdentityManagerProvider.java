package org.keycloak.picketlink.realm;

import org.keycloak.models.RealmModel;
import org.keycloak.picketlink.IdentityManagerProvider;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.PartitionManager;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class RealmIdentityManagerProvider implements IdentityManagerProvider {

    private final PartitionManagerRegistry partitionManagerRegistry;

    public RealmIdentityManagerProvider(PartitionManagerRegistry partitionManagerRegistry) {
        this.partitionManagerRegistry = partitionManagerRegistry;
    }

    @Override
    public IdentityManager getIdentityManager(RealmModel realm) {
        PartitionManager partitionManager = partitionManagerRegistry.getPartitionManager(realm);
        return partitionManager.createIdentityManager();
    }

    @Override
    public void close() {
    }
}

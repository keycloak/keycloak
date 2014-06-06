package org.keycloak.picketlink.realm;

import org.keycloak.models.RealmModel;
import org.keycloak.picketlink.AbstractIdentityManagerProvider;
import org.keycloak.picketlink.IdentityManagerProvider;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.PartitionManager;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class RealmIdentityManagerProvider extends AbstractIdentityManagerProvider {

    private final PartitionManagerRegistry partitionManagerRegistry;

    public RealmIdentityManagerProvider(PartitionManagerRegistry partitionManagerRegistry) {
        this.partitionManagerRegistry = partitionManagerRegistry;
    }

    @Override
    protected PartitionManager getPartitionManager(RealmModel realm) {
        return partitionManagerRegistry.getPartitionManager(realm);
    }
}

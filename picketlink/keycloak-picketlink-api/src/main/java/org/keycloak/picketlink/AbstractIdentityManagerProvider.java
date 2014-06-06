package org.keycloak.picketlink;

import org.keycloak.models.RealmModel;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.PartitionManager;

/**
 * Per-request IdentityManager caching . Not thread-safe
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class AbstractIdentityManagerProvider implements IdentityManagerProvider {

    private IdentityManager identityManager;

    @Override
    public IdentityManager getIdentityManager(RealmModel realm) {
        if (identityManager == null) {
            PartitionManager partitionManager = getPartitionManager(realm);
            identityManager = partitionManager.createIdentityManager();
        }

        return identityManager;
    }

    protected abstract PartitionManager getPartitionManager(RealmModel realm);

    @Override
    public void close() {
        identityManager = null;
    }
}

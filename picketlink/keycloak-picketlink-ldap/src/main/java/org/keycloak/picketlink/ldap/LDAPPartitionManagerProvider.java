package org.keycloak.picketlink.ldap;

import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.picketlink.PartitionManagerProvider;
import org.picketlink.idm.PartitionManager;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LDAPPartitionManagerProvider implements PartitionManagerProvider {

    private final PartitionManagerRegistry partitionManagerRegistry;

    public LDAPPartitionManagerProvider(PartitionManagerRegistry partitionManagerRegistry) {
        this.partitionManagerRegistry = partitionManagerRegistry;
    }

    @Override
    public PartitionManager getPartitionManager(UserFederationProviderModel model) {
        return partitionManagerRegistry.getPartitionManager(model);
    }

    @Override
    public void close() {
    }
}

package org.keycloak.picketlink.ldap;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.picketlink.PartitionManagerProvider;
import org.keycloak.picketlink.PartitionManagerProviderFactory;
import org.picketlink.idm.PartitionManager;

/**
 * Obtains {@link PartitionManager} instances from shared {@link PartitionManagerRegistry} and uses UserFederationModel configuration for it
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LDAPPartitionManagerProviderFactory implements PartitionManagerProviderFactory {

    private PartitionManagerRegistry partitionManagerRegistry;

    @Override
    public PartitionManagerProvider create(KeycloakSession session) {
        return new LDAPPartitionManagerProvider(partitionManagerRegistry);
    }

    @Override
    public void init(Config.Scope config) {
        partitionManagerRegistry = new PartitionManagerRegistry();
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return "ldap";
    }

}

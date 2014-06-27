package org.keycloak.picketlink.realm;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.picketlink.IdentityManagerProvider;
import org.keycloak.picketlink.IdentityManagerProviderFactory;
import org.picketlink.idm.PartitionManager;

/**
 * Obtains {@link PartitionManager} instances from shared {@link PartitionManagerRegistry} and uses realm configuration for it
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class RealmIdentityManagerProviderFactory implements IdentityManagerProviderFactory {

    private PartitionManagerRegistry partitionManagerRegistry;

    @Override
    public IdentityManagerProvider create(KeycloakSession session) {
        return new RealmIdentityManagerProvider(partitionManagerRegistry);
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
        return "realm";
    }

}

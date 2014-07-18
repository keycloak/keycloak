package org.keycloak.picketlink;

import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.provider.Provider;
import org.picketlink.idm.PartitionManager;

/**
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface PartitionManagerProvider extends Provider {

    PartitionManager getPartitionManager(UserFederationProviderModel model);
}

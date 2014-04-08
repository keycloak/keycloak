package org.keycloak.picketlink;

import org.keycloak.models.RealmModel;
import org.picketlink.idm.PartitionManager;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface PartitionManagerProvider {

    PartitionManager getPartitionManager(RealmModel realm);
}

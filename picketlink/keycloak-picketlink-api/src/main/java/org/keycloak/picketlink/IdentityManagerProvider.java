package org.keycloak.picketlink;

import org.keycloak.models.RealmModel;
import org.keycloak.provider.Provider;
import org.picketlink.idm.IdentityManager;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface IdentityManagerProvider extends Provider {

    IdentityManager getIdentityManager(RealmModel realm);
}

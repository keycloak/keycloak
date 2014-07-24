package org.keycloak.models.cache.entities;

import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class CachedRealmRole extends CachedRole {


    public CachedRealmRole(RoleModel model, RealmModel realm) {
        super(model, realm);

    }

}

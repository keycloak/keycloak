package org.keycloak.models.cache.entities;

import org.keycloak.models.RoleModel;

import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class CachedRealmRole extends CachedRole {


    public CachedRealmRole(RoleModel model) {
        super(model);

    }

}

package org.keycloak.models.cache.entities;

import org.keycloak.models.RoleModel;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class CachedApplicationRole extends CachedRole {
    private final String appId;

    public CachedApplicationRole(String appId, RoleModel model) {
        super(model);
        this.appId = appId;

    }

    public String getAppId() {
        return appId;
    }
}

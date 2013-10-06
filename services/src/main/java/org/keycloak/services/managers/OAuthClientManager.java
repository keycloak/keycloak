package org.keycloak.services.managers;

import org.keycloak.models.OAuthClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OAuthClientManager {
    protected RealmModel realm;

    public OAuthClientManager(RealmModel realm) {
        this.realm = realm;
    }

    public OAuthClientModel createOAuthClient(String name) {
        OAuthClientModel model = realm.addOAuthClient(name);
        RoleModel role = realm.getRole(RealmManager.IDENTITY_REQUESTER_ROLE);
        realm.grantRole(model.getOAuthAgent(), role);
        return model;
    }
}

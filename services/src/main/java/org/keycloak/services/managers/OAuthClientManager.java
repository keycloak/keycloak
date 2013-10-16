package org.keycloak.services.managers;

import org.keycloak.models.Constants;
import org.keycloak.models.OAuthClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.representations.idm.OAuthClientRepresentation;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OAuthClientManager {
    protected RealmModel realm;

    public OAuthClientManager(RealmModel realm) {
        this.realm = realm;
    }

    public OAuthClientModel create(String name) {
        OAuthClientModel model = realm.addOAuthClient(name);
        RoleModel role = realm.getRole(Constants.IDENTITY_REQUESTER_ROLE);
        realm.grantRole(model.getOAuthAgent(), role);
        return model;
    }

    public OAuthClientModel create(OAuthClientRepresentation rep) {
        OAuthClientModel model = create(rep.getName());
        model.setBaseUrl(rep.getBaseUrl());
        model.getOAuthAgent().setEnabled(rep.isEnabled());
        return model;
    }

    public void update(OAuthClientRepresentation rep, OAuthClientModel model) {
        model.setBaseUrl(rep.getBaseUrl());
    }

    public static OAuthClientRepresentation toRepresentation(OAuthClientModel model) {
        OAuthClientRepresentation rep = new OAuthClientRepresentation();
        rep.setId(model.getId());
        rep.setBaseUrl(model.getBaseUrl());
        rep.setName(model.getOAuthAgent().getLoginName());
        rep.setEnabled(model.getOAuthAgent().isEnabled());
        return rep;
    }
}

package org.keycloak.services.managers;

import org.keycloak.models.Constants;
import org.keycloak.models.OAuthClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.OAuthClientRepresentation;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

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
        update(rep, model);
        UserModel resourceUser = model.getOAuthAgent();
        if (rep.getCredentials() != null) {
            for (CredentialRepresentation cred : rep.getCredentials()) {
                UserCredentialModel credential = new UserCredentialModel();
                credential.setType(cred.getType());
                credential.setValue(cred.getValue());
                realm.updateCredential(resourceUser, credential);
            }
        }
        return model;
    }

    public void update(OAuthClientRepresentation rep, OAuthClientModel model) {
        model.getOAuthAgent().setEnabled(rep.isEnabled());
        List<String> redirectUris = rep.getRedirectUris();
        if (redirectUris != null) {
            model.getOAuthAgent().setRedirectUris(new HashSet<String>(redirectUris));
        }

        List<String> webOrigins = rep.getWebOrigins();
        if (webOrigins != null) {
            model.getOAuthAgent().setWebOrigins(new HashSet<String>(webOrigins));
        }
    }

    public static OAuthClientRepresentation toRepresentation(OAuthClientModel model) {
        OAuthClientRepresentation rep = new OAuthClientRepresentation();
        rep.setId(model.getId());
        rep.setName(model.getOAuthAgent().getLoginName());
        rep.setEnabled(model.getOAuthAgent().isEnabled());
        Set<String> redirectUris = model.getOAuthAgent().getRedirectUris();
        if (redirectUris != null) {
            rep.setRedirectUris(new LinkedList<String>(redirectUris));
        }

        Set<String> webOrigins = model.getOAuthAgent().getWebOrigins();
        if (webOrigins != null) {
            rep.setWebOrigins(new LinkedList<String>(webOrigins));
        }
        return rep;
    }
}

package org.keycloak.services.resources.admin;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OAuthClientModel;
import org.keycloak.models.RealmModel;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OAuthClientsByIdResource extends OAuthClientsResource {
    public OAuthClientsByIdResource(RealmModel realm, RealmAuth auth, KeycloakSession session) {
        super(realm, auth, session);
    }

    @Override
    protected OAuthClientModel getOAuthClientModel(String id) {
        return realm.getOAuthClientById(id);
    }

    @Override
    protected String getClientPath(OAuthClientModel oauth) {
        return oauth.getId();
    }

}

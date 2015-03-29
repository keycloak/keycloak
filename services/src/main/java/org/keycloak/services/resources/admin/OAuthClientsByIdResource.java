package org.keycloak.services.resources.admin;

import org.keycloak.events.EventBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OAuthClientModel;
import org.keycloak.models.RealmModel;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OAuthClientsByIdResource extends OAuthClientsResource {
    public OAuthClientsByIdResource(RealmModel realm, RealmAuth auth, KeycloakSession session, EventBuilder event) {
        super(realm, auth, session, event);
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

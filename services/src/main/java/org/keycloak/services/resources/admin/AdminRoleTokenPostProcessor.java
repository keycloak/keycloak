package org.keycloak.services.resources.admin;

import java.util.Map;
import java.util.Map.Entry;

import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.token.TokenPostProcessor;
import org.keycloak.protocol.oidc.token.TokenPostProcessorContext;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessToken.Access;

import static org.keycloak.models.utils.KeycloakModelUtils.removeTransientAdminRoles;

/**
 * A {@link TokenPostProcessor} that removes from access tokens any admin role not explicitly granted to the
 * subject.
 */
public class AdminRoleTokenPostProcessor implements TokenPostProcessor {

    private final KeycloakSession session;

    public AdminRoleTokenPostProcessor(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void process(TokenPostProcessorContext context) {
        ClientSessionContext clientSessionCtx = context.clientSessionCtx();
        AuthenticatedClientSessionModel clientSession = clientSessionCtx.getClientSession();
        UserSessionModel userSession = clientSession.getUserSession();
        UserModel user = userSession.getUser();
        RealmModel realm = session.getContext().getRealm();
        AccessToken accessToken = context.accessToken();

        removeTransientAdminRoles(realm, null, user, accessToken.getRealmAccess());

        Map<String, Access> resourceAccess = accessToken.getResourceAccess();

        for (Entry<String, Access> access : resourceAccess.entrySet()) {
            removeTransientAdminRoles(realm, access.getKey(), user, access.getValue());
        }
    }
}

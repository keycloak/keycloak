package org.keycloak.services.clientpolicy.context;

import java.util.Optional;

import jakarta.ws.rs.core.MultivaluedMap;

import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.services.clientpolicy.ClientPolicyEvent;

import org.jboss.logging.Logger;

import static org.keycloak.OAuth2Constants.CODE;
import static org.keycloak.protocol.oidc.utils.OAuth2CodeParser.CACHE_KEY_PREFIX;

public class PreTokenRequestContext implements ClientModelContext {

    private static final Logger LOGGER = Logger.getLogger(PreTokenRequestContext.class);

    private final KeycloakSession session;
    private final MultivaluedMap<String, String> formParams;
    private ClientModel client;

    public PreTokenRequestContext(KeycloakSession session, MultivaluedMap<String, String> formParams) {
        this.session = session;
        this.formParams = formParams;
    }

    @Override
    public ClientPolicyEvent getEvent() {
        return ClientPolicyEvent.PRE_TOKEN_REQUEST;
    }

    public ClientModel getClient() {

        // Best-effort extraction of the client (UUID) from the authorization code, without invalidating the code.
        // This is needed so that client policy conditions can be evaluated based on the client before full token processing.

        String authCode = formParams.getFirst(CODE);
        if (client == null && authCode != null) {
            String[] parsed = authCode.split("\\.", 3);
            if (parsed.length < 3) {
                LOGGER.debug("Invalid authorization code format");
                return null;
            }

            String codeUUID = parsed[0];
            String userSessionId = parsed[1];
            String clientUUID = parsed[2];

            // Avoid applying client-policy decisions to an obviously illegal/used code.
            if (!session.singleUseObjects().contains(CACHE_KEY_PREFIX + codeUUID)) {
                LOGGER.debug("Invalid or already used authorization code");
                return null;
            }

            // Retrieve UserSession
            RealmModel realm = session.getContext().getRealm();
            UserSessionModel userSession = session.sessions().getUserSession(realm, userSessionId);
            if (userSession == null) {
                LOGGER.debug("Invalid authorization code");
                return null;
            }

            AuthenticatedClientSessionModel clientSession = userSession.getAuthenticatedClientSessionByClient(clientUUID);
            client = Optional.ofNullable(clientSession)
                    .map(AuthenticatedClientSessionModel::getClient)
                    .orElse(null);
            if (client == null) {
                LOGGER.debug("No authenticated client session");
                return null;
            }
        }
        return client;
    }
}

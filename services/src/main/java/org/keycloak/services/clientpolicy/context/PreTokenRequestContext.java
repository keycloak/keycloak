package org.keycloak.services.clientpolicy.context;

import java.util.Objects;
import java.util.Optional;

import jakarta.ws.rs.core.MultivaluedMap;

import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.services.clientpolicy.ClientPolicyEvent;

import org.jboss.logging.Logger;

import static org.keycloak.OAuth2Constants.CLIENT_ASSERTION;
import static org.keycloak.OAuth2Constants.CLIENT_ASSERTION_TYPE;
import static org.keycloak.OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT;
import static org.keycloak.OAuth2Constants.CLIENT_ID;
import static org.keycloak.OAuth2Constants.CODE;
import static org.keycloak.protocol.oidc.utils.OAuth2CodeParser.CACHE_KEY_PREFIX;

public class PreTokenRequestContext implements ClientModelContext {

    private static final Logger LOGGER = Logger.getLogger(PreTokenRequestContext.class);

    private final KeycloakSession session;
    private final MultivaluedMap<String, String> formParams;
    private ClientModel client;
    private final String clientId;


    public PreTokenRequestContext(KeycloakSession session, MultivaluedMap<String, String> formParams) {
        this.session = session;
        this.formParams = formParams;
        this.clientId = resolveClientId(formParams);
    }

    /**
     * Resolves the client identifier of a token request. The {@code client_id} form parameter takes precedence. If it
     * is absent, the subject of a self-issued {@code client_assertion} (e.g. {@code private_key_jwt}) is used, as
     * {@code JWTClientAuthenticator} identifies the client the same way.
     *
     * <p>The assertion is only parsed here, not verified: it is a hint for client policy evaluation before client
     * authentication. Its signature is still validated by the client authenticator afterward.
     */
    private static String resolveClientId(MultivaluedMap<String, String> formParams) {
        String formClientId = formParams.getFirst(CLIENT_ID);
        if (formClientId != null) {
            return formClientId;
        }

        String clientAssertion = formParams.getFirst(CLIENT_ASSERTION);
        if (clientAssertion == null || !CLIENT_ASSERTION_TYPE_JWT.equals(formParams.getFirst(CLIENT_ASSERTION_TYPE))) {
            return null;
        }

        try {
            JsonWebToken jwt = new JWSInput(clientAssertion).readJsonContent(JsonWebToken.class);
            // client assertions signed by third parties do not identify the client by their subject
            if (jwt == null || jwt.getSubject() == null || !Objects.equals(jwt.getIssuer(), jwt.getSubject())) {
                return null;
            }
            return jwt.getSubject();
        } catch (JWSInputException e) {
            LOGGER.debug("Invalid client assertion format");
            return null;
        }
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

    public String getClientId() {
        return clientId;
    }

    public MultivaluedMap<String, String> getRequestParameters() {
        return formParams;
    }
}

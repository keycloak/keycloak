package org.keycloak.protocol.oidc.grants.jwtauthorization.validator;

import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.authenticators.client.ClientAssertionState;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.JsonWebToken;

public class DefaultJWTAuthorizationGrantValidator extends JWTAuthorizationGrantValidatorBase {

    public static DefaultJWTAuthorizationGrantValidator createValidator(KeycloakSession session, ClientModel client, String assertion, String scope) {
        if (assertion == null) {
            throw new RuntimeException("Missing parameter:" + OAuth2Constants.ASSERTION);
        }
        try {
            JWSInput jws = new JWSInput(assertion);
            JsonWebToken jwt = jws.readJsonContent(JsonWebToken.class);
            ClientAssertionState clientAssertionState = new ClientAssertionState(OAuth2Constants.JWT_AUTHORIZATION_GRANT, assertion, jws, jwt);
            clientAssertionState.setClient(client);
            return new DefaultJWTAuthorizationGrantValidator(session, scope, clientAssertionState);
        } catch (JWSInputException e) {
            throw new RuntimeException("The provided assertion is not a valid JWT");
        }
    }

    private DefaultJWTAuthorizationGrantValidator(KeycloakSession session, String scope, ClientAssertionState clientAssertionState) {
        super(session, clientAssertionState);
        this.scope = scope;
        this.audienceAlreadyValidated = false;
    }

}

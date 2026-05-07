package org.keycloak.protocol.oidc.grants.jwtauthorization.validator;

import org.keycloak.authentication.authenticators.client.ClientAssertionState;
import org.keycloak.models.KeycloakSession;

public class DefaultJWTAuthorizationGrantValidator extends JWTAuthorizationGrantValidatorBase {

    public DefaultJWTAuthorizationGrantValidator(KeycloakSession session, String scope, ClientAssertionState clientAssertionState) {
        super(session, clientAssertionState);
        this.scope = scope;
    }

}

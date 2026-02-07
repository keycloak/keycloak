package org.keycloak.protocol.oidc.grants.jwtauthorization.validator;

import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.AccessToken;

import org.jboss.logging.Logger;

public class IDJWTAuthorizationGrantValidator extends JWTAuthorizationGrantValidatorBase {

    private static final Logger logger = Logger.getLogger(IDJWTAuthorizationGrantValidator.class);

    public IDJWTAuthorizationGrantValidator(KeycloakSession session) {
        super(session);
    }

    public void validateClient() {
        super.validateClient();

        try{
            AccessToken accessToken = new JWSInput(getAssertion()).readJsonContent(AccessToken.class);
            String clientIdInToken = (String) accessToken.getOtherClaims().get("client_id");
            if (!session.getContext().getClient().getClientId().equals(clientIdInToken)){
                logger.warn("client id in assertion : "+clientIdInToken+" and client id in request param : "+session.getContext().getClient().getClientId());
                failureCallback("client id in assertion : "+clientIdInToken+" and client id in request param : "+session.getContext().getClient().getClientId());
                return;
            }
        } catch (JWSInputException e) {
            failureCallback("The provided assertion is not a valid JWT");
            return;
        }
    }

}

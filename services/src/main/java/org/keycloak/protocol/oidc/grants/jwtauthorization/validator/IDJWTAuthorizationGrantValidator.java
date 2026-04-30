package org.keycloak.protocol.oidc.grants.jwtauthorization.validator;

import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.JsonWebToken;

import org.jboss.logging.Logger;

/**
 * a JWT validator for Identity Assertion JWT Authorization Grant (ID-JAG).
 * Identity Assertion JWT is a new type of JWT that can be used as an authorization grant per RFC 7523.
 *  
 * https://datatracker.ietf.org/doc/draft-ietf-oauth-identity-assertion-authz-grant/
 *
 * @author <a href="mailto:yutaka.obuchi.sd@hitachi.com">Yutaka Obuchi</a>
 */
public class IDJWTAuthorizationGrantValidator extends JWTAuthorizationGrantValidatorBase {

    private static final Logger logger = Logger.getLogger(IDJWTAuthorizationGrantValidator.class);

    public IDJWTAuthorizationGrantValidator(KeycloakSession session) {
        super(session);
    }

    public void validateClient() {
        super.validateClient();

        JsonWebToken accessToken = clientAssertionState.getToken();

        String clientIdInToken = (String) accessToken.getOtherClaims().get("client_id");
        String clientIdInRequestHeaderOrBody = session.getContext().getClient().getClientId();
        if (clientIdInToken == null || !clientIdInRequestHeaderOrBody.equals(clientIdInToken)) {
                logger.warn("client id in assertion : " + clientIdInToken + " and client id in request header/body : " + clientIdInRequestHeaderOrBody);
                failureCallback("client id in assertion : " + clientIdInToken + " and client id in request header/body : " + clientIdInRequestHeaderOrBody);
                return;
        }
 
    }

    public boolean validateTokenActive(int allowedClockSkew, int maxExp, boolean reusePermitted) {

        JsonWebToken accessToken = clientAssertionState.getToken();
        if (accessToken.getIat() == null) {
            failureCallback("Token iat claim is required");
            return false;
        }

        if (reusePermitted) {
            logger.warn("Token reuse is not permitted. Token reuse permitted setting is ignored.");            
        }

        return super.validateTokenActive(allowedClockSkew, maxExp, false);

    }

}

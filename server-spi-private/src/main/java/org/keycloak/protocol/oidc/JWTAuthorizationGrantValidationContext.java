package org.keycloak.protocol.oidc;

import org.keycloak.jose.jws.JWSInput;
import org.keycloak.representations.JsonWebToken;


public interface JWTAuthorizationGrantValidationContext {

    String getAssertion();

    JsonWebToken getJWT();

    JWSInput getJws();

    default String getIssuer() {
        return getJWT().getIssuer();
    }

    default String getSubject() {
        return getJWT().getSubject();
    }
}

package org.keycloak.protocol.oidc;

import java.util.Set;

import org.keycloak.jose.jws.JWSInput;
import org.keycloak.representations.JsonWebToken;


public interface JWTAuthorizationGrantValidationContext {

    String getAssertion();

    JsonWebToken getJWT();

    JWSInput getJws();

    String getScopeParam();

    Set<String> getRestrictedScopes();

    void setRestrictedScopes(Set<String> restrictedScopes);

    void setAudienceAlreadyValidated();

    boolean isAudienceAlreadyValidated();

    default String getIssuer() {
        return getJWT().getIssuer();
    }

    default String getSubject() {
        return getJWT().getSubject();
    }
}

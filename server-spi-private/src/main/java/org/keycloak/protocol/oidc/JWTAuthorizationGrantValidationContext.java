package org.keycloak.protocol.oidc;

import org.keycloak.OAuth2Constants;
import org.keycloak.common.util.Time;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.models.ClientModel;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.JsonWebToken;

import java.util.Collections;
import java.util.List;

public class JWTAuthorizationGrantValidationContext {

    private final String assertion;

    private final ClientModel client;

    private JsonWebToken jwt;

    private final String expectedAudience;

    private JWSInput jws;

    private final long currentTime;

    public JWTAuthorizationGrantValidationContext(String assertion, ClientModel client, String expectedAudience) {
        this.assertion = assertion;
        this.client = client;
        this.expectedAudience = expectedAudience;
        this.currentTime = Time.currentTimeMillis();
    }

    public void validateJWTFormat() {
        try {
            this.jws = new JWSInput(assertion);
            this.jwt = jws.readJsonContent(JsonWebToken.class);
        }
        catch (Exception e) {
            failure("The provided assertion is not a valid JWT");
        }
    }

    public void validateAssertionParameters() {
        if (assertion == null) {
            failure("Missing parameter:" + OAuth2Constants.ASSERTION);
        }
    }

    public void validateClient() {
        if (client.isPublicClient()) {
            failure("Public client not allowed to use authorization grant");
        }

        String val = client.getAttribute(OIDCConfigAttributes.JWT_AUTHORIZATION_GRANT_ENABLED);
        if (!Boolean.parseBoolean(val)) {
            throw new RuntimeException("JWT Authorization Grant is not supported for the requested client");
        }

    }

    public void validateTokenActive() {
        JsonWebToken token = getJWT();
        int allowedClockSkew = getAllowedClockSkew();
        int maxExp = getMaximumExpirationTime();
        long lifespan;

        if (token.getExp() == null) {
            failure("Token exp claim is required");
        }

        if (!token.isActive(allowedClockSkew)) {
            failure("Token is not active");
        }

        lifespan = token.getExp() - currentTime;

        if (token.getIat() == null) {
            if (lifespan > maxExp) {
                failure("Token expiration is too far in the future and iat claim not present in token");
            }
        } else {
            if (token.getIat() - allowedClockSkew > currentTime) {
                failure("Token was issued in the future");
            }
            lifespan = Math.min(lifespan, maxExp);
            if (lifespan <= 0) {
                failure("Token is not active");
            }
            if (currentTime > token.getIat() + maxExp) {
                failure("Token was issued too far in the past to be used now");
            }
        }
    }

    public void validateAudience() {
        JsonWebToken token = getJWT();
        List<String> expectedAudiences = getExpectedAudiences();
        if (!token.hasAnyAudience(expectedAudiences)) {
            failure("Invalid token audience");
        }
    }

    public void validateIssuer() {
        if (jwt == null || jwt.getIssuer() == null) {
            failure("Missing claim: " + OAuth2Constants.ISSUER);
        }
    }

    public void validateSubject() {
        if (jwt == null || jwt.getSubject() == null) {
            failure("Missing claim: " + IDToken.SUBJECT);
        }
    }

    public void failure(String errorMessage) {
        throw new RuntimeException(errorMessage);
    }

    public JsonWebToken getJWT() {
        return jwt;
    }

    public JWSInput getJws() {
        return jws;
    }

    public String getIssuer() {
        return jwt.getIssuer();
    }

    public String getSubject() {
        return jwt.getSubject();
    }

    public String getAssertion() {
        return assertion;
    }

    private List<String> getExpectedAudiences() {
        return Collections.singletonList(expectedAudience);
    }

    private int getAllowedClockSkew() {
        return 15;
    }

    private int getMaximumExpirationTime() {
        return 300;
    }
}

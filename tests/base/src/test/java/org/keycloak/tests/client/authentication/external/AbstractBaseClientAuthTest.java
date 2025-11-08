package org.keycloak.tests.client.authentication.external;

import org.keycloak.common.util.Time;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.testframework.oauth.OAuthIdentityProvider;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public abstract class AbstractBaseClientAuthTest extends AbstractClientAuthTest {

    public AbstractBaseClientAuthTest(String expectedTokenIssuer, String internalClientId, String externalClientId) {
        super(expectedTokenIssuer, internalClientId, externalClientId);
    }

    @Test
    public void testValidToken() {
        JsonWebToken token = createDefaultToken();
        assertSuccess(internalClientId, doClientGrant(token));
        assertSuccess(internalClientId, token.getId(), expectedTokenIssuer, externalClientId, events.poll());
    }

    @Test
    public void testInvalidSignature() {
        OAuthIdentityProvider.OAuthIdentityProviderKeys keys = getIdentityProvider().createKeys();
        JsonWebToken jwt = createDefaultToken();
        String jws = getIdentityProvider().encodeToken(jwt, keys);
        assertFailure("Invalid client or Invalid client credentials", doClientGrant(jws));
        assertFailure(internalClientId, expectedTokenIssuer, externalClientId, jwt.getId(), events.poll());
    }

    @Test
    public void testInvalidSub() {
        JsonWebToken jwt = createDefaultToken();
        jwt.subject("invalid");
        Assertions.assertFalse(doClientGrant(jwt).isSuccess());
        assertFailure(null, expectedTokenIssuer, "invalid", jwt.getId(), "client_not_found", events.poll());
    }

    @Test
    public void testExpired() {
        JsonWebToken jwt = createDefaultToken();
        jwt.exp((long) (Time.currentTime() - 30));
        assertFailure("Token is not active", doClientGrant(jwt));
        assertFailure(internalClientId, expectedTokenIssuer, externalClientId, jwt.getId(), events.poll());
    }

    @Test
    public void testMissingExp() {
        JsonWebToken jwt = createDefaultToken();
        jwt.exp(null);
        assertFailure("Token exp claim is required", doClientGrant(jwt));
        assertFailure(internalClientId, expectedTokenIssuer, externalClientId, jwt.getId(), events.poll());
    }

    @Test
    public void testInvalidNbf() {
        JsonWebToken jwt = createDefaultToken();
        jwt.nbf((long) (Time.currentTime() + 60));
        assertFailure("Token is not active", doClientGrant(jwt));
        assertFailure(internalClientId, expectedTokenIssuer, externalClientId, jwt.getId(), events.poll());
    }

    @Test
    public void testInvalidAud() {
        JsonWebToken jwt = createDefaultToken();
        jwt.audience("invalid");
        assertFailure("Invalid token audience", doClientGrant(jwt));
        assertFailure(internalClientId, expectedTokenIssuer, externalClientId, jwt.getId(), events.poll());
    }

    @Test
    public void testMissingAud() {
        JsonWebToken jwt = createDefaultToken();
        jwt.audience((String) null);
        assertFailure("Invalid token audience", doClientGrant(jwt));
        assertFailure(internalClientId, expectedTokenIssuer, externalClientId, jwt.getId(), events.poll());
    }

    @Test
    public void testMultipleAud() {
        JsonWebToken jwt = createDefaultToken();
        jwt.audience(jwt.getAudience()[0], "invalid");
        assertFailure("Multiple audiences not allowed", doClientGrant(jwt));
        assertFailure(internalClientId, expectedTokenIssuer, externalClientId, jwt.getId(), events.poll());
    }

    @Test
    public void testValidInvalidAssertionType() {
        JsonWebToken jwt = createDefaultToken();
        String jws = getIdentityProvider().encodeToken(jwt);
        AccessTokenResponse response = oAuthClient.clientCredentialsGrantRequest().clientJwt(jws, "urn:ietf:params:oauth:client-assertion-type:invalid").send();
        assertFailure(response);
        assertFailure(null, expectedTokenIssuer, externalClientId, jwt.getId(), "client_not_found", events.poll());
    }

}

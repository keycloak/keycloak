package org.keycloak.tests.client.authentication.external;

import org.keycloak.common.util.Time;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.testframework.oauth.OAuthIdentityProvider;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public abstract class AbstractBaseClientAuthTest extends AbstractClientAuthTest {

    protected final String idpAlias;

    public AbstractBaseClientAuthTest(String expectedTokenIssuer, String internalClientId, String externalClientId, String idpAlias) {
        super(expectedTokenIssuer, internalClientId, externalClientId);
        this.idpAlias = idpAlias;
    }

    protected abstract ManagedRealm getRealm();

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


    @Test
    public void testClientAssertionMaxExpiration() {

        // Set max expiration for client assertions to 60 seconds
        getRealm().updateIdentityProvider(idpAlias, rep -> {
            rep.getConfig().put(IdentityProviderModel.FEDERATED_CLIENT_ASSERTION_MAX_EXPIRATION, "60");
        });

        // Token issued just now with exp within the limit should succeed
        JsonWebToken jwt = createDefaultToken();
        jwt.iat((long) Time.currentTime());
        jwt.exp((long) (Time.currentTime() + 30));
        assertSuccess(internalClientId, doClientGrant(jwt));
        assertSuccess(internalClientId, jwt.getId(), expectedTokenIssuer, externalClientId, events.poll());

        // Token issued too far in the past should fail (iat + maxExp < currentTime)
        jwt = createDefaultToken();
        jwt.iat((long) (Time.currentTime() - 120));
        jwt.exp((long) (Time.currentTime() + 30));
        assertFailure("Token was issued too far in the past to be used now", doClientGrant(jwt));
        assertFailure(internalClientId, expectedTokenIssuer, externalClientId, jwt.getId(), events.poll());
    }

    @Test
    public void testClientAssertionMaxExpirationWithoutIat() {
        // Set max expiration for client assertions to 60 seconds
        getRealm().updateIdentityProvider(idpAlias, rep -> {
            rep.getConfig().put(IdentityProviderModel.FEDERATED_CLIENT_ASSERTION_MAX_EXPIRATION, "60");
        });

        // Token without iat and exp too far in the future should fail
        JsonWebToken jwt = createDefaultToken();
        jwt.iat(null);
        jwt.exp((long) (Time.currentTime() + 120));
        assertFailure("Token expiration is too far in the future and iat claim not present in token", doClientGrant(jwt));
        assertFailure(internalClientId, expectedTokenIssuer, externalClientId, jwt.getId(), events.poll());

        // Token without iat but exp within the limit should succeed
        jwt = createDefaultToken();
        jwt.iat(null);
        jwt.exp((long) (Time.currentTime() + 30));
        assertSuccess(internalClientId, doClientGrant(jwt));
        assertSuccess(internalClientId, jwt.getId(), expectedTokenIssuer, externalClientId, events.poll());
    }

}

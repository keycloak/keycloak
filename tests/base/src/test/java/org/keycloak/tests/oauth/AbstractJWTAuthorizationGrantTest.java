package org.keycloak.tests.oauth;

import java.util.Collections;
import java.util.List;

import org.keycloak.OAuth2Constants;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.common.util.PemUtils;
import org.keycloak.common.util.Time;
import org.keycloak.crypto.Algorithm;
import org.keycloak.events.Details;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.oauth.OAuthIdentityProvider;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

public abstract class AbstractJWTAuthorizationGrantTest extends BaseAbstractJWTAuthorizationGrantTest {

    @Test
    public void testPublicClient() {
        String jwt = getIdentityProvider().encodeToken(createDefaultAuthorizationGrantToken());
        oAuthClient.client("test-public");
        AccessTokenResponse response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        assertFailure("Public client not allowed to use authorization grant", response, events.poll());
        oAuthClient.client("test-app", "test-secret");
    }

    @Test
    public void testIdpNotAllowedForClient() {
        String jwt = getIdentityProvider().encodeToken(createDefaultAuthorizationGrantToken());
        oAuthClient.client("authorization-grant-not-allowed-idp-client", "test-secret");
        AccessTokenResponse response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        assertFailure("Identity Provider is not allowed for the client", response, events.poll());
        oAuthClient.client("test-app", "test-secret");
    }

    @Test
    public void testNotAllowedClient() {
        String jwt = getIdentityProvider().encodeToken(createDefaultAuthorizationGrantToken());
        oAuthClient.client("authorization-grant-disabled-client", "test-secret");
        AccessTokenResponse response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        assertFailure("JWT Authorization Grant is not supported for the requested client", response, events.poll());
        oAuthClient.client("test-app", "test-secret");
    }

    @Test
    public void testMissingAssertionParameter() {
        AccessTokenResponse response = oAuthClient.jwtAuthorizationGrantRequest(null).send();
        assertFailure("Missing parameter:" + OAuth2Constants.ASSERTION, response, events.poll());
    }

    @Test
    public void testBadAssertionParameter() {
        AccessTokenResponse response = oAuthClient.jwtAuthorizationGrantRequest("fake-jwt").send();
        assertFailure("The provided assertion is not a valid JWT", response, events.poll());
    }

    @Test
    public void testExpiredAssertion() {
        String jwt = getIdentityProvider().encodeToken(createAuthorizationGrantToken("basic-user-id", oAuthClient.getEndpoints().getIssuer(), IDP_ISSUER, null));
        AccessTokenResponse response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        assertFailure("Token exp claim is required", response, events.poll());

        jwt = getIdentityProvider().encodeToken(createAuthorizationGrantToken("basic-user-id", oAuthClient.getEndpoints().getIssuer(), IDP_ISSUER, Time.currentTime() - 1L));
        response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        assertFailure("Token is not active", response, events.poll());

        //test max exp default settings
        jwt = getIdentityProvider().encodeToken(createAuthorizationGrantToken("basic-user-id", oAuthClient.getEndpoints().getIssuer(), IDP_ISSUER, Time.currentTime() + 305L));
        response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        assertFailure("Token expiration is too far in the future and iat claim not present in token", response, events.poll());

        //reduce max expiration to 10 seconds
        realm.updateIdentityProvider(IDP_ALIAS, rep -> {
            rep.getConfig().put(OIDCIdentityProviderConfig.JWT_AUTHORIZATION_GRANT_MAX_ALLOWED_ASSERTION_EXPIRATION, "10");
            rep.getConfig().put(OIDCIdentityProviderConfig.JWT_AUTHORIZATION_GRANT_LIMIT_ACCESS_TOKEN_EXP, "false");
        });

        jwt = getIdentityProvider().encodeToken(createAuthorizationGrantToken("basic-user-id", oAuthClient.getEndpoints().getIssuer(), IDP_ISSUER, Time.currentTime() + 11L));
        response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        assertFailure("Token expiration is too far in the future and iat claim not present in token", response, events.poll());

        jwt = getIdentityProvider().encodeToken(createAuthorizationGrantToken("basic-user-id", oAuthClient.getEndpoints().getIssuer(), IDP_ISSUER, Time.currentTime() + 5L));
        response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        assertSuccess("test-app", response);

        //test with iat
        jwt = getIdentityProvider().encodeToken(createAuthorizationGrantToken("basic-user-id", oAuthClient.getEndpoints().getIssuer(), IDP_ISSUER, Time.currentTime() + 20L, (long) Time.currentTime()));
        timeOffSet.set(15);
        response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        assertFailure("Token was issued too far in the past to be used now", response, events.poll());

        jwt = getIdentityProvider().encodeToken(createAuthorizationGrantToken("basic-user-id", oAuthClient.getEndpoints().getIssuer(), IDP_ISSUER, Time.currentTime() + 20L, (long) Time.currentTime()));
        response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        assertSuccess("test-app", response);
    }

    @Test
    public void testAudience() {
        String jwt = getIdentityProvider().encodeToken(createAuthorizationGrantToken("basic-user-id", null, IDP_ISSUER, Time.currentTime() + 300L));
        AccessTokenResponse response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        assertFailure("Invalid token audience", response, events.poll());

        jwt = getIdentityProvider().encodeToken(createAuthorizationGrantToken("basic-user-id", "fake-audience", IDP_ISSUER));
        response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        assertFailure("Invalid token audience", response, events.poll());

        // Issuer as audience works
        jwt = getIdentityProvider().encodeToken(createAuthorizationGrantToken("basic-user-id", oAuthClient.getEndpoints().getIssuer(), IDP_ISSUER));
        response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        assertSuccess("test-app", response);

        // Token endpoint as audience works
        jwt = getIdentityProvider().encodeToken(createAuthorizationGrantToken("basic-user-id", oAuthClient.getEndpoints().getToken(), IDP_ISSUER));
        response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        assertSuccess("test-app", response);

        // Introspection endpoint as audience does not work
        jwt = getIdentityProvider().encodeToken(createAuthorizationGrantToken("basic-user-id", oAuthClient.getEndpoints().getIntrospection(), IDP_ISSUER));
        response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        assertFailure("Invalid token audience", response, events.poll());

        // Multiple audiences does not work
        JsonWebToken jwtToken = createAuthorizationGrantToken("basic-user-id", oAuthClient.getEndpoints().getIssuer(), IDP_ISSUER);
        jwtToken.addAudience("fake");
        jwt = getIdentityProvider().encodeToken(jwtToken);
        response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        assertFailure("Multiple audiences not allowed", response, events.poll());

        // Multiple audiences does not work (even if both are valid)
        jwtToken = createAuthorizationGrantToken("basic-user-id", oAuthClient.getEndpoints().getIssuer(), IDP_ISSUER);
        jwtToken.addAudience(oAuthClient.getEndpoints().getToken());
        jwt = getIdentityProvider().encodeToken(jwtToken);
        response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        assertFailure("Multiple audiences not allowed", response, events.poll());
    }

    @Test
    public void testBadIssuer() {
        String jwt = getIdentityProvider().encodeToken(createAuthorizationGrantToken("basic-user-id", oAuthClient.getEndpoints().getIssuer(), null, Time.currentTime() + 300L));
        AccessTokenResponse response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        assertFailure("Missing claim: " + OAuth2Constants.ISSUER, response, events.poll());

        jwt = getIdentityProvider().encodeToken(createAuthorizationGrantToken("basic-user-id", oAuthClient.getEndpoints().getIssuer(), "fake-issuer", Time.currentTime() + 300L));
        response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        EventAssertion event = assertFailure("No Identity Provider for provided issuer", response, events.poll());
        event.details(Details.IDENTITY_PROVIDER_ISSUER, "fake-issuer");
    }

    @Test
    public void testBadSubject() {
        String jwt = getIdentityProvider().encodeToken(createAuthorizationGrantToken(null, oAuthClient.getEndpoints().getIssuer(), IDP_ISSUER, Time.currentTime() + 300L));
        AccessTokenResponse response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        assertFailure("Missing claim: " + IDToken.SUBJECT, response, events.poll());

        jwt = getIdentityProvider().encodeToken(createAuthorizationGrantToken("fake-user", oAuthClient.getEndpoints().getIssuer(), IDP_ISSUER, Time.currentTime() + 300L));
        response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        EventAssertion event =  assertFailure("User not found", response, events.poll());
        event.details(Details.IDENTITY_PROVIDER, IDP_ALIAS);
        event.details(Details.IDENTITY_PROVIDER_ISSUER, IDP_ISSUER);
        event.details(Details.IDENTITY_PROVIDER_USER_ID, "fake-user");
    }

    @Test
    public void testReplayToken() {
        String jwt = getIdentityProvider().encodeToken(createDefaultAuthorizationGrantToken());
        AccessTokenResponse response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        assertSuccess("test-app", response);

        response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        assertFailure("Token reuse detected", response, events.poll());

        realm.updateIdentityProvider(IDP_ALIAS, rep -> {
            rep.getConfig().put(OIDCIdentityProviderConfig.JWT_AUTHORIZATION_GRANT_ASSERTION_REUSE_ALLOWED, "true");
        });

        jwt = getIdentityProvider().encodeToken(createDefaultAuthorizationGrantToken());
        response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        assertSuccess("test-app", response);

        response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        assertSuccess("test-app", response);
    }

    @Test
    public void testSignatureAlg() {
        realm.updateIdentityProvider(IDP_ALIAS, rep -> {
            rep.getConfig().put(OIDCIdentityProviderConfig.JWT_AUTHORIZATION_GRANT_ASSERTION_SIGNATURE_ALG, Algorithm.ES256);
        });
        String jwt = getIdentityProvider().encodeToken(createDefaultAuthorizationGrantToken());
        AccessTokenResponse response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        assertSuccess("test-app", response);

        realm.updateIdentityProvider(IDP_ALIAS, rep -> {
            rep.getConfig().put(OIDCIdentityProviderConfig.JWT_AUTHORIZATION_GRANT_ASSERTION_SIGNATURE_ALG, Algorithm.ES512);
        });
        jwt = getIdentityProvider().encodeToken(createDefaultAuthorizationGrantToken());
        response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        assertFailure("Invalid signature algorithm", response, events.poll());
    }

    @Test
    public void testInvalidSignature() {
        JsonWebToken token = createDefaultAuthorizationGrantToken();
        OAuthIdentityProvider.OAuthIdentityProviderKeys newKeys = getIdentityProvider().createKeys();
        OAuthIdentityProvider.OAuthIdentityProviderKeys keys = getIdentityProvider().getKeys();
        newKeys.getKeyWrapper().setKid(keys.getKeyWrapper().getKid());
        String jwt = getIdentityProvider().encodeToken(token, newKeys);
        AccessTokenResponse response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        assertFailure("Invalid signature", response, events.poll());
    }

    @Test
    public void testValidateSignatureFixedKey() {
        realm.updateIdentityProvider(IDP_ALIAS, rep -> {
            rep.getConfig().put(OIDCIdentityProviderConfig.USE_JWKS_URL, Boolean.FALSE.toString());
            rep.getConfig().put(OIDCIdentityProviderConfig.JWKS_URL, "");
            rep.getConfig().put(OIDCIdentityProviderConfig.PUBLIC_KEY_SIGNATURE_VERIFIER,
                    PemUtils.encodeKey(identityProvider.getKeys().getKeyWrapper().getPublicKey()));
        });

        String jwt = getIdentityProvider().encodeToken(createAuthorizationGrantToken("basic-user-id", oAuthClient.getEndpoints().getIssuer(), IDP_ISSUER));
        AccessTokenResponse response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        assertSuccess("test-app", response);
    }

    @Test
    public void testValidateSignatureFixedKeyAndKeyId() {
        realm.updateIdentityProvider(IDP_ALIAS, rep -> {
            rep.getConfig().put(OIDCIdentityProviderConfig.USE_JWKS_URL, Boolean.FALSE.toString());
            rep.getConfig().put(OIDCIdentityProviderConfig.JWKS_URL, "");
            rep.getConfig().put(OIDCIdentityProviderConfig.PUBLIC_KEY_SIGNATURE_VERIFIER,
                    PemUtils.encodeKey(identityProvider.getKeys().getKeyWrapper().getPublicKey()));
            rep.getConfig().put(OIDCIdentityProviderConfig.PUBLIC_KEY_SIGNATURE_VERIFIER_KEY_ID,
                    identityProvider.getKeys().getKeyWrapper().getKid());
        });

        String jwt = getIdentityProvider().encodeToken(createAuthorizationGrantToken("basic-user-id", oAuthClient.getEndpoints().getIssuer(), IDP_ISSUER));
        AccessTokenResponse response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        assertSuccess("test-app", response);
    }

    @Test
    public void testValidateSignatureFixedKeyUsingJwks() {
        realm.updateIdentityProvider(IDP_ALIAS, rep -> {
            rep.getConfig().put(OIDCIdentityProviderConfig.USE_JWKS_URL, Boolean.FALSE.toString());
            rep.getConfig().put(OIDCIdentityProviderConfig.JWKS_URL, "");
            rep.getConfig().put(OIDCIdentityProviderConfig.PUBLIC_KEY_SIGNATURE_VERIFIER, identityProvider.getKeys().getJwksString());
        });

        String jwt = getIdentityProvider().encodeToken(createAuthorizationGrantToken("basic-user-id", oAuthClient.getEndpoints().getIssuer(), IDP_ISSUER));
        AccessTokenResponse response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        assertSuccess("test-app", response);
    }

    @Test
    public void testScope() {
        oAuthClient.openid(false).scope("address phone");
        try {
            String jwt = getIdentityProvider().encodeToken(createDefaultAuthorizationGrantToken());
            AccessTokenResponse response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
            AccessToken token = assertSuccess("test-app", response);
            MatcherAssert.assertThat(List.of(token.getScope().split(" ")), Matchers.containsInAnyOrder(new String[]{"email", "profile", "address", "phone"}));

            jwt = getIdentityProvider().encodeToken(createDefaultAuthorizationGrantToken());
            oAuthClient.scope("address phone wrong-scope");
            response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
            assertFailure("invalid_scope", "Invalid scopes: address phone wrong-scope", response, events.poll());
        } finally {
            oAuthClient.openid(true).scope(null);
        }
    }

    @Test
    public void textLimitAccessTokenExpiration() {
        realm.updateIdentityProvider(IDP_ALIAS, rep -> {
            rep.getConfig().put(OIDCIdentityProviderConfig.JWT_AUTHORIZATION_GRANT_LIMIT_ACCESS_TOKEN_EXP, "true");
        });

        int accessCodeLifeSpan = realm.admin().toRepresentation().getAccessTokenLifespan();
        String jwt = getIdentityProvider().encodeToken(createAuthorizationGrantToken(accessCodeLifeSpan));
        AccessTokenResponse response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        MatcherAssert.assertThat(response.getExpiresIn(), Matchers.allOf(Matchers.lessThanOrEqualTo(accessCodeLifeSpan), Matchers.greaterThan(accessCodeLifeSpan - 5)));

        jwt = getIdentityProvider().encodeToken(createAuthorizationGrantToken(120L));
        response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        MatcherAssert.assertThat(response.getExpiresIn(), Matchers.allOf(Matchers.lessThanOrEqualTo(120), Matchers.greaterThan(115)));
    }

    @Test
    public void testSuccessGrant() {
        String jwt = getIdentityProvider().encodeToken(createDefaultAuthorizationGrantToken());
        AccessTokenResponse response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        assertSuccess("test-app", response);
    }

    @Test
    public void testDisabledIdentityProvider() {
        realm.updateIdentityProvider(IDP_ALIAS, rep -> {
            rep.setEnabled(false);
        });

        String jwt = getIdentityProvider().encodeToken(createDefaultAuthorizationGrantToken());
        AccessTokenResponse response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        assertFailure("No Identity Provider for provided issuer", response, events.poll());
    }

    @Test
    public void testUserDisabled() {
        UserRepresentation userRep = user.admin().toRepresentation();
        userRep.setEnabled(false);
        user.admin().update(userRep);

        String jwt = getIdentityProvider().encodeToken(createDefaultAuthorizationGrantToken());
        AccessTokenResponse response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        assertFailure("User is not enabled", response, events.poll());

        userRep.setEnabled(true);
        user.admin().update(userRep);
    }

    @Test
    public void testUserWithRequiredAction() {
        UserRepresentation userRep = user.admin().toRepresentation();
        userRep.setRequiredActions(Collections.singletonList("UPDATE_PASSWORD"));
        user.admin().update(userRep);

        String jwt = getIdentityProvider().encodeToken(createDefaultAuthorizationGrantToken());
        AccessTokenResponse response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        assertFailure("Account is not fully set up", response, events.poll());

        userRep = user.admin().toRepresentation();
        userRep.setRequiredActions(Collections.emptyList());
        user.admin().update(userRep);
    }
}

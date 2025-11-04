package org.keycloak.tests.oauth;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.common.Profile;
import org.keycloak.common.util.Time;
import org.keycloak.events.EventType;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.OAuthIdentityProvider;
import org.keycloak.testframework.oauth.OAuthIdentityProviderConfig;
import org.keycloak.testframework.oauth.OAuthIdentityProviderConfigBuilder;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthIdentityProvider;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.tests.client.authentication.external.ClientAuthIdpServerConfig;
import org.keycloak.testsuite.util.IdentityProviderBuilder;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import java.util.UUID;

@KeycloakIntegrationTest(config = JWTAuthorizationGrantTest.JWTAuthorizationGrantServerConfig.class)
public class JWTAuthorizationGrantTest  {

    private static final String IDP_ALIAS = "authorization-grant-idp";
    private static final String IDP_ISSUER = "authorization-grant://mytrust-domain";

    @InjectOAuthIdentityProvider(config = JWTAuthorizationGrantTest.AGIdpConfig.class)
    OAuthIdentityProvider identityProvider;

    @InjectRealm(config = JWTAuthorizationGranthRealmConfig.class)
    protected ManagedRealm realm;

    @InjectUser(config = FederatedUserConfiguration.class)
    ManagedUser user;

    @InjectOAuthClient
    OAuthClient oAuthClient;

    @InjectEvents
    Events events;


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
    public void testNotAllowedIdentityProvider() {
        realm.updateIdentityProviderWithCleanup(IDP_ALIAS, rep -> {
            rep.getConfig().put(OIDCIdentityProviderConfig.JWT_AUTHORIZATION_GRANT_ENABLED, "false");
        });

        String jwt = getIdentityProvider().encodeToken(createDefaultAuthorizationGrantToken());
        AccessTokenResponse response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        assertFailure("JWT Authorization Granted is not enabled for the identity provider", response, events.poll());
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
    }

    @Test
    public void testBadAudience() {
        String jwt = getIdentityProvider().encodeToken(createAuthorizationGrantToken("basic-user-id", null, IDP_ISSUER, Time.currentTime() + 300L));
        AccessTokenResponse response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        assertFailure("Invalid token audience", response, events.poll());

        jwt = getIdentityProvider().encodeToken(createAuthorizationGrantToken("basic-user-id", "fake-audience", IDP_ISSUER));
        response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        assertFailure("Invalid token audience", response, events.poll());
    }

    @Test
    public void testBadIssuer() {
        String jwt = getIdentityProvider().encodeToken(createAuthorizationGrantToken("basic-user-id", oAuthClient.getEndpoints().getIssuer(), null, Time.currentTime() + 300L));
        AccessTokenResponse response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        assertFailure("Missing claim: " + OAuth2Constants.ISSUER, response, events.poll());

        jwt = getIdentityProvider().encodeToken(createAuthorizationGrantToken("basic-user-id", oAuthClient.getEndpoints().getIssuer(), "fake-issuer", Time.currentTime() + 300L));
        response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        assertFailure("No Identity Provider for provided issuer", response, events.poll());
    }

    @Test
    public void testBadSubject() {
        String jwt = getIdentityProvider().encodeToken(createAuthorizationGrantToken(null, oAuthClient.getEndpoints().getIssuer(), IDP_ISSUER, Time.currentTime() + 300L));
        AccessTokenResponse response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        assertFailure("Missing claim: " + IDToken.SUBJECT, response, events.poll());

        jwt = getIdentityProvider().encodeToken(createAuthorizationGrantToken("fake-user", oAuthClient.getEndpoints().getIssuer(), IDP_ISSUER, Time.currentTime() + 300L));
        response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        assertFailure("User not found", response, events.poll());
    }

    @Test
    public void testReplayToken() {
        String jwt = getIdentityProvider().encodeToken(createDefaultAuthorizationGrantToken());
        AccessTokenResponse response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        assertSuccess("test-app", "basic-user", response);

        response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        assertFailure("Token reuse detected", response, events.poll());

        realm.updateIdentityProviderWithCleanup(IDP_ALIAS, rep -> {
            rep.getConfig().put(OIDCIdentityProviderConfig.JWT_AUTHORIZATION_GRANT_ASSERTION_REUSE_ALLOWED, "true");
        });

        jwt = getIdentityProvider().encodeToken(createDefaultAuthorizationGrantToken());
        response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        assertSuccess("test-app", "basic-user", response);

        response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        assertSuccess("test-app", "basic-user", response);
    }

    @Test
    public void testInvalidSignature() throws Exception {
        JsonWebToken token = createDefaultAuthorizationGrantToken();
        OAuthIdentityProvider.OAuthIdentityProviderKeys newKeys = getIdentityProvider().createKeys();
        OAuthIdentityProvider.OAuthIdentityProviderKeys keys = getIdentityProvider().getKeys();
        newKeys.getKeyWrapper().setKid(keys.getKeyWrapper().getKid());
        String jwt = getIdentityProvider().encodeToken(token, newKeys);
        AccessTokenResponse response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        assertFailure("Invalid signature", response, events.poll());
    }

    @Test
    public void testSuccessGrant() {
        String jwt = getIdentityProvider().encodeToken(createDefaultAuthorizationGrantToken());
        AccessTokenResponse response = oAuthClient.jwtAuthorizationGrantRequest(jwt).send();
        assertSuccess("test-app", "basic-user", response);
    }

    protected JsonWebToken createDefaultAuthorizationGrantToken() {
        return createAuthorizationGrantToken("basic-user-id", oAuthClient.getEndpoints().getIssuer(), IDP_ISSUER, Time.currentTime() + 300L);
    }

    protected JsonWebToken createAuthorizationGrantToken(String subject, String audience, String issuer) {
        return createAuthorizationGrantToken(subject, audience, issuer, Time.currentTime() + 300L);
    }

    protected JsonWebToken createAuthorizationGrantToken(String subject, String audience, String issuer, Long exp) {
        JsonWebToken token = new JsonWebToken();
        token.id(UUID.randomUUID().toString());
        token.subject(subject);
        token.audience(audience);
        token.issuer(issuer);
        token.exp(exp);
        return token;
    }

    public OAuthIdentityProvider getIdentityProvider() {
        return identityProvider;
    }

    public static class AGIdpConfig implements OAuthIdentityProviderConfig {

        @Override
        public OAuthIdentityProviderConfigBuilder configure(OAuthIdentityProviderConfigBuilder config) {
            return config;
        }
    }

    public static class JWTAuthorizationGrantServerConfig extends ClientAuthIdpServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return super.configure(config).features(Profile.Feature.JWT_AUTHORIZATION_GRANT);
        }
    }

    public static class JWTAuthorizationGranthRealmConfig implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {

            realm.addClient("test-public").publicClient(true);

            realm.addClient("authorization-grant-disabled-client").publicClient(false).secret("test-secret");

            realm.addClient("authorization-grant-not-allowed-idp-client").publicClient(false).attribute(OIDCConfigAttributes.JWT_AUTHORIZATION_GRANT_ENABLED, "true").secret("test-secret");

            realm.identityProvider(
                    IdentityProviderBuilder.create()
                            .providerId(OIDCIdentityProviderFactory.PROVIDER_ID)
                            .alias(IDP_ALIAS)
                            .setAttribute(IdentityProviderModel.ISSUER, IDP_ISSUER)
                            .setAttribute(OIDCIdentityProviderConfig.VALIDATE_SIGNATURE, Boolean.TRUE.toString())
                            .setAttribute(OIDCIdentityProviderConfig.JWKS_URL, "http://127.0.0.1:8500/idp/jwks")
                            .setAttribute(OIDCIdentityProviderConfig.USE_JWKS_URL, Boolean.TRUE.toString())
                            .setAttribute(OIDCIdentityProviderConfig.JWT_AUTHORIZATION_GRANT_ENABLED, Boolean.TRUE.toString())
                            .build());
            return realm;
        }
    }

    public static class FederatedUserConfiguration implements UserConfig {

        @Override
        public UserConfigBuilder configure(UserConfigBuilder user) {
            return user.username("basic-user").password("password").email("basic@localhost").name("First", "Last").federatedLink(IDP_ALIAS, "basic-user-id", "basic-user");
        }
    }

    protected void assertSuccess(String expectedClientId, String username, AccessTokenResponse response) {
        Assertions.assertTrue(response.isSuccess());
        AccessToken accessToken = oAuthClient.parseToken(response.getAccessToken(), AccessToken.class);
        Assertions.assertEquals(expectedClientId, accessToken.getIssuedFor());
        Assertions.assertEquals(username, accessToken.getPreferredUsername());
        EventAssertion.assertSuccess(events.poll())
                .type(EventType.LOGIN)
                .clientId(expectedClientId)
                .details("grant_type", OAuth2Constants.JWT_AUTHORIZATION_GRANT)
                .details("username", username);
    }

    protected void assertFailure(String expectedErrorDescription, AccessTokenResponse response, EventRepresentation event) {
        Assertions.assertFalse(response.isSuccess());
        Assertions.assertEquals("invalid_request", response.getError());
        Assertions.assertEquals(expectedErrorDescription, response.getErrorDescription());
        EventAssertion.assertError(event)
                .type(EventType.LOGIN_ERROR)
                .error("invalid_request")
                .details("grant_type", OAuth2Constants.JWT_AUTHORIZATION_GRANT)
                .details("reason", expectedErrorDescription);
    }
}

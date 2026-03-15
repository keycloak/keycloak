package org.keycloak.tests.oauth;

import org.keycloak.OAuth2Constants;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.common.Profile;
import org.keycloak.common.util.Time;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.representations.AccessToken;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testsuite.util.IdentityProviderBuilder;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@KeycloakIntegrationTest(config = IDJWTAuthorizationGrantTest.JWTAuthorizationGrantServerConfig.class)
@TestMethodOrder(MethodOrderer.MethodName.class)
public class IDJWTAuthorizationGrantTest extends BaseAbstractJWTAuthorizationGrantTest {

    @InjectRealm(config = IDJWTAuthorizationGrantTest.JWTAuthorizationGrantRealmConfig.class)
    protected ManagedRealm realm;

    @InjectUser(config = IDJWTAuthorizationGrantTest.FederatedUserConfiguration.class)
    protected ManagedUser user;
  
    @Test
    public void testNullIssInToken() {
        AccessToken jwt = createAuthorizationGrantToken("basic-user-id",oAuthClient.getEndpoints().getIssuer(),IDP_ISSUER,Time.currentTime() + 300L,(long) Time.currentTime(), null);
        jwt.setOtherClaims("client_id","clientid_issued_by_resource_authz_to_client_app");
        jwt.issuer(null);
        String jwtString = getIdentityProvider().encodeIDJAG(jwt);
        AccessTokenResponse response = oAuthClient.client("clientid_issued_by_resource_authz_to_client_app", "test-secret").jwtAuthorizationGrantRequest(jwtString).send();
        assertFailure("Missing claim: iss", response, events.poll());
    }

    @Test
    public void testNullAudInToken() {
        AccessToken jwt = createAuthorizationGrantToken("basic-user-id",oAuthClient.getEndpoints().getIssuer(),IDP_ISSUER,Time.currentTime() + 300L,(long) Time.currentTime(), null);
        jwt.setOtherClaims("client_id","clientid_issued_by_resource_authz_to_client_app");
        jwt.audience(null);
        String jwtString = getIdentityProvider().encodeIDJAG(jwt);
        AccessTokenResponse response = oAuthClient.client("clientid_issued_by_resource_authz_to_client_app", "test-secret").jwtAuthorizationGrantRequest(jwtString).send();
        assertFailure("Invalid token audience", response, events.poll());
    }

    @Test
    public void testNullSubInToken() {
        AccessToken jwt = createAuthorizationGrantToken("basic-user-id",oAuthClient.getEndpoints().getIssuer(),IDP_ISSUER,Time.currentTime() + 300L,(long) Time.currentTime(), null);
        jwt.setOtherClaims("client_id","clientid_issued_by_resource_authz_to_client_app");
        jwt.subject(null);
        String jwtString = getIdentityProvider().encodeIDJAG(jwt);
        AccessTokenResponse response = oAuthClient.client("clientid_issued_by_resource_authz_to_client_app", "test-secret").jwtAuthorizationGrantRequest(jwtString).send();
        assertFailure("Missing claim: sub", response, events.poll());
    }
    
    @Test
    public void testNullExpInToken() {
        AccessToken jwt = createAuthorizationGrantToken("basic-user-id",oAuthClient.getEndpoints().getIssuer(),IDP_ISSUER,Time.currentTime() + 300L,(long) Time.currentTime(), null);
        jwt.setOtherClaims("client_id","clientid_issued_by_resource_authz_to_client_app");
        jwt.exp(null);
        String jwtString = getIdentityProvider().encodeIDJAG(jwt);
        AccessTokenResponse response = oAuthClient.client("clientid_issued_by_resource_authz_to_client_app", "test-secret").jwtAuthorizationGrantRequest(jwtString).send();
        assertFailure("Token exp claim is required", response, events.poll());
    }

    @Test
    public void testNullIatInToken() {
        AccessToken jwt = createAuthorizationGrantToken("basic-user-id",oAuthClient.getEndpoints().getIssuer(),IDP_ISSUER,Time.currentTime() + 300L,(long) Time.currentTime(), null);
        jwt.setOtherClaims("client_id","clientid_issued_by_resource_authz_to_client_app");
        jwt.iat(null);
        String jwtString = getIdentityProvider().encodeIDJAG(jwt);
        AccessTokenResponse response = oAuthClient.client("clientid_issued_by_resource_authz_to_client_app", "test-secret").jwtAuthorizationGrantRequest(jwtString).send();
        assertFailure("Token iat claim is required", response, events.poll());
    }

    @Test
    public void testNullJtiInToken() {
        AccessToken jwt = createAuthorizationGrantToken("basic-user-id",oAuthClient.getEndpoints().getIssuer(),IDP_ISSUER,Time.currentTime() + 300L,(long) Time.currentTime(), null);
        jwt.setOtherClaims("client_id","clientid_issued_by_resource_authz_to_client_app");
        jwt.id(null);
        String jwtString = getIdentityProvider().encodeIDJAG(jwt);
        AccessTokenResponse response = oAuthClient.client("clientid_issued_by_resource_authz_to_client_app", "test-secret").jwtAuthorizationGrantRequest(jwtString).send();
        assertFailure("Token jti claim is required", response, events.poll());
    }

    @Test
    public void testClientInTokenAndInRequestParameter() {
        AccessToken jwt = createAuthorizationGrantToken("basic-user-id",oAuthClient.getEndpoints().getIssuer(),IDP_ISSUER,Time.currentTime() + 300L,(long) Time.currentTime(), null);
        jwt.setOtherClaims("client_id","clientid_issued_by_resource_authz_to_client_app");
        String jwtString = getIdentityProvider().encodeIDJAG(jwt);
        AccessTokenResponse response = oAuthClient.client("test-app", "test-secret").jwtAuthorizationGrantRequest(jwtString).send();
        assertFailure("client id in assertion : clientid_issued_by_resource_authz_to_client_app and client id in request header/body : test-app", response, events.poll());
    }

    @Test
    public void testInvalidTokenAudience() {
        AccessToken jwt = createAuthorizationGrantToken("basic-user-id","fake-audience",IDP_ISSUER,Time.currentTime() + 300L,(long) Time.currentTime(), null);
        jwt.setOtherClaims("client_id","clientid_issued_by_resource_authz_to_client_app");
        String jwtString = getIdentityProvider().encodeIDJAG(jwt);
        AccessTokenResponse response = oAuthClient.client("clientid_issued_by_resource_authz_to_client_app", "test-secret").jwtAuthorizationGrantRequest(jwtString).send();
        assertFailure("Invalid token audience", response, events.poll());
    }

    @Test
    public void testNoClientInToken() {
        AccessToken jwt = createAuthorizationGrantToken("basic-user-id",oAuthClient.getEndpoints().getIssuer(),IDP_ISSUER,Time.currentTime() + 300L,(long) Time.currentTime(), null);
        String jwtString = getIdentityProvider().encodeIDJAG(jwt);
        AccessTokenResponse response = oAuthClient.client("test-app", "test-secret").jwtAuthorizationGrantRequest(jwtString).send();
        assertFailure("client id in assertion : null and client id in request header/body : test-app", response, events.poll());
    }
    
    @Test
    public void testNoAssertion() {
        String jwtString = null;
        AccessTokenResponse response = oAuthClient.client("clientid_issued_by_resource_authz_to_client_app", "test-secret").jwtAuthorizationGrantRequest(jwtString).send();
        assertFailure("Missing parameter:assertion", response, events.poll());
    }
    
    @Test
    public void testNoJWTAssertion() {
        String jwtString = "no-jwt-string";
        AccessTokenResponse response = oAuthClient.client("clientid_issued_by_resource_authz_to_client_app", "test-secret").jwtAuthorizationGrantRequest(jwtString).send();
        assertFailure("The provided assertion is not a valid JWT", response, events.poll());
    }

    @Test
    public void testSuccessGrant() {
        AccessToken jwt = createAuthorizationGrantToken("basic-user-id",oAuthClient.getEndpoints().getIssuer(),IDP_ISSUER,Time.currentTime() + 300L,(long) Time.currentTime(), null);
        jwt.setOtherClaims("client_id","clientid_issued_by_resource_authz_to_client_app");
        String jwtString = getIdentityProvider().encodeIDJAG(jwt);
        AccessTokenResponse response = oAuthClient.client("clientid_issued_by_resource_authz_to_client_app", "test-secret").jwtAuthorizationGrantRequest(jwtString).send();
        assertSuccess("clientid_issued_by_resource_authz_to_client_app", response);
    }

    protected AccessToken assertSuccess(String expectedClientId, AccessTokenResponse response) {
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertNull(response.getRefreshToken());
        AccessToken accessToken = oAuthClient.parseToken(response.getAccessToken(), AccessToken.class);
        Assertions.assertNull(accessToken.getSessionId());
        MatcherAssert.assertThat(accessToken.getId(), Matchers.startsWith("trrtag:"));
        Assertions.assertEquals(expectedClientId, accessToken.getIssuedFor());
        Assertions.assertEquals(user.getUsername(), accessToken.getPreferredUsername());
        EventAssertion.assertSuccess(events.poll())
                .type(EventType.JWT_AUTHORIZATION_GRANT)
                .clientId(expectedClientId)
                .sessionId(null)
                .userId(user.getId())
                .details(Details.GRANT_TYPE, OAuth2Constants.JWT_AUTHORIZATION_GRANT)
                .details(Details.IDENTITY_PROVIDER, IDP_ALIAS)
                .details(Details.IDENTITY_PROVIDER_ISSUER, IDP_ISSUER)
                .details(Details.IDENTITY_PROVIDER_USER_ID, "basic-user-id")
                .details(Details.USERNAME, user.getUsername());
        return accessToken;
    }

    public static class JWTAuthorizationGrantRealmConfig extends AbstractJWTAuthorizationGrantTest.JWTAuthorizationGrantRealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            realm.addClient("clientid_issued_by_resource_authz_to_client_app")
                 .serviceAccountsEnabled(true)
                 .directAccessGrantsEnabled(true)
                 .publicClient(false)
                 .attribute(OIDCConfigAttributes.JWT_AUTHORIZATION_GRANT_ENABLED, "true")
                 .secret("test-secret")
                 .attribute(OIDCConfigAttributes.JWT_AUTHORIZATION_GRANT_IDP, IDP_ALIAS);
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

    public static class FederatedUserConfiguration extends AbstractJWTAuthorizationGrantTest.FederatedUserConfiguration {

        @Override
        public UserConfigBuilder configure(UserConfigBuilder user) {
            return user
                    .username("basic-user")
                    .password("password")
                    .email("basic@localhost")
                    .name("First", "Last")
                    .federatedLink(IDP_ALIAS, "basic-user-id", "basic-user");
        }
    }

    public static class JWTAuthorizationGrantServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.IDENTITY_ASSERTION_JWT);
        }
    }
}

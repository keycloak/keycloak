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
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.tests.client.authentication.external.ClientAuthIdpServerConfig;
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
    public void testClientInTokenAndInRequestPrameter() {
        AccessToken jwt = createAuthorizationGrantToken("basic-user-id",oAuthClient.getEndpoints().getIssuer(),IDP_ISSUER,Time.currentTime() + 300L,(long) Time.currentTime(), null);
        jwt.setOtherClaims("client_id","clientid_issued_by_resource_authz_to_client_app");
        String jwtString = getIdentityProvider().encodeIDJAG(jwt);
        AccessTokenResponse response = oAuthClient.client("test-app", "test-secret").jwtAuthorizationGrantRequest(jwtString).send();
        assertFailure("client id in assertion : clientid_issued_by_resource_authz_to_client_app and client id in request param : test-app", response, events.poll());
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
                .type(EventType.LOGIN)
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

    public static class JWTAuthorizationGrantServerConfig extends ClientAuthIdpServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return super.configure(config).
                features(Profile.Feature.JWT_AUTHORIZATION_GRANT).
                features(Profile.Feature.IDENTITY_ASSERTION_JWT_VALIDATOR);
        }
    }
}

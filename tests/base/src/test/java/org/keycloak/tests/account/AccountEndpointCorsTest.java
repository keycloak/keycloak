package org.keycloak.tests.account;

import java.util.Map;

import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AccountResponse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@KeycloakIntegrationTest
public class AccountEndpointCorsTest {

    private static final String VALID_CORS_URL = "http://localtest.me:8180";
    private static final String INVALID_CORS_URL = "http://invalid.localtest.me:8180";
    private static final String REALM_NAME = "test";

    @InjectRealm(ref = REALM_NAME, config = TestRealmConfig.class)
    protected ManagedRealm realm;

    @InjectOAuthClient(realmRef = REALM_NAME, config = TestOAuthClientConfig.class)
    protected OAuthClient oauth;

    @InjectOAuthClient(ref = "no-audience-client", realmRef = REALM_NAME, config = TestOAuthClientNoAudienceConfig.class)
    protected OAuthClient oauthNoAudience;

    @Test
    public void accountEndpointCorsResponseWhenSessionEnded() throws Exception {
        AccessTokenResponse response = oauth.doPasswordGrantRequest("test-user", "password");
        assertEquals(200, response.getStatusCode());

        String accountUrl = realm.getBaseUrl() + "/account";
        final var accountResponse = oauth
                .accountRequest(response.getAccessToken())
                .endpoint(accountUrl)
                .header("Origin", VALID_CORS_URL)
                .send();
        assertEquals(200, accountResponse.getStatusCode());
        assertCors(accountResponse);

        final var userRep = realm.admin().users().searchByUsername("test-user", true).get(0);
        realm.admin().users().get(userRep.getId()).logout();

        final var accountResponseAfterLogout = oauth
                .accountRequest(response.getAccessToken())
                .endpoint(accountUrl)
                .header("Origin", VALID_CORS_URL)
                .send();
        assertEquals(401, accountResponseAfterLogout.getStatusCode());
        assertCors(accountResponseAfterLogout);
    }

    @Test
    public void accountEndpointCorsResponseWhenInvalidUrl() throws Exception {
        AccessTokenResponse response = oauth.doPasswordGrantRequest("test-user", "password");
        assertEquals(200, response.getStatusCode());

        String accountUrl = realm.getBaseUrl() + "/account";
        final var accountResponse = oauth
                .accountRequest(response.getAccessToken())
                .endpoint(accountUrl)
                .header("Origin", INVALID_CORS_URL)
                .send();
        assertEquals(403, accountResponse.getStatusCode());
        assertNotCors(accountResponse);
    }

    @Test
    public void accountEndpointCorsResponseWhenInvalidAudience() throws Exception {
        AccessTokenResponse response = oauthNoAudience.doPasswordGrantRequest("test-user-no-account-roles", "password");
        assertEquals(200, response.getStatusCode());

        String accountUrl = realm.getBaseUrl() + "/account";
        final var accountResponse = oauthNoAudience
                .accountRequest(response.getAccessToken())
                .endpoint(accountUrl)
                .header("Origin", VALID_CORS_URL)
                .send();
        assertEquals(401, accountResponse.getStatusCode());
        assertCors(accountResponse);
    }

    private static void assertCors(AccountResponse response) {
        assertEquals("true", response.getHeaders().get("Access-Control-Allow-Credentials"));
        assertEquals(VALID_CORS_URL, response.getHeaders().get("Access-Control-Allow-Origin"));
    }

    private static void assertNotCors(AccountResponse response) {
        assertNull(response.getHeaders().get("Access-Control-Allow-Credentials"));
        assertNull(response.getHeaders().get("Access-Control-Allow-Origin"));
        assertNull(response.getHeaders().get("Access-Control-Expose-Headers"));
    }

    static class TestRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm
                    .enabled(true)
                    .users(
                            UserBuilder.create("test-user")
                                    .name("Test", "User")
                                    .email("test-user@localhost")
                                    .emailVerified(true)
                                    .clientRoles("account", "view-profile", "manage-account")
                                    .password("password"),
                            UserBuilder.create("test-user-no-account-roles")
                                    .name("Test", "UserNoRoles")
                                    .email("test-user-no-account-roles@localhost")
                                    .emailVerified(true)
                                    .password("password"));
        }
    }

    static class TestOAuthClientConfig implements ClientConfig {
        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return client
                    .clientId("test-app")
                    .publicClient()
                    .directAccessGrantsEnabled()
                    .webOrigins(VALID_CORS_URL)
                    .protocolMappers(audienceMapper("aud-account", "account"));
        }
    }

    static class TestOAuthClientNoAudienceConfig implements ClientConfig {
        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return client
                    .clientId("test-app-no-audience")
                    .publicClient()
                    .directAccessGrantsEnabled()
                    .webOrigins(VALID_CORS_URL);
        }
    }

    private static ProtocolMapperRepresentation audienceMapper(String name, String includedClientAudience) {
        ProtocolMapperRepresentation mapper = new ProtocolMapperRepresentation();
        mapper.setName(name);
        mapper.setProtocol("openid-connect");
        mapper.setProtocolMapper("oidc-audience-mapper");
        mapper.setConfig(Map.of(
                "included.client.audience", includedClientAudience,
                "id.token.claim", "true",
                "access.token.claim", "true"
        ));
        return mapper;
    }
}

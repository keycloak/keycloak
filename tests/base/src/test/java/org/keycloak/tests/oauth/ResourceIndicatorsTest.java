package org.keycloak.tests.oauth;

import org.keycloak.OAuth2Constants;
import org.keycloak.common.Profile;
import org.keycloak.representations.AccessToken;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.DefaultOAuthClientConfiguration;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientConfigBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest(config = ResourceIndicatorsTest.ResourceIndicatorServerConfig.class)
public class ResourceIndicatorsTest {

    @InjectRealm(config = ResourceIndicatorsRealm.class)
    ManagedRealm realm;

    @InjectOAuthClient(config = OAuthClientConfig.class)
    OAuthClient oauth;

    @Test
    public void testValidTarget() {
        AccessTokenResponse tokenResponse = oauth.passwordGrantRequest("user", "pass").param(OAuth2Constants.RESOURCE, "urn:client:theservice").send();
        Assertions.assertTrue(tokenResponse.isSuccess());

        AccessToken accessToken = oauth.parseToken(tokenResponse.getAccessToken(), AccessToken.class);
        String[] audience = accessToken.getAudience();
        Assertions.assertArrayEquals(new String[] { "theservice" }, audience);
    }

    @Test
    public void testValidTargetByUrl() {
        AccessTokenResponse tokenResponse = oauth.passwordGrantRequest("user", "pass").param(OAuth2Constants.RESOURCE, "https://theservice").send();
        Assertions.assertTrue(tokenResponse.isSuccess());

        AccessToken accessToken = oauth.parseToken(tokenResponse.getAccessToken(), AccessToken.class);
        String[] audience = accessToken.getAudience();
        Assertions.assertArrayEquals(new String[] { "https://theservice" }, audience);
    }

    @Test
    public void testInvalidTarget() {
        AccessTokenResponse tokenResponse = oauth.passwordGrantRequest("user", "pass").param(OAuth2Constants.RESOURCE, "urn:client:somethingelse").send();
        Assertions.assertFalse(tokenResponse.isSuccess());
        Assertions.assertEquals("invalid_target", tokenResponse.getError());
    }

    private static final class ResourceIndicatorsRealm implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            realm.addClient("theservice").attribute("resource_url", "https://theservice");
            realm.clientRoles("theservice", "myrole");

            realm.addUser("user").firstName("user").lastName("user").password("pass").email("the@email.localhost").clientRoles("theservice", "myrole");

            return realm;
        }
    }

    private static final class OAuthClientConfig extends DefaultOAuthClientConfiguration {

        @Override
        public ClientConfigBuilder configure(ClientConfigBuilder client) {
            return super.configure(client).fullScopeEnabled(true);
        }
    }

    protected static final class ResourceIndicatorServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.RESOURCE_INDICATORS);
        }
    }

}

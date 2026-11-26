package org.keycloak.tests.admin.user;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.keycloak.common.Profile;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the native "last successful authentication" tracking added for
 * https://github.com/keycloak/keycloak/issues/52319 (experimental, behind
 * {@link Profile.Feature#USER_LAST_SUCCESSFUL_AUTHENTICATION}): a durable per-user timestamp plus a
 * per-client "last login" attribute compatible with the one previously written by the community
 * keycloak-auditor plugin.
 */
@KeycloakIntegrationTest(config = LastSuccessfulAuthenticationTest.ServerConfig.class)
public class LastSuccessfulAuthenticationTest {

    @InjectRealm
    ManagedRealm realm;

    @InjectOAuthClient
    OAuthClient oauth;

    // METHOD lifecycle so each test gets a fresh, never-logged-in user - otherwise the password-grant
    // test would leave the user's lastSuccessfulAuthentication set for whichever test runs next.
    @InjectUser(lifecycle = LifeCycle.METHOD, config = LastSuccessfulAuthenticationUserConfig.class)
    ManagedUser user;

    @Test
    public void userHasNoLastSuccessfulAuthenticationBeforeFirstLogin() {
        UserRepresentation rep = realm.admin().users().get(user.getId()).toRepresentation();

        assertNull(rep.getLastSuccessfulAuthentication());
    }

    @Test
    public void directGrantLoginRecordsLastSuccessfulAuthenticationAndPerClientAttribute() {
        long before = System.currentTimeMillis();

        AccessTokenResponse response = oauth.doPasswordGrantRequest(user.getUsername(), "password");
        assertEquals(200, response.getStatusCode());
        assertNotNull(response.getAccessToken());

        long after = System.currentTimeMillis();

        UserRepresentation rep = realm.admin().users().get(user.getId()).toRepresentation();

        Long lastSuccessfulAuthentication = rep.getLastSuccessfulAuthentication();
        assertNotNull(lastSuccessfulAuthentication);
        assertTrue(lastSuccessfulAuthentication >= before && lastSuccessfulAuthentication <= after,
                "lastSuccessfulAuthentication should fall within the request's time window");

        Map<String, List<String>> attributes = rep.getAttributes();
        assertNotNull(attributes);
        String perClientAttribute = "aud_usr_last-login_" + oauth.getClientId();
        assertTrue(attributes.containsKey(perClientAttribute),
                "Expected per-client last-login attribute '" + perClientAttribute + "' to be set");

        // ISO-8601 OffsetDateTime string, matching the format/key the community keycloak-auditor plugin
        // uses, so existing plugin deployments can switch to this native implementation without losing data.
        String loginTimeValue = attributes.get(perClientAttribute).get(0);
        assertNotNull(OffsetDateTime.parse(loginTimeValue, DateTimeFormatter.ISO_DATE_TIME));
    }

    public static class LastSuccessfulAuthenticationUserConfig implements UserConfig {
        @Override
        public UserBuilder configure(UserBuilder user) {
            return user.username("last-login-user")
                    .password("password")
                    .email("last-login-user@localhost")
                    .emailVerified(true);
        }
    }

    public static class ServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.USER_LAST_SUCCESSFUL_AUTHENTICATION);
        }
    }
}

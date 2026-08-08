package org.keycloak.tests.oauth;

import org.keycloak.services.validation.Validation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@KeycloakIntegrationTest
public class DirectGrantInputValidationTest {

    @InjectRealm
    ManagedRealm realm;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectUser(config = DirectGrantUserConfig.class)
    ManagedUser user;

    @Test
    public void directGrantRejectsUsernameLongerThanMaxLength() {
        AccessTokenResponse response = doGrant("a".repeat(Validation.MAX_USERNAME_LENGTH + 1), "password");
        assertEquals(400, response.getStatusCode());
        assertEquals("invalid_grant", response.getError());
        assertNull(response.getAccessToken());
    }

    @Test
    public void directGrantAcceptsUsernameLengthAtMaxLength() {
        // A username exactly at the limit passes the length check; the user doesn't exist so we get
        // the standard user-not-found error rather than the over-length rejection.
        AccessTokenResponse response = doGrant("a".repeat(Validation.MAX_USERNAME_LENGTH), "password");
        assertEquals(400, response.getStatusCode());
        assertEquals("invalid_grant", response.getError());
    }

    @Test
    public void directGrantRejectsWhitespaceOnlyUsernameAsMissing() {
        AccessTokenResponse response = doGrant("   ", "password");
        assertEquals(401, response.getStatusCode());
        assertEquals("invalid_request", response.getError());
        assertEquals("Missing parameter: username", response.getErrorDescription());
        assertNull(response.getAccessToken());
    }

    @Test
    public void directGrantAcceptsUsernameWithSurroundingWhitespace() {
        AccessTokenResponse response = doGrant("  validuser  ", "password");
        assertEquals(200, response.getStatusCode());
        assertNotNull(response.getAccessToken());
    }

    @Test
    public void directGrantAcceptsUsernameHappyPath() {
        AccessTokenResponse response = doGrant("validuser", "password");
        assertEquals(200, response.getStatusCode());
        assertNotNull(response.getAccessToken());
    }

    private AccessTokenResponse doGrant(String username, String password) {
        return oauth.doPasswordGrantRequest(username, password);
    }

    public static class DirectGrantUserConfig implements UserConfig {
        @Override
        public UserBuilder configure(UserBuilder user) {
            return user.username("validuser")
                    .password("password")
                    .email("validuser@localhost")
                    .name("Valid", "User")
                    .emailVerified(true);
        }
    }
}

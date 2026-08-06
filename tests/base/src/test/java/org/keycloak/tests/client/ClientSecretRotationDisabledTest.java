package org.keycloak.tests.client;

import org.keycloak.common.util.Time;
import org.keycloak.models.ClientSecretConstants;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.tests.common.TestRealmUserConfig;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@KeycloakIntegrationTest
public class ClientSecretRotationDisabledTest {

    private static final String CLIENT_ID = "test-app";

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectUser(config = TestRealmUserConfig.class)
    ManagedUser user;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    /**
     * Verifies that rotated client secrets are not accepted when the CLIENT_SECRET_ROTATION
     * feature is disabled, even if the rotated secret attributes remain in the database.
     *
     * @see <a href="https://github.com/keycloak/keycloak/issues/50855">Issue #50855</a>
     */
    @Test
    public void rotatedSecretNotAcceptedWhenFeatureDisabled() {
        String originalSecret = oauth.clientResource().getSecret().getValue();

        // Verify the original secret works before rotating
        oauth.client(CLIENT_ID, originalSecret);
        oauth.doLogin(user.getUsername(), "password");
        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse response = oauth.doAccessTokenRequest(code);
        assertEquals(200, response.getStatusCode());
        assertNotNull(response.getAccessToken());

        String newSecret = oauth.clientResource().generateNewSecret().getValue();

        // Set rotated secret attributes directly on the server-side model to bypass the admin
        // API cleanup in ClientResource.update() which always removes rotation info when no
        // rotation executor is active.
        String creationTime = String.valueOf(Time.currentTimeSeconds());
        String expirationTime = String.valueOf(Time.currentTimeSeconds() + 3600);
        runOnServer.run(session -> {
            var realmModel = session.getContext().getRealm();
            var client = realmModel.getClientByClientId(CLIENT_ID);
            client.setAttribute(ClientSecretConstants.CLIENT_ROTATED_SECRET, originalSecret);
            client.setAttribute(ClientSecretConstants.CLIENT_ROTATED_SECRET_CREATION_TIME, creationTime);
            client.setAttribute(ClientSecretConstants.CLIENT_ROTATED_SECRET_EXPIRATION_TIME, expirationTime);
        });

        // Authentication with the rotated (original) secret must fail when feature is disabled
        oauth.client(CLIENT_ID, originalSecret);
        oauth.openLoginForm();
        code = oauth.parseLoginResponse().getCode();
        response = oauth.doAccessTokenRequest(code);
        assertEquals(401, response.getStatusCode());

        // Authentication with the current (new) secret must always work
        oauth.client(CLIENT_ID, newSecret);
        oauth.openLoginForm();
        code = oauth.parseLoginResponse().getCode();
        response = oauth.doAccessTokenRequest(code);
        assertEquals(200, response.getStatusCode());
        assertNotNull(response.getAccessToken());
    }
}

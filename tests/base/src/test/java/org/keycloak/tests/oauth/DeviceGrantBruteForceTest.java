package org.keycloak.tests.oauth;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.resource.AttackDetectionResource;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.OAuth2DeviceCodeModel;
import org.keycloak.models.OAuth2DeviceConfig;
import org.keycloak.models.RealmModel;
import org.keycloak.models.SingleUseObjectProvider;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.device.DeviceAuthorizationResponse;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@KeycloakIntegrationTest
public class DeviceGrantBruteForceTest {

    private static final String CLIENT_ID = "device-bf-client";
    private static final String USERNAME = "device-bf-user";
    private static final String PASSWORD = "password";


    @InjectRealm(config = DeviceBruteForceRealmConfig.class)
    ManagedRealm realm;

    @InjectUser(config = DeviceBruteForceUserConfig.class)
    ManagedUser user;

    @InjectOAuthClient(config = DeviceBruteForceClientConfig.class)
    OAuthClient oauth;

    @InjectRunOnServer
    RunOnServerClient runOnServer;


    @AfterEach
    public void clearBruteForceState() {
        realm.admin().attackDetection().clearBruteForceForUser(user.getId());
    }


    @Test
    public void shouldRejectDeviceTokenEndpointWhenUserIsTemporarilyLocked() {
        oauth.client(CLIENT_ID);
        DeviceAuthorizationResponse response = oauth.device().doDeviceAuthorizationRequest();
        assertEquals(200, response.getStatusCode());
        String device = response.getDeviceCode();
        String userCode = response.getUserCode();

        ServerState state = new ServerState(realm.getName(), CLIENT_ID, USERNAME, device);
        runOnServer.run(session -> {
               RealmModel realmModel = session.realms().getRealmByName(state.realmName);
               session.getContext().setRealm(realmModel);
               UserModel userModel = session.users().getUserByUsername(realmModel, state.username);
               ClientModel clientModel = realmModel.getClientByClientId(state.clientId);

               UserSessionModel userSession  =  session.sessions().createUserSession(null, realmModel, userModel, state.username, "120.0.0.1", "form",
                        false, null, null, UserSessionModel.SessionPersistenceState.PERSISTENT);

                // Create an authenticated client session so the token endpoint can
                // resolve the clientSession after approval
                AuthenticatedClientSessionModel clientSession =  session.sessions().createClientSession(realmModel, clientModel, userSession);
                clientSession.setRedirectUri("http://localhost");


                SingleUseObjectProvider store = session.singleUseObjects();

                Map<String, String> notes = store.get(OAuth2DeviceCodeModel.createKey(state.deviceCode));
                assertNotNull(notes, "Device code notes missing from store for: " + state.deviceCode);

                OAuth2DeviceCodeModel model = OAuth2DeviceCodeModel.fromCache(realmModel, state.deviceCode, notes);
                assertNotNull(model, "Failed to parse model from cache notes");
                OAuth2DeviceCodeModel approved = model.approve(userSession.getId(), null);
                store.replace(approved.serializeKey(), approved.toMap());

        });

        AttackDetectionResource detection = realm.admin().attackDetection();
        oauth.client(CLIENT_ID).doPasswordGrantRequest(USERNAME, "WRONG_PASSWORD");
        awaitNumFailures(detection, user.getId(), 1);
        oauth.client(CLIENT_ID).doPasswordGrantRequest(USERNAME, "WRONG_PASSWORD");
        awaitNumFailures(detection, user.getId(), 2);

        // Confirm the account is brute-force locked but NOT admin-disabled
        // (enabled=true throughout, just as in the CVE reproduction)
        assertNotNull(realm.admin().attackDetection().bruteForceUserStatus(user.getId()).get("disabled"));
        assertEquals(Boolean.TRUE, realm.admin().attackDetection().bruteForceUserStatus(user.getId()).get("disabled"));

        AccessTokenResponse tokenResponse = oauth.client(CLIENT_ID).device().doDeviceTokenRequest(device);

        assertEquals(400, tokenResponse.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_GRANT, tokenResponse.getError());
        assertEquals("Invalid user credentials", tokenResponse.getErrorDescription());
        assertNull(tokenResponse.getAccessToken(), "No access token must be issued to a brute-force-locked user");
        assertNull(tokenResponse.getRefreshToken(), "No refresh token must be issued to a brute-force-locked user");
    }


    //  Helpers

    // Serializable value holder for use inside runOnServer lambdas
    private record ServerState(String realmName, String clientId, String username, String deviceCode)
            implements Serializable {}

    private void awaitNumFailures(AttackDetectionResource detection, String userId, int expected) {
        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(200, TimeUnit.MILLISECONDS)
                .untilAsserted(() ->
                        assertEquals(expected,
                                detection.bruteForceUserStatus(userId).get("numFailures")));
    }


    // Configuration
    private static class DeviceBruteForceRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm
                    .bruteForceProtected(true)
                    .failureFactor(2)
                    .maxTemporaryLockouts(3)
                    .permanentLockout(false)
                    .maxDeltaTimeSeconds(900);
        }
    }

    private static class DeviceBruteForceUserConfig implements UserConfig {

        @Override
        public UserBuilder configure(UserBuilder user) {
            return user.username(USERNAME)
                    .password(PASSWORD)
                    .firstName("Device")
                    .lastName("User")
                    .email("device-user@example.com")
                    .emailVerified(true);
        }
    }

    private static class DeviceBruteForceClientConfig implements ClientConfig {

        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return client.clientId(CLIENT_ID)
                    .publicClient(true)
                    .directAccessGrantsEnabled(true)
                    .attribute(OAuth2DeviceConfig.OAUTH2_DEVICE_AUTHORIZATION_GRANT_ENABLED, "true");
        }
    }
}

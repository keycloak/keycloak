package org.keycloak.tests.oauth;

import java.io.Serializable;
import java.util.Map;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.models.ClientModel;
import org.keycloak.models.OAuth2DeviceCodeModel;
import org.keycloak.models.OAuth2DeviceConfig;
import org.keycloak.models.RealmModel;
import org.keycloak.models.SingleUseObjectProvider;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.PkceGenerator;
import org.keycloak.testsuite.util.oauth.device.DeviceAuthorizationResponse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@KeycloakIntegrationTest
public class DeviceGrantPkceEnforcedClientTest {

    private static final String CLIENT_ID = "device-pkce-client";
    private static final String USERNAME = "device-pkce-user";
    private static final String PASSWORD = "password";

    @InjectRealm
    ManagedRealm realm;

    @InjectUser(config = DevicePkceUserConfig.class)
    ManagedUser user;

    @InjectOAuthClient(config = DevicePkceClientConfig.class)
    OAuthClient oauth;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @Test
    public void deviceCodeIsExchangedWithCodeVerifier() {
        realm.cleanup().add(RealmResource::logoutAll);
        setPkceMethod(OAuth2Constants.PKCE_METHOD_S256);

        PkceGenerator pkce = PkceGenerator.s256();
        DeviceAuthorizationResponse response = oauth.device().deviceAuthorizationRequest().codeChallenge(pkce).send();
        assertEquals(200, response.getStatusCode());
        approve(response.getDeviceCode());

        AccessTokenResponse tokenResponse = oauth.device().deviceTokenRequest(response.getDeviceCode()).codeVerifier(pkce).send();
        assertEquals(200, tokenResponse.getStatusCode());
    }

    @Test
    public void deviceCodeIsRejectedWhenMethodChanged() {
        realm.cleanup().add(RealmResource::logoutAll);
        setPkceMethod(OAuth2Constants.PKCE_METHOD_PLAIN);

        PkceGenerator plain = PkceGenerator.plain();
        DeviceAuthorizationResponse response = oauth.device().deviceAuthorizationRequest().codeChallenge(plain).send();
        assertEquals(200, response.getStatusCode());
        approve(response.getDeviceCode());

        setPkceMethod(OAuth2Constants.PKCE_METHOD_S256);
        AccessTokenResponse tokenResponse = oauth.device().deviceTokenRequest(response.getDeviceCode()).codeVerifier(plain).send();
        assertEquals(400, tokenResponse.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_GRANT, tokenResponse.getError());
        assertEquals("Invalid parameter: code challenge method is not matching the configured one", tokenResponse.getErrorDescription());
    }

    @Test
    public void deviceCodeWithoutCodeChallengeIsRejectedWhenPkceEnabled() {
        realm.cleanup().add(RealmResource::logoutAll);
        setPkceMethod(null);

        DeviceAuthorizationResponse response = oauth.device().doDeviceAuthorizationRequest();
        assertEquals(200, response.getStatusCode());
        approve(response.getDeviceCode());

        setPkceMethod(OAuth2Constants.PKCE_METHOD_S256);
        AccessTokenResponse tokenResponse = oauth.device().doDeviceTokenRequest(response.getDeviceCode());
        assertEquals(400, tokenResponse.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_GRANT, tokenResponse.getError());
        assertEquals("Missing parameter: code_challenge_method", tokenResponse.getErrorDescription());
    }

    private void setPkceMethod(String method) {
        ClientRepresentation client = oauth.clientResource().toRepresentation();
        client.getAttributes().put(OIDCConfigAttributes.PKCE_CODE_CHALLENGE_METHOD, method == null ? "" : method);
        oauth.clientResource().update(client);
    }

    // approves the device code on the server, as the user would on the verification page
    private void approve(String deviceCode) {
        ServerState state = new ServerState(realm.getName(), CLIENT_ID, USERNAME, deviceCode);
        runOnServer.run(session -> {
            RealmModel realmModel = session.realms().getRealmByName(state.realmName);
            session.getContext().setRealm(realmModel);
            UserModel userModel = session.users().getUserByUsername(realmModel, state.username);
            ClientModel clientModel = realmModel.getClientByClientId(state.clientId);

            UserSessionModel userSession = session.sessions().createUserSession(null, realmModel, userModel, state.username, "127.0.0.1", "form",
                    false, null, null, UserSessionModel.SessionPersistenceState.PERSISTENT);
            session.sessions().createClientSession(realmModel, clientModel, userSession).setRedirectUri("http://localhost");

            SingleUseObjectProvider store = session.singleUseObjects();
            Map<String, String> notes = store.get(OAuth2DeviceCodeModel.createKey(state.deviceCode));
            assertNotNull(notes, "Device code notes missing from store for: " + state.deviceCode);

            OAuth2DeviceCodeModel approved = OAuth2DeviceCodeModel.fromCache(realmModel, state.deviceCode, notes).approve(userSession.getId(), null);
            store.replace(approved.serializeKey(), approved.toMap());
        });
    }

    private record ServerState(String realmName, String clientId, String username, String deviceCode)
            implements Serializable {}

    private static class DevicePkceUserConfig implements UserConfig {
        @Override
        public UserBuilder configure(UserBuilder user) {
            return user.username(USERNAME)
                    .password(PASSWORD)
                    .name("Device", "Pkce")
                    .email("device-pkce-user@localhost")
                    .emailVerified(true);
        }
    }

    private static class DevicePkceClientConfig implements ClientConfig {
        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return client.clientId(CLIENT_ID)
                    .publicClient()
                    .attribute(OAuth2DeviceConfig.OAUTH2_DEVICE_AUTHORIZATION_GRANT_ENABLED, "true");
        }
    }
}

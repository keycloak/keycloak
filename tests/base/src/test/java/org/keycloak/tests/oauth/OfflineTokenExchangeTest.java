package org.keycloak.tests.oauth;

import java.util.Map;

import jakarta.persistence.EntityManager;

import org.keycloak.OAuth2Constants;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.common.Profile;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.jpa.session.JpaSessionUtil;
import org.keycloak.models.jpa.session.PersistentClientSessionEntity;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.authorization.ClientPolicyRepresentation;
import org.keycloak.services.resources.admin.fgap.AdminPermissionManagement;
import org.keycloak.services.resources.admin.fgap.AdminPermissions;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.util.TokenUtil;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Reproducer for https://github.com/keycloak/keycloak/issues/52139 via the V1 token-exchange path.
 * <p>
 * V1 token exchange calls {@code TokenManager.attachAuthenticationSession} directly with the offline
 * user session (unlike V2 which wraps it in a transient session first). When the requester client
 * has no existing session on the offline user session, {@code createClientSession} is invoked with
 * the offline session as the user session. Before the fix, the resulting
 * {@code PersistentClientSessionEntity} was stored with {@code offline="0"} (online), making it
 * invisible to the offline session and orphaning it once the online session expired.
 */
@KeycloakIntegrationTest(config = OfflineTokenExchangeTest.TokenExchangeServerConfig.class)
public class OfflineTokenExchangeTest {

    private static final String OFFLINE_CLIENT_ID = "offline-client";
    private static final String TEST_APP_CLIENT_ID = "test-app";
    private static final String TARGET_CLIENT_ID = "target-client";
    private static final String OFFLINE_CLIENT_APP_URI = "http://localhost:8080/offline-client";

    @InjectRealm(config = OfflineTokenExchangeTest.OfflineTokenExchangeRealmConfig.class)
    ManagedRealm realm;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    /**
     * V1 token exchange with an offline source session triggers {@code createClientSession} with the
     * offline user session directly. Verifies the resulting client session is stored as offline
     * ({@code offline="1"}) and not as online ({@code offline="0"}) in the database.
     */
    @Test
    public void testClientSessionStoredAsOfflineDuringTokenExchange() {
        oauth.client(OFFLINE_CLIENT_ID, "secret1");
        oauth.redirectUri(OFFLINE_CLIENT_APP_URI);
        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        oauth.doLogin("test-user@localhost", "password");

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);

        assertEquals(200, tokenResponse.getStatusCode());
        RefreshToken offlineToken = oauth.parseRefreshToken(tokenResponse.getRefreshToken());
        assertEquals(TokenUtil.TOKEN_TYPE_OFFLINE, offlineToken.getType());
        String sessionId = offlineToken.getSessionState();
        String accessToken = tokenResponse.getAccessToken();

        // V1 token exchange: test-app (which is in the audience of offline-client's token) exchanges
        // the access token targeting itself. This calls attachAuthenticationSession with the offline
        // user session, which in turn calls createClientSession(realm, test-app, offlineUserSession).
        AccessTokenResponse exchangeResponse = oauth.tokenExchangeRequest(accessToken)
                .client(TEST_APP_CLIENT_ID, "test-secret")
                .audience(TEST_APP_CLIENT_ID)
                .send();
        assertEquals(200, exchangeResponse.getStatusCode());
        // V1 defaults requested_token_type to REFRESH_TOKEN_TYPE and the offline session is persistent,
        // so a refresh token must be returned. Any fix must preserve this behaviour.
        assertNotNull(exchangeResponse.getRefreshToken(), "V1 token exchange must return a refresh token");

        // Verify: the test-app client session on the offline user session must be stored as offline="1".
        // Before the fix, createClientSession wrote offline="0", orphaning the client session.
        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName("test");
            assertNotNull(session.sessions().getOfflineUserSession(realm, sessionId), "Offline user session not found");

            ClientModel testAppClient = realm.getClientByClientId(TEST_APP_CLIENT_ID);
            EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();

            PersistentClientSessionEntity onlineEntry = em.find(PersistentClientSessionEntity.class,
                    new PersistentClientSessionEntity.Key(sessionId, testAppClient.getId(),
                            PersistentClientSessionEntity.LOCAL, PersistentClientSessionEntity.LOCAL,
                            JpaSessionUtil.offlineToString(false)));
            assertNull(onlineEntry, "createClientSession must not write an online (offline=0) DB entry for an offline session");

            PersistentClientSessionEntity offlineEntry = em.find(PersistentClientSessionEntity.class,
                    new PersistentClientSessionEntity.Key(sessionId, testAppClient.getId(),
                            PersistentClientSessionEntity.LOCAL, PersistentClientSessionEntity.LOCAL,
                            JpaSessionUtil.offlineToString(true)));
            assertNotNull(offlineEntry, "createClientSession must write an offline (offline=1) DB entry for an offline session");
        });

        // Use the refresh token returned by the exchange to refresh: the client session must be
        // reachable from the offline session, otherwise this refresh will fail with INVALID_GRANT.
        oauth.scope(null);
        oauth.client(TEST_APP_CLIENT_ID, "test-secret");
        AccessTokenResponse refreshResponse = oauth.doRefreshTokenRequest(exchangeResponse.getRefreshToken());
        assertEquals(200, refreshResponse.getStatusCode());
    }

    /**
     * Exercises the requester-client path (lines 341–343 in V1TokenExchangeProvider): when the
     * requester and target clients differ, the V1 exchange creates a client session for both.
     * Verifies that both sessions are stored as offline (offline=1) in the database.
     */
    @Test
    public void testRequesterClientSessionStoredAsOfflineDuringTokenExchange() {
        oauth.client(OFFLINE_CLIENT_ID, "secret1");
        oauth.redirectUri(OFFLINE_CLIENT_APP_URI);
        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        oauth.doLogin("test-user@localhost", "password");

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);

        assertEquals(200, tokenResponse.getStatusCode());
        String sessionId = oauth.parseRefreshToken(tokenResponse.getRefreshToken()).getSessionState();
        String accessToken = tokenResponse.getAccessToken();

        // Grant test-app permission to exchange tokens to target-client via fine-grained admin permissions.
        runOnServer.run(session -> {
            RealmModel r = session.realms().getRealmByName("test");
            ClientModel requesterClient = r.getClientByClientId(TEST_APP_CLIENT_ID);
            ClientModel targetClient = r.getClientByClientId(TARGET_CLIENT_ID);

            ClientPolicyRepresentation policyRep = new ClientPolicyRepresentation();
            policyRep.setName("allow-test-app-to-target-client");
            policyRep.addClient(requesterClient.getId());

            AdminPermissionManagement management = AdminPermissions.management(session, r);
            management.clients().setPermissionsEnabled(targetClient, true);
            ResourceServer server = management.realmResourceServer();
            Policy policy = management.authz().getStoreFactory().getPolicyStore().create(server, policyRep);
            management.clients().exchangeToPermission(targetClient).addAssociatedPolicy(policy);
        });

        // V1 token exchange: test-app (requester) exchanges to target-client (target).
        // Since requester != target, attachAuthenticationSession is called once for target-client
        // and once for test-app (requester path). Both must be stored as offline.
        AccessTokenResponse exchangeResponse = oauth.tokenExchangeRequest(accessToken)
                .client(TEST_APP_CLIENT_ID, "test-secret")
                .audience(TARGET_CLIENT_ID)
                .send();
        assertEquals(200, exchangeResponse.getStatusCode());
        assertNotNull(exchangeResponse.getRefreshToken(), "V1 token exchange must return a refresh token");

        runOnServer.run(session -> {
            RealmModel r = session.realms().getRealmByName("test");
            EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();

            ClientModel targetClient = r.getClientByClientId(TARGET_CLIENT_ID);
            PersistentClientSessionEntity targetOnline = em.find(PersistentClientSessionEntity.class,
                    new PersistentClientSessionEntity.Key(sessionId, targetClient.getId(),
                            PersistentClientSessionEntity.LOCAL, PersistentClientSessionEntity.LOCAL,
                            JpaSessionUtil.offlineToString(false)));
            assertNull(targetOnline, "target-client must not have an online (offline=0) DB entry");
            PersistentClientSessionEntity targetOffline = em.find(PersistentClientSessionEntity.class,
                    new PersistentClientSessionEntity.Key(sessionId, targetClient.getId(),
                            PersistentClientSessionEntity.LOCAL, PersistentClientSessionEntity.LOCAL,
                            JpaSessionUtil.offlineToString(true)));
            assertNotNull(targetOffline, "target-client must have an offline (offline=1) DB entry");

            ClientModel requesterClient = r.getClientByClientId(TEST_APP_CLIENT_ID);
            PersistentClientSessionEntity requesterOnline = em.find(PersistentClientSessionEntity.class,
                    new PersistentClientSessionEntity.Key(sessionId, requesterClient.getId(),
                            PersistentClientSessionEntity.LOCAL, PersistentClientSessionEntity.LOCAL,
                            JpaSessionUtil.offlineToString(false)));
            assertNull(requesterOnline, "test-app (requester) must not have an online (offline=0) DB entry");
            PersistentClientSessionEntity requesterOffline = em.find(PersistentClientSessionEntity.class,
                    new PersistentClientSessionEntity.Key(sessionId, requesterClient.getId(),
                            PersistentClientSessionEntity.LOCAL, PersistentClientSessionEntity.LOCAL,
                            JpaSessionUtil.offlineToString(true)));
            assertNotNull(requesterOffline, "test-app (requester) must have an offline (offline=1) DB entry");
        });

        oauth.scope(null);
        oauth.client(TEST_APP_CLIENT_ID, "test-secret");
        AccessTokenResponse refreshResponse = oauth.doRefreshTokenRequest(exchangeResponse.getRefreshToken());
        assertEquals(200, refreshResponse.getStatusCode());
    }

    public static class TokenExchangeServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            config.features(Profile.Feature.TOKEN_EXCHANGE, Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ);
            return config;
        }
    }

    public static class OfflineTokenExchangeRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder builder) {
            builder.name("test");

            // offline-client is the subject client; it includes test-app in the access token audience
            // so that test-app can perform a V1 self-exchange without needing fine-grained permissions.
            builder.clients(ClientBuilder.create(OFFLINE_CLIENT_ID)
                    .secret("secret1")
                    .redirectUris(OFFLINE_CLIENT_APP_URI)
                    .protocolMappers(createAudienceMapper("test-app-audience", TEST_APP_CLIENT_ID)));

            // target-client is used as the exchange target when testing the requester path
            // (requester = test-app, target = target-client). Fine-grained exchange permissions
            // gate this path; no audience mapper is needed.
            builder.clients(ClientBuilder.create(TARGET_CLIENT_ID)
                    .secret("target-secret")
                    .redirectUris("http://localhost:8080/target-client"));

            builder.users(UserBuilder.create("test-user@localhost")
                    .name("Test", "User")
                    .email("test-user@localhost")
                    .emailVerified(true)
                    .password("password")
                    .realmRoles("user", "offline_access"));

            return builder;
        }

        private ProtocolMapperRepresentation createAudienceMapper(String name, String audience) {
            ProtocolMapperRepresentation mapper = new ProtocolMapperRepresentation();
            mapper.setName(name);
            mapper.setProtocol("openid-connect");
            mapper.setProtocolMapper("oidc-audience-mapper");
            mapper.setConfig(Map.of("included.client.audience", audience, "access.token.claim", "true"));
            return mapper;
        }
    }
}

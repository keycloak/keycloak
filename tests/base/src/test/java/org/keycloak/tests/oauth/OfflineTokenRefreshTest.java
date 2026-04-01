package org.keycloak.tests.oauth;

import java.io.IOException;

import jakarta.ws.rs.NotFoundException;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testframework.admin.AdminClientFactory;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectAdminClientFactory;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ClientConfigBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.remote.providers.timeoffset.InfinispanTimeUtil;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.tests.utils.admin.AdminApiUtil;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.IntrospectionResponse;
import org.keycloak.util.TokenUtil;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.tests.utils.admin.AdminApiUtil.findUserByUsername;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class OfflineTokenRefreshTest {

    private static final String OFFLINE_CLIENT_ID = "offline-client";
    private static final String OFFLINE_CLIENT_APP_URI = "http://localhost:8080/offline-client";
    private static final String TEST_APP_REDIRECT_URI = "http://localhost:8080/auth/realms/test/app/auth";
    private String userId;

    @InjectRealm(config = OfflineTokenRefreshTest.OfflineTokenRealmConfig.class)
    ManagedRealm realm;

    @InjectOAuthClient(config = OfflineTokenRefreshTest.OfflineAuthClientConfig.class)
    OAuthClient oauth;

    @InjectEvents
    Events events;

    @InjectAdminClient
    Keycloak adminClient;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @InjectTimeOffSet
    TimeOffSet timeOffSet;

    @InjectAdminClientFactory
    AdminClientFactory adminClientFactory;

    @BeforeEach
    public void clientConfiguration() {

        timeOffSet.set(0);

        // Reset OAuth client config to defaults
        oauth.realm("test");
        oauth.client("test-app");  // Reset to default client
        oauth.redirectUri(TEST_APP_REDIRECT_URI);  // Reset to default redirect
        oauth.scope(null);  // Clear any scope
        oauth.responseType(OAuth2Constants.CODE);  // Reset to default

        // Force server-side logout
        try {
            adminClient.realm("test").logoutAll();
        } catch (NotFoundException e) {
            // Expected behavior on the first run if the realm/sessions don't exist yet. Safe to ignore.
        }

        userId = findUserByUsername(adminClient.realm("test"), "test-user@localhost").getId();

        events.clear();
    }

    @AfterEach
    public void cleanup() {
        // Reset time offset
        timeOffSet.set(0);

        // Clear events
        events.clear();
    }

    @Test
    public void refreshTokenUserClientMaxLifespanSmallerThanSession() {
        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        oauth.client("offline-client", "secret1");
        oauth.redirectUri(OFFLINE_CLIENT_APP_URI);

        int[] prev = changeOfflineSessionSettings(true, 3600, 7200, 1000, 7200);
        runOnServer.run(InfinispanTimeUtil.enableTestingTimeService());
        try {
            oauth.doLogin("test-user@localhost", "password");
            EventRepresentation loginEvent = events.poll();
            EventAssertion.assertSuccess(loginEvent)
                    .type(EventType.LOGIN)
                    .clientId("offline-client")
                    .details(Details.REDIRECT_URI, OFFLINE_CLIENT_APP_URI);

            String sessionId = loginEvent.getSessionId();

            String code = oauth.parseLoginResponse().getCode();
            AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);
            assertEquals(TokenUtil.TOKEN_TYPE_OFFLINE, oauth.parseRefreshToken(tokenResponse.getRefreshToken()).getType());
            assertTrue(0 < tokenResponse.getRefreshExpiresIn() && tokenResponse.getRefreshExpiresIn() <= 1000, "Invalid ExpiresIn");
            String clientSessionId = getOfflineClientSessionUuid(sessionId, loginEvent.getClientId());
            assertEquals(2, checkIfUserAndClientSessionExist(sessionId, loginEvent.getClientId(), clientSessionId));

            events.poll();

            timeOffSet.set(600);
            String refreshId = oauth.parseRefreshToken(tokenResponse.getRefreshToken()).getId();
            tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());
            assertEquals(TokenUtil.TOKEN_TYPE_OFFLINE, oauth.parseRefreshToken(tokenResponse.getRefreshToken()).getType());
            assertTrue(0 < tokenResponse.getRefreshExpiresIn() && tokenResponse.getRefreshExpiresIn() <= 400, "Invalid ExpiresIn");
            assertEquals(2, checkIfUserAndClientSessionExist(sessionId, loginEvent.getClientId(), clientSessionId));
            EventRepresentation refreshEvent = events.poll();
            EventAssertion.assertSuccess(refreshEvent)
                    .type(EventType.REFRESH_TOKEN)
                    .clientId("offline-client")
                    .sessionId(sessionId)
                    .details(Details.REFRESH_TOKEN_ID, refreshId)
                    .details(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_OFFLINE);

            timeOffSet.set(1100);
            tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());
            assertEquals(400, tokenResponse.getStatusCode());
            assertNull(tokenResponse.getAccessToken());
            assertNull(tokenResponse.getRefreshToken());
            EventRepresentation errorEvent = events.poll();
            EventAssertion.assertError(errorEvent)
                    .type(EventType.REFRESH_TOKEN_ERROR)
                    .clientId("offline-client")
                    .error(Errors.INVALID_TOKEN);
            assertEquals(1, checkIfUserAndClientSessionExist(sessionId, loginEvent.getClientId(), clientSessionId));
        } finally {
            changeOfflineSessionSettings(false, prev[0], prev[1], prev[2], prev[3]);
            runOnServer.run(InfinispanTimeUtil.disableTestingTimeService());
            events.clear();
            timeOffSet.set(0);
        }
    }

    @Test
    public void refreshTokenUserClientMaxLifespanGreaterThanSession() {
        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        oauth.client("offline-client", "secret1");
        oauth.redirectUri(OFFLINE_CLIENT_APP_URI);

        int[] prev = changeOfflineSessionSettings(true, 3600, 7200, 5000, 7200);
        runOnServer.run(InfinispanTimeUtil.enableTestingTimeService());
        try {
            oauth.doLogin("test-user@localhost", "password");
            EventRepresentation loginEvent = events.poll();
            EventAssertion.assertSuccess(loginEvent)
                    .type(EventType.LOGIN)
                    .clientId("offline-client")
                    .details(Details.REDIRECT_URI, OFFLINE_CLIENT_APP_URI);

            String sessionId = loginEvent.getSessionId();

            String code = oauth.parseLoginResponse().getCode();
            AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);
            assertEquals(TokenUtil.TOKEN_TYPE_OFFLINE, oauth.parseRefreshToken(tokenResponse.getRefreshToken()).getType());
            assertTrue(0 < tokenResponse.getRefreshExpiresIn() && tokenResponse.getRefreshExpiresIn() <= 3600, "Invalid ExpiresIn");
            String clientSessionId = getOfflineClientSessionUuid(sessionId, loginEvent.getClientId());
            assertEquals(2, checkIfUserAndClientSessionExist(sessionId, loginEvent.getClientId(), clientSessionId));

            events.poll();

            timeOffSet.set(1800);
            String refreshId = oauth.parseRefreshToken(tokenResponse.getRefreshToken()).getId();
            tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());
            assertEquals(TokenUtil.TOKEN_TYPE_OFFLINE, oauth.parseRefreshToken(tokenResponse.getRefreshToken()).getType());
            assertTrue(0 < tokenResponse.getRefreshExpiresIn() && tokenResponse.getRefreshExpiresIn() <= 1800, "Invalid ExpiresIn");
            assertEquals(2, checkIfUserAndClientSessionExist(sessionId, loginEvent.getClientId(), clientSessionId));
            EventRepresentation refreshEvent2 = events.poll();
            EventAssertion.assertSuccess(refreshEvent2)
                    .type(EventType.REFRESH_TOKEN)
                    .clientId("offline-client")
                    .sessionId(sessionId)
                    .details(Details.REFRESH_TOKEN_ID, refreshId)
                    .details(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_OFFLINE);

            timeOffSet.set(3700);
            tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());
            assertEquals(400, tokenResponse.getStatusCode());
            assertNull(tokenResponse.getAccessToken());
            assertNull(tokenResponse.getRefreshToken());
            EventRepresentation errorEvent2 = events.poll();
            EventAssertion.assertError(errorEvent2)
                    .type(EventType.REFRESH_TOKEN_ERROR)
                    .clientId("offline-client")
                    .error(Errors.INVALID_TOKEN);
            assertEquals(0, checkIfUserAndClientSessionExist(sessionId, loginEvent.getClientId(), clientSessionId));
        } finally {
            changeOfflineSessionSettings(false, prev[0], prev[1], prev[2], prev[3]);
            runOnServer.run(InfinispanTimeUtil.disableTestingTimeService());
            events.clear();
            timeOffSet.set(0);
        }
    }

    @Test
    public void refreshTokenUserSessionMaxLifespanModifiedAfterTokenRefresh() {
        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        oauth.client("offline-client", "secret1");
        oauth.redirectUri(OFFLINE_CLIENT_APP_URI);

        RealmResource realmResource = adminClient.realm("test");
        runOnServer.run(InfinispanTimeUtil.enableTestingTimeService());

        int[] prev = changeOfflineSessionSettings(true, 7200, 7200, 7200, 7200);
        try {
            oauth.doLogin("test-user@localhost", "password");
            EventRepresentation loginEvent = events.poll();
            EventAssertion.assertSuccess(loginEvent)
                    .type(EventType.LOGIN)
                    .clientId("offline-client")
                    .details(Details.REDIRECT_URI, OFFLINE_CLIENT_APP_URI);

            String sessionId = loginEvent.getSessionId();

            String code = oauth.parseLoginResponse().getCode();
            AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);
            assertEquals(TokenUtil.TOKEN_TYPE_OFFLINE, oauth.parseRefreshToken(tokenResponse.getRefreshToken()).getType());
            assertTrue(0 < tokenResponse.getRefreshExpiresIn() && tokenResponse.getRefreshExpiresIn() <= 7200, "Invalid ExpiresIn");
            String clientSessionId = getOfflineClientSessionUuid(sessionId, loginEvent.getClientId());
            assertEquals(2, checkIfUserAndClientSessionExist(sessionId, loginEvent.getClientId(), clientSessionId));

            events.poll();

            RealmRepresentation rep = realmResource.toRepresentation();
            rep.setOfflineSessionMaxLifespan(3600);
            realmResource.update(rep);

            timeOffSet.set(3700);
            tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());
            assertEquals(400, tokenResponse.getStatusCode());
            assertNull(tokenResponse.getAccessToken());
            assertNull(tokenResponse.getRefreshToken());
            EventRepresentation errorEvent3 = events.poll();
            EventAssertion.assertError(errorEvent3)
                    .type(EventType.REFRESH_TOKEN_ERROR)
                    .clientId("offline-client")
                    .error(Errors.INVALID_TOKEN)
                    .sessionId(sessionId)
                    .details(Details.REFRESH_TOKEN_SUB, loginEvent.getUserId());
            assertEquals(0, checkIfUserAndClientSessionExist(sessionId, loginEvent.getClientId(), clientSessionId));
        } finally {
            changeOfflineSessionSettings(false, prev[0], prev[1], prev[2], prev[3]);
            runOnServer.run(InfinispanTimeUtil.disableTestingTimeService());
            events.clear();
            timeOffSet.set(0);
        }
    }

    @Test
    public void refreshTokenClientSessionMaxLifespanModifiedAfterTokenRefresh() {
        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        oauth.client("offline-client", "secret1");
        oauth.redirectUri(OFFLINE_CLIENT_APP_URI);

        RealmResource realmResource = adminClient.realm("test");
        runOnServer.run(InfinispanTimeUtil.enableTestingTimeService());

        int[] prev = changeOfflineSessionSettings(true, 7200, 7200, 7200, 7200);
        try {
            oauth.doLogin("test-user@localhost", "password");
            EventRepresentation loginEvent = events.poll();
            EventAssertion.assertSuccess(loginEvent)
                    .type(EventType.LOGIN)
                    .clientId("offline-client")
                    .details(Details.REDIRECT_URI, OFFLINE_CLIENT_APP_URI);

            String sessionId = loginEvent.getSessionId();

            String code = oauth.parseLoginResponse().getCode();
            AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);
            assertEquals(TokenUtil.TOKEN_TYPE_OFFLINE, oauth.parseRefreshToken(tokenResponse.getRefreshToken()).getType());
            assertTrue(0 < tokenResponse.getRefreshExpiresIn() && tokenResponse.getRefreshExpiresIn() <= 7200, "Invalid ExpiresIn");
            String clientSessionId = getOfflineClientSessionUuid(sessionId, loginEvent.getClientId());
            assertEquals(2, checkIfUserAndClientSessionExist(sessionId, loginEvent.getClientId(), clientSessionId));

            events.poll();

            RealmRepresentation rep = realmResource.toRepresentation();
            rep.setClientOfflineSessionMaxLifespan(3600);
            realmResource.update(rep);

            timeOffSet.set(3700);
            tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());
            assertEquals(400, tokenResponse.getStatusCode());
            assertNull(tokenResponse.getAccessToken());
            assertNull(tokenResponse.getRefreshToken());
            EventRepresentation errorEvent4 = events.poll();
            EventAssertion.assertError(errorEvent4)
                    .type(EventType.REFRESH_TOKEN_ERROR)
                    .clientId("offline-client")
                    .error(Errors.INVALID_TOKEN)
                    .sessionId(sessionId);
            assertEquals(1, checkIfUserAndClientSessionExist(sessionId, loginEvent.getClientId(), clientSessionId));
        } finally {
            changeOfflineSessionSettings(false, prev[0], prev[1], prev[2], prev[3]);
            runOnServer.run(InfinispanTimeUtil.disableTestingTimeService());
            events.clear();
            timeOffSet.set(0);
        }
    }

    @Test
    public void offlineTokenRefreshWithoutOfflineAccessScope() {
        ClientResource offlineClientResource = AdminApiUtil.findClientByClientId(adminClient.realm("test"), "offline-client");
        ClientRepresentation clientRep = offlineClientResource.toRepresentation();
        clientRep.setFullScopeAllowed(false);
        offlineClientResource.update(clientRep);
        try {
            oauth.scope("openid " + OAuth2Constants.OFFLINE_ACCESS);
            oauth.client("offline-client", "secret1");
            oauth.redirectUri(OFFLINE_CLIENT_APP_URI);
            oauth.doLogin("test-user@localhost", "password");
            String code = oauth.parseLoginResponse().getCode();
            AccessTokenResponse response = oauth.doAccessTokenRequest(code);

            oauth.scope("openid");
            response = oauth.doRefreshTokenRequest(response.getRefreshToken());
            assertEquals(200, response.getStatusCode());

            AccessToken token = oauth.verifyToken(response.getAccessToken());
            // access token scope does not contain offline_access due to luck of it in scope request parameter
            assertFalse(token.getScope().contains(OAuth2Constants.OFFLINE_ACCESS));
            RefreshToken offlineToken = oauth.parseRefreshToken(response.getRefreshToken());
            // refresh token scope are always equal to original refresh token scope
            Assertions.assertEquals(TokenUtil.TOKEN_TYPE_OFFLINE, offlineToken.getType());
            assertTrue(offlineToken.getScope().contains(OAuth2Constants.OFFLINE_ACCESS));
        }
        finally {
            ClientResource offlineClientResource1 = AdminApiUtil.findClientByClientId(adminClient.realm("test"), "offline-client");
            ClientRepresentation clientRep1 = offlineClientResource1.toRepresentation();
            clientRep1.setFullScopeAllowed(true);
            offlineClientResource.update(clientRep1);
        }
    }

    @Test
    public void offlineRefreshWhenNoOfflineScope() throws Exception {

        // login to obtain a refresh token
        oauth.scope("openid " + OAuth2Constants.OFFLINE_ACCESS);
        oauth.client("offline-client", "secret1");
        oauth.redirectUri(OFFLINE_CLIENT_APP_URI);
        oauth.doLogin("test-user@localhost", "password");
        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse response = oauth.doAccessTokenRequest(code);

        EventRepresentation loginEvent = events.poll();
        EventAssertion.assertSuccess(loginEvent)
                .type(EventType.LOGIN)
                .clientId("offline-client")
                .details(Details.REDIRECT_URI, OFFLINE_CLIENT_APP_URI);

        EventRepresentation codeToTokenEvent = events.poll();
        EventAssertion.assertSuccess(codeToTokenEvent)
                .type(EventType.CODE_TO_TOKEN)
                .clientId("offline-client")
                .sessionId(loginEvent.getSessionId())
                .details(Details.CODE_ID, loginEvent.getDetails().get(Details.CODE_ID))
                .details(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_OFFLINE);

        // check refresh is successful
        RefreshToken offlineToken = oauth.parseRefreshToken(response.getRefreshToken());
        oauth.scope(null);
        response = oauth.doRefreshTokenRequest(response.getRefreshToken());
        assertEquals(200, response.getStatusCode());
        Assertions.assertEquals(0, response.getRefreshExpiresIn());
        EventRepresentation refreshEvent = events.poll();
        EventAssertion.assertSuccess(refreshEvent)
                .type(EventType.REFRESH_TOKEN)
                .clientId("offline-client")
                .userId(userId)
                .sessionId(loginEvent.getSessionId())
                .details(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_OFFLINE)
                .details(Details.REFRESH_TOKEN_ID, offlineToken.getId());
        offlineToken = oauth.parseRefreshToken(response.getRefreshToken());

        IntrospectionResponse introspectionResponse = oauth.doIntrospectionAccessTokenRequest(response.getAccessToken());
        assertTrue(introspectionResponse.asJsonNode().get("active").asBoolean());
        EventRepresentation introspectEvent = events.poll();
        EventAssertion.assertSuccess(introspectEvent)
                .type(EventType.INTROSPECT_TOKEN)
                .clientId("offline-client")
                .sessionId(loginEvent.getSessionId());

        introspectionResponse = oauth.doIntrospectionAccessTokenRequest(response.getRefreshToken());
        assertTrue(introspectionResponse.asJsonNode().get("active").asBoolean());
        EventRepresentation introspectEvent2 = events.poll();
        EventAssertion.assertSuccess(introspectEvent2)
                .type(EventType.INTROSPECT_TOKEN)
                .clientId("offline-client")
                .sessionId(loginEvent.getSessionId());

        // remove offline scope from the client and perform a second refresh
        ClientResource offlineClientResource = AdminApiUtil.findClientByClientId(adminClient.realm("test"), "offline-client");
        ClientScopeRepresentation offlineAccessScope = adminClient.realm("test").clientScopes().findAll().stream()
                .filter(scope -> "offline_access".equals(scope.getName()))
                .findFirst()
                .orElseThrow();

        // Remove the offline_access scope
        offlineClientResource.removeOptionalClientScope(offlineAccessScope.getId());

        try {
            introspectionResponse = oauth.doIntrospectionAccessTokenRequest(response.getAccessToken());
            assertFalse(introspectionResponse.asJsonNode().get("active").asBoolean());
            EventRepresentation introspectErrorEvent = events.poll();
            EventAssertion.assertError(introspectErrorEvent)
                    .type(EventType.INTROSPECT_TOKEN_ERROR)
                    .clientId("offline-client")
                    .sessionId(loginEvent.getSessionId())
                    .error(Errors.SESSION_EXPIRED)
                    .details(Details.REASON, "Offline session invalid because offline access not granted anymore");

            introspectionResponse = oauth.doIntrospectionAccessTokenRequest(response.getRefreshToken());
            assertFalse(introspectionResponse.asJsonNode().get("active").asBoolean());
            EventRepresentation introspectErrorEvent2 = events.poll();
            EventAssertion.assertError(introspectErrorEvent2)
                    .type(EventType.INTROSPECT_TOKEN_ERROR)
                    .clientId("offline-client")
                    .sessionId(loginEvent.getSessionId())
                    .error(Errors.SESSION_EXPIRED)
                    .details(Details.REASON, "Offline session invalid because offline access not granted anymore");

            response = oauth.doRefreshTokenRequest(response.getRefreshToken());
            assertEquals(400, response.getStatusCode());
            assertEquals(OAuthErrorException.INVALID_GRANT, response.getError());
            assertEquals("Offline session invalid because offline access not granted anymore", response.getErrorDescription());
            EventRepresentation refreshErrorEvent = events.poll();
            EventAssertion.assertError(refreshErrorEvent)
                    .type(EventType.REFRESH_TOKEN_ERROR)
                    .clientId("offline-client")
                    .sessionId(loginEvent.getSessionId())
                    .error(Errors.INVALID_TOKEN)
                    .details(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_OFFLINE)
                    .details(Details.REFRESH_TOKEN_ID, offlineToken.getId())
                    .details(Details.REASON, "Offline session invalid because offline access not granted anymore");
        } catch (IOException e) {
            throw new RuntimeException("Failed to perform offline token introspection", e);
        } finally {
            // put the offline_access scope back
            offlineClientResource.addOptionalClientScope(offlineAccessScope.getId());
        }
    }

    // KEYCLOAK-7688 Offline Session Max for Offline Token
    private int[] changeOfflineSessionSettings(boolean isEnabled, int sessionMax, int sessionIdle, int clientSessionMax, int clientSessionIdle) {
        int[] prev = new int[5];
        RealmRepresentation rep = adminClient.realm("test").toRepresentation();
        prev[0] = rep.getOfflineSessionMaxLifespan();
        prev[1] = rep.getOfflineSessionIdleTimeout();
        prev[2] = rep.getClientOfflineSessionMaxLifespan();
        prev[3] = rep.getClientOfflineSessionIdleTimeout();
        RealmConfigBuilder realmBuilder = RealmConfigBuilder.create();
        realmBuilder.update(r -> {
            r.setOfflineSessionMaxLifespanEnabled(isEnabled);
            r.setOfflineSessionMaxLifespan(sessionMax);
            r.setOfflineSessionIdleTimeout(sessionIdle);
            r.setClientOfflineSessionMaxLifespan(clientSessionMax);
            r.setClientOfflineSessionIdleTimeout(clientSessionIdle);
        });
        adminClient.realm("test").update(realmBuilder.build());
        return prev;
    }

    private int checkIfUserAndClientSessionExist(final String userSessionId, final String clientId, final String clientSessionId) {
        return runOnServer.fetch(session -> {
            RealmModel realmModel = session.realms().getRealmByName("test");
            session.getContext().setRealm(realmModel);
            ClientModel clientModel = realmModel.getClientByClientId(clientId);
            UserSessionModel userSession = session.sessions().getOfflineUserSession(realmModel, userSessionId);
            if (userSession != null) {
                AuthenticatedClientSessionModel clientSession = userSession.getAuthenticatedClientSessionByClient(clientModel.getId());
                return clientSession != null && clientSessionId.equals(clientSession.getId())? 2 : 1;
            }
            return 0;
        }, Integer.class);
    }

    private String getOfflineClientSessionUuid(final String userSessionId, final String clientId) {
        return runOnServer.fetch(session -> {
            RealmModel realmModel = session.realms().getRealmByName("test");
            ClientModel clientModel = realmModel.getClientByClientId(clientId);
            UserSessionModel userSession = session.sessions().getOfflineUserSession(realmModel, userSessionId);
            AuthenticatedClientSessionModel clientSession = userSession.getAuthenticatedClientSessionByClient(clientModel.getId());
            return clientSession.getId();
        }, String.class);
    }

    public static class OfflineTokenRealmConfig implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder builder) {
            builder.name("test")
                    .eventsEnabled(true)
                    .ssoSessionIdleTimeout(30)
                    .update(r -> r.setAccessTokenLifespan(10));

            // Enable all event types
            builder.update(r -> {
                r.setEnabledEventTypes(java.util.Arrays.asList(
                        "LOGIN",
                        "LOGIN_ERROR",
                        "LOGOUT",
                        "CODE_TO_TOKEN",
                        "REFRESH_TOKEN",
                        "REFRESH_TOKEN_ERROR",
                        "INTROSPECT_TOKEN",
                        "INTROSPECT_TOKEN_ERROR"
                ));
            });

            // Only create offline-client - test-app is created by @InjectOAuthClient
            builder.addClient(OFFLINE_CLIENT_ID)
                    .secret("secret1")
                    .redirectUris(OFFLINE_CLIENT_APP_URI)
                    .adminUrl(OFFLINE_CLIENT_APP_URI)
                    .directAccessGrantsEnabled(true)
                    .serviceAccountsEnabled(true)
                    .attribute(OIDCConfigAttributes.USE_REFRESH_TOKEN_FOR_CLIENT_CREDENTIALS_GRANT, "true");

            // Users WITHOUT test-app client roles
            builder.addUser("test-user@localhost")
                    .name("Tom", "Brady")
                    .email("test-user@localhost")
                    .emailVerified(true)
                    .password("password")
                    .roles("user", "offline_access");

            return builder;
        }
    }

    public static class OfflineAuthClientConfig implements ClientConfig {
        @Override
        public ClientConfigBuilder configure(ClientConfigBuilder client) {
            return client.clientId("test-app")
                    .secret("password")
                    .serviceAccountsEnabled(true)
                    .directAccessGrantsEnabled(true)
                    .redirectUris(
                            "http://localhost:8080/test-app",  // Default
                            TEST_APP_REDIRECT_URI              // Custom URI
                    );
        }
    }
}

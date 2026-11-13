package org.keycloak.tests.oauth;

import jakarta.ws.rs.NotFoundException;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.common.Profile;
import org.keycloak.common.util.Time;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.session.UserSessionPersisterProvider;
import org.keycloak.models.utils.SessionTimeoutHelper;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.AudienceProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
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
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.remote.providers.timeoffset.InfinispanTimeUtil;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.LogoutResponse;
import org.keycloak.util.TokenUtil;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.tests.utils.Assert.assertExpiration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class OfflineTokenSessionManagementTest {

    private static final String OFFLINE_CLIENT_ID = "offline-client";
    private static final String OFFLINE_CLIENT_APP_URI = "http://localhost:8080/offline-client";
    private static final String TEST_APP_REDIRECT_URI = "http://localhost:8080/auth/realms/test/app/auth";

    @InjectRealm(config = OfflineTokenSessionManagementTest.OfflineTokenRealmConfig.class)
    ManagedRealm realm;

    @InjectOAuthClient(config = OfflineTokenSessionManagementTest.OfflineAuthClientConfig.class)
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

        // Clear browser state
        driver.driver().manage().deleteAllCookies();
        events.clear();
    }

    @AfterEach
    public void cleanup() {
        // Clear events
        events.clear();

        // Clear browser state
        try {
            driver.driver().manage().deleteAllCookies();
        } catch (Exception e) {
            // Ignore if driver is already closed
        }
    }

    @Test
    public void offlineTokenLogout() {
        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        oauth.client("offline-client", "secret1");
        AccessTokenResponse response = oauth.doPasswordGrantRequest("test-user@localhost", "password");
        assertEquals(200, response.getStatusCode());

        response = oauth.doRefreshTokenRequest(response.getRefreshToken());
        assertEquals(200, response.getStatusCode());

        LogoutResponse logoutResponse = oauth.doLogout(response.getRefreshToken());
        assertTrue(logoutResponse.isSuccess());

        response = oauth.doRefreshTokenRequest(response.getRefreshToken());
        assertEquals(400, response.getStatusCode());
    }

    @Test
    public void onlineOfflineTokenLogout() {
        oauth.client("offline-client", "secret1");

        // create online session
        AccessTokenResponse response = oauth.doPasswordGrantRequest("test-user@localhost", "password");
        assertEquals(200, response.getStatusCode());

        // assert refresh token
        response = oauth.doRefreshTokenRequest(response.getRefreshToken());
        assertEquals(200, response.getStatusCode());

        // create offline session
        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        AccessTokenResponse offlineResponse = oauth.doPasswordGrantRequest("test-user@localhost", "password");
        assertEquals(200, offlineResponse.getStatusCode());

        // assert refresh offline token
        AccessTokenResponse offlineRefresh = oauth.doRefreshTokenRequest(offlineResponse.getRefreshToken());
        assertEquals(200, offlineRefresh.getStatusCode());

        // logout online session
        LogoutResponse logoutResponse = oauth.scope(null).doLogout(response.getRefreshToken());
        assertTrue(logoutResponse.isSuccess());

        // assert the online session is gone
        response = oauth.doRefreshTokenRequest(response.getRefreshToken());
        assertEquals(400, response.getStatusCode());

        // assert the offline token refresh still works
        offlineRefresh = oauth.doRefreshTokenRequest(offlineResponse.getRefreshToken());
        assertEquals(200, offlineRefresh.getStatusCode());
    }

    @Test
    public void browserOfflineTokenLogoutFollowedByLoginSameSession() {
        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        oauth.client("offline-client", "secret1");
        oauth.redirectUri(OFFLINE_CLIENT_APP_URI);
        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.poll();
        EventAssertion.assertSuccess(loginEvent)
                .type(EventType.LOGIN)
                .clientId("offline-client")
                .details(Details.REDIRECT_URI, OFFLINE_CLIENT_APP_URI);

        final String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);

        String code = oauth.parseLoginResponse().getCode();

        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);
        oauth.verifyToken(tokenResponse.getAccessToken());
        String offlineTokenString = tokenResponse.getRefreshToken();
        RefreshToken offlineToken = oauth.parseRefreshToken(offlineTokenString);

        EventRepresentation codeToTokenEvent = events.poll();
        EventAssertion.assertSuccess(codeToTokenEvent)
                .type(EventType.CODE_TO_TOKEN)
                .clientId("offline-client")
                .sessionId(sessionId)
                .details(Details.CODE_ID, codeId)
                .details(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_OFFLINE);

        assertEquals(TokenUtil.TOKEN_TYPE_OFFLINE, offlineToken.getType());

        String offlineUserSessionId = runOnServer.fetch(session -> {
            return session.sessions().getOfflineUserSession(session.getContext().getRealm(), offlineToken.getSessionId()).getId();
        }, String.class);

        // logout offline session
        LogoutResponse logoutResponse = oauth.doLogout(offlineTokenString);
        assertTrue(logoutResponse.isSuccess());
        EventRepresentation logoutEvent = events.poll();
        EventAssertion.assertSuccess(logoutEvent)
                .type(EventType.LOGOUT)
                .clientId("offline-client")
                .sessionId(offlineUserSessionId);

        // Need to login again now
        oauth.doLogin("test-user@localhost", "password");
        String code2 = oauth.parseLoginResponse().getCode();

        AccessTokenResponse tokenResponse2 = oauth.doAccessTokenRequest(code2);
        assertEquals(200, tokenResponse2.getStatusCode());
        oauth.verifyToken(tokenResponse2.getAccessToken());
        String offlineTokenString2 = tokenResponse2.getRefreshToken();
        RefreshToken offlineToken2 = oauth.parseRefreshToken(offlineTokenString2);

        loginEvent = events.poll();
        EventAssertion.assertSuccess(loginEvent)
                .type(EventType.LOGIN)
                .clientId("offline-client")
                .details(Details.REDIRECT_URI, OFFLINE_CLIENT_APP_URI);

        codeId = loginEvent.getDetails().get(Details.CODE_ID);

        EventRepresentation codeToTokenEvent2 = events.poll();
        EventAssertion.assertSuccess(codeToTokenEvent2)
                .type(EventType.CODE_TO_TOKEN)
                .clientId("offline-client")
                .sessionId(offlineToken2.getSessionId())
                .details(Details.CODE_ID, codeId)
                .details(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_OFFLINE);

        assertEquals(TokenUtil.TOKEN_TYPE_OFFLINE, offlineToken2.getType());

        // Assert session changed
        assertNotEquals(offlineToken.getSessionId(), offlineToken2.getSessionId());
    }


    @Test
    public void offlineTokenBrowserFlowMaxLifespanExpired() {
        // expect that offline session expired by max lifespan
        final int MAX_LIFESPAN = 3600;
        final int IDLE_LIFESPAN = 6000;
        testOfflineSessionExpiration(IDLE_LIFESPAN, MAX_LIFESPAN, MAX_LIFESPAN / 2, MAX_LIFESPAN + 60);
    }

    @Test
    public void offlineTokenBrowserFlowIdleTimeExpired() {
        // expect that offline session expired by idle time
        final int MAX_LIFESPAN = 3000;
        final int IDLE_LIFESPAN = 600;
        // Additional time window is added for the case when session was updated in different DC and the update to current DC was postponed
        //testOfflineSessionExpiration(IDLE_LIFESPAN, MAX_LIFESPAN, 0, IDLE_LIFESPAN + (Profile.isFeatureEnabled(Profile.Feature.PERSISTENT_USER_SESSIONS) ? 0 : SessionTimeoutHelper.IDLE_TIMEOUT_WINDOW_SECONDS) + 60);

        // Check feature on server side
        boolean isPersistentUserSessionsEnabled = runOnServer.fetch(session -> {
            return Profile.isFeatureEnabled(Profile.Feature.PERSISTENT_USER_SESSIONS);
        }, Boolean.class);

        int additionalTimeout = isPersistentUserSessionsEnabled ? 0 : SessionTimeoutHelper.IDLE_TIMEOUT_WINDOW_SECONDS;
        testOfflineSessionExpiration(IDLE_LIFESPAN, MAX_LIFESPAN, 0, IDLE_LIFESPAN + additionalTimeout + 60);
    }

    // Issue 13706
    @Test
    public void offlineTokenReauthenticationWhenOfflineClientSessionExpired() throws Exception {
        // expect that offline session expired by idle timeout
        final int MAX_LIFESPAN = 360000;
        final int IDLE_LIFESPAN = 900;

        runOnServer.run(InfinispanTimeUtil.enableTestingTimeService());
        realm.cleanup().add(r -> runOnServer.run(InfinispanTimeUtil.disableTestingTimeService()));

        realm.updateWithCleanup(r -> r
                .ssoSessionIdleTimeout(900)
                .offlineSessionMaxLifespanEnabled(true)
                .offlineSessionMaxLifespan(MAX_LIFESPAN)
                .offlineSessionIdleTimeout(IDLE_LIFESPAN)
                .clientOfflineSessionMaxLifespan(0)
                .clientOfflineSessionIdleTimeout(0));

        // Step 1 - online login with "tets-app"
        oauth.scope(null);
        oauth.client("test-app", "password");
        oauth.redirectUri(TEST_APP_REDIRECT_URI);
        oauth.doLogin("test-user@localhost", "password");
        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);
        assertRefreshToken(tokenResponse, TokenUtil.TOKEN_TYPE_REFRESH);

        // Clear browser state between logins.
        driver.driver().manage().deleteAllCookies();

        // Step 2 - offline login with "offline-client"
        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        oauth.client("offline-client", "secret1");
        oauth.redirectUri(OFFLINE_CLIENT_APP_URI);

        oauth.openLoginForm();
        code = oauth.parseLoginResponse().getCode();
        tokenResponse = oauth.doAccessTokenRequest(code);
        assertOfflineToken(tokenResponse);

        // Step 3 - set some offset to refresh SSO session and offline user session. But use different client, so that we don't refresh offlineClientSession of client "offline-client"
        timeOffSet.set(800);
        oauth.client("test-app", "password");
        oauth.redirectUri(TEST_APP_REDIRECT_URI);
        oauth.openLoginForm();

        code = oauth.parseLoginResponse().getCode();
        tokenResponse = oauth.doAccessTokenRequest(code);
        assertOfflineToken(tokenResponse);

        // Step 4 - set bigger time offset and login with the original client "offline-token". Login should be successful and offline client session for "offline-client" should be re-created now
        timeOffSet.set(900 + SessionTimeoutHelper.PERIODIC_CLEANER_IDLE_TIMEOUT_WINDOW_SECONDS + 20);
        oauth.client("offline-client", "secret1");
        oauth.redirectUri(OFFLINE_CLIENT_APP_URI);
        oauth.openLoginForm();

        code = oauth.parseLoginResponse().getCode();
        tokenResponse = oauth.doAccessTokenRequest(code);
        assertOfflineToken(tokenResponse);
    }

    @Test
    public void testShortOfflineSessionMax() throws Exception {
        realm.updateWithCleanup(r -> r
                .offlineSessionMaxLifespanEnabled(true)
                .offlineSessionMaxLifespan(60)
                .offlineSessionIdleTimeout(30)
                .clientOfflineSessionMaxLifespan(0)
                .clientOfflineSessionIdleTimeout(0)
                .ssoSessionIdleTimeout(1800)
                .accessTokenLifespan(300));

        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        oauth.client("offline-client", "secret1");
        oauth.redirectUri(OFFLINE_CLIENT_APP_URI);
        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.poll();
        EventAssertion.assertSuccess(loginEvent)
                .type(EventType.LOGIN)
                .clientId("offline-client")
                .details(Details.REDIRECT_URI, OFFLINE_CLIENT_APP_URI);

        String code = oauth.parseLoginResponse().getCode();

        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);
        String offlineTokenString = tokenResponse.getRefreshToken();
        RefreshToken offlineToken = oauth.parseRefreshToken(offlineTokenString);

        assertThat(tokenResponse.getExpiresIn(), allOf(greaterThanOrEqualTo(59), lessThanOrEqualTo(60)));
        assertThat(tokenResponse.getRefreshExpiresIn(), allOf(greaterThanOrEqualTo(29), lessThanOrEqualTo(30)));
        assertEquals(TokenUtil.TOKEN_TYPE_OFFLINE, offlineToken.getType());

        JsonNode jsonNode = oauth.doIntrospectionAccessTokenRequest(tokenResponse.getAccessToken()).asJsonNode();
        assertTrue(jsonNode.get("active").asBoolean());
        Assertions.assertEquals("test-user@localhost", jsonNode.get("email").asText());
        assertThat(jsonNode.get("exp").asInt() - Time.currentTime(),
                allOf(greaterThanOrEqualTo(59), lessThanOrEqualTo(60)));
    }

    @Test
    public void testClientOfflineSessionMaxLifespan() {
        int offlineSessionMaxLifespan = this.realm.admin().toRepresentation().getOfflineSessionIdleTimeout() - 100;

        this.realm.updateWithCleanup(r -> r
                .offlineSessionMaxLifespanEnabled(true)
                .offlineSessionMaxLifespan(offlineSessionMaxLifespan));

        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        oauth.client("offline-client", "secret1");
        oauth.redirectUri(OFFLINE_CLIENT_APP_URI);
        oauth.doLogin("test-user@localhost", "password");
        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse response = oauth.doAccessTokenRequest(code);
        assertEquals(200, response.getStatusCode());
        assertExpiration(response.getRefreshExpiresIn(), offlineSessionMaxLifespan);

        this.realm.updateWithCleanup(r -> r.clientOfflineSessionMaxLifespan(offlineSessionMaxLifespan - 100));

        String refreshToken = response.getRefreshToken();
        response = oauth.doRefreshTokenRequest(refreshToken);
        assertEquals(200, response.getStatusCode());
        assertExpiration(response.getRefreshExpiresIn(), offlineSessionMaxLifespan - 100);

        this.realm.updateClientWithCleanup("offline-client", c -> c.attribute(
                OIDCConfigAttributes.CLIENT_OFFLINE_SESSION_MAX_LIFESPAN,
                Integer.toString(offlineSessionMaxLifespan - 200)));

        refreshToken = response.getRefreshToken();
        response = oauth.doRefreshTokenRequest(refreshToken);
        assertEquals(200, response.getStatusCode());
        assertExpiration(response.getRefreshExpiresIn(), offlineSessionMaxLifespan - 200);
    }

    @Test
    public void testClientOfflineSessionIdleTimeout() {
        int offlineSessionIdleTimeout = this.realm.admin().toRepresentation().getOfflineSessionIdleTimeout();

        this.realm.updateWithCleanup(r -> r.offlineSessionMaxLifespanEnabled(true));

        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        oauth.client("offline-client", "secret1");
        oauth.redirectUri(OFFLINE_CLIENT_APP_URI);
        oauth.doLogin("test-user@localhost", "password");
        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse response = oauth.doAccessTokenRequest(code);
        assertEquals(200, response.getStatusCode());
        assertExpiration(response.getRefreshExpiresIn(), offlineSessionIdleTimeout);

        this.realm.updateWithCleanup(r -> r.clientOfflineSessionIdleTimeout(offlineSessionIdleTimeout - 100));

        String refreshToken = response.getRefreshToken();
        response = oauth.doRefreshTokenRequest(refreshToken);
        assertEquals(200, response.getStatusCode());
        assertExpiration(response.getRefreshExpiresIn(), offlineSessionIdleTimeout - 100);

        this.realm.updateClientWithCleanup("offline-client", c -> c.attribute(
                OIDCConfigAttributes.CLIENT_OFFLINE_SESSION_IDLE_TIMEOUT,
                Integer.toString(offlineSessionIdleTimeout - 200)));

        refreshToken = response.getRefreshToken();
        response = oauth.doRefreshTokenRequest(refreshToken);
        assertEquals(200, response.getStatusCode());
        assertExpiration(response.getRefreshExpiresIn(), offlineSessionIdleTimeout - 200);
    }


    @Test
    public void testClientOfflineSessionIdleTimeoutCappedByUserSessionIdle() {
        int realmIdleTimeout = 600;
        this.realm.updateWithCleanup(r -> r.offlineSessionIdleTimeout(realmIdleTimeout));
        this.realm.updateClientWithCleanup("offline-client", c -> c.attribute(
                OIDCConfigAttributes.CLIENT_OFFLINE_SESSION_IDLE_TIMEOUT,
                Integer.toString(realmIdleTimeout + 600)));

        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        oauth.client("offline-client", "secret1");
        oauth.redirectUri(OFFLINE_CLIENT_APP_URI);
        oauth.doLogin("test-user@localhost", "password");
        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse response = oauth.doAccessTokenRequest(code);
        assertEquals(200, response.getStatusCode());
        assertExpiration(response.getRefreshExpiresIn(), realmIdleTimeout);
    }

    @Test
    public void offlineRefreshWhenNoStartedAtClientNote() {
        realm.updateWithCleanup(r -> r
                .offlineSessionMaxLifespanEnabled(true)
                .offlineSessionMaxLifespan(3600)
                .offlineSessionIdleTimeout(3600)
                .clientOfflineSessionMaxLifespan(0)
                .clientOfflineSessionIdleTimeout(0));

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

        // remove the started notes that can be missed in previous versions
        removeClientSessionStartedAtNote(loginEvent.getSessionId(), loginEvent.getClientId());

        // check refresh is successful
        response = oauth.doRefreshTokenRequest(response.getRefreshToken());
        assertEquals(200, response.getStatusCode());
        assertTrue(0 < response.getRefreshExpiresIn() && response.getRefreshExpiresIn() <= 3600, "Invalid ExpiresIn");

        // check refresh a second time
        response = oauth.doRefreshTokenRequest(response.getRefreshToken());
        assertEquals(200, response.getStatusCode());
        assertTrue(0 < response.getRefreshExpiresIn() && response.getRefreshExpiresIn() <= 3600, "Invalid ExpiresIn");
    }


    private void testOfflineSessionExpiration(int idleTime, int maxLifespan, int offsetHalf, int offset) {
        runOnServer.run(InfinispanTimeUtil.enableTestingTimeService());
        realm.cleanup().add(r -> runOnServer.run(InfinispanTimeUtil.disableTestingTimeService()));

        realm.updateWithCleanup(r -> r
                .offlineSessionMaxLifespanEnabled(true)
                .offlineSessionMaxLifespan(maxLifespan)
                .offlineSessionIdleTimeout(idleTime)
                .clientOfflineSessionMaxLifespan(0)
                .clientOfflineSessionIdleTimeout(0));

        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        oauth.client("offline-client", "secret1");
        oauth.redirectUri(OFFLINE_CLIENT_APP_URI);
        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.poll();
        EventAssertion.assertSuccess(loginEvent)
                .type(EventType.LOGIN)
                .clientId("offline-client")
                .details(Details.REDIRECT_URI, OFFLINE_CLIENT_APP_URI);

        final String sessionId = loginEvent.getSessionId();

        String code = oauth.parseLoginResponse().getCode();

        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);
        String offlineTokenString = tokenResponse.getRefreshToken();
        RefreshToken offlineToken = oauth.parseRefreshToken(offlineTokenString);

        assertEquals(TokenUtil.TOKEN_TYPE_OFFLINE, offlineToken.getType());

        // obtain the client session ID
        final String clientSessionId = getOfflineClientSessionUuid(sessionId, loginEvent.getClientId());
        assertEquals(2, checkIfUserAndClientSessionExist(sessionId, loginEvent.getClientId(), clientSessionId));

        // perform a refresh in the half-time
        timeOffSet.set(offsetHalf);

        tokenResponse = oauth.doRefreshTokenRequest(offlineTokenString);
        oauth.verifyToken(tokenResponse.getAccessToken());
        offlineTokenString = tokenResponse.getRefreshToken();
        oauth.parseRefreshToken(offlineTokenString);

        Assertions.assertEquals(200, tokenResponse.getStatusCode());
        assertEquals(2, checkIfUserAndClientSessionExist(sessionId, loginEvent.getClientId(), clientSessionId));

        // wait to expire
        timeOffSet.set(offset);

        tokenResponse = oauth.doRefreshTokenRequest(offlineTokenString);

        Assertions.assertEquals(400, tokenResponse.getStatusCode());
        assertEquals("invalid_grant", tokenResponse.getError());

        // Assert userSession expired
        assertEquals(0, checkIfUserAndClientSessionExist(sessionId, loginEvent.getClientId(), clientSessionId));
        runOnServer.run(session -> {
            session.getProvider(UserSessionPersisterProvider.class).removeExpired(session.getContext().getRealm());
        });
        try {
            runOnServer.run(session -> {
                UserSessionModel userSession = session.sessions().getUserSession(session.getContext().getRealm(), sessionId);
                if (userSession != null) {
                    session.sessions().removeUserSession(session.getContext().getRealm(), userSession);
                }
            });
        } catch (NotFoundException nfe) {
            // Ignore
        }
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

    private void assertOfflineToken(AccessTokenResponse tokenResponse) {
        assertRefreshToken(tokenResponse, TokenUtil.TOKEN_TYPE_OFFLINE);
    }

    // Asserts that refresh token in the tokenResponse is of the given type. Return parsed token
    private RefreshToken assertRefreshToken(AccessTokenResponse tokenResponse, String tokenType) {
        Assertions.assertEquals(200, tokenResponse.getStatusCode());
        String offlineTokenString = tokenResponse.getRefreshToken();
        RefreshToken refreshToken = oauth.parseRefreshToken(offlineTokenString);
        assertEquals(tokenType, refreshToken.getType());
        return refreshToken;
    }

    private void removeClientSessionStartedAtNote(final String userSessionId, final String clientId) {
        runOnServer.run(session -> {
            RealmModel realmModel = session.realms().getRealmByName("test");
            session.getContext().setRealm(realmModel);
            ClientModel clientModel = realmModel.getClientByClientId(clientId);
            UserSessionModel userSession = session.sessions().getOfflineUserSession(realmModel, userSessionId);
            if (userSession != null) {
                AuthenticatedClientSessionModel clientSession = userSession.getAuthenticatedClientSessionByClient(clientModel.getId());
                if (clientSession != null) {
                    clientSession.removeNote(AuthenticatedClientSessionModel.STARTED_AT_NOTE);
                    clientSession.removeNote(AuthenticatedClientSessionModel.USER_SESSION_STARTED_AT_NOTE);
                }
            }
        });
    }

    public static class OfflineTokenRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder builder) {
            builder.name("test")
                    .eventsEnabled(true)
                    .ssoSessionIdleTimeout(30)
                    .update(r -> r.setAccessTokenLifespan(10));

            // Enable all event types
            builder.update(r -> {
                r.setEnabledEventTypes(java.util.Arrays.asList(
                        "LOGIN",
                        "LOGOUT",
                        "CODE_TO_TOKEN",
                        "REFRESH_TOKEN"
                ));
            });

            // Only create offline-client - test-app is created by @InjectOAuthClient
            ProtocolMapperRepresentation audienceMapper = new ProtocolMapperRepresentation();
            audienceMapper.setName("audience-offline-client");
            audienceMapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
            audienceMapper.setProtocolMapper(AudienceProtocolMapper.PROVIDER_ID);
            audienceMapper.setConfig(new java.util.HashMap<>());
            audienceMapper.getConfig().put(AudienceProtocolMapper.INCLUDED_CUSTOM_AUDIENCE, OFFLINE_CLIENT_ID);
            audienceMapper.getConfig().put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true");

            builder.clients(ClientBuilder.create(OFFLINE_CLIENT_ID)
                    .secret("secret1")
                    .redirectUris(OFFLINE_CLIENT_APP_URI)
                    .adminUrl(OFFLINE_CLIENT_APP_URI)
                    .directAccessGrantsEnabled(true)
                    .serviceAccountsEnabled(true)
                    .attribute(OIDCConfigAttributes.USE_REFRESH_TOKEN_FOR_CLIENT_CREDENTIALS_GRANT, "true")
                    .protocolMappers(audienceMapper));

            // Users WITHOUT test-app client roles
            builder.users(UserBuilder.create("test-user@localhost")
                    .name("Tom", "Brady")
                    .email("test-user@localhost")
                    .emailVerified(true)
                    .password("password")
                    .realmRoles("user", "offline_access"));

            return builder;
        }
    }

    public static class OfflineAuthClientConfig implements ClientConfig {
        @Override
        public ClientBuilder configure(ClientBuilder client) {
            ProtocolMapperRepresentation audienceMapper = new ProtocolMapperRepresentation();
            audienceMapper.setName("audience-test-app");
            audienceMapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
            audienceMapper.setProtocolMapper(AudienceProtocolMapper.PROVIDER_ID);
            audienceMapper.setConfig(new java.util.HashMap<>());
            audienceMapper.getConfig().put(AudienceProtocolMapper.INCLUDED_CUSTOM_AUDIENCE, "test-app");
            audienceMapper.getConfig().put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true");
            return client.clientId("test-app")
                    .secret("password")
                    .serviceAccountsEnabled(true)
                    .directAccessGrantsEnabled(true)
                    .redirectUris(
                            "http://localhost:8080/test-app",  // Default
                            TEST_APP_REDIRECT_URI              // Custom URI
                    )
                    .protocolMappers(audienceMapper);
        }
    }
}

package org.keycloak.tests.oauth;

import java.util.UUID;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.common.Profile;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;
import org.keycloak.models.utils.SessionTimeoutHelper;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;
import org.keycloak.representations.info.FeatureRepresentation;
import org.keycloak.representations.info.ServerInfoRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.common.TestRealmUserConfig;
import org.keycloak.tests.utils.Assert;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.hamcrest.Matchers;
import org.infinispan.Cache;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.CLIENT_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.USER_SESSION_CACHE_NAME;
import static org.keycloak.protocol.oidc.OIDCConfigAttributes.CLIENT_SESSION_IDLE_TIMEOUT;
import static org.keycloak.protocol.oidc.OIDCConfigAttributes.CLIENT_SESSION_MAX_LIFESPAN;
import static org.keycloak.tests.oauth.RefreshTokenTest.enableRefreshTokenEvents;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test for the scenarios related to refresh-token and involving userSession and clientSession timeouts (idle-timeout, max session timeout etc).
 */
@KeycloakIntegrationTest
public class RefreshTokenTimeoutsTest {

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectAdminClient(mode = InjectAdminClient.Mode.BOOTSTRAP)
    Keycloak adminClient;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @InjectEvents
    Events events;

    @InjectTimeOffSet(enableForCaches = true)
    TimeOffSet timeOffSet;

    @InjectPage
    LoginPage loginPage;

    @InjectRealm(config = RefreshTokenTest.RefreshTokenTestRealmConfig.class)
    protected ManagedRealm realm;

    @InjectUser(config = TestRealmUserConfig.class)
    protected ManagedUser user;

    @InjectClient(attachTo = "test-app")
    ManagedClient managedClient;

    @BeforeEach
    public void before() {
        enableRefreshTokenEvents(realm);
        AccountHelper.logout(realm.admin(), user.getUsername());
    }

    @Test
    public void testUserSessionRefreshAndIdle() {
        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.poll();
        EventAssertion.assertSuccess(loginEvent)
                .userId(user.getId())
                .clientId("test-app")
                .type(EventType.LOGIN);

        String sessionId = loginEvent.getSessionId();
        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);

        EventRepresentation tokenEvent = events.poll();
        EventAssertion.assertSuccess(tokenEvent)
                .userId(user.getId())
                .sessionId(sessionId)
                .clientId("test-app")
                .type(EventType.CODE_TO_TOKEN);

        long last = getLastSessionRefresh(sessionId);

        timeOffSet.set(2);

        tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());

        oauth.verifyToken(tokenResponse.getAccessToken());
        oauth.parseRefreshToken(tokenResponse.getRefreshToken());

        assertEquals(200, tokenResponse.getStatusCode());

        long next = getLastSessionRefresh(sessionId);

        assertNotEquals(last, next);

        RealmRepresentation realmRep = realm.admin().toRepresentation();
        int lastAccessTokenLifespan = realmRep.getAccessTokenLifespan();
        int originalIdle = realmRep.getSsoSessionIdleTimeout();

        try {
            realmRep.setAccessTokenLifespan(100000);
            realm.admin().update(realmRep);

            timeOffSet.set(4);
            tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());

            next = getLastSessionRefresh(sessionId);

            // lastSEssionRefresh should be updated because access code lifespan is higher than sso idle timeout
            assertThat(next, allOf(greaterThan(last), lessThan(last + 50)));

            realmRep.setSsoSessionIdleTimeout(1);
            realm.admin().update(realmRep);

            events.clear();
            // Needs to add some additional time due the tollerance allowed by IDLE_TIMEOUT_WINDOW_SECONDS
            timeOffSet.set(6 + (isPersistentSessionsFeatureEnabled() ? 0 : SessionTimeoutHelper.IDLE_TIMEOUT_WINDOW_SECONDS));
            tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());

            // test idle timeout
            assertEquals(400, tokenResponse.getStatusCode());
            assertNull(tokenResponse.getAccessToken());
            assertNull(tokenResponse.getRefreshToken());

            EventAssertion.assertError(events.poll())
                    .type(EventType.REFRESH_TOKEN_ERROR)
                    .error(Errors.INVALID_TOKEN);

        } finally {
            realmRep.setSsoSessionIdleTimeout(originalIdle);
            realmRep.setAccessTokenLifespan(lastAccessTokenLifespan);
            realm.admin().update(realmRep);
            events.clear();
        }

    }

    private long getLastSessionRefresh(String sessionId) {
        UserSessionRepresentation userSession = user.admin().getUserSessions().stream()
                .filter(session -> sessionId.equals(session.getId()))
                .findAny().orElseThrow();
        return userSession.getLastAccess() / 1000;
    }

    @Test
    public void testUserSessionRefreshAndIdleRememberMe() {
        realm.updateWithCleanup(r -> r
                .setRememberMe(true)
                .ssoSessionIdleTimeoutRememberMe(500)
                .ssoSessionIdleTimeout(100));

        oauth.openLoginForm();
        loginPage.rememberMe(true);
        loginPage.fillLogin("test-user@localhost", "password");
        loginPage.submit();

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);
        String sessionId = tokenResponse.getSessionState();

        long last = getLastSessionRefresh(sessionId);

        timeOffSet.set(110 + (isPersistentSessionsFeatureEnabled() ? 0 : SessionTimeoutHelper.IDLE_TIMEOUT_WINDOW_SECONDS));
        tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());
        oauth.verifyToken(tokenResponse.getAccessToken());
        oauth.parseRefreshToken(tokenResponse.getRefreshToken());
        assertEquals(200, tokenResponse.getStatusCode());

        long next = getLastSessionRefresh(sessionId);
        assertNotEquals(last, next);

        events.clear();
        // Needs to add some additional time due the tollerance allowed by IDLE_TIMEOUT_WINDOW_SECONDS
        timeOffSet.set(620 + 2 * (isPersistentSessionsFeatureEnabled() ? 0 : SessionTimeoutHelper.IDLE_TIMEOUT_WINDOW_SECONDS));
        tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());

        // test idle remember me timeout
        assertEquals(400, tokenResponse.getStatusCode());
        assertNull(tokenResponse.getAccessToken());
        assertNull(tokenResponse.getRefreshToken());

        EventAssertion.assertError(events.poll())
                .type(EventType.REFRESH_TOKEN_ERROR)
                .error(Errors.INVALID_TOKEN);
    }


    private String getClientSessionUuid(final String userSessionId, String clientId) {
        String realmName = realm.getName();
        return runOnServer.fetchString(session -> {
            RealmModel realmModel = session.realms().getRealmByName(realmName);
            ClientModel clientModel = realmModel.getClientByClientId(clientId);
            UserSessionModel userSession = session.sessions().getUserSession(realmModel, userSessionId);
            AuthenticatedClientSessionModel clientSession = userSession.getAuthenticatedClientSessionByClient(clientModel.getId());
            return clientSession.getId();
        });
    }

    private int checkIfUserAndClientSessionExist(final String userSessionId, final String clientId, final String clientSessionId) {
        String realmName = realm.getName();
        return runOnServer.fetch(session -> {
            RealmModel realmModel = session.realms().getRealmByName(realmName);
            ClientModel clientModel = realmModel.getClientByClientId(clientId);
            UserSessionModel userSession = session.sessions().getUserSession(realmModel, userSessionId);
            if (userSession != null) {
                AuthenticatedClientSessionModel clientSession = userSession.getAuthenticatedClientSessionByClient(clientModel.getId());
                return clientSession != null && clientSessionId.equals(clientSession.getId())? 2 : 1;
            }
            return 0;
        }, Integer.class);
    }


    @Test
    public void refreshTokenUserSessionMaxLifespan() {
        realm.updateWithCleanup(r -> r
                .ssoSessionMaxLifespan(3600)
                .ssoSessionIdleTimeout(7200));

        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.poll();
        EventAssertion.assertSuccess(loginEvent)
                .userId(user.getId())
                .clientId("test-app")
                .type(EventType.LOGIN);

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);
        String sessionId = tokenResponse.getSessionState();

        assertTrue(0 < tokenResponse.getRefreshExpiresIn() && tokenResponse.getRefreshExpiresIn() <= 3600, "Invalid ExpiresIn");
        final String clientSessionId = getClientSessionUuid(sessionId, loginEvent.getClientId());
        assertEquals(2, checkIfUserAndClientSessionExist(sessionId, loginEvent.getClientId(), clientSessionId));

        events.poll();

        timeOffSet.set(1800);

        String refreshId = oauth.parseRefreshToken(tokenResponse.getRefreshToken()).getId();
        tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());
        assertTrue(0 < tokenResponse.getRefreshExpiresIn() && tokenResponse.getRefreshExpiresIn() <= 1800, "Invalid ExpiresIn");
        assertEquals(2, checkIfUserAndClientSessionExist(sessionId, loginEvent.getClientId(), clientSessionId));

        EventRepresentation refreshEvent = events.poll();
        EventAssertion.assertSuccess(refreshEvent)
                .sessionId(sessionId)
                .details(Details.REFRESH_TOKEN_ID, refreshId)
                .type(EventType.REFRESH_TOKEN);

        timeOffSet.set(3700);
        oauth.parseRefreshToken(tokenResponse.getRefreshToken());
        tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());

        assertEquals(400, tokenResponse.getStatusCode());
        assertNull(tokenResponse.getAccessToken());
        assertNull(tokenResponse.getRefreshToken());
        EventAssertion.assertError(events.poll())
                .type(EventType.REFRESH_TOKEN_ERROR)
                .error(Errors.INVALID_TOKEN);
        assertEquals(0, checkIfUserAndClientSessionExist(sessionId, loginEvent.getClientId(), clientSessionId));
    }

    @Test
    public void refreshTokenUserClientMaxLifespanSmallerThanSession() {
        realm.updateWithCleanup(r ->
                r.ssoSessionMaxLifespan(3600)
                        .ssoSessionIdleTimeout(7200)
                        .clientSessionMaxLifespan(1000)
                        .clientSessionIdleTimeout(7200)
        );

        oauth.doLogin("test-user@localhost", "password");
        EventRepresentation loginEvent = events.poll();
        EventAssertion.assertSuccess(loginEvent)
                .userId(user.getId())
                .clientId("test-app")
                .type(EventType.LOGIN);

        String sessionId = loginEvent.getSessionId();

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);
        assertTrue(0 < tokenResponse.getRefreshExpiresIn() && tokenResponse.getRefreshExpiresIn() <= 1000, "Invalid ExpiresIn");
        String clientSessionId = getClientSessionUuid(sessionId, loginEvent.getClientId());
        assertEquals(2, checkIfUserAndClientSessionExist(sessionId, loginEvent.getClientId(), clientSessionId));

        events.poll();

        timeOffSet.set(600);
        String refreshId = oauth.parseRefreshToken(tokenResponse.getRefreshToken()).getId();
        tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());
        assertTrue(0 < tokenResponse.getRefreshExpiresIn() && tokenResponse.getRefreshExpiresIn() <= 400, "Invalid ExpiresIn");
        assertEquals(2, checkIfUserAndClientSessionExist(sessionId, loginEvent.getClientId(), clientSessionId));
        expectRefreshEventSuccess(refreshId, sessionId);

        timeOffSet.set(1100);
        tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());
        assertEquals(400, tokenResponse.getStatusCode());
        assertNull(tokenResponse.getAccessToken());
        assertNull(tokenResponse.getRefreshToken());
        expectRefreshEventError();
        assertEquals(1, checkIfUserAndClientSessionExist(sessionId, loginEvent.getClientId(), clientSessionId));

        timeOffSet.set(1600);
        oauth.openLoginForm();
        loginEvent = events.poll();
        EventAssertion.assertSuccess(loginEvent)
                .userId(user.getId())
                .clientId("test-app")
                .type(EventType.LOGIN);
        sessionId = loginEvent.getSessionId();
        code = oauth.parseLoginResponse().getCode();
        tokenResponse = oauth.doAccessTokenRequest(code);
        assertTrue(0 < tokenResponse.getRefreshExpiresIn() && tokenResponse.getRefreshExpiresIn() <= 1000, "Invalid ExpiresIn");
        events.poll();

        clientSessionId = getClientSessionUuid(sessionId, loginEvent.getClientId());
        assertEquals(2, checkIfUserAndClientSessionExist(sessionId, loginEvent.getClientId(), clientSessionId));

        timeOffSet.set(3700);
        tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());
        assertEquals(400, tokenResponse.getStatusCode());
        assertNull(tokenResponse.getAccessToken());
        assertNull(tokenResponse.getRefreshToken());
        expectRefreshEventError();
        assertEquals(0, checkIfUserAndClientSessionExist(sessionId, loginEvent.getClientId(), clientSessionId));
    }

    private void expectRefreshEventSuccess(String refreshId, String sessionId) {
        EventAssertion.assertSuccess(events.poll())
                .sessionId(sessionId)
                .details(Details.REFRESH_TOKEN_ID, refreshId)
                .type(EventType.REFRESH_TOKEN);
    }

    private void expectRefreshEventError() {
        EventAssertion.assertError(events.poll())
                .type(EventType.REFRESH_TOKEN_ERROR)
                .error(Errors.INVALID_TOKEN);
    }

    private boolean isPersistentSessionsFeatureEnabled() {
        return isPersistentSessionsFeatureEnabled(adminClient);
    }

    static boolean isPersistentSessionsFeatureEnabled(Keycloak adminClient) {
        ServerInfoRepresentation serverInfo = adminClient.serverInfo().getInfo();
        FeatureRepresentation feature = serverInfo.getFeatures().stream()
                .filter(feat -> Profile.Feature.PERSISTENT_USER_SESSIONS.name().equals(feat.getName()))
                .findFirst().orElseThrow(() -> new RuntimeException("Persistent user sessions feature not found"));
        return feature.isEnabled();
    }

    @Test
    public void refreshTokenUserSessionMaxLifespanModifiedAfterTokenRefresh() {
        isPersistentSessionsFeatureEnabled();
        realm.updateWithCleanup(r ->
                r.ssoSessionMaxLifespan(7200)
                        .ssoSessionIdleTimeout(7200)
                        .clientSessionMaxLifespan(7200)
                        .clientSessionIdleTimeout(7200)
        );

        oauth.doLogin("test-user@localhost", "password");
        EventRepresentation loginEvent = events.poll();
        EventAssertion.assertSuccess(loginEvent)
                .type(EventType.LOGIN);

        String sessionId = loginEvent.getSessionId();

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);
        assertTrue(0 < tokenResponse.getRefreshExpiresIn() && tokenResponse.getRefreshExpiresIn() <= 7200, "Invalid ExpiresIn");
        final String clientSessionId = getClientSessionUuid(sessionId, loginEvent.getClientId());
        assertEquals(2, checkIfUserAndClientSessionExist(sessionId, loginEvent.getClientId(), clientSessionId));

        events.poll();

        RealmRepresentation rep = realm.admin().toRepresentation();
        rep.setSsoSessionMaxLifespan(3600);
        rep.setClientSessionMaxLifespan(3600);
        realm.admin().update(rep);

        timeOffSet.set(3700);
        tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());
        assertEquals(400, tokenResponse.getStatusCode());
        assertNull(tokenResponse.getAccessToken());
        assertNull(tokenResponse.getRefreshToken());
        expectRefreshEventError();
        //events.assertRefreshTokenErrorAndMaybeSessionExpired(sessionId, loginEvent.getUserId(), loginEvent.getClientId());
        assertEquals(0, checkIfUserAndClientSessionExist(sessionId, loginEvent.getClientId(), clientSessionId));
    }

    @Test
    public void refreshTokenClientSessionMaxLifespanModifiedAfterTokenRefresh() {
        realm.updateWithCleanup(r ->
                r.ssoSessionMaxLifespan(7200)
                        .ssoSessionIdleTimeout(7200)
                        .clientSessionMaxLifespan(7200)
                        .clientSessionIdleTimeout(7200)
        );

        oauth.doLogin("test-user@localhost", "password");
        EventRepresentation loginEvent = events.poll();
        EventAssertion.assertSuccess(loginEvent)
                .type(EventType.LOGIN);

        String sessionId = loginEvent.getSessionId();

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);
        assertEquals(200, tokenResponse.getStatusCode());
        assertTrue(0 < tokenResponse.getRefreshExpiresIn() && tokenResponse.getRefreshExpiresIn() <= 7200, "Invalid ExpiresIn: " + tokenResponse.getRefreshExpiresIn());
        String clientSessionId = getClientSessionUuid(sessionId, loginEvent.getClientId());
        assertEquals(2, checkIfUserAndClientSessionExist(sessionId, loginEvent.getClientId(), clientSessionId));

        events.poll();

        RealmRepresentation rep = realm.admin().toRepresentation();
        rep.setClientSessionMaxLifespan(3600);
        realm.admin().update(rep);

        timeOffSet.set(3700);
        tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());
        assertEquals(400, tokenResponse.getStatusCode());
        assertNull(tokenResponse.getAccessToken());
        assertNull(tokenResponse.getRefreshToken());
        expectRefreshEventError();
        assertEquals(1, checkIfUserAndClientSessionExist(sessionId, loginEvent.getClientId(), clientSessionId));

        timeOffSet.set(4200);
        oauth.openLoginForm();
        loginEvent = events.poll();
        EventAssertion.assertSuccess(loginEvent)
                .type(EventType.LOGIN);
        sessionId = loginEvent.getSessionId();
        code = oauth.parseLoginResponse().getCode();
        tokenResponse = oauth.doAccessTokenRequest(code);
        assertEquals(200, tokenResponse.getStatusCode());
        assertTrue(0 < tokenResponse.getRefreshExpiresIn() && tokenResponse.getRefreshExpiresIn() <= 3000, "Invalid ExpiresIn: " + tokenResponse.getRefreshExpiresIn());
        EventAssertion.assertSuccess(events.poll())
                .type(EventType.CODE_TO_TOKEN);

        clientSessionId = getClientSessionUuid(sessionId, loginEvent.getClientId());
        assertEquals(2, checkIfUserAndClientSessionExist(sessionId, loginEvent.getClientId(), clientSessionId));

        timeOffSet.set(7300);
        tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());
        assertEquals(400, tokenResponse.getStatusCode());
        assertNull(tokenResponse.getAccessToken());
        assertNull(tokenResponse.getRefreshToken());
        expectRefreshEventError();
        assertEquals(0, checkIfUserAndClientSessionExist(sessionId, loginEvent.getClientId(), clientSessionId));
    }

    @Test
    public void silentLoginClientSessionMaxLifespanModifiedAfterTokenRefresh() {
        realm.updateWithCleanup(r ->
                r.ssoSessionMaxLifespan(7200)
                        .ssoSessionIdleTimeout(7200)
                        .clientSessionMaxLifespan(7200)
                        .clientSessionIdleTimeout(7200)
        );

        oauth.doLogin("test-user@localhost", "password");
        EventRepresentation loginEvent = events.poll();
        EventAssertion.assertSuccess(loginEvent)
                .type(EventType.LOGIN);

        String sessionId = loginEvent.getSessionId();

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);
        assertTrue(0 < tokenResponse.getRefreshExpiresIn() && tokenResponse.getRefreshExpiresIn() <= 7200, "Invalid ExpiresIn");
        String clientSessionId = getClientSessionUuid(sessionId, loginEvent.getClientId());
        assertEquals(2, checkIfUserAndClientSessionExist(sessionId, loginEvent.getClientId(), clientSessionId));

        events.poll();

        RealmRepresentation rep = realm.admin().toRepresentation();
        rep.setClientSessionMaxLifespan(3600);
        realm.admin().update(rep);

        timeOffSet.set(4200);
        oauth.openLoginForm();
        loginEvent = events.poll();
        EventAssertion.assertSuccess(loginEvent)
                .type(EventType.LOGIN);
        sessionId = loginEvent.getSessionId();
        code = oauth.parseLoginResponse().getCode();
        tokenResponse = oauth.doAccessTokenRequest(code);
        assertTrue(0 < tokenResponse.getRefreshExpiresIn() && tokenResponse.getRefreshExpiresIn() <= 3000, "Invalid ExpiresIn");
        EventAssertion.assertSuccess(events.poll())
                .type(EventType.CODE_TO_TOKEN);

        clientSessionId = getClientSessionUuid(sessionId, loginEvent.getClientId());
        assertEquals(2, checkIfUserAndClientSessionExist(sessionId, loginEvent.getClientId(), clientSessionId));

        timeOffSet.set(7300);
        tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());
        assertEquals(400, tokenResponse.getStatusCode());
        assertNull(tokenResponse.getAccessToken());
        assertNull(tokenResponse.getRefreshToken());
        expectRefreshEventError();
        assertEquals(0, checkIfUserAndClientSessionExist(sessionId, loginEvent.getClientId(), clientSessionId));
    }

    /**
     * KEYCLOAK-1267
     */
    @Test
    public void refreshTokenUserSessionMaxLifespanWithRememberMe() {
        realm.updateWithCleanup(r -> r
                .setRememberMe(true)
                .ssoSessionMaxLifespanRememberMe(100)
                .ssoSessionMaxLifespan(50));

        oauth.openLoginForm();
        loginPage.rememberMe(true);
        loginPage.fillLogin("test-user@localhost", "password");
        loginPage.submit();

        EventRepresentation loginEvent = events.poll();
        EventAssertion.assertSuccess(loginEvent)
                .type(EventType.LOGIN);

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);

        events.poll();

        timeOffSet.set(110);

        tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());

        assertEquals(400, tokenResponse.getStatusCode());
        assertNull(tokenResponse.getAccessToken());
        assertNull(tokenResponse.getRefreshToken());

        expectRefreshEventError();
        events.clear();
    }


    @Test
    public void refreshTokenClientSessionMaxLifespan() {
        RealmResource realm = this.realm.admin();
        RealmRepresentation rep = realm.toRepresentation();
        Integer originalSsoSessionMaxLifespan = rep.getSsoSessionMaxLifespan();

        ClientResource client = managedClient.admin();
        ClientRepresentation clientRepresentation = client.toRepresentation();

        try {
            rep.setSsoSessionMaxLifespan(1000);
            realm.update(rep);

            clientRepresentation.getAttributes().put(CLIENT_SESSION_MAX_LIFESPAN, "500");
            client.update(clientRepresentation);

            oauth.doLogin("test-user@localhost", "password");

            EventRepresentation loginEvent = events.poll();
            EventAssertion.assertSuccess(loginEvent)
                    .type(EventType.LOGIN);

            String code = oauth.parseLoginResponse().getCode();
            AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);

            events.poll();

            tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());
            assertTrue(0 < tokenResponse.getRefreshExpiresIn() && tokenResponse.getRefreshExpiresIn() <= 500, "Invalid RefreshExpiresIn" + tokenResponse.getRefreshExpiresIn());

            timeOffSet.set(100);

            tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());
            assertTrue(0 < tokenResponse.getRefreshExpiresIn() && tokenResponse.getRefreshExpiresIn() <= 400, "Invalid RefreshExpiresIn");

            timeOffSet.set(600);

            oauth.openLoginForm();
            code = oauth.parseLoginResponse().getCode();

            tokenResponse = oauth.doAccessTokenRequest(code);
            assertEquals(200, tokenResponse.getStatusCode());
            assertTrue(0 < tokenResponse.getRefreshExpiresIn() && tokenResponse.getRefreshExpiresIn() <= 400, "Invalid RefreshExpiresIn" + tokenResponse.getRefreshExpiresIn());

            timeOffSet.set(700);

            tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());
            assertEquals(200, tokenResponse.getStatusCode());
            assertTrue(0 < tokenResponse.getRefreshExpiresIn() && tokenResponse.getRefreshExpiresIn() <= 300, "Invalid RefreshExpiresIn" + tokenResponse.getRefreshExpiresIn());

            timeOffSet.set(1100);

            tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());
            assertEquals(400, tokenResponse.getStatusCode());
            assertNull(tokenResponse.getAccessToken());
            assertNull(tokenResponse.getRefreshToken());

            expectRefreshEventError();
        } finally {
            rep.setSsoSessionMaxLifespan(originalSsoSessionMaxLifespan);
            realm.update(rep);
            clientRepresentation.getAttributes().put(CLIENT_SESSION_MAX_LIFESPAN, null);
            client.update(clientRepresentation);
        }
    }

    /**
     * This is a very esoteric test specific to bug <a href="https://github.com/keycloak/keycloak/issues/38591">#38591</a>.
     * Consider removing or rewriting the test if the loading of sessions from the database has changed and no longer
     * updates the client session timestamp. It is also specific to the case when the idle timeout of a client is reduced
     * while some client sessions already exist.
     */
    @Test
    public void refreshTokenClientSessionIdleTimeoutTwoClientsWithReloadingFromDatabase() {
        Assumptions.assumeTrue(isPersistentSessionsFeatureEnabled(), "Skip as persistent_user_sessions feature is disabled");

        RealmResource realm = this.realm.admin();

        ClientResource client = managedClient.admin();
        ClientRepresentation clientRepresentation = client.toRepresentation();

        // Duplicate the primary client to have two clients to test with
        ClientRepresentation clientRepresentation2 = client.toRepresentation();
        clientRepresentation2.setClientId("test-app2");
        clientRepresentation2.getAttributes().put(CLIENT_SESSION_IDLE_TIMEOUT, "500");
        clientRepresentation2.setId(null);
        String clientUUID;
        try (Response resp = realm.clients().create(clientRepresentation2)) {
            clientUUID = ApiUtil.getCreatedId(resp);
        }

        String origClientId = oauth.getClientId();
        String origClientSecret = oauth.config().getClientSecret();

        try {
            oauth.doLogin("test-user@localhost", "password");

            EventRepresentation loginEvent = events.poll();
            EventAssertion.assertSuccess(loginEvent)
                    .type(EventType.LOGIN);
            String sessionId = loginEvent.getSessionId();

            String code = oauth.parseLoginResponse().getCode();
            AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);

            // Reduce the idle time so that the originally issued refresh token is valid, but it will be considered invalid due to the client configuration
            clientRepresentation.getAttributes().put(CLIENT_SESSION_IDLE_TIMEOUT, "500");
            client.update(clientRepresentation);

            oauth.client("test-app2", origClientSecret);

            // We are already logged in due to the token
            oauth.openLoginForm();

            String code2 = oauth.parseLoginResponse().getCode();
            AccessTokenResponse tokenResponse2 = oauth.doAccessTokenRequest(code2);

            assertThat(sessionId, Matchers.equalTo(tokenResponse2.getSessionState()));

            timeOffSet.set(100);

            tokenResponse2 = oauth.doRefreshTokenRequest(tokenResponse2.getRefreshToken());
            assertEquals(200, tokenResponse2.getStatusCode());
            assertTrue(0 < tokenResponse2.getRefreshExpiresIn() && tokenResponse2.getRefreshExpiresIn() <= 500, "Invalid RefreshExpiresIn: " + tokenResponse2.getRefreshExpiresIn());

            // Clear all entries from the cache to enforce re-loading the data from the database
            runOnServer.run(session -> {
                InfinispanConnectionProvider connections = session.getProvider(InfinispanConnectionProvider.class);
                if (connections != null) {
                    Cache<String, SessionEntityWrapper<UserSessionEntity>> sessionCache = connections.getCache(USER_SESSION_CACHE_NAME);
                    Cache<UUID, SessionEntityWrapper<AuthenticatedClientSessionEntity>> clientSessionCache = connections.getCache(CLIENT_SESSION_CACHE_NAME);
                    if (sessionCache != null) {
                        sessionCache.clear();
                    }
                    if (clientSessionCache != null) {
                        clientSessionCache.clear();
                    }
                }
            });

            timeOffSet.set(550);
            oauth.client(origClientId, origClientSecret);
            events.poll();

            // The client session of the first client should have expired by now
            tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());

            assertEquals(400, tokenResponse.getStatusCode());
            assertNull(tokenResponse.getAccessToken());
            assertNull(tokenResponse.getRefreshToken());
            expectRefreshEventError();
        } finally {
            clientRepresentation.getAttributes().put(CLIENT_SESSION_IDLE_TIMEOUT, "");
            client.update(clientRepresentation);
            Response ignored = realm.clients().delete(clientUUID);
            ignored.close();
        }
    }


    @Test
    public void testClientSessionMaxLifespan() {
        ClientResource client = managedClient.admin();
        ClientRepresentation clientRepresentation = client.toRepresentation();

        RealmResource realm = this.realm.admin();
        RealmRepresentation rep = realm.toRepresentation();
        Integer originalSsoSessionMaxLifespan = rep.getSsoSessionMaxLifespan();
        int ssoSessionMaxLifespan = rep.getSsoSessionIdleTimeout() - 100;
        Integer originalClientSessionMaxLifespan = rep.getClientSessionMaxLifespan();

        try {
            rep.setSsoSessionMaxLifespan(ssoSessionMaxLifespan);
            realm.update(rep);

            oauth.doLogin("test-user@localhost", "password");
            String code = oauth.parseLoginResponse().getCode();
            AccessTokenResponse response = oauth.doAccessTokenRequest(code);
            assertEquals(200, response.getStatusCode());
            Assert.assertExpiration(response.getRefreshExpiresIn(), ssoSessionMaxLifespan);

            rep.setClientSessionMaxLifespan(ssoSessionMaxLifespan - 100);
            realm.update(rep);

            String refreshToken = response.getRefreshToken();
            response = oauth.doRefreshTokenRequest(refreshToken);
            assertEquals(200, response.getStatusCode());
            Assert.assertExpiration(response.getRefreshExpiresIn(), ssoSessionMaxLifespan - 100);

            clientRepresentation.getAttributes().put(CLIENT_SESSION_MAX_LIFESPAN,
                    Integer.toString(ssoSessionMaxLifespan - 200));
            client.update(clientRepresentation);

            refreshToken = response.getRefreshToken();
            response = oauth.doRefreshTokenRequest(refreshToken);
            assertEquals(200, response.getStatusCode());
            Assert.assertExpiration(response.getRefreshExpiresIn(), ssoSessionMaxLifespan - 200);
        } finally {
            rep.setSsoSessionMaxLifespan(originalSsoSessionMaxLifespan);
            rep.setClientSessionMaxLifespan(originalClientSessionMaxLifespan);
            realm.update(rep);
            clientRepresentation.getAttributes().put(CLIENT_SESSION_MAX_LIFESPAN, "");
            client.update(clientRepresentation);
        }
    }

    @Test
    public void testClientSessionIdleTimeout() {
        ClientResource client = managedClient.admin();
        ClientRepresentation clientRepresentation = client.toRepresentation();

        RealmResource realm = this.realm.admin();
        RealmRepresentation rep = realm.toRepresentation();
        int ssoSessionIdleTimeout = rep.getSsoSessionIdleTimeout();
        Integer originalClientSessionIdleTimeout = rep.getClientSessionIdleTimeout();

        try {
            oauth.doLogin("test-user@localhost", "password");
            String code = oauth.parseLoginResponse().getCode();
            AccessTokenResponse response = oauth.doAccessTokenRequest(code);
            assertEquals(200, response.getStatusCode());
            Assert.assertExpiration(response.getRefreshExpiresIn(), ssoSessionIdleTimeout);

            rep.setClientSessionIdleTimeout(ssoSessionIdleTimeout - 100);
            realm.update(rep);

            String refreshToken = response.getRefreshToken();
            response = oauth.doRefreshTokenRequest(refreshToken);
            assertEquals(200, response.getStatusCode());
            Assert.assertExpiration(response.getRefreshExpiresIn(), ssoSessionIdleTimeout - 100);

            clientRepresentation.getAttributes().put(CLIENT_SESSION_IDLE_TIMEOUT,
                    Integer.toString(ssoSessionIdleTimeout - 200));
            client.update(clientRepresentation);

            refreshToken = response.getRefreshToken();
            response = oauth.doRefreshTokenRequest(refreshToken);
            assertEquals(200, response.getStatusCode());
            Assert.assertExpiration(response.getRefreshExpiresIn(), ssoSessionIdleTimeout - 200);
        } finally {
            rep.setClientSessionIdleTimeout(originalClientSessionIdleTimeout);
            realm.update(rep);
            clientRepresentation.getAttributes().put(CLIENT_SESSION_IDLE_TIMEOUT, "");
            client.update(clientRepresentation);
        }
    }

    @Test // KEYCLOAK-17323
    public void testRefreshTokenWhenClientSessionTimeoutPassedButRealmDidNot() {
        realm.updateWithCleanup(r -> r
                .ssoSessionIdleTimeout(2592000) // 30 Days
                .ssoSessionMaxLifespan(86313600) // 999 Days
        );

        ClientResource client = managedClient.admin();
        ClientRepresentation clientRepresentation = client.toRepresentation();
        clientRepresentation.getAttributes().put(CLIENT_SESSION_IDLE_TIMEOUT, "60"); // 1 minute
        clientRepresentation.getAttributes().put(CLIENT_SESSION_MAX_LIFESPAN, "65"); // 1 minute 5 seconds
        client.update(clientRepresentation);

        try {
            oauth.doLogin("test-user@localhost", "password");
            String code = oauth.parseLoginResponse().getCode();
            AccessTokenResponse response = oauth.doAccessTokenRequest(code);
            assertEquals(200, response.getStatusCode());
            Assert.assertExpiration(response.getExpiresIn(), 65);

            timeOffSet.set(70);

            oauth.openLoginForm();
            code = oauth.parseLoginResponse().getCode();
            AccessTokenResponse response2 = oauth.doAccessTokenRequest(code);
            Assert.assertExpiration(response2.getExpiresIn(), 65);
        } finally {
            clientRepresentation.getAttributes().put(CLIENT_SESSION_IDLE_TIMEOUT, "");
            clientRepresentation.getAttributes().put(CLIENT_SESSION_MAX_LIFESPAN, "");
            client.update(clientRepresentation);
        }
    }


}

package org.keycloak.tests.oauth;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.ws.rs.NotFoundException;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.constants.ServiceAccountConstants;
import org.keycloak.crypto.Algorithm;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.session.UserSessionPersisterProvider;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.encode.AccessTokenContext;
import org.keycloak.protocol.oidc.encode.TokenContextEncoderProvider;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
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
import org.keycloak.testframework.realm.RoleConfigBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.tests.utils.admin.AdminApiUtil;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.util.TokenUtil;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.tests.utils.admin.AdminApiUtil.findRealmRoleByName;
import static org.keycloak.tests.utils.admin.AdminApiUtil.findUserByUsername;
import static org.keycloak.tests.utils.admin.AdminApiUtil.findUserByUsernameId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class OfflineTokenBasicFlowTest {

    private static final String OFFLINE_CLIENT_ID = "offline-client";
    private static final String OFFLINE_CLIENT_APP_URI = "http://localhost:8080/offline-client";
    private static final String TEST_APP_REDIRECT_URI = "http://localhost:8080/auth/realms/test/app/auth";
    private String userId;
    private String serviceAccountUserId;

    @InjectRealm(config = OfflineTokenBasicFlowTest.OfflineTokenRealmConfig.class)
    ManagedRealm realm;

    @InjectOAuthClient(config = OfflineTokenBasicFlowTest.OfflineAuthClientConfig.class)
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

        // Fetch the auto-generated service account user
        serviceAccountUserId = realm.admin().users()
                .search(ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX + OFFLINE_CLIENT_ID, true)
                .get(0).getId();

        userId = findUserByUsername(adminClient.realm("test"), "test-user@localhost").getId();

        events.clear();
    }

    @Test
    public void offlineTokenDisabledForClient() {
        // Remove offline-access scope from client
        ClientScopeRepresentation offlineScope = adminClient.realm("test").clientScopes().findAll().stream()
                .filter((ClientScopeRepresentation clientScope) -> OAuth2Constants.OFFLINE_ACCESS.equals(clientScope.getName()))
                .findFirst().orElseThrow();

        ClientResource clientResource = realm.admin().clients()
                .get(realm.admin().clients().findByClientId("offline-client").get(0).getId());

        ClientRepresentation client = clientResource.toRepresentation();
        client.setFullScopeAllowed(false);
        clientResource.update(client);
        clientResource.removeOptionalClientScope(offlineScope.getId());

        try {
            // Test that offline access is denied
            oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
            oauth.client("offline-client", "secret1");
            oauth.redirectUri(OFFLINE_CLIENT_APP_URI);
            oauth.openLoginForm();

            EventRepresentation errorEvent = events.poll();
            EventAssertion.assertError(errorEvent)
                    .type(EventType.LOGIN_ERROR)
                    .clientId("offline-client")
                    .error(Errors.INVALID_REQUEST)
                    .details(Details.REASON, "Invalid scopes: openid offline_access");

        } finally {
            // Revert changes
            client.setFullScopeAllowed(true);
            clientResource.update(client);
            clientResource.addOptionalClientScope(offlineScope.getId());
        }
    }

    @Test
    public void offlineTokenUserNotAllowed() {
        String userId = realm.admin().users()
                .search("keycloak-user@localhost", true)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("User 'keycloak-user@localhost' not found!"))
                .getId();

        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        oauth.client("offline-client", "secret1");
        oauth.redirectUri(OFFLINE_CLIENT_APP_URI);
        oauth.doLogin("keycloak-user@localhost", "password");

        EventRepresentation loginEvent = events.poll();
        EventAssertion.assertSuccess(loginEvent)
                .type(EventType.LOGIN)
                .clientId("offline-client")
                .userId(userId)
                .details(Details.REDIRECT_URI, OFFLINE_CLIENT_APP_URI);

        String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);

        String code = oauth.parseLoginResponse().getCode();

        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);

        assertEquals(400, tokenResponse.getStatusCode());
        assertEquals("not_allowed", tokenResponse.getError());

        EventRepresentation tokenEvent = events.poll();
        EventAssertion.assertError(tokenEvent)
                .type(EventType.CODE_TO_TOKEN_ERROR)
                .clientId("offline-client")
                .userId(userId)
                .sessionId(sessionId)
                .error("not_allowed")
                .details(Details.CODE_ID, codeId);
    }

    @Test
    public void offlineTokenBrowserFlow() {
        setupCustomerUserRoles();

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
        AccessToken token = oauth.verifyToken(tokenResponse.getAccessToken());
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
        Assertions.assertNull(offlineToken.getExp());

        AccessTokenContext ctx = runOnServer.fetch(session -> {
            return session.getProvider(TokenContextEncoderProvider.class)
                    .getTokenContextFromTokenId(token.getId());
        }, AccessTokenContext.class);
        Assertions.assertEquals(AccessTokenContext.SessionType.OFFLINE, ctx.getSessionType());
        Assertions.assertEquals(AccessTokenContext.TokenType.REGULAR, ctx.getTokenType());
        Assertions.assertEquals(OAuth2Constants.AUTHORIZATION_CODE, ctx.getGrantType());

        assertTrue(tokenResponse.getScope().contains(OAuth2Constants.OFFLINE_ACCESS));

        // check only offline session is created
        checkNumberOfSessions(userId, "offline-client", offlineToken.getSessionId(), 0, 1);

        String newRefreshTokenString = testRefreshWithOfflineToken(token, offlineToken, offlineTokenString, sessionId, userId);

        // Change offset to very big value to ensure offline session expires
        timeOffSet.set(3000000);

        AccessTokenResponse response = oauth.doRefreshTokenRequest(newRefreshTokenString);
        RefreshToken newRefreshToken = oauth.parseRefreshToken(newRefreshTokenString);
        Assertions.assertEquals(400, response.getStatusCode());
        assertEquals("invalid_grant", response.getError());

        EventRepresentation refreshErrorEvent = events.poll();
        EventAssertion.assertError(refreshErrorEvent)
                .type(EventType.REFRESH_TOKEN_ERROR)
                .sessionId(newRefreshToken.getSessionId())
                //.userId(loginEvent.getUserId())
                .clientId("offline-client")
                .error(Errors.INVALID_TOKEN)
                .details(Details.REFRESH_TOKEN_SUB, loginEvent.getUserId());
        timeOffSet.set(0);
    }


    @Test
    public void onlineOfflineTokenBrowserFlow() {
        // request an online token for the client
        oauth.scope(null);
        oauth.client("offline-client", "secret1");
        oauth.redirectUri(OFFLINE_CLIENT_APP_URI);
        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation onlineLoginEvent = events.poll();
        EventAssertion.assertSuccess(onlineLoginEvent)
                .type(EventType.LOGIN)
                .clientId("offline-client")
                .details(Details.REDIRECT_URI, OFFLINE_CLIENT_APP_URI);

        final String onlineSessionId = onlineLoginEvent.getSessionId();
        String codeId = onlineLoginEvent.getDetails().get(Details.CODE_ID);
        AccessTokenResponse onlineTokenResponse = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        RefreshToken onlineRefreshToken = assertRefreshToken(onlineTokenResponse, TokenUtil.TOKEN_TYPE_REFRESH);

        EventRepresentation onlineCodeToTokenEvent = events.poll();
        EventAssertion.assertSuccess(onlineCodeToTokenEvent)
                .type(EventType.CODE_TO_TOKEN)
                .clientId("offline-client")
                .sessionId(onlineSessionId)
                .details(Details.CODE_ID, codeId)
                .details(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_REFRESH);
        assertEquals(TokenUtil.TOKEN_TYPE_REFRESH, onlineRefreshToken.getType());
        Assertions.assertNotNull(onlineRefreshToken.getExp());

        // request an offline token for the same client
        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        oauth.openLoginForm();
        EventRepresentation offlineLoginEvent = events.poll();
        EventAssertion.assertSuccess(offlineLoginEvent)
                .type(EventType.LOGIN)
                .clientId("offline-client")
                .details(Details.REDIRECT_URI, OFFLINE_CLIENT_APP_URI);
        AccessTokenResponse offlineTokenResponse = oauth.doAccessTokenRequest(
                oauth.parseLoginResponse().getCode());
        RefreshToken offlineRefreshToken = assertRefreshToken(offlineTokenResponse, TokenUtil.TOKEN_TYPE_OFFLINE);
        final String offlineSessionId = offlineLoginEvent.getSessionId();

        EventRepresentation offlineCodeToTokenEvent = events.poll();
        EventAssertion.assertSuccess(offlineCodeToTokenEvent)
                .type(EventType.CODE_TO_TOKEN)
                .clientId("offline-client")
                .sessionId(onlineSessionId)
                .details(Details.CODE_ID, codeId)
                .details(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_OFFLINE);
        assertEquals(TokenUtil.TOKEN_TYPE_OFFLINE, offlineRefreshToken.getType());
        Assertions.assertNull(offlineRefreshToken.getExp());
        assertTrue(offlineTokenResponse.getScope().contains(OAuth2Constants.OFFLINE_ACCESS));

        // check both sessions are created
        checkNumberOfSessions(userId, "offline-client", onlineRefreshToken.getSessionId(), 1, 1);

        // check online token can be refreshed
        onlineTokenResponse = oauth.doRefreshTokenRequest(onlineTokenResponse.getRefreshToken());
        assertRefreshToken(onlineTokenResponse, TokenUtil.TOKEN_TYPE_REFRESH);
        AccessToken renewedOnlineAccessToken = oauth.verifyToken(onlineTokenResponse.getAccessToken());

        EventRepresentation onlineRefreshEvent = events.poll();
        EventAssertion.assertSuccess(onlineRefreshEvent)
                .type(EventType.REFRESH_TOKEN)
                .clientId("offline-client")
                .userId(userId)
                .sessionId(onlineSessionId)
                .details(Details.TOKEN_ID, renewedOnlineAccessToken.getId())
                .details(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_REFRESH)
                .details(Details.REFRESH_TOKEN_ID, onlineRefreshToken.getId());

        // check offline token can be refreshed
        offlineTokenResponse = oauth.doRefreshTokenRequest(offlineTokenResponse.getRefreshToken());
        assertRefreshToken(offlineTokenResponse, TokenUtil.TOKEN_TYPE_OFFLINE);
        AccessToken renewedOfflineAccessToken = oauth.verifyToken(offlineTokenResponse.getAccessToken());

        EventRepresentation offlineRefreshEvent = events.poll();
        EventAssertion.assertSuccess(offlineRefreshEvent)
                .type(EventType.REFRESH_TOKEN)
                .clientId("offline-client")
                .userId(userId)
                .sessionId(offlineSessionId)
                .details(Details.TOKEN_ID, renewedOfflineAccessToken.getId())
                .details(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_OFFLINE)
                .details(Details.REFRESH_TOKEN_ID, offlineRefreshToken.getId());
    }

    @Test
    public void offlineTokenDirectGrantFlow() {
        setupCustomerUserRoles();

        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        oauth.client("offline-client", "secret1");
        AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest("test-user@localhost", "password");
        Assertions.assertNull(tokenResponse.getErrorDescription());
        AccessToken token = oauth.verifyToken(tokenResponse.getAccessToken());
        String offlineTokenString = tokenResponse.getRefreshToken();
        RefreshToken offlineToken = oauth.parseRefreshToken(offlineTokenString);

        EventRepresentation loginEvent = events.poll();
        EventAssertion.assertSuccess(loginEvent)
                .type(EventType.LOGIN)
                .clientId("offline-client")
                .userId(userId)
                .sessionId(token.getSessionId())
                .details(Details.GRANT_TYPE, OAuth2Constants.PASSWORD)
                .details(Details.TOKEN_ID, token.getId())
                .details(Details.REFRESH_TOKEN_ID, offlineToken.getId())
                .details(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_OFFLINE)
                .details(Details.USERNAME, "test-user@localhost");

        Assertions.assertEquals(TokenUtil.TOKEN_TYPE_OFFLINE, offlineToken.getType());
        Assertions.assertNull(offlineToken.getExp());

        // check only the offline session is created
        checkNumberOfSessions(userId, "offline-client", offlineToken.getSessionId(), 0, 1);

        // refresh token
        testRefreshWithOfflineToken(token, offlineToken, offlineTokenString, token.getSessionId(), userId);

        // Assert same token can be refreshed again
        testRefreshWithOfflineToken(token, offlineToken, offlineTokenString, token.getSessionId(), userId);
    }

    @Test
    public void offlineTokenDirectGrantFlowWithRefreshTokensRevoked() {
        setupCustomerUserRoles();
        realm.updateWithCleanup(r -> r.revokeRefreshToken(true));

        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        oauth.client("offline-client", "secret1");
        AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest("test-user@localhost", "password");

        AccessToken token = oauth.verifyToken(tokenResponse.getAccessToken());
        String offlineTokenString = tokenResponse.getRefreshToken();
        RefreshToken offlineToken = oauth.parseRefreshToken(offlineTokenString);

        EventRepresentation loginEvent = events.poll();
        EventAssertion.assertSuccess(loginEvent)
                .type(EventType.LOGIN)
                .clientId("offline-client")
                .userId(userId)
                .sessionId(token.getSessionId())
                .details(Details.GRANT_TYPE, OAuth2Constants.PASSWORD)
                .details(Details.TOKEN_ID, token.getId())
                .details(Details.REFRESH_TOKEN_ID, offlineToken.getId())
                .details(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_OFFLINE)
                .details(Details.USERNAME, "test-user@localhost");

        Assertions.assertEquals(TokenUtil.TOKEN_TYPE_OFFLINE, offlineToken.getType());
        Assertions.assertNull(offlineToken.getExp());

        String offlineTokenString2 = testRefreshWithOfflineToken(token, offlineToken, offlineTokenString, token.getSessionId(), userId);
        RefreshToken offlineToken2 = oauth.parseRefreshToken(offlineTokenString2);

        // Clear the events queue to prevent any pollution from time-shifted events
        // generated inside testRefreshWithOfflineToken
        events.clear();

        // Assert second refresh with same refresh token will fail
        AccessTokenResponse response = oauth.doRefreshTokenRequest(offlineTokenString);
        Assertions.assertEquals(400, response.getStatusCode());
        EventRepresentation refreshEvent = events.poll();
        EventAssertion.assertError(refreshEvent)
                .type(EventType.REFRESH_TOKEN_ERROR)
                .clientId("offline-client")
                .userId(null)
                .error(Errors.INVALID_TOKEN)
                .sessionId(token.getSessionId())
                .details(Details.REFRESH_TOKEN_ID, offlineToken.getId());

        // Refresh with new refreshToken fails as well (client session was invalidated because of attempt to refresh with revoked refresh token)
        AccessTokenResponse response2 = oauth.doRefreshTokenRequest(offlineTokenString2);
        Assertions.assertEquals(400, response2.getStatusCode());
        EventRepresentation refreshEvent2 = events.poll();
        EventAssertion.assertError(refreshEvent2)
                .type(EventType.REFRESH_TOKEN_ERROR)
                .clientId("offline-client")
                .userId(null)
                .error(Errors.INVALID_TOKEN)
                .sessionId(offlineToken2.getSessionId())
                .details(Details.REFRESH_TOKEN_ID, offlineToken2.getId());

        realm.updateWithCleanup(r -> r.revokeRefreshToken(false));
    }

    @Test
    public void offlineTokenServiceAccountFlow() {
        setupCustomerUserRoles();

        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        oauth.client("offline-client", "secret1");
        AccessTokenResponse tokenResponse = oauth.doClientCredentialsGrantAccessTokenRequest();

        AccessToken token = oauth.verifyToken(tokenResponse.getAccessToken());
        String offlineTokenString = tokenResponse.getRefreshToken();
        RefreshToken offlineToken = oauth.parseRefreshToken(offlineTokenString);

        EventRepresentation loginEvent = events.poll();
        EventAssertion.assertSuccess(loginEvent)
                .type(EventType.CLIENT_LOGIN)
                .clientId("offline-client")
                .userId(serviceAccountUserId)
                .sessionId(token.getSessionId())
                .details(Details.TOKEN_ID, token.getId())
                .details(Details.REFRESH_TOKEN_ID, offlineToken.getId())
                .details(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_OFFLINE)
                .details(Details.USERNAME, ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX + "offline-client");

        Assertions.assertEquals(TokenUtil.TOKEN_TYPE_OFFLINE, offlineToken.getType());
        Assertions.assertNull(offlineToken.getExp());

        // check only the offline session is created
        checkNumberOfSessions(serviceAccountUserId, "offline-client", offlineToken.getSessionId(), 0, 1);

        testRefreshWithOfflineToken(token, offlineToken, offlineTokenString, token.getSessionId(), serviceAccountUserId);

        // Now retrieve another offline token and verify that previous offline token is still valid
        tokenResponse = oauth.doClientCredentialsGrantAccessTokenRequest();

        AccessToken token2 = oauth.verifyToken(tokenResponse.getAccessToken());
        String offlineTokenString2 = tokenResponse.getRefreshToken();
        RefreshToken offlineToken2 = oauth.parseRefreshToken(offlineTokenString2);

        EventRepresentation loginEvent2 = events.poll();
        EventAssertion.assertSuccess(loginEvent2)
                .type(EventType.CLIENT_LOGIN)
                .clientId("offline-client")
                .userId(serviceAccountUserId)
                .sessionId(token2.getSessionId())
                .details(Details.TOKEN_ID, token2.getId())
                .details(Details.REFRESH_TOKEN_ID, offlineToken2.getId())
                .details(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_OFFLINE)
                .details(Details.USERNAME, ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX + "offline-client");

        // check only the offline session is created
        checkNumberOfSessions(serviceAccountUserId, "offline-client", offlineToken2.getSessionId(), 0, 1);

        // Refresh with both offline tokens is fine
        testRefreshWithOfflineToken(token, offlineToken, offlineTokenString, token.getSessionId(), serviceAccountUserId);
        testRefreshWithOfflineToken(token2, offlineToken2, offlineTokenString2, token2.getSessionId(), serviceAccountUserId);
    }


    @Test
    public void offlineTokenAllowedWithCompositeRole() {
        setupCustomerUserRoles();
        RealmResource appRealm = adminClient.realm("test");
        UserResource testUser = findUserByUsernameId(appRealm, "test-user@localhost");
        RoleRepresentation offlineAccess = findRealmRoleByName(adminClient.realm("test"),
                Constants.OFFLINE_ACCESS_ROLE).toRepresentation();

        // Grant offline_access role indirectly through composite role
        appRealm.roles().create(RoleConfigBuilder.create().name("composite").build());
        RoleResource roleResource = appRealm.roles().get("composite");
        roleResource.addComposites(Collections.singletonList(offlineAccess));

        testUser.roles().realmLevel().remove(Collections.singletonList(offlineAccess));
        testUser.roles().realmLevel().add(Collections.singletonList(roleResource.toRepresentation()));

        // Integration test
        offlineTokenDirectGrantFlow();

        // Revert changes
        testUser.roles().realmLevel().remove(Collections.singletonList(appRealm.roles().get("composite").toRepresentation()));
        appRealm.roles().get("composite").remove();
        testUser.roles().realmLevel().add(Collections.singletonList(offlineAccess));
    }

    /**
     * KEYCLOAK-4201
     *
     */
    @Test
    public void offlineTokenAdminRESTAccess() {
        // Grant "view-realm" role to user
        RealmResource appRealm = adminClient.realm("test");
        ClientResource realmMgmt = AdminApiUtil.findClientByClientId(appRealm, Constants.REALM_MANAGEMENT_CLIENT_ID);
        assert realmMgmt != null;
        String realmMgmtUuid = realmMgmt.toRepresentation().getId();
        RoleRepresentation roleRep = realmMgmt.roles().get(AdminRoles.VIEW_REALM).toRepresentation();

        UserResource testUser = findUserByUsernameId(appRealm, "test-user@localhost");
        testUser.roles().clientLevel(realmMgmtUuid).add(Collections.singletonList(roleRep));

        try {
            // Login with offline token now
            oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
            oauth.client("offline-client", "secret1");
            AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest("test-user@localhost", "password");

            events.clear();

            // Set the time offset, so that "normal" userSession expires
            timeOffSet.set(86400);

            // Remove expired sessions. This will remove "normal" userSession
            runOnServer.run(session -> {
                session.getProvider(UserSessionPersisterProvider.class).removeExpired(session.getContext().getRealm());
            });

            // Refresh with the offline token
            tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());
            Assertions.assertNull(tokenResponse.getError(), "received error " + tokenResponse.getError() + ", " + tokenResponse.getErrorDescription());

            // Use accessToken to admin REST request
            try (Keycloak offlineTokenAdmin = adminClientFactory.create()
                    .realm("master")
                    .authorization(tokenResponse.getAccessToken())
                    .clientId(Constants.ADMIN_CLI_CLIENT_ID)
                    .build()) {
                RealmRepresentation testRealm = offlineTokenAdmin.realm("test").toRepresentation();
                Assertions.assertNotNull(testRealm);
            }
        } finally {
            // clean up the admin role
            testUser.roles().clientLevel(realmMgmtUuid).remove(Collections.singletonList(roleRep));
        }
    }

    // KEYCLOAK-4525
    @Test
    public void offlineTokenRemoveClientWithTokens() {
        // Create new client
        RealmResource appRealm = adminClient.realm("test");

        ClientRepresentation clientRep = ClientConfigBuilder.create().clientId("offline-client-2")
                .id(KeycloakModelUtils.generateId())
                .directAccessGrantsEnabled(true)
                .secret("secret1").build();

        appRealm.clients().create(clientRep).close();

        // Direct grant login requesting offline token
        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        oauth.client("offline-client-2", "secret1");
        AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest("test-user@localhost", "password");
        Assertions.assertNull(tokenResponse.getErrorDescription());
        AccessToken token = oauth.verifyToken(tokenResponse.getAccessToken());
        String offlineTokenString = tokenResponse.getRefreshToken();
        RefreshToken offlineToken = oauth.parseRefreshToken(offlineTokenString);

        EventRepresentation loginEvent = events.poll();
        EventAssertion.assertSuccess(loginEvent)
                .type(EventType.LOGIN)
                .clientId("offline-client-2")
                .userId(userId)
                .sessionId(token.getSessionId())
                .details(Details.GRANT_TYPE, OAuth2Constants.PASSWORD)
                .details(Details.TOKEN_ID, token.getId())
                .details(Details.REFRESH_TOKEN_ID, offlineToken.getId())
                .details(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_OFFLINE)
                .details(Details.USERNAME, "test-user@localhost");

        // Confirm that offline-client-2 token was granted
        List<Map<String, Object>> userConsents = AccountHelper.getUserConsents(adminClient.realm("test"), "test-user@localhost");

        String clientId2 = "", offlineAdditionalGrant = "";
        for (Map<String, Object> consent : userConsents) {
            if (consent.get("clientId").equals("offline-client-2")) {
                clientId2 = String.valueOf(consent.get("clientId"));
                //noinspection unchecked
                offlineAdditionalGrant = String.valueOf((((List<Map<String, ?>>) consent.get("additionalGrants")).get(0)).get("key"));
            }
        }

        assertEquals("offline-client-2", clientId2);
        assertEquals("Offline Token", offlineAdditionalGrant);

        // Now remove the client
        ClientResource offlineTokenClient2 = AdminApiUtil.findClientByClientId(appRealm, "offline-client-2" );
        assert offlineTokenClient2 != null;
        offlineTokenClient2.remove();

        // Confirm that offline-client-2 token was deleted
        assertNull(AdminApiUtil.findClientByClientId(appRealm, "offline-client-2"));

        // Login as admin and see consents of user
        UserResource user = AdminApiUtil.findUserByUsernameId(appRealm, "test-user@localhost");
        List<Map<String, Object>> consents = user.getConsents();
        for (Map<String, Object> consent : consents) {
            assertNotEquals("offline-client-2", consent.get("clientId"));
        }
    }


    @Test
    public void offlineTokenRequest_ClientES256_RealmPS256() throws Exception {
        conductOfflineTokenRequest(Constants.INTERNAL_SIGNATURE_ALGORITHM, Algorithm.ES256, Algorithm.PS256);
    }

    @Test
    public void offlineTokenRequest_ClientPS256_RealmES256() throws Exception {
        conductOfflineTokenRequest(Constants.INTERNAL_SIGNATURE_ALGORITHM, Algorithm.PS256, Algorithm.ES256);
    }

    private void setupCustomerUserRoles() {
        String testAppClientUuid = realm.admin().clients().findByClientId("test-app").get(0).getId();
        ClientResource testAppClient = realm.admin().clients().get(testAppClientUuid);

        try {
            RoleRepresentation customerUserRole = new RoleRepresentation();
            customerUserRole.setName("customer-user");
            testAppClient.roles().create(customerUserRole);
        } catch (Exception e) {
            // Role already exists
        }

        RoleRepresentation customerUserRole = testAppClient.roles().get("customer-user").toRepresentation();

        // Assign to test-user
        UserResource testUser = realm.admin().users().get(userId);
        testUser.roles().clientLevel(testAppClientUuid).add(Collections.singletonList(customerUserRole));

        // Assign to service account
        UserResource serviceAccountUser = realm.admin().users().get(serviceAccountUserId);
        serviceAccountUser.roles().clientLevel(testAppClientUuid).add(Collections.singletonList(customerUserRole));

        RoleRepresentation offlineAccessRole = realm.admin().roles().get(OAuth2Constants.OFFLINE_ACCESS).toRepresentation();
        RoleRepresentation userRole = realm.admin().roles().get("user").toRepresentation();
        serviceAccountUser.roles().realmLevel().add(java.util.Arrays.asList(offlineAccessRole, userRole));
    }


    private void checkNumberOfSessions(String userId, String clientId, String sessionId, int onlineSessions, int offlineSessions) {
        RealmResource realm = adminClient.realm("test");
        String clientUuid = Objects.requireNonNull(AdminApiUtil.findClientByClientId(realm, clientId)).toRepresentation().getId();
        Assertions.assertEquals(onlineSessions, realm.users().get(userId).getUserSessions()
                .stream().filter(s -> sessionId.equals(s.getId())).count());
        Assertions.assertEquals(offlineSessions, realm.users().get(userId).getOfflineSessions(clientUuid)
                .stream().filter(s -> sessionId.equals(s.getId())).count());
    }


    private String testRefreshWithOfflineToken(AccessToken oldToken, RefreshToken offlineToken, String offlineTokenString,
                                               final String sessionId, String userId) {
        // Change offset to big value to ensure userSession expired
        timeOffSet.set(99999);
        assertFalse(oldToken.isActive());
        assertTrue(offlineToken.isActive());

        // Assert userSession expired
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
        }

        AccessTokenResponse response = oauth.doRefreshTokenRequest(offlineTokenString);
        AccessToken refreshedToken = oauth.verifyToken(response.getAccessToken());
        Assertions.assertEquals(200, response.getStatusCode());
        AccessTokenContext ctx = runOnServer.fetch(session -> {
            return session.getProvider(TokenContextEncoderProvider.class)
                    .getTokenContextFromTokenId(refreshedToken.getId());
        }, AccessTokenContext.class);
        Assertions.assertEquals(AccessTokenContext.SessionType.OFFLINE, ctx.getSessionType());
        Assertions.assertEquals(AccessTokenContext.TokenType.REGULAR, ctx.getTokenType());
        Assertions.assertEquals(OAuth2Constants.REFRESH_TOKEN, ctx.getGrantType());

        // Assert new refreshToken in the response
        String newRefreshToken = response.getRefreshToken();
        RefreshToken newRefreshTokenFull = oauth.parseRefreshToken(newRefreshToken);
        Assertions.assertNotNull(newRefreshToken);
        Assertions.assertNotEquals(oldToken.getId(), refreshedToken.getId());

        // scope parameter either does not exist either contains offline_access
        assertTrue(refreshedToken.getScope().contains(OAuth2Constants.OFFLINE_ACCESS));

        // Assert refresh token scope parameter contains "offline_access"
        assertTrue(newRefreshTokenFull.getScope().contains(OAuth2Constants.OFFLINE_ACCESS));
        Assertions.assertEquals(TokenUtil.TOKEN_TYPE_OFFLINE, newRefreshTokenFull.getType());

        Assertions.assertEquals(userId, refreshedToken.getSubject());

        assertTrue(refreshedToken.getRealmAccess().isUserInRole("user"));
        assertTrue(refreshedToken.getRealmAccess().isUserInRole(Constants.OFFLINE_ACCESS_ROLE));

        Assertions.assertEquals(1, refreshedToken.getResourceAccess("test-app").getRoles().size());
        assertTrue(refreshedToken.getResourceAccess("test-app").isUserInRole("customer-user"));

        EventRepresentation refreshEvent = events.poll();
        EventAssertion.assertSuccess(refreshEvent)
                .type(EventType.REFRESH_TOKEN)
                .clientId("offline-client")
                .userId(userId)
                .sessionId(sessionId)
                .details(Details.REFRESH_TOKEN_ID, offlineToken.getId())
                .details(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_OFFLINE);
        Assertions.assertNotEquals(oldToken.getId(), refreshEvent.getDetails().get(Details.TOKEN_ID));

        timeOffSet.set(0);
        return newRefreshToken;
    }

    // Asserts that refresh token in the tokenResponse is of the given type. Return parsed token
    private RefreshToken assertRefreshToken(AccessTokenResponse tokenResponse, String tokenType) {
        Assertions.assertEquals(200, tokenResponse.getStatusCode());
        String offlineTokenString = tokenResponse.getRefreshToken();
        RefreshToken refreshToken = oauth.parseRefreshToken(offlineTokenString);
        assertEquals(tokenType, refreshToken.getType());
        return refreshToken;
    }

    private void conductOfflineTokenRequest(String expectedRefreshAlg, String expectedAccessAlg, String expectedIdTokenAlg) throws Exception {
        try {
            /// Realm Setting is used for ID Token Signature Algorithm
            setupCustomerUserRoles();
            changeRealmTokenSignatureProvider(expectedIdTokenAlg);
            changeClientAccessTokenSignatureProvider(AdminApiUtil.findClientByClientId(adminClient.realm("test"), "offline-client"), expectedAccessAlg);
            offlineTokenRequest(expectedRefreshAlg, expectedAccessAlg, expectedIdTokenAlg);
            offlineTokenRequestWithScopeParameter(expectedRefreshAlg, expectedAccessAlg, expectedIdTokenAlg);
        } finally {
            changeRealmTokenSignatureProvider(Algorithm.RS256);
            changeClientAccessTokenSignatureProvider(AdminApiUtil.findClientByClientId(adminClient.realm("test"), "offline-client"), Algorithm.RS256);
        }
    }


    private void offlineTokenRequest(String expectedRefreshAlg, String expectedAccessAlg, String expectedIdTokenAlg) throws Exception {
        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        oauth.client("offline-client", "secret1");
        AccessTokenResponse tokenResponse = oauth.doClientCredentialsGrantAccessTokenRequest();

        JWSHeader header;
        String idToken = tokenResponse.getIdToken();
        String accessToken = tokenResponse.getAccessToken();
        String refreshToken = tokenResponse.getRefreshToken();
        if (idToken != null) {
            header = new JWSInput(idToken).getHeader();
            assertEquals(expectedIdTokenAlg, header.getAlgorithm().name());
            assertEquals("JWT", header.getType());
            assertNull(header.getContentType());
        }
        if (accessToken != null) {
            header = new JWSInput(accessToken).getHeader();
            assertEquals(expectedAccessAlg, header.getAlgorithm().name());
            assertEquals("JWT", header.getType());
            assertNull(header.getContentType());
        }
        if (refreshToken != null) {
            header = new JWSInput(refreshToken).getHeader();
            assertEquals(expectedRefreshAlg, header.getAlgorithm().name());
            assertEquals("JWT", header.getType());
            assertNull(header.getContentType());
        }

        AccessToken token = oauth.verifyToken(tokenResponse.getAccessToken());
        String offlineTokenString = tokenResponse.getRefreshToken();
        RefreshToken offlineToken = oauth.parseRefreshToken(offlineTokenString);

        EventRepresentation clientLoginEvent = events.poll();
        EventAssertion.assertSuccess(clientLoginEvent)
                .type(EventType.CLIENT_LOGIN)
                .clientId("offline-client")
                .userId(serviceAccountUserId)
                .sessionId(token.getSessionId())
                .details(Details.TOKEN_ID, token.getId())
                .details(Details.REFRESH_TOKEN_ID, offlineToken.getId())
                .details(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_OFFLINE)
                .details(Details.USERNAME, ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX + "offline-client");

        Assertions.assertEquals(TokenUtil.TOKEN_TYPE_OFFLINE, offlineToken.getType());
        Assertions.assertNull(offlineToken.getExp());

        testRefreshWithOfflineToken(token, offlineToken, offlineTokenString, token.getSessionId(), serviceAccountUserId);

        // Now retrieve another offline token and decode that previous offline token is still valid
        tokenResponse = oauth.doClientCredentialsGrantAccessTokenRequest();

        AccessToken token2 = oauth.verifyToken(tokenResponse.getAccessToken());
        String offlineTokenString2 = tokenResponse.getRefreshToken();
        RefreshToken offlineToken2 = oauth.parseRefreshToken(offlineTokenString2);

        EventRepresentation clientLoginEvent2 = events.poll();
        EventAssertion.assertSuccess(clientLoginEvent2)
                .type(EventType.CLIENT_LOGIN)
                .clientId("offline-client")
                .userId(serviceAccountUserId)
                .sessionId(token2.getSessionId())
                .details(Details.TOKEN_ID, token2.getId())
                .details(Details.REFRESH_TOKEN_ID, offlineToken2.getId())
                .details(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_OFFLINE)
                .details(Details.USERNAME, ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX + "offline-client");

        // Refresh with both offline tokens is fine
        testRefreshWithOfflineToken(token, offlineToken, offlineTokenString, token.getSessionId(), serviceAccountUserId);
        testRefreshWithOfflineToken(token2, offlineToken2, offlineTokenString2, token2.getSessionId(), serviceAccountUserId);
    }


    private void offlineTokenRequestWithScopeParameter(String expectedRefreshAlg, String expectedAccessAlg, String expectedIdTokenAlg) throws Exception {
        ClientScopeRepresentation phoneScope = adminClient.realm("test").clientScopes().findAll().stream().filter((ClientScopeRepresentation clientScope) ->"phone".equals(clientScope.getName())).findFirst().get();
        ClientResource offlineClientResource = AdminApiUtil.findClientByClientId(adminClient.realm("test"), oauth.getClientId());
        offlineClientResource.addOptionalClientScope(phoneScope.getId());
        try {
            oauth.scope(OAuth2Constants.OFFLINE_ACCESS+" phone");
            oauth.client("offline-client", "secret1");
            AccessTokenResponse tokenResponse = oauth.doClientCredentialsGrantAccessTokenRequest();

            JWSHeader header;
            String idToken = tokenResponse.getIdToken();
            String accessToken = tokenResponse.getAccessToken();
            String refreshToken = tokenResponse.getRefreshToken();
            if (idToken != null) {
                header = new JWSInput(idToken).getHeader();
                assertEquals(expectedIdTokenAlg, header.getAlgorithm().name());
                assertEquals("JWT", header.getType());
                assertNull(header.getContentType());
            }
            if (accessToken != null) {
                header = new JWSInput(accessToken).getHeader();
                assertEquals(expectedAccessAlg, header.getAlgorithm().name());
                assertEquals("JWT", header.getType());
                assertNull(header.getContentType());
            }
            if (refreshToken != null) {
                header = new JWSInput(refreshToken).getHeader();
                assertEquals(expectedRefreshAlg, header.getAlgorithm().name());
                assertEquals("JWT", header.getType());
                assertNull(header.getContentType());
            }

            AccessToken token = oauth.verifyToken(tokenResponse.getAccessToken());
            String offlineTokenString = tokenResponse.getRefreshToken();
            RefreshToken offlineToken = oauth.parseRefreshToken(offlineTokenString);

            EventRepresentation clientLoginEvent3 = events.poll();
            EventAssertion.assertSuccess(clientLoginEvent3)
                    .type(EventType.CLIENT_LOGIN)
                    .clientId("offline-client")
                    .userId(serviceAccountUserId)
                    .sessionId(token.getSessionId())
                    .details(Details.TOKEN_ID, token.getId())
                    .details(Details.REFRESH_TOKEN_ID, offlineToken.getId())
                    .details(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_OFFLINE)
                    .details(Details.USERNAME, ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX + "offline-client");

            Assertions.assertEquals(TokenUtil.TOKEN_TYPE_OFFLINE, offlineToken.getType());
            Assertions.assertNull(offlineToken.getExp());
        } finally {
            //  remove the phone scope
            offlineClientResource.removeOptionalClientScope(phoneScope.getId());
        }
    }

    private void changeRealmTokenSignatureProvider(String toSigAlgName) {
        RealmRepresentation rep = realm.admin().toRepresentation();
        rep.setDefaultSignatureAlgorithm(toSigAlgName);
        realm.admin().update(rep);
    }

    private void changeClientAccessTokenSignatureProvider(ClientResource client, String toSigAlgName) {
        ClientRepresentation clientRep = client.toRepresentation();
        clientRep.getAttributes().put(OIDCConfigAttributes.ACCESS_TOKEN_SIGNED_RESPONSE_ALG, toSigAlgName);
        client.update(clientRep);
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
                        "CODE_TO_TOKEN_ERROR",
                        "REFRESH_TOKEN",
                        "REFRESH_TOKEN_ERROR",
                        "CLIENT_LOGIN"
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

            builder.addUser("keycloak-user@localhost")
                    .name("Keycloak", "User") // <-- Add this to satisfy VERIFY_PROFILE
                    .email("keycloak-user@localhost")
                    .emailVerified(true)
                    .password("password")
                    .roles("user");

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

/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.testsuite.oauth;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.NotFoundException;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.Profile;
import org.keycloak.common.constants.ServiceAccountConstants;
import org.keycloak.crypto.Algorithm;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.SessionTimeoutHelper;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.encode.AccessTokenContext;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.ProfileAssume;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.auth.page.AuthRealm;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.updaters.ClientAttributeUpdater;
import org.keycloak.testsuite.updaters.RealmAttributeUpdater;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.ClientManager;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.RealmManager;
import org.keycloak.testsuite.util.RoleBuilder;
import org.keycloak.testsuite.util.TokenSignatureUtil;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.IntrospectionResponse;
import org.keycloak.testsuite.util.oauth.LogoutResponse;
import org.keycloak.testsuite.utils.tls.TLSUtils;
import org.keycloak.util.TokenUtil;

import com.fasterxml.jackson.databind.JsonNode;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.keycloak.testsuite.AbstractAdminTest.loadJson;
import static org.keycloak.testsuite.Assert.assertExpiration;
import static org.keycloak.testsuite.admin.ApiUtil.findRealmRoleByName;
import static org.keycloak.testsuite.admin.ApiUtil.findUserByUsername;
import static org.keycloak.testsuite.admin.ApiUtil.findUserByUsernameId;
import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;
import static org.keycloak.testsuite.util.ServerURLs.getAuthServerContextRoot;
import static org.keycloak.testsuite.util.oauth.OAuthClient.APP_ROOT;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class OfflineTokenTest extends AbstractKeycloakTest {

    private static String userId;
    private static String offlineClientAppUri;
    private static String serviceAccountUserId;

    @Page
    protected LoginPage loginPage;

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Override
    public void beforeAbstractKeycloakTest() throws Exception {
        super.beforeAbstractKeycloakTest();
    }

    @Before
    public void clientConfiguration() {
        userId = findUserByUsername(adminClient.realm("test"), "test-user@localhost").getId();
        oauth.client("test-app");
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {

        RealmRepresentation realmRepresentation = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);

        RealmBuilder realm = RealmBuilder.edit(realmRepresentation)
                .accessTokenLifespan(10)
                .ssoSessionIdleTimeout(30)
                .testEventListener();

        offlineClientAppUri = APP_ROOT + "/offline-client";

        ClientRepresentation app = ClientBuilder.create().clientId("offline-client")
                .id(KeycloakModelUtils.generateId())
                .adminUrl(offlineClientAppUri)
                .redirectUris(offlineClientAppUri)
                .directAccessGrants()
                .serviceAccountsEnabled(true)
                .attribute(OIDCConfigAttributes.USE_REFRESH_TOKEN_FOR_CLIENT_CREDENTIALS_GRANT, "true")
                .secret("secret1").build();

        realm.client(app);

        UserRepresentation serviceAccountUser = UserBuilder.create()
                .id(serviceAccountUserId)
                .addRoles("user", "offline_access")
                .role("test-app", "customer-user")
                .username(ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX + app.getClientId())
                .serviceAccountId(app.getClientId()).build();

        realm.user(serviceAccountUser);

        testRealms.add(realm.build());

    }

    @Override
    public void importTestRealms() {
        super.importTestRealms();
        serviceAccountUserId = adminClient.realm("test").users().search(ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX + "offline-client", true).get(0).getId();
    }

    @Test
    public void offlineTokenDisabledForClient() {
        // Remove offline-access scope from client
        ClientScopeRepresentation offlineScope = adminClient.realm("test").clientScopes().findAll().stream()
                .filter((ClientScopeRepresentation clientScope) -> OAuth2Constants.OFFLINE_ACCESS.equals(clientScope.getName()))
                .findFirst().get();

        ClientManager.realm(adminClient.realm("test")).clientId("offline-client")
                .fullScopeAllowed(false)
                .removeClientScope(offlineScope.getId(), false);


        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        oauth.client("offline-client", "secret1");
        oauth.redirectUri(offlineClientAppUri);
        oauth.openLoginForm();
        assertTrue(driver.getCurrentUrl().contains("error_description=Invalid+scopes"));

        // Revert changes
        ClientManager.realm(adminClient.realm("test")).clientId("offline-client")
                .fullScopeAllowed(true)
                .addClientScope(offlineScope.getId(), false);

    }

    @Test
    public void offlineTokenUserNotAllowed() {
        String userId = findUserByUsername(adminClient.realm("test"), "keycloak-user@localhost").getId();

        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        oauth.client("offline-client", "secret1");
        oauth.redirectUri(offlineClientAppUri);
        oauth.doLogin("keycloak-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin()
                .client("offline-client")
                .user(userId)
                .detail(Details.REDIRECT_URI, offlineClientAppUri)
                .assertEvent();

        String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);

        String code = oauth.parseLoginResponse().getCode();

        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);

        assertEquals(400, tokenResponse.getStatusCode());
        assertEquals("not_allowed", tokenResponse.getError());

        events.expectCodeToToken(codeId, sessionId)
                .client("offline-client")
                .user(userId)
                .error("not_allowed")
                .clearDetails()
                .assertEvent();
    }

    @Test
    public void offlineTokenBrowserFlow() {
        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        oauth.client("offline-client", "secret1");
        oauth.redirectUri(offlineClientAppUri);
        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin()
                .client("offline-client")
                .detail(Details.REDIRECT_URI, offlineClientAppUri)
                .assertEvent();

        final String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);

        String code = oauth.parseLoginResponse().getCode();

        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);
        AccessToken token = oauth.verifyToken(tokenResponse.getAccessToken());
        String offlineTokenString = tokenResponse.getRefreshToken();
        RefreshToken offlineToken = oauth.parseRefreshToken(offlineTokenString);

        events.expectCodeToToken(codeId, sessionId)
                .client("offline-client")
                .detail(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_OFFLINE)
                .assertEvent();

        assertEquals(TokenUtil.TOKEN_TYPE_OFFLINE, offlineToken.getType());
        Assert.assertNull(offlineToken.getExp());

        AccessTokenContext ctx = testingClient.testing("test").getTokenContext(token.getId());
        Assert.assertEquals(AccessTokenContext.SessionType.OFFLINE, ctx.getSessionType());
        Assert.assertEquals(AccessTokenContext.TokenType.REGULAR, ctx.getTokenType());
        Assert.assertEquals(OAuth2Constants.AUTHORIZATION_CODE, ctx.getGrantType());

        assertTrue(tokenResponse.getScope().contains(OAuth2Constants.OFFLINE_ACCESS));

        // check only offline session is created
        checkNumberOfSessions(userId, "offline-client", offlineToken.getSessionId(), 0, 1);

        String newRefreshTokenString = testRefreshWithOfflineToken(token, offlineToken, offlineTokenString, sessionId, userId);

        // Change offset to very big value to ensure offline session expires
        setTimeOffset(3000000);

        AccessTokenResponse response = oauth.doRefreshTokenRequest(newRefreshTokenString);
        RefreshToken newRefreshToken = oauth.parseRefreshToken(newRefreshTokenString);
        Assert.assertEquals(400, response.getStatusCode());
        assertEquals("invalid_grant", response.getError());

        events.assertRefreshTokenErrorAndMaybeSessionExpired(newRefreshToken.getSessionId(), loginEvent.getUserId(), "offline-client");

        setTimeOffset(0);
    }

    @Test
    public void onlineOfflineTokenBrowserFlow() {
        // request an online token for the client
        oauth.scope(null);
        oauth.client("offline-client", "secret1");
        oauth.redirectUri(offlineClientAppUri);
        oauth.doLogin("test-user@localhost", "password");
        EventRepresentation loginEvent = events.expectLogin()
                .client("offline-client")
                .detail(Details.REDIRECT_URI, offlineClientAppUri)
                .assertEvent();
        final String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        RefreshToken onlineToken = assertRefreshToken(tokenResponse, TokenUtil.TOKEN_TYPE_REFRESH);
        AccessToken token = oauth.verifyToken(tokenResponse.getAccessToken());
        events.expectCodeToToken(codeId, sessionId)
                .client("offline-client")
                .detail(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_REFRESH)
                .assertEvent();
        assertEquals(TokenUtil.TOKEN_TYPE_REFRESH, onlineToken.getType());
        Assert.assertNotNull(onlineToken.getExp());
        // request an offline token for the same client
        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        oauth.openLoginForm();
        events.expectLogin()
                .client("offline-client")
                .detail(Details.REDIRECT_URI, offlineClientAppUri)
                .assertEvent();
        AccessTokenResponse tokenOfflineResponse = oauth.doAccessTokenRequest(
                oauth.parseLoginResponse().getCode());
        RefreshToken offlineToken = assertRefreshToken(tokenOfflineResponse, TokenUtil.TOKEN_TYPE_OFFLINE);
        events.expectCodeToToken(codeId, sessionId)
                .client("offline-client")
                .detail(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_OFFLINE)
                .assertEvent();
        assertEquals(TokenUtil.TOKEN_TYPE_OFFLINE, offlineToken.getType());
        Assert.assertNull(offlineToken.getExp());
        assertTrue(tokenOfflineResponse.getScope().contains(OAuth2Constants.OFFLINE_ACCESS));
        // check both sessions are created
        checkNumberOfSessions(userId, "offline-client", onlineToken.getSessionId(), 1, 1);
        // check online token can be refreshed
        tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());
        assertRefreshToken(tokenResponse, TokenUtil.TOKEN_TYPE_REFRESH);
        events.expectRefresh(token.getId(), sessionId)
                .client("offline-client")
                .user(userId)
                .detail(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_REFRESH)
                .detail(Details.REFRESH_TOKEN_ID, onlineToken.getId())
                .assertEvent();
        // check offline token can be refreshed
        tokenOfflineResponse = oauth.doRefreshTokenRequest(tokenOfflineResponse.getRefreshToken());
        assertRefreshToken(tokenOfflineResponse, TokenUtil.TOKEN_TYPE_OFFLINE);
        events.expectRefresh(token.getId(), sessionId)
                .client("offline-client")
                .user(userId)
                .detail(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_OFFLINE)
                .detail(Details.REFRESH_TOKEN_ID, offlineToken.getId())
                .assertEvent();
    }

    private String testRefreshWithOfflineToken(AccessToken oldToken, RefreshToken offlineToken, String offlineTokenString,
                                               final String sessionId, String userId) {
        // Change offset to big value to ensure userSession expired
        setTimeOffset(99999);
        assertFalse(oldToken.isActive());
        assertTrue(offlineToken.isActive());

        // Assert userSession expired
        testingClient.testing().removeExpired("test");
        try {
            testingClient.testing().removeUserSession("test", sessionId);
        } catch (NotFoundException nfe) {
            // Ignore
        }

        AccessTokenResponse response = oauth.doRefreshTokenRequest(offlineTokenString);
        AccessToken refreshedToken = oauth.verifyToken(response.getAccessToken());
        Assert.assertEquals(200, response.getStatusCode());
        AccessTokenContext ctx = testingClient.testing("test").getTokenContext(refreshedToken.getId());
        Assert.assertEquals(AccessTokenContext.SessionType.OFFLINE, ctx.getSessionType());
        Assert.assertEquals(AccessTokenContext.TokenType.REGULAR, ctx.getTokenType());
        Assert.assertEquals(OAuth2Constants.REFRESH_TOKEN, ctx.getGrantType());

        // Assert new refreshToken in the response
        String newRefreshToken = response.getRefreshToken();
        RefreshToken newRefreshTokenFull = oauth.parseRefreshToken(newRefreshToken);
        Assert.assertNotNull(newRefreshToken);
        Assert.assertNotEquals(oldToken.getId(), refreshedToken.getId());

        // scope parameter either does not exist either contains offline_access
        assertTrue(refreshedToken.getScope().contains(OAuth2Constants.OFFLINE_ACCESS));
        // Assert refresh token scope parameter contains "offline_access"
        assertTrue(newRefreshTokenFull.getScope().contains(OAuth2Constants.OFFLINE_ACCESS));
        Assert.assertEquals(TokenUtil.TOKEN_TYPE_OFFLINE, newRefreshTokenFull.getType());

        Assert.assertEquals(userId, refreshedToken.getSubject());

        assertTrue(refreshedToken.getRealmAccess().isUserInRole("user"));
        assertTrue(refreshedToken.getRealmAccess().isUserInRole(Constants.OFFLINE_ACCESS_ROLE));

        Assert.assertEquals(1, refreshedToken.getResourceAccess("test-app").getRoles().size());
        assertTrue(refreshedToken.getResourceAccess("test-app").isUserInRole("customer-user"));

        EventRepresentation refreshEvent = events.expectRefresh(offlineToken.getId(), sessionId)
                .client("offline-client")
                .user(userId)
                .removeDetail(Details.UPDATED_REFRESH_TOKEN_ID)
                .detail(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_OFFLINE)
                .assertEvent();
        Assert.assertNotEquals(oldToken.getId(), refreshEvent.getDetails().get(Details.TOKEN_ID));

        setTimeOffset(0);
        return newRefreshToken;
    }

    private void checkNumberOfSessions(String userId, String clientId, String sessionId, int onlineSessions, int offlineSessions) {
        RealmResource realm = adminClient.realm("test");
        String clientUuid = ApiUtil.findClientByClientId(realm, clientId).toRepresentation().getId();
        Assert.assertEquals(onlineSessions, realm.users().get(userId).getUserSessions()
                .stream().filter(s -> sessionId.equals(s.getId())).count());
        Assert.assertEquals(offlineSessions, realm.users().get(userId).getOfflineSessions(clientUuid)
                .stream().filter(s -> sessionId.equals(s.getId())).count());
    }

    @Test
    public void offlineTokenDirectGrantFlow() {
        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        oauth.client("offline-client", "secret1");
        AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest("test-user@localhost", "password");
        Assert.assertNull(tokenResponse.getErrorDescription());
        AccessToken token = oauth.verifyToken(tokenResponse.getAccessToken());
        String offlineTokenString = tokenResponse.getRefreshToken();
        RefreshToken offlineToken = oauth.parseRefreshToken(offlineTokenString);

        events.expectLogin()
                .client("offline-client")
                .user(userId)
                .session(token.getSessionId())
                .detail(Details.GRANT_TYPE, OAuth2Constants.PASSWORD)
                .detail(Details.TOKEN_ID, token.getId())
                .detail(Details.REFRESH_TOKEN_ID, offlineToken.getId())
                .detail(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_OFFLINE)
                .detail(Details.USERNAME, "test-user@localhost")
                .removeDetail(Details.CODE_ID)
                .removeDetail(Details.REDIRECT_URI)
                .removeDetail(Details.CONSENT)
                .assertEvent();

        Assert.assertEquals(TokenUtil.TOKEN_TYPE_OFFLINE, offlineToken.getType());
        Assert.assertNull(offlineToken.getExp());

        // check only the offline session is created
        checkNumberOfSessions(userId, "offline-client", offlineToken.getSessionId(), 0, 1);

        // refresh token
        testRefreshWithOfflineToken(token, offlineToken, offlineTokenString, token.getSessionId(), userId);

        // Assert same token can be refreshed again
        testRefreshWithOfflineToken(token, offlineToken, offlineTokenString, token.getSessionId(), userId);
    }

    @Test
    public void offlineTokenDirectGrantFlowWithRefreshTokensRevoked() {
        RealmManager.realm(adminClient.realm("test")).revokeRefreshToken(true);

        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        oauth.client("offline-client", "secret1");
        AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest("test-user@localhost", "password");

        AccessToken token = oauth.verifyToken(tokenResponse.getAccessToken());
        String offlineTokenString = tokenResponse.getRefreshToken();
        RefreshToken offlineToken = oauth.parseRefreshToken(offlineTokenString);

        events.expectLogin()
                .client("offline-client")
                .user(userId)
                .session(token.getSessionId())
                .detail(Details.GRANT_TYPE, OAuth2Constants.PASSWORD)
                .detail(Details.TOKEN_ID, token.getId())
                .detail(Details.REFRESH_TOKEN_ID, offlineToken.getId())
                .detail(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_OFFLINE)
                .detail(Details.USERNAME, "test-user@localhost")
                .removeDetail(Details.CODE_ID)
                .removeDetail(Details.REDIRECT_URI)
                .removeDetail(Details.CONSENT)
                .assertEvent();

        Assert.assertEquals(TokenUtil.TOKEN_TYPE_OFFLINE, offlineToken.getType());
        Assert.assertNull(offlineToken.getExp());

        String offlineTokenString2 = testRefreshWithOfflineToken(token, offlineToken, offlineTokenString, token.getSessionId(), userId);
        RefreshToken offlineToken2 = oauth.parseRefreshToken(offlineTokenString2);

        // Assert second refresh with same refresh token will fail
        AccessTokenResponse response = oauth.doRefreshTokenRequest(offlineTokenString);
        Assert.assertEquals(400, response.getStatusCode());
        events.expectRefresh(offlineToken.getId(), token.getSessionId())
                .client("offline-client")
                .user((String) null)
                .error(Errors.INVALID_TOKEN)
                .clearDetails()
                .assertEvent();

        // Refresh with new refreshToken fails as well (client session was invalidated because of attempt to refresh with revoked refresh token)
        AccessTokenResponse response2 = oauth.doRefreshTokenRequest(offlineTokenString2);
        Assert.assertEquals(400, response2.getStatusCode());
        events.expectRefresh(offlineToken2.getId(), offlineToken2.getSessionId())
                .client("offline-client")
                .user((String) null)
                .error(Errors.INVALID_TOKEN)
                .clearDetails()
                .assertEvent();

        RealmManager.realm(adminClient.realm("test")).revokeRefreshToken(false);
    }

    @Test
    public void offlineTokenServiceAccountFlow() {
        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        oauth.client("offline-client", "secret1");
        AccessTokenResponse tokenResponse = oauth.doClientCredentialsGrantAccessTokenRequest();

        AccessToken token = oauth.verifyToken(tokenResponse.getAccessToken());
        String offlineTokenString = tokenResponse.getRefreshToken();
        RefreshToken offlineToken = oauth.parseRefreshToken(offlineTokenString);

        events.expectClientLogin()
                .client("offline-client")
                .user(serviceAccountUserId)
                .session(token.getSessionId())
                .detail(Details.TOKEN_ID, token.getId())
                .detail(Details.REFRESH_TOKEN_ID, offlineToken.getId())
                .detail(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_OFFLINE)
                .detail(Details.USERNAME, ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX + "offline-client")
                .assertEvent();

        Assert.assertEquals(TokenUtil.TOKEN_TYPE_OFFLINE, offlineToken.getType());
        Assert.assertNull(offlineToken.getExp());

        // check only the offline session is created
        checkNumberOfSessions(serviceAccountUserId, "offline-client", offlineToken.getSessionId(), 0, 1);

        testRefreshWithOfflineToken(token, offlineToken, offlineTokenString, token.getSessionId(), serviceAccountUserId);

        // Now retrieve another offline token and verify that previous offline token is still valid
        tokenResponse = oauth.doClientCredentialsGrantAccessTokenRequest();

        AccessToken token2 = oauth.verifyToken(tokenResponse.getAccessToken());
        String offlineTokenString2 = tokenResponse.getRefreshToken();
        RefreshToken offlineToken2 = oauth.parseRefreshToken(offlineTokenString2);

        events.expectClientLogin()
                .client("offline-client")
                .user(serviceAccountUserId)
                .session(token2.getSessionId())
                .detail(Details.TOKEN_ID, token2.getId())
                .detail(Details.REFRESH_TOKEN_ID, offlineToken2.getId())
                .detail(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_OFFLINE)
                .detail(Details.USERNAME, ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX + "offline-client")
                .assertEvent();

        // check only the offline session is created
        checkNumberOfSessions(serviceAccountUserId, "offline-client", offlineToken2.getSessionId(), 0, 1);

        // Refresh with both offline tokens is fine
        testRefreshWithOfflineToken(token, offlineToken, offlineTokenString, token.getSessionId(), serviceAccountUserId);
        testRefreshWithOfflineToken(token2, offlineToken2, offlineTokenString2, token2.getSessionId(), serviceAccountUserId);
    }

    @Test
    public void offlineTokenAllowedWithCompositeRole() {
        RealmResource appRealm = adminClient.realm("test");
        UserResource testUser = findUserByUsernameId(appRealm, "test-user@localhost");
        RoleRepresentation offlineAccess = findRealmRoleByName(adminClient.realm("test"),
                Constants.OFFLINE_ACCESS_ROLE).toRepresentation();

        // Grant offline_access role indirectly through composite role
        appRealm.roles().create(RoleBuilder.create().name("composite").build());
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
        ClientResource realmMgmt = ApiUtil.findClientByClientId(appRealm, Constants.REALM_MANAGEMENT_CLIENT_ID);
        String realmMgmtUuid = realmMgmt.toRepresentation().getId();
        RoleRepresentation roleRep = realmMgmt.roles().get(AdminRoles.VIEW_REALM).toRepresentation();

        UserResource testUser = findUserByUsernameId(appRealm, "test-user@localhost");
        testUser.roles().clientLevel(realmMgmtUuid).add(Collections.singletonList(roleRep));

        // Login with offline token now
        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        oauth.client("offline-client", "secret1");
        AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest("test-user@localhost", "password");

        events.clear();

        // Set the time offset, so that "normal" userSession expires
        setTimeOffset(86400);

        // Remove expired sessions. This will remove "normal" userSession
        testingClient.testing().removeExpired("test");

        // Refresh with the offline token
        tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());
        Assert.assertNull("received error " + tokenResponse.getError() + ", " + tokenResponse.getErrorDescription(), tokenResponse.getError());

        // Use accessToken to admin REST request
        try (Keycloak offlineTokenAdmin = Keycloak.getInstance(getAuthServerContextRoot() + "/auth",
                AuthRealm.MASTER, Constants.ADMIN_CLI_CLIENT_ID, tokenResponse.getAccessToken(), TLSUtils.initializeTLS())) {
            RealmRepresentation testRealm = offlineTokenAdmin.realm("test").toRepresentation();
            Assert.assertNotNull(testRealm);
        }
    }


    // KEYCLOAK-4525
    @Test
    public void offlineTokenRemoveClientWithTokens() {
        // Create new client
        RealmResource appRealm = adminClient.realm("test");

        ClientRepresentation clientRep = ClientBuilder.create().clientId("offline-client-2")
                .id(KeycloakModelUtils.generateId())
                .directAccessGrants()
                .secret("secret1").build();

        appRealm.clients().create(clientRep).close();

        // Direct grant login requesting offline token
        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        oauth.client("offline-client-2", "secret1");
        AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest("test-user@localhost", "password");
        Assert.assertNull(tokenResponse.getErrorDescription());
        AccessToken token = oauth.verifyToken(tokenResponse.getAccessToken());
        String offlineTokenString = tokenResponse.getRefreshToken();
        RefreshToken offlineToken = oauth.parseRefreshToken(offlineTokenString);

        events.expectLogin()
                .client("offline-client-2")
                .user(userId)
                .session(token.getSessionId())
                .detail(Details.GRANT_TYPE, OAuth2Constants.PASSWORD)
                .detail(Details.TOKEN_ID, token.getId())
                .detail(Details.REFRESH_TOKEN_ID, offlineToken.getId())
                .detail(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_OFFLINE)
                .detail(Details.USERNAME, "test-user@localhost")
                .removeDetail(Details.CODE_ID)
                .removeDetail(Details.REDIRECT_URI)
                .removeDetail(Details.CONSENT)
                .assertEvent();

        // Confirm that offline-client-2 token was granted
        List<Map<String, Object>> userConsents = AccountHelper.getUserConsents(adminClient.realm(TEST), "test-user@localhost");

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
        ClientResource offlineTokenClient2 = ApiUtil.findClientByClientId(appRealm, "offline-client-2" );
        offlineTokenClient2.remove();

        // Confirm that offline-client-2 token was deleted
        assertNull(ApiUtil.findClientByClientId(appRealm, "offline-client-2"));

        // Login as admin and see consents of user
        UserResource user = ApiUtil.findUserByUsernameId(appRealm, "test-user@localhost");
        List<Map<String, Object>> consents = user.getConsents();
        for (Map<String, Object> consent : consents) {
            assertNotEquals("offline-client-2", consent.get("clientId"));
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
        oauth.redirectUri(offlineClientAppUri);
        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin()
                .client("offline-client")
                .detail(Details.REDIRECT_URI, offlineClientAppUri)
                .assertEvent();

        final String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);

        String code = oauth.parseLoginResponse().getCode();

        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);
        oauth.verifyToken(tokenResponse.getAccessToken());
        String offlineTokenString = tokenResponse.getRefreshToken();
        RefreshToken offlineToken = oauth.parseRefreshToken(offlineTokenString);

        events.expectCodeToToken(codeId, sessionId)
                .client("offline-client")
                .detail(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_OFFLINE)
                .assertEvent();

        assertEquals(TokenUtil.TOKEN_TYPE_OFFLINE, offlineToken.getType());
        assertNull(offlineToken.getExp());

        String offlineUserSessionId = testingClient.server().fetch((KeycloakSession session) ->
                session.sessions().getOfflineUserSession(session.realms().getRealmByName("test"), offlineToken.getSessionId()).getId(), String.class);

        // logout offline session
        LogoutResponse logoutResponse = oauth.doLogout(offlineTokenString);
        assertTrue(logoutResponse.isSuccess());
        events.expectLogout(offlineUserSessionId)
                .client("offline-client")
                .removeDetail(Details.REDIRECT_URI)
                .assertEvent();

        // Need to login again now
        oauth.doLogin("test-user@localhost", "password");
        String code2 = oauth.parseLoginResponse().getCode();

        AccessTokenResponse tokenResponse2 = oauth.doAccessTokenRequest(code2);
        assertEquals(200, tokenResponse2.getStatusCode());
        oauth.verifyToken(tokenResponse2.getAccessToken());
        String offlineTokenString2 = tokenResponse2.getRefreshToken();
        RefreshToken offlineToken2 = oauth.parseRefreshToken(offlineTokenString2);

        loginEvent = events.expectLogin()
                .client("offline-client")
                .detail(Details.REDIRECT_URI, offlineClientAppUri)
                .assertEvent();

        codeId = loginEvent.getDetails().get(Details.CODE_ID);

        events.expectCodeToToken(codeId, offlineToken2.getSessionId())
                .client("offline-client")
                .detail(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_OFFLINE)
                .assertEvent();

        assertEquals(TokenUtil.TOKEN_TYPE_OFFLINE, offlineToken2.getType());
        Assert.assertNull(offlineToken.getExp());

        // Assert session changed
        assertNotEquals(offlineToken.getSessionId(), offlineToken2.getSessionId());
    }

    // KEYCLOAK-7688 Offline Session Max for Offline Token
    private int[] changeOfflineSessionSettings(boolean isEnabled, int sessionMax, int sessionIdle, int clientSessionMax, int clientSessionIdle) {
        int prev[] = new int[5];
        RealmRepresentation rep = adminClient.realm("test").toRepresentation();
        prev[0] = rep.getOfflineSessionMaxLifespan();
        prev[1] = rep.getOfflineSessionIdleTimeout();
        prev[2] = rep.getClientOfflineSessionMaxLifespan();
        prev[3] = rep.getClientOfflineSessionIdleTimeout();
        RealmBuilder realmBuilder = RealmBuilder.create();
        realmBuilder.offlineSessionMaxLifespanEnabled(isEnabled).offlineSessionMaxLifespan(sessionMax).offlineSessionIdleTimeout(sessionIdle)
                .clientOfflineSessionMaxLifespan(clientSessionMax).clientOfflineSessionIdleTimeout(clientSessionIdle);
        adminClient.realm("test").update(realmBuilder.build());
        return prev;
    }

    private int[] changeSessionSettings(int ssoSessionIdle, int accessTokenLifespan) {
        int prev[] = new int[2];
        RealmRepresentation rep = adminClient.realm("test").toRepresentation();
        prev[0] = rep.getOfflineSessionMaxLifespan();
        prev[1] = rep.getOfflineSessionIdleTimeout();
        RealmBuilder realmBuilder = RealmBuilder.create();
        realmBuilder.ssoSessionIdleTimeout(ssoSessionIdle).accessTokenLifespan(accessTokenLifespan);
        adminClient.realm("test").update(realmBuilder.build());
        return prev;
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
        testOfflineSessionExpiration(IDLE_LIFESPAN, MAX_LIFESPAN, 0, IDLE_LIFESPAN + (ProfileAssume.isFeatureEnabled(Profile.Feature.PERSISTENT_USER_SESSIONS) ? 0 : SessionTimeoutHelper.IDLE_TIMEOUT_WINDOW_SECONDS) + 60);
    }

    // Issue 13706
    @Test
    public void offlineTokenReauthenticationWhenOfflinClientSessionExpired() throws Exception {
        // expect that offline session expired by idle timeout
        final int MAX_LIFESPAN = 360000;
        final int IDLE_LIFESPAN = 900;

        getTestingClient().testing().setTestingInfinispanTimeService();

        int[] prev = null;
        try (RealmAttributeUpdater ignored = new RealmAttributeUpdater(adminClient.realm("test")).setSsoSessionIdleTimeout(900).update()) {
            prev = changeOfflineSessionSettings(true, MAX_LIFESPAN, IDLE_LIFESPAN, 0, 0);

            // Step 1 - online login with "tets-app"
            oauth.scope(null);
            oauth.client("test-app", "password");
            oauth.redirectUri(APP_ROOT + "/auth");
            oauth.doLogin("test-user@localhost", "password");
            String code = oauth.parseLoginResponse().getCode();
            AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);
            assertRefreshToken(tokenResponse, TokenUtil.TOKEN_TYPE_REFRESH);

            // Step 2 - offline login with "offline-client"
            oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
            oauth.client("offline-client", "secret1");
            oauth.redirectUri(offlineClientAppUri);

            oauth.openLoginForm();
            code = oauth.parseLoginResponse().getCode();
            tokenResponse = oauth.doAccessTokenRequest(code);
            assertOfflineToken(tokenResponse);

            // Step 3 - set some offset to refresh SSO session and offline user session. But use different client, so that we don't refresh offlineClientSession of client "offline-client"
            setTimeOffset(800);
            oauth.client("test-app", "password");
            oauth.redirectUri(APP_ROOT + "/auth");
            oauth.openLoginForm();

            code = oauth.parseLoginResponse().getCode();
            tokenResponse = oauth.doAccessTokenRequest(code);
            assertOfflineToken(tokenResponse);

            // Step 4 - set bigger time offset and login with the original client "offline-token". Login should be successful and offline client session for "offline-client" should be re-created now
            setTimeOffset(900 + SessionTimeoutHelper.PERIODIC_CLEANER_IDLE_TIMEOUT_WINDOW_SECONDS + 20);
            oauth.client("offline-client", "secret1");
            oauth.redirectUri(offlineClientAppUri);
            oauth.openLoginForm();

            code = oauth.parseLoginResponse().getCode();
            tokenResponse = oauth.doAccessTokenRequest(code);
            assertOfflineToken(tokenResponse);

        } finally {
            getTestingClient().testing().revertTestingInfinispanTimeService();
            changeOfflineSessionSettings(false, prev[0], prev[1], 0, 0);
        }
    }

    private void assertOfflineToken(AccessTokenResponse tokenResponse) {
        assertRefreshToken(tokenResponse, TokenUtil.TOKEN_TYPE_OFFLINE);
    }

    // Asserts that refresh token in the tokenResponse is of the given type. Return parsed token
    private RefreshToken assertRefreshToken(AccessTokenResponse tokenResponse, String tokenType) {
        Assert.assertEquals(200, tokenResponse.getStatusCode());
        String offlineTokenString = tokenResponse.getRefreshToken();
        RefreshToken refreshToken = oauth.parseRefreshToken(offlineTokenString);
        assertEquals(tokenType, refreshToken.getType());
        return refreshToken;
    }

    @Test
    public void offlineTokenRequest_ClientES256_RealmPS256() throws Exception {
        conductOfflineTokenRequest(Constants.INTERNAL_SIGNATURE_ALGORITHM, Algorithm.ES256, Algorithm.PS256);
    }

    @Test
    public void offlineTokenRequest_ClientPS256_RealmES256() throws Exception {
        conductOfflineTokenRequest(Constants.INTERNAL_SIGNATURE_ALGORITHM, Algorithm.PS256, Algorithm.ES256);
    }

    private void conductOfflineTokenRequest(String expectedRefreshAlg, String expectedAccessAlg, String expectedIdTokenAlg) throws Exception {
        try {
            /// Realm Setting is used for ID Token Signature Algorithm
            TokenSignatureUtil.changeRealmTokenSignatureProvider(adminClient, expectedIdTokenAlg);
            TokenSignatureUtil.changeClientAccessTokenSignatureProvider(ApiUtil.findClientByClientId(adminClient.realm("test"), "offline-client"), expectedAccessAlg);
            offlineTokenRequest(expectedRefreshAlg, expectedAccessAlg, expectedIdTokenAlg);
            offlineTokenRequestWithScopeParameter(expectedRefreshAlg, expectedAccessAlg, expectedIdTokenAlg);
        } finally {
            TokenSignatureUtil.changeRealmTokenSignatureProvider(adminClient, Algorithm.RS256);
            TokenSignatureUtil.changeClientAccessTokenSignatureProvider(ApiUtil.findClientByClientId(adminClient.realm("test"), "offline-client"), Algorithm.RS256);
        }
    }

    private String getOfflineClientSessionUuid(final String userSessionId, final String clientId) {
        return testingClient.server().fetch(session -> {
            RealmModel realmModel = session.realms().getRealmByName("test");
            ClientModel clientModel = realmModel.getClientByClientId(clientId);
            UserSessionModel userSession = session.sessions().getOfflineUserSession(realmModel, userSessionId);
            AuthenticatedClientSessionModel clientSession = userSession.getAuthenticatedClientSessionByClient(clientModel.getId());
            return clientSession.getId();
        }, String.class);
    }

    private int checkIfUserAndClientSessionExist(final String userSessionId, final String clientId, final String clientSessionId) {
        return testingClient.server().fetch(session -> {
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

    private void removeClientSessionStartedAtNote(final String userSessionId, final String clientId) {
        testingClient.server().run(session -> {
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

    private void testOfflineSessionExpiration(int idleTime, int maxLifespan, int offsetHalf, int offset) {
        int prev[] = null;
        getTestingClient().testing().setTestingInfinispanTimeService();
        try {
            prev = changeOfflineSessionSettings(true, maxLifespan, idleTime, 0, 0);

            oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
            oauth.client("offline-client", "secret1");
            oauth.redirectUri(offlineClientAppUri);
            oauth.doLogin("test-user@localhost", "password");

            EventRepresentation loginEvent = events.expectLogin()
                    .client("offline-client")
                    .detail(Details.REDIRECT_URI, offlineClientAppUri)
                    .assertEvent();

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
            setTimeOffset(offsetHalf);

            tokenResponse = oauth.doRefreshTokenRequest(offlineTokenString);
            oauth.verifyToken(tokenResponse.getAccessToken());
            offlineTokenString = tokenResponse.getRefreshToken();
            oauth.parseRefreshToken(offlineTokenString);

            Assert.assertEquals(200, tokenResponse.getStatusCode());
            assertEquals(2, checkIfUserAndClientSessionExist(sessionId, loginEvent.getClientId(), clientSessionId));

            // wait to expire
            setTimeOffset(offset);

            tokenResponse = oauth.doRefreshTokenRequest(offlineTokenString);

            Assert.assertEquals(400, tokenResponse.getStatusCode());
            assertEquals("invalid_grant", tokenResponse.getError());

            // Assert userSession expired
            assertEquals(0, checkIfUserAndClientSessionExist(sessionId, loginEvent.getClientId(), clientSessionId));
            testingClient.testing().removeExpired("test");
            try {
                testingClient.testing().removeUserSession("test", sessionId);
            } catch (NotFoundException nfe) {
                // Ignore
            }

            setTimeOffset(0);

        } finally {
            getTestingClient().testing().revertTestingInfinispanTimeService();
            changeOfflineSessionSettings(false, prev[0], prev[1], prev[2], prev[3]);
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

        events.expectClientLogin()
                .client("offline-client")
                .user(serviceAccountUserId)
                .session(token.getSessionId())
                .detail(Details.TOKEN_ID, token.getId())
                .detail(Details.REFRESH_TOKEN_ID, offlineToken.getId())
                .detail(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_OFFLINE)
                .detail(Details.USERNAME, ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX + "offline-client")
                .assertEvent();

        Assert.assertEquals(TokenUtil.TOKEN_TYPE_OFFLINE, offlineToken.getType());
        Assert.assertNull(offlineToken.getExp());

        testRefreshWithOfflineToken(token, offlineToken, offlineTokenString, token.getSessionId(), serviceAccountUserId);

        // Now retrieve another offline token and decode that previous offline token is still valid
        tokenResponse = oauth.doClientCredentialsGrantAccessTokenRequest();

        AccessToken token2 = oauth.verifyToken(tokenResponse.getAccessToken());
        String offlineTokenString2 = tokenResponse.getRefreshToken();
        RefreshToken offlineToken2 = oauth.parseRefreshToken(offlineTokenString2);

        events.expectClientLogin()
                .client("offline-client")
                .user(serviceAccountUserId)
                .session(token2.getSessionId())
                .detail(Details.TOKEN_ID, token2.getId())
                .detail(Details.REFRESH_TOKEN_ID, offlineToken2.getId())
                .detail(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_OFFLINE)
                .detail(Details.USERNAME, ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX + "offline-client")
                .assertEvent();

        // Refresh with both offline tokens is fine
        testRefreshWithOfflineToken(token, offlineToken, offlineTokenString, token.getSessionId(), serviceAccountUserId);
        testRefreshWithOfflineToken(token2, offlineToken2, offlineTokenString2, token2.getSessionId(), serviceAccountUserId);
    }

    private void offlineTokenRequestWithScopeParameter(String expectedRefreshAlg, String expectedAccessAlg, String expectedIdTokenAlg) throws Exception {
        ClientScopeRepresentation phoneScope = adminClient.realm("test").clientScopes().findAll().stream().filter((ClientScopeRepresentation clientScope) ->"phone".equals(clientScope.getName())).findFirst().get();
        ClientManager.realm(adminClient.realm("test")).clientId(oauth.getClientId()).addClientScope(phoneScope.getId(),false);
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

        events.expectClientLogin()
                .client("offline-client")
                .user(serviceAccountUserId)
                .session(token.getSessionId())
                .detail(Details.TOKEN_ID, token.getId())
                .detail(Details.REFRESH_TOKEN_ID, offlineToken.getId())
                .detail(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_OFFLINE)
                .detail(Details.USERNAME, ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX + "offline-client")
                .assertEvent();

        Assert.assertEquals(TokenUtil.TOKEN_TYPE_OFFLINE, offlineToken.getType());
        Assert.assertNull(offlineToken.getExp());
    }

    @Test
    public void refreshTokenUserClientMaxLifespanSmallerThanSession() {
        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        oauth.client("offline-client", "secret1");
        oauth.redirectUri(offlineClientAppUri);

        int[] prev = changeOfflineSessionSettings(true, 3600, 7200, 1000, 7200);
        getTestingClient().testing().setTestingInfinispanTimeService();
        try {
            oauth.doLogin("test-user@localhost", "password");
            EventRepresentation loginEvent = events.expectLogin().client("offline-client")
                    .detail(Details.REDIRECT_URI, offlineClientAppUri).assertEvent();

            String sessionId = loginEvent.getSessionId();

            String code = oauth.parseLoginResponse().getCode();
            AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);
            assertEquals(TokenUtil.TOKEN_TYPE_OFFLINE, oauth.parseRefreshToken(tokenResponse.getRefreshToken()).getType());
            assertTrue("Invalid ExpiresIn", 0 < tokenResponse.getRefreshExpiresIn() && tokenResponse.getRefreshExpiresIn() <= 1000);
            String clientSessionId = getOfflineClientSessionUuid(sessionId, loginEvent.getClientId());
            assertEquals(2, checkIfUserAndClientSessionExist(sessionId, loginEvent.getClientId(), clientSessionId));

            events.poll();

            setTimeOffset(600);
            String refreshId = oauth.parseRefreshToken(tokenResponse.getRefreshToken()).getId();
            tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());
            assertEquals(TokenUtil.TOKEN_TYPE_OFFLINE, oauth.parseRefreshToken(tokenResponse.getRefreshToken()).getType());
            assertTrue("Invalid ExpiresIn", 0 < tokenResponse.getRefreshExpiresIn() && tokenResponse.getRefreshExpiresIn() <= 400);
            assertEquals(2, checkIfUserAndClientSessionExist(sessionId, loginEvent.getClientId(), clientSessionId));
            events.expectRefresh(refreshId, sessionId).client("offline-client").detail(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_OFFLINE).assertEvent();

            setTimeOffset(1100);
            tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());
            assertEquals(400, tokenResponse.getStatusCode());
            assertNull(tokenResponse.getAccessToken());
            assertNull(tokenResponse.getRefreshToken());
            events.expect(EventType.REFRESH_TOKEN).client("offline-client").error(Errors.INVALID_TOKEN).user((String) null).assertEvent();
            assertEquals(1, checkIfUserAndClientSessionExist(sessionId, loginEvent.getClientId(), clientSessionId));
        } finally {
            changeOfflineSessionSettings(false, prev[0], prev[1], prev[2], prev[3]);
            getTestingClient().testing().revertTestingInfinispanTimeService();
            events.clear();
            resetTimeOffset();
        }
    }

    @Test
    public void refreshTokenUserClientMaxLifespanGreaterThanSession() {
        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        oauth.client("offline-client", "secret1");
        oauth.redirectUri(offlineClientAppUri);

        int[] prev = changeOfflineSessionSettings(true, 3600, 7200, 5000, 7200);
        getTestingClient().testing().setTestingInfinispanTimeService();
        try {
            oauth.doLogin("test-user@localhost", "password");
            EventRepresentation loginEvent = events.expectLogin().client("offline-client")
                    .detail(Details.REDIRECT_URI, offlineClientAppUri).assertEvent();

            String sessionId = loginEvent.getSessionId();

            String code = oauth.parseLoginResponse().getCode();
            AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);
            assertEquals(TokenUtil.TOKEN_TYPE_OFFLINE, oauth.parseRefreshToken(tokenResponse.getRefreshToken()).getType());
            assertTrue("Invalid ExpiresIn", 0 < tokenResponse.getRefreshExpiresIn() && tokenResponse.getRefreshExpiresIn() <= 3600);
            String clientSessionId = getOfflineClientSessionUuid(sessionId, loginEvent.getClientId());
            assertEquals(2, checkIfUserAndClientSessionExist(sessionId, loginEvent.getClientId(), clientSessionId));

            events.poll();

            setTimeOffset(1800);
            String refreshId = oauth.parseRefreshToken(tokenResponse.getRefreshToken()).getId();
            tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());
            assertEquals(TokenUtil.TOKEN_TYPE_OFFLINE, oauth.parseRefreshToken(tokenResponse.getRefreshToken()).getType());
            assertTrue("Invalid ExpiresIn", 0 < tokenResponse.getRefreshExpiresIn() && tokenResponse.getRefreshExpiresIn() <= 1800);
            assertEquals(2, checkIfUserAndClientSessionExist(sessionId, loginEvent.getClientId(), clientSessionId));
            events.expectRefresh(refreshId, sessionId).client("offline-client").detail(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_OFFLINE).assertEvent();

            setTimeOffset(3700);
            tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());
            assertEquals(400, tokenResponse.getStatusCode());
            assertNull(tokenResponse.getAccessToken());
            assertNull(tokenResponse.getRefreshToken());
            events.expect(EventType.REFRESH_TOKEN).client("offline-client").error(Errors.INVALID_TOKEN).user((String) null).assertEvent();
            assertEquals(0, checkIfUserAndClientSessionExist(sessionId, loginEvent.getClientId(), clientSessionId));
        } finally {
            changeOfflineSessionSettings(false, prev[0], prev[1], prev[2], prev[3]);
            getTestingClient().testing().revertTestingInfinispanTimeService();
            events.clear();
            resetTimeOffset();
        }
    }

    @Test
    public void refreshTokenUserSessionMaxLifespanModifiedAfterTokenRefresh() {
        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        oauth.client("offline-client", "secret1");
        oauth.redirectUri(offlineClientAppUri);

        RealmResource realmResource = adminClient.realm("test");
        getTestingClient().testing().setTestingInfinispanTimeService();

        int[] prev = changeOfflineSessionSettings(true, 7200, 7200, 7200, 7200);
        try {
            oauth.doLogin("test-user@localhost", "password");
            EventRepresentation loginEvent = events.expectLogin().client("offline-client")
                    .detail(Details.REDIRECT_URI, offlineClientAppUri).assertEvent();

            String sessionId = loginEvent.getSessionId();

            String code = oauth.parseLoginResponse().getCode();
            AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);
            assertEquals(TokenUtil.TOKEN_TYPE_OFFLINE, oauth.parseRefreshToken(tokenResponse.getRefreshToken()).getType());
            assertTrue("Invalid ExpiresIn", 0 < tokenResponse.getRefreshExpiresIn() && tokenResponse.getRefreshExpiresIn() <= 7200);
            String clientSessionId = getOfflineClientSessionUuid(sessionId, loginEvent.getClientId());
            assertEquals(2, checkIfUserAndClientSessionExist(sessionId, loginEvent.getClientId(), clientSessionId));

            events.poll();

            RealmRepresentation rep = realmResource.toRepresentation();
            rep.setOfflineSessionMaxLifespan(3600);
            realmResource.update(rep);

            setTimeOffset(3700);
            tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());
            assertEquals(400, tokenResponse.getStatusCode());
            assertNull(tokenResponse.getAccessToken());
            assertNull(tokenResponse.getRefreshToken());
            events.assertRefreshTokenErrorAndMaybeSessionExpired(sessionId, loginEvent.getUserId(), "offline-client");
            assertEquals(0, checkIfUserAndClientSessionExist(sessionId, loginEvent.getClientId(), clientSessionId));
        } finally {
            changeOfflineSessionSettings(false, prev[0], prev[1], prev[2], prev[3]);
            getTestingClient().testing().revertTestingInfinispanTimeService();
            events.clear();
            resetTimeOffset();
        }
    }

    @Test
    public void refreshTokenClientSessionMaxLifespanModifiedAfterTokenRefresh() {
        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        oauth.client("offline-client", "secret1");
        oauth.redirectUri(offlineClientAppUri);

        RealmResource realmResource = adminClient.realm("test");
        getTestingClient().testing().setTestingInfinispanTimeService();

        int[] prev = changeOfflineSessionSettings(true, 7200, 7200, 7200, 7200);
        try {
            oauth.doLogin("test-user@localhost", "password");
            EventRepresentation loginEvent = events.expectLogin().client("offline-client")
                    .detail(Details.REDIRECT_URI, offlineClientAppUri).assertEvent();

            String sessionId = loginEvent.getSessionId();

            String code = oauth.parseLoginResponse().getCode();
            AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);
            assertEquals(TokenUtil.TOKEN_TYPE_OFFLINE, oauth.parseRefreshToken(tokenResponse.getRefreshToken()).getType());
            assertTrue("Invalid ExpiresIn", 0 < tokenResponse.getRefreshExpiresIn() && tokenResponse.getRefreshExpiresIn() <= 7200);
            String clientSessionId = getOfflineClientSessionUuid(sessionId, loginEvent.getClientId());
            assertEquals(2, checkIfUserAndClientSessionExist(sessionId, loginEvent.getClientId(), clientSessionId));

            events.poll();

            RealmRepresentation rep = realmResource.toRepresentation();
            rep.setClientOfflineSessionMaxLifespan(3600);
            realmResource.update(rep);

            setTimeOffset(3700);
            tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());
            assertEquals(400, tokenResponse.getStatusCode());
            assertNull(tokenResponse.getAccessToken());
            assertNull(tokenResponse.getRefreshToken());
            events.expect(EventType.REFRESH_TOKEN).client("offline-client").error(Errors.INVALID_TOKEN).session(sessionId).user((String) null).assertEvent();
            assertEquals(1, checkIfUserAndClientSessionExist(sessionId, loginEvent.getClientId(), clientSessionId));
        } finally {
            changeOfflineSessionSettings(false, prev[0], prev[1], prev[2], prev[3]);
            getTestingClient().testing().revertTestingInfinispanTimeService();
            events.clear();
            resetTimeOffset();
        }
    }

    @Test
    public void testShortOfflineSessionMax() throws Exception {
        int prevOfflineSession[] = null;
        int prevSession[] = null;
        try {
            prevOfflineSession = changeOfflineSessionSettings(true, 60, 30, 0, 0);
            prevSession = changeSessionSettings(1800, 300);

            oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
            oauth.client("offline-client", "secret1");
            oauth.redirectUri(offlineClientAppUri);
            oauth.doLogin("test-user@localhost", "password");

            events.expectLogin().client("offline-client").detail(Details.REDIRECT_URI, offlineClientAppUri).assertEvent();

            String code = oauth.parseLoginResponse().getCode();

            AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);
            String offlineTokenString = tokenResponse.getRefreshToken();
            RefreshToken offlineToken = oauth.parseRefreshToken(offlineTokenString);

            assertThat(tokenResponse.getExpiresIn(), allOf(greaterThanOrEqualTo(59), lessThanOrEqualTo(60)));
            assertThat(tokenResponse.getRefreshExpiresIn(), allOf(greaterThanOrEqualTo(29), lessThanOrEqualTo(30)));
            assertEquals(TokenUtil.TOKEN_TYPE_OFFLINE, offlineToken.getType());

            JsonNode jsonNode = oauth.doIntrospectionAccessTokenRequest(tokenResponse.getAccessToken()).asJsonNode();
            assertTrue(jsonNode.get("active").asBoolean());
            Assert.assertEquals("test-user@localhost", jsonNode.get("email").asText());
            assertThat(jsonNode.get("exp").asInt() - getCurrentTime(),
                allOf(greaterThanOrEqualTo(59), lessThanOrEqualTo(60)));

        } finally {
            changeOfflineSessionSettings(false, prevOfflineSession[0], prevOfflineSession[1], prevOfflineSession[2], prevOfflineSession[3]);
            changeSessionSettings(prevSession[0], prevSession[1]);
        }
    }

    @Test
    public void testClientOfflineSessionMaxLifespan() {
        ClientResource client = ApiUtil.findClientByClientId(adminClient.realm("test"), "offline-client");
        ClientRepresentation clientRepresentation = client.toRepresentation();

        RealmResource realm = adminClient.realm("test");
        RealmRepresentation rep = realm.toRepresentation();
        Boolean originalOfflineSessionMaxLifespanEnabled = rep.getOfflineSessionMaxLifespanEnabled();
        Integer originalOfflineSessionMaxLifespan = rep.getOfflineSessionMaxLifespan();
        int offlineSessionMaxLifespan = rep.getOfflineSessionIdleTimeout() - 100;
        Integer originalClientOfflineSessionMaxLifespan = rep.getClientOfflineSessionMaxLifespan();

        try {
            rep.setOfflineSessionMaxLifespanEnabled(true);
            rep.setOfflineSessionMaxLifespan(offlineSessionMaxLifespan);
            realm.update(rep);

            oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
            oauth.client("offline-client", "secret1");
            oauth.redirectUri(offlineClientAppUri);
            oauth.doLogin("test-user@localhost", "password");
            String code = oauth.parseLoginResponse().getCode();
            AccessTokenResponse response = oauth.doAccessTokenRequest(code);
            assertEquals(200, response.getStatusCode());
            assertExpiration(response.getRefreshExpiresIn(), offlineSessionMaxLifespan);

            rep.setClientOfflineSessionMaxLifespan(offlineSessionMaxLifespan - 100);
            realm.update(rep);

            String refreshToken = response.getRefreshToken();
            response = oauth.doRefreshTokenRequest(refreshToken);
            assertEquals(200, response.getStatusCode());
            assertExpiration(response.getRefreshExpiresIn(), offlineSessionMaxLifespan - 100);

            clientRepresentation.getAttributes().put(OIDCConfigAttributes.CLIENT_OFFLINE_SESSION_MAX_LIFESPAN,
                Integer.toString(offlineSessionMaxLifespan - 200));
            client.update(clientRepresentation);

            refreshToken = response.getRefreshToken();
            response = oauth.doRefreshTokenRequest(refreshToken);
            assertEquals(200, response.getStatusCode());
            assertExpiration(response.getRefreshExpiresIn(), offlineSessionMaxLifespan - 200);
        } finally {
            rep.setOfflineSessionMaxLifespanEnabled(originalOfflineSessionMaxLifespanEnabled);
            rep.setOfflineSessionMaxLifespan(originalOfflineSessionMaxLifespan);
            rep.setClientOfflineSessionMaxLifespan(originalClientOfflineSessionMaxLifespan);
            realm.update(rep);
            clientRepresentation.getAttributes().put(OIDCConfigAttributes.CLIENT_OFFLINE_SESSION_MAX_LIFESPAN, null);
            client.update(clientRepresentation);
        }
    }

    @Test
    public void testClientOfflineSessionIdleTimeout() {
        ClientResource client = ApiUtil.findClientByClientId(adminClient.realm("test"), "offline-client");
        ClientRepresentation clientRepresentation = client.toRepresentation();

        RealmResource realm = adminClient.realm("test");
        RealmRepresentation rep = realm.toRepresentation();
        Boolean originalOfflineSessionMaxLifespanEnabled = rep.getOfflineSessionMaxLifespanEnabled();
        int offlineSessionIdleTimeout = rep.getOfflineSessionIdleTimeout();
        Integer originalClientOfflineSessionIdleTimeout = rep.getClientOfflineSessionIdleTimeout();

        try {
            rep.setOfflineSessionMaxLifespanEnabled(true);
            realm.update(rep);

            oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
            oauth.client("offline-client", "secret1");
            oauth.redirectUri(offlineClientAppUri);
            oauth.doLogin("test-user@localhost", "password");
            String code = oauth.parseLoginResponse().getCode();
            AccessTokenResponse response = oauth.doAccessTokenRequest(code);
            assertEquals(200, response.getStatusCode());
            assertExpiration(response.getRefreshExpiresIn(), offlineSessionIdleTimeout);

            rep.setClientOfflineSessionIdleTimeout(offlineSessionIdleTimeout - 100);
            realm.update(rep);

            String refreshToken = response.getRefreshToken();
            response = oauth.doRefreshTokenRequest(refreshToken);
            assertEquals(200, response.getStatusCode());
            assertExpiration(response.getRefreshExpiresIn(), offlineSessionIdleTimeout - 100);

            clientRepresentation.getAttributes().put(OIDCConfigAttributes.CLIENT_OFFLINE_SESSION_IDLE_TIMEOUT,
                Integer.toString(offlineSessionIdleTimeout - 200));
            client.update(clientRepresentation);

            refreshToken = response.getRefreshToken();
            response = oauth.doRefreshTokenRequest(refreshToken);
            assertEquals(200, response.getStatusCode());
            assertExpiration(response.getRefreshExpiresIn(), offlineSessionIdleTimeout - 200);
        } finally {
            rep.setOfflineSessionMaxLifespanEnabled(originalOfflineSessionMaxLifespanEnabled);
            rep.setClientOfflineSessionIdleTimeout(originalClientOfflineSessionIdleTimeout);
            realm.update(rep);
            clientRepresentation.getAttributes().put(OIDCConfigAttributes.CLIENT_OFFLINE_SESSION_IDLE_TIMEOUT, null);
            client.update(clientRepresentation);
        }
    }

    @Test
    public void offlineTokenRefreshWithoutOfflineAccessScope() {
        ClientManager.realm(adminClient.realm("test")).clientId("offline-client").fullScopeAllowed(false);

        try {
            oauth.scope("openid " + OAuth2Constants.OFFLINE_ACCESS);
            oauth.client("offline-client", "secret1");
            oauth.redirectUri(offlineClientAppUri);
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
            Assert.assertEquals(TokenUtil.TOKEN_TYPE_OFFLINE, offlineToken.getType());
            assertTrue(offlineToken.getScope().contains(OAuth2Constants.OFFLINE_ACCESS));
        }
        finally {
            ClientManager.realm(adminClient.realm("test")).clientId("offline-client").fullScopeAllowed(true);
        }
    }

    @Test
    public void offlineRefreshWhenNoStartedAtClientNote() {
        int prevOfflineSession[] = null;
        try {
            prevOfflineSession = changeOfflineSessionSettings(true, 3600, 3600, 0, 0);

            // login to obtain a refresh token
            oauth.scope("openid " + OAuth2Constants.OFFLINE_ACCESS);
            oauth.client("offline-client", "secret1");
            oauth.redirectUri(offlineClientAppUri);
            oauth.doLogin("test-user@localhost", "password");
            String code = oauth.parseLoginResponse().getCode();
            AccessTokenResponse response = oauth.doAccessTokenRequest(code);

            EventRepresentation loginEvent = events.expectLogin()
                    .client("offline-client")
                    .detail(Details.REDIRECT_URI, offlineClientAppUri)
                    .assertEvent();

            // remove the started notes that can be missed in previous versions
            removeClientSessionStartedAtNote(loginEvent.getSessionId(), loginEvent.getClientId());

            // check refresh is successful
            response = oauth.doRefreshTokenRequest(response.getRefreshToken());
            assertEquals(200, response.getStatusCode());
            assertTrue("Invalid ExpiresIn", 0 < response.getRefreshExpiresIn() && response.getRefreshExpiresIn() <= 3600);

            // check refresh a second time
            response = oauth.doRefreshTokenRequest(response.getRefreshToken());
            assertEquals(200, response.getStatusCode());
            assertTrue("Invalid ExpiresIn", 0 < response.getRefreshExpiresIn() && response.getRefreshExpiresIn() <= 3600);
        } finally {
            changeOfflineSessionSettings(false, prevOfflineSession[0], prevOfflineSession[1], prevOfflineSession[2], prevOfflineSession[3]);
        }
    }

    @Test
    public void offlineRefreshWhenNoOfflineScope() throws Exception {
        // login to obtain a refresh token
        oauth.scope("openid " + OAuth2Constants.OFFLINE_ACCESS);
        oauth.client("offline-client", "secret1");
        oauth.redirectUri(offlineClientAppUri);
        oauth.doLogin("test-user@localhost", "password");
        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse response = oauth.doAccessTokenRequest(code);

        EventRepresentation loginEvent = events.expectLogin()
                .client("offline-client")
                .detail(Details.REDIRECT_URI, offlineClientAppUri)
                .assertEvent();

        events.expectCodeToToken(loginEvent.getDetails().get(Details.CODE_ID), loginEvent.getSessionId())
                .client("offline-client")
                .detail(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_OFFLINE)
                .assertEvent();

        // check refresh is successful
        RefreshToken offlineToken = oauth.parseRefreshToken(response.getRefreshToken());
        oauth.scope(null);
        response = oauth.doRefreshTokenRequest(response.getRefreshToken());
        assertEquals(200, response.getStatusCode());
        Assert.assertEquals(0, response.getRefreshExpiresIn());
        events.expectRefresh(offlineToken.getId(), loginEvent.getSessionId())
                .client("offline-client")
                .user(userId)
                .detail(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_OFFLINE)
                .detail(Details.REFRESH_TOKEN_ID, offlineToken.getId())
                .assertEvent();
        offlineToken = oauth.parseRefreshToken(response.getRefreshToken());

        IntrospectionResponse introspectionResponse = oauth.doIntrospectionAccessTokenRequest(response.getAccessToken());
        assertTrue(introspectionResponse.asJsonNode().get("active").asBoolean());
        events.expect(EventType.INTROSPECT_TOKEN)
                .client("offline-client")
                .session(loginEvent.getSessionId())
                .assertEvent();

        introspectionResponse = oauth.doIntrospectionAccessTokenRequest(response.getRefreshToken());
        assertTrue(introspectionResponse.asJsonNode().get("active").asBoolean());
        events.expect(EventType.INTROSPECT_TOKEN)
                .client("offline-client")
                .session(loginEvent.getSessionId())
                .assertEvent();

        // remove offline scope from the client and perform a second refresh
        try (ClientAttributeUpdater ignored = ClientAttributeUpdater.forClient(adminClient, TEST, "offline-client")
                .removeOptionalClientScope("offline_access").update()) {

            introspectionResponse = oauth.doIntrospectionAccessTokenRequest(response.getAccessToken());
            assertFalse(introspectionResponse.asJsonNode().get("active").asBoolean());
            events.expect(EventType.INTROSPECT_TOKEN_ERROR)
                    .client("offline-client")
                    .session(loginEvent.getSessionId())
                    .error(Errors.SESSION_EXPIRED)
                    .detail(Details.REASON, "Offline session invalid because offline access not granted anymore")
                    .assertEvent();

            introspectionResponse = oauth.doIntrospectionAccessTokenRequest(response.getRefreshToken());
            assertFalse(introspectionResponse.asJsonNode().get("active").asBoolean());
            events.expect(EventType.INTROSPECT_TOKEN_ERROR)
                    .client("offline-client")
                    .session(loginEvent.getSessionId())
                    .error(Errors.SESSION_EXPIRED)
                    .detail(Details.REASON, "Offline session invalid because offline access not granted anymore")
                    .assertEvent();

            response = oauth.doRefreshTokenRequest(response.getRefreshToken());
            assertEquals(400, response.getStatusCode());
            assertEquals(OAuthErrorException.INVALID_GRANT, response.getError());
            assertEquals("Offline session invalid because offline access not granted anymore", response.getErrorDescription());
            events.expect(EventType.REFRESH_TOKEN_ERROR)
                    .client("offline-client")
                    .session(loginEvent.getSessionId())
                    .user((String) null)
                    .error(Errors.INVALID_TOKEN)
                    .detail(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_OFFLINE)
                    .detail(Details.REFRESH_TOKEN_ID, offlineToken.getId())
                    .detail(Details.REASON, "Offline session invalid because offline access not granted anymore")
                    .assertEvent();
        }
    }
}

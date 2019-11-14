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

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.enums.SslRequired;
import org.keycloak.crypto.Algorithm;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.SessionTimeoutHelper;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.util.ClientManager;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.RealmManager;
import org.keycloak.testsuite.util.TokenSignatureUtil;
import org.keycloak.testsuite.util.UserManager;
import org.keycloak.testsuite.util.WaitUtils;
import org.keycloak.util.BasicAuthHelper;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.security.Security;
import java.util.List;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;
import static org.keycloak.testsuite.admin.ApiUtil.findUserByUsername;
import static org.keycloak.testsuite.arquillian.AuthServerTestEnricher.AUTH_SERVER_SSL_REQUIRED;
import static org.keycloak.testsuite.util.OAuthClient.AUTH_SERVER_ROOT;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class RefreshTokenTest extends AbstractKeycloakTest {

    @Page
    protected LoginPage loginPage;

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Override
    public void beforeAbstractKeycloakTest() throws Exception {
        super.beforeAbstractKeycloakTest();
    }

    @BeforeClass
    public static void addBouncyCastleProvider() {
        if (Security.getProvider("BC") == null) Security.addProvider(new BouncyCastleProvider());
    }

    @Before
    public void clientConfiguration() {
        ClientManager.realm(adminClient.realm("test")).clientId("test-app").directAccessGrant(true);
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {

        RealmRepresentation realmRepresentation = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);

        realmRepresentation.getClients().add(org.keycloak.testsuite.util.ClientBuilder.create()
                .clientId("service-account-app")
                .serviceAccount()
                .secret("secret")
                .build());

        RealmBuilder realm = RealmBuilder.edit(realmRepresentation)
                .testEventListener();

        testRealms.add(realm.build());

    }


    /**
     * KEYCLOAK-547
     *
     * @throws Exception
     */
    @Test
    public void nullRefreshToken() throws Exception {
        Client client = ClientBuilder.newClient();
        UriBuilder builder = UriBuilder.fromUri(AUTH_SERVER_ROOT);
        URI uri = OIDCLoginProtocolService.tokenUrl(builder).build("test");
        WebTarget target = client.target(uri);

        org.keycloak.representations.AccessTokenResponse tokenResponse = null;
        {
            String header = BasicAuthHelper.createHeader("test-app", "password");
            Form form = new Form();
            Response response = target.request()
                    .header(HttpHeaders.AUTHORIZATION, header)
                    .post(Entity.form(form));
            assertEquals(400, response.getStatus());
            response.close();
        }
        events.clear();


    }

    @Test
    public void invalidRefreshToken() throws Exception {
        OAuthClient.AccessTokenResponse response = oauth.doRefreshTokenRequest("invalid", "password");
        assertEquals(400, response.getStatusCode());
        assertEquals("invalid_grant", response.getError());
        events.clear();
    }

    @Test
    public void refreshTokenRequest() throws Exception {
        oauth.nonce("123456");
        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin().assertEvent();

        String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "password");
        AccessToken token = oauth.verifyToken(tokenResponse.getAccessToken());
        assertEquals("123456", token.getNonce());

        String refreshTokenString = tokenResponse.getRefreshToken();
        RefreshToken refreshToken = oauth.parseRefreshToken(refreshTokenString);

        EventRepresentation tokenEvent = events.expectCodeToToken(codeId, sessionId).assertEvent();

        assertNotNull(refreshTokenString);

        assertEquals("bearer", tokenResponse.getTokenType());

        Assert.assertThat(token.getExpiration() - getCurrentTime(), allOf(greaterThanOrEqualTo(200), lessThanOrEqualTo(350)));
        int actual = refreshToken.getExpiration() - getCurrentTime();
        Assert.assertThat(actual, allOf(greaterThanOrEqualTo(1799), lessThanOrEqualTo(1800)));

        assertEquals(sessionId, refreshToken.getSessionState());

        setTimeOffset(2);

        OAuthClient.AccessTokenResponse response = oauth.doRefreshTokenRequest(refreshTokenString, "password");
        AccessToken refreshedToken = oauth.verifyToken(response.getAccessToken());
        RefreshToken refreshedRefreshToken = oauth.parseRefreshToken(response.getRefreshToken());

        assertEquals(200, response.getStatusCode());

        assertEquals(sessionId, refreshedToken.getSessionState());
        assertEquals(sessionId, refreshedRefreshToken.getSessionState());

        Assert.assertThat(response.getExpiresIn(), allOf(greaterThanOrEqualTo(250), lessThanOrEqualTo(300)));
        Assert.assertThat(refreshedToken.getExpiration() - getCurrentTime(), allOf(greaterThanOrEqualTo(250), lessThanOrEqualTo(300)));

        Assert.assertThat(refreshedToken.getExpiration() - token.getExpiration(), allOf(greaterThanOrEqualTo(1), lessThanOrEqualTo(10)));
        Assert.assertThat(refreshedRefreshToken.getExpiration() - refreshToken.getExpiration(), allOf(greaterThanOrEqualTo(1), lessThanOrEqualTo(10)));

        // "test-app" should not be an audience in the refresh token
        assertEquals("test-app", refreshedRefreshToken.getIssuedFor());
        Assert.assertFalse(refreshedRefreshToken.hasAudience("test-app"));

        Assert.assertNotEquals(token.getId(), refreshedToken.getId());
        Assert.assertNotEquals(refreshToken.getId(), refreshedRefreshToken.getId());

        assertEquals("bearer", response.getTokenType());

        assertEquals(findUserByUsername(adminClient.realm("test"), "test-user@localhost").getId(), refreshedToken.getSubject());
        Assert.assertNotEquals("test-user@localhost", refreshedToken.getSubject());

        Assert.assertTrue(refreshedToken.getRealmAccess().isUserInRole("user"));

        assertEquals(1, refreshedToken.getResourceAccess(oauth.getClientId()).getRoles().size());
        Assert.assertTrue(refreshedToken.getResourceAccess(oauth.getClientId()).isUserInRole("customer-user"));

        EventRepresentation refreshEvent = events.expectRefresh(tokenEvent.getDetails().get(Details.REFRESH_TOKEN_ID), sessionId).assertEvent();
        Assert.assertNotEquals(tokenEvent.getDetails().get(Details.TOKEN_ID), refreshEvent.getDetails().get(Details.TOKEN_ID));
        Assert.assertNotEquals(tokenEvent.getDetails().get(Details.REFRESH_TOKEN_ID), refreshEvent.getDetails().get(Details.UPDATED_REFRESH_TOKEN_ID));

        assertEquals("123456", refreshedToken.getNonce());

        setTimeOffset(0);
    }
    @Test
    public void refreshTokenWithAccessToken() throws Exception {
        oauth.doLogin("test-user@localhost", "password");

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "password");
        String accessTokenString = tokenResponse.getAccessToken();

        setTimeOffset(2);
        OAuthClient.AccessTokenResponse response = oauth.doRefreshTokenRequest(accessTokenString, "password");

        Assert.assertNotEquals(200, response.getStatusCode());
    }

    @Test
    public void refreshTokenReuseTokenWithoutRefreshTokensRevoked() throws Exception {
        try {
            oauth.doLogin("test-user@localhost", "password");

            EventRepresentation loginEvent = events.expectLogin().assertEvent();

            String sessionId = loginEvent.getSessionId();
            String codeId = loginEvent.getDetails().get(Details.CODE_ID);

            String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

            OAuthClient.AccessTokenResponse response1 = oauth.doAccessTokenRequest(code, "password");
            RefreshToken refreshToken1 = oauth.parseRefreshToken(response1.getRefreshToken());

            events.expectCodeToToken(codeId, sessionId).assertEvent();

            setTimeOffset(2);

            OAuthClient.AccessTokenResponse response2 = oauth.doRefreshTokenRequest(response1.getRefreshToken(), "password");
            assertEquals(200, response2.getStatusCode());

            events.expectRefresh(refreshToken1.getId(), sessionId).assertEvent();

            setTimeOffset(4);

            OAuthClient.AccessTokenResponse response3 = oauth.doRefreshTokenRequest(response1.getRefreshToken(), "password");

            assertEquals(200, response3.getStatusCode());

            events.expectRefresh(refreshToken1.getId(), sessionId).assertEvent();
        } finally {
            setTimeOffset(0);
        }
    }

    @Test
    public void refreshTokenReuseTokenWithRefreshTokensRevoked() throws Exception {
        try {

            RealmManager.realm(adminClient.realm("test")).revokeRefreshToken(true);

            oauth.doLogin("test-user@localhost", "password");

            EventRepresentation loginEvent = events.expectLogin().assertEvent();

            String sessionId = loginEvent.getSessionId();
            String codeId = loginEvent.getDetails().get(Details.CODE_ID);

            String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

            OAuthClient.AccessTokenResponse response1 = oauth.doAccessTokenRequest(code, "password");
            RefreshToken refreshToken1 = oauth.parseRefreshToken(response1.getRefreshToken());

            events.expectCodeToToken(codeId, sessionId).assertEvent();

            setTimeOffset(2);

            OAuthClient.AccessTokenResponse response2 = oauth.doRefreshTokenRequest(response1.getRefreshToken(), "password");
            RefreshToken refreshToken2 = oauth.parseRefreshToken(response2.getRefreshToken());

            assertEquals(200, response2.getStatusCode());

            events.expectRefresh(refreshToken1.getId(), sessionId).assertEvent();

            setTimeOffset(4);

            OAuthClient.AccessTokenResponse response3 = oauth.doRefreshTokenRequest(response1.getRefreshToken(), "password");

            assertEquals(400, response3.getStatusCode());

            events.expectRefresh(refreshToken1.getId(), sessionId).removeDetail(Details.TOKEN_ID).removeDetail(Details.UPDATED_REFRESH_TOKEN_ID).error("invalid_token").assertEvent();

            setTimeOffset(6);
            oauth.doRefreshTokenRequest(response2.getRefreshToken(), "password");

            events.expectRefresh(refreshToken2.getId(), sessionId).assertEvent();
        } finally {
            setTimeOffset(0);
            RealmManager.realm(adminClient.realm("test")).revokeRefreshToken(false);
        }
    }

    @Test
    public void refreshTokenReuseTokenWithRefreshTokensRevokedAfterSingleReuse() throws Exception {
        try {
            RealmManager.realm(adminClient.realm("test"))
                    .revokeRefreshToken(true)
                    .refreshTokenMaxReuse(1);

            oauth.doLogin("test-user@localhost", "password");

            EventRepresentation loginEvent = events.expectLogin().assertEvent();

            String sessionId = loginEvent.getSessionId();
            String codeId = loginEvent.getDetails().get(Details.CODE_ID);

            String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

            OAuthClient.AccessTokenResponse initialResponse = oauth.doAccessTokenRequest(code, "password");
            RefreshToken initialRefreshToken = oauth.parseRefreshToken(initialResponse.getRefreshToken());

            events.expectCodeToToken(codeId, sessionId).assertEvent();

            setTimeOffset(2);

            // Initial refresh.
            OAuthClient.AccessTokenResponse responseFirstUse = oauth.doRefreshTokenRequest(initialResponse.getRefreshToken(), "password");
            RefreshToken newTokenFirstUse = oauth.parseRefreshToken(responseFirstUse.getRefreshToken());

            assertEquals(200, responseFirstUse.getStatusCode());

            events.expectRefresh(initialRefreshToken.getId(), sessionId).assertEvent();

            setTimeOffset(4);

            // Second refresh (allowed).
            OAuthClient.AccessTokenResponse responseFirstReuse = oauth.doRefreshTokenRequest(initialResponse.getRefreshToken(), "password");
            RefreshToken newTokenFirstReuse = oauth.parseRefreshToken(responseFirstReuse.getRefreshToken());

            assertEquals(200, responseFirstReuse.getStatusCode());

            events.expectRefresh(initialRefreshToken.getId(), sessionId).assertEvent();

            setTimeOffset(6);
            // Token reused twice, became invalid.
            OAuthClient.AccessTokenResponse responseSecondReuse = oauth.doRefreshTokenRequest(initialResponse.getRefreshToken(), "password");

            assertEquals(400, responseSecondReuse.getStatusCode());

            events.expectRefresh(initialRefreshToken.getId(), sessionId).removeDetail(Details.TOKEN_ID)
                    .removeDetail(Details.UPDATED_REFRESH_TOKEN_ID).error("invalid_token").assertEvent();

            setTimeOffset(8);
            // Refresh token from first use became invalid.
            OAuthClient.AccessTokenResponse responseUseOfInvalidatedRefreshToken =
                    oauth.doRefreshTokenRequest(responseFirstUse.getRefreshToken(), "password");

            assertEquals(400, responseUseOfInvalidatedRefreshToken.getStatusCode());

            events.expectRefresh(newTokenFirstUse.getId(), sessionId).removeDetail(Details.TOKEN_ID)
                    .removeDetail(Details.UPDATED_REFRESH_TOKEN_ID).error("invalid_token").assertEvent();

            setTimeOffset(10);
            // Refresh token from reuse is still valid.
            OAuthClient.AccessTokenResponse responseUseOfValidRefreshToken =
                    oauth.doRefreshTokenRequest(responseFirstReuse.getRefreshToken(), "password");

            assertEquals(200, responseUseOfValidRefreshToken.getStatusCode());

            events.expectRefresh(newTokenFirstReuse.getId(), sessionId).assertEvent();
        } finally {
            setTimeOffset(0);
            RealmManager.realm(adminClient.realm("test"))
                    .refreshTokenMaxReuse(0)
                    .revokeRefreshToken(false);
        }
    }

    @Test
    public void refreshTokenReuseOfExistingTokenAfterEnablingReuseRevokation() throws Exception {
        try {
            oauth.doLogin("test-user@localhost", "password");

            EventRepresentation loginEvent = events.expectLogin().assertEvent();

            String sessionId = loginEvent.getSessionId();
            String codeId = loginEvent.getDetails().get(Details.CODE_ID);

            String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

            OAuthClient.AccessTokenResponse initialResponse = oauth.doAccessTokenRequest(code, "password");
            RefreshToken initialRefreshToken = oauth.parseRefreshToken(initialResponse.getRefreshToken());

            events.expectCodeToToken(codeId, sessionId).assertEvent();

            setTimeOffset(2);

            // Infinite reuse allowed
            processExpectedValidRefresh(sessionId, initialRefreshToken, initialResponse.getRefreshToken());
            processExpectedValidRefresh(sessionId, initialRefreshToken, initialResponse.getRefreshToken());
            processExpectedValidRefresh(sessionId, initialRefreshToken, initialResponse.getRefreshToken());

            RealmManager.realm(adminClient.realm("test")).revokeRefreshToken(true).refreshTokenMaxReuse(1);

            // Config changed, we start tracking reuse.
            processExpectedValidRefresh(sessionId, initialRefreshToken, initialResponse.getRefreshToken());
            processExpectedValidRefresh(sessionId, initialRefreshToken, initialResponse.getRefreshToken());

            OAuthClient.AccessTokenResponse responseReuseExceeded = oauth.doRefreshTokenRequest(initialResponse.getRefreshToken(), "password");

            assertEquals(400, responseReuseExceeded.getStatusCode());

            events.expectRefresh(initialRefreshToken.getId(), sessionId).removeDetail(Details.TOKEN_ID).removeDetail(Details.UPDATED_REFRESH_TOKEN_ID).error("invalid_token").assertEvent();
        } finally {
            setTimeOffset(0);
            RealmManager.realm(adminClient.realm("test"))
                    .refreshTokenMaxReuse(0)
                    .revokeRefreshToken(false);
        }
    }

    @Test
    public void refreshTokenReuseOfExistingTokenAfterDisablingReuseRevokation() throws Exception {
        try {
            RealmManager.realm(adminClient.realm("test")).revokeRefreshToken(true).refreshTokenMaxReuse(1);

            oauth.doLogin("test-user@localhost", "password");

            EventRepresentation loginEvent = events.expectLogin().assertEvent();

            String sessionId = loginEvent.getSessionId();
            String codeId = loginEvent.getDetails().get(Details.CODE_ID);

            String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

            OAuthClient.AccessTokenResponse initialResponse = oauth.doAccessTokenRequest(code, "password");
            RefreshToken initialRefreshToken = oauth.parseRefreshToken(initialResponse.getRefreshToken());

            events.expectCodeToToken(codeId, sessionId).assertEvent();

            setTimeOffset(2);

            // Single reuse authorized.
            processExpectedValidRefresh(sessionId, initialRefreshToken, initialResponse.getRefreshToken());
            processExpectedValidRefresh(sessionId, initialRefreshToken, initialResponse.getRefreshToken());

            OAuthClient.AccessTokenResponse responseReuseExceeded = oauth.doRefreshTokenRequest(initialResponse.getRefreshToken(), "password");

            assertEquals(400, responseReuseExceeded.getStatusCode());

            events.expectRefresh(initialRefreshToken.getId(), sessionId).removeDetail(Details.TOKEN_ID)
                    .removeDetail(Details.UPDATED_REFRESH_TOKEN_ID).error("invalid_token").assertEvent();

            RealmManager.realm(adminClient.realm("test")).revokeRefreshToken(false);

            // Config changed, token can be reused again
            processExpectedValidRefresh(sessionId, initialRefreshToken, initialResponse.getRefreshToken());
            processExpectedValidRefresh(sessionId, initialRefreshToken, initialResponse.getRefreshToken());
            processExpectedValidRefresh(sessionId, initialRefreshToken, initialResponse.getRefreshToken());
        } finally {
            setTimeOffset(0);
            RealmManager.realm(adminClient.realm("test"))
                    .refreshTokenMaxReuse(0)
                    .revokeRefreshToken(false);
        }
    }

    private void processExpectedValidRefresh(String sessionId, RefreshToken requestToken, String refreshToken) {
        OAuthClient.AccessTokenResponse response2 = oauth.doRefreshTokenRequest(refreshToken, "password");

        assertEquals(200, response2.getStatusCode());

        events.expectRefresh(requestToken.getId(), sessionId).assertEvent();
    }


    String privateKey;
    String publicKey;

    @Test
    public void refreshTokenClientDisabled() throws Exception {
        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin().assertEvent();

        String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");
        String refreshTokenString = response.getRefreshToken();
        RefreshToken refreshToken = oauth.parseRefreshToken(refreshTokenString);

        events.expectCodeToToken(codeId, sessionId).assertEvent();

        try {
            ClientManager.realm(adminClient.realm("test")).clientId(oauth.getClientId()).enabled(false);

            setTimeOffset(2);
            response = oauth.doRefreshTokenRequest(refreshTokenString, "password");

            assertEquals(400, response.getStatusCode());
            assertEquals("unauthorized_client", response.getError());

            events.expectRefresh(refreshToken.getId(), sessionId).user((String) null).session((String) null).clearDetails().error(Errors.CLIENT_DISABLED).assertEvent();
        } finally {
            ClientManager.realm(adminClient.realm("test")).clientId(oauth.getClientId()).enabled(true);
        }
    }

    @Test
    public void refreshTokenUserSessionExpired() {
        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin().assertEvent();

        String sessionId = loginEvent.getSessionId();

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "password");

        events.poll();

        String refreshId = oauth.parseRefreshToken(tokenResponse.getRefreshToken()).getId();

        testingClient.testing().removeUserSession("test", sessionId);

        setTimeOffset(2);
        tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken(), "password");

        assertEquals(400, tokenResponse.getStatusCode());
        assertNull(tokenResponse.getAccessToken());
        assertNull(tokenResponse.getRefreshToken());

        events.expectRefresh(refreshId, sessionId).error(Errors.INVALID_TOKEN);

        events.clear();
    }

    @Test
    public void refreshTokenAfterUserLogoutAndLoginAgain() {
        String refreshToken1 = loginAndForceNewLoginPage();

        oauth.doLogout(refreshToken1, "password");
        events.clear();

        // Set time offset to 2 (Just to simulate to be more close to real situation)
        setTimeOffset(2);

        // Continue with login
        oauth.fillLoginForm("test-user@localhost", "password");

        assertFalse(loginPage.isCurrent());

        OAuthClient.AccessTokenResponse tokenResponse2 = null;
        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        tokenResponse2 = oauth.doAccessTokenRequest(code, "password");

        setTimeOffset(4);
        // Now try refresh with the original refreshToken1 created in logged-out userSession. It should fail
        OAuthClient.AccessTokenResponse responseReuseExceeded = oauth.doRefreshTokenRequest(refreshToken1, "password");
        assertEquals(400, responseReuseExceeded.getStatusCode());

        setTimeOffset(6);
        // Finally try with valid refresh token
        responseReuseExceeded = oauth.doRefreshTokenRequest(tokenResponse2.getRefreshToken(), "password");
        assertEquals(200, responseReuseExceeded.getStatusCode());
    }

    @Test
    public void refreshTokenAfterAdminLogoutAllAndLoginAgain() {
        String refreshToken1 = loginAndForceNewLoginPage();

        adminClient.realm("test").logoutAll();
        // Must wait for server to execute the request. Sometimes, there is issue with the execution and another tests failed, because of this.
        WaitUtils.pause(500);

        events.clear();

        // Set time offset to 2 (Just to simulate to be more close to real situation)
        setTimeOffset(2);

        // Continue with login
        oauth.fillLoginForm("test-user@localhost", "password");

        assertFalse(loginPage.isCurrent());

        OAuthClient.AccessTokenResponse tokenResponse2 = null;
        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        tokenResponse2 = oauth.doAccessTokenRequest(code, "password");

        setTimeOffset(4);

        // Now try refresh with the original refreshToken1 created in logged-out userSession. It should fail
        OAuthClient.AccessTokenResponse responseReuseExceeded = oauth.doRefreshTokenRequest(refreshToken1, "password");
        assertEquals(400, responseReuseExceeded.getStatusCode());

        setTimeOffset(6);

        // Finally try with valid refresh token
        responseReuseExceeded = oauth.doRefreshTokenRequest(tokenResponse2.getRefreshToken(), "password");
        assertEquals(200, responseReuseExceeded.getStatusCode());
    }

    @Test
    @AuthServerContainerExclude(AuthServerContainerExclude.AuthServer.REMOTE)
    public void refreshTokenAfterUserAdminLogoutEndpointAndLoginAgain() {
        try {
            String refreshToken1 = loginAndForceNewLoginPage();

            RefreshToken refreshTokenParsed1 = oauth.parseRefreshToken(refreshToken1);
            String userId = refreshTokenParsed1.getSubject();
            UserResource user = adminClient.realm("test").users().get(userId);
            user.logout();

            // Set time offset to 2 (Just to simulate to be more close to real situation)
            setTimeOffset(2);

            // Continue with login
            oauth.fillLoginForm("test-user@localhost", "password");

            assertFalse(loginPage.isCurrent());

            OAuthClient.AccessTokenResponse tokenResponse2 = null;
            String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
            tokenResponse2 = oauth.doAccessTokenRequest(code, "password");

            setTimeOffset(4);

            // Now try refresh with the original refreshToken1 created in logged-out userSession. It should fail
            OAuthClient.AccessTokenResponse responseReuseExceeded = oauth.doRefreshTokenRequest(refreshToken1, "password");
            assertEquals(400, responseReuseExceeded.getStatusCode());

            setTimeOffset(6);

            // Finally try with valid refresh token
            responseReuseExceeded = oauth.doRefreshTokenRequest(tokenResponse2.getRefreshToken(), "password");
            assertEquals(200, responseReuseExceeded.getStatusCode());
        } finally {
            // Need to reset not-before of user, which was updated during user.logout()
            testingClient.server().run(session -> {
                RealmModel realm = session.realms().getRealmByName("test");
                UserModel user = session.users().getUserByUsername("test-user@localhost", realm);
                session.users().setNotBeforeForUser(realm, user, 0);
            });
        }
    }

    @Test
    public void testUserSessionRefreshAndIdle() throws Exception {
        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin().assertEvent();

        String sessionId = loginEvent.getSessionId();

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "password");

        events.poll();

        String refreshId = oauth.parseRefreshToken(tokenResponse.getRefreshToken()).getId();

        int last = testingClient.testing().getLastSessionRefresh("test", sessionId, false);

        setTimeOffset(2);

        tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken(), "password");

        AccessToken refreshedToken = oauth.verifyToken(tokenResponse.getAccessToken());
        RefreshToken refreshedRefreshToken = oauth.parseRefreshToken(tokenResponse.getRefreshToken());

        assertEquals(200, tokenResponse.getStatusCode());

        int next = testingClient.testing().getLastSessionRefresh("test", sessionId, false);

        Assert.assertNotEquals(last, next);

        RealmResource realmResource = adminClient.realm("test");
        int lastAccessTokenLifespan = realmResource.toRepresentation().getAccessTokenLifespan();
        int originalIdle = realmResource.toRepresentation().getSsoSessionIdleTimeout();

        try {
            RealmManager.realm(realmResource).accessTokenLifespan(100000);

            setTimeOffset(4);
            tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken(), "password");

            next = testingClient.testing().getLastSessionRefresh("test", sessionId, false);

            // lastSEssionRefresh should be updated because access code lifespan is higher than sso idle timeout
            Assert.assertThat(next, allOf(greaterThan(last), lessThan(last + 50)));

            RealmManager.realm(realmResource).ssoSessionIdleTimeout(1);

            events.clear();
            // Needs to add some additional time due the tollerance allowed by IDLE_TIMEOUT_WINDOW_SECONDS
            setTimeOffset(6 + SessionTimeoutHelper.IDLE_TIMEOUT_WINDOW_SECONDS);
            tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken(), "password");

            // test idle timeout
            assertEquals(400, tokenResponse.getStatusCode());
            assertNull(tokenResponse.getAccessToken());
            assertNull(tokenResponse.getRefreshToken());

            events.expectRefresh(refreshId, sessionId).error(Errors.INVALID_TOKEN);

        } finally {
            RealmManager.realm(realmResource).ssoSessionIdleTimeout(originalIdle).accessTokenLifespan(lastAccessTokenLifespan);
            events.clear();
            setTimeOffset(0);
        }

    }

    @Test
    public void testUserSessionRefreshAndIdleRememberMe() throws Exception {
        RealmResource testRealm = adminClient.realm("test");
        RealmRepresentation testRealmRep = testRealm.toRepresentation();
        Boolean previousRememberMe = testRealmRep.isRememberMe();
        int originalIdleRememberMe = testRealmRep.getSsoSessionIdleTimeoutRememberMe();

        try {
            testRealmRep.setRememberMe(true);
            testRealm.update(testRealmRep);

            oauth.doRememberMeLogin("test-user@localhost", "password");

            EventRepresentation loginEvent = events.expectLogin().assertEvent();

            String sessionId = loginEvent.getSessionId();

            String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
            OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "password");

            events.poll();

            String refreshId = oauth.parseRefreshToken(tokenResponse.getRefreshToken()).getId();
            int last = testingClient.testing().getLastSessionRefresh("test", sessionId, false);

            setTimeOffset(2);
            tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken(), "password");
            oauth.verifyToken(tokenResponse.getAccessToken());
            oauth.parseRefreshToken(tokenResponse.getRefreshToken());
            assertEquals(200, tokenResponse.getStatusCode());

            int next = testingClient.testing().getLastSessionRefresh("test", sessionId, false);
            Assert.assertNotEquals(last, next);

            testRealmRep.setSsoSessionIdleTimeoutRememberMe(1);
            testRealm.update(testRealmRep);

            events.clear();
            // Needs to add some additional time due the tollerance allowed by IDLE_TIMEOUT_WINDOW_SECONDS
            setTimeOffset(6 + SessionTimeoutHelper.IDLE_TIMEOUT_WINDOW_SECONDS);
            tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken(), "password");

            // test idle remember me timeout
            assertEquals(400, tokenResponse.getStatusCode());
            assertNull(tokenResponse.getAccessToken());
            assertNull(tokenResponse.getRefreshToken());

            events.expectRefresh(refreshId, sessionId).error(Errors.INVALID_TOKEN);
            events.clear();

        } finally {
            testRealmRep.setSsoSessionIdleTimeoutRememberMe(originalIdleRememberMe);
            testRealmRep.setRememberMe(previousRememberMe);
            testRealm.update(testRealmRep);
            setTimeOffset(0);
        }
    }

    @Test
    public void refreshTokenUserSessionMaxLifespan() throws Exception {
        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin().assertEvent();

        String sessionId = loginEvent.getSessionId();

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "password");

        events.poll();

        String refreshId = oauth.parseRefreshToken(tokenResponse.getRefreshToken()).getId();

        RealmResource realmResource = adminClient.realm("test");
        Integer maxLifespan = realmResource.toRepresentation().getSsoSessionMaxLifespan();
        try {
            RealmManager.realm(realmResource).ssoSessionMaxLifespan(1);

            setTimeOffset(2);

            tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken(), "password");

            assertEquals(400, tokenResponse.getStatusCode());
            assertNull(tokenResponse.getAccessToken());
            assertNull(tokenResponse.getRefreshToken());

            events.expectRefresh(refreshId, sessionId).error(Errors.INVALID_TOKEN);
        } finally {
            RealmManager.realm(realmResource).ssoSessionMaxLifespan(maxLifespan);
            events.clear();
            resetTimeOffset();
        }
    }

    /**
     * KEYCLOAK-1267
     * @throws Exception
     */
    @Test
    public void refreshTokenUserSessionMaxLifespanWithRememberMe() throws Exception {

        RealmResource testRealm = adminClient.realm("test");
        RealmRepresentation testRealmRep = testRealm.toRepresentation();
        Boolean previousRememberMe = testRealmRep.isRememberMe();
        int previousSsoMaxLifespanRememberMe = testRealmRep.getSsoSessionMaxLifespanRememberMe();

        try {
            testRealmRep.setRememberMe(true);
            testRealm.update(testRealmRep);

            oauth.doRememberMeLogin("test-user@localhost", "password");

            EventRepresentation loginEvent = events.expectLogin().assertEvent();

            String sessionId = loginEvent.getSessionId();

            String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
            OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "password");

            events.poll();

            String refreshId = oauth.parseRefreshToken(tokenResponse.getRefreshToken()).getId();

            testRealmRep.setSsoSessionMaxLifespanRememberMe(1);
            testRealm.update(testRealmRep);

            setTimeOffset(2);

            tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken(), "password");

            assertEquals(400, tokenResponse.getStatusCode());
            assertNull(tokenResponse.getAccessToken());
            assertNull(tokenResponse.getRefreshToken());

            events.expectRefresh(refreshId, sessionId).error(Errors.INVALID_TOKEN);
            events.clear();

        } finally {
            testRealmRep.setSsoSessionMaxLifespanRememberMe(previousSsoMaxLifespanRememberMe);
            testRealmRep.setRememberMe(previousRememberMe);
            testRealm.update(testRealmRep);
            setTimeOffset(0);
        }
    }

    @Test
    public void testCheckSsl() throws Exception {
        Client client = ClientBuilder.newClient();
        try {
            UriBuilder builder = UriBuilder.fromUri(AUTH_SERVER_ROOT);
            URI grantUri = OIDCLoginProtocolService.tokenUrl(builder).build("test");
            WebTarget grantTarget = client.target(grantUri);
            builder = UriBuilder.fromUri(AUTH_SERVER_ROOT);
            URI uri = OIDCLoginProtocolService.tokenUrl(builder).build("test");
            WebTarget refreshTarget = client.target(uri);

            String refreshToken = null;
            {
                Response response = executeGrantAccessTokenRequest(grantTarget);
                assertEquals(200, response.getStatus());
                org.keycloak.representations.AccessTokenResponse tokenResponse = response.readEntity(org.keycloak.representations.AccessTokenResponse.class);
                refreshToken = tokenResponse.getRefreshToken();
                response.close();
            }

            {
                Response response = executeRefreshToken(refreshTarget, refreshToken);
                assertEquals(200, response.getStatus());
                org.keycloak.representations.AccessTokenResponse tokenResponse = response.readEntity(org.keycloak.representations.AccessTokenResponse.class);
                refreshToken = tokenResponse.getRefreshToken();
                response.close();
            }

            if (!AUTH_SERVER_SSL_REQUIRED) {   // test checkSsl
                RealmResource realmResource = adminClient.realm("test");
                {
                    RealmManager.realm(realmResource).sslRequired(SslRequired.ALL.toString());
                }

                Response response = executeRefreshToken(refreshTarget, refreshToken);
                assertEquals(403, response.getStatus());
                response.close();

                {
                    RealmManager.realm(realmResource).sslRequired(SslRequired.EXTERNAL.toString());
                }
            }

            {
                Response response = executeRefreshToken(refreshTarget, refreshToken);
                assertEquals(200, response.getStatus());
                org.keycloak.representations.AccessTokenResponse tokenResponse = response.readEntity(org.keycloak.representations.AccessTokenResponse.class);
                refreshToken = tokenResponse.getRefreshToken();
                response.close();
            }
        } finally {
            client.close();
            resetTimeOffset();
            events.clear();
        }

    }

    @Test
    public void refreshTokenUserDisabled() throws Exception {
        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin().assertEvent();

        String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");
        String refreshTokenString = response.getRefreshToken();
        RefreshToken refreshToken = oauth.parseRefreshToken(refreshTokenString);

        events.expectCodeToToken(codeId, sessionId).assertEvent();

        try {
            UserManager.realm(adminClient.realm("test")).username("test-user@localhost").enabled(false);
            setTimeOffset(2);
            response = oauth.doRefreshTokenRequest(refreshTokenString, "password");
            assertEquals(400, response.getStatusCode());
            assertEquals("invalid_grant", response.getError());

            events.expectRefresh(refreshToken.getId(), sessionId).clearDetails().error(Errors.INVALID_TOKEN).assertEvent();
        } finally {
            UserManager.realm(adminClient.realm("test")).username("test-user@localhost").enabled(true);
        }
    }

    @Test
    public void refreshTokenUserDeleted() throws Exception {
        String userId = createUser("test", "temp-user@localhost", "password");
        oauth.doLogin("temp-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin().user(userId).assertEvent();

        String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");
        String refreshTokenString = response.getRefreshToken();
        RefreshToken refreshToken = oauth.parseRefreshToken(refreshTokenString);

        events.expectCodeToToken(codeId, sessionId).user(userId).assertEvent();

        adminClient.realm("test").users().delete(userId);

        setTimeOffset(2);
        response = oauth.doRefreshTokenRequest(refreshTokenString, "password");
        assertEquals(400, response.getStatusCode());
        assertEquals("invalid_grant", response.getError());

        events.expectRefresh(refreshToken.getId(), sessionId).user(userId).clearDetails().error(Errors.INVALID_TOKEN).assertEvent();
    }

    @Test
    public void refreshTokenServiceAccount() throws Exception {
        OAuthClient.AccessTokenResponse response = oauth.clientId("service-account-app").doClientCredentialsGrantAccessTokenRequest("secret");

        assertNotNull(response.getRefreshToken());

        response = oauth.doRefreshTokenRequest(response.getRefreshToken(), "secret");

        assertNotNull(response.getRefreshToken());
    }

    @Test
    public void tokenRefreshRequest_ClientRS384_RealmRS384() throws Exception {
        conductTokenRefreshRequest(Algorithm.HS256, Algorithm.RS384, Algorithm.RS384);
    }

    @Test
    public void tokenRefreshRequest_ClientRS512_RealmRS256() throws Exception {
        conductTokenRefreshRequest(Algorithm.HS256, Algorithm.RS512, Algorithm.RS256);
    }

    @Test
    public void tokenRefreshRequest_ClientES256_RealmRS256() throws Exception {
        conductTokenRefreshRequest(Algorithm.HS256, Algorithm.ES256, Algorithm.RS256);
    }

    @Test
    public void tokenRefreshRequest_ClientES384_RealmES384() throws Exception {
        conductTokenRefreshRequest(Algorithm.HS256, Algorithm.ES384, Algorithm.ES384);
    }

    @Test
    public void tokenRefreshRequest_ClientES512_RealmRS256() throws Exception {
        conductTokenRefreshRequest(Algorithm.HS256, Algorithm.ES512, Algorithm.RS256);
    }

    @Test
    public void tokenRefreshRequest_ClientPS256_RealmRS256() throws Exception {
        conductTokenRefreshRequest(Algorithm.HS256, Algorithm.PS256, Algorithm.RS256);
    }

    @Test
    public void tokenRefreshRequest_ClientPS384_RealmES384() throws Exception {
        conductTokenRefreshRequest(Algorithm.HS256, Algorithm.PS384, Algorithm.ES384);
    }

    @Test
    public void tokenRefreshRequest_ClientPS512_RealmPS256() throws Exception {
        conductTokenRefreshRequest(Algorithm.HS256, Algorithm.PS512, Algorithm.PS256);
    }

    protected Response executeRefreshToken(WebTarget refreshTarget, String refreshToken) {
        String header = BasicAuthHelper.createHeader("test-app", "password");
        Form form = new Form();
        form.param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.REFRESH_TOKEN);
        form.param("refresh_token", refreshToken);
        return refreshTarget.request()
                .header(HttpHeaders.AUTHORIZATION, header)
                .post(Entity.form(form));
    }

    protected Response executeGrantAccessTokenRequest(WebTarget grantTarget) {
        String header = BasicAuthHelper.createHeader("test-app", "password");
        Form form = new Form();
        form.param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.PASSWORD)
                .param("username", "test-user@localhost")
                .param("password", "password");
        return grantTarget.request()
                .header(HttpHeaders.AUTHORIZATION, header)
                .post(Entity.form(form));
    }

    private void conductTokenRefreshRequest(String expectedRefreshAlg, String expectedAccessAlg, String expectedIdTokenAlg) throws Exception {
        try {
            // Realm setting is used for ID Token signature algorithm
            TokenSignatureUtil.changeRealmTokenSignatureProvider(adminClient, expectedIdTokenAlg);
            TokenSignatureUtil.changeClientAccessTokenSignatureProvider(ApiUtil.findClientByClientId(adminClient.realm("test"), "test-app"), expectedAccessAlg);
            refreshToken(expectedRefreshAlg, expectedAccessAlg, expectedIdTokenAlg);
        } finally {
            TokenSignatureUtil.changeRealmTokenSignatureProvider(adminClient, Algorithm.RS256);
            TokenSignatureUtil.changeClientAccessTokenSignatureProvider(ApiUtil.findClientByClientId(adminClient.realm("test"), "test-app"), Algorithm.RS256);
        }
    }

    private void refreshToken(String expectedRefreshAlg, String expectedAccessAlg, String expectedIdTokenAlg) throws Exception {
        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin().assertEvent();

        String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "password");

        JWSHeader header = new JWSInput(tokenResponse.getAccessToken()).getHeader();
        assertEquals(expectedAccessAlg, header.getAlgorithm().name());
        assertEquals("JWT", header.getType());
        assertNull(header.getContentType());

        header = new JWSInput(tokenResponse.getIdToken()).getHeader();
        assertEquals(expectedIdTokenAlg, header.getAlgorithm().name());
        assertEquals("JWT", header.getType());
        assertNull(header.getContentType());

        header = new JWSInput(tokenResponse.getRefreshToken()).getHeader();
        assertEquals(expectedRefreshAlg, header.getAlgorithm().name());
        assertEquals("JWT", header.getType());
        assertNull(header.getContentType());

        AccessToken token = oauth.verifyToken(tokenResponse.getAccessToken());
        String refreshTokenString = tokenResponse.getRefreshToken();
        RefreshToken refreshToken = oauth.parseRefreshToken(refreshTokenString);

        EventRepresentation tokenEvent = events.expectCodeToToken(codeId, sessionId).assertEvent();

        assertNotNull(refreshTokenString);

        assertEquals("bearer", tokenResponse.getTokenType());

        assertEquals(sessionId, refreshToken.getSessionState());

        setTimeOffset(2);

        OAuthClient.AccessTokenResponse response = oauth.doRefreshTokenRequest(refreshTokenString, "password");
        if (response.getError() != null || response.getErrorDescription() != null) {
            log.debugf("Refresh token error: %s, error description: %s", response.getError(), response.getErrorDescription());
        }

        AccessToken refreshedToken = oauth.verifyToken(response.getAccessToken());
        RefreshToken refreshedRefreshToken = oauth.parseRefreshToken(response.getRefreshToken());

        assertEquals(200, response.getStatusCode());

        assertEquals(sessionId, refreshedToken.getSessionState());
        assertEquals(sessionId, refreshedRefreshToken.getSessionState());

        Assert.assertNotEquals(token.getId(), refreshedToken.getId());
        Assert.assertNotEquals(refreshToken.getId(), refreshedRefreshToken.getId());

        assertEquals("bearer", response.getTokenType());

        assertEquals(findUserByUsername(adminClient.realm("test"), "test-user@localhost").getId(), refreshedToken.getSubject());
        Assert.assertNotEquals("test-user@localhost", refreshedToken.getSubject());

        EventRepresentation refreshEvent = events.expectRefresh(tokenEvent.getDetails().get(Details.REFRESH_TOKEN_ID), sessionId).assertEvent();
        Assert.assertNotEquals(tokenEvent.getDetails().get(Details.TOKEN_ID), refreshEvent.getDetails().get(Details.TOKEN_ID));
        Assert.assertNotEquals(tokenEvent.getDetails().get(Details.REFRESH_TOKEN_ID), refreshEvent.getDetails().get(Details.UPDATED_REFRESH_TOKEN_ID));

        setTimeOffset(0);
    }

    private String loginAndForceNewLoginPage() {
        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin().assertEvent();

        String sessionId = loginEvent.getSessionId();

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "password");

        events.poll();

        // Assert refresh successful
        String refreshToken = tokenResponse.getRefreshToken();
        RefreshToken refreshTokenParsed1 = oauth.parseRefreshToken(tokenResponse.getRefreshToken());
        processExpectedValidRefresh(sessionId, refreshTokenParsed1, refreshToken);

        // Set time offset to 1 (Just to simulate to be more close to real situation)
        setTimeOffset(1);

        // Open the tab with prompt=login. AuthenticationSession will be created with same ID like userSession
        String loginFormUri = UriBuilder.fromUri(oauth.getLoginFormUrl())
                .queryParam(OIDCLoginProtocol.PROMPT_PARAM, OIDCLoginProtocol.PROMPT_VALUE_LOGIN)
                .build().toString();
        driver.navigate().to(loginFormUri);

        loginPage.assertCurrent();

        return refreshToken;
    }
}

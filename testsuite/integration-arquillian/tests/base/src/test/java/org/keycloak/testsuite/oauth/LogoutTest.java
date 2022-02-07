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

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.common.util.Time;
import org.keycloak.events.Details;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.util.*;

import java.util.List;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlEquals;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class LogoutTest extends AbstractKeycloakTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected LoginPage loginPage;

    @Override
    public void beforeAbstractKeycloakTest() throws Exception {
        super.beforeAbstractKeycloakTest();
    }

    @Before
    public void clientConfiguration() {
        ClientManager.realm(adminClient.realm("test")).clientId("test-app").directAccessGrant(true);
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realmRepresentation = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);
        RealmBuilder realm = RealmBuilder.edit(realmRepresentation).testEventListener();

        testRealms.add(realm.build());
    }

    @Test
    public void postLogout() throws Exception {
        oauth.doLogin("test-user@localhost", "password");

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        oauth.clientSessionState("client-session");
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "password");
        String refreshTokenString = tokenResponse.getRefreshToken();

        try (CloseableHttpResponse response = oauth.doLogout(refreshTokenString, "password")) {
            assertThat(response, Matchers.statusCodeIsHC(Status.NO_CONTENT));

            assertNotNull(testingClient.testApp().getAdminLogoutAction());
        }
    }

    @Test
    public void postLogoutExpiredRefreshToken() throws Exception {
        oauth.doLogin("test-user@localhost", "password");

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        oauth.clientSessionState("client-session");
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "password");
        String refreshTokenString = tokenResponse.getRefreshToken();

        adminClient.realm("test").update(RealmBuilder.create().notBefore(Time.currentTime() + 1).build());

        // Logout should succeed with expired refresh token, see KEYCLOAK-3302
        try (CloseableHttpResponse response = oauth.doLogout(refreshTokenString, "password")) {
            assertThat(response, Matchers.statusCodeIsHC(Status.NO_CONTENT));

            assertNotNull(testingClient.testApp().getAdminLogoutAction());
        }
    }

    @Test
    public void logoutIDTokenHint() {
        oauth.doLogin("test-user@localhost", "password");

        String sessionId = events.expectLogin().assertEvent().getSessionId();

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "password");
        String idToken = tokenResponse.getIdToken();

        events.clear();

        driver.navigate().to(oauth.getLogoutUrl().redirectUri(oauth.APP_AUTH_ROOT).idTokenHint(idToken).build());
        events.expectLogout(sessionId).detail(Details.REDIRECT_URI, oauth.APP_AUTH_ROOT).assertEvent();

        assertCurrentUrlEquals(oauth.APP_AUTH_ROOT);
    }

    @Test
    public void browserLogoutWithAccessToken() {
        oauth.doLogin("test-user@localhost", "password");

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "password");
        String accessToken = tokenResponse.getAccessToken();

        events.clear();

        driver.navigate().to(oauth.getLogoutUrl().redirectUri(oauth.APP_AUTH_ROOT).idTokenHint(accessToken).build());

        events.expectLogoutError(OAuthErrorException.INVALID_TOKEN).assertEvent();
    }

    @Test
    public void postLogoutWithRefreshTokenAfterUserSessionLogoutAndLoginAgain() throws Exception {
        // Login
        OAuthClient.AccessTokenResponse accessTokenResponse = loginAndForceNewLoginPage();
        String refreshToken1 = accessTokenResponse.getRefreshToken();

        oauth.doLogout(refreshToken1, "password");

        setTimeOffset(2);

        WaitUtils.waitForPageToLoad();
        loginPage.login("password");

        Assert.assertFalse(loginPage.isCurrent());

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse tokenResponse2 = oauth.doAccessTokenRequest(code, "password");

        // POST logout with token should fail
        try (CloseableHttpResponse response = oauth.doLogout(refreshToken1, "password")) {
            assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatusLine().getStatusCode());
        }

        String logoutUrl = oauth.getLogoutUrl()
                .idTokenHint(accessTokenResponse.getIdToken())
                .postLogoutRedirectUri(oauth.APP_AUTH_ROOT)
                .build();

        // GET logout with ID token should fail as well
        try (CloseableHttpClient c = HttpClientBuilder.create().disableRedirectHandling().build();
             CloseableHttpResponse response = c.execute(new HttpGet(logoutUrl))) {
            assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatusLine().getStatusCode());
        }

        // finally POST logout with VALID token should succeed
        try (CloseableHttpResponse response = oauth.doLogout(tokenResponse2.getRefreshToken(), "password")) {
            assertThat(response, Matchers.statusCodeIsHC(Status.NO_CONTENT));

            assertNotNull(testingClient.testApp().getAdminLogoutAction());
        }
    }


    @Test
    public void postLogoutFailWithCredentialsOfDifferentClient() throws Exception {
        oauth.doLogin("test-user@localhost", "password");

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        oauth.clientSessionState("client-session");
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "password");
        String refreshTokenString = tokenResponse.getRefreshToken();

        oauth.clientId("test-app-scope");

        // Assert logout fails with 400 when trying to use different client credentials
        try (CloseableHttpResponse response = oauth.doLogout(refreshTokenString, "password")) {
            assertThat(response, Matchers.statusCodeIsHC(Status.BAD_REQUEST));
        }

        oauth.clientId("test-app");
    }

    @Test
    public void postLogoutWithValidIdToken() throws Exception {
        oauth.doLogin("test-user@localhost", "password");

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        oauth.clientSessionState("client-session");
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "password");
        String idTokenString = tokenResponse.getIdToken();

        String logoutUrl = oauth.getLogoutUrl()
          .idTokenHint(idTokenString)
          .postLogoutRedirectUri(oauth.APP_AUTH_ROOT)
          .build();
        
        try (CloseableHttpClient c = HttpClientBuilder.create().disableRedirectHandling().build();
          CloseableHttpResponse response = c.execute(new HttpGet(logoutUrl))) {
            assertThat(response, Matchers.statusCodeIsHC(Status.FOUND));
            assertThat(response.getFirstHeader(HttpHeaders.LOCATION).getValue(), is(oauth.APP_AUTH_ROOT));
        }
    }

    @Test
    public void postLogoutWithExpiredIdToken() throws Exception {
        oauth.doLogin("test-user@localhost", "password");

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        oauth.clientSessionState("client-session");
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "password");
        String idTokenString = tokenResponse.getIdToken();

        // Logout should succeed with expired ID token, see KEYCLOAK-3399
        setTimeOffset(60 * 60 * 24);

        String logoutUrl = oauth.getLogoutUrl()
          .idTokenHint(idTokenString)
          .postLogoutRedirectUri(oauth.APP_AUTH_ROOT)
          .build();

        try (CloseableHttpClient c = HttpClientBuilder.create().disableRedirectHandling().build();
          CloseableHttpResponse response = c.execute(new HttpGet(logoutUrl))) {
            assertThat(response, Matchers.statusCodeIsHC(Status.FOUND));
            assertThat(response.getFirstHeader(HttpHeaders.LOCATION).getValue(), is(oauth.APP_AUTH_ROOT));
        }
    }

    @Test
    public void postLogoutWithValidIdTokenWhenLoggedOutByAdmin() throws Exception {
        oauth.doLogin("test-user@localhost", "password");

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        oauth.clientSessionState("client-session");
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "password");
        String idTokenString = tokenResponse.getIdToken();

        adminClient.realm("test").logoutAll();

        // Logout should succeed with user already logged out, see KEYCLOAK-3399
        String logoutUrl = oauth.getLogoutUrl()
          .idTokenHint(idTokenString)
          .postLogoutRedirectUri(oauth.APP_AUTH_ROOT)
          .build();

        try (CloseableHttpClient c = HttpClientBuilder.create().disableRedirectHandling().build();
          CloseableHttpResponse response = c.execute(new HttpGet(logoutUrl))) {
            assertThat(response, Matchers.statusCodeIsHC(Status.FOUND));
            assertThat(response.getFirstHeader(HttpHeaders.LOCATION).getValue(), is(oauth.APP_AUTH_ROOT));
        }
    }

    private void backchannelLogoutRequest(String expectedRefreshAlg, String expectedAccessAlg, String expectedIdTokenAlg) throws Exception {
        oauth.doLogin("test-user@localhost", "password");

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        oauth.clientSessionState("client-session");
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "password");
        String idTokenString = tokenResponse.getIdToken();

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

        String logoutUrl = oauth.getLogoutUrl()
          .idTokenHint(idTokenString)
          .postLogoutRedirectUri(oauth.APP_AUTH_ROOT)
          .build();
        
        try (CloseableHttpClient c = HttpClientBuilder.create().disableRedirectHandling().build();
          CloseableHttpResponse response = c.execute(new HttpGet(logoutUrl))) {
            assertThat(response, Matchers.statusCodeIsHC(Status.FOUND));
            assertThat(response.getFirstHeader(HttpHeaders.LOCATION).getValue(), is(oauth.APP_AUTH_ROOT));
        }
    }

    @Test
    public void backchannelLogoutRequest_RealmRS384_ClientRS512() throws Exception {
        try {
            TokenSignatureUtil.changeRealmTokenSignatureProvider(adminClient, "RS384");
            TokenSignatureUtil.changeClientAccessTokenSignatureProvider(ApiUtil.findClientByClientId(adminClient.realm("test"), "test-app"), "RS512");
            backchannelLogoutRequest("HS256", "RS512", "RS384");
        } finally {
            TokenSignatureUtil.changeRealmTokenSignatureProvider(adminClient, "RS256");
            TokenSignatureUtil.changeClientAccessTokenSignatureProvider(ApiUtil.findClientByClientId(adminClient.realm("test"), "test-app"), "RS256");
        }
    }

    @Test
    public void successfulKLogoutAfterEmptyBackChannelUrl() throws Exception {
        ClientsResource clients = adminClient.realm(oauth.getRealm()).clients();
        ClientRepresentation rep = clients.findByClientId(oauth.getClientId()).get(0);

        rep.getAttributes().put(OIDCConfigAttributes.BACKCHANNEL_LOGOUT_URL, "");

        clients.get(rep.getId()).update(rep);

        oauth.doLogin("test-user@localhost", "password");

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        oauth.clientSessionState("client-session");

        OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "password");
        String idTokenString = tokenResponse.getIdToken();
        String logoutUrl = oauth.getLogoutUrl()
                .idTokenHint(idTokenString)
                .postLogoutRedirectUri(oauth.APP_AUTH_ROOT)
                .build();

        try (CloseableHttpClient c = HttpClientBuilder.create().disableRedirectHandling().build();
                CloseableHttpResponse response = c.execute(new HttpGet(logoutUrl))) {
            assertThat(response, Matchers.statusCodeIsHC(Status.FOUND));
            assertThat(response.getFirstHeader(HttpHeaders.LOCATION).getValue(), is(oauth.APP_AUTH_ROOT));
        }

        assertNotNull(testingClient.testApp().getAdminLogoutAction());
    }

    @Test
    public void backChannelPreferenceOverKLogout() throws Exception {
        ClientsResource clients = adminClient.realm(oauth.getRealm()).clients();
        ClientRepresentation rep = clients.findByClientId(oauth.getClientId()).get(0);

        rep.getAttributes().put(OIDCConfigAttributes.BACKCHANNEL_LOGOUT_URL, oauth.APP_ROOT + "/admin/backchannelLogout");

        ClientResource clientResource = clients.get(rep.getId());
        clientResource.update(rep);

        try {
            oauth.doLogin("test-user@localhost", "password");

            String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

            oauth.clientSessionState("client-session");

            OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "password");
            String idTokenString = tokenResponse.getIdToken();
            String logoutUrl = oauth.getLogoutUrl()
                    .idTokenHint(idTokenString)
                    .postLogoutRedirectUri(oauth.APP_AUTH_ROOT)
                    .build();

            try (CloseableHttpClient c = HttpClientBuilder.create().disableRedirectHandling().build();
                    CloseableHttpResponse response = c.execute(new HttpGet(logoutUrl))) {
                assertThat(response, Matchers.statusCodeIsHC(Status.FOUND));
                assertThat(response.getFirstHeader(HttpHeaders.LOCATION).getValue(), is(oauth.APP_AUTH_ROOT));
            }

            assertNotNull(testingClient.testApp().getBackChannelLogoutToken());
        } finally {
            rep.getAttributes().put(OIDCConfigAttributes.BACKCHANNEL_LOGOUT_URL, "");
            clientResource.update(rep);
        }
    }

    private OAuthClient.AccessTokenResponse loginAndForceNewLoginPage() {
        oauth.doLogin("test-user@localhost", "password");

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        oauth.clientSessionState("client-session");

        OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "password");

        setTimeOffset(1);

        String loginFormUri = UriBuilder.fromUri(oauth.getLoginFormUrl())
                .queryParam(OIDCLoginProtocol.PROMPT_PARAM, OIDCLoginProtocol.PROMPT_VALUE_LOGIN)
                .build().toString();
        driver.navigate().to(loginFormUri);

        loginPage.assertCurrent();

        return tokenResponse;
    }
}

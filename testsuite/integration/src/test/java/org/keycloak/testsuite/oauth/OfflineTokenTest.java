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

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.UriBuilder;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.OAuth2Constants;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.common.constants.ServiceAccountConstants;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.services.managers.ClientManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.OAuthClient;
import org.keycloak.testsuite.pages.AccountApplicationsPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.OAuthGrantPage;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.TokenUtil;
import org.keycloak.common.util.Time;
import org.keycloak.common.util.UriUtils;
import org.openqa.selenium.WebDriver;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class OfflineTokenTest {

    @ClassRule
    public static KeycloakRule keycloakRule = new KeycloakRule(new KeycloakRule.KeycloakSetup() {

        @Override
        public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
            // For testing
            appRealm.setAccessTokenLifespan(10);
            appRealm.setSsoSessionIdleTimeout(30);

            ClientModel app = KeycloakModelUtils.createClient(appRealm, "offline-client");
            app.setDirectAccessGrantsEnabled(true);
            app.setSecret("secret1");
            String testAppRedirectUri = appRealm.getClientByClientId("test-app").getRedirectUris().iterator().next();
            offlineClientAppUri = UriUtils.getOrigin(testAppRedirectUri) + "/offline-client";
            app.setRedirectUris(new HashSet<>(Arrays.asList(offlineClientAppUri)));
            app.setManagementUrl(offlineClientAppUri);

            new ClientManager(manager).enableServiceAccount(app);
            UserModel serviceAccountUser = manager.getSession().users().getUserByUsername(ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX + "offline-client", appRealm);
            RoleModel customerUserRole = appRealm.getClientByClientId("test-app").getRole("customer-user");
            serviceAccountUser.grantRole(customerUserRole);

            userId = manager.getSession().users().getUserByUsername("test-user@localhost", appRealm).getId();

            URL url = getClass().getResource("/oidc/offline-client-keycloak.json");
            keycloakRule.createApplicationDeployment()
                    .name("offline-client").contextPath("/offline-client")
                    .servletClass(OfflineTokenServlet.class).adapterConfigPath(url.getPath())
                    .role("user").deployApplication();
        }

    });

    private static String userId;
    private static String offlineClientAppUri;

    @Rule
    public WebRule webRule = new WebRule(this);

    @WebResource
    protected WebDriver driver;

    @WebResource
    protected OAuthClient oauth;

    @WebResource
    protected LoginPage loginPage;

    @WebResource
    protected OAuthGrantPage oauthGrantPage;

    @WebResource
    protected AccountApplicationsPage accountAppPage;

    @Rule
    public AssertEvents events = new AssertEvents(keycloakRule);


//    @Test
//    public void testSleep() throws Exception {
//        Thread.sleep(9999000);
//    }

    @Test
    public void offlineTokenDisabledForClient() throws Exception {
        keycloakRule.update(new KeycloakRule.KeycloakSetup() {

            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                appRealm.getClientByClientId("offline-client").setFullScopeAllowed(false);
            }

        });

        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        oauth.clientId("offline-client");
        oauth.redirectUri(offlineClientAppUri);
        oauth.doLogin("test-user@localhost", "password");

        Event loginEvent = events.expectLogin()
                .client("offline-client")
                .detail(Details.REDIRECT_URI, offlineClientAppUri)
                .assertEvent();

        String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "secret1");

        assertEquals(400, tokenResponse.getStatusCode());
        assertEquals("not_allowed", tokenResponse.getError());

        events.expectCodeToToken(codeId, sessionId)
                .client("offline-client")
                .error("not_allowed")
                .clearDetails()
                .assertEvent();

        keycloakRule.update(new KeycloakRule.KeycloakSetup() {

            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                appRealm.getClientByClientId("offline-client").setFullScopeAllowed(true);
            }

        });
    }

    @Test
    public void offlineTokenUserNotAllowed() throws Exception {
        String userId = keycloakRule.getUser("test", "keycloak-user@localhost").getId();

        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        oauth.clientId("offline-client");
        oauth.redirectUri(offlineClientAppUri);
        oauth.doLogin("keycloak-user@localhost", "password");

        Event loginEvent = events.expectLogin()
                .client("offline-client")
                .user(userId)
                .detail(Details.REDIRECT_URI, offlineClientAppUri)
                .assertEvent();

        String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "secret1");

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
    public void offlineTokenBrowserFlow() throws Exception {
        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        oauth.clientId("offline-client");
        oauth.redirectUri(offlineClientAppUri);
        oauth.doLogin("test-user@localhost", "password");

        Event loginEvent = events.expectLogin()
                .client("offline-client")
                .detail(Details.REDIRECT_URI, offlineClientAppUri)
                .assertEvent();

        final String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "secret1");
        AccessToken token = oauth.verifyToken(tokenResponse.getAccessToken());
        String offlineTokenString = tokenResponse.getRefreshToken();
        RefreshToken offlineToken = oauth.verifyRefreshToken(offlineTokenString);

        events.expectCodeToToken(codeId, sessionId)
                .client("offline-client")
                .detail(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_OFFLINE)
                .assertEvent();

        Assert.assertEquals(TokenUtil.TOKEN_TYPE_OFFLINE, offlineToken.getType());
        Assert.assertEquals(0, offlineToken.getExpiration());

        String newRefreshTokenString = testRefreshWithOfflineToken(token, offlineToken, offlineTokenString, sessionId, userId);

        // Change offset to very big value to ensure offline session expires
        Time.setOffset(3000000);

        OAuthClient.AccessTokenResponse response = oauth.doRefreshTokenRequest(newRefreshTokenString, "secret1");
        Assert.assertEquals(400, response.getStatusCode());
        assertEquals("invalid_grant", response.getError());

        events.expectRefresh(offlineToken.getId(), sessionId)
                .client("offline-client")
                .error(Errors.INVALID_TOKEN)
                .user(userId)
                .clearDetails()
                .assertEvent();


        Time.setOffset(0);
    }

    private String testRefreshWithOfflineToken(AccessToken oldToken, RefreshToken offlineToken, String offlineTokenString,
                                             final String sessionId, String userId) {
        // Change offset to big value to ensure userSession expired
        Time.setOffset(99999);
        Assert.assertFalse(oldToken.isActive());
        Assert.assertTrue(offlineToken.isActive());

        // Assert userSession expired
        keycloakRule.update(new KeycloakRule.KeycloakSetup() {

            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                manager.getSession().sessions().removeExpired(appRealm);
            }

        });
        keycloakRule.update(new KeycloakRule.KeycloakSetup() {

            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                Assert.assertNull(manager.getSession().sessions().getUserSession(appRealm, sessionId));
            }

        });


        OAuthClient.AccessTokenResponse response = oauth.doRefreshTokenRequest(offlineTokenString, "secret1");
        AccessToken refreshedToken = oauth.verifyToken(response.getAccessToken());
        Assert.assertEquals(200, response.getStatusCode());
        Assert.assertEquals(sessionId, refreshedToken.getSessionState());

        // Assert new refreshToken in the response
        String newRefreshToken = response.getRefreshToken();
        Assert.assertNotNull(newRefreshToken);
        Assert.assertNotEquals(oldToken.getId(), refreshedToken.getId());

        Assert.assertEquals(userId, refreshedToken.getSubject());

        Assert.assertTrue(refreshedToken.getRealmAccess().isUserInRole("user"));
        Assert.assertTrue(refreshedToken.getRealmAccess().isUserInRole(Constants.OFFLINE_ACCESS_ROLE));

        Assert.assertEquals(1, refreshedToken.getResourceAccess("test-app").getRoles().size());
        Assert.assertTrue(refreshedToken.getResourceAccess("test-app").isUserInRole("customer-user"));

        Event refreshEvent = events.expectRefresh(offlineToken.getId(), sessionId)
                .client("offline-client")
                .user(userId)
                .removeDetail(Details.UPDATED_REFRESH_TOKEN_ID)
                .detail(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_OFFLINE)
                .assertEvent();
        Assert.assertNotEquals(oldToken.getId(), refreshEvent.getDetails().get(Details.TOKEN_ID));

        Time.setOffset(0);
        return newRefreshToken;
    }

    @Test
    public void offlineTokenDirectGrantFlow() throws Exception {
        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        oauth.clientId("offline-client");
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doGrantAccessTokenRequest("secret1", "test-user@localhost", "password");

        AccessToken token = oauth.verifyToken(tokenResponse.getAccessToken());
        String offlineTokenString = tokenResponse.getRefreshToken();
        RefreshToken offlineToken = oauth.verifyRefreshToken(offlineTokenString);

        events.expectLogin()
                .client("offline-client")
                .user(userId)
                .session(token.getSessionState())
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
        Assert.assertEquals(0, offlineToken.getExpiration());

        testRefreshWithOfflineToken(token, offlineToken, offlineTokenString, token.getSessionState(), userId);

        // Assert same token can be refreshed again
        testRefreshWithOfflineToken(token, offlineToken, offlineTokenString, token.getSessionState(), userId);
    }

    @Test
    public void offlineTokenDirectGrantFlowWithRefreshTokensRevoked() throws Exception {
        keycloakRule.configure(new KeycloakRule.KeycloakSetup() {

            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                appRealm.setRevokeRefreshToken(true);
            }

        });

        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        oauth.clientId("offline-client");
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doGrantAccessTokenRequest("secret1", "test-user@localhost", "password");

        AccessToken token = oauth.verifyToken(tokenResponse.getAccessToken());
        String offlineTokenString = tokenResponse.getRefreshToken();
        RefreshToken offlineToken = oauth.verifyRefreshToken(offlineTokenString);

        events.expectLogin()
                .client("offline-client")
                .user(userId)
                .session(token.getSessionState())
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
        Assert.assertEquals(0, offlineToken.getExpiration());

        String offlineTokenString2 = testRefreshWithOfflineToken(token, offlineToken, offlineTokenString, token.getSessionState(), userId);
        RefreshToken offlineToken2 = oauth.verifyRefreshToken(offlineTokenString2);

        // Assert second refresh with same refresh token will fail
        OAuthClient.AccessTokenResponse response = oauth.doRefreshTokenRequest(offlineTokenString, "secret1");
        Assert.assertEquals(400, response.getStatusCode());
        events.expectRefresh(offlineToken.getId(), token.getSessionState())
                .client("offline-client")
                .error(Errors.INVALID_TOKEN)
                .user(userId)
                .clearDetails()
                .assertEvent();

        // Refresh with new refreshToken is successful now
        testRefreshWithOfflineToken(token, offlineToken2, offlineTokenString2, token.getSessionState(), userId);

        keycloakRule.configure(new KeycloakRule.KeycloakSetup() {

            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                appRealm.setRevokeRefreshToken(false);
            }

        });
    }

    @Test
    public void offlineTokenServiceAccountFlow() throws Exception {
        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        oauth.clientId("offline-client");
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doClientCredentialsGrantAccessTokenRequest("secret1");

        AccessToken token = oauth.verifyToken(tokenResponse.getAccessToken());
        String offlineTokenString = tokenResponse.getRefreshToken();
        RefreshToken offlineToken = oauth.verifyRefreshToken(offlineTokenString);

        String serviceAccountUserId = keycloakRule.getUser("test",  ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX + "offline-client").getId();

        events.expectClientLogin()
                .client("offline-client")
                .user(serviceAccountUserId)
                .session(token.getSessionState())
                .detail(Details.TOKEN_ID, token.getId())
                .detail(Details.REFRESH_TOKEN_ID, offlineToken.getId())
                .detail(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_OFFLINE)
                .detail(Details.USERNAME, ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX + "offline-client")
                .assertEvent();

        Assert.assertEquals(TokenUtil.TOKEN_TYPE_OFFLINE, offlineToken.getType());
        Assert.assertEquals(0, offlineToken.getExpiration());

        testRefreshWithOfflineToken(token, offlineToken, offlineTokenString, token.getSessionState(), serviceAccountUserId);


        // Now retrieve another offline token and verify that previous offline token is still valid
        tokenResponse = oauth.doClientCredentialsGrantAccessTokenRequest("secret1");

        AccessToken token2 = oauth.verifyToken(tokenResponse.getAccessToken());
        String offlineTokenString2 = tokenResponse.getRefreshToken();
        RefreshToken offlineToken2 = oauth.verifyRefreshToken(offlineTokenString2);

        events.expectClientLogin()
                .client("offline-client")
                .user(serviceAccountUserId)
                .session(token2.getSessionState())
                .detail(Details.TOKEN_ID, token2.getId())
                .detail(Details.REFRESH_TOKEN_ID, offlineToken2.getId())
                .detail(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_OFFLINE)
                .detail(Details.USERNAME, ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX + "offline-client")
                .assertEvent();

        // Refresh with both offline tokens is fine
        testRefreshWithOfflineToken(token, offlineToken, offlineTokenString, token.getSessionState(), serviceAccountUserId);
        testRefreshWithOfflineToken(token2, offlineToken2, offlineTokenString2, token2.getSessionState(), serviceAccountUserId);
    }

    @Test
    public void offlineTokenAllowedWithCompositeRole() throws Exception {
        keycloakRule.update(new KeycloakRule.KeycloakSetup() {

            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                ClientModel offlineClient = appRealm.getClientByClientId("offline-client");
                UserModel testUser = session.users().getUserByUsername("test-user@localhost", appRealm);
                RoleModel offlineAccess = appRealm.getRole(Constants.OFFLINE_ACCESS_ROLE);

                // Test access
                Assert.assertFalse(TokenManager.getAccess(null, true, offlineClient, testUser).contains(offlineAccess));
                Assert.assertTrue(TokenManager.getAccess(OAuth2Constants.OFFLINE_ACCESS, true, offlineClient, testUser).contains(offlineAccess));

                // Grant offline_access role indirectly through composite role
                RoleModel composite = appRealm.addRole("composite");
                composite.addCompositeRole(offlineAccess);

                testUser.deleteRoleMapping(offlineAccess);
                testUser.grantRole(composite);

                // Test access
                Assert.assertFalse(TokenManager.getAccess(null, true, offlineClient, testUser).contains(offlineAccess));
                Assert.assertTrue(TokenManager.getAccess(OAuth2Constants.OFFLINE_ACCESS, true, offlineClient, testUser).contains(offlineAccess));
            }

        });

        // Integration test
        offlineTokenDirectGrantFlow();

        // Revert changes
        keycloakRule.update(new KeycloakRule.KeycloakSetup() {

            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                RoleModel composite = appRealm.getRole("composite");
                RoleModel offlineAccess = appRealm.getRole(Constants.OFFLINE_ACCESS_ROLE);
                UserModel testUser = session.users().getUserByUsername("test-user@localhost", appRealm);

                testUser.deleteRoleMapping(composite);
                appRealm.removeRole(composite);
                testUser.grantRole(offlineAccess);
            }

        });
    }

    @Test
    public void testServlet() {
        OfflineTokenServlet.tokenInfo = null;

        String servletUri = UriBuilder.fromUri(offlineClientAppUri)
                .queryParam(OAuth2Constants.SCOPE, OAuth2Constants.OFFLINE_ACCESS)
                .build().toString();
        driver.navigate().to(servletUri);
        loginPage.login("test-user@localhost", "password");
        Assert.assertTrue(driver.getCurrentUrl().startsWith(offlineClientAppUri));

        Assert.assertEquals(OfflineTokenServlet.tokenInfo.refreshToken.getType(), TokenUtil.TOKEN_TYPE_OFFLINE);
        Assert.assertEquals(OfflineTokenServlet.tokenInfo.refreshToken.getExpiration(), 0);

        String accessTokenId = OfflineTokenServlet.tokenInfo.accessToken.getId();
        String refreshTokenId = OfflineTokenServlet.tokenInfo.refreshToken.getId();

        // Assert access token and offline token are refreshed
        Time.setOffset(9999);
        driver.navigate().to(offlineClientAppUri);
        Assert.assertTrue(driver.getCurrentUrl().startsWith(offlineClientAppUri));
        Assert.assertNotEquals(OfflineTokenServlet.tokenInfo.refreshToken.getId(), refreshTokenId);
        Assert.assertNotEquals(OfflineTokenServlet.tokenInfo.accessToken.getId(), accessTokenId);

        // Ensure that logout works for webapp (even if offline token will be still valid in Keycloak DB)
        driver.navigate().to(offlineClientAppUri + "/logout");
        loginPage.assertCurrent();
        driver.navigate().to(offlineClientAppUri);
        loginPage.assertCurrent();

        Time.setOffset(0);
        events.clear();
    }

    @Test
    public void testServletWithRevoke() {
        // Login to servlet first with offline token
        String servletUri = UriBuilder.fromUri(offlineClientAppUri)
                .queryParam(OAuth2Constants.SCOPE, OAuth2Constants.OFFLINE_ACCESS)
                .build().toString();
        driver.navigate().to(servletUri);
        loginPage.login("test-user@localhost", "password");
        Assert.assertTrue(driver.getCurrentUrl().startsWith(offlineClientAppUri));

        Assert.assertEquals(OfflineTokenServlet.tokenInfo.refreshToken.getType(), TokenUtil.TOKEN_TYPE_OFFLINE);

        // Assert refresh works with increased time
        Time.setOffset(9999);
        driver.navigate().to(offlineClientAppUri);
        Assert.assertTrue(driver.getCurrentUrl().startsWith(offlineClientAppUri));
        Time.setOffset(0);

        events.clear();

        // Go to account service and revoke grant
        accountAppPage.open();
        List<String> additionalGrants = accountAppPage.getApplications().get("offline-client").getAdditionalGrants();
        Assert.assertEquals(additionalGrants.size(), 1);
        Assert.assertEquals(additionalGrants.get(0), "Offline Token");
        accountAppPage.revokeGrant("offline-client");
        Assert.assertEquals(accountAppPage.getApplications().get("offline-client").getAdditionalGrants().size(), 0);

        events.expect(EventType.REVOKE_GRANT)
                .client("account").detail(Details.REVOKED_CLIENT, "offline-client").assertEvent();

        // Assert refresh doesn't work now (increase time one more time)
        Time.setOffset(9999);
        driver.navigate().to(offlineClientAppUri);
        Assert.assertFalse(driver.getCurrentUrl().startsWith(offlineClientAppUri));
        loginPage.assertCurrent();
        Time.setOffset(0);
    }

    @Test
    public void testServletWithConsent() {
        keycloakRule.update(new KeycloakRule.KeycloakSetup() {

            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                appRealm.getClientByClientId("offline-client").setConsentRequired(true);
            }

        });

        // Assert grant page doesn't have 'Offline Access' role when offline token is not requested
        driver.navigate().to(offlineClientAppUri);
        loginPage.login("test-user@localhost", "password");
        oauthGrantPage.assertCurrent();
        Assert.assertFalse(driver.getPageSource().contains("Offline access"));
        oauthGrantPage.cancel();

        // Assert grant page has 'Offline Access' role now
        String servletUri = UriBuilder.fromUri(offlineClientAppUri)
                .queryParam(OAuth2Constants.SCOPE, OAuth2Constants.OFFLINE_ACCESS)
                .build().toString();
        driver.navigate().to(servletUri);
        loginPage.login("test-user@localhost", "password");
        oauthGrantPage.assertCurrent();
        Assert.assertTrue(driver.getPageSource().contains("Offline access"));
        oauthGrantPage.accept();

        Assert.assertTrue(driver.getCurrentUrl().startsWith(offlineClientAppUri));
        Assert.assertEquals(OfflineTokenServlet.tokenInfo.refreshToken.getType(), TokenUtil.TOKEN_TYPE_OFFLINE);

        accountAppPage.open();
        AccountApplicationsPage.AppEntry offlineClient = accountAppPage.getApplications().get("offline-client");
        Assert.assertTrue(offlineClient.getRolesGranted().contains("Offline access"));
        Assert.assertTrue(offlineClient.getAdditionalGrants().contains("Offline Token"));

        events.clear();

        // Revert change
        keycloakRule.update(new KeycloakRule.KeycloakSetup() {

            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                appRealm.getClientByClientId("offline-client").setConsentRequired(false);
            }

        });
    }

    public static class OfflineTokenServlet extends HttpServlet {

        private static TokenInfo tokenInfo;

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            if (req.getRequestURI().endsWith("logout")) {

                UriBuilder redirectUriBuilder = UriBuilder.fromUri(offlineClientAppUri);
                if (req.getParameter(OAuth2Constants.SCOPE) != null) {
                    redirectUriBuilder.queryParam(OAuth2Constants.SCOPE, req.getParameter(OAuth2Constants.SCOPE));
                }
                String redirectUri = redirectUriBuilder.build().toString();

                String origin = UriUtils.getOrigin(req.getRequestURL().toString());
                String serverLogoutRedirect = UriBuilder.fromUri(origin + "/auth/realms/test/protocol/openid-connect/logout")
                        .queryParam("redirect_uri", redirectUri)
                        .build().toString();

                resp.sendRedirect(serverLogoutRedirect);
                return;
            }

            StringBuilder response = new StringBuilder("<html><head><title>Offline token servlet</title></head><body><pre>");
            RefreshableKeycloakSecurityContext ctx = (RefreshableKeycloakSecurityContext) req.getAttribute(KeycloakSecurityContext.class.getName());
            String accessTokenPretty = JsonSerialization.writeValueAsPrettyString(ctx.getToken());
            RefreshToken refreshToken = null;
            try {
                refreshToken = new JWSInput(ctx.getRefreshToken()).readJsonContent(RefreshToken.class);
            } catch (JWSInputException e) {
                throw new IOException(e);
            }
            String refreshTokenPretty = JsonSerialization.writeValueAsPrettyString(refreshToken);

            response = response.append(accessTokenPretty)
                    .append(refreshTokenPretty)
                    .append("</pre></body></html>");
            resp.getWriter().println(response.toString());

            tokenInfo = new TokenInfo(ctx.getToken(), refreshToken);
        }

    }

    private static class TokenInfo {

        private final AccessToken accessToken;
        private final RefreshToken refreshToken;

        public TokenInfo(AccessToken accessToken, RefreshToken refreshToken) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }
    }
}

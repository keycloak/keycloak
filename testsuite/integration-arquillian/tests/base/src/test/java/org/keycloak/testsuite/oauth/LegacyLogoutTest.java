/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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
 *
 */

package org.keycloak.testsuite.oauth;

import java.io.Closeable;
import java.util.Collections;

import javax.ws.rs.NotFoundException;

import org.hamcrest.MatcherAssert;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.auth.page.account.AccountManagement;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.InfoPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LogoutConfirmPage;
import org.keycloak.testsuite.pages.OAuthGrantPage;
import org.keycloak.testsuite.updaters.ClientAttributeUpdater;
import org.keycloak.testsuite.util.InfinispanTestTimeServiceRule;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.ServerURLs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlDoesntStartWith;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlEquals;

/**
 * Test logout endpoint with deprecated "redirect_uri" parameter
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LegacyLogoutTest extends AbstractTestRealmKeycloakTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Rule
    public InfinispanTestTimeServiceRule ispnTestTimeService = new InfinispanTestTimeServiceRule(this);

    @Page
    protected AppPage appPage;

    @Page
    protected LoginPage loginPage;

    @Page
    protected OAuthGrantPage grantPage;

    @Page
    protected LogoutConfirmPage logoutConfirmPage;

    @Page
    protected InfoPage infoPage;

    @Page
    protected AccountManagement accountManagementPage;

    @Page
    private ErrorPage errorPage;

    private String APP_REDIRECT_URI;

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }

    @Before
    public void configLegacyRedirectUriEnabled() {
        getTestingClient().testing().setSystemPropertyOnServer("oidc." + OIDCLoginProtocolFactory.CONFIG_LEGACY_LOGOUT_REDIRECT_URI, "true");
        getTestingClient().testing().reinitializeProviderFactoryWithSystemPropertiesScope(LoginProtocol.class.getName(), OIDCLoginProtocol.LOGIN_PROTOCOL, "oidc.");

        APP_REDIRECT_URI = oauth.APP_AUTH_ROOT;
    }

    @After
    public void revertConfiguration() {
        getTestingClient().testing().setSystemPropertyOnServer("oidc." + OIDCLoginProtocolFactory.CONFIG_LEGACY_LOGOUT_REDIRECT_URI, "false");
        getTestingClient().testing().reinitializeProviderFactoryWithSystemPropertiesScope(LoginProtocol.class.getName(), OIDCLoginProtocol.LOGIN_PROTOCOL, "oidc.");
    }


    // Test logout with deprecated "redirect_uri" and with "id_token_hint" . Should od automatic redirect
    @Test
    public void logoutWithLegacyRedirectUriAndIdTokenHint() throws Exception {
        OAuthClient.AccessTokenResponse tokenResponse = loginUser();
        String idTokenString = tokenResponse.getIdToken();
        String sessionId = tokenResponse.getSessionState();

        String logoutUrl = oauth.getLogoutUrl().redirectUri(APP_REDIRECT_URI).idTokenHint(idTokenString).build();
        driver.navigate().to(logoutUrl);

        events.expectLogout(sessionId).detail(Details.REDIRECT_URI, APP_REDIRECT_URI).assertEvent();
        Assert.assertThat(false, is(isSessionActive(sessionId)));
        assertCurrentUrlEquals(APP_REDIRECT_URI);
    }

    // Test logout with deprecated "redirect_uri" and without "id_token_hint" . User should confirm logout
    @Test
    public void logoutWithLegacyRedirectUriAndWithoutIdTokenHint() throws Exception {
        OAuthClient.AccessTokenResponse tokenResponse = loginUser();
        String sessionId = tokenResponse.getSessionState();

        String logoutUrl = oauth.getLogoutUrl().redirectUri(APP_REDIRECT_URI).build();
        driver.navigate().to(logoutUrl);

        // Assert logout confirmation page. Session still exists. Assert default language on logout page (English)
        logoutConfirmPage.assertCurrent();
        Assert.assertThat(true, is(isSessionActive(sessionId)));
        events.assertEmpty();
        logoutConfirmPage.confirmLogout();

        // Redirected back to the application with expected state
        events.expectLogout(sessionId).removeDetail(Details.REDIRECT_URI).assertEvent();
        Assert.assertThat(false, is(isSessionActive(sessionId)));
        assertCurrentUrlEquals(APP_REDIRECT_URI);
    }

    // Test with "post_logout_redirect_uri" without "id_token_hint":  User should confirm logout.
    @Test
    public void logoutWithPostLogoutUriWithoutIdTokenHint() {
        OAuthClient.AccessTokenResponse tokenResponse = loginUser();
        String sessionId = tokenResponse.getSessionState();

        String logoutUrl = oauth.getLogoutUrl().postLogoutRedirectUri(APP_REDIRECT_URI).build();
        driver.navigate().to(logoutUrl);

        // Assert logout confirmation page. Session still exists. Assert default language on logout page (English)
        logoutConfirmPage.assertCurrent();
        assertThat(true, is(isSessionActive(sessionId)));
        events.assertEmpty();
        logoutConfirmPage.confirmLogout();

        // Redirected back to the application with expected state
        events.expectLogout(sessionId).removeDetail(Details.REDIRECT_URI).assertEvent();
        assertThat(false, is(isSessionActive(sessionId)));
        assertCurrentUrlEquals(APP_REDIRECT_URI);
    }


    // KEYCLOAK-16517 Make sure that just real clients with standardFlow or implicitFlow enabled are considered for redirectUri
    @Test
    public void logoutRedirectWithStarRedirectUriForDirectGrantClient() {
        // Set "*" as redirectUri for some directGrant client
        ClientResource clientRes = ApiUtil.findClientByClientId(testRealm(), "direct-grant");
        ClientRepresentation clientRepOrig = clientRes.toRepresentation();
        ClientRepresentation clientRep = clientRes.toRepresentation();
        clientRep.setStandardFlowEnabled(false);
        clientRep.setImplicitFlowEnabled(false);
        clientRep.setRedirectUris(Collections.singletonList("*"));
        clientRes.update(clientRep);

        try {
            OAuthClient.AccessTokenResponse tokenResponse = loginUser();

            String invalidRedirectUri = ServerURLs.getAuthServerContextRoot() + "/bar";

            String idTokenString = tokenResponse.getIdToken();

            String logoutUrl = oauth.getLogoutUrl().redirectUri(invalidRedirectUri).build();
            driver.navigate().to(logoutUrl);

            events.expectLogoutError(Errors.INVALID_REDIRECT_URI).assertEvent();

            assertCurrentUrlDoesntStartWith(invalidRedirectUri);
            errorPage.assertCurrent();
            Assert.assertEquals("Invalid redirect uri", errorPage.getError());

            // Session still active
            Assert.assertThat(true, is(isSessionActive(tokenResponse.getSessionState())));
        } finally {
            // Revert
            clientRes.update(clientRepOrig);
        }
    }

    // Test logout with deprecated "redirect_uri" and without "id_token_hint" and client disabled after login
    @Test
    public void logoutWithLegacyRedirectUriAndWithoutIdTokenHintClientDisabled() throws Exception {
        OAuthClient.AccessTokenResponse tokenResponse = loginUser();
        String sessionId = tokenResponse.getSessionState();

        try (Closeable testAppClient = ClientAttributeUpdater.forClient(adminClient, "test", oauth.getClientId())
                .setEnabled(false).update()) {

            ClientsResource clients = adminClient.realm(oauth.getRealm()).clients();
            ClientRepresentation rep = clients.findByClientId(oauth.getClientId()).get(0);
            MatcherAssert.assertThat(false, is(rep.isEnabled()));

            String logoutUrl = oauth.getLogoutUrl().redirectUri(APP_REDIRECT_URI).build();
            driver.navigate().to(logoutUrl);

            // Assert logout confirmation page. Session still exists. Assert default language on logout page (English)
            logoutConfirmPage.assertCurrent();
            MatcherAssert.assertThat(true, is(isSessionActive(sessionId)));
            events.assertEmpty();
            logoutConfirmPage.confirmLogout();

            // Redirected back to the application with expected state
            events.expectLogout(sessionId).removeDetail(Details.REDIRECT_URI).assertEvent();
            MatcherAssert.assertThat(false, is(isSessionActive(sessionId)));
            assertCurrentUrlEquals(APP_REDIRECT_URI);
        }

    }

    private OAuthClient.AccessTokenResponse loginUser() {
        oauth.doLogin("test-user@localhost", "password");
        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        oauth.clientSessionState("client-session");
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "password");
        events.clear();
        return tokenResponse;
    }

    private boolean isSessionActive(String sessionId) {
        try {
            testingClient.testing().getClientSessionsCountInUserSession("test", sessionId);
            return true;
        } catch (NotFoundException nfe) {
            return false;
        }
    }
}

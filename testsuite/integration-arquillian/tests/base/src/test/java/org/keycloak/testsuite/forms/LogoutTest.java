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
package org.keycloak.testsuite.forms;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.common.Profile;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.common.util.Retry;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.DisableFeature;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.LoginPage;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlDoesntStartWith;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlEquals;

import org.keycloak.testsuite.auth.page.account.AccountManagement;
import org.keycloak.testsuite.updaters.ClientAttributeUpdater;
import org.keycloak.testsuite.updaters.RealmAttributeUpdater;
import org.keycloak.testsuite.util.ClientManager;
import org.keycloak.testsuite.util.InfinispanTestTimeServiceRule;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.ServerURLs;
import org.keycloak.testsuite.util.WaitUtils;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class LogoutTest extends AbstractTestRealmKeycloakTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Rule
    public InfinispanTestTimeServiceRule ispnTestTimeService = new InfinispanTestTimeServiceRule(this);

    @Page
    protected AppPage appPage;

    @Page
    protected LoginPage loginPage;

    @Page
    protected AccountManagement accountManagementPage;

    @Page
    private ErrorPage errorPage;

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }

    @Before
    public void clientConfiguration() {
        ClientManager.realm(adminClient.realm("test")).clientId("test-app").directAccessGrant(true);
    }

    @Test
    public void logoutRedirect() {
        loginPage.open();
        loginPage.login("test-user@localhost", "password");
        assertTrue(appPage.isCurrent());

        String sessionId = events.expectLogin().assertEvent().getSessionId();

        String redirectUri = oauth.APP_AUTH_ROOT + "?logout";

        String logoutUrl = oauth.getLogoutUrl().redirectUri(redirectUri).build();
        driver.navigate().to(logoutUrl);

        events.expectLogout(sessionId).detail(Details.REDIRECT_URI, redirectUri).assertEvent();

        assertCurrentUrlEquals(redirectUri);

        loginPage.open();
        loginPage.login("test-user@localhost", "password");
        assertTrue(appPage.isCurrent());

        String sessionId2 = events.expectLogin().assertEvent().getSessionId();
        assertNotEquals(sessionId, sessionId2);

        driver.navigate().to(logoutUrl);
        events.expectLogout(sessionId2).detail(Details.REDIRECT_URI, redirectUri).assertEvent();
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
            loginPage.open();
            loginPage.login("test-user@localhost", "password");
            assertTrue(appPage.isCurrent());

            events.expectLogin().assertEvent();

            String invalidRedirectUri = ServerURLs.getAuthServerContextRoot() + "/bar";

            String logoutUrl = oauth.getLogoutUrl().redirectUri(invalidRedirectUri).build();
            driver.navigate().to(logoutUrl);

            events.expectLogoutError(Errors.INVALID_REDIRECT_URI).assertEvent();

            assertCurrentUrlDoesntStartWith(invalidRedirectUri);
            errorPage.assertCurrent();
            Assert.assertEquals("Invalid redirect uri", errorPage.getError());
        } finally {
            // Revert
            clientRes.update(clientRepOrig);
        }
    }

    @Test
    public void logoutSession() {
        loginPage.open();
        loginPage.login("test-user@localhost", "password");
        assertTrue(appPage.isCurrent());

        String sessionId = events.expectLogin().assertEvent().getSessionId();

        String logoutUrl = oauth.getLogoutUrl().sessionState(sessionId).build();
        driver.navigate().to(logoutUrl);

        events.expectLogout(sessionId).removeDetail(Details.REDIRECT_URI).assertEvent();

        assertCurrentUrlEquals(logoutUrl);

        loginPage.open();
        loginPage.login("test-user@localhost", "password");
        assertTrue(appPage.isCurrent());

        String sessionId2 = events.expectLogin().assertEvent().getSessionId();
        assertNotEquals(sessionId, sessionId2);
    }

    @Test
    public void logoutWithExpiredSession() throws Exception {
        try (AutoCloseable c = new RealmAttributeUpdater(adminClient.realm("test"))
                .updateWith(r -> r.setSsoSessionMaxLifespan(2))
                .update()) {

            oauth.doLogin("test-user@localhost", "password");

            String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

            oauth.clientSessionState("client-session");
            OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "password");
            String idTokenString = tokenResponse.getIdToken();

            // expire online user session
            setTimeOffset(9999);

            String logoutUrl = oauth.getLogoutUrl().redirectUri(oauth.APP_AUTH_ROOT).idTokenHint(idTokenString).build();
            driver.navigate().to(logoutUrl);

            // should not throw an internal server error
            appPage.assertCurrent();

            // check if the back channel logout succeeded
            driver.navigate().to(oauth.getLoginFormUrl());
            WaitUtils.waitForPageToLoad();
            loginPage.assertCurrent();
        }
    }

    @Test
    public void logoutMultipleSessions() throws IOException {
        // Login session 1
        loginPage.open();
        loginPage.login("test-user@localhost", "password");
        assertTrue(appPage.isCurrent());

        String sessionId = events.expectLogin().assertEvent().getSessionId();

        // Check session 1 logged-in
        oauth.openLoginForm();
        events.expectLogin().session(sessionId).removeDetail(Details.USERNAME).assertEvent();

         //  Logout session 1 by redirect
        driver.navigate().to(oauth.getLogoutUrl().redirectUri(oauth.APP_AUTH_ROOT).build());
        events.expectLogout(sessionId).detail(Details.REDIRECT_URI, oauth.APP_AUTH_ROOT).assertEvent();

         // Check session 1 not logged-in
        oauth.openLoginForm();
        loginPage.assertCurrent();

        // Login session 3
        oauth.doLogin("test-user@localhost", "password");
        String sessionId3 = events.expectLogin().assertEvent().getSessionId();
        assertNotEquals(sessionId, sessionId3);

        // Check session 3 logged-in
        oauth.openLoginForm();
        events.expectLogin().session(sessionId3).removeDetail(Details.USERNAME).assertEvent();

        //  Logout session 3 by redirect
        driver.navigate().to(oauth.getLogoutUrl().redirectUri(oauth.APP_AUTH_ROOT).build());
        events.expectLogout(sessionId3).detail(Details.REDIRECT_URI, oauth.APP_AUTH_ROOT).assertEvent();
    }

    //KEYCLOAK-2741
    @Test
    @DisableFeature(value = Profile.Feature.ACCOUNT2, skipRestart = true) // TODO remove this (KEYCLOAK-16228)
    public void logoutWithRememberMe() {
        setRememberMe(true);
        
        try {
            loginPage.open();
            assertFalse(loginPage.isRememberMeChecked());
            loginPage.setRememberMe(true);
            assertTrue(loginPage.isRememberMeChecked());
            loginPage.login("test-user@localhost", "password");

            String sessionId = events.expectLogin().assertEvent().getSessionId();

            // Expire session
            testingClient.testing().removeUserSession("test", sessionId);

            // Assert rememberMe checked and username/email prefilled
            loginPage.open();
            assertTrue(loginPage.isRememberMeChecked());
            assertEquals("test-user@localhost", loginPage.getUsername());

            loginPage.login("test-user@localhost", "password");
            
            //log out
            appPage.openAccount();
            accountManagementPage.signOut();
            // Assert rememberMe not checked nor username/email prefilled
            assertTrue(loginPage.isCurrent());
            assertFalse(loginPage.isRememberMeChecked());
            assertNotEquals("test-user@localhost", loginPage.getUsername());
        } finally {
            setRememberMe(false);
        }
    }
    
    private void setRememberMe(boolean enabled) {
        RealmRepresentation rep = adminClient.realm("test").toRepresentation();
        rep.setRememberMe(enabled);
        adminClient.realm("test").update(rep);
    }

    @Test
    public void logoutSessionWhenLoggedOutByAdmin() {
        loginPage.open();
        loginPage.login("test-user@localhost", "password");
        assertTrue(appPage.isCurrent());

        String sessionId = events.expectLogin().assertEvent().getSessionId();

        adminClient.realm("test").logoutAll();

        String logoutUrl = oauth.getLogoutUrl().sessionState(sessionId).build();
        driver.navigate().to(logoutUrl);

        assertCurrentUrlEquals(logoutUrl);

        loginPage.open();
        loginPage.login("test-user@localhost", "password");
        assertTrue(appPage.isCurrent());

        String sessionId2 = events.expectLogin().assertEvent().getSessionId();
        assertNotEquals(sessionId, sessionId2);

        driver.navigate().to(logoutUrl);
        events.expectLogout(sessionId2).removeDetail(Details.REDIRECT_URI).assertEvent();
    }

    @Test
    public void logoutUserByAdmin() {
        loginPage.open();
        loginPage.login("test-user@localhost", "password");
        assertTrue(appPage.isCurrent());
        String sessionId = events.expectLogin().assertEvent().getSessionId();

        UserRepresentation user = ApiUtil.findUserByUsername(adminClient.realm("test"), "test-user@localhost");
        Assert.assertEquals((Object) 0, user.getNotBefore());

        adminClient.realm("test").users().get(user.getId()).logout();

        Retry.execute(() -> {
            UserRepresentation u = adminClient.realm("test").users().get(user.getId()).toRepresentation();
            Assert.assertTrue(u.getNotBefore() > 0);

            loginPage.open();
            loginPage.assertCurrent();
        }, 10, 200);
    }


    // KEYCLOAK-5982
    @Test
    public void testLogoutWhenAccountClientRenamed() throws IOException {
        // Temporarily rename client "account" . Revert it back after the test
        try (Closeable accountClientUpdater = ClientAttributeUpdater.forClient(adminClient, "test", Constants.ACCOUNT_MANAGEMENT_CLIENT_ID)
                .setClientId("account-changed")
                .update()) {

            // Assert logout works
            logoutRedirect();
        }
    }

}

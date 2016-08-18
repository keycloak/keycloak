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

package org.keycloak.testsuite.oidc;

import java.util.List;


import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuthErrorException;
import org.keycloak.common.util.Time;
import org.keycloak.events.Details;
import org.keycloak.models.Constants;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.TestRealmKeycloakTest;
import org.keycloak.testsuite.admin.AbstractAdminTest;
import org.keycloak.testsuite.pages.AccountUpdateProfilePage;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.OAuthGrantPage;
import org.keycloak.testsuite.util.ClientManager;
import org.keycloak.testsuite.util.OAuthClient;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test for supporting advanced parameters of OIDC specs (max_age, prompt, ...)
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class OIDCAdvancedRequestParamsTest extends TestRealmKeycloakTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected AppPage appPage;

    @Page
    protected LoginPage loginPage;

    @Page
    protected AccountUpdateProfilePage profilePage;

    @Page
    protected OAuthGrantPage grantPage;


    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }

    @Before
    public void clientConfiguration() {
        ClientManager.realm(adminClient.realm("test")).clientId("test-app").directAccessGrant(true);
        /*
         * Configure the default client ID. Seems like OAuthClient is keeping the state of clientID
         * For example: If some test case configure oauth.clientId("sample-public-client"), other tests
         * will faile and the clientID will always be "sample-public-client
         * @see AccessTokenTest#testAuthorizationNegotiateHeaderIgnored()
         */
        oauth.clientId("test-app");
        oauth.maxAge(null);
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realm = AbstractAdminTest.loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);
        testRealms.add(realm);
    }


    // Max_age

    @Test
    public void testMaxAge1() {
        // Open login form and login successfully
        oauth.doLogin("test-user@localhost", "password");
        EventRepresentation loginEvent = events.expectLogin().assertEvent();

        IDToken idToken = sendTokenRequestAndGetIDToken(loginEvent);

        // Check that authTime is available and set to current time
        int authTime = idToken.getAuthTime();
        int currentTime = Time.currentTime();
        Assert.assertTrue(authTime <= currentTime && authTime + 3 >= currentTime);

        // Set time offset
        setTimeOffset(10);

        // Now open login form with maxAge=1
        oauth.maxAge("1");

        // Assert I need to login again through the login form
        oauth.doLogin("test-user@localhost", "password");
        loginEvent = events.expectLogin().assertEvent();

        idToken = sendTokenRequestAndGetIDToken(loginEvent);

        // Assert that authTime was updated
        int authTimeUpdated = idToken.getAuthTime();
        Assert.assertTrue(authTime + 10 <= authTimeUpdated);
    }

    @Test
    public void testMaxAge10000() {
        // Open login form and login successfully
        oauth.doLogin("test-user@localhost", "password");
        EventRepresentation loginEvent = events.expectLogin().assertEvent();

        IDToken idToken = sendTokenRequestAndGetIDToken(loginEvent);

        // Check that authTime is available and set to current time
        int authTime = idToken.getAuthTime();
        int currentTime = Time.currentTime();
        Assert.assertTrue(authTime <= currentTime && authTime + 3 >= currentTime);

        // Set time offset
        setTimeOffset(10);

        // Now open login form with maxAge=10000
        oauth.maxAge("10000");

        // Assert that I will be automatically logged through cookie
        oauth.openLoginForm();
        loginEvent = events.expectLogin().assertEvent();

        idToken = sendTokenRequestAndGetIDToken(loginEvent);

        // Assert that authTime is still the same
        int authTimeUpdated = idToken.getAuthTime();
        Assert.assertEquals(authTime, authTimeUpdated);
    }


    // Prompt

    @Test
    public void promptNoneNotLogged() {
        // Send request with prompt=none
        driver.navigate().to(oauth.getLoginFormUrl() + "&prompt=none");

        assertFalse(loginPage.isCurrent());
        assertTrue(appPage.isCurrent());

        events.assertEmpty();

        // Assert error response was sent because not logged in
        OAuthClient.AuthorizationEndpointResponse resp = new OAuthClient.AuthorizationEndpointResponse(oauth);
        Assert.assertNull(resp.getCode());
        Assert.assertEquals(OAuthErrorException.LOGIN_REQUIRED, resp.getError());


    }

    @Test
    public void promptNoneSuccess() {
        // Login user
        loginPage.open();
        loginPage.login("test-user@localhost", "password");
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());

        EventRepresentation loginEvent = events.expectLogin().detail(Details.USERNAME, "test-user@localhost").assertEvent();
        IDToken idToken = sendTokenRequestAndGetIDToken(loginEvent);
        int authTime = idToken.getAuthTime();

        // Set time offset
        setTimeOffset(10);

        // Assert user still logged with previous authTime
        driver.navigate().to(oauth.getLoginFormUrl() + "&prompt=none");
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());

        loginEvent = events.expectLogin().removeDetail(Details.USERNAME).assertEvent();
        idToken = sendTokenRequestAndGetIDToken(loginEvent);
        int authTime2 = idToken.getAuthTime();

        Assert.assertEquals(authTime, authTime2);
    }


    // Prompt=none with consent required for client
    @Test
    public void promptNoneConsentRequired() throws Exception {
        // Require consent
        ClientManager.realm(adminClient.realm("test")).clientId("test-app").consentRequired(true);

        try {
            // login to account mgmt.
            profilePage.open();
            assertTrue(loginPage.isCurrent());
            loginPage.login("test-user@localhost", "password");
            profilePage.assertCurrent();

            events.expectLogin().client(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID)
                    .removeDetail(Details.REDIRECT_URI)
                    .detail(Details.USERNAME, "test-user@localhost").assertEvent();

            // Assert error shown when trying prompt=none and consent not yet retrieved
            driver.navigate().to(oauth.getLoginFormUrl() + "&prompt=none");
            assertTrue(appPage.isCurrent());
            Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());

            OAuthClient.AuthorizationEndpointResponse resp = new OAuthClient.AuthorizationEndpointResponse(oauth);
            Assert.assertNull(resp.getCode());
            Assert.assertEquals(OAuthErrorException.INTERACTION_REQUIRED, resp.getError());

            // Confirm consent
            driver.navigate().to(oauth.getLoginFormUrl());
            grantPage.assertCurrent();
            grantPage.accept();

            events.expectLogin()
                    .detail(Details.USERNAME, "test-user@localhost")
                    .detail(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED)
                    .assertEvent();

            // Consent not required anymore. Login with prompt=none should success
            driver.navigate().to(oauth.getLoginFormUrl() + "&prompt=none");
            Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());

            resp = new OAuthClient.AuthorizationEndpointResponse(oauth);
            Assert.assertNotNull(resp.getCode());
            Assert.assertNull(resp.getError());

            events.expectLogin()
                    .detail(Details.USERNAME, "test-user@localhost")
                    .detail(Details.CONSENT, Details.CONSENT_VALUE_PERSISTED_CONSENT)
                    .assertEvent();

        } finally {
            //  revert require consent
            ClientManager.realm(adminClient.realm("test")).clientId("test-app").consentRequired(false);
        }
    }


    // prompt=login
    @Test
    public void promptLogin() {
        // Login user
        loginPage.open();
        loginPage.login("test-user@localhost", "password");
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());

        EventRepresentation loginEvent = events.expectLogin().detail(Details.USERNAME, "test-user@localhost").assertEvent();
        IDToken idToken = sendTokenRequestAndGetIDToken(loginEvent);
        int authTime = idToken.getAuthTime();

        // Set time offset
        setTimeOffset(10);

        // Assert need to re-authenticate with prompt=login
        driver.navigate().to(oauth.getLoginFormUrl() + "&prompt=login");

        loginPage.assertCurrent();
        loginPage.login("test-user@localhost", "password");
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());

        loginEvent = events.expectLogin().detail(Details.USERNAME, "test-user@localhost").assertEvent();
        idToken = sendTokenRequestAndGetIDToken(loginEvent);
        int authTimeUpdated = idToken.getAuthTime();

        // Assert that authTime was updated
        Assert.assertTrue(authTime + 10 <= authTimeUpdated);

    }

    // DISPLAY & OTHERS

    @Test
    public void nonSupportedParams() {
        driver.navigate().to(oauth.getLoginFormUrl() + "&display=popup&foo=foobar&claims_locales=fr");

        loginPage.assertCurrent();
        loginPage.login("test-user@localhost", "password");
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());

        EventRepresentation loginEvent = events.expectLogin().detail(Details.USERNAME, "test-user@localhost").assertEvent();
        IDToken idToken = sendTokenRequestAndGetIDToken(loginEvent);

        Assert.assertNotNull(idToken);
    }

    // REQUEST & REQUEST_URI

    @Test
    public void requestParam() {
        driver.navigate().to(oauth.getLoginFormUrl() + "&request=abc");

        assertFalse(loginPage.isCurrent());
        assertTrue(appPage.isCurrent());

        // Assert error response was sent because not logged in
        OAuthClient.AuthorizationEndpointResponse resp = new OAuthClient.AuthorizationEndpointResponse(oauth);
        Assert.assertNull(resp.getCode());
        Assert.assertEquals(OAuthErrorException.REQUEST_NOT_SUPPORTED, resp.getError());
    }

    @Test
    public void requestUriParam() {
        driver.navigate().to(oauth.getLoginFormUrl() + "&request_uri=https%3A%2F%2Flocalhost%3A60784%2Fexport%2FqzHTG11W48.jwt");

        assertFalse(loginPage.isCurrent());
        assertTrue(appPage.isCurrent());

        // Assert error response was sent because not logged in
        OAuthClient.AuthorizationEndpointResponse resp = new OAuthClient.AuthorizationEndpointResponse(oauth);
        Assert.assertNull(resp.getCode());
        Assert.assertEquals(OAuthErrorException.REQUEST_URI_NOT_SUPPORTED, resp.getError());
    }

    // LOGIN_HINT

    @Test
    public void loginHint() {
        // Assert need to re-authenticate with prompt=login
        driver.navigate().to(oauth.getLoginFormUrl() + "&" + OIDCLoginProtocol.LOGIN_HINT_PARAM + "=test-user%40localhost");

                loginPage.assertCurrent();
        Assert.assertEquals("test-user@localhost", loginPage.getUsername());
        loginPage.login("password");
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());

        events.expectLogin().detail(Details.USERNAME, "test-user@localhost").assertEvent();
    }

}

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

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.authentication.authenticators.client.JWTClientAuthenticator;
import org.keycloak.common.util.Time;
import org.keycloak.events.Details;
import org.keycloak.jose.jws.Algorithm;
import org.keycloak.models.Constants;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.CertificateRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.util.CertificateInfoHelper;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.admin.AbstractAdminTest;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.client.resources.TestApplicationResourceUrls;
import org.keycloak.testsuite.client.resources.TestOIDCEndpointsApplicationResource;
import org.keycloak.testsuite.pages.AccountUpdateProfilePage;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.OAuthGrantPage;
import org.keycloak.testsuite.rest.resource.TestingOIDCEndpointsApplicationResource;
import org.keycloak.testsuite.util.ClientManager;
import org.keycloak.testsuite.util.OAuthClient;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test for supporting advanced parameters of OIDC specs (max_age, prompt, ...)
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class OIDCAdvancedRequestParamsTest extends AbstractTestRealmKeycloakTest {

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

    @Page
    protected ErrorPage errorPage;


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
        IDToken oldIdToken = sendTokenRequestAndGetIDToken(loginEvent);

        // Set time offset
        setTimeOffset(10);

        // SSO login first WITHOUT prompt=login ( Tests KEYCLOAK-5248 )
        driver.navigate().to(oauth.getLoginFormUrl());
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
        loginEvent = events.expectLogin().detail(Details.USERNAME, "test-user@localhost").assertEvent();
        IDToken newIdToken = sendTokenRequestAndGetIDToken(loginEvent);

        // Assert that authTime wasn't updated
        Assert.assertEquals(oldIdToken.getAuthTime(), newIdToken.getAuthTime());

        // Set time offset
        setTimeOffset(20);

        // Assert need to re-authenticate with prompt=login
        driver.navigate().to(oauth.getLoginFormUrl() + "&prompt=login");

        loginPage.assertCurrent();
        loginPage.login("test-user@localhost", "password");
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());

        loginEvent = events.expectLogin().detail(Details.USERNAME, "test-user@localhost").assertEvent();
        newIdToken = sendTokenRequestAndGetIDToken(loginEvent);

        // Assert that authTime was updated
        Assert.assertTrue("Expected auth time to change. old auth time: " + oldIdToken.getAuthTime() + " , new auth time: " + newIdToken.getAuthTime(),
                oldIdToken.getAuthTime() + 20 <= newIdToken.getAuthTime());

        // Assert userSession didn't change
        Assert.assertEquals(oldIdToken.getSessionState(), newIdToken.getSessionState());
    }


    @Test
    public void promptLoginDifferentUser() throws Exception {
        String sss = oauth.getLoginFormUrl();
        System.out.println(sss);

        // Login user
        loginPage.open();
        loginPage.login("test-user@localhost", "password");
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());

        EventRepresentation loginEvent = events.expectLogin().detail(Details.USERNAME, "test-user@localhost").assertEvent();
        IDToken idToken = sendTokenRequestAndGetIDToken(loginEvent);

        // Assert need to re-authenticate with prompt=login
        driver.navigate().to(oauth.getLoginFormUrl() + "&prompt=login");

        // Authenticate as different user
        loginPage.assertCurrent();
        loginPage.login("john-doh@localhost", "password");

        errorPage.assertCurrent();
        Assert.assertTrue(errorPage.getError().startsWith("You are already authenticated as different user"));
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
    public void requestParamUnsigned() throws Exception {
        oauth.stateParamHardcoded("mystate2");

        String validRedirectUri = oauth.getRedirectUri();
        TestOIDCEndpointsApplicationResource oidcClientEndpointsResource = testingClient.testApp().oidcClientEndpoints();

        // Send request object with invalid redirect uri.
        oidcClientEndpointsResource.setOIDCRequest("test", "test-app", "http://invalid", null, Algorithm.none.toString());
        String requestStr = oidcClientEndpointsResource.getOIDCRequest();

        oauth.request(requestStr);
        oauth.openLoginForm();
        Assert.assertTrue(errorPage.isCurrent());
        assertEquals("Invalid parameter: redirect_uri", errorPage.getError());

        // Assert the value from request object has bigger priority then from the query parameter.
        oauth.redirectUri("http://invalid");
        oidcClientEndpointsResource.setOIDCRequest("test", "test-app", validRedirectUri, "10", Algorithm.none.toString());
        requestStr = oidcClientEndpointsResource.getOIDCRequest();

        oauth.request(requestStr);
        OAuthClient.AuthorizationEndpointResponse response = oauth.doLogin("test-user@localhost", "password");
        Assert.assertNotNull(response.getCode());
        Assert.assertEquals("mystate2", response.getState());
        assertTrue(appPage.isCurrent());
    }

    @Test
    public void requestUriParamUnsigned() throws Exception {
        oauth.stateParamHardcoded("mystate1");

        String validRedirectUri = oauth.getRedirectUri();
        TestOIDCEndpointsApplicationResource oidcClientEndpointsResource = testingClient.testApp().oidcClientEndpoints();

        // Send request object with invalid redirect uri.
        oidcClientEndpointsResource.setOIDCRequest("test", "test-app", "http://invalid", null, Algorithm.none.toString());

        oauth.requestUri(TestApplicationResourceUrls.clientRequestUri());
        oauth.openLoginForm();
        Assert.assertTrue(errorPage.isCurrent());
        assertEquals("Invalid parameter: redirect_uri", errorPage.getError());

        // Assert the value from request object has bigger priority then from the query parameter.
        oauth.redirectUri("http://invalid");
        oidcClientEndpointsResource.setOIDCRequest("test", "test-app", validRedirectUri, "10", Algorithm.none.toString());

        OAuthClient.AuthorizationEndpointResponse response = oauth.doLogin("test-user@localhost", "password");
        Assert.assertNotNull(response.getCode());
        Assert.assertEquals("mystate1", response.getState());
        assertTrue(appPage.isCurrent());
    }

    @Test
    public void requestUriParamSigned() throws Exception {
        oauth.stateParamHardcoded("mystate3");

        String validRedirectUri = oauth.getRedirectUri();
        TestOIDCEndpointsApplicationResource oidcClientEndpointsResource = testingClient.testApp().oidcClientEndpoints();

        // Set required signature for request_uri
        ClientResource clientResource = ApiUtil.findClientByClientId(adminClient.realm("test"), "test-app");
        ClientRepresentation clientRep = clientResource.toRepresentation();
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setRequestObjectSignatureAlg(Algorithm.RS256);
        clientResource.update(clientRep);

        // Verify unsigned request_uri will fail
        oidcClientEndpointsResource.setOIDCRequest("test", "test-app", validRedirectUri, "10", Algorithm.none.toString());
        oauth.requestUri(TestApplicationResourceUrls.clientRequestUri());
        oauth.openLoginForm();
        Assert.assertTrue(errorPage.isCurrent());
        assertEquals("Invalid Request", errorPage.getError());

        // Generate keypair for client
        String clientPublicKeyPem = oidcClientEndpointsResource.generateKeys().get(TestingOIDCEndpointsApplicationResource.PUBLIC_KEY);

        // Verify signed request_uri will fail due to failed signature validation
        oidcClientEndpointsResource.setOIDCRequest("test", "test-app", validRedirectUri, "10", Algorithm.RS256.toString());
        oauth.openLoginForm();
        Assert.assertTrue(errorPage.isCurrent());
        assertEquals("Invalid Request", errorPage.getError());


        // Update clientModel with publicKey for signing
        clientRep = clientResource.toRepresentation();
        CertificateRepresentation cert = new CertificateRepresentation();
        cert.setPublicKey(clientPublicKeyPem);
        CertificateInfoHelper.updateClientRepresentationCertificateInfo(clientRep, cert, JWTClientAuthenticator.ATTR_PREFIX);
        clientResource.update(clientRep);

        // set time offset, so that new keys are downloaded
        setTimeOffset(20);

        // Check signed request_uri will pass
        OAuthClient.AuthorizationEndpointResponse response = oauth.doLogin("test-user@localhost", "password");
        Assert.assertNotNull(response.getCode());
        Assert.assertEquals("mystate3", response.getState());
        assertTrue(appPage.isCurrent());

        // Revert requiring signature for client
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setRequestObjectSignatureAlg(null);
        clientResource.update(clientRep);
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

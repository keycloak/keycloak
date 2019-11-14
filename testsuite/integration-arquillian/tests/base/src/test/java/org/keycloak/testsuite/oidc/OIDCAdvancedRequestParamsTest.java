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

import com.google.common.collect.ImmutableMap;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.authentication.authenticators.client.JWTClientAuthenticator;
import org.keycloak.common.util.Time;
import org.keycloak.events.Details;
import org.keycloak.jose.jws.Algorithm;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.Constants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.CertificateRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.util.CertificateInfoHelper;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.AbstractAdminTest;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
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
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;

/**
 * Test for supporting advanced parameters of OIDC specs (max_age, prompt, ...)
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@AuthServerContainerExclude(AuthServer.REMOTE)
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
            // Revert consent
            UserResource user = ApiUtil.findUserByUsernameId(adminClient.realm("test"), "test-user@localhost");
            user.revokeConsent("test-app");

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


    // prompt=consent
    @Test
    public void promptConsent() {
        // Require consent
        ClientManager.realm(adminClient.realm("test")).clientId("test-app").consentRequired(true);

        try {
            // Login user
            loginPage.open();
            loginPage.login("test-user@localhost", "password");

            // Grant consent
            grantPage.assertCurrent();
            grantPage.accept();

            appPage.assertCurrent();
            Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());

            events.expectLogin()
                    .detail(Details.USERNAME, "test-user@localhost")
                    .detail(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED)
                    .assertEvent();


            // Re-login without prompt=consent. The previous persistent consent was used
            driver.navigate().to(oauth.getLoginFormUrl());
            Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
            events.expectLogin()
                    .detail(Details.USERNAME, "test-user@localhost")
                    .detail(Details.CONSENT, Details.CONSENT_VALUE_PERSISTED_CONSENT)
                    .assertEvent();

            // Re-login with prompt=consent.
            String loginFormUri = UriBuilder.fromUri(oauth.getLoginFormUrl())
                    .queryParam(OIDCLoginProtocol.PROMPT_PARAM, OIDCLoginProtocol.PROMPT_VALUE_CONSENT)
                    .build().toString();
            driver.navigate().to(loginFormUri);

            // Assert grant page displayed again. Will need to grant consent again
            grantPage.assertCurrent();
            grantPage.accept();

            appPage.assertCurrent();
            Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());

            events.expectLogin()
                    .detail(Details.USERNAME, "test-user@localhost")
                    .detail(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED)
                    .assertEvent();

        } finally {
            // Revert consent
            UserResource user = ApiUtil.findUserByUsernameId(adminClient.realm("test"), "test-user@localhost");
            user.revokeConsent("test-app");

            //  revert require consent
            ClientManager.realm(adminClient.realm("test")).clientId("test-app").consentRequired(false);
        }
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
    public void requestObjectNotRequiredNotProvided() throws Exception {
        oauth.stateParamHardcoded("mystate2");
        // Set request object not required for client
        ClientResource clientResource = ApiUtil.findClientByClientId(adminClient.realm("test"), "test-app");
        ClientRepresentation clientRep = clientResource.toRepresentation();
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setRequestObjectRequired(null);
        clientResource.update(clientRep);
        
        // Send request without request object
        // Assert that the request is accepted
        OAuthClient.AuthorizationEndpointResponse response = oauth.doLogin("test-user@localhost", "password");
        Assert.assertNotNull(response.getCode());
        Assert.assertEquals("mystate2", response.getState());
        assertTrue(appPage.isCurrent());
    }
    
    @Test
    public void requestObjectNotRequiredProvidedInRequestParam() throws Exception {
        oauth.stateParamHardcoded("mystate2");
        // Set request object not required for client
        ClientResource clientResource = ApiUtil.findClientByClientId(adminClient.realm("test"), "test-app");
        ClientRepresentation clientRep = clientResource.toRepresentation();
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setRequestObjectRequired(null);
        clientResource.update(clientRep);
        
        // Set up a request object
        TestOIDCEndpointsApplicationResource oidcClientEndpointsResource = testingClient.testApp().oidcClientEndpoints();
        oidcClientEndpointsResource.setOIDCRequest("test", "test-app", oauth.getRedirectUri(), "10", Algorithm.none.toString());
        
        // Send request object in "request" param
        oauth.request(oidcClientEndpointsResource.getOIDCRequest());
        // Assert that the request is accepted
        OAuthClient.AuthorizationEndpointResponse response1 = oauth.doLogin("test-user@localhost", "password");
        Assert.assertNotNull(response1.getCode());
        Assert.assertEquals("mystate2", response1.getState());
        assertTrue(appPage.isCurrent());
    }
    
    @Test
    public void requestObjectNotRequiredProvidedInRequestUriParam() throws Exception {
        oauth.stateParamHardcoded("mystate2");
        // Set request object not required for client
        ClientResource clientResource = ApiUtil.findClientByClientId(adminClient.realm("test"), "test-app");
        ClientRepresentation clientRep = clientResource.toRepresentation();
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setRequestObjectRequired(null);
        clientResource.update(clientRep);
        
        // Set up a request object
        TestOIDCEndpointsApplicationResource oidcClientEndpointsResource = testingClient.testApp().oidcClientEndpoints();
        oidcClientEndpointsResource.setOIDCRequest("test", "test-app", oauth.getRedirectUri(), "10", Algorithm.none.toString());
        
        // Send request object reference in "request_uri" param
        oauth.requestUri(TestApplicationResourceUrls.clientRequestUri());
        // Assert that the request is accepted
        OAuthClient.AuthorizationEndpointResponse response2 = oauth.doLogin("test-user@localhost", "password");
        Assert.assertNotNull(response2.getCode());
        Assert.assertEquals("mystate2", response2.getState());
        assertTrue(appPage.isCurrent());
    }
    
    @Test
    public void requestObjectRequiredNotProvided() throws Exception {
        oauth.stateParamHardcoded("mystate2");
        // Set request object not required for client
        ClientResource clientResource = ApiUtil.findClientByClientId(adminClient.realm("test"), "test-app");
        ClientRepresentation clientRep = clientResource.toRepresentation();
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setRequestObjectRequired(OIDCConfigAttributes.REQUEST_OBJECT_REQUIRED_REQUEST_OR_REQUEST_URI);
        clientResource.update(clientRep);
        
        // Send request without request object
        // Assert that the request is not accepted
        oauth.openLoginForm();
        Assert.assertTrue(errorPage.isCurrent());
        assertEquals("Invalid Request", errorPage.getError());
        
        // Revert requiring request object for client
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setRequestObjectRequired(null);
        clientResource.update(clientRep);
    }
    
    @Test
    public void requestObjectRequiredProvidedInRequestParam() throws Exception {
        oauth.stateParamHardcoded("mystate2");
        // Set request object not required for client
        ClientResource clientResource = ApiUtil.findClientByClientId(adminClient.realm("test"), "test-app");
        ClientRepresentation clientRep = clientResource.toRepresentation();
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setRequestObjectRequired(OIDCConfigAttributes.REQUEST_OBJECT_REQUIRED_REQUEST_OR_REQUEST_URI);
        clientResource.update(clientRep);
        
        // Set up a request object
        TestOIDCEndpointsApplicationResource oidcClientEndpointsResource = testingClient.testApp().oidcClientEndpoints();
        oidcClientEndpointsResource.setOIDCRequest("test", "test-app", oauth.getRedirectUri(), "10", Algorithm.none.toString());
        
        // Send request object in "request" param
        oauth.request(oidcClientEndpointsResource.getOIDCRequest());
        // Assert that the request is accepted
        OAuthClient.AuthorizationEndpointResponse response1 = oauth.doLogin("test-user@localhost", "password");
        Assert.assertNotNull(response1.getCode());
        Assert.assertEquals("mystate2", response1.getState());
        assertTrue(appPage.isCurrent());
        
        // Revert requiring request object for client
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setRequestObjectRequired(null);
        clientResource.update(clientRep);
    }
    
    @Test
    public void requestObjectRequiredProvidedInRequestUriParam() throws Exception {
        oauth.stateParamHardcoded("mystate2");
        // Set request object not required for client
        ClientResource clientResource = ApiUtil.findClientByClientId(adminClient.realm("test"), "test-app");
        ClientRepresentation clientRep = clientResource.toRepresentation();
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setRequestObjectRequired(OIDCConfigAttributes.REQUEST_OBJECT_REQUIRED_REQUEST_OR_REQUEST_URI);
        clientResource.update(clientRep);
        
        // Set up a request object
        TestOIDCEndpointsApplicationResource oidcClientEndpointsResource = testingClient.testApp().oidcClientEndpoints();
        oidcClientEndpointsResource.setOIDCRequest("test", "test-app", oauth.getRedirectUri(), "10", Algorithm.none.toString());
        
        // Send request object reference in "request_uri" param
        oauth.requestUri(TestApplicationResourceUrls.clientRequestUri());
        // Assert that the request is accepted
        OAuthClient.AuthorizationEndpointResponse response2 = oauth.doLogin("test-user@localhost", "password");
        Assert.assertNotNull(response2.getCode());
        Assert.assertEquals("mystate2", response2.getState());
        assertTrue(appPage.isCurrent());
        
        // Revert requiring request object for client
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setRequestObjectRequired(null);
        clientResource.update(clientRep);
    }
    
    @Test
    public void requestObjectRequiredAsRequestParamNotProvided() throws Exception {
        oauth.stateParamHardcoded("mystate2");
        // Set request object not required for client
        ClientResource clientResource = ApiUtil.findClientByClientId(adminClient.realm("test"), "test-app");
        ClientRepresentation clientRep = clientResource.toRepresentation();
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setRequestObjectRequired(OIDCConfigAttributes.REQUEST_OBJECT_REQUIRED_REQUEST);
        clientResource.update(clientRep);
        
        // Send request without request object
        // Assert that the request is not accepted
        oauth.openLoginForm();
        Assert.assertTrue(errorPage.isCurrent());
        assertEquals("Invalid Request", errorPage.getError());
        
        // Revert requiring request object for client
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setRequestObjectRequired(null);
        clientResource.update(clientRep);
    }
    
    @Test
    public void requestObjectRequiredAsRequestParamProvidedInRequestParam() throws Exception {
        oauth.stateParamHardcoded("mystate2");
        // Set request object not required for client
        ClientResource clientResource = ApiUtil.findClientByClientId(adminClient.realm("test"), "test-app");
        ClientRepresentation clientRep = clientResource.toRepresentation();
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setRequestObjectRequired(OIDCConfigAttributes.REQUEST_OBJECT_REQUIRED_REQUEST);
        clientResource.update(clientRep);
        
        // Set up a request object
        TestOIDCEndpointsApplicationResource oidcClientEndpointsResource = testingClient.testApp().oidcClientEndpoints();
        oidcClientEndpointsResource.setOIDCRequest("test", "test-app", oauth.getRedirectUri(), "10", Algorithm.none.toString());
        
        // Send request object in "request" param
        oauth.request(oidcClientEndpointsResource.getOIDCRequest());
        // Assert that the request is accepted
        OAuthClient.AuthorizationEndpointResponse response1 = oauth.doLogin("test-user@localhost", "password");
        Assert.assertNotNull(response1.getCode());
        Assert.assertEquals("mystate2", response1.getState());
        assertTrue(appPage.isCurrent());
        
        // Revert requiring request object for client
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setRequestObjectRequired(null);
        clientResource.update(clientRep);
    }
    
    @Test
    public void requestObjectRequiredAsRequestParamProvidedInRequestUriParam() throws Exception {
        oauth.stateParamHardcoded("mystate2");
        // Set request object not required for client
        ClientResource clientResource = ApiUtil.findClientByClientId(adminClient.realm("test"), "test-app");
        ClientRepresentation clientRep = clientResource.toRepresentation();
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setRequestObjectRequired(OIDCConfigAttributes.REQUEST_OBJECT_REQUIRED_REQUEST);
        clientResource.update(clientRep);
        
        // Set up a request object
        TestOIDCEndpointsApplicationResource oidcClientEndpointsResource = testingClient.testApp().oidcClientEndpoints();
        oidcClientEndpointsResource.setOIDCRequest("test", "test-app", oauth.getRedirectUri(), "10", Algorithm.none.toString());
        
        // Send request object reference in "request_uri" param
        oauth.requestUri(TestApplicationResourceUrls.clientRequestUri());
        // Assert that the request is accepted
        oauth.openLoginForm();
        Assert.assertTrue(errorPage.isCurrent());
        assertEquals("Invalid Request", errorPage.getError());
        
        // Revert requiring request object for client
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setRequestObjectRequired(null);
        clientResource.update(clientRep);
    }
    
    @Test
    public void requestObjectRequiredAsRequestUriParamNotProvided() throws Exception {
        oauth.stateParamHardcoded("mystate2");
        // Set request object not required for client
        ClientResource clientResource = ApiUtil.findClientByClientId(adminClient.realm("test"), "test-app");
        ClientRepresentation clientRep = clientResource.toRepresentation();
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setRequestObjectRequired(OIDCConfigAttributes.REQUEST_OBJECT_REQUIRED_REQUEST_URI);
        clientResource.update(clientRep);
        
        // Send request without request object
        // Assert that the request is not accepted
        oauth.openLoginForm();
        Assert.assertTrue(errorPage.isCurrent());
        assertEquals("Invalid Request", errorPage.getError());
        
        // Revert requiring request object for client
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setRequestObjectRequired(null);
        clientResource.update(clientRep);
    }
    
    @Test
    public void requestObjectRequiredAsRequestUriParamProvidedInRequestParam() throws Exception {
        oauth.stateParamHardcoded("mystate2");
        // Set request object not required for client
        ClientResource clientResource = ApiUtil.findClientByClientId(adminClient.realm("test"), "test-app");
        ClientRepresentation clientRep = clientResource.toRepresentation();
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setRequestObjectRequired(OIDCConfigAttributes.REQUEST_OBJECT_REQUIRED_REQUEST_URI);
        clientResource.update(clientRep);
        
        // Set up a request object
        TestOIDCEndpointsApplicationResource oidcClientEndpointsResource = testingClient.testApp().oidcClientEndpoints();
        oidcClientEndpointsResource.setOIDCRequest("test", "test-app", oauth.getRedirectUri(), "10", Algorithm.none.toString());
        
        // Send request object in "request" param
        oauth.request(oidcClientEndpointsResource.getOIDCRequest());
        // Assert that the request is not accepted
        oauth.openLoginForm();
        Assert.assertTrue(errorPage.isCurrent());
        assertEquals("Invalid Request", errorPage.getError());
        
        // Revert requiring request object for client
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setRequestObjectRequired(null);
        clientResource.update(clientRep);
    }
    
    @Test
    public void requestObjectRequiredAsRequestUriParamProvidedInRequestUriParam() throws Exception {
        oauth.stateParamHardcoded("mystate2");
        // Set request object not required for client
        ClientResource clientResource = ApiUtil.findClientByClientId(adminClient.realm("test"), "test-app");
        ClientRepresentation clientRep = clientResource.toRepresentation();
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setRequestObjectRequired(OIDCConfigAttributes.REQUEST_OBJECT_REQUIRED_REQUEST_URI);
        clientResource.update(clientRep);
        
        // Set up a request object
        TestOIDCEndpointsApplicationResource oidcClientEndpointsResource = testingClient.testApp().oidcClientEndpoints();
        oidcClientEndpointsResource.setOIDCRequest("test", "test-app", oauth.getRedirectUri(), "10", Algorithm.none.toString());
        
        // Send request object reference in "request_uri" param
        oauth.requestUri(TestApplicationResourceUrls.clientRequestUri());
        // Assert that the request is accepted
        OAuthClient.AuthorizationEndpointResponse response1 = oauth.doLogin("test-user@localhost", "password");
        Assert.assertNotNull(response1.getCode());
        Assert.assertEquals("mystate2", response1.getState());
        assertTrue(appPage.isCurrent());
        
        // Revert requiring request object for client
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setRequestObjectRequired(null);
        clientResource.update(clientRep);
    }

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
        String clientPublicKeyPem = oidcClientEndpointsResource.generateKeys("RS256").get(TestingOIDCEndpointsApplicationResource.PUBLIC_KEY);

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

    private void requestUriParamSignedIn(Algorithm expectedAlgorithm, Algorithm actualAlgorithm) throws Exception {
        ClientResource clientResource = null;
        ClientRepresentation clientRep = null;
        try {
            oauth.stateParamHardcoded("mystate3");

            String validRedirectUri = oauth.getRedirectUri();
            TestOIDCEndpointsApplicationResource oidcClientEndpointsResource = testingClient.testApp().oidcClientEndpoints();

            // Set required signature for request_uri
            clientResource = ApiUtil.findClientByClientId(adminClient.realm("test"), "test-app");
            clientRep = clientResource.toRepresentation();
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setRequestObjectSignatureAlg(expectedAlgorithm);
            clientResource.update(clientRep);

            // generate and register client keypair
            if (Algorithm.none != actualAlgorithm) oidcClientEndpointsResource.generateKeys(actualAlgorithm.name());

            // register request object
            oidcClientEndpointsResource.setOIDCRequest("test", "test-app", validRedirectUri, "10", actualAlgorithm.name());

            // use and set jwks_url
            clientResource = ApiUtil.findClientByClientId(adminClient.realm("test"), "test-app");
            clientRep = clientResource.toRepresentation();
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setUseJwksUrl(true);
            String jwksUrl = TestApplicationResourceUrls.clientJwksUri();
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setJwksUrl(jwksUrl);
            clientResource.update(clientRep);

            // set time offset, so that new keys are downloaded
            setTimeOffset(20);

            oauth.requestUri(TestApplicationResourceUrls.clientRequestUri());
            if (expectedAlgorithm == null || expectedAlgorithm == actualAlgorithm) {
                // Check signed request_uri will pass
                OAuthClient.AuthorizationEndpointResponse response = oauth.doLogin("test-user@localhost", "password");
                Assert.assertNotNull(response.getCode());
                Assert.assertEquals("mystate3", response.getState());
                assertTrue(appPage.isCurrent());
            } else {
                // Verify signed request_uri will fail due to failed signature validation
                oauth.openLoginForm();
                Assert.assertTrue(errorPage.isCurrent());
                assertEquals("Invalid Request", errorPage.getError());
            }

        } finally {
            // Revert requiring signature for client
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setRequestObjectSignatureAlg(null);
            // Revert jwks_url settings
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setUseJwksUrl(false);
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setJwksUrl(null);
            clientResource.update(clientRep);
        }
    }

    @Test
    public void requestUriParamSignedExpectedES256ActualRS256() throws Exception {
        // will fail
        requestUriParamSignedIn(Algorithm.ES256, Algorithm.RS256);
    }

    @Test
    public void requestUriParamSignedExpectedNoneActualES256() throws Exception {
        // will fail
        requestUriParamSignedIn(Algorithm.none, Algorithm.ES256);
    }

    @Test
    public void requestUriParamSignedExpectedNoneActualNone() throws Exception {
        // will success
        requestUriParamSignedIn(Algorithm.none, Algorithm.none);
    }

    @Test
    public void requestUriParamSignedExpectedES256ActualES256() throws Exception {
        // will success
        requestUriParamSignedIn(Algorithm.ES256, Algorithm.ES256);
    }

    @Test
    public void requestUriParamSignedExpectedES384ActualES384() throws Exception {
        // will success
        requestUriParamSignedIn(Algorithm.ES384, Algorithm.ES384);
    }

    @Test
    public void requestUriParamSignedExpectedES512ActualES512() throws Exception {
        // will success
        requestUriParamSignedIn(Algorithm.ES512, Algorithm.ES512);
    }

    @Test
    public void requestUriParamSignedExpectedRS384ActualRS384() throws Exception {
        // will success
        requestUriParamSignedIn(Algorithm.RS384, Algorithm.RS384);
    }

    @Test
    public void requestUriParamSignedExpectedRS512ActualRS512() throws Exception {
        // will success
        requestUriParamSignedIn(Algorithm.RS512, Algorithm.RS512);
    }

    @Test
    public void requestUriParamSignedExpectedPS256ActualPS256() throws Exception {
        // will success
        requestUriParamSignedIn(Algorithm.PS256, Algorithm.PS256);
    }

    @Test
    public void requestUriParamSignedExpectedPS384ActualPS384() throws Exception {
        // will success
        requestUriParamSignedIn(Algorithm.PS384, Algorithm.PS384);
    }

    @Test
    public void requestUriParamSignedExpectedPS512ActualPS512() throws Exception {
        // will success
        requestUriParamSignedIn(Algorithm.PS512, Algorithm.PS512);
    }

    @Test
    public void requestUriParamSignedExpectedAnyActualES256() throws Exception {
        // Algorithm is null if 'any'
        // will success
        requestUriParamSignedIn(null, Algorithm.ES256);
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
    
    // CLAIMS
    // included in the session client notes, so custom providers can make use of it
    
    @Test
    public void processClaimsQueryParam() throws IOException {
        Map<String, Object> claims = ImmutableMap.of(
                "id_token", ImmutableMap.of(
                        "test_claim", ImmutableMap.of(
                                "essential", true)));

        String claimsJson = JsonSerialization.writeValueAsString(claims);

        driver.navigate().to(oauth.getLoginFormUrl() + "&" + OIDCLoginProtocol.CLAIMS_PARAM + "=" + claimsJson);
        
        // need to login so session id can be read from event
        loginPage.assertCurrent();
        loginPage.login("test-user@localhost", "password");
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());

        EventRepresentation loginEvent = events.expectLogin().detail(Details.USERNAME, "test-user@localhost").assertEvent();
        String sessionId = loginEvent.getSessionId();
        String clientId = loginEvent.getClientId();
        
        testingClient.server("test").run(session -> {
            RealmModel realmModel = session.getContext().getRealm();
            String clientUuid = realmModel.getClientByClientId(clientId).getId();
            UserSessionModel userSession = session.sessions().getUserSession(realmModel, sessionId);
            AuthenticatedClientSessionModel clientSession = userSession.getAuthenticatedClientSessions().get(clientUuid);
            
            String claimsInSession = clientSession.getNote(OIDCLoginProtocol.CLAIMS_PARAM);
            assertEquals(claimsJson, claimsInSession);
        });
    }
    
    @Test
    public void processClaimsRequestParam() throws Exception {
        Map<String, Object> claims = ImmutableMap.of(
                "id_token", ImmutableMap.of(
                        "test_claim", ImmutableMap.of(
                                "essential", true)));
        
        String claimsJson = JsonSerialization.writeValueAsString(claims);

        Map<String, Object> oidcRequest = new HashMap<>();
        oidcRequest.put(OIDCLoginProtocol.CLIENT_ID_PARAM, "test-app");
        oidcRequest.put(OIDCLoginProtocol.RESPONSE_TYPE_PARAM, OAuth2Constants.CODE);
        oidcRequest.put(OIDCLoginProtocol.REDIRECT_URI_PARAM, oauth.getRedirectUri());
        oidcRequest.put(OIDCLoginProtocol.CLAIMS_PARAM, claims);

        String request = new JWSBuilder().jsonContent(oidcRequest).none();
        
        driver.navigate().to(oauth.getLoginFormUrl() + "&" + OIDCLoginProtocol.REQUEST_PARAM + "=" + request);
        
        // need to login so session id can be read from event
        loginPage.assertCurrent();
        loginPage.login("test-user@localhost", "password");
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());

        EventRepresentation loginEvent = events.expectLogin().detail(Details.USERNAME, "test-user@localhost").assertEvent();
        String sessionId = loginEvent.getSessionId();
        String clientId = loginEvent.getClientId();
        
        testingClient.server("test").run(session -> {
            RealmModel realmModel = session.getContext().getRealm();
            String clientUuid = realmModel.getClientByClientId(clientId).getId();
            UserSessionModel userSession = session.sessions().getUserSession(realmModel, sessionId);
            AuthenticatedClientSessionModel clientSession = userSession.getAuthenticatedClientSessionByClient(clientUuid);
            
            String claimsInSession = clientSession.getNote(OIDCLoginProtocol.CLAIMS_PARAM);
            assertEquals(claimsJson, claimsInSession);
        });
    }

}

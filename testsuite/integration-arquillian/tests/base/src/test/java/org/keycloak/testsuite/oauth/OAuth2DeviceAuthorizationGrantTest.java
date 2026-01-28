/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.common.Profile;
import org.keycloak.events.Errors;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.OAuth2DeviceConfig;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.grants.device.endpoints.DeviceEndpoint;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.UserInfo;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.OAuth2DeviceVerificationPage;
import org.keycloak.testsuite.pages.OAuthGrantPage;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.ContainerAssume;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.OAuthClient;
import org.keycloak.testsuite.util.oauth.PkceGenerator;
import org.keycloak.testsuite.util.oauth.device.DeviceAuthorizationResponse;
import org.keycloak.util.BasicAuthHelper;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.Cookie;

import static org.keycloak.models.OAuth2DeviceConfig.DEFAULT_OAUTH2_DEVICE_CODE_LIFESPAN;
import static org.keycloak.models.OAuth2DeviceConfig.DEFAULT_OAUTH2_DEVICE_POLLING_INTERVAL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author <a href="mailto:h2-wada@nri.co.jp">Hiroyuki Wada</a>
 */
public class OAuth2DeviceAuthorizationGrantTest extends AbstractKeycloakTest {

    private static String userId;

    private static final String REALM_NAME = "test";
    private static final String DEVICE_APP = "test-device";
    private static final String DEVICE_APP_SECRET = "secret";
    private static final String DEVICE_APP_PUBLIC = "test-device-public";
    private static final String DEVICE_APP_PUBLIC_CUSTOM_CONSENT = "test-device-public-custom-consent";
    private static final String DEVICE_APP_WITHOUT_SCOPES = "test-device-without-scopes";
    private static final String SHORT_DEVICE_FLOW_URL = "https://keycloak.org/device";

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected OAuth2DeviceVerificationPage verificationPage;

    @Page
    protected OAuthGrantPage grantPage;

    @Page
    protected ErrorPage errorPage;

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmBuilder realm = RealmBuilder.create().name(REALM_NAME)
                .testEventListener();


        ClientRepresentation app = ClientBuilder.create()
                .id(KeycloakModelUtils.generateId())
                .clientId(DEVICE_APP)
                .secret("secret")
                .attribute(OAuth2DeviceConfig.OAUTH2_DEVICE_AUTHORIZATION_GRANT_ENABLED, "true")
                .build();
        realm.client(app);

        ClientRepresentation appPublic = ClientBuilder.create().id(KeycloakModelUtils.generateId()).publicClient()
            .clientId(DEVICE_APP_PUBLIC).attribute(OAuth2DeviceConfig.OAUTH2_DEVICE_AUTHORIZATION_GRANT_ENABLED, "true")
            .redirectUris(OAuthClient.APP_ROOT + "/auth")
            .build();
        realm.client(appPublic);

        ClientRepresentation appPublicCustomConsent = ClientBuilder.create().id(KeycloakModelUtils.generateId()).publicClient()
                .clientId(DEVICE_APP_PUBLIC_CUSTOM_CONSENT).attribute(OAuth2DeviceConfig.OAUTH2_DEVICE_AUTHORIZATION_GRANT_ENABLED, "true")
                .consentRequired(true)
                .attribute(ClientScopeModel.DISPLAY_ON_CONSENT_SCREEN, "true")
                .attribute(ClientScopeModel.CONSENT_SCREEN_TEXT, "This is the custom consent screen text.")
                .build();
        realm.client(appPublicCustomConsent);

        ClientRepresentation appWithoutScopes = ClientBuilder.create().publicClient()
                .id(KeycloakModelUtils.generateId())
                .clientId(DEVICE_APP_WITHOUT_SCOPES)
                .attribute(OAuth2DeviceConfig.OAUTH2_DEVICE_AUTHORIZATION_GRANT_ENABLED, "true")
                .build();
        realm.client(appWithoutScopes);

        userId = KeycloakModelUtils.generateId();
        UserRepresentation user = UserBuilder.create()
                .id(userId)
                .username("device-login")
                .email("device-login@localhost")
                .password("password")
                .addAttribute("phoneNumber","211211211")
                .build();
        realm.user(user);

        testRealms.add(realm.build());
    }

    @Before
    public void resetConfig() {
        RealmRepresentation realm = getAdminClient().realm(REALM_NAME).toRepresentation();
        realm.setOAuth2DeviceCodeLifespan(60);
        realm.setOAuth2DevicePollingInterval(5);
        getAdminClient().realm(REALM_NAME).update(realm);
    }

    @Test
    public void testConfidentialClient() throws Exception {
        // Device Authorization Request from device
        oauth.realm(REALM_NAME);
        oauth.client(DEVICE_APP, DEVICE_APP_SECRET);
        DeviceAuthorizationResponse response = oauth.device().doDeviceAuthorizationRequest();

        Assert.assertEquals(200, response.getStatusCode());
        assertNotNull(response.getDeviceCode());
        assertNotNull(response.getUserCode());
        assertNotNull(response.getVerificationUri());
        assertNotNull(response.getVerificationUriComplete());
        Assert.assertEquals(60, response.getExpiresIn());
        Assert.assertEquals(5, response.getInterval());

        // Verify user code from verification page using browser
        openVerificationPage(response.getVerificationUri());
        verificationPage.assertCurrent();
        verificationPage.submit(response.getUserCode());

        loginPage.assertCurrent();

        // Do Login
        oauth.fillLoginForm("device-login", "password");

        // Consent
        grantPage.assertCurrent();
        grantPage.assertGrants(OAuthGrantPage.PROFILE_CONSENT_TEXT, OAuthGrantPage.EMAIL_CONSENT_TEXT, OAuthGrantPage.ROLES_CONSENT_TEXT);
        grantPage.accept();

        verificationPage.assertApprovedPage();

        // Token request from device
        AccessTokenResponse tokenResponse = oauth.device().doDeviceTokenRequest(response.getDeviceCode());

        Assert.assertEquals(200, tokenResponse.getStatusCode());

        String tokenString = tokenResponse.getAccessToken();
        assertNotNull(tokenString);
        AccessToken token = oauth.verifyToken(tokenString);

        assertNotNull(token);
    }

    @Test
    public void testPublicClient() throws Exception {
        // Device Authorization Request from device
        oauth.realm(REALM_NAME);
        oauth.client(DEVICE_APP_PUBLIC);
        DeviceAuthorizationResponse response = oauth.device().doDeviceAuthorizationRequest();

        Assert.assertEquals(200, response.getStatusCode());
        assertNotNull(response.getDeviceCode());
        assertNotNull(response.getUserCode());
        assertNotNull(response.getVerificationUri());
        assertNotNull(response.getVerificationUriComplete());
        Assert.assertEquals(60, response.getExpiresIn());
        Assert.assertEquals(5, response.getInterval());

        openVerificationPage(response.getVerificationUriComplete());

        // Do Login
        oauth.fillLoginForm("device-login", "password");

        // Consent
        grantPage.accept();

        // Token request from device
        AccessTokenResponse tokenResponse = oauth.device().doDeviceTokenRequest(response.getDeviceCode());

        Assert.assertEquals(200, tokenResponse.getStatusCode());

        String tokenString = tokenResponse.getAccessToken();
        assertNotNull(tokenString);
        AccessToken token = oauth.verifyToken(tokenString);

        assertNotNull(token);
    }


    @Test
    public void testCustomVerificationUri() throws Exception {
        // Device Authorization Request from device
        try {
            RealmResource testRealm = adminClient.realm(REALM_NAME);
            RealmRepresentation realmRep = testRealm.toRepresentation();
            realmRep.getAttributes().put(DeviceEndpoint.SHORT_VERIFICATION_URI, SHORT_DEVICE_FLOW_URL);
            testRealm.update(realmRep);
            oauth.realm(REALM_NAME);
            oauth.client(DEVICE_APP_PUBLIC);
            DeviceAuthorizationResponse response = oauth.device().doDeviceAuthorizationRequest();

            Assert.assertEquals(200, response.getStatusCode());
            assertNotNull(response.getDeviceCode());
            assertNotNull(response.getUserCode());
            Assert.assertEquals(SHORT_DEVICE_FLOW_URL,response.getVerificationUri());
            Assert.assertEquals(SHORT_DEVICE_FLOW_URL + "?user_code=" + response.getUserCode(),response.getVerificationUriComplete());
        } finally {
            RealmResource testRealm = adminClient.realm(REALM_NAME);
            RealmRepresentation realmRep = testRealm.toRepresentation();
            realmRep.getAttributes().remove("shortVerificationUri");
            testRealm.update(realmRep);
        }
    }

    @Test
    public void testVerifyHolderOfDeviceCode() throws Exception {
        // Device Authorization Request from device
        oauth.realm(REALM_NAME);
        oauth.client(DEVICE_APP_PUBLIC);
        DeviceAuthorizationResponse response = oauth.device().doDeviceAuthorizationRequest();

        assertEquals(200, response.getStatusCode());
        assertNotNull(response.getDeviceCode());
        assertNotNull(response.getUserCode());
        assertNotNull(response.getVerificationUri());
        assertNotNull(response.getVerificationUriComplete());
        assertEquals(60, response.getExpiresIn());
        assertEquals(5, response.getInterval());

        openVerificationPage(response.getVerificationUriComplete());

        // Do Login
        oauth.fillLoginForm("device-login", "password");

        // Consent
        grantPage.accept();

        // Token request from device
        AccessTokenResponse tokenResponse = oauth.device().doDeviceTokenRequest(response.getDeviceCode());

        assertEquals(200, tokenResponse.getStatusCode());

        String tokenString = tokenResponse.getAccessToken();
        assertNotNull(tokenString);
        AccessToken token = oauth.verifyToken(tokenString);

        assertNotNull(token);

        for (Cookie cookie : driver.manage().getCookies()) {
            driver.manage().deleteCookie(cookie);
        }

        oauth.openLoginForm();

        oauth.realm(REALM_NAME);
        oauth.client(DEVICE_APP_PUBLIC_CUSTOM_CONSENT);

        oauth.fillLoginForm("device-login", "password");

        for (Cookie cookie : driver.manage().getCookies()) {
            driver.manage().deleteCookie(cookie);
        }

        oauth.openLoginForm();

        response = oauth.device().doDeviceAuthorizationRequest();

        openVerificationPage(response.getVerificationUriComplete());

        // Consent
        Assert.assertTrue(grantPage.getDisplayedGrants().contains("This is the custom consent screen text."));
        grantPage.accept();

        oauth.client(DEVICE_APP_PUBLIC);

        // Token request from device
        tokenResponse = oauth.device().doDeviceTokenRequest(response.getDeviceCode());

        assertEquals(400, tokenResponse.getStatusCode());
        assertEquals("unauthorized client", tokenResponse.getErrorDescription());
    }

    @Test
    public void testPublicClientOptionalScope() throws Exception {
        // Device Authorization Request from device - check giving optional scope phone
        oauth.realm(REALM_NAME);
        oauth.client(DEVICE_APP_PUBLIC);
        DeviceAuthorizationResponse response = null;
        try {
            oauth.scope("phone");
            response = oauth.device().doDeviceAuthorizationRequest();
        } finally {
            oauth.scope(null);
        }

        Assert.assertEquals(200, response.getStatusCode());
        assertNotNull(response.getDeviceCode());
        assertNotNull(response.getUserCode());
        assertNotNull(response.getVerificationUri());
        assertNotNull(response.getVerificationUriComplete());
        Assert.assertEquals(60, response.getExpiresIn());
        Assert.assertEquals(5, response.getInterval());

        openVerificationPage(response.getVerificationUriComplete());

        // Do Login
        oauth.fillLoginForm("device-login", "password");

        // Consent
        grantPage.assertCurrent();
        grantPage.assertGrants(OAuthGrantPage.PROFILE_CONSENT_TEXT, OAuthGrantPage.EMAIL_CONSENT_TEXT, OAuthGrantPage.ROLES_CONSENT_TEXT, OAuthGrantPage.PHONE_CONSENT_TEXT);
        grantPage.accept();

        // Token request from device
        AccessTokenResponse tokenResponse = oauth.device().doDeviceTokenRequest(response.getDeviceCode());

        Assert.assertEquals(200, tokenResponse.getStatusCode());

        String tokenString = tokenResponse.getAccessToken();
        assertNotNull(tokenString);
        AccessToken token = oauth.verifyToken(tokenString);

        assertNotNull(token);

        UserInfo userInfo = oauth.doUserInfoRequest(tokenString).getUserInfo();
        assertNotNull(userInfo);
        //UserInfo consists preferredUsername, email( required scopes) and phoneNumber(given optional scope)
        Assert.assertEquals("device-login", userInfo.getPreferredUsername());
        Assert.assertEquals("device-login@localhost", userInfo.getEmail());
        Assert.assertEquals("211211211", userInfo.getPhoneNumber());
    }

    @Test
    public void testPublicClientWithPKCESuccess() throws Exception {
        // Successful Device Authorization Request with PKCE from device
        oauth.realm(REALM_NAME);
        oauth.client(DEVICE_APP_PUBLIC);
        PkceGenerator pkce = PkceGenerator.s256();
        DeviceAuthorizationResponse response = oauth.device().deviceAuthorizationRequest().codeChallenge(pkce).send();

        Assert.assertEquals(200, response.getStatusCode());
        assertNotNull(response.getDeviceCode());
        assertNotNull(response.getUserCode());
        assertNotNull(response.getVerificationUri());
        assertNotNull(response.getVerificationUriComplete());
        Assert.assertEquals(60, response.getExpiresIn());
        Assert.assertEquals(5, response.getInterval());

        openVerificationPage(response.getVerificationUriComplete());

        // Do Login
        oauth.fillLoginForm("device-login", "password");

        // Consent
        grantPage.accept();

        // Token request from device
        AccessTokenResponse tokenResponse = oauth.device().deviceTokenRequest(response.getDeviceCode()).codeVerifier(pkce.getCodeVerifier()).send();

        Assert.assertEquals(200, tokenResponse.getStatusCode());

        String tokenString = tokenResponse.getAccessToken();
        assertNotNull(tokenString);
        AccessToken token = oauth.verifyToken(tokenString);

        assertNotNull(token);
    }

    @Test
    public void testPublicClientWithPKCEFail() throws Exception {
        // Device Authorization Request with PKCE from device - device send false code_verifier
        oauth.realm(REALM_NAME);
        oauth.client(DEVICE_APP_PUBLIC);
        PkceGenerator pkce = PkceGenerator.s256();
        DeviceAuthorizationResponse response = oauth.device().deviceAuthorizationRequest().codeChallenge(pkce).send();

        Assert.assertEquals(200, response.getStatusCode());
        assertNotNull(response.getDeviceCode());
        assertNotNull(response.getUserCode());
        assertNotNull(response.getVerificationUri());
        assertNotNull(response.getVerificationUriComplete());
        Assert.assertEquals(60, response.getExpiresIn());
        Assert.assertEquals(5, response.getInterval());

        openVerificationPage(response.getVerificationUriComplete());

        // Do Login
        oauth.fillLoginForm("device-login", "password");

        // Consent
        grantPage.accept();

        // Token request from device
        AccessTokenResponse tokenResponse = oauth.device().deviceTokenRequest(response.getDeviceCode()).codeVerifier(pkce.getCodeVerifier()+"a").send();

        Assert.assertEquals(400, tokenResponse.getStatusCode());
        Assert.assertEquals("invalid_grant", tokenResponse.getError());
        Assert.assertEquals("PKCE verification failed: Code mismatch", tokenResponse.getErrorDescription());
    }


    @Test
    public void testPublicClientCustomConsent() throws Exception {
        // Device Authorization Request from device
        oauth.realm(REALM_NAME);
        oauth.client(DEVICE_APP_PUBLIC_CUSTOM_CONSENT);
        DeviceAuthorizationResponse response = oauth.device().doDeviceAuthorizationRequest();

        Assert.assertEquals(200, response.getStatusCode());
        assertNotNull(response.getDeviceCode());
        assertNotNull(response.getUserCode());
        assertNotNull(response.getVerificationUri());
        assertNotNull(response.getVerificationUriComplete());
        Assert.assertEquals(60, response.getExpiresIn());
        Assert.assertEquals(5, response.getInterval());

        openVerificationPage(response.getVerificationUriComplete());

        // Do Login
        oauth.fillLoginForm("device-login", "password");

        // Consent
        Assert.assertTrue(grantPage.getDisplayedGrants().contains("This is the custom consent screen text."));
        grantPage.accept();

        // Token request from device
        AccessTokenResponse tokenResponse = oauth.device().doDeviceTokenRequest(response.getDeviceCode());

        Assert.assertEquals(200, tokenResponse.getStatusCode());

        String tokenString = tokenResponse.getAccessToken();
        assertNotNull(tokenString);
        AccessToken token = oauth.verifyToken(tokenString);

        assertNotNull(token);
    }

    @Test
    public void testPublicClientConsentWithoutScopes() throws Exception {

        ClientsResource clients = realmsResouce().realm(REALM_NAME).clients();
        ClientRepresentation clientRep = clients.findByClientId(DEVICE_APP_WITHOUT_SCOPES).get(0);
        ClientResource client =  clients.get(clientRep.getId());

        List<ClientScopeRepresentation> defaultClientScopes =  client.getDefaultClientScopes();
        defaultClientScopes.forEach(scope -> client.removeDefaultClientScope(scope.getId()));

        List<ClientScopeRepresentation> optionalClientScopes =  client.getOptionalClientScopes();
        optionalClientScopes.forEach(scope -> client.removeOptionalClientScope(scope.getId()));

        // Device Authorization Request from device
        oauth.realm(REALM_NAME);
        oauth.client(DEVICE_APP_WITHOUT_SCOPES);
        DeviceAuthorizationResponse response = oauth.device().doDeviceAuthorizationRequest();

        Assert.assertEquals(200, response.getStatusCode());
        assertNotNull(response.getVerificationUriComplete());
        openVerificationPage(response.getVerificationUriComplete());
        loginPage.assertCurrent();

        // Do Login
        oauth.fillLoginForm("device-login", "password");
        // Consent
        grantPage.assertCurrent();
    }

    @Test
    public void testNoRefreshToken() throws Exception {
        ClientResource client = ApiUtil.findClientByClientId(adminClient.realm(REALM_NAME), DEVICE_APP);
        ClientRepresentation clientRepresentation = client.toRepresentation();
        clientRepresentation.getAttributes().put(OIDCConfigAttributes.USE_REFRESH_TOKEN, "false");
        client.update(clientRepresentation);
        // Device Authorization Request from device
        oauth.realm(REALM_NAME);
        oauth.client(DEVICE_APP, DEVICE_APP_SECRET);
        DeviceAuthorizationResponse response = oauth.device().doDeviceAuthorizationRequest();

        // Verify user code from verification page using browser
        openVerificationPage(response.getVerificationUri());
        verificationPage.submit(response.getUserCode());

        // Do Login
        oauth.fillLoginForm("device-login", "password");

        // Consent
        grantPage.accept();

        // Token request from device
        AccessTokenResponse tokenResponse = oauth.device().doDeviceTokenRequest(response.getDeviceCode());

        Assert.assertEquals(200, tokenResponse.getStatusCode());

        assertNotNull(tokenResponse.getAccessToken());
        assertNull(tokenResponse.getRefreshToken());

        clientRepresentation.getAttributes().put(OIDCConfigAttributes.USE_REFRESH_TOKEN, "true");
        client.update(clientRepresentation);
    }

    @Test
    public void testConsentCancel() throws Exception {
        // Device Authorization Request from device
        oauth.realm(REALM_NAME);
        oauth.client(DEVICE_APP, DEVICE_APP_SECRET);
        DeviceAuthorizationResponse response = oauth.device().doDeviceAuthorizationRequest();

        Assert.assertEquals(200, response.getStatusCode());
        assertNotNull(response.getDeviceCode());
        assertNotNull(response.getUserCode());
        assertNotNull(response.getVerificationUri());
        assertNotNull(response.getVerificationUriComplete());
        Assert.assertEquals(60, response.getExpiresIn());
        Assert.assertEquals(5, response.getInterval());

        openVerificationPage(response.getVerificationUriComplete());
        loginPage.assertCurrent();

        // Do Login
        oauth.fillLoginForm("device-login", "password");

        // Consent
        grantPage.assertCurrent();
        grantPage.assertGrants(OAuthGrantPage.PROFILE_CONSENT_TEXT, OAuthGrantPage.EMAIL_CONSENT_TEXT, OAuthGrantPage.ROLES_CONSENT_TEXT);
        grantPage.cancel();

        verificationPage.assertDeniedPage();
 
        AccessTokenResponse tokenResponse = oauth.device().doDeviceTokenRequest(response.getDeviceCode());

        Assert.assertEquals(400, tokenResponse.getStatusCode());
        Assert.assertEquals("access_denied", tokenResponse.getError());
    }

    @Test
    public void testInvalidUserCode() throws Exception {
        // Device Authorization Request from device
        oauth.realm(REALM_NAME);
        oauth.client(DEVICE_APP, DEVICE_APP_SECRET);
        DeviceAuthorizationResponse response = oauth.device().doDeviceAuthorizationRequest();

        Assert.assertEquals(200, response.getStatusCode());
        assertNotNull(response.getDeviceCode());
        assertNotNull(response.getUserCode());
        assertNotNull(response.getVerificationUri());
        assertNotNull(response.getVerificationUriComplete());
        Assert.assertEquals(60, response.getExpiresIn());
        Assert.assertEquals(5, response.getInterval());

        openVerificationPage(response.getVerificationUri());
        verificationPage.submit("x");

        verificationPage.assertInvalidUserCodePage();
    }

    @Test
    public void testExpiredUserCodeTest() throws Exception {
        getTestingClient().testing().setTestingInfinispanTimeService();
        // Device Authorization Request from device
        oauth.realm(REALM_NAME);
        oauth.client(DEVICE_APP, DEVICE_APP_SECRET);
        DeviceAuthorizationResponse response = oauth.device().doDeviceAuthorizationRequest();

        Assert.assertEquals(200, response.getStatusCode());
        assertNotNull(response.getDeviceCode());
        assertNotNull(response.getUserCode());
        assertNotNull(response.getVerificationUri());
        assertNotNull(response.getVerificationUriComplete());
        Assert.assertEquals(60, response.getExpiresIn());
        Assert.assertEquals(5, response.getInterval());

        try {
            setTimeOffset(610);
            openVerificationPage(response.getVerificationUriComplete());
        } finally {
            getTestingClient().testing().revertTestingInfinispanTimeService();
            resetTimeOffset();
        }

        // device code not found in the cache because of expiration => invalid_grant error and redirection to the login page
        loginPage.assertCurrent();
    }

    @Test
    public void testInvalidDeviceCode() throws Exception {
        // Device Authorization Request from device
        oauth.realm(REALM_NAME);
        oauth.client(DEVICE_APP, DEVICE_APP_SECRET);
        DeviceAuthorizationResponse response = oauth.device().doDeviceAuthorizationRequest();

        Assert.assertEquals(200, response.getStatusCode());
        assertNotNull(response.getDeviceCode());
        assertNotNull(response.getUserCode());
        assertNotNull(response.getVerificationUri());
        assertNotNull(response.getVerificationUriComplete());
        Assert.assertEquals(60, response.getExpiresIn());
        Assert.assertEquals(5, response.getInterval());

        openVerificationPage(response.getVerificationUriComplete());
        loginPage.assertCurrent();

        // Do Login
        oauth.fillLoginForm("device-login", "password");

        // Consent
        grantPage.assertCurrent();
        grantPage.assertGrants(OAuthGrantPage.PROFILE_CONSENT_TEXT, OAuthGrantPage.EMAIL_CONSENT_TEXT, OAuthGrantPage.ROLES_CONSENT_TEXT);
        grantPage.accept();

        // Token request from device
        AccessTokenResponse tokenResponse = oauth.device().doDeviceTokenRequest("x");

        Assert.assertEquals(400, tokenResponse.getStatusCode());
        Assert.assertEquals("invalid_grant", tokenResponse.getError());
    }

    @Test
    public void testSuccessVerificationUriComplete() throws Exception {
        // Device Authorization Request from device
        oauth.realm(REALM_NAME);
        oauth.client(DEVICE_APP, DEVICE_APP_SECRET);
        DeviceAuthorizationResponse response = oauth.device().doDeviceAuthorizationRequest();

        Assert.assertEquals(200, response.getStatusCode());
        assertNotNull(response.getDeviceCode());
        assertNotNull(response.getUserCode());
        assertNotNull(response.getVerificationUri());
        assertNotNull(response.getVerificationUriComplete());
        Assert.assertEquals(60, response.getExpiresIn());
        Assert.assertEquals(5, response.getInterval());

        openVerificationPage(response.getVerificationUriComplete());
        loginPage.assertCurrent();

        // Do Login
        oauth.fillLoginForm("device-login", "password");

        // Consent
        grantPage.assertCurrent();
        grantPage.assertGrants(OAuthGrantPage.PROFILE_CONSENT_TEXT, OAuthGrantPage.EMAIL_CONSENT_TEXT, OAuthGrantPage.ROLES_CONSENT_TEXT);
        grantPage.accept();

        verificationPage.assertApprovedPage();

        // Token request from device
        AccessTokenResponse tokenResponse = oauth.device().doDeviceTokenRequest(response.getDeviceCode());

        Assert.assertEquals(200, tokenResponse.getStatusCode());
    }

    @Test
    public void testExpiredDeviceCode() throws Exception {
        getTestingClient().testing().setTestingInfinispanTimeService();
        // Device Authorization Request from device
        oauth.realm(REALM_NAME);
        oauth.client(DEVICE_APP, DEVICE_APP_SECRET);
        DeviceAuthorizationResponse response = oauth.device().doDeviceAuthorizationRequest();

        Assert.assertEquals(200, response.getStatusCode());
        assertNotNull(response.getDeviceCode());
        assertNotNull(response.getUserCode());
        assertNotNull(response.getVerificationUri());
        assertNotNull(response.getVerificationUriComplete());
        Assert.assertEquals(60, response.getExpiresIn());
        Assert.assertEquals(5, response.getInterval());

        try {
            setTimeOffset(610);
            // Token request from device
            AccessTokenResponse tokenResponse = oauth.device().doDeviceTokenRequest(response.getDeviceCode());

            Assert.assertEquals(400, tokenResponse.getStatusCode());
            Assert.assertEquals("invalid_grant", tokenResponse.getError());
        } finally {
            getTestingClient().testing().revertTestingInfinispanTimeService();
            resetTimeOffset();
        }
    }

    @Test
    public void testDuplicatedRequestParams() throws Exception {
        oauth.realm(REALM_NAME);
        oauth.client(DEVICE_APP_PUBLIC);
        DeviceAuthorizationResponse response = doDeviceAuthorizationWithDuplicatedParams(DEVICE_APP_PUBLIC, null);
        
        Assert.assertEquals(400, response.getStatusCode());
        Assert.assertEquals("invalid_grant", response.getError());
        Assert.assertEquals("duplicated parameter", response.getErrorDescription());
    }

    @Test
    public void testDeviceCodeLifespanPerClient() throws Exception {
        getTestingClient().testing().setTestingInfinispanTimeService();
        ClientResource client = ApiUtil.findClientByClientId(adminClient.realm(REALM_NAME), DEVICE_APP);
        ClientRepresentation clientRepresentation = client.toRepresentation();
        // Device Authorization Request from device
        oauth.realm(REALM_NAME);
        oauth.client(DEVICE_APP, DEVICE_APP_SECRET);
        DeviceAuthorizationResponse response = oauth.device().doDeviceAuthorizationRequest();

        Assert.assertEquals(200, response.getStatusCode());
        assertNotNull(response.getDeviceCode());
        assertNotNull(response.getUserCode());
        assertNotNull(response.getVerificationUri());
        assertNotNull(response.getVerificationUriComplete());
        Assert.assertEquals(60, response.getExpiresIn());
        Assert.assertEquals(5, response.getInterval());

        clientRepresentation.getAttributes().put(OAuth2DeviceConfig.OAUTH2_DEVICE_CODE_LIFESPAN_PER_CLIENT, "120");
        clientRepresentation.getAttributes().put(OAuth2DeviceConfig.OAUTH2_DEVICE_POLLING_INTERVAL_PER_CLIENT, "600000");
        client.update(clientRepresentation);

        response = oauth.device().doDeviceAuthorizationRequest();
        Assert.assertEquals(120, response.getExpiresIn());
        AccessTokenResponse tokenResponse;

        try {
            setTimeOffset(100);
            // Token request from device
            tokenResponse = oauth.device().doDeviceTokenRequest(response.getDeviceCode());

            Assert.assertEquals(400, tokenResponse.getStatusCode());
            Assert.assertEquals("authorization_pending", tokenResponse.getError());

            setTimeOffset(125);
            // Token request from device
            tokenResponse = oauth.device().doDeviceTokenRequest(response.getDeviceCode());

            Assert.assertEquals(400, tokenResponse.getStatusCode());
            Assert.assertEquals("expired_token", tokenResponse.getError());
        } finally {
            getTestingClient().testing().revertTestingInfinispanTimeService();
            resetTimeOffset();
        }

        clientRepresentation.getAttributes().put(OAuth2DeviceConfig.OAUTH2_DEVICE_CODE_LIFESPAN_PER_CLIENT, "");
        clientRepresentation.getAttributes().put(OAuth2DeviceConfig.OAUTH2_DEVICE_POLLING_INTERVAL_PER_CLIENT, "");
        client.update(clientRepresentation);
    }

    @Test
    public void testDevicePollingIntervalPerClient() throws Exception {
        getTestingClient().testing().setTestingInfinispanTimeService();
        ClientResource client = ApiUtil.findClientByClientId(adminClient.realm(REALM_NAME), DEVICE_APP);
        ClientRepresentation clientRepresentation = client.toRepresentation();
        // Device Authorization Request from device
        oauth.realm(REALM_NAME);
        oauth.client(DEVICE_APP, DEVICE_APP_SECRET);
        DeviceAuthorizationResponse response = oauth.device().doDeviceAuthorizationRequest();

        Assert.assertEquals(200, response.getStatusCode());
        assertNotNull(response.getDeviceCode());
        assertNotNull(response.getUserCode());
        assertNotNull(response.getVerificationUri());
        assertNotNull(response.getVerificationUriComplete());
        Assert.assertEquals(60, response.getExpiresIn());
        Assert.assertEquals(5, response.getInterval());

        clientRepresentation.getAttributes().put(OAuth2DeviceConfig.OAUTH2_DEVICE_POLLING_INTERVAL_PER_CLIENT, "10");
        client.update(clientRepresentation);

        response = oauth.device().doDeviceAuthorizationRequest();
        Assert.assertEquals(10, response.getInterval());

        try {
            // Token request from device
            AccessTokenResponse tokenResponse = oauth.device().doDeviceTokenRequest(response.getDeviceCode());

            Assert.assertEquals(400, tokenResponse.getStatusCode());
            Assert.assertEquals("authorization_pending", tokenResponse.getError());

            // Token request from device
            tokenResponse = oauth.device().doDeviceTokenRequest(response.getDeviceCode());

            Assert.assertEquals(400, tokenResponse.getStatusCode());
            Assert.assertEquals("slow_down", tokenResponse.getError());

            setTimeOffset(7);

            // Token request from device
            tokenResponse = oauth.device().doDeviceTokenRequest(response.getDeviceCode());

            Assert.assertEquals(400, tokenResponse.getStatusCode());
            Assert.assertEquals("slow_down", tokenResponse.getError());

            setTimeOffset(10);

            // Token request from device
            tokenResponse = oauth.device().doDeviceTokenRequest(response.getDeviceCode());

            Assert.assertEquals(400, tokenResponse.getStatusCode());
            Assert.assertEquals("authorization_pending", tokenResponse.getError());
        } finally {
            getTestingClient().testing().revertTestingInfinispanTimeService();
            clientRepresentation.getAttributes().put(OAuth2DeviceConfig.OAUTH2_DEVICE_POLLING_INTERVAL_PER_CLIENT, "");
            client.update(clientRepresentation);
        }
    }

    @Test
    public void testPolling() throws Exception {
        getTestingClient().testing().setTestingInfinispanTimeService();

        try {
            RealmRepresentation realm = getAdminClient().realm(REALM_NAME).toRepresentation();
            realm.setOAuth2DeviceCodeLifespan(600);
            getAdminClient().realm(REALM_NAME).update(realm);
            // Device Authorization Request from device
            oauth.realm(REALM_NAME);
            oauth.client(DEVICE_APP, DEVICE_APP_SECRET);
            DeviceAuthorizationResponse response = oauth.device().doDeviceAuthorizationRequest();

            Assert.assertEquals(600, response.getExpiresIn());
            Assert.assertEquals(5, response.getInterval());

            // Polling token request from device
            AccessTokenResponse tokenResponse = oauth.device().doDeviceTokenRequest(response.getDeviceCode());

            // Not approved yet
            Assert.assertEquals(400, tokenResponse.getStatusCode());
            Assert.assertEquals("authorization_pending", tokenResponse.getError());

            // Polling again without waiting
            tokenResponse = oauth.device().doDeviceTokenRequest(response.getDeviceCode());

            // Slow down
            Assert.assertEquals(400, tokenResponse.getStatusCode());
            Assert.assertEquals("slow_down", tokenResponse.getError());

            // Wait the interval
            setTimeOffset(5);

            // Polling again
            tokenResponse = oauth.device().doDeviceTokenRequest(response.getDeviceCode());

            // Not approved yet
            Assert.assertEquals(400, tokenResponse.getStatusCode());
            Assert.assertEquals("authorization_pending", tokenResponse.getError());

            // Change the interval setting of the realm from 5 seconds to 10 seconds.
            realm.setOAuth2DevicePollingInterval(10);
            getAdminClient().realm(REALM_NAME).update(realm);

            // Checking the new interval is applied
            response = oauth.device().doDeviceAuthorizationRequest();

            Assert.assertEquals(600, response.getExpiresIn());
            Assert.assertEquals(10, response.getInterval());

            // Polling token request from device
            tokenResponse = oauth.device().doDeviceTokenRequest(response.getDeviceCode());

            // Not approved yet
            Assert.assertEquals(400, tokenResponse.getStatusCode());
            Assert.assertEquals("authorization_pending", tokenResponse.getError());

            // Wait
            setTimeOffset(10);

            // Polling again without waiting
            tokenResponse = oauth.device().doDeviceTokenRequest(response.getDeviceCode());

            // Slow down
            Assert.assertEquals(400, tokenResponse.getStatusCode());
            Assert.assertEquals("slow_down", tokenResponse.getError());

            // Wait
            setTimeOffset(15);

            // Polling again
            tokenResponse = oauth.device().doDeviceTokenRequest(response.getDeviceCode());

            // Not approved yet
            Assert.assertEquals(400, tokenResponse.getStatusCode());
            Assert.assertEquals("authorization_pending", tokenResponse.getError());
        } finally {
            getTestingClient().testing().revertTestingInfinispanTimeService();
        }
    }

    @Test
    public void testUpdateConfig() {
        RealmResource realm = getAdminClient().realm(REALM_NAME);
        RealmRepresentation rep = realm.toRepresentation();

        rep.setOAuth2DevicePollingInterval(DEFAULT_OAUTH2_DEVICE_POLLING_INTERVAL);
        rep.setOAuth2DeviceCodeLifespan(DEFAULT_OAUTH2_DEVICE_CODE_LIFESPAN);

        realm.update(rep);
        rep = realm.toRepresentation();

        Assert.assertEquals(DEFAULT_OAUTH2_DEVICE_POLLING_INTERVAL, rep.getOAuth2DevicePollingInterval().intValue());
        Assert.assertEquals(DEFAULT_OAUTH2_DEVICE_CODE_LIFESPAN, rep.getOAuth2DeviceCodeLifespan().intValue());

        rep.setOAuth2DevicePollingInterval(10);
        rep.setOAuth2DeviceCodeLifespan(15);

        realm.update(rep);
        rep = realm.toRepresentation();

        Assert.assertEquals(10, rep.getOAuth2DevicePollingInterval().intValue());
        Assert.assertEquals(15, rep.getOAuth2DeviceCodeLifespan().intValue());
    }

    // KEYCLOAK-19700
    @Test
    public void testConsentCancelCannotBeReused() throws Exception {
        // Device Authorization Request from device
        oauth.realm(REALM_NAME);
        oauth.client(DEVICE_APP, DEVICE_APP_SECRET);
        DeviceAuthorizationResponse response = oauth.device().doDeviceAuthorizationRequest();

        Assert.assertEquals(200, response.getStatusCode());
        assertNotNull(response.getDeviceCode());
        assertNotNull(response.getUserCode());
        assertNotNull(response.getVerificationUri());
        assertNotNull(response.getVerificationUriComplete());
        Assert.assertEquals(60, response.getExpiresIn());
        Assert.assertEquals(5, response.getInterval());

        openVerificationPage(response.getVerificationUriComplete());
        loginPage.assertCurrent();

        // Do Login
        oauth.fillLoginForm("device-login", "password");

        // Consent
        grantPage.assertCurrent();
        grantPage.cancel();

        verificationPage.assertDeniedPage();

        openVerificationPage(response.getVerificationUriComplete());

        verificationPage.assertInvalidUserCodePage();
    }

    @Test
    public void testConsentCancelCannotBeReusedAfterBackClicked() throws Exception {
        // Device Authorization Request from device
        oauth.realm(REALM_NAME);
        oauth.client(DEVICE_APP, DEVICE_APP_SECRET);
        DeviceAuthorizationResponse response = oauth.device().doDeviceAuthorizationRequest();

        Assert.assertEquals(200, response.getStatusCode());
        assertNotNull(response.getDeviceCode());
        assertNotNull(response.getUserCode());
        assertNotNull(response.getVerificationUri());
        assertNotNull(response.getVerificationUriComplete());
        Assert.assertEquals(60, response.getExpiresIn());
        Assert.assertEquals(5, response.getInterval());

        openVerificationPage(response.getVerificationUriComplete());
        loginPage.assertCurrent();

        // Do Login
        oauth.fillLoginForm("device-login", "password");

        // Consent
        grantPage.assertCurrent();
        grantPage.cancel();

        //click back after cancel
        driver.navigate().back();

        // Accept consent
        grantPage.assertCurrent();
        grantPage.assertGrants(OAuthGrantPage.PROFILE_CONSENT_TEXT, OAuthGrantPage.EMAIL_CONSENT_TEXT, OAuthGrantPage.ROLES_CONSENT_TEXT);
        grantPage.accept();

        verificationPage.assertDeniedPage();

        AccessTokenResponse tokenResponse = oauth.device().doDeviceTokenRequest(response.getDeviceCode());

        Assert.assertEquals(400, tokenResponse.getStatusCode());
        Assert.assertEquals("access_denied", tokenResponse.getError());
    }

    @Test
    public void testNotFoundClient() throws Exception {
        oauth.realm(REALM_NAME);
        oauth.client("test-device-public2");
        DeviceAuthorizationResponse response = oauth.device().doDeviceAuthorizationRequest();

        Assert.assertEquals(401, response.getStatusCode());
        Assert.assertEquals(Errors.INVALID_CLIENT, response.getError());
        Assert.assertEquals("Invalid client or Invalid client credentials", response.getErrorDescription());
    }
    @Test
    public void testClientWithErrors() throws Exception {
        try {
            ClientResource client = ApiUtil.findClientByClientId(adminClient.realm(REALM_NAME), DEVICE_APP_PUBLIC);
            ClientRepresentation clientRepresentation = client.toRepresentation();
            clientRepresentation.getAttributes().put(OAuth2DeviceConfig.OAUTH2_DEVICE_AUTHORIZATION_GRANT_ENABLED, "false");
            client.update(clientRepresentation);
            oauth.realm(REALM_NAME);
            oauth.client(DEVICE_APP_PUBLIC);

            //DeviceAuthorizationGrant not enabled
            DeviceAuthorizationResponse response = oauth.device().doDeviceAuthorizationRequest();
            Assert.assertEquals(400, response.getStatusCode());
            Assert.assertEquals(Errors.UNAUTHORIZED_CLIENT, response.getError());
            Assert.assertEquals("Client is not allowed to initiate OAuth 2.0 Device Authorization Grant. The flow is disabled for the client.", response.getErrorDescription());

            clientRepresentation.getAttributes().put(OAuth2DeviceConfig.OAUTH2_DEVICE_AUTHORIZATION_GRANT_ENABLED, "true");
            clientRepresentation.setBearerOnly(true);
            client.update(clientRepresentation);

            //BearerOnly client
            response = oauth.device().doDeviceAuthorizationRequest();
            Assert.assertEquals(403, response.getStatusCode());
            Assert.assertEquals(Errors.UNAUTHORIZED_CLIENT, response.getError());
            Assert.assertEquals("Bearer-only applications are not allowed to initiate browser login.", response.getErrorDescription());

        } finally {
            ClientResource client = ApiUtil.findClientByClientId(adminClient.realm(REALM_NAME), DEVICE_APP_PUBLIC);
            ClientRepresentation clientRepresentation = client.toRepresentation();
            clientRepresentation.getAttributes().put(OAuth2DeviceConfig.OAUTH2_DEVICE_AUTHORIZATION_GRANT_ENABLED, "true");
            clientRepresentation.setBearerOnly(false);
            client.update(clientRepresentation);
        }
    }

    @Test
    public void ensureDeviceFlowConfigPresentWhenDeviceFlowIsEnabled() {

        OIDCConfigurationRepresentation oidcConfigRep = oauth.doWellKnownRequest();
        Assert.assertNotNull("deviceAuthorizationEndpoint should be not null", oidcConfigRep.getDeviceAuthorizationEndpoint());
        Assert.assertNotNull("mtlsEndpointAliases.deviceAuthorizationEndpoint should be not null", oidcConfigRep.getMtlsEndpointAliases().getDeviceAuthorizationEndpoint());
    }

    @Test
    // @DisableFeature(value = Profile.Feature.DEVICE_FLOW, executeAsLast = false, skipRestart = true)
    public void ensureDeviceFlowConfigNotPresentWhenDeviceFlowIsDisabled() throws Exception {

        // this test currently does not work with -Pauth-server-quarkus
        ContainerAssume.assumeAuthServerUndertow();

        testingClient.disableFeature(Profile.Feature.DEVICE_FLOW);

        try {
            OIDCConfigurationRepresentation oidcConfigRep = oauth.doWellKnownRequest();
            Assert.assertNull("deviceAuthorizationEndpoint should be null", oidcConfigRep.getDeviceAuthorizationEndpoint());
            Assert.assertNull("mtlsEndpointAliases.deviceAuthorizationEndpoint should be null", oidcConfigRep.getMtlsEndpointAliases().getDeviceAuthorizationEndpoint());

            try (CloseableHttpResponse response = oauth.httpClient().get().execute(new HttpGet(oauth.getEndpoints().getDeviceAuthorization()))) {
                Assert.assertEquals("Should return not found for device auth endpoint", 404, response.getStatusLine().getStatusCode());
            }

            oauth.realm(REALM_NAME);
            oauth.client(DEVICE_APP_PUBLIC);
            AccessTokenResponse tokenResponse = oauth.device().doDeviceTokenRequest("dummy");
            Assert.assertEquals(OAuthErrorException.UNSUPPORTED_GRANT_TYPE, tokenResponse.getError());
        } finally {
            testingClient.resetFeature(Profile.Feature.DEVICE_FLOW);
        }
    }

    private void openVerificationPage(String verificationUri) {
        driver.navigate().to(verificationUri);
    }

    private DeviceAuthorizationResponse doDeviceAuthorizationWithDuplicatedParams(String clientId, String clientSecret) throws Exception {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpPost post = new HttpPost(oauth.getEndpoints().getDeviceAuthorization());

            List<NameValuePair> parameters = new LinkedList<>();
            if (clientSecret != null) {
                String authorization = BasicAuthHelper.createHeader(clientId, clientSecret);
                post.setHeader("Authorization", authorization);
            } else {
                parameters.add(new BasicNameValuePair("client_id", clientId));
            }

            parameters.add(new BasicNameValuePair(OAuth2Constants.SCOPE, "profile"));
            parameters.add(new BasicNameValuePair(OAuth2Constants.SCOPE, "foo"));

            UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8);
            post.setEntity(formEntity);

            return new DeviceAuthorizationResponse(client.execute(post));
        }
    }
}

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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.keycloak.models.OAuth2DeviceConfig.DEFAULT_OAUTH2_DEVICE_CODE_LIFESPAN;
import static org.keycloak.models.OAuth2DeviceConfig.DEFAULT_OAUTH2_DEVICE_POLLING_INTERVAL;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.OAuth2DeviceConfig;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.UserInfo;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.oidc.PkceGenerator;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.OAuth2DeviceVerificationPage;
import org.keycloak.testsuite.pages.OAuthGrantPage;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.UserBuilder;

import java.util.List;

/**
 * @author <a href="mailto:h2-wada@nri.co.jp">Hiroyuki Wada</a>
 */
public class OAuth2DeviceAuthorizationGrantTest extends AbstractKeycloakTest {

    private static String userId;

    public static final String REALM_NAME = "test";
    public static final String DEVICE_APP = "test-device";
    public static final String DEVICE_APP_PUBLIC = "test-device-public";
    public static final String DEVICE_APP_PUBLIC_CUSTOM_CONSENT = "test-device-public-custom-consent";

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
                .privateKey("MIICXAIBAAKBgQCrVrCuTtArbgaZzL1hvh0xtL5mc7o0NqPVnYXkLvgcwiC3BjLGw1tGEGoJaXDuSaRllobm53JBhjx33UNv+5z/UMG4kytBWxheNVKnL6GgqlNabMaFfPLPCF8kAgKnsi79NMo+n6KnSY8YeUmec/p2vjO2NjsSAVcWEQMVhJ31LwIDAQABAoGAfmO8gVhyBxdqlxmIuglbz8bcjQbhXJLR2EoS8ngTXmN1bo2L90M0mUKSdc7qF10LgETBzqL8jYlQIbt+e6TH8fcEpKCjUlyq0Mf/vVbfZSNaVycY13nTzo27iPyWQHK5NLuJzn1xvxxrUeXI6A2WFpGEBLbHjwpx5WQG9A+2scECQQDvdn9NE75HPTVPxBqsEd2z10TKkl9CZxu10Qby3iQQmWLEJ9LNmy3acvKrE3gMiYNWb6xHPKiIqOR1as7L24aTAkEAtyvQOlCvr5kAjVqrEKXalj0Tzewjweuxc0pskvArTI2Oo070h65GpoIKLc9jf+UA69cRtquwP93aZKtW06U8dQJAF2Y44ks/mK5+eyDqik3koCI08qaC8HYq2wVl7G2QkJ6sbAaILtcvD92ToOvyGyeE0flvmDZxMYlvaZnaQ0lcSQJBAKZU6umJi3/xeEbkJqMfeLclD27XGEFoPeNrmdx0q10Azp4NfJAY+Z8KRyQCR2BEG+oNitBOZ+YXF9KCpH3cdmECQHEigJhYg+ykOvr1aiZUMFT72HU0jnmQe2FVekuG+LJUt2Tm7GtMjTFoGpf0JwrVuZN39fOYAlo+nTixgeW7X8Y=")
                .publicKey("MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCrVrCuTtArbgaZzL1hvh0xtL5mc7o0NqPVnYXkLvgcwiC3BjLGw1tGEGoJaXDuSaRllobm53JBhjx33UNv+5z/UMG4kytBWxheNVKnL6GgqlNabMaFfPLPCF8kAgKnsi79NMo+n6KnSY8YeUmec/p2vjO2NjsSAVcWEQMVhJ31LwIDAQAB")
                .testEventListener();


        ClientRepresentation app = ClientBuilder.create()
                .id(KeycloakModelUtils.generateId())
                .clientId("test-device")
                .secret("secret")
                .attribute(OAuth2DeviceConfig.OAUTH2_DEVICE_AUTHORIZATION_GRANT_ENABLED, "true")
                .build();
        realm.client(app);

        ClientRepresentation appPublic = ClientBuilder.create().id(KeycloakModelUtils.generateId()).publicClient()
            .clientId(DEVICE_APP_PUBLIC).attribute(OAuth2DeviceConfig.OAUTH2_DEVICE_AUTHORIZATION_GRANT_ENABLED, "true")
            .build();
        realm.client(appPublic);

        ClientRepresentation appPublicCustomConsent = ClientBuilder.create().id(KeycloakModelUtils.generateId()).publicClient()
                .clientId(DEVICE_APP_PUBLIC_CUSTOM_CONSENT).attribute(OAuth2DeviceConfig.OAUTH2_DEVICE_AUTHORIZATION_GRANT_ENABLED, "true")
                .consentRequired(true)
                .attribute(ClientScopeModel.DISPLAY_ON_CONSENT_SCREEN, "true")
                .attribute(ClientScopeModel.CONSENT_SCREEN_TEXT, "This is the custom consent screen text.")
                .build();
        realm.client(appPublicCustomConsent);

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
        oauth.clientId(DEVICE_APP);
        OAuthClient.DeviceAuthorizationResponse response = oauth.doDeviceAuthorizationRequest(DEVICE_APP, "secret");

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
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doDeviceTokenRequest(DEVICE_APP, "secret", response.getDeviceCode());

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
        oauth.clientId(DEVICE_APP_PUBLIC);
        OAuthClient.DeviceAuthorizationResponse response = oauth.doDeviceAuthorizationRequest(DEVICE_APP_PUBLIC, null);

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
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doDeviceTokenRequest(DEVICE_APP_PUBLIC, null, response.getDeviceCode());

        Assert.assertEquals(200, tokenResponse.getStatusCode());

        String tokenString = tokenResponse.getAccessToken();
        assertNotNull(tokenString);
        AccessToken token = oauth.verifyToken(tokenString);

        assertNotNull(token);
    }

    @Test
    public void testPublicClientOptionalScope() throws Exception {
        // Device Authorization Request from device - check giving optional scope phone
        oauth.realm(REALM_NAME);
        oauth.clientId(DEVICE_APP_PUBLIC);
        OAuthClient.DeviceAuthorizationResponse response = null;
        try {
            oauth.scope("phone");
            response = oauth.doDeviceAuthorizationRequest(DEVICE_APP_PUBLIC, null);
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
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doDeviceTokenRequest(DEVICE_APP_PUBLIC, null, response.getDeviceCode());

        Assert.assertEquals(200, tokenResponse.getStatusCode());

        String tokenString = tokenResponse.getAccessToken();
        assertNotNull(tokenString);
        AccessToken token = oauth.verifyToken(tokenString);

        assertNotNull(token);

        UserInfo userInfo = oauth.doUserInfoRequest(tokenString);
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
        oauth.clientId(DEVICE_APP_PUBLIC);
        PkceGenerator pkce = new PkceGenerator();
        oauth.codeChallenge(pkce.getCodeChallenge());
        oauth.codeChallengeMethod(OAuth2Constants.PKCE_METHOD_S256);
        oauth.codeVerifier(pkce.getCodeVerifier());
        OAuthClient.DeviceAuthorizationResponse response = oauth.doDeviceAuthorizationRequest(DEVICE_APP_PUBLIC, null);

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
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doDeviceTokenRequest(DEVICE_APP_PUBLIC, null, response.getDeviceCode());

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
        oauth.clientId(DEVICE_APP_PUBLIC);
        PkceGenerator pkce = new PkceGenerator();
        oauth.codeChallenge(pkce.getCodeChallenge());
        oauth.codeChallengeMethod(OAuth2Constants.PKCE_METHOD_S256);
        oauth.codeVerifier(pkce.getCodeVerifier()+"a");
        OAuthClient.DeviceAuthorizationResponse response = oauth.doDeviceAuthorizationRequest(DEVICE_APP_PUBLIC, null);

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
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doDeviceTokenRequest(DEVICE_APP_PUBLIC, null, response.getDeviceCode());

        Assert.assertEquals(400, tokenResponse.getStatusCode());
        Assert.assertEquals("invalid_grant", tokenResponse.getError());
        Assert.assertEquals("PKCE verification failed", tokenResponse.getErrorDescription());
    }


    @Test
    public void testPublicClientCustomConsent() throws Exception {
        // Device Authorization Request from device
        oauth.realm(REALM_NAME);
        oauth.clientId(DEVICE_APP_PUBLIC_CUSTOM_CONSENT);
        OAuthClient.DeviceAuthorizationResponse response = oauth.doDeviceAuthorizationRequest(DEVICE_APP_PUBLIC_CUSTOM_CONSENT, null);

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
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doDeviceTokenRequest(DEVICE_APP_PUBLIC_CUSTOM_CONSENT, null, response.getDeviceCode());

        Assert.assertEquals(200, tokenResponse.getStatusCode());

        String tokenString = tokenResponse.getAccessToken();
        assertNotNull(tokenString);
        AccessToken token = oauth.verifyToken(tokenString);

        assertNotNull(token);
    }

    @Test
    public void testNoRefreshToken() throws Exception {
        ClientResource client = ApiUtil.findClientByClientId(adminClient.realm(REALM_NAME), DEVICE_APP);
        ClientRepresentation clientRepresentation = client.toRepresentation();
        clientRepresentation.getAttributes().put(OIDCConfigAttributes.USE_REFRESH_TOKEN, "false");
        client.update(clientRepresentation);
        // Device Authorization Request from device
        oauth.realm(REALM_NAME);
        oauth.clientId(DEVICE_APP);
        OAuthClient.DeviceAuthorizationResponse response = oauth.doDeviceAuthorizationRequest(DEVICE_APP, "secret");

        // Verify user code from verification page using browser
        openVerificationPage(response.getVerificationUri());
        verificationPage.submit(response.getUserCode());

        // Do Login
        oauth.fillLoginForm("device-login", "password");

        // Consent
        grantPage.accept();

        // Token request from device
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doDeviceTokenRequest(DEVICE_APP, "secret",
            response.getDeviceCode());

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
        oauth.clientId(DEVICE_APP);
        OAuthClient.DeviceAuthorizationResponse response = oauth.doDeviceAuthorizationRequest(DEVICE_APP, "secret");

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
 
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doDeviceTokenRequest(DEVICE_APP, "secret", response.getDeviceCode());

        Assert.assertEquals(400, tokenResponse.getStatusCode());
        Assert.assertEquals("access_denied", tokenResponse.getError());
    }

    @Test
    public void testInvalidUserCode() throws Exception {
        // Device Authorization Request from device
        oauth.realm(REALM_NAME);
        oauth.clientId(DEVICE_APP);
        OAuthClient.DeviceAuthorizationResponse response = oauth.doDeviceAuthorizationRequest(DEVICE_APP, "secret");

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
        // Device Authorization Request from device
        oauth.realm(REALM_NAME);
        oauth.clientId(DEVICE_APP);
        OAuthClient.DeviceAuthorizationResponse response = oauth.doDeviceAuthorizationRequest(DEVICE_APP, "secret");

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
            resetTimeOffset();
        }

        verificationPage.assertExpiredUserCodePage();
    }

    @Test
    public void testInvalidDeviceCode() throws Exception {
        // Device Authorization Request from device
        oauth.realm(REALM_NAME);
        oauth.clientId(DEVICE_APP);
        OAuthClient.DeviceAuthorizationResponse response = oauth.doDeviceAuthorizationRequest(DEVICE_APP, "secret");

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
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doDeviceTokenRequest(DEVICE_APP, "secret", "x");

        Assert.assertEquals(400, tokenResponse.getStatusCode());
        Assert.assertEquals("invalid_grant", tokenResponse.getError());
    }

    @Test
    public void testSuccessVerificationUriComplete() throws Exception {
        // Device Authorization Request from device
        oauth.realm(REALM_NAME);
        oauth.clientId(DEVICE_APP);
        OAuthClient.DeviceAuthorizationResponse response = oauth.doDeviceAuthorizationRequest(DEVICE_APP, "secret");

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
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doDeviceTokenRequest(DEVICE_APP, "secret", response.getDeviceCode());

        Assert.assertEquals(200, tokenResponse.getStatusCode());
    }

    @Test
    public void testExpiredDeviceCode() throws Exception {
        // Device Authorization Request from device
        oauth.realm(REALM_NAME);
        oauth.clientId(DEVICE_APP);
        OAuthClient.DeviceAuthorizationResponse response = oauth.doDeviceAuthorizationRequest(DEVICE_APP, "secret");

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
            OAuthClient.AccessTokenResponse tokenResponse = oauth.doDeviceTokenRequest(DEVICE_APP, "secret",
                response.getDeviceCode());

            Assert.assertEquals(400, tokenResponse.getStatusCode());
            Assert.assertEquals("expired_token", tokenResponse.getError());
        } finally {
            resetTimeOffset();
        }
    }

    @Test
    public void testDeviceCodeLifespanPerClient() throws Exception {
        ClientResource client = ApiUtil.findClientByClientId(adminClient.realm(REALM_NAME), DEVICE_APP);
        ClientRepresentation clientRepresentation = client.toRepresentation();
        // Device Authorization Request from device
        oauth.realm(REALM_NAME);
        oauth.clientId(DEVICE_APP);
        OAuthClient.DeviceAuthorizationResponse response = oauth.doDeviceAuthorizationRequest(DEVICE_APP, "secret");

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

        response = oauth.doDeviceAuthorizationRequest(DEVICE_APP, "secret");
        Assert.assertEquals(120, response.getExpiresIn());
        OAuthClient.AccessTokenResponse tokenResponse;

        try {
            setTimeOffset(100);
            // Token request from device
            tokenResponse = oauth.doDeviceTokenRequest(DEVICE_APP, "secret", response.getDeviceCode());

            Assert.assertEquals(400, tokenResponse.getStatusCode());
            Assert.assertEquals("authorization_pending", tokenResponse.getError());

            setTimeOffset(125);
            // Token request from device
            tokenResponse = oauth.doDeviceTokenRequest(DEVICE_APP, "secret", response.getDeviceCode());

            Assert.assertEquals(400, tokenResponse.getStatusCode());
            Assert.assertEquals("expired_token", tokenResponse.getError());
        } finally {
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
        oauth.clientId(DEVICE_APP);
        OAuthClient.DeviceAuthorizationResponse response = oauth.doDeviceAuthorizationRequest(DEVICE_APP, "secret");

        Assert.assertEquals(200, response.getStatusCode());
        assertNotNull(response.getDeviceCode());
        assertNotNull(response.getUserCode());
        assertNotNull(response.getVerificationUri());
        assertNotNull(response.getVerificationUriComplete());
        Assert.assertEquals(60, response.getExpiresIn());
        Assert.assertEquals(5, response.getInterval());

        clientRepresentation.getAttributes().put(OAuth2DeviceConfig.OAUTH2_DEVICE_POLLING_INTERVAL_PER_CLIENT, "10");
        client.update(clientRepresentation);

        response = oauth.doDeviceAuthorizationRequest(DEVICE_APP, "secret");
        Assert.assertEquals(10, response.getInterval());

        try {
            // Token request from device
            OAuthClient.AccessTokenResponse tokenResponse = oauth.doDeviceTokenRequest(DEVICE_APP, "secret",
                response.getDeviceCode());

            Assert.assertEquals(400, tokenResponse.getStatusCode());
            Assert.assertEquals("authorization_pending", tokenResponse.getError());

            // Token request from device
            tokenResponse = oauth.doDeviceTokenRequest(DEVICE_APP, "secret", response.getDeviceCode());

            Assert.assertEquals(400, tokenResponse.getStatusCode());
            Assert.assertEquals("slow_down", tokenResponse.getError());

            setTimeOffset(7);

            // Token request from device
            tokenResponse = oauth.doDeviceTokenRequest(DEVICE_APP, "secret", response.getDeviceCode());

            Assert.assertEquals(400, tokenResponse.getStatusCode());
            Assert.assertEquals("slow_down", tokenResponse.getError());

            setTimeOffset(10);

            // Token request from device
            tokenResponse = oauth.doDeviceTokenRequest(DEVICE_APP, "secret", response.getDeviceCode());

            Assert.assertEquals(400, tokenResponse.getStatusCode());
            Assert.assertEquals("authorization_pending", tokenResponse.getError());
        } finally {
            getTestingClient().testing().revertTestingInfinispanTimeService();
            clientRepresentation.getAttributes().put(OAuth2DeviceConfig.OAUTH2_DEVICE_POLLING_INTERVAL_PER_CLIENT, "");
            client.update(clientRepresentation);
        }
    }

    @Test
    public void testPooling() throws Exception {
        getTestingClient().testing().setTestingInfinispanTimeService();

        try {
            RealmRepresentation realm = getAdminClient().realm(REALM_NAME).toRepresentation();
            realm.setOAuth2DeviceCodeLifespan(600);
            getAdminClient().realm(REALM_NAME).update(realm);
            // Device Authorization Request from device
            oauth.realm(REALM_NAME);
            oauth.clientId(DEVICE_APP);
            OAuthClient.DeviceAuthorizationResponse response = oauth.doDeviceAuthorizationRequest(DEVICE_APP, "secret");

            Assert.assertEquals(600, response.getExpiresIn());
            Assert.assertEquals(5, response.getInterval());

            // Polling token request from device
            OAuthClient.AccessTokenResponse tokenResponse = oauth.doDeviceTokenRequest(DEVICE_APP, "secret", response.getDeviceCode());

            // Not approved yet
            Assert.assertEquals(400, tokenResponse.getStatusCode());
            Assert.assertEquals("authorization_pending", tokenResponse.getError());

            // Polling again without waiting
            tokenResponse = oauth.doDeviceTokenRequest(DEVICE_APP, "secret", response.getDeviceCode());

            // Slow down
            Assert.assertEquals(400, tokenResponse.getStatusCode());
            Assert.assertEquals("slow_down", tokenResponse.getError());

            // Wait the interval
            setTimeOffset(5);

            // Polling again
            tokenResponse = oauth.doDeviceTokenRequest(DEVICE_APP, "secret", response.getDeviceCode());

            // Not approved yet
            Assert.assertEquals(400, tokenResponse.getStatusCode());
            Assert.assertEquals("authorization_pending", tokenResponse.getError());

            // Change the interval setting of the realm from 5 seconds to 10 seconds.
            realm.setOAuth2DevicePollingInterval(10);
            getAdminClient().realm(REALM_NAME).update(realm);

            // Checking the new interval is applied
            response = oauth.doDeviceAuthorizationRequest(DEVICE_APP, "secret");

            Assert.assertEquals(600, response.getExpiresIn());
            Assert.assertEquals(10, response.getInterval());

            // Polling token request from device
            tokenResponse = oauth.doDeviceTokenRequest(DEVICE_APP, "secret", response.getDeviceCode());

            // Not approved yet
            Assert.assertEquals(400, tokenResponse.getStatusCode());
            Assert.assertEquals("authorization_pending", tokenResponse.getError());

            // Wait
            setTimeOffset(10);

            // Polling again without waiting
            tokenResponse = oauth.doDeviceTokenRequest(DEVICE_APP, "secret", response.getDeviceCode());

            // Slow down
            Assert.assertEquals(400, tokenResponse.getStatusCode());
            Assert.assertEquals("slow_down", tokenResponse.getError());

            // Wait
            setTimeOffset(15);

            // Polling again
            tokenResponse = oauth.doDeviceTokenRequest(DEVICE_APP, "secret", response.getDeviceCode());

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
        oauth.clientId(DEVICE_APP);
        OAuthClient.DeviceAuthorizationResponse response = oauth.doDeviceAuthorizationRequest(DEVICE_APP, "secret");

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

    private void openVerificationPage(String verificationUri) {
        driver.navigate().to(verificationUri);
    }
}

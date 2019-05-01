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

import org.jboss.arquillian.graphene.page.Page;
import org.junit.*;
import org.keycloak.events.Details;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.OAuth2DeviceVerificationPage;
import org.keycloak.testsuite.pages.OAuthGrantPage;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.testsuite.util.WaitUtils;

import java.util.List;

/**
 * @author <a href="mailto:h2-wada@nri.co.jp">Hiroyuki Wada</a>
 */
public class OAuth2DeviceAuthorizationGrantTest extends AbstractKeycloakTest {

    private static String userId;

    public static final String REALM_NAME = "test";
    public static final String DEVICE_APP = "test-device";
    public static final String DEVICE_APP_PUBLIC = "test-device-public";

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
                .oauth2DeviceAuthorizationGrant()
                .secret("secret")
                .build();
        realm.client(app);

        ClientRepresentation app2 = ClientBuilder.create()
                .id(KeycloakModelUtils.generateId())
                .clientId("test-device-public")
                .oauth2DeviceAuthorizationGrant()
                .publicClient()
                .build();
        realm.client(app2);

        userId = KeycloakModelUtils.generateId();
        UserRepresentation user = UserBuilder.create()
                .id(userId)
                .username("device-login")
                .email("device-login@localhost")
                .password("password")
                .build();
        realm.user(user);

        testRealms.add(realm.build());
    }

    @Before
    public void resetConifg() {
        RealmRepresentation realm = getAdminClient().realm(REALM_NAME).toRepresentation();
        realm.setOAuth2DeviceCodeLifespan(600);
        realm.setOAuth2DevicePollingInterval(5);
        getAdminClient().realm(REALM_NAME).update(realm);
    }

    @Test
    public void publicClientTest() throws Exception {
        // Device Authorization Request from device
        oauth.realm(REALM_NAME);
        oauth.clientId(DEVICE_APP_PUBLIC);
        OAuthClient.DeviceAuthorizationResponse response = oauth.doDeviceAuthorizationRequest(DEVICE_APP_PUBLIC, null);

        Assert.assertEquals(200, response.getStatusCode());
        Assert.assertNotNull(response.getDeviceCode());
        Assert.assertNotNull(response.getUserCode());
        Assert.assertNotNull(response.getVerificationUri());
        Assert.assertNotNull(response.getVerificationUriComplete());
        Assert.assertEquals(600, response.getExpiresIn());
        Assert.assertEquals(5, response.getInterval());

        // Verify user code from verification page using browser
        openVerificationPage(response.getVerificationUri());
        verificationPage.assertCurrent();
        verificationPage.submit(response.getUserCode());

        EventRepresentation verifyEvent = events.expectDeviceVerifyUserCode(DEVICE_APP_PUBLIC).assertEvent();
        String codeId = verifyEvent.getDetails().get(Details.CODE_ID);

        verificationPage.assertLoginPage();

        // Do Login
        oauth.fillLoginForm("device-login", "password");

        // Consent
        grantPage.assertCurrent();
        grantPage.assertGrants(OAuthGrantPage.PROFILE_CONSENT_TEXT, OAuthGrantPage.EMAIL_CONSENT_TEXT, OAuthGrantPage.ROLES_CONSENT_TEXT);
        grantPage.accept();

        verificationPage.assertApprovedPage();

        events.expectDeviceLogin(DEVICE_APP_PUBLIC, codeId, userId).assertEvent();

        // Token request from device
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doDeviceTokenRequest(DEVICE_APP_PUBLIC, null, response.getDeviceCode());

        Assert.assertEquals(200, tokenResponse.getStatusCode());

        String tokenString = tokenResponse.getAccessToken();
        Assert.assertNotNull(tokenString);
        AccessToken token = oauth.verifyToken(tokenString);

        // Check receiving access token which is bound to the user session of the verification process
        Assert.assertTrue(codeId.equals(token.getSessionState()));

        events.expectDeviceCodeToToken(DEVICE_APP_PUBLIC, codeId, userId).assertEvent();
    }

    @Test
    public void confidentialClientTest() throws Exception {
        // Device Authorization Request from device
        oauth.realm(REALM_NAME);
        oauth.clientId(DEVICE_APP);
        OAuthClient.DeviceAuthorizationResponse response = oauth.doDeviceAuthorizationRequest(DEVICE_APP, "secret");

        Assert.assertEquals(200, response.getStatusCode());
        Assert.assertNotNull(response.getDeviceCode());
        Assert.assertNotNull(response.getUserCode());
        Assert.assertNotNull(response.getVerificationUri());
        Assert.assertNotNull(response.getVerificationUriComplete());
        Assert.assertEquals(600, response.getExpiresIn());
        Assert.assertEquals(5, response.getInterval());

        // Verify user code from verification page using browser
        openVerificationPage(response.getVerificationUri());
        verificationPage.assertCurrent();
        verificationPage.submit(response.getUserCode());

        EventRepresentation verifyEvent = events.expectDeviceVerifyUserCode(DEVICE_APP).assertEvent();
        String codeId = verifyEvent.getDetails().get(Details.CODE_ID);

        verificationPage.assertLoginPage();

        // Do Login
        oauth.fillLoginForm("device-login", "password");

        // Consent
        grantPage.assertCurrent();
        grantPage.assertGrants(OAuthGrantPage.PROFILE_CONSENT_TEXT, OAuthGrantPage.EMAIL_CONSENT_TEXT, OAuthGrantPage.ROLES_CONSENT_TEXT);
        grantPage.accept();

        verificationPage.assertApprovedPage();

        events.expectDeviceLogin(DEVICE_APP, codeId, userId).assertEvent();

        // Token request from device
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doDeviceTokenRequest(DEVICE_APP, "secret", response.getDeviceCode());

        Assert.assertEquals(200, tokenResponse.getStatusCode());

        String tokenString = tokenResponse.getAccessToken();
        Assert.assertNotNull(tokenString);
        AccessToken token = oauth.verifyToken(tokenString);

        // Check receiving access token which is bound to the user session of the verification process
        Assert.assertTrue(codeId.equals(token.getSessionState()));

        events.expectDeviceCodeToToken(DEVICE_APP, codeId, userId).assertEvent();
    }

    @Test
    public void pollingTest() throws Exception {
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
        WaitUtils.pause(5000);

        // Polling again
        tokenResponse = oauth.doDeviceTokenRequest(DEVICE_APP, "secret", response.getDeviceCode());

        // Not approved yet
        Assert.assertEquals(400, tokenResponse.getStatusCode());
        Assert.assertEquals("authorization_pending", tokenResponse.getError());


        // Change the interval setting of the realm from 5 seconds to 10 seconds.
        RealmRepresentation realm = getAdminClient().realm(REALM_NAME).toRepresentation();
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
        WaitUtils.pause(5000);

        // Polling again without waiting
        tokenResponse = oauth.doDeviceTokenRequest(DEVICE_APP, "secret", response.getDeviceCode());

        // Slow down
        Assert.assertEquals(400, tokenResponse.getStatusCode());
        Assert.assertEquals("slow_down", tokenResponse.getError());

        // Wait
        WaitUtils.pause(5000);

        // Polling again
        tokenResponse = oauth.doDeviceTokenRequest(DEVICE_APP, "secret", response.getDeviceCode());

        // Not approved yet
        Assert.assertEquals(400, tokenResponse.getStatusCode());
        Assert.assertEquals("authorization_pending", tokenResponse.getError());
    }

    private void openVerificationPage(String verificationUri) {
        driver.navigate().to(verificationUri);
    }
}

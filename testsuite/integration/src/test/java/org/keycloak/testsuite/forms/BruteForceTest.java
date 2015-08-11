/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.keycloak.testsuite.forms;

import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.models.Constants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.OAuthClient;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.AppPage.RequestType;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginTotpPage;
import org.keycloak.testsuite.rule.GreenMailRule;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.KeycloakRule.KeycloakSetup;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.openqa.selenium.WebDriver;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.net.MalformedURLException;
import java.util.Collections;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class BruteForceTest {

    @ClassRule
    public static KeycloakRule keycloakRule = new KeycloakRule(new KeycloakSetup() {

        @Override
        public void config(RealmManager manager, RealmModel defaultRealm, RealmModel appRealm) {
            UserModel user = manager.getSession().users().getUserByUsername("test-user@localhost", appRealm);

            UserCredentialModel credentials = new UserCredentialModel();
            credentials.setType(CredentialRepresentation.TOTP);
            credentials.setValue("totpSecret");
            user.updateCredential(credentials);

            user.setOtpEnabled(true);
            appRealm.setEventsListeners(Collections.singleton("dummy"));

            appRealm.setBruteForceProtected(true);
            appRealm.setFailureFactor(2);
        }

    });

    @Rule
    public AssertEvents events = new AssertEvents(keycloakRule);

    @Rule
    public WebRule webRule = new WebRule(this);

    @Rule
    public GreenMailRule greenMail = new GreenMailRule();

    @WebResource
    protected WebDriver driver;

    @WebResource
    protected AppPage appPage;

    @WebResource
    protected LoginPage loginPage;

    @WebResource
    protected LoginTotpPage loginTotpPage;

    @WebResource
    protected OAuthClient oauth;


    private TimeBasedOTP totp = new TimeBasedOTP();

    private int lifespan;

    @Before
    public void before() throws MalformedURLException {
        totp = new TimeBasedOTP();
    }

    public String getAdminToken() throws Exception {
        String clientId = Constants.ADMIN_CONSOLE_CLIENT_ID;
        return oauth.doGrantAccessTokenRequest("master", "admin", "admin", null, clientId, null).getAccessToken();
    }

    public OAuthClient.AccessTokenResponse getTestToken(String password, String totp) throws Exception {
        return oauth.doGrantAccessTokenRequest("test", "test-user@localhost", password, totp, oauth.getClientId(), "password");

    }

    protected void clearUserFailures() throws Exception {
        String token = getAdminToken();
        Client client = ClientBuilder.newClient();
        Response response = client.target(AppPage.AUTH_SERVER_URL)
                .path("admin/realms/test/attack-detection/brute-force/usernames/test-user@localhost")
                .request()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .delete();
        Assert.assertEquals(204, response.getStatus());
        response.close();
        client.close();


    }

    protected void clearAllUserFailures() throws Exception {
        String token = getAdminToken();
        Client client = ClientBuilder.newClient();
        Response response = client.target(AppPage.AUTH_SERVER_URL)
                .path("admin/realms/test/attack-detection/brute-force/usernames")
                .request()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .delete();
        Assert.assertEquals(204, response.getStatus());
        response.close();
        client.close();


    }

    @Test
    public void testGrantInvalidPassword() throws Exception {
        {
            String totpSecret = totp.generateTOTP("totpSecret");
            OAuthClient.AccessTokenResponse response = getTestToken("password", totpSecret);
            Assert.assertNotNull(response.getAccessToken());
            Assert.assertNull(response.getError());
            events.clear();
        }
        {
            String totpSecret = totp.generateTOTP("totpSecret");
            OAuthClient.AccessTokenResponse response = getTestToken("invalid", totpSecret);
            Assert.assertNull(response.getAccessToken());
            Assert.assertEquals(response.getError(), "invalid_grant");
            Assert.assertEquals(response.getErrorDescription(), "Invalid user credentials");
            events.clear();
        }
        {
            String totpSecret = totp.generateTOTP("totpSecret");
            OAuthClient.AccessTokenResponse response = getTestToken("invalid", totpSecret);
            Assert.assertNull(response.getAccessToken());
            Assert.assertEquals(response.getError(), "invalid_grant");
            Assert.assertEquals(response.getErrorDescription(), "Invalid user credentials");
            events.clear();
        }
        {
            String totpSecret = totp.generateTOTP("totpSecret");
            OAuthClient.AccessTokenResponse response = getTestToken("password", totpSecret);
            Assert.assertNull(response.getAccessToken());
            Assert.assertNotNull(response.getError());
            Assert.assertEquals(response.getError(), "invalid_grant");
            Assert.assertEquals(response.getErrorDescription(), "Account temporarily disabled");
            events.clear();
        }
        clearUserFailures();
        {
            String totpSecret = totp.generateTOTP("totpSecret");
            OAuthClient.AccessTokenResponse response = getTestToken("password", totpSecret);
            Assert.assertNotNull(response.getAccessToken());
            Assert.assertNull(response.getError());
            events.clear();
        }

    }

    @Test
    public void testGrantInvalidOtp() throws Exception {
        {
            String totpSecret = totp.generateTOTP("totpSecret");
            OAuthClient.AccessTokenResponse response = getTestToken("password", totpSecret);
            Assert.assertNotNull(response.getAccessToken());
            Assert.assertNull(response.getError());
            events.clear();
        }
        {
            OAuthClient.AccessTokenResponse response = getTestToken("password", "shite");
            Assert.assertNull(response.getAccessToken());
            Assert.assertEquals(response.getError(), "invalid_grant");
            Assert.assertEquals(response.getErrorDescription(), "Invalid user credentials");
            events.clear();
        }
        {
            OAuthClient.AccessTokenResponse response = getTestToken("password", "shite");
            Assert.assertNull(response.getAccessToken());
            Assert.assertEquals(response.getError(), "invalid_grant");
            Assert.assertEquals(response.getErrorDescription(), "Invalid user credentials");
            events.clear();
        }
        {
            String totpSecret = totp.generateTOTP("totpSecret");
            OAuthClient.AccessTokenResponse response = getTestToken("password", totpSecret);
            Assert.assertNull(response.getAccessToken());
            Assert.assertNotNull(response.getError());
            Assert.assertEquals(response.getError(), "invalid_grant");
            Assert.assertEquals(response.getErrorDescription(), "Account temporarily disabled");
            events.clear();
        }
        clearUserFailures();
        {
            String totpSecret = totp.generateTOTP("totpSecret");
            OAuthClient.AccessTokenResponse response = getTestToken("password", totpSecret);
            Assert.assertNotNull(response.getAccessToken());
            Assert.assertNull(response.getError());
            events.clear();
        }

    }   @Test
        public void testGrantMissingOtp() throws Exception {
        {
            String totpSecret = totp.generateTOTP("totpSecret");
            OAuthClient.AccessTokenResponse response = getTestToken("password", totpSecret);
            Assert.assertNotNull(response.getAccessToken());
            Assert.assertNull(response.getError());
            events.clear();
        }
        {
            OAuthClient.AccessTokenResponse response = getTestToken("password", null);
            Assert.assertNull(response.getAccessToken());
            Assert.assertEquals(response.getError(), "invalid_grant");
            Assert.assertEquals(response.getErrorDescription(), "Invalid user credentials");
            events.clear();
        }
        {
            OAuthClient.AccessTokenResponse response = getTestToken("password", null);
            Assert.assertNull(response.getAccessToken());
            Assert.assertEquals(response.getError(), "invalid_grant");
            Assert.assertEquals(response.getErrorDescription(), "Invalid user credentials");
            events.clear();
        }
        {
            String totpSecret = totp.generateTOTP("totpSecret");
            OAuthClient.AccessTokenResponse response = getTestToken("password", totpSecret);
            Assert.assertNull(response.getAccessToken());
            Assert.assertNotNull(response.getError());
            Assert.assertEquals(response.getError(), "invalid_grant");
            Assert.assertEquals(response.getErrorDescription(), "Account temporarily disabled");
            events.clear();
        }
        clearUserFailures();
        {
            String totpSecret = totp.generateTOTP("totpSecret");
            OAuthClient.AccessTokenResponse response = getTestToken("password", totpSecret);
            Assert.assertNotNull(response.getAccessToken());
            Assert.assertNull(response.getError());
            events.clear();
        }

    }




    @Test
    public void testBrowserInvalidPassword() throws Exception {
        loginSuccess();
        loginInvalidPassword();
        loginInvalidPassword();
        expectTemporarilyDisabled();
        clearUserFailures();
        loginSuccess();
        loginInvalidPassword();
        loginInvalidPassword();
        expectTemporarilyDisabled();
        clearAllUserFailures();
        loginSuccess();
    }

    @Test
    public void testBrowserMissingPassword() throws Exception {
        loginSuccess();
        loginMissingPassword();
        loginMissingPassword();
        expectTemporarilyDisabled();
        clearUserFailures();
        loginSuccess();
    }

    @Test
    public void testBrowserInvalidTotp() throws Exception {
        loginSuccess();
        loginWithTotpFailure();
        loginWithTotpFailure();
        expectTemporarilyDisabled();
        clearUserFailures();
        loginSuccess();
    }

    @Test
    public void testBrowserMissingTotp() throws Exception {
        loginSuccess();
        loginWithMissingTotp();
        loginWithMissingTotp();
        expectTemporarilyDisabled();
        clearUserFailures();
        loginSuccess();
    }

    public void expectTemporarilyDisabled() throws Exception {
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        loginPage.assertCurrent();
        String src = driver.getPageSource();
        Assert.assertEquals("Account is temporarily disabled, contact admin or try again later.", loginPage.getError());
        events.expectLogin().session((String) null).error(Errors.USER_TEMPORARILY_DISABLED)
                .detail(Details.USERNAME, "test-user@localhost")
                .removeDetail(Details.CONSENT)
                .assertEvent();
    }



    public void loginSuccess() throws Exception {
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        loginTotpPage.assertCurrent();

        String totpSecret = totp.generateTOTP("totpSecret");
        loginTotpPage.login(totpSecret);

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        events.expectLogin().assertEvent();

        appPage.logout();
        events.clear();


    }

    public void loginWithTotpFailure() throws Exception {
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        loginTotpPage.assertCurrent();

        loginTotpPage.login("123456");
        loginTotpPage.assertCurrent();
        Assert.assertEquals("Invalid authenticator code.", loginPage.getError());
        events.clear();
    }

    public void loginWithMissingTotp() throws Exception {
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        loginTotpPage.assertCurrent();

        loginTotpPage.login(null);
        loginTotpPage.assertCurrent();
        Assert.assertEquals("Invalid authenticator code.", loginPage.getError());

        events.clear();
    }


    public void loginInvalidPassword() throws Exception {
        loginPage.open();
        loginPage.login("test-user@localhost", "invalid");

        loginPage.assertCurrent();

        Assert.assertEquals("Invalid username or password.", loginPage.getError());

        events.clear();
    }

    public void loginMissingPassword() {
        loginPage.open();
        loginPage.missingPassword("test-user@localhost");

        loginPage.assertCurrent();

        Assert.assertEquals("Invalid username or password.", loginPage.getError());
        events.clear();
    }

}

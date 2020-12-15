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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.events.Details;
import org.keycloak.models.Constants;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.AppPage.RequestType;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginTotpPage;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.GreenMailRule;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.RealmRepUtil;
import org.keycloak.testsuite.util.UserBuilder;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.MalformedURLException;

import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class LoginTotpTest extends AbstractTestRealmKeycloakTest {

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        UserRepresentation user = RealmRepUtil.findUser(testRealm, "test-user@localhost");
        UserBuilder.edit(user)
                   .totpSecret("totpSecret")
                   .otpEnabled();
    }

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Rule
    public GreenMailRule greenMail = new GreenMailRule();

    @Page
    protected AppPage appPage;

    @Page
    protected LoginPage loginPage;

    @Page
    protected LoginTotpPage loginTotpPage;

    private TimeBasedOTP totp = new TimeBasedOTP();

    private int lifespan;

    @Before
    public void before() throws MalformedURLException {
        totp = new TimeBasedOTP();
    }

    @Test
    public void loginWithTotpFailure() throws Exception {
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        Assert.assertTrue(loginTotpPage.isCurrent());

        loginTotpPage.login("123456");
        loginTotpPage.assertCurrent();
        Assert.assertEquals("Invalid authenticator code.", loginTotpPage.getInputError());

        //loginPage.assertCurrent();  // Invalid authenticator code.
        //Assert.assertEquals("Invalid username or password.", loginPage.getError());

        events.expectLogin().error("invalid_user_credentials").session((String) null)
                .removeDetail(Details.CONSENT)
                .assertEvent();
    }

    @Test
    public void loginWithMissingTotp() throws Exception {
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        Assert.assertTrue(loginTotpPage.isCurrent());

        loginTotpPage.login(null);
        loginTotpPage.assertCurrent();
        Assert.assertEquals("Invalid authenticator code.", loginTotpPage.getInputError());

        //loginPage.assertCurrent();  // Invalid authenticator code.
        //Assert.assertEquals("Invalid username or password.", loginPage.getError());

        events.expectLogin().error("invalid_user_credentials").session((String) null)
                .removeDetail(Details.CONSENT)
                .assertEvent();
    }

    @Test
    public void loginWithTotpSuccess() throws Exception {
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        Assert.assertTrue(loginTotpPage.isCurrent());

        loginTotpPage.login(totp.generateTOTP("totpSecret"));

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        events.expectLogin().assertEvent();
    }

    // KEYCLOAK-3835
    @Test
    public void loginWithTotpRefreshTotpPage() throws Exception {
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        Assert.assertTrue(loginTotpPage.isCurrent());

        // Refresh TOTP page
        driver.navigate().refresh();

        System.out.println(driver.getPageSource());

        loginTotpPage.login(totp.generateTOTP("totpSecret"));

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        events.expectLogin().assertEvent();
    }

    @Test
    public void loginWithTotpInvalidPassword() throws Exception {
        loginPage.open();
        loginPage.login("test-user@localhost", "invalid");

        Assert.assertTrue(loginPage.isCurrent());

        Assert.assertEquals("Invalid username or password.", loginPage.getInputError());

        events.expectLogin().error("invalid_user_credentials").session((String) null)
                .removeDetail(Details.CONSENT)
                .assertEvent();
    }


    @Test
    public void loginWithTotp_testAttemptedUsernameAndResetLogin() throws Exception {
        loginPage.open();

        // Assert attempted-username NOT available
        loginPage.assertAttemptedUsernameAvailability(false);

        loginPage.login("test-user@localhost", "password");

        Assert.assertTrue(loginTotpPage.isCurrent());

        // Assert attempted-username available
        loginPage.assertAttemptedUsernameAvailability(true);
        Assert.assertEquals("test-user@localhost", loginPage.getAttemptedUsername());

        // Reset login and assert back on the login screen
        loginTotpPage.clickResetLogin();

        loginPage.assertCurrent();
    }

    //KEYCLOAK-12908
    @Test
    public void loginWithTotp_getToken_checkCompatibilityCLI() throws IOException {
        Client httpClient = AdminClientUtil.createResteasyClient();
        try {
            WebTarget exchangeUrl = httpClient.target(OAuthClient.AUTH_SERVER_ROOT)
                    .path("/realms")
                    .path(TEST)
                    .path("protocol/openid-connect/token");

            Form form = new Form()
                    .param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.PASSWORD)
                    .param(OAuth2Constants.USERNAME, "test-user@localhost")
                    .param(OAuth2Constants.PASSWORD, "password")
                    .param(OAuth2Constants.CLIENT_ID, Constants.ADMIN_CLI_CLIENT_ID);

            // Compatibility between "otp" and "totp"
            Response response = exchangeUrl.request()
                    .post(Entity.form(form.param("otp", totp.generateTOTP("totpSecret"))));

            Assert.assertEquals(200, response.getStatus());
            response.close();

            response = exchangeUrl.request()
                    .post(Entity.form(form.param("totp", totp.generateTOTP("totpSecret"))));

            Assert.assertEquals(200, response.getStatus());
            response.close();

        } finally {
            httpClient.close();
        }
    }
}

/*
 * Copyright 2017 Analytical Graphics, Inc. and/or its affiliates
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

package org.keycloak.testsuite.x509;

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.util.PhantomJSBrowser;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel;
import org.keycloak.events.Details;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.pages.AppPage;

import javax.ws.rs.core.Response;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel.IdentityMapperType.USERNAME_EMAIL;
import static org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel.IdentityMapperType.USER_ATTRIBUTE;
import static org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel.MappingSourceType.ISSUERDN_CN;
import static org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel.MappingSourceType.ISSUERDN_EMAIL;
import static org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel.MappingSourceType.SERIALNUMBER;
import static org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel.MappingSourceType.SUBJECTDN;
import static org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel.MappingSourceType.SUBJECTDN_EMAIL;
import org.keycloak.testsuite.ProfileAssume;
import org.keycloak.testsuite.util.DroneUtils;
import org.openqa.selenium.WebDriver;

/**
 * @author <a href="mailto:brat000012001@gmail.com">Peter Nalyvayko</a>
 * @version $Revision: 1 $
 * @date 8/12/2016
 */

public class X509BrowserLoginTest extends AbstractX509AuthenticationTest {


    @Drone
    @PhantomJSBrowser
    private WebDriver phantomJS;


    @Before
    public void replaceTheDefaultDriver() {
        replaceDefaultWebDriver(phantomJS);
    }


    @Test
    public void loginAsUserFromCertSubjectEmail() throws Exception {
        // Login using an e-mail extracted from certificate's subject DN
        x509BrowserLogin(createLoginSubjectEmail2UsernameOrEmailConfig(), userId, "test-user@localhost", "test-user@localhost");
    }

    @Test
    public void loginWithNonMatchingRegex() throws Exception {
        X509AuthenticatorConfigModel config = createLoginIssuerDN_OU2CustomAttributeConfig();
        config.setRegularExpression("INVALID=(.*?)(?:,|$)");
        AuthenticatorConfigRepresentation cfg = newConfig("x509-browser-config", config.getConfig());

        String cfgId = createConfig(browserExecution.getId(), cfg);
        Assert.assertNotNull(cfgId);

        loginConfirmationPage.open();

        events.expectLogin()
                .user((String) null)
                .session((String) null)
                .error("invalid_user_credentials")
                .removeDetail(Details.CONSENT)
                .removeDetail(Details.REDIRECT_URI)
                .assertEvent();
    }

    @Test
    public void loginWithNonSupportedCertKeyUsage() throws Exception {
        // Set the X509 authenticator configuration
        AuthenticatorConfigRepresentation cfg = newConfig("x509-browser-config",
                createLoginSubjectEmailWithKeyUsage("dataEncipherment").getConfig());
        String cfgId = createConfig(browserExecution.getId(), cfg);
        Assert.assertNotNull(cfgId);

        loginConfirmationPage.open();

        Assert.assertThat(loginPage.getError(), containsString("Certificate validation's failed.\n" +
                "Key Usage bit 'dataEncipherment' is not set."));
    }

    @Test
    public void loginWithNonSupportedCertExtendedKeyUsage() throws Exception {
        x509BrowserLogin(createLoginSubjectEmailWithExtendedKeyUsage("serverAuth"), userId, "test-user@localhost", "test-user@localhost");
    }

    @Test
    public void loginIgnoreX509IdentityContinueToFormLogin() throws Exception {
        // Set the X509 authenticator configuration
        AuthenticatorConfigRepresentation cfg = newConfig("x509-browser-config", createLoginSubjectEmail2UsernameOrEmailConfig().getConfig());
        String cfgId = createConfig(browserExecution.getId(), cfg);
        Assert.assertNotNull(cfgId);

        loginConfirmationPage.open();

        Assert.assertTrue(loginConfirmationPage.getSubjectDistinguishedNameText().startsWith("EMAILADDRESS=test-user@localhost"));
        Assert.assertEquals("test-user@localhost", loginConfirmationPage.getUsernameText());

        loginConfirmationPage.ignore();
        loginPage.login("test-user@localhost", "password");

        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

         events.expectLogin()
                 .user(userId)
                 .detail(Details.USERNAME, "test-user@localhost")
                 .removeDetail(Details.REDIRECT_URI)
                 .assertEvent();
    }

    @Test
    public void loginAsUserFromCertSubjectCN() {
        // Login using a CN extracted from certificate's subject DN
        x509BrowserLogin(createLoginSubjectCN2UsernameOrEmailConfig(), userId, "test-user@localhost", "test-user@localhost");
    }

    @Test
    public void loginAsUserFromCertIssuerCNMappedToUserAttribute() {
        x509BrowserLogin(createLoginWithSpecifiedSourceTypeToCustomAttributeConfig(ISSUERDN_CN, "x509_issuer_identity"),
                userId2, "keycloak", "Keycloak Intermediate CA");
    }

    @Test
    public void loginAsUserFromCertIssuerDNMappedToUserAttribute() {

        UserRepresentation user = testRealm().users().get(userId2).toRepresentation();
        Assert.assertNotNull(user);

        user.singleAttribute("x509_certificate_identity", "Red Hat");
        this.updateUser(user);

        events.clear();

        x509BrowserLogin(createLoginIssuerDN_OU2CustomAttributeConfig(), userId2, "keycloak", "Red Hat");
    }


    @Test
    public void loginAsUserFromCertIssuerEmailMappedToUserAttribute() {

        UserRepresentation user = testRealm().users().get(userId2).toRepresentation();
        Assert.assertNotNull(user);

        user.singleAttribute("x509_issuer_identity", "contact@keycloak.org");
        this.updateUser(user);

        events.clear();

        x509BrowserLogin(createLoginWithSpecifiedSourceTypeToCustomAttributeConfig(ISSUERDN_EMAIL, "x509_issuer_identity"),
                userId2, "keycloak", "contact@keycloak.org");
    }


    @Test
    public void loginAsUserFromCertSerialNumberMappedToUserAttribute() {

        UserRepresentation user = testRealm().users().get(userId2).toRepresentation();
        Assert.assertNotNull(user);

        user.singleAttribute("x509_serial_number", "4105");
        this.updateUser(user);

        events.clear();

        x509BrowserLogin(createLoginWithSpecifiedSourceTypeToCustomAttributeConfig(SERIALNUMBER, "x509_serial_number"),
                userId2, "keycloak", "4105");
    }


    @Test
    public void loginDuplicateUsersNotAllowed() {

        AuthenticatorConfigRepresentation cfg = newConfig("x509-browser-config", createLoginIssuerDN_OU2CustomAttributeConfig().getConfig());
        String cfgId = createConfig(browserExecution.getId(), cfg);
        Assert.assertNotNull(cfgId);

        // Set up the users so that the identity extracted from X509 client cert
        // matches more than a single user to trigger DuplicateModelException.

        UserRepresentation user = testRealm().users().get(userId2).toRepresentation();
        Assert.assertNotNull(user);

        user.singleAttribute("x509_certificate_identity", "Red Hat");
        this.updateUser(user);

        user = testRealm().users().get(userId).toRepresentation();
        Assert.assertNotNull(user);

        user.singleAttribute("x509_certificate_identity", "Red Hat");
        this.updateUser(user);

        events.clear();

        loginPage.open();

        Assert.assertThat(loginPage.getError(), containsString("X509 certificate authentication's failed."));

        loginPage.login("test-user@localhost", "password");

        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

        events.expectLogin()
                .user(userId)
                .detail(Details.USERNAME, "test-user@localhost")
                .removeDetail(Details.REDIRECT_URI)
                .assertEvent();
    }

    @Test
    public void loginAttemptedNoConfig() {

        loginConfirmationPage.open();
        loginPage.assertCurrent();

        Assert.assertThat(loginPage.getInfoMessage(), containsString("X509 client authentication has not been configured yet"));
        // Continue with form based login
        loginPage.login("test-user@localhost", "password");

        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));
        events.expectLogin()
                .user(userId)
                .detail(Details.USERNAME, "test-user@localhost")
                .removeDetail(Details.REDIRECT_URI)
                .assertEvent();
    }

    @Test
    public void loginWithX509CertCustomAttributeUserNotFound() {
        X509AuthenticatorConfigModel config =
                new X509AuthenticatorConfigModel()
                        .setConfirmationPageAllowed(true)
                        .setMappingSourceType(SUBJECTDN)
                        .setRegularExpression("O=(.*?)(?:,|$)")
                        .setCustomAttributeName("x509_certificate_identity")
                        .setUserIdentityMapperType(USER_ATTRIBUTE);
        AuthenticatorConfigRepresentation cfg = newConfig("x509-browser-config", config.getConfig());
        String cfgId = createConfig(browserExecution.getId(), cfg);
        Assert.assertNotNull(cfgId);

        loginConfirmationPage.open();
        loginPage.assertCurrent();

        // Verify there is an error message
        Assert.assertNotNull(loginPage.getError());

        Assert.assertThat(loginPage.getError(), containsString("X509 certificate authentication's failed."));
        events.expectLogin()
                .user((String) null)
                .session((String) null)
                .error("user_not_found")
                .detail(Details.USERNAME, "Red Hat")
                .removeDetail(Details.CONSENT)
                .removeDetail(Details.REDIRECT_URI)
                .assertEvent();

        // Continue with form based login
        loginPage.login("test-user@localhost", "password");

        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));
        events.expectLogin()
                .user(userId)
                .detail(Details.USERNAME, "test-user@localhost")
                .removeDetail(Details.REDIRECT_URI)
                .assertEvent();
    }

    @Test
    public void loginWithX509CertCustomAttributeSuccess() {
        X509AuthenticatorConfigModel config =
                new X509AuthenticatorConfigModel()
                        .setConfirmationPageAllowed(true)
                        .setMappingSourceType(SUBJECTDN)
                        .setRegularExpression("O=(.*?)(?:,|$)")
                        .setCustomAttributeName("x509_certificate_identity")
                        .setUserIdentityMapperType(USER_ATTRIBUTE);
        AuthenticatorConfigRepresentation cfg = newConfig("x509-browser-config", config.getConfig());
        String cfgId = createConfig(browserExecution.getId(), cfg);
        Assert.assertNotNull(cfgId);

        // Update the attribute used to match the user identity to that
        // extracted from the client certificate
        UserRepresentation user = findUser("test-user@localhost");
        Assert.assertNotNull(user);
        user.singleAttribute("x509_certificate_identity", "Red Hat");
        this.updateUser(user);

        events.clear();

        loginConfirmationPage.open();

        Assert.assertTrue(loginConfirmationPage.getSubjectDistinguishedNameText().startsWith("EMAILADDRESS=test-user@localhost"));
        Assert.assertEquals("test-user@localhost", loginConfirmationPage.getUsernameText());

        loginConfirmationPage.confirm();

        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));
    }

    @Test
    public void loginWithX509CertBadUserOrNotFound() {
        AuthenticatorConfigRepresentation cfg = newConfig("x509-browser-config", createLoginSubjectEmail2UsernameOrEmailConfig().getConfig());
        String cfgId = createConfig(browserExecution.getId(), cfg);
        Assert.assertNotNull(cfgId);

        // Delete user
        UserRepresentation user = findUser("test-user@localhost");
        Assert.assertNotNull(user);

        Response response = testRealm().users().delete(userId);
        assertEquals(204, response.getStatus());
        response.close();
        // TODO causes the test to fail
        //assertAdminEvents.assertEvent(REALM_NAME, OperationType.DELETE, AdminEventPaths.userResourcePath(userId));

        loginConfirmationPage.open();
        loginPage.assertCurrent();

        // Verify there is an error message
        Assert.assertNotNull(loginPage.getError());

        Assert.assertThat(loginPage.getError(), containsString("X509 certificate authentication's failed."));

        AssertEvents.ExpectedEvent expectedEvent = events.expectLogin()
                .user((String) null)
                .session((String) null)
                .error("user_not_found")
                .detail(Details.USERNAME, "test-user@localhost")
                .removeDetail(Details.CONSENT)
                .removeDetail(Details.REDIRECT_URI);

        addX509CertificateDetails(expectedEvent)
                .assertEvent();

        // Continue with form based login
        loginPage.login("test-user@localhost", "password");
        loginPage.assertCurrent();

        Assert.assertEquals("test-user@localhost", loginPage.getUsername());
        Assert.assertEquals("", loginPage.getPassword());

        Assert.assertEquals("Invalid username or password.", loginPage.getError());
    }

    @Test
    public void loginValidCertificateDisabledUser() {
        setUserEnabled("test-user@localhost", false);

        try {
            AuthenticatorConfigRepresentation cfg = newConfig("x509-browser-config", createLoginSubjectEmail2UsernameOrEmailConfig().getConfig());
            String cfgId = createConfig(browserExecution.getId(), cfg);
            Assert.assertNotNull(cfgId);

            loginConfirmationPage.open();
            loginPage.assertCurrent();

            Assert.assertNotNull(loginPage.getError());

            Assert.assertThat(loginPage.getError(), containsString("X509 certificate authentication's failed.\nUser is disabled"));

            events.expectLogin()
                    .user(userId)
                    .session((String) null)
                    .error("user_disabled")
                    .detail(Details.USERNAME, "test-user@localhost")
                    .removeDetail(Details.CONSENT)
                    .removeDetail(Details.REDIRECT_URI)
                    .assertEvent();

            loginPage.login("test-user@localhost", "password");
            loginPage.assertCurrent();

            // KEYCLOAK-1741 - assert form field values kept
            Assert.assertEquals("test-user@localhost", loginPage.getUsername());
            Assert.assertEquals("", loginPage.getPassword());

            // KEYCLOAK-2024
            Assert.assertEquals("Account is disabled, contact admin.", loginPage.getError());

            events.expectLogin()
                    .user(userId)
                    .session((String) null)
                    .error("user_disabled")
                    .detail(Details.USERNAME, "test-user@localhost")
                    .removeDetail(Details.CONSENT)
                    .removeDetail(Details.REDIRECT_URI)
                    .assertEvent();
        } finally {
            setUserEnabled("test-user@localhost", true);
        }
    }


    @Test
    public void loginNoIdentityConfirmationPage() {
        X509AuthenticatorConfigModel config =
                new X509AuthenticatorConfigModel()
                    .setConfirmationPageAllowed(false)
                    .setMappingSourceType(SUBJECTDN_EMAIL)
                    .setUserIdentityMapperType(USERNAME_EMAIL);
        AuthenticatorConfigRepresentation cfg = newConfig("x509-browser-config", config.getConfig());
        String cfgId = createConfig(browserExecution.getId(), cfg);
        Assert.assertNotNull(cfgId);

        oauth.openLoginForm();
        // X509 authenticator extracts the user identity, maps it to an existing
        // user and automatically logs the user in without prompting to confirm
        // the identity.
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

        AssertEvents.ExpectedEvent expectedEvent = events.expectLogin()
                .user(userId)
                .detail(Details.USERNAME, "test-user@localhost")
                .removeDetail(Details.REDIRECT_URI);

        addX509CertificateDetails(expectedEvent)
                .assertEvent();
    }


    // KEYCLOAK-5466
    @Test
    public void loginWithCertificateAddedLater() throws Exception {
        // Start with normal login form
        loginConfirmationPage.open();
        loginPage.assertCurrent();

        Assert.assertThat(loginPage.getInfoMessage(), containsString("X509 client authentication has not been configured yet"));
        loginPage.assertCurrent();

        // Now setup certificate and login with certificate in existing authenticationSession (Not 100% same scenario as KEYCLOAK-5466, but very similar)
        loginAsUserFromCertSubjectEmail();
    }

    // KEYCLOAK-6866
    @Test
    public void changeLocaleOnX509InfoPage() {
        ProfileAssume.assumeCommunity();

        AuthenticatorConfigRepresentation cfg = newConfig("x509-browser-config", createLoginSubjectEmail2UsernameOrEmailConfig().getConfig());
        String cfgId = createConfig(browserExecution.getId(), cfg);
        Assert.assertNotNull(cfgId);

        log.debug("Open confirm page");
        loginConfirmationPage.open();

        log.debug("check if on confirm page");
        Assert.assertThat(loginConfirmationPage.getSubjectDistinguishedNameText(), startsWith("EMAILADDRESS=test-user@localhost"));
        log.debug("check if locale is EN");
        Assert.assertThat(loginConfirmationPage.getLanguageDropdownText(), is(equalTo("English")));

        log.debug("change locale to DE");
        loginConfirmationPage.openLanguage("Deutsch");
        log.debug("check if locale is DE");
        Assert.assertThat(loginConfirmationPage.getLanguageDropdownText(), is(equalTo("Deutsch")));
        Assert.assertThat(DroneUtils.getCurrentDriver().getPageSource(), containsString("X509 Client Zertifikat:"));

        log.debug("confirm cert");
        loginConfirmationPage.confirm();

        log.debug("check if logged in");
        Assert.assertThat(appPage.getRequestType(), is(equalTo(AppPage.RequestType.AUTH_RESPONSE)));
    }
}

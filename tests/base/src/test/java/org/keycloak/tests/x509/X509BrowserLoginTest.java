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

package org.keycloak.tests.x509;

import java.util.Collections;

import jakarta.ws.rs.core.Response;

import org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.client.AbstractMutualTLSClientTest;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel.IdentityMapperType.USERNAME_EMAIL;
import static org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel.IdentityMapperType.USER_ATTRIBUTE;
import static org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel.MappingSourceType.SERIALNUMBER;
import static org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel.MappingSourceType.SERIALNUMBER_ISSUERDN;
import static org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel.MappingSourceType.SHA256_THUMBPRINT;
import static org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel.MappingSourceType.SUBJECTDN;
import static org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel.MappingSourceType.SUBJECTDN_EMAIL;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

/**
 * @author <a href="mailto:brat000012001@gmail.com">Peter Nalyvayko</a>
 * @version $Revision: 1 $
 * @date 8/12/2016
 */
@KeycloakIntegrationTest
public class X509BrowserLoginTest extends AbstractX509AuthenticationTest {

    protected static final Logger log = Logger.getLogger(X509BrowserLoginTest.class);

    @Test
    public void loginAsUserFromCertSubjectEmail() {
        // Login using an e-mail extracted from certificate's subject DN
        x509BrowserLogin(createLoginSubjectEmail2UsernameOrEmailConfig(), x509User.getId(), x509User.getUsername(), x509User.getUsername());
    }

    @Test
    public void loginWithNonMatchingRegex() {
        X509AuthenticatorConfigModel config = createLoginIssuerDN_OU2CustomAttributeConfig();
        config.setRegularExpression("INVALID=(.*?)(?:,|$)");
        AuthenticatorConfigRepresentation cfg = newConfig("x509-browser-config", config.getConfig());

        String cfgId = createConfig(browserExecution.getId(), cfg);
        Assertions.assertNotNull(cfgId);

        oauth.openLoginForm();

        EventAssertion.expectLoginError(events.poll())
                .userId(null)
                .sessionId(null)
                .error(Errors.INVALID_USER_CREDENTIALS)
                .withoutDetails(Details.CONSENT);
    }

    @Test
    public void loginWithNonSupportedCertKeyUsage() {
        // Set the X509 authenticator configuration
        AuthenticatorConfigRepresentation cfg = newConfig("x509-browser-config",
                createLoginSubjectEmailWithKeyUsage("dataEncipherment").getConfig());
        String cfgId = createConfig(browserExecution.getId(), cfg);
        Assertions.assertNotNull(cfgId);

        oauth.openLoginForm();

        assertThat(loginPage.getErrorMessage().get(), containsString("Certificate validation's failed. " +
                "Certificate revoked or incorrect."));
    }

    @Test
    public void loginWithCertExtendedKeyUsage() {
        x509BrowserLogin(createLoginSubjectEmailWithExtendedKeyUsage("1.3.6.1.5.5.7.3.2"), x509User.getId(), x509User.getUsername(), x509User.getUsername());
    }

    @Test
    public void loginWithNonSupportedCertExtendedKeyUsage() {
        AuthenticatorConfigRepresentation cfg = newConfig("x509-browser-config", createLoginSubjectEmailWithExtendedKeyUsage("1.3.6.1.5.5.7.3.1").getConfig());
        String cfgId = createConfig(browserExecution.getId(), cfg);
        Assertions.assertNotNull(cfgId);

        oauth.openLoginForm();
        loginPage.assertCurrent();

        // Verify there is an error message
        Assertions.assertTrue(loginPage.getErrorMessage().isPresent());

        assertThat(loginPage.getErrorMessage().get(), containsString("Certificate validation's failed."));
    }

    @Test
    public void loginWithRevalidateCertEnabledCertIsTrusted() {
        x509BrowserLogin(createLoginSubjectEmailWithRevalidateCert(true), x509User.getId(), x509User.getUsername(), x509User.getUsername());
    }

    @Test
    public void loginWithRevalidateCertEnabledCertIsTrustedAndCASubjectDN() {
        x509BrowserLogin(createLoginSubjectEmailWithRevalidateCert("InvalidCN", "CN=Other", AbstractMutualTLSClientTest.CA_CERTIFICATE_SUBJECT_DN), x509User.getId(), x509User.getUsername(), x509User.getUsername());
    }

    @Test
    public void loginWithRevalidateCertEnabledAndInvalidCASubjectDN() {
        AuthenticatorConfigRepresentation cfg = newConfig("x509-browser-config", createLoginSubjectEmailWithRevalidateCert(
                false, AbstractMutualTLSClientTest.DEFAULT_KEYSTORE_SUBJECT_DN, "CN=Other").getConfig());
        String cfgId = createConfig(browserExecution.getId(), cfg);
        Assertions.assertNotNull(cfgId);

        oauth.openLoginForm();
        loginPage.assertCurrent();

        // Verify there is an error message
        Assertions.assertTrue(loginPage.getErrorMessage().isPresent());

        assertThat(loginPage.getErrorMessage().get(), containsString("Certificate validation's failed."));
    }

    @Test
    public void loginWithRevalidateCertEnabledCertWithIncorrectTruststoreConfig() {
        try {
            // Simulate disabling of Truststore SPI on server
            disableTruststoreSpi();

            AuthenticatorConfigRepresentation cfg = newConfig("x509-browser-config", createLoginSubjectEmailWithRevalidateCert(true).getConfig());
            String cfgId = createConfig(browserExecution.getId(), cfg);
            Assertions.assertNotNull(cfgId);

            oauth.openLoginForm();
            loginPage.assertCurrent();

            // Verify there is an error message
            Assertions.assertTrue(loginPage.getErrorMessage().isPresent());

            assertThat(loginPage.getErrorMessage().get(), containsString("Certificate validation's failed."));
        } finally {
            reenableTruststoreSpi();
        }
    }

    @Test
    public void loginIgnoreX509IdentityContinueToFormLogin() {
        // Set the X509 authenticator configuration
        AuthenticatorConfigRepresentation cfg = newConfig("x509-browser-config", createLoginSubjectEmail2UsernameOrEmailConfig().getConfig());
        String cfgId = createConfig(browserExecution.getId(), cfg);
        Assertions.assertNotNull(cfgId);

        oauth.openLoginForm();

        Assertions.assertTrue(loginConfirmationPage.getSubjectDistinguishedNameText().startsWith("EMAILADDRESS=test-user@localhost"));
        Assertions.assertEquals(x509User.getUsername(), loginConfirmationPage.getUsernameText());

        loginConfirmationPage.ignore();
        loginPage.fillLogin(x509User.getUsername(), x509User.getPassword());
        loginPage.submit();

        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());

         EventAssertion.expectLoginSuccess(events.poll())
                 .userId(x509User.getId())
                 .details(Details.USERNAME, x509User.getUsername());
    }

    @Test
    public void loginAsUserFromCertSubjectCN() {
        // Login using a CN extracted from certificate's subject DN
        x509BrowserLogin(createLoginSubjectCN2UsernameOrEmailConfig(), x509User.getId(), x509User.getUsername(), x509User.getUsername());
    }

    @Test
    public void loginAsUserFromCertSerialnumberAndIssuerDNMappedToUserAttribute()  {
        x509User.updateWithCleanup(user -> user.attribute("x509_certificate_serialnumber", "4105")
                .attribute("x509_issuer_dn", "EMAILADDRESS=contact@keycloak.org, CN=Keycloak Intermediate CA, OU=Keycloak, O=Red Hat, ST=MA, C=US"));
        events.clear();

        x509BrowserLogin(createLoginWithSpecifiedSourceTypeToCustomAttributeConfig(SERIALNUMBER_ISSUERDN, "x509_certificate_serialnumber##x509_issuer_dn"),
                x509User.getId(), x509User.getUsername(), "4105##EMAILADDRESS=contact@keycloak.org, CN=Keycloak Intermediate CA, OU=Keycloak, O=Red Hat, ST=MA, C=US");
    }

    @Test
    public void loginAsUserFromHexCertSerialnumberAndIssuerDNMappedToUserAttribute() {
        x509User.updateWithCleanup(user -> user.attribute("x509_certificate_serialnumber", "1009")
                .attribute("x509_issuer_dn", "EMAILADDRESS=contact@keycloak.org, CN=Keycloak Intermediate CA, OU=Keycloak, O=Red Hat, ST=MA, C=US"));
        events.clear();

        X509AuthenticatorConfigModel config = createLoginWithSpecifiedSourceTypeToCustomAttributeConfig(SERIALNUMBER_ISSUERDN, "x509_certificate_serialnumber##x509_issuer_dn");
        config.setSerialnumberHex(true);
        x509BrowserLogin(config, x509User.getId(), x509User.getUsername(), "1009##EMAILADDRESS=contact@keycloak.org, CN=Keycloak Intermediate CA, OU=Keycloak, O=Red Hat, ST=MA, C=US");
    }

    @Test
    public void loginAsUserFromCertIssuerDNMappedToUserAttribute() {
        x509User.updateWithCleanup(user -> user.attribute("x509_certificate_identity", "Red Hat"));
        events.clear();

        x509BrowserLogin(createLoginIssuerDN_OU2CustomAttributeConfig(), x509User.getId(), x509User.getUsername(), "Red Hat");
    }

    @Test
    public void loginAsUserFromCertSHA256MappedToUserAttribute() {
        x509User.updateWithCleanup(user -> user.attribute("x509_cert_sha256thumbprint", "71237a14c118a90cc8406f14d039ed3431c9065f68e535293ee919d4c33b5e15"));
        events.clear();

        x509BrowserLogin(createLoginWithSpecifiedSourceTypeToCustomAttributeConfig(SHA256_THUMBPRINT, "x509_cert_sha256thumbprint"),
                x509User.getId(), x509User.getUsername(), "71237a14c118a90cc8406f14d039ed3431c9065f68e535293ee919d4c33b5e15");
    }

    @Test
    public void loginAsUserFromCertSerialNumberMappedToUserAttribute() {
        x509User.updateWithCleanup(user -> user.attribute("x509_serial_number", "4105"));
        events.clear();

        x509BrowserLogin(createLoginWithSpecifiedSourceTypeToCustomAttributeConfig(SERIALNUMBER, "x509_serial_number"),
                x509User.getId(), x509User.getUsername(), "4105");
    }

    @Test
    public void loginAsUserFromHexCertSerialNumberMappedToUserAttribute() {
        x509User.updateWithCleanup(user -> user.attribute("x509_serial_number", "1009"));
        events.clear();

        X509AuthenticatorConfigModel config = createLoginWithSpecifiedSourceTypeToCustomAttributeConfig(SERIALNUMBER, "x509_serial_number");
        config.setSerialnumberHex(true);
        x509BrowserLogin(config, x509User.getId(), x509User.getUsername(), "1009");
    }

    @Test
    public void loginDuplicateUsersNotAllowed() {

        AuthenticatorConfigRepresentation cfg = newConfig("x509-browser-config", createLoginIssuerDN_OU2CustomAttributeConfig().getConfig());
        String cfgId = createConfig(browserExecution.getId(), cfg);
        Assertions.assertNotNull(cfgId);

        // Set up the users so that the identity extracted from X509 client cert
        // matches more than a single user to trigger DuplicateModelException.
        x509User.updateWithCleanup(user -> user.attribute("x509_certificate_identity", "Red Hat"));
        try (Response res = managedRealm.admin().users().create(UserBuilder.create("user2")
                .password("password")
                .emailVerified(true)
                .name("User", "Two")
                .email("user2@localhost")
                .attribute("x509_certificate_identity", "Red Hat")
                .build())) {
            Assertions.assertEquals(201, res.getStatus());
            managedRealm.cleanup().add(r -> r.users().delete(ApiUtil.getCreatedId(res)));
        }

        events.clear();

        oauth.openLoginForm();

        assertThat(loginPage.getErrorMessage().get(), containsString("X509 certificate authentication's failed."));

        loginPage.fillLogin(x509User.getUsername(), x509User.getPassword());
        loginPage.submit();

        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());

        EventAssertion.expectLoginSuccess(events.poll())
                .userId(x509User.getId())
                .details(Details.USERNAME, x509User.getUsername());
    }

    @Test
    public void loginAttemptedNoConfig() {
        AuthenticatorConfigRepresentation cfg = newConfig("x509-browser-config", Collections.emptyMap());
        String cfgId = createConfig(browserExecution.getId(), cfg);
        removeConfig(cfgId);

        oauth.openLoginForm();
        loginPage.assertCurrent();

        assertThat(loginPage.getInfoMessage().get(), containsString("X509 client authentication has not been configured yet"));
        // Continue with form based login
        oauth.doLogin(x509User.getUsername(), x509User.getPassword());

        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
        EventAssertion.expectLoginSuccess(events.poll())
                .userId(x509User.getId())
                .details(Details.USERNAME, x509User.getUsername());
    }

    @Test
    public void loginWithX509CertCustomAttributeUserNotFound()  {
        X509AuthenticatorConfigModel config =
                new X509AuthenticatorConfigModel()
                        .setConfirmationPageAllowed(true)
                        .setMappingSourceType(SUBJECTDN)
                        .setRegularExpression("O=(.*?)(?:,|$)")
                        .setCustomAttributeName("x509_certificate_identity")
                        .setUserIdentityMapperType(USER_ATTRIBUTE);
        AuthenticatorConfigRepresentation cfg = newConfig("x509-browser-config", config.getConfig());
        String cfgId = createConfig(browserExecution.getId(), cfg);
        Assertions.assertNotNull(cfgId);

        oauth.openLoginForm();
        loginPage.assertCurrent();

        // Verify there is an error message
        Assertions.assertTrue(loginPage.getErrorMessage().isPresent());

        assertThat(loginPage.getErrorMessage().get(), containsString("X509 certificate authentication's failed."));
        EventAssertion.expectLoginError(events.poll())
                .userId(null)
                .sessionId(null)
                .error(Errors.USER_NOT_FOUND)
                .details(Details.USERNAME, "Red Hat")
                .withoutDetails(Details.CONSENT);

        // Continue with form based login
        loginPage.fillLogin(x509User.getUsername(), x509User.getPassword());
        loginPage.submit();

        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
        EventAssertion.expectLoginSuccess(events.poll())
                .userId(x509User.getId())
                .details(Details.USERNAME, x509User.getUsername());
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
        Assertions.assertNotNull(cfgId);

        // Update the attribute used to match the user identity to that
        // extracted from the client certificate
        x509User.updateWithCleanup(user -> user.attribute("x509_certificate_identity", "Red Hat"));
        events.clear();

        oauth.openLoginForm();

        Assertions.assertTrue(loginConfirmationPage.getSubjectDistinguishedNameText().startsWith("EMAILADDRESS=test-user@localhost"));
        Assertions.assertEquals(x509User.getUsername(), loginConfirmationPage.getUsernameText());

        loginConfirmationPage.confirm();

        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
    }

    @Test
    public void loginWithX509CertBadUserOrNotFound() {
        AuthenticatorConfigRepresentation cfg = newConfig("x509-browser-config", createLoginSubjectEmail2UsernameOrEmailConfig().getConfig());
        String cfgId = createConfig(browserExecution.getId(), cfg);
        Assertions.assertNotNull(cfgId);

        // Delete user
        x509User.admin().remove();
        // TODO causes the test to fail
        //assertAdminEvents.assertEvent(REALM_NAME, OperationType.DELETE, AdminEventPaths.userResourcePath(userId));

        oauth.openLoginForm();
        loginPage.assertCurrent();

        // Verify there is an error message
        Assertions.assertTrue(loginPage.getErrorMessage().isPresent());

        assertThat(loginPage.getErrorMessage().get(), containsString("X509 certificate authentication's failed."));

        EventRepresentation eventRep = EventAssertion.expectLoginError(events.poll())
                .userId(null)
                .sessionId(null)
                .error(Errors.USER_NOT_FOUND)
                .details(Details.USERNAME, x509User.getUsername())
                .withoutDetails(Details.CONSENT).getEvent();

        MatcherAssert.assertThat(eventRep.getDetails().get(Details.X509_CERTIFICATE_SERIAL_NUMBER), Matchers.not(is(emptyOrNullString())));
        MatcherAssert.assertThat(eventRep.getDetails().get(Details.X509_CERTIFICATE_SUBJECT_DISTINGUISHED_NAME), Matchers.startsWith("EMAILADDRESS=test-user@localhost"));
        MatcherAssert.assertThat(eventRep.getDetails().get(Details.X509_CERTIFICATE_ISSUER_DISTINGUISHED_NAME), Matchers.startsWith("EMAILADDRESS=contact@keycloak.org"));

        // Continue with form based login
        loginPage.fillLogin(x509User.getUsername(), x509User.getPassword());
        loginPage.submit();
        loginPage.assertCurrent();

        Assertions.assertEquals(x509User.getUsername(), loginPage.getUsername());

        Assertions.assertEquals("Invalid username or password.", loginPage.getUsernameInputError());
    }

    @Test
    public void loginValidCertificateDisabledUser() {
        x509User.updateWithCleanup(user -> user.enabled(false));

        AuthenticatorConfigRepresentation cfg = newConfig("x509-browser-config", createLoginSubjectEmail2UsernameOrEmailConfig().getConfig());
        String cfgId = createConfig(browserExecution.getId(), cfg);
        Assertions.assertNotNull(cfgId);

        oauth.openLoginForm();
        loginPage.assertCurrent();

        Assertions.assertTrue(loginPage.getErrorMessage().isPresent());

        assertThat(loginPage.getErrorMessage().get(), containsString("X509 certificate authentication's failed. User is disabled"));

        EventAssertion.expectLoginError(events.poll())
                .userId(x509User.getId())
                .sessionId(null)
                .error(Errors.USER_DISABLED)
                .details(Details.USERNAME, x509User.getUsername())
                .withoutDetails(Details.CONSENT);

        loginPage.fillLogin(x509User.getUsername(), x509User.getPassword());
        loginPage.submit();
        loginPage.assertCurrent();

        // KEYCLOAK-1741 - assert form field values kept
        Assertions.assertEquals(x509User.getUsername(), loginPage.getUsername());

        // KEYCLOAK-2024
        Assertions.assertEquals("Account is disabled, contact your administrator.", loginPage.getErrorMessage().get());

        EventAssertion.expectLoginError(events.poll())
                .userId(x509User.getId())
                .sessionId(null)
                .error(Errors.USER_DISABLED)
                .details(Details.USERNAME, x509User.getUsername())
                .withoutDetails(Details.CONSENT);
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
        Assertions.assertNotNull(cfgId);

        oauth.openLoginForm();
        // X509 authenticator extracts the user identity, maps it to an existing
        // user and automatically logs the user in without prompting to confirm
        // the identity.
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());

        EventRepresentation eventRep = EventAssertion.expectLoginSuccess(events.poll())
                .userId(x509User.getId())
                .details(Details.USERNAME, x509User.getUsername()).getEvent();

        MatcherAssert.assertThat(eventRep.getDetails().get(Details.X509_CERTIFICATE_SERIAL_NUMBER), Matchers.not(is(emptyOrNullString())));
        MatcherAssert.assertThat(eventRep.getDetails().get(Details.X509_CERTIFICATE_SUBJECT_DISTINGUISHED_NAME), Matchers.startsWith("EMAILADDRESS=test-user@localhost"));
        MatcherAssert.assertThat(eventRep.getDetails().get(Details.X509_CERTIFICATE_ISSUER_DISTINGUISHED_NAME), Matchers.startsWith("EMAILADDRESS=contact@keycloak.org"));
    }

    // KEYCLOAK-5466
    @Test
    public void loginWithCertificateAddedLater() throws Exception {
        AuthenticatorConfigRepresentation cfg = newConfig("x509-browser-config", Collections.emptyMap());
        String cfgId = createConfig(browserExecution.getId(), cfg);
        removeConfig(cfgId);

        // Start with normal login form
        oauth.openLoginForm();
        loginPage.assertCurrent();

        assertThat(loginPage.getInfoMessage().get(), containsString("X509 client authentication has not been configured yet"));
        loginPage.assertCurrent();

        // Now setup certificate and login with certificate in existing authenticationSession (Not 100% same scenario as KEYCLOAK-5466, but very similar)
        loginAsUserFromCertSubjectEmail();
    }

    // KEYCLOAK-6866
    @Test
    public void changeLocaleOnX509InfoPage() {
        AuthenticatorConfigRepresentation cfg = newConfig("x509-browser-config", createLoginSubjectEmail2UsernameOrEmailConfig().getConfig());
        String cfgId = createConfig(browserExecution.getId(), cfg);
        Assertions.assertNotNull(cfgId);

        log.debug("Open confirm page");
        oauth.openLoginForm();

        log.debug("check if on confirm page");
        assertThat(loginConfirmationPage.getSubjectDistinguishedNameText(), startsWith("EMAILADDRESS=test-user@localhost"));
        log.debug("check if locale is EN");
        assertThat(loginConfirmationPage.getSelectedLanguage(), is(equalTo("English")));

        log.debug("change locale to DE");
        loginConfirmationPage.selectLanguage("Deutsch");
        log.debug("check if locale is DE");
        assertThat(loginConfirmationPage.getSelectedLanguage(), is(equalTo("Deutsch")));
        assertThat(oauth.getDriver().getPageSource(), containsString("X509 Client Zertifikat:"));

        log.debug("confirm cert");
        loginConfirmationPage.confirm();

        log.debug("check if logged in");
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
    }
}

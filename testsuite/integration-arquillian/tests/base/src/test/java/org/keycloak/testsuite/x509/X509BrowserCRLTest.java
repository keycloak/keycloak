/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.x509;

import org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel;
import org.keycloak.events.Details;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.ContainerAssume;
import org.keycloak.testsuite.util.HtmlUnitBrowser;

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.openqa.selenium.WebDriver;

import static org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel.IdentityMapperType.USERNAME_EMAIL;
import static org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel.MappingSourceType.SUBJECTDN_EMAIL;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class X509BrowserCRLTest extends AbstractX509AuthenticationTest {

    @ClassRule
    public static CRLRule crlRule = new CRLRule();

    @Drone
    @HtmlUnitBrowser
    private WebDriver htmlUnit;


    @Before
    public void replaceTheDefaultDriver() {
        replaceDefaultWebDriver(htmlUnit);
    }


    @Test
    public void loginSuccessWithEmptyRevocationListFromFile() {
        // Not possible to test file CRL on undertow at this moment - jboss config dir doesn't exist
        ContainerAssume.assumeNotAuthServerUndertow();

        X509AuthenticatorConfigModel config =
                new X509AuthenticatorConfigModel()
                        .setCRLEnabled(true)
                        .setCrlAbortIfNonUpdated(true)
                        .setCRLRelativePath(EMPTY_CRL_PATH)
                        .setConfirmationPageAllowed(true)
                        .setMappingSourceType(SUBJECTDN_EMAIL)
                        .setUserIdentityMapperType(USERNAME_EMAIL);
        x509BrowserLogin(config, userId, "test-user@localhost", "test-user@localhost");
    }

    @Test
    public void loginFailureWithEmptyRevocationListFromFileButExpired() {
        // Not possible to test file CRL on undertow at this moment - jboss config dir doesn't exist
        ContainerAssume.assumeNotAuthServerUndertow();

        X509AuthenticatorConfigModel config =
                new X509AuthenticatorConfigModel()
                        .setCRLEnabled(true)
                        .setCrlAbortIfNonUpdated(true)
                        .setCRLRelativePath(EMPTY_EXPIRED_CRL_PATH)
                        .setConfirmationPageAllowed(true)
                        .setMappingSourceType(SUBJECTDN_EMAIL)
                        .setUserIdentityMapperType(USERNAME_EMAIL);
        AuthenticatorConfigRepresentation cfg = newConfig("x509-browser-config", config.getConfig());
        String cfgId = createConfig(browserExecution.getId(), cfg);
        Assert.assertNotNull(cfgId);

        assertLoginFailedDueRevokedCertificate();
    }

    @Test
    public void loginFailedWithIntermediateRevocationListFromFile() {
        // Not possible to test file CRL on undertow at this moment - jboss config dir doesn't exist
        ContainerAssume.assumeNotAuthServerUndertow();

        X509AuthenticatorConfigModel config =
                new X509AuthenticatorConfigModel()
                        .setCRLEnabled(true)
                        .setCrlAbortIfNonUpdated(true)
                        .setCRLRelativePath(INTERMEDIATE_CA_CRL_PATH)
                        .setConfirmationPageAllowed(true)
                        .setMappingSourceType(SUBJECTDN_EMAIL)
                        .setUserIdentityMapperType(USERNAME_EMAIL);
        AuthenticatorConfigRepresentation cfg = newConfig("x509-browser-config", config.getConfig());
        String cfgId = createConfig(browserExecution.getId(), cfg);
        Assert.assertNotNull(cfgId);

        assertLoginFailedDueRevokedCertificate();
    }


    @Test
    public void loginSuccessWithEmptyRevocationListFromHttp() {
        X509AuthenticatorConfigModel config =
                new X509AuthenticatorConfigModel()
                        .setCRLEnabled(true)
                        .setCrlAbortIfNonUpdated(true)
                        .setCRLRelativePath(CRLRule.CRL_RESPONDER_ORIGIN + "/" + EMPTY_CRL_PATH)
                        .setConfirmationPageAllowed(true)
                        .setMappingSourceType(SUBJECTDN_EMAIL)
                        .setUserIdentityMapperType(USERNAME_EMAIL);
        x509BrowserLogin(config, userId, "test-user@localhost", "test-user@localhost");
    }

    @Test
    public void loginFailureWithEmptyRevocationListFromHttpButExpired() {
        X509AuthenticatorConfigModel config =
                new X509AuthenticatorConfigModel()
                        .setCRLEnabled(true)
                        .setCrlAbortIfNonUpdated(true)
                        .setCRLRelativePath(CRLRule.CRL_RESPONDER_ORIGIN + "/" + EMPTY_EXPIRED_CRL_PATH)
                        .setConfirmationPageAllowed(true)
                        .setMappingSourceType(SUBJECTDN_EMAIL)
                        .setUserIdentityMapperType(USERNAME_EMAIL);
        AuthenticatorConfigRepresentation cfg = newConfig("x509-browser-config", config.getConfig());
        String cfgId = createConfig(browserExecution.getId(), cfg);
        Assert.assertNotNull(cfgId);

        assertLoginFailedDueRevokedCertificate();
    }

    @Test
    public void loginTestCRLCaching() {
        X509AuthenticatorConfigModel config =
                new X509AuthenticatorConfigModel()
                        .setCRLEnabled(true)
                        .setCRLRelativePath(CRLRule.CRL_RESPONDER_ORIGIN + "/cached-crl")
                        .setConfirmationPageAllowed(true)
                        .setMappingSourceType(SUBJECTDN_EMAIL)
                        .setUserIdentityMapperType(USERNAME_EMAIL);
        AuthenticatorConfigRepresentation cfg = newConfig("x509-browser-config", config.getConfig());
        String cfgId = createConfig(browserExecution.getId(), cfg);
        Assert.assertNotNull(cfgId);

        try {
            // change CRL to the empty but expired, it should login OK
            crlRule.addHandler("cached-crl", EMPTY_EXPIRED_CRL_PATH);
            x509BrowserLogin(config, userId, "test-user@localhost", "test-user@localhost");
            AccountHelper.logout(testRealm(), "test-user@localhost");
            Assert.assertEquals(1, crlRule.getCounter("cached-crl"));

            // change the CRL to the new one but it is cached the min time
            crlRule.setCrlForHandler("cached-crl", INTERMEDIATE_CA_CRL_PATH);
            x509BrowserLogin(config, userId, "test-user@localhost", "test-user@localhost");
            AccountHelper.logout(testRealm(), "test-user@localhost");
            Assert.assertEquals(1, crlRule.getCounter("cached-crl"));

            // wait the min time and it should be refreshed now and fail
            setTimeOffset(10);
            assertLoginFailedDueRevokedCertificate();
            AccountHelper.logout(testRealm(), "test-user@localhost");
            Assert.assertEquals(2, crlRule.getCounter("cached-crl"));

            // now it's cached until next update 50 years
            setTimeOffset(3600);
            assertLoginFailedDueRevokedCertificate();
            AccountHelper.logout(testRealm(), "test-user@localhost");
            Assert.assertEquals(2, crlRule.getCounter("cached-crl"));

            // clear the cache
            testRealm().clearCrlCache();
            assertLoginFailedDueRevokedCertificate();
            AccountHelper.logout(testRealm(), "test-user@localhost");
            Assert.assertEquals(3, crlRule.getCounter("cached-crl"));
        } finally {
            crlRule.removeHandler("cached-crl");
        }
    }

    @Test
    public void loginFailedWithIntermediateRevocationListFromHttp() {
        X509AuthenticatorConfigModel config =
                new X509AuthenticatorConfigModel()
                        .setCRLEnabled(true)
                        .setCrlAbortIfNonUpdated(true)
                        .setCRLRelativePath(CRLRule.CRL_RESPONDER_ORIGIN + "/" + INTERMEDIATE_CA_CRL_PATH)
                        .setConfirmationPageAllowed(true)
                        .setMappingSourceType(SUBJECTDN_EMAIL)
                        .setUserIdentityMapperType(USERNAME_EMAIL);
        AuthenticatorConfigRepresentation cfg = newConfig("x509-browser-config", config.getConfig());
        String cfgId = createConfig(browserExecution.getId(), cfg);
        Assert.assertNotNull(cfgId);

        assertLoginFailedDueRevokedCertificate();
    }


    @Test
    public void loginFailedWithInvalidSignatureCRL() {
        X509AuthenticatorConfigModel config =
                new X509AuthenticatorConfigModel()
                        .setCRLEnabled(true)
                        .setCrlAbortIfNonUpdated(true)
                        .setCRLRelativePath(CRLRule.CRL_RESPONDER_ORIGIN + "/" + INTERMEDIATE_CA_INVALID_SIGNATURE_CRL_PATH)
                        .setConfirmationPageAllowed(true)
                        .setMappingSourceType(SUBJECTDN_EMAIL)
                        .setUserIdentityMapperType(USERNAME_EMAIL);
        AuthenticatorConfigRepresentation cfg = newConfig("x509-browser-config", config.getConfig());
        String cfgId = createConfig(browserExecution.getId(), cfg);
        Assert.assertNotNull(cfgId);

        // Verify there is an error message because of invalid CRL signature
        assertLoginFailedWithExpectedX509Error("Certificate validation's failed.\nCertificate revoked or incorrect.");
    }


    @Test
    public void loginSuccessWithCRLSignedWithIntermediateCA3FromTruststore() {
        X509AuthenticatorConfigModel config =
                new X509AuthenticatorConfigModel()
                        .setCRLEnabled(true)
                        .setCrlAbortIfNonUpdated(false)
                        .setCRLRelativePath(CRLRule.CRL_RESPONDER_ORIGIN + "/" + INTERMEDIATE_CA_3_CRL_PATH)
                        .setConfirmationPageAllowed(true)
                        .setMappingSourceType(SUBJECTDN_EMAIL)
                        .setUserIdentityMapperType(USERNAME_EMAIL);
        AuthenticatorConfigRepresentation cfg = newConfig("x509-browser-config", config.getConfig());
        String cfgId = createConfig(browserExecution.getId(), cfg);
        Assert.assertNotNull(cfgId);

        // Verify there is an error message because of invalid CRL signature
        x509BrowserLogin(config, userId, "test-user@localhost", "test-user@localhost");
    }


    @Test
    public void loginWithMultipleRevocationLists() {
        X509AuthenticatorConfigModel config =
                new X509AuthenticatorConfigModel()
                        .setCRLEnabled(true)
                        .setCrlAbortIfNonUpdated(true)
                        .setCRLRelativePath(CRLRule.CRL_RESPONDER_ORIGIN + "/" + EMPTY_CRL_PATH + Constants.CFG_DELIMITER + CRLRule.CRL_RESPONDER_ORIGIN + "/" + INTERMEDIATE_CA_CRL_PATH)
                        .setConfirmationPageAllowed(true)
                        .setMappingSourceType(SUBJECTDN_EMAIL)
                        .setUserIdentityMapperType(USERNAME_EMAIL);
        AuthenticatorConfigRepresentation cfg = newConfig("x509-browser-config", config.getConfig());
        String cfgId = createConfig(browserExecution.getId(), cfg);
        Assert.assertNotNull(cfgId);

        assertLoginFailedDueRevokedCertificate();
    }

    @Test
    public void loginWithMultipleRevocationListsUsingInvalidCert() {
        // not sure why it is failing on Undertow - works with Quarkus
        ContainerAssume.assumeNotAuthServerUndertow();

        X509AuthenticatorConfigModel config =
                new X509AuthenticatorConfigModel()
                        .setCRLEnabled(true)
                        .setCrlAbortIfNonUpdated(false)
                        .setCRLRelativePath(CRLRule.CRL_RESPONDER_ORIGIN + "/" + INVALID_CRL_PATH)
                        .setConfirmationPageAllowed(true)
                        .setMappingSourceType(SUBJECTDN_EMAIL)
                        .setUserIdentityMapperType(USERNAME_EMAIL);
        AuthenticatorConfigRepresentation cfg = newConfig("x509-browser-config", config.getConfig());
        String cfgId = createConfig(browserExecution.getId(), cfg);
        Assert.assertNotNull(cfgId);

        x509BrowserLogin(config, userId, "test-user@localhost", "test-user@localhost");
    }

    @Test
    public void loginFailedWithRevocationListFromDistributionPoints() {
        X509AuthenticatorConfigModel config =
                new X509AuthenticatorConfigModel()
                        .setCRLEnabled(true)
                        .setCrlAbortIfNonUpdated(true)
                        .setCRLDistributionPointEnabled(true)
                        .setConfirmationPageAllowed(true)
                        .setMappingSourceType(SUBJECTDN_EMAIL)
                        .setUserIdentityMapperType(USERNAME_EMAIL);
        AuthenticatorConfigRepresentation cfg = newConfig("x509-browser-config", config.getConfig());
        String cfgId = createConfig(browserExecution.getId(), cfg);
        Assert.assertNotNull(cfgId);

        assertLoginFailedDueRevokedCertificate();
    }



    private void assertLoginFailedDueRevokedCertificate() {
        assertLoginFailedWithExpectedX509Error("Certificate validation's failed.\nCertificate revoked or incorrect.");
    }

    private void assertLoginFailedWithExpectedX509Error(String expectedError) {
        oauth.openLoginForm();
        loginPage.assertCurrent();

        // Verify there is an error message
        Assert.assertNotNull(loginPage.getError());

        assertThat(loginPage.getError(), containsString(expectedError));

        // Continue with form based login
        loginPage.login("test-user@localhost", "password");

        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.parseLoginResponse().getCode());

        events.expectLogin()
                .user(userId)
                .detail(Details.USERNAME, "test-user@localhost")
                .removeDetail(Details.REDIRECT_URI)
                .assertEvent();
    }
}

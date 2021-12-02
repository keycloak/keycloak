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

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel;
import org.keycloak.events.Details;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.util.ContainerAssume;
import org.keycloak.testsuite.util.PhantomJSBrowser;
import org.keycloak.testsuite.util.WaitUtils;
import org.openqa.selenium.WebDriver;

import static org.hamcrest.Matchers.containsString;
import static org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel.IdentityMapperType.USERNAME_EMAIL;
import static org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel.MappingSourceType.SUBJECTDN_EMAIL;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class X509BrowserCRLTest extends AbstractX509AuthenticationTest {

    @ClassRule
    public static CRLRule crlRule = new CRLRule();

    @Drone
    @PhantomJSBrowser
    private WebDriver phantomJS;


    @Before
    public void replaceTheDefaultDriver() {
        replaceDefaultWebDriver(phantomJS);
    }


    @Test
    public void loginSuccessWithEmptyRevocationListFromFile() {
        // Not possible to test file CRL on undertow at this moment - jboss config dir doesn't exist
        ContainerAssume.assumeNotAuthServerUndertow();

        X509AuthenticatorConfigModel config =
                new X509AuthenticatorConfigModel()
                        .setCRLEnabled(true)
                        .setCRLRelativePath(EMPTY_CRL_PATH)
                        .setConfirmationPageAllowed(true)
                        .setMappingSourceType(SUBJECTDN_EMAIL)
                        .setUserIdentityMapperType(USERNAME_EMAIL);
        x509BrowserLogin(config, userId, "test-user@localhost", "test-user@localhost");
    }


    @Test
    public void loginFailedWithIntermediateRevocationListFromFile() {
        // Not possible to test file CRL on undertow at this moment - jboss config dir doesn't exist
        ContainerAssume.assumeNotAuthServerUndertow();

        X509AuthenticatorConfigModel config =
                new X509AuthenticatorConfigModel()
                        .setCRLEnabled(true)
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
                        .setCRLRelativePath(CRLRule.CRL_RESPONDER_ORIGIN + "/" + EMPTY_CRL_PATH)
                        .setConfirmationPageAllowed(true)
                        .setMappingSourceType(SUBJECTDN_EMAIL)
                        .setUserIdentityMapperType(USERNAME_EMAIL);
        x509BrowserLogin(config, userId, "test-user@localhost", "test-user@localhost");
    }


    @Test
    public void loginFailedWithIntermediateRevocationListFromHttp() {
        X509AuthenticatorConfigModel config =
                new X509AuthenticatorConfigModel()
                        .setCRLEnabled(true)
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
                        .setCRLRelativePath(CRLRule.CRL_RESPONDER_ORIGIN + "/" + INTERMEDIATE_CA_INVALID_SIGNATURE_CRL_PATH)
                        .setConfirmationPageAllowed(true)
                        .setMappingSourceType(SUBJECTDN_EMAIL)
                        .setUserIdentityMapperType(USERNAME_EMAIL);
        AuthenticatorConfigRepresentation cfg = newConfig("x509-browser-config", config.getConfig());
        String cfgId = createConfig(browserExecution.getId(), cfg);
        Assert.assertNotNull(cfgId);

        // Verify there is an error message because of invalid CRL signature
        assertLoginFailedWithExpectedX509Error("Certificate validation's failed.\nSignature length not correct");
    }


    @Test
    public void loginSuccessWithCRLSignedWithIntermediateCA3FromTruststore() {
        X509AuthenticatorConfigModel config =
                new X509AuthenticatorConfigModel()
                        .setCRLEnabled(true)
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
    public void loginFailedWithRevocationListFromDistributionPoints() {
        X509AuthenticatorConfigModel config =
                new X509AuthenticatorConfigModel()
                        .setCRLEnabled(true)
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
        assertLoginFailedWithExpectedX509Error("Certificate validation's failed.\nCertificate has been revoked, certificate's subject:");
    }

    private void assertLoginFailedWithExpectedX509Error(String expectedError) {
        loginConfirmationPage.open();
        loginPage.assertCurrent();

        // Verify there is an error message
        Assert.assertNotNull(loginPage.getError());

        Assert.assertThat(loginPage.getError(), containsString(expectedError));

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
}

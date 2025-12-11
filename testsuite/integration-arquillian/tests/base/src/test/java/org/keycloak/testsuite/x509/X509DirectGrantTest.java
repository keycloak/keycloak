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

import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.util.ContainerAssume;
import org.keycloak.testsuite.util.HtmlUnitBrowser;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.WebDriver;

import static org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel.IdentityMapperType.USERNAME_EMAIL;
import static org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel.IdentityMapperType.USER_ATTRIBUTE;
import static org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel.MappingSourceType.ISSUERDN;
import static org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel.MappingSourceType.SUBJECTDN_EMAIL;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:brat000012001@gmail.com">Peter Nalyvayko</a>
 * @version $Revision: 1 $
 * @since 10/28/2016
 */

public class X509DirectGrantTest extends AbstractX509AuthenticationTest {

    @Drone
    @HtmlUnitBrowser
    private WebDriver htmlUnit;

    @Before
    public void replaceTheDefaultDriver() {
        replaceDefaultWebDriver(htmlUnit);
    }

    @Test
    public void loginFailedOnDuplicateUsers() throws Exception {

        AuthenticatorConfigRepresentation cfg = newConfig("x509-directgrant-config", createLoginIssuerDN_OU2CustomAttributeConfig().getConfig());
        String cfgId = createConfig(directGrantExecution.getId(), cfg);
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

        oauth.client("resource-owner", "secret");
        AccessTokenResponse response = oauth.doPasswordGrantRequest("", "");

        assertEquals(401, response.getStatusCode());
        assertEquals("invalid_request", response.getError());
        assertThat(response.getErrorDescription(), containsString("X509 certificate authentication's failed."));
    }

    @Test
    public void loginFailedOnInvalidUser() throws Exception {

        AuthenticatorConfigRepresentation cfg = newConfig("x509-directgrant-config", createLoginIssuerDN_OU2CustomAttributeConfig().getConfig());
        String cfgId = createConfig(directGrantExecution.getId(), cfg);
        Assert.assertNotNull(cfgId);

        UserRepresentation user = testRealm().users().get(userId2).toRepresentation();
        Assert.assertNotNull(user);

        user.singleAttribute("x509_certificate_identity", "-");
        this.updateUser(user);

        events.clear();

        oauth.client("resource-owner", "secret");
        AccessTokenResponse response = oauth.doPasswordGrantRequest("", "");

        events.expectLogin()
                .user((String) null)
                .session((String) null)
                .error(Errors.INVALID_USER_CREDENTIALS)
                .client("resource-owner")
                .removeDetail(Details.CODE_ID)
                .removeDetail(Details.USERNAME)
                .removeDetail(Details.CONSENT)
                .removeDetail(Details.REDIRECT_URI)
                .assertEvent();

        assertEquals(401, response.getStatusCode());
        assertEquals("invalid_grant", response.getError());
        assertEquals("Invalid user credentials", response.getErrorDescription());
    }

    @Test
    public void loginWithNonSupportedCertKeyUsage() throws Exception {
        // Set the X509 authenticator configuration
        AuthenticatorConfigRepresentation cfg = newConfig("x509-directgrant-config",
                createLoginSubjectEmailWithKeyUsage("dataEncipherment").getConfig());
        String cfgId = createConfig(directGrantExecution.getId(), cfg);
        Assert.assertNotNull(cfgId);

        oauth.client("resource-owner", "secret");
        AccessTokenResponse response = oauth.doPasswordGrantRequest("", "");

        assertEquals(401, response.getStatusCode());
        assertEquals("invalid_request", response.getError());
        assertThat(response.getErrorDescription(), containsString("Key Usage bit 'dataEncipherment' is not set."));
        events.clear();
    }

    @Test
    public void loginWithNonSupportedCertExtendedKeyUsage() throws Exception {
        // Set the X509 authenticator configuration
        AuthenticatorConfigRepresentation cfg = newConfig("x509-directgrant-config",
                createLoginSubjectEmailWithExtendedKeyUsage("serverAuth").getConfig());
        String cfgId = createConfig(directGrantExecution.getId(), cfg);
        Assert.assertNotNull(cfgId);

        oauth.client("resource-owner", "secret");
        AccessTokenResponse response = oauth.doPasswordGrantRequest("", "");

        assertEquals(200, response.getStatusCode());
    }

    @Test
    public void loginWithNonMatchingRegex() throws Exception {
        X509AuthenticatorConfigModel config = createLoginIssuerDN_OU2CustomAttributeConfig();
        config.setRegularExpression("INVALID=(.*?)(?:,|$)");
        AuthenticatorConfigRepresentation cfg = newConfig("x509-directgrant-config", config.getConfig());

        String cfgId = createConfig(directGrantExecution.getId(), cfg);
        Assert.assertNotNull(cfgId);

        oauth.client("resource-owner", "secret");
        AccessTokenResponse response = oauth.doPasswordGrantRequest("", "");

        assertEquals(401, response.getStatusCode());

        AssertEvents.ExpectedEvent expectedEvent = events.expectLogin()
                .user((String) null)
                .session((String) null)
                .error("invalid_user_credentials")
                .client("resource-owner")
                .removeDetail(Details.CODE_ID)
                .removeDetail(Details.CONSENT)
                .removeDetail(Details.REDIRECT_URI);

        addX509CertificateDetails(expectedEvent)
                .assertEvent();
    }

    @Test
    public void loginFailedDisabledUser() throws Exception {
        setUserEnabled("test-user@localhost", false);

        try {
            AuthenticatorConfigRepresentation cfg = newConfig("x509-directgrant-config", createLoginSubjectEmail2UsernameOrEmailConfig().getConfig());
            String cfgId = createConfig(directGrantExecution.getId(), cfg);
            Assert.assertNotNull(cfgId);

            oauth.client("resource-owner", "secret");
            AccessTokenResponse response = oauth.doPasswordGrantRequest("", "");

            events.expectLogin()
                    .user(userId)
                    .session((String) null)
                    .error(Errors.USER_DISABLED)
                    .client("resource-owner")
                    .detail(Details.USERNAME, "test-user@localhost")
                    .removeDetail(Details.CODE_ID)
                    .removeDetail(Details.CONSENT)
                    .removeDetail(Details.REDIRECT_URI)
                    .assertEvent();

            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());
            assertEquals("invalid_grant", response.getError());
            assertEquals("Account disabled", response.getErrorDescription());

        } finally {
            setUserEnabled("test-user@localhost", true);
        }
    }

    @Test
    public void loginCertificateRevoked() throws Exception {
        // Not possible to test file CRL on undertow at this moment - jboss config dir doesn't exist
        ContainerAssume.assumeNotAuthServerUndertow();

        X509AuthenticatorConfigModel config =
                new X509AuthenticatorConfigModel()
                        .setCRLEnabled(true)
                        .setCRLRelativePath(INTERMEDIATE_CA_CRL_PATH)
                        .setConfirmationPageAllowed(true)
                        .setMappingSourceType(SUBJECTDN_EMAIL)
                        .setUserIdentityMapperType(USERNAME_EMAIL);
        AuthenticatorConfigRepresentation cfg = newConfig("x509-directgrant-config", config.getConfig());
        String cfgId = createConfig(directGrantExecution.getId(), cfg);
        Assert.assertNotNull(cfgId);

        oauth.client("resource-owner", "secret");
        AccessTokenResponse response = oauth.doPasswordGrantRequest("", "");

        assertEquals(401, response.getStatusCode());
        assertEquals("invalid_request", response.getError());
        assertThat(response.getErrorDescription(), containsString("Certificate has been revoked, certificate's subject:"));

    }

    @Test
    public void loginCertificateNotExpired() throws Exception {
        X509AuthenticatorConfigModel config =
                new X509AuthenticatorConfigModel()
                    .setCertValidationEnabled(true)
                    .setConfirmationPageAllowed(true)
                    .setMappingSourceType(SUBJECTDN_EMAIL)
                    .setUserIdentityMapperType(USERNAME_EMAIL);
        AuthenticatorConfigRepresentation cfg = newConfig("x509-directgrant-config", config.getConfig());
        String cfgId = createConfig(directGrantExecution.getId(), cfg);
        Assert.assertNotNull(cfgId);

        oauth.client("resource-owner", "secret");
        AccessTokenResponse response = oauth.doPasswordGrantRequest("", "");

        assertEquals(200, response.getStatusCode());
    }

    @Test
    public void loginCertificateExpired() throws Exception {
        X509AuthenticatorConfigModel config =
                new X509AuthenticatorConfigModel()
                    .setCertValidationEnabled(true)
                    .setConfirmationPageAllowed(true)
                    .setMappingSourceType(SUBJECTDN_EMAIL)
                    .setUserIdentityMapperType(USERNAME_EMAIL);
        AuthenticatorConfigRepresentation cfg = newConfig("x509-directgrant-config", config.getConfig());
        String cfgId = createConfig(directGrantExecution.getId(), cfg);
        Assert.assertNotNull(cfgId);

        setTimeOffset(50 * 365 * 24 * 60 * 60);

        oauth.client("resource-owner", "secret");
        AccessTokenResponse response = oauth.doPasswordGrantRequest("", "");

        setTimeOffset(0);

        assertEquals(401, response.getStatusCode());
        assertEquals("invalid_request", response.getError());
        assertThat(response.getErrorDescription(), containsString("has expired on:"));
    }

    private void loginForceTemporaryAccountLock() throws Exception {
        X509AuthenticatorConfigModel config = new X509AuthenticatorConfigModel()
                .setMappingSourceType(ISSUERDN)
                .setRegularExpression("OU=(.*?)(?:,|$)")
                .setUserIdentityMapperType(USER_ATTRIBUTE)
                .setCustomAttributeName("x509_certificate_identity");

        AuthenticatorConfigRepresentation cfg = newConfig("x509-directgrant-config", config.getConfig());
        String cfgId = createConfig(directGrantExecution.getId(), cfg);
        Assert.assertNotNull(cfgId);

        UserRepresentation user = testRealm().users().get(userId).toRepresentation();
        Assert.assertNotNull(user);

        user.singleAttribute("x509_certificate_identity", "-");
        this.updateUser(user);

        events.clear();

        oauth.client("resource-owner", "secret");
        oauth.doPasswordGrantRequest("", "");
        oauth.doPasswordGrantRequest("", "");
        oauth.doPasswordGrantRequest("", "");

        events.clear();
    }


    @Test
    @Ignore
    public void loginFailedTemporarilyDisabledUser() throws Exception {

        loginForceTemporaryAccountLock();

        AuthenticatorConfigRepresentation cfg = newConfig("x509-directgrant-config", createLoginSubjectEmail2UsernameOrEmailConfig().getConfig());
        String cfgId = createConfig(directGrantExecution.getId(), cfg);
        Assert.assertNotNull(cfgId);

        oauth.client("resource-owner", "secret");
        AccessTokenResponse response = oauth.doPasswordGrantRequest("", "");

        events.expectLogin()
                .user(userId)
                .session((String) null)
                .error(Errors.USER_TEMPORARILY_DISABLED)
                .detail(Details.USERNAME, "test-user@localhost")
                .removeDetail(Details.CODE_ID)
                .removeDetail(Details.CONSENT)
                .removeDetail(Details.REDIRECT_URI)
                .assertEvent();

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());
        assertEquals("invalid_grant", response.getError());
        assertEquals("Account temporarily disabled", response.getErrorDescription());
    }


    private void doResourceOwnerCredentialsLogin(String clientId, String clientSecret, String login, String password) throws Exception {

        oauth.client(clientId, clientSecret);
        AccessTokenResponse response = oauth.doPasswordGrantRequest( "", "");

        assertEquals(200, response.getStatusCode());

        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        RefreshToken refreshToken = oauth.parseRefreshToken(response.getRefreshToken());

        AssertEvents.ExpectedEvent expectedEvent = events.expectLogin()
                .client(clientId)
                .user(userId)
                .session(accessToken.getSessionState())
                .detail(Details.GRANT_TYPE, OAuth2Constants.PASSWORD)
                .detail(Details.TOKEN_ID, accessToken.getId())
                .detail(Details.REFRESH_TOKEN_ID, refreshToken.getId())
                .detail(Details.USERNAME, login)
                .removeDetail(Details.CODE_ID)
                .removeDetail(Details.REDIRECT_URI)
                .removeDetail(Details.CONSENT);

        addX509CertificateDetails(expectedEvent)
                .assertEvent();
    }

    @Test
    public void loginResourceOwnerCredentialsSuccess() throws Exception {
        X509AuthenticatorConfigModel config =
                new X509AuthenticatorConfigModel()
                        .setMappingSourceType(SUBJECTDN_EMAIL)
                        .setUserIdentityMapperType(USERNAME_EMAIL);
        AuthenticatorConfigRepresentation cfg = newConfig("x509-directgrant-config", config.getConfig());
        String cfgId = createConfig(directGrantExecution.getId(), cfg);
        Assert.assertNotNull(cfgId);

        doResourceOwnerCredentialsLogin("resource-owner", "secret", "test-user@localhost", "");
    }

}

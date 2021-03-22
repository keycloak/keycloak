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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
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
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.PhantomJSBrowser;
import org.openqa.selenium.WebDriver;

import javax.ws.rs.core.Response;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel.IdentityMapperType.USERNAME_EMAIL;
import static org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel.IdentityMapperType.USER_ATTRIBUTE;
import static org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel.MappingSourceType.ISSUERDN;
import static org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel.MappingSourceType.SUBJECTDN_EMAIL;

/**
 * @author <a href="mailto:brat000012001@gmail.com">Peter Nalyvayko</a>
 * @version $Revision: 1 $
 * @since 10/28/2016
 */

public class X509DirectGrantTest extends AbstractX509AuthenticationTest {

    @Drone
    @PhantomJSBrowser
    private WebDriver phantomJS;

    @Before
    public void replaceTheDefaultDriver() {
        replaceDefaultWebDriver(phantomJS);
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

        oauth.clientId("resource-owner");
        OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest("secret", "", "", null);

        assertEquals(401, response.getStatusCode());
        assertEquals("invalid_request", response.getError());
        Assert.assertThat(response.getErrorDescription(), containsString("X509 certificate authentication's failed."));
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

        oauth.clientId("resource-owner");
        OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest("secret", "", "", null);

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

        oauth.clientId("resource-owner");
        OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest("secret", "", "", null);

        assertEquals(401, response.getStatusCode());
        assertEquals("invalid_request", response.getError());
        Assert.assertThat(response.getErrorDescription(), containsString("Key Usage bit 'dataEncipherment' is not set."));
        events.clear();
    }

    @Test
    public void loginWithNonSupportedCertExtendedKeyUsage() throws Exception {
        // Set the X509 authenticator configuration
        AuthenticatorConfigRepresentation cfg = newConfig("x509-directgrant-config",
                createLoginSubjectEmailWithExtendedKeyUsage("serverAuth").getConfig());
        String cfgId = createConfig(directGrantExecution.getId(), cfg);
        Assert.assertNotNull(cfgId);

        oauth.clientId("resource-owner");
        OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest("secret", "", "", null);

        assertEquals(200, response.getStatusCode());
    }

    @Test
    public void loginWithNonMatchingRegex() throws Exception {
        X509AuthenticatorConfigModel config = createLoginIssuerDN_OU2CustomAttributeConfig();
        config.setRegularExpression("INVALID=(.*?)(?:,|$)");
        AuthenticatorConfigRepresentation cfg = newConfig("x509-directgrant-config", config.getConfig());

        String cfgId = createConfig(directGrantExecution.getId(), cfg);
        Assert.assertNotNull(cfgId);

        oauth.clientId("resource-owner");
        OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest("secret", "", "", null);

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

            oauth.clientId("resource-owner");
            OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest("secret", "", "", null);

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
        // Not possible to test file CRL on undertow at this moment - jboss config dir doesn't exists
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

        oauth.clientId("resource-owner");
        OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest("secret", "", "", null);

        assertEquals(401, response.getStatusCode());
        assertEquals("invalid_request", response.getError());
        Assert.assertThat(response.getErrorDescription(), containsString("Certificate has been revoked, certificate's subject:"));

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

        oauth.clientId("resource-owner");
        oauth.doGrantAccessTokenRequest("secret", "", "", null);
        oauth.doGrantAccessTokenRequest("secret", "", "", null);
        oauth.doGrantAccessTokenRequest("secret", "", "", null);

        events.clear();
    }


    @Test
    @Ignore
    public void loginFailedTemporarilyDisabledUser() throws Exception {

        loginForceTemporaryAccountLock();

        AuthenticatorConfigRepresentation cfg = newConfig("x509-directgrant-config", createLoginSubjectEmail2UsernameOrEmailConfig().getConfig());
        String cfgId = createConfig(directGrantExecution.getId(), cfg);
        Assert.assertNotNull(cfgId);

        oauth.clientId("resource-owner");
        OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest("secret", "", "", null);

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

        oauth.clientId(clientId);
        OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest(clientSecret, "", "", null);

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

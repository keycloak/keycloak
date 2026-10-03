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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.client.AbstractMutualTLSClientTest;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel.IdentityMapperType.USERNAME_EMAIL;
import static org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel.IdentityMapperType.USER_ATTRIBUTE;
import static org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel.MappingSourceType.ISSUERDN;
import static org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel.MappingSourceType.SUBJECTDN_EMAIL;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author <a href="mailto:brat000012001@gmail.com">Peter Nalyvayko</a>
 * @version $Revision: 1 $
 * @since 10/28/2016
 */
@KeycloakIntegrationTest
public class X509DirectGrantTest extends AbstractX509AuthenticationTest {

    @Test
    public void loginFailedOnDuplicateUsers() {

        AuthenticatorConfigRepresentation cfg = newConfig("x509-directgrant-config", createLoginIssuerDN_OU2CustomAttributeConfig().getConfig());
        String cfgId = createConfig(directGrantExecution.getId(), cfg);
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

        AccessTokenResponse response = oauth.doPasswordGrantRequest("", "");

        assertEquals(401, response.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_REQUEST, response.getError());
        assertThat(response.getErrorDescription(), containsString("X509 certificate authentication's failed."));
    }

    @Test
    public void loginFailedOnInvalidUser() {

        AuthenticatorConfigRepresentation cfg = newConfig("x509-directgrant-config", createLoginIssuerDN_OU2CustomAttributeConfig().getConfig());
        String cfgId = createConfig(directGrantExecution.getId(), cfg);
        Assertions.assertNotNull(cfgId);

        x509User.updateWithCleanup(user -> user.attribute("x509_certificate_identity", "-"));
        events.clear();

        AccessTokenResponse response = oauth.doPasswordGrantRequest("", "");

        EventAssertion.assertError(events.poll())
                .type(EventType.LOGIN_ERROR)
                .userId(null)
                .sessionId(null)
                .error(Errors.INVALID_USER_CREDENTIALS)
                .clientId(oauth.getClientId())
                .withoutDetails(Details.CODE_ID)
                .withoutDetails(Details.CONSENT)
                .withoutDetails(Details.REDIRECT_URI);

        assertEquals(400, response.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_GRANT, response.getError());
        assertEquals("Invalid user credentials", response.getErrorDescription());
    }

    @Test
    public void loginWithNonSupportedCertKeyUsage() {
        // Set the X509 authenticator configuration
        AuthenticatorConfigRepresentation cfg = newConfig("x509-directgrant-config",
                createLoginSubjectEmailWithKeyUsage("dataEncipherment").getConfig());
        String cfgId = createConfig(directGrantExecution.getId(), cfg);
        Assertions.assertNotNull(cfgId);

        AccessTokenResponse response = oauth.doPasswordGrantRequest("", "");

        assertEquals(401, response.getStatusCode());
        assertEquals("invalid_request", response.getError());
        assertThat(response.getErrorDescription(), containsString("Key Usage bit 'dataEncipherment' is not set."));
        events.clear();
    }

    @Test
    public void loginWithCertExtendedKeyUsage() {
        // Set the X509 authenticator configuration
        AuthenticatorConfigRepresentation cfg = newConfig("x509-directgrant-config",
                createLoginSubjectEmailWithExtendedKeyUsage("1.3.6.1.5.5.7.3.2").getConfig());
        String cfgId = createConfig(directGrantExecution.getId(), cfg);
        Assertions.assertNotNull(cfgId);

        AccessTokenResponse response = oauth.doPasswordGrantRequest("", "");

        assertEquals(200, response.getStatusCode());
    }

    @Test
    public void loginWithNonSupportedCertExtendedKeyUsage() {
        // Set the X509 authenticator configuration
        AuthenticatorConfigRepresentation cfg = newConfig("x509-directgrant-config",
                createLoginSubjectEmailWithExtendedKeyUsage("1.3.6.1.5.5.7.3.1").getConfig());
        String cfgId = createConfig(directGrantExecution.getId(), cfg);
        Assertions.assertNotNull(cfgId);

        AccessTokenResponse response = oauth.doPasswordGrantRequest("", "");

        assertEquals(401, response.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_REQUEST, response.getError());
        assertThat(response.getErrorDescription(), containsString("Extended Key Usage '1.3.6.1.5.5.7.3.1' is missing."));
        events.clear();
    }

    @Test
    public void loginWithRevalidateCertEnabledCertIsTrusted() {
        // Set the X509 authenticator configuration
        AuthenticatorConfigRepresentation cfg = newConfig("x509-directgrant-config",
                createLoginSubjectEmailWithRevalidateCert(AbstractMutualTLSClientTest.CA_CERTIFICATE_SUBJECT_DN).getConfig());
        String cfgId = createConfig(directGrantExecution.getId(), cfg);
        Assertions.assertNotNull(cfgId);

        AccessTokenResponse response = oauth.doPasswordGrantRequest("", "");

        assertEquals(200, response.getStatusCode());
    }

    @Test
    public void loginWithRevalidateCertEnabledAndInvalidCASubjectDN() {
        // Set the X509 authenticator configuration
        AuthenticatorConfigRepresentation cfg = newConfig("x509-directgrant-config",
                createLoginSubjectEmailWithRevalidateCert(AbstractMutualTLSClientTest.DEFAULT_KEYSTORE_SUBJECT_DN).getConfig());
        String cfgId = createConfig(directGrantExecution.getId(), cfg);
        Assertions.assertNotNull(cfgId);

        AccessTokenResponse response = oauth.doPasswordGrantRequest("", "");

        assertEquals(401, response.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_REQUEST, response.getError());
        assertThat(response.getErrorDescription(), containsString("Invalid trust anchor for the certificate"));
        events.clear();
    }

    @Test
    public void loginWithNonMatchingRegex() {
        X509AuthenticatorConfigModel config = createLoginIssuerDN_OU2CustomAttributeConfig();
        config.setRegularExpression("INVALID=(.*?)(?:,|$)");
        AuthenticatorConfigRepresentation cfg = newConfig("x509-directgrant-config", config.getConfig());

        String cfgId = createConfig(directGrantExecution.getId(), cfg);
        Assertions.assertNotNull(cfgId);

        AccessTokenResponse response = oauth.doPasswordGrantRequest("", "");

        assertEquals(401, response.getStatusCode());

        EventRepresentation eventRep = EventAssertion.assertError(events.poll())
                .type(EventType.LOGIN_ERROR)
                .userId(null)
                .sessionId(null)
                .error(Errors.INVALID_USER_CREDENTIALS)
                .clientId(oauth.getClientId())
                .withoutDetails(Details.CODE_ID)
                .withoutDetails(Details.CONSENT)
                .withoutDetails(Details.REDIRECT_URI).getEvent();

        MatcherAssert.assertThat(eventRep.getDetails().get(Details.X509_CERTIFICATE_SERIAL_NUMBER), Matchers.not(is(emptyOrNullString())));
        MatcherAssert.assertThat(eventRep.getDetails().get(Details.X509_CERTIFICATE_SUBJECT_DISTINGUISHED_NAME), Matchers.startsWith("EMAILADDRESS=test-user@localhost"));
        MatcherAssert.assertThat(eventRep.getDetails().get(Details.X509_CERTIFICATE_ISSUER_DISTINGUISHED_NAME), Matchers.startsWith("EMAILADDRESS=contact@keycloak.org"));
    }

    @Test
    public void loginFailedDisabledUser() {
        x509User.updateWithCleanup(user -> user.enabled(false));

        AuthenticatorConfigRepresentation cfg = newConfig("x509-directgrant-config", createLoginSubjectEmail2UsernameOrEmailConfig().getConfig());
        String cfgId = createConfig(directGrantExecution.getId(), cfg);
        Assertions.assertNotNull(cfgId);

        AccessTokenResponse response = oauth.doPasswordGrantRequest("", "");

        EventAssertion.assertError(events.poll())
                .type(EventType.LOGIN_ERROR)
                .userId(x509User.getId())
                .sessionId(null)
                .error(Errors.USER_DISABLED)
                .clientId(oauth.getClientId())
                .details(Details.USERNAME, x509User.getUsername())
                .withoutDetails(Details.CODE_ID)
                .withoutDetails(Details.CONSENT)
                .withoutDetails(Details.REDIRECT_URI);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_GRANT, response.getError());
        assertEquals("Account disabled", response.getErrorDescription());
    }

    @Test
    public void loginCertificateRevoked() throws Exception {
        // copy the crl file to the conf directory
        Path source = Paths.get(this.getClass().getResource("/" + INTERMEDIATE_CA_CRL_PATH).toURI());
        Path target = Paths.get(runOnServer.fetch(session -> System.getProperty("jboss.server.config.dir"), String.class), INTERMEDIATE_CA_CRL_PATH);
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        X509AuthenticatorConfigModel config =
                new X509AuthenticatorConfigModel()
                        .setCRLEnabled(true)
                        .setCRLRelativePath(INTERMEDIATE_CA_CRL_PATH)
                        .setConfirmationPageAllowed(true)
                        .setMappingSourceType(SUBJECTDN_EMAIL)
                        .setUserIdentityMapperType(USERNAME_EMAIL);
        AuthenticatorConfigRepresentation cfg = newConfig("x509-directgrant-config", config.getConfig());
        String cfgId = createConfig(directGrantExecution.getId(), cfg);
        Assertions.assertNotNull(cfgId);

        AccessTokenResponse response = oauth.doPasswordGrantRequest("", "");

        assertEquals(401, response.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_REQUEST, response.getError());
        assertThat(response.getErrorDescription(), containsString("Certificate has been revoked, certificate's subject:"));
    }

    @Test
    public void loginCertificateNotExpired() {
        X509AuthenticatorConfigModel config =
                new X509AuthenticatorConfigModel()
                    .setCertValidationEnabled(true)
                    .setConfirmationPageAllowed(true)
                    .setMappingSourceType(SUBJECTDN_EMAIL)
                    .setUserIdentityMapperType(USERNAME_EMAIL);
        AuthenticatorConfigRepresentation cfg = newConfig("x509-directgrant-config", config.getConfig());
        String cfgId = createConfig(directGrantExecution.getId(), cfg);
        Assertions.assertNotNull(cfgId);

        AccessTokenResponse response = oauth.doPasswordGrantRequest("", "");

        assertEquals(200, response.getStatusCode());
    }

    @Test
    public void loginCertificateExpired() {
        X509AuthenticatorConfigModel config =
                new X509AuthenticatorConfigModel()
                    .setCertValidationEnabled(true)
                    .setConfirmationPageAllowed(true)
                    .setMappingSourceType(SUBJECTDN_EMAIL)
                    .setUserIdentityMapperType(USERNAME_EMAIL);
        AuthenticatorConfigRepresentation cfg = newConfig("x509-directgrant-config", config.getConfig());
        String cfgId = createConfig(directGrantExecution.getId(), cfg);
        Assertions.assertNotNull(cfgId);

        timeOffSet.set(50 * 365 * 24 * 60 * 60);

        AccessTokenResponse response = oauth.doPasswordGrantRequest("", "");

        assertEquals(401, response.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_REQUEST, response.getError());
        assertThat(response.getErrorDescription(), containsString("has expired on:"));
    }

    private void loginForceTemporaryAccountLock() {
        X509AuthenticatorConfigModel config = new X509AuthenticatorConfigModel()
                .setMappingSourceType(ISSUERDN)
                .setRegularExpression("OU=(.*?)(?:,|$)")
                .setUserIdentityMapperType(USER_ATTRIBUTE)
                .setCustomAttributeName("x509_certificate_identity");

        AuthenticatorConfigRepresentation cfg = newConfig("x509-directgrant-config", config.getConfig());
        String cfgId = createConfig(directGrantExecution.getId(), cfg);
        Assertions.assertNotNull(cfgId);

        x509User.updateWithCleanup(user -> user.attribute("x509_certificate_identity", "-"));
        events.clear();

        oauth.doPasswordGrantRequest("", "");
        oauth.doPasswordGrantRequest("", "");
        oauth.doPasswordGrantRequest("", "");

        events.clear();
    }

    @Test
    @Disabled
    public void loginFailedTemporarilyDisabledUser() {
        loginForceTemporaryAccountLock();

        AuthenticatorConfigRepresentation cfg = newConfig("x509-directgrant-config", createLoginSubjectEmail2UsernameOrEmailConfig().getConfig());
        String cfgId = createConfig(directGrantExecution.getId(), cfg);
        Assertions.assertNotNull(cfgId);

        AccessTokenResponse response = oauth.doPasswordGrantRequest("", "");

        EventAssertion.expectLoginError(events.poll())
                .userId(x509User.getId())
                .sessionId(null)
                .error(Errors.USER_TEMPORARILY_DISABLED)
                .details(Details.USERNAME, x509User.getId())
                .withoutDetails(Details.CODE_ID)
                .withoutDetails(Details.CONSENT)
                .withoutDetails(Details.REDIRECT_URI);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_GRANT, response.getError());
        assertEquals("Account temporarily disabled", response.getErrorDescription());
    }

    private void doResourceOwnerCredentialsLogin() {

        AccessTokenResponse response = oauth.doPasswordGrantRequest("", "");

        assertEquals(200, response.getStatusCode());

        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        RefreshToken refreshToken = oauth.parseRefreshToken(response.getRefreshToken());

        EventRepresentation eventRep = EventAssertion.assertSuccess(events.poll())
                .type(EventType.LOGIN)
                .clientId(oauth.getClientId())
                .userId(x509User.getId())
                .sessionId(accessToken.getSessionId())
                .details(Details.GRANT_TYPE, OAuth2Constants.PASSWORD)
                .details(Details.TOKEN_ID, accessToken.getId())
                .details(Details.REFRESH_TOKEN_ID, refreshToken.getId())
                .details(Details.USERNAME, x509User.getUsername())
                .withoutDetails(Details.CODE_ID)
                .withoutDetails(Details.REDIRECT_URI)
                .withoutDetails(Details.CONSENT).getEvent();

        MatcherAssert.assertThat(eventRep.getDetails().get(Details.X509_CERTIFICATE_SERIAL_NUMBER), Matchers.not(is(emptyOrNullString())));
        MatcherAssert.assertThat(eventRep.getDetails().get(Details.X509_CERTIFICATE_SUBJECT_DISTINGUISHED_NAME), Matchers.startsWith("EMAILADDRESS=test-user@localhost"));
        MatcherAssert.assertThat(eventRep.getDetails().get(Details.X509_CERTIFICATE_ISSUER_DISTINGUISHED_NAME), Matchers.startsWith("EMAILADDRESS=contact@keycloak.org"));
    }

    @Test
    public void loginResourceOwnerCredentialsSuccess() {
        X509AuthenticatorConfigModel config =
                new X509AuthenticatorConfigModel()
                        .setMappingSourceType(SUBJECTDN_EMAIL)
                        .setUserIdentityMapperType(USERNAME_EMAIL);
        AuthenticatorConfigRepresentation cfg = newConfig("x509-directgrant-config", config.getConfig());
        String cfgId = createConfig(directGrantExecution.getId(), cfg);
        Assertions.assertNotNull(cfgId);

        doResourceOwnerCredentialsLogin();
    }
}

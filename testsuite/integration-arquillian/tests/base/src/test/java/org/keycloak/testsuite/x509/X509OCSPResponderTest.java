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

import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

import jakarta.ws.rs.core.Response;

import org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel;
import org.keycloak.common.util.PemUtils;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.testsuite.util.HtmlUnitBrowser;
import org.keycloak.testsuite.util.MutualTLSUtils;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import io.undertow.Undertow;
import io.undertow.server.handlers.BlockingHandler;
import org.apache.commons.io.IOUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.WebDriver;

import static org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel.IdentityMapperType.USERNAME_EMAIL;
import static org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel.MappingSourceType.SUBJECTDN_EMAIL;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;

/**
 * Verifies Certificate revocation using OCSP responder.
 * The tests rely on an OCSP responder service listening
 * for OCSP requests on http://localhost:8888
 * @author <a href="mailto:brat000012001@gmail.com">Peter Nalyvayko</a>
 * @version $Revision: 1 $
 * @since 11/2/2016
 */

public class X509OCSPResponderTest extends AbstractX509AuthenticationTest {

    private static final String OCSP_RESPONDER_HOST = "localhost";

    private static final int OCSP_RESPONDER_PORT = 8888;

    private Undertow ocspResponder;

    @Drone
    @HtmlUnitBrowser
    private WebDriver htmlUnit;

    @Before
    public void replaceTheDefaultDriver() {
        replaceDefaultWebDriver(htmlUnit);
    }

    @Test
    public void loginFailedOnOCSPResponderRevocationCheck() throws Exception {
        X509AuthenticatorConfigModel config =
                new X509AuthenticatorConfigModel()
                        .setOCSPEnabled(true)
                        .setMappingSourceType(SUBJECTDN_EMAIL)
                        .setUserIdentityMapperType(USERNAME_EMAIL);
        AuthenticatorConfigRepresentation cfg = newConfig("x509-directgrant-config", config.getConfig());
        String cfgId = createConfig(directGrantExecution.getId(), cfg);
        Assert.assertNotNull(cfgId);

        oauth.client("resource-owner", "secret");
        AccessTokenResponse response = oauth.doPasswordGrantRequest("", "");

        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatusCode());
        assertEquals("invalid_request", response.getError());

        assertThat(response.getErrorDescription(), containsString("Certificate's been revoked."));
    }

    @Test
    public void loginFailedOnOCSPResponderRevocationCheckWithoutCA() throws Exception {
        X509AuthenticatorConfigModel config =
                new X509AuthenticatorConfigModel()
                        .setOCSPEnabled(true)
                        .setMappingSourceType(SUBJECTDN_EMAIL)
                        .setOCSPResponder("http://" + OCSP_RESPONDER_HOST + ":" + OCSP_RESPONDER_PORT + "/oscp")
                        .setUserIdentityMapperType(USERNAME_EMAIL);
        AuthenticatorConfigRepresentation cfg = newConfig("x509-directgrant-config", config.getConfig());
        String cfgId = createConfig(directGrantExecution.getId(), cfg);
        Assert.assertNotNull(cfgId);

        String keyStorePath = Paths.get(System.getProperty("client.certificate.keystore"))
                .getParent().resolve("client-ca.jks").toString();
        String keyStorePassword = System.getProperty("client.certificate.keystore.passphrase");
        String trustStorePath = System.getProperty("client.truststore");
        String trustStorePassword = System.getProperty("client.truststore.passphrase");
        try (CloseableHttpClient client = MutualTLSUtils.newCloseableHttpClient(keyStorePath, keyStorePassword, trustStorePath, trustStorePassword)) {
            oauth.client("resource-owner", "secret");
            oauth.httpClient().set(client);
            AccessTokenResponse response = oauth.doPasswordGrantRequest("", "");

            assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatusCode());
            assertEquals("invalid_request", response.getError());

            // the ocsp signer cert is issued by the same CA but no OCSP-Signing extension so error
            assertThat(response.getErrorDescription(), containsString("Responder's certificate not valid for signing OCSP responses"));
        } finally {
            oauth.httpClient().reset();
        }
    }

    @Test
    public void loginClientCertSignedByIntermediateCA() throws Exception {
        X509AuthenticatorConfigModel config =
                new X509AuthenticatorConfigModel()
                        .setOCSPEnabled(true)
                        .setMappingSourceType(SUBJECTDN_EMAIL)
                        .setOCSPResponder("http://" + OCSP_RESPONDER_HOST + ":" + OCSP_RESPONDER_PORT + "/oscp")
                        .setUserIdentityMapperType(USERNAME_EMAIL);
        AuthenticatorConfigRepresentation cfg = newConfig("x509-directgrant-config", config.getConfig());
        String cfgId = createConfig(directGrantExecution.getId(), cfg);
        Assert.assertNotNull(cfgId);

        String keyStorePath = Paths.get(System.getProperty("client.certificate.keystore"))
                .getParent().resolve("test-user-cert-intermediary-ca.jks").toString();
        String keyStorePassword = System.getProperty("client.certificate.keystore.passphrase");
        String trustStorePath = System.getProperty("client.truststore");
        String trustStorePassword = System.getProperty("client.truststore.passphrase");
        try (CloseableHttpClient client = MutualTLSUtils.newCloseableHttpClient(keyStorePath, keyStorePassword, trustStorePath, trustStorePassword)) {
            oauth.client("resource-owner", "secret");
            oauth.httpClient().set(client);
            AccessTokenResponse response = oauth.doPasswordGrantRequest("", "");

            // now it's OK because the certificate is fixed
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode());
        } finally {
            oauth.httpClient().reset();
        }
    }

    @Test
    public void loginOKOnOCSPResponderRevocationCheckWithoutCA() throws Exception {
        X509AuthenticatorConfigModel config =
                new X509AuthenticatorConfigModel()
                        .setOCSPEnabled(true)
                        .setMappingSourceType(SUBJECTDN_EMAIL)
                        .setOCSPResponder("http://" + OCSP_RESPONDER_HOST + ":" + OCSP_RESPONDER_PORT + "/oscp")
                        .setOCSPResponderCertificate(
                                IOUtils.toString(this.getClass().getResourceAsStream(OcspHandler.OCSP_RESPONDER_CERT_PATH), StandardCharsets.UTF_8)
                                        .replace(PemUtils.BEGIN_CERT, "")
                                        .replace(PemUtils.END_CERT, ""))
                        .setUserIdentityMapperType(USERNAME_EMAIL);
        AuthenticatorConfigRepresentation cfg = newConfig("x509-directgrant-config", config.getConfig());
        String cfgId = createConfig(directGrantExecution.getId(), cfg);
        Assert.assertNotNull(cfgId);

        String keyStorePath = Paths.get(System.getProperty("client.certificate.keystore"))
                .getParent().resolve("client-ca.jks").toString();
        String keyStorePassword = System.getProperty("client.certificate.keystore.passphrase");
        String trustStorePath = System.getProperty("client.truststore");
        String trustStorePassword = System.getProperty("client.truststore.passphrase");
        try (CloseableHttpClient client = MutualTLSUtils.newCloseableHttpClient(keyStorePath, keyStorePassword, trustStorePath, trustStorePassword)) {
            oauth.client("resource-owner", "secret");
            oauth.httpClient().set(client);
            AccessTokenResponse response = oauth.doPasswordGrantRequest("", "");

            // now it's OK because the certificate is fixed
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode());
        } finally {
            oauth.httpClient().reset();
        }
    }

    @Before
    public void startOCSPResponder() throws Exception {
        ocspResponder = Undertow.builder().addHttpListener(OCSP_RESPONDER_PORT, OCSP_RESPONDER_HOST)
                .setHandler(new BlockingHandler(
                        new OcspHandler(OcspHandler.OCSP_RESPONDER_CERT_PATH, OcspHandler.OCSP_RESPONDER_KEYPAIR_PATH))
                ).build();

        ocspResponder.start();
    }

    @After
    public void stopOCSPResponder() {
        ocspResponder.stop();
    }

}

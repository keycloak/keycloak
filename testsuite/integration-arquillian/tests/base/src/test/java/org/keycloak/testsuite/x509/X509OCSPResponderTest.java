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

import com.google.common.base.Charsets;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.testsuite.util.OAuthClient;

import javax.ws.rs.core.Response;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel.IdentityMapperType.USERNAME_EMAIL;
import static org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel.MappingSourceType.SUBJECTDN_EMAIL;

import io.undertow.Undertow;
import io.undertow.server.handlers.BlockingHandler;
import org.keycloak.testsuite.util.PhantomJSBrowser;
import org.openqa.selenium.WebDriver;
import java.nio.file.Paths;
import java.util.function.Supplier;
import org.apache.commons.io.IOUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.keycloak.testsuite.util.PhantomJSBrowser;
import org.openqa.selenium.WebDriver;

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
    @PhantomJSBrowser
    private WebDriver phantomJS;

    @Before
    public void replaceTheDefaultDriver() {
        replaceDefaultWebDriver(phantomJS);
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

        oauth.clientId("resource-owner");
        OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest("secret", "", "", null);

        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatusCode());
        assertEquals("invalid_request", response.getError());

        Assert.assertThat(response.getErrorDescription(), containsString("Certificate's been revoked."));
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
        Supplier<CloseableHttpClient> previous = oauth.getHttpClient();
        try {
            oauth.clientId("resource-owner");
            oauth.httpClient(() -> OAuthClient.newCloseableHttpClientSSL(keyStorePath, keyStorePassword, trustStorePath, trustStorePassword));
            OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest("secret", "", "", null);

            assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatusCode());
            assertEquals("invalid_request", response.getError());

            // the ocsp signer cert is issued by the same CA but no OCSP-Signing extension so error
            Assert.assertThat(response.getErrorDescription(), containsString("Responder's certificate not valid for signing OCSP responses"));
        } finally {
            oauth.httpClient(previous);
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
                                IOUtils.toString(this.getClass().getResourceAsStream(OcspHandler.OCSP_RESPONDER_CERT_PATH), Charsets.UTF_8)
                                        .replace("-----BEGIN CERTIFICATE-----", "")
                                        .replace("-----END CERTIFICATE-----", ""))
                        .setUserIdentityMapperType(USERNAME_EMAIL);
        AuthenticatorConfigRepresentation cfg = newConfig("x509-directgrant-config", config.getConfig());
        String cfgId = createConfig(directGrantExecution.getId(), cfg);
        Assert.assertNotNull(cfgId);

        String keyStorePath = Paths.get(System.getProperty("client.certificate.keystore"))
                .getParent().resolve("client-ca.jks").toString();
        String keyStorePassword = System.getProperty("client.certificate.keystore.passphrase");
        String trustStorePath = System.getProperty("client.truststore");
        String trustStorePassword = System.getProperty("client.truststore.passphrase");
        Supplier<CloseableHttpClient> previous = oauth.getHttpClient();
        try {
            oauth.clientId("resource-owner");
            oauth.httpClient(() -> OAuthClient.newCloseableHttpClientSSL(keyStorePath, keyStorePassword, trustStorePath, trustStorePassword));
            OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest("secret", "", "", null);

            // now it's OK because the certificate is fixed
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode());
        } finally {
            oauth.httpClient(previous);
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

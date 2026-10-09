package org.keycloak.testsuite.x509;

import java.nio.file.Paths;

import jakarta.ws.rs.core.Response;

import org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.testsuite.arquillian.annotation.SetDefaultProvider;
import org.keycloak.testsuite.util.HtmlUnitBrowser;
import org.keycloak.testsuite.util.MutualTLSUtils;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import io.undertow.Undertow;
import io.undertow.server.handlers.BlockingHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.openqa.selenium.WebDriver;

import static org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel.IdentityMapperType.USERNAME_EMAIL;
import static org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel.MappingSourceType.SUBJECTDN_EMAIL;
import static org.keycloak.testsuite.x509.AbstractX509AuthenticationTest.newConfig;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;

/**
 *
 * @author rmartinc
 */
// set max consumed size to only 10 bytes, so any ocsp or crl access should fail
@SetDefaultProvider(spi = "connections-http-client", providerId = "default", config = {"max-consumed-response-size", "10"})
public class X509MaxConsumedResponseSizeTest extends AbstractX509AuthenticationTest {

    @ClassRule
    public static CRLRule crlRule = new CRLRule();

    private static final String OCSP_RESPONDER_HOST = "localhost";
    private static final int OCSP_RESPONDER_PORT = 8888;

    @Drone
    @HtmlUnitBrowser
    private WebDriver htmlUnit;

    private Undertow ocspResponder;

    @Before
    public void replaceTheDefaultDriver() {
        replaceDefaultWebDriver(htmlUnit);
    }

    @Test
    public void loginFailedWithEmptyRevocationListFromHttp() {
        X509AuthenticatorConfigModel config =
                new X509AuthenticatorConfigModel()
                        .setCRLEnabled(true)
                        .setCrlAbortIfNonUpdated(true)
                        .setCRLRelativePath(CRLRule.CRL_RESPONDER_ORIGIN + "/" + EMPTY_CRL_PATH)
                        .setConfirmationPageAllowed(true)
                        .setMappingSourceType(SUBJECTDN_EMAIL)
                        .setUserIdentityMapperType(USERNAME_EMAIL);
        AuthenticatorConfigRepresentation cfg = newConfig("x509-browser-config", config.getConfig());
        String cfgId = createConfig(browserExecution.getId(), cfg);
        Assert.assertNotNull(cfgId);

        oauth.openLoginForm();
        loginPage.assertCurrent();

        // Verify there is an error message
        Assert.assertNotNull(loginPage.getError());

        assertThat(loginPage.getError(), containsString("Certificate validation's failed. Certificate revoked or incorrect."));
    }

    @Test
    public void loginFailedClientCertSignedByIntermediateCA() throws Exception {
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

            assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatusCode());
            assertEquals("invalid_request", response.getError());

            // the OCSP response exceeds the configured 10-byte limit, so the revocation check fails
            assertThat(response.getErrorDescription(), is("OCSP check failed"));
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

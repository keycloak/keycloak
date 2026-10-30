package org.keycloak.tests.saml;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;

import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.models.BrowserSecurityHeaders;
import org.keycloak.protocol.saml.DefaultSamlArtifactResolver;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.tests.suites.DatabaseTest;

import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Verifies realm browser security headers are present on SP-initiated SAML responses
 * from {@code /realms/{realm}/protocol/saml} (issue #54122).
 */
@KeycloakIntegrationTest
@DatabaseTest
public class SamlSecurityHeadersTest extends AbstractSamlTest {

    @InjectHttpClient(followRedirects = false)
    CloseableHttpClient httpClient;

    @Test
    public void testBrowserSecurityHeadersOnPostBinding() throws Exception {
        // POST binding redirects to the authentication flow URL (back-button protection).
        assertBrowserSecurityHeaders(SamlClient.Binding.POST, true);
    }

    @Test
    public void testBrowserSecurityHeadersOnRedirectBinding() throws Exception {
        // Redirect binding renders the login form directly from /protocol/saml.
        assertBrowserSecurityHeaders(SamlClient.Binding.REDIRECT, false);
    }

    /**
     * Artifact resolution goes through {@code AsyncResponseTransaction}. When resolution fails the transaction
     * rolls back; headers must still be applied to the final resumed error page.
     */
    @Test
    public void testBrowserSecurityHeadersOnArtifactResolutionRollback() throws Exception {
        samlRealm.updateClientWithCleanup(SAML_CLIENT_ID_SALES_POST, builder -> builder
                .attribute(SamlProtocol.SAML_ARTIFACT_RESOLUTION_SERVICE_URL_ATTRIBUTE, "http://127.0.0.1:1/"));

        String artifact = new DefaultSamlArtifactResolver().createArtifact(SAML_CLIENT_ID_SALES_POST);
        URI samlEndpoint = getAuthServerSamlEndpoint(REALM_NAME);
        HttpUriRequest request = SamlClient.Binding.POST.createSamlArtifactRequest(samlEndpoint, null, artifact);

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(equalTo(500)));
            assertHeaders(response, Arrays.asList(BrowserSecurityHeaders.values()));
        }
    }

    private void assertBrowserSecurityHeaders(SamlClient.Binding binding, boolean expectRedirect) throws URISyntaxException,
            ParsingException, ConfigurationException, ProcessingException, IOException {
        AuthnRequestType loginRep = createLoginRequestDocument(SAML_CLIENT_ID_SALES_POST, getSamlAssertionConsumerUrl(), REALM_NAME);
        Document samlRequest = SAML2Request.convert(loginRep);
        URI samlEndpoint = getAuthServerSamlEndpoint(REALM_NAME);

        HttpUriRequest request = binding.createSamlUnsignedRequest(samlEndpoint, null, samlRequest);

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int status = response.getStatusLine().getStatusCode();
            if (expectRedirect) {
                assertThat(status, anyOf(is(302), is(303)));
                assertHeaders(response, BrowserSecurityHeaders.REDIRECT_HEADERS);
            } else {
                assertThat(status, is(equalTo(200)));
                assertHeaders(response, Arrays.asList(BrowserSecurityHeaders.values()));
            }
        }
    }

    private void assertHeaders(CloseableHttpResponse response, Collection<BrowserSecurityHeaders> headers) {
        for (BrowserSecurityHeaders header : headers) {
            Header headerValue = response.getFirstHeader(header.getHeaderName());
            String expectedValue = header.getDefaultValue();
            if (expectedValue.isEmpty()) {
                assertNull(headerValue);
            } else {
                assertNotNull(headerValue, "Missing security header: " + header.getHeaderName());
                assertThat(header.getHeaderName(), headerValue.getValue(), is(equalTo(expectedValue)));
            }
        }
    }
}

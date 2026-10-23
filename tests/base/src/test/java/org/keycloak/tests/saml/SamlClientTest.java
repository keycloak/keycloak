package org.keycloak.tests.saml;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.tests.suites.DatabaseTest;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 *
 * @author mkanis
 */
@KeycloakIntegrationTest
@DatabaseTest
public class SamlClientTest extends AbstractSamlTest {

    @InjectHttpClient(followRedirects = false)
    CloseableHttpClient httpClient;

    @Test
    public void testLoginWithOIDCClient() throws ParsingException, ConfigurationException, ProcessingException, IOException, URISyntaxException {
        samlRealm.updateClientWithCleanup(SAML_CLIENT_ID_SALES_POST, client -> client.protocol(OIDCLoginProtocol.LOGIN_PROTOCOL));
        AuthnRequestType loginRep = createLoginRequestDocument(SAML_CLIENT_ID_SALES_POST, getSamlAssertionConsumerUrl(), REALM_NAME);
        Document samlRequest = SAML2Request.convert(loginRep);
        URI samlEndpoint = getAuthServerSamlEndpoint(REALM_NAME);

        HttpUriRequest post = SamlClient.Binding.POST.createSamlUnsignedRequest(samlEndpoint, null, samlRequest);

        try (CloseableHttpResponse response = httpClient.execute(post)) {
            Assertions.assertEquals(400, response.getStatusLine().getStatusCode());
            String s = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            assertThat(s, Matchers.containsString("Wrong client protocol."));
        }
    }

    @Test
    public void testLoginAcceptsAcsUrlWithOidcNamedParameter() throws ParsingException, ConfigurationException, ProcessingException, IOException, URISyntaxException {
        // Regression test for keycloak/keycloak#53823: a SAML ACS URL that carries a query parameter named
        // after an OIDC response parameter (here: state) used to be rejected with "Invalid redirect uri",
        // because the SAML call sites inherited the OIDC forbidden-parameters check from the fix for #49430.
        // SAML responses never append OIDC parameters, so the ACS URL must not be checked against them.
        AuthnRequestType loginRep = createLoginRequestDocument(SAML_CLIENT_ID_SALES_POST, getSamlAssertionConsumerUrl() + "?state=foo", REALM_NAME);
        Document samlRequest = SAML2Request.convert(loginRep);
        URI samlEndpoint = getAuthServerSamlEndpoint(REALM_NAME);

        HttpUriRequest post = SamlClient.Binding.POST.createSamlUnsignedRequest(samlEndpoint, null, samlRequest);

        try (CloseableHttpResponse response = httpClient.execute(post)) {
            // POST binding redirects to the authentication flow (redirectToAuthentication = true),
            // so a valid AuthnRequest is answered with 302 and not with an error page.
            Assertions.assertEquals(302, response.getStatusLine().getStatusCode(),
                    "SAML AuthnRequest must not be rejected only because its ACS URL contains an OIDC-named query parameter");
        }
    }

    @Test
    public void testLoginStillRejectsAcsUrlOutsideValidRedirects() throws ParsingException, ConfigurationException, ProcessingException, IOException, URISyntaxException {
        // Guard: the ACS URL validation itself must stay intact, a redirect URI not matching the
        // client's configured redirect URIs is still refused.
        AuthnRequestType loginRep = createLoginRequestDocument(SAML_CLIENT_ID_SALES_POST, "https://evil.example.com/acs?state=foo", REALM_NAME);
        Document samlRequest = SAML2Request.convert(loginRep);
        URI samlEndpoint = getAuthServerSamlEndpoint(REALM_NAME);

        HttpUriRequest post = SamlClient.Binding.POST.createSamlUnsignedRequest(samlEndpoint, null, samlRequest);

        try (CloseableHttpResponse response = httpClient.execute(post)) {
            Assertions.assertEquals(400, response.getStatusLine().getStatusCode());
            String s = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            assertThat(s, Matchers.containsString("Invalid redirect uri"));
        }
    }
}

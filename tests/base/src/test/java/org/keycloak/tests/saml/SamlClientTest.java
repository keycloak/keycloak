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
import org.keycloak.tests.utils.SamlClient;

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
}

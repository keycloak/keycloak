package org.keycloak.testsuite.saml;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.SamlClient;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 *
 * @author mkanis
 */
public class SamlClientTest extends AbstractSamlTest {

    @Test
    public void testLoginWithOIDCClient() throws ParsingException, ConfigurationException, ProcessingException, IOException {
        ClientRepresentation salesRep = adminClient.realm(REALM_NAME).clients().findByClientId(SAML_CLIENT_ID_SALES_POST).get(0);
        adminClient.realm(REALM_NAME).clients().get(salesRep.getId()).update(ClientBuilder.edit(salesRep)
                        .protocol(OIDCLoginProtocol.LOGIN_PROTOCOL).build());

        AuthnRequestType loginRep = createLoginRequestDocument(SAML_CLIENT_ID_SALES_POST, SAML_ASSERTION_CONSUMER_URL_SALES_POST, REALM_NAME);
        Document samlRequest = SAML2Request.convert(loginRep);

        SamlClient.RedirectStrategyWithSwitchableFollowRedirect strategy = new SamlClient.RedirectStrategyWithSwitchableFollowRedirect();
        URI samlEndpoint = getAuthServerSamlEndpoint(REALM_NAME);

        try (CloseableHttpClient client = HttpClientBuilder.create().setRedirectStrategy(strategy).build()) {
            HttpUriRequest post = SamlClient.Binding.POST.createSamlUnsignedRequest(samlEndpoint, null, samlRequest);
            CloseableHttpResponse response = sendPost(post, client);
            Assert.assertEquals(response.getStatusLine().getStatusCode(), 400);
            String s = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            assertThat(s, Matchers.containsString("Wrong client protocol."));

            response.close();
        }

        adminClient.realm(REALM_NAME).clients().get(salesRep.getId()).update(ClientBuilder.edit(salesRep)
                .protocol(SamlProtocol.LOGIN_PROTOCOL).build());
    }

    private CloseableHttpResponse sendPost(HttpUriRequest post, final CloseableHttpClient client) {
        CloseableHttpResponse response;
        try {
            HttpClientContext context = HttpClientContext.create();
            response = client.execute(post, context);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        return response;
    }
}

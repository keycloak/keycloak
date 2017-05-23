package org.keycloak.testsuite.saml;

import org.junit.Test;
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.testsuite.util.SamlClient;
import org.keycloak.testsuite.util.SamlClient.Binding;
import org.keycloak.testsuite.util.SamlClient.RedirectStrategyWithSwitchableFollowRedirect;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.hamcrest.Matcher;
import org.w3c.dom.Document;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.keycloak.testsuite.util.IOUtil.documentToString;
import static org.keycloak.testsuite.util.IOUtil.setDocElementAttributeValue;
import static org.keycloak.testsuite.util.Matchers.statusCodeIsHC;
import static org.keycloak.testsuite.util.SamlClient.login;

/**
 * @author mhajas
 */
public class BasicSamlTest extends AbstractSamlTest {

    // KEYCLOAK-4160
    @Test
    public void testPropertyValueInAssertion() throws ParsingException, ConfigurationException, ProcessingException {
        AuthnRequestType loginRep = createLoginRequestDocument(SAML_CLIENT_ID_SALES_POST, SAML_ASSERTION_CONSUMER_URL_SALES_POST, REALM_NAME);

        Document doc = SAML2Request.convert(loginRep);

        setDocElementAttributeValue(doc, "samlp:AuthnRequest", "ID", "${java.version}" );

        SAMLDocumentHolder document = login(bburkeUser, getAuthServerSamlEndpoint(REALM_NAME), doc, null, SamlClient.Binding.POST, SamlClient.Binding.POST);

        assertThat(documentToString(document.getSamlDocument()), not(containsString("InResponseTo=\"" + System.getProperty("java.version") + "\"")));
    }

    @Test
    public void testNoPortInDestination() throws Exception {
        // note that this test relies on settings of the login-protocol.saml.knownProtocols configuration option
        testWithOverriddenPort(-1, Response.Status.OK, containsString("login"));
    }

    @Test
    public void testExplicitPortInDestination() throws Exception {
        testWithOverriddenPort(Integer.valueOf(System.getProperty("auth.server.http.port")), Response.Status.OK, containsString("login"));
    }

    @Test
    public void testWrongPortInDestination() throws Exception {
        testWithOverriddenPort(123, Response.Status.INTERNAL_SERVER_ERROR, containsString("Invalid Request"));
    }

    private void testWithOverriddenPort(int port, Response.Status expectedHttpCode, Matcher<String> pageTextMatcher) throws Exception {
        AuthnRequestType loginRep = SamlClient.createLoginRequestDocument(SAML_CLIENT_ID_SALES_POST, SAML_ASSERTION_CONSUMER_URL_SALES_POST,
          RealmsResource.protocolUrl(UriBuilder.fromUri(getAuthServerRoot()).port(port)).build(REALM_NAME, SamlProtocol.LOGIN_PROTOCOL));

        Document doc = SAML2Request.convert(loginRep);
        HttpUriRequest post = Binding.POST.createSamlUnsignedRequest(getAuthServerSamlEndpoint(REALM_NAME), null, doc);

        try (CloseableHttpClient client = HttpClientBuilder.create().setRedirectStrategy(new RedirectStrategyWithSwitchableFollowRedirect()).build();
          CloseableHttpResponse response = client.execute(post)) {
            assertThat(response, statusCodeIsHC(expectedHttpCode));
            assertThat(EntityUtils.toString(response.getEntity(), "UTF-8"), pageTextMatcher);
        }
    }
}

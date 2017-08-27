package org.keycloak.testsuite.saml;

import org.junit.Test;
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.saml.SignatureAlgorithm;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.saml.processing.web.util.RedirectBindingUtil;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.testsuite.util.KeyUtils;
import org.keycloak.testsuite.util.SamlClient;
import org.keycloak.testsuite.util.SamlClient.Binding;
import org.keycloak.testsuite.util.SamlClient.RedirectStrategyWithSwitchableFollowRedirect;
import org.keycloak.testsuite.util.SamlClientBuilder;
import java.net.URI;
import java.security.Signature;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.hamcrest.Matcher;
import org.jboss.resteasy.util.Encode;
import org.w3c.dom.Document;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.keycloak.testsuite.saml.AbstractSamlTest.REALM_NAME;
import static org.keycloak.testsuite.util.IOUtil.documentToString;
import static org.keycloak.testsuite.util.IOUtil.setDocElementAttributeValue;
import static org.keycloak.testsuite.util.Matchers.statusCodeIsHC;

/**
 * @author mhajas
 */
public class BasicSamlTest extends AbstractSamlTest {

    // KEYCLOAK-4160
    @Test
    public void testPropertyValueInAssertion() throws ParsingException, ConfigurationException, ProcessingException {
        SAMLDocumentHolder document = new SamlClientBuilder()
          .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST, SAML_ASSERTION_CONSUMER_URL_SALES_POST, Binding.POST)
            .transformDocument(doc -> {
                setDocElementAttributeValue(doc, "samlp:AuthnRequest", "ID", "${java.version}" );
                return doc;
            })
            .build()
          .login().user(bburkeUser).build()
          .getSamlResponse(Binding.POST);

        assertThat(documentToString(document.getSamlDocument()), not(containsString("InResponseTo=\"" + System.getProperty("java.version") + "\"")));
    }

    @Test
    public void testRedirectUrlSigned() throws Exception {
        testSpecialCharsInRelayState(null);
    }

    @Test
    public void testRedirectUrlUnencodedSpecialChars() throws Exception {
        testSpecialCharsInRelayState("New%20Document%20(1).doc");
    }

    @Test
    public void testRedirectUrlEncodedSpecialChars() throws Exception {
        testSpecialCharsInRelayState("New%20Document%20%281%29.doc");
    }

    private void testSpecialCharsInRelayState(String encodedRelayState) throws Exception {
        AuthnRequestType loginRep = SamlClient.createLoginRequestDocument(SAML_CLIENT_ID_SALES_POST_SIG, SAML_ASSERTION_CONSUMER_URL_SALES_POST_SIG, getAuthServerSamlEndpoint(REALM_NAME));

        Document doc = SAML2Request.convert(loginRep);
        URI redirect = Binding.REDIRECT.createSamlUnsignedRequest(getAuthServerSamlEndpoint(REALM_NAME), null, doc).getURI();
        String query = redirect.getRawQuery();
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.RSA_SHA256;

        // now add the relayState
        String relayStatePart = encodedRelayState == null
          ? ""
          : ("&" + GeneralConstants.RELAY_STATE + "=" + encodedRelayState);
        String sigAlgPart = "&" + GeneralConstants.SAML_SIG_ALG_REQUEST_KEY + "=" + Encode.encodeQueryParamAsIs(signatureAlgorithm.getXmlSignatureMethod());

        Signature signature = signatureAlgorithm.createSignature();
        byte[] sig;

        signature.initSign(KeyUtils.privateKeyFromString(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY));
        signature.update(query.getBytes(GeneralConstants.SAML_CHARSET));
        signature.update(relayStatePart.getBytes(GeneralConstants.SAML_CHARSET));
        signature.update(sigAlgPart.getBytes(GeneralConstants.SAML_CHARSET));
        sig = signature.sign();

        String encodedSig = RedirectBindingUtil.base64Encode(sig);
        String sigPart = "&" + GeneralConstants.SAML_SIGNATURE_REQUEST_KEY + "=" + Encode.encodeQueryParamAsIs(encodedSig);

        new SamlClientBuilder()
          .navigateTo(redirect.toString() + relayStatePart + sigAlgPart + sigPart)
          .assertResponse(statusCodeIsHC(Status.OK))
          .execute();
    }

    @Test
    public void testNoDestinationPost() throws Exception {
        AuthnRequestType loginRep = SamlClient.createLoginRequestDocument(SAML_CLIENT_ID_SALES_POST, SAML_ASSERTION_CONSUMER_URL_SALES_POST, null);

        Document doc = SAML2Request.convert(loginRep);
        HttpUriRequest post = Binding.POST.createSamlUnsignedRequest(getAuthServerSamlEndpoint(REALM_NAME), null, doc);

        try (CloseableHttpClient client = HttpClientBuilder.create().setRedirectStrategy(new RedirectStrategyWithSwitchableFollowRedirect()).build();
          CloseableHttpResponse response = client.execute(post)) {
            assertThat(response, statusCodeIsHC(Response.Status.OK));
            assertThat(EntityUtils.toString(response.getEntity(), "UTF-8"), containsString("login"));
        }
    }

    @Test
    public void testNoDestinationRedirect() throws Exception {
        AuthnRequestType loginRep = SamlClient.createLoginRequestDocument(SAML_CLIENT_ID_SALES_POST, SAML_ASSERTION_CONSUMER_URL_SALES_POST, null);

        Document doc = SAML2Request.convert(loginRep);
        HttpUriRequest post = Binding.REDIRECT.createSamlUnsignedRequest(getAuthServerSamlEndpoint(REALM_NAME), null, doc);

        try (CloseableHttpClient client = HttpClientBuilder.create().setRedirectStrategy(new RedirectStrategyWithSwitchableFollowRedirect()).build();
          CloseableHttpResponse response = client.execute(post)) {
            assertThat(response, statusCodeIsHC(Response.Status.OK));
            assertThat(EntityUtils.toString(response.getEntity(), "UTF-8"), containsString("login"));
        }
    }

    @Test
    public void testNoDestinationSignedPost() throws Exception {
        AuthnRequestType loginRep = SamlClient.createLoginRequestDocument(SAML_CLIENT_ID_SALES_POST_SIG, SAML_ASSERTION_CONSUMER_URL_SALES_POST_SIG, null);

        Document doc = SAML2Request.convert(loginRep);
        HttpUriRequest post = Binding.POST.createSamlSignedRequest(getAuthServerSamlEndpoint(REALM_NAME), null, doc, SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY, SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY);

        try (CloseableHttpClient client = HttpClientBuilder.create().setRedirectStrategy(new RedirectStrategyWithSwitchableFollowRedirect()).build();
          CloseableHttpResponse response = client.execute(post)) {
            assertThat(response, statusCodeIsHC(Response.Status.INTERNAL_SERVER_ERROR));
        }
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

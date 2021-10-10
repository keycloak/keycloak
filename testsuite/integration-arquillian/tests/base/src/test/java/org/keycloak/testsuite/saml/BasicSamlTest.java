package org.keycloak.testsuite.saml;

import org.junit.Test;
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.saml.SignatureAlgorithm;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.saml.processing.web.util.RedirectBindingUtil;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;
import org.keycloak.testsuite.util.KeyUtils;
import org.keycloak.testsuite.util.Matchers;
import org.keycloak.testsuite.util.SamlClient;
import org.keycloak.testsuite.util.SamlClient.Binding;
import org.keycloak.testsuite.util.SamlClient.RedirectStrategyWithSwitchableFollowRedirect;
import org.keycloak.testsuite.util.SamlClient.Step;
import org.keycloak.testsuite.util.SamlClientBuilder;
import java.io.IOException;
import java.net.URI;
import java.security.Signature;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.keycloak.saml.common.constants.JBossSAMLURIConstants.NAMEID_FORMAT_TRANSIENT;
import static org.keycloak.saml.common.constants.JBossSAMLURIConstants.PROTOCOL_NSURI;
import static org.keycloak.testsuite.util.ServerURLs.AUTH_SERVER_PORT;
import static org.keycloak.testsuite.utils.io.IOUtil.documentToString;
import static org.keycloak.testsuite.utils.io.IOUtil.setDocElementAttributeValue;
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
            assertThat(response, statusCodeIsHC(Status.BAD_REQUEST));
        }
    }

    @Test
    @AuthServerContainerExclude({AuthServer.REMOTE})
    public void testNoPortInDestination() throws Exception {
        // note that this test relies on settings of the login-protocol.saml.knownProtocols configuration option
        testWithOverriddenPort(-1, Response.Status.OK, containsString("login"));
    }

    @Test
    public void testExplicitPortInDestination() throws Exception {
        testWithOverriddenPort(Integer.valueOf(AUTH_SERVER_PORT), Response.Status.OK, containsString("login"));
    }

    @Test
    public void testWrongPortInDestination() throws Exception {
        testWithOverriddenPort(123, Status.BAD_REQUEST, containsString("Invalid Request"));
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

    @Test
    public void testReauthnWithForceAuthnNotSet() throws Exception {
        testReauthnWithForceAuthn(null);
    }

    @Test
    public void testReauthnWithForceAuthnFalse() throws Exception {
        testReauthnWithForceAuthn(false);
    }

    @Test
    public void testReauthnWithForceAuthnTrue() throws Exception {
        testReauthnWithForceAuthn(true);
    }

    private void testReauthnWithForceAuthn(Boolean reloginRequired) throws Exception {
        // Ensure that the first authentication passes
        SamlClient samlClient = new SamlClientBuilder()
          // First authn
          .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST, SAML_ASSERTION_CONSUMER_URL_SALES_POST, Binding.POST)
          .build()

          .login().user(bburkeUser).build()

          .execute(hr -> {
            try {
                SAMLDocumentHolder doc = Binding.POST.extractResponse(hr);
                assertThat(doc.getSamlObject(), Matchers.isSamlStatusResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
            } catch (IOException ex) {
                Logger.getLogger(BasicSamlTest.class.getName()).log(Level.SEVERE, null, ex);
            }
        });

        List<Step> secondAuthn = new SamlClientBuilder()
          // Second authn with forceAuth not set (SSO)
          .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST2, SAML_ASSERTION_CONSUMER_URL_SALES_POST2, Binding.POST)
          .transformObject(so -> {
              so.setForceAuthn(reloginRequired);
              return so;
          })
          .build()

          .assertResponse(Matchers.bodyHC(containsString(
            Objects.equals(reloginRequired, Boolean.TRUE)
              ? "Sign in"
              : GeneralConstants.SAML_RESPONSE_KEY
          )))

          .getSteps();

        samlClient.execute(secondAuthn);
    }

    @Test
    public void testIsPassiveAttributeEmittedWhenTrue() throws Exception {
        // Verifies that the IsPassive attribute is emitted in the authnRequest
        // when it is set to true

        // Build the login request document
        AuthnRequestType loginRep = SamlClient.createLoginRequestDocument(SAML_CLIENT_ID_SALES_POST, SAML_ASSERTION_CONSUMER_URL_SALES_POST, getAuthServerSamlEndpoint(REALM_NAME));
        loginRep.setIsPassive(true);

        Document document = SAML2Request.convert(loginRep);

        // Find the AuthnRequest element
        Element authnRequestElement = document.getDocumentElement();
        Attr isPassiveAttribute = authnRequestElement.getAttributeNode("IsPassive");
        assertThat("AuthnRequest element should contain the IsPassive attribute when isPassive is true, but it doesn't", isPassiveAttribute, notNullValue());
        assertThat("AuthnRequest/IsPassive attribute should be true when isPassive is true, but it isn't", isPassiveAttribute.getNodeValue(), is("true"));
    }

    @Test
    public void testIsPassiveAttributeOmittedWhenFalse() throws Exception {
        // Verifies that the IsPassive attribute is not emitted in the authnRequest
        // when it is set to false

        // Build the login request document
        AuthnRequestType loginRep = SamlClient.createLoginRequestDocument(SAML_CLIENT_ID_SALES_POST, SAML_ASSERTION_CONSUMER_URL_SALES_POST, getAuthServerSamlEndpoint(REALM_NAME));
        loginRep.setIsPassive(false);

        Document document = SAML2Request.convert(loginRep);

        // Find the AuthnRequest element
        Element authnRequestElement = document.getDocumentElement();
        Attr isPassiveAttribute = authnRequestElement.getAttributeNode("IsPassive");
        assertThat("AuthnRequest element shouldn't contain the IsPassive attribute when isPassive is false, but it does", isPassiveAttribute, nullValue());
    }

    @Test
    public void testAllowCreateAttributeOmittedWhenTransient() throws Exception {
        // Verifies that the AllowCreate attribute is not emitted in the AuthnRequest
        // when NameIDFormat is Transient

        // Build the login request document
        AuthnRequestType loginRep = SamlClient.createLoginRequestDocument(SAML_CLIENT_ID_SALES_POST, SAML_ASSERTION_CONSUMER_URL_SALES_POST, getAuthServerSamlEndpoint(REALM_NAME));
        loginRep.getNameIDPolicy().setFormat(NAMEID_FORMAT_TRANSIENT.getUri());
        loginRep.getNameIDPolicy().setAllowCreate(true);

        Document document = SAML2Request.convert(loginRep);

        // Find the AuthnRequest element
        Element authnRequestElement = document.getDocumentElement();
        Element nameIdPolicyElement = DocumentUtil.getDirectChildElement(authnRequestElement, PROTOCOL_NSURI.get(), "NameIDPolicy");

        Attr formatAttribute = nameIdPolicyElement.getAttributeNode("Format");
        Attr allowCreateAttribute = nameIdPolicyElement.getAttributeNode("AllowCreate");
        assertThat("AuthnRequest/NameIdPolicy Format should be present, but it is not", formatAttribute, notNullValue());
        assertThat("AuthnRequest/NameIdPolicy Format should be Transient, but it is not", formatAttribute.getNodeValue(), is(NAMEID_FORMAT_TRANSIENT.get()));
        assertThat("AuthnRequest/NameIdPolicy element shouldn't contain the AllowCreate attribute when Format is set to Transient, but it does", allowCreateAttribute, nullValue());
    }
}

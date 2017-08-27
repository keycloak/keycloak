package org.keycloak.testsuite.saml;

import org.junit.Test;
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.saml.SignatureAlgorithm;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.Base64;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.saml.processing.web.util.RedirectBindingUtil;
import org.keycloak.testsuite.util.KeyUtils;
import org.keycloak.testsuite.util.Matchers;
import org.keycloak.testsuite.util.SamlClient;
import org.keycloak.testsuite.util.SamlClient.Binding;
import java.net.URI;
import java.security.Signature;
import javax.ws.rs.core.Response.Status;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jboss.resteasy.util.Encode;
import org.w3c.dom.Document;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.keycloak.testsuite.util.IOUtil.documentToString;
import static org.keycloak.testsuite.util.IOUtil.setDocElementAttributeValue;
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
        AuthnRequestType loginRep = createLoginRequestDocument(SAML_CLIENT_ID_SALES_POST_SIG, SAML_ASSERTION_CONSUMER_URL_SALES_POST_SIG, REALM_NAME);

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

        String encodedSig = Base64.encodeBytes(sig, Base64.DONT_BREAK_LINES);
        String sigPart = "&" + GeneralConstants.SAML_SIGNATURE_REQUEST_KEY + "=" + Encode.encodeQueryParamAsIs(encodedSig);

        try (CloseableHttpClient c = HttpClientBuilder.create().build();
          CloseableHttpResponse r = c.execute(new HttpGet(redirect.toString() + relayStatePart + sigAlgPart + sigPart))) {
            assertThat(r, Matchers.statusCodeIsHC(Status.OK));
        }
    }

}

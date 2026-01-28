package org.keycloak.testsuite.saml;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriBuilder;

import org.keycloak.adapters.saml.SamlDeployment;
import org.keycloak.common.util.PemUtils;
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.protocol.saml.SamlProtocolUtils;
import org.keycloak.rotation.HardcodedKeyLocator;
import org.keycloak.saml.SignatureAlgorithm;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.saml.processing.core.saml.v2.util.AssertionUtil;
import org.keycloak.saml.processing.web.util.RedirectBindingUtil;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.testsuite.updaters.ClientAttributeUpdater;
import org.keycloak.testsuite.util.KeyUtils;
import org.keycloak.testsuite.util.Matchers;
import org.keycloak.testsuite.util.SamlClient;
import org.keycloak.testsuite.util.SamlClient.Binding;
import org.keycloak.testsuite.util.SamlClient.RedirectStrategyWithSwitchableFollowRedirect;
import org.keycloak.testsuite.util.SamlClient.Step;
import org.keycloak.testsuite.util.SamlClientBuilder;
import org.keycloak.testsuite.util.SamlUtils;
import org.keycloak.utils.StringUtil;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.xml.security.encryption.EncryptedData;
import org.apache.xml.security.encryption.XMLCipher;
import org.apache.xml.security.utils.EncryptionConstants;
import org.hamcrest.Matcher;
import org.jboss.resteasy.util.Encode;
import org.junit.Test;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import static org.keycloak.saml.common.constants.JBossSAMLURIConstants.NAMEID_FORMAT_TRANSIENT;
import static org.keycloak.saml.common.constants.JBossSAMLURIConstants.PROTOCOL_NSURI;
import static org.keycloak.testsuite.util.Matchers.statusCodeIsHC;
import static org.keycloak.testsuite.util.ServerURLs.AUTH_SERVER_PORT;
import static org.keycloak.testsuite.utils.io.IOUtil.documentToString;
import static org.keycloak.testsuite.utils.io.IOUtil.setDocElementAttributeValue;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesRegex;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertTrue;

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
            assertThat(EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8), containsString("login"));
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
            assertThat(EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8), containsString("login"));
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
            assertThat(EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8), pageTextMatcher);
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

    @Test
    public void testSignatureContainsAllowedCharactersOnly() throws IOException {
        try (var c = ClientAttributeUpdater.forClient(adminClient, REALM_NAME, SAML_CLIENT_ID_SALES_POST)
          .setAttribute(SamlConfigAttributes.SAML_SERVER_SIGNATURE, "true")
          .update()
        ) {
            SAMLDocumentHolder documentHolder = new SamlClientBuilder()
            .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST, SAML_ASSERTION_CONSUMER_URL_SALES_POST, Binding.POST).build()
            .login().user(bburkeUser).build()
            .getSamlResponse(Binding.POST);

            final String signature = documentHolder.getSamlDocument()
              .getElementsByTagName("dsig:SignatureValue")
              .item(0).getTextContent();

            // Corresponds to https://www.w3.org/TR/xmlschema-2/#base64Binary
            assertThat(signature, matchesRegex("^[A-Za-z0-9+/ ]+[= ]*$"));
        }
    }

    @Test
    public void testInvalidAssertionConsumerServiceURL() throws IOException {
        try (var c = ClientAttributeUpdater.forClient(adminClient, REALM_NAME, SAML_CLIENT_ID_SALES_POST)
                .setRedirectUris(Collections.singletonList("*"))
                .update()) {

            String page = new SamlClientBuilder()
                    .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST, "javascript:alert('XSS')", Binding.POST)
                    .build()
                    .executeAndTransform(response -> {
                        assertThat(response, statusCodeIsHC(Status.BAD_REQUEST));
                        return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                    });
            assertThat(page, containsString("Invalid redirect uri"));
        }
    }

    @Test
    public void testConsumerServiceURLHtmlEntities() throws IOException {
        try (var c = ClientAttributeUpdater.forClient(adminClient, REALM_NAME, SAML_CLIENT_ID_SALES_POST)
                .setRedirectUris(Collections.singletonList("*"))
                .update()) {

            String action = new SamlClientBuilder()
                    .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST, "javascript&colon;alert('xss');", Binding.POST)
                    .build()
                    .login().user(bburkeUser).build()
                    .executeAndTransform(response -> {
                        assertThat(response, statusCodeIsHC(Response.Status.OK));
                        String responsePage = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                        return SamlClient.extractFormFromPostResponse(responsePage)
                                .attributes().asList().stream()
                                .filter(a -> "action".equalsIgnoreCase(a.getKey()))
                                .map(org.jsoup.nodes.Attribute::getValue)
                                .findAny().orElse(null);
                    });
            // if not encoded properly jsoup returns ":" instead of "&colon;"
            assertThat(action, endsWith("javascript&colon;alert('xss');"));
        }
    }

    private void testEncryption(String algorithm, String keyAlgorithm, String digestMethod, String maskGenerationFunction) throws Exception {
        testEncryption(algorithm, keyAlgorithm, digestMethod, maskGenerationFunction,
                algorithm, keyAlgorithm, digestMethod, maskGenerationFunction);
    }

    private void testEncryption(String algorithm, String keyAlgorithm, String digestMethod, String maskGenerationFunction,
            String expectedAlgorithm, String expectedKeyAlgorithm, String expectedDigestMethod, String expectedMaxskGenerationFuntion) throws Exception {
        try (var c = ClientAttributeUpdater.forClient(adminClient, REALM_NAME, SAML_CLIENT_ID_SALES_POST_ENC)
                .setAttribute(SamlConfigAttributes.SAML_SERVER_SIGNATURE, Boolean.TRUE.toString())
                .setAttribute(SamlConfigAttributes.SAML_ENCRYPT, Boolean.TRUE.toString())
                .setAttribute(SamlConfigAttributes.SAML_ENCRYPTION_ALGORITHM, algorithm)
                .setAttribute(SamlConfigAttributes.SAML_ENCRYPTION_KEY_ALGORITHM, keyAlgorithm)
                .setAttribute(SamlConfigAttributes.SAML_ENCRYPTION_DIGEST_METHOD, digestMethod)
                .setAttribute(SamlConfigAttributes.SAML_ENCRYPTION_MASK_GENERATION_FUNTION, maskGenerationFunction)
                .update()) {
            SAMLDocumentHolder holder = new SamlClientBuilder()
                    .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST_ENC, SAML_ASSERTION_CONSUMER_URL_SALES_POST_ENC, Binding.POST)
                    .signWith(SAML_CLIENT_SALES_POST_ENC_PRIVATE_KEY, SAML_CLIENT_SALES_POST_ENC_PUBLIC_KEY)
                    .build()
                    .login().user(bburkeUser).build()
                    .getSamlResponse(Binding.POST);

            // check it is signed
            SamlProtocolUtils.verifyDocumentSignature(holder.getSamlDocument(), new HardcodedKeyLocator(PemUtils.decodePublicKey(REALM_PUBLIC_KEY)));

            // check document is encrypted
            ResponseType responseType = (ResponseType) holder.getSamlObject();
            assertTrue("Assertion is not encrypted", AssertionUtil.isAssertionEncrypted(responseType));

            SamlDeployment deployment = SamlUtils.getSamlDeploymentForClient("sales-post-enc");
            AssertionUtil.decryptAssertion(responseType, (EncryptedData encryptedData) -> Collections.singletonList(deployment.getDecryptionKey()));

            // check algorithm is the expected one
            NodeList list = holder.getSamlDocument().getElementsByTagNameNS(JBossSAMLURIConstants.XMLENC_NSURI.get(), "EncryptedData");
            assertThat("EncryptedData missing", list.getLength(), is(1));
            Element encryptedData = (Element) list.item(0);
            list = encryptedData.getElementsByTagNameNS(JBossSAMLURIConstants.XMLENC_NSURI.get(), "EncryptionMethod");
            assertThat("EncryptionMethod missing", list.getLength(), is(2));
            Element encryptionMethod = (Element) list.item(0);
            assertThat("Unexpected encryption method", encryptionMethod.getAttribute("Algorithm"), is(expectedAlgorithm));

            // check keyAlgorithm is the expected one
            list = encryptedData.getElementsByTagNameNS(JBossSAMLURIConstants.XMLENC_NSURI.get(), "EncryptedKey");
            assertThat("EncryptedKey missing", list.getLength(), is(1));
            Element encryptedKey = (Element) list.item(0);
            list = encryptedKey.getElementsByTagNameNS(JBossSAMLURIConstants.XMLENC_NSURI.get(), "EncryptionMethod");
            assertThat("EncryptionMethod missing", list.getLength(), is(1));
            encryptionMethod = (Element) list.item(0);
            assertThat("Unexpected key encryption method", encryptionMethod.getAttribute("Algorithm"), is(expectedKeyAlgorithm));

            // check digestMethod is the expected one or missing
            if (StringUtil.isNotBlank(expectedDigestMethod)) {
                list = encryptionMethod.getElementsByTagNameNS(JBossSAMLURIConstants.XMLDSIG_NSURI.get(), "DigestMethod");
                assertThat("EncryptionMethod missing", list.getLength(), is(1));
                Element ds = (Element) list.item(0);
                assertThat("Unexpected digest method", ds.getAttribute("Algorithm"), is(expectedDigestMethod));
            } else {
                assertThat(encryptionMethod.getElementsByTagNameNS(JBossSAMLURIConstants.XMLDSIG_NSURI.get(), "DigestMethod").getLength(), is(0));
            }

            // check mgf is the expected one or missing
            if (StringUtil.isNotBlank(expectedMaxskGenerationFuntion)) {
                list = encryptionMethod.getElementsByTagNameNS(JBossSAMLURIConstants.XMLENC11_NSURI.get(), "MGF");
                assertThat("EncryptionMethod missing", list.getLength(), is(1));
                Element mgf = (Element) list.item(0);
                assertThat("Unexpected mgf method", mgf.getAttribute("Algorithm"), is(expectedMaxskGenerationFuntion));
            } else {
                assertThat(encryptionMethod.getElementsByTagNameNS(JBossSAMLURIConstants.XMLENC11_NSURI.get(), "MGF").getLength(), is(0));
            }
        }
    }

    @Test
    public void testEncryptionDefault() throws Exception {
        testEncryption("", "", "", "", XMLCipher.AES_256_GCM, XMLCipher.RSA_OAEP_11, XMLCipher.SHA256, EncryptionConstants.MGF1_SHA256);
    }

    @Test
    public void testEncryptionRsaOaep11() throws Exception {
        testEncryption(XMLCipher.AES_256_GCM, XMLCipher.RSA_OAEP_11, XMLCipher.SHA512, EncryptionConstants.MGF1_SHA512);
    }

    @Test
    public void testEncryptionRsaOaep11Default() throws Exception {
        testEncryption(XMLCipher.AES_256_GCM, XMLCipher.RSA_OAEP_11, XMLCipher.SHA1, EncryptionConstants.MGF1_SHA1, XMLCipher.AES_256_GCM, XMLCipher.RSA_OAEP_11, "", EncryptionConstants.MGF1_SHA1);
    }

    @Test
    public void testEncryptionRsaOaep() throws Exception {
        testEncryption(XMLCipher.AES_256_GCM, XMLCipher.RSA_OAEP, XMLCipher.SHA256, "", XMLCipher.AES_256_GCM, XMLCipher.RSA_OAEP, XMLCipher.SHA256, EncryptionConstants.MGF1_SHA1);
    }

    @Test
    public void testEncryptionRsaOaepLegacy() throws Exception {
        testEncryption(XMLCipher.AES_128, XMLCipher.RSA_OAEP, XMLCipher.SHA1, "", XMLCipher.AES_128, XMLCipher.RSA_OAEP, "", EncryptionConstants.MGF1_SHA1);
    }

    @Test
    public void testEncryptionRsa15() throws Exception {
        testEncryption(XMLCipher.AES_256_GCM, XMLCipher.RSA_v1dot5, "", "");
    }
}

package org.keycloak.testsuite.broker;

import java.io.Closeable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.namespace.QName;

import jakarta.ws.rs.core.Response.Status;

import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.crypto.Algorithm;
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.jose.jwe.JWEConstants;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.KeysMetadataRepresentation;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.saml.processing.core.parsers.saml.assertion.SAMLAssertionQNames;
import org.keycloak.saml.processing.core.parsers.saml.protocol.SAMLProtocolQNames;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.testsuite.saml.AbstractSamlTest;
import org.keycloak.testsuite.updaters.ClientAttributeUpdater;
import org.keycloak.testsuite.updaters.IdentityProviderAttributeUpdater;
import org.keycloak.testsuite.updaters.RealmAttributeUpdater;
import org.keycloak.testsuite.util.KeyUtils;
import org.keycloak.testsuite.util.SamlClient;
import org.keycloak.testsuite.util.SamlClient.Binding;
import org.keycloak.testsuite.util.SamlClientBuilder;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.saml.SamlDocumentStepBuilder.Saml2DocumentTransformer;

import org.apache.http.HttpResponse;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import static org.keycloak.testsuite.broker.BrokerTestTools.getConsumerRoot;
import static org.keycloak.testsuite.broker.BrokerTestTools.getProviderRoot;
import static org.keycloak.testsuite.util.Matchers.bodyHC;
import static org.keycloak.testsuite.util.Matchers.isSamlResponse;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

public class KcSamlSignedBrokerTest extends AbstractBrokerTest {


    public void withSignedEncryptedAssertions(Runnable testBody, boolean signedDocument, boolean signedAssertion, boolean encryptedAssertion) throws Exception {

        KeysMetadataRepresentation consumerKeysMetadata = adminClient.realm(bc.consumerRealmName()).keys().getKeyMetadata();
        KeysMetadataRepresentation providerKeysMetadata = adminClient.realm(bc.providerRealmName()).keys().getKeyMetadata();

        String providerSigCert = KeyUtils.findActiveSigningKey(adminClient.realm(bc.providerRealmName()), Algorithm.RS256).getCertificate();
        assertThat(providerSigCert, Matchers.notNullValue());

        String consumerEncCert = KeyUtils.findActiveEncryptingKey(adminClient.realm(bc.consumerRealmName()), Algorithm.RSA_OAEP).getCertificate();
        assertThat(consumerEncCert, Matchers.notNullValue());

        String consumerSigCert = KeyUtils.findActiveSigningKey(adminClient.realm(bc.consumerRealmName()), Algorithm.RS256).getCertificate();
        assertThat(consumerSigCert, Matchers.notNullValue());

        try (Closeable idpUpdater = new IdentityProviderAttributeUpdater(identityProviderResource)
            .setAttribute(SAMLIdentityProviderConfig.VALIDATE_SIGNATURE, Boolean.toString(signedAssertion || signedDocument))
            .setAttribute(SAMLIdentityProviderConfig.WANT_ASSERTIONS_SIGNED, Boolean.toString(signedAssertion))
            .setAttribute(SAMLIdentityProviderConfig.WANT_ASSERTIONS_ENCRYPTED, Boolean.toString(encryptedAssertion))
            .setAttribute(SAMLIdentityProviderConfig.WANT_AUTHN_REQUESTS_SIGNED, "false")
            .setAttribute(SAMLIdentityProviderConfig.ENCRYPTION_ALGORITHM, JWEConstants.RSA_OAEP)
            .setAttribute(SAMLIdentityProviderConfig.SIGNING_CERTIFICATE_KEY, providerSigCert)
            .update();
          Closeable clientUpdater = ClientAttributeUpdater.forClient(adminClient, bc.providerRealmName(), bc.getIDPClientIdInProviderRealm())
            .setAttribute(SamlConfigAttributes.SAML_ENCRYPT, Boolean.toString(encryptedAssertion))
            .setAttribute(SamlConfigAttributes.SAML_ENCRYPTION_CERTIFICATE_ATTRIBUTE, consumerEncCert)
            .setAttribute(SamlConfigAttributes.SAML_SERVER_SIGNATURE, Boolean.toString(signedDocument))
            .setAttribute(SamlConfigAttributes.SAML_ASSERTION_SIGNATURE, Boolean.toString(signedAssertion))
            .setAttribute(SamlConfigAttributes.SAML_SIGNING_CERTIFICATE_ATTRIBUTE, consumerSigCert)
            .setAttribute(SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE, "false")    // Do not require client signature
            .update())
        {
            testBody.run();
        }
    }

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return new KcSamlSignedBrokerConfiguration();
    }

    @Test
    public void testWithExpiredBrokerCertificate() throws Exception {
        try (Closeable idpUpdater = new IdentityProviderAttributeUpdater(identityProviderResource)
            .setAttribute(SAMLIdentityProviderConfig.VALIDATE_SIGNATURE, Boolean.toString(true))
            .setAttribute(SAMLIdentityProviderConfig.WANT_ASSERTIONS_SIGNED, Boolean.toString(true))
            .setAttribute(SAMLIdentityProviderConfig.WANT_ASSERTIONS_ENCRYPTED, Boolean.toString(false))
            .setAttribute(SAMLIdentityProviderConfig.WANT_AUTHN_REQUESTS_SIGNED, "true")
            .setAttribute(SAMLIdentityProviderConfig.SIGNING_CERTIFICATE_KEY, AbstractSamlTest.SAML_CLIENT_SALES_POST_SIG_EXPIRED_CERTIFICATE)
            .update();
          Closeable clientUpdater = ClientAttributeUpdater.forClient(adminClient, bc.providerRealmName(), bc.getIDPClientIdInProviderRealm())
            .setAttribute(SamlConfigAttributes.SAML_ENCRYPT, Boolean.toString(false))
            .setAttribute(SamlConfigAttributes.SAML_SERVER_SIGNATURE, "true")
            .setAttribute(SamlConfigAttributes.SAML_ASSERTION_SIGNATURE, Boolean.toString(true))
            .setAttribute(SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE, "false")
            .update();
          Closeable realmUpdater = new RealmAttributeUpdater(adminClient.realm(bc.providerRealmName()))
            .setPublicKey(AbstractSamlTest.SAML_CLIENT_SALES_POST_SIG_EXPIRED_PUBLIC_KEY)
            .setPrivateKey(AbstractSamlTest.SAML_CLIENT_SALES_POST_SIG_EXPIRED_PRIVATE_KEY)
            .update())
        {
            AuthnRequestType loginRep = SamlClient.createLoginRequestDocument(AbstractSamlTest.SAML_CLIENT_ID_SALES_POST + ".dot/ted", getConsumerRoot() + "/sales-post/saml", null);

            Document doc = SAML2Request.convert(loginRep);
            new SamlClientBuilder()
              .authnRequest(getConsumerSamlEndpoint(bc.consumerRealmName()), doc, Binding.POST).build()   // Request to consumer IdP
              .login().idp(bc.getIDPAlias()).build()

              .assertResponse(org.keycloak.testsuite.util.Matchers.statusCodeIsHC(Status.BAD_REQUEST));
        }

    }

    @Test
    public void testSignedEncryptedAssertions() throws Exception {
        withSignedEncryptedAssertions(this::testAssertionSignatureRespected, false, true, true);
    }

    @Test
    public void testSignedAssertion() throws Exception {
        withSignedEncryptedAssertions(this::testAssertionSignatureRespected, false, true, false);
    }

    private void testAssertionSignatureRespected() {
        // Login should pass because assertion is signed.
        loginUser();

        // Logout should fail because logout response is not signed.
        final String code = oauth.parseLoginResponse().getCode();
        final AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);
        final String idTokenString = tokenResponse.getIdToken();
        final String redirectUri = getAccountUrl(getProviderRoot(), bc.providerRealmName());
        oauth.realm(bc.providerRealmName()).logoutForm()
            .idTokenHint(idTokenString)
            .postLogoutRedirectUri(redirectUri).open();

        errorPage.assertCurrent();
    }

    private Document extractNamespacesToTopLevelElement(Document original) {
        HashMap<String, String> namespaces = new HashMap<>();
        enumerateAndRemoveNamespaces(original.getDocumentElement(), namespaces);

        log.infof("Namespaces: %s", namespaces);
        log.infof("Document: %s", DocumentUtil.asString(original));

        Element rootNode = original.getDocumentElement();
        for (Entry<String, String> me : namespaces.entrySet()) {
            rootNode.setAttribute(me.getKey(), me.getValue());
        }

        log.infof("Updated document: %s", DocumentUtil.asString(original));

        return original;
    }

    private void enumerateAndRemoveNamespaces(Element documentElement, HashMap<String, String> namespaces) {
        final NamedNodeMap attrs = documentElement.getAttributes();
        if (attrs != null) {
            final Set<String> found = new HashSet<>();

            for (int i = attrs.getLength() - 1; i >= 0; i--) {
                Node item = attrs.item(i);
                String nodeName = item.getNodeName();
                if (nodeName != null && nodeName.startsWith("xmlns:")) {
                    namespaces.put(nodeName, item.getNodeValue());
                    found.add(nodeName);
                }
            }

            found.forEach(documentElement::removeAttribute);
        }

        NodeList childNodes = documentElement.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i ++) {
            Node childNode = childNodes.item(i);
            if (childNode instanceof Element) {
                enumerateAndRemoveNamespaces((Element) childNode, namespaces);
            }
        }
    }

    // KEYCLOAK-5581
    @Test
    public void loginUserAllNamespacesInTopElement() {
        AuthnRequestType loginRep = SamlClient.createLoginRequestDocument(AbstractSamlTest.SAML_CLIENT_ID_SALES_POST, getConsumerRoot() + "/sales-post/saml", null);

        Document doc;
        try {
            doc = extractNamespacesToTopLevelElement(SAML2Request.convert(loginRep));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        SAMLDocumentHolder samlResponse = new SamlClientBuilder()
          .authnRequest(getConsumerSamlEndpoint(bc.consumerRealmName()), doc, Binding.POST).build()   // Request to consumer IdP
          .login().idp(bc.getIDPAlias()).build()

          .processSamlResponse(Binding.POST)    // AuthnRequest to producer IdP
            .targetAttributeSamlRequest()
            .transformDocument(this::extractNamespacesToTopLevelElement)
            .build()

          .login().user(bc.getUserLogin(), bc.getUserPassword()).build()

          .processSamlResponse(Binding.POST)    // Response from producer IdP
            .transformDocument(this::extractNamespacesToTopLevelElement)
            .build()

          // first-broker flow
          .updateProfile().firstName("a").lastName("b").email(bc.getUserEmail()).username(bc.getUserLogin()).build()
          .followOneRedirect()

          .getSamlResponse(Binding.POST);       // Response from consumer IdP

        assertThat(samlResponse, Matchers.notNullValue());
        assertThat(samlResponse.getSamlObject(), isSamlResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
    }

    @Test
    public void loginUserAllNamespacesInTopElementSignedEncryptedAssertion() throws Exception {
        withSignedEncryptedAssertions(this::loginUserAllNamespacesInTopElement, false, true, true);
    }

    @Test
    public void loginUserAllNamespacesInTopElementSignedAssertion() throws Exception {
        withSignedEncryptedAssertions(this::loginUserAllNamespacesInTopElement, false, true, false);
    }

    @Test
    public void loginUserAllNamespacesInTopElementEncryptedAssertion() throws Exception {
        withSignedEncryptedAssertions(this::loginUserAllNamespacesInTopElement, false, false, true);
    }

    public class KcSamlSignedBrokerConfiguration extends KcSamlBrokerConfiguration {


        @Override
        public List<ClientRepresentation> createProviderClients() {
            List<ClientRepresentation> clientRepresentationList = super.createProviderClients();

            String consumerCert = KeyUtils.findActiveSigningKey(adminClient.realm(consumerRealmName()), Algorithm.RS256).getCertificate();
            assertThat(consumerCert, Matchers.notNullValue());

            for (ClientRepresentation client : clientRepresentationList) {
                client.setClientAuthenticatorType("client-secret");
                client.setSurrogateAuthRequired(false);

                Map<String, String> attributes = client.getAttributes();
                if (attributes == null) {
                    attributes = new HashMap<>();
                    client.setAttributes(attributes);
                }

                attributes.put(SamlConfigAttributes.SAML_ASSERTION_SIGNATURE, "true");
                attributes.put(SamlConfigAttributes.SAML_SERVER_SIGNATURE, "true");
                attributes.put(SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE, "true");
                attributes.put(SamlConfigAttributes.SAML_SIGNATURE_ALGORITHM, "RSA_SHA256");
                attributes.put(SamlConfigAttributes.SAML_SIGNING_CERTIFICATE_ATTRIBUTE, consumerCert);
            }

            return clientRepresentationList;
        }

        @Override
        public IdentityProviderRepresentation setUpIdentityProvider(IdentityProviderSyncMode syncMode) {
            IdentityProviderRepresentation result = super.setUpIdentityProvider(syncMode);

            String providerCert = KeyUtils.findActiveSigningKey(adminClient.realm(providerRealmName()), Algorithm.RS256).getCertificate();
            assertThat(providerCert, Matchers.notNullValue());

            Map<String, String> config = result.getConfig();

            config.put(SAMLIdentityProviderConfig.VALIDATE_SIGNATURE, "true");
            config.put(SAMLIdentityProviderConfig.WANT_ASSERTIONS_SIGNED, "true");
            config.put(SAMLIdentityProviderConfig.WANT_AUTHN_REQUESTS_SIGNED, "true");
            config.put(SAMLIdentityProviderConfig.SIGNING_CERTIFICATE_KEY, providerCert);

            return result;
        }
    }

    @Test
    public void testSignatureTampering_NOsignDoc_NOsignAssert_NOencAssert() throws Exception {
        loginAttackChangeSignature(false, false, false);
    }

    @Test
    public void testSignatureTampering_NOsignDoc_NOsignAssert_encAssert() throws Exception {
        loginAttackChangeSignature(false, false, true);
    }

    @Test
    public void testSignatureTampering_NOsignDoc_signAssert_NOencAssert() throws Exception {
        loginAttackChangeSignature(false, true, false);
    }

    @Test
    public void testSignatureTampering_NOsignDoc_signAssert_encAssert() throws Exception {
        loginAttackChangeSignature(false, true, true);
    }

    @Test
    public void testSignatureTampering_signDoc_NOsignAssert_NOencAssert() throws Exception {
        loginAttackChangeSignature(true, false, false);
    }

    @Test
    public void testSignatureTampering_signDoc_NOsignAssert_encAssert() throws Exception {
        loginAttackChangeSignature(true, false, true);
    }

    @Test
    public void testSignatureTampering_signDoc_signAssert_NOencAssert() throws Exception {
        loginAttackChangeSignature(true, true, false);
    }

    @Test
    public void testSignatureTampering_signDoc_signAssert_encAssert() throws Exception {
        loginAttackChangeSignature(true, true, true);
    }

    private Document removeDocumentSignature(Document orig) {
        return removeSignatureTag(orig, Collections.singleton(SAMLProtocolQNames.RESPONSE.getQName()));
    }

    private Document removeAssertionSignature(Document orig) {
        return removeSignatureTag(orig, Collections.singleton(SAMLAssertionQNames.ASSERTION.getQName()));
    }

    private Document removeDocumentAndAssertionSignature(Document orig) {
        return removeSignatureTag(orig,
          new HashSet<>(Arrays.asList(SAMLProtocolQNames.RESPONSE.getQName(), SAMLAssertionQNames.ASSERTION.getQName()))
        );
    }

    private Document removeSignatureTag(Document orig, final Set<QName> qNames) throws DOMException {
        NodeList sigElements = orig.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
        LinkedList<Node> nodesToRemove = new LinkedList<>();
        for (int i = 0; i < sigElements.getLength(); i ++) {
            Node n = sigElements.item(i);
            final Node p = n.getParentNode();
            QName q = new QName(p.getNamespaceURI(), p.getLocalName());
            if (qNames.contains(q)) {
                nodesToRemove.add(n);
            }
        }
        nodesToRemove.forEach(n -> n.getParentNode().removeChild(n));
        return orig;
    }

    private void loginAttackChangeSignature(boolean producerSignDocument, boolean producerSignAssertions, boolean producerEncryptAssertions) throws Exception {
        log.debug("");

        loginAttackChangeSignature("No changes to SAML document", producerSignDocument, producerSignAssertions, producerEncryptAssertions,
          t -> t, true);

        // TODO: producerSignAssertions should be removed once there would be option to force check SAML document signature
        boolean validAfterTamperingWithDocumentSignature = ! producerSignDocument || producerSignAssertions;

        loginAttackChangeSignature("Remove document signature", producerSignDocument, producerSignAssertions, producerEncryptAssertions,
          this::removeDocumentSignature, validAfterTamperingWithDocumentSignature);


        // Tests for assertion signature manipulation follow. Tampering with assertion signature is
        // relevant only if assertion is not encrypted since signature is part of encrypted data,
        // hence skipped in the opposite case.
        if (producerEncryptAssertions) {
            return;
        }

        // When assertion signature is removed, the expected document validation passes only
        // if neither the document was not signed
        // (otherwise document signature is invalidated by removing signature from the assertion)
        // nor the assertion is
        boolean validAfterTamperingWithAssertionSignature = ! producerSignAssertions;

        // When both assertion and document signatures are removed, the document validation passes only
        // if neiter the document nor assertion were signed
        boolean validAfterTamperingWithBothDocumentAndAssertionSignature = ! producerSignDocument && ! producerSignAssertions;

        loginAttackChangeSignature("Remove assertion signature", producerSignDocument, producerSignAssertions, producerEncryptAssertions,
          this::removeAssertionSignature, validAfterTamperingWithAssertionSignature);
        loginAttackChangeSignature("Remove both document and assertion signature", producerSignDocument, producerSignAssertions, producerEncryptAssertions,
          this::removeDocumentAndAssertionSignature, validAfterTamperingWithBothDocumentAndAssertionSignature);
    }

    private void loginAttackChangeSignature(String description,
      boolean producerSignDocument, boolean producerSignAssertions, boolean producerEncryptAssertions,
      Saml2DocumentTransformer tr, boolean shouldSucceed) throws Exception {
        log.infof("producerSignDocument: %s, producerSignAssertions: %s, producerEncryptAssertions: %s", producerSignDocument, producerSignAssertions, producerEncryptAssertions);

        Matcher<HttpResponse> responseFromConsumerMatcher = shouldSucceed
          ?     bodyHC(containsString("Update Account Information"))
          : not(bodyHC(containsString("Update Account Information")));

        AuthnRequestType loginRep = SamlClient.createLoginRequestDocument(AbstractSamlTest.SAML_CLIENT_ID_SALES_POST, getConsumerRoot() + "/sales-post/saml", null);
        Document doc = SAML2Request.convert(loginRep);

        withSignedEncryptedAssertions(() -> {
            new SamlClientBuilder()
              .authnRequest(getConsumerSamlEndpoint(bc.consumerRealmName()), doc, Binding.POST).build()   // Request to consumer IdP

              .login().idp(bc.getIDPAlias()).build()

              .processSamlResponse(Binding.POST).build()    // AuthnRequest to producer IdP

              .login().user(bc.getUserLogin(), bc.getUserPassword()).build()

              .processSamlResponse(Binding.POST)    // Response from producer IdP
                .transformDocument(tr)
                .build()

              // first-broker flow: if valid request, it displays an update profile page on consumer realm
              .execute(currentResponse -> assertThat(description, currentResponse, responseFromConsumerMatcher));
        }, producerSignDocument, producerSignAssertions, producerEncryptAssertions);
    }

    @Test
    public void testSignatureDataWhenWantsRequestsSigned() throws Exception {
        // Verifies that an AuthnRequest contains the KeyInfo/X509Data element when
        // client AuthnRequest signature is requested
        String providerCert = KeyUtils.findActiveSigningKey(adminClient.realm(bc.providerRealmName()), Algorithm.RS256).getCertificate();
        assertThat(providerCert, Matchers.notNullValue());

        String consumerCert = KeyUtils.findActiveSigningKey(adminClient.realm(bc.consumerRealmName()), Algorithm.RS256).getCertificate();
        assertThat(consumerCert, Matchers.notNullValue());

        try (Closeable idpUpdater = new IdentityProviderAttributeUpdater(identityProviderResource)
            .setAttribute(SAMLIdentityProviderConfig.VALIDATE_SIGNATURE, Boolean.toString(true))
            .setAttribute(SAMLIdentityProviderConfig.WANT_ASSERTIONS_SIGNED, Boolean.toString(true))
            .setAttribute(SAMLIdentityProviderConfig.WANT_ASSERTIONS_ENCRYPTED, Boolean.toString(false))
            .setAttribute(SAMLIdentityProviderConfig.WANT_AUTHN_REQUESTS_SIGNED, "true")
            .setAttribute(SAMLIdentityProviderConfig.SIGNING_CERTIFICATE_KEY, AbstractSamlTest.SAML_CLIENT_SALES_POST_SIG_EXPIRED_CERTIFICATE)
            .update();
          Closeable clientUpdater = ClientAttributeUpdater.forClient(adminClient, bc.providerRealmName(), bc.getIDPClientIdInProviderRealm())
            .setAttribute(SamlConfigAttributes.SAML_ENCRYPT, Boolean.toString(false))
            .setAttribute(SamlConfigAttributes.SAML_SERVER_SIGNATURE, "true")
            .setAttribute(SamlConfigAttributes.SAML_ASSERTION_SIGNATURE, Boolean.toString(true))
            .setAttribute(SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE, "false")
            .update())
        {
            // Build the login request document
            AuthnRequestType loginRep = SamlClient.createLoginRequestDocument(AbstractSamlTest.SAML_CLIENT_ID_SALES_POST + ".dot/ted", getConsumerRoot() + "/sales-post/saml", null);
            Document doc = SAML2Request.convert(loginRep);
            new SamlClientBuilder()
                .authnRequest(getConsumerSamlEndpoint(bc.consumerRealmName()), doc, Binding.POST)
                .build()   // Request to consumer IdP
                .login().idp(bc.getIDPAlias()).build()
                .processSamlResponse(Binding.POST)    // AuthnRequest to producer IdP
                  .targetAttributeSamlRequest()
                  .transformDocument(this::extractNamespacesToTopLevelElement)
                  .transformDocument((document) -> {
                    try
                    {
                        // Find the Signature element
                        Element signatureElement = DocumentUtil.getDirectChildElement(document.getDocumentElement(), XMLSignature.XMLNS, "Signature");
                        assertThat("Signature element not found in request document", signatureElement, Matchers.notNullValue());

                        // Find the KeyInfo element
                        Element keyInfoElement = DocumentUtil.getDirectChildElement(signatureElement, XMLSignature.XMLNS, "KeyInfo");
                        assertThat("KeyInfo element not found in request Signature element", keyInfoElement, Matchers.notNullValue());

                        // Find the X509Data element
                        Element x509DataElement = DocumentUtil.getDirectChildElement(keyInfoElement, XMLSignature.XMLNS, "X509Data");
                        assertThat("X509Data element not found in request Signature/KeyInfo element", x509DataElement, Matchers.notNullValue());
                    }
                    catch (Exception ex)
                    {
                        throw new RuntimeException(ex);
                    }
                  })
                  .build()
                .execute();
        }
    }

    @Test
    public void testSignatureDataTwoCertificatesPostBinding() throws Exception {
        // Check two certifcates work with POST binding
        String badCert = KeyUtils.findActiveSigningKey(adminClient.realm(bc.consumerRealmName()), Algorithm.RS256).getCertificate();
        String goodCert = KeyUtils.findActiveSigningKey(adminClient.realm(bc.providerRealmName()), Algorithm.RS256).getCertificate();

        try (Closeable clientUpdater = ClientAttributeUpdater.forClient(adminClient, bc.providerRealmName(), bc.getIDPClientIdInProviderRealm())
                .setAttribute(SamlConfigAttributes.SAML_ENCRYPT, "false")
                .setAttribute(SamlConfigAttributes.SAML_SERVER_SIGNATURE, "true")
                .setAttribute(SamlConfigAttributes.SAML_ASSERTION_SIGNATURE, "false")
                .setAttribute(SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE, "false")
                .update();
             Closeable idpUpdater = new IdentityProviderAttributeUpdater(identityProviderResource)
                .setAttribute(SAMLIdentityProviderConfig.VALIDATE_SIGNATURE, "true")
                .setAttribute(SAMLIdentityProviderConfig.WANT_ASSERTIONS_SIGNED, "false")
                .setAttribute(SAMLIdentityProviderConfig.WANT_ASSERTIONS_ENCRYPTED, "false")
                .setAttribute(SAMLIdentityProviderConfig.WANT_AUTHN_REQUESTS_SIGNED, "true")
                .setAttribute(SAMLIdentityProviderConfig.SIGNING_CERTIFICATE_KEY, badCert + "," + goodCert)
                .update();
          )
        {
            // Build the login request document
            AuthnRequestType loginRep = SamlClient.createLoginRequestDocument(AbstractSamlTest.SAML_CLIENT_ID_SALES_POST, getConsumerRoot() + "/sales-post/saml", null);
            Document doc = SAML2Request.convert(loginRep);
            new SamlClientBuilder()
                    .authnRequest(getConsumerSamlEndpoint(bc.consumerRealmName()), doc, Binding.POST)
                    .build() // Request to consumer IdP
                    .login().idp(bc.getIDPAlias()).build()
                    .processSamlResponse(Binding.POST).build() // AuthnRequest to producer IdP
                    .login().user(bc.getUserLogin(), bc.getUserPassword()).build()
                    .processSamlResponse(Binding.POST) // Response from producer IdP
                    .build()
                    // first-broker flow: if valid request, it displays an update profile page on consumer realm
                    .execute(currentResponse -> assertThat(currentResponse, bodyHC(containsString("Update Account Information"))));
        }
    }

    @Test
    public void testSignatureDataTwoCertificatesRedirectBinding() throws Exception {
        // Check two certifcates work with REDIRECT binding
        String badCert = KeyUtils.findActiveSigningKey(adminClient.realm(bc.consumerRealmName()), Algorithm.RS256).getCertificate();
        String goodCert = KeyUtils.findActiveSigningKey(adminClient.realm(bc.providerRealmName()), Algorithm.RS256).getCertificate();

        try (Closeable clientProviderUpdater = ClientAttributeUpdater.forClient(adminClient, bc.providerRealmName(), bc.getIDPClientIdInProviderRealm())
                .setAttribute(SamlConfigAttributes.SAML_ENCRYPT, "false")
                .setAttribute(SamlConfigAttributes.SAML_SERVER_SIGNATURE, "true")
                .setAttribute(SamlConfigAttributes.SAML_ASSERTION_SIGNATURE, "false")
                .setAttribute(SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE, "false")
                .setAttribute(SamlConfigAttributes.SAML_FORCE_POST_BINDING, "false")
                .update();
             Closeable clientConsumerUpdater = ClientAttributeUpdater.forClient(adminClient, bc.providerRealmName(), bc.getIDPClientIdInProviderRealm())
                .setAttribute(SamlConfigAttributes.SAML_FORCE_POST_BINDING, "false")
                .update();
             Closeable idpUpdater = new IdentityProviderAttributeUpdater(identityProviderResource)
                .setAttribute(SAMLIdentityProviderConfig.VALIDATE_SIGNATURE, "true")
                .setAttribute(SAMLIdentityProviderConfig.WANT_ASSERTIONS_SIGNED, "false")
                .setAttribute(SAMLIdentityProviderConfig.WANT_ASSERTIONS_ENCRYPTED, "false")
                .setAttribute(SAMLIdentityProviderConfig.WANT_AUTHN_REQUESTS_SIGNED, "true")
                .setAttribute(SAMLIdentityProviderConfig.POST_BINDING_AUTHN_REQUEST, "false")
                 .setAttribute(SAMLIdentityProviderConfig.POST_BINDING_RESPONSE, "false")
                .setAttribute(SAMLIdentityProviderConfig.SIGNING_CERTIFICATE_KEY, badCert + "," + goodCert)
                .update();
          )
        {
            // Build the login request document
            AuthnRequestType loginRep = SamlClient.createLoginRequestDocument(AbstractSamlTest.SAML_CLIENT_ID_SALES_POST, getConsumerRoot() + "/sales-post/saml", null);
            Document doc = SAML2Request.convert(loginRep);
            new SamlClientBuilder()
                    .authnRequest(getConsumerSamlEndpoint(bc.consumerRealmName()), doc, Binding.REDIRECT)
                    .build() // Request to consumer IdP
                    .login().idp(bc.getIDPAlias()).build()
                    .processSamlResponse(Binding.REDIRECT).build() // AuthnRequest to producer IdP
                    .login().user(bc.getUserLogin(), bc.getUserPassword()).build()
                    .processSamlResponse(Binding.REDIRECT) // Response from producer IdP
                    .build()
                    // first-broker flow: if valid request, it displays an update profile page on consumer realm
                    .execute(currentResponse -> assertThat(currentResponse, bodyHC(containsString("Update Account Information"))));
        }
    }
}

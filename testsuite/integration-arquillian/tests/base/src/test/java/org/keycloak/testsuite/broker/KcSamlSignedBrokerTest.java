package org.keycloak.testsuite.broker;

import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.crypto.Algorithm;
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.saml.processing.core.parsers.saml.assertion.SAMLAssertionQNames;
import org.keycloak.saml.processing.core.parsers.saml.protocol.SAMLProtocolQNames;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.testsuite.arquillian.SuiteContext;

import org.keycloak.testsuite.saml.AbstractSamlTest;
import org.keycloak.testsuite.updaters.ClientAttributeUpdater;
import org.keycloak.testsuite.updaters.IdentityProviderAttributeUpdater;
import org.keycloak.testsuite.updaters.RealmAttributeUpdater;
import org.keycloak.testsuite.util.KeyUtils;
import org.keycloak.testsuite.util.SamlClient;
import org.keycloak.testsuite.util.SamlClient.Binding;
import org.keycloak.testsuite.util.SamlClientBuilder;
import org.keycloak.testsuite.util.saml.SamlDocumentStepBuilder.Saml2DocumentTransformer;
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
import javax.ws.rs.core.Response.Status;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.namespace.QName;
import org.apache.http.HttpResponse;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.keycloak.testsuite.arquillian.AuthServerTestEnricher.getAuthServerContextRoot;
import static org.keycloak.testsuite.broker.BrokerTestConstants.*;
import static org.keycloak.testsuite.util.Matchers.bodyHC;
import static org.keycloak.testsuite.util.Matchers.isSamlResponse;

public class KcSamlSignedBrokerTest extends AbstractBrokerTest {

    private static final String PRIVATE_KEY = "MIIBVQIBADANBgkqhkiG9w0BAQEFAASCAT8wggE7AgEAAkEAs46ICYPRIkmr8diECmyT59cChTWIEiXYBY3T6OLlZrF8ofVCzbEeoUOmhrtHijxxuKSoqLWP4nNOt3rINtQNBQIDAQABAkBL2nyxuFQTLhhLdPJjDPd2y6gu6ixvrjkSL5ZEHgZXWRHzhTzBT0eRxg/5rJA2NDRMBzTTegaEGkWUt7lF5wDJAiEA5pC+h9NEgqDJSw42I52BOml3II35Z6NlNwl6OMfnD1sCIQDHXUiOIJy4ZcSgv5WGue1KbdNVOT2gop1XzfuyWgtjHwIhAOCjLb9QC3PqC7Tgx8azcnDiyHojWVesTrTsuvQPcAP5AiAkX5OeQrr1NbQTNAEe7IsrmjAFi4T/6stUOsOiPaV4NwIhAJIeyh4foIXIVQ+M4To2koaDFRssxKI9/O72vnZSJ+uA";
    private static final String PUBLIC_KEY = "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBALOOiAmD0SJJq/HYhApsk+fXAoU1iBIl2AWN0+ji5WaxfKH1Qs2xHqFDpoa7R4o8cbikqKi1j+JzTrd6yDbUDQUCAwEAAQ==";

    public class KcSamlSignedBrokerConfiguration extends KcSamlBrokerConfiguration {

        @Override
        public RealmRepresentation createProviderRealm() {
            RealmRepresentation realm = super.createProviderRealm();

            realm.setPublicKey(REALM_PUBLIC_KEY);
            realm.setPrivateKey(REALM_PRIVATE_KEY);

            return realm;
        }

        @Override
        public RealmRepresentation createConsumerRealm() {
            RealmRepresentation realm = super.createConsumerRealm();

            realm.setPublicKey(REALM_PUBLIC_KEY);
            realm.setPrivateKey(REALM_PRIVATE_KEY);

            return realm;
        }

        @Override
        public List<ClientRepresentation> createProviderClients(SuiteContext suiteContext) {
            List<ClientRepresentation> clientRepresentationList = super.createProviderClients(suiteContext);

            String consumerCert = KeyUtils.getActiveKey(adminClient.realm(consumerRealmName()).keys().getKeyMetadata(), Algorithm.RS256).getCertificate();
            Assert.assertThat(consumerCert, Matchers.notNullValue());

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
        public IdentityProviderRepresentation setUpIdentityProvider(SuiteContext suiteContext) {
            IdentityProviderRepresentation result = super.setUpIdentityProvider(suiteContext);

            String providerCert = KeyUtils.getActiveKey(adminClient.realm(providerRealmName()).keys().getKeyMetadata(), Algorithm.RS256).getCertificate();
            Assert.assertThat(providerCert, Matchers.notNullValue());

            Map<String, String> config = result.getConfig();

            config.put(SAMLIdentityProviderConfig.VALIDATE_SIGNATURE, "true");
            config.put(SAMLIdentityProviderConfig.WANT_ASSERTIONS_SIGNED, "true");
            config.put(SAMLIdentityProviderConfig.WANT_AUTHN_REQUESTS_SIGNED, "true");
            config.put(SAMLIdentityProviderConfig.SIGNING_CERTIFICATE_KEY, providerCert);

            return result;
        }
    }

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return new KcSamlSignedBrokerConfiguration();
    }

    public void withSignedEncryptedAssertions(Runnable testBody, boolean signedDocument, boolean signedAssertion, boolean encryptedAssertion) throws Exception {
        String providerCert = KeyUtils.getActiveKey(adminClient.realm(bc.providerRealmName()).keys().getKeyMetadata(), Algorithm.RS256).getCertificate();
        Assert.assertThat(providerCert, Matchers.notNullValue());

        String consumerCert = KeyUtils.getActiveKey(adminClient.realm(bc.consumerRealmName()).keys().getKeyMetadata(), Algorithm.RS256).getCertificate();
        Assert.assertThat(consumerCert, Matchers.notNullValue());

        try (Closeable idpUpdater = new IdentityProviderAttributeUpdater(identityProviderResource)
            .setAttribute(SAMLIdentityProviderConfig.VALIDATE_SIGNATURE, Boolean.toString(signedAssertion || signedDocument))
            .setAttribute(SAMLIdentityProviderConfig.WANT_ASSERTIONS_SIGNED, Boolean.toString(signedAssertion))
            .setAttribute(SAMLIdentityProviderConfig.WANT_ASSERTIONS_ENCRYPTED, Boolean.toString(encryptedAssertion))
            .setAttribute(SAMLIdentityProviderConfig.WANT_AUTHN_REQUESTS_SIGNED, "false")
            .setAttribute(SAMLIdentityProviderConfig.ENCRYPTION_PUBLIC_KEY, PUBLIC_KEY)
            .setAttribute(SAMLIdentityProviderConfig.SIGNING_CERTIFICATE_KEY, providerCert)
            .update();
          Closeable clientUpdater = ClientAttributeUpdater.forClient(adminClient, bc.providerRealmName(), bc.getIDPClientIdInProviderRealm(suiteContext))
            .setAttribute(SamlConfigAttributes.SAML_ENCRYPT, Boolean.toString(encryptedAssertion))
            .setAttribute(SamlConfigAttributes.SAML_ENCRYPTION_CERTIFICATE_ATTRIBUTE, consumerCert)
            .setAttribute(SamlConfigAttributes.SAML_SERVER_SIGNATURE, Boolean.toString(signedDocument))
            .setAttribute(SamlConfigAttributes.SAML_ASSERTION_SIGNATURE, Boolean.toString(signedAssertion))
            .setAttribute(SamlConfigAttributes.SAML_ENCRYPTION_PRIVATE_KEY_ATTRIBUTE, PRIVATE_KEY)
            .setAttribute(SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE, "false")    // Do not require client signature
            .update())
        {
            testBody.run();
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
        final String redirectUri = getAccountUrl(bc.providerRealmName());
        final String logoutUri = oauth.realm(bc.providerRealmName()).getLogoutUrl().redirectUri(redirectUri).build();
        driver.navigate().to(logoutUri);

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
        AuthnRequestType loginRep = SamlClient.createLoginRequestDocument(AbstractSamlTest.SAML_CLIENT_ID_SALES_POST, getAuthServerContextRoot() + "/sales-post/saml", null);

        Document doc;
        try {
            doc = extractNamespacesToTopLevelElement(SAML2Request.convert(loginRep));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        SAMLDocumentHolder samlResponse = new SamlClientBuilder()
          .authnRequest(getAuthServerSamlEndpoint(bc.consumerRealmName()), doc, Binding.POST).build()   // Request to consumer IdP
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

        Assert.assertThat(samlResponse, Matchers.notNullValue());
        Assert.assertThat(samlResponse.getSamlObject(), isSamlResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
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

    @Test
    public void testWithExpiredBrokerCertificate() throws Exception {
        try (Closeable idpUpdater = new IdentityProviderAttributeUpdater(identityProviderResource)
            .setAttribute(SAMLIdentityProviderConfig.VALIDATE_SIGNATURE, Boolean.toString(true))
            .setAttribute(SAMLIdentityProviderConfig.WANT_ASSERTIONS_SIGNED, Boolean.toString(true))
            .setAttribute(SAMLIdentityProviderConfig.WANT_ASSERTIONS_ENCRYPTED, Boolean.toString(false))
            .setAttribute(SAMLIdentityProviderConfig.WANT_AUTHN_REQUESTS_SIGNED, "true")
            .setAttribute(SAMLIdentityProviderConfig.SIGNING_CERTIFICATE_KEY, AbstractSamlTest.SAML_CLIENT_SALES_POST_SIG_EXPIRED_CERTIFICATE)
            .update();
          Closeable clientUpdater = ClientAttributeUpdater.forClient(adminClient, bc.providerRealmName(), bc.getIDPClientIdInProviderRealm(suiteContext))
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
            AuthnRequestType loginRep = SamlClient.createLoginRequestDocument(AbstractSamlTest.SAML_CLIENT_ID_SALES_POST + ".dot/ted", AbstractSamlTest.SAML_ASSERTION_CONSUMER_URL_SALES_POST, null);

            Document doc = SAML2Request.convert(loginRep);
            new SamlClientBuilder()
              .authnRequest(getAuthServerSamlEndpoint(bc.consumerRealmName()), doc, Binding.POST).build()   // Request to consumer IdP
              .login().idp(bc.getIDPAlias()).build()

              .assertResponse(org.keycloak.testsuite.util.Matchers.statusCodeIsHC(Status.BAD_REQUEST));
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

        AuthnRequestType loginRep = SamlClient.createLoginRequestDocument(AbstractSamlTest.SAML_CLIENT_ID_SALES_POST, getAuthServerContextRoot() + "/sales-post/saml", null);
        Document doc = SAML2Request.convert(loginRep);

        withSignedEncryptedAssertions(() -> {
            new SamlClientBuilder()
              .authnRequest(getAuthServerSamlEndpoint(bc.consumerRealmName()), doc, Binding.POST).build()   // Request to consumer IdP

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

}

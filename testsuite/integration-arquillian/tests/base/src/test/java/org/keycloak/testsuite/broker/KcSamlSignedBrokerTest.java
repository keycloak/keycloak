package org.keycloak.testsuite.broker;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.testsuite.arquillian.SuiteContext;

import org.keycloak.testsuite.saml.AbstractSamlTest;
import org.keycloak.testsuite.updaters.ClientAttributeUpdater;
import org.keycloak.testsuite.updaters.IdentityProviderAttributeUpdater;
import org.keycloak.testsuite.util.SamlClient;
import org.keycloak.testsuite.util.SamlClient.Binding;
import org.keycloak.testsuite.util.SamlClientBuilder;
import java.io.Closeable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import java.util.Map.Entry;
import java.util.Set;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import static org.keycloak.testsuite.broker.BrokerTestConstants.*;
import static org.keycloak.testsuite.broker.BrokerTestTools.encodeUrl;
import static org.keycloak.testsuite.util.Matchers.isSamlResponse;

public class KcSamlSignedBrokerTest extends KcSamlBrokerTest {

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

            String consumerCert = adminClient.realm(consumerRealmName()).keys().getKeyMetadata().getKeys().get(0).getCertificate();
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

            String providerCert = adminClient.realm(providerRealmName()).keys().getKeyMetadata().getKeys().get(0).getCertificate();
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

    @Test
    public void testSignedEncryptedAssertions() throws Exception {
        ClientRepresentation client = adminClient.realm(bc.providerRealmName())
          .clients()
          .findByClientId(bc.getIDPClientIdInProviderRealm(suiteContext))
          .get(0);

        final ClientResource clientResource = realmsResouce().realm(bc.providerRealmName()).clients().get(client.getId());
        Assert.assertThat(clientResource, Matchers.notNullValue());

        String providerCert = adminClient.realm(bc.providerRealmName()).keys().getKeyMetadata().getKeys().get(0).getCertificate();
        Assert.assertThat(providerCert, Matchers.notNullValue());

        String consumerCert = adminClient.realm(bc.consumerRealmName()).keys().getKeyMetadata().getKeys().get(0).getCertificate();
        Assert.assertThat(consumerCert, Matchers.notNullValue());

        try (Closeable idpUpdater = new IdentityProviderAttributeUpdater(identityProviderResource)
            .setAttribute(SAMLIdentityProviderConfig.VALIDATE_SIGNATURE, "true")
            .setAttribute(SAMLIdentityProviderConfig.WANT_ASSERTIONS_SIGNED, "true")
            .setAttribute(SAMLIdentityProviderConfig.WANT_ASSERTIONS_ENCRYPTED, "true")
            .setAttribute(SAMLIdentityProviderConfig.WANT_AUTHN_REQUESTS_SIGNED, "false")
            .setAttribute(SAMLIdentityProviderConfig.SIGNING_CERTIFICATE_KEY, providerCert)
            .update();
          Closeable clientUpdater = new ClientAttributeUpdater(clientResource)
            .setAttribute(SamlConfigAttributes.SAML_ENCRYPT, "true")
            .setAttribute(SamlConfigAttributes.SAML_ENCRYPTION_CERTIFICATE_ATTRIBUTE, consumerCert)
            .setAttribute(SamlConfigAttributes.SAML_SERVER_SIGNATURE, "false")      // only sign assertions
            .setAttribute(SamlConfigAttributes.SAML_ASSERTION_SIGNATURE, "true")
            .setAttribute(SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE, "false")
            .update())
        {
            // Login should pass because assertion is signed.
            loginUser();

            // Logout should fail because logout response is not signed.
            driver.navigate().to(BrokerTestTools.getAuthRoot(suiteContext)
                    + "/auth/realms/" + bc.providerRealmName()
                    + "/protocol/" + "openid-connect"
                    + "/logout?redirect_uri=" + encodeUrl(getAccountUrl(bc.providerRealmName())));

            errorPage.assertCurrent();
        }
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
    public void loginUserAllNamespacesInTopElement() throws Exception {
        AuthnRequestType loginRep = SamlClient.createLoginRequestDocument(AbstractSamlTest.SAML_CLIENT_ID_SALES_POST, AbstractSamlTest.SAML_ASSERTION_CONSUMER_URL_SALES_POST, null);

        Document doc = extractNamespacesToTopLevelElement(SAML2Request.convert(loginRep));

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

}

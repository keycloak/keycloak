package org.keycloak.tests.broker.saml;

import java.util.Map;
import java.util.function.Consumer;

import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.https.InjectCertificates;
import org.keycloak.testframework.https.ManagedCertificates;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.ErrorPage;
import org.keycloak.tests.broker.AbstractKcSamlBrokerTest;
import org.keycloak.tests.saml.SamlClient;
import org.keycloak.tests.saml.SamlClientBuilder;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import static org.junit.jupiter.api.Assertions.assertEquals;

@KeycloakIntegrationTest
public class KcSamlBrokerTest extends AbstractKcSamlBrokerTest {

    private static final String ASSERTION_NS = "urn:oasis:names:tc:SAML:2.0:assertion";
    private static final String BEARER_METHOD = "urn:oasis:names:tc:SAML:2.0:cm:bearer";

    @InjectCertificates
    ManagedCertificates certificates;

    @InjectPage
    ErrorPage errorPage;

    @InjectEvents(realmRef = "consumer")
    Events events;

    @Test
    public void testRejectsSamlResponseWithoutSubject() throws Exception {
        assertInvalidSamlResponse(this::removeSubject);
    }

    @Test
    public void testRejectsSamlResponseWithoutBearerSubjectConfirmation() throws Exception {
        assertInvalidSamlResponse(this::removeBearerConfirmation);
    }

    @Test
    public void testRejectsBearerSubjectConfirmationWithoutData() throws Exception {
        assertInvalidSamlResponse(this::removeSubjectConfirmationData);
    }

    @Test
    public void testRejectsBearerSubjectConfirmationWithoutNotOnOrAfter() throws Exception {
        assertInvalidSamlResponse(this::removeNotOnOrAfter);
    }

    @Test
    public void testRejectsBearerSubjectConfirmationWithoutRecipient() throws Exception {
        assertInvalidSamlResponse(this::removeRecipient);
    }

    private void assertInvalidSamlResponse(Consumer<Document> mutator) throws Exception {
        new SamlClientBuilder(certificates)
                .navigateTo(oauth.loginForm().param("kc_idp_hint", IDP_SAML_ALIAS).build())
                .processSamlResponse(SamlClient.Binding.POST)
                    .targetAttributeSamlRequest()
                    .build()
                .login().user(getUserLogin(), getUserPassword()).build()
                .processSamlResponse(SamlClient.Binding.POST)
                    .transformDocument(mutator)
                    .build()
                .execute(response -> assertEquals(400, response.getStatusLine().getStatusCode(),
                        "Malformed broker SAML response should be rejected"));
    }

    private void removeSubject(Document document) {
        Node assertion = document.getElementsByTagNameNS(ASSERTION_NS, "Assertion").item(0);
        Node subject = firstChild(assertion, "Subject");
        assertion.removeChild(subject);
    }

    private void removeBearerConfirmation(Document document) {
        Node assertion = document.getElementsByTagNameNS(ASSERTION_NS, "Assertion").item(0);
        Node subject = firstChild(assertion, "Subject");
        Node confirmation = findBearerConfirmation(subject);
        subject.removeChild(confirmation);
    }

    private void removeSubjectConfirmationData(Document document) {
        Node assertion = document.getElementsByTagNameNS(ASSERTION_NS, "Assertion").item(0);
        Node subject = firstChild(assertion, "Subject");
        Node confirmation = findBearerConfirmation(subject);
        Node data = firstChild(confirmation, "SubjectConfirmationData");
        confirmation.removeChild(data);
    }

    private void removeNotOnOrAfter(Document document) {
        Node assertion = document.getElementsByTagNameNS(ASSERTION_NS, "Assertion").item(0);
        Node subject = firstChild(assertion, "Subject");
        Node confirmation = findBearerConfirmation(subject);
        Node data = firstChild(confirmation, "SubjectConfirmationData");
        data.getAttributes().removeNamedItem("NotOnOrAfter");
    }

    private void removeRecipient(Document document) {
        Node assertion = document.getElementsByTagNameNS(ASSERTION_NS, "Assertion").item(0);
        Node subject = firstChild(assertion, "Subject");
        Node confirmation = findBearerConfirmation(subject);
        Node data = firstChild(confirmation, "SubjectConfirmationData");
        data.getAttributes().removeNamedItem("Recipient");
    }

    private static Node findBearerConfirmation(Node subject) {
        Node confirmation = firstChild(subject, "SubjectConfirmation");
        while (confirmation != null && (confirmation.getAttributes().getNamedItem("Method") == null
                || !BEARER_METHOD.equals(confirmation.getAttributes().getNamedItem("Method").getNodeValue()))) {
            confirmation = nextSibling(confirmation, "SubjectConfirmation");
        }
        if (confirmation == null) {
            throw new IllegalStateException("Missing Bearer SubjectConfirmation (Method=" + BEARER_METHOD + ")");
        }
        return confirmation;
    }

    private static Node firstChild(Node parent, String localName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (localName.equals(child.getLocalName())) {
                return child;
            }
        }
        throw new IllegalStateException("Missing SAML element " + localName);
    }

    private static Node nextSibling(Node node, String localName) {
        for (Node sibling = node.getNextSibling(); sibling != null; sibling = sibling.getNextSibling()) {
            if (localName.equals(sibling.getLocalName())) {
                return sibling;
            }
        }
        return null;
    }

    @Test
    public void testLoginFailsWhenResponse_UnsignedANDValidateSignatureIsOmitted() {
        // Configure the provider
        String samlClientId = consumerRealm.getBaseUrl();
        ClientRepresentation client = providerRealm.admin().clients().findByClientId(samlClientId).get(0);
        client.getAttributes().put(SamlConfigAttributes.SAML_ASSERTION_SIGNATURE, "false");
        client.getAttributes().put(SamlConfigAttributes.SAML_SERVER_SIGNATURE, "false");
        providerRealm.admin().clients().get(client.getId()).update(client);

        IdentityProviderRepresentation idp = consumerRealm.admin().identityProviders().get(getIdpAlias()).toRepresentation();

        Map<String, String> config = idp.getConfig();
        config.put(SAMLIdentityProviderConfig.VALIDATE_SIGNATURE, "true");
        config.put(SAMLIdentityProviderConfig.SIGNING_CERTIFICATE_KEY, activeRs256SigCert(providerRealm));

        consumerRealm.admin().identityProviders().get(getIdpAlias()).update(idp);
        // Attempt to log in should fail due to missing signature
        assertLoginFailsWithSignatureError();
    }

    @Test
    public void testLoginFailsWhenAssertionHasInvalidSignatureAndWantAssertionsSignedIsTrue() {
        String samlClientId = consumerRealm.getBaseUrl();
        ClientRepresentation client = providerRealm.admin().clients().findByClientId(samlClientId).get(0);
        client.getAttributes().put(SamlConfigAttributes.SAML_ASSERTION_SIGNATURE, "true");
        client.getAttributes().put(SamlConfigAttributes.SAML_SERVER_SIGNATURE, "false");

        providerRealm.admin().clients().get(client.getId()).update(client);

        String consumerSigningCert = activeRs256SigCert(consumerRealm);

        IdentityProviderRepresentation idp = consumerRealm.admin().identityProviders().get(getIdpAlias()).toRepresentation();

        Map<String, String> config = idp.getConfig();
        config.put(SAMLIdentityProviderConfig.WANT_ASSERTIONS_SIGNED, "true");
        config.put(SAMLIdentityProviderConfig.SIGNING_CERTIFICATE_KEY, consumerSigningCert);
        config.put(SAMLIdentityProviderConfig.VALIDATE_SIGNATURE, "true");

        consumerRealm.admin().identityProviders().get(getIdpAlias()).update(idp);
        assertLoginFailsWithSignatureError();
    }

    @Test
    public void testLoginFailsWhenWantAssertionsSignedIsOmitted() {
        // wantAssertionsSigned defaults to true when omitted.
        configureSignedResponseWithUnsignedAssertion(true, true);
        assertLoginFailsWithSignatureError();
    }

    @Test
    public void testLoginSucceedsWhenWantAssertionsSignedIsFalse() {
        configureSignedResponseWithUnsignedAssertion(false, true);
        loginUser();
    }

    @Test
    public void testLoginFailsWhenWantAssertionsSignedIsTrueAndValidateSignatureIsFalse() {
        configureSignedResponseWithUnsignedAssertion(true, false);
        assertLoginFailsWithSignatureError();
    }

    private void assertLoginFailsWithSignatureError() {
        oauth.openLoginForm();
        logInWithBroker();
        loginPage.fillLogin(getUserLogin(), getUserPassword());
        loginPage.submit();

        errorPage.assertCurrent();
        EventAssertion.assertError(events.poll())
                .type(EventType.IDENTITY_PROVIDER_RESPONSE_ERROR)
                .error(Errors.INVALID_SIGNATURE);
    }

    private void configureSignedResponseWithUnsignedAssertion(
            Boolean wantAssertionsSigned,
            boolean validateSignature) {
        String samlClientId = consumerRealm.getBaseUrl();
        ClientRepresentation client = providerRealm.admin().clients()
                .findByClientId(samlClientId).get(0);

        client.getAttributes().put(SamlConfigAttributes.SAML_ASSERTION_SIGNATURE, "false");
        client.getAttributes().put(SamlConfigAttributes.SAML_SERVER_SIGNATURE, "true");
        client.getAttributes().put(SamlConfigAttributes.SAML_SIGNATURE_ALGORITHM, "RSA_SHA256");
        providerRealm.admin().clients().get(client.getId()).update(client);

        IdentityProviderRepresentation idp =
                consumerRealm.admin().identityProviders().get(getIdpAlias()).toRepresentation();

        Map<String, String> config = idp.getConfig();
        config.put(SAMLIdentityProviderConfig.VALIDATE_SIGNATURE,
                Boolean.toString(validateSignature));
        config.put(SAMLIdentityProviderConfig.SIGNING_CERTIFICATE_KEY,
                activeRs256SigCert(providerRealm));

        if (wantAssertionsSigned == null) {
            config.remove(SAMLIdentityProviderConfig.WANT_ASSERTIONS_SIGNED);
        } else {
            config.put(SAMLIdentityProviderConfig.WANT_ASSERTIONS_SIGNED,
                    Boolean.toString(wantAssertionsSigned));
        }

        consumerRealm.admin().identityProviders().get(getIdpAlias()).update(idp);
    }
}

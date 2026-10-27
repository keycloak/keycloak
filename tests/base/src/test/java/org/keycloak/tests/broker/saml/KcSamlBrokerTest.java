package org.keycloak.tests.broker.saml;

import java.util.function.Consumer;

import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.https.InjectCertificates;
import org.keycloak.testframework.https.ManagedCertificates;
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
}

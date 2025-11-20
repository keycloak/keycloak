package org.keycloak.testsuite.broker;

import java.security.PublicKey;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.namespace.QName;

import org.keycloak.saml.RandomSecret;
import org.keycloak.saml.common.constants.JBossSAMLConstants;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.processing.core.util.XMLEncryptionUtil;
import org.keycloak.testsuite.util.saml.SamlDocumentStepBuilder;

import org.hamcrest.CoreMatchers;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import static org.keycloak.saml.common.constants.JBossSAMLURIConstants.ASSERTION_NSURI;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

public class KcSamlEncryptedIdTest extends AbstractKcSamlEncryptedElementsTest {

    @Override
    protected SamlDocumentStepBuilder.Saml2DocumentTransformer encryptDocument(PublicKey publicKey, String keyEncryptionAlgorithm, String keyEncryptionDigestMethod, String keyEncryptionMgfAlgorithm) {
        return document -> { // Replace Subject -> NameID with EncryptedId
            Node assertionElement = document.getDocumentElement()
                    .getElementsByTagNameNS(ASSERTION_NSURI.get(), JBossSAMLConstants.ASSERTION.get()).item(0);

            if (assertionElement == null) {
                throw new IllegalStateException("Unable to find assertion in saml response document");
            }

            String samlNSPrefix = assertionElement.getPrefix();
            String username;
            try {
                QName encryptedIdElementQName = new QName(ASSERTION_NSURI.get(), JBossSAMLConstants.ENCRYPTED_ID.get(), samlNSPrefix);
                QName nameIdQName = new QName(ASSERTION_NSURI.get(),
                        JBossSAMLConstants.NAMEID.get(), samlNSPrefix);

                // Add xmlns:saml attribute to NameId element,
                // this is necessary as it is decrypted as a separate doc and saml namespace is not know
                // unless added to NameId element
                Element nameIdElement = DocumentUtil.getElement(document, nameIdQName);
                if (nameIdElement == null) {
                    throw new RuntimeException("Assertion doesn't contain NameId " + DocumentUtil.asString(document));
                }
                nameIdElement.setAttribute("xmlns:" + samlNSPrefix, ASSERTION_NSURI.get());
                username = nameIdElement.getTextContent();

                byte[] secret = RandomSecret.createRandomSecret(128 / 8);
                SecretKey secretKey = new SecretKeySpec(secret, "AES");

                // encrypt the Assertion element and replace it with a EncryptedAssertion element.
                XMLEncryptionUtil.encryptElement(nameIdQName, document, publicKey,
                        secretKey, 128, encryptedIdElementQName, true, keyEncryptionAlgorithm, keyEncryptionDigestMethod, keyEncryptionMgfAlgorithm);
            } catch (Exception e) {
                throw new ProcessingException("failed to encrypt", e);
            }

            String doc = DocumentUtil.asString(document);
            assertThat(doc, not(containsString(username)));
            assertThat(doc, CoreMatchers.containsString(keyEncryptionAlgorithm));
            return document;
        };
    }
}

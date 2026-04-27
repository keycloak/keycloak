/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.testsuite.broker;

import java.security.PublicKey;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.namespace.QName;

import org.keycloak.saml.RandomSecret;
import org.keycloak.saml.common.constants.JBossSAMLConstants;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.processing.core.util.XMLEncryptionUtil;
import org.keycloak.testsuite.util.saml.SamlDocumentStepBuilder;

import org.w3c.dom.Node;

import static org.keycloak.saml.common.constants.JBossSAMLURIConstants.ASSERTION_NSURI;
import static org.keycloak.testsuite.utils.io.IOUtil.setDocElementAttributeValue;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

public class KcSamlEncryptedAssertionTest extends AbstractKcSamlEncryptedElementsTest {

    @Override
    protected SamlDocumentStepBuilder.Saml2DocumentTransformer encryptDocument(PublicKey publicKey, String keyEncryptionAlgorithm, String keyEncryptionDigestMethod, String keyEncryptionMgfAlgorithm) {
        return document -> { // Replace Assertion with EncryptedAssertion
            Node assertionElement = document.getDocumentElement()
                    .getElementsByTagNameNS(ASSERTION_NSURI.get(), JBossSAMLConstants.ASSERTION.get()).item(0);

            if (assertionElement == null) {
                throw new IllegalStateException("Unable to find assertion in saml response document");
            }

            String samlNSPrefix = assertionElement.getPrefix();

            // We need to add saml namespace to Assertion
            //  reason for that is because decryption is performed with assertion element extracted from the original
            //  document which has definition of saml namespace. After decrypting Assertion element without parent element
            //  saml namespace is not bound, so we add it
            setDocElementAttributeValue(document, samlNSPrefix + ":" + JBossSAMLConstants.ASSERTION.get(), "xmlns:saml", "urn:oasis:names:tc:SAML:2.0:assertion");

            try {

                QName encryptedAssertionElementQName = new QName(JBossSAMLURIConstants.ASSERTION_NSURI.get(),
                        JBossSAMLConstants.ENCRYPTED_ASSERTION.get(), samlNSPrefix);

                int encryptionKeySize = 128;

                byte[] secret = RandomSecret.createRandomSecret(encryptionKeySize / 8);
                SecretKey secretKey = new SecretKeySpec(secret, "AES");

                // encrypt the Assertion element and replace it with a EncryptedAssertion element.
                XMLEncryptionUtil.encryptElement(new QName(JBossSAMLURIConstants.ASSERTION_NSURI.get(),
                                JBossSAMLConstants.ASSERTION.get(), samlNSPrefix), document, publicKey,
                        secretKey, encryptionKeySize, encryptedAssertionElementQName, true, keyEncryptionAlgorithm, keyEncryptionDigestMethod, keyEncryptionMgfAlgorithm);
            } catch (Exception e) {
                throw new ProcessingException("failed to encrypt", e);
            }

            assertThat(DocumentUtil.asString(document), containsString(keyEncryptionAlgorithm));
            return document;
        };
    }
}

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

package org.keycloak.protocol.saml;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.keycloak.crypto.Algorithm;

import org.apache.xml.security.encryption.XMLCipher;

/**
 * This enum provides mapping between Keycloak provided encryption algorithms and algorithms from xmlsec.
 * It is used to make sure we are using keys generated for given algorithm only with that algorithm.
 */
public enum SAMLEncryptionAlgorithms {
    RSA_OAEP(Algorithm.RSA_OAEP, XMLCipher.RSA_OAEP, XMLCipher.RSA_OAEP_11),
    RSA1_5(Algorithm.RSA1_5, XMLCipher.RSA_v1dot5);

    private final String[] xmlEncIdentifier;
    private final String keycloakIdentifier;
    private static final Map<String, SAMLEncryptionAlgorithms> forKeycloakIdentifier;
    private static final Map<String, SAMLEncryptionAlgorithms> forXMLEncIdentifier;

    static {
        Map<String, SAMLEncryptionAlgorithms> forKeycloakIdentifierTmp = new HashMap<>();
        Map<String, SAMLEncryptionAlgorithms> forXMLEncIdentifierTmp = new HashMap<>();
        for (SAMLEncryptionAlgorithms alg: values()) {
            forKeycloakIdentifierTmp.put(alg.getKeycloakIdentifier(), alg);
            for (String xmlAlg : alg.getXmlEncIdentifiers()) {
                forXMLEncIdentifierTmp.put(xmlAlg, alg);
            }
        }
        forKeycloakIdentifier = Collections.unmodifiableMap(forKeycloakIdentifierTmp);
        forXMLEncIdentifier = Collections.unmodifiableMap(forXMLEncIdentifierTmp);
    }

    SAMLEncryptionAlgorithms(String keycloakIdentifier, String... xmlEncIdentifier) {
        assert xmlEncIdentifier.length > 0 : "xmlEncIdentifier should contain at least one identifier";
        this.xmlEncIdentifier = xmlEncIdentifier;
        this.keycloakIdentifier = keycloakIdentifier;
    }

    /**
     * Getter for all the XML encoding identifiers.
     * There should be at least one.
     * @return The array of XML encoding identifiers
     */
    public String[] getXmlEncIdentifiers() {
        return xmlEncIdentifier;
    }

    /**
     * Getter for the keycloak identifier.
     * @return The keycloak identifier.
     */
    public String getKeycloakIdentifier() {
        return keycloakIdentifier;
    }

    /**
     * Returns the SAMLEncryptionAlgorithms that contains the xml enc identifier.
     * @param xmlEncIdentifier The Xml encoding identifier
     * @return The associated SAMLEncryptionAlgorithms or null
     */
    public static SAMLEncryptionAlgorithms forXMLEncIdentifier(String xmlEncIdentifier) {
        return forXMLEncIdentifier.get(xmlEncIdentifier);
    }

    /**
     * Returns the SAMLEncryptionAlgorithms for the keycloak identifier.
     * @param keycloakIdentifier The keycloak identifier
     * @return The associated SAMLEncryptionAlgorithms or null
     */
    public static SAMLEncryptionAlgorithms forKeycloakIdentifier(String keycloakIdentifier) {
        return forKeycloakIdentifier.get(keycloakIdentifier);
    }
}

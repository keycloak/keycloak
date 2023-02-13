/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.dom.saml.v2.assertion;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <p>
 * Java class for EvidenceType complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="EvidenceType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;choice maxOccurs="unbounded">
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}AssertionIDRef"/>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}AssertionURIRef"/>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}Assertion"/>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}EncryptedAssertion"/>
 *       &lt;/choice>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
public class EvidenceType implements Serializable {

    protected List<ChoiceType> evidences = new ArrayList<>();

    /**
     * Add an evidence
     *
     * @param evidence
     */
    public void addEvidence(ChoiceType evidence) {
        evidences.add(evidence);
    }

    /**
     * Remove an evidence
     *
     * @param evidence
     */
    public void removeEvidence(ChoiceType evidence) {
        evidences.remove(evidence);
    }

    /**
     * Get the list of evidences as a read only list
     *
     * @return
     */
    public List<ChoiceType> evidences() {
        return Collections.unmodifiableList(evidences);
    }

    public static class ChoiceType implements Serializable {

        private String AssertionIDRef;
        private URI AssertionURIRef;
        private AssertionType assertion;
        private EncryptedAssertionType encryptedAssertion;

        public ChoiceType(String assertionIDRef) {
            AssertionIDRef = assertionIDRef;
        }

        public ChoiceType(URI assertionURIRef) {
            AssertionURIRef = assertionURIRef;
        }

        public ChoiceType(AssertionType assertion) {
            this.assertion = assertion;
        }

        public ChoiceType(EncryptedAssertionType encryptedAssertion) {
            this.encryptedAssertion = encryptedAssertion;
        }

        public String getAssertionIDRef() {
            return AssertionIDRef;
        }

        public URI getAssertionURIRef() {
            return AssertionURIRef;
        }

        public AssertionType getAssertion() {
            return assertion;
        }

        public EncryptedAssertionType getEncryptedAssertion() {
            return encryptedAssertion;
        }
    }
}
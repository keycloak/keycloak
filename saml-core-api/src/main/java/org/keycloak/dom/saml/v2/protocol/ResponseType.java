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
package org.keycloak.dom.saml.v2.protocol;

import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.EncryptedAssertionType;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <p>
 * Java class for ResponseType complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="ResponseType">
 *   &lt;complexContent>
 *     &lt;extension base="{urn:oasis:names:tc:SAML:2.0:protocol}StatusResponseType">
 *       &lt;choice maxOccurs="unbounded" minOccurs="0">
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}Assertion"/>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}EncryptedAssertion"/>
 *       &lt;/choice>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
public class ResponseType extends StatusResponseType {

    protected List<RTChoiceType> assertions = new ArrayList<>();

    public ResponseType(String id, XMLGregorianCalendar issueInstant) {
        super(id, issueInstant);
    }

    public ResponseType(StatusResponseType srt) {
        super(srt);
    }

    /**
     * Add an assertion
     *
     * @param choice
     */
    public void addAssertion(RTChoiceType choice) {
        assertions.add(choice);
    }

    /**
     * Remove an assertion
     *
     * @param choice
     */
    public void removeAssertion(RTChoiceType choice) {
        assertions.remove(choice);
    }

    /**
     * Replace the first assertion with the passed assertion
     *
     * @param id id of the old assertion
     * @param newAssertion
     */
    public void replaceAssertion(String id, RTChoiceType newAssertion) {
        int index = 0;
        if (id != null && !id.isEmpty()) {
            for (RTChoiceType assertion : assertions) {
                if (assertion.getID().equals(id)) {
                    break;
                }
                index++;
            }
        }
        assertions.remove(index);
        assertions.add(index, newAssertion);
    }

    /**
     * Gets a read only list of assertions
     */
    public List<RTChoiceType> getAssertions() {
        return Collections.unmodifiableList(assertions);
    }

    public static class RTChoiceType {

        private AssertionType assertion;

        private EncryptedAssertionType encryptedAssertion;

        private String id;

        public RTChoiceType(AssertionType assertion) {
            this.assertion = assertion;
            this.id = assertion.getID();
        }

        public RTChoiceType(EncryptedAssertionType encryptedAssertion) {
            this.encryptedAssertion = encryptedAssertion;

        }

        public AssertionType getAssertion() {
            return assertion;
        }

        public EncryptedAssertionType getEncryptedAssertion() {
            return encryptedAssertion;
        }

        public String getID() {
            return id;
        }
    }
}
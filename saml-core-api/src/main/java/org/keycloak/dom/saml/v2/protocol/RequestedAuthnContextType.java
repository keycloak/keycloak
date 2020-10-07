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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <p>
 * Java class for RequestedAuthnContextType complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="RequestedAuthnContextType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;choice>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}AuthnContextClassRef" maxOccurs="unbounded"/>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}AuthnContextDeclRef" maxOccurs="unbounded"/>
 *       &lt;/choice>
 *       &lt;attribute name="Comparison" type="{urn:oasis:names:tc:SAML:2.0:protocol}AuthnContextComparisonType" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
public class RequestedAuthnContextType {

    protected List<String> authnContextClassRef = new ArrayList<>();
    protected List<String> authnContextDeclRef = new ArrayList<>();
    protected AuthnContextComparisonType comparison;

    /**
     * Add an authn Context class ref
     *
     * @param str
     */
    public void addAuthnContextClassRef(String str) {
        this.authnContextClassRef.add(str);
    }

    /**
     * Add authn context decl ref
     *
     * @param str
     */
    public void addAuthnContextDeclRef(String str) {
        this.authnContextDeclRef.add(str);
    }

    /**
     * Remove an authn Context class ref
     *
     * @param str
     */
    public void removeAuthnContextClassRef(String str) {
        this.authnContextClassRef.remove(str);
    }

    /**
     * remove authn context decl ref
     *
     * @param str
     */
    public void removeAuthnContextDeclRef(String str) {
        this.authnContextDeclRef.remove(str);
    }

    /**
     * Gets the value of the authnContextClassRef property.
     */
    public List<String> getAuthnContextClassRef() {
        return Collections.unmodifiableList(this.authnContextClassRef);
    }

    /**
     * Gets the value of the authnContextDeclRef property.
     *
     * <p>
     * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to
     * the
     * returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method for the
     * authnContextDeclRef property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getAuthnContextDeclRef().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     */
    public List<String> getAuthnContextDeclRef() {
        return Collections.unmodifiableList(this.authnContextDeclRef);
    }

    /**
     * Gets the value of the comparison property.
     *
     * @return possible object is {@link AuthnContextComparisonType }
     */
    public AuthnContextComparisonType getComparison() {
        return comparison;
    }

    /**
     * Sets the value of the comparison property.
     *
     * @param value allowed object is {@link AuthnContextComparisonType }
     */
    public void setComparison(AuthnContextComparisonType value) {
        this.comparison = value;
    }
}
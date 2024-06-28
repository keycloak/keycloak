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
package org.keycloak.dom.saml.v2.metadata;

import org.keycloak.dom.saml.v2.assertion.AttributeType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <p>
 * Java class for IDPSSODescriptorType complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="IDPSSODescriptorType">
 *   &lt;complexContent>
 *     &lt;extension base="{urn:oasis:names:tc:SAML:2.0:metadata}SSODescriptorType">
 *       &lt;sequence>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:metadata}SingleSignOnService" maxOccurs="unbounded"/>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:metadata}NameIDMappingService" maxOccurs="unbounded"
 * minOccurs="0"/>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:metadata}AssertionIDRequestService" maxOccurs="unbounded"
 * minOccurs="0"/>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:metadata}AttributeProfile" maxOccurs="unbounded"
 * minOccurs="0"/>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}Attribute" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="WantAuthnRequestsSigned" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
public class IDPSSODescriptorType extends SSODescriptorType {

    protected List<EndpointType> singleSignOnService = new ArrayList<>();

    protected List<EndpointType> nameIDMappingService = new ArrayList<>();

    protected List<EndpointType> assertionIDRequestService = new ArrayList<>();

    protected List<String> attributeProfile = new ArrayList<>();

    protected List<AttributeType> attribute = new ArrayList<>();

    protected Boolean wantAuthnRequestsSigned = false;

    public IDPSSODescriptorType(List<String> protocolSupport) {
        super(protocolSupport);
    }

    /**
     * Add a SSO service
     *
     * @param endpt
     */
    public void addSingleSignOnService(EndpointType endpt) {
        this.singleSignOnService.add(endpt);
    }

    /**
     * Add name id mapping service
     *
     * @param endpt
     */
    public void addNameIDMappingService(EndpointType endpt) {
        this.nameIDMappingService.add(endpt);
    }

    /**
     * Add assertion id request service
     *
     * @param endpt
     */
    public void addAssertionIDRequestService(EndpointType endpt) {
        this.assertionIDRequestService.add(endpt);
    }

    /**
     * Add attribute profile
     *
     * @param str
     */
    public void addAttributeProfile(String str) {
        this.attributeProfile.add(str);
    }

    /**
     * Add attribute
     *
     * @param att
     */
    public void addAttribute(AttributeType att) {
        this.attribute.add(att);
    }

    /**
     * Remove a SSO service
     *
     * @param endpt
     */
    public void removeSingleSignOnService(EndpointType endpt) {
        this.singleSignOnService.remove(endpt);
    }

    /**
     * remove name id mapping service
     *
     * @param endpt
     */
    public void removeNameIDMappingService(EndpointType endpt) {
        this.nameIDMappingService.remove(endpt);
    }

    /**
     * remove assertion id request service
     *
     * @param endpt
     */
    public void removeAssertionIDRequestService(EndpointType endpt) {
        this.assertionIDRequestService.remove(endpt);
    }

    /**
     * Add attribute profile
     *
     * @param str
     */
    public void removeAttributeProfile(String str) {
        this.attributeProfile.remove(str);
    }

    /**
     * Add attribute
     *
     * @param att
     */
    public void removeAttribute(AttributeType att) {
        this.attribute.remove(att);
    }

    /**
     * Gets the value of the singleSignOnService property.
     * <p>
     * Objects of the following type(s) are allowed in the list {@link EndpointType }
     */
    public List<EndpointType> getSingleSignOnService() {
        return Collections.unmodifiableList(this.singleSignOnService);
    }

    /**
     * Gets the value of the nameIDMappingService property.
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link EndpointType }
     */
    public List<EndpointType> getNameIDMappingService() {
        return Collections.unmodifiableList(this.nameIDMappingService);
    }

    /**
     * Gets the value of the assertionIDRequestService property.
     * <p>
     * Objects of the following type(s) are allowed in the list {@link EndpointType }
     */
    public List<EndpointType> getAssertionIDRequestService() {
        return Collections.unmodifiableList(this.assertionIDRequestService);
    }

    /**
     * Gets the value of the attributeProfile property.
     *
     * <p>
     * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to
     * the
     * returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method for the
     * attributeProfile property.
     *
     * <p>
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     */
    public List<String> getAttributeProfile() {
        return Collections.unmodifiableList(this.attributeProfile);
    }

    /**
     * Gets the value of the attribute property.
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link AttributeType }
     */
    public List<AttributeType> getAttribute() {
        return Collections.unmodifiableList(this.attribute);
    }

    /**
     * Gets the value of the wantAuthnRequestsSigned property.
     *
     * @return possible object is {@link Boolean }
     */
    public Boolean isWantAuthnRequestsSigned() {
        return wantAuthnRequestsSigned;
    }

    /**
     * Sets the value of the wantAuthnRequestsSigned property.
     *
     * @param value allowed object is {@link Boolean }
     */
    public void setWantAuthnRequestsSigned(Boolean value) {
        this.wantAuthnRequestsSigned = value;
    }
}
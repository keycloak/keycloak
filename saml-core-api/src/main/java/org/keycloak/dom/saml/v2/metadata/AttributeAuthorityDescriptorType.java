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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.keycloak.dom.saml.v2.assertion.AttributeType;

/**
 * <p>
 * Java class for AttributeAuthorityDescriptorType complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="AttributeAuthorityDescriptorType">
 *   &lt;complexContent>
 *     &lt;extension base="{urn:oasis:names:tc:SAML:2.0:metadata}RoleDescriptorType">
 *       &lt;sequence>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:metadata}AttributeService" maxOccurs="unbounded"/>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:metadata}AssertionIDRequestService" maxOccurs="unbounded"
 * minOccurs="0"/>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:metadata}NameIDFormat" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:metadata}AttributeProfile" maxOccurs="unbounded"
 * minOccurs="0"/>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}Attribute" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */

public class AttributeAuthorityDescriptorType extends RoleDescriptorType {

    protected List<EndpointType> attributeService = new ArrayList<>();

    protected List<EndpointType> assertionIDRequestService = new ArrayList<>();

    protected List<String> nameIDFormat = new ArrayList<>();

    protected List<String> attributeProfile = new ArrayList<>();

    protected List<AttributeType> attribute = new ArrayList<>();

    public AttributeAuthorityDescriptorType(List<String> protocolSupport) {
        super(protocolSupport);
    }

    /**
     * Add an attribute service
     *
     * @param endpoint
     */
    public void addAttributeService(EndpointType endpoint) {
        this.attributeService.add(endpoint);
    }

    /**
     * Add an assertion id request service
     *
     * @param endpoint
     */
    public void addAssertionIDRequestService(EndpointType endpoint) {
        this.assertionIDRequestService.add(endpoint);
    }

    /**
     * Add a name id
     *
     * @param str
     */
    public void addNameIDFormat(String str) {
        this.nameIDFormat.add(str);
    }

    /**
     * Add an attribute profile
     *
     * @param str
     */
    public void addAttributeProfile(String str) {
        this.attributeProfile.add(str);
    }

    /**
     * Add an attribute
     *
     * @param attribute
     */
    public void addAttribute(AttributeType attribute) {
        this.attribute.add(attribute);
    }

    /**
     * Remove an attribute service
     *
     * @param endpoint
     */
    public void removeAttributeService(EndpointType endpoint) {
        this.attributeService.remove(endpoint);
    }

    /**
     * Remove assertion id request service
     *
     * @param endpoint
     */
    public void removeAssertionIDRequestService(EndpointType endpoint) {
        this.assertionIDRequestService.remove(endpoint);
    }

    /**
     * Remove Name ID
     *
     * @param str
     */
    public void removeNameIDFormat(String str) {
        this.nameIDFormat.remove(str);
    }

    /**
     * Remove attribute profile
     *
     * @param str
     */
    public void removeAttributeProfile(String str) {
        this.attributeProfile.remove(str);
    }

    /**
     * Remove attribute
     *
     * @param attribute
     */
    public void removeAttribute(AttributeType attribute) {
        this.attribute.remove(attribute);
    }

    /**
     * Gets the value of the attributeService property.
     * <p>
     * Objects of the following type(s) are allowed in the list {@link EndpointType }
     */
    public List<EndpointType> getAttributeService() {
        return Collections.unmodifiableList(this.attributeService);
    }

    /**
     * Gets the value of the assertionIDRequestService property.
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link EndpointType }
     */
    public List<EndpointType> getAssertionIDRequestService() {
        return Collections.unmodifiableList(this.assertionIDRequestService);
    }

    /**
     * Gets the value of the nameIDFormat property.
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     */
    public List<String> getNameIDFormat() {
        return Collections.unmodifiableList(this.nameIDFormat);
    }

    /**
     * Gets the value of the attributeProfile property.
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     */
    public List<String> getAttributeProfile() {
        return Collections.unmodifiableList(this.attributeProfile);
    }

    /**
     * Gets the value of the attribute property.
     * <p>
     * Objects of the following type(s) are allowed in the list {@link AttributeType }
     */
    public List<AttributeType> getAttribute() {
        return Collections.unmodifiableList(this.attribute);
    }
}
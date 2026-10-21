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

/**
 * <p>
 * Java class for OrganizationType complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="OrganizationType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:metadata}Extensions" minOccurs="0"/>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:metadata}OrganizationName" maxOccurs="unbounded"/>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:metadata}OrganizationDisplayName" maxOccurs="unbounded"/>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:metadata}OrganizationURL" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */

public class OrganizationType extends TypeWithOtherAttributes {

    protected ExtensionsType extensions;

    protected List<LocalizedNameType> organizationName = new ArrayList<>();

    protected List<LocalizedNameType> organizationDisplayName = new ArrayList<>();

    protected List<LocalizedURIType> organizationURL = new ArrayList<>();

    /**
     * Add an organization name
     *
     * @param name
     */
    public void addOrganizationName(LocalizedNameType name) {
        this.organizationName.add(name);
    }

    /**
     * Add organization display name
     *
     * @param name
     */
    public void addOrganizationDisplayName(LocalizedNameType name) {
        this.organizationDisplayName.add(name);
    }

    /**
     * Add organization url
     *
     * @param uri
     */
    public void addOrganizationURL(LocalizedURIType uri) {
        this.organizationURL.add(uri);
    }

    /**
     * remove an organization name
     *
     * @param name
     */
    public void removeOrganizationName(LocalizedNameType name) {
        this.organizationName.remove(name);
    }

    /**
     * remove organization display name
     *
     * @param name
     */
    public void removeOrganizationDisplayName(LocalizedNameType name) {
        this.organizationDisplayName.remove(name);
    }

    /**
     * remove organization url
     *
     * @param uri
     */
    public void removeOrganizationURL(LocalizedURIType uri) {
        this.organizationURL.remove(uri);
    }

    /**
     * Gets the value of the extensions property.
     *
     * @return possible object is {@link ExtensionsType }
     */
    public ExtensionsType getExtensions() {
        return extensions;
    }

    /**
     * Sets the value of the extensions property.
     *
     * @param value allowed object is {@link ExtensionsType }
     */
    public void setExtensions(ExtensionsType value) {
        this.extensions = value;
    }

    /**
     * Gets the value of the organizationName property.
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link LocalizedNameType }
     */
    public List<LocalizedNameType> getOrganizationName() {
        return Collections.unmodifiableList(this.organizationName);
    }

    /**
     * Gets the value of the organizationDisplayName property.
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link LocalizedNameType }
     */
    public List<LocalizedNameType> getOrganizationDisplayName() {
        return Collections.unmodifiableList(this.organizationDisplayName);
    }

    /**
     * Gets the value of the organizationURL property.
     */
    public List<LocalizedURIType> getOrganizationURL() {
        return Collections.unmodifiableList(this.organizationURL);
    }
}
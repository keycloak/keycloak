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
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import org.w3c.dom.Element;

/**
 * <p>
 * Java class for RoleDescriptorType complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="RoleDescriptorType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{http://www.w3.org/2000/09/xmldsig#}Signature" minOccurs="0"/>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:metadata}Extensions" minOccurs="0"/>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:metadata}KeyDescriptor" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:metadata}Organization" minOccurs="0"/>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:metadata}ContactPerson" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="ID" type="{http://www.w3.org/2001/XMLSchema}ID" />
 *       &lt;attribute name="validUntil" type="{http://www.w3.org/2001/XMLSchema}dateTime" />
 *       &lt;attribute name="cacheDuration" type="{http://www.w3.org/2001/XMLSchema}duration" />
 *       &lt;attribute name="protocolSupportEnumeration" use="required" type="{urn:oasis:names:tc:SAML:2.0:metadata}anyURIListType"
 * />
 *       &lt;attribute name="errorURL" type="{http://www.w3.org/2001/XMLSchema}anyURI" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
public abstract class RoleDescriptorType extends TypeWithOtherAttributes {

    protected Element signature;

    protected ExtensionsType extensions;

    protected List<KeyDescriptorType> keyDescriptor = new ArrayList<>();

    protected OrganizationType organization;

    protected List<ContactType> contactPerson = new ArrayList<>();

    protected String id;

    protected XMLGregorianCalendar validUntil;

    protected Duration cacheDuration;

    protected List<String> protocolSupportEnumeration = new ArrayList<>();

    protected String errorURL;

    public RoleDescriptorType(List<String> protocolSupport) {
        protocolSupportEnumeration.addAll(protocolSupport);
    }

    /**
     * Add key descriptor
     *
     * @param keyD
     */
    public void addKeyDescriptor(KeyDescriptorType keyD) {
        this.keyDescriptor.add(keyD);
    }

    /**
     * Add contact
     *
     * @param contact
     */
    public void addContactPerson(ContactType contact) {
        this.contactPerson.add(contact);
    }

    /**
     * remove key descriptor
     *
     * @param keyD
     */
    public void removeKeyDescriptor(KeyDescriptorType keyD) {
        this.keyDescriptor.remove(keyD);
    }

    /**
     * remove contact
     *
     * @param contact
     */
    public void removeContactPerson(ContactType contact) {
        this.contactPerson.remove(contact);
    }

    /**
     * Gets the value of the signature property.
     *
     * @return possible object is {@link org.keycloak.dom.xmlsec.w3.xmldsig.SignatureType }
     */
    public Element getSignature() {
        return signature;
    }

    /**
     * Sets the value of the signature property.
     *
     * @param value allowed object is {@link org.keycloak.dom.xmlsec.w3.xmldsig.SignatureType }
     */
    public void setSignature(Element value) {
        this.signature = value;
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
     * Gets the value of the keyDescriptor property.
     * <p>
     * Objects of the following type(s) are allowed in the list {@link KeyDescriptorType }
     */
    public List<KeyDescriptorType> getKeyDescriptor() {
        return Collections.unmodifiableList(this.keyDescriptor);
    }

    /**
     * Gets the value of the organization property.
     *
     * @return possible object is {@link OrganizationType }
     */
    public OrganizationType getOrganization() {
        return organization;
    }

    /**
     * Sets the value of the organization property.
     *
     * @param value allowed object is {@link OrganizationType }
     */
    public void setOrganization(OrganizationType value) {
        this.organization = value;
    }

    /**
     * Gets the value of the contactPerson property.
     * <p>
     * Objects of the following type(s) are allowed in the list {@link ContactType }
     */
    public List<ContactType> getContactPerson() {
        return Collections.unmodifiableList(this.contactPerson);
    }

    /**
     * Gets the value of the id property.
     *
     * @return possible object is {@link String }
     */
    public String getID() {
        return id;
    }

    /**
     * Sets the value of the id property.
     *
     * @param value allowed object is {@link String }
     */
    public void setID(String value) {
        this.id = value;
    }

    /**
     * Gets the value of the validUntil property.
     *
     * @return possible object is {@link XMLGregorianCalendar }
     */
    public XMLGregorianCalendar getValidUntil() {
        return validUntil;
    }

    /**
     * Sets the value of the validUntil property.
     *
     * @param value allowed object is {@link XMLGregorianCalendar }
     */
    public void setValidUntil(XMLGregorianCalendar value) {
        this.validUntil = value;
    }

    /**
     * Gets the value of the cacheDuration property.
     *
     * @return possible object is {@link Duration }
     */
    public Duration getCacheDuration() {
        return cacheDuration;
    }

    /**
     * Sets the value of the cacheDuration property.
     *
     * @param value allowed object is {@link Duration }
     */
    public void setCacheDuration(Duration value) {
        this.cacheDuration = value;
    }

    /**
     * Gets the value of the protocolSupportEnumeration property.
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     */
    public List<String> getProtocolSupportEnumeration() {
        return Collections.unmodifiableList(this.protocolSupportEnumeration);
    }

    /**
     * Gets the value of the errorURL property.
     *
     * @return possible object is {@link String }
     */
    public String getErrorURL() {
        return errorURL;
    }

    /**
     * Sets the value of the errorURL property.
     *
     * @param value allowed object is {@link String }
     */
    public void setErrorURL(String value) {
        this.errorURL = value;
    }
}
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
 * Java class for ContactType complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="ContactType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:metadata}Extensions" minOccurs="0"/>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:metadata}Company" minOccurs="0"/>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:metadata}GivenName" minOccurs="0"/>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:metadata}SurName" minOccurs="0"/>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:metadata}EmailAddress" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:metadata}TelephoneNumber" maxOccurs="unbounded"
 * minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="contactType" use="required" type="{urn:oasis:names:tc:SAML:2.0:metadata}ContactTypeType"
 * />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
public class ContactType extends TypeWithOtherAttributes {

    protected ExtensionsType extensions;

    protected String company;

    protected String givenName;

    protected String surName;

    protected List<String> emailAddress = new ArrayList<>();

    protected List<String> telephoneNumber = new ArrayList<>();

    protected ContactTypeType contactType;

    public ContactType(ContactTypeType contactType) {
        this.contactType = contactType;
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
     * Gets the value of the company property.
     *
     * @return possible object is {@link String }
     */
    public String getCompany() {
        return company;
    }

    /**
     * Sets the value of the company property.
     *
     * @param value allowed object is {@link String }
     */
    public void setCompany(String value) {
        this.company = value;
    }

    /**
     * Gets the value of the givenName property.
     *
     * @return possible object is {@link String }
     */
    public String getGivenName() {
        return givenName;
    }

    /**
     * Sets the value of the givenName property.
     *
     * @param value allowed object is {@link String }
     */
    public void setGivenName(String value) {
        this.givenName = value;
    }

    /**
     * Gets the value of the surName property.
     *
     * @return possible object is {@link String }
     */
    public String getSurName() {
        return surName;
    }

    /**
     * Sets the value of the surName property.
     *
     * @param value allowed object is {@link String }
     */
    public void setSurName(String value) {
        this.surName = value;
    }

    /**
     * Add an email address
     *
     * @param email
     */
    public void addEmailAddress(String email) {
        this.emailAddress.add(email);
    }

    /**
     * remove a telephone
     *
     * @param tel
     */
    public void removeTelephone(String tel) {
        this.telephoneNumber.remove(tel);
    }

    /**
     * remove an email address
     *
     * @param email
     */
    public void removeEmailAddress(String email) {
        this.emailAddress.remove(email);
    }

    /**
     * Add a telephone
     *
     * @param tel
     */
    public void addTelephone(String tel) {
        this.telephoneNumber.add(tel);
    }

    /**
     * Gets the value of the emailAddress property.
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     */
    public List<String> getEmailAddress() {
        return Collections.unmodifiableList(this.emailAddress);
    }

    /**
     * Gets the value of the telephoneNumber property.
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     */
    public List<String> getTelephoneNumber() {
        return Collections.unmodifiableList(this.telephoneNumber);
    }

    /**
     * Gets the value of the contactType property.
     *
     * @return possible object is {@link ContactTypeType }
     */
    public ContactTypeType getContactType() {
        return contactType;
    }

    /**
     * Sets the value of the contactType property.
     *
     * @param value allowed object is {@link ContactTypeType }
     */
    public void setContactType(ContactTypeType value) {
        this.contactType = value;
    }
}
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

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Java class for SubjectConfirmationDataType complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="SubjectConfirmationDataType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;any/>
 *       &lt;/sequence>
 *       &lt;attribute name="NotBefore" type="{http://www.w3.org/2001/XMLSchema}dateTime" />
 *       &lt;attribute name="NotOnOrAfter" type="{http://www.w3.org/2001/XMLSchema}dateTime" />
 *       &lt;attribute name="Recipient" type="{http://www.w3.org/2001/XMLSchema}anyURI" />
 *       &lt;attribute name="InResponseTo" type="{http://www.w3.org/2001/XMLSchema}NCName" />
 *       &lt;attribute name="Address" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
public class SubjectConfirmationDataType implements Serializable {

    protected XMLGregorianCalendar notBefore;

    protected XMLGregorianCalendar notOnOrAfter;

    protected String recipient;

    protected String inResponseTo;

    protected String address;

    private final Map<QName, String> otherAttributes = new HashMap<>();

    private Object anyType;

    public Object getAnyType() {
        return anyType;
    }

    public void setAnyType(Object anyType) {
        this.anyType = anyType;
    }

    /**
     * Gets the value of the notBefore property.
     *
     * @return possible object is {@link XMLGregorianCalendar }
     */
    public XMLGregorianCalendar getNotBefore() {
        return notBefore;
    }

    /**
     * Sets the value of the notBefore property.
     *
     * @param value allowed object is {@link XMLGregorianCalendar }
     */
    public void setNotBefore(XMLGregorianCalendar value) {
        this.notBefore = value;
    }

    /**
     * Gets the value of the notOnOrAfter property.
     *
     * @return possible object is {@link XMLGregorianCalendar }
     */
    public XMLGregorianCalendar getNotOnOrAfter() {
        return notOnOrAfter;
    }

    /**
     * Sets the value of the notOnOrAfter property.
     *
     * @param value allowed object is {@link XMLGregorianCalendar }
     */
    public void setNotOnOrAfter(XMLGregorianCalendar value) {
        this.notOnOrAfter = value;
    }

    /**
     * Gets the value of the recipient property.
     *
     * @return possible object is {@link String }
     */
    public String getRecipient() {
        return recipient;
    }

    /**
     * Sets the value of the recipient property.
     *
     * @param value allowed object is {@link String }
     */
    public void setRecipient(String value) {
        this.recipient = value;
    }

    /**
     * Gets the value of the inResponseTo property.
     *
     * @return possible object is {@link String }
     */
    public String getInResponseTo() {
        return inResponseTo;
    }

    /**
     * Sets the value of the inResponseTo property.
     *
     * @param value allowed object is {@link String }
     */
    public void setInResponseTo(String value) {
        this.inResponseTo = value;
    }

    /**
     * Gets the value of the address property.
     *
     * @return possible object is {@link String }
     */
    public String getAddress() {
        return address;
    }

    /**
     * Sets the value of the address property.
     *
     * @param value allowed object is {@link String }
     */
    public void setAddress(String value) {
        this.address = value;
    }

    /**
     * Gets a map that contains attributes that aren't bound to any typed property on this class.
     *
     * <p>
     * the map is keyed by the name of the attribute and the value is the string value of the attribute.
     *
     * @return always non-null
     */
    public Map<QName, String> getOtherAttributes() {
        return Collections.unmodifiableMap(otherAttributes);
    }

    /**
     * Add an other attribute
     *
     * @param qname
     * @param str
     */
    public void addOtherAttribute(QName qname, String str) {
        otherAttributes.put(qname, str);
    }

    /**
     * Remove an other attribute
     *
     * @param qname {@link QName} of the attribute to be removed
     */
    public void removeOtherAttribute(QName qname) {
        otherAttributes.remove(qname);
    }
}
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

import java.net.URI;
import javax.xml.datatype.XMLGregorianCalendar;

import org.keycloak.dom.saml.v2.assertion.ConditionsType;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.assertion.SubjectType;

/**
 * <p>
 * Java class for AuthnRequestType complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="AuthnRequestType">
 *   &lt;complexContent>
 *     &lt;extension base="{urn:oasis:names:tc:SAML:2.0:protocol}RequestAbstractType">
 *       &lt;sequence>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}Subject" minOccurs="0"/>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:protocol}NameIDPolicy" minOccurs="0"/>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}Conditions" minOccurs="0"/>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:protocol}RequestedAuthnContext" minOccurs="0"/>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:protocol}Scoping" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="ForceAuthn" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="IsPassive" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="ProtocolBinding" type="{http://www.w3.org/2001/XMLSchema}anyURI" />
 *       &lt;attribute name="AssertionConsumerServiceIndex" type="{http://www.w3.org/2001/XMLSchema}unsignedShort" />
 *       &lt;attribute name="AssertionConsumerServiceURL" type="{http://www.w3.org/2001/XMLSchema}anyURI" />
 *       &lt;attribute name="AttributeConsumingServiceIndex" type="{http://www.w3.org/2001/XMLSchema}unsignedShort" />
 *       &lt;attribute name="ProviderName" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
public class AuthnRequestType extends RequestAbstractType {

    protected SubjectType subject;

    protected NameIDPolicyType nameIDPolicy;

    protected ConditionsType conditions;

    protected RequestedAuthnContextType requestedAuthnContext;

    protected ScopingType scoping;

    protected Boolean forceAuthn = Boolean.FALSE;

    protected Boolean isPassive = Boolean.FALSE;

    protected URI protocolBinding;

    protected Integer assertionConsumerServiceIndex;

    protected URI assertionConsumerServiceURL;

    protected Integer attributeConsumingServiceIndex;

    protected String providerName;

    public AuthnRequestType(String id, XMLGregorianCalendar instant) {
        super(id, instant);
    }

    /**
     * Gets the value of the subject property.
     *
     * @return possible object is {@link SubjectType }
     */
    public SubjectType getSubject() {
        return subject;
    }

    /**
     * Sets the value of the subject property.
     *
     * @param value allowed object is {@link SubjectType }
     */
    public void setSubject(SubjectType value) {
        this.subject = value;
    }

    /**
     * Gets the value of the nameIDPolicy property.
     *
     * @return possible object is {@link NameIDPolicyType }
     */
    public NameIDPolicyType getNameIDPolicy() {
        return nameIDPolicy;
    }

    /**
     * Sets the value of the nameIDPolicy property.
     *
     * @param value allowed object is {@link NameIDPolicyType }
     */
    public void setNameIDPolicy(NameIDPolicyType value) {
        this.nameIDPolicy = value;
    }

    /**
     * Gets the value of the conditions property.
     *
     * @return possible object is {@link ConditionsType }
     */
    public ConditionsType getConditions() {
        return conditions;
    }

    /**
     * Sets the value of the conditions property.
     *
     * @param value allowed object is {@link ConditionsType }
     */
    public void setConditions(ConditionsType value) {
        this.conditions = value;
    }

    /**
     * Gets the value of the requestedAuthnContext property.
     *
     * @return possible object is {@link RequestedAuthnContextType }
     */
    public RequestedAuthnContextType getRequestedAuthnContext() {
        return requestedAuthnContext;
    }

    /**
     * Sets the value of the requestedAuthnContext property.
     *
     * @param value allowed object is {@link RequestedAuthnContextType }
     */
    public void setRequestedAuthnContext(RequestedAuthnContextType value) {
        this.requestedAuthnContext = value;
    }

    /**
     * Gets the value of the scoping property.
     *
     * @return possible object is {@link ScopingType }
     */
    public ScopingType getScoping() {
        return scoping;
    }

    /**
     * Sets the value of the scoping property.
     *
     * @param value allowed object is {@link ScopingType }
     */
    public void setScoping(ScopingType value) {
        this.scoping = value;
    }

    /**
     * Gets the value of the forceAuthn property.
     *
     * @return possible object is {@link Boolean }
     */
    public Boolean isForceAuthn() {
        return forceAuthn;
    }

    /**
     * Sets the value of the forceAuthn property.
     *
     * @param value allowed object is {@link Boolean }
     */
    public void setForceAuthn(Boolean value) {
        this.forceAuthn = value;
    }

    /**
     * Gets the value of the isPassive property.
     *
     * @return possible object is {@link Boolean }
     */
    public Boolean isIsPassive() {
        return isPassive;
    }

    /**
     * Sets the value of the isPassive property.
     *
     * @param value allowed object is {@link Boolean }
     */
    public void setIsPassive(Boolean value) {
        this.isPassive = value;
    }

    /**
     * Gets the value of the protocolBinding property.
     *
     * @return possible object is {@link String }
     */
    public URI getProtocolBinding() {
        return protocolBinding;
    }

    /**
     * Sets the value of the protocolBinding property.
     *
     * @param value allowed object is {@link String }
     */
    public void setProtocolBinding(URI value) {
        this.protocolBinding = value;
    }

    /**
     * Gets the value of the assertionConsumerServiceIndex property.
     *
     * @return possible object is {@link Integer }
     */
    public Integer getAssertionConsumerServiceIndex() {
        return assertionConsumerServiceIndex;
    }

    /**
     * Sets the value of the assertionConsumerServiceIndex property.
     *
     * @param value allowed object is {@link Integer }
     */
    public void setAssertionConsumerServiceIndex(Integer value) {
        this.assertionConsumerServiceIndex = value;
    }

    /**
     * Gets the value of the assertionConsumerServiceURL property.
     *
     * @return possible object is {@link String }
     */
    public URI getAssertionConsumerServiceURL() {
        return assertionConsumerServiceURL;
    }

    /**
     * Sets the value of the assertionConsumerServiceURL property.
     *
     * @param value allowed object is {@link String }
     */
    public void setAssertionConsumerServiceURL(URI value) {
        this.assertionConsumerServiceURL = value;
    }

    /**
     * Gets the value of the attributeConsumingServiceIndex property.
     *
     * @return possible object is {@link Integer }
     */
    public Integer getAttributeConsumingServiceIndex() {
        return attributeConsumingServiceIndex;
    }

    /**
     * Sets the value of the attributeConsumingServiceIndex property.
     *
     * @param value allowed object is {@link Integer }
     */
    public void setAttributeConsumingServiceIndex(Integer value) {
        this.attributeConsumingServiceIndex = value;
    }

    /**
     * Gets the value of the providerName property.
     *
     * @return possible object is {@link String }
     */
    public String getProviderName() {
        return providerName;
    }

    /**
     * Sets the value of the providerName property.
     *
     * @param value allowed object is {@link String }
     */
    public void setProviderName(String value) {
        this.providerName = value;
    }

    public URI getSenderURL() {
        URI assertionConsumerServiceURL = getAssertionConsumerServiceURL();

        if (assertionConsumerServiceURL != null) {
            return assertionConsumerServiceURL;
        }

        NameIDType issuer = getIssuer();

        try {
            if (issuer != null) {
                return URI.create(issuer.getValue());
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not resolve sender URL using the Issuer value [" + issuer.getValue() + "]." , e);
        }

        throw new RuntimeException("Could not resolve sender URL. AuthnRequest must have a value for AssertionConsumerServiceURL or Issuer.");
    }
}
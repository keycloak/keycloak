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
 * Java class for SPSSODescriptorType complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="SPSSODescriptorType">
 *   &lt;complexContent>
 *     &lt;extension base="{urn:oasis:names:tc:SAML:2.0:metadata}SSODescriptorType">
 *       &lt;sequence>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:metadata}AssertionConsumerService" maxOccurs="unbounded"/>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:metadata}AttributeConsumingService" maxOccurs="unbounded"
 * minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="AuthnRequestsSigned" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="WantAssertionsSigned" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
public class SPSSODescriptorType extends SSODescriptorType {

    protected List<IndexedEndpointType> assertionConsumerService = new ArrayList<>();

    protected List<AttributeConsumingServiceType> attributeConsumingService = new ArrayList<>();

    protected boolean authnRequestsSigned = false;

    protected boolean wantAssertionsSigned = false;

    public SPSSODescriptorType(List<String> protocolSupport) {
        super(protocolSupport);
    }

    /**
     * Add an Assertion Consumer Service
     *
     * @param assertionConsumer an endpoint of type {@link IndexedEndpointType}
     */
    public void addAssertionConsumerService(IndexedEndpointType assertionConsumer) {
        this.assertionConsumerService.add(assertionConsumer);
    }

    /**
     * Add an attribute consumer
     *
     * @param attributeConsumer an instance of type {@link AttributeConsumingServiceType}
     */
    public void addAttributeConsumerService(AttributeConsumingServiceType attributeConsumer) {
        this.attributeConsumingService.add(attributeConsumer);
    }

    /**
     * Remove an Assertion Consumer Service
     *
     * @param assertionConsumer an endpoint of type {@link IndexedEndpointType}
     */
    public void removeAssertionConsumerService(IndexedEndpointType assertionConsumer) {
        this.assertionConsumerService.remove(assertionConsumer);
    }

    /**
     * Remove an attribute consumer
     *
     * @param attributeConsumer an instance of type {@link AttributeConsumingServiceType}
     */
    public void removeAttributeConsumerService(AttributeConsumingServiceType attributeConsumer) {
        this.attributeConsumingService.remove(attributeConsumer);
    }

    /**
     * Gets the value of the assertionConsumerService property.
     * <p>
     * Objects of the following type(s) are allowed in the list {@link IndexedEndpointType }
     */
    public List<IndexedEndpointType> getAssertionConsumerService() {
        return Collections.unmodifiableList(this.assertionConsumerService);
    }

    /**
     * Gets the value of the attributeConsumingService property.
     * <p>
     * Objects of the following type(s) are allowed in the list {@link AttributeConsumingServiceType }
     */
    public List<AttributeConsumingServiceType> getAttributeConsumingService() {
        return Collections.unmodifiableList(this.attributeConsumingService);
    }

    /**
     * Gets the value of the authnRequestsSigned property.
     *
     * @return possible object is {@link Boolean }
     */
    public Boolean isAuthnRequestsSigned() {
        return authnRequestsSigned;
    }

    /**
     * Sets the value of the authnRequestsSigned property.
     *
     * @param value allowed object is {@link Boolean }
     */
    public void setAuthnRequestsSigned(Boolean value) {
        this.authnRequestsSigned = value;
    }

    /**
     * Gets the value of the wantAssertionsSigned property.
     *
     * @return possible object is {@link Boolean }
     */
    public Boolean isWantAssertionsSigned() {
        return wantAssertionsSigned;
    }

    /**
     * Sets the value of the wantAssertionsSigned property.
     *
     * @param value allowed object is {@link Boolean }
     */
    public void setWantAssertionsSigned(Boolean value) {
        this.wantAssertionsSigned = value;
    }
}
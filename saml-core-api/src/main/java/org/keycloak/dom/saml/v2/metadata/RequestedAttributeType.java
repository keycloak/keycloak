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

/**
 * <p>
 * Java class for RequestedAttributeType complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="RequestedAttributeType">
 *   &lt;complexContent>
 *     &lt;extension base="{urn:oasis:names:tc:SAML:2.0:assertion}AttributeType">
 *       &lt;attribute name="isRequired" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
public class RequestedAttributeType extends AttributeType {

    public RequestedAttributeType(String name) {
        super(name);
    }

    protected Boolean isRequired = Boolean.FALSE;

    /**
     * Gets the value of the isRequired property.
     *
     * @return possible object is {@link Boolean }
     */
    public Boolean isIsRequired() {
        return isRequired;
    }

    /**
     * Sets the value of the isRequired property.
     *
     * @param value allowed object is {@link Boolean }
     */
    public void setIsRequired(Boolean value) {
        this.isRequired = value;
    }
}
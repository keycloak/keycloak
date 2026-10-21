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
package org.keycloak.dom.saml.v1.protocol;

import java.io.Serializable;
import javax.xml.namespace.QName;

/**
 * <complexType name="StatusCodeType"> <sequence> <element ref="samlp:StatusCode" minOccurs="0"/> </sequence>
 * <attribute
 * name="Value" type="QName" use="required"/> </complexType>
 *
 * @author Anil.Saldhana@redhat.com
 * @since Jun 22, 2011
 */
public class SAML11StatusCodeType implements Serializable {

    public static final SAML11StatusCodeType SUCCESS = new SAML11StatusCodeType(new QName("samlp:Success"));

    protected SAML11StatusCodeType statusCode;

    protected QName value;

    public SAML11StatusCodeType(QName theValue) {
        value = theValue;
    }

    /**
     * Gets the value of the statusCode property.
     *
     * @return possible object is {@link StatusCodeType }
     */
    public SAML11StatusCodeType getStatusCode() {
        return statusCode;
    }

    /**
     * Sets the value of the statusCode property.
     *
     * @param value allowed object is {@link StatusCodeType }
     */
    public void setStatusCode(SAML11StatusCodeType value) {
        this.statusCode = value;
    }

    /**
     * Gets the value of the value property.
     *
     * @return possible object is {@link String }
     */
    public QName getValue() {
        return value;
    }
}
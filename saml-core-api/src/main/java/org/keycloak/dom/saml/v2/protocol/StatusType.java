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

import java.io.Serializable;

/**
 * <p>
 * Java class for StatusType complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="StatusType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:protocol}StatusCode"/>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:protocol}StatusMessage" minOccurs="0"/>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:protocol}StatusDetail" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
public class StatusType implements Serializable {

    protected String statusMessage;
    protected StatusCodeType statusCode;
    protected StatusDetailType statusDetail;

    /**
     * Gets the value of the statusCode property.
     *
     * @return possible object is {@link StatusCodeType }
     */
    public StatusCodeType getStatusCode() {
        return statusCode;
    }

    /**
     * Sets the value of the statusCode property.
     *
     * @param value allowed object is {@link StatusCodeType }
     */
    public void setStatusCode(StatusCodeType value) {
        this.statusCode = value;
    }

    /**
     * Gets the value of the statusMessage property.
     *
     * @return possible object is {@link String }
     */
    public String getStatusMessage() {
        return statusMessage;
    }

    /**
     * Sets the value of the statusMessage property.
     *
     * @param value allowed object is {@link String }
     */
    public void setStatusMessage(String value) {
        this.statusMessage = value;
    }

    /**
     * Gets the value of the statusDetail property.
     *
     * @return possible object is {@link StatusDetailType }
     */
    public StatusDetailType getStatusDetail() {
        return statusDetail;
    }

    /**
     * Sets the value of the statusDetail property.
     *
     * @param value allowed object is {@link StatusDetailType }
     */
    public void setStatusDetail(StatusDetailType value) {
        this.statusDetail = value;
    }

    @Override
    public String toString() {
        return "StatusType [statusCode=" + statusCode + ", statusMessage=" + statusMessage + ", statusDetail=" + statusDetail + "]";
    }

}

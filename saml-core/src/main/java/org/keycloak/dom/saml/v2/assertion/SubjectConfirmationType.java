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

import java.io.Serializable;

/**
 * <p>
 * Java class for SubjectConfirmationType complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="SubjectConfirmationType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;choice minOccurs="0">
 *           &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}BaseID"/>
 *           &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}NameID"/>
 *           &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}EncryptedID"/>
 *         &lt;/choice>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}SubjectConfirmationData" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="Method" use="required" type="{http://www.w3.org/2001/XMLSchema}anyURI" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
public class SubjectConfirmationType implements Serializable {

    protected BaseIDAbstractType baseID;
    protected NameIDType nameID;
    protected EncryptedElementType encryptedID;
    protected SubjectConfirmationDataType subjectConfirmationData;
    protected String method;

    /**
     * Gets the value of the baseID property.
     *
     * @return possible object is {@link BaseIDAbstractType }
     */
    public BaseIDAbstractType getBaseID() {
        return baseID;
    }

    /**
     * Sets the value of the baseID property.
     *
     * @param value allowed object is {@link BaseIDAbstractType }
     */
    public void setBaseID(BaseIDAbstractType value) {
        this.baseID = value;
    }

    /**
     * Gets the value of the nameID property.
     *
     * @return possible object is {@link NameIDType }
     */
    public NameIDType getNameID() {
        return nameID;
    }

    /**
     * Sets the value of the nameID property.
     *
     * @param value allowed object is {@link NameIDType }
     */
    public void setNameID(NameIDType value) {
        this.nameID = value;
    }

    /**
     * Gets the value of the encryptedID property.
     *
     * @return possible object is {@link EncryptedElementType }
     */
    public EncryptedElementType getEncryptedID() {
        return encryptedID;
    }

    /**
     * Sets the value of the encryptedID property.
     *
     * @param value allowed object is {@link EncryptedElementType }
     */
    public void setEncryptedID(EncryptedElementType value) {
        this.encryptedID = value;
    }

    /**
     * Gets the value of the subjectConfirmationData property.
     *
     * @return possible object is {@link SubjectConfirmationDataType }
     */
    public SubjectConfirmationDataType getSubjectConfirmationData() {
        return subjectConfirmationData;
    }

    /**
     * Sets the value of the subjectConfirmationData property.
     *
     * @param value allowed object is {@link SubjectConfirmationDataType }
     */
    public void setSubjectConfirmationData(SubjectConfirmationDataType value) {
        this.subjectConfirmationData = value;
    }

    /**
     * Gets the value of the method property.
     *
     * @return possible object is {@link String }
     */
    public String getMethod() {
        return method;
    }

    /**
     * Sets the value of the method property.
     *
     * @param value allowed object is {@link String }
     */
    public void setMethod(String value) {
        this.method = value;
    }
}
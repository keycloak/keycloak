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

import org.keycloak.dom.xmlsec.w3.xmlenc.EncryptionMethodType;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <p>
 * Java class for KeyDescriptorType complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="KeyDescriptorType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{http://www.w3.org/2000/09/xmldsig#}KeyInfo"/>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:metadata}EncryptionMethod" maxOccurs="unbounded"
 * minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="use" type="{urn:oasis:names:tc:SAML:2.0:metadata}KeyTypes" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
public class KeyDescriptorType {

    protected Element keyInfo;

    protected List<EncryptionMethodType> encryptionMethod = new ArrayList<>();

    protected KeyTypes use;

    /**
     * Gets the value of the keyInfo property.
     *
     * @return possible object is {@link org.keycloak.dom.xmlsec.w3.xmldsig.KeyInfoType }
     */
    public Element getKeyInfo() {
        return keyInfo;
    }

    /**
     * Sets the value of the keyInfo property.
     *
     * @param value allowed object is {@link org.keycloak.dom.xmlsec.w3.xmldsig.KeyInfoType }
     */
    public void setKeyInfo(Element value) {
        this.keyInfo = value;
    }

    /**
     * Add encryption method type
     *
     * @param e
     */
    public void addEncryptionMethod(EncryptionMethodType e) {
        this.encryptionMethod.add(e);
    }

    /**
     * Remove encryption method type
     *
     * @param e
     */
    public void removeEncryptionMethod(EncryptionMethodType e) {
        this.encryptionMethod.remove(e);
    }

    /**
     * Gets the value of the encryptionMethod property.
     * <p>
     * Objects of the following type(s) are allowed in the list {@link EncryptionMethodType }
     */
    public List<EncryptionMethodType> getEncryptionMethod() {
        return Collections.unmodifiableList(this.encryptionMethod);
    }

    /**
     * Gets the value of the use property.
     *
     * @return possible object is {@link KeyTypes }
     */
    public KeyTypes getUse() {
        return use;
    }

    /**
     * Sets the value of the use property.
     *
     * @param value allowed object is {@link KeyTypes }
     */
    public void setUse(KeyTypes value) {
        this.use = value;
    }
}
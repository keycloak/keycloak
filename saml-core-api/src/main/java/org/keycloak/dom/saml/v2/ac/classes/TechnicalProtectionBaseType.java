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

package org.keycloak.dom.saml.v2.ac.classes;

/**
 * <p>
 * Java class for TechnicalProtectionBaseType complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="TechnicalProtectionBaseType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;choice minOccurs="0">
 *           &lt;element ref="{urn:oasis:names:tc:SAML:2.0:ac:classes:AuthenticatedTelephony}PrivateKeyProtection"/>
 *           &lt;element ref="{urn:oasis:names:tc:SAML:2.0:ac:classes:AuthenticatedTelephony}SecretKeyProtection"/>
 *         &lt;/choice>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:ac:classes:AuthenticatedTelephony}Extension"
 * maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
public class TechnicalProtectionBaseType extends ExtensionListType {

    protected PrivateKeyProtectionType privateKeyProtection;
    protected SecretKeyProtectionType secretKeyProtection;

    /**
     * Gets the value of the privateKeyProtection property.
     *
     * @return possible object is {@link PrivateKeyProtectionType }
     */
    public PrivateKeyProtectionType getPrivateKeyProtection() {
        return privateKeyProtection;
    }

    /**
     * Sets the value of the privateKeyProtection property.
     *
     * @param value allowed object is {@link PrivateKeyProtectionType }
     */
    public void setPrivateKeyProtection(PrivateKeyProtectionType value) {
        this.privateKeyProtection = value;
    }

    /**
     * Gets the value of the secretKeyProtection property.
     *
     * @return possible object is {@link SecretKeyProtectionType }
     */
    public SecretKeyProtectionType getSecretKeyProtection() {
        return secretKeyProtection;
    }

    /**
     * Sets the value of the secretKeyProtection property.
     *
     * @param value allowed object is {@link SecretKeyProtectionType }
     */
    public void setSecretKeyProtection(SecretKeyProtectionType value) {
        this.secretKeyProtection = value;
    }
}
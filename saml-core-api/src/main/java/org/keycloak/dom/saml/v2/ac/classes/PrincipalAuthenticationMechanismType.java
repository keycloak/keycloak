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

import java.math.BigInteger;

/**
 * <p>
 * Java class for PrincipalAuthenticationMechanismType complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="PrincipalAuthenticationMechanismType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:ac:classes:AuthenticatedTelephony}Password" minOccurs="0"/>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:ac:classes:AuthenticatedTelephony}RestrictedPassword"
 * minOccurs="0"/>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:ac:classes:AuthenticatedTelephony}Token" minOccurs="0"/>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:ac:classes:AuthenticatedTelephony}Smartcard" minOccurs="0"/>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:ac:classes:AuthenticatedTelephony}ActivationPin"
 * minOccurs="0"/>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:ac:classes:AuthenticatedTelephony}Extension"
 * maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="preauth" type="{http://www.w3.org/2001/XMLSchema}integer" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
public class PrincipalAuthenticationMechanismType extends ExtensionListType {

    protected PasswordType password;
    protected RestrictedPasswordType restrictedPassword;
    protected TokenType token;
    protected ExtensionOnlyType smartcard;
    protected ActivationPinType activationPin;
    protected BigInteger preauth;

    /**
     * Gets the value of the password property.
     *
     * @return possible object is {@link PasswordType }
     */
    public PasswordType getPassword() {
        return password;
    }

    /**
     * Sets the value of the password property.
     *
     * @param value allowed object is {@link PasswordType }
     */
    public void setPassword(PasswordType value) {
        this.password = value;
    }

    /**
     * Gets the value of the restrictedPassword property.
     *
     * @return possible object is {@link RestrictedPasswordType }
     */
    public RestrictedPasswordType getRestrictedPassword() {
        return restrictedPassword;
    }

    /**
     * Sets the value of the restrictedPassword property.
     *
     * @param value allowed object is {@link RestrictedPasswordType }
     */
    public void setRestrictedPassword(RestrictedPasswordType value) {
        this.restrictedPassword = value;
    }

    /**
     * Gets the value of the token property.
     *
     * @return possible object is {@link TokenType }
     */
    public TokenType getToken() {
        return token;
    }

    /**
     * Sets the value of the token property.
     *
     * @param value allowed object is {@link TokenType }
     */
    public void setToken(TokenType value) {
        this.token = value;
    }

    /**
     * Gets the value of the smartcard property.
     *
     * @return possible object is {@link ExtensionOnlyType }
     */
    public ExtensionOnlyType getSmartcard() {
        return smartcard;
    }

    /**
     * Sets the value of the smartcard property.
     *
     * @param value allowed object is {@link ExtensionOnlyType }
     */
    public void setSmartcard(ExtensionOnlyType value) {
        this.smartcard = value;
    }

    /**
     * Gets the value of the activationPin property.
     *
     * @return possible object is {@link ActivationPinType }
     */
    public ActivationPinType getActivationPin() {
        return activationPin;
    }

    /**
     * Sets the value of the activationPin property.
     *
     * @param value allowed object is {@link ActivationPinType }
     */
    public void setActivationPin(ActivationPinType value) {
        this.activationPin = value;
    }

    /**
     * Gets the value of the preauth property.
     *
     * @return possible object is {@link BigInteger }
     */
    public BigInteger getPreauth() {
        return preauth;
    }

    /**
     * Sets the value of the preauth property.
     *
     * @param value allowed object is {@link BigInteger }
     */
    public void setPreauth(BigInteger value) {
        this.preauth = value;
    }

}

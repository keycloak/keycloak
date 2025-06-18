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
 * Java class for AuthenticatorTransportProtocolType complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="AuthenticatorTransportProtocolType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;choice minOccurs="0">
 *           &lt;element ref="{urn:oasis:names:tc:SAML:2.0:ac:classes:AuthenticatedTelephony}HTTP"/>
 *           &lt;element ref="{urn:oasis:names:tc:SAML:2.0:ac:classes:AuthenticatedTelephony}SSL"/>
 *           &lt;element ref="{urn:oasis:names:tc:SAML:2.0:ac:classes:AuthenticatedTelephony}MobileNetworkNoEncryption"/>
 *           &lt;element ref="{urn:oasis:names:tc:SAML:2.0:ac:classes:AuthenticatedTelephony}MobileNetworkRadioEncryption"/>
 *           &lt;element ref="{urn:oasis:names:tc:SAML:2.0:ac:classes:AuthenticatedTelephony}MobileNetworkEndToEndEncryption"/>
 *           &lt;element ref="{urn:oasis:names:tc:SAML:2.0:ac:classes:AuthenticatedTelephony}WTLS"/>
 *           &lt;element ref="{urn:oasis:names:tc:SAML:2.0:ac:classes:AuthenticatedTelephony}IPSec"/>
 *           &lt;element ref="{urn:oasis:names:tc:SAML:2.0:ac:classes:AuthenticatedTelephony}PSTN"/>
 *           &lt;element ref="{urn:oasis:names:tc:SAML:2.0:ac:classes:AuthenticatedTelephony}ISDN"/>
 *           &lt;element ref="{urn:oasis:names:tc:SAML:2.0:ac:classes:AuthenticatedTelephony}ADSL"/>
 *         &lt;/choice>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:ac:classes:AuthenticatedTelephony}Extension"
 * maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
public class OriginalAuthenticatorTransportProtocolType extends ExtensionListType {

    protected ExtensionOnlyType http;
    protected ExtensionOnlyType ssl;
    protected ExtensionOnlyType mobileNetworkNoEncryption;
    protected ExtensionOnlyType mobileNetworkRadioEncryption;
    protected ExtensionOnlyType mobileNetworkEndToEndEncryption;
    protected ExtensionOnlyType wtls;
    protected ExtensionOnlyType ipSec;
    protected ExtensionOnlyType pstn;
    protected ExtensionOnlyType isdn;
    protected ExtensionOnlyType adsl;

    /**
     * Gets the value of the http property.
     *
     * @return possible object is {@link ExtensionOnlyType }
     */
    public ExtensionOnlyType getHTTP() {
        return http;
    }

    /**
     * Sets the value of the http property.
     *
     * @param value allowed object is {@link ExtensionOnlyType }
     */
    public void setHTTP(ExtensionOnlyType value) {
        this.http = value;
    }

    /**
     * Gets the value of the ssl property.
     *
     * @return possible object is {@link ExtensionOnlyType }
     */
    public ExtensionOnlyType getSSL() {
        return ssl;
    }

    /**
     * Sets the value of the ssl property.
     *
     * @param value allowed object is {@link ExtensionOnlyType }
     */
    public void setSSL(ExtensionOnlyType value) {
        this.ssl = value;
    }

    /**
     * Gets the value of the mobileNetworkNoEncryption property.
     *
     * @return possible object is {@link ExtensionOnlyType }
     */
    public ExtensionOnlyType getMobileNetworkNoEncryption() {
        return mobileNetworkNoEncryption;
    }

    /**
     * Sets the value of the mobileNetworkNoEncryption property.
     *
     * @param value allowed object is {@link ExtensionOnlyType }
     */
    public void setMobileNetworkNoEncryption(ExtensionOnlyType value) {
        this.mobileNetworkNoEncryption = value;
    }

    /**
     * Gets the value of the mobileNetworkRadioEncryption property.
     *
     * @return possible object is {@link ExtensionOnlyType }
     */
    public ExtensionOnlyType getMobileNetworkRadioEncryption() {
        return mobileNetworkRadioEncryption;
    }

    /**
     * Sets the value of the mobileNetworkRadioEncryption property.
     *
     * @param value allowed object is {@link ExtensionOnlyType }
     */
    public void setMobileNetworkRadioEncryption(ExtensionOnlyType value) {
        this.mobileNetworkRadioEncryption = value;
    }

    /**
     * Gets the value of the mobileNetworkEndToEndEncryption property.
     *
     * @return possible object is {@link ExtensionOnlyType }
     */
    public ExtensionOnlyType getMobileNetworkEndToEndEncryption() {
        return mobileNetworkEndToEndEncryption;
    }

    /**
     * Sets the value of the mobileNetworkEndToEndEncryption property.
     *
     * @param value allowed object is {@link ExtensionOnlyType }
     */
    public void setMobileNetworkEndToEndEncryption(ExtensionOnlyType value) {
        this.mobileNetworkEndToEndEncryption = value;
    }

    /**
     * Gets the value of the wtls property.
     *
     * @return possible object is {@link ExtensionOnlyType }
     */
    public ExtensionOnlyType getWTLS() {
        return wtls;
    }

    /**
     * Sets the value of the wtls property.
     *
     * @param value allowed object is {@link ExtensionOnlyType }
     */
    public void setWTLS(ExtensionOnlyType value) {
        this.wtls = value;
    }

    /**
     * Gets the value of the ipSec property.
     *
     * @return possible object is {@link ExtensionOnlyType }
     */
    public ExtensionOnlyType getIPSec() {
        return ipSec;
    }

    /**
     * Sets the value of the ipSec property.
     *
     * @param value allowed object is {@link ExtensionOnlyType }
     */
    public void setIPSec(ExtensionOnlyType value) {
        this.ipSec = value;
    }

    /**
     * Gets the value of the pstn property.
     *
     * @return possible object is {@link ExtensionOnlyType }
     */
    public ExtensionOnlyType getPSTN() {
        return pstn;
    }

    /**
     * Sets the value of the pstn property.
     *
     * @param value allowed object is {@link ExtensionOnlyType }
     */
    public void setPSTN(ExtensionOnlyType value) {
        this.pstn = value;
    }

    /**
     * Gets the value of the isdn property.
     *
     * @return possible object is {@link ExtensionOnlyType }
     */
    public ExtensionOnlyType getISDN() {
        return isdn;
    }

    /**
     * Sets the value of the isdn property.
     *
     * @param value allowed object is {@link ExtensionOnlyType }
     */
    public void setISDN(ExtensionOnlyType value) {
        this.isdn = value;
    }

    /**
     * Gets the value of the adsl property.
     *
     * @return possible object is {@link ExtensionOnlyType }
     */
    public ExtensionOnlyType getADSL() {
        return adsl;
    }

    /**
     * Sets the value of the adsl property.
     *
     * @param value allowed object is {@link ExtensionOnlyType }
     */
    public void setADSL(ExtensionOnlyType value) {
        this.adsl = value;
    }

}

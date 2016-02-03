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

package org.keycloak.dom.xmlsec.w3.xmldsig;

import org.keycloak.saml.common.constants.WSTrustConstants;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.Base64;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;

/**
 * <p>
 * Java class for RSAKeyValueType complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="RSAKeyValueType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="Modulus" type="{http://www.w3.org/2000/09/xmldsig#}CryptoBinary"/>
 *         &lt;element name="Exponent" type="{http://www.w3.org/2000/09/xmldsig#}CryptoBinary"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
public class RSAKeyValueType implements KeyValueType {

    protected byte[] modulus;
    protected byte[] exponent;

    /**
     * Gets the value of the modulus property.
     *
     * @return possible object is byte[]
     */
    public byte[] getModulus() {
        return modulus;
    }

    /**
     * Sets the value of the modulus property.
     *
     * @param value allowed object is byte[]
     */
    public void setModulus(byte[] value) {
        this.modulus = ((byte[]) value);
    }

    /**
     * Gets the value of the exponent property.
     *
     * @return possible object is byte[]
     */
    public byte[] getExponent() {
        return exponent;
    }

    /**
     * Sets the value of the exponent property.
     *
     * @param value allowed object is byte[]
     */
    public void setExponent(byte[] value) {
        this.exponent = ((byte[]) value);
    }

    /**
     * Convert to the JDK representation of a RSA Public Key
     *
     * @return
     *
     * @throws org.keycloak.saml.common.exceptions.ProcessingException
     */
    public RSAPublicKey convertToPublicKey() throws ProcessingException {
        BigInteger bigModulus = new BigInteger(1, massage(Base64.decode(new String(modulus))));
        BigInteger bigEx = new BigInteger(1, massage(Base64.decode(new String(exponent))));

        try {
            KeyFactory rsaKeyFactory = KeyFactory.getInstance("rsa");
            RSAPublicKeySpec kspec = new RSAPublicKeySpec(bigModulus, bigEx);
            return (RSAPublicKey) rsaKeyFactory.generatePublic(kspec);
        } catch (Exception e) {
            throw new ProcessingException(e);
        }
    }

    /**
     * Convert to the JDK representation of a RSA Private Key
     *
     * @return
     *
     * @throws ProcessingException
     */
    public RSAPrivateKey convertToPrivateKey() throws ProcessingException {
        BigInteger bigModulus = new BigInteger(1, massage(Base64.decode(new String(modulus))));
        BigInteger bigEx = new BigInteger(1, massage(Base64.decode(new String(exponent))));

        try {
            KeyFactory rsaKeyFactory = KeyFactory.getInstance("rsa");
            RSAPrivateKeySpec kspec = new RSAPrivateKeySpec(bigModulus, bigEx);
            return (RSAPrivateKey) rsaKeyFactory.generatePrivate(kspec);
        } catch (Exception e) {
            throw new ProcessingException(e);
        }
    }

    public String toString() {
        String prefix = WSTrustConstants.XMLDSig.DSIG_PREFIX;
        String colon = ":";
        String left = "<";
        String right = ">";
        String slash = "/";

        StringBuilder sb = new StringBuilder();

        sb.append(left).append(prefix).append(colon).append(WSTrustConstants.XMLDSig.RSA_KEYVALUE).append(right);

        sb.append(left).append(prefix).append(colon).append(WSTrustConstants.XMLDSig.MODULUS).append(right);
        sb.append(new String(getModulus()));
        sb.append(left).append(slash).append(prefix).append(colon).append(WSTrustConstants.XMLDSig.MODULUS).append(right);

        sb.append(left).append(prefix).append(colon).append(WSTrustConstants.XMLDSig.EXPONENT).append(right);
        sb.append(new String(getExponent()));
        sb.append(left).append(slash).append(prefix).append(colon).append(WSTrustConstants.XMLDSig.EXPONENT).append(right);

        sb.append(left).append(slash).append(prefix).append(colon).append(WSTrustConstants.XMLDSig.RSA_KEYVALUE).append(right);
        return sb.toString();
    }

    private byte[] massage(byte[] byteArray) {
        if (byteArray[0] == 0) {
            byte[] substring = new byte[byteArray.length - 1];
            System.arraycopy(byteArray, 1, substring, 0, byteArray.length - 1);
            return substring;
        }
        return byteArray;
    }
}
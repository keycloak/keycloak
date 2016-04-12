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
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.DSAPublicKey;
import java.security.spec.DSAPrivateKeySpec;
import java.security.spec.DSAPublicKeySpec;

/**
 * <p>
 * Java class for DSAKeyValueType complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="DSAKeyValueType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;sequence minOccurs="0">
 *           &lt;element name="P" type="{http://www.w3.org/2000/09/xmldsig#}CryptoBinary"/>
 *           &lt;element name="Q" type="{http://www.w3.org/2000/09/xmldsig#}CryptoBinary"/>
 *         &lt;/sequence>
 *         &lt;element name="G" type="{http://www.w3.org/2000/09/xmldsig#}CryptoBinary" minOccurs="0"/>
 *         &lt;element name="Y" type="{http://www.w3.org/2000/09/xmldsig#}CryptoBinary"/>
 *         &lt;element name="J" type="{http://www.w3.org/2000/09/xmldsig#}CryptoBinary" minOccurs="0"/>
 *         &lt;sequence minOccurs="0">
 *           &lt;element name="Seed" type="{http://www.w3.org/2000/09/xmldsig#}CryptoBinary"/>
 *           &lt;element name="PgenCounter" type="{http://www.w3.org/2000/09/xmldsig#}CryptoBinary"/>
 *         &lt;/sequence>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
public class DSAKeyValueType implements KeyValueType {

    protected byte[] p;
    protected byte[] q;
    protected byte[] g;
    protected byte[] y;
    protected byte[] j;
    protected byte[] seed;
    protected byte[] pgenCounter;

    /**
     * Gets the value of the p property.
     *
     * @return possible object is byte[]
     */
    public byte[] getP() {
        return p;
    }

    /**
     * Sets the value of the p property.
     *
     * @param value allowed object is byte[]
     */
    public void setP(byte[] value) {
        this.p = ((byte[]) value);
    }

    /**
     * Gets the value of the q property.
     *
     * @return possible object is byte[]
     */
    public byte[] getQ() {
        return q;
    }

    /**
     * Sets the value of the q property.
     *
     * @param value allowed object is byte[]
     */
    public void setQ(byte[] value) {
        this.q = ((byte[]) value);
    }

    /**
     * Gets the value of the g property.
     *
     * @return possible object is byte[]
     */
    public byte[] getG() {
        return g;
    }

    /**
     * Sets the value of the g property.
     *
     * @param value allowed object is byte[]
     */
    public void setG(byte[] value) {
        this.g = ((byte[]) value);
    }

    /**
     * Gets the value of the y property.
     *
     * @return possible object is byte[]
     */
    public byte[] getY() {
        return y;
    }

    /**
     * Sets the value of the y property.
     *
     * @param value allowed object is byte[]
     */
    public void setY(byte[] value) {
        this.y = ((byte[]) value);
    }

    /**
     * Gets the value of the j property.
     *
     * @return possible object is byte[]
     */
    public byte[] getJ() {
        return j;
    }

    /**
     * Sets the value of the j property.
     *
     * @param value allowed object is byte[]
     */
    public void setJ(byte[] value) {
        this.j = ((byte[]) value);
    }

    /**
     * Gets the value of the seed property.
     *
     * @return possible object is byte[]
     */
    public byte[] getSeed() {
        return seed;
    }

    /**
     * Sets the value of the seed property.
     *
     * @param value allowed object is byte[]
     */
    public void setSeed(byte[] value) {
        this.seed = ((byte[]) value);
    }

    /**
     * Gets the value of the pgenCounter property.
     *
     * @return possible object is byte[]
     */
    public byte[] getPgenCounter() {
        return pgenCounter;
    }

    /**
     * Sets the value of the pgenCounter property.
     *
     * @param value allowed object is byte[]
     */
    public void setPgenCounter(byte[] value) {
        this.pgenCounter = ((byte[]) value);
    }

    /**
     * Convert to the JDK representation of a DSA Public Key
     *
     * @return
     *
     * @throws org.keycloak.saml.common.exceptions.ProcessingException
     */
    public DSAPublicKey convertToPublicKey() throws ProcessingException {
        BigInteger BigY, BigP, BigQ, BigG;

        BigY = new BigInteger(1, massage(Base64.decode(new String(y))));
        BigP = new BigInteger(1, massage(Base64.decode(new String(p))));
        BigQ = new BigInteger(1, massage(Base64.decode(new String(q))));
        BigG = new BigInteger(1, massage(Base64.decode(new String(g))));

        try {
            KeyFactory dsaKeyFactory = KeyFactory.getInstance("dsa");
            DSAPublicKeySpec kspec = new DSAPublicKeySpec(BigY, BigP, BigQ, BigG);
            return (DSAPublicKey) dsaKeyFactory.generatePublic(kspec);
        } catch (Exception e) {
            throw new ProcessingException(e);
        }
    }

    /**
     * Convert to the JDK representation of a DSA Private Key
     *
     * @return
     *
     * @throws ProcessingException
     */
    public DSAPrivateKey convertToPrivateKey() throws ProcessingException {
        BigInteger BigY, BigP, BigQ, BigG;

        BigY = new BigInteger(1, massage(Base64.decode(new String(y))));
        BigP = new BigInteger(1, massage(Base64.decode(new String(p))));
        BigQ = new BigInteger(1, massage(Base64.decode(new String(q))));
        BigG = new BigInteger(1, massage(Base64.decode(new String(g))));

        try {
            KeyFactory dsaKeyFactory = KeyFactory.getInstance("dsa");
            DSAPrivateKeySpec kspec = new DSAPrivateKeySpec(BigY, BigP, BigQ, BigG);
            return (DSAPrivateKey) dsaKeyFactory.generatePrivate(kspec);
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

        sb.append(left).append(prefix).append(colon).append(WSTrustConstants.XMLDSig.DSA_KEYVALUE).append(right);

        if (p != null) {
            sb.append(left).append(prefix).append(colon).append(WSTrustConstants.XMLDSig.P).append(right);
            sb.append(new String(getP()));
            sb.append(left).append(slash).append(prefix).append(colon).append(WSTrustConstants.XMLDSig.P).append(right);
        }

        if (q != null) {
            sb.append(left).append(prefix).append(colon).append(WSTrustConstants.XMLDSig.Q).append(right);
            sb.append(new String(getQ()));
            sb.append(left).append(slash).append(prefix).append(colon).append(WSTrustConstants.XMLDSig.Q).append(right);
        }

        if (g != null) {
            sb.append(left).append(prefix).append(colon).append(WSTrustConstants.XMLDSig.G).append(right);
            sb.append(new String(getG()));
            sb.append(left).append(slash).append(prefix).append(colon).append(WSTrustConstants.XMLDSig.G).append(right);
        }

        if (y != null) {
            sb.append(left).append(prefix).append(colon).append(WSTrustConstants.XMLDSig.Y).append(right);
            sb.append(new String(getY()));
            sb.append(left).append(slash).append(prefix).append(colon).append(WSTrustConstants.XMLDSig.Y).append(right);
        }

        if (seed != null) {
            sb.append(left).append(prefix).append(colon).append(WSTrustConstants.XMLDSig.SEED).append(right);
            sb.append(new String(getSeed()));
            sb.append(left).append(slash).append(prefix).append(colon).append(WSTrustConstants.XMLDSig.SEED).append(right);
        }

        if (pgenCounter != null) {
            sb.append(left).append(prefix).append(colon).append(WSTrustConstants.XMLDSig.PGEN_COUNTER).append(right);
            sb.append(new String(getPgenCounter()));
            sb.append(left).append(slash).append(prefix).append(colon).append(WSTrustConstants.XMLDSig.PGEN_COUNTER).append(right);
        }

        sb.append(left).append(slash).append(prefix).append(colon).append(WSTrustConstants.XMLDSig.DSA_KEYVALUE).append(right);
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
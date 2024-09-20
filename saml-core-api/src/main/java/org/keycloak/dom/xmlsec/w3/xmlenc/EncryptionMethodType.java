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
package org.keycloak.dom.xmlsec.w3.xmlenc;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Java class for EncryptionMethodType complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="EncryptionMethodType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="KeySize" type="{http://www.w3.org/2001/04/xmlenc#}KeySizeType" minOccurs="0"/>
 *         &lt;element name="OAEPparams" type="{http://www.w3.org/2001/XMLSchema}base64Binary" minOccurs="0"/>
 *         &lt;any/>
 *       &lt;/sequence>
 *       &lt;attribute name="Algorithm" use="required" type="{http://www.w3.org/2001/XMLSchema}anyURI" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
public class EncryptionMethodType {

    protected String algorithm;

    protected EncryptionMethod encryptionMethod;

    public static class EncryptionMethod {

        protected BigInteger keySize;
        protected byte[] OAEPparams;

        protected List<Object> any = new ArrayList<>();

        public EncryptionMethod(BigInteger bigInteger, byte[] oAEPparams) {
            this.keySize = bigInteger;
            OAEPparams = oAEPparams;
        }

        public EncryptionMethod() {
            this.keySize = null;
            this.OAEPparams = null;
        }

        public BigInteger getKeySize() {
            return keySize;
        }

        public byte[] getOAEPparams() {
            return OAEPparams;
        }

        public void setKeySize(BigInteger keySize) {
            this.keySize = keySize;
        }

        public void setOAEPparams(byte[] OAEPparams) {
            this.OAEPparams = OAEPparams;
        }

        public List<Object> getAny() {
            return any;
        }

        public void addAny(Object e) {
            this.any.add(e);
        }
    }

    public EncryptionMethodType(String algo) {
        this.algorithm = algo;
    }

    public EncryptionMethod getEncryptionMethod() {
        return encryptionMethod;
    }

    public void setEncryptionMethod(EncryptionMethod encryptionMethod) {
        this.encryptionMethod = encryptionMethod;
    }

    /**
     * Gets the value of the algorithm property.
     *
     * @return possible object is {@link String }
     */
    public String getAlgorithm() {
        return algorithm;
    }

}
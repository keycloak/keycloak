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

package org.keycloak.saml.processing.core.saml.v2.util;


import org.keycloak.dom.xmlsec.w3.xmldsig.DSAKeyValueType;
import org.keycloak.dom.xmlsec.w3.xmldsig.KeyInfoType;
import org.keycloak.dom.xmlsec.w3.xmldsig.KeyValueType;
import org.keycloak.dom.xmlsec.w3.xmldsig.RSAKeyValueType;
import org.keycloak.dom.xmlsec.w3.xmldsig.X509CertificateType;
import org.keycloak.dom.xmlsec.w3.xmldsig.X509DataType;
import org.keycloak.saml.common.ErrorCodes;
import org.keycloak.saml.common.PicketLinkLogger;
import org.keycloak.saml.common.PicketLinkLoggerFactory;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.constants.WSTrustConstants;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.StaxUtil;

import org.w3c.dom.Element;

import javax.xml.stream.XMLStreamWriter;

/**
 * Utility methods for stax writing
 *
 * @author anil saldhana
 * @since Jan 28, 2013
 */
public class StaxWriterUtil {

    private static final PicketLinkLogger logger = PicketLinkLoggerFactory.getLogger();

    /**
     * Write the {@link org.keycloak.dom.xmlsec.w3.xmldsig.KeyInfoType}
     *
     * @param writer
     * @param keyInfo
     *
     * @throws org.keycloak.saml.common.exceptions.ProcessingException
     */
    public static void writeKeyInfo(XMLStreamWriter writer, KeyInfoType keyInfo) throws ProcessingException {
        if (keyInfo.getContent() == null || keyInfo.getContent().size() == 0)
            throw logger.writerInvalidKeyInfoNullContentError();
        StaxUtil.writeStartElement(writer, WSTrustConstants.XMLDSig.DSIG_PREFIX, WSTrustConstants.XMLDSig.KEYINFO,
                WSTrustConstants.XMLDSig.DSIG_NS);
        StaxUtil.writeNameSpace(writer, WSTrustConstants.XMLDSig.DSIG_PREFIX, WSTrustConstants.XMLDSig.DSIG_NS);
        // write the keyInfo content.
        Object content = keyInfo.getContent().get(0);
        if (content instanceof Element) {
            Element element = (Element) keyInfo.getContent().get(0);
            StaxUtil.writeDOMNode(writer, element);
        } else if (content instanceof X509DataType) {
            X509DataType type = (X509DataType) content;
            if (type.getDataObjects().size() == 0)
                throw logger.writerNullValueError("X509Data");
            StaxUtil.writeStartElement(writer, WSTrustConstants.XMLDSig.DSIG_PREFIX, WSTrustConstants.XMLDSig.X509DATA,
                    WSTrustConstants.XMLDSig.DSIG_NS);
            Object obj = type.getDataObjects().get(0);
            if (obj instanceof Element) {
                Element element = (Element) obj;
                StaxUtil.writeDOMElement(writer, element);
            } else if (obj instanceof X509CertificateType) {
                X509CertificateType cert = (X509CertificateType) obj;
                StaxUtil.writeStartElement(writer, WSTrustConstants.XMLDSig.DSIG_PREFIX, WSTrustConstants.XMLDSig.X509CERT,
                        WSTrustConstants.XMLDSig.DSIG_NS);
                StaxUtil.writeCharacters(writer, new String(cert.getEncodedCertificate(), GeneralConstants.SAML_CHARSET));
                StaxUtil.writeEndElement(writer);
            }
            StaxUtil.writeEndElement(writer);
        } else if (content instanceof KeyValueType) {
            KeyValueType keyvalueType = (KeyValueType) content;
            StaxUtil.writeStartElement(writer, WSTrustConstants.XMLDSig.DSIG_PREFIX, WSTrustConstants.XMLDSig.KEYVALUE,
                    WSTrustConstants.XMLDSig.DSIG_NS);
            if (keyvalueType instanceof DSAKeyValueType) {
                writeDSAKeyValueType(writer, (DSAKeyValueType) keyvalueType);
            }
            if (keyvalueType instanceof RSAKeyValueType) {
                writeRSAKeyValueType(writer, (RSAKeyValueType) keyvalueType);
            }
            StaxUtil.writeEndElement(writer);
        } else
            throw new ProcessingException(ErrorCodes.UNSUPPORTED_TYPE + content);

        StaxUtil.writeEndElement(writer);
    }

    public static void writeRSAKeyValueType(XMLStreamWriter writer, RSAKeyValueType type) throws ProcessingException {
        String prefix = WSTrustConstants.XMLDSig.DSIG_PREFIX;

        StaxUtil.writeStartElement(writer, prefix, WSTrustConstants.XMLDSig.RSA_KEYVALUE, WSTrustConstants.DSIG_NS);
        // write the rsa key modulus.
        byte[] modulus = type.getModulus();
        StaxUtil.writeStartElement(writer, prefix, WSTrustConstants.XMLDSig.MODULUS, WSTrustConstants.DSIG_NS);
        StaxUtil.writeCharacters(writer, new String(modulus, GeneralConstants.SAML_CHARSET));
        StaxUtil.writeEndElement(writer);

        // write the rsa key exponent.
        byte[] exponent = type.getExponent();
        StaxUtil.writeStartElement(writer, prefix, WSTrustConstants.XMLDSig.EXPONENT, WSTrustConstants.DSIG_NS);
        StaxUtil.writeCharacters(writer, new String(exponent, GeneralConstants.SAML_CHARSET));
        StaxUtil.writeEndElement(writer);

        StaxUtil.writeEndElement(writer);
    }

    public static void writeDSAKeyValueType(XMLStreamWriter writer, DSAKeyValueType type) throws ProcessingException {

        String prefix = WSTrustConstants.XMLDSig.DSIG_PREFIX;

        StaxUtil.writeStartElement(writer, prefix, WSTrustConstants.XMLDSig.DSA_KEYVALUE, WSTrustConstants.DSIG_NS);

        byte[] p = type.getP();
        if (p != null) {
            StaxUtil.writeStartElement(writer, prefix, WSTrustConstants.XMLDSig.P, WSTrustConstants.DSIG_NS);
            StaxUtil.writeCharacters(writer, new String(p, GeneralConstants.SAML_CHARSET));
            StaxUtil.writeEndElement(writer);
        }
        byte[] q = type.getQ();
        if (q != null) {
            StaxUtil.writeStartElement(writer, prefix, WSTrustConstants.XMLDSig.Q, WSTrustConstants.DSIG_NS);
            StaxUtil.writeCharacters(writer, new String(q, GeneralConstants.SAML_CHARSET));
            StaxUtil.writeEndElement(writer);
        }
        byte[] g = type.getG();
        if (g != null) {
            StaxUtil.writeStartElement(writer, prefix, WSTrustConstants.XMLDSig.G, WSTrustConstants.DSIG_NS);
            StaxUtil.writeCharacters(writer, new String(g, GeneralConstants.SAML_CHARSET));
            StaxUtil.writeEndElement(writer);
        }
        byte[] y = type.getY();
        if (y != null) {
            StaxUtil.writeStartElement(writer, prefix, WSTrustConstants.XMLDSig.Y, WSTrustConstants.DSIG_NS);
            StaxUtil.writeCharacters(writer, new String(y, GeneralConstants.SAML_CHARSET));
            StaxUtil.writeEndElement(writer);
        }
        byte[] seed = type.getSeed();
        if (seed != null) {
            StaxUtil.writeStartElement(writer, prefix, WSTrustConstants.XMLDSig.SEED, WSTrustConstants.DSIG_NS);
            StaxUtil.writeCharacters(writer, new String(seed, GeneralConstants.SAML_CHARSET));
            StaxUtil.writeEndElement(writer);
        }
        byte[] pgen = type.getPgenCounter();
        if (pgen != null) {
            StaxUtil.writeStartElement(writer, prefix, WSTrustConstants.XMLDSig.PGEN_COUNTER, WSTrustConstants.DSIG_NS);
            StaxUtil.writeCharacters(writer, new String(pgen, GeneralConstants.SAML_CHARSET));
            StaxUtil.writeEndElement(writer);
        }

        StaxUtil.writeEndElement(writer);
    }
}
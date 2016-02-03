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

import org.keycloak.saml.common.PicketLinkLogger;
import org.keycloak.saml.common.PicketLinkLoggerFactory;
import org.keycloak.saml.common.constants.JBossSAMLConstants;
import org.keycloak.saml.common.constants.WSTrustConstants;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.util.Base64;
import org.keycloak.saml.processing.core.constants.PicketLinkFederationConstants;
import org.keycloak.dom.xmlsec.w3.xmldsig.DSAKeyValueType;
import org.keycloak.dom.xmlsec.w3.xmldsig.KeyValueType;
import org.keycloak.dom.xmlsec.w3.xmldsig.RSAKeyValueType;
import org.keycloak.dom.xmlsec.w3.xmldsig.SignatureType;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.RSAPublicKey;

/**
 * Signature utility for signing content
 *
 * @author Anil.Saldhana@redhat.com
 * @since Dec 16, 2008
 */
public class SignatureUtil {

    private static final PicketLinkLogger logger = PicketLinkLoggerFactory.getLogger();

    /**
     * Marshall a SignatureType to output stream
     *
     * @param signature
     * @param os
     *
     * @throws SAXException
     * @throws JAXBException
     */
    public static void marshall(SignatureType signature, OutputStream os) throws JAXBException, SAXException {
        throw logger.notImplementedYet("NYI");
        /*
         * JAXBElement<SignatureType> jsig = objectFactory.createSignature(signature); Marshaller marshaller =
         * JAXBUtil.getValidatingMarshaller(pkgName, schemaLocation); marshaller.marshal(jsig, os);
         */
    }

    /**
     * Get the XML Signature URI for the algo (RSA, DSA)
     *
     * @param algo
     *
     * @return
     */
    public static String getXMLSignatureAlgorithmURI(String algo) {
        String xmlSignatureAlgo = null;

        if ("DSA".equalsIgnoreCase(algo)) {
            xmlSignatureAlgo = JBossSAMLConstants.SIGNATURE_SHA1_WITH_DSA.get();
        } else if ("RSA".equalsIgnoreCase(algo)) {
            xmlSignatureAlgo = JBossSAMLConstants.SIGNATURE_SHA1_WITH_RSA.get();
        }
        return xmlSignatureAlgo;
    }

    /**
     * Sign a string using the private key
     *
     * @param stringToBeSigned
     * @param signingKey
     *
     * @return
     *
     * @throws GeneralSecurityException
     */
    public static byte[] sign(String stringToBeSigned, PrivateKey signingKey) throws GeneralSecurityException {
        if (stringToBeSigned == null)
            throw logger.nullArgumentError("stringToBeSigned");
        if (signingKey == null)
            throw logger.nullArgumentError("signingKey");

        String algo = signingKey.getAlgorithm();
        Signature sig = getSignature(algo);
        sig.initSign(signingKey);
        sig.update(stringToBeSigned.getBytes());
        return sig.sign();
    }

    /**
     * Validate the signed content with the signature value
     *
     * @param signedContent
     * @param signatureValue
     * @param validatingKey
     *
     * @return
     *
     * @throws GeneralSecurityException
     */
    public static boolean validate(byte[] signedContent, byte[] signatureValue, PublicKey validatingKey)
            throws GeneralSecurityException {
        if (signedContent == null)
            throw logger.nullArgumentError("signedContent");
        if (signatureValue == null)
            throw logger.nullArgumentError("signatureValue");
        if (validatingKey == null)
            throw logger.nullArgumentError("validatingKey");

        // We assume that the sigatureValue has the same algorithm as the public key
        // If not, there will be an exception anyway
        String algo = validatingKey.getAlgorithm();
        Signature sig = getSignature(algo);

        sig.initVerify(validatingKey);
        sig.update(signedContent);
        return sig.verify(signatureValue);
    }

    /**
     * Validate the signature using a x509 certificate
     *
     * @param signedContent
     * @param signatureValue
     * @param signatureAlgorithm
     * @param validatingCert
     *
     * @return
     *
     * @throws GeneralSecurityException
     */
    public static boolean validate(byte[] signedContent, byte[] signatureValue, String signatureAlgorithm,
                                   X509Certificate validatingCert) throws GeneralSecurityException {
        if (signedContent == null)
            throw logger.nullArgumentError("signedContent");
        if (signatureValue == null)
            throw logger.nullArgumentError("signatureValue");
        if (signatureAlgorithm == null)
            throw logger.nullArgumentError("signatureAlgorithm");
        if (validatingCert == null)
            throw logger.nullArgumentError("validatingCert");

        Signature sig = getSignature(signatureAlgorithm);

        sig.initVerify(validatingCert);
        sig.update(signedContent);
        return sig.verify(signatureValue);
    }


    /**
     * Given a dsig:DSAKeyValue element, return {@link DSAKeyValueType}
     *
     * @param element
     *
     * @return
     *
     * @throws org.keycloak.saml.common.exceptions.ParsingException
     */
    public static DSAKeyValueType getDSAKeyValue(Element element) throws ParsingException {
        DSAKeyValueType dsa = new DSAKeyValueType();
        NodeList nl = element.getChildNodes();
        int length = nl.getLength();

        for (int i = 0; i < length; i++) {
            Node node = nl.item(i);
            if (node instanceof Element) {
                Element childElement = (Element) node;
                String tag = childElement.getLocalName();

                byte[] text = childElement.getTextContent().getBytes();

                if (WSTrustConstants.XMLDSig.P.equals(tag)) {
                    dsa.setP(text);
                } else if (WSTrustConstants.XMLDSig.Q.equals(tag)) {
                    dsa.setQ(text);
                } else if (WSTrustConstants.XMLDSig.G.equals(tag)) {
                    dsa.setG(text);
                } else if (WSTrustConstants.XMLDSig.Y.equals(tag)) {
                    dsa.setY(text);
                } else if (WSTrustConstants.XMLDSig.SEED.equals(tag)) {
                    dsa.setSeed(text);
                } else if (WSTrustConstants.XMLDSig.PGEN_COUNTER.equals(tag)) {
                    dsa.setPgenCounter(text);
                }
            }
        }

        return dsa;
    }

    /**
     * Given a dsig:DSAKeyValue element, return {@link DSAKeyValueType}
     *
     * @param element
     *
     * @return
     *
     * @throws ParsingException
     */
    public static RSAKeyValueType getRSAKeyValue(Element element) throws ParsingException {
        RSAKeyValueType rsa = new RSAKeyValueType();
        NodeList nl = element.getChildNodes();
        int length = nl.getLength();

        for (int i = 0; i < length; i++) {
            Node node = nl.item(i);
            if (node instanceof Element) {
                Element childElement = (Element) node;
                String tag = childElement.getLocalName();

                byte[] text = childElement.getTextContent().getBytes();

                if (WSTrustConstants.XMLDSig.MODULUS.equals(tag)) {
                    rsa.setModulus(text);
                } else if (WSTrustConstants.XMLDSig.EXPONENT.equals(tag)) {
                    rsa.setExponent(text);
                }
            }
        }

        return rsa;
    }

    /**
     * <p>
     * Creates a {@code KeyValueType} that wraps the specified public key. This method supports DSA and RSA keys.
     * </p>
     *
     * @param key the {@code PublicKey} that will be represented as a {@code KeyValueType}.
     *
     * @return the constructed {@code KeyValueType} or {@code null} if the specified key is neither a DSA nor a RSA
     *         key.
     */
    public static KeyValueType createKeyValue(PublicKey key) {
        if (key instanceof RSAPublicKey) {
            RSAPublicKey pubKey = (RSAPublicKey) key;
            byte[] modulus = pubKey.getModulus().toByteArray();
            byte[] exponent = pubKey.getPublicExponent().toByteArray();

            RSAKeyValueType rsaKeyValue = new RSAKeyValueType();
            rsaKeyValue.setModulus(Base64.encodeBytes(modulus).getBytes());
            rsaKeyValue.setExponent(Base64.encodeBytes(exponent).getBytes());
            return rsaKeyValue;
        } else if (key instanceof DSAPublicKey) {
            DSAPublicKey pubKey = (DSAPublicKey) key;
            byte[] P = pubKey.getParams().getP().toByteArray();
            byte[] Q = pubKey.getParams().getQ().toByteArray();
            byte[] G = pubKey.getParams().getG().toByteArray();
            byte[] Y = pubKey.getY().toByteArray();

            DSAKeyValueType dsaKeyValue = new DSAKeyValueType();
            dsaKeyValue.setP(Base64.encodeBytes(P).getBytes());
            dsaKeyValue.setQ(Base64.encodeBytes(Q).getBytes());
            dsaKeyValue.setG(Base64.encodeBytes(G).getBytes());
            dsaKeyValue.setY(Base64.encodeBytes(Y).getBytes());
            return dsaKeyValue;
        }
        throw logger.unsupportedType(key.toString());
    }

    private static Signature getSignature(String algo) throws GeneralSecurityException {
        Signature sig = null;

        if ("DSA".equalsIgnoreCase(algo)) {
            sig = Signature.getInstance(PicketLinkFederationConstants.DSA_SIGNATURE_ALGORITHM);
        } else if ("RSA".equalsIgnoreCase(algo)) {
            sig = Signature.getInstance(PicketLinkFederationConstants.RSA_SIGNATURE_ALGORITHM);
        } else
            throw logger.signatureUnknownAlgo(algo);
        return sig;
    }
}
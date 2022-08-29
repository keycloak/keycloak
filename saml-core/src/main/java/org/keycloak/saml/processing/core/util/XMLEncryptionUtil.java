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
package org.keycloak.saml.processing.core.util;

import org.apache.xml.security.encryption.EncryptedData;
import org.apache.xml.security.encryption.EncryptedKey;
import org.apache.xml.security.encryption.XMLCipher;
import org.apache.xml.security.encryption.XMLEncryptionException;
import org.apache.xml.security.utils.EncryptionConstants;

import org.keycloak.saml.common.PicketLinkLogger;
import org.keycloak.saml.common.PicketLinkLoggerFactory;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.common.util.StringUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.crypto.SecretKey;
import javax.xml.namespace.QName;
import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Objects;
import javax.xml.XMLConstants;
import javax.xml.crypto.dsig.XMLSignature;

/**
 * Utility for XML Encryption <b>Note: </b> This utility is currently using Apache XML Security library API. JSR-106 is
 * not yet
 * final. Until that happens,we rely on the non-standard API.
 *
 * @author Anil.Saldhana@redhat.com
 * @since May 4, 2009
 */
public class XMLEncryptionUtil {

    private static final PicketLinkLogger logger = PicketLinkLoggerFactory.getLogger();

    static {
        // Initialize the Apache XML Security Library
        org.apache.xml.security.Init.init();
    }

    public static final String DS_KEY_INFO = "ds:KeyInfo";

    private static final String RSA_ENCRYPTION_SCHEME = Objects.equals(System.getProperty("keycloak.saml.key_trans.rsa_v1.5"), "true")
      ? XMLCipher.RSA_v1dot5
      : XMLCipher.RSA_OAEP;

    /**
     * <p>
     * Encrypt the Key to be transported
     * </p>
     * <p>
     * Data is encrypted with a SecretKey. Then the key needs to be transported to the other end where it is needed for
     * decryption. For the Key transport, the SecretKey is encrypted with the recipient's public key. At the receiving
     * end, the
     * receiver can decrypt the Secret Key using his private key.s
     * </p>
     *
     * @param document
     * @param keyToBeEncrypted Symmetric Key (SecretKey)
     * @param keyUsedToEncryptSecretKey Asymmetric Key (Public Key)
     * @param keySize Length of the key
     *
     * @return
     *
     * @throws org.keycloak.saml.common.exceptions.ProcessingException
     */
    private static EncryptedKey encryptKey(Document document, SecretKey keyToBeEncrypted, PublicKey keyUsedToEncryptSecretKey,
                                          int keySize) throws ProcessingException {
        XMLCipher keyCipher;
        String pubKeyAlg = keyUsedToEncryptSecretKey.getAlgorithm();

        try {
            String keyWrapAlgo = getXMLEncryptionURLForKeyUnwrap(pubKeyAlg, keySize);
            keyCipher = XMLCipher.getInstance(keyWrapAlgo);

            keyCipher.init(XMLCipher.WRAP_MODE, keyUsedToEncryptSecretKey);
            return keyCipher.encryptKey(document, keyToBeEncrypted);
        } catch (XMLEncryptionException e) {
            throw logger.processingError(e);
        }
    }

    /**
     * Given an element in a Document, encrypt the element and replace the element in the document with the encrypted
     * data
     *
     * @param elementQName QName of the element that we like to encrypt
     * @param document
     * @param publicKey
     * @param secretKey
     * @param keySize
     * @param wrappingElementQName A QName of an element that will wrap the encrypted element
     * @param addEncryptedKeyInKeyInfo Need for the EncryptedKey to be placed in ds:KeyInfo
     *
     * @throws ProcessingException
     */
    public static void encryptElement(QName elementQName, Document document, PublicKey publicKey, SecretKey secretKey,
                                      int keySize, QName wrappingElementQName, boolean addEncryptedKeyInKeyInfo) throws ProcessingException {
        if (elementQName == null)
            throw logger.nullArgumentError("elementQName");
        if (document == null)
            throw logger.nullArgumentError("document");
        String wrappingElementPrefix = wrappingElementQName.getPrefix();
        if (wrappingElementPrefix == null || "".equals(wrappingElementPrefix))
            throw logger.wrongTypeError("Wrapping element prefix invalid");

        Element documentElement = DocumentUtil.getElement(document, elementQName);

        if (documentElement == null)
            throw logger.domMissingDocElementError(elementQName.toString());

        XMLCipher cipher = null;
        EncryptedKey encryptedKey = encryptKey(document, secretKey, publicKey, keySize);

        String encryptionAlgorithm = getXMLEncryptionURL(secretKey.getAlgorithm(), keySize);
        // Encrypt the Document
        try {
            cipher = XMLCipher.getInstance(encryptionAlgorithm);
            cipher.init(XMLCipher.ENCRYPT_MODE, secretKey);
        } catch (XMLEncryptionException e1) {
            throw logger.processingError(e1);
        }

        Document encryptedDoc;
        try {
            encryptedDoc = cipher.doFinal(document, documentElement);
        } catch (Exception e) {
            throw logger.processingError(e);
        }

        // The EncryptedKey element is added
        Element encryptedKeyElement = cipher.martial(document, encryptedKey);

        final String wrappingElementName;

        if (StringUtil.isNullOrEmpty(wrappingElementPrefix)) {
            wrappingElementName = wrappingElementQName.getLocalPart();
        } else {
            wrappingElementName = wrappingElementPrefix + ":" + wrappingElementQName.getLocalPart();
        }
        // Create the wrapping element and set its attribute NS
        Element wrappingElement = encryptedDoc.createElementNS(wrappingElementQName.getNamespaceURI(), wrappingElementName);

        if (! StringUtil.isNullOrEmpty(wrappingElementPrefix)) {
            wrappingElement.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:" + wrappingElementPrefix, wrappingElementQName.getNamespaceURI());
        }

        // Get Hold of the Cipher Data
        NodeList cipherElements = encryptedDoc.getElementsByTagNameNS(EncryptionConstants.EncryptionSpecNS, EncryptionConstants._TAG_ENCRYPTEDDATA);
        if (cipherElements == null || cipherElements.getLength() == 0)
            throw logger.domMissingElementError("xenc:EncryptedData");
        Element encryptedDataElement = (Element) cipherElements.item(0);

        Node parentOfEncNode = encryptedDataElement.getParentNode();
        parentOfEncNode.replaceChild(wrappingElement, encryptedDataElement);

        wrappingElement.appendChild(encryptedDataElement);

        if (addEncryptedKeyInKeyInfo) {
            // Outer ds:KeyInfo Element to hold the EncryptionKey
            Element sigElement = encryptedDoc.createElementNS(XMLSignature.XMLNS, DS_KEY_INFO);
            sigElement.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:ds", XMLSignature.XMLNS);
            sigElement.appendChild(encryptedKeyElement);

            // Insert the Encrypted key before the CipherData element
            NodeList nodeList = encryptedDoc.getElementsByTagNameNS(EncryptionConstants.EncryptionSpecNS, EncryptionConstants._TAG_CIPHERDATA);
            if (nodeList == null || nodeList.getLength() == 0)
                throw logger.domMissingElementError("xenc:CipherData");
            Element cipherDataElement = (Element) nodeList.item(0);
            Node cipherParent = cipherDataElement.getParentNode();
            cipherParent.insertBefore(sigElement, cipherDataElement);
        } else {
            // Add the encrypted key as a child of the wrapping element
            wrappingElement.appendChild(encryptedKeyElement);
        }
    }

    /**
     * Decrypt an encrypted element inside a document
     *
     * @param documentWithEncryptedElement
     * @param privateKey key need to unwrap the encryption key
     *
     * @return the document with the encrypted element replaced by the data element
     */
    public static Element decryptElementInDocument(Document documentWithEncryptedElement, PrivateKey privateKey)
            throws ProcessingException {
        if (documentWithEncryptedElement == null)
            throw logger.nullArgumentError("Input document is null");

        // Look for encrypted data element
        Element documentRoot = documentWithEncryptedElement.getDocumentElement();
        Element encDataElement = getNextElementNode(documentRoot.getFirstChild());
        if (encDataElement == null)
            throw logger.domMissingElementError("No element representing the encrypted data found");

        // Look at siblings for the key
        Element encKeyElement = getNextElementNode(encDataElement.getNextSibling());
        if (encKeyElement == null) {
            // Search the enc data element for enc key
            NodeList nodeList = encDataElement.getElementsByTagNameNS(EncryptionConstants.EncryptionSpecNS, EncryptionConstants._TAG_ENCRYPTEDKEY);

            if (nodeList == null || nodeList.getLength() == 0)
                throw logger.nullValueError("Encrypted Key not found in the enc data");

            encKeyElement = (Element) nodeList.item(0);
        }

        XMLCipher cipher;
        EncryptedData encryptedData;
        EncryptedKey encryptedKey;
        try {
            cipher = XMLCipher.getInstance();
            cipher.init(XMLCipher.DECRYPT_MODE, null);
            encryptedData = cipher.loadEncryptedData(documentWithEncryptedElement, encDataElement);
            encryptedKey = cipher.loadEncryptedKey(documentWithEncryptedElement, encKeyElement);
        } catch (XMLEncryptionException e1) {
            throw logger.processingError(e1);
        }

        Document decryptedDoc = null;

        if (encryptedData != null && encryptedKey != null) {
            try {
                String encAlgoURL = encryptedData.getEncryptionMethod().getAlgorithm();
                XMLCipher keyCipher = XMLCipher.getInstance();
                keyCipher.init(XMLCipher.UNWRAP_MODE, privateKey);
                Key encryptionKey = keyCipher.decryptKey(encryptedKey, encAlgoURL);
                cipher = XMLCipher.getInstance();
                cipher.init(XMLCipher.DECRYPT_MODE, encryptionKey);

                decryptedDoc = cipher.doFinal(documentWithEncryptedElement, encDataElement);
            } catch (Exception e) {
                throw logger.processingError(e);
            }
        }

        if(decryptedDoc == null){
            throw logger.nullValueError("decryptedDoc");
        }

        Element decryptedRoot = decryptedDoc.getDocumentElement();
        Element dataElement = getNextElementNode(decryptedRoot.getFirstChild());
        if (dataElement == null)
            throw logger.nullValueError("Data Element after encryption is null");

        decryptedRoot.removeChild(dataElement);
        decryptedDoc.replaceChild(dataElement, decryptedRoot);

        return decryptedDoc.getDocumentElement();
    }

    /**
     * From the secret key, get the W3C XML Encryption URL
     *
     * @param publicKeyAlgo
     * @param keySize
     *
     * @return
     */
    private static String getXMLEncryptionURLForKeyUnwrap(String publicKeyAlgo, int keySize) {
        if ("AES".equals(publicKeyAlgo)) {
            switch (keySize) {
                case 192:
                    return XMLCipher.AES_192_KeyWrap;
                case 256:
                    return XMLCipher.AES_256_KeyWrap;
                default:
                    return XMLCipher.AES_128_KeyWrap;
            }
        }
        if (publicKeyAlgo.contains("RSA"))
            return RSA_ENCRYPTION_SCHEME;
        throw logger.unsupportedType("unsupported publicKey Algo:" + publicKeyAlgo);
    }

    /**
     * From the secret key, get the W3C XML Encryption URL
     *
     * @param secretKey
     * @param keySize
     *
     * @return
     */
    private static String getXMLEncryptionURL(String algo, int keySize) {
        if ("AES".equals(algo)) {
            switch (keySize) {
                case 192:
                    return XMLCipher.AES_192;
                case 256:
                    return XMLCipher.AES_256;
                default:
                    return XMLCipher.AES_128;
            }
        }
        if (algo.contains("RSA"))
            return XMLCipher.RSA_v1dot5;
        throw logger.unsupportedType("Secret Key with unsupported algo:" + algo);
    }

    /**
     * Returns the next Element node.
     */
    private static Element getNextElementNode(Node node) {
        while (node != null) {
            if (Node.ELEMENT_NODE == node.getNodeType())
                return (Element) node;
            node = node.getNextSibling();
        }
        return null;
    }
}
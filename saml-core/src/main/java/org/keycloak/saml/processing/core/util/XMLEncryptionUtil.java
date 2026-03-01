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

import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;
import javax.crypto.SecretKey;
import javax.xml.XMLConstants;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.namespace.QName;

import org.keycloak.saml.common.PicketLinkLogger;
import org.keycloak.saml.common.PicketLinkLoggerFactory;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.common.util.StringUtil;

import org.apache.xml.security.algorithms.JCEMapper;
import org.apache.xml.security.encryption.EncryptedData;
import org.apache.xml.security.encryption.EncryptedKey;
import org.apache.xml.security.encryption.XMLCipher;
import org.apache.xml.security.encryption.XMLEncryptionException;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.utils.EncryptionConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Utility for XML Encryption <b>Note: </b> This utility is currently using Apache XML Security library API. JSR-106 is
 * not yet
 * final. Until that happens,we rely on the non-standard API.
 *
 * @author Anil.Saldhana@redhat.com
 * @since May 4, 2009
 */
public class XMLEncryptionUtil {

    public interface DecryptionKeyLocator {

        /**
         * Provides a list of private keys that are suitable for decrypting
         * the given {@code encryptedData}.
         *
         * @param encryptedData data that need to be decrypted
         * @return a list of private keys
         */
        List<PrivateKey> getKeys(EncryptedData encryptedData);
    }

    private static final PicketLinkLogger logger = PicketLinkLoggerFactory.getLogger();

    static {
        // Initialize the Apache XML Security Library
        org.apache.xml.security.Init.init();
    }

    public static final String DS_KEY_INFO = "ds:KeyInfo";

    private static final String RSA_ENCRYPTION_SCHEME = XMLCipher.RSA_OAEP_11;

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
     *
     * @return
     *
     * @throws org.keycloak.saml.common.exceptions.ProcessingException
     */
    private static EncryptedKey encryptKey(Document document, SecretKey keyToBeEncrypted, PublicKey keyUsedToEncryptSecretKey,
                                          String keyEncryptionAlgorithm, String keyEncryptionDigestMethod,
                                          String keyEncryptionMgfAlgorithm) throws ProcessingException {
        XMLCipher keyCipher;

        try {
            keyCipher = XMLCipher.getInstance(keyEncryptionAlgorithm, null, keyEncryptionDigestMethod);

            keyCipher.init(XMLCipher.WRAP_MODE, keyUsedToEncryptSecretKey);
            return keyCipher.encryptKey(document, keyToBeEncrypted, keyEncryptionMgfAlgorithm, null);
        } catch (XMLEncryptionException e) {
            throw logger.processingError(e);
        }
    }

    public static String getJCEKeyAlgorithmFromURI(String algorithm) {
        return JCEMapper.getJCEKeyAlgorithmFromURI(algorithm);
    }

    public static int getKeyLengthFromURI(String algorithm) {
        return JCEMapper.getKeyLengthFromURI(algorithm);
    }

    public static void encryptElement(QName elementQName, Document document, PublicKey publicKey, SecretKey secretKey,
                                      int keySize, QName wrappingElementQName, boolean addEncryptedKeyInKeyInfo) throws ProcessingException {
        encryptElement(elementQName, document, publicKey, secretKey, keySize, wrappingElementQName, addEncryptedKeyInKeyInfo,
                null, null, null, null);
    }

    public static void encryptElement(QName elementQName, Document document, PublicKey publicKey, SecretKey secretKey,
                                      int keySize, QName wrappingElementQName, boolean addEncryptedKeyInKeyInfo,
                                      String keyEncryptionAlgorithm) throws ProcessingException {
        encryptElement(elementQName, document, publicKey, secretKey, keySize, wrappingElementQName,
                addEncryptedKeyInKeyInfo, null, keyEncryptionAlgorithm, null, null);
    }

    public static void encryptElement(QName elementQName, Document document, PublicKey publicKey, SecretKey secretKey,
                                      int keySize, QName wrappingElementQName, boolean addEncryptedKeyInKeyInfo, String keyEncryptionAlgorithm,
                                      String keyEncryptionDigestMethod, String keyEncryptionMgfAlgorithm) throws ProcessingException {
        encryptElement(elementQName, document, publicKey, secretKey, keySize, wrappingElementQName, addEncryptedKeyInKeyInfo,
                null, keyEncryptionAlgorithm, keyEncryptionDigestMethod, keyEncryptionMgfAlgorithm);
    }

    /**
     * Given an element in a Document, encrypt the element and replace the element in the document with the encrypted
     * data
     *
     * @param elementQName QName of the element that we like to encrypt
     * @param document The document with the element to encrypt
     * @param publicKey The public Key to wrap the secret key
     * @param secretKey The secret key to use for encryption
     * @param keySize The size of the public key
     * @param wrappingElementQName A QName of an element that will wrap the encrypted element
     * @param addEncryptedKeyInKeyInfo Need for the EncryptedKey to be placed in ds:KeyInfo
     * @param encryptionAlgorithm The encryption algorithm
     * @param keyEncryptionAlgorithm The wrap algorithm for the secret key (can be null, default is used depending the publicKey type)
     * @param keyEncryptionDigestMethod An optional digestMethod to use (can be null)
     * @param keyEncryptionMgfAlgorithm The xenc11 MGF Algorithm to use (can be null)
     *
     * @throws ProcessingException
     */
    public static void encryptElement(QName elementQName, Document document, PublicKey publicKey, SecretKey secretKey,
                                      int keySize, QName wrappingElementQName, boolean addEncryptedKeyInKeyInfo, String encryptionAlgorithm,
                                      String keyEncryptionAlgorithm, String keyEncryptionDigestMethod, String keyEncryptionMgfAlgorithm) throws ProcessingException {
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

        // set default algorithms
        if (encryptionAlgorithm == null) {
            // set default encryption based on the secret key passed
            encryptionAlgorithm = getXMLEncryptionURL(secretKey.getAlgorithm(), keySize);
        }

        if (keyEncryptionAlgorithm == null) {
            // get default one for the public key
            keyEncryptionAlgorithm = getXMLEncryptionURLForKeyUnwrap(publicKey.getAlgorithm(), keySize);
        }

        if ((XMLCipher.RSA_OAEP.equals(keyEncryptionAlgorithm) || XMLCipher.RSA_OAEP_11.equals(keyEncryptionAlgorithm))) {
            if (keyEncryptionDigestMethod == null) {
                keyEncryptionDigestMethod = XMLCipher.SHA256; // default digest method to SHA256
            } else if (XMLCipher.SHA1.equals(keyEncryptionDigestMethod)){
                keyEncryptionDigestMethod = null; // default by spec
            }
        } else {
            keyEncryptionDigestMethod = null; // not used for RSA_v1dot5
        }

        if (XMLCipher.RSA_OAEP_11.equals(keyEncryptionAlgorithm)) {
            if (keyEncryptionMgfAlgorithm == null) {
                keyEncryptionMgfAlgorithm = EncryptionConstants.MGF1_SHA256; // default mgf to mgf1sha256
            } else if (EncryptionConstants.MGF1_SHA1.equals(keyEncryptionMgfAlgorithm)) {
                keyEncryptionMgfAlgorithm = null; // default by spec
            }
        } else {
            keyEncryptionMgfAlgorithm = null; // only available for RSA_OAEP_11
        }

        EncryptedKey encryptedKey = encryptKey(document, secretKey, publicKey, keyEncryptionAlgorithm, keyEncryptionDigestMethod, keyEncryptionMgfAlgorithm);

        XMLCipher cipher = null;
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
     * Decrypts an encrypted element inside a document. It tries to use all
     * keys provided by {@code decryptionKeyLocator} and if it does not
     * succeed it throws {@link ProcessingException}.
     *
     * @param documentWithEncryptedElement document containing encrypted element
     * @param decryptionKeyLocator decryption key locator
     *
     * @return the document with the encrypted element replaced by the data element
     *
     * @throws ProcessingException when decrypting was not successful
     */
    public static Element decryptElementInDocument(Document documentWithEncryptedElement, DecryptionKeyLocator decryptionKeyLocator)
            throws ProcessingException {
        if (documentWithEncryptedElement == null)
            throw logger.nullArgumentError("Input document is null");

        // Look for encrypted data element
        Element documentRoot = documentWithEncryptedElement.getDocumentElement();
        Element encDataElement = getNextElementNode(documentRoot.getFirstChild());
        if (encDataElement == null)
            throw logger.domMissingElementError("No element representing the encrypted data found");

        XMLCipher cipher;
        EncryptedData encryptedData;
        EncryptedKey encryptedKey;
        try {
            cipher = XMLCipher.getInstance();
            cipher.init(XMLCipher.DECRYPT_MODE, null);
            encryptedData = cipher.loadEncryptedData(documentWithEncryptedElement, encDataElement);
            if (encryptedData.getKeyInfo() == null) {
                throw logger.domMissingElementError("No element representing KeyInfo found in the EncryptedData");
            }

            encryptedKey = encryptedData.getKeyInfo().itemEncryptedKey(0);
            if (encryptedKey == null) {
                // the encrypted key is not inside the encrypted data, locate it
                Element encKeyElement = locateEncryptedKeyElement(encDataElement);
                encryptedKey = cipher.loadEncryptedKey(documentWithEncryptedElement, encKeyElement);
                encryptedData.getKeyInfo().add(encryptedKey);
            }
        } catch (XMLSecurityException e1) {
            throw logger.processingError(e1);
        }

        Document decryptedDoc = null;

        if (encryptedData != null && encryptedKey != null) {
            boolean success = false;
            final Exception enclosingThrowable = new RuntimeException("Cannot decrypt element in document");
            List<PrivateKey> encryptionKeys;
            encryptionKeys = decryptionKeyLocator.getKeys(encryptedData);

            if (encryptionKeys == null || encryptionKeys.isEmpty()) {
                throw logger.nullValueError("Key for EncryptedData not found.");
            }

            for (PrivateKey privateKey : encryptionKeys) {
                try {
                    String encAlgoURL = encryptedData.getEncryptionMethod().getAlgorithm();
                    XMLCipher keyCipher = XMLCipher.getInstance();
                    keyCipher.init(XMLCipher.UNWRAP_MODE, privateKey);
                    Key encryptionKey = keyCipher.decryptKey(encryptedKey, encAlgoURL);
                    cipher = XMLCipher.getInstance();
                    cipher.init(XMLCipher.DECRYPT_MODE, encryptionKey);

                    decryptedDoc = cipher.doFinal(documentWithEncryptedElement, encDataElement);
                    success = true;
                    break;
                } catch (Exception e) {
                    enclosingThrowable.addSuppressed(e);
                }
            }

            if (!success) {
                throw logger.processingError(enclosingThrowable);
            }
        }

        if (decryptedDoc == null) {
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
     * Locates the EncryptedKey element once the EncryptedData element is found.
     * A exception is thrown if not found.
     *
     * @param encDataElement The EncryptedData element found
     * @return The EncryptedKey element
     */
    private static Element locateEncryptedKeyElement(Element encDataElement) {
        // Look at siblings for the key
        Element encKeyElement = getNextElementNode(encDataElement.getNextSibling());
        if (encKeyElement == null) {
            // Search the enc data element for enc key
            NodeList nodeList = encDataElement.getElementsByTagNameNS(EncryptionConstants.EncryptionSpecNS, EncryptionConstants._TAG_ENCRYPTEDKEY);

            if (nodeList == null || nodeList.getLength() == 0)
                throw logger.nullValueError("Encrypted Key not found in the enc data");

            encKeyElement = (Element) nodeList.item(0);
        }
        return encKeyElement;
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
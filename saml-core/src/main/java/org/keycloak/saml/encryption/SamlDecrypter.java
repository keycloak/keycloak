package org.keycloak.saml.encryption;

import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.dom.saml.v2.assertion.SAMLEncryptedAttribute;
import org.keycloak.dom.saml.v2.assertion.SAMLEncryptedType;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.xmlsec.w3.xmlenc.EncryptedKeyType;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.processing.core.parsers.saml.assertion.SAMLAttributeParser;
import org.keycloak.saml.processing.core.parsers.util.SAMLParserUtil;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.util.Arrays;
import java.util.Base64;

public class SamlDecrypter {

    public static byte[] decrypt(SAMLEncryptedType encryptedType, PrivateKey key) throws DecryptionException {
        if(encryptedType.getEncryptedData() == null) {
            throw new DecryptionException("Element had no EncryptedData child");
        }

        byte[] decSymKey = null;
        for(EncryptedKeyType encryptedKey : encryptedType.getEncryptedKeys()) {
            byte[] encryptedSymmetricKey = decode(encryptedKey.getCipherData().getCipherValue());
            decSymKey = decryptSymmetricKey(key, encryptedSymmetricKey);
        }

        byte[] encData = decode(encryptedType.getEncryptedData().getCipherData().getCipherValue());
        return decryptData(encData, decSymKey, encryptedType.getEncryptedData().getEncryptionMethod().getAlgorithm());
    }

    public static AttributeStatementType.ASTChoiceType decryptToASTChoiceType(SAMLEncryptedAttribute attribute, PrivateKey privateKey) throws DecryptionException {
        byte[] decryptedAttribute = decrypt(attribute, privateKey);
        try {
            XMLEventReader xmlEventReader = getXmlEventReader(decryptedAttribute);
            return new AttributeStatementType.ASTChoiceType(SAMLAttributeParser.getInstance().parse(xmlEventReader));
        } catch (XMLStreamException | ParsingException e) {
            String errorMessage = "Error creating ASTChoiceType from attribute " + Arrays.toString(decryptedAttribute);
            throw new DecryptionException(errorMessage, e);
        }
    }

    public static NameIDType decryptToNameID(SAMLEncryptedType attribute, PrivateKey privateKey) throws DecryptionException {
        byte[] decryptedAttribute = decrypt(attribute, privateKey);
        try {
            XMLEventReader xmlEventReader = getXmlEventReader(decryptedAttribute);
            return SAMLParserUtil.parseNameIDType(xmlEventReader);
        } catch (XMLStreamException | ParsingException e) {
            String errorMessage ="Error creating NameIDType from attribute " + Arrays.toString(decryptedAttribute);
            throw new DecryptionException(errorMessage, e);
        }
    }

    private static XMLEventReader getXmlEventReader(byte[] decryptedAttribute) throws XMLStreamException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(decryptedAttribute);
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        return inputFactory.createXMLEventReader(inputStream);
    }

    private static byte[] decryptData(byte[] encData, byte[] decSymKey, String algorithm) throws DecryptionException {
        try {
            Cipher decryptCipher = Cipher.getInstance("AES/CBC/NoPadding");
            final SecretKeySpec keySpec = new SecretKeySpec(decSymKey, "AES");

            // The encoded cipher text is prefixed by the IV.
            int ivLength = getIVLength(algorithm);
            byte[] IV = new byte[ivLength];
            byte[] encDataWithoutIV = new byte[encData.length - ivLength];
            System.arraycopy(encData, 0, IV, 0, ivLength);
            System.arraycopy(encData, ivLength, encDataWithoutIV, 0, encData.length - ivLength);

            final IvParameterSpec ivSpec = new IvParameterSpec(IV);
            decryptCipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            return decryptCipher.doFinal(encDataWithoutIV);
        } catch (GeneralSecurityException e) {
            throw new DecryptionException("Error while decrypting value.", e);
        }
    }

    private static int getIVLength(String algorithm) {
        int length;
        switch (algorithm) {
            case "http://www.w3.org/2001/04/xmlenc#aes256-cbc":
            case "http://www.w3.org/2001/04/xmlenc#aes192-cbc":
            case "http://www.w3.org/2001/04/xmlenc#aes128-cbc":
            default:
                length = 16;
                break;
        }
        return length;
    }

    private static byte[] decryptSymmetricKey(PrivateKey key, byte[] encryptedSymmetricKey) throws DecryptionException {
        try {
            Cipher decryptCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding");
            decryptCipher.init(Cipher.DECRYPT_MODE, key);
            return decryptCipher.doFinal(encryptedSymmetricKey);
        } catch (GeneralSecurityException e) {
            throw new DecryptionException("Error while decrypting symmetric key.", e);
        }
    }

    private static byte[] decode(byte[] encoded) {
        return Base64.getDecoder().decode(encoded);
    }
}

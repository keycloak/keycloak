package org.keycloak.authentication.authenticators.browser;

import java.nio.charset.StandardCharsets;
import java.security.spec.KeySpec;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;

public class EncryptionLogic {
    private static final String UNICODE_FORMAT;
    //private static final Logger LOGGER;
    private KeySpec keySpec;
    private SecretKeyFactory keyFactory;
    private Cipher cipher;
    public EncryptionLogic(String encryptionScheme, String encryptionKey) {
        if (encryptionKey != null && encryptionKey.trim().length() >= 24) {
            try {
                byte[] keyAsBytes = encryptionKey.getBytes(UNICODE_FORMAT);
                if (encryptionScheme.equals("DESede")) {
                    this.keySpec = new DESedeKeySpec(keyAsBytes);
                    this.keyFactory = SecretKeyFactory.getInstance(encryptionScheme);
                    this.cipher = Cipher.getInstance(encryptionScheme);
                }
            } catch (Exception var4) {
                //LOGGER.error("Encryption logic is not completed: {}", var4);
            }
        } else {
//            LOGGER.error("Encryption key was not valid");
        }
    }
    public String encrypt(String stringToEncrypt) {
        if (stringToEncrypt != null && stringToEncrypt.trim().length() != 0) {
            try {
                //Base64 base64encoder = new Base64();
                SecretKey key = this.keyFactory.generateSecret(this.keySpec);
                this.cipher.init(1, key);
                byte[] cleartext = stringToEncrypt.getBytes(UNICODE_FORMAT);
                byte[] ciphertext = this.cipher.doFinal(cleartext);
                return Base64.getEncoder().encodeToString(ciphertext);
            } catch (Exception var6) {
//                LOGGER.error("Encryption is not completed..returning same string: {}", var6);
                return stringToEncrypt;
            }
        } else {
//            LOGGER.warn("String was null or empty, can't be encrypted.");
            return stringToEncrypt;
        }
    }
    public String decrypt(String stringToDecrypt) {
        if (stringToDecrypt != null && stringToDecrypt.trim().length() >= 8) {
            try {
                SecretKey key = this.keyFactory.generateSecret(this.keySpec);
                this.cipher.init(2, key);
                byte[] cleartext = Base64.getDecoder().decode(stringToDecrypt.getBytes(StandardCharsets.UTF_8));
                byte[] ciphertext = this.cipher.doFinal(cleartext);
                return this.bytes2String(ciphertext);
            } catch (Exception var6) {
                return stringToDecrypt;
            }
        } else {
            return stringToDecrypt;
        }
    }
    private String bytes2String(byte[] bytes) {
        StringBuffer stringBuffer = new StringBuffer();
        for(int i = 0; i < bytes.length; ++i) {
            stringBuffer.append((char)bytes[i]);
        }
        return stringBuffer.toString();
    }
    static {
        UNICODE_FORMAT = StandardCharsets.UTF_8.toString();
    }
}

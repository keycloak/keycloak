package org.keycloak.models.utils;

import org.apache.commons.codec.binary.Hex;
import org.jboss.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES加密 解密
 */
public class AesEncrypt {

    protected final static Logger log = Logger.getLogger(AesEncrypt.class);
    private static final String keys = "dc58af43160ddf8cd776379c91cfdef88e31acbede1d56db2d34db534e069d77c0f104f71a9e05c05275b19aa8a88b497495e31412ce8fbb234234dsse64565";


    /**
     * 加密
     *
     * @param content
     * @param strKey
     * @return
     */
    public static String encrypt(String content, String strKey) {
        try {
            SecretKey key = generateMySQLAESKey(strKey, "ASCII");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] cleartext = content.getBytes("UTF-8");
            byte[] ciphertextBytes = cipher.doFinal(cleartext);
            return new String(Hex.encodeHex(ciphertextBytes));
        } catch (Exception e) {
            log.error("加密错误", e);
        }
        return content;
    }

    /**
     * 加密
     *
     * @param content
     * @return
     */
    public static String encrypt(String content) {
        return encrypt(content, keys);
    }


    /**
     * 解密
     *
     * @param content
     * @param aesKey
     * @return
     */
    public static String decrypt(String content, String aesKey) {
        try {
            SecretKey key = generateMySQLAESKey(aesKey, "ASCII");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] cleartext = Hex.decodeHex(content.toCharArray());
            byte[] ciphertextBytes = cipher.doFinal(cleartext);
            return new String(ciphertextBytes, "UTF-8");
        } catch (Exception e) {
            log.error("解密错误", e);
        }
        return content;
    }


    /**
     * 解密
     *
     * @param content
     * @return
     */
    public static String decrypt(String content) {
        return decrypt(content, keys);
    }


    public static SecretKeySpec generateMySQLAESKey(final String key, final String encoding) {
        try {
            final byte[] finalKey = new byte[16];
            int i = 0;
            for (byte b : key.getBytes(encoding)) {
                finalKey[i++ % 16] ^= b;
            }
            return new SecretKeySpec(finalKey, "AES");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}

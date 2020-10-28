package org.keycloak.crypto;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.UUID;

public class Aes128GcmEncryptor {
    private static final int IV_SIZE_BYTE = 12;
    private static final int AUTH_TAG_SIZE_BYTE = 16;
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final byte[] IV = new byte[IV_SIZE_BYTE];

    private Aes128GcmEncryptor() {
    }

    public static byte[] encrypt(String secret, byte[] bytes) throws GeneralSecurityException {
        return getCipher(Cipher.ENCRYPT_MODE, secret).doFinal(bytes);
    }

    public static byte[] decrypt(String secret, byte[] encryptedBytes) throws GeneralSecurityException {
        return getCipher(Cipher.DECRYPT_MODE, secret).doFinal(encryptedBytes);
    }

    private static Cipher getCipher(int mode, String secret) throws GeneralSecurityException {
        UUID uuid = UUID.fromString(secret);
        byte[] bytes = ByteBuffer.wrap(new byte[AUTH_TAG_SIZE_BYTE])
                               .putLong(uuid.getMostSignificantBits())
                               .putLong(uuid.getLeastSignificantBits())
                               .array();
        SecretKeySpec secretKey = new SecretKeySpec(bytes, "AES");
        Cipher cipher = Cipher.getInstance(ALGORITHM, new BouncyCastleProvider());
        GCMParameterSpec gcmParams = new GCMParameterSpec(AUTH_TAG_SIZE_BYTE * 8, IV);
        cipher.init(mode, secretKey, gcmParams);
        return cipher;
    }
}

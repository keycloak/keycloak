package org.keycloak.crypto.def;

import java.security.Key;
import javax.crypto.Cipher;

import org.keycloak.jose.jwe.JWEHeader;
import org.keycloak.jose.jwe.JWEHeader.JWEHeaderBuilder;
import org.keycloak.jose.jwe.JWEKeyStorage;
import org.keycloak.jose.jwe.alg.JWEAlgorithmProvider;
import org.keycloak.jose.jwe.enc.JWEEncryptionProvider;

public class DefaultRsaKeyEncryptionJWEAlgorithmProvider implements JWEAlgorithmProvider {

    private final String jcaAlgorithmName;

    public DefaultRsaKeyEncryptionJWEAlgorithmProvider(String jcaAlgorithmName) {
        this.jcaAlgorithmName = jcaAlgorithmName;
    }

    @Override
    public byte[] decodeCek(byte[] encodedCek, Key privateKey, JWEHeader header, JWEEncryptionProvider encryptionProvider) throws Exception {
        Cipher cipher = getCipherProvider();
        initCipher(cipher, Cipher.DECRYPT_MODE, privateKey);
        return cipher.doFinal(encodedCek);
    }

    @Override
    public byte[] encodeCek(JWEEncryptionProvider encryptionProvider, JWEKeyStorage keyStorage, Key publicKey, JWEHeaderBuilder headerBuilder) throws Exception {
        Cipher cipher = getCipherProvider();
        initCipher(cipher, Cipher.ENCRYPT_MODE, publicKey);
        byte[] cekBytes = keyStorage.getCekBytes();
        return cipher.doFinal(cekBytes);
    }

    private Cipher getCipherProvider() throws Exception {
        return Cipher.getInstance(jcaAlgorithmName);
    }

    protected void initCipher(Cipher cipher, int mode, Key key) throws Exception {
        cipher.init(mode, key);
    }
}

package org.keycloak.crypto.fips;

import java.security.Key;

import org.bouncycastle.crypto.KeyUnwrapper;
import org.bouncycastle.crypto.KeyWrapper;
import org.bouncycastle.crypto.SymmetricKey;
import org.bouncycastle.crypto.SymmetricSecretKey;
import org.bouncycastle.crypto.fips.FipsAES;
import org.keycloak.jose.jwe.JWEKeyStorage;
import org.keycloak.jose.jwe.alg.JWEAlgorithmProvider;
import org.keycloak.jose.jwe.enc.JWEEncryptionProvider;

/**
 * Variant of {@link org.keycloak.jose.jwe.alg.AesKeyWrapAlgorithmProvider} based on FIPS
 */
public class FIPSAesKeyWrapAlgorithmProvider implements JWEAlgorithmProvider {

    @Override
    public byte[] decodeCek(byte[] encodedCek, Key encryptionKey) throws Exception {
        byte[] keyBytes = encryptionKey.getEncoded(); // bytes making up AES key doing the wrapping
        SymmetricKey aesKey = new SymmetricSecretKey(FipsAES.KW, keyBytes);
        FipsAES.KeyWrapOperatorFactory factory = new FipsAES.KeyWrapOperatorFactory();
        KeyUnwrapper unwrapper = factory.createKeyUnwrapper(aesKey, FipsAES.KW);
        return unwrapper.unwrap(encodedCek, 0, encodedCek.length);
    }

    @Override
    public byte[] encodeCek(JWEEncryptionProvider encryptionProvider, JWEKeyStorage keyStorage, Key encryptionKey) throws Exception {
        byte[] inputKeyBytes = keyStorage.getCekBytes(); // bytes making up the key to be wrapped
        byte[] keyBytes = encryptionKey.getEncoded(); // bytes making up AES key doing the wrapping
        SymmetricKey aesKey = new SymmetricSecretKey(FipsAES.KW, keyBytes);
        FipsAES.KeyWrapOperatorFactory factory = new FipsAES.KeyWrapOperatorFactory();
        KeyWrapper wrapper = factory.createKeyWrapper(aesKey, FipsAES.KW);
        return wrapper.wrap(inputKeyBytes, 0, inputKeyBytes.length);
    }
}

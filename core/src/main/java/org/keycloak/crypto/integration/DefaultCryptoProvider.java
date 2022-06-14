package org.keycloak.crypto.integration;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.keycloak.jose.jwe.alg.AesKeyWrapAlgorithmProvider;
import org.keycloak.jose.jwe.alg.JWEAlgorithmProvider;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class DefaultCryptoProvider implements CryptoProvider {

    @Override
    public SecureRandom getSecureRandom() throws NoSuchAlgorithmException {
        return SecureRandom.getInstance("SHA1PRNG");
    }

    @Override
    public JWEAlgorithmProvider getAesKeyWrapAlgorithmProvider() {
        return new AesKeyWrapAlgorithmProvider();
    }
}

package org.keycloak.crypto.integration;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;

import org.keycloak.jose.jwe.alg.JWEAlgorithmProvider;

/**
 * Abstraction to handle differences between the APIs for non-fips and fips mode
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface CryptoProvider {

    /**
     * @return secureRandom implementation based on the available security algorithms according to environment (FIPS non-fips)
     */
    SecureRandom getSecureRandom() throws NoSuchAlgorithmException, NoSuchProviderException;

    JWEAlgorithmProvider getAesKeyWrapAlgorithmProvider();
}

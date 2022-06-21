package org.keycloak.crypto.fips;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;

import org.keycloak.crypto.integration.CryptoProvider;
import org.keycloak.jose.jwe.alg.JWEAlgorithmProvider;


/**
 * Integration based on FIPS 140-2
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class FIPS1402Provider implements CryptoProvider {

    @Override
    public SecureRandom getSecureRandom() throws NoSuchAlgorithmException, NoSuchProviderException {
        return SecureRandom.getInstance("DEFAULT","BCFIPS");
    }

    @Override
    public JWEAlgorithmProvider getAesKeyWrapAlgorithmProvider() {
        return new FIPSAesKeyWrapAlgorithmProvider();
    }
}

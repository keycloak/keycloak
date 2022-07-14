package org.keycloak.crypto.fips;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;
import org.keycloak.common.crypto.CryptoProvider;
import org.keycloak.common.crypto.CryptoProviderTypes;
import org.keycloak.common.crypto.CertificateUtilsProvider;
import org.keycloak.common.crypto.PemUtilsProvider;


/**
 * Integration based on FIPS 140-2
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class FIPS1402Provider implements CryptoProvider {

    private Map<String, Supplier<?>> providers = new HashMap<>();

    public FIPS1402Provider() {
        providers.put(CryptoProviderTypes.BC_SECURITY_PROVIDER, BouncyCastleFipsProvider::new);
        providers.put(CryptoProviderTypes.AES_KEY_WRAP_ALGORITHM_PROVIDER, FIPSAesKeyWrapAlgorithmProvider::new);
    }

    @Override
    public SecureRandom getSecureRandom() throws NoSuchAlgorithmException, NoSuchProviderException {
        return SecureRandom.getInstance("DEFAULT","BCFIPS");
    }

    @Override
    public <T> T getAlgorithmProvider(Class<T> clazz, String algorithm) {
        Object o = providers.get(algorithm).get();
        if (o == null) {
            throw new IllegalArgumentException("Not found provider of algorithm: " + algorithm);
        }
        return clazz.cast(o);
    }

    @Override
    public CertificateUtilsProvider getCertificateUtils() {
        return new BCFIPSCertificateUtilsProvider();
    }

    @Override
    public PemUtilsProvider getPemUtils() {
        return new BCFIPSPemUtilsProvider();
    }
}

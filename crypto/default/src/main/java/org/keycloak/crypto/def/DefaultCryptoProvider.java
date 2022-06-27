package org.keycloak.crypto.def;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.keycloak.common.crypto.CryptoProvider;
import org.keycloak.common.crypto.CryptoProviderTypes;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class DefaultCryptoProvider implements CryptoProvider {

    private Map<String, Supplier<?>> providers = new HashMap<>();

    public DefaultCryptoProvider() {
        providers.put(CryptoProviderTypes.BC_SECURITY_PROVIDER, BouncyCastleProvider::new);
        providers.put(CryptoProviderTypes.AES_KEY_WRAP_ALGORITHM_PROVIDER, AesKeyWrapAlgorithmProvider::new);
    }

    @Override
    public SecureRandom getSecureRandom() throws NoSuchAlgorithmException {
        return SecureRandom.getInstance("SHA1PRNG");
    }

    @Override
    public <T> T getAlgorithmProvider(Class<T> clazz, String algorithm) {
        Object o = providers.get(algorithm).get();
        if (o == null) {
            throw new IllegalArgumentException("Not found provider of algorithm: " + algorithm);
        }
        return clazz.cast(o);
    }
}

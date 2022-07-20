package org.keycloak.crypto.def.test;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.crypto.CryptoProviderTypes;
import org.keycloak.crypto.def.AesKeyWrapAlgorithmProvider;
import org.keycloak.jose.jwe.alg.JWEAlgorithmProvider;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class DefaultCryptoUnitTest {

    @Before
    public void init() {
        CryptoIntegration.init(ClassLoader.getSystemClassLoader());
    }

    @Test
    public void testDefaultCrypto() throws Exception {
        JWEAlgorithmProvider jweAlg = CryptoIntegration.getProvider().getAlgorithmProvider(JWEAlgorithmProvider.class, CryptoProviderTypes.AES_KEY_WRAP_ALGORITHM_PROVIDER);
        Assert.assertEquals(jweAlg.getClass(), AesKeyWrapAlgorithmProvider.class);
    }
}

package org.keycloak.crypto.def.test;

import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.crypto.CryptoConstants;
import org.keycloak.crypto.def.AesKeyWrapAlgorithmProvider;
import org.keycloak.jose.jwe.alg.JWEAlgorithmProvider;
import org.keycloak.rule.CryptoInitRule;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class DefaultCryptoUnitTest {

    @ClassRule
    public static CryptoInitRule cryptoInitRule = new CryptoInitRule();

    @Test
    public void testDefaultCrypto() throws Exception {
        JWEAlgorithmProvider jweAlg = CryptoIntegration.getProvider().getAlgorithmProvider(JWEAlgorithmProvider.class, CryptoConstants.A128KW);
        Assert.assertEquals(jweAlg.getClass(), AesKeyWrapAlgorithmProvider.class);
    }
}

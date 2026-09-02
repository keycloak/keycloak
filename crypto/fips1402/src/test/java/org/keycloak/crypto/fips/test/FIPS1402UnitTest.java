package org.keycloak.crypto.fips.test;

import org.keycloak.common.crypto.CryptoConstants;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.crypto.fips.FIPSAesKeyWrapAlgorithmProvider;
import org.keycloak.jose.jwe.alg.JWEAlgorithmProvider;
import org.keycloak.rule.CryptoInitRule;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class FIPS1402UnitTest {

    @ClassRule
    public static CryptoInitRule cryptoInitRule = new CryptoInitRule();

    @Test
    public void testFips() throws Exception {
        JWEAlgorithmProvider jweAlg = CryptoIntegration.getProvider().getAlgorithmProvider(JWEAlgorithmProvider.class, CryptoConstants.A128KW);
        Assert.assertEquals(jweAlg.getClass(), FIPSAesKeyWrapAlgorithmProvider.class);
    }
}

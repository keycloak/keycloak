package org.keycloak.crypto.fips.test;

import java.security.SecureRandom;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.crypto.fips.FIPSAesKeyWrapAlgorithmProvider;
import org.keycloak.crypto.integration.CryptoIntegration;
import org.keycloak.jose.jwe.alg.JWEAlgorithmProvider;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class FIPS1402UnitTest {


    @Test
    public void testFips() throws Exception {
        JWEAlgorithmProvider jweAlg = CryptoIntegration.getProvider().getAesKeyWrapAlgorithmProvider();
        Assert.assertEquals(jweAlg.getClass(), FIPSAesKeyWrapAlgorithmProvider.class);

        SecureRandom scr = CryptoIntegration.getProvider().getSecureRandom();
        Assert.assertEquals("BCFIPS", scr.getProvider().getName());
    }
}

package org.keycloak.crypto.fips.test;


import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.SecretKeySpec;

import org.keycloak.common.util.BouncyIntegration;
import org.keycloak.common.util.Environment;
import org.keycloak.jose.HmacTest;

import org.bouncycastle.crypto.CryptoServicesRegistrar;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;


/**
 * Another variation to this test using SecretKeyFactory
 *
 */
public class FIPS1402HmacTest extends HmacTest {

    @Before
    public void before() {
        // Run this test just if java is in FIPS mode
        Assume.assumeTrue("Java is not in FIPS mode. Skipping the test.", Environment.isJavaInFipsMode());
    }

    @Test
    public void testHmacSignaturesWithRandomSecretKeyCreatedByFactory() throws Exception {
        SecretKeyFactory skFact = SecretKeyFactory.getInstance("HmacSHA256", BouncyIntegration.PROVIDER );
        SecretKey secretKey = skFact.generateSecret(new SecretKeySpec(UUID.randomUUID().toString().getBytes(), "HmacSHA256"));
        testHMACSignAndVerify(secretKey, "testHmacSignaturesWithRandomSecretKeyCreatedByFactory");
    }

    @Override
    public void testHmacSignaturesWithShortSecretKey() throws Exception {
        // With BCFIPS approved mode, secret key used for HmacSHA256 must be at least 112 bits long (14 characters). Short key won't work
        Assume.assumeFalse(CryptoServicesRegistrar.isInApprovedOnlyMode());
        super.testHmacSignaturesWithShortSecretKey();
    }
}

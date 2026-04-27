package org.keycloak.crypto.fips.test;

import org.keycloak.KeyPairVerifierTest;
import org.keycloak.common.util.Environment;

import org.bouncycastle.crypto.CryptoServicesRegistrar;
import org.bouncycastle.crypto.fips.FipsUnapprovedOperationError;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

/**
 * Test with fips1402 security provider and bouncycastle-fips
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class FIPS1402KeyPairVerifierTest extends KeyPairVerifierTest {

    @Before
    public void before() {
        // Run this test just if java is in FIPS mode
        Assume.assumeTrue("Java is not in FIPS mode. Skipping the test.", Environment.isJavaInFipsMode());
    }

    @Test
    public void verifyWith1024PrivateKeyInTraditionalRSAFormat() throws Exception {
        // Signature generation with RSA 1024 key works just in non-approved mode
        Assume.assumeFalse(CryptoServicesRegistrar.isInApprovedOnlyMode());
        super.verifyWith1024PrivateKeyInTraditionalRSAFormat();
    }

    @Test
    public void verifyWith1024PrivateKeyInPKCS8Format() throws Exception {
        // Signature generation with RSA 1024 key works just in non-approved mode
        Assume.assumeFalse(CryptoServicesRegistrar.isInApprovedOnlyMode());
        super.verifyWith1024PrivateKeyInPKCS8Format();
    }

    @Test
    public void verifyWith1024PrivateKeyInTraditionalRSAFormatShouldFail() throws Exception {
        // Signature generation with RSA 1024 key works just in non-approved mode
        Assume.assumeTrue(CryptoServicesRegistrar.isInApprovedOnlyMode());
        try {
            super.verifyWith1024PrivateKeyInTraditionalRSAFormat();
            Assert.fail("Should not successfully generate signature with RSA 1024 key in BC approved mode");
        } catch (FipsUnapprovedOperationError fipsError) {
            // expected
        }
    }

    @Test
    public void verifyWith1024PrivateKeyInPKCS8FormatShouldFail() throws Exception {
        // Signature generation with RSA 1024 key works just in non-approved mode
        Assume.assumeTrue(CryptoServicesRegistrar.isInApprovedOnlyMode());
        try {
            super.verifyWith1024PrivateKeyInPKCS8Format();
            Assert.fail("Should not successfully generate signature with RSA 1024 key in BC approved mode");
        } catch (FipsUnapprovedOperationError fipsError) {
            // expected
        }
    }
}

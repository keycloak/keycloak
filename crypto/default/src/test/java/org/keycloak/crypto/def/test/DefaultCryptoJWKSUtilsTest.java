package org.keycloak.crypto.def.test;

import org.junit.Assume;
import org.junit.Before;
import org.keycloak.common.util.Environment;
import org.keycloak.util.JWKSUtilsTest;

/**
 * Test with bouncycastle security provider
 * 
 */
public class DefaultCryptoJWKSUtilsTest extends JWKSUtilsTest {

    @Before
    public void before() {
        // Run this test just if java is not in FIPS mode
        Assume.assumeFalse("Java is in FIPS mode. Skipping the test.", Environment.isJavaInFipsMode());
    }
}

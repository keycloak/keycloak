package org.keycloak.crypto.def.test;

import org.keycloak.common.util.Environment;
import org.keycloak.jose.JWETest;

import org.junit.Assume;
import org.junit.Before;

/**
 * Test with default security provider and non-fips bouncycastle
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class DefaultCryptoJWETest extends JWETest {

    @Before
    public void before() {
        // Run this test just if java is not in FIPS mode
        Assume.assumeFalse("Java is in FIPS mode. Skipping the test.", Environment.isJavaInFipsMode());
    }
}

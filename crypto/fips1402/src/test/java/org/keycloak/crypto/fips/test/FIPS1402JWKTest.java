package org.keycloak.crypto.fips.test;

import org.keycloak.common.util.Environment;
import org.keycloak.jose.jwk.JWKTest;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test with fips1402 security provider and bouncycastle-fips
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class FIPS1402JWKTest extends JWKTest {

    @Before
    public void before() {
        // Run this test just if java is in FIPS mode
        Assume.assumeTrue("Java is not in FIPS mode. Skipping the test.", Environment.isJavaInFipsMode());
    }

    @Ignore("Test not supported by BC FIPS")
    @Test
    public void publicEs256() throws Exception {
        // Do nothing
    }
}

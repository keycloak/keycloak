package org.keycloak.crypto.fips.test;

import org.junit.Before;
import org.junit.Assume;
import org.keycloak.KeyPairVerifierTest;
import org.keycloak.common.util.Environment;

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
}

package org.keycloak.crypto.fips.test;

import org.keycloak.common.util.Environment;
import org.keycloak.jose.JWETest;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

/**
 * Test with fips1402 security provider and bouncycastle-fips
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class FIPS1402JWETest extends JWETest {

    @Before
    public void before() {
        // Run this test just if java is in FIPS mode
        Assume.assumeTrue("Java is not in FIPS mode. Skipping the test.", Environment.isJavaInFipsMode());
    }

    @Test
    @Override
    public void testRSA1_5_A128GCM() throws Exception {
        // https://www.bouncycastle.org/download/bouncy-castle-java-fips/#release-notes
        // The provider blocks RSA with PKCS1.5 encryption
        Assume.assumeFalse("approved_only is set", Boolean.getBoolean("org.bouncycastle.fips.approved_only"));
        super.testRSA1_5_A128GCM();
    }

    @Test
    @Override
    public void testRSA1_5_A128CBCHS256() throws Exception {
        Assume.assumeFalse("approved_only is set", Boolean.getBoolean("org.bouncycastle.fips.approved_only"));
        super.testRSA1_5_A128CBCHS256();
    }
}

package org.keycloak.crypto.fips.test;

import org.keycloak.common.util.Environment;
import org.keycloak.util.PemUtilsTest;

import org.junit.Assume;
import org.junit.Before;

public class PemUtilsBCFIPSTest extends PemUtilsTest {

    @Before
    public void before() {
        // Run this test just if java is in FIPS mode
        Assume.assumeTrue("Java is not in FIPS mode. Skipping the test.", Environment.isJavaInFipsMode());
    }
}

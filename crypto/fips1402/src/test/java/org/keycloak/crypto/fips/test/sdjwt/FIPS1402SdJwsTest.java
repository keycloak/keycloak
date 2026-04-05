package org.keycloak.crypto.fips.test.sdjwt;

import org.keycloak.common.util.Environment;
import org.keycloak.sdjwt.SdJwsTest;

import org.junit.Assume;
import org.junit.Before;

public class FIPS1402SdJwsTest extends SdJwsTest {

    @Before
    public void before() {
        // Run this test just if java is not in FIPS mode
        Assume.assumeFalse("Java is in FIPS mode. Skipping the test.", Environment.isJavaInFipsMode());
    }
}

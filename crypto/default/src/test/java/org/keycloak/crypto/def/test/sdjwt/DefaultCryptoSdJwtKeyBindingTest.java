package org.keycloak.crypto.def.test.sdjwt;

import org.keycloak.common.util.Environment;
import org.keycloak.sdjwt.sdjwtvp.SdJwtKeyBindingTest;

import org.junit.Assume;
import org.junit.Before;

public class DefaultCryptoSdJwtKeyBindingTest extends SdJwtKeyBindingTest {

    @Before
    public void before() {
        // Run this test just if java is not in FIPS mode
        Assume.assumeFalse("Java is in FIPS mode. Skipping the test.", Environment.isJavaInFipsMode());
    }
}

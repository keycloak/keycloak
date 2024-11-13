package org.keycloak.crypto.def.test.sdjwt;

import org.junit.Assume;
import org.junit.Before;
import org.keycloak.common.util.Environment;
import org.keycloak.sdjwt.SdJwtFacadeTest;

/**
 * @author <a href="mailto:rodrick.awambeng@adorsys.com">Rodrick Awambeng</a>
 */
public class DefaultCryptoSdJwtFacadeTest extends SdJwtFacadeTest {

    @Before
    public void before() {
        // Run this test just if java is not in FIPS mode
        Assume.assumeFalse("Java is in FIPS mode. Skipping the test.", Environment.isJavaInFipsMode());
    }
}

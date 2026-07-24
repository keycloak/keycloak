package org.keycloak.crypto.def.test;

import org.keycloak.common.util.Environment;
import org.keycloak.jose.jwk.AKPJWKTest;

import org.junit.Assume;
import org.junit.Before;

public class DefaultCryptoAKPJWKTest extends AKPJWKTest {

    @Before
    public void before() {
        Assume.assumeFalse("Java is in FIPS mode. Skipping the test.", Environment.isJavaInFipsMode());
    }

}

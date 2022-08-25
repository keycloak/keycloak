package org.keycloak.crypto.def.test;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.common.util.Environment;

import java.security.NoSuchAlgorithmException;

import static org.junit.Assert.assertEquals;

public class PemUtilsBCTest {

    @Before
    public void before() {
        // Run this test just if java is not in FIPS mode
        Assume.assumeFalse("Java is in FIPS mode. Skipping the test.", Environment.isJavaInFipsMode());
    }

    @Test
    public void testGenerateThumbprintSha1() throws NoSuchAlgorithmException {
        String[] test = new String[] {"abcdefg"};
        String encoded = org.keycloak.common.util.PemUtils.generateThumbprint(test, "SHA-1");
        assertEquals(27, encoded.length());
    }

    @Test
    public void testGenerateThumbprintSha256() throws NoSuchAlgorithmException {
        String[] test = new String[] {"abcdefg"};
        String encoded = org.keycloak.common.util.PemUtils.generateThumbprint(test, "SHA-256");
        assertEquals(43, encoded.length());
    }
}


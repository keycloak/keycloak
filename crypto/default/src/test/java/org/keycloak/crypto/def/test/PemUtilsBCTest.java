package org.keycloak.crypto.def.test;

import org.junit.Test;

import java.security.NoSuchAlgorithmException;

import static org.junit.Assert.assertEquals;

public class PemUtilsBCTest {


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


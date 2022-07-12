package org.keycloak.crypto.fips.test;

import org.junit.Test;
import org.keycloak.common.util.PemUtils;

import java.security.NoSuchAlgorithmException;

import static org.junit.Assert.assertEquals;

public class PemUtilsBCFIPSTest {

    @Test
    public void testGenerateThumbprintSha1() throws NoSuchAlgorithmException {
        String[] test = new String[] {"abcdefg"};
        String encoded = PemUtils.generateThumbprint(test, "SHA-1");
        assertEquals(27, encoded.length());
    }

    @Test
    public void testGenerateThumbprintSha256() throws NoSuchAlgorithmException {
        String[] test = new String[] {"abcdefg"};
        String encoded = PemUtils.generateThumbprint(test, "SHA-256");
        assertEquals(43, encoded.length());
    }
}


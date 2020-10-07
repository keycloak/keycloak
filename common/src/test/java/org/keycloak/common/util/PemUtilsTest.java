package org.keycloak.common.util;

import org.junit.Test;

import java.security.NoSuchAlgorithmException;

import static org.junit.Assert.assertEquals;

public class PemUtilsTest {

    @Test
    public void testGenerateThumbprintBytesSha1() throws NoSuchAlgorithmException {
        String[] test = new String[] {"abcdefg"};
        byte[] digest = PemUtils.generateThumbprintBytes(test, "SHA-1");
        assertEquals(20, digest.length);
    }

    @Test
    public void testGenerateThumbprintBytesSha256() throws NoSuchAlgorithmException {
        String[] test = new String[] {"abcdefg"};
        byte[] digest = PemUtils.generateThumbprintBytes(test, "SHA-256");
        assertEquals(32, digest.length);
    }

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


package org.keycloak.common.util;

import org.junit.Test;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class KeyUtilsTest {

    @Test
    public void loadSecretKey() {
        byte[] secretBytes = new byte[32];
        ThreadLocalRandom.current().nextBytes(secretBytes);
        SecretKeySpec expected = new SecretKeySpec(secretBytes, "HmacSHA256");
        SecretKey actual = KeyUtils.loadSecretKey(secretBytes, "HmacSHA256");
        assertEquals(expected.getAlgorithm(), actual.getAlgorithm());
        assertArrayEquals(expected.getEncoded(), actual.getEncoded());
    }

}
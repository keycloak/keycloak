package org.keycloak.authentication.authenticators.util;

import org.junit.Assert;
import org.junit.Test;

public class PhoneOtpUtilsTest {

    @Test
    public void generateCodePadsWithZeros() {
        String code = PhoneOtpUtils.generateCode(6, () -> 42);
        Assert.assertEquals("000042", code);
    }

    @Test
    public void hashCodeChangesWithInput() {
        String salt = "test-salt";
        String hash1 = PhoneOtpUtils.hashCode("123456", salt);
        String hash2 = PhoneOtpUtils.hashCode("654321", salt);
        Assert.assertNotEquals(hash1, hash2);
    }

    @Test
    public void isExpiredUsesTtl() {
        Assert.assertFalse(PhoneOtpUtils.isExpired(100, 30, 129));
        Assert.assertTrue(PhoneOtpUtils.isExpired(100, 30, 131));
    }

    @Test
    public void cooldownRemainingHandlesElapsed() {
        Assert.assertEquals(Long.valueOf(10), PhoneOtpUtils.cooldownRemaining(100, 30, 120));
        Assert.assertNull(PhoneOtpUtils.cooldownRemaining(100, 30, 131));
    }
}

package org.keycloak.models;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class PasswordPolicyTest {

    @Test
    public void testLength() {
        PasswordPolicy policy = new PasswordPolicy("length");
        Assert.assertEquals("invalidPasswordMinLengthMessage", policy.validate("jdoe", "1234567").getMessage());
        Assert.assertArrayEquals(new Object[]{8}, policy.validate("jdoe", "1234567").getParameters());
        Assert.assertNull(policy.validate("jdoe", "12345678"));

        policy = new PasswordPolicy("length(4)");
        Assert.assertEquals("invalidPasswordMinLengthMessage", policy.validate("jdoe", "123").getMessage());
        Assert.assertArrayEquals(new Object[]{4}, policy.validate("jdoe", "123").getParameters());
        Assert.assertNull(policy.validate("jdoe", "1234"));
    }

    @Test
    public void testDigits() {
        PasswordPolicy policy = new PasswordPolicy("digits");
        Assert.assertEquals("invalidPasswordMinDigitsMessage", policy.validate("jdoe", "abcd").getMessage());
        Assert.assertArrayEquals(new Object[]{1}, policy.validate("jdoe", "abcd").getParameters());
        Assert.assertNull(policy.validate("jdoe", "abcd1"));

        policy = new PasswordPolicy("digits(2)");
        Assert.assertEquals("invalidPasswordMinDigitsMessage", policy.validate("jdoe", "abcd1").getMessage());
        Assert.assertArrayEquals(new Object[]{2}, policy.validate("jdoe", "abcd1").getParameters());
        Assert.assertNull(policy.validate("jdoe", "abcd12"));
    }

    @Test
    public void testLowerCase() {
        PasswordPolicy policy = new PasswordPolicy("lowerCase");
        Assert.assertEquals("invalidPasswordMinLowerCaseCharsMessage", policy.validate("jdoe", "ABCD1234").getMessage());
        Assert.assertArrayEquals(new Object[]{1}, policy.validate("jdoe", "ABCD1234").getParameters());
        Assert.assertNull(policy.validate("jdoe", "ABcD1234"));

        policy = new PasswordPolicy("lowerCase(2)");
        Assert.assertEquals("invalidPasswordMinLowerCaseCharsMessage", policy.validate("jdoe", "ABcD1234").getMessage());
        Assert.assertArrayEquals(new Object[]{2}, policy.validate("jdoe", "ABcD1234").getParameters());
        Assert.assertNull(policy.validate("jdoe", "aBcD1234"));
    }

    @Test
    public void testUpperCase() {
        PasswordPolicy policy = new PasswordPolicy("upperCase");
        Assert.assertEquals("invalidPasswordMinUpperCaseCharsMessage", policy.validate("jdoe", "abcd1234").getMessage());
        Assert.assertArrayEquals(new Object[]{1}, policy.validate("jdoe", "abcd1234").getParameters());
        Assert.assertNull(policy.validate("jdoe", "abCd1234"));

        policy = new PasswordPolicy("upperCase(2)");
        Assert.assertEquals("invalidPasswordMinUpperCaseCharsMessage", policy.validate("jdoe", "abCd1234").getMessage());
        Assert.assertArrayEquals(new Object[]{2}, policy.validate("jdoe", "abCd1234").getParameters());
        Assert.assertNull(policy.validate("jdoe", "AbCd1234"));
    }

    @Test
    public void testSpecialChars() {
        PasswordPolicy policy = new PasswordPolicy("specialChars");
        Assert.assertEquals("invalidPasswordMinSpecialCharsMessage", policy.validate("jdoe", "abcd1234").getMessage());
        Assert.assertArrayEquals(new Object[]{1}, policy.validate("jdoe", "abcd1234").getParameters());
        Assert.assertNull(policy.validate("jdoe", "ab&d1234"));

        policy = new PasswordPolicy("specialChars(2)");
        Assert.assertEquals("invalidPasswordMinSpecialCharsMessage", policy.validate("jdoe", "ab&d1234").getMessage());
        Assert.assertArrayEquals(new Object[]{2}, policy.validate("jdoe", "ab&d1234").getParameters());
        Assert.assertNull(policy.validate("jdoe", "ab&d-234"));
    }

    @Test
    public void testNotUsername() {
        PasswordPolicy policy = new PasswordPolicy("notUsername");
        Assert.assertEquals("invalidPasswordNotUsernameMessage", policy.validate("jdoe", "jdoe").getMessage());
        Assert.assertNull(policy.validate("jdoe", "ab&d1234"));
    }

    @Test
    public void testComplex() {
        PasswordPolicy policy = new PasswordPolicy("length(8) and digits(2) and lowerCase(2) and upperCase(2) and specialChars(2) and notUsername()");
        Assert.assertNotNull(policy.validate("jdoe", "12aaBB&"));
        Assert.assertNotNull(policy.validate("jdoe", "aaaaBB&-"));
        Assert.assertNotNull(policy.validate("jdoe", "12AABB&-"));
        Assert.assertNotNull(policy.validate("jdoe", "12aabb&-"));
        Assert.assertNotNull(policy.validate("jdoe", "12aaBBcc"));
        Assert.assertNotNull(policy.validate("12aaBB&-", "12aaBB&-"));

        Assert.assertNull(policy.validate("jdoe", "12aaBB&-"));
    }

}

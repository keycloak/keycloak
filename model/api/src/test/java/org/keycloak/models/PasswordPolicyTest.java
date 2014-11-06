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
        Assert.assertEquals("Invalid password: minimum length 8", policy.validate("1234567"));
        Assert.assertNull(policy.validate("12345678"));

        policy = new PasswordPolicy("length(4)");
        Assert.assertEquals("Invalid password: minimum length 4", policy.validate("123"));
        Assert.assertNull(policy.validate("1234"));
    }

    @Test
    public void testDigits() {
        PasswordPolicy policy = new PasswordPolicy("digits");
        Assert.assertEquals("Invalid password: must contain at least 1 numerical digits", policy.validate("abcd"));
        Assert.assertNull(policy.validate("abcd1"));

        policy = new PasswordPolicy("digits(2)");
        Assert.assertEquals("Invalid password: must contain at least 2 numerical digits", policy.validate("abcd1"));
        Assert.assertNull(policy.validate("abcd12"));
    }

    @Test
    public void testLowerCase() {
        PasswordPolicy policy = new PasswordPolicy("lowerCase");
        Assert.assertEquals("Invalid password: must contain at least 1 lower case characters", policy.validate("ABCD1234"));
        Assert.assertNull(policy.validate("ABcD1234"));

        policy = new PasswordPolicy("lowerCase(2)");
        Assert.assertEquals("Invalid password: must contain at least 2 lower case characters", policy.validate("ABcD1234"));
        Assert.assertNull(policy.validate("aBcD1234"));
    }

    @Test
    public void testUpperCase() {
        PasswordPolicy policy = new PasswordPolicy("upperCase");
        Assert.assertEquals("Invalid password: must contain at least 1 upper case characters", policy.validate("abcd1234"));
        Assert.assertNull(policy.validate("abCd1234"));

        policy = new PasswordPolicy("upperCase(2)");
        Assert.assertEquals("Invalid password: must contain at least 2 upper case characters", policy.validate("abCd1234"));
        Assert.assertNull(policy.validate("AbCd1234"));
    }

    @Test
    public void testSpecialChars() {
        PasswordPolicy policy = new PasswordPolicy("specialChars");
        Assert.assertEquals("Invalid password: must contain at least 1 special characters", policy.validate("abcd1234"));
        Assert.assertNull(policy.validate("ab&d1234"));

        policy = new PasswordPolicy("specialChars(2)");
        Assert.assertEquals("Invalid password: must contain at least 2 special characters", policy.validate("ab&d1234"));
        Assert.assertNull(policy.validate("ab&d-234"));
    }

    @Test
    public void testComplex() {
        PasswordPolicy policy = new PasswordPolicy("length(8) and digits(2) and lowerCase(2) and upperCase(2) and specialChars(2)");
        Assert.assertNotNull(policy.validate("12aaBB&"));
        Assert.assertNotNull(policy.validate("aaaaBB&-"));
        Assert.assertNotNull(policy.validate("12AABB&-"));
        Assert.assertNotNull(policy.validate("12aabb&-"));
        Assert.assertNotNull(policy.validate("12aaBBcc"));

        Assert.assertNull(policy.validate("12aaBB&-"));
    }

}

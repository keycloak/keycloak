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
        Assert.assertNotNull(policy.validate("1234567"));
        Assert.assertNull(policy.validate("12345678"));

        policy = new PasswordPolicy("length(4)");
        Assert.assertNotNull(policy.validate("123"));
        Assert.assertNull(policy.validate("1234"));
    }

    @Test
    public void testDigits() {
        PasswordPolicy policy = new PasswordPolicy("digits");
        Assert.assertNotNull(policy.validate("abcd"));
        Assert.assertNull(policy.validate("abcd1"));

        policy = new PasswordPolicy("digits(2)");
        Assert.assertNotNull(policy.validate("abcd1"));
        Assert.assertNull(policy.validate("abcd12"));
    }

    @Test
    public void testLowerCase() {
        PasswordPolicy policy = new PasswordPolicy("lowerCase");
        Assert.assertNotNull(policy.validate("ABCD1234"));
        Assert.assertNull(policy.validate("ABcD1234"));

        policy = new PasswordPolicy("lowerCase(2)");
        Assert.assertNotNull(policy.validate("ABcD1234"));
        Assert.assertNull(policy.validate("aBcD1234"));
    }

    @Test
    public void testUpperCase() {
        PasswordPolicy policy = new PasswordPolicy("upperCase");
        Assert.assertNotNull(policy.validate("abcd1234"));
        Assert.assertNull(policy.validate("abCd1234"));

        policy = new PasswordPolicy("upperCase(2)");
        Assert.assertNotNull(policy.validate("abCd1234"));
        Assert.assertNull(policy.validate("AbCd1234"));
    }

    @Test
    public void testSpecialChars() {
        PasswordPolicy policy = new PasswordPolicy("specialChars");
        Assert.assertNotNull(policy.validate("abcd1234"));
        Assert.assertNull(policy.validate("ab&d1234"));

        policy = new PasswordPolicy("specialChars(2)");
        Assert.assertNotNull(policy.validate("ab&d1234"));
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

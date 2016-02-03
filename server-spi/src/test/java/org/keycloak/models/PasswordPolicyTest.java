/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.models;

import org.junit.Assert;
import org.junit.Test;

import java.util.regex.PatternSyntaxException;

import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class PasswordPolicyTest {

    @Test
    public void testLength() {
        PasswordPolicy policy = new PasswordPolicy("length");
        Assert.assertEquals("invalidPasswordMinLengthMessage", policy.validate(null, "jdoe", "1234567").getMessage());
        Assert.assertArrayEquals(new Object[]{8}, policy.validate(null, "jdoe", "1234567").getParameters());
        Assert.assertNull(policy.validate(null, "jdoe", "12345678"));

        policy = new PasswordPolicy("length(4)");
        Assert.assertEquals("invalidPasswordMinLengthMessage", policy.validate(null, "jdoe", "123").getMessage());
        Assert.assertArrayEquals(new Object[]{4}, policy.validate(null, "jdoe", "123").getParameters());
        Assert.assertNull(policy.validate(null, "jdoe", "1234"));
    }

    @Test
    public void testDigits() {
        PasswordPolicy policy = new PasswordPolicy("digits");
        Assert.assertEquals("invalidPasswordMinDigitsMessage", policy.validate(null, "jdoe", "abcd").getMessage());
        Assert.assertArrayEquals(new Object[]{1}, policy.validate(null, "jdoe", "abcd").getParameters());
        Assert.assertNull(policy.validate(null, "jdoe", "abcd1"));

        policy = new PasswordPolicy("digits(2)");
        Assert.assertEquals("invalidPasswordMinDigitsMessage", policy.validate(null, "jdoe", "abcd1").getMessage());
        Assert.assertArrayEquals(new Object[]{2}, policy.validate(null, "jdoe", "abcd1").getParameters());
        Assert.assertNull(policy.validate(null, "jdoe", "abcd12"));
    }

    @Test
    public void testLowerCase() {
        PasswordPolicy policy = new PasswordPolicy("lowerCase");
        Assert.assertEquals("invalidPasswordMinLowerCaseCharsMessage", policy.validate(null, "jdoe", "ABCD1234").getMessage());
        Assert.assertArrayEquals(new Object[]{1}, policy.validate(null, "jdoe", "ABCD1234").getParameters());
        Assert.assertNull(policy.validate(null, "jdoe", "ABcD1234"));

        policy = new PasswordPolicy("lowerCase(2)");
        Assert.assertEquals("invalidPasswordMinLowerCaseCharsMessage", policy.validate(null, "jdoe", "ABcD1234").getMessage());
        Assert.assertArrayEquals(new Object[]{2}, policy.validate(null, "jdoe", "ABcD1234").getParameters());
        Assert.assertNull(policy.validate(null, "jdoe", "aBcD1234"));
    }

    @Test
    public void testUpperCase() {
        PasswordPolicy policy = new PasswordPolicy("upperCase");
        Assert.assertEquals("invalidPasswordMinUpperCaseCharsMessage", policy.validate(null, "jdoe", "abcd1234").getMessage());
        Assert.assertArrayEquals(new Object[]{1}, policy.validate(null, "jdoe", "abcd1234").getParameters());
        Assert.assertNull(policy.validate(null, "jdoe", "abCd1234"));

        policy = new PasswordPolicy("upperCase(2)");
        Assert.assertEquals("invalidPasswordMinUpperCaseCharsMessage", policy.validate(null, "jdoe", "abCd1234").getMessage());
        Assert.assertArrayEquals(new Object[]{2}, policy.validate(null, "jdoe", "abCd1234").getParameters());
        Assert.assertNull(policy.validate(null, "jdoe", "AbCd1234"));
    }

    @Test
    public void testSpecialChars() {
        PasswordPolicy policy = new PasswordPolicy("specialChars");
        Assert.assertEquals("invalidPasswordMinSpecialCharsMessage", policy.validate(null, "jdoe", "abcd1234").getMessage());
        Assert.assertArrayEquals(new Object[]{1}, policy.validate(null, "jdoe", "abcd1234").getParameters());
        Assert.assertNull(policy.validate(null, "jdoe", "ab&d1234"));

        policy = new PasswordPolicy("specialChars(2)");
        Assert.assertEquals("invalidPasswordMinSpecialCharsMessage", policy.validate(null, "jdoe", "ab&d1234").getMessage());
        Assert.assertArrayEquals(new Object[]{2}, policy.validate(null, "jdoe", "ab&d1234").getParameters());
        Assert.assertNull(policy.validate(null, "jdoe", "ab&d-234"));
    }

    @Test
    public void testNotUsername() {
        PasswordPolicy policy = new PasswordPolicy("notUsername");
        Assert.assertEquals("invalidPasswordNotUsernameMessage", policy.validate(null, "jdoe", "jdoe").getMessage());
        Assert.assertNull(policy.validate(null, "jdoe", "ab&d1234"));
    }

    @Test
    public void testInvalidPolicyName() {
        try {
            PasswordPolicy policy = new PasswordPolicy("noSuchPolicy");
            Assert.fail("Expected exception");
        } catch (IllegalArgumentException e) {
        }
    }
    
    @Test
    public void testRegexPatterns() {
        PasswordPolicy policy = null;
        try {
            policy = new PasswordPolicy("regexPattern");
            fail("Expected NullPointerException: Regex Pattern cannot be null.");
        } catch (NullPointerException e) {
            // Expected NPE as regex pattern is null.
        }
        
        try {
            policy = new PasswordPolicy("regexPattern(*)");
            fail("Expected PatternSyntaxException: Regex Pattern cannot be null.");
        } catch (PatternSyntaxException e) {
            // Expected PSE as regex pattern(or any of its token) is not quantifiable.
        }
        
        try {
            policy = new PasswordPolicy("regexPattern(*,**)");
            fail("Expected PatternSyntaxException: Regex Pattern cannot be null.");
        } catch (PatternSyntaxException e) {
            // Expected PSE as regex pattern(or any of its token) is not quantifiable.
        }
        
        //Fails to match one of the regex pattern
        policy = new PasswordPolicy("regexPattern(jdoe) and regexPattern(j*d)");
        Assert.assertEquals("invalidPasswordRegexPatternMessage", policy.validate(null, "jdoe", "jdoe").getMessage());
        
        ////Fails to match all of the regex patterns
        policy = new PasswordPolicy("regexPattern(j*p) and regexPattern(j*d) and regexPattern(adoe)");
        Assert.assertEquals("invalidPasswordRegexPatternMessage", policy.validate(null, "jdoe", "jdoe").getMessage());
        
        policy = new PasswordPolicy("regexPattern([a-z][a-z][a-z][a-z][0-9])");
        Assert.assertEquals("invalidPasswordRegexPatternMessage", policy.validate(null, "jdoe", "jdoe").getMessage());
        
        policy = new PasswordPolicy("regexPattern(jdoe)");
        Assert.assertNull(policy.validate(null, "jdoe", "jdoe"));
        
        policy = new PasswordPolicy("regexPattern([a-z][a-z][a-z][a-z][0-9])");
        Assert.assertNull(policy.validate(null, "jdoe", "jdoe0"));
    }

    @Test
    public void testComplex() {
        PasswordPolicy policy = new PasswordPolicy("length(8) and digits(2) and lowerCase(2) and upperCase(2) and specialChars(2) and notUsername()");
        Assert.assertNotNull(policy.validate(null, "jdoe", "12aaBB&"));
        Assert.assertNotNull(policy.validate(null, "jdoe", "aaaaBB&-"));
        Assert.assertNotNull(policy.validate(null, "jdoe", "12AABB&-"));
        Assert.assertNotNull(policy.validate(null, "jdoe", "12aabb&-"));
        Assert.assertNotNull(policy.validate(null, "jdoe", "12aaBBcc"));
        Assert.assertNotNull(policy.validate(null, "12aaBB&-", "12aaBB&-"));

        Assert.assertNull(policy.validate(null, "jdoe", "12aaBB&-"));
    }

}

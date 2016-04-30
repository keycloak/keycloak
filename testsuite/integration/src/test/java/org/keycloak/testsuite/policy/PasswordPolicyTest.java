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

package org.keycloak.testsuite.policy;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import org.keycloak.models.KeycloakSession;
import org.keycloak.policy.PasswordPolicy;
import org.keycloak.testsuite.rule.KeycloakRule;

import java.util.regex.PatternSyntaxException;

import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class PasswordPolicyTest {

    @ClassRule
    public static KeycloakRule kc = new KeycloakRule();

    private KeycloakSession session;

    @Before
    public void before() throws Exception {
        session = kc.startSession();
    }

    @After
    public void after() throws Exception {
        kc.stopSession(session, true);
    }

    @Test
    public void testLength() {
        PasswordPolicy policy = new PasswordPolicy("length", session);
        Assert.assertEquals("invalidPasswordMinLengthMessage", policy.validate(null, "jdoe", "1234567").getMessage());
        Assert.assertArrayEquals(new Object[]{8}, policy.validate(null, "jdoe", "1234567").getParameters());
        Assert.assertNull(policy.validate(null, "jdoe", "12345678"));

        policy = new PasswordPolicy("length(4)", session);
        Assert.assertEquals("invalidPasswordMinLengthMessage", policy.validate(null, "jdoe", "123").getMessage());
        Assert.assertArrayEquals(new Object[]{4}, policy.validate(null, "jdoe", "123").getParameters());
        Assert.assertNull(policy.validate(null, "jdoe", "1234"));
    }

    @Test
    public void testDigits() {
        PasswordPolicy policy = new PasswordPolicy("digits", session);
        Assert.assertEquals("invalidPasswordMinDigitsMessage", policy.validate(null, "jdoe", "abcd").getMessage());
        Assert.assertArrayEquals(new Object[]{1}, policy.validate(null, "jdoe", "abcd").getParameters());
        Assert.assertNull(policy.validate(null, "jdoe", "abcd1"));

        policy = new PasswordPolicy("digits(2)", session);
        Assert.assertEquals("invalidPasswordMinDigitsMessage", policy.validate(null, "jdoe", "abcd1").getMessage());
        Assert.assertArrayEquals(new Object[]{2}, policy.validate(null, "jdoe", "abcd1").getParameters());
        Assert.assertNull(policy.validate(null, "jdoe", "abcd12"));
    }

    @Test
    public void testLowerCase() {
        PasswordPolicy policy = new PasswordPolicy("lowerCase", session);
        Assert.assertEquals("invalidPasswordMinLowerCaseCharsMessage", policy.validate(null, "jdoe", "ABCD1234").getMessage());
        Assert.assertArrayEquals(new Object[]{1}, policy.validate(null, "jdoe", "ABCD1234").getParameters());
        Assert.assertNull(policy.validate(null, "jdoe", "ABcD1234"));

        policy = new PasswordPolicy("lowerCase(2)", session);
        Assert.assertEquals("invalidPasswordMinLowerCaseCharsMessage", policy.validate(null, "jdoe", "ABcD1234").getMessage());
        Assert.assertArrayEquals(new Object[]{2}, policy.validate(null, "jdoe", "ABcD1234").getParameters());
        Assert.assertNull(policy.validate(null, "jdoe", "aBcD1234"));
    }

    @Test
    public void testUpperCase() {
        PasswordPolicy policy = new PasswordPolicy("upperCase", session);
        Assert.assertEquals("invalidPasswordMinUpperCaseCharsMessage", policy.validate(null, "jdoe", "abcd1234").getMessage());
        Assert.assertArrayEquals(new Object[]{1}, policy.validate(null, "jdoe", "abcd1234").getParameters());
        Assert.assertNull(policy.validate(null, "jdoe", "abCd1234"));

        policy = new PasswordPolicy("upperCase(2)", session);
        Assert.assertEquals("invalidPasswordMinUpperCaseCharsMessage", policy.validate(null, "jdoe", "abCd1234").getMessage());
        Assert.assertArrayEquals(new Object[]{2}, policy.validate(null, "jdoe", "abCd1234").getParameters());
        Assert.assertNull(policy.validate(null, "jdoe", "AbCd1234"));
    }

    @Test
    public void testSpecialChars() {
        PasswordPolicy policy = new PasswordPolicy("specialChars", session);
        Assert.assertEquals("invalidPasswordMinSpecialCharsMessage", policy.validate(null, "jdoe", "abcd1234").getMessage());
        Assert.assertArrayEquals(new Object[]{1}, policy.validate(null, "jdoe", "abcd1234").getParameters());
        Assert.assertNull(policy.validate(null, "jdoe", "ab&d1234"));

        policy = new PasswordPolicy("specialChars(2)", session);
        Assert.assertEquals("invalidPasswordMinSpecialCharsMessage", policy.validate(null, "jdoe", "ab&d1234").getMessage());
        Assert.assertArrayEquals(new Object[]{2}, policy.validate(null, "jdoe", "ab&d1234").getParameters());
        Assert.assertNull(policy.validate(null, "jdoe", "ab&d-234"));
    }

    @Test
    public void testNotUsername() {
        PasswordPolicy policy = new PasswordPolicy("notUsername", session);
        Assert.assertEquals("invalidPasswordNotUsernameMessage", policy.validate(null, "jdoe", "jdoe").getMessage());
        Assert.assertNull(policy.validate(null, "jdoe", "ab&d1234"));
    }

    @Test
    public void testInvalidPolicyName() {
        try {
            PasswordPolicy policy = new PasswordPolicy("noSuchPolicy", session);
            Assert.fail("Expected exception");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void testRegexPatterns() {
        PasswordPolicy policy = null;
        try {
            policy = new PasswordPolicy("regexPattern", session);
            fail("Expected NullPointerException: Regex Pattern cannot be null.");
        } catch (NullPointerException e) {
            // Expected NPE as regex pattern is null.
        }

        try {
            policy = new PasswordPolicy("regexPattern(*)", session);
            policy.validate(session, "joe", "test");
            fail("Expected PatternSyntaxException: Regex Pattern cannot be null.");
        } catch (PatternSyntaxException e) {
            // Expected PSE as regex pattern(or any of its token) is not quantifiable.
        }

        try {
            policy = new PasswordPolicy("regexPattern(*,**)", session);
            policy.validate(session, "joe", "test");
            fail("Expected PatternSyntaxException: Regex Pattern cannot be null.");
        } catch (PatternSyntaxException e) {
            // Expected PSE as regex pattern(or any of its token) is not quantifiable.
        }

        //Fails to match one of the regex pattern
        policy = new PasswordPolicy("regexPattern(jdoe) and regexPattern(j*d)", session);
        Assert.assertEquals("invalidPasswordRegexPatternMessage", policy.validate(null, "jdoe", "jdoe").getMessage());

        ////Fails to match all of the regex patterns
        policy = new PasswordPolicy("regexPattern(j*p) and regexPattern(j*d) and regexPattern(adoe)", session);
        Assert.assertEquals("invalidPasswordRegexPatternMessage", policy.validate(null, "jdoe", "jdoe").getMessage());

        policy = new PasswordPolicy("regexPattern([a-z][a-z][a-z][a-z][0-9])", session);
        Assert.assertEquals("invalidPasswordRegexPatternMessage", policy.validate(null, "jdoe", "jdoe").getMessage());

        policy = new PasswordPolicy("regexPattern(jdoe)", session);
        Assert.assertNull(policy.validate(null, "jdoe", "jdoe"));

        policy = new PasswordPolicy("regexPattern([a-z][a-z][a-z][a-z][0-9])", session);
        Assert.assertNull(policy.validate(null, "jdoe", "jdoe0"));
    }

    @Test
    public void testComplex() {
        PasswordPolicy policy = new PasswordPolicy("length(8) and digits(2) and lowerCase(2) and upperCase(2) and specialChars(2) and notUsername()", session);
        Assert.assertNotNull(policy.validate(null, "jdoe", "12aaBB&"));
        Assert.assertNotNull(policy.validate(null, "jdoe", "aaaaBB&-"));
        Assert.assertNotNull(policy.validate(null, "jdoe", "12AABB&-"));
        Assert.assertNotNull(policy.validate(null, "jdoe", "12aabb&-"));
        Assert.assertNotNull(policy.validate(null, "jdoe", "12aaBBcc"));
        Assert.assertNotNull(policy.validate(null, "12aaBB&-", "12aaBB&-"));

        Assert.assertNull(policy.validate(null, "jdoe", "12aaBB&-"));
    }
}

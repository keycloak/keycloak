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

package org.keycloak.testsuite.model;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.policy.PasswordPolicyManagerProvider;

import java.util.regex.PatternSyntaxException;

import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class PasswordPolicyTest extends AbstractModelTest {

    private RealmModel realmModel;
    private PasswordPolicyManagerProvider policyManager;

    @Before
    public void before() throws Exception {
        super.before();
        realmModel = realmManager.createRealm("JUGGLER");
        session.getContext().setRealm(realmModel);
        policyManager = session.getProvider(PasswordPolicyManagerProvider.class);
    }

    @Test
    public void testLength() {
        realmModel.setPasswordPolicy(PasswordPolicy.parse(session, "length"));

        Assert.assertEquals("invalidPasswordMinLengthMessage", policyManager.validate("jdoe", "1234567").getMessage());
        Assert.assertArrayEquals(new Object[]{8}, policyManager.validate("jdoe", "1234567").getParameters());
        Assert.assertNull(policyManager.validate("jdoe", "12345678"));

        realmModel.setPasswordPolicy(PasswordPolicy.parse(session, "length(4)"));

        Assert.assertEquals("invalidPasswordMinLengthMessage", policyManager.validate("jdoe", "123").getMessage());
        Assert.assertArrayEquals(new Object[]{4}, policyManager.validate("jdoe", "123").getParameters());
        Assert.assertNull(policyManager.validate("jdoe", "1234"));
    }

    @Test
    public void testDigits() {
        realmModel.setPasswordPolicy(PasswordPolicy.parse(session, "digits"));
        Assert.assertEquals("invalidPasswordMinDigitsMessage", policyManager.validate("jdoe", "abcd").getMessage());
        Assert.assertArrayEquals(new Object[]{1}, policyManager.validate("jdoe", "abcd").getParameters());
        Assert.assertNull(policyManager.validate("jdoe", "abcd1"));

        realmModel.setPasswordPolicy(PasswordPolicy.parse(session, "digits(2)"));
        Assert.assertEquals("invalidPasswordMinDigitsMessage", policyManager.validate("jdoe", "abcd1").getMessage());
        Assert.assertArrayEquals(new Object[]{2}, policyManager.validate("jdoe", "abcd1").getParameters());
        Assert.assertNull(policyManager.validate("jdoe", "abcd12"));
    }

    @Test
    public void testLowerCase() {
        realmModel.setPasswordPolicy(PasswordPolicy.parse(session, "lowerCase"));
        Assert.assertEquals("invalidPasswordMinLowerCaseCharsMessage", policyManager.validate("jdoe", "ABCD1234").getMessage());
        Assert.assertArrayEquals(new Object[]{1}, policyManager.validate("jdoe", "ABCD1234").getParameters());
        Assert.assertNull(policyManager.validate("jdoe", "ABcD1234"));

        realmModel.setPasswordPolicy(PasswordPolicy.parse(session, "lowerCase(2)"));
        Assert.assertEquals("invalidPasswordMinLowerCaseCharsMessage", policyManager.validate("jdoe", "ABcD1234").getMessage());
        Assert.assertArrayEquals(new Object[]{2}, policyManager.validate("jdoe", "ABcD1234").getParameters());
        Assert.assertNull(policyManager.validate("jdoe", "aBcD1234"));
    }

    @Test
    public void testUpperCase() {
        realmModel.setPasswordPolicy(PasswordPolicy.parse(session, "upperCase"));
        Assert.assertEquals("invalidPasswordMinUpperCaseCharsMessage", policyManager.validate("jdoe", "abcd1234").getMessage());
        Assert.assertArrayEquals(new Object[]{1}, policyManager.validate("jdoe", "abcd1234").getParameters());
        Assert.assertNull(policyManager.validate("jdoe", "abCd1234"));

        realmModel.setPasswordPolicy(PasswordPolicy.parse(session, "upperCase(2)"));
        Assert.assertEquals("invalidPasswordMinUpperCaseCharsMessage", policyManager.validate("jdoe", "abCd1234").getMessage());
        Assert.assertArrayEquals(new Object[]{2}, policyManager.validate("jdoe", "abCd1234").getParameters());
        Assert.assertNull(policyManager.validate("jdoe", "AbCd1234"));
    }

    @Test
    public void testSpecialChars() {
        realmModel.setPasswordPolicy(PasswordPolicy.parse(session, "specialChars"));
        Assert.assertEquals("invalidPasswordMinSpecialCharsMessage", policyManager.validate("jdoe", "abcd1234").getMessage());
        Assert.assertArrayEquals(new Object[]{1}, policyManager.validate("jdoe", "abcd1234").getParameters());
        Assert.assertNull(policyManager.validate("jdoe", "ab&d1234"));

        realmModel.setPasswordPolicy(PasswordPolicy.parse(session, "specialChars(2)"));
        Assert.assertEquals("invalidPasswordMinSpecialCharsMessage", policyManager.validate("jdoe", "ab&d1234").getMessage());
        Assert.assertArrayEquals(new Object[]{2}, policyManager.validate("jdoe", "ab&d1234").getParameters());
        Assert.assertNull(policyManager.validate("jdoe", "ab&d-234"));
    }

    @Test
    public void testNotUsername() {
        realmModel.setPasswordPolicy(PasswordPolicy.parse(session, "notUsername"));
        Assert.assertEquals("invalidPasswordNotUsernameMessage", policyManager.validate("jdoe", "jdoe").getMessage());
        Assert.assertNull(policyManager.validate("jdoe", "ab&d1234"));
    }

    @Test
    public void testInvalidPolicyName() {
        try {
            realmModel.setPasswordPolicy(PasswordPolicy.parse(session, "noSuchPolicy"));
            Assert.fail("Expected exception");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void testRegexPatterns() {
        PasswordPolicy policy = null;
        try {
            realmModel.setPasswordPolicy(PasswordPolicy.parse(session, "regexPattern"));
            fail("Expected NullPointerException: Regex Pattern cannot be null.");
        } catch (IllegalArgumentException e) {
            // Expected NPE as regex pattern is null.
        }

        try {
            realmModel.setPasswordPolicy(PasswordPolicy.parse(session, "regexPattern(*)"));
            fail("Expected PatternSyntaxException: Regex Pattern cannot be null.");
        } catch (PatternSyntaxException e) {
            // Expected PSE as regex pattern(or any of its token) is not quantifiable.
        }

        try {
            realmModel.setPasswordPolicy(PasswordPolicy.parse(session, "regexPattern(*,**)"));
            fail("Expected PatternSyntaxException: Regex Pattern cannot be null.");
        } catch (PatternSyntaxException e) {
            // Expected PSE as regex pattern(or any of its token) is not quantifiable.
        }

        //Fails to match one of the regex pattern
        realmModel.setPasswordPolicy(PasswordPolicy.parse(session, "regexPattern(jdoe) and regexPattern(j*d)"));
        Assert.assertEquals("invalidPasswordRegexPatternMessage", policyManager.validate("jdoe", "jdoe").getMessage());

        ////Fails to match all of the regex patterns
        realmModel.setPasswordPolicy(PasswordPolicy.parse(session, "regexPattern(j*p) and regexPattern(j*d) and regexPattern(adoe)"));
        Assert.assertEquals("invalidPasswordRegexPatternMessage", policyManager.validate("jdoe", "jdoe").getMessage());

        realmModel.setPasswordPolicy(PasswordPolicy.parse(session, "regexPattern([a-z][a-z][a-z][a-z][0-9])"));
        Assert.assertEquals("invalidPasswordRegexPatternMessage", policyManager.validate("jdoe", "jdoe").getMessage());

        realmModel.setPasswordPolicy(PasswordPolicy.parse(session, "regexPattern(jdoe)"));
        Assert.assertNull(policyManager.validate("jdoe", "jdoe"));

        realmModel.setPasswordPolicy(PasswordPolicy.parse(session, "regexPattern([a-z][a-z][a-z][a-z][0-9])"));
        Assert.assertNull(policyManager.validate("jdoe", "jdoe0"));
    }

    @Test
    public void testComplex() {
        realmModel.setPasswordPolicy(PasswordPolicy.parse(session, "length(8) and digits(2) and lowerCase(2) and upperCase(2) and specialChars(2) and notUsername()"));
        Assert.assertNotNull(policyManager.validate("jdoe", "12aaBB&"));
        Assert.assertNotNull(policyManager.validate("jdoe", "aaaaBB&-"));
        Assert.assertNotNull(policyManager.validate("jdoe", "12AABB&-"));
        Assert.assertNotNull(policyManager.validate("jdoe", "12aabb&-"));
        Assert.assertNotNull(policyManager.validate("jdoe", "12aaBBcc"));
        Assert.assertNotNull(policyManager.validate("12aaBB&-", "12aaBB&-"));

        Assert.assertNull(policyManager.validate("jdoe", "12aaBB&-"));
    }

}

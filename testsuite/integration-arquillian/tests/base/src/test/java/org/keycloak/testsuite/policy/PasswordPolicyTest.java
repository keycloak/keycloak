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

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.models.ModelException;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.policy.BlacklistPasswordPolicyProvider;
import org.keycloak.policy.MaximumLengthPasswordPolicyProviderFactory;
import org.keycloak.policy.PasswordPolicyManagerProvider;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;
import org.keycloak.testsuite.util.ContainerAssume;
import org.keycloak.testsuite.util.RealmBuilder;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@AuthServerContainerExclude(AuthServer.REMOTE)
public class PasswordPolicyTest extends AbstractKeycloakTest {

    @Test
    public void testLength() {
        testingClient.server("passwordPolicy").run(session -> {
            RealmModel realmModel = session.getContext().getRealm();
            PasswordPolicyManagerProvider policyManager = session.getProvider(PasswordPolicyManagerProvider.class);

            realmModel.setPasswordPolicy(PasswordPolicy.parse(session, "length"));

            Assert.assertEquals("invalidPasswordMinLengthMessage", policyManager.validate("jdoe", "1234567").getMessage());
            Assert.assertArrayEquals(new Object[]{8}, policyManager.validate("jdoe", "1234567").getParameters());
            assertNull(policyManager.validate("jdoe", "12345678"));

            realmModel.setPasswordPolicy(PasswordPolicy.parse(session, "length(4)"));

            Assert.assertEquals("invalidPasswordMinLengthMessage", policyManager.validate("jdoe", "123").getMessage());
            Assert.assertArrayEquals(new Object[]{4}, policyManager.validate("jdoe", "123").getParameters());
            assertNull(policyManager.validate("jdoe", "1234"));
        });
    }

    @Test
    public void testMaximumLength() {
        testingClient.server("passwordPolicy").run(session -> {
            RealmModel realmModel = session.getContext().getRealm();
            PasswordPolicyManagerProvider policyManager = session.getProvider(PasswordPolicyManagerProvider.class);

            realmModel.setPasswordPolicy(PasswordPolicy.parse(session, "maxLength"));

            Assert.assertEquals("invalidPasswordMaxLengthMessage",
                    policyManager.validate("jdoe", "12345678901234567890123456789012345678901234567890123456789012345").getMessage());
            Assert.assertArrayEquals(new Object[]{MaximumLengthPasswordPolicyProviderFactory.DEFAULT_MAX_LENGTH},
                    policyManager.validate("jdoe", "12345678901234567890123456789012345678901234567890123456789012345").getParameters());
            assertNull(policyManager.validate("jdoe", "1234567890123456789012345678901234567890123456789012345678901234"));

            realmModel.setPasswordPolicy(PasswordPolicy.parse(session, "maxLength(24)"));

            Assert.assertEquals("invalidPasswordMaxLengthMessage",
                    policyManager.validate("jdoe", "1234567890123456789012345").getMessage());
            Assert.assertArrayEquals(new Object[]{24},
                    policyManager.validate("jdoe", "1234567890123456789012345").getParameters());
            assertNull(policyManager.validate("jdoe", "123456789012345678901234"));
        });
    }

    @Test
    public void testDigits() {
        testingClient.server("passwordPolicy").run(session -> {
            RealmModel realmModel = session.getContext().getRealm();
            PasswordPolicyManagerProvider policyManager = session.getProvider(PasswordPolicyManagerProvider.class);

            realmModel.setPasswordPolicy(PasswordPolicy.parse(session, "digits"));
            Assert.assertEquals("invalidPasswordMinDigitsMessage", policyManager.validate("jdoe", "abcd").getMessage());
            Assert.assertArrayEquals(new Object[]{1}, policyManager.validate("jdoe", "abcd").getParameters());
            assertNull(policyManager.validate("jdoe", "abcd1"));

            realmModel.setPasswordPolicy(PasswordPolicy.parse(session, "digits(2)"));
            Assert.assertEquals("invalidPasswordMinDigitsMessage", policyManager.validate("jdoe", "abcd1").getMessage());
            Assert.assertArrayEquals(new Object[]{2}, policyManager.validate("jdoe", "abcd1").getParameters());
            assertNull(policyManager.validate("jdoe", "abcd12"));
        });
    }

    @Test
    public void testLowerCase() {
        testingClient.server("passwordPolicy").run(session -> {
            RealmModel realmModel = session.getContext().getRealm();
            PasswordPolicyManagerProvider policyManager = session.getProvider(PasswordPolicyManagerProvider.class);

            realmModel.setPasswordPolicy(PasswordPolicy.parse(session, "lowerCase"));
            Assert.assertEquals("invalidPasswordMinLowerCaseCharsMessage", policyManager.validate("jdoe", "ABCD1234").getMessage());
            Assert.assertArrayEquals(new Object[]{1}, policyManager.validate("jdoe", "ABCD1234").getParameters());
            assertNull(policyManager.validate("jdoe", "ABcD1234"));

            realmModel.setPasswordPolicy(PasswordPolicy.parse(session, "lowerCase(2)"));
            Assert.assertEquals("invalidPasswordMinLowerCaseCharsMessage", policyManager.validate("jdoe", "ABcD1234").getMessage());
            Assert.assertArrayEquals(new Object[]{2}, policyManager.validate("jdoe", "ABcD1234").getParameters());
            assertNull(policyManager.validate("jdoe", "aBcD1234"));
        });
    }

    @Test
    public void testUpperCase() {
        testingClient.server("passwordPolicy").run(session -> {
            RealmModel realmModel = session.getContext().getRealm();
            PasswordPolicyManagerProvider policyManager = session.getProvider(PasswordPolicyManagerProvider.class);

            realmModel.setPasswordPolicy(PasswordPolicy.parse(session, "upperCase"));
            Assert.assertEquals("invalidPasswordMinUpperCaseCharsMessage", policyManager.validate("jdoe", "abcd1234").getMessage());
            Assert.assertArrayEquals(new Object[]{1}, policyManager.validate("jdoe", "abcd1234").getParameters());
            assertNull(policyManager.validate("jdoe", "abCd1234"));

            realmModel.setPasswordPolicy(PasswordPolicy.parse(session, "upperCase(2)"));
            Assert.assertEquals("invalidPasswordMinUpperCaseCharsMessage", policyManager.validate("jdoe", "abCd1234").getMessage());
            Assert.assertArrayEquals(new Object[]{2}, policyManager.validate("jdoe", "abCd1234").getParameters());
            assertNull(policyManager.validate("jdoe", "AbCd1234"));
        });
    }

    @Test
    public void testSpecialChars() {
        testingClient.server("passwordPolicy").run(session -> {
            RealmModel realmModel = session.getContext().getRealm();
            PasswordPolicyManagerProvider policyManager = session.getProvider(PasswordPolicyManagerProvider.class);

            realmModel.setPasswordPolicy(PasswordPolicy.parse(session, "specialChars"));
            Assert.assertEquals("invalidPasswordMinSpecialCharsMessage", policyManager.validate("jdoe", "abcd1234").getMessage());
            Assert.assertArrayEquals(new Object[]{1}, policyManager.validate("jdoe", "abcd1234").getParameters());
            assertNull(policyManager.validate("jdoe", "ab&d1234"));

            realmModel.setPasswordPolicy(PasswordPolicy.parse(session, "specialChars(2)"));
            Assert.assertEquals("invalidPasswordMinSpecialCharsMessage", policyManager.validate("jdoe", "ab&d1234").getMessage());
            Assert.assertArrayEquals(new Object[]{2}, policyManager.validate("jdoe", "ab&d1234").getParameters());
            assertNull(policyManager.validate("jdoe", "ab&d-234"));
        });
    }

    /**
     * KEYCLOAK-5244
     */
    @Test
    public void testBlacklistPasswordPolicyWithTestBlacklist() throws Exception {

        ContainerAssume.assumeNotAuthServerRemote();

        testingClient.server("passwordPolicy").run(session -> {

            RealmModel realmModel = session.getContext().getRealm();
            PasswordPolicyManagerProvider policyManager = session.getProvider(PasswordPolicyManagerProvider.class);

            realmModel.setPasswordPolicy(PasswordPolicy.parse(session, "passwordBlacklist(test-password-blacklist.txt)"));

            Assert.assertEquals(BlacklistPasswordPolicyProvider.ERROR_MESSAGE, policyManager.validate("jdoe", "blacklisted1").getMessage());
            Assert.assertEquals(BlacklistPasswordPolicyProvider.ERROR_MESSAGE, policyManager.validate("jdoe", "blacklisted2").getMessage());
            Assert.assertEquals(BlacklistPasswordPolicyProvider.ERROR_MESSAGE, policyManager.validate("jdoe", "bLaCkLiSteD2").getMessage());
            assertNull(policyManager.validate("jdoe", "notblacklisted"));
        });
    }

    @Test
    public void testNotUsername() {
        testingClient.server("passwordPolicy").run(session -> {
            RealmModel realmModel = session.getContext().getRealm();
            PasswordPolicyManagerProvider policyManager = session.getProvider(PasswordPolicyManagerProvider.class);

            realmModel.setPasswordPolicy(PasswordPolicy.parse(session, "notUsername"));
            Assert.assertEquals("invalidPasswordNotUsernameMessage", policyManager.validate("jdoe", "jdoe").getMessage());
            assertNull(policyManager.validate("jdoe", "ab&d1234"));
        });
    }

    @Test
    public void testInvalidPolicyName() {
        testingClient.server("passwordPolicy").run(session -> {
            RealmModel realmModel = session.getContext().getRealm();
            PasswordPolicyManagerProvider policyManager = session.getProvider(PasswordPolicyManagerProvider.class);

            try {
                realmModel.setPasswordPolicy(PasswordPolicy.parse(session, "noSuchPolicy"));
                Assert.fail("Expected exception");
            } catch (ModelException e) {
                assertEquals("Password policy not found", e.getMessage());
            }
        });
    }

    @Test
    public void testRegexPatterns() {
        testingClient.server("passwordPolicy").run(session -> {
            RealmModel realmModel = session.getContext().getRealm();
            PasswordPolicyManagerProvider policyManager = session.getProvider(PasswordPolicyManagerProvider.class);

            PasswordPolicy policy = null;
            try {
                realmModel.setPasswordPolicy(PasswordPolicy.parse(session, "regexPattern"));
                fail("Expected NullPointerException: Regex Pattern cannot be null.");
            } catch (ModelException e) {
                assertEquals("Invalid config for regexPattern: Config required", e.getMessage());
            }

            try {
                realmModel.setPasswordPolicy(PasswordPolicy.parse(session, "regexPattern(*)"));
                fail("Expected PatternSyntaxException: Regex Pattern cannot be null.");
            } catch (ModelException e) {
                assertEquals("Invalid config for regexPattern: Not a valid regular expression", e.getMessage());
            }

            try {
                realmModel.setPasswordPolicy(PasswordPolicy.parse(session, "regexPattern(*,**)"));
                fail("Expected PatternSyntaxException: Regex Pattern cannot be null.");
            } catch (ModelException e) {
                assertEquals("Invalid config for regexPattern: Not a valid regular expression", e.getMessage());
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
            assertNull(policyManager.validate("jdoe", "jdoe"));

            realmModel.setPasswordPolicy(PasswordPolicy.parse(session, "regexPattern([a-z][a-z][a-z][a-z][0-9])"));
            assertNull(policyManager.validate("jdoe", "jdoe0"));
        });
    }

    @Test
    public void testComplex() {
        testingClient.server("passwordPolicy").run(session -> {
            RealmModel realmModel = session.getContext().getRealm();
            PasswordPolicyManagerProvider policyManager = session.getProvider(PasswordPolicyManagerProvider.class);

            realmModel.setPasswordPolicy(PasswordPolicy.parse(session, "length(8) and maxLength(32) and digits(2) and lowerCase(2) and upperCase(2) and specialChars(2) and notUsername()"));
            Assert.assertNotNull(policyManager.validate("jdoe", "12aaBB&"));
            Assert.assertNotNull(policyManager.validate("jdoe", "aaaaBB&-"));
            Assert.assertNotNull(policyManager.validate("jdoe", "12AABB&-"));
            Assert.assertNotNull(policyManager.validate("jdoe", "12aabb&-"));
            Assert.assertNotNull(policyManager.validate("jdoe", "12aaBBcc"));
            Assert.assertNotNull(policyManager.validate("12aaBB&-", "12aaBB&-"));
            Assert.assertNotNull(policyManager.validate("jdoe", "12aaBB&-12aaBB&-12aaBB&-12aaBB&-1"));

            assertNull(policyManager.validate("jdoe", "12aaBB&-"));
        });
    }

    @Test
    public void testBuilder() {
        testingClient.server("passwordPolicy").run(session -> {
            PasswordPolicy.Builder builder = PasswordPolicy.parse(session, "hashIterations(20000)").toBuilder();
            assertFalse(builder.contains(PasswordPolicy.HASH_ALGORITHM_ID));
            assertTrue("20000".equals(builder.get(PasswordPolicy.HASH_ITERATIONS_ID)));

            builder.remove(PasswordPolicy.HASH_ITERATIONS_ID);

            assertNull(builder.asString());

            builder = PasswordPolicy.parse(session, "hashIterations(20000) and hashAlgorithm(pbkdf2)").toBuilder();
            assertTrue(builder.contains(PasswordPolicy.HASH_ALGORITHM_ID));

            builder = PasswordPolicy.parse(session, "hashIterations(20000) and length(100)").toBuilder();
            builder.remove(PasswordPolicy.HASH_ITERATIONS_ID);
            assertEquals("length(100)", builder.asString());

            builder = PasswordPolicy.parse(session, "digits(10) and hashIterations(20000) and length(100)").toBuilder();
            builder.remove(PasswordPolicy.HASH_ITERATIONS_ID);
            assertEquals("digits(10) and length(100)", builder.asString());
        });
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        testRealms.add(RealmBuilder.create().name("passwordPolicy").build());
    }

}

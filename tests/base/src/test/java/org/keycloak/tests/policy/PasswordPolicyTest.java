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

package org.keycloak.tests.policy;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;

import org.keycloak.models.ModelException;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.policy.DenylistPasswordPolicyProvider;
import org.keycloak.policy.DenylistPasswordPolicyProviderFactory;
import org.keycloak.policy.MaximumLengthPasswordPolicyProviderFactory;
import org.keycloak.policy.PasswordPolicyManagerProvider;
import org.keycloak.policy.PasswordPolicyProvider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.tests.suites.DatabaseTest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@KeycloakIntegrationTest(config = PasswordPolicyTest.PasswordPolicyServerConfig.class)
@DatabaseTest
public class PasswordPolicyTest {

    @InjectRealm(config = PasswordPolicyRealmConfig.class)
    ManagedRealm managedRealm;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @Test
    public void testLength() {
        runOnServer.run(session -> {
            RealmModel realmModel = session.getContext().getRealm();
            PasswordPolicyManagerProvider policyManager = session.getProvider(PasswordPolicyManagerProvider.class);

            realmModel.setPasswordPolicy(PasswordPolicy.parse(session, "length"));

            Assertions.assertEquals("invalidPasswordMinLengthMessage", policyManager.validate("jdoe", "1234567").getMessage());
            Assertions.assertArrayEquals(new Object[]{8}, policyManager.validate("jdoe", "1234567").getParameters());
            assertNull(policyManager.validate("jdoe", "12345678"));

            realmModel.setPasswordPolicy(PasswordPolicy.parse(session, "length(4)"));

            Assertions.assertEquals("invalidPasswordMinLengthMessage", policyManager.validate("jdoe", "123").getMessage());
            Assertions.assertArrayEquals(new Object[]{4}, policyManager.validate("jdoe", "123").getParameters());
            assertNull(policyManager.validate("jdoe", "1234"));
        });
    }

    @Test
    public void testMaximumLength() {
        runOnServer.run(session -> {
            RealmModel realmModel = session.getContext().getRealm();
            PasswordPolicyManagerProvider policyManager = session.getProvider(PasswordPolicyManagerProvider.class);

            realmModel.setPasswordPolicy(PasswordPolicy.parse(session, "maxLength"));

            Assertions.assertEquals("invalidPasswordMaxLengthMessage",
                    policyManager.validate("jdoe", "12345678901234567890123456789012345678901234567890123456789012345").getMessage());
            Assertions.assertArrayEquals(new Object[]{MaximumLengthPasswordPolicyProviderFactory.DEFAULT_MAX_LENGTH},
                    policyManager.validate("jdoe", "12345678901234567890123456789012345678901234567890123456789012345").getParameters());
            assertNull(policyManager.validate("jdoe", "1234567890123456789012345678901234567890123456789012345678901234"));

            realmModel.setPasswordPolicy(PasswordPolicy.parse(session, "maxLength(24)"));

            Assertions.assertEquals("invalidPasswordMaxLengthMessage",
                    policyManager.validate("jdoe", "1234567890123456789012345").getMessage());
            Assertions.assertArrayEquals(new Object[]{24},
                    policyManager.validate("jdoe", "1234567890123456789012345").getParameters());
            assertNull(policyManager.validate("jdoe", "123456789012345678901234"));
        });
    }

    @Test
    public void testDigits() {
        runOnServer.run(session -> {
            RealmModel realmModel = session.getContext().getRealm();
            PasswordPolicyManagerProvider policyManager = session.getProvider(PasswordPolicyManagerProvider.class);

            realmModel.setPasswordPolicy(PasswordPolicy.parse(session, "digits"));
            Assertions.assertEquals("invalidPasswordMinDigitsMessage", policyManager.validate("jdoe", "abcd").getMessage());
            Assertions.assertArrayEquals(new Object[]{1}, policyManager.validate("jdoe", "abcd").getParameters());
            assertNull(policyManager.validate("jdoe", "abcd1"));

            realmModel.setPasswordPolicy(PasswordPolicy.parse(session, "digits(2)"));
            Assertions.assertEquals("invalidPasswordMinDigitsMessage", policyManager.validate("jdoe", "abcd1").getMessage());
            Assertions.assertArrayEquals(new Object[]{2}, policyManager.validate("jdoe", "abcd1").getParameters());
            assertNull(policyManager.validate("jdoe", "abcd12"));
        });
    }

    @Test
    public void testLowerCase() {
        runOnServer.run(session -> {
            RealmModel realmModel = session.getContext().getRealm();
            PasswordPolicyManagerProvider policyManager = session.getProvider(PasswordPolicyManagerProvider.class);

            realmModel.setPasswordPolicy(PasswordPolicy.parse(session, "lowerCase"));
            Assertions.assertEquals("invalidPasswordMinLowerCaseCharsMessage", policyManager.validate("jdoe", "ABCD1234").getMessage());
            Assertions.assertArrayEquals(new Object[]{1}, policyManager.validate("jdoe", "ABCD1234").getParameters());
            assertNull(policyManager.validate("jdoe", "ABcD1234"));

            realmModel.setPasswordPolicy(PasswordPolicy.parse(session, "lowerCase(2)"));
            Assertions.assertEquals("invalidPasswordMinLowerCaseCharsMessage", policyManager.validate("jdoe", "ABcD1234").getMessage());
            Assertions.assertArrayEquals(new Object[]{2}, policyManager.validate("jdoe", "ABcD1234").getParameters());
            assertNull(policyManager.validate("jdoe", "aBcD1234"));
        });
    }

    @Test
    public void testUpperCase() {
        runOnServer.run(session -> {
            RealmModel realmModel = session.getContext().getRealm();
            PasswordPolicyManagerProvider policyManager = session.getProvider(PasswordPolicyManagerProvider.class);

            realmModel.setPasswordPolicy(PasswordPolicy.parse(session, "upperCase"));
            Assertions.assertEquals("invalidPasswordMinUpperCaseCharsMessage", policyManager.validate("jdoe", "abcd1234").getMessage());
            Assertions.assertArrayEquals(new Object[]{1}, policyManager.validate("jdoe", "abcd1234").getParameters());
            assertNull(policyManager.validate("jdoe", "abCd1234"));

            realmModel.setPasswordPolicy(PasswordPolicy.parse(session, "upperCase(2)"));
            Assertions.assertEquals("invalidPasswordMinUpperCaseCharsMessage", policyManager.validate("jdoe", "abCd1234").getMessage());
            Assertions.assertArrayEquals(new Object[]{2}, policyManager.validate("jdoe", "abCd1234").getParameters());
            assertNull(policyManager.validate("jdoe", "AbCd1234"));
        });
    }

    @Test
    public void testSpecialChars() {
        runOnServer.run(session -> {
            RealmModel realmModel = session.getContext().getRealm();
            PasswordPolicyManagerProvider policyManager = session.getProvider(PasswordPolicyManagerProvider.class);

            realmModel.setPasswordPolicy(PasswordPolicy.parse(session, "specialChars"));
            Assertions.assertEquals("invalidPasswordMinSpecialCharsMessage", policyManager.validate("jdoe", "abcd1234").getMessage());
            Assertions.assertArrayEquals(new Object[]{1}, policyManager.validate("jdoe", "abcd1234").getParameters());
            assertNull(policyManager.validate("jdoe", "ab&d1234"));

            realmModel.setPasswordPolicy(PasswordPolicy.parse(session, "specialChars(2)"));
            Assertions.assertEquals("invalidPasswordMinSpecialCharsMessage", policyManager.validate("jdoe", "ab&d1234").getMessage());
            Assertions.assertArrayEquals(new Object[]{2}, policyManager.validate("jdoe", "ab&d1234").getParameters());
            assertNull(policyManager.validate("jdoe", "ab&d-234"));
        });
    }

    /**
     * KEYCLOAK-5244
     */
    @Test
    public void testDenylistPasswordPolicyWithTestDenylist() {
        runOnServer.run(session -> {

            RealmModel realmModel = session.getContext().getRealm();
            PasswordPolicyManagerProvider policyManager = session.getProvider(PasswordPolicyManagerProvider.class);

            realmModel.setPasswordPolicy(PasswordPolicy.parse(session, "passwordBlacklist(test-password-blacklist.txt)"));

            Assertions.assertEquals(DenylistPasswordPolicyProvider.ERROR_MESSAGE, policyManager.validate("jdoe", "blacklisted1").getMessage());
            Assertions.assertEquals(DenylistPasswordPolicyProvider.ERROR_MESSAGE, policyManager.validate("jdoe", "blacklisted2").getMessage());
            Assertions.assertEquals(DenylistPasswordPolicyProvider.ERROR_MESSAGE, policyManager.validate("jdoe", "bLaCkLiSteD2").getMessage());
            assertNull(policyManager.validate("jdoe", "notblacklisted"));
        });
    }

    @Test
    public void testDenylistPasswordPolicyDefaultPath() {
        final String SEPARATOR = File.separator;

        runOnServer.run(session -> {
            ProviderFactory<PasswordPolicyProvider> passPolicyFact = session.getKeycloakSessionFactory().getProviderFactory(
                    PasswordPolicyProvider.class, DenylistPasswordPolicyProviderFactory.ID);
            assertThat(passPolicyFact, instanceOf(DenylistPasswordPolicyProviderFactory.class));
            assertThat(((DenylistPasswordPolicyProviderFactory) passPolicyFact).getDefaultDenylistsBasePath(),
                    endsWith(SEPARATOR + "data" + SEPARATOR + "password-blacklists" + SEPARATOR));
        });
    }

    @Test
    public void testNotUsername() {
        runOnServer.run(session -> {
            RealmModel realmModel = session.getContext().getRealm();
            PasswordPolicyManagerProvider policyManager = session.getProvider(PasswordPolicyManagerProvider.class);

            realmModel.setPasswordPolicy(PasswordPolicy.parse(session, "notUsername"));
            Assertions.assertEquals("invalidPasswordNotUsernameMessage", policyManager.validate("jdoe", "jdoe").getMessage());
            assertNull(policyManager.validate("jdoe", "ab&d1234"));
        });
    }

    @Test
    public void testInvalidPolicyName() {
        runOnServer.run(session -> {
            RealmModel realmModel = session.getContext().getRealm();

            try {
                realmModel.setPasswordPolicy(PasswordPolicy.parse(session, "noSuchPolicy"));
                Assertions.fail("Expected exception");
            } catch (ModelException e) {
                assertEquals("Password policy not found", e.getMessage());
            }
        });
    }

    @Test
    public void testRegexPatterns() {
        runOnServer.run(session -> {
            RealmModel realmModel = session.getContext().getRealm();
            PasswordPolicyManagerProvider policyManager = session.getProvider(PasswordPolicyManagerProvider.class);

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
            Assertions.assertEquals("invalidPasswordRegexPatternMessage", policyManager.validate("jdoe", "jdoe").getMessage());

            //Fails to match all of the regex patterns
            realmModel.setPasswordPolicy(PasswordPolicy.parse(session, "regexPattern(j*p) and regexPattern(j*d) and regexPattern(adoe)"));
            Assertions.assertEquals("invalidPasswordRegexPatternMessage", policyManager.validate("jdoe", "jdoe").getMessage());

            realmModel.setPasswordPolicy(PasswordPolicy.parse(session, "regexPattern([a-z][a-z][a-z][a-z][0-9])"));
            Assertions.assertEquals("invalidPasswordRegexPatternMessage", policyManager.validate("jdoe", "jdoe").getMessage());

            realmModel.setPasswordPolicy(PasswordPolicy.parse(session, "regexPattern(jdoe)"));
            assertNull(policyManager.validate("jdoe", "jdoe"));

            realmModel.setPasswordPolicy(PasswordPolicy.parse(session, "regexPattern([a-z][a-z][a-z][a-z][0-9])"));
            assertNull(policyManager.validate("jdoe", "jdoe0"));
        });
    }

    @Test
    public void testComplex() {
        runOnServer.run(session -> {
            RealmModel realmModel = session.getContext().getRealm();
            PasswordPolicyManagerProvider policyManager = session.getProvider(PasswordPolicyManagerProvider.class);

            realmModel.setPasswordPolicy(PasswordPolicy.parse(session, "length(8) and maxLength(32) and digits(2) and lowerCase(2) and upperCase(2) and specialChars(2) and notUsername()"));
            Assertions.assertNotNull(policyManager.validate("jdoe", "12aaBB&"));
            Assertions.assertNotNull(policyManager.validate("jdoe", "aaaaBB&-"));
            Assertions.assertNotNull(policyManager.validate("jdoe", "12AABB&-"));
            Assertions.assertNotNull(policyManager.validate("jdoe", "12aabb&-"));
            Assertions.assertNotNull(policyManager.validate("jdoe", "12aaBBcc"));
            Assertions.assertNotNull(policyManager.validate("12aaBB&-", "12aaBB&-"));
            Assertions.assertNotNull(policyManager.validate("jdoe", "12aaBB&-12aaBB&-12aaBB&-12aaBB&-1"));

            assertNull(policyManager.validate("jdoe", "12aaBB&-"));
        });
    }

    @Test
    public void testBuilder() {
        runOnServer.run(session -> {
            PasswordPolicy.Builder builder = PasswordPolicy.parse(session, "hashIterations(20000)").toBuilder();
            assertFalse(builder.contains(PasswordPolicy.HASH_ALGORITHM_ID));
            assertEquals("20000", builder.get(PasswordPolicy.HASH_ITERATIONS_ID));

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

    public static class PasswordPolicyServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            try {
                URL resourceUrl = PasswordPolicyTest.class.getResource("/password-blacklists");
                if (resourceUrl == null) {
                    throw new RuntimeException("Unable to find the password-blacklists file in the classpath for PasswordPolicyTest");
                }
                String resourcePath = Paths.get(resourceUrl.toURI()).toString();
                return config.spiOption("password-policy", "password-blacklist", "blacklists-path", resourcePath);

            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class PasswordPolicyRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.name("passwordPolicy");
        }
    }
}

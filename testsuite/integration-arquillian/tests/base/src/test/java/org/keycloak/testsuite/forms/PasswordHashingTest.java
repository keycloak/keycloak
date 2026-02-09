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
package org.keycloak.testsuite.forms;

import java.nio.charset.StandardCharsets;
import java.security.spec.KeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import jakarta.persistence.EntityManager;
import jakarta.ws.rs.BadRequestException;

import org.keycloak.common.crypto.FipsMode;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.hash.PasswordHashProvider;
import org.keycloak.credential.hash.PasswordHashProviderFactory;
import org.keycloak.credential.hash.Pbkdf2PasswordHashProvider;
import org.keycloak.credential.hash.Pbkdf2PasswordHashProviderFactory;
import org.keycloak.credential.hash.Pbkdf2Sha256PasswordHashProviderFactory;
import org.keycloak.credential.hash.Pbkdf2Sha512PasswordHashProviderFactory;
import org.keycloak.crypto.hash.Argon2Parameters;
import org.keycloak.crypto.hash.Argon2PasswordHashProviderFactory;
import org.keycloak.exportimport.util.ExportUtils;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.cache.UserCache;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.credential.dto.PasswordCredentialData;
import org.keycloak.models.jpa.entities.CredentialEntity;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.AuthServerTestEnricher;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.DefaultPasswordHash;
import org.keycloak.testsuite.util.UserBuilder;

import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class PasswordHashingTest extends AbstractTestRealmKeycloakTest {

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }

    @Page
    protected LoginPage loginPage;

    @Page
    protected AppPage appPage;

    @Test
    public void testSetInvalidProvider() {
        try {
            setPasswordPolicy("hashAlgorithm(nosuch)");
            fail("Expected error");
        } catch (BadRequestException e) {
            ErrorRepresentation error = e.getResponse().readEntity(ErrorRepresentation.class);
            assertEquals("Invalid config for hashAlgorithm: Password hashing provider not found", error.getErrorMessage());
        }
    }

    @Test
    public void testPasswordRehashedOnAlgorithmChanged() throws Exception {
        setPasswordPolicy("hashAlgorithm(" + Pbkdf2Sha256PasswordHashProviderFactory.ID + ") and hashIterations(1)");

        String username = "testPasswordRehashedOnAlgorithmChanged";
        final String password = createUser(username);

        PasswordCredentialModel credential = PasswordCredentialModel.createFromCredentialModel(fetchCredentials(username));

        assertEquals(Pbkdf2Sha256PasswordHashProviderFactory.ID, credential.getPasswordCredentialData().getAlgorithm());

        assertEncoded(credential, password, credential.getPasswordSecretData().getSalt(), "PBKDF2WithHmacSHA256", 1);

        setPasswordPolicy("hashAlgorithm(" + Pbkdf2PasswordHashProviderFactory.ID + ") and hashIterations(1)");

        loginPage.open();
        loginPage.login(username, password);
        appPage.assertCurrent();

        credential = PasswordCredentialModel.createFromCredentialModel(fetchCredentials(username));

        assertEquals(Pbkdf2PasswordHashProviderFactory.ID, credential.getPasswordCredentialData().getAlgorithm());
        assertEncoded(credential, password, credential.getPasswordSecretData().getSalt(), "PBKDF2WithHmacSHA1", 1);
    }

    @Test
    public void testPasswordRehashedOnAlgorithmChangedWithMigratedSalt() throws Exception {
        setPasswordPolicy("hashAlgorithm(" + Pbkdf2Sha256PasswordHashProviderFactory.ID + ") and hashIterations(1)");

        String username = "testPasswordRehashedOnAlgorithmChangedWithMigratedSalt";
        final String password = createUser(username);

        PasswordCredentialModel credential = PasswordCredentialModel.createFromCredentialModel(fetchCredentials(username));

        assertEquals(Pbkdf2Sha256PasswordHashProviderFactory.ID, credential.getPasswordCredentialData().getAlgorithm());

        assertEncoded(credential, password, credential.getPasswordSecretData().getSalt(), "PBKDF2WithHmacSHA256", 1);

        setPasswordPolicy("hashAlgorithm(" + Pbkdf2PasswordHashProviderFactory.ID + ") and hashIterations(1)");

        String credentialId = credential.getId();
        testingClient.server().run(session -> {
            EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
            CredentialEntity credentialEntity = em.find(CredentialEntity.class, credentialId);
            // adding a dummy value to the salt column to trigger migration in JpaUserCredentialStore#toModel on next fetch of the credential
            credentialEntity.setSalt("dummy".getBytes(StandardCharsets.UTF_8));
            // Clearing the user cache as we updated the database directly
            session.getProvider(UserCache.class).clear();
        });

        loginPage.open();
        loginPage.login(username, password);
        appPage.assertCurrent();

        credential = PasswordCredentialModel.createFromCredentialModel(fetchCredentials(username));

        assertEquals(Pbkdf2PasswordHashProviderFactory.ID, credential.getPasswordCredentialData().getAlgorithm());
        assertEncoded(credential, password, credential.getPasswordSecretData().getSalt(), "PBKDF2WithHmacSHA1", 1);
    }

    @Test
    public void testPasswordRehashedToDefaultProviderIfHashAlgorithmRemoved() {
        setPasswordPolicy("hashAlgorithm(" + Pbkdf2Sha256PasswordHashProviderFactory.ID + ")");

        String username = "testPasswordRehashedToDefaultProviderIfHashAlgorithmRemoved";
        final String password = createUser(username);

        PasswordCredentialModel credential = PasswordCredentialModel.createFromCredentialModel(fetchCredentials(username));

        assertEquals(Pbkdf2Sha256PasswordHashProviderFactory.ID, credential.getPasswordCredentialData().getAlgorithm());

        setPasswordPolicy("");

        loginPage.open();
        loginPage.login(username, password);
        appPage.assertCurrent();

        credential = PasswordCredentialModel.createFromCredentialModel(fetchCredentials(username));

        assertEquals(DefaultPasswordHash.getDefaultAlgorithm(), credential.getPasswordCredentialData().getAlgorithm());
    }

    @Test
    public void testPasswordRehashedOnIterationsChanged() throws Exception {
        setPasswordPolicy("hashIterations(1)");

        String username = "testPasswordRehashedOnIterationsChanged";
        final String password = createUser(username);

        PasswordCredentialModel credential = PasswordCredentialModel.createFromCredentialModel(fetchCredentials(username));

        assertEquals(1, credential.getPasswordCredentialData().getHashIterations());

        setPasswordPolicy("hashIterations(2)");

        loginPage.open();
        loginPage.login(username, password);
        appPage.assertCurrent();

        credential = PasswordCredentialModel.createFromCredentialModel(fetchCredentials(username));

        assertEquals(2, credential.getPasswordCredentialData().getHashIterations());

        if (notFips()) {
            assertEncoded(credential, password, credential.getPasswordSecretData().getSalt(), "Argon2id", 2);
        } else {
            assertEncoded(credential, password, credential.getPasswordSecretData().getSalt(), "PBKDF2WithHmacSHA512", 2);
        }
    }

    // KEYCLOAK-5282
    @Test
    public void testPasswordNotRehasedUnchangedIterations() {
        setPasswordPolicy("");

        String username = "testPasswordNotRehasedUnchangedIterations";
        final String password = createUser(username);

        PasswordCredentialModel credential = PasswordCredentialModel.createFromCredentialModel(fetchCredentials(username));
        String credentialId = credential.getId();
        byte[] salt = credential.getPasswordSecretData().getSalt();

        setPasswordPolicy("hashIterations");

        loginPage.open();
        loginPage.login(username, password);
        appPage.assertCurrent();

        credential = PasswordCredentialModel.createFromCredentialModel(fetchCredentials(username));

        assertEquals(credentialId, credential.getId());
        assertArrayEquals(salt, credential.getPasswordSecretData().getSalt());

        setPasswordPolicy("hashIterations(" + Pbkdf2Sha512PasswordHashProviderFactory.DEFAULT_ITERATIONS + ")");

        AccountHelper.logout(adminClient.realm("test"), username);

        loginPage.open();
        loginPage.login(username, password);
        appPage.assertCurrent();

        credential = PasswordCredentialModel.createFromCredentialModel(fetchCredentials(username));

        assertEquals(credentialId, credential.getId());
        assertArrayEquals(salt, credential.getPasswordSecretData().getSalt());
    }

    @Test
    public void testPasswordRehashedWhenCredentialImportedWithDifferentKeySize() {
        setPasswordPolicy("hashAlgorithm(" + Pbkdf2Sha512PasswordHashProviderFactory.ID + ") and hashIterations(" + Pbkdf2Sha512PasswordHashProviderFactory.DEFAULT_ITERATIONS + ")");

        String username = "testPasswordRehashedWhenCredentialImportedWithDifferentKeySize";
        String password = generatePassword();

        // Encode with a specific key size (256 instead of default: 512)
        Pbkdf2PasswordHashProvider specificKeySizeHashProvider = new Pbkdf2PasswordHashProvider(Pbkdf2Sha512PasswordHashProviderFactory.ID,
                Pbkdf2Sha512PasswordHashProviderFactory.PBKDF2_ALGORITHM,
                Pbkdf2Sha512PasswordHashProviderFactory.DEFAULT_ITERATIONS,
                0,
                256);
        PasswordCredentialModel passwordCredentialModel = specificKeySizeHashProvider.encodedCredential(password, -1);

        // Create a user with the encoded password, simulating a user import from a different system using a specific key size
        UserRepresentation user = UserBuilder.create().username(username).build();
        user.setCredentials(List.of(ExportUtils.exportCredential(passwordCredentialModel)));
        ApiUtil.createUserWithAdminClient(adminClient.realm("test"), user);

        loginPage.open();
        loginPage.login(username, password);
        appPage.assertCurrent();

        PasswordCredentialModel postLoginCredentials = PasswordCredentialModel.createFromCredentialModel(fetchCredentials(username));
        // Check that the password was rehashed and the secret string is now twice the size as before
        assertEquals(passwordCredentialModel.getPasswordSecretData().getValue().length() * 2, postLoginCredentials.getPasswordSecretData().getValue().length());

    }

    @Test
    public void testPbkdf2Sha1() throws Exception {
        setPasswordPolicy("hashAlgorithm(" + Pbkdf2PasswordHashProviderFactory.ID + ")");
        String username = "testPbkdf2Sha1";
        final String password = createUser(username);

        PasswordCredentialModel credential = PasswordCredentialModel.createFromCredentialModel(fetchCredentials(username));
        assertEncoded(credential, password, credential.getPasswordSecretData().getSalt(), "PBKDF2WithHmacSHA1", Pbkdf2PasswordHashProviderFactory.DEFAULT_ITERATIONS);
    }

    @Test
    public void testArgon2() {
        Assume.assumeTrue("Argon2 tests skipped in FIPS mode", notFips());

        setPasswordPolicy("hashAlgorithm(" + Argon2PasswordHashProviderFactory.ID + ")");
        String username = "testArgon2";
        final String password = createUser(username);

        PasswordCredentialModel credential = PasswordCredentialModel.createFromCredentialModel(fetchCredentials(username));
        PasswordCredentialData data = credential.getPasswordCredentialData();

        Assert.assertEquals("argon2", data.getAlgorithm());
        Assert.assertEquals(5, data.getHashIterations());
        Assert.assertEquals("1.3", data.getAdditionalParameters().getFirst("version"));
        Assert.assertEquals("id", data.getAdditionalParameters().getFirst("type"));
        Assert.assertEquals("32", data.getAdditionalParameters().getFirst("hashLength"));
        Assert.assertEquals("7168", data.getAdditionalParameters().getFirst("memory"));
        Assert.assertEquals("1", data.getAdditionalParameters().getFirst("parallelism"));

        loginPage.open();
        loginPage.login("testArgon2", "invalid");
        loginPage.assertCurrent();
        Assert.assertEquals("Invalid username or password.", loginPage.getInputError());

        loginPage.login("testArgon2", password);

        appPage.assertCurrent();
    }

    private static boolean notFips() {
        return AuthServerTestEnricher.AUTH_SERVER_FIPS_MODE == FipsMode.DISABLED;
    }

    @Test
    public void testDefault() throws Exception {
        setPasswordPolicy("");
        String username = "testDefault";
        final String password = createUser(username);
        PasswordCredentialModel credential = PasswordCredentialModel.createFromCredentialModel(fetchCredentials(username));

        if (notFips()) {
            assertEncoded(credential, password, credential.getPasswordSecretData().getSalt(), "Argon2id", Argon2Parameters.DEFAULT_ITERATIONS);
        } else {
            assertEncoded(credential, password, credential.getPasswordSecretData().getSalt(), "PBKDF2WithHmacSHA512", Pbkdf2Sha512PasswordHashProviderFactory.DEFAULT_ITERATIONS);
        }
    }

    @Test
    public void testPbkdf2Sha256() throws Exception {
        setPasswordPolicy("hashAlgorithm(" + Pbkdf2Sha256PasswordHashProviderFactory.ID + ")");
        String username = "testPbkdf2Sha256";
        final String password = createUser(username);

        PasswordCredentialModel credential = PasswordCredentialModel.createFromCredentialModel(fetchCredentials(username));
        assertEncoded(credential, password, credential.getPasswordSecretData().getSalt(), "PBKDF2WithHmacSHA256", Pbkdf2Sha256PasswordHashProviderFactory.DEFAULT_ITERATIONS);
    }

    @Test
    public void testPbkdf2Sha512() throws Exception {
        setPasswordPolicy("hashAlgorithm(" + Pbkdf2Sha512PasswordHashProviderFactory.ID + ")");
        String username = "testPbkdf2Sha512";
        final String password = createUser(username);

        PasswordCredentialModel credential = PasswordCredentialModel.createFromCredentialModel(fetchCredentials(username));
        assertEncoded(credential, password, credential.getPasswordSecretData().getSalt(), "PBKDF2WithHmacSHA512", Pbkdf2Sha512PasswordHashProviderFactory.DEFAULT_ITERATIONS);
    }

    @Test
    public void testPbkdf2Sha256WithPadding() throws Exception {
        setPasswordPolicy("hashAlgorithm(" + Pbkdf2Sha256PasswordHashProviderFactory.ID + ")");

        int originalPaddingLength = configurePaddingForKeycloak(14);
        try {
            // Assert password created with padding enabled can be verified
            String username1 = "test1-Pbkdf2Sha2562";
            final String password = createUser(username1);

            PasswordCredentialModel credential = PasswordCredentialModel.createFromCredentialModel(fetchCredentials(username1));
            assertEncoded(credential, password, credential.getPasswordSecretData().getSalt(), "PBKDF2WithHmacSHA256", Pbkdf2Sha256PasswordHashProviderFactory.DEFAULT_ITERATIONS);

            // Now configure padding to bigger than 64. The verification without padding would fail as for longer padding than 64 characters, the hashes of the padded password and unpadded password would be different
            configurePaddingForKeycloak(65);
            String username2 = "test2-Pbkdf2Sha2562";
            createUser(username2);

            credential = PasswordCredentialModel.createFromCredentialModel(fetchCredentials(username2));
            assertEncoded(credential, password, credential.getPasswordSecretData().getSalt(), "PBKDF2WithHmacSHA256", Pbkdf2Sha256PasswordHashProviderFactory.DEFAULT_ITERATIONS, false);

        } finally {
            configurePaddingForKeycloak(originalPaddingLength);
        }
    }


    private String createUser(String username) {
        final String password = generatePassword();
        ApiUtil.createUserAndResetPasswordWithAdminClient(adminClient.realm("test"), UserBuilder.create().username(username).build(), password);
        return password;
    }

    private void setPasswordPolicy(String policy) {
        RealmRepresentation realmRep = testRealm().toRepresentation();
        realmRep.setPasswordPolicy(policy);
        testRealm().update(realmRep);
    }

    private CredentialModel fetchCredentials(String username) {
        return testingClient.server("test").fetch(session -> {
            RealmModel realm = session.getContext().getRealm();
            UserModel user = session.users().getUserByUsername(realm, username);
            return user.credentialManager().getStoredCredentialsByTypeStream(CredentialRepresentation.PASSWORD)
                    .findFirst().orElse(null);
        }, CredentialModel.class);
    }

    private void assertEncoded(PasswordCredentialModel credential, String password, byte[] salt, String algorithm, int iterations) throws Exception {
        assertEncoded(credential, password, salt, algorithm, iterations, true);
    }

    private void assertEncoded(PasswordCredentialModel credential, String password, byte[] salt, String algorithm, int iterations, boolean expectedSuccess) throws Exception {
        if (algorithm.startsWith("PBKDF2")) {
            int keyLength = 512;

            if (Pbkdf2Sha256PasswordHashProviderFactory.ID.equals(credential.getPasswordCredentialData().getAlgorithm())) {
                keyLength = 256;
            }

            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, keyLength);
            byte[] key = SecretKeyFactory.getInstance(algorithm).generateSecret(spec).getEncoded();
            if (expectedSuccess) {
                assertEquals(Base64.getEncoder().encodeToString(key), credential.getPasswordSecretData().getValue());
            } else {
                assertNotEquals(Base64.getEncoder().encodeToString(key), credential.getPasswordSecretData().getValue());
            }
        } else if (algorithm.equals("Argon2id")) {
            org.bouncycastle.crypto.params.Argon2Parameters parameters = new org.bouncycastle.crypto.params.Argon2Parameters.Builder(org.bouncycastle.crypto.params.Argon2Parameters.ARGON2_id)
                    .withVersion(org.bouncycastle.crypto.params.Argon2Parameters.ARGON2_VERSION_13)
                    .withSalt(salt)
                    .withParallelism(1)
                    .withMemoryAsKB(7168)
                    .withIterations(iterations).build();

            Argon2BytesGenerator generator = new Argon2BytesGenerator();
            generator.init(parameters);

            byte[] result = new byte[32];
            generator.generateBytes(password.toCharArray(), result);
            Assert.assertEquals(Base64.getEncoder().encodeToString(result), credential.getPasswordSecretData().getValue());
        }
    }

    private int configurePaddingForKeycloak(int paddingLength) {
        return testingClient.server("test").fetch(session -> {
            Pbkdf2Sha256PasswordHashProviderFactory factory = (Pbkdf2Sha256PasswordHashProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(PasswordHashProvider.class, Pbkdf2Sha256PasswordHashProviderFactory.ID);
            int origPaddingLength = factory.getMaxPaddingLength();
            factory.setMaxPaddingLength(paddingLength);
            return origPaddingLength;
        }, Integer.class);
    }

    /**
     * Simple test to compare runtimes of different password hashing configurations.
     */
//    @Test
    public void testBenchmarkPasswordHashingConfigurations() {

        int numberOfPasswords = 1000;
        List<String> plainTextPasswords = IntStream.rangeClosed(1, numberOfPasswords).mapToObj(i -> UUID.randomUUID().toString()).collect(Collectors.toList());

        Function<Runnable, Duration> timeit = runner -> {

            long time = -System.nanoTime();
            runner.run();
            time += System.nanoTime();

            return Duration.ofNanos((long) (time / ((double) plainTextPasswords.size())));
        };

        BiFunction<PasswordHashProvider, Integer, Long> hasher = (provider, iterations) -> {
            long result = 0L;
            for (String password : plainTextPasswords) {
                String encoded = provider.encodedCredential(password, iterations).getPasswordSecretData().getValue();
                result += encoded.hashCode();
            }
            return result;
        };

        var comparisons = List.of(
                // this takes quite a long time. Run this with a low value numberOfPasswords, e.g. 1-10
                // new PasswordHashComparison(new Pbkdf2PasswordHashProviderFactory(), 20_000, 1_300_000),

                new PasswordHashComparison(new Pbkdf2Sha256PasswordHashProviderFactory(), 27_500, 600_000),
                new PasswordHashComparison(new Pbkdf2Sha512PasswordHashProviderFactory(), 30_000, 210_000)
        );

        comparisons.forEach(comp -> {
            Pbkdf2PasswordHashProvider hashProvider = (Pbkdf2PasswordHashProvider) comp.factory.create(null);
            System.out.printf("Hashing %s password(s) with %s%n", plainTextPasswords.size(), hashProvider.getPbkdf2Algorithm());

            var durationOld = timeit.apply(() -> hasher.apply(hashProvider, comp.iterationsOld));
            System.out.printf("\tØ hashing duration with %d iterations: %sms%n", comp.iterationsOld, durationOld.toMillis());

            var durationNew = timeit.apply(() -> hasher.apply(hashProvider, comp.iterationsNew));
            System.out.printf("\tØ hashing duration with %d iterations: %sms%n", comp.iterationsNew, durationNew.toMillis());

            var deltaTimeMillis = durationNew.toMillis() - durationOld.toMillis();
            System.out.printf("\tDifference: +%s ms%n", deltaTimeMillis);
        });
    }


    class PasswordHashComparison {

        final PasswordHashProviderFactory factory;

        final int iterationsOld;

        final int iterationsNew;

        public PasswordHashComparison(PasswordHashProviderFactory factory, int iterationsOld, int iterationsNew) {
            this.factory = factory;
            this.iterationsOld = iterationsOld;
            this.iterationsNew = iterationsNew;
        }
    }
}

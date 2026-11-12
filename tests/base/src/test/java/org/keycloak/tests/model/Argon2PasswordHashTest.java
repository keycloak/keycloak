package org.keycloak.tests.model;

import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.hash.PasswordHashProvider;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.credential.dto.PasswordCredentialData;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@KeycloakIntegrationTest
public class Argon2PasswordHashTest {

    private static final String PASSWORD = "Password-1";

    @InjectRealm
    ManagedRealm realm;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @Test
    public void defaultParameters() {
        String username = "argon2-defaults";
        CredentialModel credential = createUserWithPolicy(username, "hashAlgorithm(argon2)");

        PasswordCredentialData data = PasswordCredentialModel.createFromCredentialModel(credential).getPasswordCredentialData();
        assertEquals("argon2", data.getAlgorithm());
        assertEquals(5, data.getHashIterations());
        assertEquals("1.3", data.getAdditionalParameters().getFirst("version"));
        assertEquals("id", data.getAdditionalParameters().getFirst("type"));
        assertEquals("32", data.getAdditionalParameters().getFirst("hashLength"));
        assertEquals("7168", data.getAdditionalParameters().getFirst("memory"));
        assertEquals("1", data.getAdditionalParameters().getFirst("parallelism"));

        assertFalse(isPasswordValid(username, "invalid"));
        assertTrue(isPasswordValid(username, PASSWORD));
    }

    @Test
    public void successfulLoginDoesNotRehashPasswordWithoutPolicyIterations() {
        String username = "argon2-no-policy-iterations";
        CredentialModel original = createUserWithPolicy(username, "hashAlgorithm(argon2)");

        assertTrue(isPasswordValid(username, PASSWORD));

        assertEquals(original.getSecretData(), credential(username).getSecretData(), "Password hash created with the provider defaults must not be re-hashed on login");
    }

    @Test
    public void policyIterationsAbove100AreIgnored() {
        String username = "argon2-large-policy-iterations";
        // 210000 is the PBKDF2 default, which is far too large for Argon2, so the provider default is used
        CredentialModel original = createUserWithPolicy(username, "hashAlgorithm(argon2) and hashIterations(210000)");
        assertEquals(5, iterations(original));

        assertTrue(isPasswordValid(username, PASSWORD));

        assertEquals(original.getSecretData(), credential(username).getSecretData(), "Password hash created with the provider default iterations must not be re-hashed on login");
    }

    @Test
    public void successfulLoginRehashesPbkdf2PasswordWhenAlgorithmChangesToArgon2() {
        String username = "argon2-from-pbkdf2";
        CredentialModel original = createUserWithPolicy(username, "hashAlgorithm(pbkdf2-sha512)");
        assertEquals("pbkdf2-sha512", algorithm(original));

        setPolicy("hashAlgorithm(argon2)");
        assertTrue(isPasswordValid(username, PASSWORD));

        assertEquals("argon2", algorithm(credential(username)));
    }

    @Test
    public void successfulLoginDoesNotRehashPasswordHashedWithPolicyIterations() {
        String username = "argon2-up-to-date";
        // the realm policy iterations differ from the Argon2 provider default, so the hash is created with them
        CredentialModel original = createUserWithPolicy(username, "hashAlgorithm(argon2) and hashIterations(2)");
        assertEquals(2, iterations(original));

        assertTrue(isPasswordValid(username, PASSWORD));

        // a re-hash would be visible as different secret data, as every hash uses a new random salt
        assertEquals(original.getSecretData(), credential(username).getSecretData(), "Password hash matching the policy must not be re-hashed on login");
    }

    @Test
    public void successfulLoginRehashesPasswordWhenPolicyIterationsChange() {
        String username = "argon2-outdated";
        CredentialModel original = createUserWithPolicy(username, "hashAlgorithm(argon2) and hashIterations(2)");
        assertEquals(2, iterations(original));

        setPolicy("hashAlgorithm(argon2) and hashIterations(3)");
        assertTrue(isPasswordValid(username, PASSWORD));

        CredentialModel rehashed = credential(username);
        assertNotEquals(original.getSecretData(), rehashed.getSecretData(), "Password hash not matching the policy must be re-hashed on login");
        assertEquals(3, iterations(rehashed));
    }

    private CredentialModel createUserWithPolicy(String username, String policy) {
        assumeTrue(runOnServer.fetch(session -> session.getProvider(PasswordHashProvider.class, "argon2") != null, Boolean.class),
                "Argon2 is unavailable in FIPS mode");
        setPolicy(policy);
        runOnServer.run(session -> {
            UserModel user = session.users().addUser(session.getContext().getRealm(), username);
            assertTrue(user.credentialManager().updateCredential(UserCredentialModel.password(PASSWORD)));
        });
        return credential(username);
    }

    // the policy is reset to the original one when the test completes
    private void setPolicy(String policy) {
        realm.updateWithCleanup(r -> r.passwordPolicy(policy));
    }

    private boolean isPasswordValid(String username, String password) {
        return runOnServer.fetch(session -> session.users().getUserByUsername(session.getContext().getRealm(), username)
                .credentialManager().isValid(UserCredentialModel.password(password)), Boolean.class);
    }

    private static String algorithm(CredentialModel credential) {
        return PasswordCredentialModel.createFromCredentialModel(credential).getPasswordCredentialData().getAlgorithm();
    }

    private static int iterations(CredentialModel credential) {
        return PasswordCredentialModel.createFromCredentialModel(credential).getPasswordCredentialData().getHashIterations();
    }

    private CredentialModel credential(String username) {
        return runOnServer.fetch(session -> session.users().getUserByUsername(session.getContext().getRealm(), username)
                .credentialManager().getStoredCredentialsByTypeStream(PasswordCredentialModel.TYPE).findFirst().orElseThrow(),
                CredentialModel.class);
    }
}

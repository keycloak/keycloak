package org.keycloak.tests.model;

import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.hash.PasswordHashProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.tests.common.CustomProvidersServerConfig;
import org.keycloak.tests.providers.federation.NoCredentialUserStorageProviderFactory;
import org.keycloak.tests.providers.hash.RehashPasswordHashProviderFactory;
import org.keycloak.tests.providers.hash.RehashPasswordHashProviderFactory.Phase;
import org.keycloak.tests.suites.DatabaseTest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@KeycloakIntegrationTest(config = CustomProvidersServerConfig.class)
@DatabaseTest
public class PasswordRehashTest {

    private static final String OLD_PASSWORD = "Old-password1";
    private static final String NEW_PASSWORD = "New-password1";

    @InjectRealm(lifecycle = LifeCycle.METHOD)
    ManagedRealm realm;

    @InjectRunOnServer(permittedPackages = "org.keycloak.tests.model")
    RunOnServerClient runOnServer;

    @ParameterizedTest(name = "federated={0}, phase={1}")
    @CsvSource({
            "false, DURING_HASH",
            "true, DURING_HASH",
            "false, BEFORE_COMMIT",
            "true, BEFORE_COMMIT"
    })
    public void passwordChangeSurvivesRehash(boolean federated, Phase phase) {
        String username = prepareOutdatedPassword(federated);

        runOnServer.run(session -> {
            RehashPasswordHashProviderFactory.onNextHash(session, phase, rehashSession -> KeycloakModelUtils.runJobInTransaction(
                    rehashSession.getKeycloakSessionFactory(), rehashSession.getContext(), other -> assertTrue(user(other, username)
                            .credentialManager().updateCredential(UserCredentialModel.password(NEW_PASSWORD)))));
            assertTrue(isPasswordValid(session, username, OLD_PASSWORD));
        });

        runOnServer.run(session -> {
            assertTrue(isPasswordValid(session, username, NEW_PASSWORD), "New password must remain valid");
            assertFalse(isPasswordValid(session, username, OLD_PASSWORD), "Old password must remain invalid");
        });
    }

    @Test
    public void rehashPreservesConcurrentMetadataChanges() {
        String username = prepareOutdatedPassword(false);
        CredentialModel original = credential(username);

        runOnServer.run(session -> {
            RehashPasswordHashProviderFactory.onNextHash(session, Phase.DURING_HASH, rehashSession -> KeycloakModelUtils.runJobInTransaction(
                    rehashSession.getKeycloakSessionFactory(), rehashSession.getContext(), other -> {
                        UserModel user = user(other, username);
                        CredentialModel credential = storedPassword(user);
                        credential.setUserLabel("updated-label");
                        credential.setCreatedDate(1234L);
                        user.credentialManager().updateStoredCredential(credential);
                    }));
            assertTrue(isPasswordValid(session, username, OLD_PASSWORD));
        });

        CredentialModel current = credential(username);
        assertEquals(original.getId(), current.getId());
        assertEquals("updated-label", current.getUserLabel());
        assertEquals(Long.valueOf(1234L), current.getCreatedDate());
        assertNotEquals(original.getSecretData(), current.getSecretData(), "Rehash must still update the password hash");
        assertEquals(2, PasswordCredentialModel.createFromCredentialModel(current).getPasswordCredentialData().getHashIterations());
        runOnServer.run(session -> assertTrue(isPasswordValid(session, username, OLD_PASSWORD)));
    }

    @Test
    public void freshArgon2HashWithRealmIterationsIsNotRehashed() {
        assumeTrue(runOnServer.fetch(session -> session.getProvider(PasswordHashProvider.class, "argon2") != null, Boolean.class),
                "Argon2 is unavailable in FIPS mode");
        String username = "argon2-user";
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            realm.setPasswordPolicy(PasswordPolicy.parse(session, "hashAlgorithm(argon2) and hashIterations(2)"));
            UserModel user = session.users().addUser(realm, username);
            assertTrue(user.credentialManager().updateCredential(UserCredentialModel.password(OLD_PASSWORD)));
        });
        CredentialModel original = credential(username);
        assertEquals(2, PasswordCredentialModel.createFromCredentialModel(original).getPasswordCredentialData().getHashIterations());

        runOnServer.run(session -> assertTrue(isPasswordValid(session, username, OLD_PASSWORD)));

        assertEquals(original.getSecretData(), credential(username).getSecretData(), "An up-to-date credential must not be re-hashed");
    }

    @AfterEach
    public void clearHashAction() {
        runOnServer.run(RehashPasswordHashProviderFactory::clearAction);
    }

    private String prepareOutdatedPassword(boolean federated) {
        String username = federated ? NoCredentialUserStorageProviderFactory.USERNAME_PREFIX + "rehash-user" : "rehash-user";
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            realm.setPasswordPolicy(PasswordPolicy.parse(session, "hashAlgorithm(" + RehashPasswordHashProviderFactory.ID + ") and hashIterations(1)"));
            UserModel user;
            if (federated) {
                ComponentModel component = new ComponentModel();
                component.setName("no-credential");
                component.setParentId(realm.getId());
                component.setProviderType(UserStorageProvider.class.getName());
                component.setProviderId(NoCredentialUserStorageProviderFactory.ID);
                realm.addComponentModel(component);
                user = user(session, username);
            } else {
                user = session.users().addUser(realm, username);
            }
            assertEquals(!federated, StorageId.isLocalStorage(user.getId()));
            assertTrue(user.credentialManager().updateCredential(UserCredentialModel.password(OLD_PASSWORD)));
            realm.setPasswordPolicy(PasswordPolicy.parse(session, "hashAlgorithm(" + RehashPasswordHashProviderFactory.ID + ") and hashIterations(2)"));
        });
        return username;
    }

    private CredentialModel credential(String username) {
        return runOnServer.fetch(session -> storedPassword(user(session, username)), CredentialModel.class);
    }

    private static boolean isPasswordValid(KeycloakSession session, String username, String password) {
        return user(session, username).credentialManager().isValid(UserCredentialModel.password(password));
    }

    private static UserModel user(KeycloakSession session, String username) {
        return session.users().getUserByUsername(session.getContext().getRealm(), username);
    }

    private static CredentialModel storedPassword(UserModel user) {
        return user.credentialManager().getStoredCredentialsByTypeStream(PasswordCredentialModel.TYPE).findFirst().orElseThrow();
    }
}

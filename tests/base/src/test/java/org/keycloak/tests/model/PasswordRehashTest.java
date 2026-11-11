package org.keycloak.tests.model;

import org.keycloak.credential.CredentialModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class PasswordRehashTest {

    private static final String OLD_PASSWORD = "Old-password1";
    private static final String NEW_PASSWORD = "New-password1";

    @InjectRealm
    ManagedRealm realm;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @Test
    public void passwordUpdatedInSameTransactionAfterValidationIsKept() {
        String username = createUserWithOutdatedHash("rehash-same-transaction");

        runOnServer.run(session -> {
            UserModel user = user(session, username);
            assertTrue(user.credentialManager().isValid(UserCredentialModel.password(OLD_PASSWORD)));
            assertTrue(user.credentialManager().updateCredential(UserCredentialModel.password(NEW_PASSWORD)));
        });

        assertPasswordValid(username, NEW_PASSWORD, true);
        assertPasswordValid(username, OLD_PASSWORD, false);
    }

    @Test
    public void passwordUpdatedByOtherTransactionAfterValidationIsKept() {
        String username = createUserWithOutdatedHash("rehash-other-transaction");

        runOnServer.run(session -> {
            assertTrue(user(session, username).credentialManager().isValid(UserCredentialModel.password(OLD_PASSWORD)));
            // commits before the re-hash is run when the current transaction completes
            KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), session.getContext(),
                    other -> assertTrue(user(other, username).credentialManager().updateCredential(UserCredentialModel.password(NEW_PASSWORD))));
        });

        assertPasswordValid(username, NEW_PASSWORD, true);
        assertPasswordValid(username, OLD_PASSWORD, false);
    }

    @Test
    public void passwordIsRehashedWhenNothingChangedAfterValidation() {
        String username = createUserWithOutdatedHash("rehash-unchanged");
        CredentialModel original = credential(username);

        assertPasswordValid(username, OLD_PASSWORD, true);

        CredentialModel rehashed = credential(username);
        assertEquals(original.getId(), rehashed.getId());
        assertEquals(2, PasswordCredentialModel.createFromCredentialModel(rehashed).getPasswordCredentialData().getHashIterations());
        assertPasswordValid(username, OLD_PASSWORD, true);
    }

    @Test
    public void updateStoredCredentialOnlyReplacesUnchangedCredential() {
        String username = createUserWithOutdatedHash("rehash-swap");

        runOnServer.run(session -> {
            UserModel user = user(session, username);
            CredentialModel old = storedPassword(user);

            CredentialModel changedLabel = old.shallowClone();
            changedLabel.setUserLabel("changed");
            assertTrue(user.credentialManager().updateStoredCredential(old, changedLabel));

            CredentialModel replacement = old.shallowClone();
            replacement.setUserLabel("replacement");
            assertFalse(user.credentialManager().updateStoredCredential(old, replacement), "The credential was changed since it was read");
            assertEquals("changed", storedPassword(user).getUserLabel());

            assertTrue(user.credentialManager().updateStoredCredential(changedLabel, replacement));
            assertEquals("replacement", storedPassword(user).getUserLabel());
        });
    }

    private String createUserWithOutdatedHash(String username) {
        realm.updateWithCleanup(r -> r.passwordPolicy("hashAlgorithm(pbkdf2-sha512) and hashIterations(1)"));
        runOnServer.run(session -> {
            UserModel user = session.users().addUser(session.getContext().getRealm(), username);
            assertTrue(user.credentialManager().updateCredential(UserCredentialModel.password(OLD_PASSWORD)));
        });
        realm.updateWithCleanup(r -> r.passwordPolicy("hashAlgorithm(pbkdf2-sha512) and hashIterations(2)"));
        return username;
    }

    private void assertPasswordValid(String username, String password, boolean expected) {
        assertEquals(expected, runOnServer.fetch(session -> user(session, username).credentialManager()
                .isValid(UserCredentialModel.password(password)), Boolean.class));
    }

    private CredentialModel credential(String username) {
        return runOnServer.fetch(session -> storedPassword(user(session, username)), CredentialModel.class);
    }

    private static UserModel user(KeycloakSession session, String username) {
        return session.users().getUserByUsername(session.getContext().getRealm(), username);
    }

    private static CredentialModel storedPassword(UserModel user) {
        return user.credentialManager().getStoredCredentialsByTypeStream(PasswordCredentialModel.TYPE).findFirst().orElseThrow();
    }
}

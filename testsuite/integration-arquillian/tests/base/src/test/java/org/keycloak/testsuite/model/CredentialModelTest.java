package org.keycloak.testsuite.model;

import org.junit.Test;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.ModelTest;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer.REMOTE;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@AuthServerContainerExclude(REMOTE)
public class CredentialModelTest extends AbstractTestRealmKeycloakTest {

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {

    }


    @Test
    @ModelTest
    public void testCredentialCRUD(KeycloakSession session) throws Exception {
        AtomicReference<String> passwordId = new AtomicReference<>();
        AtomicReference<String> otp1Id = new AtomicReference<>();
        AtomicReference<String> otp2Id = new AtomicReference<>();

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession currentSession) -> {
            RealmModel realm = currentSession.realms().getRealmByName("test");

            UserModel user = currentSession.users().getUserByUsername(realm, "test-user@localhost");
            List<CredentialModel> list = user.credentialManager().getStoredCredentialsStream()
                    .collect(Collectors.toList());
            Assert.assertEquals(1, list.size());
            passwordId.set(list.get(0).getId());

            // Create 2 OTP credentials (password was already created)
            CredentialModel otp1 = OTPCredentialModel.createFromPolicy(realm, "secret1");
            CredentialModel otp2 = OTPCredentialModel.createFromPolicy(realm, "secret2");
            otp1 = user.credentialManager().createStoredCredential(otp1);
            otp2 = user.credentialManager().createStoredCredential(otp2);
            otp1Id.set(otp1.getId());
            otp2Id.set(otp2.getId());
        });


        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession currentSession) -> {
            RealmModel realm = currentSession.realms().getRealmByName("test");
            UserModel user = currentSession.users().getUserByUsername(realm, "test-user@localhost");

            // Assert priorities: password, otp1, otp2
            List<CredentialModel> list = user.credentialManager().getStoredCredentialsStream()
                    .collect(Collectors.toList());
            assertOrder(list, passwordId.get(), otp1Id.get(), otp2Id.get());

            // Assert can't move password when newPreviousCredential not found
            Assert.assertFalse(user.credentialManager().moveStoredCredentialTo(passwordId.get(), "not-known"));

            // Assert can't move credential when not found
            Assert.assertFalse(user.credentialManager().moveStoredCredentialTo("not-known", otp2Id.get()));

            // Move otp2 up 1 position
            Assert.assertTrue(user.credentialManager().moveStoredCredentialTo(otp2Id.get(), passwordId.get()));
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession currentSession) -> {
            RealmModel realm = currentSession.realms().getRealmByName("test");
            UserModel user = currentSession.users().getUserByUsername(realm, "test-user@localhost");

            // Assert priorities: password, otp2, otp1
            List<CredentialModel> list = user.credentialManager().getStoredCredentialsStream()
                    .collect(Collectors.toList());
            assertOrder(list, passwordId.get(), otp2Id.get(), otp1Id.get());

            // Move otp2 to the top
            Assert.assertTrue(user.credentialManager().moveStoredCredentialTo(otp2Id.get(), null));
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession currentSession) -> {
            RealmModel realm = currentSession.realms().getRealmByName("test");
            UserModel user = currentSession.users().getUserByUsername(realm, "test-user@localhost");

            // Assert priorities: otp2, password, otp1
            List<CredentialModel> list = user.credentialManager().getStoredCredentialsStream()
                    .collect(Collectors.toList());
            assertOrder(list, otp2Id.get(), passwordId.get(), otp1Id.get());

            // Move password down
            Assert.assertTrue(user.credentialManager().moveStoredCredentialTo(passwordId.get(), otp1Id.get()));
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession currentSession) -> {
            RealmModel realm = currentSession.realms().getRealmByName("test");
            UserModel user = currentSession.users().getUserByUsername(realm, "test-user@localhost");

            // Assert priorities: otp2, otp1, password
            List<CredentialModel> list = user.credentialManager().getStoredCredentialsStream()
                    .collect(Collectors.toList());
            assertOrder(list, otp2Id.get(), otp1Id.get(), passwordId.get());

            // Remove otp2 down two positions
            Assert.assertTrue(user.credentialManager().moveStoredCredentialTo(otp2Id.get(), passwordId.get()));
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession currentSession) -> {
            RealmModel realm = currentSession.realms().getRealmByName("test");
            UserModel user = currentSession.users().getUserByUsername(realm, "test-user@localhost");

            // Assert priorities: otp2, otp1, password
            List<CredentialModel> list = user.credentialManager().getStoredCredentialsStream()
                    .collect(Collectors.toList());
            assertOrder(list, otp1Id.get(), passwordId.get(), otp2Id.get());

            // Remove password
            Assert.assertTrue(user.credentialManager().removeStoredCredentialById(passwordId.get()));
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession currentSession) -> {
            RealmModel realm = currentSession.realms().getRealmByName("test");
            UserModel user = currentSession.users().getUserByUsername(realm, "test-user@localhost");

            // Assert priorities: otp2, password
            List<CredentialModel> list = user.credentialManager().getStoredCredentialsStream()
                    .collect(Collectors.toList());
            assertOrder(list, otp1Id.get(), otp2Id.get());
        });
    }


    private void assertOrder(List<CredentialModel> creds, String... expectedIds) {
        Assert.assertEquals(expectedIds.length, creds.size());

        if (creds.size() == 0) return;

        for (int i=0 ; i<expectedIds.length ; i++) {
            Assert.assertEquals(creds.get(i).getId(), expectedIds[i]);
        }
    }

}

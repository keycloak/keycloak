package org.keycloak.tests.admin.user;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.util.ObjectUtil;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.AdminEventAssertion;
import org.keycloak.tests.utils.admin.AdminEventPaths;
import org.keycloak.tests.utils.admin.ApiUtil;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@KeycloakIntegrationTest
public class UserCredentialTest extends AbstractUserTest {

    @Test
    public void resetUserPassword() {
        String userId = createUser("user1", "user1@localhost");

        CredentialRepresentation cred = new CredentialRepresentation();
        cred.setType(CredentialRepresentation.PASSWORD);
        cred.setValue("password");
        cred.setTemporary(false);

        managedRealm.admin().users().get(userId).resetPassword(cred);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.ACTION, AdminEventPaths.userResetPasswordPath(userId), ResourceType.USER);

        oauth.realm(managedRealm.getName());
        oauth.openLoginForm();

        assertEquals("Sign in to your account", PageUtils.getPageTitle(driver));

        loginPage.login("user1", "password");

        assertTrue(driver.getTitle().contains("AUTH_RESPONSE"));

        // oauth cleanup
        oauth.realm("test");
    }

    @Test
    public void resetUserInvalidPassword() {
        String userId = createUser("user1", "user1@localhost");

        try {
            CredentialRepresentation cred = new CredentialRepresentation();
            cred.setType(CredentialRepresentation.PASSWORD);
            cred.setValue(" ");
            cred.setTemporary(false);
            managedRealm.admin().users().get(userId).resetPassword(cred);
            fail("Expected failure");
        } catch (ClientErrorException e) {
            assertEquals(400, e.getResponse().getStatus());
            e.getResponse().close();
            assertAdminEvents.assertEmpty();
        }
    }

    @Test
    public void loginShouldFailAfterPasswordDeleted() {
        String userName = "credential-tester";
        String userPass = "s3cr37";
        String userId = createUser(REALM_NAME, userName, userPass);
        getCleanup(REALM_NAME).addUserId(userId);

        oauth.realm(REALM_NAME);
        oauth.openLoginForm();
        assertEquals("Test user should be on the login page.", "Sign in to your account", PageUtils.getPageTitle(driver));
        loginPage.login(userName, userPass);
        assertTrue("Test user should be successfully logged in.", driver.getTitle().contains("AUTH_RESPONSE"));
        AccountHelper.logout(realm, userName);

        Optional<CredentialRepresentation> passwordCredential =
                realm.users().get(userId).credentials().stream()
                        .filter(c -> CredentialRepresentation.PASSWORD.equals(c.getType()))
                        .findFirst();
        assertTrue("Test user should have a password credential set.", passwordCredential.isPresent());
        realm.users().get(userId).removeCredential(passwordCredential.get().getId());

        oauth.openLoginForm();
        assertEquals("Test user should be on the login page.", "Sign in to your account", PageUtils.getPageTitle(driver));
        loginPage.login(userName, userPass);
        assertTrue("Test user should fail to log in after password was deleted.",
                driver.getCurrentUrl().contains(String.format("/realms/%s/login-actions/authenticate", REALM_NAME)));

        //oauth cleanup
        oauth.realm("test");
    }

    @Test
    public void testUpdateCredentials() {
        importTestRealms();

        // Get user user-with-one-configured-otp and assert he has no label linked to its OTP credential
        UserResource user = ApiUtil.findUserByUsernameId(managedRealm.admin(), "user-with-one-configured-otp");
        CredentialRepresentation otpCred = user.credentials().get(0);
        Assertions.assertNull(otpCred.getUserLabel());

        // Set and check a new label
        String newLabel = "the label";
        user.setCredentialUserLabel(otpCred.getId(), newLabel);
        Assertions.assertEquals(newLabel, user.credentials().get(0).getUserLabel());
    }

    @Test
    public void testDeleteCredentials() {
        UserResource user = ApiUtil.findUserByUsernameId(managedRealm.admin(), "john-doh@localhost");
        List<CredentialRepresentation> creds = user.credentials();
        Assertions.assertEquals(1, creds.size());
        CredentialRepresentation credPasswd = creds.get(0);
        Assertions.assertEquals("password", credPasswd.getType());

        // Remove password
        user.removeCredential(credPasswd.getId());
        Assertions.assertEquals(0, user.credentials().size());

        // Restore password
        credPasswd.setValue("password");
        user.resetPassword(credPasswd);
        Assertions.assertEquals(1, user.credentials().size());
    }

    @Test
    public void testCRUDCredentialsOfDifferentUser() {
        // Get credential ID of the OTP credential of the user1
        UserResource user1 = ApiUtil.findUserByUsernameId(managedRealm.admin(), "user-with-one-configured-otp");
        CredentialRepresentation otpCredential = user1.credentials().stream()
                .filter(credentialRep -> OTPCredentialModel.TYPE.equals(credentialRep.getType()))
                .findFirst()
                .get();

        // Test that when admin operates on user "user2", he can't update, move or remove credentials of different user "user1"
        UserResource user2 = ApiUtil.findUserByUsernameId(managedRealm.admin(), "test-user@localhost");
        try {
            user2.setCredentialUserLabel(otpCredential.getId(), "new-label");
            Assertions.fail("Not expected to successfully update user label");
        } catch (NotFoundException nfe) {
            // Expected
        }

        try {
            user2.moveCredentialToFirst(otpCredential.getId());
            Assertions.fail("Not expected to successfully move credential");
        } catch (NotFoundException nfe) {
            // Expected
        }

        try {
            user2.removeCredential(otpCredential.getId());
            Assertions.fail("Not expected to successfully remove credential");
        } catch (NotFoundException nfe) {
            // Expected
        }

        // Assert credential was not removed or updated
        CredentialRepresentation otpCredentialLoaded = user1.credentials().stream()
                .filter(credentialRep -> OTPCredentialModel.TYPE.equals(credentialRep.getType()))
                .findFirst()
                .get();
        Assertions.assertTrue(ObjectUtil.isEqualOrBothNull(otpCredential.getUserLabel(), otpCredentialLoaded.getUserLabel()));
        Assertions.assertTrue(ObjectUtil.isEqualOrBothNull(otpCredential.getPriority(), otpCredentialLoaded.getPriority()));
    }

    @Test
    public void testGetAndMoveCredentials() {
        importTestRealms();

        UserResource user = ApiUtil.findUserByUsernameId(managedRealm.admin(), "user-with-two-configured-otp");
        List<CredentialRepresentation> creds = user.credentials();
        List<String> expectedCredIds = Arrays.asList(creds.get(0).getId(), creds.get(1).getId(), creds.get(2).getId());

        // Check actual user credentials
        assertSameIds(expectedCredIds, user.credentials());

        // Move first credential after second one
        user.moveCredentialAfter(expectedCredIds.get(0), expectedCredIds.get(1));
        List<String> newOrderCredIds = Arrays.asList(expectedCredIds.get(1), expectedCredIds.get(0), expectedCredIds.get(2));
        assertSameIds(newOrderCredIds, user.credentials());

        // Move last credential in first position
        user.moveCredentialToFirst(expectedCredIds.get(2));
        newOrderCredIds = Arrays.asList(expectedCredIds.get(2), expectedCredIds.get(1), expectedCredIds.get(0));
        assertSameIds(newOrderCredIds, user.credentials());

        // Restore initial state
        user.moveCredentialToFirst(expectedCredIds.get(1));
        user.moveCredentialToFirst(expectedCredIds.get(0));
        assertSameIds(expectedCredIds, user.credentials());
    }

    @Test
    public void expectNoPasswordShownWhenCreatingUserWithPassword() throws IOException {
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue("password");

        UserRepresentation user = new UserRepresentation();
        user.setUsername("test");
        user.setCredentials(Collections.singletonList(credential));
        user.setEnabled(true);

        createUser(user, false);

        String actualRepresentation = adminEvents.poll().getRepresentation();
        assertEquals(
                JsonSerialization.writeValueAsString(user),
                actualRepresentation
        );
    }

    private void assertSameIds(List<String> expectedIds, List<CredentialRepresentation> actual) {
        Assertions.assertEquals(expectedIds.size(), actual.size());
        for (int i = 0; i < expectedIds.size(); i++) {
            Assertions.assertEquals(expectedIds.get(i), actual.get(i).getId());
        }
    }
}

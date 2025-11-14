package org.keycloak.tests.admin.user;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.util.ObjectUtil;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.AdminEventAssertion;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.tests.utils.admin.AdminApiUtil;
import org.keycloak.tests.utils.admin.AdminEventPaths;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.util.JsonSerialization;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@KeycloakIntegrationTest
public class UserCredentialTest extends AbstractUserTest {

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectUser(ref = "user-with-one-configured-otp", config = UserCredentialOtp1UserConf.class)
    ManagedUser userOtp1;

    @InjectUser(ref = "user-with-two-configured-otp", config = UserCredentialOtp2UserConf.class)
    ManagedUser userOtp2;

    @InjectUser(ref = "john-doh@localhost", config = UserCredentialJohnDohUserConf.class)
    ManagedUser johnDoh;

    @InjectUser(ref = "test-user@localhost", config = UserCredentialTestUserConf.class)
    ManagedUser testUser;

    @Test
    public void resetUserPassword() {
        UserRepresentation userRep = UserConfigBuilder.create()
                .username("user1").name("User", "One").email("user1@localhost").build();

        String userId = createUser(userRep);

        CredentialRepresentation cred = new CredentialRepresentation();
        cred.setType(CredentialRepresentation.PASSWORD);
        cred.setValue("paSSw0rd");
        cred.setTemporary(false);

        managedRealm.admin().users().get(userId).resetPassword(cred);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.ACTION, AdminEventPaths.userResetPasswordPath(userId), ResourceType.USER);

        oauth.openLoginForm();

        loginPage.assertCurrent();

        loginPage.fillLogin("user1", "paSSw0rd");
        loginPage.submit();

        assertTrue(driver.getPageSource().contains("Happy days"));

        AccountHelper.logout(managedRealm.admin(), "user1");
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
            Assertions.assertNull(adminEvents.poll());
        }
    }

    @Test
    public void loginShouldFailAfterPasswordDeleted() {
        String userName = "credential-tester";
        String userPass = "s3cr37";
        UserRepresentation userRep = UserConfigBuilder.create()
                .username(userName).password(userPass).name("credential", "tester").email("credential@tester").build();
        String userId = createUser(userRep);

        oauth.openLoginForm();
        loginPage.assertCurrent();
        loginPage.fillLogin(userName, userPass);
        loginPage.submit();
        assertTrue(driver.getPageSource().contains("Happy days"), "Test user should be successfully logged in.");
        AccountHelper.logout(managedRealm.admin(), userName);

        Optional<CredentialRepresentation> passwordCredential =
                managedRealm.admin().users().get(userId).credentials().stream()
                        .filter(c -> CredentialRepresentation.PASSWORD.equals(c.getType()))
                        .findFirst();
        assertTrue(passwordCredential.isPresent(), "Test user should have a password credential set.");
        managedRealm.admin().users().get(userId).removeCredential(passwordCredential.get().getId());

        oauth.openLoginForm();
        loginPage.assertCurrent();
        loginPage.fillLogin(userName, userPass);
        loginPage.submit();
        assertTrue(driver.getCurrentUrl().contains(String.format("/realms/%s/login-actions/authenticate", managedRealm.getName())), "Test user should fail to log in after password was deleted.");
    }

    @Test
    public void testUpdateCredentials() {
        // both credentials have a null priority - stable ordering is not guaranteed between calls
        // Get user user-with-one-configured-otp and assert he has no label linked to its OTP credential
        UserResource user = userOtp1.admin();
        CredentialRepresentation otpCred = user.credentials().stream().filter(cr -> "otp".equals(cr.getType()))
                .findFirst().orElseThrow();
        Assertions.assertNull(otpCred.getUserLabel());

        // Set and check a new label
        String newLabel = "the label";
        user.setCredentialUserLabel(otpCred.getId(), newLabel);
        Assertions.assertEquals(newLabel, user.credentials().stream().filter(cr -> cr.getId().equals(otpCred.getId()))
                .findFirst().orElseThrow().getUserLabel());
    }

    @Test
    public void testShouldFailToSetCredentialUserLabelWhenLabelIsEmpty() {
        UserResource user = userOtp1.admin();
        CredentialRepresentation otpCred = user.credentials().get(0);
        BadRequestException ex = Assertions.assertThrows(BadRequestException.class, () -> {
            user.setCredentialUserLabel(otpCred.getId(), "   ");
        });

        Response response = ex.getResponse();
        String body = response.readEntity(String.class);

        Assertions.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        Assertions.assertTrue(body.contains("missingCredentialLabel"));
        Assertions.assertTrue(body.contains("Credential label must not be empty"));
    }

    @Test
    public void testShouldFailToSetCredentialUserLabelWhenLabelAlreadyExists() {
        UserResource user = userOtp2.admin();

        List<CredentialRepresentation> credentials = user.credentials().stream()
                .filter(c -> c.getType().equals(OTPCredentialModel.TYPE))
                .toList();
        Assertions.assertEquals(2, credentials.size());

        String firstId = credentials.get(0).getId();
        String secondId = credentials.get(1).getId();

        user.setCredentialUserLabel(firstId, "Device");
        user.setCredentialUserLabel(secondId, "Second Device");

        // Attempt to update second credential to use the same label as the first
        ClientErrorException ex = Assertions.assertThrows(ClientErrorException.class, () -> {
            user.setCredentialUserLabel(secondId, "Device");
        });

        Response response = ex.getResponse();
        Assertions.assertEquals(Response.Status.CONFLICT.getStatusCode(), response.getStatus());

        String body = response.readEntity(String.class);
        Assertions.assertNotNull(body);
        Assertions.assertTrue(body.contains("Device already exists with the same name"));
    }

    @Test
    public void testDeleteCredentials() {
        UserResource user = johnDoh.admin();
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
        UserResource user1 = userOtp1.admin();
        CredentialRepresentation otpCredential = user1.credentials().stream()
                .filter(credentialRep -> OTPCredentialModel.TYPE.equals(credentialRep.getType()))
                .findFirst()
                .get();

        // Test that when admin operates on user "user2", he can't update, move or remove credentials of different user "user1"
        UserResource user2 = AdminApiUtil.findUserByUsernameId(managedRealm.admin(), testUser.getUsername());
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
        UserResource user = userOtp2.admin();
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

    private static class UserCredentialJohnDohUserConf implements UserConfig {

        @Override
        public UserConfigBuilder configure(UserConfigBuilder builder) {
            builder.username("john-doh@localhost");
            builder.password("password");
            builder.name("John", "Doh");
            builder.email("john-doh@localhost");
            builder.emailVerified(true);

            return builder;
        }
    }

    private static class UserCredentialTestUserConf implements UserConfig {

        @Override
        public UserConfigBuilder configure(UserConfigBuilder builder) {
            builder.username("test-user@localhost");
            builder.password("password");
            builder.name("Tom", "Brady");
            builder.email("test-user@localhost");
            builder.emailVerified(true);

            return builder;
        }
    }

    private static class UserCredentialOtp1UserConf implements UserConfig {

        @Override
        public UserConfigBuilder configure(UserConfigBuilder builder) {
            builder.username("user-with-one-configured-otp");
            builder.password("password");
            builder.name("Otp", "1");
            builder.email("otp1@redhat.com");
            builder.emailVerified(true);
            builder.totpSecret("DJmQfC73VGFhw7D4QJ8A");

            return builder;
        }
    }

    private static class UserCredentialOtp2UserConf implements UserConfig {

        @Override
        public UserConfigBuilder configure(UserConfigBuilder builder) {
            builder.username("user-with-two-configured-otp");
            builder.password("password");
            builder.name("Otp", "2");
            builder.email("otp2@redhat.com");
            builder.emailVerified(true);
            builder.totpSecret("DJmQfC73VGFhw7D4QJ8A");
            builder.totpSecret("ABCQfC73VGFhw7D4QJ8A");

            return builder;
        }
    }
}

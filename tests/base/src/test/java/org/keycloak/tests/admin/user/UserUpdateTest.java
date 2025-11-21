package org.keycloak.tests.admin.user;

import java.util.Arrays;
import java.util.Collections;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.NotFoundException;

import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.crypto.hash.Argon2Parameters;
import org.keycloak.crypto.hash.Argon2PasswordHashProviderFactory;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.AdminEventAssertion;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.utils.admin.AdminEventPaths;
import org.keycloak.testsuite.util.AccountHelper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@KeycloakIntegrationTest
public class UserUpdateTest extends AbstractUserTest {

    @InjectRealm(lifecycle = LifeCycle.METHOD)
    ManagedRealm managedRealm;

    @InjectOAuthClient
    OAuthClient oauth;

    @Test
    public void updateUserWithHashedCredentials() {
        UserRepresentation userRep = UserConfigBuilder.create()
                .username("user_hashed_creds").name("Hashed", "User").email("user_hashed_creds@localhost").build();

        String userId = createUser(userRep);

        byte[] salt = new byte[]{-69, 85, 87, 99, 26, -107, 125, 99, -77, 30, -111, 118, 108, 100, -117, -56};

        PasswordCredentialModel credentialModel = PasswordCredentialModel.createFromValues("pbkdf2-sha256", salt,
                27500, "uskEPZWMr83pl2mzNB95SFXfIabe2UH9ClENVx/rrQqOjFEjL2aAOGpWsFNNF3qoll7Qht2mY5KxIDm3Rnve2w==");
        credentialModel.setCreatedDate(1001L);
        CredentialRepresentation hashedPassword = ModelToRepresentation.toRepresentation(credentialModel);

        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setCredentials(Collections.singletonList(hashedPassword));

        managedRealm.admin().users().get(userId).update(userRepresentation);

        oauth.openLoginForm();

        loginPage.assertCurrent();

        loginPage.fillLogin("user_hashed_creds", "admin");
        loginPage.submit();

        assertTrue(driver.getPageSource().contains("Happy days"));

        AccountHelper.logout(managedRealm.admin(), "user_hashed_creds");
    }

    @Test
    public void updateUserWithNewUsername() {
        switchEditUsernameAllowedOn(true);

        String id = createUser();

        UserResource user = managedRealm.admin().users().get(id);
        UserRepresentation userRep = user.toRepresentation();
        userRep.setUsername("user11");
        updateUser(user, userRep);

        userRep = managedRealm.admin().users().get(id).toRepresentation();
        Assertions.assertEquals("user11", userRep.getUsername());
    }

    @Test
    public void updateUserWithoutUsername() {
        switchEditUsernameAllowedOn(true);

        String id = createUser();

        UserResource user = managedRealm.admin().users().get(id);

        UserRepresentation rep = new UserRepresentation();
        rep.setFirstName("Firstname");

        user.update(rep);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.UPDATE, AdminEventPaths.userResourcePath(id), rep, ResourceType.USER);

        rep = new UserRepresentation();
        rep.setLastName("Lastname");

        user.update(rep);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.UPDATE, AdminEventPaths.userResourcePath(id), rep, ResourceType.USER);

        rep = managedRealm.admin().users().get(id).toRepresentation();

        Assertions.assertEquals("user1", rep.getUsername());
        Assertions.assertEquals("user1@localhost", rep.getEmail());
        Assertions.assertEquals("Firstname", rep.getFirstName());
        Assertions.assertEquals("Lastname", rep.getLastName());
    }

    @Test
    public void updateUserWithEmailAsUsernameEditUsernameDisabled() {
        switchRegistrationEmailAsUsername(true);
        RealmRepresentation rep = managedRealm.admin().toRepresentation();
        assertFalse(rep.isEditUsernameAllowed());
        String id = createUser();

        UserResource user = managedRealm.admin().users().get(id);
        UserRepresentation userRep = user.toRepresentation();
        Assertions.assertEquals("user1@localhost", userRep.getUsername());

        userRep.setEmail("user11@localhost");
        updateUser(user, userRep);

        userRep = managedRealm.admin().users().get(id).toRepresentation();
        Assertions.assertEquals("user11@localhost", userRep.getUsername());
        Assertions.assertEquals("user11@localhost", userRep.getEmail());
    }

    @Test
    public void updateUserWithEmailAsUsernameEditUsernameAllowed() {
        switchRegistrationEmailAsUsername(true);
        switchEditUsernameAllowedOn(true);

        String id = createUser();
        UserResource user = managedRealm.admin().users().get(id);
        UserRepresentation userRep = user.toRepresentation();
        Assertions.assertEquals("user1@localhost", userRep.getUsername());

        userRep.setEmail("user11@localhost");
        updateUser(user, userRep);

        userRep = managedRealm.admin().users().get(id).toRepresentation();
        Assertions.assertEquals("user11@localhost", userRep.getUsername());
        Assertions.assertEquals("user11@localhost", userRep.getEmail());
    }

    @Test
    public void updateUserWithExistingEmail() {
        final String userId = createUser();
        assertNotNull(userId);
        assertNotNull(createUser("user2", "user2@localhost"));

        UserResource user = managedRealm.admin().users().get(userId);
        UserRepresentation userRep = user.toRepresentation();
        assertNotNull(userRep);
        userRep.setEmail("user2@localhost");

        try {
            updateUser(user, userRep);
            fail("Expected failure - Email conflict");
        } catch (ClientErrorException e) {
            assertNotNull(e.getResponse());
            assertThat(e.getResponse().getStatus(), is(409));

            ErrorRepresentation error = e.getResponse().readEntity(ErrorRepresentation.class);
            Assertions.assertEquals("User exists with same email", error.getErrorMessage());
            Assertions.assertNull(adminEvents.poll());
        }
    }

    @Test
    public void updateUserWithNewUsernameNotPossible() {
        RealmRepresentation realmRep = managedRealm.admin().toRepresentation();
        assertFalse(realmRep.isEditUsernameAllowed());
        String id = createUser();

        UserResource user = managedRealm.admin().users().get(id);
        UserRepresentation userRep = user.toRepresentation();
        userRep.setUsername("user11");

        try {
            updateUser(user, userRep);
            fail("Should fail because realm does not allow edit username");
        } catch (BadRequestException expected) {
            ErrorRepresentation error = expected.getResponse().readEntity(ErrorRepresentation.class);
            Assertions.assertEquals("error-user-attribute-read-only", error.getErrorMessage());
        }

        userRep = managedRealm.admin().users().get(id).toRepresentation();
        Assertions.assertEquals("user1", userRep.getUsername());
    }

    @Test
    public void updateUserWithNewUsernameAccessingViaOldUsername() {
        switchEditUsernameAllowedOn(true);
        createUser();

        try {
            UserResource user = managedRealm.admin().users().get("user1");

            UserRepresentation userRep = user.toRepresentation();
            userRep.setUsername("user1");
            updateUser(user, userRep);

            managedRealm.admin().users().get("user11").toRepresentation();
            fail("Expected failure");
        } catch (ClientErrorException e) {
            Assertions.assertEquals(404, e.getResponse().getStatus());
            Assertions.assertNull(adminEvents.poll());
        } finally {
            switchEditUsernameAllowedOn(false);
        }
    }

    @Test
    public void updateUserWithExistingUsername() {
        switchEditUsernameAllowedOn(true);
        enableBruteForce(true);
        createUser();

        UserRepresentation userRep = new UserRepresentation();
        userRep.setUsername("user2");

        String createdId = createUser(userRep);

        try {
            UserResource user = managedRealm.admin().users().get(createdId);
            userRep = user.toRepresentation();
            userRep.setUsername("user1");
            user.update(userRep);
            fail("Expected failure");
        } catch (ClientErrorException e) {
            Assertions.assertEquals(409, e.getResponse().getStatus());

            Assertions.assertNull(adminEvents.poll());
        } finally {
            enableBruteForce(false);
            switchEditUsernameAllowedOn(false);
        }
    }

    @Test
    public void updateUserWithRawCredentials() {
        UserRepresentation user = new UserRepresentation();
        user.setUsername("user_rawpw");
        user.setEmail("email.raw@localhost");

        CredentialRepresentation rawPassword = new CredentialRepresentation();
        rawPassword.setValue("ABCD");
        rawPassword.setType(CredentialRepresentation.PASSWORD);
        user.setCredentials(Arrays.asList(rawPassword));

        String id = createUser(user);

        PasswordCredentialModel credential = PasswordCredentialModel
                .createFromCredentialModel(fetchCredentials("user_rawpw"));
        assertNotNull(credential, "Expecting credential");
        assertEquals(Argon2PasswordHashProviderFactory.ID, credential.getPasswordCredentialData().getAlgorithm());
        assertEquals(Argon2Parameters.DEFAULT_ITERATIONS, credential.getPasswordCredentialData().getHashIterations());
        assertNotEquals("ABCD", credential.getPasswordSecretData().getValue());
        Assertions.assertEquals(CredentialRepresentation.PASSWORD, credential.getType());

        UserResource userResource = managedRealm.admin().users().get(id);
        UserRepresentation userRep = userResource.toRepresentation();

        CredentialRepresentation rawPasswordForUpdate = new CredentialRepresentation();
        rawPasswordForUpdate.setValue("EFGH");
        rawPasswordForUpdate.setType(CredentialRepresentation.PASSWORD);
        userRep.setCredentials(Arrays.asList(rawPasswordForUpdate));

        updateUser(userResource, userRep);

        PasswordCredentialModel updatedCredential = PasswordCredentialModel
                .createFromCredentialModel(fetchCredentials("user_rawpw"));
        assertNotNull(updatedCredential, "Expecting credential");
        assertEquals(Argon2PasswordHashProviderFactory.ID, updatedCredential.getPasswordCredentialData().getAlgorithm());
        assertEquals(Argon2Parameters.DEFAULT_ITERATIONS, updatedCredential.getPasswordCredentialData().getHashIterations());
        assertNotEquals("EFGH", updatedCredential.getPasswordSecretData().getValue());
        Assertions.assertEquals(CredentialRepresentation.PASSWORD, updatedCredential.getType());
    }

    @Test
    public void testAccessUserFromOtherRealm() {
        RealmRepresentation firstRealm = new RealmRepresentation();

        firstRealm.setRealm("first-realm");

        adminClient.realms().create(firstRealm);

        UserRepresentation firstUser = new UserRepresentation();

        firstUser.setUsername("first");
        firstUser.setEmail("first@first-realm.org");

        firstUser.setId(ApiUtil.getCreatedId(adminClient.realm(firstRealm.getRealm()).users().create(firstUser)));

        RealmRepresentation secondRealm = new RealmRepresentation();

        secondRealm.setRealm("second-realm");

        adminClient.realms().create(secondRealm);

        adminClient.realm(firstRealm.getRealm()).users().get(firstUser.getId()).update(firstUser);

        try {
            adminClient.realm(secondRealm.getRealm()).users().get(firstUser.getId()).toRepresentation();
            fail("Should not have access to firstUser from another realm");
        } catch (NotFoundException nfe) {
            // ignore
        } finally {
            adminClient.realm(secondRealm.getRealm()).remove();
            adminClient.realm(firstRealm.getRealm()).remove();
        }
    }

    private void enableBruteForce(boolean enable) {
        RealmRepresentation rep = managedRealm.admin().toRepresentation();
        managedRealm.cleanup().add(r -> r.update(rep));
        rep.setBruteForceProtected(enable);
        managedRealm.admin().update(rep);
        AdminEventAssertion.assertSuccess(adminEvents.poll()).operationType(OperationType.UPDATE).representation(rep).resourceType(ResourceType.REALM);
    }
}

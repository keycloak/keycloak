package org.keycloak.tests.admin.user;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.NotFoundException;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.AdminEventAssertion;
import org.keycloak.tests.utils.admin.AdminEventPaths;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class UserUpdateTest extends AbstractUserTest {

    @Test
    public void updateUserWithHashedCredentials() {
        String userId = createUser("user_hashed_creds", "user_hashed_creds@localhost");

        byte[] salt = new byte[]{-69, 85, 87, 99, 26, -107, 125, 99, -77, 30, -111, 118, 108, 100, -117, -56};

        PasswordCredentialModel credentialModel = PasswordCredentialModel.createFromValues("pbkdf2-sha256", salt,
                27500, "uskEPZWMr83pl2mzNB95SFXfIabe2UH9ClENVx/rrQqOjFEjL2aAOGpWsFNNF3qoll7Qht2mY5KxIDm3Rnve2w==");
        credentialModel.setCreatedDate(1001l);
        CredentialRepresentation hashedPassword = ModelToRepresentation.toRepresentation(credentialModel);

        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setCredentials(Collections.singletonList(hashedPassword));

        managedRealm.admin().users().get(userId).update(userRepresentation);

        oauth.openLoginForm();

        assertEquals("Sign in to your account", driver.getTitle());

        loginPage.login("user_hashed_creds", "admin");

        assertTrue(driver.getTitle().contains("AUTH_RESPONSE"));

        // oauth cleanup
        oauth.realm("test");
    }

    @Test
    public void updateUserWithNewUsername() {
        switchEditUsernameAllowedOn(true);
        getCleanup().addCleanup(() -> switchEditUsernameAllowedOn(false));

        String id = createUser();

        UserResource user = realm.users().get(id);
        UserRepresentation userRep = user.toRepresentation();
        userRep.setUsername("user11");
        updateUser(user, userRep);

        userRep = realm.users().get(id).toRepresentation();
        Assert.assertEquals("user11", userRep.getUsername());
    }

    @Test
    public void updateUserWithoutUsername() {
        switchEditUsernameAllowedOn(true);
        getCleanup().addCleanup(() -> switchEditUsernameAllowedOn(false));

        String id = createUser();

        UserResource user = realm.users().get(id);

        UserRepresentation rep = new UserRepresentation();
        rep.setFirstName("Firstname");

        user.update(rep);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.UPDATE, AdminEventPaths.userResourcePath(id), rep, ResourceType.USER);

        rep = new UserRepresentation();
        rep.setLastName("Lastname");

        user.update(rep);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.UPDATE, AdminEventPaths.userResourcePath(id), rep, ResourceType.USER);

        rep = realm.users().get(id).toRepresentation();

        Assert.assertEquals("user1", rep.getUsername());
        Assert.assertEquals("user1@localhost", rep.getEmail());
        Assert.assertEquals("Firstname", rep.getFirstName());
        Assert.assertEquals("Lastname", rep.getLastName());
    }

    @Test
    public void updateUserWithEmailAsUsernameEditUsernameDisabled() {
        switchRegistrationEmailAsUsername(true);
        getCleanup().addCleanup(() -> switchRegistrationEmailAsUsername(false));
        RealmRepresentation rep = realm.toRepresentation();
        assertFalse(rep.isEditUsernameAllowed());
        String id = createUser();

        UserResource user = realm.users().get(id);
        UserRepresentation userRep = user.toRepresentation();
        Assert.assertEquals("user1@localhost", userRep.getUsername());

        userRep.setEmail("user11@localhost");
        updateUser(user, userRep);

        userRep = realm.users().get(id).toRepresentation();
        Assert.assertEquals("user11@localhost", userRep.getUsername());
        Assert.assertEquals("user11@localhost", userRep.getEmail());
    }

    @Test
    public void updateUserWithEmailAsUsernameEditUsernameAllowed() {
        switchRegistrationEmailAsUsername(true);
        getCleanup().addCleanup(() -> switchRegistrationEmailAsUsername(false));
        switchEditUsernameAllowedOn(true);
        getCleanup().addCleanup(() -> switchEditUsernameAllowedOn(false));

        String id = createUser();
        UserResource user = realm.users().get(id);
        UserRepresentation userRep = user.toRepresentation();
        Assert.assertEquals("user1@localhost", userRep.getUsername());

        userRep.setEmail("user11@localhost");
        updateUser(user, userRep);

        userRep = realm.users().get(id).toRepresentation();
        Assert.assertEquals("user11@localhost", userRep.getUsername());
        Assert.assertEquals("user11@localhost", userRep.getEmail());
    }

    @Test
    public void updateUserWithExistingEmail() {
        final String userId = createUser();
        assertNotNull(userId);
        assertNotNull(createUser("user2", "user2@localhost"));

        UserResource user = realm.users().get(userId);
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
            assertAdminEvents.assertEmpty();
        }
    }

    @Test
    public void updateUserWithNewUsernameNotPossible() {
        RealmRepresentation realmRep = realm.toRepresentation();
        assertFalse(realmRep.isEditUsernameAllowed());
        String id = createUser();

        UserResource user = realm.users().get(id);
        UserRepresentation userRep = user.toRepresentation();
        userRep.setUsername("user11");

        try {
            updateUser(user, userRep);
            fail("Should fail because realm does not allow edit username");
        } catch (BadRequestException expected) {
            ErrorRepresentation error = expected.getResponse().readEntity(ErrorRepresentation.class);
            Assert.assertEquals("error-user-attribute-read-only", error.getErrorMessage());
        }

        userRep = realm.users().get(id).toRepresentation();
        Assert.assertEquals("user1", userRep.getUsername());
    }

    @Test
    public void updateUserWithNewUsernameAccessingViaOldUsername() {
        switchEditUsernameAllowedOn(true);
        createUser();

        try {
            UserResource user = realm.users().get("user1");

            userRep = user.toRepresentation();
            userRep.setUsername("user1");
            updateUser(user, userRep);

            realm.users().get("user11").toRepresentation();
            fail("Expected failure");
        } catch (ClientErrorException e) {
            Assert.assertEquals(404, e.getResponse().getStatus());
            assertAdminEvents.assertEmpty();
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
            UserResource user = realm.users().get(createdId);
            userRep = user.toRepresentation();
            userRep.setUsername("user1");
            user.update(userRep);
            fail("Expected failure");
        } catch (ClientErrorException e) {
            Assert.assertEquals(409, e.getResponse().getStatus());

            assertAdminEvents.assertEmpty();
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
        assertNotNull("Expecting credential", credential);
        assertEquals(DefaultPasswordHash.getDefaultAlgorithm(), credential.getPasswordCredentialData().getAlgorithm());
        assertEquals(DefaultPasswordHash.getDefaultIterations(), credential.getPasswordCredentialData().getHashIterations());
        assertNotEquals("ABCD", credential.getPasswordSecretData().getValue());
        Assert.assertEquals(CredentialRepresentation.PASSWORD, credential.getType());

        UserResource userResource = realm.users().get(id);
        UserRepresentation userRep = userResource.toRepresentation();

        CredentialRepresentation rawPasswordForUpdate = new CredentialRepresentation();
        rawPasswordForUpdate.setValue("EFGH");
        rawPasswordForUpdate.setType(CredentialRepresentation.PASSWORD);
        userRep.setCredentials(Arrays.asList(rawPasswordForUpdate));

        updateUser(userResource, userRep);

        PasswordCredentialModel updatedCredential = PasswordCredentialModel
                .createFromCredentialModel(fetchCredentials("user_rawpw"));
        assertNotNull("Expecting credential", updatedCredential);
        assertEquals(DefaultPasswordHash.getDefaultAlgorithm(), updatedCredential.getPasswordCredentialData().getAlgorithm());
        assertEquals(DefaultPasswordHash.getDefaultIterations(), updatedCredential.getPasswordCredentialData().getHashIterations());
        assertNotEquals("EFGH", updatedCredential.getPasswordSecretData().getValue());
        Assert.assertEquals(CredentialRepresentation.PASSWORD, updatedCredential.getType());
    }

    @Test
    public void testAccessUserFromOtherRealm() {
        RealmRepresentation firstRealm = new RealmRepresentation();

        firstRealm.setRealm("first-realm");

        adminClient.realms().create(firstRealm);
        getCleanup().addCleanup(new AutoCloseable() {
            @Override
            public void close() throws Exception {
                adminClient.realms().realm(firstRealm.getRealm()).remove();
            }
        });

        realm = adminClient.realm(firstRealm.getRealm());
        realmId = realm.toRepresentation().getId();

        UserRepresentation firstUser = new UserRepresentation();

        firstUser.setUsername("first");
        firstUser.setEmail("first@first-realm.org");

        firstUser.setId(createUser(firstUser, false));

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
        }
    }

    private void enableBruteForce(boolean enable) {
        RealmRepresentation rep = realm.toRepresentation();
        rep.setBruteForceProtected(enable);
        realm.update(rep);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.UPDATE, Matchers.nullValue(String.class), rep, ResourceType.REALM);
    }
}

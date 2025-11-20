package org.keycloak.tests.admin.user;

import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.credential.CredentialModel;
import org.keycloak.crypto.hash.Argon2Parameters;
import org.keycloak.crypto.hash.Argon2PasswordHashProviderFactory;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.Constants;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.testframework.admin.AdminClientFactory;
import org.keycloak.testframework.annotations.InjectAdminClientFactory;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.AdminEventAssertion;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RoleConfigBuilder;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.utils.Assert;
import org.keycloak.tests.utils.admin.AdminEventPaths;
import org.keycloak.testsuite.federation.DummyUserFederationProviderFactory;
import org.keycloak.util.JsonSerialization;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.endsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@KeycloakIntegrationTest(config = UserCreateTest.UserCreateServerConf.class)
public class UserCreateTest extends AbstractUserTest {

    @InjectRealm(lifecycle = LifeCycle.METHOD)
    ManagedRealm managedRealm;

    @InjectAdminClientFactory
    AdminClientFactory clientFactory;

    @Test
    public void verifyCreateUser() {
        createUser();
    }

    /**
     * See KEYCLOAK-11003
     */
    @Test
    public void createUserWithTemporaryPasswordWithAdditionalPasswordUpdateShouldRemoveUpdatePasswordRequiredAction() {

        String userId = createUser();

        CredentialRepresentation credTmp = new CredentialRepresentation();
        credTmp.setType(CredentialRepresentation.PASSWORD);
        credTmp.setValue("temp");
        credTmp.setTemporary(Boolean.TRUE);

        managedRealm.admin().users().get(userId).resetPassword(credTmp);

        CredentialRepresentation credPerm = new CredentialRepresentation();
        credPerm.setType(CredentialRepresentation.PASSWORD);
        credPerm.setValue("perm");
        credPerm.setTemporary(null);

        managedRealm.admin().users().get(userId).resetPassword(credPerm);

        UserRepresentation userRep = managedRealm.admin().users().get(userId).toRepresentation();

        Assertions.assertFalse(userRep.getRequiredActions().contains(UserModel.RequiredAction.UPDATE_PASSWORD.name()));
    }

    @Test
    public void createDuplicatedUser1() {
        createUser();

        UserRepresentation user = new UserRepresentation();
        user.setUsername("user1");
        try (Response response = managedRealm.admin().users().create(user)) {
            assertEquals(409, response.getStatus());
            Assertions.assertNull(adminEvents.poll());

            // Just to show how to retrieve underlying error message
            ErrorRepresentation error = response.readEntity(ErrorRepresentation.class);
            Assertions.assertEquals("User exists with same username", error.getErrorMessage());
        }
    }

    @Test
    public void createDuplicatedUser2() {
        createUser();

        UserRepresentation user = new UserRepresentation();
        user.setUsername("user2");
        user.setEmail("user1@localhost");

        try (Response response = managedRealm.admin().users().create(user)) {
            assertEquals(409, response.getStatus());
            assertNull(adminEvents.poll());

            // Alternative way of showing underlying error message
            try {
                CreatedResponseUtil.getCreatedId(response);
                Assertions.fail("Not expected getCreatedId to success");
            } catch (WebApplicationException wae) {
                MatcherAssert.assertThat(wae.getMessage(), endsWith("ErrorMessage: User exists with same email"));
            }
        }
    }

    @Test
    public void createDuplicatedUsernameWithEmail() {
        createUser("user1@local.com", "user1@local.org");

        UserRepresentation user = new UserRepresentation();
        user.setUsername("user1@local.org");
        user.setEmail("user2@localhost");
        try (Response response = managedRealm.admin().users().create(user)) {
            assertEquals(409, response.getStatus());
            assertNull(adminEvents.poll());

            ErrorRepresentation error = response.readEntity(ErrorRepresentation.class);
            Assertions.assertEquals("User exists with same username", error.getErrorMessage());
        }
    }

    @Test
    public void createDuplicatedEmailWithUsername() {
        createUser("user1@local.com", "user1@local.org");

        UserRepresentation user = new UserRepresentation();
        user.setUsername("user2");
        user.setEmail("user1@local.com");

        try (Response response = managedRealm.admin().users().create(user)) {
            assertEquals(409, response.getStatus());
            assertNull(adminEvents.poll());

            ErrorRepresentation error = response.readEntity(ErrorRepresentation.class);
            Assertions.assertEquals("User exists with same email", error.getErrorMessage());
        }
    }

    //KEYCLOAK-14611
    @Test
    public void createDuplicateEmailWithExistingDuplicates() {
        //Allow duplicate emails
        RealmRepresentation rep = managedRealm.admin().toRepresentation();
        rep.setDuplicateEmailsAllowed(true);
        managedRealm.admin().update(rep);

        //Create 2 users with the same email
        UserRepresentation user = new UserRepresentation();
        user.setEmail("user1@localhost");
        user.setUsername("user1");
        createUser(user, false);
        user.setUsername("user2");
        createUser(user, false);

        //Disallow duplicate emails
        rep.setDuplicateEmailsAllowed(false);
        managedRealm.admin().update(rep);

        //Create a third user with the same email
        user.setUsername("user3");
        adminEvents.clear();

        try (Response response = managedRealm.admin().users().create(user)) {
            assertEquals(409, response.getStatus());
            ErrorRepresentation error = response.readEntity(ErrorRepresentation.class);
            Assertions.assertEquals("User exists with same username or email", error.getErrorMessage());
            assertNull(adminEvents.poll());
        }
    }

    @Test
    public void createUserWithHashedCredentials() {
        UserRepresentation user = new UserRepresentation();
        user.setUsername("user_creds");
        user.setEmail("email@localhost");

        PasswordCredentialModel pcm = PasswordCredentialModel.createFromValues("my-algorithm", "theSalt".getBytes(), 22, "ABC");
        CredentialRepresentation hashedPassword = ModelToRepresentation.toRepresentation(pcm);
        hashedPassword.setCreatedDate(1001L);
        hashedPassword.setUserLabel("deviceX");
        hashedPassword.setType(CredentialRepresentation.PASSWORD);

        user.setCredentials(Arrays.asList(hashedPassword));

        createUser(user);

        CredentialModel credentialHashed = fetchCredentials("user_creds");
        PasswordCredentialModel pcmh = PasswordCredentialModel.createFromCredentialModel(credentialHashed);
        assertNotNull(credentialHashed, "Expecting credential");
        assertEquals("my-algorithm", pcmh.getPasswordCredentialData().getAlgorithm());
        assertEquals(Long.valueOf(1001), credentialHashed.getCreatedDate());
        assertEquals("deviceX", credentialHashed.getUserLabel());
        assertEquals(22, pcmh.getPasswordCredentialData().getHashIterations());
        assertEquals("ABC", pcmh.getPasswordSecretData().getValue());
        assertEquals("theSalt", new String(pcmh.getPasswordSecretData().getSalt()));
        assertEquals(CredentialRepresentation.PASSWORD, credentialHashed.getType());
    }


    @Test
    public void createUserWithDeprecatedCredentialsFormat() throws IOException {
        UserRepresentation user = new UserRepresentation();
        user.setUsername("user_creds");
        user.setEmail("email@localhost");

        PasswordCredentialModel pcm = PasswordCredentialModel.createFromValues("my-algorithm", "theSalt".getBytes(), 22, "ABC");
        //CredentialRepresentation hashedPassword = ModelToRepresentation.toRepresentation(pcm);
        String deprecatedCredential = "{\n" +
                "      \"type\" : \"password\",\n" +
                "      \"hashedSaltedValue\" : \"" + pcm.getPasswordSecretData().getValue() + "\",\n" +
                "      \"salt\" : \"" + Base64.getEncoder().encodeToString(pcm.getPasswordSecretData().getSalt()) + "\",\n" +
                "      \"hashIterations\" : " + pcm.getPasswordCredentialData().getHashIterations() + ",\n" +
                "      \"algorithm\" : \"" + pcm.getPasswordCredentialData().getAlgorithm() + "\"\n" +
                "    }";

        CredentialRepresentation deprecatedHashedPassword = JsonSerialization.readValue(deprecatedCredential, CredentialRepresentation.class);
        Assertions.assertNotNull(deprecatedHashedPassword.getHashedSaltedValue());
        Assertions.assertNull(deprecatedHashedPassword.getCredentialData());

        deprecatedHashedPassword.setCreatedDate(1001l);
        deprecatedHashedPassword.setUserLabel("deviceX");
        deprecatedHashedPassword.setType(CredentialRepresentation.PASSWORD);

        user.setCredentials(Arrays.asList(deprecatedHashedPassword));

        createUser(user, false);

        CredentialModel credentialHashed = fetchCredentials("user_creds");
        PasswordCredentialModel pcmh = PasswordCredentialModel.createFromCredentialModel(credentialHashed);
        assertNotNull(credentialHashed, "Expecting credential");
        assertEquals("my-algorithm", pcmh.getPasswordCredentialData().getAlgorithm());
        assertEquals(Long.valueOf(1001), credentialHashed.getCreatedDate());
        assertEquals("deviceX", credentialHashed.getUserLabel());
        assertEquals(22, pcmh.getPasswordCredentialData().getHashIterations());
        assertEquals("ABC", pcmh.getPasswordSecretData().getValue());
        assertEquals("theSalt", new String(pcmh.getPasswordSecretData().getSalt()));
        assertEquals(CredentialRepresentation.PASSWORD, credentialHashed.getType());
    }

    @Test
    public void createUserWithTemporaryCredentials() {
        UserRepresentation user = new UserRepresentation();
        user.setUsername("user_temppw");
        user.setEmail("email.temppw@localhost");

        CredentialRepresentation password = new CredentialRepresentation();
        password.setValue("password");
        password.setType(CredentialRepresentation.PASSWORD);
        password.setTemporary(true);
        user.setCredentials(Arrays.asList(password));

        String userId = createUser(user);

        UserRepresentation userRep = managedRealm.admin().users().get(userId).toRepresentation();
        Assertions.assertEquals(1, userRep.getRequiredActions().size());
        Assertions.assertEquals(UserModel.RequiredAction.UPDATE_PASSWORD.toString(), userRep.getRequiredActions().get(0));
    }

    @Test
    public void createUserWithRawCredentials() {
        UserRepresentation user = new UserRepresentation();
        user.setUsername("user_rawpw");
        user.setEmail("email.raw@localhost");

        CredentialRepresentation rawPassword = new CredentialRepresentation();
        rawPassword.setValue("ABCD");
        rawPassword.setType(CredentialRepresentation.PASSWORD);
        user.setCredentials(Arrays.asList(rawPassword));

        createUser(user);

        CredentialModel credential = fetchCredentials("user_rawpw");
        assertNotNull(credential, "Expecting credential");
        PasswordCredentialModel pcm = PasswordCredentialModel.createFromCredentialModel(credential);
        assertEquals(Argon2PasswordHashProviderFactory.ID, pcm.getPasswordCredentialData().getAlgorithm());
        assertEquals(Argon2Parameters.DEFAULT_ITERATIONS, pcm.getPasswordCredentialData().getHashIterations());
        assertNotEquals("ABCD", pcm.getPasswordSecretData().getValue());
        assertEquals(CredentialRepresentation.PASSWORD, credential.getType());
    }

    @Test
    public void createDuplicatedUser3() {
        createUser();

        UserRepresentation user = new UserRepresentation();
        user.setUsername("User1");

        try (Response response = managedRealm.admin().users().create(user)) {
            assertEquals(409, response.getStatus());
            Assertions.assertNull(adminEvents.poll());
        }
    }

    @Test
    public void createDuplicatedUser4() {
        createUser();

        UserRepresentation user = new UserRepresentation();
        user.setUsername("USER1");

        try (Response response = managedRealm.admin().users().create(user)) {
            assertEquals(409, response.getStatus());
            Assertions.assertNull(adminEvents.poll());
        }
    }

    @Test
    public void createDuplicatedUser5() {
        createUser();

        UserRepresentation user = new UserRepresentation();
        user.setUsername("user2");
        user.setEmail("User1@localhost");

        try (Response response = managedRealm.admin().users().create(user)) {
            assertEquals(409, response.getStatus());
            Assertions.assertNull(adminEvents.poll());
        }
    }

    @Test
    public void createDuplicatedUser6() {
        createUser();

        UserRepresentation user = new UserRepresentation();
        user.setUsername("user2");
        user.setEmail("user1@LOCALHOST");

        try (Response response = managedRealm.admin().users().create(user)) {
            assertEquals(409, response.getStatus());
            Assertions.assertNull(adminEvents.poll());
        }
    }

    @Test
    public void createDuplicatedUser7() {
        createUser("user1", "USer1@Localhost");

        UserRepresentation user = new UserRepresentation();
        user.setUsername("user2");
        user.setEmail("user1@localhost");

        try (Response response = managedRealm.admin().users().create(user)) {
            assertEquals(409, response.getStatus());
            Assertions.assertNull(adminEvents.poll());
        }
    }

    // KEYCLOAK-7015
    @Test
    public void createTwoUsersWithEmptyStringEmails() {
        createUser("user1", "");
        createUser("user2", "");
    }

    @Test
    public void createUserWithFederationLink() {

        // add a dummy federation provider
        ComponentRepresentation dummyFederationProvider = new ComponentRepresentation();
        String componentId = KeycloakModelUtils.generateId();
        dummyFederationProvider.setId(componentId);
        dummyFederationProvider.setName(DummyUserFederationProviderFactory.PROVIDER_NAME);
        dummyFederationProvider.setProviderId(DummyUserFederationProviderFactory.PROVIDER_NAME);
        dummyFederationProvider.setProviderType(UserStorageProvider.class.getName());
        managedRealm.admin().components().add(dummyFederationProvider);

        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.componentPath(componentId), dummyFederationProvider, ResourceType.COMPONENT);

        UserRepresentation user = new UserRepresentation();
        user.setUsername("user1");
        user.setEmail("user1@localhost");
        user.setFederationLink(componentId);

        String userId = createUser(user);

        // fetch user again and see federation link filled in
        UserRepresentation createdUser = managedRealm.admin().users().get(userId).toRepresentation();
        assertNotNull(createdUser);
        assertEquals(user.getFederationLink(), createdUser.getFederationLink());
    }

    @Test
    public void createUserWithoutUsername() {
        UserRepresentation user = new UserRepresentation();
        user.setEmail("user1@localhost");

        try (Response response = managedRealm.admin().users().create(user)) {
            assertEquals(400, response.getStatus());
            ErrorRepresentation error = response.readEntity(ErrorRepresentation.class);
            Assertions.assertEquals("User name is missing", error.getErrorMessage());
            Assertions.assertNull(adminEvents.poll());
        }
    }

    @Test
    public void createUserWithEmailAsUsername() {
        switchRegistrationEmailAsUsername(true);
        switchEditUsernameAllowedOn(false);
        String id = createUser();
        UserResource user = managedRealm.admin().users().get(id);
        UserRepresentation userRep = user.toRepresentation();
        assertEquals("user1@localhost", userRep.getEmail());
        assertEquals(userRep.getEmail(), userRep.getUsername());
        deleteUser(id);

        switchRegistrationEmailAsUsername(true);
        switchEditUsernameAllowedOn(true);
        id = createUser();
        user = managedRealm.admin().users().get(id);
        userRep = user.toRepresentation();
        assertEquals("user1@localhost", userRep.getEmail());
        assertEquals(userRep.getEmail(), userRep.getUsername());
        deleteUser(id);

        switchRegistrationEmailAsUsername(false);
        switchEditUsernameAllowedOn(true);
        id = createUser();
        user = managedRealm.admin().users().get(id);
        userRep = user.toRepresentation();
        assertEquals("user1", userRep.getUsername());
        assertEquals("user1@localhost", userRep.getEmail());
        deleteUser(id);

        switchRegistrationEmailAsUsername(false);
        switchEditUsernameAllowedOn(false);
        id = createUser();
        user = managedRealm.admin().users().get(id);
        userRep = user.toRepresentation();
        assertEquals("user1", userRep.getUsername());
        assertEquals("user1@localhost", userRep.getEmail());
    }

    @Test
    public void createUserWithEmptyUsername() {
        UserRepresentation user = new UserRepresentation();
        user.setUsername("");
        user.setEmail("user2@localhost");

        try (Response response = managedRealm.admin().users().create(user)) {
            assertEquals(400, response.getStatus());
            ErrorRepresentation error = response.readEntity(ErrorRepresentation.class);
            Assertions.assertEquals("User name is missing", error.getErrorMessage());
            Assertions.assertNull(adminEvents.poll());
        }
    }

    @Test
    public void createUserWithInvalidPolicyPassword() {
        RealmRepresentation rep = managedRealm.admin().toRepresentation();
        String passwordPolicy = rep.getPasswordPolicy();
        rep.setPasswordPolicy("length(8)");
        managedRealm.admin().update(rep);
        UserRepresentation user = new UserRepresentation();
        user.setUsername("user4");
        user.setEmail("user4@localhost");
        CredentialRepresentation rawPassword = new CredentialRepresentation();
        rawPassword.setValue("ABCD");
        rawPassword.setType(CredentialRepresentation.PASSWORD);
        user.setCredentials(Collections.singletonList(rawPassword));
        adminEvents.clear();

        try (Response response = managedRealm.admin().users().create(user)) {
            assertEquals(400, response.getStatus());
            OAuth2ErrorRepresentation error = response.readEntity(OAuth2ErrorRepresentation.class);
            Assertions.assertEquals("invalidPasswordMinLengthMessage", error.getError());
            Assertions.assertEquals("Invalid password: minimum length 8.", error.getErrorDescription());
            rep.setPasswordPolicy(passwordPolicy);
            Assertions.assertNull(adminEvents.poll());
            managedRealm.admin().update(rep);
        }
    }

    @Test
    public void createUserWithCreateTimestamp() {
        UserRepresentation user = new UserRepresentation();
        user.setUsername("user1");
        user.setEmail("user1@localhost");
        Long createdTimestamp = 1695238476L;
        user.setCreatedTimestamp(createdTimestamp);

        String userId = createUser(user);

        // fetch user again and see created timestamp filled in
        UserRepresentation createdUser = managedRealm.admin().users().get(userId).toRepresentation();
        assertNotNull(createdUser);
        assertEquals(user.getCreatedTimestamp(), createdUser.getCreatedTimestamp());
    }

    @Test
    public void failCreateUserUsingRegularUser() throws Exception {
        managedRealm.admin().users().create(UserConfigBuilder.create().username("regular-user").password("password").email("regular@local").name("Regular", "User").build());

        try (Keycloak localAdminClient = clientFactory.create()
                .realm(managedRealm.getName()).username("regular-user").password("password")
                .clientId(Constants.ADMIN_CLI_CLIENT_ID).build()) {
            UserRepresentation invalidUser = new UserRepresentation();
            invalidUser.setUsername("do-not-create-me");

            Response response = localAdminClient.realm("default").users().create(invalidUser);
            Assert.assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());

            invalidUser.setGroups(Collections.emptyList());
            response = localAdminClient.realm("default").users().create(invalidUser);

            Assert.assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void testCreateUserDoNotGrantRole() {
        managedRealm.admin().roles().create(RoleConfigBuilder.create().name("realm-role").build());

        try {
            UserRepresentation userRep = UserConfigBuilder.create().username("alice").password("password").roles("realm-role")
                    .build();
            String userId = ApiUtil.getCreatedId(managedRealm.admin().users().create(userRep));
            UserResource user = managedRealm.admin().users().get(userId);
            List<RoleRepresentation> realmMappings = user.roles().getAll().getRealmMappings();

            assertFalse(realmMappings.stream().map(RoleRepresentation::getName).anyMatch("realm-role"::equals));
        } finally {
            managedRealm.admin().roles().get("realm-role").remove();
        }
    }

    @Test
    public void testDefaultCharacterValidationOnUsername() {
        List<String> invalidNames = List.of("1user\\\\", "2user\\\\%", "3user\\\\*", "4user\\\\_");

        for (String invalidName : invalidNames) {
            UserRepresentation invalidUser = UserConfigBuilder.create().username(invalidName).email("test@invalid.org").build();
            Response response = managedRealm.admin().users().create(invalidUser);
            Assert.assertEquals(400, response.getStatus());
            Assert.assertEquals("error-username-invalid-character", response.readEntity(ErrorRepresentation.class).getErrorMessage());
        }
    }

    public static class UserCreateServerConf implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder builder) {
            return builder.dependency("org.keycloak.tests", "keycloak-tests-custom-providers");
        }
    }
}

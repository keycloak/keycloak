package org.keycloak.test.showcase;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.common.util.Base64;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.StripSecretsUtils;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.test.framework.annotations.InjectOAuthClient;
import org.keycloak.test.framework.annotations.InjectPage;
import org.keycloak.test.framework.annotations.InjectRealm;
import org.keycloak.test.framework.annotations.InjectWebDriver;
import org.keycloak.test.framework.annotations.KeycloakIntegrationTest;
import org.keycloak.test.framework.oauth.OAuthClient;
import org.keycloak.test.framework.page.LoginPage;
import org.keycloak.test.framework.realm.ManagedRealm;
import org.keycloak.test.framework.util.ApiUtil;
import org.keycloak.util.JsonSerialization;
import org.openqa.selenium.WebDriver;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@KeycloakIntegrationTest
public class UserTest {

    @InjectRealm
    ManagedRealm realm;

    @InjectOAuthClient
    OAuthClient oAuthClient;

    @InjectWebDriver
    WebDriver webDriver;

    @InjectPage
    LoginPage loginPage;

    @AfterEach
    public void afterEach() {
        realm.admin().identityProviders().findAll()
                .forEach(ip -> realm.admin().identityProviders().get(ip.getAlias()).remove());
        realm.admin().groups().groups()
                .forEach(g -> realm.admin().groups().group(g.getId()).remove());
        realm.admin().users().list()
                .forEach(u -> realm.admin().users().delete(u.getId()));
    }

    public String createUser() {
        return createUser("user1", "user1@localhost");
    }

    public String createUser(String username, String email) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setEmail(email);
        user.setRequiredActions(Collections.emptyList());
        user.setEnabled(true);

        return createUser(user);
    }

    private String createUser(UserRepresentation userRep) {
        return createUser(userRep, true);
    }

    private String createUser(UserRepresentation userRep, boolean assertAdminEvent) {
        final String createdId;
        try (Response response = realm.admin().users().create(userRep)) {
            createdId = ApiUtil.handleCreatedResponse(response);
            Assertions.assertNotNull(createdId);
        }

        StripSecretsUtils.stripSecrets(null, userRep);

        // TODO
/*        if (assertAdminEvent) {
            assertAdminEvents.assertEvent(realmId, OperationType.CREATE, AdminEventPaths.userResourcePath(createdId), userRep,
                    ResourceType.USER);
        }

        getCleanup().addUserId(createdId);*/

        return createdId;
    }

    @Test
    public void verifyCreateUser() {
        createUser();
    }

    @Test
    public void createUserWithTemporaryPasswordWithAdditionalPasswordUpdateShouldRemoveUpdatePasswordRequiredAction() {

        String userId = createUser();

        CredentialRepresentation credTmp = new CredentialRepresentation();
        credTmp.setType(CredentialRepresentation.PASSWORD);
        credTmp.setValue("temp");
        credTmp.setTemporary(Boolean.TRUE);

        realm.admin().users().get(userId).resetPassword(credTmp);

        CredentialRepresentation credPerm = new CredentialRepresentation();
        credPerm.setType(CredentialRepresentation.PASSWORD);
        credPerm.setValue("perm");
        credPerm.setTemporary(null);

        realm.admin().users().get(userId).resetPassword(credPerm);

        UserRepresentation userRep = realm.admin().users().get(userId).toRepresentation();

        Assertions.assertFalse(userRep.getRequiredActions().contains(UserModel.RequiredAction.UPDATE_PASSWORD.name()));
    }

    @Test
    public void createDuplicatedUser1() {
        createUser();

        UserRepresentation user = new UserRepresentation();
        user.setUsername("user1");
        try (Response response = realm.admin().users().create(user)) {
            Assertions.assertEquals(409, response.getStatus());
            //assertAdminEvents.assertEmpty();

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

        try (Response response = realm.admin().users().create(user)) {
            Assertions.assertEquals(409, response.getStatus());
            //assertAdminEvents.assertEmpty();

            ErrorRepresentation error = response.readEntity(ErrorRepresentation.class);
            Assertions.assertEquals("User exists with same email", error.getErrorMessage());
        }
    }

    @Test
    public void createDuplicatedUsernameWithEmail() {
        createUser("user1@local.com", "user1@local.org");

        UserRepresentation user = new UserRepresentation();
        user.setUsername("user1@local.org");
        user.setEmail("user2@localhost");
        try (Response response = realm.admin().users().create(user)) {
            Assertions.assertEquals(409, response.getStatus());
            //assertAdminEvents.assertEmpty();

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

        try (Response response = realm.admin().users().create(user)) {
            Assertions.assertEquals(409, response.getStatus());
            //assertAdminEvents.assertEmpty();

            ErrorRepresentation error = response.readEntity(ErrorRepresentation.class);
            Assertions.assertEquals("User exists with same email", error.getErrorMessage());
        }
    }

    @Test
    public void createDuplicateEmailWithExistingDuplicates() {
        //Allow duplicate emails
        RealmRepresentation rep = realm.admin().toRepresentation();
        rep.setDuplicateEmailsAllowed(true);
        realm.admin().update(rep);

        //Create 2 users with the same email
        UserRepresentation user = new UserRepresentation();
        user.setEmail("user1@localhost");
        user.setUsername("user1");
        createUser(user, false);
        user.setUsername("user2");
        createUser(user, false);

        //Disallow duplicate emails
        rep.setDuplicateEmailsAllowed(false);
        realm.admin().update(rep);

        //Create a third user with the same email
        user.setUsername("user3");
        //assertAdminEvents.clear();

        try (Response response = realm.admin().users().create(user)) {
            Assertions.assertEquals(409, response.getStatus());
            ErrorRepresentation error = response.readEntity(ErrorRepresentation.class);
            Assertions.assertEquals("User exists with same username or email", error.getErrorMessage());
            //assertAdminEvents.assertEmpty();
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

        user.setCredentials(List.of(hashedPassword));

        createUser(user);

        // TODO: add TestingClient
        /*CredentialModel credentialHashed = fetchCredentials("user_creds");
        PasswordCredentialModel pcmh = PasswordCredentialModel.createFromCredentialModel(credentialHashed);
        assertNotNull("Expecting credential", credentialHashed);
        assertEquals("my-algorithm", pcmh.getPasswordCredentialData().getAlgorithm());
        assertEquals(Long.valueOf(1001), credentialHashed.getCreatedDate());
        assertEquals("deviceX", credentialHashed.getUserLabel());
        assertEquals(22, pcmh.getPasswordCredentialData().getHashIterations());
        assertEquals("ABC", pcmh.getPasswordSecretData().getValue());
        assertEquals("theSalt", new String(pcmh.getPasswordSecretData().getSalt()));
        assertEquals(CredentialRepresentation.PASSWORD, credentialHashed.getType());*/
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
                "      \"salt\" : \"" + Base64.encodeBytes(pcm.getPasswordSecretData().getSalt()) + "\",\n" +
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

        // TODO: add TestingClient
        /*CredentialModel credentialHashed = fetchCredentials("user_creds");
        PasswordCredentialModel pcmh = PasswordCredentialModel.createFromCredentialModel(credentialHashed);
        assertNotNull("Expecting credential", credentialHashed);
        assertEquals("my-algorithm", pcmh.getPasswordCredentialData().getAlgorithm());
        assertEquals(Long.valueOf(1001), credentialHashed.getCreatedDate());
        assertEquals("deviceX", credentialHashed.getUserLabel());
        assertEquals(22, pcmh.getPasswordCredentialData().getHashIterations());
        assertEquals("ABC", pcmh.getPasswordSecretData().getValue());
        assertEquals("theSalt", new String(pcmh.getPasswordSecretData().getSalt()));
        assertEquals(CredentialRepresentation.PASSWORD, credentialHashed.getType());*/
    }

    /*@Test
    public void updateUserWithHashedCredentials() {
        String userId = createUser("user_hashed_creds", "user_hashed_creds@localhost");

        byte[] salt = new byte[]{-69, 85, 87, 99, 26, -107, 125, 99, -77, 30, -111, 118, 108, 100, -117, -56};

        PasswordCredentialModel credentialModel = PasswordCredentialModel.createFromValues("pbkdf2-sha256", salt,
                27500, "uskEPZWMr83pl2mzNB95SFXfIabe2UH9ClENVx/rrQqOjFEjL2aAOGpWsFNNF3qoll7Qht2mY5KxIDm3Rnve2w==");
        credentialModel.setCreatedDate(1001l);
        CredentialRepresentation hashedPassword = ModelToRepresentation.toRepresentation(credentialModel);

        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setCredentials(Collections.singletonList(hashedPassword));

        realm.admin().users().get(userId).update(userRepresentation);

        oAuthClient.realm("REALM_NAME");
        driver.navigate().to(oauth.getLoginFormUrl());

        assertEquals("Sign in to your account", PageUtils.getPageTitle(driver));

        loginPage.login("user_hashed_creds", "admin");

        assertTrue(driver.getTitle().contains("AUTH_RESPONSE"));

        // oauth cleanup
        oauth.realm("test");
    }*/

    @Test
    public void createUserWithTempolaryCredentials() {
        UserRepresentation user = new UserRepresentation();
        user.setUsername("user_temppw");
        user.setEmail("email.temppw@localhost");

        CredentialRepresentation password = new CredentialRepresentation();
        password.setValue("password");
        password.setType(CredentialRepresentation.PASSWORD);
        password.setTemporary(true);
        user.setCredentials(Arrays.asList(password));

        String userId = createUser(user);

        UserRepresentation userRep = realm.admin().users().get(userId).toRepresentation();
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
        user.setCredentials(List.of(rawPassword));

        createUser(user);

        /*CredentialModel credential = fetchCredentials("user_rawpw");
        assertNotNull("Expecting credential", credential);
        PasswordCredentialModel pcm = PasswordCredentialModel.createFromCredentialModel(credential);
        assertEquals(DefaultPasswordHash.getDefaultAlgorithm(), pcm.getPasswordCredentialData().getAlgorithm());
        assertEquals(DefaultPasswordHash.getDefaultIterations(), pcm.getPasswordCredentialData().getHashIterations());
        assertNotEquals("ABCD", pcm.getPasswordSecretData().getValue());
        assertEquals(CredentialRepresentation.PASSWORD, credential.getType());*/
    }

/*    private CredentialModel fetchCredentials(String username) {
        return getTestingClient().server(REALM_NAME).fetch(RunHelpers.fetchCredentials(username));
    }*/

    @Test
    public void createDuplicatedUser3() {
        createUser();

        UserRepresentation user = new UserRepresentation();
        user.setUsername("User1");

        try (Response response = realm.admin().users().create(user)) {
            Assertions.assertEquals(409, response.getStatus());
            //assertAdminEvents.assertEmpty();
        }
    }

    @Test
    public void createDuplicatedUser4() {
        createUser();

        UserRepresentation user = new UserRepresentation();
        user.setUsername("USER1");

        try (Response response = realm.admin().users().create(user)) {
            Assertions.assertEquals(409, response.getStatus());
            //assertAdminEvents.assertEmpty();
        }
    }

    @Test
    public void createDuplicatedUser5() {
        createUser();

        UserRepresentation user = new UserRepresentation();
        user.setUsername("user2");
        user.setEmail("User1@localhost");

        try (Response response = realm.admin().users().create(user)) {
            Assertions.assertEquals(409, response.getStatus());
            //assertAdminEvents.assertEmpty();
        }
    }

    @Test
    public void createDuplicatedUser6() {
        createUser();

        UserRepresentation user = new UserRepresentation();
        user.setUsername("user2");
        user.setEmail("user1@LOCALHOST");

        try (Response response = realm.admin().users().create(user)) {
            Assertions.assertEquals(409, response.getStatus());
            //assertAdminEvents.assertEmpty();
        }
    }

    @Test
    public void createDuplicatedUser7() {
        createUser("user1", "USer1@Localhost");

        UserRepresentation user = new UserRepresentation();
        user.setUsername("user2");
        user.setEmail("user1@localhost");

        try (Response response = realm.admin().users().create(user)) {
            Assertions.assertEquals(409, response.getStatus());
            //assertAdminEvents.assertEmpty();
        }
    }

    @Test
    public void createTwoUsersWithEmptyStringEmails() {
        createUser("user1", "");
        createUser("user2", "");
    }


}

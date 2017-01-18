package org.keycloak.testsuite.sssd;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.pages.AccountPasswordPage;
import org.keycloak.testsuite.pages.AccountUpdateProfilePage;
import org.keycloak.testsuite.pages.LoginPage;

import javax.ws.rs.core.Response;
import java.util.List;

public class SSSDTest extends AbstractKeycloakTest {

    private static final String DISPLAY_NAME = "Test user federation";
    private static final String PROVIDER_NAME = "sssd";
    private static final String REALM_NAME = "test";

    private static final String USERNAME = "emily";
    private static final String PASSWORD = "emily123";
    private static final String DISABLED_USER = "david";
    private static final String DISABLED_USER_PASSWORD = "david123";
    private static final String NO_EMAIL_USER = "bart";
    private static final String NO_EMAIL_USER_PASSWORD = "bart123";

    private static final String DEFINITELY_NOT_PASSWORD = "not" + PASSWORD;

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "password";

    @Page
    protected LoginPage accountLoginPage;

    @Page
    protected AccountPasswordPage changePasswordPage;

    @Page
    protected AccountUpdateProfilePage profilePage;

    @Rule
    public AssertEvents events = new AssertEvents(this);

    private String SSSDFederationID;

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realm = new RealmRepresentation();

        realm.setRealm(REALM_NAME);
        realm.setEnabled(true);

        testRealms.add(realm);
    }

    @Before
    public void createUserFederation() {
        ComponentRepresentation userFederation = new ComponentRepresentation();

        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
        userFederation.setConfig(config);

        userFederation.setName(DISPLAY_NAME);
        userFederation.getConfig().putSingle("priority", "0");
        userFederation.setProviderType(UserStorageProvider.class.getName());
        userFederation.setProviderId(PROVIDER_NAME);

        Response response = adminClient.realm(REALM_NAME).components().add(userFederation);
        SSSDFederationID = ApiUtil.getCreatedId(response);
        response.close();
    }

    @Test
    public void testWrongUser() {
        log.debug("Testing wrong password for user " + USERNAME);

        driver.navigate().to(getAccountUrl());
        Assert.assertEquals("Browser should be on login page now", "Log in to " + REALM_NAME, driver.getTitle());
        accountLoginPage.login(USERNAME, DEFINITELY_NOT_PASSWORD);

        Assert.assertEquals("Invalid username or password.", accountLoginPage.getError());
    }

    @Test
    public void testDisabledUser() {
        log.debug("Testing disabled user " + USERNAME);

        driver.navigate().to(getAccountUrl());
        Assert.assertEquals("Browser should be on login page now", "Log in to " + REALM_NAME, driver.getTitle());
        accountLoginPage.login(DISABLED_USER, DISABLED_USER_PASSWORD);

        Assert.assertEquals("Invalid username or password.", accountLoginPage.getError());
    }

    @Test
    public void testAdmin() {
        log.debug("Testing password for user " + ADMIN_USERNAME);

        driver.navigate().to(getAccountUrl());
        Assert.assertEquals("Browser should be on login page now", "Log in to " + REALM_NAME, driver.getTitle());
        accountLoginPage.login(ADMIN_USERNAME, ADMIN_PASSWORD);
        Assert.assertTrue(profilePage.isCurrent());
    }

    @Test
    public void testExistingUserLogIn() {
        log.debug("Testing correct password");

        driver.navigate().to(getAccountUrl());
        Assert.assertEquals("Browser should be on login page now", "Log in to " + REALM_NAME, driver.getTitle());
        accountLoginPage.login(USERNAME, PASSWORD);
        Assert.assertTrue(profilePage.isCurrent());
        testUserGroups();
    }

    @Test
    public void testExistingUserWithNoEmailLogIn() {
        log.debug("Testing correct password, but no e-mail provided");

        driver.navigate().to(getAccountUrl());
        Assert.assertEquals("Browser should be on login page now", "Log in to " + REALM_NAME, driver.getTitle());
        accountLoginPage.login(NO_EMAIL_USER, NO_EMAIL_USER_PASSWORD);
        Assert.assertTrue(profilePage.isCurrent());
    }

    @Test
    public void testDeleteSSSDFederationProvider() {
        log.debug("Testing correct password");

        driver.navigate().to(getAccountUrl());
        Assert.assertEquals("Browser should be on login page now", "Log in to " + REALM_NAME, driver.getTitle());
        accountLoginPage.login(USERNAME, PASSWORD);
        Assert.assertTrue(profilePage.isCurrent());
        testUserGroups();
        int componentsListSize = adminClient.realm(REALM_NAME).components().query().size();
        adminClient.realm(REALM_NAME).components().component(SSSDFederationID).remove();
        Assert.assertEquals(componentsListSize - 1, adminClient.realm(REALM_NAME).components().query().size());
    }


    @Test
    public void changeReadOnlyProfile() throws Exception {

        profilePage.open();
        accountLoginPage.login(USERNAME, PASSWORD);

        Assert.assertEquals("emily", profilePage.getUsername());
        Assert.assertEquals("Emily", profilePage.getFirstName());
        Assert.assertEquals("Jones", profilePage.getLastName());
        Assert.assertEquals("emily@jones.com", profilePage.getEmail());

        profilePage.updateProfile("New first", "New last", "new@email.com");

        Assert.assertEquals("You can't update your account as it is read only.", profilePage.getError());
    }

    @Test
    public void changeReadOnlyPassword() {
        changePasswordPage.open();
        accountLoginPage.login(USERNAME, PASSWORD);

        changePasswordPage.changePassword(PASSWORD, "new-password", "new-password");
        Assert.assertEquals("You can't update your password as your account is read only.", profilePage.getError());
    }


    private void testUserGroups() {
        log.debug("Testing user groups");

        List<UserRepresentation> users = adminClient.realm(REALM_NAME).users().search(USERNAME, 0, 1);

        Assert.assertTrue("There must be at least one user", users.size() > 0);
        Assert.assertEquals("Exactly our test user", USERNAME, users.get(0).getUsername());

        List<GroupRepresentation> groups = adminClient.realm(REALM_NAME).users().get(users.get(0).getId()).groups();

        Assert.assertEquals("User must have exactly two groups", 2, groups.size());
        boolean wrongGroup = false;
        for (GroupRepresentation group : groups) {
            if (!group.getName().equalsIgnoreCase("ipausers") && !group.getName().equalsIgnoreCase("testgroup")) {
                wrongGroup = true;
                break;
            }
        }

        Assert.assertFalse("There exists some wrong group", wrongGroup);
    }

    private String getAccountUrl() {
        return getAuthRoot() + "/auth/realms/" + REALM_NAME + "/account";
    }

    private String getAuthRoot() {
        return suiteContext.getAuthServerInfo().getContextRoot().toString();
    }
}

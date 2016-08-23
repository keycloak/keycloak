package org.keycloak.testsuite.sssd;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserFederationProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.pages.LoginPage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SSSDTest extends AbstractKeycloakTest {

    private static final String DISPLAY_NAME = "Test user federation";
    private static final String PROVIDER_NAME = "sssd";
    private static final String REALM_NAME = "test";

    private static final String USERNAME = "emily";
    private static final String PASSWORD = "emily123";
    private static final String DEFINITELY_NOT_PASSWORD = "not" + PASSWORD;

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "password";

    @Page
    private LoginPage accountLoginPage;

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realm = new RealmRepresentation();

        realm.setRealm(REALM_NAME);
        realm.setEnabled(true);

        testRealms.add(realm);
    }

    @Before
    public void createUserFederation() {
        UserFederationProviderRepresentation userFederation = new UserFederationProviderRepresentation();

        Map<String, String> config = new HashMap<>();
        userFederation.setConfig(config);

        userFederation.setDisplayName(DISPLAY_NAME);
        userFederation.setPriority(0);
        userFederation.setProviderName(PROVIDER_NAME);

        adminClient.realm(REALM_NAME).userFederation().create(userFederation);
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
    public void testAdmin() {
        log.debug("Testing wrong password for user " + ADMIN_USERNAME);

        driver.navigate().to(getAccountUrl());
        Assert.assertEquals("Browser should be on login page now", "Log in to " + REALM_NAME, driver.getTitle());
        accountLoginPage.login(ADMIN_USERNAME, ADMIN_PASSWORD);

        Assert.assertEquals("Unexpected error when handling authentication request to identity provider.", accountLoginPage.getInstruction());
    }

    @Test
    public void testExistingUserLogIn() {
        log.debug("Testing correct password");

        driver.navigate().to(getAccountUrl());
        Assert.assertEquals("Browser should be on login page now", "Log in to " + REALM_NAME, driver.getTitle());
        accountLoginPage.login(USERNAME, PASSWORD);
        Assert.assertEquals("Browser should be on account page now, logged in", "Keycloak Account Management", driver.getTitle());

        testUserGroups();
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

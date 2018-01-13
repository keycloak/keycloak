package org.keycloak.testsuite.springboot;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;

public class BasicSpringBootTest extends AbstractSpringBootTest {

    private static final String USER_LOGIN_2 = "testuser2";
    private static final String USER_EMAIL_2 = "user2@email.test";
    private static final String USER_PASSWORD_2 = "user2-password";

    private static final String INCORRECT_ROLE = "wrong-admin";

    @Before
    public void addIncorrectUser() {
        RolesResource rolesResource = adminClient.realm(REALM_NAME).roles();

        RoleRepresentation role = new RoleRepresentation(INCORRECT_ROLE, INCORRECT_ROLE, false);

        rolesResource.create(role);

        addUser(USER_LOGIN_2, USER_EMAIL_2, USER_PASSWORD_2, INCORRECT_ROLE);
    }

    @After
    public void removeUser() {
        UserRepresentation user = ApiUtil.findUserByUsername(adminClient.realm(REALM_NAME), USER_LOGIN_2);

        if (user != null) {
            adminClient.realm(REALM_NAME).users().delete(user.getId());
        }

        adminClient.realm(REALM_NAME).roles().deleteRole(INCORRECT_ROLE);
    }

    @Test
    public void testCorrectUser() {
        driver.navigate().to(APPLICATION_URL + "/index.html");

        Assert.assertTrue("Must be on application page", applicationPage.isCurrent());

        applicationPage.goAdmin();

        Assert.assertTrue("Must be on login page", loginPage.isCurrent());

        loginPage.login(USER_LOGIN, USER_PASSWORD);

        Assert.assertTrue("Must be on admin page", adminPage.isCurrent());
        Assert.assertTrue("Admin page must contain correct div",
                driver.getPageSource().contains("You are now admin"));

        driver.navigate().to(logoutPage(BASE_URL));

        Assert.assertTrue("Must be on login page", loginPage.isCurrent());

    }

    @Test
    public void testIncorrectUser() {
        driver.navigate().to(APPLICATION_URL + "/index.html");

        Assert.assertTrue("Must be on application page", applicationPage.isCurrent());

        applicationPage.goAdmin();

        Assert.assertTrue("Must be on login page", loginPage.isCurrent());

        loginPage.login(USER_LOGIN_2, USER_PASSWORD_2);

        Assert.assertTrue("Must return 403 because of incorrect role",
                driver.getPageSource().contains("There was an unexpected error (type=Forbidden, status=403)")
                || driver.getPageSource().contains("\"status\":403,\"error\":\"Forbidden\""));
    }

    @Test
    public void testIncorrectCredentials() {
        driver.navigate().to(APPLICATION_URL + "/index.html");

        Assert.assertTrue("Must be on application page", applicationPage.isCurrent());

        applicationPage.goAdmin();

        Assert.assertTrue("Must be on login page", loginPage.isCurrent());

        loginPage.login(USER_LOGIN, USER_PASSWORD_2);

        Assert.assertEquals("Error message about password",
                "Invalid username or password.", loginPage.getError());
    }
}

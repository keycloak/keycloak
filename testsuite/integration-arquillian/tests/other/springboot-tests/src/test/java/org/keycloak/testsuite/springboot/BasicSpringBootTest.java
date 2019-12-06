package org.keycloak.testsuite.springboot;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWith;
import static org.keycloak.testsuite.util.WaitUtils.waitForPageToLoad;

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

        testRealmLoginPage.setAuthRealm(REALM_NAME);
    }

    @After
    public void removeUser() {
        UserRepresentation user = ApiUtil.findUserByUsername(adminClient.realm(REALM_NAME), USER_LOGIN_2);

        if (user != null) {
            adminClient.realm(REALM_NAME).users().delete(user.getId());
        }

        adminClient.realm(REALM_NAME).roles().deleteRole(INCORRECT_ROLE);
    }

    private void navigateToApplication() {
        driver.navigate().to(APPLICATION_URL + "/index.html");
        waitForPageToLoad();
    }

    @Test
    public void testCorrectUser() {
        navigateToApplication();

        applicationPage.assertIsCurrent();
        applicationPage.goAdmin();

        assertCurrentUrlStartsWith(testRealmLoginPage);

        testRealmLoginPage.form().login(USER_LOGIN, USER_PASSWORD);

        adminPage.assertIsCurrent();
        assertThat(driver.getPageSource(), containsString("You are now admin"));

        driver.navigate().to(logoutPage(BASE_URL));
        waitForPageToLoad();

        assertCurrentUrlStartsWith(testRealmLoginPage);

    }

    @Test
    public void testIncorrectUser() {
        navigateToApplication();

        applicationPage.assertIsCurrent();
        applicationPage.goAdmin();

        assertCurrentUrlStartsWith(testRealmLoginPage);

        testRealmLoginPage.form().login(USER_LOGIN_2, USER_PASSWORD_2);

        assertThat(driver.getPageSource(), containsString("Forbidden"));

        driver.navigate().to(logoutPage(BASE_URL));
        waitForPageToLoad();
    }

    @Test
    public void testIncorrectCredentials() {
        navigateToApplication();

        applicationPage.assertIsCurrent();
        applicationPage.goAdmin();

        assertCurrentUrlStartsWith(testRealmLoginPage);

        testRealmLoginPage.form().login(USER_LOGIN, USER_PASSWORD_2);

        assertThat(testRealmLoginPage.feedbackMessage().isError(), is(true));
        assertThat(testRealmLoginPage.feedbackMessage().getText(), is(equalTo("Invalid username or password.")));
    }
}

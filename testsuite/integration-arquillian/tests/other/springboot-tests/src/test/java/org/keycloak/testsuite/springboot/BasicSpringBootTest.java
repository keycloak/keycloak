package org.keycloak.testsuite.springboot;

import org.junit.Assert;
import org.junit.Test;

public class BasicSpringBootTest extends AbstractSpringBootTest {
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

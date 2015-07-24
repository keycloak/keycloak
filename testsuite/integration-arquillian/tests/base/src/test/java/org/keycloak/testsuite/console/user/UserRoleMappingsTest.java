package org.keycloak.testsuite.console.user;

import org.jboss.arquillian.graphene.findby.FindByJQuery;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.testsuite.console.AbstractAdminConsoleTest;
import org.keycloak.testsuite.console.page.fragment.FlashMessage;
import org.keycloak.testsuite.model.User;
import org.keycloak.testsuite.console.page.fragment.RoleMappings;
import org.keycloak.testsuite.console.page.settings.user.UserPage;

import static org.junit.Assert.assertTrue;
import static org.openqa.selenium.By.linkText;

/**
 * Created by fkiss.
 */
public class UserRoleMappingsTest extends AbstractAdminConsoleTest<RoleMappings> {

    @Page
    private UserPage userPage;

    @FindByJQuery(".alert")
    private FlashMessage flashMessage;

    @Before
    public void beforeRoleMappingsTest() {
        navigation.users();
    }

    @Test
    public void addUserAndAssignRole() {
        String testUsername = "tester1";
        User testUser = new User(testUsername, "pass");
        userPage.addUser(testUser);
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
        navigation.users();
        userPage.findUser(testUsername);
        driver.findElement(linkText(testUsername)).click();
        navigation.roleMappings(testUsername);

        page.addAvailableRole("create-realm");
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
        navigation.users();
        userPage.deleteUser(testUsername);
    }

    @Ignore
    @Test
    public void addAndRemoveUserAndAssignRole() {
        String testUsername = "tester2";
        User testUser = new User(testUsername, "pass");
        userPage.addUser(testUser);
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
        navigation.users();
        userPage.findUser(testUsername);
        driver.findElement(linkText(testUsername)).click();
        navigation.roleMappings(testUsername);

        page.addAvailableRole("create-realm");
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
        page.removeAssignedRole("create-realm");
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
        navigation.users();
        userPage.deleteUser(testUsername);
    }
}

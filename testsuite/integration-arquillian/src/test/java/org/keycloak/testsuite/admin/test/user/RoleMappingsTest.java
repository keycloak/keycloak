package org.keycloak.testsuite.admin.test.user;

import org.jboss.arquillian.graphene.findby.FindByJQuery;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.testsuite.admin.AbstractKeycloakTest;
import org.keycloak.testsuite.admin.fragment.FlashMessage;
import org.keycloak.testsuite.admin.model.User;
import org.keycloak.testsuite.admin.page.settings.user.RoleMappingsPage;
import org.keycloak.testsuite.admin.page.settings.user.UserPage;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.openqa.selenium.By.linkText;

/**
 * Created by fkiss.
 */
public class RoleMappingsTest extends AbstractKeycloakTest<RoleMappingsPage> {

    @Page
    private UserPage userPage;

    @FindByJQuery(".alert")
    private FlashMessage flashMessage;

    @Before
    public void beforeAddNewUserTest() {
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

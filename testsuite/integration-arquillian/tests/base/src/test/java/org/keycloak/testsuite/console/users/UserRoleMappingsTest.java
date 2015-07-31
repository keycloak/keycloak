package org.keycloak.testsuite.console.users;

import org.jboss.arquillian.graphene.findby.FindByJQuery;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.testsuite.console.AbstractAdminConsoleTest;
import org.keycloak.testsuite.console.page.fragment.FlashMessage;
import org.keycloak.testsuite.console.page.fragment.RoleMappings;
import org.keycloak.testsuite.console.page.users.Users;

import static org.junit.Assert.assertTrue;
import static org.keycloak.representations.idm.CredentialRepresentation.PASSWORD;
import org.keycloak.representations.idm.UserRepresentation;
import static org.openqa.selenium.By.linkText;

/**
 * Created by fkiss.
 */
public class UserRoleMappingsTest extends AbstractAdminConsoleTest {

    @Page
    private RoleMappings page;

    @Page
    private Users userPage;

    private UserRepresentation testUser;

    @FindByJQuery(".alert")
    private FlashMessage flashMessage;

    @Before
    public void beforeRoleMappingsTest() {
        navigation.users();
        testUser = new UserRepresentation();
    }

    @Test
    public void addUserAndAssignRole() {
        String testUsername = "tester1";
        testUser.setUsername(testUsername);
        testUser.credential(PASSWORD, "pass");
        userPage.addUser(testUser);
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
        navigation.users();
        userPage.findUser(testUsername);
        driver.findElement(linkText(testUsername)).click();
        navigation.roleMappings(testUsername);

        page.addAvailableRole("test-role");
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
        navigation.users();
        userPage.deleteUser(testUsername);
    }

    @Ignore
    @Test
    public void addAndRemoveUserAndAssignRole() {
        String testUsername = "tester2";
        testUser.setUsername(testUsername);
        testUser.credential(PASSWORD, "pass");
        userPage.addUser(testUser);
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
        navigation.users();
        userPage.findUser(testUsername);
        driver.findElement(linkText(testUsername)).click();
        navigation.roleMappings(testUsername);

        page.addAvailableRole("test-role");
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
        page.removeAssignedRole("test-role");
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
        navigation.users();
        userPage.deleteUser(testUsername);
    }
}

package org.keycloak.testsuite.ui.test.user;

import org.jboss.arquillian.graphene.findby.FindByJQuery;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.testsuite.ui.AbstractKeyCloakTest;
import org.keycloak.testsuite.ui.fragment.FlashMessage;
import org.keycloak.testsuite.ui.model.User;
import org.keycloak.testsuite.ui.fragment.RoleMappings;
import org.keycloak.testsuite.ui.page.settings.user.UserPage;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.openqa.selenium.By.linkText;

/**
 * Created by fkiss.
 */
public class RoleMappingsTest extends AbstractKeyCloakTest<RoleMappings> {

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

    //@Ignore
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
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        page.removeAssignedRole("create-realm");
        //assertTrue(flashMessage.getText(), flashMessage.isSuccess());
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        navigation.users();
        userPage.deleteUser(testUsername);
    }
}

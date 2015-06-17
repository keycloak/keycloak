package org.keycloak.testsuite.ui.test.client;

import org.jboss.arquillian.graphene.findby.FindByJQuery;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.testsuite.ui.AbstractKeyCloakTest;
import org.keycloak.testsuite.ui.fragment.FlashMessage;
import org.keycloak.testsuite.ui.fragment.RoleMappings;
import org.keycloak.testsuite.ui.model.Role;
import org.keycloak.testsuite.ui.model.User;
import org.keycloak.testsuite.ui.page.settings.RolesPage;
import org.keycloak.testsuite.ui.page.settings.user.UserPage;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.ui.util.SeleniumUtils.waitGuiForElement;
import static org.openqa.selenium.By.linkText;

/**
 * Created by fkiss.
 */
public class DefaultRolesTest extends AbstractKeyCloakTest<RolesPage> {

    @Page
    private UserPage userPage;

    @Page
    private RoleMappings roleMappings;

    @FindByJQuery(".alert")
    private FlashMessage flashMessage;

    @Before
    public void beforeDefaultRolesTest() {
        navigation.roles();
    }

    @FindBy(css = "tr[ng-repeat='user in users']")
    private WebElement dataTable;

    @Test
    public void testSetDefaultRole() {
        String testUsername = "defaultrole tester";
        String defaultRole = "default-role";
        Role role = new Role(defaultRole);
        page.addRole(role);
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());

        navigation.roles();
        navigation.defaultRoles();
        roleMappings.addAvailableRole(defaultRole);
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());

        User testUser = new User(testUsername, "pass");
        navigation.users();
        userPage.addUser(testUser);
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
        navigation.users();
        userPage.findUser(testUsername);
        userPage.goToUser(testUsername);

        navigation.roleMappings(testUsername);
        assertTrue(roleMappings.isAssignedRole(defaultRole));

        navigation.roles();
        page.deleteRole(role);

        navigation.users();
        userPage.deleteUser(testUsername);
    }
}

package org.keycloak.testsuite.console.clients;

import org.jboss.arquillian.graphene.findby.FindByJQuery;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.testsuite.console.AbstractAdminConsoleTest;
import org.keycloak.testsuite.console.page.fragment.FlashMessage;
import org.keycloak.testsuite.console.page.fragment.RoleMappings;
import org.keycloak.testsuite.console.page.roles.RolesPage;
import org.keycloak.testsuite.console.page.users.Users;

import static org.junit.Assert.assertTrue;
import static org.keycloak.representations.idm.CredentialRepresentation.PASSWORD;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

/**
 * Created by fkiss.
 */
public class DefaultRolesTest extends AbstractAdminConsoleTest {

    @Page
    private RolesPage page;

    @Page
    private Users userPage;

    @Page
    private RoleMappings roleMappings;

    @FindByJQuery(".alert")
    private FlashMessage flashMessage;

    @Before
    public void beforeDefaultRolesTest() {
        navigation.roles();
    }

    @Test
    public void testSetDefaultRole() {
        String testUsername = "defaultrole tester";
        String defaultRole = "default-role";
        RoleRepresentation role = new RoleRepresentation(defaultRole, "");
        page.addRole(role);
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());

        navigation.roles();
        navigation.defaultRoles();
        roleMappings.addAvailableRole(defaultRole);
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());

        UserRepresentation testUser = new UserRepresentation();
        testUser.setUsername(testUsername);
        testUser.credential(PASSWORD, "pass");
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

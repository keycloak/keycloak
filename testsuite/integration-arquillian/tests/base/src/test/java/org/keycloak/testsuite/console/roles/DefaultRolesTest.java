package org.keycloak.testsuite.console.roles;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.testsuite.console.AbstractConsoleTest;

import static org.junit.Assert.assertTrue;
import static org.keycloak.representations.idm.CredentialRepresentation.PASSWORD;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.console.page.roles.DefaultRoles;
import org.keycloak.testsuite.console.page.roles.RealmRoles;
import org.keycloak.testsuite.console.page.users.UserRoleMappings;

/**
 * Created by fkiss.
 */
public class DefaultRolesTest extends AbstractConsoleTest {

    @Page
    private RealmRoles realmRoles;
    @Page
    private DefaultRoles defaultRoles;
    @Page
    private UserRoleMappings userRoleMappings;

    @Before
    public void beforeDefaultRolesTest() {
        realmRoles.navigateTo();
    }

    @Test
    public void testSetDefaultRole() {
        String testUsername = "defaultrole tester";
        String defaultRole = "default-role";
        RoleRepresentation role = new RoleRepresentation(defaultRole, "");
        realmRoles.addRole(role);
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());

        defaultRoles.navigateTo();
        defaultRoles.form().addAvailableRole(defaultRole);
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());

        UserRepresentation testUser = new UserRepresentation();
        testUser.setUsername(testUsername);
        testUser.credential(PASSWORD, "pass");
        users.navigateTo();
        createUser(testUser);
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
        users.navigateTo();
        users.findUser(testUsername);
        users.clickUser(testUsername);

        userRoleMappings.navigateTo();
        assertTrue(userRoleMappings.form().isAssignedRole(defaultRole));

        realmRoles.navigateTo();
        realmRoles.deleteRole(role);

        users.navigateTo();
        users.deleteUser(testUsername);
    }
}

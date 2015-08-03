package org.keycloak.testsuite.console.roles;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;
import org.keycloak.testsuite.console.AbstractConsoleTest;
import org.keycloak.testsuite.console.page.users.UserRoleMappingsForm;
import org.keycloak.testsuite.console.page.roles.RealmRoles;

import java.util.ArrayList;
import java.util.List;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.console.page.users.User;

/**
 * Created by fkiss.
 */
public class AddCompositeRoleTest extends AbstractConsoleTest {
    
    @Page
    private RealmRoles roles;

    @Page
    private User user;
    @Page
    private UserRoleMappingsForm roleMappings;

    @Before
    public void beforeTestAddCompositeRole() {
        roles.navigateTo();
    }

    @Ignore//KEYCLOAK-1497
    @Test
    public void testAddCompositeRole() {
        UserRepresentation testUserRep = new UserRepresentation();
        testUserRep.setUsername("usercomposite");

        RoleRepresentation compositeRole = new RoleRepresentation("compositeRole", "");
        RoleRepresentation subRole1 = new RoleRepresentation("subRole1", "");
        RoleRepresentation subRole2 = new RoleRepresentation("subRole2", "");
        List<RoleRepresentation> testRoles = new ArrayList<>();
        compositeRole.setComposite(true);
        testRoles.add(compositeRole);
        testRoles.add(subRole1);
        testRoles.add(subRole2);

        //create roles and user
        for (RoleRepresentation role : testRoles) {
            roles.addRole(role);
            flashMessage.waitUntilPresent();
            assertTrue(flashMessage.getText(), flashMessage.isSuccess());
            roles.navigateTo();
            assertEquals(role.getName(), roles.findRole(role.getName()).getName());
        }
        users.navigateTo();
        createUser(testUserRep);
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());

        //adding subroles to composite role
        roles.navigateTo();
        roles.findRole(compositeRole.getName());
        roles.goToRole(compositeRole);
        roles.setCompositeRole(compositeRole);
        roleMappings.addAvailableRole(subRole1.getName(), subRole2.getName());
        //flashMessage.waitUntilPresent();
        //assertTrue(flashMessage.getText(), flashMessage.isSuccess()); KEYCLOAK-1497

        //check if subroles work as expected
        users.navigateTo();
        users.findUser(testUserRep.getUsername());
        users.clickUser(testUserRep.getUsername());
        user.tabs().roleMappings();
        roleMappings.addAvailableRole(compositeRole.getName());
        assertTrue(roleMappings.checkIfEffectiveRealmRolesAreComplete(compositeRole, subRole1, subRole2));

        //delete everything
        roles.navigateTo();
        roles.deleteRole(compositeRole);
        roles.navigateTo();
        roles.deleteRole(subRole1);
        roles.navigateTo();
        roles.deleteRole(subRole2);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        users.navigateTo();
        users.deleteUser(testUserRep.getUsername());
    }

}

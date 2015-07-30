package org.keycloak.testsuite.console.role;

import org.jboss.arquillian.graphene.findby.FindByJQuery;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;
import org.keycloak.testsuite.console.AbstractAdminConsoleTest;
import org.keycloak.testsuite.console.page.fragment.FlashMessage;
import org.keycloak.testsuite.console.page.fragment.RoleMappings;
import org.keycloak.testsuite.model.Role;
import org.keycloak.testsuite.console.page.settings.RolesPage;
import org.keycloak.testsuite.console.page.settings.user.UserPage;

import java.util.ArrayList;
import java.util.List;
import org.keycloak.representations.idm.UserRepresentation;

/**
 * Created by fkiss.
 */
public class AddCompositeRoleTest extends AbstractAdminConsoleTest {
    
    @Page
    private RolesPage page;

    @Page
    private UserPage userPage;

    @Page
    private RolesPage rolesPage;

    @Page
    private RoleMappings roleMappings;

    @FindByJQuery(".alert")
    private FlashMessage flashMessage;

    @Before
    public void beforeTestAddCompositeRole() {
        navigation.roles();
    }

    @Ignore//KEYCLOAK-1497
    @Test
    public void testAddCompositeRole() {
        UserRepresentation user = new UserRepresentation();
        user.setUsername("usercomposite");

        Role compositeRole = new Role("compositeRole");
        Role subRole1 = new Role("subRole1");
        Role subRole2 = new Role("subRole2");
        List<Role> roles = new ArrayList<>();
        compositeRole.setComposite(true);
        roles.add(compositeRole);
        roles.add(subRole1);
        roles.add(subRole2);

        //create roles and user
        for (Role role : roles) {
            page.addRole(role);
            flashMessage.waitUntilPresent();
            assertTrue(flashMessage.getText(), flashMessage.isSuccess());
            navigation.roles();
            assertEquals(role.getName(), page.findRole(role.getName()).getName());
        }
        navigation.users();
        userPage.addUser(user);
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());

        //adding subroles to composite role
        navigation.roles();
        page.findRole(compositeRole.getName());
        page.goToRole(compositeRole);
        page.setCompositeRole(compositeRole);
        roleMappings.addAvailableRole(subRole1.getName(), subRole2.getName());
        //flashMessage.waitUntilPresent();
        //assertTrue(flashMessage.getText(), flashMessage.isSuccess()); KEYCLOAK-1497

        //check if subroles work as expected
        navigation.users();
        userPage.findUser(user.getUsername());
        userPage.goToUser(user.getUsername());
        navigation.roleMappings(user.getUsername());
        roleMappings.addAvailableRole(compositeRole.getName());
        assertTrue(roleMappings.checkIfEffectiveRealmRolesAreComplete(compositeRole, subRole1, subRole2));

        //delete everything
        navigation.roles();
        page.deleteRole(compositeRole);
        navigation.roles();
        page.deleteRole(subRole1);
        navigation.roles();
        page.deleteRole(subRole2);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        navigation.users();
        userPage.deleteUser(user.getUsername());
    }

}

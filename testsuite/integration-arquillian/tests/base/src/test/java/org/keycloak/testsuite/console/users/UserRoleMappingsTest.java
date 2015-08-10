package org.keycloak.testsuite.console.users;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import org.keycloak.testsuite.console.page.users.UserRoleMappings;

/**
 * Created by fkiss.
 */
public class UserRoleMappingsTest extends AbstractUserTest {

    @Page
    private UserRoleMappings roleMappings;

    @Before
    public void beforeUserRoleMappingsTest() {
        users.navigateTo();
        users.table().searchUsers(testRealmUser.getUsername());
        users.table().clickUser(testRealmUser.getUsername());
        roleMappings.tabs().roleMappings();
    }
    
    @Test
    @Ignore
    public void assignRole() {
        roleMappings.form().addAvailableRole("create-realm");
        assertFlashMessageSuccess();
        
        roleMappings.breadcrumb().clickItemOneLevelUp();
        users.table().searchUsers(testRealmUser.getUsername());
        users.table().deleteUser(testRealmUser.getUsername());
    }

//    @Test
//    public void addAndRemoveUserAndAssignRole() {
//        roleMappings.form().addAvailableRole("create-realm");
//        assertFlashMessageSuccess();
//        
//        roleMappings.form().removeAssignedRole("create-realm");
//        assertFlashMessageSuccess();
//        
//        users.navigateTo();
//        users.table().deleteUser(testUsername);
//    }



//    @Test // this should be moved to users tests
//    public void testRoleIsAvailableForUsers() {
//        RoleRepresentation role = new RoleRepresentation("User role", "");
//        roles.addRole(role);
//        flashMessage.waitUntilPresent();
//        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
//        users.navigateTo();
//        users.viewAllUsers();
//        users.clickUser("admin");
//        user.tabs().roleMappings();
//        Select rolesSelect = new Select(driver.findElement(id("available")));
//        assertEquals("User role should be present in admin role mapping",
//                role.getName(), rolesSelect.getOptions().get(0).getText());
//        roles.navigateTo();
//        roles.deleteRole(role);
//    }
//
//    @Ignore//KEYCLOAK-1497
//    @Test
//    public void testAddCompositeRole() {
//        UserRepresentation testUserRep = new UserRepresentation();
//        testUserRep.setUsername("usercomposite");
//
//        RoleRepresentation compositeRole = new RoleRepresentation("compositeRole", "");
//        RoleRepresentation subRole1 = new RoleRepresentation("subRole1", "");
//        RoleRepresentation subRole2 = new RoleRepresentation("subRole2", "");
//        List<RoleRepresentation> testRoles = new ArrayList<>();
//        compositeRole.setComposite(true);
//        testRoles.add(compositeRole);
//        testRoles.add(subRole1);
//        testRoles.add(subRole2);
//
//        //create roles and user
//        for (RoleRepresentation role : testRoles) {
//            roles.addRole(role);
//            flashMessage.waitUntilPresent();
//            assertTrue(flashMessage.getText(), flashMessage.isSuccess());
//            roles.navigateTo();
//            assertEquals(role.getName(), roles.findRole(role.getName()).getName());
//        }
//        users.navigateTo();
//        createUser(testUserRep);
//        flashMessage.waitUntilPresent();
//        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
//
//        //adding subroles to composite role
//        roles.navigateTo();
//        roles.findRole(compositeRole.getName());
//        roles.clickRole(compositeRole);
//        roles.setCompositeRole(compositeRole);
//        roleMappings.addAvailableRole(subRole1.getName(), subRole2.getName());
//        //flashMessage.waitUntilPresent();
//        //assertTrue(flashMessage.getText(), flashMessage.isSuccess()); KEYCLOAK-1497
//
//        //check if subroles work as expected
//        users.navigateTo();
//        users.findUser(testUserRep.getUsername());
//        users.clickUser(testUserRep.getUsername());
//        user.tabs().roleMappings();
//        roleMappings.addAvailableRole(compositeRole.getName());
//        assertTrue(roleMappings.isEffectiveRealmRolesComplete(compositeRole, subRole1, subRole2));
//
//        //delete everything
//        roles.navigateTo();
//        roles.deleteRole(compositeRole);
//        roles.navigateTo();
//        roles.deleteRole(subRole1);
//        roles.navigateTo();
//        roles.deleteRole(subRole2);
//        try {
//            Thread.sleep(2000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        users.navigateTo();
//        users.deleteUser(testUserRep.getUsername());
//    }
//

}

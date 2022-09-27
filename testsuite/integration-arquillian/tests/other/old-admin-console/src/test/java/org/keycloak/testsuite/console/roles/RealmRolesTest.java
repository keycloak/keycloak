package org.keycloak.testsuite.console.roles;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.testsuite.console.page.roles.CreateRole;
import org.keycloak.testsuite.console.page.roles.RealmRoles;
import org.keycloak.testsuite.console.page.roles.RoleDetails;
import org.keycloak.testsuite.util.Timer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.keycloak.models.Constants;
import org.keycloak.testsuite.console.page.roles.DefaultRoles;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlEquals;
import static org.keycloak.testsuite.util.WaitUtils.pause;

/**
 *
 * @author Petr Mensik
 * @author tkyjovsk
 */
public class RealmRolesTest extends AbstractRolesTest {
    
    @Page
    private RealmRoles realmRolesPage;
    @Page
    private CreateRole createRolePage;
    @Page
    private RoleDetails roleDetailsPage;
    @Page
    private DefaultRoles defaultRolesPage;

    private RoleRepresentation testRole;
    
    @Before
    public void beforeTestAddNewRole() {
        testRole = new RoleRepresentation("test_role", "role description", false);
        realmRolesPage.navigateTo();
    }
    
    public void addRole(RoleRepresentation roleRep) {
        assertCurrentUrlEquals(realmRolesPage);
        realmRolesPage.table().addRole();
        assertCurrentUrlEquals(createRolePage);
        createRolePage.form().setBasicAttributes(roleRep);
        createRolePage.form().save();
        assertAlertSuccess();
        createRolePage.form().setCompositeRoles(roleRep);
        // TODO add verification of notification message when KEYCLOAK-1497 gets resolved
    }
    
    public void updateRole(RoleRepresentation roleRep) {
        assertCurrentUrlEquals(realmRolesPage);
        realmRolesPage.table().editRole(roleRep.getName());
//        assertCurrentUrl(role); // can't do this, role id needed as uri param
        roleDetailsPage.form().setBasicAttributes(roleRep);
        roleDetailsPage.form().save();
        assertAlertSuccess();
        roleDetailsPage.form().setCompositeRoles(roleRep);
    }
    
    public void assertBasicRoleAttributesEqual(RoleRepresentation r1, RoleRepresentation r2) {
        assertEquals(r1.getName(), r2.getName());
        assertEquals(r1.getDescription(), r2.getDescription());
        assertEquals(r1.isComposite(), r2.isComposite());
    }
    
    @Test
    @Ignore
    public void crudRole() {
        addRole(testRole);
        
        configure().roles();
        RoleRepresentation foundRole = realmRolesPage.table().findRole(testRole.getName()); // search & get role from table
        assertBasicRoleAttributesEqual(testRole, foundRole);
        realmRolesPage.table().editRole(testRole.getName());
        foundRole = roleDetailsPage.form().getBasicAttributes();
        assertBasicRoleAttributesEqual(testRole, foundRole);
        
        testRole.setDescription("updated role description");
        roleDetailsPage.form().setDescription(testRole.getDescription());
        roleDetailsPage.form().save();
        assertAlertSuccess();
        
        configure().roles();
        foundRole = realmRolesPage.table().findRole(testRole.getName()); // search & get role from table
        assertBasicRoleAttributesEqual(testRole, foundRole);

        // delete from table
        realmRolesPage.table().deleteRole(testRole.getName());
        modalDialog.cancel();
        assertTrue(realmRolesPage.table().containsRole(testRole.getName()));
        realmRolesPage.table().deleteRole(testRole.getName());
        modalDialog.confirmDeletion();
        pause(250);
        assertFalse(realmRolesPage.table().containsRole(testRole.getName()));

        // add again
        addRole(testRole);
        // delete from page
        roleDetailsPage.form().delete();
        modalDialog.confirmDeletion();
        assertCurrentUrlEquals(realmRolesPage);
    }
    
    @Test
    @Ignore
    public void testAddRoleWithLongName() {
        String name = "hjewr89y1894yh98(*&*&$jhjkashd)*(&y8934h*&@#hjkahsdj";
        addRole(new RoleRepresentation(name, "", false));
        assertNotNull(realmRolesPage.table().findRole(name));
    }
    
    @Test
    public void testAddExistingRole() {
        addRole(testRole);
        
        configure().roles();
        realmRolesPage.table().addRole();
        createRolePage.form().setBasicAttributes(testRole);
        createRolePage.form().save();
        assertAlertDanger();
    }

    @Test
    public void testDefaultRoleWithinRoleList() {
        //test role name link leads to Default Roles tab
        configure().roles();
        realmRolesPage.table().clickRole(Constants.DEFAULT_ROLES_ROLE_PREFIX + "-test");
        defaultRolesPage.assertCurrent();

        //test role edit button leads to Default Roles tab        
        configure().roles();
        realmRolesPage.table().editRole(Constants.DEFAULT_ROLES_ROLE_PREFIX + "-test");
        defaultRolesPage.assertCurrent();

        //test delete default role doesn't work
        configure().roles();
        realmRolesPage.table().deleteRole(Constants.DEFAULT_ROLES_ROLE_PREFIX + "-test");
        assertTrue(realmRolesPage.table().containsRole(Constants.DEFAULT_ROLES_ROLE_PREFIX + "-test"));
    }

    public void createTestRoles(String namePrefix, int count) {
        Timer.DEFAULT.reset();
        for (int i = 0; i < count; i++) {
            String roleName = String.format("%s%02d", namePrefix, i);
            RoleRepresentation rr = new RoleRepresentation(roleName, "", false);
            testRealmResource().roles().create(rr);
        }
        Timer.DEFAULT.reset("create " + count + " roles");
    }

//    @Test
    public void rolesPagination() {
        createTestRoles("test_role_", 100);
        realmRolesPage.navigateTo();
        pause(100000);
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

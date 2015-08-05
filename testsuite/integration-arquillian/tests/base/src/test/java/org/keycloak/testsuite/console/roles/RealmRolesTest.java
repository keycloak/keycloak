package org.keycloak.testsuite.console.roles;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Test;
import org.keycloak.testsuite.console.page.roles.RealmRoles;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Ignore;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.testsuite.console.page.roles.CreateRole;
import org.keycloak.testsuite.console.page.roles.Role;
import org.keycloak.testsuite.console.page.roles.RoleForm;
import org.keycloak.testsuite.console.page.roles.RolesTable;
import static org.keycloak.testsuite.util.PageAssert.assertCurrentUrl;

/**
 *
 * @author Petr Mensik
 * @author tkyjovsk
 */
public class RealmRolesTest extends AbstractRolesTest {

    @Page
    private RealmRoles realmRoles;
    @Page
    private CreateRole createRole;
    @Page
    private Role role;

    private RoleRepresentation testRole;

    @Before
    public void beforeTestAddNewRole() {
        testRole = new RoleRepresentation("test_role", "role description");
        configure().roles();
    }

    public void addRole(RoleRepresentation roleRep) {
        assertCurrentUrl(realmRoles);
        realmRoles.table().addRole();
        assertCurrentUrl(createRole);
        createRole.form().setBasicAttributes(roleRep);
        createRole.form().save();
        assertFlashMessageSuccess();
        createRole.form().setCompositeRoles(roleRep);
        // TODO add verification of notification message when KEYCLOAK-1497 gets resolved
    }

    public void updateRole(RoleRepresentation roleRep) {
        assertCurrentUrl(realmRoles);
        realmRoles.table().editRole(roleRep);
//        assertCurrentUrl(role); // can't do this, role id needed as uri param
        role.form().setBasicAttributes(roleRep);
        role.form().save();
        assertFlashMessageSuccess();
        role.form().setCompositeRoles(roleRep);
    }

    public void assertBasicRoleAttributesEqual(RoleRepresentation r1, RoleRepresentation r2) {
        assertEquals(r1.getName(), r2.getName());
        assertEquals(r1.getDescription(), r2.getDescription());
        assertEquals(r1.isComposite(), r2.isComposite());
    }

    protected RolesTable realmRolesTable() {
        return realmRoles.table();
    }
    
    @Test
    // TODO use updateRole
    public void testCRUDRole() {
        addRole(testRole);

        configure().roles();
        RoleRepresentation foundRole = realmRoles.table().findRole(testRole.getName()); // search & get role from table
        assertBasicRoleAttributesEqual(foundRole, testRole);
        realmRoles.table().editRole(testRole);
        foundRole = role.form().getBasicAttributes();
        assertBasicRoleAttributesEqual(foundRole, testRole);

        testRole.setDescription("updated role description");
        role.form().setDescription(testRole.getDescription());
        role.form().save();
        assertFlashMessageSuccess();

        configure().roles();
        foundRole = realmRoles.table().findRole(testRole.getName()); // search & get role from table
        assertBasicRoleAttributesEqual(foundRole, testRole);

        // delete from table
        realmRoles.table().deleteRole(testRole.getName(), false); // cancel deletion
        assertFalse(realmRoles.table().searchRoles(testRole.getName()).isEmpty());
        realmRoles.table().deleteRole(testRole.getName(), true); // confirm deletion
        assertTrue(realmRoles.table().searchRoles(testRole.getName()).isEmpty());

        // add again
        addRole(testRole);
        // delete from page
        String urlBeforeDelete = driver.getCurrentUrl();
        role.form().delete(false); // cancel deletion
        assertCurrentUrl(driver, urlBeforeDelete);
        role.form().delete(true); // confirm deletion
        assertCurrentUrl(realmRoles);
    }

    @Test
    @Ignore
    public void testAddRoleWithLongName() {
        String name = "hjewr89y1894yh98(*&*&$jhjkashd)*(&y8934h*&@#hjkahsdj";
        addRole(new RoleRepresentation(name, ""));
        assertNotNull(realmRoles.table().findRole(name));
    }

    @Test
    public void testAddExistingRole() {
        addRole(testRole);
        assertFlashMessageSuccess();

        configure().roles();
        addRole(testRole);
        assertFlashMessageDanger();
    }
    
    @Test
    public void testAddCompositeRole() {
        addRole(testRole);
    }

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

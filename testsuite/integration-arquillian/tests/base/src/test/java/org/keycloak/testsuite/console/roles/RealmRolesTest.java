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
        role.role().setBasicAttributes(roleRep);
        role.role().save();
        assertFlashMessageSuccess();
        role.role().setCompositeRoles(roleRep);
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
    @Ignore // FIXME findRole
    public void crudRole() {
        addRole(testRole);

        configure().roles();
        RoleRepresentation foundRole = realmRoles.table().findRole(testRole.getName()); // search & get role from table
        assertBasicRoleAttributesEqual(testRole, foundRole);
        realmRoles.table().editRole(testRole);
        foundRole = role.role().getBasicAttributes();
        assertBasicRoleAttributesEqual(testRole, foundRole);

        testRole.setDescription("updated role description");
        role.role().setDescription(testRole.getDescription());
        role.role().save();
        assertFlashMessageSuccess();

        configure().roles();
        foundRole = realmRoles.table().findRole(testRole.getName()); // search & get role from table
        assertBasicRoleAttributesEqual(testRole, foundRole);

        // delete from table
        realmRoles.table().deleteRole(testRole.getName(), false); // cancel deletion
        assertNull(realmRoles.table().findRole(testRole.getName()));
        realmRoles.table().deleteRole(testRole.getName(), true); // confirm deletion
        assertNotNull(realmRoles.table().findRole(testRole.getName()));

        // add again
        addRole(testRole);
        // delete from page
        String urlBeforeDelete = driver.getCurrentUrl();
        role.role().delete(false); // cancel deletion
        assertCurrentUrl(driver, urlBeforeDelete);
        role.role().delete(true); // confirm deletion
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
        realmRoles.table().addRole();
        createRole.form().setBasicAttributes(testRole);
        createRole.form().save();
        assertFlashMessageDanger();
    }

    @Test
    @Ignore
    public void testAddCompositeRole() {
        addRole(testRole);
    }

}

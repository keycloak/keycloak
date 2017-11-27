/**
 * 
 */
package org.keycloak.testsuite.console.roles;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.console.page.roles.DefaultRoles;
import org.keycloak.testsuite.console.page.roles.RealmRoles;
import org.keycloak.testsuite.console.page.roles.UsersInRole;
import org.keycloak.testsuite.console.page.users.UserRoleMappings;
import org.keycloak.testsuite.util.RealmRepUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.admin.ApiUtil.assignRealmRoles;
import static org.keycloak.testsuite.admin.ApiUtil.createUserWithAdminClient;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlEquals;

/**
 * See KEYCLOAK-2035
 *
 * @author <a href="mailto:antonio.ferreira@fiercely.pt">Antonio Ferreira</a>
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class UsersInRoleTest extends AbstractRolesTest {
    

    @Page
    private DefaultRoles defaultRolesPage;

    @Page
    private UserRoleMappings userRolesPage;

    @Page
    private UsersInRole usersInRolePage;

    @Page
    private RealmRoles realmRolesPage;

    private RoleRepresentation testRoleRep;
    private RoleRepresentation emptyTestRoleRep;
    private UserRepresentation newUser;

    @Before
    public void beforeUsersInRoleTestClass() {
        // create a role via admin client
        testRoleRep = new RoleRepresentation("test-role", "", false);
        testRealmResource().roles().create(testRoleRep);

        emptyTestRoleRep = new RoleRepresentation("empty-test-role", "", false);
        testRealmResource().roles().create(emptyTestRoleRep);

        newUser = new UserRepresentation();
        newUser.setUsername("test_user");
        newUser.setEnabled(true);
        newUser.setId(createUserWithAdminClient(testRealmResource(), newUser));

        assignRealmRoles(testRealmResource(), newUser.getId(), testRoleRep.getName());

        userPage.setId(newUser.getId());
    }

    @Test
    public void userInRoleIsPresent() {
        // Clicking user name link
        navigateToUsersInRole(testRoleRep);
        assertEquals(1, usersInRolePage.usersTable().getUsersFromTableRows().size());
        usersInRolePage.usersTable().clickUser(newUser.getUsername());
        assertCurrentUrlEquals(userPage);

        // Clicking edit button
        navigateToUsersInRole(testRoleRep);
        usersInRolePage.usersTable().editUser(newUser.getUsername());
        assertCurrentUrlEquals(userPage);
    }

    @Test
    public void emptyRoleTest() {
        navigateToUsersInRole(emptyTestRoleRep);
        assertEquals(0, usersInRolePage.usersTable().getUsersFromTableRows().size());
        assertTrue("No roles members message is not displayed", usersInRolePage.usersTable().noRoleMembersIsDisplayed());
    }

    private void navigateToUsersInRole(RoleRepresentation role) {
        realmRolesPage.navigateTo();
        realmRolesPage.tabs().realmRoles();
        realmRolesPage.table().clickRole(role.getName());
        usersInRolePage.roleTabs().usersInRole();
    }
}

package org.keycloak.testsuite.console.roles;

import org.jboss.arquillian.graphene.page.Page;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import static org.keycloak.testsuite.admin.ApiUtil.createUserWithAdminClient;
import org.keycloak.testsuite.console.page.roles.DefaultRoles;
import org.keycloak.testsuite.console.page.users.UserRoleMappings;
import org.keycloak.testsuite.console.page.users.Users;

/**
 * Created by fkiss.
 */
public class DefaultRolesTest extends AbstractRolesTest {

    @Page
    private DefaultRoles defaultRoles;

    @Page
    private UserRoleMappings userRoleMappings;

    private RoleRepresentation defaultRoleRep;

    @Page
    private Users users;

    @Before
    public void beforeDefaultRolesTest() {
        // create a role via admin client
        defaultRoleRep = new RoleRepresentation("default-role", "");
        roles.rolesResource().create(defaultRoleRep);

        // navigate to default roles page
        roles.tabs().defaultRoles();
    }

    @Test
    public void defaultRoleAssignedToNewUser() {

        String defaultRoleName = defaultRoleRep.getName();

        defaultRoles.form().addAvailableRole(defaultRoleName);
        assertFlashMessageSuccess();

        UserRepresentation newUser = new UserRepresentation();
        newUser.setUsername("new_user");

        createUserWithAdminClient(testRealmResource(), newUser);
        users.navigateTo();
        users.table().search(newUser.getUsername());
        users.table().clickUser(newUser.getUsername());

        user.tabs().roleMappings();
        assertTrue(userRoleMappings.form().isAssignedRole(defaultRoleName));
    }

}

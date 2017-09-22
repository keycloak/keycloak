/**
 * 
 */
package org.keycloak.testsuite.console.roles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.admin.ApiUtil.createUserWithAdminClient;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.console.page.roles.DefaultRoles;
import org.keycloak.testsuite.console.page.roles.RealmRoles;
import org.keycloak.testsuite.console.page.roles.Role;
import org.keycloak.testsuite.console.page.roles.Roles;
import org.keycloak.testsuite.console.page.users.UserRoleMappings;
import org.keycloak.testsuite.console.page.users.Users;

/**
 * @author <a href="mailto:antonio.ferreira@fiercely.pt">Antonio Ferreira</a>
 *
 */
public class UsersInRoleTest extends AbstractRolesTest {
    

    @Page
    private DefaultRoles defaultRolesPage;

    @Page
    private UserRoleMappings userRolesPage;
    
    @Page
    private Users usersPage;
    
    @Page
    private Roles rolesPage;
    
    @Page
    private Role rolePage;

    @Page
    private RealmRoles realmRolesPage;
    
    private RoleRepresentation testRoleRep;
    private UserRepresentation newUser;



    @Before
    public void beforeDefaultRolesTest() {
        // create a role via admin client
        testRoleRep = new RoleRepresentation("test-role", "", false);
        rolesResource().create(testRoleRep);

        newUser = new UserRepresentation();
        newUser.setUsername("test_user");
        newUser.setEnabled(true);
        newUser.setEmail("test-role-member@test-role-member.com");
        newUser.setRequiredActions(Collections.<String>emptyList());
        //testRealmResource().users().create(newUser);
        createUserWithAdminClient(testRealmResource(), newUser);
        rolesResource().create(testRoleRep);
        rolesPage.navigateTo();
    }


    public RolesResource rolesResource() {
        return testRealmResource().roles();
    }
    
    //Added for KEYCLOAK-2035
    @Test
    public void usersInRoleTabIsPresent() {

        rolesPage.navigateTo();
        rolesPage.tabs().realmRoles();
        realmRolesPage.table().search(testRoleRep.getName());
        realmRolesPage.table().clickRole(testRoleRep.getName());
        //assert no users in list
        //Role Page class missing a getUsers() method        
        
        List<UserRepresentation> users = testRealmResource().users().search("test_user", null, null, null, null, null);
        assertEquals(1, users.size());
        UserResource user = testRealmResource().users().get(users.get(0).getId());
        UserRepresentation userRep = user.toRepresentation();

        usersPage.navigateTo();
        usersPage.table().search(userRep.getUsername());
        usersPage.table().clickUser(userRep.getUsername());

        assertFalse(userRolesPage.form().isAssignedRole(testRoleRep.getName()));
        
        RoleResource roleResource = testRealmResource().roles().get(testRoleRep.getName());        
        List<RoleRepresentation> rolesToAdd = new LinkedList<>();
        rolesToAdd.add(roleResource.toRepresentation());
        testRealmResource().users().get(userRep.getId()).roles().realmLevel().add(rolesToAdd);        
        
        rolesPage.navigateTo();
        rolesPage.tabs().realmRoles();
        realmRolesPage.table().search(testRoleRep.getName());
        realmRolesPage.table().clickRole(testRoleRep.getName());
        
        assertTrue(userRolesPage.form().isAssignedRole(testRoleRep.getName()));
    }


}

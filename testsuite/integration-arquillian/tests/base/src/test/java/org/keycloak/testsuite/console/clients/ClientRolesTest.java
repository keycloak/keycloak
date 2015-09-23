package org.keycloak.testsuite.console.clients;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Test;
import org.keycloak.testsuite.console.page.users.UserRoleMappingsForm;

import static org.junit.Assert.*;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.testsuite.console.page.clients.ClientRole;
import org.keycloak.testsuite.console.page.clients.ClientRoles;
import org.keycloak.testsuite.console.page.clients.CreateClientRole;
import org.keycloak.testsuite.console.page.users.User;

/**
 * Created by fkiss.
 */
public class ClientRolesTest extends AbstractClientTest {

    @Page
    private ClientRoles clientRolesPage;
    @Page
    private CreateClientRole createClientRolePage;
    @Page
    private ClientRole clientRolePage;

    @Page
    private User userPage; // note: cannot call navigateTo() unless user id is set

    @Page
    private UserRoleMappingsForm userRolesPage;

    public void addClientRole(RoleRepresentation roleRep) {
//        assertCurrentUrl(clientRoles);
        clientRolesPage.roles().addRole();
//        assertCurrentUrl(createClientRole); // can't do this, need client id to build uri
        createClientRolePage.form().setBasicAttributes(roleRep);
        createClientRolePage.form().save();
        assertFlashMessageSuccess();
        createClientRolePage.form().setCompositeRoles(roleRep);
        // TODO add verification of notification message when KEYCLOAK-1497 gets resolved
    }

    @Test
    public void testAddClientRole() {
        ClientRepresentation newClient = createClientRepresentation("test-client1", "http://example.com/*");
        RoleRepresentation newRole = new RoleRepresentation("client-role", "", false);

        createClient(newClient);
        assertFlashMessageSuccess();

        clientPage.tabs().roles();
        addClientRole(newRole);
        assertFlashMessageSuccess();

        clientRolePage.backToClientRolesViaBreadcrumb();
        assertFalse(clientRolesPage.roles().getRolesFromTableRows().isEmpty());

        configure().clients();
        clientsPage.table().search(newClient.getClientId());
        clientsPage.table().deleteClient(newClient.getClientId());
        modalDialog.confirmDeletion();
        assertFlashMessageSuccess();
        assertNull(clientsPage.table().findClient(newClient.getClientId()));
    }

//    @Test
//    @Jira("KEYCLOAK-1497")
//    public void testAddClientRoleToUser() {
//        ClientRepresentation newClient = createClientRepresentation("test-client2", "http://example.com/*");
//        RoleRepresentation newRole = new RoleRepresentation("client-role2", "");
//        String testUsername = "test-user2";
//        UserRepresentation newUser = new UserRepresentation();
//        newUser.setUsername(testUsername);
//        newUser.credential(PASSWORD, "pass");
//
//        createClient(newClient);
//        assertFlashMessageSuccess();
//
//        client.tabs().roles();
//        addClientRole(newRole);
//        assertFlashMessageSuccess();
//
//        clientRole.backToClientRolesViaBreadcrumb();
//        assertFalse(clientRoles.table().searchRoles(newRole.getName()).isEmpty());
//
//        users.navigateTo();
//        createUser(newUser);
//        flashMessage.waitUntilPresent();
//        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
//
//        users.navigateTo();
//        users.findUser(testUsername);
//        users.clickUser(testUsername);
//
//        user.tabs().roleMappings();
//        roleMappings.selectClientRole(newClient.getClientId());
//        roleMappings.addAvailableClientRole(newRole.getName());
//        //flashMessage.waitUntilPresent();
//        //assertTrue(flashMessage.getText(), flashMessage.isSuccess());
//        //KEYCLOAK-1497
//        assertTrue(roleMappings.isAssignedClientRole(newRole.getName()));
//
//        users.navigateTo();
//        users.deleteUser(testUsername);
//
//        clients.navigateTo();
//        clients.deleteClient(newClient.getClientId());
//        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
//        assertNull(clients.findClient(newClient.getClientId()));
//    }
//
//    @Test
//    @Jira("KEYCLOAK-1496, KEYCLOAK-1497")
//    @Ignore // TODO use REST to create test data (user/roles)
//    public void testAddCompositeRealmClientRoleToUser() {
//        ClientRepresentation newClient = createClientRepresentation("test-client3", "http://example.com/*");
//        RoleRepresentation clientCompositeRole = new RoleRepresentation("client-composite-role", "");
//        String testUsername = "test-user3";
//        UserRepresentation newUser = new UserRepresentation();
//        newUser.setUsername(testUsername);
//        newUser.credential(PASSWORD, "pass");
//
//        RoleRepresentation subRole1 = new RoleRepresentation("sub-role1", "");
//        RoleRepresentation subRole2 = new RoleRepresentation("sub-role2", "");
//        List<RoleRepresentation> testRoles = new ArrayList<>();
//        clientCompositeRole.setComposite(true);
//        testRoles.add(subRole1);
//        testRoles.add(subRole2);
//
//        //create sub-roles
//        configure().roles();
//        for (RoleRepresentation role : testRoles) {
//            realmRoles.addRole(role);
//            flashMessage.waitUntilPresent();
//            assertTrue(flashMessage.getText(), flashMessage.isSuccess());
//            configure().roles();
//            assertEquals(role.getName(), realmRoles.findRole(role.getName()).getName());
//        }
//
//        //create client
//        clients.navigateTo();
//        createClient(newClient);
//        flashMessage.waitUntilPresent();
//        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
//
//        //add client role
//        configure().roles();
//        realmRoles.addRole(clientCompositeRole);
//        flashMessage.waitUntilPresent();
//        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
//
//        //add realm composite roles
//        realmRoles.setCompositeRole(clientCompositeRole);
//        roleMappings.addAvailableRole(subRole1.getName(), subRole2.getName());
//        //flashMessage.waitUntilPresent();
//        //assertTrue(flashMessage.getText(), flashMessage.isSuccess());
//        //KEYCLOAK-1497
//
//        //create user
//        users.navigateTo();
//        createUser(newUser);
//        flashMessage.waitUntilPresent();
//        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
//
//        //add client role to user and verify
//        users.navigateTo();
//        users.findUser(testUsername);
//        users.clickUser(testUsername);
//
//        user.tabs().roleMappings();
//        roleMappings.selectClientRole(newClient.getClientId());
//        roleMappings.addAvailableClientRole(clientCompositeRole.getName());
//        //flashMessage.waitUntilPresent();
//        //assertTrue(flashMessage.getText(), flashMessage.isSuccess());
//        //KEYCLOAK-1497
//        assertTrue(roleMappings.isAssignedClientRole(clientCompositeRole.getName()));
//        assertTrue(roleMappings.isEffectiveRealmRolesComplete(subRole1, subRole2)); //KEYCLOAK-1496
//        assertTrue(roleMappings.isEffectiveClientRolesComplete(clientCompositeRole));
//
//        //delete everything
//        users.navigateTo();
//        users.deleteUser(testUsername);
//
//        configure().roles();
//        realmRoles.deleteRole(subRole1);
//        configure().roles();
//        realmRoles.deleteRole(subRole2);
//
//        clients.navigateTo();
//        clients.deleteClient(newClient.getClientId());
//        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
//        assertNull(clients.findClient(newClient.getClientId()));
//    }
//
//    @Test
//    @Jira("KEYCLOAK-1504, KEYCLOAK-1497")
//    public void testAddCompositeClientRoleToUser() {
//        ClientRepresentation newClient = createClientRepresentation("test-client4", "http://example.com/*");
//        RoleRepresentation clientCompositeRole = new RoleRepresentation("client-composite-role2", "");
//        String testUsername = "test-user4";
//        UserRepresentation newUser = new UserRepresentation();
//        newUser.setUsername(testUsername);
//        newUser.credential(PASSWORD, "pass");
//
//        RoleRepresentation subRole1 = new RoleRepresentation("client-sub-role1", "");
//        RoleRepresentation subRole2 = new RoleRepresentation("client-sub-role2", "");
//        List<RoleRepresentation> testRoles = new ArrayList<>();
//        clientCompositeRole.setComposite(true);
//        testRoles.add(clientCompositeRole);
//        testRoles.add(subRole1);
//        testRoles.add(subRole2);
//
//        //create client
//        createClient(newClient);
//        flashMessage.waitUntilPresent();
//        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
//
//        //create sub-roles
//        configure().roles();
//        for (RoleRepresentation role : testRoles) {
//            clients.navigateTo();
//            clients.clickClient(newClient.getClientId());
//            configure().roles();
//            realmRoles.addRole(role);
//            flashMessage.waitUntilPresent();
//            assertTrue(flashMessage.getText(), flashMessage.isSuccess());
//        }
//
//        //add client composite roles
//        clients.navigateTo();
//        clients.clickClient(newClient);
//        configure().roles();
//        realmRoles.clickRole(clientCompositeRole);
//        realmRoles.setCompositeRole(clientCompositeRole);
//        roleMappings.selectClientRole(newClient.getClientId());
//        roleMappings.addAvailableClientRole(subRole1.getName(), subRole2.getName());
//        //flashMessage.waitUntilPresent();
//        //assertTrue(flashMessage.getText(), flashMessage.isSuccess());
//        //KEYCLOAK-1504, KEYCLOAK-1497
//
//        //create user
//        users.navigateTo();
//        createUser(newUser);
//        flashMessage.waitUntilPresent();
//        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
//
//        //add client role to user and verify
//        users.navigateTo();
//        users.findUser(testUsername);
//        users.clickUser(testUsername);
//
//        user.tabs().roleMappings();
//        roleMappings.selectClientRole(newClient.getClientId());
//        roleMappings.addAvailableClientRole(clientCompositeRole.getName());
//        assertTrue(roleMappings.isAssignedClientRole(clientCompositeRole.getName()));
//        assertTrue(roleMappings.isEffectiveClientRolesComplete(clientCompositeRole, subRole1, subRole2));
//
//        //delete everything
//        users.navigateTo();
//        users.deleteUser(testUsername);
//
//        configure().roles();
//        realmRoles.deleteRole(subRole1);
//        configure().roles();
//        realmRoles.deleteRole(subRole2);
//
//        clients.navigateTo();
//        clients.deleteClient(newClient.getClientId());
//        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
//        assertNull(clients.findClient(newClient.getClientId()));
//    }
}

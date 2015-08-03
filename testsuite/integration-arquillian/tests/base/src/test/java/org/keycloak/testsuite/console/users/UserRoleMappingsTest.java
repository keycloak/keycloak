package org.keycloak.testsuite.console.users;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.keycloak.representations.idm.CredentialRepresentation.PASSWORD;
import org.keycloak.testsuite.console.page.users.UserRoleMappings;

/**
 * Created by fkiss.
 */
public class UserRoleMappingsTest extends AbstractUserTest {

    @Page
    private UserRoleMappings roleMappings;
    
    @Test
    public void addUserAndAssignRole() {
        String testUsername = "tester1";
        testUser.setUsername(testUsername);
        testUser.credential(PASSWORD, "pass");
        createUser(testUser);
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
        users.navigateTo();
        users.clickUser(testUsername);
        roleMappings.tabs().roleMappings();

        roleMappings.form().addAvailableRole("create-realm");
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
        users.navigateTo();
        users.deleteUser(testUsername);
    }

    @Test
    public void addAndRemoveUserAndAssignRole() {
        String testUsername = "tester2";
        testUser.setUsername(testUsername);
        testUser.credential(PASSWORD, "pass");
        createUser(testUser);
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
        users.navigateTo();
        users.clickUser(testUsername);
        
        roleMappings.tabs().roleMappings();
        roleMappings.form().addAvailableRole("create-realm");
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
        roleMappings.form().removeAssignedRole("create-realm");
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
        users.navigateTo();
        users.deleteUser(testUsername);
    }
}

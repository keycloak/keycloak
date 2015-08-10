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


}

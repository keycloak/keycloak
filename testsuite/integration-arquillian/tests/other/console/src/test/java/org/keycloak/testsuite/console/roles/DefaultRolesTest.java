package org.keycloak.testsuite.console.roles;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.console.page.roles.DefaultRoles;
import org.keycloak.testsuite.console.page.users.UserRoleMappings;
import org.keycloak.testsuite.console.page.users.Users;

import static org.junit.Assert.assertTrue;
import org.junit.Ignore;
import static org.keycloak.testsuite.admin.ApiUtil.createUserWithAdminClient;

/**
 * Created by fkiss.
 */
public class DefaultRolesTest extends AbstractRolesTest {

    @Page
    private DefaultRoles defaultRolesPage;

    @Page
    private UserRoleMappings userRolesPage;

    private RoleRepresentation defaultRoleRep;

    @Page
    private Users users;

    @Before
    public void beforeDefaultRolesTest() {
        // create a role via admin client
        defaultRoleRep = new RoleRepresentation("default-role", "", false);
        rolesResource().create(defaultRoleRep);

        defaultRolesPage.navigateTo();
        // navigate to default roles page
//        rolesPage.tabs().defaultRoles();
    }

    @Ignore
    @Test
    public void defaultRoleAssignedToNewUser() {
        /*  This test is broken because of an apparent problem with Selenium.
            The code below demonstrates that clicking on the option in the
            "Add available realm roles" does not cause the "Add selected" 
            button to become enabled.  So button.click() doesn't work.
            Hopefully, this will be fixed when we upgrade Arquillian Drone
            and Selenium.
        
        WebElement option = driver.findElement(By.xpath("//option"));
        WebElement button = driver.findElement(By.cssSelector("button[ng-click*='addRealm']"));
        
        System.out.println("---------------");
        //System.out.println(driver.getPageSource());
        System.out.println("found option=" + option.getText());
        System.out.println("found button=" + button.getText());
        System.out.println("button isEnabled=" + button.isEnabled());
        System.out.println("---------------");
        option.click();
        button.click();
        */
        
        String defaultRoleName = defaultRoleRep.getName();
        
        defaultRolesPage.form().addAvailableRole(defaultRoleName);
        assertAlertSuccess();
        
        UserRepresentation newUser = new UserRepresentation();
        newUser.setUsername("new_user");

        createUserWithAdminClient(testRealmResource(), newUser);
        users.navigateTo();
        users.table().search(newUser.getUsername());
        users.table().clickUser(newUser.getUsername());

        userPage.tabs().roleMappings();
        assertTrue(userRolesPage.form().isAssignedRole(defaultRoleName));
    }

    public RolesResource rolesResource() {
        return testRealmResource().roles();
    }

}

package org.keycloak.testsuite.console.users;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Test;
import org.keycloak.testsuite.console.AbstractAdminConsoleTest;
import org.keycloak.testsuite.console.page.users.Users;

/**
 *
 * @author tkyjovsk
 */
public class DisableUsersTest extends AbstractAdminConsoleTest {
    
    @Page
    protected Users users;

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
    }
    
    @Test
    public void testDisableUser() {
        testAdminConsoleRealm.navigateTo();
        testAdminConsoleRealm.users();
    }
    
}
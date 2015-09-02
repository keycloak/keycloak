package org.keycloak.testsuite.console.roles;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.keycloak.testsuite.console.AbstractConsoleTest;
import org.keycloak.testsuite.console.page.roles.Roles;
import org.keycloak.testsuite.console.page.users.User;

/**
 *
 * @author tkyjovsk
 */
public abstract class AbstractRolesTest extends AbstractConsoleTest {
    
    @Page
    protected Roles roles;
    
    @Page
    protected User user;
    
    @Before
    public void beforeRolesTest() {
        configure().roles();
    }
    
}

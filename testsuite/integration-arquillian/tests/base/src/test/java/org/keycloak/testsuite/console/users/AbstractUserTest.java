package org.keycloak.testsuite.console.users;

import org.junit.Before;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.console.AbstractConsoleTest;

/**
 *
 * @author tkyjovsk
 */
public abstract class AbstractUserTest extends AbstractConsoleTest {

    protected UserRepresentation testUser;

    @Before
    public void beforeRoleMappingsTest() {
        testUser = new UserRepresentation();
        users.navigateTo();
    }
    
}

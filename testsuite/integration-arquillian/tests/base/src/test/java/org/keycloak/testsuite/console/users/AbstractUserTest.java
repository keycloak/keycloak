package org.keycloak.testsuite.console.users;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.console.AbstractConsoleTest;
import org.keycloak.testsuite.console.page.users.CreateUser;
import org.keycloak.testsuite.console.page.users.Users;
import static org.keycloak.testsuite.util.PageAssert.assertCurrentUrl;

/**
 *
 * @author tkyjovsk
 */
public abstract class AbstractUserTest extends AbstractConsoleTest {

    @Page
    protected Users users;
    @Page
    protected CreateUser createUser;

    @Before
    public void beforeRoleMappingsTest() {
        testUser = new UserRepresentation();
        users.navigateTo();
    }

    public void createUser(UserRepresentation user) {
        assertCurrentUrl(users);
        users.addUser();
        assertCurrentUrl(createUser);
        createUser.form().setValues(user);
        createUser.form().save();
    }

}

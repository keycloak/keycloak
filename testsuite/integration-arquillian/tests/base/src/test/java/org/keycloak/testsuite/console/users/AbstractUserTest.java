package org.keycloak.testsuite.console.users;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.console.AbstractConsoleTest;
import org.keycloak.testsuite.console.page.users.CreateUser;
import org.keycloak.testsuite.console.page.users.Users;
import static org.keycloak.testsuite.util.PageAssert.assertCurrentUrl;
import static org.keycloak.testsuite.util.PageAssert.assertCurrentUrlStartsWith;

/**
 *
 * @author tkyjovsk
 */
public abstract class AbstractUserTest extends AbstractConsoleTest {

    @Page
    protected Users users;
    @Page
    protected CreateUser createUser;

    protected UserRepresentation newTestRealmUser;

    @Before
    public void beforeUserTest() {
        newTestRealmUser = new UserRepresentation();
        manage().users();
    }

    public void createUser(UserRepresentation user) {
        assertCurrentUrl(users);
        users.table().addUser();
        assertCurrentUrlStartsWith(createUser);
        createUser.form().setValues(user);
        createUser.form().save();
    }

}
